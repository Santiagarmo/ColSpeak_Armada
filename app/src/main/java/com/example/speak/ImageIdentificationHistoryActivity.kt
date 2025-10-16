package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.speak.database.DatabaseHelper
import kotlin.math.abs

class ImageIdentificationHistoryActivity : AppCompatActivity() {
    private var birdImageView: ImageView? = null
    private var resultTitleTextView: TextView? = null
    private var finalScoreTextView: TextView? = null
    private var summaryTextView: TextView? = null
    private var detailsContainer: LinearLayout? = null
    private var btnBackToMap: Button? = null
    private var btnRetry: Button? = null
    private var continueButton: Button? = null
    private var btnViewDetails: Button? = null

    private var dbHelper: DatabaseHelper? = null
    private var finalScore = 0
    private var totalQuestions = 0
    private var correctAnswers = 0
    private var topic: String? = null
    private var level: String? = null
    private var sessionTimestamp: Long = 0
    private var sourceMap: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_identification_results)


        // Inicializar base de datos
        dbHelper = DatabaseHelper(this)


        // Obtener datos del intent
        val intent = getIntent()
        finalScore = intent.getIntExtra("FINAL_SCORE", 0)
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        topic = intent.getStringExtra("TOPIC")
        level = intent.getStringExtra("LEVEL")
        sessionTimestamp = intent.getLongExtra("SESSION_TIMESTAMP", -1)
        sourceMap = intent.getStringExtra("SOURCE_MAP")


        // Inicializar vistas
        initViews()


        // Configurar interfaz
        setupBirdImage()
        setupTexts()
        loadHistoryData()
        setupButtons()
        setupContinueButton()
    }

    private fun initViews() {
        birdImageView = findViewById<ImageView>(R.id.birdImageView)
        resultTitleTextView = findViewById<TextView>(R.id.resultTitleTextView)
        finalScoreTextView = findViewById<TextView>(R.id.finalScoreTextView)
        summaryTextView = findViewById<TextView>(R.id.summaryTextView)
        detailsContainer = findViewById<LinearLayout>(R.id.detailsContainer)
        btnBackToMap = findViewById<Button>(R.id.btnBackToMap)
        btnRetry = findViewById<Button>(R.id.btnRetry)
        continueButton = findViewById<Button>(R.id.btnContinue)
        btnViewDetails = findViewById<Button>(R.id.btnViewDetails)
    }

    private fun setupBirdImage() {
        // Configurar imagen del pájaro según el puntaje
        if (finalScore >= 70) {
            birdImageView!!.setImageResource(R.drawable.crab_ok)
        } else if (finalScore >= 50) {
            birdImageView!!.setImageResource(R.drawable.crab_test)
        } else {
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        }
    }

    private fun setupTexts() {
        // Título del resultado
        resultTitleTextView!!.setText("🖼️ Historial de Identificación de Imágenes")


        // Puntaje final
        finalScoreTextView!!.setText(finalScore.toString() + "%")
        finalScoreTextView!!.setTextColor(
            getResources().getColor(
                if (finalScore >= 70) R.color.white else if (finalScore >= 50) R.color.orangered else R.color.error_color
            )
        )


        // Resumen
        val status =
            if (finalScore >= 70) "¡Excelente identificación!" else if (finalScore >= 50) "¡Buen intento!" else "¡Sigue practicando!"

        summaryTextView!!.setText(
            String.format(
                "%s\n\nTema: %s\nNivel: %s\n\nRespuestas correctas: %d de %d\n(70%% requerido para aprobar)",
                status, topic, level, correctAnswers, totalQuestions
            )
        )
    }

    private fun loadHistoryData() {
        if (sessionTimestamp == -1L) {
            return
        }

        detailsContainer!!.removeAllViews()


        // Obtener datos del historial de la base de datos
        val cursor = dbHelper!!.getQuizHistory()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val timestamp =
                    cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP))
                val quizType =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE))
                val questionText =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION))
                val userAnswer =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER))
                val correctAnswer =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER))
                val isCorrect =
                    cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1


                // Solo mostrar resultados de Image Identification de la sesión actual
                if (quizType != null && quizType.contains("Image Identification") && abs(timestamp - sessionTimestamp) < 60000) { // Dentro de 1 minuto

                    val cardView = CardView(this)
                    val cardParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    cardParams.setMargins(0, 8, 0, 8)
                    cardView.setLayoutParams(cardParams)
                    cardView.setRadius(8f)
                    cardView.setCardBackgroundColor(
                        getResources().getColor(
                            if (isCorrect) R.color.success_color else R.color.error_color
                        )
                    )

                    val cardContent = LinearLayout(this)
                    cardContent.setOrientation(LinearLayout.VERTICAL)
                    cardContent.setPadding(16, 16, 16, 16)

                    val questionTextView = TextView(this)
                    questionTextView.setText("Pregunta: " + questionText)
                    questionTextView.setTextSize(16f)
                    questionTextView.setTextColor(getResources().getColor(R.color.black))
                    questionTextView.setLayoutParams(
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )

                    val answerText = TextView(this)
                    answerText.setText("Tu respuesta: " + userAnswer + "\nRespuesta correcta: " + correctAnswer)
                    answerText.setTextSize(14f)
                    answerText.setTextColor(getResources().getColor(R.color.black))
                    answerText.setLayoutParams(
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )

                    val resultText = TextView(this)
                    resultText.setText(if (isCorrect) "✅ Correcta" else "❌ Incorrecta")
                    resultText.setTextSize(14f)
                    resultText.setTextColor(getResources().getColor(R.color.black))
                    resultText.setLayoutParams(
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )

                    cardContent.addView(questionTextView)
                    cardContent.addView(answerText)
                    cardContent.addView(resultText)
                    cardView.addView(cardContent)
                    detailsContainer!!.addView(cardView)
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    private fun setupButtons() {
        btnBackToMap!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Volver al mapa correspondiente según el origen
            val destinationMapClass = ProgressionHelper.getDestinationMapClass(sourceMap)
            val intent = Intent(this, destinationMapClass)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        })

        btnRetry!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Volver a la actividad de Image Identification con el mapa de origen
            val intent = Intent(this, ImageIdentificationActivity::class.java)
            intent.putExtra("TOPIC", topic)
            intent.putExtra("LEVEL", level)
            intent.putExtra("SOURCE_MAP", sourceMap)
            startActivity(intent)
            finish()
        })

        btnViewDetails!!.setVisibility(View.GONE) // Ya estamos en la vista de detalles
    }

    private fun setupContinueButton() {
        if (finalScore >= 70) {
            if ("READING" == sourceMap) {
                // Si viene del mapa de Reading, usar la progresión de Reading
                val nextReadingTopic =
                    ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap)

                if (nextReadingTopic != null) {
                    // Hay siguiente tema en Reading
                    continueButton!!.setText("➡️ Continuar: " + nextReadingTopic)
                    continueButton!!.setVisibility(View.VISIBLE)
                    continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                        // Determinar qué actividad usar según el tema
                        val nextActivityClass =
                            ProgressionHelper.getReadingActivityClass(nextReadingTopic)


                        // Si vamos a TranslationReadingActivity, marcar el tema como desbloqueado
                        if (nextActivityClass == TranslationReadingActivity::class.java) {
                            ProgressionHelper.markTopicCompleted(this, nextReadingTopic, 70)
                        }

                        val intent = Intent(this, nextActivityClass)
                        intent.putExtra("TOPIC", nextReadingTopic)
                        intent.putExtra("LEVEL", level)
                        if (nextActivityClass == ImageIdentificationActivity::class.java ||
                            nextActivityClass == ImageIdentificationAudioActivity::class.java
                        ) {
                            intent.putExtra("SOURCE_MAP", sourceMap)
                        }
                        startActivity(intent)
                        finish()
                    })
                } else {
                    // Es el último tema de Reading, desbloquear Writing
                    continueButton!!.setText("✍️ ¡Desbloquear Writing!")
                    continueButton!!.setVisibility(View.VISIBLE)
                    continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                        val intent = Intent(this, MenuWritingActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    })
                }
            } else {
                // Si viene del mapa de Listening, usar la progresión de Image Identification
                val nextTopic =
                    ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap)

                if (nextTopic != null) {
                    // Hay siguiente tema en Image Identification
                    continueButton!!.setText("➡️ Continuar: " + nextTopic)
                    continueButton!!.setVisibility(View.VISIBLE)
                    continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                        val intent = Intent(this, ImageIdentificationActivity::class.java)
                        intent.putExtra("TOPIC", nextTopic)
                        intent.putExtra("LEVEL", level)
                        intent.putExtra("SOURCE_MAP", sourceMap)
                        startActivity(intent)
                        finish()
                    })
                } else {
                    // Es el último tema del módulo, desbloquear siguiente módulo
                    if (ProgressionHelper.isLastTopicOfModule(topic, sourceMap)) {
                        // Determinar el siguiente módulo en la progresión
                        val nextModuleClass = ProgressionHelper.getNextModuleClass(sourceMap)
                        val nextModuleName = getModuleDisplayName(sourceMap!!)

                        continueButton!!.setText("🎯 ¡Desbloquear " + nextModuleName + "!")
                        continueButton!!.setVisibility(View.VISIBLE)
                        continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                            val intent = Intent(this, nextModuleClass)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        })
                    } else {
                        // No es el último tema, volver al mapa actual
                        val destinationMapClass =
                            ProgressionHelper.getDestinationMapClass(sourceMap)
                        continueButton!!.setText("🎯 ¡Completar módulo!")
                        continueButton!!.setVisibility(View.VISIBLE)
                        continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                            val intent = Intent(this, destinationMapClass)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        })
                    }
                }
            }
        } else {
            // No aprobó, ocultar botón continuar
            continueButton!!.setVisibility(View.GONE)
        }
    }

    /**
     * Obtiene el nombre de visualización del módulo
     */
    private fun getModuleDisplayName(sourceMap: String): String {
        when (sourceMap) {
            "LISTENING" -> return "Speaking"
            "SPEAKING" -> return "Reading"
            "READING" -> return "Writing"
            "WRITING" -> return "Listening"
            else -> return "Siguiente Módulo"
        }
    }
}