package com.example.speak

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.PictureDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.speak.components.ReusableAudioPlayerCard
import com.example.speak.database.DatabaseHelper
import com.example.speak.helpers.HelpModalHelper
import com.example.speak.helpers.StarEarnedDialog
import com.example.speak.helpers.StarProgressHelper
import com.example.speak.helpers.VideoHelper
import com.example.speak.helpers.WildcardHelper
import com.example.speak.helpers.WildcardHelper.WildcardCallbacks
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.dropLastWhile
import kotlin.collections.toTypedArray
import kotlin.math.min

class ImageIdentificationAudioActivity : AppCompatActivity() {
    // UI Elements
    private var questionNumberTextView: TextView? = null
    private var levelTextView: TextView? = null
    private var topicTextView: TextView? = null
    private var scoreTextView: TextView? = null
    private var hiddenWordTextView: TextView? = null
    private var nextButton: Button? = null

    // Option buttons (ImageButtons instead of regular Buttons)
    private var option1Button: ImageButton? = null
    private var option2Button: ImageButton? = null
    private var option3Button: ImageButton? = null
    private var option4Button: ImageButton? = null

    // Bird image for feedback
    private var birdImageView: ImageView? = null

    // ============================================================
    // NUEVO: Componente reutilizable de audio
    // ============================================================
    private var reusableAudioCard: ReusableAudioPlayerCard? = null
    private var lastReusablePlaying = false
    private val reusableMonitorHandler: Handler? = Handler(Looper.getMainLooper())
    private var reusableMonitorRunnable: Runnable? = null

    // MediaPlayer para sonidos de feedback
    private var correctSoundPlayer: MediaPlayer? = null
    private var incorrectSoundPlayer: MediaPlayer? = null

    // Quiz data
    private var allQuestions: MutableList<ImageQuestion?>? = null
    private var currentQuestions: MutableList<ImageQuestion>? = null
    private var currentQuestionIndex = 0
    private var score = 0

    // Database and Firebase
    private var dbHelper: DatabaseHelper? = null
    private var mAuth: FirebaseAuth? = null
    private var mDatabase: DatabaseReference? = null
    private var userId: Long = 0
    private var isOfflineMode = false
    private var sessionTimestamp: Long = 0

    // Topic and level
    private var selectedTopic: String? = null
    private var selectedLevel: String? = null

    // Return menu
    private var returnContainer: LinearLayout? = null
    private var wildcardButton: ImageView? = null
    private var helpButton: ImageView? = null

