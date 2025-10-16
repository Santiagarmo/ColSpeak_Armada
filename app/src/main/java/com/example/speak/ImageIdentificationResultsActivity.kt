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

public class ImageIdentificationResultsActivity extends AppCompatActivity {
    
    private ImageView birdImageView;
    private TextView resultTitleTextView;
    private TextView finalScoreTextView;
    private TextView summaryTextView;
    private LinearLayout detailsContainer;
    private Button btnBackToMap;
    private Button btnRetry;
    private Button btnContinue;
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
    private String sourceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_identification_results);
        
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
        sourceMap = intent.getStringExtra("SOURCE_MAP");
        
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
        messageTextView = findViewById(R.id.messageTextView);
        counterTextView = findViewById(R.id.counterTextView);
        resultTitleTextView = findViewById(R.id.resultTitleTextView);
        finalScoreTextView = findViewById(R.id.finalScoreTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        detailsContainer = findViewById(R.id.detailsContainer);
        btnBackToMap = findViewById(R.id.btnBackToMap);
        btnRetry = findViewById(R.id.btnRetry);
        btnContinue = findViewById(R.id.btnContinue);
        btnViewDetails = findViewById(R.id.btnViewDetails);
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
        resultTitleTextView.setText("🖼️ Resultados de Identificación de Imágenes");
        
        // Puntaje final
        finalScoreTextView.setText("Score: " + finalScore + "%");
        /*finalScoreTextView.setTextColor(getResources().getColor(
            finalScore >= 70 ? android.R.color.white :
            finalScore >= 50 ? android.R.color.holo_orange_dark : 
            android.R.color.holo_red_dark
        ));*/
        
        // Resumen
        String status = finalScore >= 70 ? "¡Excelente identificación!" : 
                       finalScore >= 50 ? "¡Buen intento!" : 
                       "¡Sigue practicando!";
        
        summaryTextView.setText(String.format(
            "%s\n\nTema: %s\nNivel: %s\n\nRespuestas correctas: %d de %d\n(70%% requerido para aprobar)",
            status, topic, level, correctAnswers, totalQuestions
        ));
    }
    
    private void setupDetailedResults() {
        if (questionResults == null || questions == null) {
            return;
        }
        
        detailsContainer.removeAllViews();
        
        for (int i = 0; i < questionResults.length; i++) {
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 8, 0, 8);
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(8);
            cardView.setCardBackgroundColor(getResources().getColor(
                questionResults[i] ? android.R.color.white : android.R.color.holo_red_light
            ));
            
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(16, 16, 16, 16);
            
            TextView questionText = new TextView(this);
            questionText.setText("Pregunta " + (i + 1) + ": " + questions[i]);
            questionText.setTextSize(16);
            questionText.setTextColor(getResources().getColor(android.R.color.black));
            questionText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            TextView resultText = new TextView(this);
            resultText.setText(questionResults[i] ? "✅ Correcta" : "❌ Incorrecta");
            resultText.setTextSize(14);
            resultText.setTextColor(getResources().getColor(android.R.color.black));
            resultText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            cardContent.addView(questionText);
            cardContent.addView(resultText);
            cardView.addView(cardContent);
            detailsContainer.addView(cardView);
        }
    }
    
    private void setupButtons() {
        btnBackToMap.setOnClickListener(v -> {
            // Volver al mapa correspondiente según el origen
            Class<?> destinationMapClass = ProgressionHelper.getDestinationMapClass(sourceMap);
            Intent intent = new Intent(this, destinationMapClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        btnRetry.setOnClickListener(v -> {
            // Volver a la actividad de Image Identification con el mapa de origen
            Intent intent = new Intent(this, ImageIdentificationActivity.class);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("LEVEL", level);
            intent.putExtra("SOURCE_MAP", sourceMap);
            startActivity(intent);
            finish();
        });
        
        btnViewDetails.setOnClickListener(v -> {
            // Abrir la actividad de historial con los datos actuales
            Intent intent = new Intent(this, ImageIdentificationHistoryActivity.class);
            intent.putExtra("TOPIC", topic);
            intent.putExtra("FINAL_SCORE", finalScore);
            intent.putExtra("TOTAL_QUESTIONS", totalQuestions);
            intent.putExtra("CORRECT_ANSWERS", correctAnswers);
            intent.putExtra("LEVEL", level);
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp);
            intent.putExtra("SOURCE_MAP", sourceMap);
            startActivity(intent);
        });
    }
    
    private void setupContinueButton() {
        if (finalScore >= 70) {
            if ("READING".equals(sourceMap)) {
                // Si viene del mapa de Reading, usar la progresión de Reading
                String nextReadingTopic = ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap);
                
                if (nextReadingTopic != null) {
                    // Hay siguiente tema en Reading
                    btnContinue.setText("Continuar: " + nextReadingTopic);
                    btnContinue.setVisibility(View.VISIBLE);
                    btnContinue.setOnClickListener(v -> {
                        // Determinar qué actividad usar según el tema
                        Class<?> nextActivityClass = ProgressionHelper.getReadingActivityClass(nextReadingTopic);
                        
                        // Si vamos a TranslationReadingActivity, marcar el tema como desbloqueado
                        if (nextActivityClass == TranslationReadingActivity.class) {
                            ProgressionHelper.markTopicCompleted(this, nextReadingTopic, 70);
                        }
                        
                        Intent intent = new Intent(this, nextActivityClass);
                        intent.putExtra("TOPIC", nextReadingTopic);
                        intent.putExtra("LEVEL", level);
                        if (nextActivityClass == ImageIdentificationActivity.class || 
                            nextActivityClass == ImageIdentificationAudioActivity.class) {
                            intent.putExtra("SOURCE_MAP", sourceMap);
                        }
                        startActivity(intent);
                        finish();
                    });
                } else {
                    // Es el último tema de Reading, desbloquear Writing
                    btnContinue.setText("✍️ ¡Desbloquear Writing!");
                    btnContinue.setVisibility(View.VISIBLE);
                    btnContinue.setOnClickListener(v -> {
                        Intent intent = new Intent(this, MenuWritingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            } else {
                // Si viene del mapa de Listening, usar la progresión de Image Identification
                String nextTopic = ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap);
                
                if (nextTopic != null) {
                    // Hay siguiente tema en Image Identification
                    btnContinue.setText("Continuar: " + nextTopic);
                    btnContinue.setVisibility(View.VISIBLE);
                    btnContinue.setOnClickListener(v -> {
                        Intent intent = new Intent(this, ImageIdentificationActivity.class);
                        intent.putExtra("TOPIC", nextTopic);
                        intent.putExtra("LEVEL", level);
                        intent.putExtra("SOURCE_MAP", sourceMap);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    // Es el último tema del módulo, desbloquear siguiente módulo
                    if (ProgressionHelper.isLastTopicOfModule(topic, sourceMap)) {
                        // Determinar el siguiente módulo en la progresión
                        Class<?> nextModuleClass = ProgressionHelper.getNextModuleClass(sourceMap);
                        String nextModuleName = getModuleDisplayName(sourceMap);
                        
                        btnContinue.setText("🎯 ¡Desbloquear " + nextModuleName + "!");
                        btnContinue.setVisibility(View.VISIBLE);
                        btnContinue.setOnClickListener(v -> {
                            Intent intent = new Intent(this, nextModuleClass);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        // No es el último tema, volver al mapa actual
                        Class<?> destinationMapClass = ProgressionHelper.getDestinationMapClass(sourceMap);
                        btnContinue.setText("🎯 ¡Completar módulo!");
                        btnContinue.setVisibility(View.VISIBLE);
                        btnContinue.setOnClickListener(v -> {
                            Intent intent = new Intent(this, destinationMapClass);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        });
                    }
                }
            }
        } else {
            // No aprobó, ocultar botón continuar
            btnContinue.setVisibility(View.GONE);
        }
    }
    
    /**
     * Obtiene el nombre de visualización del módulo
     */
    private String getModuleDisplayName(String sourceMap) {
        switch (sourceMap) {
            case "LISTENING":
                return "Speaking";
            case "SPEAKING":
                return "Reading";
            case "READING":
                return "Writing";
            case "WRITING":
                return "Listening";
            default:
                return "Siguiente Módulo";
        }
    }
} 