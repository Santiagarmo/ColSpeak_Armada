package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.database.DatabaseHelper;
import com.example.speak.helpers.WildcardHelper;
import com.example.speak.helpers.HelpModalHelper;
import com.example.speak.helpers.StarEarnedDialog;
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

public class ListeningActivity extends AppCompatActivity {
    private static final String TAG = "ListeningActivity";

    private TextView questionTextView;
    private TextView levelTextView;
    private TextView topicTextView;
    private TextView questionNumberTextView;
    private LinearLayout optionsRadioGroup;

    private Button submitButton;
    private Button nextButton;

    // Reproductor de audio
    private com.example.speak.components.ReusableAudioPlayerCard reusableAudioCard;
    private boolean lastReusablePlaying = false;
    private Handler reusableMonitorHandler = new Handler(Looper.getMainLooper());
    private Runnable reusableMonitorRunnable;

    private TextView selectionInstructionText;

    // Botones de opciones
    private Button option1Button;
    private Button option2Button;
    private Button option3Button;
    private Button option4Button;

    private ImageView birdImageView;
    private TextToSpeech textToSpeech;
    private boolean isTextToSpeechReady = false;

    // MediaPlayer para sonidos de feedback
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    private List<ListeningQuestion> allQuestions;
    private List<ListeningQuestion> currentQuestions;

    private int currentQuestionIndex = 0;
    private int currentIndex = 0;
    private String selectedTopic;
    private String selectedLevel;

    private int score = 0;
    private int evaluatedQuestionsCount = 0;

    private float currentSpeed = 0.1f;
    private float currentPitch = 1.0f;

    // Control de reproducción
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long startTime = 0;
    private long pauseTime = 0;
    private long totalDuration = 10000;
    private long currentPosition = 0;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long userId;
    private boolean isOfflineMode = false;
    private long sessionTimestamp;

    // Sistema de comodines
    private WildcardHelper wildcardHelper;
    private ImageView wildcardButton;

