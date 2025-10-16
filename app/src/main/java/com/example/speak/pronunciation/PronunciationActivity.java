package com.example.speak.pronunciation;

//Se importan clases necesarias para: permisos, conectividad, interfaz de usuario, y control de base de datos.
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speak.MenuReadingActivity;
import com.example.speak.R;
import com.example.speak.database.DatabaseHelper;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.SharedPreferences;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.BroadcastReceiver;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.util.Log;
import android.app.AlertDialog;
import android.os.Handler;
import java.util.Locale;
import com.example.speak.vosk.VoskSpeechRecognizer;
import android.view.MotionEvent;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;

import com.example.speak.MainActivity;
import com.example.speak.HelpActivity;

import android.widget.LinearLayout;

import com.example.speak.QuizHistoryActivity;
import com.example.speak.MenuSpeakingActivity;
import com.example.speak.ProgressionHelper;
import com.example.speak.pronunciation.PronunciationResultsActivity;
import com.example.speak.helpers.WildcardHelper;
import com.example.speak.helpers.HelpModalHelper;
import com.example.speak.helpers.StarProgressHelper;
import com.example.speak.helpers.StarEarnedDialog;

// Actividad principal de pronunciación
public class PronunciationActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123; // Código para solicitar permisos
    private PronunciationEvaluator evaluator; // Evaluador de pronunciación
    private TextView referenceTextView;
    private Button startButton;
    private Button stopButton;
    private Button nextButton;
    private TextView resultTextView;
    private TextView recognizedTextView;
    private TextView questionNumberTextView;
    private List<PronunciationQuestion> questions; // Cambiado de List<String> a List<PronunciationQuestion>
    private int currentQuestionIndex = 0;
    private Random random = new Random(); // Para seleccionar preguntas aleatorias
    private DatabaseHelper dbHelper; // Acceso a la base de datos
    private boolean isOfflineMode = false; // Indica si hay conexión o no
    private static final String TAG = "PronunciationActivity";
    private VoskSpeechRecognizer voskRecognizer;
    private boolean isListening = false;
    private boolean isButtonPressed = false;
    private Handler buttonHandler = new Handler();
    private static final long BUTTON_PRESS_DELAY = 100; // 100ms para detectar presión sostenida
    private ImageView audioIcon;
    private TextToSpeech textToSpeech;
    private boolean isPlayingAudio = false; // SEGURIDAD: Controlar cuando se reproduce audio

    // MediaPlayer para sonidos de victoria y derrota
    private MediaPlayer victorySound;
    private MediaPlayer defeatSound;

    //Return Menú
    private LinearLayout returnContainer;

    private TextView topicTextView;
    private TextView levelTextView;
    
    // Variables para tema y nivel seleccionados
    private String selectedTopic;
    private String selectedLevel;
    private List<PronunciationQuestion> allQuestions;
    private List<PronunciationQuestion> currentQuestions;
    
    // Sistema de puntuaciones reales
    
    // Sistema de comodines
    private WildcardHelper wildcardHelper;
    private ImageView wildcardButton;
    private List<Double> questionScores; // Puntuaciones reales de cada pregunta
    private int totalCorrectAnswers = 0; // Contador de respuestas correctas
    private long sessionTimestamp; // Timestamp de sesión para agrupar resultados

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pronunciation);

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Inicializar timestamp de sesión
        sessionTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Session timestamp (Pronunciation) initialized: " + sessionTimestamp);

        // Recibir los parámetros enviados desde el menú
        Intent intent = getIntent();
        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");

        // Obtener el ID del usuario actual
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1);
        
        // Si no hay usuario autenticado, verificar si hay usuario invitado
        if (userId == -1) {
            String deviceId = prefs.getString("device_id", null);
            if (deviceId != null) {
                // Verificar si existe usuario invitado
                if (dbHelper.isGuestUserExists(deviceId)) {
                    Cursor guestCursor = dbHelper.getGuestUser(deviceId);
                    if (guestCursor != null && guestCursor.moveToFirst()) {
                        userId = guestCursor.getLong(guestCursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                        guestCursor.close();
                        // Guardar el ID del usuario invitado
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("user_id", userId);
                        editor.apply();
                    }
                } else {
                    // Crear nuevo usuario invitado
                    userId = dbHelper.createGuestUser(deviceId);
                    if (userId != -1) {
            SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("user_id", userId);
            editor.apply();
                    }
                }
            }
        }

        // Verificar conexión a internet
        isOfflineMode = !isNetworkAvailable();
        if (isOfflineMode) {
            Toast.makeText(this, "Modo offline activado", Toast.LENGTH_LONG).show();
            initializeVoskRecognizer();
        }

        // Inicializar vistas
        referenceTextView = findViewById(R.id.referenceTextView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        nextButton = findViewById(R.id.nextButton);
        resultTextView = findViewById(R.id.resultTextView);
        recognizedTextView = findViewById(R.id.recognizedTextView);
        questionNumberTextView = findViewById(R.id.questionNumberTextView);

        topicTextView = findViewById(R.id.topicTextView);
        levelTextView = findViewById(R.id.levelTextView);

        // Inicializar botón de comodines
        wildcardButton = findViewById(R.id.wildcardButton);
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> showWildcardMenu());
        }

        // Inicializar botón de ayuda
        ImageView helpButton = findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> HelpModalHelper.show(this, selectedTopic, selectedLevel));
        }

        // Mostrar topic y level en la interfaz
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("Topic: " + selectedTopic);
        }

        // Configurar botones
        startButton.setOnClickListener(v -> startPronunciationTest());
        stopButton.setOnClickListener(v -> stopPronunciationTest());
        nextButton.setOnClickListener(v -> loadNextQuestion());
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);

        // Verificar permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }

        // Inicializar sistema de comodines
        wildcardHelper = new WildcardHelper(this, "SPEAKING", selectedTopic);
        wildcardHelper.setCallbacks(new WildcardHelper.WildcardCallbacks() {
            @Override
            public void onChangeQuestion() {
                changeCurrentQuestion();
            }

            @Override
            public void onShowContentImage() {
                showContentImage();
            }

            @Override
            public void onShowInstructorVideo() {
                showInstructorVideo();
            }

            @Override
            public void onShowFiftyFifty() {
                // No aplicable para speaking, pero mantenemos la interfaz
                Toast.makeText(PronunciationActivity.this, "50/50 no aplicable para ejercicios de pronunciación", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShowCreativeHelp() {
                showCreativeHelp();
            }
            
            @Override
            public void onShowWildcardInfo() {
                showWildcardUsageInfo();
            }
        });

        // Cargar las preguntas del archivo por tema específico
        loadQuestionsFromFile(selectedTopic, selectedLevel);

        // Initialize TextToSpeech for audio playback
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                    Log.d(TAG, "TextToSpeech inicializado correctamente");
            } else {
                    Log.e(TAG, "TextToSpeech initialization failed");
                }
            }
        });

        // Initialize victory and defeat sounds
        try {
            victorySound = MediaPlayer.create(this, getResources().getIdentifier(
                "mario_bros_vida", "raw", getPackageName()));
            defeatSound = MediaPlayer.create(this, getResources().getIdentifier(
                "pacman_dies", "raw", getPackageName()));
            Log.d(TAG, "Sonidos de victoria y derrota inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando sonidos: " + e.getMessage());
        }

        // Initialize audio icon
        audioIcon = findViewById(R.id.audioIcon);
        audioIcon.setOnClickListener(v -> {
            // SEGURIDAD: No permitir reproducir audio durante grabación
            if (isListening) {
                Log.d(TAG, "⚠️ No se puede reproducir audio durante grabación");
                Toast.makeText(this, "No puedes reproducir audio mientras grabas", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String textToSpeak = referenceTextView.getText().toString();
            if (textToSpeech != null && !textToSpeak.isEmpty() && !isPlayingAudio) {
                // SEGURIDAD: Bloquear botones durante reproducción de audio
                blockButtonsDuringAudio();
                
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                Log.d(TAG, "🔊 Reproduciendo audio: " + textToSpeak);
                
                // Calcular duración estimada basada en longitud del texto (aprox. 150 palabras por minuto)
                int estimatedDuration = Math.max(2000, textToSpeak.length() * 100); // Mínimo 2 segundos
                
                // Desbloquear botones después de la duración estimada
                new Handler().postDelayed(() -> {
                    unblockButtonsAfterAudio();
                }, estimatedDuration);
            } else if (isPlayingAudio) {
                Log.d(TAG, "⚠️ Audio ya se está reproduciendo, espera a que termine");
            }
        });

        //Return Menu
        returnContainer = findViewById(R.id.returnContainer);
        returnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });
        
        // Migrar progreso anterior a claves correctas
        migrateOldProgressKeys();
    }

    /**
     * Migra progreso guardado con claves antiguas a las claves correctas
     * Esto asegura que el progreso anterior no se pierda
     */
    private void migrateOldProgressKeys() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean needsUpdate = false;
        
        Log.d(TAG, "=== MIGRANDO PROGRESO ANTERIOR ===");
        
        // Mapeo de claves antiguas a claves nuevas
        String[][] keyMigrations = {
            {"PRONUNCIATION_PASSED_ALPHABET", "PASSED_PRON_ALPHABET"},
            {"PRONUNCIATION_PASSED_NUMBERS", "PASSED_PRON_NUMBERS"},
            {"PRONUNCIATION_PASSED_COLORS", "PASSED_PRON_COLORS"},
            {"PRONUNCIATION_PASSED_PERSONAL_PRONOUNS", "PASSED_PRON_PERSONAL_PRONOUNS"},
            {"PRONUNCIATION_PASSED_POSSESSIVE_ADJECTIVES", "PASSED_PRON_POSSESSIVE_ADJECTIVES"},
            {"PRONUNCIATION_PASSED_PREPOSITIONS_OF_PLACE", "PASSED_PRON_PREPOSITIONS_OF_PLACE"},
            {"PRONUNCIATION_PASSED_ADJECTIVES", "PASSED_PRON_ADJECTIVES"}
        };
        
        for (String[] migration : keyMigrations) {
            String oldKey = migration[0];
            String newKey = migration[1];
            
            // Si existe progreso con la clave antigua y no existe con la nueva
            if (prefs.getBoolean(oldKey, false) && !prefs.getBoolean(newKey, false)) {
                editor.putBoolean(newKey, true);
                needsUpdate = true;
                Log.d(TAG, "✅ Migrado: " + oldKey + " → " + newKey);
            }
        }
        
        if (needsUpdate) {
            editor.apply();
            Log.d(TAG, "=== MIGRACIÓN COMPLETADA ===");
        } else {
            Log.d(TAG, "=== NO SE REQUIERE MIGRACIÓN ===");
        }
    }

    private void initializeVoskRecognizer() {
        voskRecognizer = new VoskSpeechRecognizer(this);
        voskRecognizer.initialize(new VoskSpeechRecognizer.RecognitionListener() {
            @Override
            public void onResult(String text) {
                runOnUiThread(() -> {
                    // Solo actualizar el texto reconocido, sin evaluar
                    recognizedTextView.setText("Reconocido: " + text);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PronunciationActivity.this, error, Toast.LENGTH_SHORT).show();
                    recognizedTextView.setText("Error: " + error);
                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                    nextButton.setEnabled(true);
                });
            }

            @Override
            public void onPartialResult(String text) {
                runOnUiThread(() -> {
                    recognizedTextView.setText("Reconociendo: " + text);
                });
            }
        });
    }

    private void checkLanguageModelStatus() {
        Log.d(TAG, "Verificando estado del modelo de idioma");
        Intent checkIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        sendOrderedBroadcast(checkIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = getResultExtras(true);
                if (extras != null) {
                    ArrayList<String> supportedLanguages = extras.getStringArrayList(
                        RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                    
                    boolean isEnglishInstalled = supportedLanguages != null && 
                        supportedLanguages.contains("en-US");
                    
                    Log.d(TAG, "Modelo de inglés instalado: " + isEnglishInstalled);
                    
                    if (!isEnglishInstalled) {
                        showLanguageModelInstallOptions();
                    } else {
                        Log.d(TAG, "Modelo de inglés listo para uso");
                        initializeSpeechRecognizer();
                    }
                }
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    private void showLanguageModelInstallOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modelo de inglés requerido")
               .setMessage("Para usar el reconocimiento de voz offline, necesitas instalar el modelo de inglés. ¿Cómo deseas proceder?")
               .setPositiveButton("Instalar desde Google", (dialog, which) -> {
                   installLanguageModelFromGoogle();
               })
               .setNeutralButton("Instalar desde configuración", (dialog, which) -> {
                   openLanguageSettings();
               })
               .setNegativeButton("Cancelar", (dialog, which) -> {
                   Toast.makeText(this, 
                       "El reconocimiento de voz offline no funcionará sin el modelo de inglés", 
                       Toast.LENGTH_LONG).show();
               })
               .setCancelable(false)
               .show();
    }

    private void installLanguageModelFromGoogle() {
        try {
            Intent installIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
            installIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            installIntent.putExtra("android.speech.extra.INSTALL_VOICE_DATA", true);
            startActivity(installIntent);
            
            // Verificar periódicamente si el modelo se ha instalado
            new Handler().postDelayed(() -> {
                checkLanguageModelStatus();
            }, 5000);
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar instalación desde Google", e);
            Toast.makeText(this, "Error al iniciar la instalación", Toast.LENGTH_SHORT).show();
            openLanguageSettings();
        }
    }

    private void openLanguageSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir configuración de idiomas", e);
            Toast.makeText(this, "No se pudo abrir la configuración de idiomas", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                // Destruir el reconocedor anterior si existe
                if (voskRecognizer != null) {
                    voskRecognizer.destroy();
                }
                
                // Crear nuevo reconocedor
                voskRecognizer = new VoskSpeechRecognizer(this);
                voskRecognizer.initialize(new VoskSpeechRecognizer.RecognitionListener() {
                    @Override
                    public void onResult(String text) {
                        runOnUiThread(() -> {
                            recognizedTextView.setText("Reconocido: " + text);
                            resultTextView.setText("Procesando...");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Error en reconocimiento online: " + error);
                            resultTextView.setText(error);
                            recognizedTextView.setText("Error: " + error);
                            
                            // Habilitar botón start para permitir reintentar la misma pregunta
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            nextButton.setEnabled(false);
                            
                            // Casos especiales donde se mantiene esperando
                            if (error.contains("¡Ahora!") || error.contains("Escuchando")) {
                                stopButton.setEnabled(true);
                                startButton.setEnabled(false);
                                nextButton.setEnabled(false);
                            }
                        });
                    }

                    @Override
                    public void onPartialResult(String text) {
                        runOnUiThread(() -> {
                            recognizedTextView.setText("Reconociendo: " + text);
                        });
                    }
                });
                
                // Verificar la disponibilidad del micrófono
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_CODE);
                }
                
                Log.d(TAG, "SpeechRecognizer inicializado para modo " + (isOfflineMode ? "offline" : "online"));
            } catch (Exception e) {
                Log.e(TAG, "Error al inicializar SpeechRecognizer", e);
                // Si hay error, reintentar
                new Handler().postDelayed(() -> {
                    initializeSpeechRecognizer();
                }, 2000);
            }
        } else {
            Log.e(TAG, "Reconocimiento de voz no disponible en el dispositivo");
            Toast.makeText(this, "El reconocimiento de voz no está disponible en este dispositivo", 
                Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupEvaluator() {
        evaluator = new PronunciationEvaluator(this);
        evaluator.setOfflineMode(isOfflineMode);
        Log.d(TAG, "Evaluador configurado en modo: " + (isOfflineMode ? "Offline" : "Online"));
    }

    // Comprueba si hay conexión a internet (ya sea WiFi o datos móviles)
    private boolean isNetworkAvailable() {
        // Obtiene el servicio del sistema encargado de gestionar la conectividad de red (WiFi, datos, etc.)
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Si el servicio fue obtenido correctamente (no es nulo)
        if (connectivityManager != null) {

            // Obtiene información sobre la red activa en ese momento (puede ser WiFi o datos móviles)
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            // Devuelve true si hay una red activa y está conectada
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        // En caso de que el ConnectivityManager sea nulo (caso raro), se asume que no hay red
        return false;
    }

    // Carga preguntas desde un archivo .txt del directorio assets filtradas por tema
    private void loadQuestionsFromFile(String topic, String level) {
        allQuestions = new ArrayList<>();
        try {
            java.io.InputStream inputStream = getAssets().open("A1.1_Basic_English_Topics.txt");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream));

            String line;
            String currentTopic = "";
            String currentLevel = "";
            String currentQuestion = "";
            boolean isMatchingTopic = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Topic:")) {
                    currentTopic = line.substring(6).trim();
                    isMatchingTopic = currentTopic.equalsIgnoreCase(topic);
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim();
                    isMatchingTopic = isMatchingTopic && currentLevel.equalsIgnoreCase(level);
                } else if (line.startsWith("Q:")) {
                    currentQuestion = line.substring(2).trim();
                    if (isMatchingTopic && !currentQuestion.isEmpty()) {
                        allQuestions.add(new PronunciationQuestion(currentQuestion, currentTopic, currentLevel));
                        Log.d(TAG, "Pregunta cargada - Topic: " + currentTopic + ", Level: " + currentLevel + ", Question: " + currentQuestion);
                    }
                }
            }

            reader.close();

            // Shuffle all questions
            Collections.shuffle(allQuestions);

            // Select first 10 questions (or all if less than 10)
            currentQuestions = new ArrayList<>(allQuestions.subList(0, Math.min(10, allQuestions.size())));

            // Use currentQuestions instead of questions
            questions = currentQuestions;

            // Inicializar sistema de puntuaciones reales
            questionScores = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                questionScores.add(0.0); // Inicializar todas las puntuaciones en 0
            }
            totalCorrectAnswers = 0;

            // Display the first question
            if (!questions.isEmpty()) {
                currentQuestionIndex = 0;
                showCurrentQuestion();
                
                // Asegurar que el audioIcon se muestre correctamente para la primera pregunta
                new Handler().postDelayed(() -> {
                    if (questions != null && currentQuestionIndex < questions.size()) {
                        PronunciationQuestion question = questions.get(currentQuestionIndex);
                        if (shouldShowAudioPlayer(question.getTopic())) {
                            audioIcon.setVisibility(View.VISIBLE);
                            Log.d(TAG, "AudioIcon forzado a VISIBLE para primera pregunta: " + question.getTopic());
                        }
                    }
                }, 100); // Pequeño retraso para asegurar que la UI esté lista
            } else {
                Toast.makeText(this, "No hay preguntas disponibles para el tema: " + topic, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar las preguntas: " + e.getMessage());
            Toast.makeText(this, "Error al cargar las preguntas", Toast.LENGTH_SHORT).show();
        }
    }

    // Muestra en pantalla la pregunta actual
    private void showCurrentQuestion() {
        if (currentQuestionIndex < questions.size()) {
            PronunciationQuestion question = questions.get(currentQuestionIndex);
            referenceTextView.setText(question.getQuestion());
            questionNumberTextView.setText(String.format("%d/%d", currentQuestionIndex + 1, questions.size()));
            topicTextView.setText("Topic: " + question.getTopic());
            levelTextView.setText("Level: " + question.getLevel());
            recognizedTextView.setText("");
            resultTextView.setText("");
            
            // NUEVA LÓGICA: Mostrar audioIcon solo para temas avanzados a partir de PERSONAL PRONOUNS
            if (shouldShowAudioPlayer(question.getTopic())) {
                audioIcon.setVisibility(View.VISIBLE);
                
                // SEGURIDAD: Asegurar estado correcto del audioIcon
                if (!isListening && !isPlayingAudio) {
                    audioIcon.setEnabled(true);
                    audioIcon.setAlpha(1.0f);
                }
                
                Log.d(TAG, "AudioIcon activado para tema: " + question.getTopic());
            } else {
            audioIcon.setVisibility(View.GONE);
                Log.d(TAG, "AudioIcon desactivado para tema: " + question.getTopic());
            }
            
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            showFinalResults();
        }
    }

    /**
     * Determina si se debe mostrar el reproductor de audio para el tema especificado
     * @param topic El tema actual
     * @return true si debe mostrar el audioIcon, false en caso contrario
     */
    private boolean shouldShowAudioPlayer(String topic) {
        if (topic == null) return false;
        
        // Temas básicos: sin reproductor de audio (mantener comportamiento original)
        String[] basicTopics = {
            "ALPHABET",
            "NUMBERS", 
            "COLORS"
        };
        
        // Verificar si es un tema básico
        for (String basicTopic : basicTopics) {
            if (basicTopic.equalsIgnoreCase(topic)) {
                return false; // No mostrar reproductor para temas básicos
            }
        }
        
        // Temas avanzados: con reproductor de audio (nuevos)
        String[] advancedTopics = {
            "PERSONAL PRONOUNS",
            "POSSESSIVE ADJECTIVES",
            "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION",
            "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)"
        };
        
        // Verificar si es un tema avanzado
        for (String advancedTopic : advancedTopics) {
            if (advancedTopic.equalsIgnoreCase(topic)) {
                // Mostrar mensaje informativo solo para PERSONAL PRONOUNS (primer tema avanzado)
                if ("PERSONAL PRONOUNS".equalsIgnoreCase(topic) && currentQuestionIndex == 0) {
                    showAudioPlayerWelcomeMessage();
                }
                return true; // Mostrar reproductor para temas avanzados
            }
        }
        
        // Por defecto, para cualquier tema nuevo también mostrar el reproductor
        return true;
    }

    /**
     * Muestra un mensaje de bienvenida sobre el reproductor de audio
     */
    private void showAudioPlayerWelcomeMessage() {
        new Handler().postDelayed(() -> {
            if (audioIcon != null && audioIcon.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, 
                    "🔊 ¡Nuevo! Toca el icono de audio para escuchar la pronunciación antes de practicar", 
                    Toast.LENGTH_LONG).show();
            }
        }, 500); // Retraso para que se vea bien el audioIcon primero
    }

    // Muestra el resultado final cuando se completa la actividad
    private void showFinalResults() {
        // Calcular puntaje final usando las puntuaciones REALES
        int totalQuestions = questions.size();
        int passedQuestions = 0;
        double totalScore = 0;

        // Usar las puntuaciones reales guardadas durante el quiz
        for (int i = 0; i < questionScores.size(); i++) {
            double score = questionScores.get(i);
            totalScore += score;
            if (score >= 70.0) { // 70% como umbral
                passedQuestions++;
            }
            Log.d(TAG, "Pregunta " + (i + 1) + ": " + score + "% " + (score >= 70 ? "(APROBADA)" : "(REPROBADA)"));
        }

        double averageScore = totalScore / totalQuestions; // Ya está en porcentaje
        int finalScore = (int) Math.round(averageScore); // Convertir a entero para mostrar

        Log.d(TAG, "=== RESULTADO FINAL ===");
        Log.d(TAG, "Total preguntas: " + totalQuestions);
        Log.d(TAG, "Preguntas aprobadas: " + passedQuestions);
        Log.d(TAG, "Puntaje promedio: " + averageScore + "%");

        // Guardar progreso si alcanza el 70%
        savePronunciationProgress(averageScore);

        // Create a dialog to show the result with bird image (como en ListeningActivity)
        //android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        //android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);

        // Crear el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);
        builder.setView(dialogView);

        builder.setView(dialogView);
        builder.setCancelable(false); // Evitar que se cierre sin seleccionar una opción

        // Crear y mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show(); // 👈 Primero se muestra

        // Eliminar el fondo blanco del contenedor
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        android.widget.ImageView birdImageView = dialogView.findViewById(R.id.birdImageView);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        TextView counterTextView = dialogView.findViewById(R.id.counterTextView);
        android.widget.TextView scoreTextView = dialogView.findViewById(R.id.scoreTextView);
        android.widget.Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        android.widget.TextView btnReintentar = dialogView.findViewById(R.id.btnReintentar);
        android.widget.LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

        // Set bird image based on score (misma lógica que ListeningActivity)
        if (finalScore >= 100) {
            messageTextView.setText("Excellent your English is getting better!");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 90) {
            messageTextView.setText("Good, but you can do it better!");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 80) {
            messageTextView.setText("Good, but you can do it better!");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 69) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 60) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 40) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 30) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 20) {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else {
            messageTextView.setText("You should practice more!");
            birdImageView.setImageResource(R.drawable.crab_bad);
        }

        scoreTextView.setText("Score: " + finalScore + "%");
        counterTextView.setText(passedQuestions + "/" + totalQuestions);

        // Sonido final: victoria si aprueba (>=70), derrota si no
        if (finalScore >= 70) {
            playVictorySound();
        } else {
            playDefeatSound();
        }

        // Crear copias final para usar en lambdas (CORREGIDO: Variables effectively final)
        final String finalSelectedTopic = selectedTopic;
        final String finalSelectedLevel = selectedLevel;
        final int finalScoreForLambda = finalScore;
        final int finalTotalQuestions = totalQuestions;
        final int finalPassedQuestions = passedQuestions; // CORREGIDO: Crear variable final

        // Configurar botón Continuar según score
        if (finalScore < 70) {
            // Mostrar "Try Again" en el botón principal
            btnContinue.setText("Try Again");
            btnContinue.setVisibility(View.VISIBLE);
            btnReintentar.setVisibility(View.GONE);

            btnContinue.setOnClickListener(v -> {
                // Reiniciar la misma actividad
                Intent intent = new Intent(this, PronunciationActivity.class);
                intent.putExtra("TOPIC", finalSelectedTopic);
                intent.putExtra("LEVEL", finalSelectedLevel);
                startActivity(intent);
                finish();
            });
        } else if (finalScore >= 70) {
            btnContinue.setVisibility(View.VISIBLE);
            btnReintentar.setVisibility(View.GONE);

            // Como ya está en speaking, solo navegar al siguiente tema del mapa
            if ("POSSESSIVE ADJECTIVES".equals(selectedTopic)) {
                btnContinue.setText("📖 ¡Desbloquear Reading!");
                btnContinue.setOnClickListener(v -> {
                    // Ir al mapa de reading
                    Intent intent = new Intent(this, MenuReadingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            } else {
                // Continuar con el siguiente tema de speaking
                String nextTopic = getNextSpeakingTopic(selectedTopic);
                if (nextTopic != null) {
                    btnContinue.setText("Continuar: " + nextTopic);
                    btnContinue.setOnClickListener(v -> {
                        Intent intent = new Intent(this, getNextSpeakingActivity(nextTopic));
                        intent.putExtra("TOPIC", nextTopic);
                        intent.putExtra("LEVEL", finalSelectedLevel);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    btnContinue.setText("¡Speaking Completado!");
                    btnContinue.setOnClickListener(v -> {
                        Intent intent = new Intent(this, MenuSpeakingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            }
        }
        
        // Configurar botón Ver detalles (CORREGIDO: Usar variables final)
        btnViewDetails.setOnClickListener(v -> {
            // Crear nueva actividad específica para resultados de pronunciación
            Intent intent = new Intent(this, PronunciationResultsActivity.class);
            intent.putExtra("FINAL_SCORE", finalScoreForLambda);
            intent.putExtra("TOTAL_QUESTIONS", finalTotalQuestions);
            intent.putExtra("PASSED_QUESTIONS", finalPassedQuestions);
            intent.putExtra("TOPIC", finalSelectedTopic);
            intent.putExtra("LEVEL", finalSelectedLevel);
            // Pasar las puntuaciones individuales
            double[] scoresArray = new double[questionScores.size()];
            for (int i = 0; i < questionScores.size(); i++) {
                scoresArray[i] = questionScores.get(i);
            }
            intent.putExtra("INDIVIDUAL_SCORES", scoresArray);
            // Pasar el mismo timestamp de sesión para filtrar resultados de pronunciación
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
            startActivity(intent);
            finish();
        });

        // Mostrar modal de estrella ganada si aprobó (igual a ListeningActivity)
        final int finalScoreCaptured = finalScore;
        if (finalScoreCaptured >= 70) {
            new Handler().postDelayed(() -> {
                try {
                    StarEarnedDialog.show(PronunciationActivity.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.getMessage());
                }
            }, 200);
        }

    }

    // Carga la siguiente pregunta
    private void loadNextQuestion() {
        if (questions != null && !questions.isEmpty()) {
            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
            showCurrentQuestion();
            } else {
                // Quiz completed
                showFinalResults();
            }
        }
    }

    // Inicia la prueba de pronunciación
    private void startPronunciationTest() {
        // SEGURIDAD: Verificar que no se esté reproduciendo audio
        if (isPlayingAudio) {
            Log.d(TAG, "⚠️ No se puede iniciar grabación mientras se reproduce audio");
            Toast.makeText(this, "Espera a que termine el audio antes de hablar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);

        recognizedTextView.setText("");
        resultTextView.setText("");

        evaluator = new PronunciationEvaluator(this);
        evaluator.setOfflineMode(isOfflineMode);
        PronunciationQuestion currentQuestion = questions.get(currentQuestionIndex);
        evaluator.setReferenceText(currentQuestion.getQuestion());
        
        if (isOfflineMode) {
            if (voskRecognizer != null) {
                startListening();
            }
        } else {
        evaluator.startListening(new PronunciationEvaluator.PronunciationCallback() {
            @Override
            public void onResult(double score, String recognizedText) {
                runOnUiThread(() -> {
                    // Guardar la puntuación real de esta pregunta (score ya viene en porcentaje 0-100)
                    if (currentQuestionIndex < questionScores.size()) {
                        questionScores.set(currentQuestionIndex, score * 100); // Convertir a porcentaje
                        if (score * 100 >= 70.0) { // 70% como umbral de correcto
                            totalCorrectAnswers++;
                        }
                        Log.d(TAG, "Pregunta " + (currentQuestionIndex + 1) + " puntuación guardada: " + (score * 100) + "%");
                    }
                    
                    recognizedTextView.setText("Reconocido: " + recognizedText);
                    resultTextView.setText(String.format("Puntuación: %.1f%%", score * 100).replace(".", ","));
                    
                    // Reproducir sonido según el puntaje de la pregunta individual
                    if (score * 100 >= 70.0) {
                        playVictorySound();
                        Log.d(TAG, "🎉 Sonido de victoria para pregunta individual (score: " + (score * 100) + "%)");
                    } else {
                        playDefeatSound();
                        Log.d(TAG, "😔 Sonido de derrota para pregunta individual (score: " + (score * 100) + "%)");
                    }
                    
                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                    nextButton.setEnabled(true);

                    // En modo online: el usuario podrá avanzar con Detener (ahora habilitado) o Next
                    // Mostrar resultado y reproducir sonido ya se hizo arriba
                });
            }

            @Override
            public void onError(String error) {
                final String errorMsg = (error == null) ? "" : error;
                runOnUiThread(() -> {
                    Log.d(TAG, "Error en reconocimiento online: " + errorMsg);

                    // Caso de inicio de escucha: mantener flujo de grabación (no permitir Next aún)
                    if (errorMsg.contains("¡Ahora!") || errorMsg.contains("Escuchando")) {
                        recognizedTextView.setText("Escuchando...");
                        resultTextView.setText("");
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        nextButton.setEnabled(false);
                        return;
                    }

                    // Si ya hay un puntaje mostrado, no sobrescribirlo con mensajes de error
                    CharSequence currentResult = resultTextView.getText();
                    boolean hasScoreShown = currentResult != null && currentResult.toString().startsWith("Puntuación:");

                    if (!hasScoreShown) {
                        // Para errores operativos, limpiar mensajes molestos pero permitir decidir
                        if (errorMsg.contains("Error del cliente") || errorMsg.contains("Error de red") ||
                            errorMsg.contains("No se pudo reconocer") || errorMsg.contains("Tiempo de espera") ||
                            errorMsg.contains("desconocido")) {
                            resultTextView.setText("");
                            recognizedTextView.setText("");
                        } else {
                            // Otros errores informativos
                            resultTextView.setText(errorMsg);
                            recognizedTextView.setText("Error: " + errorMsg);
                        }
                    }

                    // Permitir al usuario reintentar o pasar a la siguiente
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    nextButton.setEnabled(true);
                });
            }

            @Override
            public void onSpeechDetected() {
                runOnUiThread(() -> {
                    stopButton.setEnabled(true);
                    startButton.setEnabled(false);
                    nextButton.setEnabled(false);
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                runOnUiThread(() -> {
                        recognizedTextView.setText("Reconociendo: " + partialText);
                });
            }
        });
        }
    }

    // Eliminado diálogo para mantener flujo simple (Detener/Next controlan avance)

    // Detiene la grabación de audio
    private void stopPronunciationTest() {
        if (isOfflineMode) {
            stopListening();
            
            String recognizedText = recognizedTextView.getText().toString()
                .replace("Reconocido: ", "")
                .replace("Escuchando...", "")
                .replace("Reconociendo: ", "");
            
            if (!recognizedText.isEmpty() && questions != null && currentQuestionIndex < questions.size()) {
                PronunciationQuestion currentQuestion = questions.get(currentQuestionIndex);
                String referenceText = currentQuestion.getQuestion();
                PronunciationEvaluator evaluator = new PronunciationEvaluator(PronunciationActivity.this);
                evaluator.setReferenceText(referenceText);
                double score = evaluator.evaluatePronunciation(recognizedText);
                
                // Convertir el score a porcentaje y guardar la puntuación real
                double percentage = score * 100;
                
                // Verificar si esta pregunta fue cambiada por un comodín
                boolean wasQuestionChanged = wildcardHelper.wasQuestionChanged(currentQuestionIndex);
                
                if (currentQuestionIndex < questionScores.size()) {
                    questionScores.set(currentQuestionIndex, percentage);
                    if (percentage >= 70.0) { // 70% como umbral de correcto
                        // Solo sumar puntos si la pregunta NO fue cambiada por un comodín
                        if (!wasQuestionChanged) {
                            totalCorrectAnswers++;
                            Log.d(TAG, "Puntos sumados: +1 (Total: " + totalCorrectAnswers + ")");
                        } else {
                            Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín");
                        }
                    }
                    Log.d(TAG, "Pregunta " + (currentQuestionIndex + 1) + " puntuación guardada (stopTest offline): " + percentage + "%");
                    Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged);
                }
                
                if (dbHelper != null) {
                    long resultId = dbHelper.savePronunciationResult(
                        dbHelper.getCurrentUserId(),
                        referenceText,
                        recognizedText,
                        score,
                        currentQuestion.getTopic(),
                        currentQuestion.getLevel(),
                        sessionTimestamp
                    );
                    if (resultId != -1) {
                        Log.d(TAG, "Pronunciation result saved locally with ID: " + resultId);
                    }
                }
                
                resultTextView.setText(String.format("Puntuación: %.1f%%", percentage).replace(".", ","));
                
                // Reproducir sonido según el puntaje de la pregunta individual (modo offline)
                if (percentage >= 70.0) {
                    playVictorySound();
                    Log.d(TAG, "🎉 Sonido de victoria para pregunta individual offline (score: " + percentage + "%)");
                } else {
                    playDefeatSound();
                    Log.d(TAG, "😔 Sonido de derrota para pregunta individual offline (score: " + percentage + "%)");
                }
                
                // NUEVA LÓGICA: Mostrar parlante DESPUÉS de contestar para retroalimentación
                showAudioIconAfterAnswer(currentQuestion.getTopic());
            }
            
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            nextButton.setEnabled(true);
        } else {
            if (evaluator != null) {
                evaluator.stopListening();

                // Mostrar parlante de retroalimentación
                if (questions != null && currentQuestionIndex < questions.size()) {
                    PronunciationQuestion currentQuestion = questions.get(currentQuestionIndex);
                    showAudioIconAfterAnswer(currentQuestion.getTopic());
                }

                // No avanzar automáticamente; habilitar Next para que el usuario decida
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                nextButton.setEnabled(true);

                // Evaluar y mostrar puntaje también en modo online (cuando el usuario detiene)
                try {
                    String recognizedText = recognizedTextView.getText().toString()
                        .replace("Reconocido: ", "")
                        .replace("Escuchando...", "")
                        .replace("Reconociendo: ", "").trim();

                    if (!recognizedText.isEmpty() && questions != null && currentQuestionIndex < questions.size()) {
                        PronunciationQuestion currentQuestion = questions.get(currentQuestionIndex);
                        String referenceText = currentQuestion.getQuestion();
                        PronunciationEvaluator localEvaluator = new PronunciationEvaluator(PronunciationActivity.this);
                        localEvaluator.setReferenceText(referenceText);
                        double score = localEvaluator.evaluatePronunciation(recognizedText);
                        double percentage = Math.min(100.0, Math.max(0.0, score * 100));

                        // Guardar puntuación real en arreglo
                        if (currentQuestionIndex < questionScores.size()) {
                            questionScores.set(currentQuestionIndex, percentage);
                            if (percentage >= 70.0) {
                                totalCorrectAnswers++;
                            }
                        }

                        // Guardar localmente (como en offline)
                        if (dbHelper != null) {
                            long resultId = dbHelper.savePronunciationResult(
                                dbHelper.getCurrentUserId(),
                                referenceText,
                                recognizedText,
                                score,
                                currentQuestion.getTopic(),
                                currentQuestion.getLevel(),
                                sessionTimestamp
                            );
                            if (resultId != -1) {
                                Log.d(TAG, "Pronunciation result saved locally (online stop) ID: " + resultId);
                            }
                        }

                        // Mostrar resultado y sonido acorde
                        resultTextView.setText(String.format("Puntuación: %.1f%%", percentage).replace('.', ','));
                        if (percentage >= 70.0) {
                            playVictorySound();
                        } else {
                            playDefeatSound();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error evaluando puntaje en modo online al detener: " + e.getMessage());
                }
            }
        }
    }

    // Maneja el resultado de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPronunciationTest();
            } else {
                Snackbar.make(findViewById(android.R.id.content), 
                    "Se requiere permiso de micrófono para esta función", 
                    Snackbar.LENGTH_LONG).show();
            }
        }
    }

    // Limpieza al cerrar la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonHandler.removeCallbacksAndMessages(null);
        if (voskRecognizer != null) {
            voskRecognizer.destroy();
            voskRecognizer = null;
        }
        if (evaluator != null) {
            evaluator.destroy();
            evaluator = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        
        // Liberar recursos de MediaPlayer
        if (victorySound != null) {
            victorySound.release();
            victorySound = null;
        }
        if (defeatSound != null) {
            defeatSound.release();
            defeatSound = null;
        }
    }

    // Guardar estado durante rotación de pantalla
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Guardando estado de la actividad");
        
        // Guardar datos esenciales
        outState.putString("selectedTopic", selectedTopic);
        outState.putString("selectedLevel", selectedLevel);
        outState.putInt("currentQuestionIndex", currentQuestionIndex);
        
        // Guardar toda la lista de preguntas para evitar shuffle al restaurar
        if (questions != null && !questions.isEmpty()) {
            ArrayList<String> questionTexts = new ArrayList<>();
            ArrayList<String> questionTopics = new ArrayList<>();
            ArrayList<String> questionLevels = new ArrayList<>();
            
            for (PronunciationQuestion q : questions) {
                questionTexts.add(q.getQuestion());
                questionTopics.add(q.getTopic());
                questionLevels.add(q.getLevel());
            }
            
            outState.putStringArrayList("questionTexts", questionTexts);
            outState.putStringArrayList("questionTopics", questionTopics);
            outState.putStringArrayList("questionLevels", questionLevels);
        }
        
        // Guardar estado de la pregunta actual
        if (questions != null && currentQuestionIndex < questions.size()) {
            PronunciationQuestion currentQuestion = questions.get(currentQuestionIndex);
            outState.putString("currentQuestionText", currentQuestion.getQuestion());
            outState.putString("currentQuestionTopic", currentQuestion.getTopic());
            outState.putString("currentQuestionLevel", currentQuestion.getLevel());
        }
        
        // Guardar texto de las vistas
        if (recognizedTextView != null) {
            outState.putString("recognizedText", recognizedTextView.getText().toString());
        }
        if (resultTextView != null) {
            outState.putString("resultText", resultTextView.getText().toString());
        }
        
        // Guardar estado de botones
        outState.putBoolean("startButtonEnabled", startButton != null ? startButton.isEnabled() : true);
        outState.putBoolean("stopButtonEnabled", stopButton != null ? stopButton.isEnabled() : false);
        outState.putBoolean("nextButtonEnabled", nextButton != null ? nextButton.isEnabled() : false);
        
        Log.d(TAG, "Estado guardado - Pregunta: " + currentQuestionIndex + ", Tema: " + selectedTopic);
    }

    // Restaurar estado después de rotación de pantalla
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "Restaurando estado de la actividad");
        
        if (savedInstanceState != null) {
            // Restaurar datos esenciales
            selectedTopic = savedInstanceState.getString("selectedTopic");
            selectedLevel = savedInstanceState.getString("selectedLevel");
            currentQuestionIndex = savedInstanceState.getInt("currentQuestionIndex", 0);
            
            // Restaurar lista de preguntas para mantener el mismo orden
            ArrayList<String> questionTexts = savedInstanceState.getStringArrayList("questionTexts");
            ArrayList<String> questionTopics = savedInstanceState.getStringArrayList("questionTopics");
            ArrayList<String> questionLevels = savedInstanceState.getStringArrayList("questionLevels");
            
            if (questionTexts != null && questionTopics != null && questionLevels != null) {
                questions = new ArrayList<>();
                for (int i = 0; i < questionTexts.size(); i++) {
                    questions.add(new PronunciationQuestion(
                        questionTexts.get(i), 
                        questionTopics.get(i), 
                        questionLevels.get(i)
                    ));
                }
                Log.d(TAG, "Lista de preguntas restaurada con " + questions.size() + " preguntas");
            }
            
            // Restaurar texto de las vistas
            String recognizedText = savedInstanceState.getString("recognizedText", "");
            String resultText = savedInstanceState.getString("resultText", "");
            
            if (recognizedTextView != null) {
                recognizedTextView.setText(recognizedText);
            }
            if (resultTextView != null) {
                resultTextView.setText(resultText);
            }
            
            // Restaurar estado de botones
            boolean startEnabled = savedInstanceState.getBoolean("startButtonEnabled", true);
            boolean stopEnabled = savedInstanceState.getBoolean("stopButtonEnabled", false);
            boolean nextEnabled = savedInstanceState.getBoolean("nextButtonEnabled", false);
            
            if (startButton != null) startButton.setEnabled(startEnabled);
            if (stopButton != null) stopButton.setEnabled(stopEnabled);
            if (nextButton != null) nextButton.setEnabled(nextEnabled);
            
            // Mostrar tema y nivel restaurados
            if (selectedLevel != null && levelTextView != null) {
                levelTextView.setText("Level: " + selectedLevel);
            }
            if (selectedTopic != null && topicTextView != null) {
                topicTextView.setText("Topic: " + selectedTopic);
            }
            
            Log.d(TAG, "Estado restaurado - Pregunta: " + currentQuestionIndex + ", Tema: " + selectedTopic);
            
            // Solo cargar preguntas si no se pudieron restaurar
            if (questions == null || questions.isEmpty()) {
                Log.d(TAG, "No se pudieron restaurar preguntas, cargando desde archivo");
                loadQuestionsFromFile(selectedTopic, selectedLevel);
            } else {
                // Mostrar la pregunta actual restaurada - MISMA PREGUNTA
                Log.d(TAG, "Mostrando pregunta restaurada: " + currentQuestionIndex);
                showCurrentQuestion();
            }
        }
    }

    private void startListening() {
        if (!isListening && voskRecognizer != null) {
            try {
                voskRecognizer.startListening();
                isListening = true;
                updateButtonStates();
                recognizedTextView.setText("Escuchando...");
                resultTextView.setText(""); // Limpiar resultado anterior
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar reconocimiento de voz", e);
                Toast.makeText(this, "Error al iniciar reconocimiento de voz", Toast.LENGTH_SHORT).show();
                isListening = false;
                updateButtonStates();
            }
        }
    }

    private void stopListening() {
        if (isListening && voskRecognizer != null) {
            try {
                voskRecognizer.stopListening();
                isListening = false;
                updateButtonStates();
                
                // Solo evaluar cuando se detiene el reconocimiento
                String recognizedText = recognizedTextView.getText().toString()
                    .replace("Reconocido: ", "")
                    .replace("Escuchando...", "")
                    .replace("Reconociendo: ", "");
                
                if (!recognizedText.isEmpty()) {
                    double score = evaluator.evaluatePronunciation(recognizedText);
                    // Asegurar que el score no exceda 1.0 (100%)
                    score = Math.min(score, 1.0);
                    
                    // Convertir el score a porcentaje (score ya está entre 0 y 1)
                    double percentage = score * 100;
                    
                    // Guardar la puntuación real de esta pregunta (modo offline)
                    if (currentQuestionIndex < questionScores.size()) {
                        questionScores.set(currentQuestionIndex, percentage);
                        if (percentage >= 70.0) { // 70% como umbral de correcto
                            // Solo sumar puntos si la pregunta NO fue cambiada por un comodín
                            boolean wasQuestionChanged = wildcardHelper.wasQuestionChanged(currentQuestionIndex);
                            if (!wasQuestionChanged) {
                                totalCorrectAnswers++;
                                Log.d(TAG, "Puntos sumados: +1 (Total: " + totalCorrectAnswers + ")");
                            } else {
                                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín");
                            }
                            Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged);
                        }
                        Log.d(TAG, "Pregunta " + (currentQuestionIndex + 1) + " puntuación guardada (offline): " + percentage + "%");
                    }
                    
                    if (score == 1.0) {
                        resultTextView.setText("Puntuación: 100%");
                    } else {
                        resultTextView.setText(String.format("Puntuación: %.1f%%", percentage));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al detener reconocimiento de voz", e);
                Toast.makeText(this, "Error al detener reconocimiento de voz", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateButtonStates() {
        startButton.setEnabled(!isListening);
        stopButton.setEnabled(isListening);
        nextButton.setEnabled(!isListening);
        
        // SEGURIDAD: Deshabilitar audioIcon durante grabación
        if (audioIcon != null) {
            audioIcon.setEnabled(!isListening);
            audioIcon.setAlpha(isListening ? 0.5f : 1.0f); // Visual feedback
            Log.d(TAG, isListening ? "🔇 AudioIcon deshabilitado durante grabación" : "🔊 AudioIcon habilitado");
        }
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(PronunciationActivity.this, MainActivity.class));
        Toast.makeText(PronunciationActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Abre la ayuda contextual para el tema y nivel actuales
     */
    private void showHelp() {
        try {
            String topicForHelp = selectedTopic;
            if (currentQuestions != null && currentQuestionIndex >= 0 && currentQuestionIndex < currentQuestions.size()) {
                PronunciationQuestion q = currentQuestions.get(currentQuestionIndex);
                if (q != null && q.getTopic() != null && !q.getTopic().isEmpty()) {
                    topicForHelp = q.getTopic();
                }
            }

            Intent helpIntent = new Intent(this, HelpActivity.class);
            helpIntent.putExtra("topic", topicForHelp);
            helpIntent.putExtra("level", selectedLevel);
            startActivity(helpIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo HelpActivity: " + e.getMessage());
            Toast.makeText(this, "No se pudo abrir la ayuda", Toast.LENGTH_SHORT).show();
        }
    }

    // Eliminadas implementaciones locales del modal: ahora se usa HelpModalHelper

    private String getReadableTopicTitle(String topic) {
        if ("ALPHABET".equals(topic)) return "Alfabeto";
        if ("NUMBERS".equals(topic)) return "Números";
        if ("COLORS".equals(topic)) return "Colores";
        if ("PERSONAL PRONOUNS".equals(topic)) return "Pronombres personales";
        if ("POSSESSIVE ADJECTIVES".equals(topic)) return "Adjetivos posesivos";
        if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equals(topic)) return "Prepositions";
        if ("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(topic)) return "Adjetivos";
        return topic;
    }

    // Obtener el siguiente tema en el mapa de speaking
    private String getNextSpeakingTopic(String currentTopic) {
        if ("ALPHABET".equals(currentTopic)) {
            return "NUMBERS";
        } else if ("NUMBERS".equals(currentTopic)) {
            return "COLORS";
        } else if ("COLORS".equals(currentTopic)) {
            return "PERSONAL PRONOUNS";
        } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
            return "POSSESSIVE ADJECTIVES";
        }
        // Si llegó aquí, ya completó todos los temas básicos de speaking
        return null;
    }
    
    // Obtener la actividad del siguiente tema
    private Class<?> getNextSpeakingActivity(String topic) {
        switch (topic) {
            case "NUMBERS":
                return com.example.speak.PronNumberActivity.class;
            case "COLORS":
                return com.example.speak.PronColorActivity.class;
            case "PERSONAL PRONOUNS":
                return com.example.speak.PronPersProActivity.class;
            case "POSSESSIVE ADJECTIVES":
                return com.example.speak.PronPosseAdjectActivity.class;
            default:
                return MenuSpeakingActivity.class;
        }
    }
    
    // Guardar progreso de pronunciación basado en puntaje real
    private void savePronunciationProgress(double averageScore) {
        if (selectedTopic != null && selectedLevel != null) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            Log.d(TAG, "=== GUARDANDO PROGRESO DE PRONUNCIACIÓN ===");
            Log.d(TAG, "Tema: " + selectedTopic);
            Log.d(TAG, "Nivel: " + selectedLevel);
            Log.d(TAG, "Puntaje promedio: " + averageScore + "%");
            
            // Guardar puntaje con clave estándar
            String scoreKey = "PRONUNCIATION_SCORE_" + selectedTopic.toUpperCase().replace(" ", "_");
            editor.putFloat(scoreKey, (float) averageScore);
            
            // Marcar como aprobado si es >= 70% - USAR CLAVES CORRECTAS PARA SPEAKING
            if (averageScore >= 70.0) {
                // Mapear temas a las claves que espera MenuSpeakingActivity
                String progressKey = null;
                if ("ALPHABET".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_ALPHABET";
                } else if ("NUMBERS".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_NUMBERS";
                } else if ("COLORS".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_COLORS";
                } else if ("PERSONAL PRONOUNS".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_PERSONAL_PRONOUNS";
                } else if ("POSSESSIVE ADJECTIVES".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_POSSESSIVE_ADJECTIVES";
                } else if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_PREPOSITIONS_OF_PLACE";
                } else if ("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(selectedTopic)) {
                    progressKey = "PASSED_PRON_ADJECTIVES";
                } else {
                    // Fallback para temas no mapeados
                    progressKey = "PRONUNCIATION_PASSED_" + selectedTopic.toUpperCase().replace(" ", "_");
                }
                
                editor.putBoolean(progressKey, true);
                Log.d(TAG, "✅ PRONUNCIACIÓN APROBADA - Clave: " + progressKey + " = true");
                Log.d(TAG, "✅ Puntaje guardado - Clave: " + scoreKey + " = " + averageScore + "%");

                // También actualizar las claves genéricas usadas para trofeos (como en Listening)
                if ("ALPHABET".equals(selectedTopic)) {
                    editor.putBoolean("PASSED_ALPHABET", true);
                } else if ("NUMBERS".equals(selectedTopic)) {
                    editor.putBoolean("PASSED_NUMBERS", true);
                } else if ("COLORS".equals(selectedTopic)) {
                    editor.putBoolean("PASSED_COLORS", true);
                } else if ("PERSONAL PRONOUNS".equals(selectedTopic)) {
                    editor.putBoolean("PASSED_PERSONAL_PRONOUNS", true);
                } else if ("POSSESSIVE ADJECTIVES".equals(selectedTopic)) {
                    editor.putBoolean("PASSED_POSSESSIVE_ADJECTIVES", true);
                }

                // Sumar estrellas por sesión aprobada (10 puntos)
                StarProgressHelper.addSessionPoints(this, 10);
                
                Toast.makeText(this, "¡Felicidades! Has completado " + selectedTopic + " con " + 
                    String.format("%.1f%%", averageScore), Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "❌ Puntaje insuficiente para aprobar: " + averageScore + "%");
                Toast.makeText(this, "Necesitas 70% para aprobar. Tu puntaje: " + 
                    String.format("%.1f%%", averageScore), Toast.LENGTH_LONG).show();
            }
            
            editor.apply();
            Log.d(TAG, "=== PROGRESO GUARDADO EXITOSAMENTE ===");
        }
    }

    /**
     * Muestra el audioIcon como retroalimentación después de contestar una pregunta
     * Incluye TODOS los temas para proporcionar retroalimentación de aprendizaje
     * @param topic El tema actual
     */
    private void showAudioIconAfterAnswer(String topic) {
        if (topic == null) return;
        
        // RETROALIMENTACIÓN: Mostrar parlante para TODOS los temas después de contestar
        audioIcon.setVisibility(View.VISIBLE);
        Log.d(TAG, "AudioIcon activado como retroalimentación para tema: " + topic);
        
        // Mensaje especial para temas básicos la primera vez
        if (isBasicTopic(topic) && currentQuestionIndex == 0) {
            new Handler().postDelayed(() -> {
                if (audioIcon != null && audioIcon.getVisibility() == View.VISIBLE) {
                    Toast.makeText(this, 
                        "🔊 ¡Retroalimentación! Toca el parlante para escuchar la pronunciación correcta", 
                        Toast.LENGTH_LONG).show();
                }
            }, 500);
        }
    }

    /**
     * Verifica si un tema es básico (ALPHABET, NUMBERS, COLORS)
     * @param topic El tema a verificar
     * @return true si es un tema básico
     */
    private boolean isBasicTopic(String topic) {
        if (topic == null) return false;
        
        String[] basicTopics = {"ALPHABET", "NUMBERS", "COLORS"};
        for (String basicTopic : basicTopics) {
            if (basicTopic.equalsIgnoreCase(topic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * SEGURIDAD: Bloquea botones durante reproducción de audio para evitar evaluación del audio
     */
    private void blockButtonsDuringAudio() {
        isPlayingAudio = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);
        
        // SEGURIDAD: También bloquear audioIcon durante reproducción de audio
        if (audioIcon != null) {
            audioIcon.setEnabled(false);
            audioIcon.setAlpha(0.5f); // Visual feedback
        }
        
        Log.d(TAG, "🔒 Botones bloqueados durante reproducción de audio");
    }

    /**
     * SEGURIDAD: Desbloquea botones después de reproducción de audio
     */
    private void unblockButtonsAfterAudio() {
        isPlayingAudio = false;
        
        // Solo habilitar START si no está en medio de una grabación
        if (!isListening) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            nextButton.setEnabled(true);
            
            // SEGURIDAD: Restaurar audioIcon si no está grabando
            if (audioIcon != null) {
                audioIcon.setEnabled(true);
                audioIcon.setAlpha(1.0f); // Restaurar visibilidad completa
            }
            
            Log.d(TAG, "🔓 Botones desbloqueados después de reproducción de audio");
        }
    }

    /**
     * Reproduce el sonido de victoria
     */
    private void playVictorySound() {
        try {
            if (victorySound != null) {
                if (victorySound.isPlaying()) {
                    victorySound.stop();
                    victorySound.prepare();
                }
                victorySound.start();
                Log.d(TAG, "🎉 Sonido de victoria reproducido");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido de victoria: " + e.getMessage());
        }
    }

    /**
     * Reproduce el sonido de derrota
     */
    private void playDefeatSound() {
        try {
            if (defeatSound != null) {
                if (defeatSound.isPlaying()) {
                    defeatSound.stop();
                    defeatSound.prepare();
                }
                defeatSound.start();
                Log.d(TAG, "😔 Sonido de derrota reproducido");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido de derrota: " + e.getMessage());
        }
    }

    // ===== SISTEMA DE COMODINES =====

    /**
     * Muestra el menú de comodines
     */
    private void showWildcardMenu() {
        if (wildcardHelper != null) {
            wildcardHelper.showWildcardMenu();
        }
    }

    /**
     * Ayuda 1: Cambiar la pregunta actual por una nueva
     * IMPORTANTE: Se marca la pregunta original como no evaluada para evitar duplicación de puntos
     */
    private void changeCurrentQuestion() {
        if (currentQuestions.size() > 1) {
            // Marcar la pregunta actual como cambiada para evitar duplicación de puntos
            wildcardHelper.markQuestionAsChanged(currentQuestionIndex);
            
            // Obtener una pregunta diferente de la lista
            int newIndex = (currentQuestionIndex + 1) % currentQuestions.size();
            currentQuestionIndex = newIndex;
            
            // Mostrar la nueva pregunta
            showCurrentQuestion();
            
            Toast.makeText(this, "Pregunta cambiada - La anterior no se evaluará", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay más preguntas disponibles", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ayuda 2: Mostrar imagen de contenido relacionado
     */
    private void showContentImage() {
        // Mostrar una imagen relacionada con el tema actual
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Imagen de Contenido");
        builder.setMessage("Aquí se mostraría una imagen relacionada con: " + selectedTopic);
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }

    /**
     * Ayuda 3: Mostrar video del instructor
     */
    private void showInstructorVideo() {
        // Usar el VideoHelper del WildcardHelper para mostrar el video
        if (wildcardHelper != null) {
            wildcardHelper.showInstructorVideo();
        } else {
            // Fallback si no hay wildcardHelper
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Video del Instructor");
            builder.setMessage("Aquí se reproduciría un video explicativo sobre: " + selectedTopic);
            builder.setPositiveButton("Entendido", null);
            builder.show();
        }
    }

    /**
     * Ayuda 5: Ayuda creativa - Mostrar pista contextual
     */
    private void showCreativeHelp() {
        if (currentQuestionIndex < currentQuestions.size()) {
            PronunciationQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            String question = currentQuestion.getQuestion();
            
            String creativeHelp = wildcardHelper.getCreativeHelp(question, "");
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("💡 Ayuda Creativa");
            builder.setMessage(creativeHelp);
            builder.setPositiveButton("Entendido", null);
            builder.show();
        }
    }
    
    /**
     * Muestra información sobre el uso de comodines en la sesión actual
     */
    private void showWildcardUsageInfo() {
        // Para PronunciationActivity, mostrar información básica
        StringBuilder info = new StringBuilder();
        info.append("📊 Información de Comodines\n\n");
        info.append("• Tema actual: ").append(selectedTopic).append("\n");
        info.append("• Nivel: ").append(selectedLevel).append("\n");
        info.append("• Comodines disponibles: ").append(wildcardHelper.getRemainingWildcardsCount()).append("\n\n");
        info.append("ℹ️ Los comodines te ayudan sin afectar tu evaluación final.\n");
        info.append("Usa las ayudas cuando las necesites para mejorar tu aprendizaje.");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎯 Uso de Comodines");
        builder.setMessage(info.toString());
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }
} 