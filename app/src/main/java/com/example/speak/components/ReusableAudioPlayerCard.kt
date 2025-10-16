package com.example.speak.components;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.speak.AudioPlayerView;
import com.example.speak.R;
import com.example.speak.helpers.ReusableAudioHelper;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.Locale;

public class ReusableAudioPlayerCard extends MaterialCardView {

    private static final String TAG = "ReusableAudioPlayerCard";

    // Views
    private ImageButton playButton;
    private AudioPlayerView audioPlayerView;
    private TextView speedIndicator;
    private ImageButton configButton;
    private LinearLayout voiceTypeCard;
    private Button languageSpanishButton;
    private Button languageEnglishButton;
    private Button voiceChildButton;
    private Button voiceGirlButton;
    private Button voiceWomanButton;
    private Button voiceManButton;

    // Audio helper
    private ReusableAudioHelper audioHelper;

    // TextToSpeech instance from Activity
    private TextToSpeech textToSpeech;

    // Configuration
    private String assetsFolder;
    private String currentText;
    private boolean isSpanishMode = true;
    private String currentVoiceType = "default";
    private float currentSpeed = 1.0f;
    private boolean isConfigVisible = false;

    // State
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private int totalDuration = 0;
    private int currentPosition = 0;
    private long startTime = 0;
    private long pauseTime = 0;

    // Handler for time updates
    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;

    // Playback listener (opcional) para notificar a actividades externas
    public interface PlaybackListener {
        void onPlayStarted();
        void onPaused();
        void onResumed();
        void onStopped();
    }
    private PlaybackListener playbackListener;

    public ReusableAudioPlayerCard(Context context) {
        super(context);
        init(context);
    }

    public ReusableAudioPlayerCard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReusableAudioPlayerCard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            LayoutInflater.from(context).inflate(R.layout.audio_player_card, this, true);

            // Configure MaterialCardView properties
            setRadius(12);
            setCardBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            setStrokeWidth(0);
            setStrokeColor(Color.TRANSPARENT);
            setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Initialize views
            playButton = findViewById(R.id.playButton);
            // Temporarily disable AudioPlayerView to avoid crashes
            audioPlayerView = findViewById(R.id.audioPlayerView);
            //audioPlayerView = null; // Disable for now
            speedIndicator = findViewById(R.id.speedIndicator);
            configButton = findViewById(R.id.configButton);
            voiceTypeCard = findViewById(R.id.voiceTypeCard);
            languageSpanishButton = findViewById(R.id.languageSpanishButton);
            languageEnglishButton = findViewById(R.id.languageEnglishButton);
            voiceChildButton = findViewById(R.id.voiceChildButton);
            voiceGirlButton = findViewById(R.id.voiceGirlButton);
            voiceWomanButton = findViewById(R.id.voiceWomanButton);
            voiceManButton = findViewById(R.id.voiceManButton);



            // Initialize audio helper
            audioHelper = new ReusableAudioHelper(context);

            // Initialize handler for time updates
            timeUpdateHandler = new Handler(Looper.getMainLooper());

            setupClickListeners();
            setupInitialState();

            // Asegurar que el componente sea visible
            setVisibility(View.VISIBLE);

