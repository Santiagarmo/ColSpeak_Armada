package com.example.speak;

// Importaciones necesarias para trabajar con UI, base de datos y diseño
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import android.view.animation.Animation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

// Importación de una clase personalizada para acceder a la base de datos
import com.example.speak.database.DatabaseHelper;
import com.example.speak.quiz.QuizActivity;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Clase que muestra el historial de preguntas de un quiz.
 * Extiende AppCompatActivity, lo que le permite ser una pantalla dentro de la app.
 */
public class QuizHistoryActivity extends AppCompatActivity {

    private static final String TAG = "QuizHistoryActivity";

    // Referencias a la base de datos y al diseño (tabla)
    private DatabaseHelper dbHelper;
    private TableLayout tableLayout;
    private TableLayout tableLayoutHeader;
    private android.widget.HorizontalScrollView headerScrollView;
    private android.widget.HorizontalScrollView contentScrollView;
    private long currentUserId;
    private Button continueButton;

    private androidx.constraintlayout.widget.ConstraintLayout eBtnReturnMenu;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    /**
     * Método que se ejecuta al crear la actividad.
     * Inicializa el layout, la base de datos y carga el historial.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        dbHelper = new DatabaseHelper(this);
        tableLayout = findViewById(R.id.quizHistoryTable);
        tableLayoutHeader = findViewById(R.id.quizHistoryTableHeader);
        headerScrollView = findViewById(R.id.headerScrollView);
        contentScrollView = findViewById(R.id.contentScrollView);
        TextView historyTitle = findViewById(R.id.historyTitle);
        LinearLayout exportButton = findViewById(R.id.exportButton);
        continueButton = findViewById(R.id.continueButton);

        // Sincronizar el scroll horizontal entre header y contenido
        setupScrollSync();

        //Declaramos las variables Menu
        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        eButtonProfile = findViewById(R.id.btnProfile);
        homeButton = findViewById(R.id.homeButton);

        // Obtener el ID del usuario actual
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("user_id", -1);
        Log.d("QuizHistory", "Current User ID: " + currentUserId);

        // Obtener extras del Intent
        String quizType = getIntent().getStringExtra("QUIZ_TYPE");
        int score = getIntent().getIntExtra("SCORE", 0);
        int totalQuestions = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        String currentTopic = getIntent().getStringExtra("TOPIC"); // Obtener el tema actual

        Log.d("QuizHistory", "Quiz Type: " + quizType);
        Log.d("QuizHistory", "Score: " + score);
        Log.d("QuizHistory", "Total Questions: " + totalQuestions);
        Log.d("QuizHistory", "Current Topic: " + currentTopic);

        // Actualizar el título según el tipo de quiz
        if (quizType != null) {
            historyTitle.setText("Resultados de " + quizType);
        } else {
            historyTitle.setText("Historial de Actividades");
        }

        try {
            // Usar el nuevo método loadQuizResults
            loadQuizResults();
        } catch (Exception e) {
            Log.e("QuizHistory", "Error loading quiz history: " + e.getMessage());
            Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show();
        }

        // Configurar el botón "Continuar"
        setupContinueButton(currentTopic, score, totalQuestions);

        // Configurar el botón de exportación
        exportButton.setOnClickListener(v -> exportToCSV());

        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(QuizHistoryActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(QuizHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(QuizHistoryActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(QuizHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });

        if (quizMenu != null) {
            quizMenu.setOnClickListener(v -> {
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    Intent intent = new Intent(QuizHistoryActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(QuizHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (pronunMenu != null) {
            pronunMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(QuizHistoryActivity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(QuizHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (birdMenu != null) {
            birdMenu.setOnClickListener(v -> {
                birdExpanded = !birdExpanded;
                if (birdExpanded) {
                    birdMenu.setImageResource(R.drawable.bird1_menu);
                    fadeInView(quizMenu);
                    fadeInView(pronunMenu);
                } else {
                    birdMenu.setImageResource(R.drawable.bird0_menu);
                    fadeOutView(quizMenu);
                    fadeOutView(pronunMenu);
                }
            });
        }
    }

    /**
     * Sincroniza el scroll horizontal entre el header y el contenido
     */
    private void setupScrollSync() {
        if (headerScrollView != null && contentScrollView != null) {
            // Variable para evitar bucles infinitos de sincronización
            final boolean[] isSyncing = {false};

            headerScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isSyncing[0]) {
                    isSyncing[0] = true;
                    contentScrollView.scrollTo(scrollX, scrollY);
                    isSyncing[0] = false;
                }
            });

            contentScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isSyncing[0]) {
                    isSyncing[0] = true;
                    headerScrollView.scrollTo(scrollX, scrollY);
                    isSyncing[0] = false;
                }
            });
        }
    }

    private void fadeOutView(final View view) {
        if (view == null) return;

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(fadeOut);
    }

    private void fadeInView(final View view) {
        if (view == null) return;

        view.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        view.startAnimation(fadeIn);
    }

    private void showFullHistory(TableLayout tableLayout, long currentUserId) {
        // Crear encabezado de la tabla
        tableLayoutHeader.removeAllViews();
        addTableHeader(tableLayoutHeader);

        Cursor cursor = dbHelper.getQuizHistory();
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // Obtener los índices de las columnas una sola vez
                int userIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID);
                int questionIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION);
                int userAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER);
                int correctAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER);
                int isCorrectIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT);
                int topicIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC);
                int levelIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL);

                do {
                    long recordUserId = cursor.getLong(userIdIndex);
                    if (recordUserId == currentUserId) {
                        addTableRow(tableLayout, cursor);
                    }
                } while (cursor.moveToNext());
            } catch (Exception e) {
                Log.e("QuizHistory", "Error processing cursor: " + e.getMessage());
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
    }

    private void showCurrentActivityResults(TableLayout tableLayout, ArrayList<ListeningQuestion> questions, int score, int totalQuestions) {
        Log.d("QuizHistory", "Showing current activity results");

        // Crear encabezado de la tabla
        tableLayoutHeader.removeAllViews();
        addTableHeader(tableLayoutHeader);

        // Obtener el timestamp de sesión
        long sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1);
        String quizType = getIntent().getStringExtra("QUIZ_TYPE");

        Log.d("QuizHistory", "Session Timestamp: " + sessionTimestamp);
        Log.d("QuizHistory", "Quiz Type: " + quizType);

        if (sessionTimestamp == -1) {
            Log.e("QuizHistory", "No session timestamp found");
            return;
        }

        // Obtener las respuestas de la sesión actual
        Cursor cursor = dbHelper.getQuizHistory();
        if (cursor == null) {
            Log.e("QuizHistory", "Cursor is null");
            return;
        }

        if (!cursor.moveToFirst()) {
            Log.e("QuizHistory", "No data in cursor");
            cursor.close();
            return;
        }

        try {
            // Obtener los índices de las columnas una sola vez
            int userIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID);
            int questionIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION);
            int userAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER);
            int correctAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER);
            int isCorrectIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT);
            int topicIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC);
            int levelIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL);
            int timestampIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP);
            int quizTypeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE);

            // Verificar que todos los índices sean válidos
            if (userIdIndex == -1 || questionIndex == -1 || userAnswerIndex == -1 ||
                    correctAnswerIndex == -1 || isCorrectIndex == -1 || topicIndex == -1 ||
                    levelIndex == -1 || timestampIndex == -1 || quizTypeIndex == -1) {
                Log.e("QuizHistory", "Invalid column index");
                return;
            }

            // Mostrar solo las respuestas de la sesión actual
            do {
                long recordUserId = cursor.getLong(userIdIndex);
                String recordQuizType = cursor.getString(quizTypeIndex);
                long recordTimestamp = cursor.getLong(timestampIndex);

                if (recordUserId == currentUserId &&
                        quizType.equals(recordQuizType) &&
                        recordTimestamp == sessionTimestamp) {  // Comparación exacta del timestamp

                    String question = cursor.getString(questionIndex);
                    String userAnswer = cursor.getString(userAnswerIndex);
                    String correctAnswer = cursor.getString(correctAnswerIndex);
                    boolean isCorrect = cursor.getInt(isCorrectIndex) == 1;
                    String topic = cursor.getString(topicIndex);
                    String level = cursor.getString(levelIndex);

                    addTableRow(tableLayout, cursor);
                }
            } while (cursor.moveToNext());
        } catch (Exception e) {
            Log.e("QuizHistory", "Error processing cursor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    private void showFilteredHistory(TableLayout tableLayout, long currentUserId, String quizType, boolean showOnlyLast10) {
        // Crear encabezado de la tabla
        tableLayoutHeader.removeAllViews();
        addTableHeader(tableLayoutHeader);

        Cursor cursor = dbHelper.getQuizHistory();
        if (cursor != null && cursor.moveToFirst()) {
            int count = 0;
            do {
                long recordUserId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID));
                String recordQuizType = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE));

                if (recordUserId == currentUserId && quizType.equals(recordQuizType)) {
                    addTableRow(tableLayout, cursor);
                    count++;

                    if (showOnlyLast10 && count >= 10) {
                        break;
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }


    private void addTableHeader(TableLayout table) {
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getResources().getColor(R.color.header_blue_table));

        String[] headers = {"Date", "Question", "Correct Answer", "Your Answer", "Result", "Topic", "Level"};
        for (String header : headers) {
            TextView textView = new TextView(this);
            textView.setText(header);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(16);
            textView.setPadding(16, 12, 16, 12);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setMinWidth(220); // Aumenta el ancho mínimo
            textView.setMaxLines(1);   // Solo una línea
            textView.setSingleLine(true); // Forzar una sola línea
            textView.setEllipsize(TextUtils.TruncateAt.END);
            headerRow.addView(textView);
        }
        table.addView(headerRow);
    }

    private void addTableRow(TableLayout table, Cursor cursor) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(cursor.getPosition() % 2 == 0 ?
                getResources().getColor(R.color.white) :
                getResources().getColor(R.color.light_gray));

        // Formatear la fecha
        long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(timestamp));

        // Obtener los datos
        String question = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION));
        String correctAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER));
        String selectedAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER));
        boolean isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1;
        String topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC));
        String level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL));

        // Crear las celdas
        String[] cellContents = {
                date,
                question,
                correctAnswer,
                selectedAnswer,
                isCorrect ? "✓" : "✗",
                topic,
                level
        };

        for (int i = 0; i < cellContents.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(cellContents[i]);
            textView.setTextSize(14);
            textView.setPadding(16, 12, 16, 12);
            textView.setMinWidth(200); // Ancho mínimo para cada columna
            textView.setMaxWidth(400); // Ancho máximo para cada columna
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(3);

            // Aplicar color rojo o verde para el resultado
            if (i == 4) { // La columna del resultado
                textView.setTextColor(isCorrect ? Color.GREEN : Color.RED);
                textView.setTextSize(18); // Hacer el símbolo más grande
                textView.setTypeface(null, Typeface.BOLD);
            }

            row.addView(textView);
        }

        table.addView(row);
    }

    /**
     * Muestra un mensaje al usuario usando Toast
     * @param message El mensaje a mostrar
     * @param isSuccess Si el mensaje es de éxito o error
     */
    private void showMessage(String message, boolean isSuccess) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Configurar el botón "Continuar" según el progreso del usuario
     */
    private void setupContinueButton(String currentTopic, int score, int totalQuestions) {
        // Si no tenemos el tema del Intent, intentar obtenerlo de la base de datos
        if (currentTopic == null || currentTopic.isEmpty()) {
            currentTopic = getTopicFromDatabase();
        }

        // Calcular el porcentaje del score
        int finalScore = (totalQuestions > 0) ? (int) ((score / (double) totalQuestions) * 100) : 0;

        Log.d(TAG, "Setting up continue button - Topic: " + currentTopic + ", Score: " + finalScore + "%");

        // Solo mostrar el botón si hay un tema válido y el usuario aprobó
        if (currentTopic != null && !currentTopic.isEmpty() && finalScore >= 70) {
            String nextTopic = ProgressionHelper.getNextTopic(this, currentTopic);

            if (nextTopic != null) {
                // Hay un siguiente tema disponible
                continueButton.setText(ProgressionHelper.getContinueButtonText(nextTopic));
                continueButton.setVisibility(View.VISIBLE);

                // Crear copia final de las variables para usar en la lambda
                final String finalCurrentTopic = currentTopic;
                final int finalScoreForLambda = finalScore;

                // Configurar el click listener
                continueButton.setOnClickListener(v -> {
                    Log.d(TAG, "Continue button clicked");
                    Log.d(TAG, "Current topic: " + finalCurrentTopic);
                    Log.d(TAG, "Score: " + finalScoreForLambda + "%");

                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, finalCurrentTopic, finalScoreForLambda);
                    Log.d(TAG, "Topic marked as completed");

                    // Crear intent para continuar con el siguiente tema
                    Intent continueIntent = ProgressionHelper.createContinueIntent(this, finalCurrentTopic, "");
                    Log.d(TAG, "Continue intent created: " + (continueIntent != null ? continueIntent.getComponent() : "null"));

                    if (continueIntent != null) {
                        Log.d(TAG, "Starting next activity");
                        startActivity(continueIntent);
                        finish();
                    } else {
                        Log.e(TAG, "Continue intent is null for topic: " + finalCurrentTopic);
                        Toast.makeText(this, "No hay más temas disponibles", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.d(TAG, "Continue button configured for next topic: " + nextTopic);
            } else {
                // Es el último tema
                continueButton.setText("¡Nivel completado!");
                continueButton.setVisibility(View.VISIBLE);
                continueButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, MenuA1Activity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });

                Log.d(TAG, "Continue button configured as level completed");
            }
        } else {
            // No mostrar el botón si no aprobó o no hay tema válido
            continueButton.setVisibility(View.GONE);
            Log.d(TAG, "Continue button hidden - Topic: " + currentTopic + ", Score: " + finalScore + "%");
        }
    }

    /**
     * Obtener el tema de la base de datos basado en la sesión actual
     */
    private String getTopicFromDatabase() {
        try {
            long sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1);
            String quizType = getIntent().getStringExtra("QUIZ_TYPE");

            if (sessionTimestamp == -1 || quizType == null) {
                Log.d(TAG, "No session timestamp or quiz type available");
                return null;
            }

            String query = "SELECT DISTINCT " + DatabaseHelper.COLUMN_TOPIC +
                    " FROM quiz_results WHERE " +
                    DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_QUIZ_TYPE + " = ? AND " +
                    DatabaseHelper.COLUMN_TIMESTAMP + " = ? LIMIT 1";

            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, new String[]{
                    String.valueOf(currentUserId), quizType, String.valueOf(sessionTimestamp)
            });

            if (cursor != null && cursor.moveToFirst()) {
                int topicIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC);
                if (topicIndex != -1) {
                    String topic = cursor.getString(topicIndex);
                    cursor.close();
                    Log.d(TAG, "Topic obtained from database: " + topic);
                    return topic;
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting topic from database: " + e.getMessage());
        }

        return null;
    }

    /**
     * Se llama cuando la actividad se destruye. Cierra la base de datos para liberar recursos.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void loadQuizResults() {
        try {
            String quizType = getIntent().getStringExtra("QUIZ_TYPE");
            boolean showCurrentActivityOnly = getIntent().getBooleanExtra("SHOW_CURRENT_ACTIVITY_ONLY", false);
            long sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1);

            Log.d(TAG, "Loading quiz results - Type: " + quizType + ", ShowCurrentOnly: " + showCurrentActivityOnly + ", Timestamp: " + sessionTimestamp);

            tableLayout.removeAllViews();
            tableLayoutHeader.removeAllViews();
            addTableHeader(tableLayoutHeader);

            if (quizType != null && quizType.equals("Writing")) {
                Log.d(TAG, "Loading Writing results");
                String writingQuery = "SELECT * FROM " + DatabaseHelper.TABLE_WRITING + " WHERE " +
                        DatabaseHelper.COLUMN_WRITING_USER_ID + " = ?";
                String[] writingSelectionArgs = new String[]{String.valueOf(currentUserId)};

                if (showCurrentActivityOnly && sessionTimestamp != -1) {
                    writingQuery += " AND " + DatabaseHelper.COLUMN_WRITING_TIMESTAMP + " = ?";
                    writingSelectionArgs = new String[]{String.valueOf(currentUserId), String.valueOf(sessionTimestamp)};
                }

                writingQuery += " ORDER BY " + DatabaseHelper.COLUMN_WRITING_TIMESTAMP + " DESC";
                Log.d(TAG, "Writing query: " + writingQuery);

                Cursor writingCursor = dbHelper.getReadableDatabase().rawQuery(writingQuery, writingSelectionArgs);

                // Verificar si el cursor tiene datos
                if (writingCursor == null) {
                    Log.e(TAG, "Writing cursor is null");
                    return;
                }

                if (!writingCursor.moveToFirst()) {
                    Log.d(TAG, "No writing results found");
                    writingCursor.close();
                    return;
                }

                // Obtener índices de columnas
                int timestampIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_TIMESTAMP);
                int questionIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_QUESTION);
                int userAnswerIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_USER_ANSWER);
                int topicIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC);
                int levelIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL);
                int similarityIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_SIMILARITY);
                int isCorrectIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_IS_CORRECT);

                // Verificar que todos los índices sean válidos
                if (timestampIndex == -1 || questionIndex == -1 || userAnswerIndex == -1 ||
                        topicIndex == -1 || levelIndex == -1 || similarityIndex == -1 || isCorrectIndex == -1) {
                    Log.e(TAG, "Invalid column index in writing table");
                    writingCursor.close();
                    return;
                }

                do {
                    try {
                        TableRow row = new TableRow(this);
                        row.setBackgroundColor(writingCursor.getPosition() % 2 == 0 ?
                                getResources().getColor(R.color.white) :
                                getResources().getColor(R.color.light_gray));

                        long timestamp = writingCursor.getLong(timestampIndex);
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(new Date(timestamp));

                        String question = writingCursor.getString(questionIndex);
                        String userAnswer = writingCursor.getString(userAnswerIndex);
                        String topic = writingCursor.getString(topicIndex);
                        String level = writingCursor.getString(levelIndex);
                        double similarity = writingCursor.getDouble(similarityIndex);
                        String similarityText = String.format("%.1f%%", similarity * 100);
                        boolean isCorrect = writingCursor.getInt(isCorrectIndex) == 1;

                        String[] cellContents = {
                                date,
                                question,
                                isCorrect ? "✓" : "✗",
                                userAnswer,
                                similarityText,
                                topic,
                                level
                        };

                        for (int i = 0; i < cellContents.length; i++) {
                            TextView textView = new TextView(this);
                            textView.setText(cellContents[i]);
                            textView.setTextSize(14);
                            textView.setPadding(16, 12, 16, 12);
                            textView.setMinWidth(220);
                            textView.setMaxLines(3);
                            textView.setEllipsize(TextUtils.TruncateAt.END);

                            // Aplicar color al símbolo de correcto/incorrecto
                            if (i == 2) { // La columna del símbolo ✓/✗
                                textView.setTextColor(isCorrect ? Color.GREEN : Color.RED);
                                textView.setTextSize(18);
                                textView.setTypeface(null, Typeface.BOLD);
                            }
                            // Aplicar color al porcentaje de similitud
                            else if (i == 4) { // La columna del porcentaje
                                textView.setTextColor(similarity >= 0.7 ? Color.GREEN :
                                        similarity >= 0.5 ? Color.rgb(255, 165, 0) : // Naranja
                                                Color.RED);
                                textView.setTextSize(16);
                                textView.setTypeface(null, Typeface.BOLD);
                            }

                            row.addView(textView);
                        }

                        tableLayout.addView(row);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing writing row: " + e.getMessage());
                    }
                } while (writingCursor.moveToNext());
                writingCursor.close();
            } else {
                Log.d(TAG, "Loading quiz_results");
                String query = "SELECT * FROM quiz_results WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(currentUserId)};

                if (showCurrentActivityOnly && sessionTimestamp != -1) {
                    query += " AND " + DatabaseHelper.COLUMN_TIMESTAMP + " = ?";
                    selectionArgs = new String[]{String.valueOf(currentUserId), String.valueOf(sessionTimestamp)};
                } else if (quizType != null && !quizType.isEmpty()) {
                    query += " AND " + DatabaseHelper.COLUMN_QUIZ_TYPE + " = ?";
                    selectionArgs = new String[]{String.valueOf(currentUserId), quizType};
                }

                query += " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " DESC";
                Log.d(TAG, "Quiz results query: " + query);

                Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, selectionArgs);
                while (cursor.moveToNext()) {
                    addTableRow(tableLayout, cursor);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadQuizResults: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar el historial: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportToCSV() {
        try {
            // Crear el directorio si no existe
            File exportDir = new File(getExternalFilesDir(null), "SpeakExports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Crear el archivo CSV con timestamp
            String fileNameTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File csvFile = new File(exportDir, "quiz_history_" + fileNameTimestamp + ".csv");
            FileWriter writer = new FileWriter(csvFile);

            // Escribir encabezados
            writer.append("Date,Question,Correct Answer,User Answer,Score,Topic,Level\n");

            // Obtener los datos de la base de datos
            Cursor cursor = dbHelper.getReadableDatabase().query(
                    DatabaseHelper.TABLE_QUIZ,
                    null,
                    DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(currentUserId)},
                    null,
                    null,
                    DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Obtener los valores de cada columna
                    long recordTimestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(new Date(recordTimestamp));
                    String question = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION));
                    String correctAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER));
                    String userAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER));
                    boolean isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1;
                    String topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC));
                    String level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL));

                    // Escapar comas y comillas en los campos
                    question = "\"" + question.replace("\"", "\"\"") + "\"";
                    correctAnswer = "\"" + correctAnswer.replace("\"", "\"\"") + "\"";
                    userAnswer = "\"" + userAnswer.replace("\"", "\"\"") + "\"";
                    topic = "\"" + topic.replace("\"", "\"\"") + "\"";
                    level = "\"" + level.replace("\"", "\"\"") + "\"";

                    // Escribir la línea en el CSV
                    writer.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            date,
                            question,
                            correctAnswer,
                            userAnswer,
                            isCorrect ? "Correct" : "Incorrect",
                            topic,
                            level
                    ));
                } while (cursor.moveToNext());
                cursor.close();
            }

            writer.flush();
            writer.close();

            // Mostrar mensaje de éxito con la ubicación del archivo
            String message = "Archivo CSV exportado exitosamente a:\n" + csvFile.getAbsolutePath();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            // Compartir el archivo
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            Uri csvUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    csvFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartir archivo CSV"));

        } catch (Exception e) {
            Log.e(TAG, "Error exporting to CSV: " + e.getMessage());
            Toast.makeText(this, "Error al exportar el archivo CSV", Toast.LENGTH_SHORT).show();
        }
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(QuizHistoryActivity.this, MainActivity.class));
        Toast.makeText(QuizHistoryActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }

}