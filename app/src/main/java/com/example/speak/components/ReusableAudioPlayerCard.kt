package com.example.speak.components

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.speak.AudioPlayerView
import com.example.speak.AudioPlayerView.OnProgressChangeListener
import com.example.speak.R
import com.example.speak.helpers.ReusableAudioHelper
import com.google.android.material.card.MaterialCardView
import kotlin.math.max
import kotlin.math.min

class ReusableAudioPlayerCard : MaterialCardView {
    // Views
    private var playButton: ImageButton? = null
    private var audioPlayerView: AudioPlayerView? = null
    private var speedIndicator: TextView? = null
    private var configButton: ImageButton? = null
    private var voiceTypeCard: LinearLayout? = null
    private var languageSpanishButton: Button? = null
    private var languageEnglishButton: Button? = null
    private var voiceChildButton: Button? = null
    private var voiceGirlButton: Button? = null
    private var voiceWomanButton: Button? = null
    private var voiceManButton: Button? = null

    // Audio helper
    private var audioHelper: ReusableAudioHelper? = null

    // TextToSpeech instance from Activity
    private var textToSpeech: TextToSpeech? = null

    // Configuration
    private var assetsFolder: String? = null
    private var currentText: String? = null
    var isSpanishMode: Boolean = true
        private set
    var currentVoiceType: String = "default"
        private set
    var currentSpeed: Float = 1.0f
        private set
    private var isConfigVisible = false

    // Public getters for state
    // State
    var isPlaying: Boolean = false
        private set
    var isPaused: Boolean = false
        private set
    private var totalDuration = 0
    private var currentPosition = 0
    private var startTime: Long = 0
    private var pauseTime: Long = 0

    // Handler for time updates
    private var timeUpdateHandler: Handler? = null
    private var timeUpdateRunnable: Runnable? = null

    // Playback listener (opcional) para notificar a actividades externas
    interface PlaybackListener {
        fun onPlayStarted()
        fun onPaused()
        fun onResumed()
        fun onStopped()
    }

