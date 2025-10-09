package com.example.speak.level;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.speak.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Clase principal que maneja la evaluación de pronunciación en inglés.
 * Esta clase es como un profesor que escucha cómo pronuncias y te da una calificación.
 */
public class LevelEvaluator {
    // Constante para identificar mensajes en el log (como una etiqueta)
    private static final String TAG = "LevelEvaluator";

    // Variables de la clase (como las propiedades de un objeto)
    private final Context context;     // El contexto de la aplicación (necesario para muchas operaciones)
    private SpeechRecognizer speechRecognizer;  // El objeto que reconoce la voz
    private String referenceText;      // El texto que el usuario debe pronunciar
    private PronunciationCallback callback;  // Interfaz para comunicar resultados
    private boolean isOfflineMode = false;  // Indica si trabajamos sin internet
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private DatabaseHelper dbHelper;   // Ayudante para la base de datos
    private LanguageIdentifier languageIdentifier;

    /**
     * Interfaz que define cómo se comunicarán los resultados.
     * Es como un contrato que dice qué métodos deben implementar las clases que quieran
     * recibir los resultados de la evaluación.
     */
    public interface PronunciationCallback {
        /**
         * Se llama cuando se completa la evaluación
         * @param score La calificación obtenida (0-100)
         * @param recognizedText Lo que el sistema entendió que dijiste
         */
        void onResult(double score, String recognizedText);

        /**
         * Se llama cuando algo sale mal
         * @param error El mensaje que explica qué salió mal
         */
        void onError(String error);

        /**
         * Se llama cuando el sistema detecta que estás hablando
         */
        void onSpeechDetected();

        /**
         * Se llama mientras estás hablando, con lo que el sistema va entendiendo
         * @param partialText Lo que el sistema ha entendido hasta ahora
         */
        void onPartialResult(String partialText);
    }

    /**
     * Constructor: se llama cuando creamos un nuevo evaluador
     * @param context El contexto de la aplicación
     */
    public LevelEvaluator(Context context) {
        this.context = context;  // Guardamos el contexto
        this.dbHelper = new DatabaseHelper(context);  // Creamos el ayudante de base de datos
        initializeLanguageIdentifier();
        initializeSpeechRecognizer();
    }

    private void initializeLanguageIdentifier() {
        languageIdentifier = LanguageIdentification.getClient();
    }

    /**
     * Configura si trabajaremos con o sin internet
     * @param offlineMode true = sin internet, false = con internet
     */
    public void setOfflineMode(boolean offlineMode) {
        this.isOfflineMode = offlineMode;
    }

    /**
     * Establece el texto que el usuario debe pronunciar
     * @param text El texto de referencia
     */
    public void setReferenceText(String text) {
        this.referenceText = text.toLowerCase();
    }

    public void setCallback(PronunciationCallback callback) {
        this.callback = callback;
    }

