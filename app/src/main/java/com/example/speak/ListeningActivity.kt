package com.example.speak

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.components.ModalAlertComponent
import com.example.speak.components.ModalAlertComponent.ModalType
import com.example.speak.components.ModalAlertComponent.OnModalActionListener
import com.example.speak.components.ReusableAudioPlayerCard
import com.example.speak.database.DatabaseHelper
import com.example.speak.helpers.HelpModalHelper
import com.example.speak.helpers.StarEarnedDialog
import com.example.speak.helpers.StarProgressHelper
import com.example.speak.helpers.WildcardHelper
import com.example.speak.helpers.WildcardHelper.WildcardCallbacks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import kotlin.math.min

class ListeningActivity : AppCompatActivity() {
    private var questionTextView: TextView? = null
    private var levelTextView: TextView? = null
    private var topicTextView: TextView? = null
    private var questionNumberTextView: TextView? = null
    private var optionsRadioGroup: LinearLayout? = null

    private var submitButton: Button? = null
    private var nextButton: Button? = null

    // Reproductor de audio
    private var reusableAudioCard: ReusableAudioPlayerCard? = null
    private var lastReusablePlaying = false
    private val reusableMonitorHandler: Handler? = Handler(Looper.getMainLooper())
    private var reusableMonitorRunnable: Runnable? = null

    private var selectionInstructionText: TextView? = null

    // Botones de opciones
    private var option1Button: Button? = null
    private var option2Button: Button? = null
    private var option3Button: Button? = null
    private var option4Button: Button? = null

    private var birdImageView: ImageView? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTextToSpeechReady = false

    // MediaPlayer para sonidos de feedback
    private var correctSoundPlayer: MediaPlayer? = null
    private var incorrectSoundPlayer: MediaPlayer? = null

    private var allQuestions: MutableList<ListeningQuestion>? = null
    private var currentQuestions: MutableList<ListeningQuestion>? = null

    private var currentQuestionIndex = 0
    private val currentIndex = 0
    private var selectedTopic: String? = null
    private var selectedLevel: String? = null

    private var score = 0
    private var evaluatedQuestionsCount = 0

    private val currentSpeed = 0.1f
    private val currentPitch = 1.0f

    // Control de reproducción
    private var isPlaying = false
    private var isPaused = false
    private var startTime: Long = 0
    private val pauseTime: Long = 0
    private val totalDuration: Long = 10000
    private var currentPosition: Long = 0
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable: Runnable? = null

    private var dbHelper: DatabaseHelper? = null
    private var mAuth: FirebaseAuth? = null
    private var mDatabase: DatabaseReference? = null
    private var userId: Long = 0
    private var isOfflineMode = false
    private var sessionTimestamp: Long = 0

    // Sistema de comodines
    private var wildcardHelper: WildcardHelper? = null
    private var wildcardButton: ImageView? = null

