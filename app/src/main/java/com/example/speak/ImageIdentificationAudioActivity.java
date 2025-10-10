package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.AssetManager;
import android.graphics.drawable.PictureDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.example.speak.database.DatabaseHelper;
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

public class ImageIdentificationAudioActivity extends AppCompatActivity {
    private static final String TAG = "ImageIdentificationAudioActivity";

    // UI Elements
    private TextView questionNumberTextView;
    private TextView levelTextView;
    private TextView topicTextView;
    private TextView scoreTextView;
    private TextView hiddenWordTextView;
    private Button nextButton;

    // Option buttons (ImageButtons instead of regular Buttons)
    private ImageButton option1Button;
    private ImageButton option2Button;
    private ImageButton option3Button;
    private ImageButton option4Button;

    // Bird image for feedback
    private ImageView birdImageView;

    // ============================================================
    // NUEVO: Componente reutilizable de audio
    // ============================================================
    private com.example.speak.components.ReusableAudioPlayerCard reusableAudioCard;
    private boolean lastReusablePlaying = false;
    private Handler reusableMonitorHandler = new Handler(Looper.getMainLooper());
    private Runnable reusableMonitorRunnable;

    // MediaPlayer para sonidos de feedback
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    // Quiz data
    private List<ImageQuestion> allQuestions;
    private List<ImageQuestion> currentQuestions;
    private int currentQuestionIndex = 0;
    private int score = 0;

    // Database and Firebase
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long userId;
    private boolean isOfflineMode = false;
    private long sessionTimestamp;

    // Topic and level
    private String selectedTopic;
    private String selectedLevel;

    // Return menu
    private LinearLayout returnContainer;
    private ImageView wildcardButton;
    private ImageView helpButton;

