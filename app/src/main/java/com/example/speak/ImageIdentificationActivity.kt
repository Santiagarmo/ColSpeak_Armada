package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.drawable.PictureDrawable;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.example.speak.database.DatabaseHelper;
import com.example.speak.helpers.WildcardHelper;
import com.example.speak.helpers.HelpModalHelper;
import com.example.speak.helpers.StarProgressHelper;
import com.example.speak.helpers.StarEarnedDialog;
import com.example.speak.components.ModalAlertComponent;
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
import java.util.Arrays;

public class ImageIdentificationActivity extends AppCompatActivity {
    private static final String TAG = "ImageIdentificationActivity";

    // UI Elements
    private TextView questionTextView;
    private TextView questionNumberTextView;
    private ImageView targetImageView;
    private Button playButton;
    private SeekBar speedSeekBar;
    private SeekBar pitchSeekBar;
    private TextView speedValue;
    private TextView pitchValue;
    private ModalAlertComponent modalAlertComponent;

    // Return menu and topic/level views
    private LinearLayout returnContainer;
    private TextView topicTextView;
    private TextView levelTextView;

    // Option buttons
    private Button option1Button;
    private Button option2Button;
    private Button option3Button;
    private Button option4Button;

    // Bird image for feedback
    private ImageView birdImageView;

    // TextToSpeech
    private TextToSpeech textToSpeech;

    // MediaPlayer para sonidos de feedback
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    // Data
    private List<ImageQuestion> allQuestions;
    private List<ImageQuestion> currentQuestions;
    private int currentQuestionIndex = 0;
    private int score = 0;

    // Settings
    private float currentSpeed = 1.0f;
    private float currentPitch = 1.0f;

    // Database and Firebase
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long userId;
    private boolean isOfflineMode = false;
    private long sessionTimestamp;

    // Topic and Level
    private String selectedTopic;
    private String selectedLevel;
    
