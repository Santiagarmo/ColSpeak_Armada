package com.example.speak;

import android.content.Intent;
import android.database.Cursor;
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
import com.example.speak.database.DatabaseHelper;

public class ImageIdentificationHistoryActivity extends AppCompatActivity {
    
    private ImageView birdImageView;
    private TextView resultTitleTextView;
    private TextView finalScoreTextView;
    private TextView summaryTextView;
    private LinearLayout detailsContainer;
    private Button btnBackToMap;
    private Button btnRetry;
    private Button continueButton;
    private Button btnViewDetails;
    
    private DatabaseHelper dbHelper;
    private int finalScore;
    private int totalQuestions;
    private int correctAnswers;
    private String topic;
    private String level;
    private long sessionTimestamp;
    private String sourceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_identification_results);
        
        // Inicializar base de datos
        dbHelper = new DatabaseHelper(this);
        
        // Obtener datos del intent
        Intent intent = getIntent();
        finalScore = intent.getIntExtra("FINAL_SCORE", 0);
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0);
        topic = intent.getStringExtra("TOPIC");
        level = intent.getStringExtra("LEVEL");
        sessionTimestamp = intent.getLongExtra("SESSION_TIMESTAMP", -1);
        sourceMap = intent.getStringExtra("SOURCE_MAP");
        
        // Inicializar vistas
        initViews();
        
        // Configurar interfaz
        setupBirdImage();
        setupTexts();
        loadHistoryData();
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
        continueButton = findViewById(R.id.btnContinue);
        btnViewDetails = findViewById(R.id.btnViewDetails);
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
        resultTitleTextView.setText("🖼️ Historial de Identificación de Imágenes");
        
        // Puntaje final
        finalScoreTextView.setText(finalScore + "%");
        finalScoreTextView.setTextColor(getResources().getColor(
            finalScore >= 70 ? android.R.color.white :
            finalScore >= 50 ? android.R.color.holo_orange_dark : 
            android.R.color.holo_red_dark
        ));
        
        // Resumen
        String status = finalScore >= 70 ? "¡Excelente identificación!" : 
                       finalScore >= 50 ? "¡Buen intento!" : 
                       "¡Sigue practicando!";
        
        summaryTextView.setText(String.format(
            "%s\n\nTema: %s\nNivel: %s\n\nRespuestas correctas: %d de %d\n(70%% requerido para aprobar)",
            status, topic, level, correctAnswers, totalQuestions
        ));
    }
    
    private void loadHistoryData() {
        if (sessionTimestamp == -1) {
            return;
        }
        
        detailsContainer.removeAllViews();
        
        // Obtener datos del historial de la base de datos
        Cursor cursor = dbHelper.getQuizHistory();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                String quizType = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE));
                String questionText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION));
                String userAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER));
                String correctAnswer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER));
                boolean isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1;
                
                // Solo mostrar resultados de Image Identification de la sesión actual
                if (quizType != null && quizType.contains("Image Identification") && 
                    Math.abs(timestamp - sessionTimestamp) < 60000) { // Dentro de 1 minuto
                    
                    CardView cardView = new CardView(this);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cardParams.setMargins(0, 8, 0, 8);
                    cardView.setLayoutParams(cardParams);
                    cardView.setRadius(8);
                    cardView.setCardBackgroundColor(getResources().getColor(
                        isCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light
                    ));
                    
                    LinearLayout cardContent = new LinearLayout(this);
                    cardContent.setOrientation(LinearLayout.VERTICAL);
                    cardContent.setPadding(16, 16, 16, 16);
                    
                    TextView questionTextView = new TextView(this);
                    questionTextView.setText("Pregunta: " + questionText);
                    questionTextView.setTextSize(16);
                    questionTextView.setTextColor(getResources().getColor(android.R.color.black));
                    questionTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    
                    TextView answerText = new TextView(this);
                    answerText.setText("Tu respuesta: " + userAnswer + "\nRespuesta correcta: " + correctAnswer);
                    answerText.setTextSize(14);
                    answerText.setTextColor(getResources().getColor(android.R.color.black));
                    answerText.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    
                    TextView resultText = new TextView(this);
                    resultText.setText(isCorrect ? "✅ Correcta" : "❌ Incorrecta");
                    resultText.setTextSize(14);
                    resultText.setTextColor(getResources().getColor(android.R.color.black));
                    resultText.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    
                    cardContent.addView(questionTextView);
                    cardContent.addView(answerText);
                    cardContent.addView(resultText);
                    cardView.addView(cardContent);
                    detailsContainer.addView(cardView);
                }
            } while (cursor.moveToNext());
            cursor.close();
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
        
        btnViewDetails.setVisibility(View.GONE); // Ya estamos en la vista de detalles
    }
    
    private void setupContinueButton() {
        if (finalScore >= 70) {
            if ("READING".equals(sourceMap)) {
                // Si viene del mapa de Reading, usar la progresión de Reading
                String nextReadingTopic = ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap);
                
                if (nextReadingTopic != null) {
                    // Hay siguiente tema en Reading
                    continueButton.setText("➡️ Continuar: " + nextReadingTopic);
                    continueButton.setVisibility(View.VISIBLE);
                    continueButton.setOnClickListener(v -> {
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
                    continueButton.setText("✍️ ¡Desbloquear Writing!");
                    continueButton.setVisibility(View.VISIBLE);
                    continueButton.setOnClickListener(v -> {
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
                    continueButton.setText("➡️ Continuar: " + nextTopic);
                    continueButton.setVisibility(View.VISIBLE);
                    continueButton.setOnClickListener(v -> {
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
                        
                        continueButton.setText("🎯 ¡Desbloquear " + nextModuleName + "!");
                        continueButton.setVisibility(View.VISIBLE);
                        continueButton.setOnClickListener(v -> {
                            Intent intent = new Intent(this, nextModuleClass);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        // No es el último tema, volver al mapa actual
                        Class<?> destinationMapClass = ProgressionHelper.getDestinationMapClass(sourceMap);
                        continueButton.setText("🎯 ¡Completar módulo!");
                        continueButton.setVisibility(View.VISIBLE);
                        continueButton.setOnClickListener(v -> {
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
            continueButton.setVisibility(View.GONE);
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