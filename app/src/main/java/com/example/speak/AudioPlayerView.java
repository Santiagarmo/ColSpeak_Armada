package com.example.speak;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class AudioPlayerView extends View {
    private Paint playedWavePaint;
    private Paint unplayedWavePaint;

    private float progress = 0f;
    private float duration = 1f; // Cambiado de 100f a 1f - será actualizado con la duración real
    private boolean isPlaying = false;
    OnProgressChangeListener listener;

    // TextToSpeech instance (optional, for calculating duration)
    private TextToSpeech textToSpeech;
    private String currentText = "";

    // Variables para las ondas de audio
    private float[] waveHeights = new float[40]; // Más ondas para mejor visualización
    private float waveAnimation = 0f;

    public interface OnProgressChangeListener {
        void onProgressChanged(float progress);
        void onPlayPause(boolean isPlaying);
        void onSeek(float position);
    }

    public AudioPlayerView(Context context) {
        super(context);
        init();
    }

    public AudioPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint para ondas ya reproducidas (color más oscuro/vibrante)
        playedWavePaint = new Paint();
        playedWavePaint.setAntiAlias(true);
        playedWavePaint.setColor(Color.parseColor("#1A4F7C")); // Azul
        playedWavePaint.setStyle(Paint.Style.FILL);

        // Paint para ondas sin reproducir (color más claro/gris)
        unplayedWavePaint = new Paint();
        unplayedWavePaint.setAntiAlias(true);
        unplayedWavePaint.setColor(Color.parseColor("#B0BEC5")); // Gris claro
        unplayedWavePaint.setStyle(Paint.Style.FILL);

        // Inicializar ondas con alturas variadas (simulando forma de onda)
        for (int i = 0; i < waveHeights.length; i++) {
            // Crear una forma de onda más realista con variación
            double wave = Math.sin(i * 0.5) * 0.5 + 0.5; // Onda sinusoidal
            double random = Math.random() * 0.4; // Variación aleatoria
            waveHeights[i] = (float) (wave * 0.6 + random * 0.4);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        // SIEMPRE dibujar las ondas de audio (reproduciendo o no)
        drawAudioWaves(canvas, width, centerY);
    }

    private void drawAudioWaves(Canvas canvas, int width, int centerY) {
        // Validaciones para evitar crashes
        if (waveHeights == null || waveHeights.length == 0) return;
        if (width <= 40) return;

        // Validar que la duración sea válida y mayor que cero
        if (duration <= 0 || Float.isNaN(duration) || Float.isInfinite(duration)) {
            duration = 1f; // Valor por defecto seguro
        }

        int waveWidth = (width - 40) / waveHeights.length;
        int startX = 20;

        // Calcular el índice de la onda que corresponde al progreso actual
        // Asegurar que progress esté dentro del rango válido
        float safeProgress = Math.max(0, Math.min(duration, progress));
        float progressRatio = safeProgress / duration;

        // Validar que progressRatio sea un número válido
        if (Float.isNaN(progressRatio) || Float.isInfinite(progressRatio)) {
            progressRatio = 0f;
        }

        progressRatio = Math.max(0f, Math.min(1f, progressRatio));
        int progressIndex = (int) (progressRatio * waveHeights.length);
        progressIndex = Math.max(0, Math.min(waveHeights.length - 1, progressIndex));

        for (int i = 0; i < waveHeights.length; i++) {
            float waveX = startX + i * waveWidth + waveWidth / 2f;

            // Altura base de las ondas - validar que no sea infinito o NaN
            float baseHeight = waveHeights[i] * 60f;
            if (Float.isNaN(baseHeight) || Float.isInfinite(baseHeight)) {
                baseHeight = 30f;
            }

            // Si está reproduciendo, animar las ondas cercanas a la posición actual
            float currentHeight = baseHeight;
            if (isPlaying) {
                // Calcular distancia a la posición actual de reproducción
                float distance = Math.abs(i - progressIndex);
                if (distance < 3) { // Solo animar ondas cercanas
                    float animationIntensity = 1.0f - (distance / 3.0f);
                    float animationOffset = (float) Math.sin(waveAnimation + i * 0.3) * 0.3f * animationIntensity;
                    currentHeight = baseHeight * (1 + animationOffset);
                }
            }

            // Dimensiones de cada barra
            float barWidth = Math.max(waveWidth * 0.6f, 3f); // Ancho adaptativo
            float barHeight = Math.max(currentHeight, 10f); // Altura mínima
            float left = waveX - barWidth / 2f;
            float top = centerY - barHeight / 2f;
            float right = waveX + barWidth / 2f;
            float bottom = centerY + barHeight / 2f;

            // Validar que las coordenadas sean válidas
            if (Float.isNaN(left) || Float.isNaN(top) || Float.isNaN(right) || Float.isNaN(bottom)) {
                continue;
            }

            // Elegir color según si la onda ya fue reproducida o no
            Paint currentPaint = (i <= progressIndex) ? playedWavePaint : unplayedWavePaint;

            // Dibujar la barra de onda con esquinas redondeadas
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, currentPaint);
        }

        // Si está reproduciendo, continuar animación
        if (isPlaying) {
            waveAnimation += 0.25f;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                int width = getWidth();

                // Validar que la duración sea válida antes de calcular
                if (duration <= 0 || Float.isNaN(duration) || Float.isInfinite(duration)) {
                    return true;
                }

                // Calcular nueva posición proporcional a la duración real
                float touchRatio = Math.max(0f, Math.min(1f, (x - 20) / (width - 40)));
                float newProgress = touchRatio * duration;
                newProgress = Math.max(0, Math.min(duration, newProgress));

                setProgress(newProgress);

                if (listener != null) {
                    listener.onSeek(newProgress);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setProgress(float progress) {
        // Validar que progress sea un valor válido
        if (Float.isNaN(progress) || Float.isInfinite(progress)) {
            progress = 0f;
        }

        this.progress = Math.max(0f, progress);
        invalidate();

        if (listener != null) {
            listener.onProgressChanged(this.progress);
        }
    }

    public void setDuration(float duration) {
        // Validar que duration sea un valor válido y positivo
        if (Float.isNaN(duration) || Float.isInfinite(duration) || duration <= 0) {
            this.duration = 1f; // Valor por defecto seguro
        } else {
            this.duration = duration;
        }

        // Ajustar el progreso si excede la nueva duración
        if (progress > this.duration) {
            progress = this.duration;
        }

        invalidate();
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        invalidate();

        if (listener != null) {
            listener.onPlayPause(playing);
        }
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        this.listener = listener;
    }

    // Método para actualizar la forma de onda con datos reales del audio
    public void setWaveform(float[] amplitudes) {
        if (amplitudes != null && amplitudes.length > 0) {
            this.waveHeights = new float[Math.min(amplitudes.length, 50)];

            // Si hay más datos de los que necesitamos, hacer downsampling
            if (amplitudes.length > waveHeights.length) {
                int step = amplitudes.length / waveHeights.length;
                for (int i = 0; i < waveHeights.length; i++) {
                    float sum = 0;
                    for (int j = 0; j < step && (i * step + j) < amplitudes.length; j++) {
                        sum += amplitudes[i * step + j];
                    }
                    waveHeights[i] = sum / step;
                }
            } else {
                // Copiar directamente
                System.arraycopy(amplitudes, 0, waveHeights, 0, amplitudes.length);
            }

            invalidate();
        }
    }

    public float getProgress() {
        return progress;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Reinicia el progreso y la duración a sus valores iniciales
     */
    public void reset() {
        this.progress = 0f;
        this.duration = 1f;
        this.isPlaying = false;
        invalidate();
    }

    /**
     * Establece la instancia de TextToSpeech para calcular duraciones
     */
    public void setTextToSpeech(TextToSpeech tts) {
        this.textToSpeech = tts;
    }

    /**
     * Establece el texto actual y actualiza la duración estimada automáticamente
     */
    public void setTextForTTS(String text) {
        this.currentText = text;
        if (text != null && !text.isEmpty()) {
            float estimatedDuration = estimateTextDurationMs(text);
            setDuration(estimatedDuration);
        }
    }

    /**
     * Estima la duración del audio en milisegundos basándose en el texto
     * Fórmula: ~150 palabras por minuto promedio en inglés
     */
    private float estimateTextDurationMs(String text) {
        if (text == null || text.isEmpty()) {
            return 1000f; // 1 segundo por defecto
        }

        // Contar palabras (aproximadamente)
        String[] words = text.trim().split("\\s+");
        int wordCount = words.length;

        // Velocidad promedio: 150 palabras por minuto
        // Tiempo = (palabras / 150) * 60 * 1000 ms
        float durationMs = (wordCount / 150.0f) * 60.0f * 1000.0f;

        // Agregar un pequeño margen de error (+20%)
        durationMs *= 1.2f;

        // Asegurar un mínimo de 2 segundos
        return Math.max(durationMs, 2000f);
    }

    /**
     * Actualiza la duración basándose en el texto actual
     */
    public void updateDurationFromText() {
        if (currentText != null && !currentText.isEmpty()) {
            float estimatedDuration = estimateTextDurationMs(currentText);
            setDuration(estimatedDuration);
        }
    }
}