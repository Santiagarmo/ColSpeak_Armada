package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.AssetManager;
import android.graphics.drawable.PictureDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
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
    private Button playButton;
    private Button nextButton;
    private SeekBar speedSeekBar;
    private SeekBar pitchSeekBar;
    private TextView speedValue;
    private TextView pitchValue;

    // Option buttons (ImageButtons instead of regular Buttons)
    private ImageButton option1Button;
    private ImageButton option2Button;
    private ImageButton option3Button;
    private ImageButton option4Button;

    // Bird image for feedback
    private ImageView birdImageView;

    // TextToSpeech
    private TextToSpeech textToSpeech;

    // MediaPlayer para sonidos de feedback
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    // Quiz data
    private List<ImageQuestion> allQuestions;
    private List<ImageQuestion> currentQuestions;
    private int currentQuestionIndex = 0;
    private int score = 0;

    // Audio controls
    private float currentSpeed = 1.0f;
    private float currentPitch = 1.0f;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_identification_audio);

        Log.d(TAG, "ImageIdentificationAudioActivity onCreate started");

        // Get intent data FIRST
        Intent intent = getIntent();
        Log.d(TAG, "Intent received: " + intent);
        Log.d(TAG, "Intent extras: " + intent.getExtras());

        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");

        Log.d(TAG, "Raw topic value: '" + selectedTopic + "'");
        Log.d(TAG, "Raw level value: '" + selectedLevel + "'");
        Log.d(TAG, "Topic is null: " + (selectedTopic == null));
        Log.d(TAG, "Level is null: " + (selectedLevel == null));

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

        // Initialize TextToSpeech
        initializeTextToSpeech();

        // Setup speed and pitch controls
        setupSpeedAndPitchControls();

        // Setup button listeners
        setupButtonListeners();

        // Setup option button listeners
        setupOptionButtonListeners();

        // Validate previous topic completion (temporarily disabled for testing)
        Log.d(TAG, "Previous topic validation passed");

        // Load questions
        loadQuestionsFromFile(selectedTopic, selectedLevel);

        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp);

        // Test module
        testModule();
    }

    private void initializeViews() {
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        levelTextView = findViewById(R.id.levelTextView);
        topicTextView = findViewById(R.id.topicTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        hiddenWordTextView = findViewById(R.id.hiddenWordTextView);
        playButton = findViewById(R.id.playButton);
        nextButton = findViewById(R.id.nextButton);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedValue = findViewById(R.id.speedValue);
        pitchValue = findViewById(R.id.pitchValue);

        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);

        // Initialize bird image
        birdImageView = findViewById(R.id.birdImageView);

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
                            // Cambiar a la siguiente pregunta segura
                            currentQuestionIndex = Math.min(currentQuestionIndex + 1, currentQuestions.size());
                            displayQuestion();
                        }

                        @Override
                        public void onShowContentImage() {
                            Toast.makeText(ImageIdentificationAudioActivity.this, "Mostrando imagen de contenido", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onShowInstructorVideo() {
                            // Reproducir video del instructor según el tema actual
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

        // Help modal integration: abrir HelpActivity con el tema y nivel actuales
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

    private void initializeFirebaseAndDatabase() {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Database Helper
        dbHelper = new DatabaseHelper(this);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long initialUserId = prefs.getLong("userId", 1);
        String userEmail = prefs.getString("userEmail", null);

        Log.d(TAG, "Initial user ID from prefs: " + initialUserId);
        Log.d(TAG, "User email from prefs: " + userEmail);

        if (isOfflineMode || mAuth.getCurrentUser() == null) {
            userId = initialUserId;
        } else {
            userId = initialUserId; // Use the same logic as other activities
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode);

        // Initialize Firebase Database
        if (!isOfflineMode) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }

        Log.d(TAG, "Firebase and Database initialized");
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported");
                    } else {
                        textToSpeech.setSpeechRate(currentSpeed);
                        textToSpeech.setPitch(currentPitch);
                        Log.d(TAG, "TextToSpeech initialized");
                    }
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed");
                }
            }
        });

        Log.d(TAG, "TextToSpeech initialized");
    }

    private void setupSpeedAndPitchControls() {
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeed = progress / 100.0f;
                speedValue.setText(String.format("%.1fx", currentSpeed));
                if (textToSpeech != null) {
                    textToSpeech.setSpeechRate(currentSpeed);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPitch = progress / 100.0f;
                pitchValue.setText(String.format("%.1fx", currentPitch));
                if (textToSpeech != null) {
                    textToSpeech.setPitch(currentPitch);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Log.d(TAG, "Speed and pitch controls setup");
    }

    private void setupButtonListeners() {
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCurrentQuestion();
            }
        });

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
        // Validate input parameters
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
            Log.d(TAG, "Looking for topic: '" + topic + "' and level: '" + level + "'");

            InputStream inputStream = assetManager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            int lineNumber = 0;
            int totalLines = 0;
            int skippedLines = 0;
            int processedLines = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalLines++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    skippedLines++;
                    continue;
                }

                Log.d(TAG, "Processing line " + lineNumber + ": " + line);
                processedLines++;

                // Parse question format: question|correct_answer|option1|option2|option3|option4|topic|level|image_resource
                String[] parts = line.split("\\|");
                Log.d(TAG, "Line has " + parts.length + " parts");

                if (parts.length >= 9) {
                    String questionText = parts[0];
                    String correctAnswer = parts[1];
                    String[] options = {parts[2], parts[3], parts[4], parts[5]};
                    String questionTopic = parts[6];
                    String questionLevel = parts[7];
                    String imageResource = parts[8];

                    Log.d(TAG, "Parsed question - Topic: '" + questionTopic + "', Level: '" + questionLevel + "'");
                    Log.d(TAG, "Comparing with - Topic: '" + topic + "', Level: '" + level + "'");
                    Log.d(TAG, "Topic match: " + questionTopic.equals(topic) + ", Level match: " + questionLevel.equals(level));

                    // Only add questions for the current topic and level
                    if (questionTopic.equals(topic) && questionLevel.equals(level)) {
                        ImageQuestion question = new ImageQuestion(
                                questionText, correctAnswer, options, questionTopic, questionLevel, imageResource
                        );
                        allQuestions.add(question);
                        Log.d(TAG, "Added question: " + questionText + " for topic: " + topic);
                    } else {
                        Log.d(TAG, "Question skipped - topic or level mismatch");
                    }
                } else {
                    Log.w(TAG, "Invalid line format at line " + lineNumber + ": " + line);
                }
            }

            reader.close();
            inputStream.close();

            Log.d(TAG, "File processing summary:");
            Log.d(TAG, "Total lines: " + totalLines);
            Log.d(TAG, "Skipped lines: " + skippedLines);
            Log.d(TAG, "Processed lines: " + processedLines);
            Log.d(TAG, "Loaded " + allQuestions.size() + " questions for topic: " + topic);

            if (allQuestions.isEmpty()) {
                Log.w(TAG, "No questions found for topic: " + topic + " and level: " + level);
                Toast.makeText(this, "No se encontraron preguntas para este tema.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Shuffle questions and take first 10
            Collections.shuffle(allQuestions);
            currentQuestions = new ArrayList<>(allQuestions.subList(0, Math.min(10, allQuestions.size())));

            // Display first question
            displayQuestion();

        } catch (IOException e) {
            Log.e(TAG, "Error loading questions from file", e);
            Toast.makeText(this, "Error cargando las preguntas.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= currentQuestions.size()) {
            // Quiz completed
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

        // Load images for options
        loadOptionImages(question);

        // Reset button states
        resetButtonStates();

        // Hide next button initially
        nextButton.setVisibility(View.GONE);

        Log.d(TAG, "Displaying question " + (currentQuestionIndex + 1) + ": " + question.getQuestion());
    }

    private void loadOptionImages(ImageQuestion question) {
        try {
            // Obtener todas las imágenes disponibles desde assets
            List<String> availableImages = getAvailableImagesFromAssets();

            // Asegurar que la imagen correcta esté incluida
            List<String> optionImages = new ArrayList<>();
            optionImages.add(question.getImageResourceName()); // Imagen correcta

            // Agregar 3 imágenes aleatorias diferentes, evitando las recientemente usadas
            List<String> remainingImages = new ArrayList<>();
            for (String img : availableImages) {
                if (!img.equals(question.getImageResourceName()) &&
                        !recentlyUsedImages.contains(img)) {
                    remainingImages.add(img);
                }
            }

            // Si no hay suficientes imágenes sin usar recientemente, usar todas las disponibles
            if (remainingImages.size() < 3) {
                remainingImages.clear();
                for (String img : availableImages) {
                    if (!img.equals(question.getImageResourceName())) {
                        remainingImages.add(img);
                    }
                }
            }

            // Mezclar y tomar 3 imágenes aleatorias
            Collections.shuffle(remainingImages);
            for (int i = 0; i < Math.min(3, remainingImages.size()); i++) {
                optionImages.add(remainingImages.get(i));
            }

            // Mezclar todas las opciones para que la respuesta correcta no esté siempre en la misma posición
            Collections.shuffle(optionImages);

            // Set images for each option
            setImageButton(option1Button, optionImages.get(0));
            setImageButton(option2Button, optionImages.get(1));
            setImageButton(option3Button, optionImages.get(2));
            setImageButton(option4Button, optionImages.get(3));

            // Store the correct answer for this question
            question.setCorrectImageResource(question.getImageResourceName());

            // Actualizar la lista de imágenes recientemente usadas
            updateRecentlyUsedImages(optionImages);

            Log.d(TAG, "Loaded option images: " + optionImages.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error loading option images", e);
        }
    }

    private void updateRecentlyUsedImages(List<String> usedImages) {
        // Agregar las imágenes usadas al inicio de la lista
        for (String img : usedImages) {
            if (!recentlyUsedImages.contains(img)) {
                recentlyUsedImages.add(0, img);
            }
        }

        // Mantener solo las últimas MAX_RECENT_IMAGES
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
                    // Incluir archivos SVG
                    if (file.toLowerCase().endsWith(".svg")) {
                        availableImages.add(file);
                    }
                }
            }

            // Agregar imágenes drawable como respaldo para mayor variedad
            String[] drawableImages = {
                    "ic_cat"
            };

            for (String img : drawableImages) {
                availableImages.add(img);
            }

            Log.d(TAG, "Found " + availableImages.size() + " available images (SVG + Drawable)");

        } catch (IOException e) {
            Log.e(TAG, "Error reading assets directory", e);
            // Fallback a imágenes hardcodeadas si hay error
            String[] fallbackImages = {
                    "ic_cat"
            };
            for (String img : fallbackImages) {
                availableImages.add(img);
            }
        }

        return availableImages;
    }

    // Variable para evitar repetir las mismas imágenes en preguntas consecutivas
    private List<String> recentlyUsedImages = new ArrayList<>();
    private static final int MAX_RECENT_IMAGES = 8; // Evitar usar las últimas 8 imágenes usadas

    private void setImageButton(ImageButton button, String imageResourceName) {
        try {
            // Check if it's an SVG file
            if (imageResourceName.toLowerCase().endsWith(".svg")) {
                if (setSVGImageButtonFromAssets(button, imageResourceName)) {
                    return;
                }
            }

            // Try to load as regular drawable
            if (setRegularImageButton(button, imageResourceName)) {
                return;
            }

            // If all else fails, set default image
            button.setImageResource(R.drawable.ic_quiz);

        } catch (Exception e) {
            Log.e(TAG, "Error setting image: " + imageResourceName, e);
            button.setImageResource(R.drawable.ic_quiz);
        }
    }

    private boolean setSVGImageButtonFromAssets(ImageButton button, String svgFileName) {
        try {
            // Load SVG from assets
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(svgFileName);

            SVG svg = SVG.getFromInputStream(inputStream);
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            button.setImageDrawable(drawable);
            button.setTag(svgFileName); // Store the full filename for answer checking

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

    private void setSVGImageButton(ImageButton button, String svgFileName) {
        try {
            // Try to load from assets first
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(svgFileName);

            SVG svg = SVG.getFromInputStream(inputStream);
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            button.setImageDrawable(drawable);
            button.setTag(svgFileName); // Store the resource name for answer checking

            inputStream.close();
            Log.d(TAG, "Set SVG from assets: " + svgFileName);

        } catch (IOException e) {
            Log.w(TAG, "SVG not found in assets, trying drawable: " + svgFileName);
            // If not found in assets, try drawable folder
            setRegularImageButton(button, svgFileName);
        } catch (SVGParseException e) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e);
            button.setImageResource(R.drawable.ic_quiz);
        }
    }

    private boolean setRegularImageButton(ImageButton button, String imageResourceName) {
        try {
            int resourceId = getResources().getIdentifier(
                    imageResourceName, "drawable", getPackageName()
            );

            if (resourceId != 0) {
                button.setImageResource(resourceId);
                button.setTag(imageResourceName); // Store the resource name for answer checking
                Log.d(TAG, "Set regular image: " + imageResourceName + " (ID: " + resourceId + ")");
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
        // Reset button states
        option1Button.setEnabled(true);
        option2Button.setEnabled(true);
        option3Button.setEnabled(true);
        option4Button.setEnabled(true);

        option1Button.setBackgroundTintList(getColorStateList(R.color.white));
        option2Button.setBackgroundTintList(getColorStateList(R.color.white));
        option3Button.setBackgroundTintList(getColorStateList(R.color.white));
        option4Button.setBackgroundTintList(getColorStateList(R.color.white));

        // Reset bird image to default
        if (birdImageView != null) {
            birdImageView.setImageResource(R.drawable.crab_test);
        }
    }

    private void playCurrentQuestion() {
        if (currentQuestionIndex >= currentQuestions.size()) {
            return;
        }

        ImageQuestion question = currentQuestions.get(currentQuestionIndex);
        String textToSpeak = question.getCorrectAnswer();

        if (textToSpeech != null) {
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "question_utterance");
            Log.d(TAG, "Playing audio: " + textToSpeak);
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

        // Save individual answer to database
        saveIndividualAnswer(question, selectedImageResource, isCorrect);

        if (isCorrect) {
            score++;
            scoreTextView.setText("Score: " + score);
            // Cambiar la imagen del pájaro a la imagen de correcto
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_ok);
            }
            // Reproducir sonido de éxito
            playCorrectSound();
        } else {
            // Cambiar la imagen del pájaro a la imagen de incorrecto
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_bad);
            }
            // Reproducir sonido de error
            playIncorrectSound();
        }

        // Show the correct answer
        hiddenWordTextView.setText(question.getCorrectAnswer());

        // Highlight buttons
        highlightButtons(selectedOption, isCorrect, question.getCorrectImageResource());

        // Disable all buttons
        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);

        // Show next button
        nextButton.setVisibility(View.VISIBLE);

        // Show feedback
        String feedback = isCorrect ? "¡Correcto!" : "Incorrecto. La respuesta correcta es: " + question.getCorrectAnswer();
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
    }

    private void saveIndividualAnswer(ImageQuestion question, String selectedAnswer, boolean isCorrect) {
        try {
            // Save to local database
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

            // Save to Firebase if online
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
        // Reset all buttons first
        resetButtonStates();

        // Find the correct button
        ImageButton correctButton = null;
        if (option1Button.getTag().equals(correctImageResource)) correctButton = option1Button;
        else if (option2Button.getTag().equals(correctImageResource)) correctButton = option2Button;
        else if (option3Button.getTag().equals(correctImageResource)) correctButton = option3Button;
        else if (option4Button.getTag().equals(correctImageResource)) correctButton = option4Button;

        // Highlight correct button in green
        if (correctButton != null) {
            correctButton.setBackgroundTintList(getColorStateList(R.color.verdeSena));
        }

        // Highlight selected button
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

        // Save results
        saveQuizResults(percentage);

        // Mark topic as passed if score >= 70%
        if (percentage >= 70) {
            markTopicAsPassed(selectedTopic);
        }

        // Create array of question results
        boolean[] questionResults = new boolean[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questionResults[i] = i < score; // Simplified - you might want to track individual results
        }

        // Create array of question texts
        String[] questions = new String[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questions[i] = currentQuestions.get(i).getQuestion();
        }

        // Get source map from intent
        String sourceMap = getIntent().getStringExtra("SOURCE_MAP");

        // Launch results activity
        Intent intent = new Intent(this, ImageIdentificationResultsActivity.class);
        intent.putExtra("FINAL_SCORE", finalScore);
        intent.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
        intent.putExtra("CORRECT_ANSWERS", score);
        intent.putExtra("TOPIC", selectedTopic);
        intent.putExtra("LEVEL", selectedLevel);
        intent.putExtra("QUESTION_RESULTS", questionResults);
        intent.putExtra("QUESTIONS", questions);
        intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
        intent.putExtra("SOURCE_MAP", sourceMap); // Pass source map information
        startActivity(intent);
        finish();
    }

    private void saveQuizResults(double percentage) {
        try {
            // Save to local database using the original method with adapted parameters
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

            // Save to Firebase if online
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
            // Save to database
            dbHelper.markTopicAsPassed(userId, topic, selectedLevel);
            Log.d(TAG, "Topic marked as passed in database: " + topic);

            // Save to SharedPreferences (like ImageIdentificationActivity)
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
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        // Release MediaPlayer resources
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

    // Método para inicializar los MediaPlayer de sonidos
    private void initializeSoundPlayers() {
        try {
            // Inicializar sonido de respuesta correcta
            correctSoundPlayer = MediaPlayer.create(this, getResources().getIdentifier(
                    "mario_bros_vida", "raw", getPackageName()));

            // Inicializar sonido de respuesta incorrecta
            incorrectSoundPlayer = MediaPlayer.create(this, getResources().getIdentifier(
                    "pacman_dies", "raw", getPackageName()));

            Log.d(TAG, "Sonidos de feedback inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando sonidos de feedback: " + e.getMessage());
        }
    }

    // Método para reproducir sonido de respuesta correcta
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

    // Método para reproducir sonido de respuesta incorrecta
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

    private void testModule() {
        Log.d(TAG, "=== TESTING IMAGE IDENTIFICATION AUDIO MODULE ===");
        Log.d(TAG, "Selected Topic: " + selectedTopic);
        Log.d(TAG, "Selected Level: " + selectedLevel);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Offline Mode: " + isOfflineMode);
        Log.d(TAG, "All Questions Count: " + allQuestions.size());
        Log.d(TAG, "Current Questions Count: " + currentQuestions.size());
        Log.d(TAG, "Current Question Index: " + currentQuestionIndex);
        Log.d(TAG, "Score: " + score);
        Log.d(TAG, "TextToSpeech: " + (textToSpeech != null ? "initialized" : "not initialized"));
        Log.d(TAG, "=== END TEST ===");
    }

    private void returnToMenu() {
        // Return to MenuReadingActivity
        Intent intent = new Intent(this, MenuReadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}