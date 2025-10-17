package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speak.database.DatabaseHelper;
import com.example.speak.helpers.HelpModalHelper;
import com.example.speak.helpers.StarEarnedDialog;
import com.example.speak.helpers.StarProgressHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationReadingActivity extends AppCompatActivity {
    private static final String TAG = "TranslationReadingActivity";
    
    // Views
    private TextView questionNumberTextView;
    private TextView scoreTextView;
    private TextView englishTextView;
    private RecyclerView selectedWordsRecyclerView;
    private RecyclerView spanishWordsRecyclerView;
    private Button clearButton;
    private Button checkButton;
    private Button nextButton;
    
    // Return menu and topic/level views
    private LinearLayout returnContainer;
    private TextView topicTextView;
    private TextView levelTextView;
    private android.widget.ImageView wildcardButton;
    private android.widget.ImageView helpButton;
    
    // Data
    private List<TranslationQuestion> allQuestions;
    private List<TranslationQuestion> currentQuestions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private List<String> selectedWords;
    private List<Boolean> individualResults;
    
    // Adapters
    private SpanishWordsAdapter spanishWordsAdapter;
    private SelectedWordsAdapter selectedWordsAdapter;
    
    // Database and Firebase
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long userId;
    private boolean isOfflineMode = false;

    // MediaPlayer para sonidos de victoria y derrota
    private MediaPlayer victorySound;
    private MediaPlayer defeatSound;

    // Topic and level selected for this reading session
    private String selectedTopic = "ALPHABET"; // Default in case extra not provided
    private String selectedLevel = "A1.1"; // Default level
    private long sessionTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation_reading);

        // Retrieve selected topic and level from Intent
        String topicExtra = getIntent().getStringExtra("TOPIC");
        String levelExtra = getIntent().getStringExtra("LEVEL");
        if (topicExtra != null && !topicExtra.trim().isEmpty()) {
            selectedTopic = topicExtra.trim();
        }
        if (levelExtra != null && !levelExtra.trim().isEmpty()) {
            selectedLevel = levelExtra.trim();
        }

        // Initialize views
        initializeViews();

        // Initialize Firebase and Database
        initializeFirebaseAndDatabase();

        // Initialize data
        selectedWords = new ArrayList<>();
        individualResults = new ArrayList<>();
        
        // Setup RecyclerViews
        setupRecyclerViews();
        
        // Setup button listeners
        setupButtonListeners();
        
        // Load questions for the selected topic
        loadQuestionsFromAssets();
        
        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis();

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
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
    }

    private void initializeViews() {
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        englishTextView = findViewById(R.id.englishTextView);
        selectedWordsRecyclerView = findViewById(R.id.selectedWordsRecyclerView);
        spanishWordsRecyclerView = findViewById(R.id.spanishWordsRecyclerView);
        clearButton = findViewById(R.id.clearButton);
        checkButton = findViewById(R.id.checkButton);
        nextButton = findViewById(R.id.nextButton);
        
        // Initialize return menu and topic/level views
        returnContainer = findViewById(R.id.returnContainer);
        topicTextView = findViewById(R.id.topicTextView);
        levelTextView = findViewById(R.id.levelTextView);
        wildcardButton = findViewById(R.id.wildcardButton);
        helpButton = findViewById(R.id.helpButton);
        
        // Set topic and level text
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText(selectedTopic);
        }
        
        // Set up return button click listener
        returnContainer.setOnClickListener(v -> returnToMenu());

        // Integración del modal de comodines
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> {
                try {
                    com.example.speak.helpers.WildcardHelper wildcardHelper = new com.example.speak.helpers.WildcardHelper(
                        TranslationReadingActivity.this,
                        "READING",
                        selectedTopic
                    );
                    wildcardHelper.setCallbacks(new com.example.speak.helpers.WildcardHelper.WildcardCallbacks() {
                        @Override
                        public void onChangeQuestion() {
                            nextQuestion();
                        }

                        @Override
                        public void onShowContentImage() {
                            new android.app.AlertDialog.Builder(TranslationReadingActivity.this)
                                .setTitle("Imagen de Contenido")
                                .setMessage("Aquí se mostraría una imagen relacionada con: " + selectedTopic)
                                .setPositiveButton("Entendido", null)
                                .show();
                        }

                        @Override
                        public void onShowInstructorVideo() {
                            try {
                                new com.example.speak.helpers.VideoHelper(TranslationReadingActivity.this)
                                    .showInstructorVideo(selectedTopic);
                            } catch (Exception ex) {
                                android.util.Log.e(TAG, "Error mostrando video del instructor: " + ex.getMessage());
                                android.widget.Toast.makeText(TranslationReadingActivity.this, "No se pudo reproducir el video", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onShowFiftyFifty() {
                            android.widget.Toast.makeText(TranslationReadingActivity.this, "50/50 no aplica en esta vista", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowCreativeHelp() {
                            android.widget.Toast.makeText(TranslationReadingActivity.this, "Ayuda creativa no disponible aquí", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowWildcardInfo() {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(TranslationReadingActivity.this);
                            builder.setTitle("🎯 Uso de Comodines");
                            builder.setMessage("• Tema actual: " + selectedTopic + "\n• Nivel: " + selectedLevel + "\n• Comodines disponibles: N/A\n\nℹ️ Los comodines te ayudan sin afectar tu evaluación final.");
                            builder.setPositiveButton("Entendido", null);
                            builder.show();
                        }
                    });
                    wildcardHelper.showWildcardMenu();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error mostrando comodines: " + e.getMessage());
                }
            });
        }

        // Integración del botón de ayuda
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                try {
                    String topicForHelp = selectedTopic;
                    if (currentQuestions != null && currentQuestionIndex >= 0 && currentQuestionIndex < currentQuestions.size()) {
                        TranslationQuestion q = currentQuestions.get(currentQuestionIndex);
                        if (q != null && q.getTopic() != null && !q.getTopic().isEmpty()) {
                            topicForHelp = q.getTopic();
                        }
                    }
                    HelpModalHelper.show(TranslationReadingActivity.this, topicForHelp, selectedLevel);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error abriendo modal de ayuda: " + e.getMessage());
                    android.widget.Toast.makeText(TranslationReadingActivity.this, "No se pudo abrir la ayuda", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initializeFirebaseAndDatabase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new DatabaseHelper(this);
        
        // Check network status
        isOfflineMode = !isNetworkAvailable();
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1);
    }

    private boolean isNetworkAvailable() {
        // Implementar verificación de red
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    private void setupRecyclerViews() {
        // Setup selected words RecyclerView (horizontal flow)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedWordsRecyclerView.setLayoutManager(linearLayoutManager);
        selectedWordsAdapter = new SelectedWordsAdapter();
        selectedWordsRecyclerView.setAdapter(selectedWordsAdapter);
        
        // Setup Spanish words RecyclerView (grid)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        spanishWordsRecyclerView.setLayoutManager(gridLayoutManager);
        spanishWordsAdapter = new SpanishWordsAdapter();
        spanishWordsRecyclerView.setAdapter(spanishWordsAdapter);
    }

    private void setupButtonListeners() {
        clearButton.setOnClickListener(v -> clearSelection());
        checkButton.setOnClickListener(v -> checkAnswer());
        nextButton.setOnClickListener(v -> nextQuestion());
    }

    private void loadQuestionsFromAssets() {
        allQuestions = new ArrayList<>();
        try {
            // Load questions from reading_translation_A1.1.txt file
            InputStream is = getAssets().open("reading_translation_A1.1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            String currentTopic = "";
            String currentLevel = "";
            String currentQuestion = "";
            String currentAnswer = "";
            String currentOptions = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("Topic:")) {
                    currentTopic = line.substring(6).trim();
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim();
                } else if (line.startsWith("Q:")) {
                    currentQuestion = line.substring(2).trim();
                } else if (line.startsWith("A:")) {
                    currentAnswer = line.substring(2).trim();
                } else if (line.startsWith("O:")) {
                    currentOptions = line.substring(2).trim();
                    
                    // Create question when we have all data
                    if (!currentQuestion.isEmpty() && !currentAnswer.isEmpty() && !currentOptions.isEmpty()) {
                        TranslationQuestion question = new TranslationQuestion(currentQuestion, currentTopic, currentLevel);
                        
                        // Parse Spanish words (options)
                        String[] spanishWordsArray = currentOptions.split("\\s*\\|\\s*");
                        List<String> spanishWords = Arrays.asList(spanishWordsArray);
                        question.setSpanishWords(spanishWords);
                        
                        // Parse correct translation
                        String[] correctTranslationArray = currentAnswer.split("\\s+");
                        List<String> correctTranslation = Arrays.asList(correctTranslationArray);
                        question.setCorrectTranslation(correctTranslation);
                        
                        allQuestions.add(question);
                        
                        // Reset for next question
                        currentQuestion = "";
                        currentAnswer = "";
                        currentOptions = "";
                    }
                }
            }
            reader.close();
            is.close();
            
            // --- Filtrar preguntas por el tópico seleccionado ---
            List<TranslationQuestion> filteredQuestions = new ArrayList<>();
            for (TranslationQuestion q : allQuestions) {
                if (q.getTopic().equalsIgnoreCase(selectedTopic)) {
                    filteredQuestions.add(q);
                }
            }

            // Si no se encontraron preguntas para el tópico, usar todas como respaldo
            List<TranslationQuestion> questionsPool = filteredQuestions.isEmpty() ? allQuestions : filteredQuestions;

            // Mezclar preguntas y tomar las primeras 10
            Collections.shuffle(questionsPool);
            currentQuestions = new ArrayList<>(questionsPool.subList(0, Math.min(10, questionsPool.size())));
            
            // Display the first question
            displayCurrentQuestion();
            
            Log.d(TAG, "Loaded " + allQuestions.size() + " questions from file");
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading questions from file", Toast.LENGTH_SHORT).show();
            // Fallback to sample questions if file loading fails
            createSampleQuestions();
            Collections.shuffle(allQuestions);
            currentQuestions = new ArrayList<>(allQuestions.subList(0, Math.min(10, allQuestions.size())));
            displayCurrentQuestion();
        }
    }

    private void createSampleQuestions() {
        // Crear preguntas de ejemplo
        List<TranslationQuestion> sampleQuestions = Arrays.asList(
            createQuestion("Hello, my name is John", 
                          Arrays.asList("Hola", "mi", "nombre", "es", "Juan", "soy", "llamar"),
                          Arrays.asList("Hola", "mi", "nombre", "es", "Juan")),
            
            createQuestion("I am a student", 
                          Arrays.asList("Yo", "soy", "un", "estudiante", "profesor", "trabajo"),
                          Arrays.asList("Yo", "soy", "un", "estudiante")),
            
            createQuestion("She is very happy", 
                          Arrays.asList("Ella", "es", "muy", "feliz", "triste", "contenta"),
                          Arrays.asList("Ella", "es", "muy", "feliz")),
            
            createQuestion("We have two cats", 
                          Arrays.asList("Nosotros", "tenemos", "dos", "gatos", "perros", "tres"),
                          Arrays.asList("Nosotros", "tenemos", "dos", "gatos")),
            
            createQuestion("The book is red", 
                          Arrays.asList("El", "libro", "es", "rojo", "azul", "verde", "mesa"),
                          Arrays.asList("El", "libro", "es", "rojo")),
            
            createQuestion("I like to eat pizza", 
                          Arrays.asList("Me", "gusta", "comer", "pizza", "hamburguesa", "beber"),
                          Arrays.asList("Me", "gusta", "comer", "pizza")),
            
            createQuestion("They are my friends", 
                          Arrays.asList("Ellos", "son", "mis", "amigos", "hermanos", "padres"),
                          Arrays.asList("Ellos", "son", "mis", "amigos")),
            
            createQuestion("The car is blue", 
                          Arrays.asList("El", "carro", "es", "azul", "rojo", "negro", "bicicleta"),
                          Arrays.asList("El", "carro", "es", "azul")),
            
            createQuestion("I go to school", 
                          Arrays.asList("Yo", "voy", "a", "la", "escuela", "casa", "trabajo"),
                          Arrays.asList("Yo", "voy", "a", "la", "escuela")),
            
            createQuestion("She has a dog", 
                          Arrays.asList("Ella", "tiene", "un", "perro", "gato", "pájaro"),
                          Arrays.asList("Ella", "tiene", "un", "perro"))
        );
        
        allQuestions.addAll(sampleQuestions);
    }

    private TranslationQuestion createQuestion(String englishText, List<String> spanishWords, List<String> correctTranslation) {
        TranslationQuestion question = new TranslationQuestion(englishText, "READING", "A1.1");
        question.setSpanishWords(new ArrayList<>(spanishWords));
        question.setCorrectTranslation(new ArrayList<>(correctTranslation));
        return question;
    }

    private void displayCurrentQuestion() {
        if (currentQuestionIndex >= currentQuestions.size()) {
            showResults();
            return;
        }

        TranslationQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);

        // Update UI
        questionNumberTextView.setVisibility(View.GONE); // Ocultar el contador
        scoreTextView.setText("Score: " + score);
        englishTextView.setText(currentQuestion.getEnglishText());
        
        // Clear previous selection
        selectedWords.clear();
        selectedWordsAdapter.notifyDataSetChanged();
        
        // Setup Spanish words
        spanishWordsAdapter.setWords(currentQuestion.getSpanishWords());
        
        // Reset buttons
        checkButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.GONE);
    }

    private void clearSelection() {
        selectedWords.clear();
        selectedWordsAdapter.notifyDataSetChanged();
        spanishWordsAdapter.resetUsedWords();
    }

    private void checkAnswer() {
        if (selectedWords.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una palabra", Toast.LENGTH_SHORT).show();
            return;
        }
        
        TranslationQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
        List<String> correctAnswer = currentQuestion.getCorrectTranslation();
        
        // Calculate accuracy
        double accuracy = calculateAccuracy(selectedWords, correctAnswer);
        boolean isCorrect = accuracy >= 0.8; // 80% threshold
        
        // Save individual result
        individualResults.add(isCorrect);
        
        if (isCorrect) {
            score++;
            Toast.makeText(this, "¡Correcto! Precisión: " + String.format("%.1f%%", accuracy * 100), 
                Toast.LENGTH_SHORT).show();
            // Reproducir sonido de victoria
            playVictorySound();
            Log.d(TAG, "🎉 Sonido de victoria para pregunta individual (accuracy: " + (accuracy * 100) + "%)");
        } else {
            Toast.makeText(this, "Incorrecto. Respuesta correcta: " + String.join(" ", correctAnswer), 
                Toast.LENGTH_LONG).show();
            // Reproducir sonido de derrota
            playDefeatSound();
            Log.d(TAG, "😔 Sonido de derrota para pregunta individual (accuracy: " + (accuracy * 100) + "%)");
        }
        
        // Save answer to database
        saveAnswerToDatabase(currentQuestion, String.join(" ", selectedWords), isCorrect, accuracy);
        
        // Show next button and hide action buttons
        checkButton.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
    }

    private double calculateAccuracy(List<String> userAnswer, List<String> correctAnswer) {
        if (correctAnswer.isEmpty()) return 0.0;
        
        int correctWords = 0;
        int totalWords = correctAnswer.size();
        
        // Check for exact matches in order
        for (int i = 0; i < Math.min(userAnswer.size(), correctAnswer.size()); i++) {
            if (userAnswer.get(i).equalsIgnoreCase(correctAnswer.get(i))) {
                correctWords++;
            }
        }
        
        return (double) correctWords / totalWords;
    }

    private void nextQuestion() {
        currentQuestionIndex++;
        displayCurrentQuestion();
    }

    private void showResults() {
        double percentage = (double) score / currentQuestions.size() * 100;
        int finalScore = (int) Math.round(percentage);
        
        Log.d(TAG, "=== SHOWING RESULTS ===");
        Log.d(TAG, "Session timestamp: " + sessionTimestamp);
        Log.d(TAG, "Final score: " + finalScore + "%");
        Log.d(TAG, "Total questions: " + currentQuestions.size());
        
        // Create array of question results using the actual individual results
        boolean[] questionResults = new boolean[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            if (i < individualResults.size()) {
                questionResults[i] = individualResults.get(i);
            } else {
                questionResults[i] = false; // Default to false if not answered
            }
        }
        
        // Create array of question texts
        String[] questions = new String[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questions[i] = currentQuestions.get(i).getEnglishText();
        }
        
        // Save final results to database
        saveFinalResults(finalScore, percentage);
        
        // Mark topic as passed if score >= 70%
        if (finalScore >= 70) {
            markTopicAsPassed();
            // Sumar 10 puntos de estrella y mostrar modal de estrella (consistente con otras actividades)
            StarProgressHelper.addSessionPoints(this, 10);
            new Handler().postDelayed(() -> {
                try {
                    StarEarnedDialog.show(TranslationReadingActivity.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.getMessage());
                }
            }, 200);
        }
        
        // Mostrar resultados en diálogo con diseño dialog_quiz_result
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        android.widget.ImageView birdImageViewDialog = dialogView.findViewById(R.id.birdImageView);
        android.widget.TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        android.widget.TextView counterTextView = dialogView.findViewById(R.id.counterTextView);
        android.widget.TextView scoreTextView = dialogView.findViewById(R.id.scoreTextView);
        android.widget.Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        android.widget.TextView btnReintentar = dialogView.findViewById(R.id.btnReintentar);
        android.widget.LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

        // Imagen y mensaje según puntaje
        if (finalScore >= 90) {
            messageTextView.setText("Excellent your English is getting better!");
            birdImageViewDialog.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 70) {
            messageTextView.setText("Good, but you can do it better!");
            birdImageViewDialog.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!");
            birdImageViewDialog.setImageResource(R.drawable.crab_test);
        } else {
            messageTextView.setText("You should practice more!");
            birdImageViewDialog.setImageResource(R.drawable.crab_bad);
        }

        counterTextView.setText(score + "/" + currentQuestions.size());
        scoreTextView.setText("Score: " + finalScore + "%");

        // Continuar: progresión de Reading
        btnContinue.setOnClickListener(v -> {
            String nextReadingTopic = com.example.speak.ProgressionHelper.getNextReadingTopic(selectedTopic);
            if (nextReadingTopic != null) {
                Class<?> nextActivityClass = com.example.speak.ProgressionHelper.getReadingActivityClass(nextReadingTopic);
                Intent next = new Intent(this, nextActivityClass);
                next.putExtra("TOPIC", nextReadingTopic);
                next.putExtra("LEVEL", selectedLevel);
                startActivity(next);
                finish();
            } else {
                Intent back = new Intent(this, MenuReadingActivity.class);
                back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(back);
                finish();
            }
        });

        // Reintentar: reiniciar esta actividad
        btnReintentar.setText("Try again");
        btnReintentar.setOnClickListener(v -> {
            Intent retry = new Intent(this, TranslationReadingActivity.class);
            retry.putExtra("TOPIC", selectedTopic);
            retry.putExtra("LEVEL", selectedLevel);
            startActivity(retry);
            finish();
        });

        // Ver resumen: abrir directamente la tabla de resultados (historial) de esta sesión
        btnViewDetails.setOnClickListener(v -> {
            Intent details = new Intent(this, ReadingHistoryActivity.class);
            details.putExtra("TOPIC", selectedTopic);
            details.putExtra("LEVEL", selectedLevel);
            details.putExtra("FINAL_SCORE", finalScore);
            details.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
            details.putExtra("CORRECT_ANSWERS", score);
            details.putExtra("SCORE", score);
            details.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
            details.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
            startActivity(details);
        });
    }

    private void saveAnswerToDatabase(TranslationQuestion question, String userAnswer, boolean isCorrect, double accuracy) {
        try {
            // Save to local database with session timestamp
            if (dbHelper != null && userId != -1) {
                dbHelper.saveReadingResult(
                    userId,
                    question.getEnglishText(),
                    question.getCorrectTranslationAsString(),
                    userAnswer,
                    isCorrect,
                    question.getTopic(),
                    question.getLevel(),
                    sessionTimestamp
                );
                
                Log.d(TAG, "Reading result saved to database - Question: " + question.getEnglishText() + 
                    ", Correct: " + isCorrect + ", Accuracy: " + accuracy + ", SessionTimestamp: " + sessionTimestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving reading result to database", e);
        }
    }

    private void saveFinalResults(int finalScore, double percentage) {
        try {
            // Save overall session result with session timestamp
            if (dbHelper != null && userId != -1) {
                // Save a summary result
                dbHelper.saveReadingResult(
                    userId,
                    "Reading Session Completed",
                    "Score: " + score + "/" + currentQuestions.size(),
                    "Percentage: " + String.format("%.1f%%", percentage),
                    finalScore >= 70,
                    selectedTopic,
                    "A1.1",
                    sessionTimestamp
                );
                
                Log.d(TAG, "Final reading session result saved - Score: " + finalScore + "%, SessionTimestamp: " + sessionTimestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving final results", e);
        }
    }

    private void markTopicAsPassed() {
        try {
            // Mark topic as passed in SharedPreferences - usar ProgressPrefs para consistencia
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            // Generar clave dinámica basada en el tópico
            String key = "PASSED_READING_" + selectedTopic.toUpperCase().replace(" ", "_");
            editor.putBoolean(key, true);
            editor.apply();
            
            // Also mark in database
            if (dbHelper != null && userId != -1) {
                dbHelper.markTopicAsPassed(userId, selectedTopic, "A1.1");
            }
            
            Log.d(TAG, "Reading topic " + selectedTopic + " marked as passed");
            Toast.makeText(this, "🎉 ¡Has completado Reading " + selectedTopic + " con éxito!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error marking topic as passed", e);
        }
    }

    // Adapter for Spanish words (selectable)
    private class SpanishWordsAdapter extends RecyclerView.Adapter<SpanishWordsAdapter.SpanishWordViewHolder> {
        private List<String> words = new ArrayList<>();
        private List<Boolean> usedWords = new ArrayList<>();

        public void setWords(List<String> words) {
            this.words = new ArrayList<>(words);
            this.usedWords = new ArrayList<>(Collections.nCopies(words.size(), false));
            notifyDataSetChanged();
        }

        public void resetUsedWords() {
            for (int i = 0; i < usedWords.size(); i++) {
                usedWords.set(i, false);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SpanishWordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_spanish_word, parent, false);
            return new SpanishWordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SpanishWordViewHolder holder, int position) {
            String word = words.get(position);
            boolean isUsed = usedWords.get(position);
            
            holder.wordButton.setText(word);
            holder.wordButton.setEnabled(!isUsed);
            holder.wordButton.setAlpha(isUsed ? 0.5f : 1.0f);
            
            holder.wordButton.setOnClickListener(v -> {
                selectedWords.add(word);
                usedWords.set(position, true);
                notifyItemChanged(position);
                selectedWordsAdapter.notifyItemInserted(selectedWords.size() - 1);
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class SpanishWordViewHolder extends RecyclerView.ViewHolder {
            Button wordButton;

            SpanishWordViewHolder(@NonNull View itemView) {
                super(itemView);
                wordButton = itemView.findViewById(R.id.spanishWordButton);
            }
        }
    }

    // Adapter for selected words (removable)
    private class SelectedWordsAdapter extends RecyclerView.Adapter<SelectedWordsAdapter.SelectedWordViewHolder> {

        @NonNull
        @Override
        public SelectedWordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_selected_word, parent, false);
            return new SelectedWordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectedWordViewHolder holder, int position) {
            String word = selectedWords.get(position);
            holder.wordTextView.setText(word);
            
            holder.wordTextView.setOnClickListener(v -> {
                // Remove word from selection
                String removedWord = selectedWords.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, selectedWords.size());
                
                // Make the word available again in Spanish words
                spanishWordsAdapter.resetUsedWords();
                for (String selectedWord : selectedWords) {
                    int index = spanishWordsAdapter.words.indexOf(selectedWord);
                    if (index != -1) {
                        spanishWordsAdapter.usedWords.set(index, true);
                    }
                }
                spanishWordsAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return selectedWords.size();
        }

        class SelectedWordViewHolder extends RecyclerView.ViewHolder {
            TextView wordTextView;

            SelectedWordViewHolder(@NonNull View itemView) {
                super(itemView);
                wordTextView = itemView.findViewById(R.id.selectedWordTextView);
            }
        }
    }

    // En TranslationReadingActivity.java o en una clase helper
    private String getNextReadingTopic(String currentTopic) {
        if ("ALPHABET".equals(currentTopic)) {
            return "NUMBERS";
        } else if ("NUMBERS".equals(currentTopic)) {
            return "COLORS";
        } else if ("COLORS".equals(currentTopic)) {
            return "PERSONAL PRONOUNS";
        } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
            return "POSSESSIVE ADJECTIVES";
        }
        // POSSESSIVE ADJECTIVES es el último tema de Reading
        return null;
    }
    
    private void returnToMenu() {
        // Return to MenuReadingActivity
        Intent intent = new Intent(this, MenuReadingActivity.class);
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
} 