    // ============================================================
    // CAMBIO: Variable del componente reutilizable de modales
    // ============================================================
    private var modalAlertComponent: ModalAlertComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listening)

        // Initialize views
        questionTextView = findViewById<TextView>(R.id.questionTextView)
        levelTextView = findViewById<TextView>(R.id.levelTextView)
        topicTextView = findViewById<TextView>(R.id.topicTextView)

        // Recibir los parámetros enviados desde el menú
        val intent = getIntent()
        selectedTopic = intent.getStringExtra("TOPIC")
        selectedLevel = intent.getStringExtra("LEVEL")

        // Log para verificar parámetros recibidos
        Log.d(TAG, "Topic recibido: '" + selectedTopic + "'")
        Log.d(TAG, "Level recibido: '" + selectedLevel + "'")

        // Mostrar topic y level en la interfaz
        if (selectedLevel != null) {
            levelTextView!!.setText("Level: " + selectedLevel)
        }
        if (selectedTopic != null) {
            topicTextView!!.setText("THE " + selectedTopic)
        }

        questionNumberTextView = findViewById<TextView>(R.id.questionNumberTextView)
        optionsRadioGroup = findViewById<LinearLayout?>(R.id.optionsRadioGroup)

        submitButton = findViewById<Button>(R.id.submitButton)
        nextButton = findViewById<Button>(R.id.nextButton)

        // Inicializar botones de opciones
        option1Button = findViewById<Button>(R.id.option1RadioButton)
        option2Button = findViewById<Button>(R.id.option2RadioButton)
        option3Button = findViewById<Button>(R.id.option3RadioButton)
        option4Button = findViewById<Button>(R.id.option4RadioButton)

        // Inicializar bird image
        birdImageView = findViewById<ImageView?>(R.id.birdImageView)

        // Inicializar botón de comodines
        wildcardButton = findViewById<ImageView?>(R.id.wildcardButton)
        if (wildcardButton != null) {
            wildcardButton!!.setOnClickListener(View.OnClickListener { v: View? -> showWildcardMenu() })
        }

        // Inicializar botón de ayuda
        val helpButton = findViewById<ImageView?>(R.id.helpButton)
        if (helpButton != null) {
            helpButton.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    HelpModalHelper.show(this@ListeningActivity, selectedTopic, selectedLevel)
                } catch (e: Exception) {
                    Log.e(TAG, "Error abriendo modal de ayuda: " + e.message)
                    Toast.makeText(
                        this@ListeningActivity,
                        "No se pudo abrir la ayuda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Inicializar reproductor de audio
        reusableAudioCard = findViewById<ReusableAudioPlayerCard?>(R.id.reusableAudioCard)
        selectionInstructionText = findViewById<TextView?>(R.id.selectionInstructionText)

        // Inicializar sonidos de feedback
        initializeSoundPlayers()

        // ============================================================
        // CAMBIO: Inicializar componente de modal ANTES de Firebase
        // ============================================================
        modalAlertComponent = findViewById<ModalAlertComponent?>(R.id.modalAlertComponent)
        if (modalAlertComponent != null) {
            modalAlertComponent!!.setOnModalActionListener(object : OnModalActionListener {
                override fun onContinuePressed(type: ModalType?) {
                    advanceToNextQuestion()
                }

                override fun onModalHidden(type: ModalType?) {
                    Log.d(TAG, "Modal hidden: " + type)
                }
            })
            Log.d(TAG, "ModalAlertComponent initialized successfully")
        } else {
            Log.e(TAG, "ModalAlertComponent is NULL - check XML layout")
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().getReference()

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Check network status first
        isOfflineMode = !this.isNetworkAvailable
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode)

        // Get user ID from SharedPreferences
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
        val email = prefs.getString("user_email", null)

        Log.d(TAG, "Initial user ID from prefs: " + userId)
        Log.d(TAG, "User email from prefs: " + email)

        if (userId == -1L) {
            if (email != null) {
                userId = dbHelper!!.getUserId(email)
                Log.d(TAG, "User ID from database: " + userId)

                if (userId != -1L) {
                    val editor = prefs.edit()
                    editor.putLong("user_id", userId)
                    editor.apply()
                }
            }
        }

        if (userId == -1L && isOfflineMode) {
            Log.d(TAG, "No user ID found in offline mode, creating guest user")

            var deviceId = prefs.getString("device_id", null)
            if (deviceId == null) {
                deviceId =
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
                if (deviceId != null) {
                    val editor = prefs.edit()
                    editor.putString("device_id", deviceId)
                    editor.apply()
                    Log.d(TAG, "Saved new device ID: " + deviceId)
                }
            }

            if (deviceId != null) {
                if (dbHelper!!.isGuestUserExists(deviceId)) {
                    Log.d(TAG, "Guest user exists for device: " + deviceId)
                    val guestCursor = dbHelper!!.getGuestUser(deviceId)
                    if (guestCursor != null && guestCursor.moveToFirst()) {
                        //userId = guestCursor.getLong(guestCursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                        guestCursor.close()
                        Log.d(TAG, "Retrieved guest user ID: " + userId)
                    }
                } else {
                    Log.d(TAG, "Creating new guest user for device: " + deviceId)
                    userId = dbHelper!!.createGuestUser(deviceId)
                    Log.d(TAG, "Created new guest user with ID: " + userId)
                }

                if (userId != -1L) {
                    val editor = prefs.edit()
                    editor.putLong("user_id", userId)
                    editor.apply()
                }
            }
        }

        if (userId == -1L) {
            Log.e(TAG, "Failed to initialize user session")
            finish()
            return
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, OnInitListener { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech!!.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTextToSpeechReady = false
                } else {
                    textToSpeech!!.setSpeechRate(currentSpeed)
                    textToSpeech!!.setPitch(currentPitch)
                    isTextToSpeechReady = true
                    Log.d(TAG, "TextToSpeech initialized successfully")

                    // Pasar el TextToSpeech al componente de audio
                    if (reusableAudioCard != null) {
                        reusableAudioCard!!.setTextToSpeech(textToSpeech)
                        Log.d(TAG, "TextToSpeech passed to ReusableAudioPlayerCard")
                    }

                    textToSpeech!!.setOnUtteranceProgressListener(object :
                        UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS started: " + utteranceId)
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS completed: " + utteranceId)
                            runOnUiThread(Runnable {
                                if (!isPaused) {
                                    isPlaying = false
                                    isPaused = false
                                    currentPosition = 0
                                    stopProgressUpdate()

                                    enableVisibleButtons()
                                    Log.d(
                                        TAG,
                                        "Botones habilitados después de completar la reproducción del audio"
                                    )
                                }
                            })
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error: " + utteranceId)
                        }
                    })
                }
            } else {
                isTextToSpeechReady = false
            }
        })

        // Configurar componente reutilizable SOLO TTS
        setupReusableAudioCardForTTS()

        // Iniciar monitor para habilitar respuestas al comenzar la reproducción
        startReusablePlaybackMonitor()

        submitButton!!.setOnClickListener(View.OnClickListener { v: View? -> })

        nextButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            currentQuestionIndex++
            displayQuestion()
        })

        // Configurar botón de retorno
        val returnContainer = findViewById<LinearLayout?>(R.id.returnContainer)
        if (returnContainer != null) {
            returnContainer.setOnClickListener(View.OnClickListener { v: View? ->
                finish()
            })
        }

        // Validación previa para bloquear acceso si el tema anterior no ha sido aprobado
        if (!isPreviousTopicPassed(selectedTopic)) {
            Toast.makeText(
                this,
                "Debes completar el tema anterior antes de continuar.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Inicializar sistema de comodines
        wildcardHelper = WildcardHelper(this, "LISTENING", selectedTopic)
        wildcardHelper!!.setCallbacks(object : WildcardCallbacks {
            override fun onChangeQuestion() {
                changeCurrentQuestion()
            }

            override fun onShowContentImage() {
                showContentImage()
            }

            override fun onShowInstructorVideo() {
                showInstructorVideo()
            }

            override fun onShowFiftyFifty() {
                applyFiftyFifty()
            }

            override fun onShowCreativeHelp() {
                showCreativeHelp()
            }

            override fun onShowWildcardInfo() {
                showWildcardUsageInfo()
            }
        })

        // Load questions
        loadQuestionsFromFile(selectedTopic, selectedLevel)

        // Inicializar contadores para la nueva sesión
        score = 0
        evaluatedQuestionsCount = 0
        currentQuestionIndex = 0

        // Inicializar el timestamp de sesión
        sessionTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp)
        Log.d(
            TAG,
            "Contadores reiniciados - Score: " + score + ", Evaluadas: " + evaluatedQuestionsCount
        )
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager =
                getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
            return activeNetworkInfo != null && activeNetworkInfo.isConnected()
        }

    private fun setupReusableAudioCardForTTS() {
        try {
            if (reusableAudioCard != null) {
                var ttsText = ""
                if (currentQuestions != null && !currentQuestions!!.isEmpty() && currentQuestionIndex < currentQuestions!!.size) {
                    val q = currentQuestions!!.get(currentQuestionIndex)
                    ttsText = q.question ?: ""
                }

                reusableAudioCard!!.configure("", ttsText)

                val englishButton =
                    reusableAudioCard!!.findViewById<View?>(R.id.languageEnglishButton)
                if (englishButton != null) {
                    englishButton.performClick()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up reusable audio card for TTS: " + e.message, e)
        }
    }

    private fun startReusablePlaybackMonitor() {
        if (reusableAudioCard == null) return
        reusableMonitorRunnable = object : Runnable {
            override fun run() {
                try {
                    val isPlayingReusable = reusableAudioCard!!.isPlaying
                    if (isPlayingReusable != lastReusablePlaying) {
                        lastReusablePlaying = isPlayingReusable
                        if (isPlayingReusable) {
                            enableVisibleButtons()
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

    private fun isPreviousTopicPassed(currentTopic: String?): Boolean {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        Log.d(TAG, "=== VERIFICACIÓN DE PROGRESO PREVIO ===")
        Log.d(TAG, "Tema actual a verificar: " + currentTopic)

        if ("NUMBERS".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_ALPHABET", false)
            val score = prefs.getInt("SCORE_ALPHABET", 0)

            Log.d(TAG, "Verificando progreso de ALPHABET:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ALPHABET")
            } else {
                Log.d(TAG, "✅ Tema ALPHABET completado correctamente")
            }

            return passed
        }

        if ("COLORS".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_NUMBERS", false)
            val score = prefs.getInt("SCORE_NUMBERS", 0)

            Log.d(TAG, "Verificando progreso de NUMBERS:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema NUMBERS")
            } else {
                Log.d(TAG, "✅ Tema NUMBERS completado correctamente")
            }

            return passed
        }

        if ("PERSONAL_PRONOUNS".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_COLORS", false)
            val score = prefs.getInt("SCORE_COLORS", 0)

            Log.d(TAG, "Verificando progreso de COLORS:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema COLORS")
            } else {
                Log.d(TAG, "✅ Tema COLORS completado correctamente")
            }

            return passed
        }

        if ("POSSESSIVE ADJECTIVES".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)
            val score = prefs.getInt("SCORE_PERSONAL_PRONOUNS", 0)

            Log.d(TAG, "Verificando progreso de PERSONAL PRONOUNS:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema PERSONAL PRONOUNS")
            } else {
                Log.d(TAG, "✅ Tema PERSONAL PRONOUNS completado correctamente")
            }

            return passed
        }

        if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equals(
                currentTopic,
                ignoreCase = true
            )
        ) {
            val passed = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false)
            val score = prefs.getInt("SCORE_POSSESSIVE_ADJECTIVES", 0)

            Log.d(TAG, "Verificando progreso de POSSESSIVE ADJECTIVES:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema POSSESSIVE ADJECTIVES")
            } else {
                Log.d(TAG, "✅ Tema POSSESSIVE ADJECTIVES completado correctamente")
            }

            return passed
        }

        if ("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(
                currentTopic,
                ignoreCase = true
            )
        ) {
            val passed = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false)
            val score = prefs.getInt("SCORE_PREPOSITIONS_OF_PLACE", 0)

            Log.d(TAG, "Verificando progreso de PREPOSITIONS OF PLACE:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema PREPOSITIONS OF PLACE")
            } else {
                Log.d(TAG, "✅ Tema PREPOSITIONS OF PLACE completado correctamente")
            }

            return passed
        }

        if ("ORDINAL AND CARDINAL NUMBERS".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_ADJECTIVES", false)
            val score = prefs.getInt("SCORE_ADJECTIVES", 0)

            Log.d(TAG, "Verificando progreso de ADJECTIVES:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ADJECTIVES")
            } else {
                Log.d(TAG, "✅ Tema ADJECTIVES completado correctamente")
            }

            return passed
        }

        if ("VERB TO BE".equals(currentTopic, ignoreCase = true)) {
            val passed = prefs.getBoolean("PASSED_ORDINAL", false)
            val score = prefs.getInt("SCORE_ORDINAL", 0)

            Log.d(TAG, "Verificando progreso de ORDINAL:")
            Log.d(TAG, "Progreso guardado: " + passed)
            Log.d(TAG, "Puntuación guardada: " + score + "%")

            if (!passed) {
                Log.d(TAG, "❌ No se ha completado el tema ORDINAL")
            } else {
                Log.d(TAG, "✅ Tema ORDINAL completado correctamente")
            }

            return passed
        }

        Log.d(TAG, "No se requiere verificación de progreso previo para este tema")
        Log.d(TAG, "=== FIN DE VERIFICACIÓN DE PROGRESO ===")
        return true
    }

    private fun loadQuestionsFromFile(topic: String?, level: String?) {
        allQuestions = ArrayList<ListeningQuestion>()
        try {
            val assetManager = getAssets()
            val `is` = getAssets().open("SENA_Level_1_A1.1.txt")
            val reader = BufferedReader(InputStreamReader(`is`))

            var line: String?
            var currentQuestion = ""
            var currentAnswer = ""
            val currentOptions: MutableList<String?> = ArrayList<String?>()
            var currentTopic = ""
            var currentLevel = ""
            var isMatchingTopic = false

            while ((reader.readLine().also { line = it }) != null) {
                line = line!!.trim { it <= ' ' }
                if (line.startsWith("Topic:")) {
                    currentTopic = line.substring(6).trim { it <= ' ' }
                    isMatchingTopic = currentTopic.equals(topic, ignoreCase = true)
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim { it <= ' ' }
                    isMatchingTopic =
                        isMatchingTopic && currentLevel.equals(level, ignoreCase = true)
                } else if (line.startsWith("Q:")) {
                    currentQuestion = line.substring(2).trim { it <= ' ' }
                } else if (line.startsWith("A:")) {
                    currentAnswer = line.substring(2).trim { it <= ' ' }
                } else if (line.startsWith("O:")) {
                    val options = line.substring(2).trim { it <= ' ' }.split("\\|".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                    currentOptions.clear()
                    for (option in options) {
                        currentOptions.add(option.trim { it <= ' ' })
                    }
                }

                if (isMatchingTopic && !currentQuestion.isEmpty() && !currentAnswer.isEmpty() && !currentOptions.isEmpty()) {
                    allQuestions!!.add(
                        ListeningQuestion(
                            currentQuestion,
                            currentAnswer,
                            currentOptions.toTypedArray<String?>(),
                            currentTopic,
                            currentLevel,
                            true,
                            ""
                        )
                    )
                    currentQuestion = ""
                    currentAnswer = ""
                    currentOptions.clear()
                }
            }

            if (!currentQuestion.isEmpty() && !currentAnswer.isEmpty() && !currentOptions.isEmpty()) {
                if (currentTopic.equals(topic, ignoreCase = true) && currentLevel.equals(
                        level,
                        ignoreCase = true
                    )
                ) {
                    allQuestions!!.add(
                        ListeningQuestion(
                            currentQuestion,
                            currentAnswer,
                            currentOptions.toTypedArray<String?>(),
                            currentTopic,
                            currentLevel,
                            true,
                            ""
                        )
                    )
                }
            }

            reader.close()
            `is`.close()

            Log.d(TAG, "Total questions loaded for topic " + topic + ": " + allQuestions!!.size)
            for (i in 0..<min(3, allQuestions!!.size)) {
                val q = allQuestions!!.get(i)
                Log.d(
                    TAG,
                    "Question " + (i + 1) + ": " + q.question + " | Answer: " + q.correctAnswer
                )
            }

            Collections.shuffle(allQuestions)
            currentQuestions = ArrayList<ListeningQuestion>(
                allQuestions!!.subList(
                    0,
                    min(10, allQuestions!!.size)
                )
            )

            Log.d(TAG, "Selected " + currentQuestions!!.size + " questions for quiz")

            displayQuestion()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < currentQuestions!!.size) {
            val question = currentQuestions!!.get(currentQuestionIndex)
            questionNumberTextView!!.setText(
                String.format(
                    "%d/%d",
                    currentQuestionIndex + 1, currentQuestions!!.size
                )
            )

            if (reusableAudioCard != null) {
                try {
                    // Primero establecer el texto (actualiza duración en AudioPlayerView)
                    reusableAudioCard!!.setText(question.question)
                    // Asegurar que esté en modo inglés (TTS)
                    reusableAudioCard!!.setEnglishMode()
                    // Luego resetear el estado de reproducción (sin cambiar el texto)
                    reusableAudioCard!!.resetForNewQuestion()
                    Log.d(TAG, "Audio card configured for new question: " + question.question)
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing reusable audio card text: " + e.message)
                }
            }

            if (shouldShowSpokenText(question.topic ?: "")) {
                val htmlText = "Texto a pronunciar <strong>/ Text to be spoken:</strong>"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    questionTextView!!.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY))
                } else {
                    questionTextView!!.setText(Html.fromHtml(htmlText))
                }
            } else {
                val htmlText =
                    "Escuche el audio y seleccione la opción correcta <b>/ Listen to the audio and select the correct option</b>"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    questionTextView!!.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY))
                } else {
                    questionTextView!!.setText(Html.fromHtml(htmlText))
                }
            }

            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_test)
            }

            val options = question.options

            if (options?.size ?: 0 > 0) {
                option1Button!!.setVisibility(View.VISIBLE)
                option1Button!!.setText(options?.get(0) ?: "")
                option1Button!!.setEnabled(false)
            } else {
                option1Button!!.setVisibility(View.GONE)
            }

            if (options?.size ?: 0 > 1) {
                option2Button!!.setVisibility(View.VISIBLE)
                option2Button!!.setText(options?.get(1) ?: "")
                option2Button!!.setEnabled(false)
            } else {
                option2Button!!.setVisibility(View.GONE)
            }

            if (options?.size ?: 0 > 2) {
                option3Button!!.setVisibility(View.VISIBLE)
                option3Button!!.setText(options?.get(2) ?: "")
                option3Button!!.setEnabled(false)
            } else {
                option3Button!!.setVisibility(View.GONE)
            }

            if (options?.size ?: 0 > 3) {
                option4Button!!.setVisibility(View.VISIBLE)
                option4Button!!.setText(options?.get(3) ?: "")
                option4Button!!.setEnabled(false)
            } else {
                option4Button!!.setVisibility(View.GONE)
            }

            resetButtonStates()
            disableAllButtons()
            setupOptionButtonListeners()

            submitButton!!.setVisibility(View.GONE)
            nextButton!!.setVisibility(View.VISIBLE)
            nextButton!!.setEnabled(false)
        } else {
            showResults()
            val finalScore = ((score / currentQuestions!!.size.toDouble()) * 100).toInt()
            saveFinalScore(finalScore)
        }
    }

    private fun shouldShowSpokenText(topic: String): Boolean {
        if (topic == "ALPHABET" || topic == "NUMBERS" || topic == "COLORS") {
            return false
        }

        if (topic == "PERSONAL PRONOUNS" ||
            topic == "POSSESSIVE ADJECTIVES" ||
            topic == "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION" ||
            topic == "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)" ||
            topic == "ORDINAL AND CARDINAL NUMBERS" ||
            topic == "VERB TO BE" ||
            topic == "SIMPLE PRESENT" ||
            topic == "SIMPLE PRESENT THIRD PERSON" ||
            topic == "SIMPLE PAST" ||
            topic == "FREQUENCY ADVERBS" ||
            topic == "DAILY ROUTINES" ||
            topic == "COUNTABLE AND UNCOUNTABLE" ||
            topic == "QUANTIFIERS" ||
            topic == "PREPOSITIONS" ||
            topic == "USED TO"
        ) {
            return true
        }

        return false
    }

    private fun resetButtonStates() {
        if (option1Button!!.getVisibility() == View.VISIBLE) {
            option1Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        }
        if (option2Button!!.getVisibility() == View.VISIBLE) {
            option2Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        }
        if (option3Button!!.getVisibility() == View.VISIBLE) {
            option3Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        }
        if (option4Button!!.getVisibility() == View.VISIBLE) {
            option4Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        }
    }

    private fun setupOptionButtonListeners() {
        option1Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(1) })
        option2Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(2) })
        option3Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(3) })
        option4Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(4) })
    }

    private fun saveFinalScore(score: Int) {
        if (selectedTopic != null && selectedLevel != null) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()

            Log.d(TAG, "=== INICIO DE GUARDADO DE PROGRESO ===")
            Log.d(TAG, "Tema actual: " + selectedTopic)
            Log.d(TAG, "Nivel actual: " + selectedLevel)
            Log.d(TAG, "Puntuación recibida: " + score)

            if (score >= 70) {
                val eButtonStartTopics = arrayOf<String?>(
                    "ALPHABET",
                    "NUMBERS",
                    "COLORS",
                    "PERSONAL PRONOUNS",
                    "POSSESSIVE ADJECTIVES"
                )
                var isEButtonStartTopic = false

                for (topic in eButtonStartTopics) {
                    if (selectedTopic == topic) {
                        isEButtonStartTopic = true
                        break
                    }
                }

                if (isEButtonStartTopic) {
                    val progressKey =
                        "PASSED_" + selectedTopic!!.uppercase(Locale.getDefault()).replace(" ", "_")
                    editor.putBoolean(progressKey, true)
                    Log.d(
                        TAG,
                        "✅ Progreso guardado - Clave: " + progressKey + " = true (Tema del eButtonStart)"
                    )
                } else {
                    Log.d(TAG, "ℹ️ Tema completado pero no es del eButtonStart: " + selectedTopic)
                }
            } else {
                Log.d(TAG, "❌ Puntuación insuficiente para desbloquear progreso: " + score + "%")
            }

            val scoreKey =
                "SCORE_" + selectedTopic!!.uppercase(Locale.getDefault()).replace(" ", "_")
            editor.putInt(scoreKey, score)
            Log.d(TAG, "Puntuación guardada - Clave: " + scoreKey + " = " + score)

            editor.apply()
            Log.d(TAG, "=== FIN DE GUARDADO DE PROGRESO ===")

            val finalProgress = prefs.getBoolean(
                "PASSED_" + selectedTopic!!.uppercase(Locale.getDefault()).replace(" ", "_"), false
            )
            val finalScore = prefs.getInt(scoreKey, 0)
            Log.d(
                TAG,
                "Verificación final - Progreso: " + finalProgress + ", Puntuación: " + finalScore
            )

            if (finalScore >= 70) {
                StarProgressHelper.addSessionPoints(this, 10)
            } else {
                Log.d(TAG, "Sesión NO aprobada (<70%). No se suman puntos ni se muestra estrella.")
            }
        } else {
            Log.e(TAG, "❌ Error: No se puede guardar el progreso - Tema o nivel es null")
            Log.e(TAG, "selectedTopic: " + selectedTopic)
            Log.e(TAG, "selectedLevel: " + selectedLevel)
        }
    }

    private fun playCurrentQuestion() {
        if (currentQuestionIndex < currentQuestions!!.size) {
            val question = currentQuestions!!.get(currentQuestionIndex)
            Log.d(TAG, "Playing question: " + question.question)

            startQuestionPlaybackFromPosition(0)

            val questionAnswered = nextButton!!.isEnabled()

            if (!questionAnswered) {
                enableVisibleButtons()
                Log.d(TAG, "Botones habilitados después de reproducir audio")
            } else {
                Log.d(TAG, "Pregunta ya respondida, no se habilitan los botones")
            }
        }
    }

    private fun enableVisibleButtons() {
        if (option1Button!!.getVisibility() == View.VISIBLE) {
            option1Button!!.setEnabled(true)
        }
        if (option2Button!!.getVisibility() == View.VISIBLE) {
            option2Button!!.setEnabled(true)
        }
        if (option3Button!!.getVisibility() == View.VISIBLE) {
            option3Button!!.setEnabled(true)
        }
        if (option4Button!!.getVisibility() == View.VISIBLE) {
            option4Button!!.setEnabled(true)
        }
    }

    private fun checkAnswer(buttonIndex: Int) {
        if (!option1Button!!.isEnabled() && !option2Button!!.isEnabled() && !option3Button!!.isEnabled() && !option4Button!!.isEnabled()) {
            Log.d(TAG, "Pregunta ya respondida, no se puede volver a seleccionar")
            return
        }

        var selectedButton: Button? = null
        var selectedAnswer = ""

        when (buttonIndex) {
            1 -> {
                selectedButton = option1Button
                selectedAnswer = option1Button!!.getText().toString()
            }

            2 -> {
                selectedButton = option2Button
                selectedAnswer = option2Button!!.getText().toString()
            }

            3 -> {
                selectedButton = option3Button
                selectedAnswer = option3Button!!.getText().toString()
            }

            4 -> {
                selectedButton = option4Button
                selectedAnswer = option4Button!!.getText().toString()
            }
        }

        if (selectedButton == null) {
            Log.e(TAG, "Botón seleccionado es null")
            return
        }

        val currentQuestion = currentQuestions!!.get(currentQuestionIndex)
        val isCorrect = selectedAnswer == currentQuestion.correctAnswer

        val wasQuestionChanged = wildcardHelper!!.wasQuestionChanged(currentQuestionIndex)

        Log.d(TAG, "=== VERIFICACIÓN DE RESPUESTA LISTENING ===")
        Log.d(TAG, "Pregunta: " + currentQuestion.question)
        Log.d(TAG, "Respuesta seleccionada: '" + selectedAnswer + "'")
        Log.d(TAG, "Respuesta correcta: '" + currentQuestion.correctAnswer + "'")
        Log.d(TAG, "¿Son iguales? " + isCorrect)
        Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged)

        if (!shouldShowSpokenText(currentQuestion.topic ?: "")) {
            questionTextView!!.setText("Texto hablado / Spoken text: " + currentQuestion.question)
        }

        saveAnswerToDatabase(currentQuestion, selectedAnswer, isCorrect)

        if (isCorrect) {
            if (!wasQuestionChanged) {
                score++
                evaluatedQuestionsCount++
                Log.d(TAG, "Puntos sumados: +1 (Total: " + score + ")")
                Log.d(TAG, "Preguntas evaluadas: " + evaluatedQuestionsCount)
            } else {
                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín")
            }

            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_ok)
            }
            playCorrectSound()

            // ============================================================
            // CAMBIO: Usar el componente para mostrar modal correcto
            // ============================================================
            if (modalAlertComponent != null) {
                modalAlertComponent!!.showCorrectModal(null, null)
                Log.d(TAG, "Correct modal shown via component")
            } else {
                Log.e(TAG, "modalAlertComponent is NULL!")
            }
        } else {
            if (!wasQuestionChanged) {
                evaluatedQuestionsCount++
                Log.d(
                    TAG,
                    "Pregunta incorrecta evaluada. Preguntas evaluadas: " + evaluatedQuestionsCount
                )
            }

            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_bad)
            }
            playIncorrectSound()

            // ============================================================
            // CAMBIO: Usar el componente para mostrar modal incorrecto
            // ============================================================
            if (modalAlertComponent != null) {
                modalAlertComponent!!.showIncorrectModal(null, null)
                Log.d(TAG, "Incorrect modal shown via component")
            } else {
                Log.e(TAG, "modalAlertComponent is NULL!")
            }
        }

        highlightButtons(buttonIndex, isCorrect, currentQuestion.correctAnswer)

        option1Button!!.setEnabled(false)
        option2Button!!.setEnabled(false)
        option3Button!!.setEnabled(false)
        option4Button!!.setEnabled(false)

        var feedback: String?
        if (isCorrect) {
            if (wasQuestionChanged) {
                feedback = "¡Correcto! (Pregunta cambiada por comodín - No suma puntos)"
            } else {
                feedback = "¡Correcto!"
            }
        } else {
            feedback = "Incorrecto. La respuesta correcta es: " + currentQuestion.correctAnswer
        }

        if (!wasQuestionChanged) {
            feedback += "\nProgreso: " + score + "/" + evaluatedQuestionsCount + " preguntas evaluadas"
        }

        nextButton!!.setEnabled(true)
    }

    /**
     * Método para avanzar a la siguiente pregunta de forma segura
     */
    private fun advanceToNextQuestion() {
        try {
            if (currentQuestionIndex + 1 < currentQuestions!!.size) {
                currentQuestionIndex++
                displayQuestion()
                Log.d(
                    TAG,
                    "Advanced to next question: " + (currentQuestionIndex + 1) + "/" + currentQuestions!!.size
                )
            } else {
                Log.d(TAG, "No more questions, showing results")
                showResults()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error advancing to next question: " + e.message, e)
        }
    }

    private fun highlightButtons(selectedOption: Int, isCorrect: Boolean, correctAnswer: String?) {
        val options = arrayOf<String?>(
            if (option1Button!!.getVisibility() == View.VISIBLE) option1Button!!.getText()
                .toString() else "",
            if (option2Button!!.getVisibility() == View.VISIBLE) option2Button!!.getText()
                .toString() else "",
            if (option3Button!!.getVisibility() == View.VISIBLE) option3Button!!.getText()
                .toString() else "",
            if (option4Button!!.getVisibility() == View.VISIBLE) option4Button!!.getText()
                .toString() else ""
        )

        var correctIndex = -1
        for (i in options.indices) {
            if (options[i] == correctAnswer) {
                correctIndex = i + 1
                break
            }
        }

        if (isCorrect) {
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.verdeSena))
        } else {
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.error_color))
            if (correctIndex != selectedOption && correctIndex != -1) {
                getButtonByIndex(correctIndex).setBackgroundTintList(getColorStateList(com.example.speak.R.color.verdeSena))
            }
        }

        Log.d(
            TAG,
            "Botones resaltados - Seleccionado: " + selectedOption + ", Correcto: " + correctIndex + ", ¿Es correcto?: " + isCorrect
        )
    }

    private fun getButtonByIndex(index: Int): Button {
        when (index) {
            1 -> return option1Button!!
            2 -> return option2Button!!
            3 -> return option3Button!!
            4 -> return option4Button!!
            else -> return option1Button!!
        }
    }

    private fun disableAllButtons() {
        option1Button!!.setEnabled(false)
        option2Button!!.setEnabled(false)
        option3Button!!.setEnabled(false)
        option4Button!!.setEnabled(false)
    }

    private fun saveAnswerToDatabase(
        question: ListeningQuestion,
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
                "Listening",
                question.topic,
                question.level,
                sessionTimestamp
            )
            Log.d(
                TAG,
                "Answer saved to quiz_results table successfully with session timestamp: " + sessionTimestamp
            )

            if (!isOfflineMode && mAuth!!.getCurrentUser() != null) {
                val questionId = currentQuestionIndex.toString()
                val answerData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                answerData.put("question", question.question)
                answerData.put("correctAnswer", question.correctAnswer)
                answerData.put("selectedAnswer", selectedAnswer)
                answerData.put("isCorrect", isCorrect)
                answerData.put("timestamp", sessionTimestamp)
                answerData.put("speed", currentSpeed)
                answerData.put("pitch", currentPitch)
                answerData.put("quizType", "Listening")
                answerData.put("topic", question.topic)
                answerData.put("level", question.level)

                mDatabase!!.child("users").child(mAuth!!.getCurrentUser()!!.getUid())
                    .child("quiz_results")
                    .child(questionId)
                    .setValue(answerData)
                Log.d(TAG, "Answer saved to Firebase successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving answer: " + e.message)
        }
    }

    private fun showResults() {
        val finalScore: Int
        if (evaluatedQuestionsCount > 0) {
            finalScore = ((score / evaluatedQuestionsCount.toDouble()) * 100).toInt()
            Log.d(TAG, "=== CÁLCULO DE PUNTAJE FINAL ===")
            Log.d(TAG, "Puntaje obtenido: " + score)
            Log.d(TAG, "Preguntas evaluadas: " + evaluatedQuestionsCount)
            Log.d(TAG, "Preguntas totales: " + currentQuestions!!.size)
            Log.d(
                TAG,
                "Preguntas cambiadas por comodín: " + (currentQuestions!!.size - evaluatedQuestionsCount)
            )
            Log.d(TAG, "Puntaje final calculado: " + finalScore + "%")
        } else {
            finalScore = 0
            Log.d(TAG, "No hay preguntas evaluadas, puntaje final: 0%")
        }

        if (selectedTopic != null && selectedTopic == "ALPHABET" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_ALPHABET", true)
            editor.apply()
            Log.d(TAG, "ALPHABET passed with score: " + finalScore)
        }

        if (selectedTopic != null && selectedTopic == "NUMBERS" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_NUMBERS", true)
            editor.apply()
            Log.d(TAG, "NUMBERS passed with score: " + finalScore)
        }

        if (selectedTopic != null && selectedTopic == "COLORS" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_COLORS", true)
            editor.apply()
            Log.d(TAG, "COLORS passed with score: " + finalScore)
        }

        if (selectedTopic != null && selectedTopic == "PERSONAL PRONOUNS" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_PERSONAL_PRONOUNS", true)
            editor.apply()
            Log.d(TAG, "PERSONAL PRONOUNS passed with score: " + finalScore)
        }

        if (selectedTopic != null && selectedTopic == "POSSESSIVE ADJECTIVES" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_POSSESSIVE_ADJECTIVES", true)
            editor.apply()
            Log.d(TAG, "POSSESSIVE ADJECTIVES passed with score: " + finalScore)
        }

        if (selectedTopic != null && selectedTopic == "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_PREPOSITIONS_OF_PLACE", true)
            editor.apply()
            Log.d(
                TAG,
                "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION passed with score: " + finalScore
            )
        }

        if (selectedTopic != null && selectedTopic == "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_ADJECTIVES", true)
            editor.apply()
            Log.d(
                TAG,
                "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY) passed with score: " + finalScore
            )
        }

        if (selectedTopic != null && selectedTopic == "ORDINAL AND CARDINAL NUMBERS" && finalScore >= 70) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("PASSED_ORDINAL", true)
            editor.apply()
            Log.d(TAG, "ORDINAL AND CARDINAL NUMBERS passed with score: " + finalScore)
        }

        // Crear el diálogo
        val builder = AlertDialog.Builder(this)
        val dialogView =
            getLayoutInflater().inflate(com.example.speak.R.layout.dialog_quiz_result, null)
        builder.setView(dialogView)

        builder.setView(dialogView)
        builder.setCancelable(false) // Evitar que se cierre sin seleccionar una opción

        // Crear y mostrar el diálogo
        val dialog = builder.create()
        dialog.show() // 👈 Primero se muestra

        // Eliminar el fondo blanco del contenedor
        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val birdImageView = dialogView.findViewById<ImageView>(com.example.speak.R.id.birdImageView)
        val messageTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.messageTextView)
        val counterTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.counterTextView)
        val scoreTextView = dialogView.findViewById<TextView>(com.example.speak.R.id.scoreTextView)
        val btnContinue = dialogView.findViewById<Button>(com.example.speak.R.id.btnContinue)
        val btnReintentar = dialogView.findViewById<TextView>(com.example.speak.R.id.btnReintentar)
        val btnViewDetails =
            dialogView.findViewById<LinearLayout>(com.example.speak.R.id.btnViewDetails)

        if (finalScore >= 100) {
            messageTextView.setText("Excellent your English is getting better!")
            counterTextView.setText("10/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_ok)
        } else if (finalScore >= 90) {
            messageTextView.setText("Good, but you can do it better!")
            counterTextView.setText("9/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_ok)
        } else if (finalScore >= 80) {
            messageTextView.setText("Good, but you can do it better!")
            counterTextView.setText("8/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_ok)
        } else if (finalScore == 70) {
            messageTextView.setText("Good, but you can do it better!")
            counterTextView.setText("7/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_test)
        } else if (finalScore >= 69) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("6/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_test)
        } else if (finalScore >= 60) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("6/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_test)
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("5/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_bad)
        } else if (finalScore >= 40) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("4/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_bad)
        } else if (finalScore >= 30) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("3/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_bad)
        } else if (finalScore >= 20) {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("2/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_bad)
        } else {
            messageTextView.setText("You should practice more!")
            counterTextView.setText("1/10")
            birdImageView.setImageResource(com.example.speak.R.drawable.crab_bad)
        }

        var resultText = "Score: " + finalScore + "%"

        val questionsChanged = currentQuestions!!.size - evaluatedQuestionsCount
        if (questionsChanged > 0) {
            resultText += "\n\n📊 Detalles:"
            resultText += "\n• Preguntas evaluadas: " + evaluatedQuestionsCount + "/" + currentQuestions!!.size
            resultText += "\n• Preguntas cambiadas por comodín: " + questionsChanged
            resultText += "\n• Puntaje basado solo en preguntas evaluadas"
        }

        scoreTextView.setText(resultText)

        val finalSelectedTopic = selectedTopic
        val finalSelectedLevel = selectedLevel
        val finalScoreForLambda = finalScore

        if (finalScore >= 70) {
            btnContinue.setText(
                ProgressionHelper.getContinueButtonTextEnhanced(
                    this,
                    selectedTopic
                )
            )
            btnContinue.setOnClickListener(View.OnClickListener { v: View? ->
                ProgressionHelper.markTopicCompleted(this, finalSelectedTopic, finalScoreForLambda)
                val continueIntent =
                    ProgressionHelper.createContinueIntent(this, finalSelectedTopic, "listening")
                if (continueIntent != null) {
                    startActivity(continueIntent)
                    finish()
                } else {
                    val intent = Intent(this, MenuA1Activity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                }
            })
        } else {
            val nextTopic = ProgressionHelper.getNextTopic(this, selectedTopic)
            if (nextTopic == null) {
                btnContinue.setText("¡Nivel completado!")
                btnContinue.setOnClickListener(View.OnClickListener { v: View? ->
                    val intent = Intent(this, MenuA1Activity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                })
            } else {
                btnReintentar.setText("Try again")
                btnReintentar.setOnClickListener(View.OnClickListener { v: View? ->
                    val intent = Intent(this, ListeningActivity::class.java)
                    intent.putExtra("TOPIC", finalSelectedTopic)
                    intent.putExtra("LEVEL", finalSelectedLevel)
                    startActivity(intent)
                    finish()
                })
            }
        }

        btnViewDetails.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this, QuizHistoryActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_QUESTIONS", currentQuestions!!.size)
            intent.putExtra("QUIZ_TYPE", "Listening")
            intent.putExtra("TOPIC", finalSelectedTopic)
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true)
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp)
            startActivity(intent)
            finish()
        })

        val finalScoreCaptured = finalScore
        if (finalScoreCaptured >= 70) {
            Handler().postDelayed(Runnable {
                try {
                    StarEarnedDialog.show(this@ListeningActivity)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.message)
                }
            }, 200)
        }
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

    override fun onDestroy() {
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

        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }

        if (correctSoundPlayer != null) {
            correctSoundPlayer!!.release()
            correctSoundPlayer = null
        }

        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer!!.release()
            incorrectSoundPlayer = null
        }

        stopProgressUpdate()

        super.onDestroy()
    }

    // ===== SISTEMA DE COMODINES =====
    private fun showWildcardMenu() {
        if (wildcardHelper != null) {
            wildcardHelper!!.showWildcardMenu()
        }
    }

    private fun showHelp() {
        val helpIntent = Intent(this, HelpActivity::class.java)
        helpIntent.putExtra("topic", selectedTopic)
        helpIntent.putExtra("level", selectedLevel)
        startActivity(helpIntent)
    }

    private fun changeCurrentQuestion() {
        if (currentQuestions!!.size > 1) {
            wildcardHelper!!.markQuestionAsChanged(currentQuestionIndex)

            val newIndex = (currentQuestionIndex + 1) % currentQuestions!!.size
            currentQuestionIndex = newIndex

            displayQuestion()

            Toast.makeText(
                this,
                "Pregunta cambiada - La anterior no se evaluará",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "No hay más preguntas disponibles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showContentImage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Imagen de Contenido")
        builder.setMessage("Aquí se mostraría una imagen relacionada con: " + selectedTopic)
        builder.setPositiveButton("Entendido", null)
        builder.show()
    }

    private fun showInstructorVideo() {
        if (wildcardHelper != null && wildcardHelper!!.getVideoHelper() != null) {
            wildcardHelper!!.getVideoHelper().showInstructorVideo(selectedTopic)
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Video del Instructor")
            builder.setMessage("Aquí se reproduciría un video explicativo sobre: " + selectedTopic)
            builder.setPositiveButton("Entendido", null)
            builder.show()
        }
    }

    private fun applyFiftyFifty() {
        if (currentQuestionIndex < currentQuestions!!.size) {
            val currentQuestion = currentQuestions!!.get(currentQuestionIndex)
            val allOptions = currentQuestion.options
            val correctAnswer = currentQuestion.correctAnswer

            val remainingOptions = wildcardHelper!!.applyFiftyFifty(
                Arrays.asList<String?>(*allOptions!!), correctAnswer
            )

            val optionsText = StringBuilder("Opciones restantes:\n\n")
            for (i in remainingOptions.indices) {
                optionsText.append((i + 1)).append(". ").append(remainingOptions.get(i))
                    .append("\n")
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("🎯 Ayuda 50/50")
            builder.setMessage(optionsText.toString())
            builder.setPositiveButton("Entendido", null)
            builder.show()

            Toast.makeText(
                this,
                "50/50 aplicado - Opciones mostradas (no afecta tu evaluación)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCreativeHelp() {
        if (currentQuestionIndex < currentQuestions!!.size) {
            val currentQuestion = currentQuestions!!.get(currentQuestionIndex)
            val question = currentQuestion.question
            val correctAnswer = currentQuestion.correctAnswer

            val creativeHelp = wildcardHelper!!.getCreativeHelp(question, correctAnswer)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("💡 Ayuda Creativa")
            builder.setMessage(creativeHelp)
            builder.setPositiveButton("Entendido", null)
            builder.show()
        }
    }

    private fun showWildcardUsageInfo() {
        val questionsChanged = currentQuestions!!.size - evaluatedQuestionsCount
        val wildcardsUsed = questionsChanged

        val info = StringBuilder()
        info.append("📊 Información de Comodines\n\n")
        info.append("• Preguntas totales: ").append(currentQuestions!!.size).append("\n")
        info.append("• Preguntas evaluadas: ").append(evaluatedQuestionsCount).append("\n")
        info.append("• Preguntas cambiadas: ").append(questionsChanged).append("\n")
        info.append("• Comodines utilizados: ").append(wildcardsUsed).append("\n\n")

        if (questionsChanged > 0) {
            info.append("ℹ️ Las preguntas cambiadas por comodín no afectan tu puntuación final.\n")
            info.append("Tu puntuación se calcula solo con las preguntas que realmente evaluaste.")
        } else {
            info.append("✅ No se han usado comodines en esta sesión.")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("🎯 Uso de Comodines")
        builder.setMessage(info.toString())
        builder.setPositiveButton("Entendido", null)
        builder.show()
    }

    private fun startQuestionPlaybackFromPosition(position: Long) {
        var position = position
        Log.d(TAG, "Starting question playback from position: " + position + "ms")

        if (textToSpeech == null) {
            Log.e(TAG, "TextToSpeech is null")
            return
        }

        if (textToSpeech!!.getLanguage() == null) {
            Log.e(TAG, "Language not set")
            return
        }

        var remainingTime = totalDuration - position
        if (remainingTime <= 0) {
            position = 0
            remainingTime = totalDuration
        }

        if (currentQuestionIndex < currentQuestions!!.size) {
            val currentQuestion = currentQuestions!!.get(currentQuestionIndex)
            val textToSpeak = currentQuestion.question

            Log.d(TAG, "Text to speak: " + textToSpeak)

            val result =
                textToSpeech!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "QuestionAudio")
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text")
            } else {
                Log.d(TAG, "Question audio playback started from position: " + position + "ms")

                isPlaying = true
                isPaused = false
                startTime = System.currentTimeMillis() - position
                currentPosition = position

                enableVisibleButtons()
                Log.d(
                    TAG,
                    "Botones habilitados después de reproducir audio desde el panel de control"
                )
            }
        }
    }

    private fun stopProgressUpdate() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable)
        }
    }

    private fun formatTime(milliseconds: Long): String {
        var seconds = milliseconds / 1000
        val minutes = seconds / 60
        seconds = seconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    companion object {
        private const val TAG = "ListeningActivity"
    }
}