    // Sistema de comodines
    private WildcardHelper wildcardHelper;
    private ImageView wildcardButton;
    private ImageView helpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_identification);

        Log.d(TAG, "ImageIdentificationActivity onCreate started");

        // Initialize views
        initializeViews();
        Log.d(TAG, "Views initialized");

        // Initialize sound players
        initializeSoundPlayers();
        Log.d(TAG, "Sound players initialized");

        // Get intent data
        Intent intent = getIntent();
        
        // Get topic and level from intent
        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");
        
        // Display topic and level in the interface
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("Topic: " + selectedTopic);
        }
        
        Log.d(TAG, "Intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel);

        // Initialize Firebase and Database
        initializeFirebaseAndDatabase();
        Log.d(TAG, "Firebase and Database initialized");

        // Initialize TextToSpeech
        initializeTextToSpeech();
        Log.d(TAG, "TextToSpeech initialized");

        // Setup controls
        setupSpeedAndPitchControls();
        Log.d(TAG, "Speed and pitch controls setup");

        // Setup button listeners
        setupButtonListeners();
        Log.d(TAG, "Button listeners setup");

        // Validate previous topic completion
        /*
        if (!isPreviousTopicPassed(selectedTopic)) {
            Log.w(TAG, "Previous topic not passed: " + selectedTopic);
            Toast.makeText(this, "Debes completar el tema anterior antes de continuar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        */
        Log.d(TAG, "Previous topic validation passed");

        // Inicializar sistema de comodines
        wildcardHelper = new WildcardHelper(this, "READING", selectedTopic);
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
                applyFiftyFifty();
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

        // Load questions
        loadQuestionsFromFile(selectedTopic, selectedLevel);
        Log.d(TAG, "Questions loaded");

        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp);
        
        // Test the module
        testModule();
    }

    private void initializeViews() {
        questionTextView = findViewById(R.id.questionTextView);
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        targetImageView = findViewById(R.id.targetImageView);
        playButton = findViewById(R.id.playButton);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedValue = findViewById(R.id.speedValue);
        pitchValue = findViewById(R.id.pitchValue);
        modalAlertComponent = findViewById(R.id.modalAlertComponent);

        // Initialize return menu and topic/level views
        returnContainer = findViewById(R.id.returnContainer);
        topicTextView = findViewById(R.id.topicTextView);
        levelTextView = findViewById(R.id.levelTextView);

        // Initialize option buttons
        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);

        // Initialize bird image
        birdImageView = findViewById(R.id.birdImageView);

        // Setup modal callbacks
        if (modalAlertComponent != null) {
            modalAlertComponent.setOnModalActionListener(new ModalAlertComponent.OnModalActionListener() {
                @Override
                public void onContinuePressed(ModalAlertComponent.ModalType type) {
                    currentQuestionIndex++;
                    modalAlertComponent.setVisibility(View.GONE);
                    displayQuestion();
                }

                @Override
                public void onModalHidden(ModalAlertComponent.ModalType type) {
                    // No action needed when modal is hidden
                }
            });
        }

        // Inicializar botón de comodines
        wildcardButton = findViewById(R.id.wildcardButton);
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> showWildcardMenu());
        }

        // Inicializar botón de ayuda (Help)
        helpButton = findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                try {
                    // Determinar el tema actual desde la pregunta visible si existe
                    String topicForHelp = selectedTopic;
                    if (currentQuestions != null && currentQuestionIndex >= 0 && currentQuestionIndex < currentQuestions.size()) {
                        ImageQuestion q = currentQuestions.get(currentQuestionIndex);
                        if (q != null && q.getTopic() != null && !q.getTopic().isEmpty()) {
                            topicForHelp = q.getTopic();
                        }
                    }

                    HelpModalHelper.show(ImageIdentificationActivity.this, topicForHelp, selectedLevel);
                } catch (Exception e) {
                    Log.e(TAG, "Error abriendo modal de ayuda: " + e.getMessage());
                    Toast.makeText(ImageIdentificationActivity.this, "No se pudo abrir la ayuda", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set topic and level text
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("Topic: " + selectedTopic);
        }

        // Set up return button click listener
        returnContainer.setOnClickListener(v -> returnToMenu());
    }

    private void initializeFirebaseAndDatabase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new DatabaseHelper(this);

        // Check network status
        isOfflineMode = !isNetworkAvailable();
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode);

        // Get user ID
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1);
        String email = prefs.getString("user_email", null);
        
        Log.d(TAG, "Initial user ID from prefs: " + userId);
        Log.d(TAG, "User email from prefs: " + email);

        if (userId == -1) {
            if (email != null) {
                userId = dbHelper.getUserId(email);
                Log.d(TAG, "User ID from database: " + userId);
                
                if (userId != -1) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("user_id", userId);
                    editor.apply();
                }
            }
        }

        // Handle guest user in offline mode
        if (userId == -1 && isOfflineMode) {
            Log.d(TAG, "No user ID found in offline mode, creating guest user");
            
            String deviceId = prefs.getString("device_id", null);
            if (deviceId == null) {
                deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (deviceId != null) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("device_id", deviceId);
                    editor.apply();
                    Log.d(TAG, "Saved new device ID: " + deviceId);
                }
            }

            if (deviceId != null) {
                if (dbHelper.isGuestUserExists(deviceId)) {
                    Log.d(TAG, "Guest user exists for device: " + deviceId);
                    Cursor guestCursor = dbHelper.getGuestUser(deviceId);
                    if (guestCursor != null && guestCursor.moveToFirst()) {
                        userId = guestCursor.getLong(guestCursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                        guestCursor.close();
                        Log.d(TAG, "Retrieved guest user ID: " + userId);
                    }
                } else {
                    Log.d(TAG, "Creating new guest user for device: " + deviceId);
                    userId = dbHelper.createGuestUser(deviceId);
                    Log.d(TAG, "Created new guest user with ID: " + userId);
                }

                if (userId != -1) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("user_id", userId);
                    editor.apply();
                }
            }
        }

        if (userId == -1) {
            Log.e(TAG, "Failed to initialize user session");
            Toast.makeText(this, "Error: Could not initialize user session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(ImageIdentificationActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    textToSpeech.setSpeechRate(currentSpeed);
                    textToSpeech.setPitch(currentPitch);
                    Log.d(TAG, "TextToSpeech initialized successfully");
                }
            } else {
                Toast.makeText(ImageIdentificationActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
            }
        });
    }

    private void setupSpeedAndPitchControls() {
        speedSeekBar.setMax(200);
        speedSeekBar.setProgress(100);
        pitchSeekBar.setMax(200);
        pitchSeekBar.setProgress(100);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeed = progress / 100f;
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
                currentPitch = progress / 100f;
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
    }

    private void setupButtonListeners() {
        playButton.setOnClickListener(v -> {
            Log.d(TAG, "Play button clicked");
            playCurrentQuestion();
        });

        // Setup option button listeners
        setupOptionButtonListeners();
    }

    private void setupOptionButtonListeners() {
        option1Button.setOnClickListener(v -> checkAnswer(1));
        option2Button.setOnClickListener(v -> checkAnswer(2));
        option3Button.setOnClickListener(v -> checkAnswer(3));
        option4Button.setOnClickListener(v -> checkAnswer(4));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean isPreviousTopicPassed(String currentTopic) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        
        // Define topic progression order
        String[] topicOrder = {
            "PERSONAL PRONOUNS",
            "POSSESSIVE ADJECTIVES", 
            "VERB TO BE",
            "SIMPLE PRESENT",
            "SIMPLE PRESENT THIRD PERSON",
            "SIMPLE PAST",
            "FREQUENCY ADVERBS",
            "DAILY ROUTINES",
            "COUNTABLE AND UNCOUNTABLE",
            "QUANTIFIERS",
            "PREPOSITIONS",
            "USED TO"
        };
        
        // Find current topic index
        int currentIndex = -1;
        for (int i = 0; i < topicOrder.length; i++) {
            if (topicOrder[i].equals(currentTopic)) {
                currentIndex = i;
                break;
            }
        }
        
        // If it's the first topic or not found, allow access
        if (currentIndex <= 0) {
            return true;
        }
        
        // Check if previous topic is passed
        String previousTopic = topicOrder[currentIndex - 1];
        String key = "PASSED_" + previousTopic.replace(" ", "_");
        boolean isPassed = prefs.getBoolean(key, false);
        
        Log.d(TAG, "Checking previous topic: " + previousTopic + " = " + isPassed);
        return isPassed;
    }

    private void loadQuestionsFromFile(String topic, String level) {
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

        // Método temporal para debuggear la carga de preguntas
        debugLoadQuestions(topic, level);
    }

    // Método temporal para debuggear la carga de preguntas
    private void debugLoadQuestions(String topic, String level) {
        Log.d(TAG, "=== DEBUG LOAD QUESTIONS ===");
        Log.d(TAG, "Topic: " + topic);
        Log.d(TAG, "Level: " + level);
        
        try {
            AssetManager assetManager = getAssets();
            String fileName = "image_questions_" + level.toLowerCase() + ".txt";
            Log.d(TAG, "File name: " + fileName);
            
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
                
                Log.d(TAG, "Line " + lineNumber + ": " + line);
                
                String[] parts = line.split("\\|");
                if (parts.length >= 9) {
                    String questionTopic = parts[6];
                    String questionLevel = parts[7];
                    
                    Log.d(TAG, "Found topic: '" + questionTopic + "', level: '" + questionLevel + "'");
                    Log.d(TAG, "Matches current topic: " + questionTopic.equals(topic));
                    Log.d(TAG, "Matches current level: " + questionLevel.equals(level));
                }
            }
            
            reader.close();
            inputStream.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Debug error", e);
        }
        
        Log.d(TAG, "=== END DEBUG ===");
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
        
        // Set question text with HTML formatting
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            questionTextView.setText(android.text.Html.fromHtml(question.getQuestion(), android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            questionTextView.setText(android.text.Html.fromHtml(question.getQuestion()));
        }
        
        // Load image
        loadImage(question.getImageResourceName());
        
        // Set options
        String[] options = question.getOptions();
        option1Button.setText(options[0]);
        option2Button.setText(options[1]);
        option3Button.setText(options[2]);
        option4Button.setText(options[3]);
        
        // Reset button states
        resetButtonStates();

        // Hide modal initially
        if (modalAlertComponent != null) {
            modalAlertComponent.setVisibility(View.GONE);
        }

        Log.d(TAG, "Displaying question " + (currentQuestionIndex + 1) + ": " + question.getQuestion());
    }

    private void loadImage(String imageResourceName) {
        try {
            // Check if it's an SVG file
            if (imageResourceName.toLowerCase().endsWith(".svg")) {
                if (loadSVGFromAssets(imageResourceName)) {
                    return;
                }
            }
            
            // Try to load as regular drawable
            if (loadRegularImage(imageResourceName)) {
                return;
            }
            
            // If all else fails, set default image
            targetImageView.setImageResource(R.drawable.ic_quiz);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + imageResourceName, e);
            targetImageView.setImageResource(R.drawable.ic_quiz);
        }
    }

    private boolean loadSVGFromAssets(String svgFileName) {
        try {
            // Load SVG from assets
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(svgFileName);
            
            SVG svg = SVG.getFromInputStream(inputStream);
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            targetImageView.setImageDrawable(drawable);
            
            inputStream.close();
            Log.d(TAG, "Loaded SVG from assets: " + svgFileName);
            return true;
            
        } catch (IOException e) {
            Log.w(TAG, "SVG not found in assets: " + svgFileName);
            return false;
        } catch (SVGParseException e) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e);
            return false;
        }
    }

    private void loadSVGImage(String svgFileName) {
        try {
            // Try to load from assets first
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(svgFileName);
            
            SVG svg = SVG.getFromInputStream(inputStream);
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            targetImageView.setImageDrawable(drawable);
            
            inputStream.close();
            Log.d(TAG, "Loaded SVG from assets: " + svgFileName);
            
        } catch (IOException e) {
            Log.w(TAG, "SVG not found in assets, trying drawable: " + svgFileName);
            // If not found in assets, try drawable folder
            loadRegularImage(svgFileName);
        } catch (SVGParseException e) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e);
            targetImageView.setImageResource(R.drawable.ic_quiz);
        }
    }

    private boolean loadRegularImage(String imageResourceName) {
        try {
            // Get resource ID from drawable
            int resourceId = getResources().getIdentifier(
                imageResourceName, "drawable", getPackageName()
            );
            
            if (resourceId != 0) {
                targetImageView.setImageResource(resourceId);
                Log.d(TAG, "Loaded regular image: " + imageResourceName + " (ID: " + resourceId + ")");
                return true;
            } else {
                Log.w(TAG, "Image resource not found: " + imageResourceName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading regular image: " + imageResourceName, e);
            return false;
        }
    }

    private void resetButtonStates() {
        // Reset button colors and enable them
        option1Button.setEnabled(true);
        option2Button.setEnabled(true);
        option3Button.setEnabled(true);
        option4Button.setEnabled(true);
        
        option1Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        option2Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        option3Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        option4Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        
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
        String textToSpeak = question.getQuestion() + ". " + question.getCorrectAnswer();
        
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
        String selectedAnswer = "";
        
        switch (selectedOption) {
            case 1:
                selectedAnswer = option1Button.getText().toString();
                break;
            case 2:
                selectedAnswer = option2Button.getText().toString();
                break;
            case 3:
                selectedAnswer = option3Button.getText().toString();
                break;
            case 4:
                selectedAnswer = option4Button.getText().toString();
                break;
        }
        
        boolean isCorrect = selectedAnswer.equals(question.getCorrectAnswer());
        
        // Verificar si esta pregunta fue cambiada por un comodín
        boolean wasQuestionChanged = wildcardHelper.wasQuestionChanged(currentQuestionIndex);
        
        Log.d(TAG, "Answer check - Selected: '" + selectedAnswer + "', Correct: '" + question.getCorrectAnswer() + "', Result: " + isCorrect);
        Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged);
        
        if (isCorrect) {
            // Solo sumar puntos si la pregunta NO fue cambiada por un comodín
            if (!wasQuestionChanged) {
                score++;
                Log.d(TAG, "Puntos sumados: +1 (Total: " + score + ")");
            } else {
                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín");
            }
            
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
        
        // Highlight buttons
        highlightButtons(selectedOption, isCorrect, question.getCorrectAnswer());
        
        // Disable all buttons
        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);

        // Show modal with appropriate message
        if (modalAlertComponent != null) {
            String primaryMsg, secondaryMsg;

            if (isCorrect) {
                if (wasQuestionChanged) {
                    primaryMsg = "¡Muy bien!, tu nivel de inglés está mejorando";
                    secondaryMsg = "(Pregunta cambiada por comodín - No suma puntos)";
                } else {
                    primaryMsg = "¡Muy bien!, tu nivel de inglés está mejorando";
                    secondaryMsg = "Amazing, you are improving your English";
                }
                modalAlertComponent.showCorrectModal(primaryMsg, secondaryMsg);
            } else {
                primaryMsg = "¡Ten cuidado!, sigue intentando";
                secondaryMsg = "La respuesta correcta es: " + question.getCorrectAnswer();
                modalAlertComponent.showIncorrectModal(primaryMsg, secondaryMsg);
            }
        }
    }

    private void highlightButtons(int selectedOption, boolean isCorrect, String correctAnswer) {
        // Get all option texts
        String[] options = {
            option1Button.getText().toString(),
            option2Button.getText().toString(),
            option3Button.getText().toString(),
            option4Button.getText().toString()
        };
        
        // Find correct answer index
        int correctIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(correctAnswer)) {
                correctIndex = i + 1;
                break;
            }
        }
        
        // Apply colors
        if (isCorrect) {
            // Selected answer is correct - show green
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.verdeSena));
        } else {
            // Selected answer is wrong - show red for selected, green for correct
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
            if (correctIndex != selectedOption) {
                getButtonByIndex(correctIndex).setBackgroundTintList(getColorStateList(R.color.verdeSena));
            }
        }
    }

    private Button getButtonByIndex(int index) {
        switch (index) {
            case 1: return option1Button;
            case 2: return option2Button;
            case 3: return option3Button;
            case 4: return option4Button;
            default: return option1Button;
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
            // Sumar puntos de estrella (10) y mostrar modal de estrella, igual que Speaking
            StarProgressHelper.addSessionPoints(this, 10);
            new Handler().postDelayed(() -> {
                try {
                    StarEarnedDialog.show(ImageIdentificationActivity.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.getMessage());
                }
            }, 200);
        }
        
        // Preparar datos para posible navegación a detalles
        boolean[] questionResults = new boolean[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questionResults[i] = i < score; // simplificado
        }
        String[] questions = new String[currentQuestions.size()];
        for (int i = 0; i < currentQuestions.size(); i++) {
            questions[i] = currentQuestions.get(i).getQuestion();
        }
        String sourceMap = getIntent().getStringExtra("SOURCE_MAP");
        
        // Mostrar diálogo de resultados (mismo estilo que dialog_quiz_result)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView birdImg = dialogView.findViewById(R.id.birdImageView);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        TextView counterTextView = dialogView.findViewById(R.id.counterTextView);
        TextView scoreTextView = dialogView.findViewById(R.id.scoreTextView);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        TextView btnReintentar = dialogView.findViewById(R.id.btnReintentar);
        LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

        // Imagen y mensaje según puntaje (criterio similar a Listening)
        if (finalScore >= 90) {
            messageTextView.setText("Excellent your English is getting better!");
            birdImg.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 70) {
            messageTextView.setText("Good, but you can do it better!");
            birdImg.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!");
            birdImg.setImageResource(R.drawable.crab_test);
        } else {
            messageTextView.setText("You should practice more!");
            birdImg.setImageResource(R.drawable.crab_bad);
        }

        counterTextView.setText(score + "/" + currentQuestions.size());
        scoreTextView.setText("Score: " + finalScore + "%");

        // Continuar: ir al siguiente tema según progresión y mapa de origen
        btnContinue.setOnClickListener(v -> {
            String nextTopic = ProgressionHelper.getNextImageIdentificationTopicBySource(selectedTopic, sourceMap);
            if (nextTopic != null) {
                Class<?> nextActivity = ProgressionHelper.getReadingActivityClass(nextTopic);
                Intent next = new Intent(this, nextActivity);
                next.putExtra("TOPIC", nextTopic);
                next.putExtra("LEVEL", selectedLevel);
                next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(next);
                finish();
            } else {
                // Si no hay siguiente tema, volver al mapa correspondiente
                Class<?> destMap = ProgressionHelper.getDestinationMapClass(sourceMap);
                Intent back = new Intent(this, destMap);
                back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(back);
                finish();
            }
        });

        // Reintentar: reiniciar esta actividad mismo tema/nivel
        btnReintentar.setText("Try again");
        btnReintentar.setOnClickListener(v -> {
            Intent retry = new Intent(this, ImageIdentificationActivity.class);
            retry.putExtra("TOPIC", selectedTopic);
            retry.putExtra("LEVEL", selectedLevel);
            startActivity(retry);
            finish();
        });

        // Ver detalles: abrir tabla de historial (QuizHistoryActivity) filtrada por la sesión actual
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizHistoryActivity.class);
            intent.putExtra("SCORE", score);
        intent.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
            intent.putExtra("QUIZ_TYPE", "Identificación Imagen");
        intent.putExtra("TOPIC", selectedTopic);
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
        intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
        startActivity(intent);
        finish();
        });
    }

    private void saveQuizResults(double percentage) {
        // Save to local database - save each question result individually
        for (ImageQuestion question : currentQuestions) {
            // For now, we'll save a summary result. In a real implementation,
            // you might want to save each individual question result
            dbHelper.saveQuizResult(
                userId, 
                "Image Identification Quiz", 
                question.getCorrectAnswer(), 
                "Quiz completed", 
                percentage >= 70, 
                "Identificación Imagen",
                selectedTopic, 
                selectedLevel, 
                sessionTimestamp
            );
        }
        
        Log.d(TAG, "Saved quiz results to database");
        
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
            quizData.put("offlineMode", isOfflineMode);
            
            mDatabase.child("quiz_results").push().setValue(quizData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Quiz result saved to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save quiz result to Firebase", e));
        }
    }

    private void markTopicAsPassed(String topic) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "PASSED_" + topic.replace(" ", "_");
        editor.putBoolean(key, true);
        editor.apply();
        
        Log.d(TAG, "Marked topic as passed: " + topic);
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

    // Método de prueba para diagnosticar problemas
    private void testModule() {
        Log.d(TAG, "=== TESTING IMAGE IDENTIFICATION MODULE ===");
        Log.d(TAG, "Selected Topic: " + selectedTopic);
        Log.d(TAG, "Selected Level: " + selectedLevel);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Offline Mode: " + isOfflineMode);
        Log.d(TAG, "All Questions Count: " + (allQuestions != null ? allQuestions.size() : "null"));
        Log.d(TAG, "Current Questions Count: " + (currentQuestions != null ? currentQuestions.size() : "null"));
        Log.d(TAG, "Current Question Index: " + currentQuestionIndex);
        Log.d(TAG, "Score: " + score);
        Log.d(TAG, "TextToSpeech: " + (textToSpeech != null ? "initialized" : "null"));
        Log.d(TAG, "=== END TEST ===");
    }

    private void returnToMenu() {
        // Return to MenuReadingActivity
        Intent intent = new Intent(this, MenuReadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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
     * Ayuda 4: Aplicar 50/50 - Mostrar solo 2 opciones (temporal)
     */
    private void applyFiftyFifty() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ImageQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            List<String> allOptions = Arrays.asList(currentQuestion.getOptions());
            String correctAnswer = currentQuestion.getCorrectAnswer();
            
            // Aplicar 50/50 usando el helper
            List<String> remainingOptions = wildcardHelper.applyFiftyFifty(allOptions, correctAnswer);
            
            // Mostrar las opciones restantes en un diálogo
            StringBuilder optionsText = new StringBuilder("Opciones restantes:\n\n");
            for (int i = 0; i < remainingOptions.size(); i++) {
                optionsText.append((i + 1)).append(". ").append(remainingOptions.get(i)).append("\n");
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🎯 Ayuda 50/50");
            builder.setMessage(optionsText.toString());
            builder.setPositiveButton("Entendido", null);
            builder.show();
            
            Toast.makeText(this, "50/50 aplicado - Opciones mostradas (no afecta tu evaluación)", Toast.LENGTH_SHORT).show();
        }
    }

    // NOTA: El método updateButtonsWithFiftyFifty fue eliminado
    // porque las ayudas NO deben afectar la interfaz de evaluación

    /**
     * Ayuda 5: Ayuda creativa - Mostrar pista contextual
     */
    private void showCreativeHelp() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ImageQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            String question = currentQuestion.getQuestion();
            String correctAnswer = currentQuestion.getCorrectAnswer();
            
            String creativeHelp = wildcardHelper.getCreativeHelp(question, correctAnswer);
            
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
        // Para ImageIdentificationActivity, mostrar información básica
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
 