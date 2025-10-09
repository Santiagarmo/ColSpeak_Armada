package com.example.speak.helpers;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.example.speak.VoiceType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AlphabetAudioHelper {
    private static final String TAG = "AlphabetAudioHelper";
    
    private Context context;
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private Map<String, String> audioResourceMap;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isTextToSpeechReady = false;
    private VoiceType currentVoiceType = VoiceType.MUJER;
    private float currentSpeed = 0.1f;
    
    public AlphabetAudioHelper(Context context) {
        this.context = context;
        this.mediaPlayer = new MediaPlayer();
        initializeAudioResources();
        setupMediaPlayerListeners();
        initializeTextToSpeech();
    }
    
    /**
     * Inicializa el mapeo de recursos de audio
     */
    private void initializeAudioResources() {
        audioResourceMap = new HashMap<>();
        
        // Mapeo para el alfabeto
        audioResourceMap.put("alphabet_help_ei", "alphabet_help_ei.mp3");
        audioResourceMap.put("alphabet_help_i", "alphabet_help_i.mp3");
        audioResourceMap.put("alphabet_help_e", "alphabet_help_e.mp3");
        audioResourceMap.put("alphabet_help_ai", "alphabet_help_ai.mp3");
        audioResourceMap.put("alphabet_help_ou", "alphabet_ou.mp3");
        audioResourceMap.put("alphabet_help_ju", "alphabet_ju.mp3");
        audioResourceMap.put("alphabet_help_ar", "alphabet_ar.mp3");
        
        // Mapeo para el alfabeto completo
        audioResourceMap.put("alphabet_complete", "alphabet_complete.mp3");
    }
    
    /**
     * Configura los listeners del MediaPlayer
     */
    private void setupMediaPlayerListeners() {
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            isPaused = false;
            Log.d(TAG, "Audio playback completed");
        });
        
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            isPlaying = false;
            isPaused = false;
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            Toast.makeText(context, "Error reproduciendo audio", Toast.LENGTH_SHORT).show();
            return true;
        });
    }
    
    /**
     * Inicializa TextToSpeech
     */
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported or not installed offline");
                } else {
                    textToSpeech.setSpeechRate(currentSpeed);
                    textToSpeech.setPitch(currentVoiceType.getPitch());
                    isTextToSpeechReady = true;
                    Log.d(TAG, "TextToSpeech initialized successfully for alphabet audio");
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
                isTextToSpeechReady = false;
            }
        });
    }
    
    /**
     * Reproduce el audio especificado (archivo MP3 original en español)
     */
    public void playAudio(String audioResource) {
        if (isPlaying) {
            stopAudio();
        }
        
        // Resetear estado de pausa cuando se reproduce un nuevo audio
        isPaused = false;
        
        try {
            String fileName = audioResourceMap.get(audioResource);
            if (fileName != null) {
                // Reproducir archivo MP3 original
                playFromAssets(fileName);
                Log.d(TAG, "Playing original MP3 file: " + fileName);
            } else {
                // Si no hay archivo específico, usar TTS como fallback
                Log.w(TAG, "Audio resource not found: " + audioResource + ", using TTS fallback");
                Toast.makeText(context, "Reproduciendo: " + audioResource, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio: " + audioResource, e);
            Toast.makeText(context, "Error reproduciendo audio", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Reproduce audio desde assets
     */
    private void playFromAssets(String fileName) throws IOException {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context.getAssets().openFd(fileName));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "Playing audio from assets: " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "Error playing from assets: " + fileName, e);
            // Fallback a TTS
            Toast.makeText(context, "Reproduciendo: " + fileName, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Detiene la reproducción de audio
     */
    public void stopAudio() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            isPlaying = false;
            isPaused = false;
            Log.d(TAG, "Audio stopped");
        }
    }
    
    /**
     * Pausa la reproducción de audio
     */
    public void pauseAudio() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            isPaused = true;
            Log.d(TAG, "Audio paused");
        }
    }
    
    /**
     * Reanuda la reproducción de audio desde donde se pausó
     */
    public void resumeAudio() {
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPlaying = true;
            isPaused = false;
            Log.d(TAG, "Audio resumed from paused position");
        }
    }
    
    /**
     * Verifica si está reproduciendo audio
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Verifica si el audio está pausado
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Libera los recursos del MediaPlayer
     */
    public void release() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            isPaused = false;
        }
        releaseTextToSpeech();
    }
    
    /**
     * Obtiene la duración del audio actual en milisegundos
     */
    public int getDuration() {
        if (mediaPlayer != null && isPlaying) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }
    
    /**
     * Obtiene la posición actual del audio en milisegundos
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }
    
    /**
     * Cambia la posición del audio
     */
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }
    
    /**
     * Obtiene la duración total del audio
     */
    public int getTotalDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }
    
    /**
     * Verifica si el audio está preparado
     */
    public boolean isPrepared() {
        return mediaPlayer != null;
    }
    
    // ===== MÉTODOS PARA TIPOS DE VOZ =====
    
    /**
     * Establece el tipo de voz actual
     */
    public void setVoiceType(VoiceType voiceType) {
        this.currentVoiceType = voiceType;
        if (textToSpeech != null && isTextToSpeechReady) {
            textToSpeech.setPitch(voiceType.getPitch());
            textToSpeech.setSpeechRate(voiceType.getSpeed());
            this.currentSpeed = voiceType.getSpeed();
            Log.d(TAG, "Voice type changed to: " + voiceType.getName() + 
                  " (Pitch: " + voiceType.getPitch() + ", Speed: " + voiceType.getSpeed() + ")");
        }
    }
    
    /**
     * Obtiene el tipo de voz actual
     */
    public VoiceType getCurrentVoiceType() {
        return currentVoiceType;
    }
    
    /**
     * Establece la velocidad de reproducción
     */
    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (textToSpeech != null && isTextToSpeechReady) {
            textToSpeech.setSpeechRate(speed);
        }
    }
    
    /**
     * Obtiene la velocidad actual
     */
    public float getCurrentSpeed() {
        return currentSpeed;
    }
    
    /**
     * Reproduce texto usando TextToSpeech (solo para inglés con efectos de tono)
     */
    public void speakText(String text) {
        if (textToSpeech != null && isTextToSpeechReady) {
            // Aplicar configuración de voz actual (efectos de tono)
            textToSpeech.setPitch(currentVoiceType.getPitch());
            textToSpeech.setSpeechRate(currentSpeed);
            
            // Detener cualquier reproducción anterior
            stopAudio();
            
            // Reproducir el texto en inglés con efectos de tono
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AlphabetAudioEnglish");
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text: " + text);
                Toast.makeText(context, "Error reproduciendo texto", Toast.LENGTH_SHORT).show();
            } else {
                isPlaying = true;
                Log.d(TAG, "Speaking English text with voice: " + currentVoiceType.getName() + 
                      " (Pitch: " + currentVoiceType.getPitch() + ", Speed: " + currentSpeed + ")");
            }
        } else {
            Log.w(TAG, "TextToSpeech not ready, falling back to audio files");
            // Fallback a archivos de audio si TTS no está disponible
            playAudio(text);
        }
    }
    
    /**
     * Verifica si TextToSpeech está listo
     */
    public boolean isTextToSpeechReady() {
        return isTextToSpeechReady;
    }
    
    /**
     * Libera los recursos de TextToSpeech
     */
    public void releaseTextToSpeech() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            isTextToSpeechReady = false;
            Log.d(TAG, "TextToSpeech released");
        }
    }
}
