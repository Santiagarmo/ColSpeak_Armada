package com.example.speak.pronunciation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.speak.R;
import com.example.speak.database.DatabaseHelper;
import com.example.speak.level.LevelEvaluator;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.app.AlertDialog;
import android.os.Handler;
import java.util.Locale;
import com.example.speak.vosk.VoskSpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.content.SharedPreferences;

public class TopicPronunciationActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private LevelEvaluator evaluator;
    private TextView referenceTextView;
    private Button startButton;
    private Button stopButton;
    private Button nextButton;
    private TextView resultTextView;
    private TextView recognizedTextView;
    private TextView questionNumberTextView;
    private List<String> topicQuestions;
    private int currentQuestionIndex = 0;
    private DatabaseHelper dbHelper;
    private boolean isOfflineMode = false;
    private static final String TAG = "TopicPronunciationActivity";
    private VoskSpeechRecognizer voskRecognizer;
    private boolean isListening = false;
    private ImageView audioIcon;
    private TextToSpeech textToSpeech;
    private String currentTopicId;
    private String currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_pronunciation);

        // Obtener el ID del tema y nivel del intent
        currentTopicId = getIntent().getStringExtra("TOPIC_ID");
        currentLevel = getIntent().getStringExtra("LEVEL");
        
        if (currentTopicId == null || currentLevel == null) {
            Toast.makeText(this, "Error: No se seleccionó un tema o nivel", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Verificar conexión a internet
        isOfflineMode = !isNetworkAvailable();
        if (isOfflineMode) {
            Toast.makeText(this, "Modo offline activado", Toast.LENGTH_LONG).show();
            initializeVoskRecognizer();
        }

        // Inicializar vistas
        initializeViews();
        
        // Cargar las preguntas del tema
        loadTopicQuestions();
    }

    private void initializeViews() {
        referenceTextView = findViewById(R.id.referenceTextView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        nextButton = findViewById(R.id.nextButton);
        resultTextView = findViewById(R.id.resultTextView);
        recognizedTextView = findViewById(R.id.recognizedTextView);
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        audioIcon = findViewById(R.id.audioIcon);

        // Configurar botones
        startButton.setOnClickListener(v -> startPronunciationTest());
        stopButton.setOnClickListener(v -> stopPronunciationTest());
        nextButton.setOnClickListener(v -> loadNextQuestion());
        stopButton.setEnabled(false);
        nextButton.setEnabled(false);

        // Configurar icono de audio
        audioIcon.setOnClickListener(v -> {
            String textToSpeak = referenceTextView.getText().toString();
            if (textToSpeech != null) {
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Inicializar TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(TopicPronunciationActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(TopicPronunciationActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTopicQuestions() {
        Cursor cursor = dbHelper.getPronunciationTopic(currentTopicId);
        if (cursor != null && cursor.moveToFirst()) {
            String questionsJson = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC_QUESTIONS));
            cursor.close();
            
            // Parsear las preguntas del JSON
            topicQuestions = parseQuestionsFromJson(questionsJson);
            
            if (topicQuestions.isEmpty()) {
                Toast.makeText(this, "No se encontraron preguntas para este tema", 
                    Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Mostrar la primera pregunta
            showCurrentQuestion();
        } else {
            Toast.makeText(this, "Error al cargar el tema", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private List<String> parseQuestionsFromJson(String questionsJson) {
        List<String> questions = new ArrayList<>();
        // Aquí implementarías la lógica para parsear el JSON
        // Por ahora, asumimos que las preguntas están separadas por comas
        String[] questionArray = questionsJson.split(",");
        for (String question : questionArray) {
            questions.add(question.trim());
        }
        return questions;
    }

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

    private void showFinalResults() {
        // Implementar lógica para mostrar resultados finales
        // Similar a la implementación existente en ActivityLevel
    }

    private void loadNextQuestion() {
        if (topicQuestions != null && !topicQuestions.isEmpty()) {
            currentQuestionIndex = (currentQuestionIndex + 1) % topicQuestions.size();
            showCurrentQuestion();
        }
    }

    private void startPronunciationTest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        try {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            nextButton.setEnabled(false);

            recognizedTextView.setText("");
            resultTextView.setText("");

            evaluator = new LevelEvaluator(this);
            evaluator.setOfflineMode(isOfflineMode);
            evaluator.setReferenceText(topicQuestions.get(currentQuestionIndex));

            if (isOfflineMode) {
                if (voskRecognizer != null) {
                    startListening();
                } else {
                    Toast.makeText(this, "Error: Reconocedor de voz no inicializado", Toast.LENGTH_SHORT).show();
                    startButton.setEnabled(true);
                }
            } else {
                evaluator.startListening(new LevelEvaluator.PronunciationCallback() {
                    @Override
                    public void onResult(double score, String recognizedText) {
                        runOnUiThread(() -> {
                            try {
                                if (recognizedText != null && !recognizedText.isEmpty()) {
                                    recognizedTextView.setText("Reconocido: " + recognizedText);
                                    resultTextView.setText(String.format("Puntuación: %.1f%%", score * 100));
                                    
                                    // Guardar el resultado en la base de datos
                                    saveResultToDatabase(recognizedText, score);
                                } else {
                                    resultTextView.setText("No se pudo reconocer el audio. Intenta de nuevo.");
                                }
                                
                                stopButton.setEnabled(false);
                                startButton.setEnabled(true);
                                nextButton.setEnabled(true);
                            } catch (Exception e) {
                                Log.e(TAG, "Error en onResult: " + e.getMessage());
                                Toast.makeText(TopicPronunciationActivity.this, 
                                    "Error al procesar el resultado", Toast.LENGTH_SHORT).show();
                                startButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            try {
                                Log.e(TAG, "Error en reconocimiento: " + error);
                                if (error.contains("¡Ahora!")) {
                                    stopButton.setEnabled(true);
                                    startButton.setEnabled(false);
                                    nextButton.setEnabled(false);
                                } else if (error.contains("Error de audio")) {
                                    resultTextView.setText("Error de audio. Asegúrate de hablar claramente y cerca del micrófono.");
                                    startButton.setEnabled(true);
                                    stopButton.setEnabled(false);
                                    nextButton.setEnabled(false);
                                } else if (error.contains("No se encontró coincidencia")) {
                                    resultTextView.setText("No se pudo reconocer el audio. Intenta hablar más claro y cerca del micrófono.");
                                    startButton.setEnabled(true);
                                    stopButton.setEnabled(false);
                                    nextButton.setEnabled(false);
                                } else {
                                    resultTextView.setText("Error: " + error);
                                    startButton.setEnabled(true);
                                    stopButton.setEnabled(false);
                                    nextButton.setEnabled(false);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error en onError: " + e.getMessage());
                                Toast.makeText(TopicPronunciationActivity.this, 
                                    "Error en el reconocimiento", Toast.LENGTH_SHORT).show();
                                startButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onSpeechDetected() {
                        runOnUiThread(() -> {
                            try {
                                stopButton.setEnabled(true);
                                startButton.setEnabled(false);
                                nextButton.setEnabled(false);
                                resultTextView.setText("Habla ahora...");
                            } catch (Exception e) {
                                Log.e(TAG, "Error en onSpeechDetected: " + e.getMessage());
                                startButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onPartialResult(String partialText) {
                        runOnUiThread(() -> {
                            try {
                                if (partialText != null && !partialText.isEmpty()) {
                                    recognizedTextView.setText("Reconocido: " + partialText);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error en onPartialResult: " + e.getMessage());
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en startPronunciationTest: " + e.getMessage());
            Toast.makeText(this, "Error al iniciar el reconocimiento", Toast.LENGTH_SHORT).show();
            startButton.setEnabled(true);
        }
    }

    private void saveResultToDatabase(String recognizedText, double score) {
        try {
            // Obtener el ID del usuario actual
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            long userId = prefs.getLong("user_id", -1);
            
            if (userId != -1) {
                // Guardar el resultado en la base de datos
                long resultId = dbHelper.savePronunciationResult(
                    userId,
                    topicQuestions.get(currentQuestionIndex), // texto de referencia
                    recognizedText,
                    score,
                    currentTopicId,
                    currentLevel
                );
                
                if (resultId != -1) {
                    Log.d(TAG, "Resultado guardado exitosamente con ID: " + resultId);
                } else {
                    Log.e(TAG, "Error al guardar el resultado en la base de datos");
                }
            } else {
                Log.e(TAG, "No se encontró ID de usuario");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar resultado: " + e.getMessage());
        }
    }

    private void stopPronunciationTest() {
        try {
            if (isOfflineMode) {
                stopListening();
            } else if (evaluator != null) {
                evaluator.stopListening();
            }
            updateButtonStates();
        } catch (Exception e) {
            Log.e(TAG, "Error en stopPronunciationTest: " + e.getMessage());
            Toast.makeText(this, "Error al detener el reconocimiento", Toast.LENGTH_SHORT).show();
            startButton.setEnabled(true);
        }
    }

    private void startListening() {
        if (!isListening && voskRecognizer != null) {
            try {
                voskRecognizer.startListening();
                isListening = true;
                updateButtonStates();
                recognizedTextView.setText("Escuchando...");
                resultTextView.setText("");
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

                String recognizedText = recognizedTextView.getText().toString()
                        .replace("Reconocido: ", "")
                        .replace("Escuchando...", "")
                        .replace("Reconociendo: ", "");

                if (!recognizedText.isEmpty()) {
                    double score = evaluator.evaluatePronunciation(recognizedText);
                    score = Math.min(score, 1.0);
                    if (score == 1.0) {
                        resultTextView.setText("Puntuación: 100%");
                    } else {
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
        try {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            nextButton.setEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Error en updateButtonStates: " + e.getMessage());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void initializeVoskRecognizer() {
        voskRecognizer = new VoskSpeechRecognizer(this);
        voskRecognizer.initialize(new VoskSpeechRecognizer.RecognitionListener() {
            @Override
            public void onResult(String text) {
                runOnUiThread(() -> {
                    recognizedTextView.setText("Reconocido: " + text);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TopicPronunciationActivity.this, error, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
} 