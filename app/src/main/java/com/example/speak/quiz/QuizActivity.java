package com.example.speak.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.speak.R;
import com.example.speak.QuizHistoryActivity;
import com.example.speak.database.DatabaseHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.speak.MainActivity;
import com.example.speak.ProgressionHelper;
import com.example.speak.MenuA1Activity;

import com.example.speak.components.ModalAlertComponent;
import com.example.speak.helpers.ModalAnimationHelper;

// Actividad principal del quiz de preguntas tipo test
public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    // Variables para la lógica del test
    private QuestionGenerator questionGenerator;
    private List<QuestionGenerator.Question> questions;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private DatabaseHelper dbHelper;
    private boolean isOfflineMode = false;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private long sessionTimestamp; // Agregar esta variable como campo de la clase

    // Nuevas variables para el sistema de progresión
    private String currentLevel = "A1.1";
    private Map<String, Integer> topicScores = new HashMap<>();
    private Map<String, List<QuestionGenerator.Question>> questionsByTopic = new HashMap<>();
    private List<String> currentLevelTopics = new ArrayList<>();
    private int questionsPerTopic = 5;
    private int totalTopicsInLevel = 0;
    private int completedTopics = 0;

    // Referencias a los elementos visuales (UI)
    private TextView scoreTextView;
    private TextView questionTextView;
    private TextView levelTextView;
    private TextView topicTextView;
    private TextView questionNumberTextView;
    private LinearLayout optionsRadioGroup;
    private Button nextButton;
    private CardView questionCard;
    private ImageView birdImageView;

    // Botones de opciones
    private Button option1Button;
    private Button option2Button;
    private Button option3Button;
    private Button option4Button;

    // MediaPlayer para sonidos de feedback
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    //Login
    private String userEmail;
    private long userId;

    // Parámetros de entrada
    private String selectedTopic;
    private String selectedLevel;

    //Return Menú
    private ImageView eBtnReturnMenu;

    // Componente reutilizable de modal de alerta
    private ModalAlertComponent modalAlertComponent;

    // Comentamos estas variables porque no existen en el layout
    // private TextView topicTextView;
    // private TextView levelTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inicializar vistas
        scoreTextView = findViewById(R.id.scoreTextView);
        questionTextView = findViewById(R.id.questionTextView);
        levelTextView = findViewById(R.id.levelTextView);
        topicTextView = findViewById(R.id.topicTextView);
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup);
        nextButton = findViewById(R.id.nextButton);
        questionCard = findViewById(R.id.questionCard);
        birdImageView = findViewById(R.id.birdImageView);
        //messageTextView = findViewById(R.id.messageTextView);
        //counterTextView = findViewById(R.id.counterTextView);

        // Inicializar botones de opciones
        option1Button = findViewById(R.id.option1RadioButton);
        option2Button = findViewById(R.id.option2RadioButton);
        option3Button = findViewById(R.id.option3RadioButton);
        option4Button = findViewById(R.id.option4RadioButton);

        // Inicializar sonidos de feedback
        initializeSoundPlayers();

        // Inicializar componente de modal
        modalAlertComponent = findViewById(R.id.modalAlertComponent);
        if (modalAlertComponent != null) {
            modalAlertComponent.setOnModalActionListener(new ModalAlertComponent.OnModalActionListener() {
                @Override
                public void onContinuePressed(ModalAlertComponent.ModalType type) {
                    advanceToNextQuestion();
                }

                @Override
                public void onModalHidden(ModalAlertComponent.ModalType type) {
                    Log.d(TAG, "Modal hidden: " + type);
                }
            });
        }

        // Recibir los parámetros enviados desde el menú (opcional)
        Intent intent = getIntent();
        selectedTopic = intent.getStringExtra("TOPIC");
        selectedLevel = intent.getStringExtra("LEVEL");

        // Nota: El topic y level se actualizarán dinámicamente con cada pregunta
        // Los valores iniciales se mostrarán cuando se cargue la primera pregunta

        // Inicializar generador de preguntas y base de datos
        questionGenerator = new QuestionGenerator(this);
        dbHelper = new DatabaseHelper(this);

        // Verificar si es usuario invitado
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);
        userId = prefs.getLong("user_id", -1);

        if (!isGuest) {
            // Si no es invitado, intentar obtener el email
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            userEmail = getSharedPreferences("SpeakApp", MODE_PRIVATE)
                    .getString("user_email", null);
        }
        if (userEmail != null) {
            userId = dbHelper.getUserId(userEmail);
            }
        }

        if (userId == -1) {
            showMessage("No se pudo obtener el usuario", false);
            finish();
            return;
        }

        // Inicializar el timestamp de sesión
        sessionTimestamp = System.currentTimeMillis();

        // Configurar temas por nivel
        setupLevelTopics();

        // Generar preguntas por tema y nivel
        generateQuestionsByTopicAndLevel();

        // Verificar si se generaron preguntas
        if (questions.isEmpty()) {
            Log.e(TAG, "No se generaron preguntas. Verificando archivo de preguntas...");
            Toast.makeText(this, "Error: No se encontraron preguntas para el nivel " + currentLevel, Toast.LENGTH_LONG).show();

            // Intentar generar preguntas con el método original como fallback
            questions = questionGenerator.generateQuestions(10);
            if (questions.isEmpty()) {
                Toast.makeText(this, "Error crítico: No se pueden cargar preguntas. Verifica el archivo de preguntas.", Toast.LENGTH_LONG).show();
                finish();
                return;
            } else {
                Log.d(TAG, "Usando preguntas de fallback: " + questions.size());
            }
        }

        Log.d(TAG, "Preguntas generadas exitosamente: " + questions.size());

        // Botones decorativos sin funcionalidad
        nextButton.setEnabled(false);

        // Mostrar primera pregunta
        showQuestion();

        //Return Menu - ahora está dentro del returnContainer
        LinearLayout returnContainer = findViewById(R.id.returnContainer);
        if (returnContainer != null) {
            returnContainer.setOnClickListener(v -> {
                ReturnMenu();
            });
            }
    }

    // Configurar temas por nivel
    private void setupLevelTopics() {
        switch (currentLevel) {
            case "A1.1":
                currentLevelTopics.add("ALPHABET");
                currentLevelTopics.add("NUMBERS");
                currentLevelTopics.add("COLORS");
                currentLevelTopics.add("PERSONAL PRONOUNS");
                currentLevelTopics.add("POSSESSIVE ADJECTIVES");
                currentLevelTopics.add("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                currentLevelTopics.add("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
                break;
            case "A1.2":
                currentLevelTopics.add("VERBS TO BE");
                currentLevelTopics.add("PRESENT SIMPLE");
                currentLevelTopics.add("DAILY ROUTINES");
                currentLevelTopics.add("FOOD AND DRINKS");
                currentLevelTopics.add("FAMILY MEMBERS");
                break;
            case "A2.1":
                currentLevelTopics.add("PAST SIMPLE");
                currentLevelTopics.add("IRREGULAR VERBS");
                currentLevelTopics.add("HOBBIES AND INTERESTS");
                currentLevelTopics.add("WEATHER AND SEASONS");
                currentLevelTopics.add("TRAVEL AND TRANSPORTATION");
                break;
            default:
                // Nivel por defecto A1.1
                currentLevelTopics.add("ALPHABET");
                currentLevelTopics.add("NUMBERS");
                currentLevelTopics.add("COLORS");
                currentLevelTopics.add("PERSONAL PRONOUNS");
                currentLevelTopics.add("POSSESSIVE ADJECTIVES");
                currentLevelTopics.add("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                currentLevelTopics.add("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
                break;
        }
        totalTopicsInLevel = currentLevelTopics.size();
        Log.d(TAG, "Configurados " + totalTopicsInLevel + " temas para el nivel " + currentLevel);
    }

    // Generar preguntas por tema y nivel
    private void generateQuestionsByTopicAndLevel() {
        questions = new ArrayList<>();

        // Obtener todas las preguntas disponibles
        List<QuestionGenerator.Question> allQuestions = questionGenerator.getAllQuestions();
        Log.d(TAG, "Total de preguntas disponibles en el archivo: " + allQuestions.size());

        // Mostrar algunas preguntas para debugging
        for (int i = 0; i < Math.min(3, allQuestions.size()); i++) {
            QuestionGenerator.Question q = allQuestions.get(i);
            Log.d(TAG, "Pregunta " + i + ": Topic='" + q.getTopic() + "', Level='" + q.getLevel() + "'");
        }

        // Filtrar preguntas por nivel y tema
        for (String topic : currentLevelTopics) {
            List<QuestionGenerator.Question> topicQuestions = new ArrayList<>();

            for (QuestionGenerator.Question question : allQuestions) {
                if (question.getTopic().equalsIgnoreCase(topic) &&
                    question.getLevel().equalsIgnoreCase(currentLevel)) {
                    topicQuestions.add(question);
                }
            }

            Log.d(TAG, "Tema '" + topic + "' en nivel '" + currentLevel + "': " + topicQuestions.size() + " preguntas encontradas");

            // Si no hay preguntas para este tema en este nivel, buscar en otros niveles
            if (topicQuestions.isEmpty()) {
                Log.d(TAG, "Buscando tema '" + topic + "' en otros niveles...");
                for (QuestionGenerator.Question question : allQuestions) {
                    if (question.getTopic().equalsIgnoreCase(topic)) {
                        topicQuestions.add(question);
                    }
                }
                Log.d(TAG, "Tema '" + topic + "' en cualquier nivel: " + topicQuestions.size() + " preguntas encontradas");
            }

            // Seleccionar 5 preguntas aleatorias por tema
            if (!topicQuestions.isEmpty()) {
                Collections.shuffle(topicQuestions);
                int questionsToAdd = Math.min(questionsPerTopic, topicQuestions.size());
                List<QuestionGenerator.Question> selectedQuestions = topicQuestions.subList(0, questionsToAdd);

                // Agregar las preguntas en orden (no mezclar)
                questions.addAll(selectedQuestions);
                questionsByTopic.put(topic, selectedQuestions);
                Log.d(TAG, "Agregadas " + questionsToAdd + " preguntas para el tema: " + topic);
            } else {
                Log.w(TAG, "No se encontraron preguntas para el tema: " + topic);
            }
        }

        // NO mezclar todas las preguntas para mantener orden por temas
        // Collections.shuffle(questions); // Comentado para mantener orden por temas

        Log.d(TAG, "Total de preguntas generadas: " + questions.size());
        Log.d(TAG, "Preguntas por tema: " + questionsByTopic.size());

        if (questions.isEmpty()) {
            Log.e(TAG, "CRÍTICO: No se generaron preguntas. Verificando archivo...");
            // Mostrar todos los temas disponibles en el archivo
            Set<String> availableTopics = new HashSet<>();
            Set<String> availableLevels = new HashSet<>();
            for (QuestionGenerator.Question q : allQuestions) {
                availableTopics.add(q.getTopic());
                availableLevels.add(q.getLevel());
            }
            Log.e(TAG, "Temas disponibles en archivo: " + availableTopics);
            Log.e(TAG, "Niveles disponibles en archivo: " + availableLevels);
        }
    }

    // Muestra la pregunta actual en pantalla
    private void showQuestion() {
        if (currentQuestionIndex < questions.size()) {
            QuestionGenerator.Question question = questions.get(currentQuestionIndex);
            questionTextView.setText(question.getQuestion());

            // Actualizar topic y level específicos de esta pregunta
            if (levelTextView != null) {
                levelTextView.setText("Level: " + question.getLevel());
            }
            if (topicTextView != null) {
                topicTextView.setText("Topic: " + question.getTopic());
            }

            // Actualizar número de pregunta
            if (questionNumberTextView != null) {
                questionNumberTextView.setText(String.format("%d/%d",
                    currentQuestionIndex + 1, questions.size()));
            }

            // Resetear la imagen del pájaro a la imagen por defecto
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_test);
            }

            // Configurar opciones en los botones
            List<String> optionsList = question.getOptions();
            String[] options = optionsList.toArray(new String[0]);

            // Configurar botones con opciones
            if (options.length > 0) {
                option1Button.setVisibility(View.VISIBLE);
                option1Button.setText(options[0]);
                //option1Button.setEnabled(true);
                // Solo habilitar si es una nueva pregunta (nextButton deshabilitado)
                option1Button.setEnabled(!nextButton.isEnabled());
            } else {
                option1Button.setVisibility(View.GONE);
            }

            if (options.length > 1) {
                option2Button.setVisibility(View.VISIBLE);
                option2Button.setText(options[1]);
                //option2Button.setEnabled(true);
                // Solo habilitar si es una nueva pregunta (nextButton deshabilitado)
                option2Button.setEnabled(!nextButton.isEnabled());
            } else {
                option2Button.setVisibility(View.GONE);
            }

            if (options.length > 2) {
                option3Button.setVisibility(View.VISIBLE);
                option3Button.setText(options[2]);
                //option3Button.setEnabled(true);
                // Solo habilitar si es una nueva pregunta (nextButton deshabilitado)
                option3Button.setEnabled(!nextButton.isEnabled());
            } else {
                option3Button.setVisibility(View.GONE);
            }

            if (options.length > 3) {
                option4Button.setVisibility(View.VISIBLE);
                option4Button.setText(options[3]);
                //option4Button.setEnabled(true);
                // Solo habilitar si es una nueva pregunta (nextButton deshabilitado)
                option4Button.setEnabled(!nextButton.isEnabled());
            } else {
                option4Button.setVisibility(View.GONE);
            }

            // Reset button states (colors)
            resetButtonStates();

            // Configurar los listeners para evaluación automática
            setupOptionButtonListeners();

            // Botones decorativos - ambos visibles pero deshabilitados
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setEnabled(false);

            updateScore(); // Actualiza la puntuación mostrada
        } else {
            showFinalScore(); // Si no hay más preguntas, muestra el resultado final
        }
    }

    // Método para resetear el estado de los botones (solo colores)
    private void resetButtonStates() {
        // Reset button colors only
        int customColor = Color.parseColor("#1A4F7C");
        if (option1Button.getVisibility() == View.VISIBLE) {
            option1Button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customColor));
        }
        if (option2Button.getVisibility() == View.VISIBLE) {
            option2Button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customColor));
        }
        if (option3Button.getVisibility() == View.VISIBLE) {
            option3Button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customColor));
        }
        if (option4Button.getVisibility() == View.VISIBLE) {
            option4Button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customColor));
        }
    }

    // Configurar listeners para los botones de opciones
    private void setupOptionButtonListeners() {
        option1Button.setOnClickListener(v -> checkAnswer(1));
        option2Button.setOnClickListener(v -> checkAnswer(2));
        option3Button.setOnClickListener(v -> checkAnswer(3));
        option4Button.setOnClickListener(v -> checkAnswer(4));
    }

    // Comprueba si la respuesta seleccionada es correcta
    private void checkAnswer(int buttonIndex) {
        // Verificar si ya se respondió esta pregunta
        if (!option1Button.isEnabled() && !option2Button.isEnabled() &&
            !option3Button.isEnabled() && !option4Button.isEnabled()) {
            Log.d(TAG, "Pregunta ya respondida, no se puede volver a seleccionar");
            return;
        }

        // Obtiene la respuesta seleccionada por el usuario basada en el índice del botón
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

        QuestionGenerator.Question currentQuestion = questions.get(currentQuestionIndex);

        // Compara la respuesta seleccionada con la correcta
        boolean isCorrect = selectedAnswer.equals(currentQuestion.getCorrectAnswer());

        // Logging detallado para debugging
        Log.d(TAG, "=== VERIFICACIÓN DE RESPUESTA QUIZ ===");
        Log.d(TAG, "Pregunta: " + currentQuestion.getQuestion());
        Log.d(TAG, "Respuesta seleccionada: '" + selectedAnswer + "'");
        Log.d(TAG, "Respuesta correcta: '" + currentQuestion.getCorrectAnswer() + "'");
        Log.d(TAG, "¿Son iguales? " + isCorrect);

        // Save individual answer to database
        saveAnswerToDatabase(currentQuestion, selectedAnswer, isCorrect);

        if (isCorrect) {
            correctAnswers++;
            // Cambiar la imagen del pájaro a la imagen de correcto
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_ok);
            }
            // Reproducir sonido de éxito
            playCorrectSound();
            Log.d(TAG, "✅ Respuesta CORRECTA - Puntuación: " + correctAnswers);

            // MOSTRAR MODAL DE RESPUESTA CORRECTA
            if (modalAlertComponent != null) {
                modalAlertComponent.showCorrectModal(null, null);
            }

        } else {
            // Cambiar la imagen del pájaro a la imagen de incorrecto
            if (birdImageView != null) {
                birdImageView.setImageResource(R.drawable.crab_bad);
            }
            // Reproducir sonido de error
            playIncorrectSound();
            Log.d(TAG, "❌ Respuesta INCORRECTA - Puntuación: " + correctAnswers);

            // MOSTRAR MODAL DE RESPUESTA INCORRECTA
            if (modalAlertComponent != null) {
                modalAlertComponent.showIncorrectModal(null, null);
            }

        }

        // Highlight buttons with correct colors (like ListeningActivity)
        highlightButtons(buttonIndex, isCorrect, currentQuestion.getCorrectAnswer());

        // Disable all buttons to prevent multiple selections
        option1Button.setEnabled(false);
        option2Button.setEnabled(false);
        option3Button.setEnabled(false);
        option4Button.setEnabled(false);

        // Los botones next y submit son decorativos, no se habilitan
        // nextButton.setEnabled(true); // Comentado - solo decorativo

        // Muestra mensaje de éxito o error
        //showMessage(isCorrect ? "¡Correcto!" : "Incorrecto. La respuesta correcta era: " +
                  //currentQuestion.getCorrectAnswer(), isCorrect);
    }

    // Método para resaltar los botones con colores (similar a ListeningActivity)
    private void highlightButtons(int selectedOption, boolean isCorrect, String correctAnswer) {
        // Get all option texts
        String[] options = {
            option1Button.getVisibility() == View.VISIBLE ? option1Button.getText().toString() : "",
            option2Button.getVisibility() == View.VISIBLE ? option2Button.getText().toString() : "",
            option3Button.getVisibility() == View.VISIBLE ? option3Button.getText().toString() : "",
            option4Button.getVisibility() == View.VISIBLE ? option4Button.getText().toString() : ""
        };

        // Find correct answer index
        int correctIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(correctAnswer)) {
                correctIndex = i + 1;
                break;
            }
        }

        // Apply colors (same logic as ListeningActivity)
        if (isCorrect) {
            // Selected answer is correct - show green
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.verdeSena));
        } else {
            // Selected answer is wrong - show red for selected, green for correct
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
            if (correctIndex != selectedOption && correctIndex != -1) {
                getButtonByIndex(correctIndex).setBackgroundTintList(getColorStateList(R.color.verdeSena));
            }
        }

        Log.d(TAG, "Botones resaltados - Seleccionado: " + selectedOption + ", Correcto: " + correctIndex + ", ¿Es correcto?: " + isCorrect);
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

    private void saveAnswerToDatabase(QuestionGenerator.Question question, String selectedAnswer, boolean isCorrect) {
        try {
            // Save to local database using quiz_results table
            dbHelper.saveQuizResult(
                userId,
                question.getQuestion(),
                question.getCorrectAnswer(),
                selectedAnswer,
                isCorrect,
                "Quiz",
                question.getTopic(),
                question.getLevel(),
                sessionTimestamp  // Agregar el timestamp de sesión
            );
            Log.d(TAG, "Answer saved to quiz_results table successfully");

            // Save to Firebase if online and user is authenticated
            if (!isOfflineMode && mAuth.getCurrentUser() != null) {
                String questionId = String.valueOf(currentQuestionIndex);
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("question", question.getQuestion());
                answerData.put("correctAnswer", question.getCorrectAnswer());
                answerData.put("selectedAnswer", selectedAnswer);
                answerData.put("isCorrect", isCorrect);
                answerData.put("timestamp", sessionTimestamp);  // Usar el timestamp de sesión
                answerData.put("quizType", "Quiz");
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
            Toast.makeText(this, "Error saving answer", Toast.LENGTH_SHORT).show();
        }
    }

    // Pasa a la siguiente pregunta
    private void showNextQuestion() {
        currentQuestionIndex++;
        showQuestion();
    }

    // Actualiza la puntuación en pantalla
    private void updateScore() {
        int score = (int) ((correctAnswers / (double) currentQuestionIndex) * 100);
        scoreTextView.setText(score + "%");
    }

    // Muestra la puntuación final cuando se acaba el test
    private void showFinalScore() {
        // Calculate final score percentage
        int finalScore = (int) ((correctAnswers / (double) questions.size()) * 100);

        // Calcular puntuación por tema
        calculateTopicScores();

        // Verificar progreso por tema y nivel
        checkLevelProgress();

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

        // Set bird image based on score
        if (finalScore >= 100) {
            messageTextView.setText("Excellent your English is getting better!");
            counterTextView.setText("5/5");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 90) {
            messageTextView.setText("Good, but you can do it better!");
            counterTextView.setText("4/5");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 80) {
            messageTextView.setText("Good, but you can do it better!");
            counterTextView.setText("4/5");
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 69) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("3/5");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 60) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("3/5");
            birdImageView.setImageResource(R.drawable.crab_test);
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("2/5");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 40) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("2/5");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 30) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("1/5");
            birdImageView.setImageResource(R.drawable.crab_bad);
        } else if (finalScore >= 20) {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("1/5");
            birdImageView.setImageResource(R.drawable.crab_bad);
        }else {
            messageTextView.setText("You should practice more!");
            counterTextView.setText("1/5");
            birdImageView.setImageResource(R.drawable.crab_bad);
            }

        scoreTextView.setText("Score: " + finalScore + "%");

        // Determinar el tema principal del quiz (usar el primer tema del nivel)
        String currentTopic = !currentLevelTopics.isEmpty() ? currentLevelTopics.get(0) : "ALPHABET";

        // Crear copias final para usar en lambdas
        final String finalCurrentTopic = currentTopic;
        final int finalScoreForLambda = finalScore;

        // Configurar botón Continuar
        String nextTopic = ProgressionHelper.getNextTopic(this, currentTopic);
        if (nextTopic != null && finalScore >= 70) {
            btnContinue.setText(ProgressionHelper.getContinueButtonText(nextTopic));
            btnContinue.setOnClickListener(v -> {
                // Marcar tema actual como completado
                ProgressionHelper.markTopicCompleted(this, finalCurrentTopic, finalScoreForLambda);

                // Crear intent para continuar con el siguiente tema
                Intent continueIntent = ProgressionHelper.createContinueIntent(this, finalCurrentTopic, "quiz");
                if (continueIntent != null) {
                    startActivity(continueIntent);
                    finish();
                }
            });
        } else {
            // No hay siguiente tema o no aprobó
            if (nextTopic == null) {
                btnContinue.setText("¡Nivel completado!");
                btnContinue.setOnClickListener(v -> {
                    // Ir al menú principal
                    Intent intent = new Intent(this, MenuA1Activity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            } else {
                btnReintentar.setText("Try again");
                btnReintentar.setOnClickListener(v -> {
                    // Reiniciar la misma actividad
                    Intent intent = new Intent(this, QuizActivity.class);
                    intent.putExtra("LEVEL", currentLevel);
                    startActivity(intent);
                    finish();
                });
            }
        }

        // Configurar botón Ver detalles
        btnViewDetails.setOnClickListener(v -> {
            // Crear una nueva actividad para mostrar los resultados actuales
            Intent intent = new Intent(this, QuizHistoryActivity.class);
            intent.putExtra("SCORE", correctAnswers);
            intent.putExtra("TOTAL_QUESTIONS", questions.size());
            intent.putExtra("QUIZ_TYPE", "Quiz");
            intent.putExtra("TOPIC", finalCurrentTopic);  // Pasar el tema actual
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);  // Pasar el timestamp de sesión
            startActivity(intent);
            finish();
        });

    }

    // Calcular puntuación por tema
    private void calculateTopicScores() {
        topicScores.clear();

        for (String topic : currentLevelTopics) {
            List<QuestionGenerator.Question> topicQuestions = questionsByTopic.get(topic);
            if (topicQuestions != null) {
                int correctTopicAnswers = 0;
                int totalTopicQuestions = topicQuestions.size();

                // Contar respuestas correctas por tema
                for (QuestionGenerator.Question question : topicQuestions) {
                    // Buscar si esta pregunta fue respondida correctamente
                    // Esto requeriría un seguimiento más detallado de las respuestas
                    // Por ahora, usamos una aproximación basada en la puntuación general
                }

                // Calcular puntuación por tema (aproximación)
                int topicScore = (int) ((correctAnswers / (double) questions.size()) * 100);
                topicScores.put(topic, topicScore);

                Log.d(TAG, "Tema " + topic + ": " + topicScore + "%");
            }
        }
    }

    // Verificar progreso por nivel
    private void checkLevelProgress() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int finalScore = (int) ((correctAnswers / (double) questions.size()) * 100);

        // Guardar puntuación del nivel actual
        String levelScoreKey = "SCORE_LEVEL_" + currentLevel.replace(".", "_");
        editor.putInt(levelScoreKey, finalScore);

        // Si la puntuación es mayor al 70%, desbloquear el siguiente nivel
        if (finalScore >= 70) {
            String levelPassedKey = "PASSED_LEVEL_" + currentLevel.replace(".", "_");
            editor.putBoolean(levelPassedKey, true);

            // Desbloquear el siguiente nivel
            unlockNextLevel();

            Log.d(TAG, "¡Nivel " + currentLevel + " completado con éxito!");
            Toast.makeText(this, "¡Felicidades! Has desbloqueado el siguiente nivel.", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Puntuación insuficiente para desbloquear el siguiente nivel: " + finalScore + "%");
            Toast.makeText(this, "Necesitas al menos 70% para desbloquear el siguiente nivel.", Toast.LENGTH_LONG).show();
        }

        editor.apply();
    }

    // Desbloquear el siguiente nivel
    private void unlockNextLevel() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        switch (currentLevel) {
            case "A1.1":
                editor.putBoolean("UNLOCKED_A1_2", true);
                Log.d(TAG, "Desbloqueado nivel A1.2");
                break;
            case "A1.2":
                editor.putBoolean("UNLOCKED_A2_1", true);
                Log.d(TAG, "Desbloqueado nivel A2.1");
                break;
            case "A2.1":
                editor.putBoolean("UNLOCKED_A2_2", true);
                Log.d(TAG, "Desbloqueado nivel A2.2");
                break;
            default:
                Log.d(TAG, "No hay siguiente nivel para desbloquear");
                break;
        }

        editor.apply();
    }

    // Añade una celda de encabezado a una fila
    private void addHeaderCell(TableRow row, String text, int weight) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setPadding(12, 16, 12, 16);
        textView.setTextSize(14);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight));
        row.addView(textView);
    }

    // Añade una celda de datos a una fila
    private void addDataCell(TableRow row, String text, int weight) {
        addDataCell(row, text, weight, false);
    }

    // Añade una celda de datos a una fila, con estilo condicional
    private void addDataCell(TableRow row, String text, int weight, boolean isCorrect) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(isCorrect ?
            getResources().getColor(R.color.verdeSena) :
            getResources().getColor(R.color.black));
        textView.setPadding(12, 12, 12, 12);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight));
        row.addView(textView);
    }

    // Muestra mensajes flotantes con diseño visual (Snackbar)
    private void showMessage(String message, boolean isSuccess) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getResources().getColor(
                isSuccess ? R.color.success_color : R.color.error_color))
            .show();
    }

    /**
     * Método para avanzar a la siguiente pregunta de forma segura
     */
    private void advanceToNextQuestion() {
        try {
            // Verificar si hay más preguntas
            if (currentQuestionIndex + 1 < questions.size()) {
                // Hay más preguntas, avanzar
                currentQuestionIndex++;

                // RESETEAR el estado del nextButton para que los botones se habiliten en la nueva pregunta
                nextButton.setEnabled(false);

                //showNextQuestion();
                showQuestion();
                Log.d(TAG, "Advanced to next question: " + (currentQuestionIndex + 1) + "/" + questions.size());
            } else {
                // No hay más preguntas, mostrar resultados
                Log.d(TAG, "No more questions, showing final score");
                showFinalScore();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error advancing to next question: " + e.getMessage(), e);
        }
    }

    // Método para inicializar los MediaPlayer de sonidos
    private void initializeSoundPlayers() {
        try {
            // Inicializar sonido de respuesta correcta
            int correctSoundId = getResources().getIdentifier("mario_bros_vida", "raw", getPackageName());
            Log.d(TAG, "Correct sound resource ID: " + correctSoundId);

            if (correctSoundId != 0) {
                correctSoundPlayer = MediaPlayer.create(this, correctSoundId);
                if (correctSoundPlayer != null) {
                    Log.d(TAG, "✅ Sonido correcto inicializado correctamente");
                } else {
                    Log.e(TAG, "❌ Error: correctSoundPlayer es null");
                }
            } else {
                Log.e(TAG, "❌ Error: No se encontró el archivo mario_bros_vida.mp3");
            }

            // Inicializar sonido de respuesta incorrecta
            int incorrectSoundId = getResources().getIdentifier("pacman_dies", "raw", getPackageName());
            Log.d(TAG, "Incorrect sound resource ID: " + incorrectSoundId);

            if (incorrectSoundId != 0) {
                incorrectSoundPlayer = MediaPlayer.create(this, incorrectSoundId);
                if (incorrectSoundPlayer != null) {
                    Log.d(TAG, "✅ Sonido incorrecto inicializado correctamente");
                } else {
                    Log.e(TAG, "❌ Error: incorrectSoundPlayer es null");
                }
            } else {
                Log.e(TAG, "❌ Error: No se encontró el archivo pacman_dies.mp3");
            }

            Log.d(TAG, "Proceso de inicialización de sonidos completado");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando sonidos de feedback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para reproducir sonido de respuesta correcta
    private void playCorrectSound() {
        try {
            Log.d(TAG, "=== INTENTANDO REPRODUCIR SONIDO CORRECTO ===");
            if (correctSoundPlayer != null) {
                Log.d(TAG, "✅ correctSoundPlayer no es null");
                if (correctSoundPlayer.isPlaying()) {
                    Log.d(TAG, "Deteniendo sonido anterior...");
                    correctSoundPlayer.stop();
                    correctSoundPlayer.prepare();
                }
                correctSoundPlayer.start();
                Log.d(TAG, "🔊 Sonido de respuesta correcta iniciado");
            } else {
                Log.e(TAG, "❌ ERROR: correctSoundPlayer es null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido correcto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para reproducir sonido de respuesta incorrecta
    private void playIncorrectSound() {
        try {
            Log.d(TAG, "=== INTENTANDO REPRODUCIR SONIDO INCORRECTO ===");
            if (incorrectSoundPlayer != null) {
                Log.d(TAG, "✅ incorrectSoundPlayer no es null");
                if (incorrectSoundPlayer.isPlaying()) {
                    Log.d(TAG, "Deteniendo sonido anterior...");
                    incorrectSoundPlayer.stop();
                    incorrectSoundPlayer.prepare();
                }
                incorrectSoundPlayer.start();
                Log.d(TAG, "🔊 Sonido de respuesta incorrecta iniciado");
            } else {
                Log.e(TAG, "❌ ERROR: incorrectSoundPlayer es null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo sonido incorrecto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Libera recursos de la base de datos al cerrar la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }

        // Limpiar MediaPlayers
        if (correctSoundPlayer != null) {
            correctSoundPlayer.release();
            correctSoundPlayer = null;
        }

        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer.release();
            incorrectSoundPlayer = null;
        }
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(QuizActivity.this, MainActivity.class));
        Toast.makeText(QuizActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }
}