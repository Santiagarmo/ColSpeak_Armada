package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.database.DatabaseHelper;
import com.example.speak.helpers.WildcardHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WritingActivity extends AppCompatActivity {
    private static final String TAG = "WritingActivity";
    private TextView questionNumberTextView;
    private TextView scoreTextView;
    private TextView instructionsTextView;
    // Nuevo componente de audio reutilizable
    private com.example.speak.components.ReusableAudioPlayerCard reusableAudioCard;
    private EditText answerEditText;
    private TextView originalTextTextView;
    private Button submitButton;
    private Button nextButton;

    // Return menu and topic/level views
    private LinearLayout returnContainer;
    private TextView topicTextView;
    private TextView levelTextView;

    private TextToSpeech textToSpeech;
    private List<WritingQuestion> allQuestions;
    private List<WritingQuestion> currentQuestions;
    private int currentQuestionIndex = 0;
    // Selected topic and level for this writing session
    private String selectedTopic = "ALPHABET";
    private String selectedLevel = "A1.1"; // Default level
    private int score = 0;
    private float currentSpeed = 1.0f;
    private float currentPitch = 1.0f;

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long userId;
    private boolean isOfflineMode = false;

    // MediaPlayer para sonidos de victoria y derrota
    private MediaPlayer victorySound;
    private MediaPlayer defeatSound;

    // Sistema de comodines
    private WildcardHelper wildcardHelper;
    private ImageView wildcardButton;
    private ImageView helpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writing);

        // Initialize views
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        reusableAudioCard = findViewById(R.id.reusableAudioCard);
        answerEditText = findViewById(R.id.answerEditText);
        originalTextTextView = findViewById(R.id.originalTextTextView);
        submitButton = findViewById(R.id.submitButton);
        nextButton = findViewById(R.id.nextButton);


        // Initialize return menu and topic/level views
        returnContainer = findViewById(R.id.returnContainer);
        topicTextView = findViewById(R.id.topicTextView);
        levelTextView = findViewById(R.id.levelTextView);

        // Set topic and level text
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("Topic: " + selectedTopic);
        }

        // Set up return button click listener
        returnContainer.setOnClickListener(v -> returnToMenu());

        // Inicializar botón de comodines
        wildcardButton = findViewById(R.id.wildcardButton);
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> showWildcardMenu());
        }

        // Inicializar botón de ayuda
        helpButton = findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> showHelp());
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Check network status
        isOfflineMode = !isNetworkAvailable();
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1);
        String email = prefs.getString("user_email", null);

        Log.d(TAG, "Initial user ID from prefs: " + userId);
        Log.d(TAG, "User email from prefs: " + email);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported");
                        Toast.makeText(WritingActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.setSpeechRate(currentSpeed);
                        textToSpeech.setPitch(currentPitch);
                        Log.d(TAG, "TextToSpeech initialized successfully");
                    }
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed");
                    Toast.makeText(WritingActivity.this, "Text to Speech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup speed and pitch controls

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

        // Retrieve topic and level from Intent
        String topicExtra = getIntent().getStringExtra("TOPIC");
        String levelExtra = getIntent().getStringExtra("LEVEL");
        if (topicExtra != null && !topicExtra.trim().isEmpty()) {
            selectedTopic = topicExtra.trim();
        }
        if (levelExtra != null && !levelExtra.trim().isEmpty()) {
            selectedLevel = levelExtra.trim();
        }

        // Inicializar sistema de comodines
        wildcardHelper = new WildcardHelper(this, "WRITING", selectedTopic);
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
                // No aplicable para writing, pero mantenemos la interfaz
                Toast.makeText(WritingActivity.this, "50/50 no aplicable para ejercicios de escritura", Toast.LENGTH_SHORT).show();
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

        // Load questions and start the exercise for selected topic
        loadQuestionsFromFile();

        // Configurar componente reutilizable para reproducir el enunciado vía TTS
        setupReusableAudioForWriting();
        if (reusableAudioCard != null) {
            reusableAudioCard.setPlaybackListener(new com.example.speak.components.ReusableAudioPlayerCard.PlaybackListener() {
                @Override public void onPlayStarted() { runOnUiThread(() -> answerEditText.setEnabled(true)); }
                @Override public void onPaused() { }
                @Override public void onResumed() { }
                @Override public void onStopped() { }
            });
        }
        submitButton.setOnClickListener(v -> checkAnswer());
        nextButton.setOnClickListener(v -> {
            currentQuestionIndex++;
            if (currentQuestionIndex < currentQuestions.size()) {
                displayQuestion();
            } else {
                showResults();
            }
        });
    }



    private void loadQuestionsFromFile() {
        allQuestions = new ArrayList<>();
        try {
            InputStream is = getAssets().open("A1.1_Basic_English_Topics.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            String currentText = "";
            String currentTopic = "";
            String currentLevel = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Topic:")) {
                    currentTopic = line.substring(6).trim();
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim();
                } else if (line.startsWith("Q: ")) {
                    // Extraer solo la parte después de "Q: "
                    currentText = line.substring(3).trim();
                    allQuestions.add(new WritingQuestion(currentText, currentTopic, currentLevel));
                }
            }
            reader.close();
            is.close();

            // --- Filtrar por topic seleccionado ---
            List<WritingQuestion> filtered = new ArrayList<>();
            for (WritingQuestion q : allQuestions) {
                if (q.getTopic().equalsIgnoreCase(selectedTopic)) {
                    filtered.add(q);
                }
            }

            List<WritingQuestion> pool = filtered.isEmpty() ? allQuestions : filtered;

            // Shuffle and take first 10
            Collections.shuffle(pool);
            currentQuestions = new ArrayList<>(pool.subList(0, Math.min(10, pool.size())));

            // Update topic and level from the first question if available
            if (!currentQuestions.isEmpty()) {
                WritingQuestion firstQuestion = currentQuestions.get(0);
                selectedTopic = firstQuestion.getTopic();
                selectedLevel = firstQuestion.getLevel();

                // Update UI with actual topic and level from questions
                if (topicTextView != null) {
                    topicTextView.setText("Topic: " + selectedTopic);
                }
                if (levelTextView != null) {
                    levelTextView.setText("Level: " + selectedLevel);
                }
            }

            // Display the first question
            displayQuestion();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading questions", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex < currentQuestions.size()) {
            WritingQuestion question = currentQuestions.get(currentQuestionIndex);
            questionNumberTextView.setText(String.format("%d/%d",
                    currentQuestionIndex + 1, currentQuestions.size()));
            scoreTextView.setText("Score: " + score);
            instructionsTextView.setText("Escucha atentamente y escribe lo que oyes /\nListen carefully and write what you hear:");
            originalTextTextView.setVisibility(View.GONE);
            answerEditText.setText("");
            answerEditText.setEnabled(false);
            submitButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.GONE);
        } else {
            showResults();
        }
    }

    private void playCurrentQuestion() {
        if (currentQuestionIndex >= currentQuestions.size()) return;
        WritingQuestion question = currentQuestions.get(currentQuestionIndex);
        String textToSpeak = question.getText();
        try {
            if (reusableAudioCard != null) {
                // Configure en modo TTS (sin carpeta de assets)
                reusableAudioCard.configure("", textToSpeak);
                reusableAudioCard.setEnglishMode();
                answerEditText.setEnabled(true);
            } else if (textToSpeech != null) {
                // Fallback
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "writing_utterance");
                answerEditText.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing with reusableAudioCard: " + e.getMessage());
            if (textToSpeech != null) {
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "writing_utterance");
                answerEditText.setEnabled(true);
            }
        }
    }

    private void setupReusableAudioForWriting() {
        try {
            if (reusableAudioCard != null) {
                String initialText = "";
                if (currentQuestions != null && !currentQuestions.isEmpty()) {
                    initialText = currentQuestions.get(0).getText();
                }
                reusableAudioCard.configure("", initialText);
                reusableAudioCard.setEnglishMode();
            }
        } catch (Exception e) {
            Log.e(TAG, "setupReusableAudioForWriting error: " + e.getMessage());
        }
    }

    private void checkAnswer() {
        String userAnswer = answerEditText.getText().toString().trim();
        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Please write your answer", Toast.LENGTH_SHORT).show();
            return;
        }

        WritingQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
        String originalText = currentQuestion.getText();

        // Calculate similarity
        double similarity = calculateSimilarity(userAnswer.toLowerCase(), originalText.toLowerCase());
        boolean isCorrect = similarity >= 0.7; // 70% threshold

        // Verificar si esta pregunta fue cambiada por un comodín
        boolean wasQuestionChanged = wildcardHelper.wasQuestionChanged(currentQuestionIndex);

        // Show the original text
        originalTextTextView.setText("Original text: " + originalText);
        originalTextTextView.setVisibility(View.VISIBLE);

        if (isCorrect) {
            // Solo sumar puntos si la pregunta NO fue cambiada por un comodín
            if (!wasQuestionChanged) {
                score++;
                Log.d(TAG, "Puntos sumados: +1 (Total: " + score + ")");
                Toast.makeText(this, "Correct! Similarity: " + String.format("%.1f%%", similarity * 100),
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín");
                Toast.makeText(this, "Correct! (Pregunta cambiada por comodín - No suma puntos) Similarity: " + String.format("%.1f%%", similarity * 100),
                        Toast.LENGTH_SHORT).show();
            }

            // Reproducir sonido de victoria
            playVictorySound();
            Log.d(TAG, "🎉 Sonido de victoria para pregunta individual (similarity: " + (similarity * 100) + "%)");
        } else {
            Toast.makeText(this, "Incorrect. Similarity: " + String.format("%.1f%%", similarity * 100),
                    Toast.LENGTH_SHORT).show();
            // Reproducir sonido de derrota
            playDefeatSound();
            Log.d(TAG, "😔 Sonido de derrota para pregunta individual (similarity: " + (similarity * 100) + "%)");
        }

        // Save answer to database
        saveAnswerToDatabase(currentQuestion, userAnswer, isCorrect, similarity);

        // Show next button and hide submit button
        submitButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        answerEditText.setEnabled(false);
    }

    private double calculateSimilarity(String s1, String s2) {
        // Simple Levenshtein distance-based similarity
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private void saveAnswerToDatabase(WritingQuestion question, String userAnswer, boolean isCorrect, double similarity) {
        try {
            // Save to local database
            dbHelper.saveWritingResult(
                    String.valueOf(userId),
                    question.getText(),
                    userAnswer,
                    isCorrect,
                    similarity,
                    question.getTopic(),
                    question.getLevel()
            );
            Log.d(TAG, "Answer saved to writing_results table successfully");

            // Save to Firebase if online and user is authenticated
            if (!isOfflineMode && mAuth.getCurrentUser() != null) {
                String questionId = String.valueOf(currentQuestionIndex);
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("question", question.getText());
                answerData.put("userAnswer", userAnswer);
                answerData.put("isCorrect", isCorrect);
                answerData.put("similarity", similarity);
                answerData.put("timestamp", System.currentTimeMillis());
                answerData.put("speed", currentSpeed);
                answerData.put("pitch", currentPitch);
                answerData.put("topic", question.getTopic());
                answerData.put("level", question.getLevel());

                mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                        .child("writing_results")
                        .child(questionId)
                        .setValue(answerData);
                Log.d(TAG, "Answer saved to Firebase successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving answer: " + e.getMessage());
            Toast.makeText(this, "Error saving answer", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResults() {
        // Calculate final score percentage
        int finalScore = (int) ((score / (double) currentQuestions.size()) * 100);

        // Create a dialog to show the result with bird image
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);

        ImageView birdImageView = dialogView.findViewById(R.id.birdImageView);
        TextView scoreTextView = dialogView.findViewById(R.id.scoreTextView);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        TextView counterTextView = dialogView.findViewById(R.id.counterTextView);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

        // Set bird image based on score
        if (finalScore >= 100) {
            messageTextView.setText("Excellent your English is getting better!");
            counterTextView.setText("10/10");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 90) {
            messageTextView.setText("Good, but you can do it better!");
            counterTextView.setText("9/10");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 80) {
            messageTextView.setText("Good, but you can do it better!");
            counterTextView.setText("8/10");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore == 70) {
            messageTextView.setText("Good, but you can do it better!");
            counterTextView.setText("7/10");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 69) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("6/10");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 60) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("6/10");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("5/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 40) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("4/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 30) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("3/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 20) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("2/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        }else {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("1/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        }


        scoreTextView.setText("Score: " + finalScore + "%");

        final String finalCurrentTopic = selectedTopic;
        final int finalScoreForLambda = finalScore;

        // Configurar botón Continuar para Writing
        if (finalScore >= 70) {
            String nextWritingTopic = ProgressionHelper.getNextWritingTopic(finalCurrentTopic);

            if (nextWritingTopic != null) {
                // Hay siguiente tema en Writing
                btnContinue.setText("Continuar: " + nextWritingTopic);
                btnContinue.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, finalCurrentTopic, finalScoreForLambda);

                    // Ir al siguiente tema de Writing
                    Intent intent = new Intent(this, WritingActivity.class);
                    intent.putExtra("TOPIC", nextWritingTopic);
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                    finish();
                });
            } else {
                // Es el último tema de Writing, desbloquear PREPOSITIONS en Listening
                btnContinue.setText("🎯 ¡Desbloquear PREPOSITIONS en Listening!");
                btnContinue.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, finalCurrentTopic, finalScoreForLambda);

                    // Marcar PREPOSITIONS como desbloqueado en Listening
                    SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", true);
                    editor.apply();

                    // Ir al mapa de Listening
                    Intent intent = new Intent(this, MenuA1Activity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }
        } else {
            // No aprobó, mostrar botón de reintentar
            btnContinue.setText("Reintentar");
            btnContinue.setOnClickListener(v -> {
                // Reiniciar la misma actividad
                Intent intent = new Intent(this, WritingActivity.class);
                intent.putExtra("TOPIC", finalCurrentTopic);
                intent.putExtra("LEVEL", "A1.1");
                startActivity(intent);
                finish();
            });
        }

        // Configurar botón Ver detalles
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizHistoryActivity.class);
            intent.putExtra("SCORE", score);
            intent.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
            intent.putExtra("QUIZ_TYPE", "Writing");
            intent.putExtra("TOPIC", finalCurrentTopic);
            intent.putExtra("SHOW_ONLY_LAST_10", true);
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
            startActivity(intent);
            finish();
        });

        // Marcar tema como pasado si corresponde
        if (finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String key = "PASSED_WRITING_" + selectedTopic.toUpperCase().replace(" ", "_");
            editor.putBoolean(key, true);
            editor.apply();
        }

        builder.setView(dialogView);
        builder.setCancelable(false); // Evitar que se cierre sin seleccionar una opción

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onDestroy() {
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

        // Cerrar base de datos
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    private static class WritingQuestion {
        private final String text;
        private final String topic;
        private final String level;

        public WritingQuestion(String text, String topic, String level) {
            this.text = text;
            this.topic = topic;
            this.level = level;
        }

        public String getText() {
            return text;
        }

        public String getTopic() {
            return topic;
        }

        public String getLevel() {
            return level;
        }
    }

    private void returnToMenu() {
        // Return to MenuWritingActivity
        Intent intent = new Intent(this, MenuWritingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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
            displayQuestion();

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
            WritingQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            String question = currentQuestion.getText();

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
        // Para WritingActivity, mostrar información básica
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

    /**
     * Abre la ayuda contextual para el tema y nivel actuales
     */
    private void showHelp() {
        try {
            String topicForHelp = selectedTopic;
            if (currentQuestions != null && currentQuestionIndex >= 0 && currentQuestionIndex < currentQuestions.size()) {
                WritingQuestion q = currentQuestions.get(currentQuestionIndex);
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
}