package com.example.speak.pronunciation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.HorizontalScrollView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.speak.MainActivity;
import com.example.speak.ProfileActivity;
import com.example.speak.PronunciationHistoryActivity;
import com.example.speak.QuizHistoryActivity;
import com.example.speak.R;
import com.example.speak.MenuSpeakingActivity;
import com.example.speak.database.DatabaseHelper;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PronunciationResultsActivity extends AppCompatActivity {
    
    private ImageView birdImageView;
    private TextView resultTitleTextView;
    private TextView finalScoreTextView;
    private TextView summaryTextView;
    private LinearLayout detailsContainer;
    private Button btnBackToMap;
    private Button btnRetry;
    
    private int finalScore;
    private int totalQuestions;
    private int passedQuestions;
    private String topic;
    private String level;
    private double[] individualScores;

    private View eBtnReturnMenu;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;
    private LinearLayout exportButton;

    private DatabaseHelper dbHelper;

    // Tabla de resultados (header + contenido) y scroll sincronizado
    private TableLayout pronunciationResultsTable;
    private TableLayout pronunciationResultsTableHeader;
    private HorizontalScrollView pronunciationHeaderScrollView;
    private HorizontalScrollView pronunciationContentScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pronunciation_results);
        
        // Obtener datos del intent
        Intent intent = getIntent();
        finalScore = intent.getIntExtra("FINAL_SCORE", 0);
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
        passedQuestions = intent.getIntExtra("PASSED_QUESTIONS", 0);
        topic = intent.getStringExtra("TOPIC");
        level = intent.getStringExtra("LEVEL");
        individualScores = intent.getDoubleArrayExtra("INDIVIDUAL_SCORES");

        // Declaramos las variables Menu
        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        eButtonProfile = findViewById(R.id.btnProfile);
        homeButton = findViewById(R.id.homeButton);
        exportButton = findViewById(R.id.exportButton);
        dbHelper = new DatabaseHelper(this);
        
        // Inicializar vistas
        initViews();
        
        // Configurar interfaz
        setupBirdImage();
        setupTexts();
        setupButtons();

        // Inicializar y poblar tabla (estructura tipo historial)
        initResultsTableViews();
        setupScrollSync();
        populatePronunciationResultsTable();

        // Exportar CSV (igual estilo que historial)
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> exportPronunciationToCSV());
        }

        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent profileIntent = new Intent(PronunciationResultsActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                } catch (Exception e) {
                    Toast.makeText(PronunciationResultsActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent homeIntent = new Intent(PronunciationResultsActivity.this, MainActivity.class);
                    startActivity(homeIntent);
                } catch (Exception e) {
                    Toast.makeText(PronunciationResultsActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Intent quizIntent = new Intent(PronunciationResultsActivity.this, QuizHistoryActivity.class);
                    startActivity(quizIntent);
                } catch (Exception e) {
                    Toast.makeText(PronunciationResultsActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (pronunMenu != null) {
            pronunMenu.setOnClickListener(v -> {
                try {
                    Intent pronunIntent = new Intent(PronunciationResultsActivity.this, PronunciationHistoryActivity.class);
                    startActivity(pronunIntent);
                } catch (Exception e) {
                    Toast.makeText(PronunciationResultsActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void exportPronunciationToCSV() {
        try {
            // Crear directorio propio
            File exportDir = new File(getExternalFilesDir(null), "SpeakExports");
            if (!exportDir.exists()) exportDir.mkdirs();

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File csvFile = new File(exportDir, "pronunciation_history_" + ts + ".csv");
            FileWriter writer = new FileWriter(csvFile);

            // Encabezados
            writer.append("Date,Reference Text,Spoken Text,Score,Topic,Level\n");

            android.database.Cursor cursor = dbHelper.getPronunciationHistory();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
                    String refText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_REFERENCE_TEXT));
                    String spoken = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SPOKEN_TEXT));
                    double score = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_SCORE));
                    String topicVal = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC));
                    String levelVal = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL));

                    // Escapar comillas
                    refText = "\"" + (refText == null ? "" : refText.replace("\"", "\"\"")) + "\"";
                    spoken = "\"" + (spoken == null ? "" : spoken.replace("\"", "\"\"")) + "\"";
                    topicVal = "\"" + (topicVal == null ? "" : topicVal.replace("\"", "\"\"")) + "\"";
                    levelVal = "\"" + (levelVal == null ? "" : levelVal.replace("\"", "\"\"")) + "\"";

                    writer.append(String.format(Locale.US, "%s,%s,%s,%.1f%%,%s,%s\n",
                            date, refText, spoken, score * 100.0, topicVal, levelVal));
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
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    private void initViews() {
        birdImageView = findViewById(R.id.birdImageView);
        resultTitleTextView = findViewById(R.id.resultTitleTextView);
        finalScoreTextView = findViewById(R.id.finalScoreTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        detailsContainer = findViewById(R.id.detailsContainer);
        btnBackToMap = findViewById(R.id.btnBackToMap);
        btnRetry = findViewById(R.id.btnRetry);
    }
    
    private void setupBirdImage() {
        // Configurar imagen del pájaro según el puntaje
        if (finalScore >= 70) {
            birdImageView.setImageResource(R.drawable.crab_ok);
        } else if (finalScore >= 50) {
            birdImageView.setImageResource(R.drawable.crab_test);
        } else {
            birdImageView.setImageResource(R.drawable.crab_bad);
        }
    }
    
    private void setupTexts() {
        // Título del resultado
        resultTitleTextView.setText("Resultados de Pronunciación");
        
        // Puntaje final
        finalScoreTextView.setText(finalScore + "%");
        finalScoreTextView.setTextColor(getResources().getColor(
            finalScore >= 70 ? android.R.color.holo_green_dark : 
            finalScore >= 50 ? android.R.color.holo_orange_dark : 
            android.R.color.holo_red_dark
        ));
        
        // Resumen
        String status = finalScore >= 70 ? "¡Excelente trabajo!" : 
                       finalScore >= 50 ? "¡Buen intento!" : 
                       "¡Sigue practicando!";
        
        summaryTextView.setText(String.format(
            "%s\n\nTema: %s\nNivel: %s\n\nPreguntas aprobadas: %d de %d\n(70%% requerido para aprobar)",
            status, topic, level, passedQuestions, totalQuestions
        ));
    }
    
    
    
    private void setupButtons() {
        // Botón volver al mapa
        btnBackToMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuSpeakingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        // Botón reintentar
        if (finalScore < 70) {
            btnRetry.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(v -> {
                Intent intent = new Intent(this, PronunciationActivity.class);
                intent.putExtra("TOPIC", topic);
                intent.putExtra("LEVEL", level);
                startActivity(intent);
                finish();
            });
        } else {
            btnRetry.setVisibility(View.GONE);
        }
    }

    // Cards de resultados eliminadas a petición: la vista usa solo la tabla superior

    private void initResultsTableViews() {
        pronunciationResultsTableHeader = findViewById(R.id.pronunciationResultsTableHeader);
        pronunciationResultsTable = findViewById(R.id.pronunciationResultsTable);
        pronunciationHeaderScrollView = findViewById(R.id.pronunciationHeaderScrollView);
        pronunciationContentScrollView = findViewById(R.id.pronunciationContentScrollView);
    }

    private void setupScrollSync() {
        if (pronunciationHeaderScrollView != null && pronunciationContentScrollView != null) {
            final boolean[] isSyncing = {false};

            pronunciationHeaderScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isSyncing[0]) {
                    isSyncing[0] = true;
                    pronunciationContentScrollView.scrollTo(scrollX, scrollY);
                    isSyncing[0] = false;
                }
            });

            pronunciationContentScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isSyncing[0]) {
                    isSyncing[0] = true;
                    pronunciationHeaderScrollView.scrollTo(scrollX, scrollY);
                    isSyncing[0] = false;
                }
            });
        }
    }

    private void populatePronunciationResultsTable() {
        if (pronunciationResultsTableHeader == null || pronunciationResultsTable == null) return;

        // Encabezado
        pronunciationResultsTableHeader.removeAllViews();
        addTableHeader(pronunciationResultsTableHeader);

        // Datos (mostrar todo el historial del usuario)
        android.database.Cursor cursor = null;
        try {
            cursor = dbHelper.getPronunciationHistory();
            if (cursor != null && cursor.moveToFirst()) {
                pronunciationResultsTable.removeAllViews();
                do {
                    addTableRow(pronunciationResultsTable, cursor);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar resultados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void addTableHeader(TableLayout table) {
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getResources().getColor(R.color.header_blue_table));

        String[] headers = {"Date", "Reference Text", "Spoken Text", "Score", "Topic", "Level"};
        for (String header : headers) {
            TextView textView = new TextView(this);
            textView.setText(header);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(16);
            textView.setPadding(16, 12, 16, 12);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setMinWidth(220);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            headerRow.addView(textView);
        }
        table.addView(headerRow);
    }

    private void addTableRow(TableLayout table, android.database.Cursor cursor) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(cursor.getPosition() % 2 == 0 ?
                getResources().getColor(R.color.white) :
                getResources().getColor(R.color.light_gray));

        long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));

        String refText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_REFERENCE_TEXT));
        String spoken = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SPOKEN_TEXT));
        double score = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_SCORE));
        String topicVal = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC));
        String levelVal = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL));

        String[] cellContents = {
                date,
                refText != null ? refText : "",
                spoken != null ? spoken : "",
                String.format(java.util.Locale.US, "%.1f%%", score * 100.0),
                topicVal != null ? topicVal : "",
                levelVal != null ? levelVal : ""
        };

        for (int i = 0; i < cellContents.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(cellContents[i]);
            textView.setTextSize(14);
            textView.setPadding(16, 12, 16, 12);
            textView.setMinWidth(200);
            textView.setMaxWidth(600);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(3);

            // Color de la columna Score
            if (i == 3) {
                if (score >= 0.7) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else if (score >= 0.5) {
                    textView.setTextColor(Color.rgb(255, 165, 0));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                textView.setTypeface(null, Typeface.BOLD);
            }

            row.addView(textView);
        }

        table.addView(row);
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(PronunciationResultsActivity.this, MainActivity.class));
        Toast.makeText(PronunciationResultsActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }
} 