    // Variable para evitar repetir las mismas imágenes en preguntas consecutivas
    private val recentlyUsedImages: MutableList<String?> = ArrayList<String?>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_identification_audio)

        Log.d(TAG, "ImageIdentificationAudioActivity onCreate started")

        // Get intent data FIRST
        val intent = getIntent()
        selectedTopic = intent.getStringExtra("TOPIC")
        selectedLevel = intent.getStringExtra("LEVEL")

        Log.d(TAG, "Intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel)

        // Validate intent data IMMEDIATELY
        if (selectedTopic == null || selectedLevel == null) {
            Log.e(
                TAG,
                "Missing intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel
            )
            Toast.makeText(this, "Error: Datos de tema o nivel no encontrados.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        // Initialize views FIRST
        initializeViews()

        // Display topic and level in the interface AFTER views are initialized
        if (selectedLevel != null) {
            levelTextView!!.setText("Level: " + selectedLevel)
        }
        if (selectedTopic != null) {
            topicTextView!!.setText("Topic: " + selectedTopic)
        }

        // Initialize sound players
        initializeSoundPlayers()
        Log.d(TAG, "Sound players initialized")

        // Check network status
        isOfflineMode = !this.isNetworkAvailable
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode)

        // Initialize Firebase and Database
        initializeFirebaseAndDatabase()

        // ============================================================
        // NUEVO: Configurar componente reutilizable de audio
        // ============================================================
        setupReusableAudioCard()
        startReusablePlaybackMonitor()

        // Setup button listeners
        setupButtonListeners()

        // Setup option button listeners
        setupOptionButtonListeners()

        // Load questions
        loadQuestionsFromFile(selectedTopic, selectedLevel)

        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp)
    }

    private fun initializeViews() {
        questionNumberTextView = findViewById<TextView>(R.id.questionNumberTextView)
        levelTextView = findViewById<TextView>(R.id.levelTextView)
        topicTextView = findViewById<TextView>(R.id.topicTextView)
        scoreTextView = findViewById<TextView>(R.id.scoreTextView)
        hiddenWordTextView = findViewById<TextView>(R.id.hiddenWordTextView)
        nextButton = findViewById<Button>(R.id.nextButton)

        option1Button = findViewById<ImageButton>(R.id.option1Button)
        option2Button = findViewById<ImageButton>(R.id.option2Button)
        option3Button = findViewById<ImageButton>(R.id.option3Button)
        option4Button = findViewById<ImageButton>(R.id.option4Button)

        // Initialize bird image
        birdImageView = findViewById<ImageView?>(R.id.birdImageView)

        // ============================================================
        // NUEVO: Inicializar componente reutilizable de audio
        // ============================================================
        reusableAudioCard = findViewById<ReusableAudioPlayerCard?>(R.id.reusableAudioCard)

        returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
        helpButton = findViewById<ImageView?>(R.id.helpButton)

        // Set up return button click listener
        returnContainer!!.setOnClickListener(View.OnClickListener { v: View? -> returnToMenu() })

        // Wildcard modal integration
        wildcardButton = findViewById<ImageView?>(R.id.wildcardButton)
        if (wildcardButton != null) {
            wildcardButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val wildcardHelper = WildcardHelper(
                        this@ImageIdentificationAudioActivity,
                        "IMAGE_AUDIO",
                        selectedTopic
                    )
                    wildcardHelper.setCallbacks(object : WildcardCallbacks {
                        override fun onChangeQuestion() {
                            currentQuestionIndex =
                                min(currentQuestionIndex + 1, currentQuestions!!.size)
                            displayQuestion()
                        }

                        override fun onShowContentImage() {
                            Toast.makeText(
                                this@ImageIdentificationAudioActivity,
                                "Mostrando imagen de contenido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onShowInstructorVideo() {
                            try {
                                VideoHelper(this@ImageIdentificationAudioActivity)
                                    .showInstructorVideo(selectedTopic)
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error mostrando video del instructor: " + ex.message)
                                Toast.makeText(
                                    this@ImageIdentificationAudioActivity,
                                    "No se pudo reproducir el video",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onShowFiftyFifty() {
                            Toast.makeText(
                                this@ImageIdentificationAudioActivity,
                                "50/50 no aplica en esta vista",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onShowCreativeHelp() {
                            Toast.makeText(
                                this@ImageIdentificationAudioActivity,
                                "Ayuda creativa no disponible aquí",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onShowWildcardInfo() {
                            Toast.makeText(
                                this@ImageIdentificationAudioActivity,
                                "Información de comodines",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                    wildcardHelper.showWildcardMenu()
                } catch (e: Exception) {
                    Log.e(TAG, "Error mostrando comodines: " + e.message)
                }
            })
        }

        // Help modal integration
        if (helpButton != null) {
            helpButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    HelpModalHelper.show(
                        this@ImageIdentificationAudioActivity,
                        selectedTopic,
                        selectedLevel
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error abriendo modal de ayuda: " + e.message)
                    Toast.makeText(
                        this@ImageIdentificationAudioActivity,
                        "No se pudo abrir la ayuda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        Log.d(TAG, "Views initialized")
    }

    // ============================================================
    // NUEVO: Configuración del componente reutilizable de audio
    // ============================================================
    private fun setupReusableAudioCard() {
        try {
            if (reusableAudioCard != null) {
                // Obtener el texto de la pregunta actual
                var audioText: String? = ""
                if (currentQuestions != null && !currentQuestions!!.isEmpty() && currentQuestionIndex < currentQuestions!!.size) {
                    val q = currentQuestions!!.get(currentQuestionIndex)
                    audioText = q.correctAnswer
                }

                // ⚠️ IMPORTANTE: Este activity usa TTS, NO archivos MP3
                // Configurar con carpeta vacía para indicar que se usará TTS
                reusableAudioCard!!.configure("", audioText)

                // Forzar modo INGLÉS (TTS) - No hay archivos MP3 disponibles
                reusableAudioCard!!.setEnglishMode()

                Log.d(TAG, "ReusableAudioCard configurado en modo TTS con texto: " + audioText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up reusable audio card: " + e.message, e)
        }
    }

    // ============================================================
    // NUEVO: Monitor de reproducción para habilitar botones
    // ============================================================
    private fun startReusablePlaybackMonitor() {
        if (reusableAudioCard == null) return
        reusableMonitorRunnable = object : Runnable {
            override fun run() {
                try {
                    val isPlayingReusable = reusableAudioCard!!.isPlaying
                    if (isPlayingReusable != lastReusablePlaying) {
                        lastReusablePlaying = isPlayingReusable
                        if (isPlayingReusable) {
                            // Habilitar botones de opciones cuando comienza la reproducción
                            enableOptionButtons()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring reusable playback: " + e.message)
                } finally {
                    reusableMonitorHandler!!.postDelayed(this, 200)
                }
            }
        }
        reusableMonitorHandler!!.post(reusableMonitorRunnable!!)
    }

    private fun enableOptionButtons() {
        option1Button!!.setEnabled(true)
        option2Button!!.setEnabled(true)
        option3Button!!.setEnabled(true)
        option4Button!!.setEnabled(true)
        Log.d(TAG, "Botones de opciones habilitados después de reproducir audio")
    }

    private fun initializeFirebaseAndDatabase() {
        mAuth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val initialUserId = prefs.getLong("userId", 1)
        val userEmail = prefs.getString("userEmail", null)

        Log.d(TAG, "Initial user ID from prefs: " + initialUserId)
        Log.d(TAG, "User email from prefs: " + userEmail)

        if (isOfflineMode || mAuth!!.getCurrentUser() == null) {
            userId = initialUserId
        } else {
            userId = initialUserId
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode)

        if (!isOfflineMode) {
            mDatabase = FirebaseDatabase.getInstance().getReference()
        }

        Log.d(TAG, "Firebase and Database initialized")
    }

    private fun setupButtonListeners() {
        nextButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                currentQuestionIndex++
                displayQuestion()
            }
        })

        Log.d(TAG, "Button listeners setup")
    }

    private fun setupOptionButtonListeners() {
        option1Button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                checkAnswer(1)
            }
        })

        option2Button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                checkAnswer(2)
            }
        })

        option3Button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                checkAnswer(3)
            }
        })

        option4Button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                checkAnswer(4)
            }
        })
    }

    private val isNetworkAvailable: Boolean
        get() = true // Simplified for now

    private fun loadQuestionsFromFile(topic: String?, level: String?) {
        if (topic == null || level == null) {
            Log.e(TAG, "Topic or level is null - Topic: " + topic + ", Level: " + level)
            Toast.makeText(this, "Error: Tema o nivel no especificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        allQuestions = ArrayList<ImageQuestion?>()
        currentQuestions = ArrayList<ImageQuestion>()

        try {
            val assetManager = getAssets()
            val fileName = "image_questions_" + level.lowercase(Locale.getDefault()) + ".txt"

            Log.d(TAG, "Loading questions from file: " + fileName)

            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            var line: String?
            var lineNumber = 0

            while ((reader.readLine().also { line = it }) != null) {
                lineNumber++
                line = line!!.trim { it <= ' ' }

                if (line.isEmpty() || line.startsWith("#")) {
                    continue
                }

                val parts =
                    line.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (parts.size >= 9) {
                    val questionText: String? = parts[0]
                    val correctAnswer: String? = parts[1]
                    val options = arrayOf<String?>(parts[2], parts[3], parts[4], parts[5])
                    val questionTopic = parts[6]
                    val questionLevel = parts[7]
                    val imageResource: String? = parts[8]

                    if (questionTopic == topic && questionLevel == level) {
                        val question = ImageQuestion(
                            questionText,
                            correctAnswer,
                            options,
                            questionTopic,
                            questionLevel,
                            imageResource
                        )
                        allQuestions!!.add(question)
                        Log.d(TAG, "Added question: " + questionText + " for topic: " + topic)
                    }
                }
            }

            reader.close()
            inputStream.close()

            Log.d(TAG, "Loaded " + allQuestions!!.size + " questions for topic: " + topic)

            if (allQuestions!!.isEmpty()) {
                Log.w(TAG, "No questions found for topic: " + topic + " and level: " + level)
                Toast.makeText(
                    this,
                    "No se encontraron preguntas para este tema.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }

            Collections.shuffle(allQuestions)
            currentQuestions =
                ArrayList(allQuestions!!.subList(0, min(10, allQuestions!!.size)).filterNotNull())

            displayQuestion()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading questions from file", e)
            Toast.makeText(this, "Error cargando las preguntas.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex >= currentQuestions!!.size) {
            showQuizResults()
            return
        }

        val question = currentQuestions!!.get(currentQuestionIndex)

        // Update question number
        questionNumberTextView!!.setText((currentQuestionIndex + 1).toString() + "/" + currentQuestions!!.size)

        // Update score
        scoreTextView!!.setText("Score: " + score)

        // Hide the word initially
        hiddenWordTextView!!.setText("?????")

        // ============================================================
        // NUEVO: Actualizar el componente de audio con la nueva pregunta
        // ============================================================
        if (reusableAudioCard != null) {
            try {
                reusableAudioCard!!.resetForNewQuestion()
                reusableAudioCard!!.setText(question.correctAnswer)
                // Asegurarse de que esté en modo TTS (inglés)
                reusableAudioCard!!.setEnglishMode()
                Log.d(TAG, "Audio card actualizado con texto TTS: " + question.correctAnswer)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing reusable audio card text: " + e.message)
            }
        }

        // Load images for options
        loadOptionImages(question)

        // Reset button states
        resetButtonStates()

        // Deshabilitar botones hasta que se reproduzca el audio
        disableOptionButtons()

        // Hide next button initially
        nextButton!!.setVisibility(View.GONE)

        Log.d(
            TAG,
            "Displaying question " + (currentQuestionIndex + 1) + ": " + question.question
        )
    }

    private fun disableOptionButtons() {
        option1Button!!.setEnabled(false)
        option2Button!!.setEnabled(false)
        option3Button!!.setEnabled(false)
        option4Button!!.setEnabled(false)
    }

    private fun loadOptionImages(question: ImageQuestion) {
        try {
            val availableImages =
                this.availableImagesFromAssets

            val optionImages: MutableList<String?> = ArrayList<String?>()
            optionImages.add(question.imageResourceName)

            val remainingImages: MutableList<String?> = ArrayList<String?>()
            for (img in availableImages) {
                if (img != question.imageResourceName &&
                    !recentlyUsedImages.contains(img)
                ) {
                    remainingImages.add(img)
                }
            }

            if (remainingImages.size < 3) {
                remainingImages.clear()
                for (img in availableImages) {
                    if (img != question.imageResourceName) {
                        remainingImages.add(img)
                    }
                }
            }

            Collections.shuffle(remainingImages)
            for (i in 0..<min(3, remainingImages.size)) {
                optionImages.add(remainingImages.get(i))
            }

            Collections.shuffle(optionImages)

            setImageButton(option1Button!!, optionImages.get(0)!!)
            setImageButton(option2Button!!, optionImages.get(1)!!)
            setImageButton(option3Button!!, optionImages.get(2)!!)
            setImageButton(option4Button!!, optionImages.get(3)!!)

            question.correctImageResource = question.imageResourceName

            updateRecentlyUsedImages(optionImages)

            Log.d(TAG, "Loaded option images: " + optionImages.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading option images", e)
        }
    }

    private fun updateRecentlyUsedImages(usedImages: MutableList<String?>) {
        for (img in usedImages) {
            if (!recentlyUsedImages.contains(img)) {
                recentlyUsedImages.add(0, img)
            }
        }

        while (recentlyUsedImages.size > MAX_RECENT_IMAGES) {
            recentlyUsedImages.removeAt(recentlyUsedImages.size - 1)
        }
    }

    private val availableImagesFromAssets: MutableList<String>
        get() {
            val availableImages: MutableList<String> =
                ArrayList<String>()
            try {
                val assetManager = getAssets()
                val files = assetManager.list("")

                if (files != null) {
                    for (file in files) {
                        if (file.lowercase(Locale.getDefault()).endsWith(".svg")) {
                            availableImages.add(file)
                        }
                    }
                }

                val drawableImages =
                    arrayOf<String?>("ic_cat")

                for (img in drawableImages) {
                    availableImages.add(img!!)
                }

                Log.d(
                    TAG,
                    "Found " + availableImages.size + " available images"
                )
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error reading assets directory",
                    e
                )
                val fallbackImages =
                    arrayOf<String?>("ic_cat")
                for (img in fallbackImages) {
                    availableImages.add(img!!)
                }
            }

            return availableImages
        }

    private fun setImageButton(button: ImageButton, imageResourceName: String) {
        try {
            if (imageResourceName.lowercase(Locale.getDefault()).endsWith(".svg")) {
                if (setSVGImageButtonFromAssets(button, imageResourceName)) {
                    return
                }
            }

            if (setRegularImageButton(button, imageResourceName)) {
                return
            }

            button.setImageResource(R.drawable.ic_quiz)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting image: " + imageResourceName, e)
            button.setImageResource(R.drawable.ic_quiz)
        }
    }

    private fun setSVGImageButtonFromAssets(button: ImageButton, svgFileName: String): Boolean {
        try {
            val assetManager = getAssets()
            val inputStream = assetManager.open(svgFileName)

            val svg = SVG.getFromInputStream(inputStream)
            val drawable = PictureDrawable(svg.renderToPicture())
            button.setImageDrawable(drawable)
            button.setTag(svgFileName)

            inputStream.close()
            Log.d(TAG, "Set SVG from assets: " + svgFileName)
            return true
        } catch (e: IOException) {
            Log.w(TAG, "SVG not found in assets: " + svgFileName)
            return false
        } catch (e: SVGParseException) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e)
            return false
        }
    }

    private fun setRegularImageButton(button: ImageButton, imageResourceName: String?): Boolean {
        try {
            val resourceId = getResources().getIdentifier(
                imageResourceName, "drawable", getPackageName()
            )

            if (resourceId != 0) {
                button.setImageResource(resourceId)
                button.setTag(imageResourceName)
                Log.d(TAG, "Set regular image: " + imageResourceName)
                return true
            } else {
                Log.w(TAG, "Image resource not found: " + imageResourceName)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting regular image: " + imageResourceName, e)
            return false
        }
    }

    private fun resetButtonStates() {
        option1Button!!.setEnabled(true)
        option2Button!!.setEnabled(true)
        option3Button!!.setEnabled(true)
        option4Button!!.setEnabled(true)

        option1Button!!.setBackgroundTintList(getColorStateList(R.color.white))
        option2Button!!.setBackgroundTintList(getColorStateList(R.color.white))
        option3Button!!.setBackgroundTintList(getColorStateList(R.color.white))
        option4Button!!.setBackgroundTintList(getColorStateList(R.color.white))

        if (birdImageView != null) {
            birdImageView!!.setImageResource(R.drawable.crab_test)
        }
    }

    private fun checkAnswer(selectedOption: Int) {
        if (currentQuestionIndex >= currentQuestions!!.size) {
            return
        }

        val question = currentQuestions!!.get(currentQuestionIndex)
        var selectedImageResource = ""

        when (selectedOption) {
            1 -> selectedImageResource = option1Button!!.getTag() as String
            2 -> selectedImageResource = option2Button!!.getTag() as String
            3 -> selectedImageResource = option3Button!!.getTag() as String
            4 -> selectedImageResource = option4Button!!.getTag() as String
        }

        val isCorrect = selectedImageResource == question.correctImageResource

        Log.d(
            TAG,
            "Answer check - Selected: '" + selectedImageResource + "', Correct: '" + question.correctImageResource + "', Result: " + isCorrect
        )

        saveIndividualAnswer(question, selectedImageResource, isCorrect)

        if (isCorrect) {
            score++
            scoreTextView!!.setText("Score: " + score)
            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_ok)
            }
            playCorrectSound()
        } else {
            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_bad)
            }
            playIncorrectSound()
        }

        hiddenWordTextView!!.setText(question.correctAnswer)

        highlightButtons(selectedOption, isCorrect, question.correctImageResource)

        option1Button!!.setEnabled(false)
        option2Button!!.setEnabled(false)
        option3Button!!.setEnabled(false)
        option4Button!!.setEnabled(false)

        nextButton!!.setVisibility(View.VISIBLE)

        val feedback =
            if (isCorrect) "¡Correcto!" else "Incorrecto. La respuesta correcta es: " + question.correctAnswer
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
    }

    private fun saveIndividualAnswer(
        question: ImageQuestion,
        selectedAnswer: String?,
        isCorrect: Boolean
    ) {
        try {
            dbHelper!!.saveQuizResult(
                userId,
                question.question,
                question.correctAnswer,
                selectedAnswer,
                isCorrect,
                "Image Identification Audio",
                selectedTopic,
                selectedLevel,
                sessionTimestamp
            )
            Log.d(TAG, "Individual answer saved to database")

            if (!isOfflineMode && mAuth!!.getCurrentUser() != null) {
                val questionId = currentQuestionIndex.toString()
                val answerData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                answerData.put("question", question.question)
                answerData.put("correctAnswer", question.correctAnswer)
                answerData.put("selectedAnswer", selectedAnswer)
                answerData.put("isCorrect", isCorrect)
                answerData.put("timestamp", sessionTimestamp)
                answerData.put("quizType", "Image Identification Audio")
                answerData.put("topic", selectedTopic)
                answerData.put("level", selectedLevel)

                mDatabase!!.child("users").child(mAuth!!.getCurrentUser()!!.getUid())
                    .child("quiz_results")
                    .child(questionId)
                    .setValue(answerData)
                Log.d(TAG, "Individual answer saved to Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving individual answer: " + e.message)
        }
    }

    private fun highlightButtons(
        selectedOption: Int,
        isCorrect: Boolean,
        correctImageResource: String?
    ) {
        resetButtonStates()

        var correctButton: ImageButton? = null
        if (option1Button!!.getTag() == correctImageResource) correctButton = option1Button
        else if (option2Button!!.getTag() == correctImageResource) correctButton = option2Button
        else if (option3Button!!.getTag() == correctImageResource) correctButton = option3Button
        else if (option4Button!!.getTag() == correctImageResource) correctButton = option4Button

        if (correctButton != null) {
            correctButton.setBackgroundTintList(getColorStateList(R.color.verdeSena))
        }

        val selectedButton = getButtonByIndex(selectedOption)
        if (selectedButton != null && !isCorrect) {
            selectedButton.setBackgroundTintList(getColorStateList(R.color.error_color))
        }
    }

    private fun getButtonByIndex(index: Int): ImageButton? {
        when (index) {
            1 -> return option1Button
            2 -> return option2Button
            3 -> return option3Button
            4 -> return option4Button
            else -> return null
        }
    }

    private fun showQuizResults() {
        val percentage = score.toDouble() / currentQuestions!!.size * 100
        val finalScore = Math.round(percentage).toInt()

        saveQuizResults(percentage)

        if (percentage >= 70) {
            markTopicAsPassed(selectedTopic!!)
            // Sumar puntos de estrella y mostrar modal de estrella (igual que en otras actividades)
            StarProgressHelper.addSessionPoints(this, 10)
            Handler().postDelayed(Runnable {
                try {
                    StarEarnedDialog.show(this@ImageIdentificationAudioActivity)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.message)
                }
            }, 200)
        }

        // Mostrar resultados en diálogo estilo dialog_quiz_result
        val builder = AlertDialog.Builder(this)
        val dialogView =
            getLayoutInflater().inflate(com.example.speak.R.layout.dialog_quiz_result, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()
        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val birdImageViewDialog =
            dialogView.findViewById<ImageView>(com.example.speak.R.id.birdImageView)
        val messageTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.messageTextView)
        val counterTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.counterTextView)
        val scoreTextView = dialogView.findViewById<TextView>(com.example.speak.R.id.scoreTextView)
        val btnContinue = dialogView.findViewById<Button>(com.example.speak.R.id.btnContinue)
        val btnReintentar = dialogView.findViewById<TextView>(com.example.speak.R.id.btnReintentar)
        val btnViewDetails =
            dialogView.findViewById<LinearLayout>(com.example.speak.R.id.btnViewDetails)

        // Imagen y mensaje según puntaje
        if (finalScore >= 90) {
            messageTextView.setText("Excellent your English is getting better!")
            birdImageViewDialog.setImageResource(com.example.speak.R.drawable.crab_ok)
        } else if (finalScore >= 70) {
            messageTextView.setText("Good, but you can do it better!")
            birdImageViewDialog.setImageResource(com.example.speak.R.drawable.crab_test)
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!")
            birdImageViewDialog.setImageResource(com.example.speak.R.drawable.crab_test)
        } else {
            messageTextView.setText("You should practice more!")
            birdImageViewDialog.setImageResource(com.example.speak.R.drawable.crab_bad)
        }

        counterTextView.setText(score.toString() + "/" + currentQuestions!!.size)
        scoreTextView.setText("Score: " + finalScore + "%")

        // Continuar: usar progresión de Reading para la variante Audio
        btnContinue.setOnClickListener(View.OnClickListener { v: View? ->
            val nextReadingTopic =
                ProgressionHelper.getNextImageIdentificationTopicBySource(selectedTopic, "READING")
            if (nextReadingTopic != null) {
                val nextActivityClass = ProgressionHelper.getReadingActivityClass(nextReadingTopic)
                val next = Intent(this, nextActivityClass)
                next.putExtra("TOPIC", nextReadingTopic)
                next.putExtra("LEVEL", selectedLevel)
                startActivity(next)
                finish()
            } else {
                val back = Intent(this, MenuReadingActivity::class.java)
                back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(back)
                finish()
            }
        })

        // Reintentar: reiniciar esta actividad
        btnReintentar.setText("Try again")
        btnReintentar.setOnClickListener(View.OnClickListener { v: View? ->
            val retry = Intent(this, ImageIdentificationAudioActivity::class.java)
            retry.putExtra("TOPIC", selectedTopic)
            retry.putExtra("LEVEL", selectedLevel)
            startActivity(retry)
            finish()
        })

        // Ver resumen (tabla): abrir QuizHistoryActivity filtrado por esta sesión
        btnViewDetails.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this, QuizHistoryActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_QUESTIONS", currentQuestions!!.size)
            intent.putExtra("QUIZ_TYPE", "Image Identification Audio")
            intent.putExtra("TOPIC", selectedTopic)
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true)
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp)
            startActivity(intent)
            finish()
        })
    }

    private fun saveQuizResults(percentage: Double) {
        try {
            dbHelper!!.saveQuizResult(
                userId,
                "Image Identification Audio Quiz",
                "Score: " + score + "/" + currentQuestions!!.size,
                "Percentage: " + String.format("%.1f%%", percentage),
                percentage >= 70,
                "Image Identification Audio",
                selectedTopic,
                selectedLevel,
                sessionTimestamp
            )
            Log.d(TAG, "Quiz result saved to local database")

            if (!isOfflineMode && mAuth!!.getCurrentUser() != null) {
                val quizData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                quizData.put("userId", userId)
                quizData.put("topic", selectedTopic)
                quizData.put("level", selectedLevel)
                quizData.put("score", score)
                quizData.put("totalQuestions", currentQuestions!!.size)
                quizData.put("percentage", percentage)
                quizData.put("timestamp", sessionTimestamp)
                quizData.put("quizType", "Image Identification Audio")
                quizData.put("offlineMode", isOfflineMode)

                mDatabase!!.child("quiz_results").push().setValue(quizData)
                    .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                        Log.d(
                            TAG,
                            "Quiz result saved to Firebase"
                        )
                    })
                    .addOnFailureListener(OnFailureListener { e: Exception? ->
                        Log.e(
                            TAG,
                            "Failed to save quiz result to Firebase",
                            e
                        )
                    })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving quiz results", e)
        }
    }

    private fun markTopicAsPassed(topic: String) {
        try {
            dbHelper!!.markTopicAsPassed(userId, topic, selectedLevel)
            Log.d(TAG, "Topic marked as passed in database: " + topic)

            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val editor = prefs.edit()
            val key = "PASSED_" + topic.replace(" ", "_")
            editor.putBoolean(key, true)
            editor.apply()

            Log.d(TAG, "Topic marked as passed in SharedPreferences: " + topic)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking topic as passed", e)
        }
    }

    override fun onDestroy() {
        // ============================================================
        // NUEVO: Detener monitor y limpiar componente de audio
        // ============================================================
        if (reusableMonitorHandler != null && reusableMonitorRunnable != null) {
            reusableMonitorHandler.removeCallbacks(reusableMonitorRunnable!!)
        }

        try {
            if (reusableAudioCard != null) {
                reusableAudioCard!!.cleanup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up reusableAudioCard: " + e.message)
        }

        if (correctSoundPlayer != null) {
            correctSoundPlayer!!.release()
            correctSoundPlayer = null
        }
        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer!!.release()
            incorrectSoundPlayer = null
        }

        super.onDestroy()
    }

    private fun initializeSoundPlayers() {
        try {
            correctSoundPlayer = MediaPlayer.create(
                this, getResources().getIdentifier(
                    "mario_bros_vida", "raw", getPackageName()
                )
            )

            incorrectSoundPlayer = MediaPlayer.create(
                this, getResources().getIdentifier(
                    "pacman_dies", "raw", getPackageName()
                )
            )

            Log.d(TAG, "Sonidos de feedback inicializados correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando sonidos de feedback: " + e.message)
        }
    }

    private fun playCorrectSound() {
        try {
            if (correctSoundPlayer != null) {
                if (correctSoundPlayer!!.isPlaying()) {
                    correctSoundPlayer!!.stop()
                    correctSoundPlayer!!.prepare()
                }
                correctSoundPlayer!!.start()
                Log.d(TAG, "Reproduciendo sonido de respuesta correcta")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo sonido correcto: " + e.message)
        }
    }

    private fun playIncorrectSound() {
        try {
            if (incorrectSoundPlayer != null) {
                if (incorrectSoundPlayer!!.isPlaying()) {
                    incorrectSoundPlayer!!.stop()
                    incorrectSoundPlayer!!.prepare()
                }
                incorrectSoundPlayer!!.start()
                Log.d(TAG, "Reproduciendo sonido de respuesta incorrecta")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo sonido incorrecto: " + e.message)
        }
    }

    private fun returnToMenu() {
        val intent = Intent(this, MenuReadingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "ImageIdentificationAudioActivity"

        private const val MAX_RECENT_IMAGES = 8
    }
}