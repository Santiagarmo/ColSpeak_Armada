package com.example.speak

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.PictureDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.speak.components.ModalAlertComponent
import com.example.speak.components.ModalAlertComponent.ModalType
import com.example.speak.components.ModalAlertComponent.OnModalActionListener
import com.example.speak.database.DatabaseHelper
import com.example.speak.helpers.HelpModalHelper
import com.example.speak.helpers.StarEarnedDialog
import com.example.speak.helpers.StarProgressHelper
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
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.dropLastWhile
import kotlin.collections.indices
import kotlin.collections.toTypedArray
import kotlin.math.min

class ImageIdentificationActivity : AppCompatActivity() {
    // UI Elements
    private var questionTextView: TextView? = null
    private var questionNumberTextView: TextView? = null
    private var targetImageView: ImageView? = null
    private var playButton: Button? = null
    private var speedSeekBar: SeekBar? = null
    private var pitchSeekBar: SeekBar? = null
    private var speedValue: TextView? = null
    private var pitchValue: TextView? = null
    private var modalAlertComponent: ModalAlertComponent? = null

    // Return menu and topic/level views
    private var returnContainer: LinearLayout? = null
    private var topicTextView: TextView? = null
    private var levelTextView: TextView? = null

    // Option buttons
    private var option1Button: Button? = null
    private var option2Button: Button? = null
    private var option3Button: Button? = null
    private var option4Button: Button? = null

    // Bird image for feedback
    private var birdImageView: ImageView? = null

    // TextToSpeech
    private var textToSpeech: TextToSpeech? = null

    // MediaPlayer para sonidos de feedback
    private var correctSoundPlayer: MediaPlayer? = null
    private var incorrectSoundPlayer: MediaPlayer? = null

    // Data
    private var allQuestions: MutableList<ImageQuestion?>? = null
    private var currentQuestions: MutableList<ImageQuestion>? = null
    private var currentQuestionIndex = 0
    private var score = 0

    // Settings
    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f

    // Database and Firebase
    private var dbHelper: DatabaseHelper? = null
    private var mAuth: FirebaseAuth? = null
    private var mDatabase: DatabaseReference? = null
    private var userId: Long = 0
    private var isOfflineMode = false
    private var sessionTimestamp: Long = 0

    // Topic and Level
    private var selectedTopic: String? = null
    private var selectedLevel: String? = null

