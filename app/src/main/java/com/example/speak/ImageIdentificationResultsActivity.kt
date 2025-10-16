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

class ImageIdentificationResultsActivity : AppCompatActivity() {
    private var birdImageView: ImageView? = null
    private var resultTitleTextView: TextView? = null
    private var finalScoreTextView: TextView? = null
    private var summaryTextView: TextView? = null
    private var detailsContainer: LinearLayout? = null
    private var btnBackToMap: Button? = null
    private var btnRetry: Button? = null
    private var btnContinue: Button? = null
    private var btnViewDetails: LinearLayout? = null
    private var messageTextView: TextView? = null
    private var counterTextView: TextView? = null

    private var finalScore = 0
    private var totalQuestions = 0
    private var correctAnswers = 0
    private var topic: String? = null
    private var level: String? = null
    private var questionResults: BooleanArray? = null
    private var questions: Array<String?>? = null
    private var sessionTimestamp: Long = 0
    private var sourceMap: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_identification_results)


        // Obtener datos del intent
        val intent = getIntent()
        finalScore = intent.getIntExtra("FINAL_SCORE", 0)
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        topic = intent.getStringExtra("TOPIC")
        level = intent.getStringExtra("LEVEL")
        questionResults = intent.getBooleanArrayExtra("QUESTION_RESULTS")
        questions = intent.getStringArrayExtra("QUESTIONS")
        sessionTimestamp = intent.getLongExtra("SESSION_TIMESTAMP", -1)
        sourceMap = intent.getStringExtra("SOURCE_MAP")


        // Inicializar vistas
        initViews()


        // Configurar interfaz
        setupBirdImage()
        setupTexts()
        setupDetailedResults()
        setupButtons()
        setupContinueButton()
    }

    private fun initViews() {
        birdImageView = findViewById<ImageView>(R.id.birdImageView)
        messageTextView = findViewById<TextView>(R.id.messageTextView)
        counterTextView = findViewById<TextView>(R.id.counterTextView)
        resultTitleTextView = findViewById<TextView>(R.id.resultTitleTextView)
        finalScoreTextView = findViewById<TextView>(R.id.finalScoreTextView)
        summaryTextView = findViewById<TextView>(R.id.summaryTextView)
        detailsContainer = findViewById<LinearLayout>(R.id.detailsContainer)
        btnBackToMap = findViewById<Button>(R.id.btnBackToMap)
        btnRetry = findViewById<Button>(R.id.btnRetry)
        btnContinue = findViewById<Button>(R.id.btnContinue)
        btnViewDetails = findViewById<LinearLayout>(R.id.btnViewDetails)
    }

    private fun setupBirdImage() {
        // Set bird image based on score (misma lógica que ListeningActivity)
        if (finalScore >= 100) {
            messageTextView!!.setText("Excellent your English is getting better!")
            birdImageView!!.setImageResource(R.drawable.crab_ok)
        } else if (finalScore >= 90) {
            messageTextView!!.setText("Good, but you can do it better!")
            birdImageView!!.setImageResource(R.drawable.crab_ok)
        } else if (finalScore >= 80) {
            messageTextView!!.setText("Good, but you can do it better!")
            birdImageView!!.setImageResource(R.drawable.crab_ok)
        } else if (finalScore >= 69) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_test)
        } else if (finalScore >= 60) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_test)
        } else if (finalScore >= 50) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        } else if (finalScore >= 40) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        } else if (finalScore >= 30) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        } else if (finalScore >= 20) {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        } else {
            messageTextView!!.setText("You should practice more!")
            birdImageView!!.setImageResource(R.drawable.crab_bad)
        }

        counterTextView!!.setText(correctAnswers.toString() + "/" + totalQuestions)
    }

    private fun setupTexts() {
        // Título del resultado
        resultTitleTextView!!.setText("🖼️ Resultados de Identificación de Imágenes")


        // Puntaje final
        finalScoreTextView!!.setText("Score: " + finalScore + "%")

        /*finalScoreTextView.setTextColor(getResources().getColor(
            finalScore >= 70 ? android.R.color.white :
            finalScore >= 50 ? android.R.color.holo_orange_dark : 
            android.R.color.holo_red_dark
        ));*/

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

    private fun setupDetailedResults() {
        if (questionResults == null || questions == null) {
            return
        }

        detailsContainer!!.removeAllViews()

        for (i in questionResults!!.indices) {
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
                    if (questionResults!![i]) R.color.white else R.color.error_color
                )
            )

            val cardContent = LinearLayout(this)
            cardContent.setOrientation(LinearLayout.VERTICAL)
            cardContent.setPadding(16, 16, 16, 16)

            val questionText = TextView(this)
            questionText.setText("Pregunta " + (i + 1) + ": " + questions!![i])
            questionText.setTextSize(16f)
            questionText.setTextColor(getResources().getColor(R.color.black))
            questionText.setLayoutParams(
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            val resultText = TextView(this)
            resultText.setText(if (questionResults!![i]) "✅ Correcta" else "❌ Incorrecta")
            resultText.setTextSize(14f)
            resultText.setTextColor(getResources().getColor(R.color.black))
            resultText.setLayoutParams(
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            cardContent.addView(questionText)
            cardContent.addView(resultText)
            cardView.addView(cardContent)
            detailsContainer!!.addView(cardView)
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

        btnViewDetails!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Abrir la actividad de historial con los datos actuales
            val intent = Intent(this, ImageIdentificationHistoryActivity::class.java)
            intent.putExtra("TOPIC", topic)
            intent.putExtra("FINAL_SCORE", finalScore)
            intent.putExtra("TOTAL_QUESTIONS", totalQuestions)
            intent.putExtra("CORRECT_ANSWERS", correctAnswers)
            intent.putExtra("LEVEL", level)
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp)
            intent.putExtra("SOURCE_MAP", sourceMap)
            startActivity(intent)
        })
    }

    private fun setupContinueButton() {
        if (finalScore >= 70) {
            if ("READING" == sourceMap) {
                // Si viene del mapa de Reading, usar la progresión de Reading
                val nextReadingTopic =
                    ProgressionHelper.getNextImageIdentificationTopicBySource(topic, sourceMap)

                if (nextReadingTopic != null) {
                    // Hay siguiente tema en Reading
                    btnContinue!!.setText("Continuar: " + nextReadingTopic)
                    btnContinue!!.setVisibility(View.VISIBLE)
                    btnContinue!!.setOnClickListener(View.OnClickListener { v: View? ->
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
                    btnContinue!!.setText("✍️ ¡Desbloquear Writing!")
                    btnContinue!!.setVisibility(View.VISIBLE)
                    btnContinue!!.setOnClickListener(View.OnClickListener { v: View? ->
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
                    btnContinue!!.setText("Continuar: " + nextTopic)
                    btnContinue!!.setVisibility(View.VISIBLE)
                    btnContinue!!.setOnClickListener(View.OnClickListener { v: View? ->
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

                        btnContinue!!.setText("🎯 ¡Desbloquear " + nextModuleName + "!")
                        btnContinue!!.setVisibility(View.VISIBLE)
                        btnContinue!!.setOnClickListener(View.OnClickListener { v: View? ->
                            val intent = Intent(this, nextModuleClass)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        })
                    } else {
                        // No es el último tema, volver al mapa actual
                        val destinationMapClass =
                            ProgressionHelper.getDestinationMapClass(sourceMap)
                        btnContinue!!.setText("🎯 ¡Completar módulo!")
                        btnContinue!!.setVisibility(View.VISIBLE)
                        btnContinue!!.setOnClickListener(View.OnClickListener { v: View? ->
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
            btnContinue!!.setVisibility(View.GONE)
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