            Log.d(TAG, "ReusableAudioPlayerCard initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ReusableAudioPlayerCard: " + e.getMessage(), e);
            // Si hay error, mostrar el componente con un mensaje de error
            setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        try {
            // Play/Pause button
            if (playButton != null) {
                playButton.setOnClickListener(v -> togglePlayPause());
            }

            // Config button
            if (configButton != null) {
                configButton.setOnClickListener(v -> toggleConfigPanel());
            }

            // Language buttons
            if (languageSpanishButton != null) {
                languageSpanishButton.setOnClickListener(v -> {
                    isSpanishMode = true;
                    updateLanguageButtons();
                    Log.d(TAG, "Switched to Spanish mode");
                });
            }

            if (languageEnglishButton != null) {
                languageEnglishButton.setOnClickListener(v -> {
                    isSpanishMode = false;
                    updateLanguageButtons();
                    Log.d(TAG, "Switched to English mode");
                });
            }

            // Voice type buttons
            if (voiceChildButton != null) {
                voiceChildButton.setOnClickListener(v -> selectVoiceType("child"));
            }
            if (voiceGirlButton != null) {
                voiceGirlButton.setOnClickListener(v -> selectVoiceType("little_girl"));
            }
            if (voiceWomanButton != null) {
                voiceWomanButton.setOnClickListener(v -> selectVoiceType("woman"));
            }
            if (voiceManButton != null) {
                voiceManButton.setOnClickListener(v -> selectVoiceType("man"));
            }

            // Speed indicator
            if (speedIndicator != null) {
                speedIndicator.setOnClickListener(v -> cycleSpeed());
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
                audioPlayerView.setOnProgressChangeListener(new AudioPlayerView.OnProgressChangeListener() {
                    @Override
                    public void onProgressChanged(float progressMs) {
                        // progressMs en milisegundos
                    }

                    @Override
                    public void onPlayPause(boolean isPlayingFromView) {
                        // Opcional: sincronizar con tu botón si quieres
                    }

                    @Override
                    public void onSeek(float positionMs) {
                        // positionMs en milisegundos
                        seekToPosition((int) positionMs);
                    }
                });
            }

            Log.d(TAG, "Click listeners setup successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void setupInitialState() {
        try {
            // Set default voice type
            currentVoiceType = "child";
            updateLanguageButtons();
            updateVoiceButtons();
            if (speedIndicator != null) {
                speedIndicator.setText("x1");
            }
            Log.d(TAG, "Initial state setup successfully with default voice: " + currentVoiceType);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up initial state: " + e.getMessage(), e);
        }
    }

    // Public configuration methods
    public void configure(String assetsFolder, String text) {
        try {
            Log.d(TAG, "Starting configure method");
            this.assetsFolder = assetsFolder;
            this.currentText = text;

            if (audioHelper != null) {
                audioHelper.configure(assetsFolder);
                Log.d(TAG, "AudioHelper configured successfully");
            } else {
                Log.e(TAG, "AudioHelper is null during configure");
            }

            // Asegurar que el componente sea visible después de configurar
            setVisibility(View.VISIBLE);
            Log.d(TAG, "Configured for folder: " + assetsFolder + ", text: " + text);
        } catch (Exception e) {
            Log.e(TAG, "Error in configure method: " + e.getMessage(), e);
            setVisibility(View.VISIBLE); // Mostrar el componente aunque haya error
        }
    }

    public void setText(String text) {
        this.currentText = text;

        // Update AudioPlayerView with the new text
        if (audioPlayerView != null && text != null && !text.isEmpty()) {
            audioPlayerView.setTextForTTS(text);
            Log.d(TAG, "AudioPlayerView updated with text: " + text);
        }
    }

    public void setAssetsFolder(String folder) {
        this.assetsFolder = folder;
        audioHelper.configure(folder);
    }

    /**
     * Establece la instancia de TextToSpeech desde la Activity
     * Esto permite al AudioPlayerView calcular la duración del audio
     */
    public void setTextToSpeech(TextToSpeech tts) {
        this.textToSpeech = tts;

        // Pass TTS to AudioPlayerView
        if (audioPlayerView != null && tts != null) {
            audioPlayerView.setTextToSpeech(tts);
            Log.d(TAG, "TextToSpeech set in AudioPlayerView");
        }

        // Pass TTS to AudioHelper so it uses the same instance
        if (audioHelper != null && tts != null) {
            audioHelper.setExternalTextToSpeech(tts);
            Log.d(TAG, "TextToSpeech set in AudioHelper");
        }
    }

    // Audio control methods
    private void togglePlayPause() {
        try {
            Log.d(TAG, "togglePlayPause called - isPlaying: " + isPlaying + ", isPaused: " + isPaused);
            if (isPlaying && !isPaused) {
                pauseAudio();
            } else if (isPlaying && isPaused) {
                resumeAudio();
            } else {
                playAudio();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in togglePlayPause: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error en control de audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudio() {
        try {
            Log.d(TAG, "playAudio called - isSpanishMode: " + isSpanishMode + ", currentVoiceType: " + currentVoiceType);
            if (audioHelper == null) {
                Log.e(TAG, "AudioHelper is null");
                return;
            }

            if (isSpanishMode) {
                // Play original Spanish MP3 file
                String audioFile = getAudioFileName();
                Log.d(TAG, "Playing Spanish audio file: " + audioFile + " from folder: " + assetsFolder);

                if (audioFile == null || audioFile.isEmpty()) {
                    Log.e(TAG, "Audio file name is null or empty");
                    Toast.makeText(getContext(), "Error: No se encontró archivo de audio", Toast.LENGTH_SHORT).show();
                    return;
                }

                audioHelper.playAudio(audioFile);
                if (playButton != null) {
                    playButton.setImageResource(R.drawable.pause);
                }

                // Get duration and setup progress
                totalDuration = audioHelper.getTotalDuration();
                Log.d(TAG, "Audio duration: " + totalDuration + "ms");

                // Temporarily disable AudioPlayerView
                if (totalDuration > 0 && audioPlayerView != null) {
                    audioPlayerView.setDuration(totalDuration);
                    audioPlayerView.setPlaying(true);
                }

                startTimeUpdate();
            } else {
                // Use TTS for English with voice types
                if (currentText != null && !currentText.isEmpty()) {
                    Log.d(TAG, "Speaking English text with voice: " + currentVoiceType);
                    audioHelper.speakTextWithVoice(currentText, currentVoiceType);
                    if (playButton != null) {
                        playButton.setImageResource(R.drawable.pause);
                    }

                    // Get duration from AudioPlayerView (already calculated there)
                    int estimatedDuration = estimateTextDuration(currentText);
                    if (audioPlayerView != null) {
                        // AudioPlayerView ya calculó la duración al llamar setTextForTTS
                        audioPlayerView.setPlaying(true);
                        // Obtener la duración que ya fue calculada
                        estimatedDuration = (int) audioPlayerView.getDuration();
                        Log.d(TAG, "Using duration from AudioPlayerView: " + estimatedDuration + "ms");
                    }

                    totalDuration = estimatedDuration;
                    startTimeUpdate();
                }
            }

            isPlaying = true;
            isPaused = false;
            startTime = System.currentTimeMillis();
            Log.d(TAG, "Audio playback started successfully");

            if (playbackListener != null) {
                try { playbackListener.onPlayStarted(); } catch (Exception ignore) {}
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in playAudio: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error reproduciendo audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Reset button state on error
            if (playButton != null) {
                playButton.setImageResource(R.drawable.reproduce);
            }
            isPlaying = false;
            isPaused = false;
        }
    }

    private void pauseAudio() {
        Log.d(TAG, "pauseAudio called - isPlaying: " + isPlaying + ", isPaused: " + isPaused);
        if (audioHelper != null && isPlaying && !isPaused) {
            // Guardar posición actual antes de pausar
            if (isSpanishMode) {
                currentPosition = audioHelper.getCurrentPosition();
            } else {
                currentPosition = (int) (System.currentTimeMillis() - startTime);
            }

            audioHelper.pauseAudio();
            if (playButton != null) {
                playButton.setImageResource(R.drawable.reproduce);
            }

            // Temporarily disable AudioPlayerView
            if (audioPlayerView != null) {
                audioPlayerView.setPlaying(false);
            }

            isPaused = true;
            pauseTime = System.currentTimeMillis();
            stopTimeUpdate();
            Log.d(TAG, "Audio paused successfully");
            if (playbackListener != null) {
                try { playbackListener.onPaused(); } catch (Exception ignore) {}
            }
        } else {
            Log.d(TAG, "Cannot pause - audioHelper: " + (audioHelper != null) + ", isPlaying: " + isPlaying + ", isPaused: " + isPaused);
        }
    }

    private void resumeAudio() {
        Log.d(TAG, "resumeAudio called - isPlaying: " + isPlaying + ", isPaused: " + isPaused);
        if (audioHelper != null && isPlaying && isPaused) {
            if (isSpanishMode) {
                // Reanudar MediaPlayer desde la posición pausada
                audioHelper.resumeAudio();
            } else {
                // TTS no soporta resume; reiniciar la locución y ajustar el progreso sintético
                if (currentText != null && !currentText.isEmpty()) {
                    audioHelper.speakTextWithVoice(currentText, currentVoiceType);
                }
                // Ajustar startTime para que el progreso sintético continúe desde currentPosition
                startTime = System.currentTimeMillis() - currentPosition;
            }
            if (playButton != null) {
                playButton.setImageResource(R.drawable.pause);
            }
            if (audioPlayerView != null) {
                audioPlayerView.setPlaying(true);
            }
            isPaused = false;
            startTimeUpdate();
            Log.d(TAG, "Audio resumed successfully");
            if (playbackListener != null) {
                try { playbackListener.onResumed(); } catch (Exception ignore) {}
            }
        } else {
            Log.d(TAG, "Cannot resume - audioHelper: " + (audioHelper != null) + ", isPlaying: " + isPlaying + ", isPaused: " + isPaused);
        }
    }

    private void stopAudio() {
        if (audioHelper != null) {
            audioHelper.stopAudio();
            playButton.setImageResource(R.drawable.reproduce);

            // Temporarily disable AudioPlayerView
            if (audioPlayerView != null) {
                audioPlayerView.setPlaying(false);
            }

            isPlaying = false;
            isPaused = false;
            currentPosition = 0;
            stopTimeUpdate();
            if (playbackListener != null) {
                try { playbackListener.onStopped(); } catch (Exception ignore) {}
            }
        }
    }

    private void seekToPosition(int position) {
        if (audioHelper != null) {
            audioHelper.seekTo(position);
        }
    }

    // Voice selection methods
    private void selectVoiceType(String voiceType) {
        Log.d(TAG, "selectVoiceType called with: " + voiceType);
        this.currentVoiceType = voiceType;
        updateVoiceButtons();
        Log.d(TAG, "Selected voice type: " + voiceType + ", isSpanishMode: " + isSpanishMode);

        // If currently playing, restart with new voice
        if (isPlaying) {
            Log.d(TAG, "Restarting audio with new voice type");
            stopAudio();
            playAudio();
        }
    }

    private void updateVoiceButtons() {
        try {
            // Reset all buttons
            if (voiceChildButton != null) resetVoiceButton(voiceChildButton);
            if (voiceGirlButton != null) resetVoiceButton(voiceGirlButton);
            if (voiceWomanButton != null) resetVoiceButton(voiceWomanButton);
            if (voiceManButton != null) resetVoiceButton(voiceManButton);

            // Highlight selected button
            Button selectedButton = null;
            switch (currentVoiceType) {
                case "child":
                    selectedButton = voiceChildButton;
                    break;
                case "little_girl":
                    selectedButton = voiceGirlButton;
                    break;
                case "woman":
                    selectedButton = voiceWomanButton;
                    break;
                case "man":
                    selectedButton = voiceManButton;
                    break;
            }

            if (selectedButton != null) {
                // Efecto de selección como en HelpActivity: agrandar y sin fondo
                selectedButton.setBackgroundResource(android.R.color.transparent);
                selectedButton.setScaleX(1.2f);
                selectedButton.setScaleY(1.2f);
                // Color de texto para seleccionado (usa @color/selected_voice si existe)
                try {
                    selectedButton.setTextColor(getResources().getColor(R.color.selected_voice));
                } catch (Exception ignore) {
                    // fallback sin romper si no existe el color
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating voice buttons: " + e.getMessage(), e);
        }
    }

    private void resetVoiceButton(Button button) {
        button.setBackgroundResource(android.R.color.transparent);
        button.setScaleX(1.0f);
        button.setScaleY(1.0f);
        // Restaurar color de texto a un color por defecto legible sobre el fondo actual
        try {
            button.setTextColor(getResources().getColor(R.color.help_audio_player_background));
        } catch (Exception ignore) {
            // fallback
        }
    }

    private void updateLanguageButtons() {
        try {
            if (languageSpanishButton != null && languageEnglishButton != null) {
                if (isSpanishMode) {
                    // Botón "Original" (español) seleccionado
                    languageSpanishButton.setScaleX(1.15f);
                    languageSpanishButton.setScaleY(1.15f);
                    languageSpanishButton.setTextColor(getResources().getColor(R.color.naranjaSena));
                    languageSpanishButton.setBackgroundResource(R.drawable.rounded_background);
                    languageSpanishButton.setElevation(8f);

                    // Botón "English" no seleccionado
                    languageEnglishButton.setScaleX(1.0f);
                    languageEnglishButton.setScaleY(1.0f);
                    languageEnglishButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    languageEnglishButton.setBackgroundResource(android.R.color.transparent);
                    languageEnglishButton.setElevation(0f);
                } else {
                    // Botón "English" seleccionado
                    languageEnglishButton.setScaleX(1.15f);
                    languageEnglishButton.setScaleY(1.15f);
                    languageEnglishButton.setTextColor(getResources().getColor(R.color.naranjaSena));
                    languageEnglishButton.setBackgroundResource(R.drawable.rounded_background);
                    languageEnglishButton.setElevation(8f);

                    // Botón "Original" no seleccionado
                    languageSpanishButton.setScaleX(1.0f);
                    languageSpanishButton.setScaleY(1.0f);
                    languageSpanishButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    languageSpanishButton.setBackgroundResource(android.R.color.transparent);
                    languageSpanishButton.setElevation(0f);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating language buttons: " + e.getMessage(), e);
        }
    }

    private void cycleSpeed() {
        if (speedIndicator == null) {
            Log.e(TAG, "speedIndicator is null");
            return;
        }

        String currentSpeedText = speedIndicator.getText().toString();
        String newSpeed;
        float speedValue;

        switch (currentSpeedText) {
            case "x1":
                newSpeed = "x1.5";
                speedValue = 1.5f;
                break;
            case "x1.5":
                newSpeed = "x2";
                speedValue = 2.0f;
                break;
            case "x2":
                newSpeed = "x1";
                speedValue = 1.0f;
                break;
            default:
                newSpeed = "x1";
                speedValue = 1.0f;
        }

        speedIndicator.setText(newSpeed);
        currentSpeed = speedValue;

        if (audioHelper != null) {
            audioHelper.setPlaybackSpeed(speedValue);
            Log.d(TAG, "Speed changed to: " + newSpeed + " (" + speedValue + ")");
        } else {
            Log.e(TAG, "audioHelper is null, cannot set speed");
        }
    }

    private void toggleConfigPanel() {
        try {
            Log.d(TAG, "toggleConfigPanel called - current visibility: " + isConfigVisible);
            isConfigVisible = !isConfigVisible;
            if (voiceTypeCard != null) {
                voiceTypeCard.setVisibility(isConfigVisible ? View.VISIBLE : View.GONE);
                Log.d(TAG, "Config panel visibility set to: " + (isConfigVisible ? "VISIBLE" : "GONE"));
            } else {
                Log.e(TAG, "voiceTypeCard is null!");
            }
            Log.d(TAG, "Config panel toggled: " + isConfigVisible);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling config panel: " + e.getMessage(), e);
        }
    }

    // Helper methods
    private String getAudioFileName() {
        if (assetsFolder == null) {
            Log.e(TAG, "assetsFolder is null!");
            return null;
        }

        // Return the appropriate audio file based on voice type and folder
        String fileName = getVoiceFileName();
        Log.d(TAG, "getAudioFileName returning: " + fileName);
        return fileName;
    }

    private String getVoiceFileName() {
        if (isSpanishMode) {
            // For Spanish mode, use the selected voice type audio file
            String fileName = currentVoiceType + ".mp3";
            Log.d(TAG, "getVoiceFileName - isSpanishMode: true, currentVoiceType: " + currentVoiceType + ", fileName: " + fileName);
            return fileName;
        } else {
            // For English mode, use TTS (no file needed)
            Log.d(TAG, "getVoiceFileName - isSpanishMode: false, using TTS");
            return null;
        }
    }

    private int estimateTextDuration(String text) {
        // Simple estimation: ~150 words per minute, ~5 characters per word
        int words = text.length() / 5;
        int durationMs = (words * 60 * 1000) / 150;
        return Math.max(durationMs, 3000); // Minimum 3 seconds
    }


    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void startTimeUpdate() {
        try {
            Log.d(TAG, "Starting time update - totalDuration: " + totalDuration + "ms, isSpanishMode: " + isSpanishMode);
            if (timeUpdateHandler != null) {
                timeUpdateRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Para TTS, usar isPlaying (estado interno) en lugar de audioHelper.isPlaying()

                            boolean shouldContinue = isPlaying && !isPaused;

                            if (!isSpanishMode) {
                                // Para TTS: usar progreso sintético basado en tiempo transcurrido
                                shouldContinue = isPlaying && !isPaused;
                            } else {
                                // Para MediaPlayer: verificar si realmente está reproduciendo
                                shouldContinue = audioHelper != null && audioHelper.isPlaying();
                            }

                            if (shouldContinue) {
                                int currentPos;

                                if (!isSpanishMode) {
                                    // Progreso sintético para TTS (basado en tiempo transcurrido)
                                    int elapsed = (int) (System.currentTimeMillis() - startTime);
                                    currentPos = Math.min(elapsed, totalDuration);
                                    Log.d(TAG, "TTS Progress: " + currentPos + "/" + totalDuration + "ms");
                                } else {
                                    // Progreso real para MediaPlayer
                                    currentPos = audioHelper.getCurrentPosition();
                                }

                                if (audioPlayerView != null && totalDuration > 0) {
                                    audioPlayerView.setProgress(currentPos);
                                    audioPlayerView.setPlaying(true);
                                }

                                // Si alcanzó o superó la duración total, finalizar visualización
                                if (currentPos >= totalDuration) {
                                    Log.d(TAG, "Audio completed - stopping time update");
                                    isPlaying = false;
                                    isPaused = false;
                                    if (audioPlayerView != null) {
                                        audioPlayerView.setPlaying(false);
                                        audioPlayerView.setProgress(totalDuration);
                                    }
                                    if (playButton != null) {
                                        playButton.setImageResource(R.drawable.reproduce);
                                    }
                                    stopTimeUpdate();
                                } else {
                                    // Continuar actualizando cada 100ms
                                    timeUpdateHandler.postDelayed(this, 100);
                                }
                            } else {
                                Log.d(TAG, "Stopping time update - not playing or paused");
                                if (audioPlayerView != null) {
                                    audioPlayerView.setPlaying(false);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in time update runnable: " + e.getMessage(), e);
                        }
                    }
                };
                timeUpdateHandler.post(timeUpdateRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting time update: " + e.getMessage(), e);
        }
    }

    private void stopTimeUpdate() {
        Log.d(TAG, "Stopping time update");
        if (timeUpdateHandler != null && timeUpdateRunnable != null) {
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
        }
    }

    // Public getters for state
    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public String getCurrentVoiceType() {
        return currentVoiceType;
    }

    public boolean isSpanishMode() {
        return isSpanishMode;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    // Force English (TTS) mode programmatically
    public void setEnglishMode() {
        try {
            isSpanishMode = false;
            updateLanguageButtons();
            Log.d(TAG, "Forced English (TTS) mode");
        } catch (Exception e) {
            Log.e(TAG, "Error forcing English mode: " + e.getMessage(), e);
        }
    }

    // Cleanup method
    public void cleanup() {
        stopTimeUpdate();
        if (audioHelper != null) {
            audioHelper.cleanup();
        }
        stopAudio();
        Log.d(TAG, "ReusableAudioPlayerCard cleaned up");
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }

    // Reset state for a new question/text without destroying TTS resources
    public void resetForNewQuestion() {
        try {
            Log.d(TAG, "resetForNewQuestion called");
            stopTimeUpdate();
            if (isPlaying || isPaused) {
                stopAudio();
            }
            isPlaying = false;
            isPaused = false;
            currentPosition = 0;
            totalDuration = 0;

            if (audioPlayerView != null) {
                audioPlayerView.setPlaying(false);
                audioPlayerView.setProgress(0);
                // Si hay texto actual, actualizar la duración para la nueva pregunta
                if (currentText != null && !currentText.isEmpty()) {
                    audioPlayerView.setTextForTTS(currentText);
                    Log.d(TAG, "AudioPlayerView duration updated for new question");
                }
            }
            if (playButton != null) {
                playButton.setImageResource(R.drawable.reproduce);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting for new question: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            Log.d(TAG, "onDetachedFromWindow - stopping and cleaning up audio");
            cleanup();
        } catch (Exception e) {
            Log.e(TAG, "Error during onDetachedFromWindow cleanup: " + e.getMessage(), e);
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        try {
            if (visibility != View.VISIBLE) {
                Log.d(TAG, "View not visible - stopping audio");
                stopAudio();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling visibility change: " + e.getMessage(), e);
        }
    }
}