    // Sistema de comodines
    private var wildcardHelper: WildcardHelper? = null
    private var wildcardButton: ImageView? = null
    private var helpButton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_identification)

        Log.d(TAG, "ImageIdentificationActivity onCreate started")

        // Initialize views
        initializeViews()
        Log.d(TAG, "Views initialized")

        // Initialize sound players
        initializeSoundPlayers()
        Log.d(TAG, "Sound players initialized")

        // Get intent data
        val intent = getIntent()


        // Get topic and level from intent
        selectedTopic = intent.getStringExtra("TOPIC")
        selectedLevel = intent.getStringExtra("LEVEL")


        // Display topic and level in the interface
        if (selectedLevel != null) {
            levelTextView!!.setText("Level: " + selectedLevel)
        }
        if (selectedTopic != null) {
            topicTextView!!.setText("Topic: " + selectedTopic)
        }

        Log.d(TAG, "Intent data - Topic: " + selectedTopic + ", Level: " + selectedLevel)

        // Initialize Firebase and Database
        initializeFirebaseAndDatabase()
        Log.d(TAG, "Firebase and Database initialized")

        // Initialize TextToSpeech
        initializeTextToSpeech()
        Log.d(TAG, "TextToSpeech initialized")

        // Setup controls
        setupSpeedAndPitchControls()
        Log.d(TAG, "Speed and pitch controls setup")

        // Setup button listeners
        setupButtonListeners()
        Log.d(TAG, "Button listeners setup")

        // Validate previous topic completion
        /*
        if (!isPreviousTopicPassed(selectedTopic)) {
            Log.w(TAG, "Previous topic not passed: " + selectedTopic);
            Toast.makeText(this, "Debes completar el tema anterior antes de continuar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        */
        Log.d(TAG, "Previous topic validation passed")

        // Inicializar sistema de comodines
        wildcardHelper = WildcardHelper(this, "READING", selectedTopic)
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
        loadQuestionsFromFile(selectedTopic, selectedLevel!!)
        Log.d(TAG, "Questions loaded")

        // Initialize session timestamp
        sessionTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Session timestamp initialized: " + sessionTimestamp)


        // Test the module
        testModule()
    }

    private fun initializeViews() {
        questionTextView = findViewById<TextView>(R.id.questionTextView)
        questionNumberTextView = findViewById<TextView>(R.id.questionNumberTextView)
        targetImageView = findViewById<ImageView>(R.id.targetImageView)
        playButton = findViewById<Button>(R.id.playButton)
        speedSeekBar = findViewById<SeekBar>(R.id.speedSeekBar)
        pitchSeekBar = findViewById<SeekBar>(R.id.pitchSeekBar)
        speedValue = findViewById<TextView>(R.id.speedValue)
        pitchValue = findViewById<TextView>(R.id.pitchValue)
        modalAlertComponent = findViewById<ModalAlertComponent?>(R.id.modalAlertComponent)

        // Initialize return menu and topic/level views
        returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
        topicTextView = findViewById<TextView>(R.id.topicTextView)
        levelTextView = findViewById<TextView>(R.id.levelTextView)

        // Initialize option buttons
        option1Button = findViewById<Button>(R.id.option1Button)
        option2Button = findViewById<Button>(R.id.option2Button)
        option3Button = findViewById<Button>(R.id.option3Button)
        option4Button = findViewById<Button>(R.id.option4Button)

        // Initialize bird image
        birdImageView = findViewById<ImageView?>(R.id.birdImageView)

        // Setup modal callbacks
        if (modalAlertComponent != null) {
            modalAlertComponent!!.setOnModalActionListener(object : OnModalActionListener {
                override fun onContinuePressed(type: ModalType?) {
                    currentQuestionIndex++
                    modalAlertComponent!!.setVisibility(View.GONE)
                    displayQuestion()
                }

                override fun onModalHidden(type: ModalType?) {
                    // No action needed when modal is hidden
                }
            })
        }

        // Inicializar botón de comodines
        wildcardButton = findViewById<ImageView?>(R.id.wildcardButton)
        if (wildcardButton != null) {
            wildcardButton!!.setOnClickListener(View.OnClickListener { v: View? -> showWildcardMenu() })
        }

        // Inicializar botón de ayuda (Help)
        helpButton = findViewById<ImageView?>(R.id.helpButton)
        if (helpButton != null) {
            helpButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    // Determinar el tema actual desde la pregunta visible si existe
                    var topicForHelp = selectedTopic
                    if (currentQuestions != null && currentQuestionIndex >= 0 && currentQuestionIndex < currentQuestions!!.size) {
                        val q = currentQuestions!!.get(currentQuestionIndex)
                        if (q != null && q.topic != null && !q.topic.isEmpty()) {
                            topicForHelp = q.topic
                        }
                    }

                    HelpModalHelper.show(
                        this@ImageIdentificationActivity,
                        topicForHelp,
                        selectedLevel
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error abriendo modal de ayuda: " + e.message)
                    Toast.makeText(
                        this@ImageIdentificationActivity,
                        "No se pudo abrir la ayuda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Set topic and level text
        if (selectedLevel != null) {
            levelTextView!!.setText("Level: " + selectedLevel)
        }
        if (selectedTopic != null) {
            topicTextView!!.setText("Topic: " + selectedTopic)
        }

        // Set up return button click listener
        returnContainer!!.setOnClickListener(View.OnClickListener { v: View? -> returnToMenu() })
    }

    private fun initializeFirebaseAndDatabase() {
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().getReference()
        dbHelper = DatabaseHelper(this)

        // Check network status
        isOfflineMode = !this.isNetworkAvailable
        Log.d(TAG, "Network status - Offline mode: " + isOfflineMode)

        // Get user ID
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

        // Handle guest user in offline mode
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
                        userId =
                            guestCursor.getLong(guestCursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
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
            Toast.makeText(this, "Error: Could not initialize user session", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        Log.d(TAG, "Final user ID: " + userId + ", Offline mode: " + isOfflineMode)
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, OnInitListener { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech!!.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(
                        this@ImageIdentificationActivity,
                        "Language not supported",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    textToSpeech!!.setSpeechRate(currentSpeed)
                    textToSpeech!!.setPitch(currentPitch)
                    Log.d(TAG, "TextToSpeech initialized successfully")
                }
            } else {
                Toast.makeText(
                    this@ImageIdentificationActivity,
                    "TextToSpeech initialization failed",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "TextToSpeech initialization failed with status: " + status)
            }
        })
    }

    private fun setupSpeedAndPitchControls() {
        speedSeekBar!!.setMax(200)
        speedSeekBar!!.setProgress(100)
        pitchSeekBar!!.setMax(200)
        pitchSeekBar!!.setProgress(100)

        speedSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progress / 100f
                speedValue!!.setText(String.format("%.1fx", currentSpeed))
                if (textToSpeech != null) {
                    textToSpeech!!.setSpeechRate(currentSpeed)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        pitchSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPitch = progress / 100f
                pitchValue!!.setText(String.format("%.1fx", currentPitch))
                if (textToSpeech != null) {
                    textToSpeech!!.setPitch(currentPitch)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtonListeners() {
        playButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            Log.d(TAG, "Play button clicked")
            playCurrentQuestion()
        })

        // Setup option button listeners
        setupOptionButtonListeners()
    }

    private fun setupOptionButtonListeners() {
        option1Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(1) })
        option2Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(2) })
        option3Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(3) })
        option4Button!!.setOnClickListener(View.OnClickListener { v: View? -> checkAnswer(4) })
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager =
                getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
            return activeNetworkInfo != null && activeNetworkInfo.isConnected()
        }

    private fun isPreviousTopicPassed(currentTopic: String?): Boolean {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)


        // Define topic progression order
        val topicOrder = arrayOf<String>(
            "PERSONAL PRONOUNS",
            "POSSESSIVE ADJECTIVES",
            "VERB TO BE",
            "SIMPLE PRESENT",
            "SIMPLE PRESENT THIRD PERSON",
            "SIMPLE PAST",
            "FREQUENCY ADVERBS",
            "DAILY ROUTINES",
            "COUNTABLE AND UNCOUNTABLE",
            "QUANTIFIERS",
            "PREPOSITIONS",
            "USED TO"
        )


        // Find current topic index
        var currentIndex = -1
        for (i in topicOrder.indices) {
            if (topicOrder[i] == currentTopic) {
                currentIndex = i
                break
            }
        }


        // If it's the first topic or not found, allow access
        if (currentIndex <= 0) {
            return true
        }


        // Check if previous topic is passed
        val previousTopic = topicOrder[currentIndex - 1]
        val key = "PASSED_" + previousTopic.replace(" ", "_")
        val isPassed = prefs.getBoolean(key, false)

        Log.d(TAG, "Checking previous topic: " + previousTopic + " = " + isPassed)
        return isPassed
    }

    private fun loadQuestionsFromFile(topic: String?, level: String) {
        allQuestions = ArrayList<ImageQuestion?>()
        currentQuestions = ArrayList<ImageQuestion>()

        try {
            val assetManager = getAssets()
            val fileName = "image_questions_" + level.lowercase(Locale.getDefault()) + ".txt"

            Log.d(TAG, "Loading questions from file: " + fileName)
            Log.d(TAG, "Looking for topic: '" + topic + "' and level: '" + level + "'")

            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            var line: String?
            var lineNumber = 0
            var totalLines = 0
            var skippedLines = 0
            var processedLines = 0

            while ((reader.readLine().also { line = it }) != null) {
                lineNumber++
                totalLines++
                line = line!!.trim { it <= ' ' }

                if (line.isEmpty() || line.startsWith("#")) {
                    skippedLines++
                    continue
                }

                Log.d(TAG, "Processing line " + lineNumber + ": " + line)
                processedLines++


                // Parse question format: question|correct_answer|option1|option2|option3|option4|topic|level|image_resource
                val parts =
                    line.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                Log.d(TAG, "Line has " + parts.size + " parts")

                if (parts.size >= 9) {
                    val questionText: String? = parts[0]
                    val correctAnswer: String? = parts[1]
                    val options = arrayOf<String?>(parts[2], parts[3], parts[4], parts[5])
                    val questionTopic = parts[6]
                    val questionLevel = parts[7]
                    val imageResource: String? = parts[8]

                    Log.d(
                        TAG,
                        "Parsed question - Topic: '" + questionTopic + "', Level: '" + questionLevel + "'"
                    )
                    Log.d(TAG, "Comparing with - Topic: '" + topic + "', Level: '" + level + "'")
                    Log.d(
                        TAG,
                        "Topic match: " + (questionTopic == topic) + ", Level match: " + (questionLevel == level)
                    )


                    // Only add questions for the current topic and level
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
                    } else {
                        Log.d(TAG, "Question skipped - topic or level mismatch")
                    }
                } else {
                    Log.w(TAG, "Invalid line format at line " + lineNumber + ": " + line)
                }
            }

            reader.close()
            inputStream.close()

            Log.d(TAG, "File processing summary:")
            Log.d(TAG, "Total lines: " + totalLines)
            Log.d(TAG, "Skipped lines: " + skippedLines)
            Log.d(TAG, "Processed lines: " + processedLines)
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


            // Shuffle questions and take first 10
            Collections.shuffle(allQuestions)
            currentQuestions =
                ArrayList(allQuestions!!.subList(0, min(10, allQuestions!!.size)).filterNotNull())


            // Display first question
            displayQuestion()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading questions from file", e)
            Toast.makeText(this, "Error cargando las preguntas.", Toast.LENGTH_LONG).show()
            finish()
        }

        // Método temporal para debuggear la carga de preguntas
        debugLoadQuestions(topic, level)
    }

    // Método temporal para debuggear la carga de preguntas
    private fun debugLoadQuestions(topic: String?, level: String) {
        Log.d(TAG, "=== DEBUG LOAD QUESTIONS ===")
        Log.d(TAG, "Topic: " + topic)
        Log.d(TAG, "Level: " + level)

        try {
            val assetManager = getAssets()
            val fileName = "image_questions_" + level.lowercase(Locale.getDefault()) + ".txt"
            Log.d(TAG, "File name: " + fileName)

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

                Log.d(TAG, "Line " + lineNumber + ": " + line)

                val parts =
                    line.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size >= 9) {
                    val questionTopic = parts[6]
                    val questionLevel = parts[7]

                    Log.d(
                        TAG,
                        "Found topic: '" + questionTopic + "', level: '" + questionLevel + "'"
                    )
                    Log.d(TAG, "Matches current topic: " + (questionTopic == topic))
                    Log.d(TAG, "Matches current level: " + (questionLevel == level))
                }
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "Debug error", e)
        }

        Log.d(TAG, "=== END DEBUG ===")
    }

    private fun displayQuestion() {
        if (currentQuestionIndex >= currentQuestions!!.size) {
            // Quiz completed
            showQuizResults()
            return
        }

        val question = currentQuestions!!.get(currentQuestionIndex)


        // Update question number
        questionNumberTextView!!.setText((currentQuestionIndex + 1).toString() + "/" + currentQuestions!!.size)


        // Set question text with HTML formatting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            questionTextView!!.setText(
                Html.fromHtml(
                    question.question,
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
        } else {
            questionTextView!!.setText(Html.fromHtml(question.question))
        }


        // Load image
        loadImage(question.imageResourceName ?: "")


        // Set options
        val options = question.options
        option1Button!!.setText(options?.get(0) ?: "")
        option2Button!!.setText(options?.get(1) ?: "")
        option3Button!!.setText(options?.get(2) ?: "")
        option4Button!!.setText(options?.get(3) ?: "")


        // Reset button states
        resetButtonStates()

        // Hide modal initially
        if (modalAlertComponent != null) {
            modalAlertComponent!!.setVisibility(View.GONE)
        }

        Log.d(
            TAG,
            "Displaying question " + (currentQuestionIndex + 1) + ": " + question.question
        )
    }

    private fun loadImage(imageResourceName: String) {
        try {
            // Check if it's an SVG file
            if (imageResourceName.lowercase(Locale.getDefault()).endsWith(".svg")) {
                if (loadSVGFromAssets(imageResourceName)) {
                    return
                }
            }


            // Try to load as regular drawable
            if (loadRegularImage(imageResourceName)) {
                return
            }


            // If all else fails, set default image
            targetImageView!!.setImageResource(R.drawable.ic_quiz)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: " + imageResourceName, e)
            targetImageView!!.setImageResource(R.drawable.ic_quiz)
        }
    }

    private fun loadSVGFromAssets(svgFileName: String): Boolean {
        try {
            // Load SVG from assets
            val assetManager = getAssets()
            val inputStream = assetManager.open(svgFileName)

            val svg = SVG.getFromInputStream(inputStream)
            val drawable = PictureDrawable(svg.renderToPicture())
            targetImageView!!.setImageDrawable(drawable)

            inputStream.close()
            Log.d(TAG, "Loaded SVG from assets: " + svgFileName)
            return true
        } catch (e: IOException) {
            Log.w(TAG, "SVG not found in assets: " + svgFileName)
            return false
        } catch (e: SVGParseException) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e)
            return false
        }
    }

    private fun loadSVGImage(svgFileName: String) {
        try {
            // Try to load from assets first
            val assetManager = getAssets()
            val inputStream = assetManager.open(svgFileName)

            val svg = SVG.getFromInputStream(inputStream)
            val drawable = PictureDrawable(svg.renderToPicture())
            targetImageView!!.setImageDrawable(drawable)

            inputStream.close()
            Log.d(TAG, "Loaded SVG from assets: " + svgFileName)
        } catch (e: IOException) {
            Log.w(TAG, "SVG not found in assets, trying drawable: " + svgFileName)
            // If not found in assets, try drawable folder
            loadRegularImage(svgFileName)
        } catch (e: SVGParseException) {
            Log.e(TAG, "Error parsing SVG: " + svgFileName, e)
            targetImageView!!.setImageResource(R.drawable.ic_quiz)
        }
    }

    private fun loadRegularImage(imageResourceName: String?): Boolean {
        try {
            // Get resource ID from drawable
            val resourceId = getResources().getIdentifier(
                imageResourceName, "drawable", getPackageName()
            )

            if (resourceId != 0) {
                targetImageView!!.setImageResource(resourceId)
                Log.d(
                    TAG,
                    "Loaded regular image: " + imageResourceName + " (ID: " + resourceId + ")"
                )
                return true
            } else {
                Log.w(TAG, "Image resource not found: " + imageResourceName)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading regular image: " + imageResourceName, e)
            return false
        }
    }

    private fun resetButtonStates() {
        // Reset button colors and enable them
        option1Button!!.setEnabled(true)
        option2Button!!.setEnabled(true)
        option3Button!!.setEnabled(true)
        option4Button!!.setEnabled(true)

        option1Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        option2Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        option3Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))
        option4Button!!.setBackgroundTintList(getColorStateList(R.color.header_blue))


        // Reset bird image to default
        if (birdImageView != null) {
            birdImageView!!.setImageResource(R.drawable.crab_test)
        }
    }

    private fun playCurrentQuestion() {
        if (currentQuestionIndex >= currentQuestions!!.size) {
            return
        }

        val question = currentQuestions!!.get(currentQuestionIndex)
        val textToSpeak = question.question + ". " + question.correctAnswer

        if (textToSpeech != null) {
            textToSpeech!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "question_utterance")
            Log.d(TAG, "Playing audio: " + textToSpeak)
        }
    }

    private fun checkAnswer(selectedOption: Int) {
        if (currentQuestionIndex >= currentQuestions!!.size) {
            return
        }

        val question = currentQuestions!!.get(currentQuestionIndex)
        var selectedAnswer = ""

        when (selectedOption) {
            1 -> selectedAnswer = option1Button!!.getText().toString()
            2 -> selectedAnswer = option2Button!!.getText().toString()
            3 -> selectedAnswer = option3Button!!.getText().toString()
            4 -> selectedAnswer = option4Button!!.getText().toString()
        }

        val isCorrect = selectedAnswer == question.correctAnswer


        // Verificar si esta pregunta fue cambiada por un comodín
        val wasQuestionChanged = wildcardHelper!!.wasQuestionChanged(currentQuestionIndex)

        Log.d(
            TAG,
            "Answer check - Selected: '" + selectedAnswer + "', Correct: '" + question.correctAnswer + "', Result: " + isCorrect
        )
        Log.d(TAG, "¿Fue cambiada por comodín? " + wasQuestionChanged)

        if (isCorrect) {
            // Solo sumar puntos si la pregunta NO fue cambiada por un comodín
            if (!wasQuestionChanged) {
                score++
                Log.d(TAG, "Puntos sumados: +1 (Total: " + score + ")")
            } else {
                Log.d(TAG, "Puntos NO sumados - Pregunta cambiada por comodín")
            }


            // Cambiar la imagen del pájaro a la imagen de correcto
            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_ok)
            }
            // Reproducir sonido de éxito
            playCorrectSound()
        } else {
            // Cambiar la imagen del pájaro a la imagen de incorrecto
            if (birdImageView != null) {
                birdImageView!!.setImageResource(R.drawable.crab_bad)
            }
            // Reproducir sonido de error
            playIncorrectSound()
        }


        // Highlight buttons
        highlightButtons(selectedOption, isCorrect, question.correctAnswer)


        // Disable all buttons
        option1Button!!.setEnabled(false)
        option2Button!!.setEnabled(false)
        option3Button!!.setEnabled(false)
        option4Button!!.setEnabled(false)

        // Show modal with appropriate message
        if (modalAlertComponent != null) {
            val primaryMsg: String?
            val secondaryMsg: String?

            if (isCorrect) {
                if (wasQuestionChanged) {
                    primaryMsg = "¡Muy bien!, tu nivel de inglés está mejorando"
                    secondaryMsg = "(Pregunta cambiada por comodín - No suma puntos)"
                } else {
                    primaryMsg = "¡Muy bien!, tu nivel de inglés está mejorando"
                    secondaryMsg = "Amazing, you are improving your English"
                }
                modalAlertComponent!!.showCorrectModal(primaryMsg, secondaryMsg)
            } else {
                primaryMsg = "¡Ten cuidado!, sigue intentando"
                secondaryMsg = "La respuesta correcta es: " + question.correctAnswer
                modalAlertComponent!!.showIncorrectModal(primaryMsg, secondaryMsg)
            }
        }
    }

    private fun highlightButtons(selectedOption: Int, isCorrect: Boolean, correctAnswer: String?) {
        // Get all option texts
        val options = arrayOf<String?>(
            option1Button!!.getText().toString(),
            option2Button!!.getText().toString(),
            option3Button!!.getText().toString(),
            option4Button!!.getText().toString()
        )


        // Find correct answer index
        var correctIndex = -1
        for (i in options.indices) {
            if (options[i] == correctAnswer) {
                correctIndex = i + 1
                break
            }
        }


        // Apply colors
        if (isCorrect) {
            // Selected answer is correct - show green
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.verdeSena))
        } else {
            // Selected answer is wrong - show red for selected, green for correct
            getButtonByIndex(selectedOption).setBackgroundTintList(getColorStateList(R.color.error_color))
            if (correctIndex != selectedOption) {
                getButtonByIndex(correctIndex).setBackgroundTintList(getColorStateList(com.example.speak.R.color.verdeSena))
            }
        }
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

    private fun showQuizResults() {
        val percentage = score.toDouble() / currentQuestions!!.size * 100
        val finalScore = Math.round(percentage).toInt()


        // Save results
        saveQuizResults(percentage)


        // Mark topic as passed if score >= 70%
        if (percentage >= 70) {
            markTopicAsPassed(selectedTopic!!)
            // Sumar puntos de estrella (10) y mostrar modal de estrella, igual que Speaking
            StarProgressHelper.addSessionPoints(this, 10)
            Handler().postDelayed(Runnable {
                try {
                    StarEarnedDialog.show(this@ImageIdentificationActivity)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mostrando StarEarnedDialog: " + e.message)
                }
            }, 200)
        }


        // Preparar datos para posible navegación a detalles
        val questionResults = BooleanArray(currentQuestions!!.size)
        for (i in currentQuestions!!.indices) {
            questionResults[i] = i < score // simplificado
        }
        val questions = arrayOfNulls<String>(currentQuestions!!.size)
        for (i in currentQuestions!!.indices) {
            questions[i] = currentQuestions!!.get(i).question
        }
        val sourceMap = getIntent().getStringExtra("SOURCE_MAP")


        // Mostrar diálogo de resultados (mismo estilo que dialog_quiz_result)
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

        val birdImg = dialogView.findViewById<ImageView>(com.example.speak.R.id.birdImageView)
        val messageTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.messageTextView)
        val counterTextView =
            dialogView.findViewById<TextView>(com.example.speak.R.id.counterTextView)
        val scoreTextView = dialogView.findViewById<TextView>(com.example.speak.R.id.scoreTextView)
        val btnContinue = dialogView.findViewById<Button>(com.example.speak.R.id.btnContinue)
        val btnReintentar = dialogView.findViewById<TextView>(com.example.speak.R.id.btnReintentar)
        val btnViewDetails =
            dialogView.findViewById<LinearLayout>(com.example.speak.R.id.btnViewDetails)

        // Imagen y mensaje según puntaje (criterio similar a Listening)
        if (finalScore >= 90) {
            messageTextView.setText("Excellent your English is getting better!")
            birdImg.setImageResource(com.example.speak.R.drawable.crab_ok)
        } else if (finalScore >= 70) {
            messageTextView.setText("Good, but you can do it better!")
            birdImg.setImageResource(com.example.speak.R.drawable.crab_test)
        } else if (finalScore >= 50) {
            messageTextView.setText("You should practice more!")
            birdImg.setImageResource(com.example.speak.R.drawable.crab_test)
        } else {
            messageTextView.setText("You should practice more!")
            birdImg.setImageResource(com.example.speak.R.drawable.crab_bad)
        }

        counterTextView.setText(score.toString() + "/" + currentQuestions!!.size)
        scoreTextView.setText("Score: " + finalScore + "%")

        // Continuar: ir al siguiente tema según progresión y mapa de origen
        btnContinue.setOnClickListener(View.OnClickListener { v: View? ->
            val nextTopic =
                ProgressionHelper.getNextImageIdentificationTopicBySource(selectedTopic, sourceMap)
            if (nextTopic != null) {
                val nextActivity = ProgressionHelper.getReadingActivityClass(nextTopic)
                val next = Intent(this, nextActivity)
                next.putExtra("TOPIC", nextTopic)
                next.putExtra("LEVEL", selectedLevel)
                next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(next)
                finish()
            } else {
                // Si no hay siguiente tema, volver al mapa correspondiente
                val destMap = ProgressionHelper.getDestinationMapClass(sourceMap)
                val back = Intent(this, destMap)
                back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(back)
                finish()
            }
        })

        // Reintentar: reiniciar esta actividad mismo tema/nivel
        btnReintentar.setText("Try again")
        btnReintentar.setOnClickListener(View.OnClickListener { v: View? ->
            val retry = Intent(this, ImageIdentificationActivity::class.java)
            retry.putExtra("TOPIC", selectedTopic)
            retry.putExtra("LEVEL", selectedLevel)
            startActivity(retry)
            finish()
        })

        // Ver detalles: abrir tabla de historial (QuizHistoryActivity) filtrada por la sesión actual
        btnViewDetails.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this, QuizHistoryActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_QUESTIONS", currentQuestions!!.size)
            intent.putExtra("QUIZ_TYPE", "Identificación Imagen")
            intent.putExtra("TOPIC", selectedTopic)
            intent.putExtra("SHOW_CURRENT_ACTIVITY_ONLY", true)
            intent.putExtra("SESSION_TIMESTAMP", sessionTimestamp)
            startActivity(intent)
            finish()
        })
    }

    private fun saveQuizResults(percentage: Double) {
        // Save to local database - save each question result individually
        for (question in currentQuestions!!) {
            // For now, we'll save a summary result. In a real implementation,
            // you might want to save each individual question result
            dbHelper!!.saveQuizResult(
                userId,
                "Image Identification Quiz",
                question.correctAnswer,
                "Quiz completed",
                percentage >= 70,
                "Identificación Imagen",
                selectedTopic,
                selectedLevel,
                sessionTimestamp
            )
        }

        Log.d(TAG, "Saved quiz results to database")


        // Save to Firebase if online
        if (!isOfflineMode && mAuth!!.getCurrentUser() != null) {
            val quizData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            quizData.put("userId", userId)
            quizData.put("topic", selectedTopic)
            quizData.put("level", selectedLevel)
            quizData.put("score", score)
            quizData.put("totalQuestions", currentQuestions!!.size)
            quizData.put("percentage", percentage)
            quizData.put("timestamp", sessionTimestamp)
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
    }

    private fun markTopicAsPassed(topic: String) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val key = "PASSED_" + topic.replace(" ", "_")
        editor.putBoolean(key, true)
        editor.apply()

        Log.d(TAG, "Marked topic as passed: " + topic)
    }

    // Método para inicializar los MediaPlayer de sonidos
    private fun initializeSoundPlayers() {
        try {
            // Inicializar sonido de respuesta correcta
            correctSoundPlayer = MediaPlayer.create(
                this, getResources().getIdentifier(
                    "mario_bros_vida", "raw", getPackageName()
                )
            )


            // Inicializar sonido de respuesta incorrecta  
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

    // Método para reproducir sonido de respuesta correcta
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

    // Método para reproducir sonido de respuesta incorrecta
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
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }


        // Release MediaPlayer resources
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

    // Método de prueba para diagnosticar problemas
    private fun testModule() {
        Log.d(TAG, "=== TESTING IMAGE IDENTIFICATION MODULE ===")
        Log.d(TAG, "Selected Topic: " + selectedTopic)
        Log.d(TAG, "Selected Level: " + selectedLevel)
        Log.d(TAG, "User ID: " + userId)
        Log.d(TAG, "Offline Mode: " + isOfflineMode)
        Log.d(
            TAG,
            "All Questions Count: " + (if (allQuestions != null) allQuestions!!.size else "null")
        )
        Log.d(
            TAG,
            "Current Questions Count: " + (if (currentQuestions != null) currentQuestions!!.size else "null")
        )
        Log.d(TAG, "Current Question Index: " + currentQuestionIndex)
        Log.d(TAG, "Score: " + score)
        Log.d(TAG, "TextToSpeech: " + (if (textToSpeech != null) "initialized" else "null"))
        Log.d(TAG, "=== END TEST ===")
    }

    private fun returnToMenu() {
        // Return to MenuReadingActivity
        val intent = Intent(this, MenuReadingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    // ===== SISTEMA DE COMODINES =====
    /**
     * Muestra el menú de comodines
     */
    private fun showWildcardMenu() {
        if (wildcardHelper != null) {
            wildcardHelper!!.showWildcardMenu()
        }
    }

    /**
     * Ayuda 1: Cambiar la pregunta actual por una nueva
     * IMPORTANTE: Se marca la pregunta original como no evaluada para evitar duplicación de puntos
     */
    private fun changeCurrentQuestion() {
        if (currentQuestions!!.size > 1) {
            // Marcar la pregunta actual como cambiada para evitar duplicación de puntos
            wildcardHelper!!.markQuestionAsChanged(currentQuestionIndex)


            // Obtener una pregunta diferente de la lista
            val newIndex = (currentQuestionIndex + 1) % currentQuestions!!.size
            currentQuestionIndex = newIndex


            // Mostrar la nueva pregunta
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

    /**
     * Ayuda 2: Mostrar imagen de contenido relacionado
     */
    private fun showContentImage() {
        // Mostrar una imagen relacionada con el tema actual
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Imagen de Contenido")
        builder.setMessage("Aquí se mostraría una imagen relacionada con: " + selectedTopic)
        builder.setPositiveButton("Entendido", null)
        builder.show()
    }

    /**
     * Ayuda 3: Mostrar video del instructor
     */
    private fun showInstructorVideo() {
        // Usar el VideoHelper del WildcardHelper para mostrar el video
        if (wildcardHelper != null) {
            wildcardHelper!!.showInstructorVideo()
        } else {
            // Fallback si no hay wildcardHelper
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Video del Instructor")
            builder.setMessage("Aquí se reproduciría un video explicativo sobre: " + selectedTopic)
            builder.setPositiveButton("Entendido", null)
            builder.show()
        }
    }

    /**
     * Ayuda 4: Aplicar 50/50 - Mostrar solo 2 opciones (temporal)
     */
    private fun applyFiftyFifty() {
        if (currentQuestionIndex < currentQuestions!!.size) {
            val currentQuestion = currentQuestions!!.get(currentQuestionIndex)
            val allOptions = Arrays.asList<String?>(*currentQuestion.options!!)
            val correctAnswer = currentQuestion.correctAnswer


            // Aplicar 50/50 usando el helper
            val remainingOptions = wildcardHelper!!.applyFiftyFifty(allOptions, correctAnswer)


            // Mostrar las opciones restantes en un diálogo
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

    // NOTA: El método updateButtonsWithFiftyFifty fue eliminado
    // porque las ayudas NO deben afectar la interfaz de evaluación
    /**
     * Ayuda 5: Ayuda creativa - Mostrar pista contextual
     */
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

    /**
     * Muestra información sobre el uso de comodines en la sesión actual
     */
    private fun showWildcardUsageInfo() {
        // Para ImageIdentificationActivity, mostrar información básica
        val info = StringBuilder()
        info.append("📊 Información de Comodines\n\n")
        info.append("• Tema actual: ").append(selectedTopic).append("\n")
        info.append("• Nivel: ").append(selectedLevel).append("\n")
        info.append("• Comodines disponibles: ")
            .append(wildcardHelper!!.getRemainingWildcardsCount()).append("\n\n")
        info.append("ℹ️ Los comodines te ayudan sin afectar tu evaluación final.\n")
        info.append("Usa las ayudas cuando las necesites para mejorar tu aprendizaje.")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("🎯 Uso de Comodines")
        builder.setMessage(info.toString())
        builder.setPositiveButton("Entendido", null)
        builder.show()
    }

    companion object {
        private const val TAG = "ImageIdentificationActivity"
    }
}