    /**
     * Inicializa el reconocedor de voz
     * Verifica si el dispositivo puede reconocer voz y lo configura
     */
    private void initializeSpeechRecognizer() {
        try {
            // Verificamos si el dispositivo puede reconocer voz
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                // Si ya existe un reconocedor, lo destruimos
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }
                // Creamos un nuevo reconocedor
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                setupSpeechRecognizer();  // Lo configuramos
            } else {
                // Si no está disponible, mostramos un error
                Log.e(TAG, "Speech recognition is not available on this device");
                if (callback != null) {
                    callback.onError("El reconocimiento de voz no está disponible en este dispositivo");
                }
            }
        } catch (Exception e) {
            // Si algo sale mal, lo registramos
            Log.e(TAG, "Error initializing speech recognizer", e);
            if (callback != null) {
                callback.onError("Error inicializando el reconocedor de voz: " + e.getMessage());
            }
        }
    }

    private boolean loadReferenceText() {
        try {
            InputStream inputStream = context.getAssets().open("SENA_Level_1_A1.1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder text = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }

            referenceText = text.toString().trim().toLowerCase();
            reader.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error loading reference text", e);
            if (callback != null) {
                callback.onError("Error cargando el texto de referencia: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Configura cómo el reconocedor responderá a diferentes eventos
     */
    private void setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            /**
             * Se llama cuando tenemos los resultados finales del reconocimiento
             */
            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "Speech recognition results received");

                // Obtenemos el texto reconocido
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0).toLowerCase();

                    // Si tenemos un texto de referencia, verificamos si es correcto
                    if (referenceText != null) {
                        // Verificar si la pronunciación es correcta (score >= 0.8 = 80%)
                        double score = evaluatePronunciation(spokenText);
                        boolean isCorrect = score >= 0.8;

                        if (isCorrect) {
                            // Pronunciación correcta: detener automáticamente y evaluar
                            isListening.set(false);
                            Log.d(TAG, "Correct pronunciation detected, stopping automatically");

                            // Guardamos el resultado en la base de datos
                            if (dbHelper != null) {
                                long userId = dbHelper.getPronunciationUserId();
                                if (userId == -1) {
                                    userId = dbHelper.getCurrentUserId();
                                    dbHelper.savePronunciationUserId(userId);
                                }
                                
                                // Obtener el tema y nivel actual de la actividad
                                String currentTopic = ((ActivityLevel) context).getCurrentTopic();
                                String currentLevel = ((ActivityLevel) context).getCurrentLevel();
                                
                                long resultId = dbHelper.savePronunciationResult(
                                    userId,
                                    referenceText,
                                    spokenText,
                                    score,
                                    currentTopic,
                                    currentLevel
                                );
                                
                                if (resultId != -1) {
                                    Log.d(TAG, "Pronunciation result saved locally with ID: " + resultId);

                                    // Si estamos en modo online, intentamos sincronizar
                                    if (!isOfflineMode) {
                                        syncPronunciationResult(resultId, referenceText, spokenText, score);
                                    }
                                }
                            }

                            // Notificamos el resultado exitoso
                            if (callback != null) {
                                callback.onResult(score * 100, spokenText);
                            }
                        } else {
                            // Pronunciación incorrecta: continuar escuchando silenciosamente
                            Log.d(TAG, "Incorrect pronunciation detected, continuing to listen silently. Score: " + score);
                            
                            // Solo mostrar el texto reconocido, sin mensajes molestos
                            if (callback != null) {
                                callback.onPartialResult(spokenText);
                            }
                            
                            // NO reiniciar automáticamente - dejar que el usuario decida
                            // El sistema continúa escuchando hasta que el usuario presione Stop o Start
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Texto de referencia no cargado");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("No se pudo reconocer el audio");
                    }
                }
            }

            /**
             * Se llama cuando ocurre un error
             */
            @Override
            public void onError(int error) {
                isListening.set(false);
                Log.e(TAG, "Speech recognition error: " + error);

                // Traducimos el código de error a un mensaje entendible
                String errorMessage = getErrorText(error);

                // Notificamos el error
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }

            /**
             * Se llama cuando el reconocedor está listo para escuchar
             */
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                isListening.set(true);
                if (callback != null) {
                    callback.onError("¡Ahora! Comienza a hablar...");
                }
            }

            /**
             * Se llama cuando comienza el habla
             */
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
                if (callback != null) {
                    callback.onSpeechDetected();
                }
            }

            /**
             * Se llama cuando cambia el nivel de audio
             * @param rmsdB Nivel de audio en decibelios
             */
            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d(TAG, "RMS changed: " + rmsdB);
            }

            /**
             * Se llama cuando se recibe un buffer de audio
             */
            @Override
            public void onBufferReceived(byte[] buffer) {}

            /**
             * Se llama cuando termina el habla
             */
            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
            }

            /**
             * Se llama con resultados parciales durante el reconocimiento
             */
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partialText = matches.get(0).toLowerCase();
                    if (callback != null) {
                        callback.onPartialResult(partialText);
                    }
                }
            }

            /**
             * Se llama para eventos específicos del reconocedor
             */
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void syncPronunciationResult(long resultId, String referenceText, String spokenText, double score) {
        // Solo intentar sincronizar si no estamos en modo offline
        if (!isOfflineMode) {
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference("pronunciation_results");

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                HashMap<String, Object> result = new HashMap<>();
                result.put("referenceText", referenceText);
                result.put("spokenText", spokenText);
                result.put("score", score);
                result.put("timestamp", System.currentTimeMillis());

                ref.child(userId).push().setValue(result)
                        .addOnSuccessListener(aVoid -> {
                            dbHelper.markPronunciationResultAsSynced(resultId);
                            Log.d(TAG, "Pronunciation result synced with Firebase");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error syncing pronunciation result", e);
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error in syncPronunciationResult", e);
            }
        }
    }

    /**
     * Inicia el proceso de escucha del reconocedor de voz
     * @param callback La interfaz que recibirá los resultados
     */
    public void startListening(PronunciationCallback callback) {
        this.callback = callback;

        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
            if (speechRecognizer == null) {
                if (callback != null) {
                    callback.onError("No se pudo inicializar el reconocedor de voz");
                }
                return;
            }
        }

        if (isListening.get()) {
            speechRecognizer.stopListening();
        }

        if (referenceText == null) {
            if (callback != null) {
                callback.onError("No hay texto de referencia para evaluar");
            }
            return;
        }

        if (callback != null) {
            callback.onError("¡Prepárate! La grabación comenzará en 3 segundos...");
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please read the text aloud");
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 0);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 0);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

            // Configuración específica para modo offline
            if (isOfflineMode) {
                intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toString());
                // Forzar el uso del modelo offline
                intent.putExtra("android.speech.extra.FORCE_OFFLINE", true);
                // Deshabilitar la red para asegurar modo offline
                intent.putExtra("android.speech.extra.DISABLE_NETWORK", true);
            }

            try {
                speechRecognizer.startListening(intent);
                isListening.set(true);
                Log.d(TAG, "Started listening for speech in " + (isOfflineMode ? "offline" : "online") + " mode");
            } catch (Exception e) {
                Log.e(TAG, "Error starting speech recognition", e);
                isListening.set(false);
                if (callback != null) {
                    callback.onError("Error iniciando el reconocimiento de voz: " + e.getMessage());
                }
            }
        }, 3000);
    }

    /**
     * Evalúa qué tan bien pronunció el usuario comparando con el texto de referencia
     * @param spokenText Lo que el usuario dijo
     * @return Un número entre 0 y 1 que representa qué tan bien pronunció
     */
    public double evaluatePronunciation(String spokenText) {
        if (referenceText == null || spokenText == null) {
            return 0.0;
        }

        String cleanReference = referenceText.toLowerCase()
                .replaceAll("[^a-záéíóúñ\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        String cleanSpoken = spokenText.toLowerCase()
                .replaceAll("[^a-záéíóúñ\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        String[] referenceTokens = cleanReference.split("\\s+");
        String[] spokenTokens = cleanSpoken.split("\\s+");

        double wordScore = calculateDetailedWordScore(referenceTokens, spokenTokens);
        double charScore = calculateCharacterSimilarity(cleanReference, cleanSpoken);
        double lengthScore = calculateLengthScore(referenceTokens, spokenTokens);

        if (isOfflineMode) {
            // Modo offline: criterios más estrictos
            double finalScore = (wordScore * 0.6) + (charScore * 0.3) + (lengthScore * 0.1);

            // Penalización por palabras incorrectas
            int incorrectWords = Math.abs(referenceTokens.length - spokenTokens.length);
            if (incorrectWords > 0) {
                finalScore *= (1.0 - (incorrectWords * 0.1));
            }

            // Asegurar que el puntaje no exceda el 100%
            return Math.min(finalScore, 1.0);
        } else {
            // Modo online: mantener exactamente igual que estaba
            return (wordScore * 0.6) + (charScore * 0.3) + (lengthScore * 0.1);
        }
    }

    private double calculateDetailedWordScore(String[] reference, String[] spoken) {
        if (reference.length == 0 || spoken.length == 0) {
            return 0.0;
        }

        double totalScore = 0.0;
        int totalWords = Math.max(reference.length, spoken.length);
        int correctWords = 0;
        int partialWords = 0;

        boolean[] matchedReference = new boolean[reference.length];
        boolean[] matchedSpoken = new boolean[spoken.length];

        // Primera pasada: buscar coincidencias exactas
        for (int i = 0; i < spoken.length; i++) {
            for (int j = 0; j < reference.length; j++) {
                if (!matchedReference[j] && !matchedSpoken[i] &&
                        spoken[i].equals(reference[j])) {
                    matchedReference[j] = true;
                    matchedSpoken[i] = true;
                    correctWords++;
                    break;
                }
            }
        }

        // Segunda pasada: buscar coincidencias parciales
        for (int i = 0; i < spoken.length; i++) {
            if (!matchedSpoken[i]) {
                for (int j = 0; j < reference.length; j++) {
                    if (!matchedReference[j] && !matchedSpoken[i]) {
                        double similarity = calculateWordSimilarity(spoken[i], reference[j]);
                        if (isOfflineMode) {
                            // Modo offline: más estricto con palabras parciales
                            if (similarity > 0.6) {
                                matchedReference[j] = true;
                                matchedSpoken[i] = true;
                                partialWords++;
                                break;
                            }
                        } else {
                            // Modo online: mantener igual que estaba
                            if (similarity > 0.6) {
                                matchedReference[j] = true;
                                matchedSpoken[i] = true;
                                partialWords++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (isOfflineMode) {
            // Modo offline: más estricto con palabras parciales
            totalScore = (correctWords + (partialWords * 0.7)) / (double) totalWords;
        } else {
            // Modo online: mantener igual que estaba
            totalScore = (correctWords + (partialWords * 0.7)) / (double) totalWords;
        }

        return totalScore;
    }

    private double calculateCharacterSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        int minLength = Math.min(s1.length(), s2.length());

        if (isOfflineMode) {
            // Modo offline: más tolerante con diferencias de longitud
            if (maxLength - minLength > 4) {
                return 0.0;
            }
        } else {
            // Modo online: mantener igual que estaba
            if (maxLength - minLength > 3) {
                return 0.0;
            }
        }

        int matches = 0;
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matches++;
            }
        }

        return (double) matches / minLength;
    }

    private double calculateWordSimilarity(String word1, String word2) {
        int maxLength = Math.max(word1.length(), word2.length());
        int minLength = Math.min(word1.length(), word2.length());

        if (isOfflineMode) {
            // Modo offline: más estricto con diferencias de longitud
            if (maxLength - minLength > 2) {
                return 0.0;
            }
        } else {
            // Modo online: mantener igual que estaba
            if (maxLength - minLength > 2) {
                return 0.0;
            }
        }

        int matches = 0;
        for (int i = 0; i < minLength; i++) {
            if (word1.charAt(i) == word2.charAt(i)) {
                matches++;
            }
        }

        // Penalización por diferencias de longitud
        double lengthPenalty = (maxLength - minLength) * 0.1;
        return Math.max(0.0, ((double) matches / minLength) - lengthPenalty);
    }

    private double calculateLengthScore(String[] reference, String[] spoken) {
        int refLength = reference.length;
        int spokenLength = spoken.length;
        return 1.0 - Math.abs(refLength - spokenLength) / (double) Math.max(refLength, spokenLength);
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Error de audio";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Error del cliente";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Permisos insuficientes";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Error de red";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Tiempo de espera de red agotado";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No se encontró coincidencia";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Reconocedor ocupado";
            case SpeechRecognizer.ERROR_SERVER:
                return "Error del servidor";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Tiempo de espera de voz agotado";
            default:
                return "Error desconocido";
        }
    }

    /**
     * Detiene el proceso de escucha y evalúa la pronunciación final
     */
    public void stopListening() {
        // Si estamos escuchando, detenemos el reconocedor
        if (speechRecognizer != null && isListening.get()) {
            try {
                Log.d(TAG, "Manual stop requested, evaluating final pronunciation");
                speechRecognizer.stopListening();
                isListening.set(false); // Marcamos que ya no estamos escuchando

                // El usuario presionó stop manualmente, los resultados vendrán a través de onResults
                // y se evaluarán normalmente
            } catch (Exception e) {
                // Si algo sale mal, lo registramos
                Log.e(TAG, "Error stopping speech recognition", e);
                if (callback != null) {
                    callback.onError("Error deteniendo el reconocimiento de voz: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reinicia el reconocimiento de voz silenciosamente para escuchar otra vez
     */
    private void restartListeningSilently() {
        if (speechRecognizer != null && !isListening.get()) {
            try {
                Log.d(TAG, "Restarting speech recognition silently for another attempt");
                
                // Reiniciar inmediatamente sin pausas ni mensajes molestos
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please read the text aloud");
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

                // Configuración unificada para ambos modos (online y offline)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toString());
                
                // Configuración específica para modo offline
                if (isOfflineMode) {
                    intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                    intent.putExtra("android.speech.extra.FORCE_OFFLINE", true);
                    intent.putExtra("android.speech.extra.DISABLE_NETWORK", true);
                }

                speechRecognizer.startListening(intent);
                isListening.set(true);
                
            } catch (Exception e) {
                Log.e(TAG, "Error restarting speech recognition silently", e);
                // No mostrar mensajes de error molestos al usuario
            }
        }
    }

    /**
     * Reinicia el reconocimiento de voz para un nuevo intento
     * Este método puede ser llamado por el usuario cuando presione "Intentar de nuevo"
     */
    public void restartListening() {
        if (speechRecognizer != null) {
            try {
                Log.d(TAG, "User requested restart of speech recognition");
                
                // Si está escuchando, detener primero
                if (isListening.get()) {
                    speechRecognizer.stopListening();
                    isListening.set(false);
                }
                
                // Pequeña pausa antes de reiniciar
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (callback != null) {
                        callback.onError("Escuchando de nuevo... Pronuncia otra vez.");
                    }
                    
                    // Reiniciar el reconocimiento
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please read the text aloud");
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

                    // Configuración unificada para ambos modos (online y offline)
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toString());
                    
                    // Configuración específica para modo offline
                    if (isOfflineMode) {
                        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                        intent.putExtra("android.speech.extra.FORCE_OFFLINE", true);
                        intent.putExtra("android.speech.extra.DISABLE_NETWORK", true);
                    }

                    speechRecognizer.startListening(intent);
                    isListening.set(true);
                }, 1000); // 1 segundo de pausa
                
            } catch (Exception e) {
                Log.e(TAG, "Error restarting speech recognition", e);
                if (callback != null) {
                    callback.onError("Error reiniciando el reconocimiento de voz: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Libera todos los recursos usados por el evaluador
     * Es importante llamar este método cuando ya no necesitemos el evaluador
     */
    public void destroy() {
        // Si hay un reconocedor activo
        if (speechRecognizer != null) {
            try {
                // Si está escuchando, lo detenemos
                if (isListening.get()) {
                    speechRecognizer.stopListening();
                }
                // Liberamos los recursos del reconocedor
                speechRecognizer.cancel();
                speechRecognizer.destroy();
                speechRecognizer = null;
                isListening.set(false);
                Log.d(TAG, "Speech recognizer destroyed");
            } catch (Exception e) {
                // Si algo sale mal, lo registramos
                Log.e(TAG, "Error destroying speech recognizer", e);
            }
        }

        // Cerramos la conexión con la base de datos
        if (dbHelper != null) {
            dbHelper.close();
        }

        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
    }
}
