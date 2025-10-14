package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.speak.database.DatabaseHelper;
import com.example.speak.ProgressionHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.File;
import java.io.FileWriter;

public class ReadingHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ReadingHistoryActivity";

    private DatabaseHelper dbHelper;
    private TableLayout tableLayout;
    private long currentUserId;
    private Button continueButton;
    private LinearLayout exportButton;
    private String currentTopic;
    private int currentScore;

    private View eBtnReturnMenu;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        // Cambiar el título del historial
        TextView titleTextView = findViewById(R.id.historyTitle);
        if (titleTextView != null) {
            titleTextView.setText("Resultados de Reading");
        }

        dbHelper = new DatabaseHelper(this);
        tableLayout = findViewById(R.id.quizHistoryTable);
        exportButton = findViewById(R.id.exportButton);

        // Obtener el ID del usuario actual
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("user_id", -1);

        // Obtener extras del Intent
        boolean showCurrentActivityOnly = getIntent().getBooleanExtra("SHOW_CURRENT_ACTIVITY_ONLY", false);
        long sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1);
        currentTopic = getIntent().getStringExtra("TOPIC");
        currentScore = getIntent().getIntExtra("SCORE", 0);

        Log.d(TAG, "Reading History - ShowCurrentOnly: " + showCurrentActivityOnly + ", Timestamp: " + sessionTimestamp);

        // Inicializar botón continue
        continueButton = findViewById(R.id.continueButton);
        setupContinueButton();

        //Declaramos las variables Menu
        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        eButtonProfile = findViewById(R.id.btnProfile);
        homeButton = findViewById(R.id.homeButton);

        try {
            loadReadingResults(showCurrentActivityOnly, sessionTimestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error loading reading history: " + e.getMessage());
            Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show();
        }

        if (exportButton != null) {
            exportButton.setOnClickListener(v -> exportToCSV());
        }

        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(ReadingHistoryActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ReadingHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(ReadingHistoryActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ReadingHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Intent intent = new Intent(ReadingHistoryActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ReadingHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (pronunMenu != null) {
            pronunMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(ReadingHistoryActivity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(ReadingHistoryActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void loadReadingResults(boolean showCurrentActivityOnly, long sessionTimestamp) {
        try {
            tableLayout.removeAllViews();
            addTableHeader();

            Cursor cursor;
            if (showCurrentActivityOnly && sessionTimestamp != -1) {
                Log.d(TAG, "Loading current session results for timestamp: " + sessionTimestamp);
                // Mostrar solo resultados de la sesión actual
                String query = "SELECT * FROM " + DatabaseHelper.TABLE_READING + 
                              " WHERE " + DatabaseHelper.COLUMN_READING_USER_ID + " = ? AND " +
                              DatabaseHelper.COLUMN_READING_TIMESTAMP + " = ? " +
                              " ORDER BY " + DatabaseHelper.COLUMN_READING_TIMESTAMP + " DESC";
                cursor = dbHelper.getReadableDatabase().rawQuery(query, 
                    new String[]{String.valueOf(currentUserId), String.valueOf(sessionTimestamp)});
            } else {
                Log.d(TAG, "Loading all reading history");
                // Mostrar todo el historial
                cursor = dbHelper.getReadingHistory();
            }

            if (cursor != null && cursor.moveToFirst()) {
                int rowCount = 0;
                do {
                    long recordUserId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_USER_ID));
                    
                    if (recordUserId == currentUserId) {
                        addTableRow(cursor);
                        rowCount++;
                    }
                } while (cursor.moveToNext());
                
                Log.d(TAG, "Loaded " + rowCount + " reading results");
                cursor.close();
                
                if (rowCount == 0) {
                    if (showCurrentActivityOnly) {
                        Toast.makeText(this, "No se encontraron resultados para esta sesión", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No hay historial de reading disponible", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.d(TAG, "No cursor data found");
                if (showCurrentActivityOnly) {
                    Toast.makeText(this, "No se encontraron resultados para esta sesión", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No hay historial de reading disponible", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading reading results: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar el historial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addTableHeader() {
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getResources().getColor(R.color.header_blue));

        String[] headers = {"Date", "Question", "Correct Answer", "Your Answer", "Result", "Topic", "Level"};
        for (String header : headers) {
            TextView textView = new TextView(this);
            textView.setText(header);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(16);
            textView.setPadding(16, 12, 16, 12);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setMinWidth(220);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            headerRow.addView(textView);
        }
        tableLayout.addView(headerRow);
    }

    private void addTableRow(Cursor cursor) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(cursor.getPosition() % 2 == 0 ? 
            getResources().getColor(R.color.white) : 
            getResources().getColor(R.color.light_gray));

        // Formatear la fecha
        long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_TIMESTAMP));
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(new Date(timestamp));

        // Obtener los datos
        String question = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_QUESTION));
        String correctAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_CORRECT_ANSWER));
        String userAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_USER_ANSWER));
        boolean isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_IS_CORRECT)) == 1;
        String topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_TOPIC));
        String level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_LEVEL));

        // Crear las celdas
        String[] cellContents = {
            date,
            question,
            correctAnswer,
            userAnswer,
            isCorrect ? "✓" : "✗",
            topic,
            level
        };

        for (int i = 0; i < cellContents.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(cellContents[i]);
            textView.setTextSize(14);
            textView.setPadding(16, 12, 16, 12);
            textView.setMinWidth(200);
            textView.setMaxWidth(400);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(3);

            // Aplicar color rojo o verde para el resultado
            if (i == 4) { // La columna del resultado
                textView.setTextColor(isCorrect ? Color.GREEN : Color.RED);
                textView.setTextSize(18);
                textView.setTypeface(null, Typeface.BOLD);
            }

            row.addView(textView);
        }

        tableLayout.addView(row);
    }

    private void exportToCSV() {
        try {
            // Crear directorio de exportación
            File exportDir = new File(getExternalFilesDir(null), "SpeakExports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Archivo CSV con timestamp
            String fileNameTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File csvFile = new File(exportDir, "reading_history_" + fileNameTimestamp + ".csv");
            FileWriter writer = new FileWriter(csvFile);

            // Encabezados
            writer.append("Date,Question,Correct Answer,User Answer,Result,Topic,Level\n");

            // Consultar la tabla de reading para el usuario actual
            Cursor cursor = dbHelper.getReadableDatabase().query(
                    DatabaseHelper.TABLE_READING,
                    null,
                    DatabaseHelper.COLUMN_READING_USER_ID + " = ?",
                    new String[]{String.valueOf(currentUserId)},
                    null,
                    null,
                    DatabaseHelper.COLUMN_READING_TIMESTAMP + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long ts = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_TIMESTAMP));
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(ts));
                    String question = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_QUESTION));
                    String correctAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_CORRECT_ANSWER));
                    String userAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_USER_ANSWER));
                    boolean isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_IS_CORRECT)) == 1;
                    String topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_TOPIC));
                    String level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_READING_LEVEL));

                    // Escapar comillas y comas
                    question = "\"" + (question == null ? "" : question.replace("\"", "\"\"")) + "\"";
                    correctAnswer = "\"" + (correctAnswer == null ? "" : correctAnswer.replace("\"", "\"\"")) + "\"";
                    userAnswer = "\"" + (userAnswer == null ? "" : userAnswer.replace("\"", "\"\"")) + "\"";
                    topic = "\"" + (topic == null ? "" : topic.replace("\"", "\"\"")) + "\"";
                    level = "\"" + (level == null ? "" : level.replace("\"", "\"\"")) + "\"";

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

            // Avisar y compartir
            String message = "Archivo CSV exportado exitosamente a:\n" + csvFile.getAbsolutePath();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            Uri csvUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    csvFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartir archivo CSV"));

        } catch (Exception e) {
            Log.e(TAG, "Error exporting reading CSV: " + e.getMessage());
            Toast.makeText(this, "Error al exportar el archivo CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupContinueButton() {
        // Solo mostrar el botón si tenemos un tema y el usuario aprobó (score >= 70)
        if (currentTopic != null && !currentTopic.isEmpty() && currentScore >= 70) {
            String nextTopic = ProgressionHelper.getNextReadingTopic(currentTopic);
            
            if (nextTopic != null) {
                // Hay un siguiente tema disponible
                continueButton.setText("➡️ Continuar: " + nextTopic);
                continueButton.setVisibility(View.VISIBLE);
                
                continueButton.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, currentTopic, currentScore);
                    
                    // Ir al siguiente tema de reading
                    Intent intent = new Intent(this, TranslationReadingActivity.class);
                    intent.putExtra("TOPIC", nextTopic);
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                    finish();
                });
            } else {
                // Es el último tema de reading, ir al mapa de writing
                continueButton.setText("✍️ ¡Desbloquear Writing!");
                continueButton.setVisibility(View.VISIBLE);
                
                continueButton.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, currentTopic, currentScore);
                    
                    // Ir al mapa de writing
                    Intent intent = new Intent(this, MenuWritingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }
        } else {
            // No hay tema o no aprobó, ocultar botón continue
            continueButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(ReadingHistoryActivity.this, MainActivity.class));
        Toast.makeText(ReadingHistoryActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }
} 