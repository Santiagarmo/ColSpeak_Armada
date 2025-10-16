package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.speak.R;
import com.example.speak.ProgressionHelper;
import com.example.speak.pronunciation.PronunciationActivity;

public class ReadingResultsActivity extends AppCompatActivity {
    
    private ImageView birdImageView;
    private TextView resultTitleTextView;
    private TextView finalScoreTextView;
    private TextView summaryTextView;
    private LinearLayout detailsContainer;
    private Button btnBackToMap;
    private Button btnRetry;
    private Button btnContinue;
    private TextView btnReintentar;
    private LinearLayout btnViewDetails;
    private TextView messageTextView;
    private TextView counterTextView;
    
    private int finalScore;
    private int totalQuestions;
    private int correctAnswers;
    private String topic;
    private String level;
    private boolean[] questionResults;
    private String[] questions;
    private long sessionTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_results);
        
        // Obtener datos del intent
        Intent intent = getIntent();
        finalScore = intent.getIntExtra("FINAL_SCORE", 0);
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0);
        topic = intent.getStringExtra("TOPIC");
        level = intent.getStringExtra("LEVEL");
        questionResults = intent.getBooleanArrayExtra("QUESTION_RESULTS");
        questions = intent.getStringArrayExtra("QUESTIONS");
        sessionTimestamp = intent.getLongExtra("SESSION_TIMESTAMP", -1);
        
        // Inicializar vistas
        initViews();
        
        // Configurar interfaz
        setupBirdImage();
        setupTexts();
        setupDetailedResults();
        setupButtons();
        setupContinueButton();
    }
    
    private void initViews() {
        birdImageView = findViewById(R.id.birdImageView);
        resultTitleTextView = findViewById(R.id.resultTitleTextView);
        finalScoreTextView = findViewById(R.id.finalScoreTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        detailsContainer = findViewById(R.id.detailsContainer);
        btnBackToMap = findViewById(R.id.btnBackToMap);
        btnRetry = findViewById(R.id.btnRetry);
        btnContinue = findViewById(R.id.btnContinue);
        btnReintentar = findViewById(R.id.btnReintentar);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        messageTextView = findViewById(R.id.messageTextView);
        counterTextView = findViewById(R.id.counterTextView);
    }

    private void setupBirdImage() {
    // Set bird image based on score (misma lógica que ListeningActivity)
        if (finalScore >= 100) {
        messageTextView.setText("Excellent your English is getting better!");
        birdImageView.setImageResource(R.drawable.crab_ok);
    } else if (finalScore >= 90) {
        messageTextView.setText("Good, but you can do it better!");
        birdImageView.setImageResource(R.drawable.crab_ok);
    } else if (finalScore >= 80) {
        messageTextView.setText("Good, but you can do it better!");
        birdImageView.setImageResource(R.drawable.crab_ok);
    } else if (finalScore >= 69) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_test);
    } else if (finalScore >= 60) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_test);
    } else if (finalScore >= 50) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_bad);
    } else if (finalScore >= 40) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_bad);
    } else if (finalScore >= 30) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_bad);
    } else if (finalScore >= 20) {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_bad);
    }else {
        messageTextView.setText("You should practice more!");
        birdImageView.setImageResource(R.drawable.crab_bad);
    }
        counterTextView.setText(correctAnswers + "/" + totalQuestions);
    }

    private void setupTexts() {

        // Título del resultado
        resultTitleTextView.setText("📚 Resultados de Reading");
        
        // Puntaje final
        finalScoreTextView.setText(finalScore + "%");
        finalScoreTextView.setTextColor(getResources().getColor(
            finalScore >= 70 ? android.R.color.holo_green_dark : 
            finalScore >= 50 ? android.R.color.holo_orange_dark : 
            android.R.color.holo_red_dark
        ));
        
        // Resumen
        String status = finalScore >= 70 ? "¡Excelente comprensión!" : 
                       finalScore >= 50 ? "¡Buen trabajo!" : 
                       "¡Sigue practicando!";
        
        summaryTextView.setText(String.format(
            "%s\n\nTema: %s\nNivel: %s\n\nRespuestas correctas: %d de %d\n(70%% requerido para aprobar)",
            status, topic, level, correctAnswers, totalQuestions
        ));
    }
    
    private void setupDetailedResults() {
        if (questionResults != null) {
            for (int i = 0; i < questionResults.length; i++) {
                String questionText = (questions != null && i < questions.length) ? 
                    questions[i] : "Pregunta " + (i + 1);
                addQuestionResult(questionText, questionResults[i]);
            }
        }
    }
    
    private void addQuestionResult(String questionText, boolean isCorrect) {
        // Crear card para cada pregunta
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(8f);
        cardView.setCardElevation(4f);
        
        // Layout interno de la card
        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(16, 12, 16, 12);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Texto de la pregunta
        TextView questionNumberText = new TextView(this);
        questionNumberText.setText(questionText);
        questionNumberText.setTextSize(16);
        questionNumberText.setTextColor(getResources().getColor(android.R.color.black));
        questionNumberText.setMaxLines(2);
        questionNumberText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // Espacio
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0);
        spacerParams.weight = 1;
        spacer.setLayoutParams(spacerParams);
        
        // Estado (correcto/incorrecto)
        TextView statusText = new TextView(this);
        statusText.setText(isCorrect ? "✓" : "✗");
        statusText.setTextSize(18);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setTextColor(getResources().getColor(
            isCorrect ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
        ));
        
        // Agregar elementos al layout
        cardContent.addView(questionNumberText);
        cardContent.addView(spacer);
        cardContent.addView(statusText);
        
        cardView.addView(cardContent);
        detailsContainer.addView(cardView);
    }
    
    private void setupButtons() {
        btnBackToMap.setOnClickListener(v -> {
            Intent intent = new Intent(ReadingResultsActivity.this, MenuReadingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(ReadingResultsActivity.this, TranslationReadingActivity.class);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("LEVEL", level);
            startActivity(intent);
            finish();
        });
        
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(ReadingResultsActivity.this, ReadingHistoryActivity.class);
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true);
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("SCORE", finalScore);
            startActivity(intent);
            finish();
        });
    }

    private void setupContinueButton() {
        // Si el score es menor a 70%, mostrar botón "Try again"
        if (finalScore < 70) {
            btnContinue.setVisibility(View.GONE);
            btnReintentar.setVisibility(View.VISIBLE);

            btnReintentar.setOnClickListener(v -> {
                // Reiniciar la misma actividad
                Intent intent = new Intent(this, TranslationReadingActivity.class);
                intent.putExtra("TOPIC", topic);
                intent.putExtra("LEVEL", level);
                startActivity(intent);
                finish();
            });
        }
        // Si el score es >= 70%, mostrar "Continue"
        else if (finalScore >= 70) {
            btnReintentar.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
            String nextTopic = ProgressionHelper.getNextReadingTopic(topic);

            if (nextTopic != null) {
                // Hay un siguiente tema disponible
                btnContinue.setText("➡️ Continuar: " + nextTopic);

                btnContinue.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, topic, finalScore);

                    // Ir al siguiente tema de reading
                    Intent intent = new Intent(this, TranslationReadingActivity.class);
                    intent.putExtra("TOPIC", nextTopic);
                    intent.putExtra("LEVEL", level);
                    startActivity(intent);
                    finish();
                });
            } else {
                // Es el último tema de reading, ir al mapa de writing
                btnContinue.setText("✍️ ¡Desbloquear Writing!");

                btnContinue.setOnClickListener(v -> {
                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(this, topic, finalScore);

                    // Ir al mapa de writing
                    Intent intent = new Intent(this, MenuWritingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }
        }
    }
} 