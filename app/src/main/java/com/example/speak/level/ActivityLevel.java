package com.example.speak.level;

//Se importan clases necesarias para: permisos, conectividad, interfaz de usuario, y control de base de datos.
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.example.speak.level.LevelEvaluator;
import com.example.speak.R;
import com.example.speak.database.DatabaseHelper;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import android.util.Log;
import android.app.AlertDialog;
import android.os.Handler;
import java.util.Locale;
import com.example.speak.vosk.VoskSpeechRecognizer;
import android.view.MotionEvent;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// Actividad principal de pronunciación
public class ActivityLevel extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123; // Código para solicitar permisos
    private LevelEvaluator evaluator; // Evaluador de pronunciación
    private TextView referenceTextView;
    private Button startButton;
    private Button stopButton;
    private Button nextButton;
    private TextView resultTextView;
    private TextView recognizedTextView;
    private TextView questionNumberTextView;
    private List<String> questions; // Lista de frases a pronunciar
    private int currentQuestionIndex = 0; // Índice actual en la lista
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
    private String currentTopic;
    private List<String> topicQuestions;
    private String currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level);

        // Obtener el tema y nivel del intent
        currentTopic = getIntent().getStringExtra("TOPIC");
        currentLevel = getIntent().getStringExtra("LEVEL");
        
        if (currentTopic == null || currentLevel == null) {
            Toast.makeText(this, "Error: No se seleccionó un tema o nivel", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Guardar el ID del usuario actual en SharedPreferences
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("user_id", dbHelper.getUserId(currentUser.getEmail()));
            editor.apply();
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

        // Cargar las preguntas del archivo
        loadQuestions();

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(ActivityLevel.this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ActivityLevel.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize audio icon
        audioIcon = findViewById(R.id.audioIcon);
        audioIcon.setOnClickListener(v -> {
            String textToSpeak = referenceTextView.getText().toString();
            if (textToSpeech != null) {
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
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
                    Toast.makeText(ActivityLevel.this, error, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ActivityLevel.this, error, Toast.LENGTH_SHORT).show();
                            recognizedTextView.setText("Error: " + error);
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
        evaluator = new LevelEvaluator(this);
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

    // Carga preguntas desde un archivo .txt del directorio assets
    private void loadQuestions() {
        topicQuestions = new ArrayList<>();
        try {
            InputStream is = getAssets().open("SENA_Level_1_A1.1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isCurrentTopic = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Detectar inicio de tema
                if (line.startsWith("Topic:")) {
                    String topicName = line.substring(6).trim();
                    isCurrentTopic = topicName.equals(currentTopic);
                }
                // Si estamos en el tema correcto, agregar preguntas
                else if (isCurrentTopic && !line.isEmpty() && 
                         !line.startsWith("Activity:") && 
                         !line.startsWith("Objective:")) {
                    topicQuestions.add(line);
                }
            }
            reader.close();
            is.close();

            if (topicQuestions.isEmpty()) {
                Toast.makeText(this, "No se encontraron preguntas para este tema", 
                    Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Mostrar la primera pregunta
            showCurrentQuestion();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error cargando preguntas", Toast.LENGTH_SHORT).show();
        }
    }

    // Muestra en pantalla la pregunta actual
    private void showCurrentQuestion() {
        if (currentQuestionIndex < topicQuestions.size()) {
            String question = topicQuestions.get(currentQuestionIndex);
            referenceTextView.setText(question);
            questionNumberTextView.setText(String.format("Pregunta %d/%d", 
                currentQuestionIndex + 1, topicQuestions.size()));
            recognizedTextView.setText("");
            resultTextView.setText("");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            showFinalResults();
        }
    }

    // Muestra el resultado final cuando se completa la actividad
    private void showFinalResults() {
        // Calcular puntaje final
        int totalQuestions = topicQuestions.size();
        int passedQuestions = 0;
        double totalScore = 0;

        // Crear un evaluador temporal para calcular los puntajes
        LevelEvaluator tempEvaluator = new LevelEvaluator(this);

        for (String question : topicQuestions) {
            tempEvaluator.setReferenceText(question);
            // Usar el método evaluatePronunciation con un texto vacío para obtener el puntaje base
            // Simula evaluación vacía
            double score = tempEvaluator.evaluatePronunciation("");
            if (score >= 0.7) { // 70% en decimal
                passedQuestions++;
            }
            totalScore += score;
        }

        // Limpiar el evaluador temporal
        tempEvaluator.destroy();

        double averageScore = (totalScore / totalQuestions) * 100; // Convertir a porcentaje
        String resultMessage = String.format(
                "¡Prueba completada!\n\n" +
                        "Preguntas aprobadas: %d de %d\n" +
                        "Puntaje promedio: %.1f%%\n\n" +
                        "Preguntas que necesitan práctica: %d",
                passedQuestions, totalQuestions, averageScore,
                totalQuestions - passedQuestions
        );

        referenceTextView.setText("");
        resultTextView.setText(resultMessage);
        questionNumberTextView.setText("");

        // Deshabilitar botones de control
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    // Carga la siguiente pregunta
    private void loadNextQuestion() {
        if (topicQuestions != null && !topicQuestions.isEmpty()) {
            currentQuestionIndex = (currentQuestionIndex + 1) % topicQuestions.size();
            showCurrentQuestion();
        }
    }

    // Inicia la evaluación de pronunciación
    private void startPronunciationTest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        // Deshabilitar botones durante la inicialización
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);

        // Limpiar resultados anteriores
        recognizedTextView.setText("");
        resultTextView.setText("");

        // Crear nuevo evaluador
        evaluator = new LevelEvaluator(this);
        evaluator.setOfflineMode(isOfflineMode);
        evaluator.setReferenceText(topicQuestions.get(currentQuestionIndex));

        if (isOfflineMode) {
            // Usar Vosk para modo offline
            if (voskRecognizer != null) {
                startListening();
            }
        } else {
            // Usar Google Speech Recognition para modo online
            evaluator.startListening(new LevelEvaluator.PronunciationCallback() {
                @Override
                public void onResult(double score, String recognizedText) {
                    runOnUiThread(() -> {
                        recognizedTextView.setText("Reconocido: " + recognizedText);
                        resultTextView.setText(String.format("Puntuación: %.1f%%", score * 100).replace(".", ","));
                        stopButton.setEnabled(false);
                        startButton.setEnabled(true);
                        nextButton.setEnabled(true);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        resultTextView.setText(error);
                        if (error.contains("¡Ahora!")) {
                            stopButton.setEnabled(true);
                            startButton.setEnabled(false);
                            nextButton.setEnabled(false);
                        }
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

    // Detiene la grabación de audio
    private void stopPronunciationTest() {
        if (isOfflineMode) {
            stopListening();

            String recognizedText = recognizedTextView.getText().toString()
                    .replace("Reconocido: ", "")
                    .replace("Escuchando...", "")
                    .replace("Reconociendo: ", "");

            if (!recognizedText.isEmpty() && topicQuestions != null && currentQuestionIndex < topicQuestions.size()) {
                String referenceText = topicQuestions.get(currentQuestionIndex);
                LevelEvaluator evaluator = new LevelEvaluator(ActivityLevel.this);
                evaluator.setReferenceText(referenceText);
                double score = evaluator.evaluatePronunciation(recognizedText);

                if (dbHelper != null) {
                    savePronunciationResult(referenceText, recognizedText, score);
                    resultTextView.setText(String.format("Puntuación: %.1f%%", score * 100).replace(".", ","));
                    audioIcon.setVisibility(View.VISIBLE);
                }
            }

            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            nextButton.setEnabled(true);
        } else {
            if (evaluator != null) {
                evaluator.stopListening();
                audioIcon.setVisibility(View.VISIBLE);
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
                    if (score == 1.0) {
                        resultTextView.setText("Puntuación: 100%");
                    } else {
                        // Convertir el score a porcentaje (score ya está entre 0 y 1)
                        double percentage = score * 100;
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
    }

    private void savePronunciationResult(String referenceText, String spokenText, double score) {
        long userId = dbHelper.getPronunciationUserId();
        if (userId == -1) {
            // Si no hay ID de usuario para pronunciación, usar el ID actual
            userId = dbHelper.getCurrentUserId();
            // Guardar el ID para futuras referencias
            dbHelper.savePronunciationUserId(userId);
        }
        
        dbHelper.savePronunciationResult(userId, referenceText, spokenText, score, currentTopic, currentLevel);
    }

    public String getCurrentTopic() {
        return currentTopic;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }
}