    // ============================================================
    // CAMBIO: Variable del componente reutilizable de modales
    // ============================================================
    private com.example.speak.components.ModalAlertComponent modalAlertComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);

        // Initialize views
        questionTextView = findViewById(R.id.questionTextView);
        levelTextView = findViewById(R.id.levelTextView);
        topicTextView = findViewById(R.id.topicTextView);

        // Recibir los parámetros enviados desde el menú
        Intent intent = getIntent();
        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");

        // Log para verificar parámetros recibidos
        Log.d(TAG, "Topic recibido: '" + selectedTopic + "'");
        Log.d(TAG, "Level recibido: '" + selectedLevel + "'");

        // Mostrar topic y level en la interfaz
        if (selectedLevel != null) {
            levelTextView.setText("Level: " + selectedLevel);
        }
        if (selectedTopic != null) {
            topicTextView.setText("THE " + selectedTopic);
        }

        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup);

        nextButton = findViewById(R.id.nextButton);

        // Inicializar botones de opciones
        option1Button = findViewById(R.id.option1RadioButton);
        option2Button = findViewById(R.id.option2RadioButton);
        option3Button = findViewById(R.id.option3RadioButton);
        option4Button = findViewById(R.id.option4RadioButton);

        // Inicializar bird image
        birdImageView = findViewById(R.id.birdImageView);

        // Inicializar botón de comodines
        wildcardButton = findViewById(R.id.wildcardButton);
        if (wildcardButton != null) {
            wildcardButton.setOnClickListener(v -> showWildcardMenu());
        }

        // Inicializar botón de ayuda
        ImageView helpButton = findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                try {
                    HelpModalHelper.show(ListeningActivity.this, selectedTopic, selectedLevel);
                } catch (Exception e) {
                    Log.e(TAG, "Error abriendo modal de ayuda: " + e.getMessage());
                    Toast.makeText(ListeningActivity.this, "No se pudo abrir la ayuda", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Inicializar reproductor de audio
        reusableAudioCard = findViewById(R.id.reusableAudioCard);
        selectionInstructionText = findViewById(R.id.selectionInstructionText);

        // Inicializar sonidos de feedback
        initializeSoundPlayers();

        // ============================================================
        // CAMBIO: Inicializar componente de modal ANTES de Firebase
        // ============================================================
        modalAlertComponent = findViewById(R.id.modalAlertComponent);
        if (modalAlertComponent != null) {
            modalAlertComponent.setOnModalActionListener(new com.example.speak.components.ModalAlertComponent.OnModalActionListener() {
                @Override
                public void onContinuePressed(com.example.speak.components.ModalAlertComponent.ModalType type) {
                    advanceToNextQuestion();
                }

                @Override
                public void onModalHidden(com.example.speak.components.ModalAlertComponent.ModalType type) {
                    Log.d(TAG, "Modal hidden: " + type);
                }
            });
            Log.d(TAG, "ModalAlertComponent initialized successfully");
        } else {
            Log.e(TAG, "ModalAlertComponent is NULL - check XML layout");
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Check network status first
        isOfflineMode = !isNetworkAvailable();
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode);

        // Get user ID from SharedPreferences
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
                        //userId = guestCursor.getLong(guestCursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
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
            finish();
            return;
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTextToSpeechReady = false;
                } else {
                    textToSpeech.setSpeechRate(currentSpeed);
                    textToSpeech.setPitch(currentPitch);
                    isTextToSpeechReady = true;
                    Log.d(TAG, "TextToSpeech initialized successfully");

                    // Pasar el TextToSpeech al componente de audio
                    if (reusableAudioCard != null) {
                        reusableAudioCard.setTextToSpeech(textToSpeech);
                        Log.d(TAG, "TextToSpeech passed to ReusableAudioPlayerCard");
                    }

                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "TTS started: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS completed: " + utteranceId);
                            runOnUiThread(() -> {
                                if (!isPaused) {
                                    isPlaying = false;
                                    isPaused = false;
                                    currentPosition = 0;
                                    stopProgressUpdate();

                                    enableVisibleButtons();
                                    Log.d(TAG, "Botones habilitados después de completar la reproducción del audio");
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS error: " + utteranceId);
                        }
                    });
                }
            } else {
                isTextToSpeechReady = false;
            }
        });

        // Configurar componente reutilizable SOLO TTS
        setupReusableAudioCardForTTS();

        // Iniciar monitor para habilitar respuestas al comenzar la reproducción
        startReusablePlaybackMonitor();

        // Botones sin funcionalidad - solo visuales
        nextButton.setEnabled(false);

        // Configurar botón de retorno
        LinearLayout returnContainer = findViewById(R.id.returnContainer);
        if (returnContainer != null) {
            returnContainer.setOnClickListener(v -> {
                finish();
            });
        }

        // Validación previa para bloquear acceso si el tema anterior no ha sido aprobado
        if (!isPreviousTopicPassed(selectedTopic)) {
            Toast.makeText(this, "Debes completar el tema anterior antes de continuar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Inicializar sistema de comodines
        wildcardHelper = new WildcardHelper(this, "LISTENING", selectedTopic);
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

        // Inicializar contadores para la nueva sesión
        score = 0;
        evaluatedQuestionsCount = 0;
        currentQuestionIndex = 0;

        // Inicializar el timestamp de sesión
        sessionTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp);
        Log.d(TAG, "Contadores reiniciados - Score: " + score + ", Evaluadas: " + evaluatedQuestionsCount);
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setupReusableAudioCardForTTS() {
        try {
            if (reusableAudioCard != null) {
                String ttsText = "";
                if (currentQuestions != null && !currentQuestions.isEmpty() && currentQuestionIndex < currentQuestions.size()) {
                    ListeningQuestion q = currentQuestions.get(currentQuestionIndex);
                    ttsText = q.getQuestion();
                }

                reusableAudioCard.configure("", ttsText);

                android.view.View englishButton = reusableAudioCard.findViewById(R.id.languageEnglishButton);
                if (englishButton != null) {
                    englishButton.performClick();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up reusable audio card for TTS: " + e.getMessage(), e);
        }
    }

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
                            enableVisibleButtons();
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

    private boolean isPreviousTopicPassed(String currentTopic) {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        Log.d(TAG, "=== VERIFICACIÓN DE PROGRESO PREVIO ===");
        Log.d(TAG, "Tema actual a verificar: " + currentTopic);

        if ("NUMBERS".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_ALPHABET", false);
            int score = prefs.getInt("SCORE_ALPHABET", 0);

            Log.d(TAG, "Verificando progreso de ALPHABET:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ALPHABET");
            } else {
                Log.d(TAG, "✅ Tema ALPHABET completado correctamente");
            }

            return passed;
        }

        if ("COLORS".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_NUMBERS", false);
            int score = prefs.getInt("SCORE_NUMBERS", 0);

            Log.d(TAG, "Verificando progreso de NUMBERS:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema NUMBERS");
            } else {
                Log.d(TAG, "✅ Tema NUMBERS completado correctamente");
            }

            return passed;
        }

        if ("PERSONAL_PRONOUNS".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_COLORS", false);
            int score = prefs.getInt("SCORE_COLORS", 0);

            Log.d(TAG, "Verificando progreso de COLORS:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema COLORS");
            } else {
                Log.d(TAG, "✅ Tema COLORS completado correctamente");
            }

            return passed;
        }

        if ("POSSESSIVE ADJECTIVES".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false);
            int score = prefs.getInt("SCORE_PERSONAL_PRONOUNS", 0);

            Log.d(TAG, "Verificando progreso de PERSONAL PRONOUNS:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema PERSONAL PRONOUNS");
            } else {
                Log.d(TAG, "✅ Tema PERSONAL PRONOUNS completado correctamente");
            }

            return passed;
        }

        if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
            int score = prefs.getInt("SCORE_POSSESSIVE_ADJECTIVES", 0);

            Log.d(TAG, "Verificando progreso de POSSESSIVE ADJECTIVES:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema POSSESSIVE ADJECTIVES");
            } else {
                Log.d(TAG, "✅ Tema POSSESSIVE ADJECTIVES completado correctamente");
            }

            return passed;
        }

        if ("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false);
            int score = prefs.getInt("SCORE_PREPOSITIONS_OF_PLACE", 0);

            Log.d(TAG, "Verificando progreso de PREPOSITIONS OF PLACE:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema PREPOSITIONS OF PLACE");
            } else {
                Log.d(TAG, "✅ Tema PREPOSITIONS OF PLACE completado correctamente");
            }

            return passed;
        }

        if ("ORDINAL AND CARDINAL NUMBERS".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_ADJECTIVES", false);
            int score = prefs.getInt("SCORE_ADJECTIVES", 0);

            Log.d(TAG, "Verificando progreso de ADJECTIVES:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ADJECTIVES");
            } else {
                Log.d(TAG, "✅ Tema ADJECTIVES completado correctamente");
            }

            return passed;
        }

        if ("VERB TO BE".equalsIgnoreCase(currentTopic)) {
            boolean passed = prefs.getBoolean("PASSED_ORDINAL", false);
            int score = prefs.getInt("SCORE_ORDINAL", 0);

            Log.d(TAG, "Verificando progreso de ORDINAL:");
            Log.d(TAG, "Progreso guardado: " + passed);
            Log.d(TAG, "Puntuación guardada: " + score + "%");

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ORDINAL");
            } else {
                Log.d(TAG, "✅ Tema ORDINAL completado correctamente");
            }

            return passed;
        }

        Log.d(TAG, "No se requiere verificación de progreso previo para este tema");
        Log.d(TAG, "=== FIN DE VERIFICACIÓN DE PROGRESO ===");
        return true;
    }

    private void loadQuestionsFromFile(String topic, String level) {
        allQuestions = new ArrayList<>();
        try {
            AssetManager assetManager = getAssets();
            InputStream is = getAssets().open("SENA_Level_1_A1.1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            String currentQuestion = "";
            String currentAnswer = "";
            List<String> currentOptions = new ArrayList<>();
            String currentTopic = "";
            String currentLevel = "";
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
                } else if (line.startsWith("A:")) {
                    currentAnswer = line.substring(2).trim();
                } else if (line.startsWith("O:")) {
                    String[] options = line.substring(2).trim().split("\\|");
                    currentOptions.clear();
                    for (String option : options) {
                        currentOptions.add(option.trim());
                    }
                }

                if (isMatchingTopic && !currentQuestion.isEmpty() && !currentAnswer.isEmpty() && !currentOptions.isEmpty()) {
                    allQuestions.add(new ListeningQuestion(
                            currentQuestion,
                            currentAnswer,
                            currentOptions.toArray(new String[0]),
                            currentTopic,
                            currentLevel,
                            true,
                            ""
                    ));
                    currentQuestion = "";
                    currentAnswer = "";
                    currentOptions.clear();
                }
            }

            if (!currentQuestion.isEmpty() && !currentAnswer.isEmpty() && !currentOptions.isEmpty()) {
                if (currentTopic.equalsIgnoreCase(topic) && currentLevel.equalsIgnoreCase(level)) {
                    allQuestions.add(new ListeningQuestion(
                            currentQuestion,
                            currentAnswer,
                            currentOptions.toArray(new String[0]),
                            currentTopic,
                            currentLevel,
                            true,
                            ""
                    ));
                }
            }

            reader.close();
            is.close();

            Log.d(TAG, "Total questions loaded for topic " + topic + ": " + allQuestions.size());
            for (int i = 0; i < Math.min(3, allQuestions.size()); i++) {
                ListeningQuestion q = allQuestions.get(i);
                Log.d(TAG, "Question " + (i + 1) + ": " + q.getQuestion() + " | Answer: " + q.getCorrectAnswer());
            }

            Collections.shuffle(allQuestions);
            currentQuestions = new ArrayList<>(allQuestions.subList(0, Math.min(10, allQuestions.size())));

            Log.d(TAG, "Selected " + currentQuestions.size() + " questions for quiz");

            displayQuestion();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ListeningQuestion question = currentQuestions.get(currentQuestionIndex);
            questionNumberTextView.setText(String.format("%d/%d",
                    currentQuestionIndex + 1, currentQuestions.size()));

            if (reusableAudioCard != null) {
                try {
                    // Primero establecer el texto (actualiza duración en AudioPlayerView)
                    reusableAudioCard.setText(question.getQuestion());
                    // Asegurar que esté en modo inglés (TTS)
                    reusableAudioCard.setEnglishMode();
                    // Luego resetear el estado de reproducción (sin cambiar el texto)
                    reusableAudioCard.resetForNewQuestion();
                    Log.d(TAG, "Audio card configured for new question: " + question.getQuestion());
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing reusable audio card text: " + e.getMessage());
                }
            }

            if (shouldShowSpokenText(question.getTopic())) {
                String htmlText = "Texto a pronunciar <strong>/ Text to be spoken:</strong>";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    questionTextView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    questionTextView.setText(Html.fromHtml(htmlText));
                }
            } else {
                String htmlText = "Escuche el audio y seleccione la opción correcta <b>/ Listen to the audio and select the correct option</b>";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    questionTextView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    questionTextView.setText(Html.fromHtml(htmlText));
                }
            }

            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_test);
            }

            String[] options = question.getOptions();

            if (options.length > 0) {
                option1Button.setVisibility(View.VISIBLE);
                option1Button.setText(options[0]);
                option1Button.setEnabled(false);
            } else {
                option1Button.setVisibility(View.GONE);
            }

            if (options.length > 1) {
                option2Button.setVisibility(View.VISIBLE);
                option2Button.setText(options[1]);
                option2Button.setEnabled(false);
            } else {
                option2Button.setVisibility(View.GONE);
            }

            if (options.length > 2) {
                option3Button.setVisibility(View.VISIBLE);
                option3Button.setText(options[2]);
                option3Button.setEnabled(false);
            } else {
                option3Button.setVisibility(View.GONE);
            }

            if (options.length > 3) {
                option4Button.setVisibility(View.VISIBLE);
                option4Button.setText(options[3]);
                option4Button.setEnabled(false);
            } else {
                option4Button.setVisibility(View.GONE);
            }

            resetButtonStates();
            disableAllButtons();
            setupOptionButtonListeners();

            // Botones visibles pero sin funcionalidad
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setEnabled(false);
        } else {
            showResults();
            int finalScore = (int) ((score / (double) currentQuestions.size()) * 100);
            saveFinalScore(finalScore);
        }
    }

    private boolean shouldShowSpokenText(String topic) {
        if (topic.equals("ALPHABET") || topic.equals("NUMBERS") || topic.equals("COLORS")) {
            return false;
        }

        if (topic.equals("PERSONAL PRONOUNS") ||
                topic.equals("POSSESSIVE ADJECTIVES") ||
                topic.equals("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION") ||
                topic.equals("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)") ||
                topic.equals("ORDINAL AND CARDINAL NUMBERS") ||
                topic.equals("VERB TO BE") ||
                topic.equals("SIMPLE PRESENT") ||
                topic.equals("SIMPLE PRESENT THIRD PERSON") ||
                topic.equals("SIMPLE PAST") ||
                topic.equals("FREQUENCY ADVERBS") ||
                topic.equals("DAILY ROUTINES") ||
                topic.equals("COUNTABLE AND UNCOUNTABLE") ||
                topic.equals("QUANTIFIERS") ||
                topic.equals("PREPOSITIONS") ||
                topic.equals("USED TO")) {
            return true;
        }

        return false;
    }

    private void resetButtonStates() {
        if (option1Button.getVisibility() == View.VISIBLE) {
            option1Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        }
        if (option2Button.getVisibility() == View.VISIBLE) {
            option2Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        }
        if (option3Button.getVisibility() == View.VISIBLE) {
            option3Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        }
        if (option4Button.getVisibility() == View.VISIBLE) {
            option4Button.setBackgroundTintList(getColorStateList(R.color.header_blue));
        }
    }

    private void setupOptionButtonListeners() {
        option1Button.setOnClickListener(v -> checkAnswer(1));
        option2Button.setOnClickListener(v -> checkAnswer(2));
        option3Button.setOnClickListener(v -> checkAnswer(3));
        option4Button.setOnClickListener(v -> checkAnswer(4));
    }

    private void saveFinalScore(int score) {
        if (selectedTopic != null && selectedLevel != null) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            Log.d(TAG, "=== INICIO DE GUARDADO DE PROGRESO ===");
            Log.d(TAG, "Tema actual: " + selectedTopic);
            Log.d(TAG, "Nivel actual: " + selectedLevel);
            Log.d(TAG, "Puntuación recibida: " + score);

            if (score >= 70) {
                String[] eButtonStartTopics = {"ALPHABET", "NUMBERS", "COLORS", "PERSONAL PRONOUNS", "POSSESSIVE ADJECTIVES"};
                boolean isEButtonStartTopic = false;

                for (String topic : eButtonStartTopics) {
                    if (selectedTopic.equals(topic)) {
                        isEButtonStartTopic = true;
                        break;
                    }
                }

                if (isEButtonStartTopic) {
                    String progressKey = "PASSED_" + selectedTopic.toUpperCase().replace(" ", "_");
                    editor.putBoolean(progressKey, true);
                    Log.d(TAG, "✅ Progreso guardado - Clave: " + progressKey + " = true (Tema del eButtonStart)");
                } else {
                    Log.d(TAG, "ℹ️ Tema completado pero no es del eButtonStart: " + selectedTopic);
                }
            } else {
                Log.d(TAG, "❌ Puntuación insuficiente para desbloquear progreso: " + score + "%");
            }

            String scoreKey = "SCORE_" + selectedTopic.toUpperCase().replace(" ", "_");
            editor.putInt(scoreKey, score);
            Log.d(TAG, "Puntuación guardada - Clave: " + scoreKey + " = " + score);

            editor.apply();
            Log.d(TAG, "=== FIN DE GUARDADO DE PROGRESO ===");

            boolean finalProgress = prefs.getBoolean("PASSED_" + selectedTopic.toUpperCase().replace(" ", "_"), false);
            int finalScore = prefs.getInt(scoreKey, 0);
            Log.d(TAG, "Verificación final - Progreso: " + finalProgress + ", Puntuación: " + finalScore);

            if (finalScore >= 70) {
                com.example.speak.helpers.StarProgressHelper.addSessionPoints(this, 10);
            } else {
                Log.d(TAG, "Sesión NO aprobada (<70%). No se suman puntos ni se muestra estrella.");
            }
        } else {
            Log.e(TAG, "❌ Error: No se puede guardar el progreso - Tema o nivel es null");
            Log.e(TAG, "selectedTopic: " + selectedTopic);
            Log.e(TAG, "selectedLevel: " + selectedLevel);
        }
    }

    private void playCurrentQuestion() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ListeningQuestion question = currentQuestions.get(currentQuestionIndex);
            Log.d(TAG, "Playing question: " + question.getQuestion());

            startQuestionPlaybackFromPosition(0);

            boolean questionAnswered = nextButton.isEnabled();

            if (!questionAnswered) {
                enableVisibleButtons();
                Log.d(TAG, "Botones habilitados después de reproducir audio");
            } else {
                Log.d(TAG, "Pregunta ya respondida, no se habilitan los botones");
            }
        }
    }

    private void enableVisibleButtons() {
        if (option1Button.getVisibility() == View.VISIBLE) {
            option1Button.setEnabled(true);
        }
        if (option2Button.getVisibility() == View.VISIBLE) {
            option2Button.setEnabled(true);
        }
        if (option3Button.getVisibility() == View.VISIBLE) {
            option3Button.setEnabled(true);
        }
        if (option4Button.getVisibility() == View.VISIBLE) {
            option4Button.setEnabled(true);
        }
    }

    private void checkAnswer(int buttonIndex) {
        if (!option1Button.isEnabled() && !option2Button.isEnabled() &&
                !option3Button.isEnabled() && !option4Button.isEnabled()) {
            Log.d(TAG, "Pregunta ya respondida, no se puede volver a seleccionar");
            return;
        }

        Button selectedButton = null;
        String selectedAnswer = "";

        switch (buttonIndex) {
            case 1:
                selectedButton = option1Button;
                selectedAnswer = option1Button.getText().toString();
                break;
            case 2:
                selectedButton = option2Button;
                selectedAnswer = option2Button.getText().toString();
                break;
            case 3:
                selectedButton = option3Button;
                selectedAnswer = option3Button.getText().toString();
                break;
            case 4:
                selectedButton = option4Button;
                selectedAnswer = option4Button.getText().toString();
                break;
        }

        if (selectedButton == null) {
            Log.e(TAG, "Botón seleccionado es null");
            return;
        }

        ListeningQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
        boolean isCorrect = selectedAnswer.equals(currentQuestion.getCorrectAnswer());

        boolean wasQuestionChanged = wildcardHelper.wasQuestionChanged(currentQuestionIndex);

        Log.d(TAG, "=== VERIFICACIÓN DE RESPUESTA LISTENING ===");
        Log.d(TAG, "Pregunta: " + currentQuestion.getQuestion());
        Log.d(TAG, "Respuesta seleccionada: '" + selectedAnswer + "'");
        Log.d(TAG, "Respuesta correcta: '" + currentQuestion.getCorrectAnswer() + "'");
        Log.d(TAG, "¿Son iguales? " + isCorrect);
        Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged);

        if (!shouldShowSpokenText(currentQuestion.getTopic())) {
            questionTextView.setText("Texto hablado / Spoken text: " + currentQuestion.getQuestion());
        }

        saveAnswerToDatabase(currentQuestion, selectedAnswer, isCorrect);

        if (isCorrect) {
            if (!wasQuestionChanged) {
                score++;
                evaluatedQuestionsCount++;
                Log.d(TAG, "Puntos sumados: +1 (Total: " + score + ")");
                Log.d(TAG, "Preguntas evaluadas: " + evaluatedQuestionsCount);
            } else {
                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín");
            }

            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_ok);
            }
            playCorrectSound();

            // ============================================================
            // CAMBIO: Usar el componente para mostrar modal correcto
            // ============================================================
            if (modalAlertComponent != null) {
                modalAlertComponent.showCorrectModal(null, null);
                Log.d(TAG, "Correct modal shown via component");
            } else {
                Log.e(TAG, "modalAlertComponent is NULL!");
            }

        } else {
            if (!wasQuestionChanged) {
                evaluatedQuestionsCount++;
                Log.d(TAG, "Pregunta incorrecta evaluada. Preguntas evaluadas: " + evaluatedQuestionsCount);
            }

            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_bad);
            }
            playIncorrectSound();

            // ============================================================
            // CAMBIO: Usar el componente para mostrar modal incorrecto
            // ============================================================
            if (modalAlertComponent != null) {
                modalAlertComponent.showIncorrectModal(null, null);
                Log.d(TAG, "Incorrect modal shown via component");
            } else {
                Log.e(TAG, "modalAlertComponent is NULL!");
            }
        }

        highlightButtons(buttonIndex, isCorrect, currentQuestion.getCorrectAnswer());

        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);

        String feedback;
        if (isCorrect) {
            if (wasQuestionChanged) {
                feedback = "¡Correcto! (Pregunta cambiada por comodín - No suma puntos)";
            } else {
                feedback = "¡Correcto!";
            }
        } else {
            feedback = "Incorrecto. La respuesta correcta es: " + currentQuestion.getCorrectAnswer();
        }

        if (!wasQuestionChanged) {
            feedback += "\nProgreso: " + score + "/" + evaluatedQuestionsCount + " preguntas evaluadas";
        }

        // Botón sin funcionalidad - permanece deshabilitado
        // nextButton.setEnabled(true);
    }

    /**
     * Método para avanzar a la siguiente pregunta de forma segura
     */
    private void advanceToNextQuestion() {
        try {
            if (currentQuestionIndex + 1 < currentQuestions.size()) {
                currentQuestionIndex++;
                displayQuestion();
                Log.d(TAG, "Advanced to next question: " + (currentQuestionIndex + 1) + "/" + currentQuestions.size());
            } else {
                Log.d(TAG, "No more questions, showing results");
                showResults();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error advancing to next question: " + e.getMessage(), e);
        }
    }

    private void highlightButtons(int selectedOption, boolean isCorrect, String correctAnswer) {
        String[] options = {
                option1Button.getVisibility() == View.VISIBLE ? option1Button.getText().toString() : "",
                option2Button.getVisibility() == View.VISIBLE ? option2Button.getText().toString() : "",
                option3Button.getVisibility() == View.VISIBLE ? option3Button.getText().toString() : "",
                option4Button.getVisibility() == View.VISIBLE ? option4Button.getText().toString() : ""
        };

        int correctIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(correctAnswer)) {
                correctIndex = i + 1;
                break;
            }
        }

        if (isCorrect) {
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.verdeSena));
        } else {
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
            if (correctIndex != selectedOption && correctIndex != -1) {
                getButtonByIndex(correctIndex).setBackgroundTintList(getColorStateList(R.color.verdeSena));
            }
        }

        Log.d(TAG, "Botones resaltados - Seleccionado: " + selectedOption + ", Correcto: " + correctIndex + ", ¿Es correcto?: " + isCorrect);
    }

    private Button getButtonByIndex(int index) {
        switch (index) {
            case 1:
                return option1Button;
            case 2:
                return option2Button;
            case 3:
                return option3Button;
            case 4:
                return option4Button;
            default:
                return option1Button;
        }
    }

    private void disableAllButtons() {
        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);
    }

    private void saveAnswerToDatabase(ListeningQuestion question, String selectedAnswer, boolean isCorrect) {
        try {
            dbHelper.saveQuizResult(
                    userId,
                    question.getQuestion(),
                    question.getCorrectAnswer(),
                    selectedAnswer,
                    isCorrect,
                    "Listening",
                    question.getTopic(),
                    question.getLevel(),
                    sessionTimestamp
            );
            Log.d(TAG, "Answer saved to quiz_results table successfully with session timestamp: " + sessionTimestamp);

            if (!isOfflineMode && mAuth.getCurrentUser() != null) {
                String questionId = String.valueOf(currentQuestionIndex);
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("question", question.getQuestion());
                answerData.put("correctAnswer", question.getCorrectAnswer());
                answerData.put("selectedAnswer", selectedAnswer);
                answerData.put("isCorrect", isCorrect);
                answerData.put("timestamp", sessionTimestamp);
                answerData.put("speed", currentSpeed);
                answerData.put("pitch", currentPitch);
                answerData.put("quizType", "Listening");
                answerData.put("topic", question.getTopic());
                answerData.put("level", question.getLevel());

                mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                        .child("quiz_results")
                        .child(questionId)
                        .setValue(answerData);
                Log.d(TAG, "Answer saved to Firebase successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving answer: " + e.getMessage());
        }
    }

    private void showResults() {
        int finalScore;
        if (evaluatedQuestionsCount > 0) {
            finalScore = (int) ((score / (double) evaluatedQuestionsCount) * 100);
            Log.d(TAG, "=== CÁLCULO DE PUNTAJE FINAL ===");
            Log.d(TAG, "Puntaje obtenido: " + score);
            Log.d(TAG, "Preguntas evaluadas: " + evaluatedQuestionsCount);
            Log.d(TAG, "Preguntas totales: " + currentQuestions.size());
            Log.d(TAG, "Preguntas cambiadas por comodín: " + (currentQuestions.size() - evaluatedQuestionsCount));
            Log.d(TAG, "Puntaje final calculado: " + finalScore + "%");
        } else {
            finalScore = 0;
            Log.d(TAG, "No hay preguntas evaluadas, puntaje final: 0%");
        }

        if (selectedTopic != null && selectedTopic.equals("ALPHABET") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_ALPHABET", true);
            editor.apply();
            Log.d(TAG, "ALPHABET passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("NUMBERS") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_NUMBERS", true);
            editor.apply();
            Log.d(TAG, "NUMBERS passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("COLORS") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_COLORS", true);
            editor.apply();
            Log.d(TAG, "COLORS passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("PERSONAL PRONOUNS") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_PERSONAL_PRONOUNS", true);
            editor.apply();
            Log.d(TAG, "PERSONAL PRONOUNS passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("POSSESSIVE ADJECTIVES") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_POSSESSIVE_ADJECTIVES", true);
            editor.apply();
            Log.d(TAG, "POSSESSIVE ADJECTIVES passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_PREPOSITIONS_OF_PLACE", true);
            editor.apply();
            Log.d(TAG, "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_ADJECTIVES", true);
            editor.apply();
            Log.d(TAG, "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY) passed with score: " + finalScore);
        }

        if (selectedTopic != null && selectedTopic.equals("ORDINAL AND CARDINAL NUMBERS") && finalScore >= 70) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("PASSED_ORDINAL", true);
            editor.apply();
            Log.d(TAG, "ORDINAL AND CARDINAL NUMBERS passed with score: " + finalScore);
        }

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

        ImageView birdImageView = dialogView.findViewById(R.id.birdImageView);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        TextView counterTextView = dialogView.findViewById(R.id.counterTextView);
        TextView scoreTextView = dialogView.findViewById(R.id.scoreTextView);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        TextView btnReintentar = dialogView.findViewById(R.id.btnReintentar);
        LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

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
        } else {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("1/10");
            birdImageView.setImageResource(R.drawable.crab_bad);
        }

        String resultText = "Score: " + finalScore + "%";

        int questionsChanged = currentQuestions.size() - evaluatedQuestionsCount;
        if (questionsChanged > 0) {
            resultText += "\n\n📊 Detalles:";
            resultText += "\n• Preguntas evaluadas: " + evaluatedQuestionsCount + "/" + currentQuestions.size();
            resultText += "\n• Preguntas cambiadas por comodín: " + questionsChanged;
            resultText += "\n• Puntaje basado solo en preguntas evaluadas";
        }

        scoreTextView.setText(resultText);

        final String finalSelectedTopic = selectedTopic;
        final String finalSelectedLevel = selectedLevel;
        final int finalScoreForLambda = finalScore;

        // Si el score es menor a 70%, mostrar "Try Again" en el botón principal
        if (finalScore < 70) {
            btnContinue.setText("Try Again");
            btnContinue.setVisibility(View.VISIBLE);
            btnReintentar.setVisibility(View.GONE);

            btnContinue.setOnClickListener(v -> {
                // Reiniciar la misma actividad
                Intent intent = new Intent(this, ListeningActivity.class);
                intent.putExtra("TOPIC", finalSelectedTopic);
                intent.putExtra("LEVEL", finalSelectedLevel);
                startActivity(intent);
                finish();
            });
        }
        // Si el score es >= 70%, mostrar "Continue"
        else if (finalScore >= 70) {
            btnContinue.setText(ProgressionHelper.getContinueButtonTextEnhanced(this, selectedTopic));
            btnContinue.setVisibility(View.VISIBLE);
            btnReintentar.setVisibility(View.GONE);

            btnContinue.setOnClickListener(v -> {
                ProgressionHelper.markTopicCompleted(this, finalSelectedTopic, finalScoreForLambda);

                Intent continueIntent = ProgressionHelper.createContinueIntent(this, finalSelectedTopic, "listening");
                if (continueIntent != null) {
                    startActivity(continueIntent);
                    finish();
                } else {
                    Intent intent = new Intent(this, MenuA1Activity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        }

        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizHistoryActivity.class);
            intent.putExtra("SCORE", score);
            intent.putExtra("TOTAL_QUESTIONS", currentQuestions.size());
            intent.putExtra("QUIZ_TYPE", "Listening");
            intent.putExtra("TOPIC", finalSelectedTopic);
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
            startActivity(intent);
            finish();
        });

        final int finalScoreCaptured = finalScore;
        if (finalScoreCaptured >= 70) {
            new android.os.Handler().postDelayed(() -> {
                try {
                    StarEarnedDialog.show(ListeningActivity.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.getMessage());
                }
            }, 200);
        }
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

    @Override
    protected void onDestroy() {
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

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (correctSoundPlayer != null) {
            correctSoundPlayer.release();
            correctSoundPlayer = null;
        }

        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer.release();
            incorrectSoundPlayer = null;
        }

        stopProgressUpdate();

        super.onDestroy();
    }

    // ===== SISTEMA DE COMODINES =====

    private void showWildcardMenu() {
        if (wildcardHelper != null) {
            wildcardHelper.showWildcardMenu();
        }
    }

    private void showHelp() {
        Intent helpIntent = new Intent(this, HelpActivity.class);
        helpIntent.putExtra("topic", selectedTopic);
        helpIntent.putExtra("level", selectedLevel);
        startActivity(helpIntent);
    }

    private void changeCurrentQuestion() {
        if (currentQuestions.size() > 1) {
            wildcardHelper.markQuestionAsChanged(currentQuestionIndex);

            int newIndex = (currentQuestionIndex + 1) % currentQuestions.size();
            currentQuestionIndex = newIndex;

            displayQuestion();

            Toast.makeText(this, "Pregunta cambiada - La anterior no se evaluará", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay más preguntas disponibles", Toast.LENGTH_SHORT).show();
        }
    }

    private void showContentImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Imagen de Contenido");
        builder.setMessage("Aquí se mostraría una imagen relacionada con: " + selectedTopic);
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }

    private void showInstructorVideo() {
        if (wildcardHelper != null && wildcardHelper.getVideoHelper() != null) {
            wildcardHelper.getVideoHelper().showInstructorVideo(selectedTopic);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Video del Instructor");
            builder.setMessage("Aquí se reproduciría un video explicativo sobre: " + selectedTopic);
            builder.setPositiveButton("Entendido", null);
            builder.show();
        }
    }

    private void applyFiftyFifty() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ListeningQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            String[] allOptions = currentQuestion.getOptions();
            String correctAnswer = currentQuestion.getCorrectAnswer();

            List<String> remainingOptions = wildcardHelper.applyFiftyFifty(
                    java.util.Arrays.asList(allOptions), correctAnswer);

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

    private void showCreativeHelp() {
        if (currentQuestionIndex < currentQuestions.size()) {
            ListeningQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
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

    private void showWildcardUsageInfo() {
        int questionsChanged = currentQuestions.size() - evaluatedQuestionsCount;
        int wildcardsUsed = questionsChanged;

        StringBuilder info = new StringBuilder();
        info.append("📊 Información de Comodines\n\n");
        info.append("• Preguntas totales: ").append(currentQuestions.size()).append("\n");
        info.append("• Preguntas evaluadas: ").append(evaluatedQuestionsCount).append("\n");
        info.append("• Preguntas cambiadas: ").append(questionsChanged).append("\n");
        info.append("• Comodines utilizados: ").append(wildcardsUsed).append("\n\n");

        if (questionsChanged > 0) {
            info.append("ℹ️ Las preguntas cambiadas por comodín no afectan tu puntuación final.\n");
            info.append("Tu puntuación se calcula solo con las preguntas que realmente evaluaste.");
        } else {
            info.append("✅ No se han usado comodines en esta sesión.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎯 Uso de Comodines");
        builder.setMessage(info.toString());
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }

    private void startQuestionPlaybackFromPosition(long position) {
        Log.d(TAG, "Starting question playback from position: " + position + "ms");

        if (textToSpeech == null) {
            Log.e(TAG, "TextToSpeech is null");
            return;
        }

        if (textToSpeech.getLanguage() == null) {
            Log.e(TAG, "Language not set");
            return;
        }

        long remainingTime = totalDuration - position;
        if (remainingTime <= 0) {
            position = 0;
            remainingTime = totalDuration;
        }

        if (currentQuestionIndex < currentQuestions.size()) {
            ListeningQuestion currentQuestion = currentQuestions.get(currentQuestionIndex);
            String textToSpeak = currentQuestion.getQuestion();

            Log.d(TAG, "Text to speak: " + textToSpeak);

            int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "QuestionAudio");
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text");
            } else {
                Log.d(TAG, "Question audio playback started from position: " + position + "ms");

                isPlaying = true;
                isPaused = false;
                startTime = System.currentTimeMillis() - position;
                currentPosition = position;

                enableVisibleButtons();
                Log.d(TAG, "Botones habilitados después de reproducir audio desde el panel de control");
            }
        }
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}