package com.example.speak.helpers;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReusableAudioHelper {
    
    private static final String TAG = "ReusableAudioHelper";
    
    private Context context;
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private TextToSpeech externalTTS; // TTS desde la Activity
    private String assetsFolder;

    // State
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private int totalDuration = 0;

    // Voice type mapping
    private Map<String, String> voiceTypeMap;

    public ReusableAudioHelper(Context context) {
        try {
            this.context = context;
            initializeMediaPlayer();
            initializeTextToSpeech();
            initializeVoiceTypeMap();
            Log.d(TAG, "ReusableAudioHelper initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ReusableAudioHelper: " + e.getMessage(), e);
        }
    }

    /**
     * Establece un TextToSpeech externo (desde la Activity)
     * Si se proporciona, se usará este en lugar del interno
     */
    public void setExternalTextToSpeech(TextToSpeech tts) {
        this.externalTTS = tts;
        if (tts != null) {
            Log.d(TAG, "External TTS set successfully");
        }
    }
    
    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "Audio playback completed");
            isPlaying = false;
            isPaused = false;
        });
        
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            isPlaying = false;
            isPaused = false;
            return true;
        });
    }
    
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    Log.d(TAG, "TextToSpeech initialized successfully");
                    // Listener para actualizar estado de reproducción TTS
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            isPlaying = true;
                            Log.d(TAG, "TTS onStart: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            isPlaying = false;
                            Log.d(TAG, "TTS onDone: " + utteranceId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            isPlaying = false;
                            Log.e(TAG, "TTS onError: " + utteranceId);
                        }
                    });
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
            }
        });
        
        // Set default voice parameters
        if (textToSpeech != null) {
            textToSpeech.setPitch(1.0f);
            textToSpeech.setSpeechRate(1.0f);
        }
    }
    
    private void initializeVoiceTypeMap() {
        voiceTypeMap = new HashMap<>();
        voiceTypeMap.put("child", "child");
        voiceTypeMap.put("little_girl", "little_girl");
        voiceTypeMap.put("woman", "woman");
        voiceTypeMap.put("man", "man");
        voiceTypeMap.put("default", "default");
    }
    
    public void configure(String assetsFolder) {
        this.assetsFolder = assetsFolder;
        Log.d(TAG, "Configured for assets folder: " + assetsFolder);
    }
    
    /**
     * Play audio file from assets
     */
    public void playAudio(String audioFile) {
        try {
            Log.d(TAG, "playAudio called with file: " + audioFile);
            
            if (audioFile == null || audioFile.isEmpty()) {
                Log.e(TAG, "Audio file is null or empty");
                Toast.makeText(context, "Error: Archivo de audio no válido", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (assetsFolder == null || assetsFolder.isEmpty()) {
                Log.e(TAG, "Assets folder is null or empty");
                Toast.makeText(context, "Error: Carpeta de assets no configurada", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isPlaying) {
                stopAudio();
            }
            
            isPaused = false;
            
            String fileName = assetsFolder + "/" + audioFile;
            Log.d(TAG, "Attempting to play: " + fileName);
            playFromAssets(fileName);
            Log.d(TAG, "Playing audio file: " + fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio: " + audioFile, e);
            Toast.makeText(context, "Error reproduciendo audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isPlaying = false;
            isPaused = false;
        }
    }
    
    /**
     * Play audio from assets
     */
    private void playFromAssets(String fileName) throws IOException {
        try {
            Log.d(TAG, "playFromAssets called with: " + fileName);
            
            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer is null");
                throw new IOException("MediaPlayer not initialized");
            }
            
            mediaPlayer.reset();
            Log.d(TAG, "MediaPlayer reset completed");
            
            // Try to open the file
            try {
                android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(fileName);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                Log.d(TAG, "Audio file opened successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error opening audio file: " + fileName, e);
                throw new IOException("No se pudo abrir el archivo: " + fileName);
            }
            
            mediaPlayer.prepare();
            Log.d(TAG, "MediaPlayer prepared");
            
            // Get duration
            totalDuration = mediaPlayer.getDuration();
            Log.d(TAG, "Audio duration: " + totalDuration + "ms");
            
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "Playing audio from assets: " + fileName);
            
        } catch (IOException e) {
            Log.e(TAG, "Error playing from assets: " + fileName, e);
            Toast.makeText(context, "Error: No se pudo reproducir el archivo de audio - " + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error playing from assets: " + fileName, e);
            Toast.makeText(context, "Error inesperado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw new IOException("Error inesperado: " + e.getMessage());
        }
    }
    
    /**
     * Speak text with specific voice type using TTS
     */
    public void speakTextWithVoice(String text, String voiceType) {
        // Usar el TTS externo si está disponible, de lo contrario usar el interno
        TextToSpeech ttsToUse = (externalTTS != null) ? externalTTS : textToSpeech;

        if (ttsToUse == null) {
            Log.e(TAG, "TextToSpeech is null");
            return;
        }

        // Guardar los parámetros actuales del TTS
        float currentPitch = 1.0f;
        float currentRate = 1.0f;

        // Set voice parameters based on type
        setVoiceParameters(voiceType, ttsToUse);

        // Speak the text
        int result = ttsToUse.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ReusableAudio");
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error speaking text");
            Toast.makeText(context, "Error reproduciendo texto", Toast.LENGTH_SHORT).show();
        } else {
            isPlaying = true;
            Log.d(TAG, "Speaking text with voice type: " + voiceType + " using " +
                  (externalTTS != null ? "external" : "internal") + " TTS");
        }
    }
    
    /**
     * Set voice parameters based on voice type
     */
    private void setVoiceParameters(String voiceType, TextToSpeech tts) {
        if (tts == null) return;

        float pitch = 1.0f;
        float rate = 1.0f;

        switch (voiceType) {
            case "child":
                pitch = 1.3f;  // Higher pitch for child
                rate = 0.9f;   // Slightly slower
                break;
            case "little_girl":
                pitch = 1.2f;  // Higher pitch for little girl
                rate = 1.0f;   // Normal rate
                break;
            case "woman":
                pitch = 1.1f;  // Slightly higher pitch
                rate = 1.0f;   // Normal rate
                break;
            case "man":
                pitch = 0.9f;  // Lower pitch for man
                rate = 1.0f;   // Normal rate
                break;
            default:
                pitch = 1.0f;
                rate = 1.0f;
                break;
        }

        tts.setPitch(pitch);
        tts.setSpeechRate(rate);

        Log.d(TAG, "Set voice parameters - Type: " + voiceType + ", Pitch: " + pitch + ", Rate: " + rate);
    }
    
    /**
     * Pause audio playback
     */
    public void pauseAudio() {
        if (mediaPlayer != null && isPlaying && !isPaused) {
            mediaPlayer.pause();
            isPaused = true;
            Log.d(TAG, "Audio paused");
        } else {
            TextToSpeech ttsToUse = (externalTTS != null) ? externalTTS : textToSpeech;
            if (ttsToUse != null && isPlaying && !isPaused) {
                ttsToUse.stop();
                isPaused = true;
                Log.d(TAG, "TTS paused using " + (externalTTS != null ? "external" : "internal") + " TTS");
            }
        }
    }

    /**
     * Resume audio playback
     */
    public void resumeAudio() {
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPaused = false;
            Log.d(TAG, "Audio resumed");
        } else {
            TextToSpeech ttsToUse = (externalTTS != null) ? externalTTS : textToSpeech;
            if (ttsToUse != null && isPaused) {
                // TTS doesn't support resume, so we need to restart
                Log.d(TAG, "TTS doesn't support resume (using " + (externalTTS != null ? "external" : "internal") + " TTS)");
            }
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(positionMs);
            } catch (Exception e) {
                Log.e(TAG, "Error seeking to: " + positionMs, e);
            }
        }
    }
    
    /**
     * Stop audio playback
     */
    public void stopAudio() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            isPlaying = false;
            isPaused = false;
            Log.d(TAG, "Audio stopped");
        }

        // Detener el TTS apropiado
        TextToSpeech ttsToUse = (externalTTS != null) ? externalTTS : textToSpeech;
        if (ttsToUse != null) {
            ttsToUse.stop();
            isPlaying = false;
            isPaused = false;
            Log.d(TAG, "TTS stopped using " + (externalTTS != null ? "external" : "internal") + " TTS");
        }
    }
    
    /**
     * Set playback speed
     */
    public void setPlaybackSpeed(float speed) {
        // Usar el TTS externo si está disponible
        TextToSpeech ttsToUse = (externalTTS != null) ? externalTTS : textToSpeech;

        if (ttsToUse != null) {
            ttsToUse.setSpeechRate(speed);
            Log.d(TAG, "TTS speed set to: " + speed + " using " +
                  (externalTTS != null ? "external" : "internal") + " TTS");
        }

        // MediaPlayer speed adjustment would require API 23+
        if (mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                Log.d(TAG, "MediaPlayer speed set to: " + speed);
            } catch (Exception e) {
                Log.e(TAG, "Error setting MediaPlayer speed", e);
            }
        }
    }
    
    /**
     * Get total duration of current audio
     */
    public int getTotalDuration() {
        if (mediaPlayer != null && isPlaying) {
            return mediaPlayer.getDuration();
        }
        return totalDuration;
    }
    
    /**
     * Get current position
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPlaying) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }
    
    // State getters
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public boolean isMediaPlayerPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    
    public boolean isTTSPlaying() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
            mediaPlayer = null;
        }
        
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        
        isPlaying = false;
        isPaused = false;
        Log.d(TAG, "AudioHelper cleaned up");
    }
}