    private var playbackListener: PlaybackListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        try {
            LayoutInflater.from(context).inflate(R.layout.audio_player_card, this, true)

            // Configure MaterialCardView properties
            setRadius(12f)
            setCardBackgroundColor(context.getResources().getColor(android.R.color.transparent))
            setStrokeWidth(0)
            setStrokeColor(Color.TRANSPARENT)
            setLayoutParams(
                ViewGroup.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )

            // Initialize views
            playButton = findViewById<ImageButton?>(R.id.playButton)
            // Temporarily disable AudioPlayerView to avoid crashes
            audioPlayerView = findViewById<AudioPlayerView?>(R.id.audioPlayerView)
            //audioPlayerView = null; // Disable for now
            speedIndicator = findViewById<TextView?>(R.id.speedIndicator)
            configButton = findViewById<ImageButton?>(R.id.configButton)
            voiceTypeCard = findViewById<LinearLayout?>(R.id.voiceTypeCard)
            languageSpanishButton = findViewById<Button?>(R.id.languageSpanishButton)
            languageEnglishButton = findViewById<Button?>(R.id.languageEnglishButton)
            voiceChildButton = findViewById<Button?>(R.id.voiceChildButton)
            voiceGirlButton = findViewById<Button?>(R.id.voiceGirlButton)
            voiceWomanButton = findViewById<Button?>(R.id.voiceWomanButton)
            voiceManButton = findViewById<Button?>(R.id.voiceManButton)


            // Initialize audio helper
            audioHelper = ReusableAudioHelper(context)

            // Initialize handler for time updates
            timeUpdateHandler = Handler(Looper.getMainLooper())

            setupClickListeners()
            setupInitialState()

            // Asegurar que el componente sea visible
            setVisibility(VISIBLE)

            Log.d(TAG, "ReusableAudioPlayerCard initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ReusableAudioPlayerCard: " + e.message, e)
            // Si hay error, mostrar el componente con un mensaje de error
            setVisibility(VISIBLE)
        }
    }

    private fun setupClickListeners() {
        try {
            // Play/Pause button
            if (playButton != null) {
                playButton!!.setOnClickListener(OnClickListener { v: View? -> togglePlayPause() })
            }

            // Config button
            if (configButton != null) {
                configButton!!.setOnClickListener(OnClickListener { v: View? -> toggleConfigPanel() })
            }

            // Language buttons
            if (languageSpanishButton != null) {
                languageSpanishButton!!.setOnClickListener(OnClickListener { v: View? ->
                    isSpanishMode = true
                    updateLanguageButtons()
                    Log.d(TAG, "Switched to Spanish mode")
                })
            }

            if (languageEnglishButton != null) {
                languageEnglishButton!!.setOnClickListener(OnClickListener { v: View? ->
                    isSpanishMode = false
                    updateLanguageButtons()
                    Log.d(TAG, "Switched to English mode")
                })
            }

            // Voice type buttons
            if (voiceChildButton != null) {
                voiceChildButton!!.setOnClickListener(OnClickListener { v: View? ->
                    selectVoiceType(
                        "child"
                    )
                })
            }
            if (voiceGirlButton != null) {
                voiceGirlButton!!.setOnClickListener(OnClickListener { v: View? -> selectVoiceType("little_girl") })
            }
            if (voiceWomanButton != null) {
                voiceWomanButton!!.setOnClickListener(OnClickListener { v: View? ->
                    selectVoiceType(
                        "woman"
                    )
                })
            }
            if (voiceManButton != null) {
                voiceManButton!!.setOnClickListener(OnClickListener { v: View? -> selectVoiceType("man") })
            }

            // Speed indicator
            if (speedIndicator != null) {
                speedIndicator!!.setOnClickListener(OnClickListener { v: View? -> cycleSpeed() })
            }

            // Temporarily disable AudioPlayerView listener
            // AudioPlayerView listener
            // if (audioPlayerView != null) {
            //     audioPlayerView.setOnProgressChangeListener(new AudioPlayerView.OnProgressChangeListener() {
            //         @Override
            //         public void onProgressChanged(float progress) {
            //             updateTimeDisplay((int) progress, totalDuration);
            //         }
            //
            //         @Override
            //         public void onPlayPause(boolean isPlaying) {
            //             // Handle play/pause from AudioPlayerView if needed
            //         }
            //
            //         @Override
            //         public void onSeek(float position) {
            //             seekToPosition((int) position);
            //         }
            //     });
            // }
            if (audioPlayerView != null) {
                audioPlayerView!!.setOnProgressChangeListener(object : OnProgressChangeListener {
                    override fun onProgressChanged(progressMs: Float) {
                        // progressMs en milisegundos
                    }

                    override fun onPlayPause(isPlayingFromView: Boolean) {
                        // Opcional: sincronizar con tu botón si quieres
                    }

                    override fun onSeek(positionMs: Float) {
                        // positionMs en milisegundos
                        seekToPosition(positionMs.toInt())
                    }
                })
            }

            Log.d(TAG, "Click listeners setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: " + e.message, e)
        }
    }

    private fun setupInitialState() {
        try {
            // Set default voice type
            currentVoiceType = "child"
            updateLanguageButtons()
            updateVoiceButtons()
            if (speedIndicator != null) {
                speedIndicator!!.setText("x1")
            }
            Log.d(TAG, "Initial state setup successfully with default voice: " + currentVoiceType)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up initial state: " + e.message, e)
        }
    }

    // Public configuration methods
    fun configure(assetsFolder: String?, text: String?) {
        try {
            Log.d(TAG, "Starting configure method")
            this.assetsFolder = assetsFolder
            this.currentText = text

            if (audioHelper != null) {
                audioHelper!!.configure(assetsFolder)
                Log.d(TAG, "AudioHelper configured successfully")
            } else {
                Log.e(TAG, "AudioHelper is null during configure")
            }

            // Asegurar que el componente sea visible después de configurar
            setVisibility(VISIBLE)
            Log.d(TAG, "Configured for folder: " + assetsFolder + ", text: " + text)
        } catch (e: Exception) {
            Log.e(TAG, "Error in configure method: " + e.message, e)
            setVisibility(VISIBLE) // Mostrar el componente aunque haya error
        }
    }

    fun setText(text: String?) {
        this.currentText = text

        // Update AudioPlayerView with the new text
        if (audioPlayerView != null && text != null && !text.isEmpty()) {
            audioPlayerView!!.setTextForTTS(text)
            Log.d(TAG, "AudioPlayerView updated with text: " + text)
        }
    }

    fun setAssetsFolder(folder: String?) {
        this.assetsFolder = folder
        audioHelper!!.configure(folder)
    }

    /**
     * Establece la instancia de TextToSpeech desde la Activity
     * Esto permite al AudioPlayerView calcular la duración del audio
     */
    fun setTextToSpeech(tts: TextToSpeech?) {
        this.textToSpeech = tts

        // Pass TTS to AudioPlayerView
        if (audioPlayerView != null && tts != null) {
            audioPlayerView!!.setTextToSpeech(tts)
            Log.d(TAG, "TextToSpeech set in AudioPlayerView")
        }

        // Pass TTS to AudioHelper so it uses the same instance
        if (audioHelper != null && tts != null) {
            audioHelper!!.setExternalTextToSpeech(tts)
            Log.d(TAG, "TextToSpeech set in AudioHelper")
        }
    }

    // Audio control methods
    private fun togglePlayPause() {
        try {
            Log.d(
                TAG,
                "togglePlayPause called - isPlaying: " + isPlaying + ", isPaused: " + isPaused
            )
            if (isPlaying && !isPaused) {
                pauseAudio()
            } else if (isPlaying && isPaused) {
                resumeAudio()
            } else {
                playAudio()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause: " + e.message, e)
            Toast.makeText(
                getContext(),
                "Error en control de audio: " + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun playAudio() {
        try {
            Log.d(
                TAG,
                "playAudio called - isSpanishMode: " + isSpanishMode + ", currentVoiceType: " + currentVoiceType
            )
            if (audioHelper == null) {
                Log.e(TAG, "AudioHelper is null")
                return
            }

            if (isSpanishMode) {
                // Play original Spanish MP3 file
                val audioFile = this.audioFileName
                Log.d(
                    TAG,
                    "Playing Spanish audio file: " + audioFile + " from folder: " + assetsFolder
                )

                if (audioFile == null || audioFile.isEmpty()) {
                    Log.e(TAG, "Audio file name is null or empty")
                    Toast.makeText(
                        getContext(),
                        "Error: No se encontró archivo de audio",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                audioHelper!!.playAudio(audioFile)
                if (playButton != null) {
                    playButton!!.setImageResource(R.drawable.pause)
                }

                // Get duration and setup progress
                totalDuration = audioHelper!!.getTotalDuration()
                Log.d(TAG, "Audio duration: " + totalDuration + "ms")

                // Temporarily disable AudioPlayerView
                if (totalDuration > 0 && audioPlayerView != null) {
                    audioPlayerView!!.setDuration(totalDuration.toFloat())
                    audioPlayerView!!.setPlaying(true)
                }

                startTimeUpdate()
            } else {
                // Use TTS for English with voice types
                if (currentText != null && !currentText!!.isEmpty()) {
                    Log.d(TAG, "Speaking English text with voice: " + currentVoiceType)
                    audioHelper!!.speakTextWithVoice(currentText, currentVoiceType)
                    if (playButton != null) {
                        playButton!!.setImageResource(R.drawable.pause)
                    }

                    // Get duration from AudioPlayerView (already calculated there)
                    var estimatedDuration = estimateTextDuration(currentText!!)
                    if (audioPlayerView != null) {
                        // AudioPlayerView ya calculó la duración al llamar setTextForTTS
                        audioPlayerView!!.setPlaying(true)
                        // Obtener la duración que ya fue calculada
                        estimatedDuration = audioPlayerView!!.getDuration().toInt()
                        Log.d(
                            TAG,
                            "Using duration from AudioPlayerView: " + estimatedDuration + "ms"
                        )
                    }

                    totalDuration = estimatedDuration
                    startTimeUpdate()
                }
            }

            isPlaying = true
            isPaused = false
            startTime = System.currentTimeMillis()
            Log.d(TAG, "Audio playback started successfully")

            if (playbackListener != null) {
                try {
                    playbackListener!!.onPlayStarted()
                } catch (ignore: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in playAudio: " + e.message, e)
            Toast.makeText(
                getContext(),
                "Error reproduciendo audio: " + e.message,
                Toast.LENGTH_SHORT
            ).show()

            // Reset button state on error
            if (playButton != null) {
                playButton!!.setImageResource(R.drawable.reproduce)
            }
            isPlaying = false
            isPaused = false
        }
    }

    private fun pauseAudio() {
        Log.d(TAG, "pauseAudio called - isPlaying: " + isPlaying + ", isPaused: " + isPaused)
        if (audioHelper != null && isPlaying && !isPaused) {
            // Guardar posición actual antes de pausar
            if (isSpanishMode) {
                currentPosition = audioHelper!!.getCurrentPosition()
            } else {
                currentPosition = (System.currentTimeMillis() - startTime).toInt()
            }

            audioHelper!!.pauseAudio()
            if (playButton != null) {
                playButton!!.setImageResource(R.drawable.reproduce)
            }

            // Temporarily disable AudioPlayerView
            if (audioPlayerView != null) {
                audioPlayerView!!.setPlaying(false)
            }

            isPaused = true
            pauseTime = System.currentTimeMillis()
            stopTimeUpdate()
            Log.d(TAG, "Audio paused successfully")
            if (playbackListener != null) {
                try {
                    playbackListener!!.onPaused()
                } catch (ignore: Exception) {
                }
            }
        } else {
            Log.d(
                TAG,
                "Cannot pause - audioHelper: " + (audioHelper != null) + ", isPlaying: " + isPlaying + ", isPaused: " + isPaused
            )
        }
    }

    private fun resumeAudio() {
        Log.d(TAG, "resumeAudio called - isPlaying: " + isPlaying + ", isPaused: " + isPaused)
        if (audioHelper != null && isPlaying && isPaused) {
            if (isSpanishMode) {
                // Reanudar MediaPlayer desde la posición pausada
                audioHelper!!.resumeAudio()
            } else {
                // TTS no soporta resume; reiniciar la locución y ajustar el progreso sintético
                if (currentText != null && !currentText!!.isEmpty()) {
                    audioHelper!!.speakTextWithVoice(currentText, currentVoiceType)
                }
                // Ajustar startTime para que el progreso sintético continúe desde currentPosition
                startTime = System.currentTimeMillis() - currentPosition
            }
            if (playButton != null) {
                playButton!!.setImageResource(R.drawable.pause)
            }
            if (audioPlayerView != null) {
                audioPlayerView!!.setPlaying(true)
            }
            isPaused = false
            startTimeUpdate()
            Log.d(TAG, "Audio resumed successfully")
            if (playbackListener != null) {
                try {
                    playbackListener!!.onResumed()
                } catch (ignore: Exception) {
                }
            }
        } else {
            Log.d(
                TAG,
                "Cannot resume - audioHelper: " + (audioHelper != null) + ", isPlaying: " + isPlaying + ", isPaused: " + isPaused
            )
        }
    }

    private fun stopAudio() {
        if (audioHelper != null) {
            audioHelper!!.stopAudio()
            playButton!!.setImageResource(R.drawable.reproduce)

            // Temporarily disable AudioPlayerView
            if (audioPlayerView != null) {
                audioPlayerView!!.setPlaying(false)
            }

            isPlaying = false
            isPaused = false
            currentPosition = 0
            stopTimeUpdate()
            if (playbackListener != null) {
                try {
                    playbackListener!!.onStopped()
                } catch (ignore: Exception) {
                }
            }
        }
    }

    private fun seekToPosition(position: Int) {
        if (audioHelper != null) {
            audioHelper!!.seekTo(position)
        }
    }

    // Voice selection methods
    private fun selectVoiceType(voiceType: String) {
        Log.d(TAG, "selectVoiceType called with: " + voiceType)
        this.currentVoiceType = voiceType
        updateVoiceButtons()
        Log.d(TAG, "Selected voice type: " + voiceType + ", isSpanishMode: " + isSpanishMode)

        // If currently playing, restart with new voice
        if (isPlaying) {
            Log.d(TAG, "Restarting audio with new voice type")
            stopAudio()
            playAudio()
        }
    }

    private fun updateVoiceButtons() {
        try {
            // Reset all buttons
            if (voiceChildButton != null) resetVoiceButton(voiceChildButton!!)
            if (voiceGirlButton != null) resetVoiceButton(voiceGirlButton!!)
            if (voiceWomanButton != null) resetVoiceButton(voiceWomanButton!!)
            if (voiceManButton != null) resetVoiceButton(voiceManButton!!)

            // Highlight selected button
            var selectedButton: Button? = null
            when (currentVoiceType) {
                "child" -> selectedButton = voiceChildButton
                "little_girl" -> selectedButton = voiceGirlButton
                "woman" -> selectedButton = voiceWomanButton
                "man" -> selectedButton = voiceManButton
            }

            if (selectedButton != null) {
                // Efecto de selección como en HelpActivity: agrandar y sin fondo
                selectedButton.setBackgroundResource(android.R.color.transparent)
                selectedButton.setScaleX(1.2f)
                selectedButton.setScaleY(1.2f)
                // Color de texto para seleccionado (usa @color/selected_voice si existe)
                try {
                    selectedButton.setTextColor(getResources().getColor(R.color.selected_voice))
                } catch (ignore: Exception) {
                    // fallback sin romper si no existe el color
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating voice buttons: " + e.message, e)
        }
    }

    private fun resetVoiceButton(button: Button) {
        button.setBackgroundResource(android.R.color.transparent)
        button.setScaleX(1.0f)
        button.setScaleY(1.0f)
        // Restaurar color de texto a un color por defecto legible sobre el fondo actual
        try {
            button.setTextColor(getResources().getColor(R.color.help_audio_player_background))
        } catch (ignore: Exception) {
            // fallback
        }
    }

    private fun updateLanguageButtons() {
        try {
            if (languageSpanishButton != null && languageEnglishButton != null) {
                if (isSpanishMode) {
                    // Botón "Original" (español) seleccionado
                    languageSpanishButton!!.setScaleX(1.15f)
                    languageSpanishButton!!.setScaleY(1.15f)
                    languageSpanishButton!!.setTextColor(getResources().getColor(R.color.naranjaSena))
                    languageSpanishButton!!.setBackgroundResource(R.drawable.rounded_background)
                    languageSpanishButton!!.setElevation(8f)

                    // Botón "English" no seleccionado
                    languageEnglishButton!!.setScaleX(1.0f)
                    languageEnglishButton!!.setScaleY(1.0f)
                    languageEnglishButton!!.setTextColor(getResources().getColor(android.R.color.darker_gray))
                    languageEnglishButton!!.setBackgroundResource(android.R.color.transparent)
                    languageEnglishButton!!.setElevation(0f)
                } else {
                    // Botón "English" seleccionado
                    languageEnglishButton!!.setScaleX(1.15f)
                    languageEnglishButton!!.setScaleY(1.15f)
                    languageEnglishButton!!.setTextColor(getResources().getColor(R.color.naranjaSena))
                    languageEnglishButton!!.setBackgroundResource(R.drawable.rounded_background)
                    languageEnglishButton!!.setElevation(8f)

                    // Botón "Original" no seleccionado
                    languageSpanishButton!!.setScaleX(1.0f)
                    languageSpanishButton!!.setScaleY(1.0f)
                    languageSpanishButton!!.setTextColor(getResources().getColor(android.R.color.darker_gray))
                    languageSpanishButton!!.setBackgroundResource(android.R.color.transparent)
                    languageSpanishButton!!.setElevation(0f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating language buttons: " + e.message, e)
        }
    }

    private fun cycleSpeed() {
        if (speedIndicator == null) {
            Log.e(TAG, "speedIndicator is null")
            return
        }

        val currentSpeedText = speedIndicator!!.getText().toString()
        val newSpeed: String?
        val speedValue: Float

        when (currentSpeedText) {
            "x1" -> {
                newSpeed = "x1.5"
                speedValue = 1.5f
            }

            "x1.5" -> {
                newSpeed = "x2"
                speedValue = 2.0f
            }

            "x2" -> {
                newSpeed = "x1"
                speedValue = 1.0f
            }

            else -> {
                newSpeed = "x1"
                speedValue = 1.0f
            }
        }

        speedIndicator!!.setText(newSpeed)
        currentSpeed = speedValue

        if (audioHelper != null) {
            audioHelper!!.setPlaybackSpeed(speedValue)
            Log.d(TAG, "Speed changed to: " + newSpeed + " (" + speedValue + ")")
        } else {
            Log.e(TAG, "audioHelper is null, cannot set speed")
        }
    }

    private fun toggleConfigPanel() {
        try {
            Log.d(TAG, "toggleConfigPanel called - current visibility: " + isConfigVisible)
            isConfigVisible = !isConfigVisible
            if (voiceTypeCard != null) {
                voiceTypeCard!!.setVisibility(if (isConfigVisible) VISIBLE else GONE)
                Log.d(
                    TAG,
                    "Config panel visibility set to: " + (if (isConfigVisible) "VISIBLE" else "GONE")
                )
            } else {
                Log.e(TAG, "voiceTypeCard is null!")
            }
            Log.d(TAG, "Config panel toggled: " + isConfigVisible)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling config panel: " + e.message, e)
        }
    }

    private val audioFileName: String?
        // Helper methods
        get() {
            if (assetsFolder == null) {
                Log.e(TAG, "assetsFolder is null!")
                return null
            }

            // Return the appropriate audio file based on voice type and folder
            val fileName = this.voiceFileName
            Log.d(
                TAG,
                "getAudioFileName returning: " + fileName
            )
            return fileName
        }

    private val voiceFileName: String?
        get() {
            if (isSpanishMode) {
                // For Spanish mode, use the selected voice type audio file
                val fileName = currentVoiceType + ".mp3"
                Log.d(
                    TAG,
                    "getVoiceFileName - isSpanishMode: true, currentVoiceType: " + currentVoiceType + ", fileName: " + fileName
                )
                return fileName
            } else {
                // For English mode, use TTS (no file needed)
                Log.d(
                    TAG,
                    "getVoiceFileName - isSpanishMode: false, using TTS"
                )
                return null
            }
        }

    private fun estimateTextDuration(text: String): Int {
        // Simple estimation: ~150 words per minute, ~5 characters per word
        val words = text.length / 5
        val durationMs = (words * 60 * 1000) / 150
        return max(durationMs, 3000) // Minimum 3 seconds
    }


    private fun formatTime(milliseconds: Int): String {
        var seconds = milliseconds / 1000
        val minutes = seconds / 60
        seconds = seconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun startTimeUpdate() {
        try {
            Log.d(
                TAG,
                "Starting time update - totalDuration: " + totalDuration + "ms, isSpanishMode: " + isSpanishMode
            )
            if (timeUpdateHandler != null) {
                timeUpdateRunnable = object : Runnable {
                    override fun run() {
                        try {
                            // Para TTS, usar isPlaying (estado interno) en lugar de audioHelper.isPlaying()

                            var shouldContinue = isPlaying && !isPaused

                            if (!isSpanishMode) {
                                // Para TTS: usar progreso sintético basado en tiempo transcurrido
                                shouldContinue = isPlaying && !isPaused
                            } else {
                                // Para MediaPlayer: verificar si realmente está reproduciendo
                                shouldContinue = audioHelper != null && audioHelper!!.isPlaying()
                            }

                            if (shouldContinue) {
                                val currentPos: Int

                                if (!isSpanishMode) {
                                    // Progreso sintético para TTS (basado en tiempo transcurrido)
                                    val elapsed = (System.currentTimeMillis() - startTime).toInt()
                                    currentPos = min(elapsed, totalDuration)
                                    Log.d(
                                        TAG,
                                        "TTS Progress: " + currentPos + "/" + totalDuration + "ms"
                                    )
                                } else {
                                    // Progreso real para MediaPlayer
                                    currentPos = audioHelper!!.getCurrentPosition()
                                }

                                if (audioPlayerView != null && totalDuration > 0) {
                                    audioPlayerView!!.setProgress(currentPos.toFloat())
                                    audioPlayerView!!.setPlaying(true)
                                }

                                // Si alcanzó o superó la duración total, finalizar visualización
                                if (currentPos >= totalDuration) {
                                    Log.d(TAG, "Audio completed - stopping time update")
                                    isPlaying = false
                                    isPaused = false
                                    if (audioPlayerView != null) {
                                        audioPlayerView!!.setPlaying(false)
                                        audioPlayerView!!.setProgress(totalDuration.toFloat())
                                    }
                                    if (playButton != null) {
                                        playButton!!.setImageResource(R.drawable.reproduce)
                                    }
                                    stopTimeUpdate()
                                } else {
                                    // Continuar actualizando cada 100ms
                                    timeUpdateHandler!!.postDelayed(this, 100)
                                }
                            } else {
                                Log.d(TAG, "Stopping time update - not playing or paused")
                                if (audioPlayerView != null) {
                                    audioPlayerView!!.setPlaying(false)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in time update runnable: " + e.message, e)
                        }
                    }
                }
                timeUpdateHandler!!.post(timeUpdateRunnable!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting time update: " + e.message, e)
        }
    }

    private fun stopTimeUpdate() {
        Log.d(TAG, "Stopping time update")
        if (timeUpdateHandler != null && timeUpdateRunnable != null) {
            timeUpdateHandler!!.removeCallbacks(timeUpdateRunnable!!)
        }
    }

    // Force English (TTS) mode programmatically
    fun setEnglishMode() {
        try {
            isSpanishMode = false
            updateLanguageButtons()
            Log.d(TAG, "Forced English (TTS) mode")
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing English mode: " + e.message, e)
        }
    }

    // Cleanup method
    fun cleanup() {
        stopTimeUpdate()
        if (audioHelper != null) {
            audioHelper!!.cleanup()
        }
        stopAudio()
        Log.d(TAG, "ReusableAudioPlayerCard cleaned up")
    }

    fun setPlaybackListener(listener: PlaybackListener?) {
        this.playbackListener = listener
    }

    // Reset state for a new question/text without destroying TTS resources
    fun resetForNewQuestion() {
        try {
            Log.d(TAG, "resetForNewQuestion called")
            stopTimeUpdate()
            if (isPlaying || isPaused) {
                stopAudio()
            }
            isPlaying = false
            isPaused = false
            currentPosition = 0
            totalDuration = 0

            if (audioPlayerView != null) {
                audioPlayerView!!.setPlaying(false)
                audioPlayerView!!.setProgress(0f)
                // Si hay texto actual, actualizar la duración para la nueva pregunta
                if (currentText != null && !currentText!!.isEmpty()) {
                    audioPlayerView!!.setTextForTTS(currentText)
                    Log.d(TAG, "AudioPlayerView duration updated for new question")
                }
            }
            if (playButton != null) {
                playButton!!.setImageResource(R.drawable.reproduce)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting for new question: " + e.message, e)
        }
    }

    override fun onDetachedFromWindow() {
        try {
            Log.d(TAG, "onDetachedFromWindow - stopping and cleaning up audio")
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during onDetachedFromWindow cleanup: " + e.message, e)
        }
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        try {
            if (visibility != VISIBLE) {
                Log.d(TAG, "View not visible - stopping audio")
                stopAudio()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling visibility change: " + e.message, e)
        }
    }

    companion object {
        private const val TAG = "ReusableAudioPlayerCard"
    }
}