    // Variable para evitar repetir las mismas imágenes en preguntas consecutivas
    private List<String> recentlyUsedImages = new ArrayList<>();
    private static final int MAX_RECENT_IMAGES = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_identification_audio);

        Log.d(TAG, "ImageIdentificationAudioActivity onCreate started");

        // Get intent data FIRST
        Intent intent = getIntent();
        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");

        Log.d(TAG, "Intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel);

        // Validate intent data IMMEDIATELY
        if (selectedTopic == null || selectedLevel == null) {
            Log.e(TAG, "Missing intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel);
            Toast.makeText(this, "Error: Datos de tema o nivel no encontrados.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views FIRST
        initializeViews();

        // Display topic and level in the interface AFTER views are initialized
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("Topic: " + selectedTopic);
        }

        // Initialize sound players
        initializeSoundPlayers();
        Log.d(TAG, "Sound players initialized");

        // Check network status
        isOfflineMode = !isNetworkAvailable();
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode);

        // Initialize Firebase and Database
        initializeFirebaseAndDatabase();

        // ============================================================
        // NUEVO: Configurar componente reutilizable de audio
        // ============================================================
        setupReusableAudioCard();
        startReusablePlaybackMonitor();

        // Setup button listeners
        setupButtonListeners();

        // Setup option button listeners
        setupOptionButtonListeners();

        // Load questions
        loadQuestionsFromFile(selectedTopic, selectedLevel);

        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp);
    }

    private void initializeViews() {
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        levelTextView = findViewById(R.id.levelTextView);
        topicTextView = findViewById(R.id.topicTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        hiddenWordTextView = findViewById(R.id.hiddenWordTextView);
        nextButton = findViewById(R.id.nextButton);

        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);

        // Initialize bird image
        birdImageView = findViewById(R.id.birdImageView);

        // ============================================================
        // NUEVO: Inicializar componente reutilizable de audio
        // ============================================================
        reusableAudioCard = findViewById(R.id.reusableAudioCard);

        returnContainer = findViewById(R.id.returnContainer);
        helpButton = findViewById(R.id.helpButton);

        // Set up return button click listener
        returnContainer.setOnClickListener(v -> returnToMenu());

        // Wildcard modal integration
        wildcardButton = findViewById(R.id.wildcardButton);
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> {
                try {
                    com.example.speak.helpers.WildcardHelper wildcardHelper = new com.example.speak.helpers.WildcardHelper(
                            ImageIdentificationAudioActivity.this,
                            "IMAGE_AUDIO",
                            selectedTopic
                    );
                    wildcardHelper.setCallbacks(new com.example.speak.helpers.WildcardHelper.WildcardCallbacks() {
                        @Override
                        public void onChangeQuestion() {
                            currentQuestionIndex = Math.min(currentQuestionIndex + 1, currentQuestions.size());
                            displayQuestion();
                        }

                        @Override
                        public void onShowContentImage() {
                            Toast.makeText(ImageIdentificationAudioActivity.this, "Mostrando imagen de contenido", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowInstructorVideo() {
                            try {
                                new com.example.speak.helpers.VideoHelper(ImageIdentificationAudioActivity.this)
                                        .showInstructorVideo(selectedTopic);
                            } catch (Exception ex) {
                                Log.e(TAG, "Error mostrando video del instructor: " + ex.getMessage());
                                Toast.makeText(ImageIdentificationAudioActivity.this, "No se pudo reproducir el video", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onShowFiftyFifty() {
                            Toast.makeText(ImageIdentificationAudioActivity.this, "50/50 no aplica en esta vista", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowCreativeHelp() {
                            Toast.makeText(ImageIdentificationAudioActivity.this, "Ayuda creativa no disponible aquí", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowWildcardInfo() {
                            Toast.makeText(ImageIdentificationAudioActivity.this, "Información de comodines", Toast.LENGTH_SHORT).show();
                        }
                    });
                    wildcardHelper.showWildcardMenu();
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando comodines: " + e.getMessage());
                }
            });
        }

        // Help modal integration
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                try {
                    Intent helpIntent = new Intent(ImageIdentificationAudioActivity.this, HelpActivity.class);
                    helpIntent.putExtra("topic", selectedTopic);
                    helpIntent.putExtra("level", selectedLevel);
                    startActivity(helpIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error abriendo HelpActivity: " + e.getMessage());
                    Toast.makeText(ImageIdentificationAudioActivity.this, "No se pudo abrir la ayuda", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Log.d(TAG, "Views initialized");
    }

    // ============================================================
    // NUEVO: Configuración del componente reutilizable de audio
    // ============================================================
    private void setupReusableAudioCard() {
        try {
            if (reusableAudioCard != null) {
                // Obtener el texto de la pregunta actual
                String audioText = "";
                if (currentQuestions != null && !currentQuestions.isEmpty() && currentQuestionIndex < currentQuestions.size()) {
                    ImageQuestion q = currentQuestions.get(currentQuestionIndex);
                    audioText = q.getCorrectAnswer();
                }

                // ⚠️ IMPORTANTE: Este activity usa TTS, NO archivos MP3
                // Configurar con carpeta vacía para indicar que se usará TTS
                reusableAudioCard.configure("", audioText);

                // Forzar modo INGLÉS (TTS) - No hay archivos MP3 disponibles
                reusableAudioCard.setEnglishMode();

                Log.d(TAG, "ReusableAudioCard configurado en modo TTS con texto: " + audioText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up reusable audio card: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // NUEVO: Monitor de reproducción para habilitar botones
    // ============================================================
    private void startReusablePlaybackMonitor() {
        if (reusableAudioCard == null) return;
        reusableMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isPlayingReusable = reusableAudioCard.isPlaying();
                    if (isPlayingReusable != lastReusablePlaying) {
                        lastReusablePlaying = isPlayingReusable;
                        if (isPlayingReusable) {
                            // Habilitar botones de opciones cuando comienza la reproducción
                            enableOptionButtons();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error monitoring reusable playback: " + e.getMessage());
                } finally {
                    reusableMonitorHandler.postDelayed(this, 200);
                }
            }
        };
        reusableMonitorHandler.post(reusableMonitorRunnable);
    }

    private void enableOptionButtons() {
        option1Button.setEnabled(true);
        option2Button.setEnabled(true);
        option3Button.setEnabled(true);
        option4Button.setEnabled(true);
        Log.d(TAG, "Botones de opciones habilitados después de reproducir audio");
    }

    private void initializeFirebaseAndDatabase() {
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long initialUserId = prefs.getLong("userId", 1);
        String userEmail = prefs.getString("userEmail", null);

        Log.d(TAG, "Initial user ID from prefs: " + initialUserId);
        Log.d(TAG, "User email from prefs: " + userEmail);

        if (isOfflineMode || mAuth.getCurrentUser() == null) {
            userId = initialUserId;
        } else {
            userId = initialUserId;
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode);

        if (!isOfflineMode) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }

        Log.d(TAG, "Firebase and Database initialized");
    }

    private void setupButtonListeners() {
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentQuestionIndex++;
                displayQuestion();
            }
        });

        Log.d(TAG, "Button listeners setup");
    }

    private void setupOptionButtonListeners() {
        option1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(1);
            }
        });

        option2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(2);
            }
        });

        option3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(3);
            }
        });

        option4Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(4);
            }
        });
    }

    private boolean isNetworkAvailable() {
        return true; // Simplified for now
    }

    private void loadQuestionsFromFile(String topic, String level) {
        if (topic == null || level == null) {
            Log.e(TAG, "Topic or level is null - Topic: " + topic + ", Level: " + level);
            Toast.makeText(this, "Error: Tema o nivel no especificado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        allQuestions = new ArrayList<>();
        currentQuestions = new ArrayList<>();

        try {
            AssetManager assetManager = getAssets();
            String fileName = "image_questions_" + level.toLowerCase() + ".txt";

            Log.d(TAG, "Loading questions from file: " + fileName);

            InputStream inputStream = assetManager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length >= 9) {
                    String questionText = parts[0];
                    String correctAnswer = parts[1];
                    String[] options = {parts[2], parts[3], parts[4], parts[5]};
                    String questionTopic = parts[6];
                    String questionLevel = parts[7];
                    String imageResource = parts[8];

                    if (questionTopic.equals(topic) && questionLevel.equals(level)) {
                        ImageQuestion question = new ImageQuestion(
                                questionText, correctAnswer, options, questionTopic, questionLevel, imageResource
                        );
                        allQuestions.add(question);
                        Log.d(TAG, "Added question: " + questionText + " for topic: " + topic);
                    }
                }
            }

            reader.close();
            inputStream.close();

            Log.d(TAG, "Loaded " + allQuestions.size() + " questions for topic: " + topic);

            if (allQuestions.isEmpty()) {
                Log.w(TAG, "No questions found for topic: " + topic + " and level: " + level);
                Toast.makeText(this, "No se encontraron preguntas para este tema.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Collections.shuffle(allQuestions);
            currentQuestions = new ArrayList<>(allQuestions.subList(0, Math.min(10, allQuestions.size())));

            displayQuestion();

        } catch (IOException e) {
            Log.e(TAG, "Error loading questions from file", e);
            Toast.makeText(this, "Error cargando las preguntas.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= currentQuestions.size()) {
            showQuizResults();
            return;
        }

        ImageQuestion question = currentQuestions.get(currentQuestionIndex);

        // Update question number
        questionNumberTextView.setText((currentQuestionIndex + 1) + "/" + currentQuestions.size());

        // Update score
        scoreTextView.setText("Score: " + score);

        // Hide the word initially
        hiddenWordTextView.setText("?????");

        // ============================================================
        // NUEVO: Actualizar el componente de audio con la nueva pregunta
        // ============================================================
        if (reusableAudioCard != null) {
            try {
                reusableAudioCard.resetForNewQuestion();
                reusableAudioCard.setText(question.getCorrectAnswer());
                // Asegurarse de que esté en modo TTS (inglés)
                reusableAudioCard.setEnglishMode();
                Log.d(TAG, "Audio card actualizado con texto TTS: " + question.getCorrectAnswer());
            } catch (Exception e) {
                Log.e(TAG, "Error syncing reusable audio card text: " + e.getMessage());
            }
        }

        // Load images for options
        loadOptionImages(question);

        // Reset button states
        resetButtonStates();

        // Deshabilitar botones hasta que se reproduzca el audio
        disableOptionButtons();

        // Hide next button initially
        nextButton.setVisibility(View.GONE);

        Log.d(TAG, "Displaying question " + (currentQuestionIndex + 1) + ": " + question.getQuestion());
    }

    private void disableOptionButtons() {
        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);
    }

    private void loadOptionImages(ImageQuestion question) {
        try {
            List<String> availableImages = getAvailableImagesFromAssets();

            List<String> optionImages = new ArrayList<>();
            optionImages.add(question.getImageResourceName());

            List<String> remainingImages = new ArrayList<>();
            for (String img : availableImages) {
                if (!img.equals(question.getImageResourceName()) &&
                        !recentlyUsedImages.contains(img)) {
                    remainingImages.add(img);
                }
            }

            if (remainingImages.size() < 3) {
                remainingImages.clear();
                for (String img : availableImages) {
                    if (!img.equals(question.getImageResourceName())) {
                        remainingImages.add(img);
                    }
                }
            }

            Collections.shuffle(remainingImages);
            for (int i = 0; i < Math.min(3, remainingImages.size()); i++) {
                optionImages.add(remainingImages.get(i));
            }

            Collections.shuffle(optionImages);

            setImageButton(option1Button, optionImages.get(0));
            setImageButton(option2Button, optionImages.get(1));
            setImageButton(option3Button, optionImages.get(2));
            setImageButton(option4Button, optionImages.get(3));

            question.setCorrectImageResource(question.getImageResourceName());

            updateRecentlyUsedImages(optionImages);

            Log.d(TAG, "Loaded option images: " + optionImages.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error loading option images", e);
        }
    }

    private void updateRecentlyUsedImages(List<String> usedImages) {
        for (String img : usedImages) {
            if (!recentlyUsedImages.contains(img)) {
                recentlyUsedImages.add(0, img);
            }
        }

        while (recentlyUsedImages.size() > MAX_RECENT_IMAGES) {
            recentlyUsedImages.remove(recentlyUsedImages.size() - 1);
        }
    }

    private List<String> getAvailableImagesFromAssets() {
        List<String> availableImages = new ArrayList<>();
        try {
            AssetManager assetManager = getAssets();
            String[] files = assetManager.list("");

            if (files != null) {
                for (String file : files) {
                    if (file.toLowerCase().endsWith(".svg")) {
                        availableImages.add(file);
                    }
                }
            }

            String[] drawableImages = {"ic_cat"};

            for (String img : drawableImages) {
                availableImages.add(img);
            }

            Log.d(TAG, "Found " + availableImages.size() + " available images");

        } catch (IOException e) {
            Log.e(TAG, "Error reading assets directory", e);
            String[] fallbackImages = {"ic_cat"};
            for (String img : fallbackImages) {
                availableImages.add(img);
            }
        }

        return availableImages;
    }

    private void setImageButton(ImageButton button, String imageResourceName) {
        try {
            if (imageResourceName.toLowerCase().endsWith(".svg")) {
                if (setSVGImageButtonFromAssets(button, imageResourceName)) {
                    return;
                }
            }

            if (setRegularImageButton(button, imageResourceName)) {
                return;
            }

            button.setImageResource(R.drawable.ic_quiz);

        } catch (Exception e) {
            Log.e(TAG, "Error setting image: " + imageResourceName, e);
            button.setImageResource(R.drawable.ic_quiz);
        }
    }

    private boolean setSVGImageButtonFromAssets(ImageButton button, String svgFileName) {
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(svgFileName);

            SVG svg = SVG.getFromInputStream(inputStream);
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            button.setImageDrawable(drawable);
            button.setTag(svgFileName);

            inputStream.close();
            Log.d(TAG, "Set SVG from assets: " + svgFileName);
            return true;

        } catch (IOException e) {
            Log.w(TAG, "SVG not found in assets: " + svgFileName);
            return false;
        } catch (SVGParseException e) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e);
            return false;
        }
    }

    private boolean setRegularImageButton(ImageButton button, String imageResourceName) {
        try {
            int resourceId = getResources().getIdentifier(
                    imageResourceName, "drawable", getPackageName()
            );

            if (resourceId != 0) {
                button.setImageResource(resourceId);
                button.setTag(imageResourceName);
                Log.d(TAG, "Set regular image: " + imageResourceName);
                return true;
            } else {
                Log.w(TAG, "Image resource not found: " + imageResourceName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting regular image: " + imageResourceName, e);
            return false;
        }
    }

    private void resetButtonStates() {
        option1Button.setEnabled(true);
        option2Button.setEnabled(true);
        option3Button.setEnabled(true);
        option4Button.setEnabled(true);

        option1Button.setBackgroundTintList(getColorStateList(R.color.white));
        option2Button.setBackgroundTintList(getColorStateList(R.color.white));
        option3Button.setBackgroundTintList(getColorStateList(R.color.white));
        option4Button.setBackgroundTintList(getColorStateList(R.color.white));

        if (birdImageView != null) {
            birdImageView.setImageResource(R.drawable.crab_test);
        }
    }

    private void checkAnswer(int selectedOption) {
        if (currentQuestionIndex >= currentQuestions.size()) {
            return;
        }

        ImageQuestion question = currentQuestions.get(currentQuestionIndex);
        String selectedImageResource = "";

        switch (selectedOption) {
            case 1:
                selectedImageResource = (String) option1Button.getTag();
                break;
            case 2:
                selectedImageResource = (String) option2Button.getTag();
                break;
            case 3:
                selectedImageResource = (String) option3Button.getTag();
                break;
            case 4:
                selectedImageResource = (String) option4Button.getTag();
                break;
        }

        boolean isCorrect = selectedImageResource.equals(question.getCorrectImageResource());

        Log.d(TAG, "Answer check - Selected: '" + selectedImageResource + "', Correct: '" + question.getCorrectImageResource() + "', Result: " + isCorrect);

        saveIndividualAnswer(question, selectedImageResource, isCorrect);

        if (isCorrect) {
            score++;
            scoreTextView.setText("Score: " + score);
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_ok);
            }
            playCorrectSound();
        } else {
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_bad);
            }
            playIncorrectSound();
        }

        hiddenWordTextView.setText(question.getCorrectAnswer());

        highlightButtons(selectedOption, isCorrect, question.getCorrectImageResource());

        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);

        nextButton.setVisibility(View.VISIBLE);

        String feedback = isCorrect ? "¡Correcto!" : "Incorrecto. La respuesta correcta es: " + question.getCorrectAnswer();
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
    }

    private void saveIndividualAnswer(ImageQuestion question, String selectedAnswer, boolean isCorrect) {
        try {
            dbHelper.saveQuizResult(
                    userId,
                    question.getQuestion(),
                    question.getCorrectAnswer(),
                    selectedAnswer,
                    isCorrect,
                    "Image Identification Audio",
                    selectedTopic,
                    selectedLevel,
                    sessionTimestamp
            );
            Log.d(TAG, "Individual answer saved to database");

            if (!isOfflineMode && mAuth.getCurrentUser() != null) {
                String questionId = String.valueOf(currentQuestionIndex);
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("question", question.getQuestion());
                answerData.put("correctAnswer", question.getCorrectAnswer());
                answerData.put("selectedAnswer", selectedAnswer);
                answerData.put("isCorrect", isCorrect);
                answerData.put("timestamp", sessionTimestamp);
                answerData.put("quizType", "Image Identification Audio");
                answerData.put("topic", selectedTopic);
                answerData.put("level", selectedLevel);

                mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                        .child("quiz_results")
                        .child(questionId)
                        .setValue(answerData);
                Log.d(TAG, "Individual answer saved to Firebase");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving individual answer: " + e.getMessage());
        }
    }

    private void highlightButtons(int selectedOption, boolean isCorrect, String correctImageResource) {
        resetButtonStates();

        ImageButton correctButton = null;
        if (option1Button.getTag().equals(correctImageResource)) correctButton = option1Button;
        else if (option2Button.getTag().equals(correctImageResource)) correctButton = option2Button;
        else if (option3Button.getTag().equals(correctImageResource)) correctButton = option3Button;
        else if (option4Button.getTag().equals(correctImageResource)) correctButton = option4Button;

        if (correctButton != null) {
            correctButton.setBackgroundTintList(getColorStateList(R.color.verdeSena));
        }

        ImageButton selectedButton = getButtonByIndex(selectedOption);
        if (selectedButton != null && !isCorrect) {
            selectedButton.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
        }
    }

    private ImageButton getButtonByIndex(int index) {
        switch (index) {
            case 1: return option1Button;
            case 2: return option2Button;
            case 3: return option3Button;
            case 4: return option4Button;
            default: return null;
        }
    }

    private void showQuizResults() {
        double percentage = (double) score / currentQuestions.size() * 100;
        int finalScore = (int) Math.round(percentage);

        saveQuizResults(percentage);

        if (percentage >= 70) {
            markTopicAsPassed(selectedTopic);
        }

        boolean[] questionResults = new boolean[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questionResults[i] = i < score;
        }

        String[] questions = new String[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questions[i] = currentQuestions.get(i).getQuestion();
        }

        String sourceMap = getIntent().getStringExtra("SOURCE_MAP");

        Intent intent = new Intent(this, ImageIdentificationResultsActivity.class);
        intent.putExtra("FINAL_SCORE", finalScore);
        intent.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
        intent.putExtra("CORRECT_ANSWERS", score);
        intent.putExtra("TOPIC", selectedTopic);
        intent.putExtra("LEVEL", selectedLevel);
        intent.putExtra("QUESTION_RESULTS", questionResults);
        intent.putExtra("QUESTIONS", questions);
        intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
        intent.putExtra("SOURCE_MAP", sourceMap);
        startActivity(intent);
        finish();
    }

    private void saveQuizResults(double percentage) {
        try {
            dbHelper.saveQuizResult(
                    userId,
                    "Image Identification Audio Quiz",
                    "Score: " + score + "/" + currentQuestions.size(),
                    "Percentage: " + String.format("%.1f%%", percentage),
                    percentage >= 70,
                    "Image Identification Audio",
                    selectedTopic,
                    selectedLevel,
                    sessionTimestamp
            );
            Log.d(TAG, "Quiz result saved to local database");

            if (!isOfflineMode && mAuth.getCurrentUser() != null) {
                Map<String, Object> quizData = new HashMap<>();
                quizData.put("userId", userId);
                quizData.put("topic", selectedTopic);
                quizData.put("level", selectedLevel);
                quizData.put("score", score);
                quizData.put("totalQuestions", currentQuestions.size());
                quizData.put("percentage", percentage);
                quizData.put("timestamp", sessionTimestamp);
                quizData.put("quizType", "Image Identification Audio");
                quizData.put("offlineMode", isOfflineMode);

                mDatabase.child("quiz_results").push().setValue(quizData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Quiz result saved to Firebase"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save quiz result to Firebase", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving quiz results", e);
        }
    }

    private void markTopicAsPassed(String topic) {
        try {
            dbHelper.markTopicAsPassed(userId, topic, selectedLevel);
            Log.d(TAG, "Topic marked as passed in database: " + topic);

            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String key = "PASSED_" + topic.replace(" ", "_");
            editor.putBoolean(key, true);
            editor.apply();

            Log.d(TAG, "Topic marked as passed in SharedPreferences: " + topic);
        } catch (Exception e) {
            Log.e(TAG, "Error marking topic as passed", e);
        }
    }

    @Override
    protected void onDestroy() {
        // ============================================================
        // NUEVO: Detener monitor y limpiar componente de audio
        // ============================================================
        if (reusableMonitorHandler != null && reusableMonitorRunnable != null) {
            reusableMonitorHandler.removeCallbacks(reusableMonitorRunnable);
        }

        try {
            if (reusableAudioCard != null) {
                reusableAudioCard.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up reusableAudioCard: " + e.getMessage());
        }

        if (correctSoundPlayer != null) {
            correctSoundPlayer.release();
            correctSoundPlayer = null;
        }
        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer.release();
            incorrectSoundPlayer = null;
        }

        super.onDestroy();
    }

    private void initializeSoundPlayers() {
        try {
            correctSoundPlayer = MediaPlayer.create(this, getResources().getIdentifier(
                    "mario_bros_vida", "raw", getPackageName()));

            incorrectSoundPlayer = MediaPlayer.create(this, getResources().getIdentifier(
                    "pacman_dies", "raw", getPackageName()));

            Log.d(TAG, "Sonidos de feedback inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando sonidos de feedback: " + e.getMessage());
        }
    }

    private void playCorrectSound() {
        try {
            if (correctSoundPlayer != null) {
                if (correctSoundPlayer.isPlaying()) {
                    correctSoundPlayer.stop();
                    correctSoundPlayer.prepare();
                }
                correctSoundPlayer.start();
                Log.d(TAG, "Reproduciendo sonido de respuesta correcta");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido correcto: " + e.getMessage());
        }
    }

    private void playIncorrectSound() {
        try {
            if (incorrectSoundPlayer != null) {
                if (incorrectSoundPlayer.isPlaying()) {
                    incorrectSoundPlayer.stop();
                    incorrectSoundPlayer.prepare();
                }
                incorrectSoundPlayer.start();
                Log.d(TAG, "Reproduciendo sonido de respuesta incorrecta");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido incorrecto: " + e.getMessage());
        }
    }

    private void returnToMenu() {
        Intent intent = new Intent(this, MenuReadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}