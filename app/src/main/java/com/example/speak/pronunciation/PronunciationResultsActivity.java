package com.example.speak.pronunciation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.speak.MainActivity;
import com.example.speak.ProfileActivity;
import com.example.speak.PronunciationHistoryActivity;
import com.example.speak.QuizHistoryActivity;
import com.example.speak.R;
import com.example.speak.MenuSpeakingActivity;

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

    private LinearLayout eBtnReturnMenu;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

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

        //Declaramos las variables Menu
        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        eButtonProfile = findViewById(R.id.btnProfile);
        homeButton = findViewById(R.id.homeButton);
        
        // Inicializar vistas
        initViews();
        
        // Configurar interfaz
        setupBirdImage();
        setupTexts();
        setupDetailedResults();
        setupButtons();

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
    
    private void setupDetailedResults() {
        if (individualScores != null) {
            for (int i = 0; i < individualScores.length; i++) {
                addQuestionResult(i + 1, individualScores[i]);
            }
        }
    }
    
    private void addQuestionResult(int questionNumber, double score) {
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
        
        // Número de pregunta
        TextView questionNumberText = new TextView(this);
        questionNumberText.setText("Pregunta " + questionNumber);
        questionNumberText.setTextSize(16);
        questionNumberText.setTextColor(getResources().getColor(android.R.color.black));
        
        // Espacio
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0);
        spacerParams.weight = 1;
        spacer.setLayoutParams(spacerParams);
        
        // Puntaje
        TextView scoreText = new TextView(this);
        scoreText.setText(String.format("%.1f%%", score));
        scoreText.setTextSize(16);
        scoreText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Estado (aprobado/reprobado)
        TextView statusText = new TextView(this);
        if (score >= 70) {
            statusText.setText("✅ Aprobada");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            scoreText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusText.setText("❌ Reprobada");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            scoreText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        statusText.setTextSize(14);
        
        // Agregar vistas al layout
        cardContent.addView(questionNumberText);
        cardContent.addView(spacer);
        cardContent.addView(scoreText);
        cardContent.addView(statusText);
        
        cardView.addView(cardContent);
        detailsContainer.addView(cardView);
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

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(PronunciationResultsActivity.this, MainActivity.class));
        Toast.makeText(PronunciationResultsActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }
} 