package com.example.speak.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.view.Gravity;
import androidx.core.content.ContextCompat;
import com.example.speak.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class VideoHelper {

    private static final String TAG = "VideoHelper";

    // Mapeo de temas a archivos de video
    private static final Map<String, String> TOPIC_VIDEOS = new HashMap<>();

    static {
        TOPIC_VIDEOS.put("ALPHABET", "alphabet.mp4");
        TOPIC_VIDEOS.put("NUMBERS", "numbers.mp4");
        TOPIC_VIDEOS.put("COLORS", "colors.mp4");
        TOPIC_VIDEOS.put("PERSONAL PRONOUNS", "pronouns.mp4");
        TOPIC_VIDEOS.put("POSSESSIVE ADJECTIVES", "possessive.mp4");
        TOPIC_VIDEOS.put("VERB TO BE", "verb_to_be.mp4");
        TOPIC_VIDEOS.put("SIMPLE PRESENT", "simple_present.mp4");
        TOPIC_VIDEOS.put("SIMPLE PAST", "simple_past.mp4");
        TOPIC_VIDEOS.put("DAILY ROUTINES", "daily_routines.mp4");
        TOPIC_VIDEOS.put("PREPOSITIONS", "prepositions.mp4");
        TOPIC_VIDEOS.put("ADJECTIVES", "adjectives.mp4");
        TOPIC_VIDEOS.put("COUNTABLE AND UNCOUNTABLE", "countable.mp4");
        TOPIC_VIDEOS.put("FREQUENCY ADVERBS", "frequency.mp4");
        TOPIC_VIDEOS.put("ORDINAL NUMBERS", "ordinal.mp4");
        TOPIC_VIDEOS.put("QUANTIFIERS", "quantifiers.mp4");
        TOPIC_VIDEOS.put("USED TO", "used_to.mp4");
        TOPIC_VIDEOS.put("WORD ORDER", "word_order.mp4");
        TOPIC_VIDEOS.put("ED PAST PRONUNCIATION", "ed_pronunciation.mp4");
        TOPIC_VIDEOS.put("VERB NEAR", "verb_near.mp4");
    }

    private final Context context;

    public VideoHelper(Context context) {
        this.context = context;
    }

    /**
     * Muestra el video del instructor con controles completos y pantalla completa
     */
    public void showInstructorVideo(String topic) {
        String videoFileName = getVideoFileName(topic);

        if (videoFileName == null) {
            Log.w(TAG, "No hay video disponible para el tema: " + topic);
            showVideoNotAvailableDialog(topic);
            return;
        }

        Log.d(TAG, "=== MOSTRANDO VIDEO DEL INSTRUCTOR ===");
        Log.d(TAG, "Archivo: " + videoFileName);
        Log.d(TAG, "Tema: " + topic);

        try {
            // Crear archivo temporal desde assets
            File tempFile = File.createTempFile("instructor_", ".mp4", context.getCacheDir());
            java.io.InputStream inputStream = context.getAssets().open(videoFileName);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "✅ Archivo temporal creado: " + tempFile.getAbsolutePath());

            // Crear diálogo a pantalla completa
            AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            // ✅ CAMBIAR A FRAMELAYOUT para permitir posicionamiento absoluto
            FrameLayout fullscreenLayout = new FrameLayout(context);
            fullscreenLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            fullscreenLayout.setBackgroundColor(android.graphics.Color.BLACK);

            // 🎥 CREAR VIDEOVIEW CON CONFIGURACIÓN CORRECTA
            VideoView videoView = new VideoView(context);
            FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            videoView.setLayoutParams(videoParams);

            // 🔧 CONFIGURACIÓN BÁSICA DEL VIDEOVIEW
            videoView.setKeepScreenOn(true);
            videoView.setFocusable(true);
            videoView.setFocusableInTouchMode(true);

            // 📁 CONFIGURAR RUTA DEL VIDEO
            videoView.setVideoPath(tempFile.getAbsolutePath());
            Log.d(TAG, "✅ Video configurado con archivo temporal: " + tempFile.getAbsolutePath());

            // 🎮 CONFIGURAR MEDIACONTROLLER ANTES DE LOS LISTENERS
            MediaController mediaController = new MediaController(context);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            // 🎯 CONFIGURAR LISTENERS DEL VIDEOVIEW
            videoView.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ Video del instructor preparado - Duración: " + mp.getDuration() + "ms");

                // 🎬 INICIAR REPRODUCCIÓN AUTOMÁTICAMENTE
                videoView.start();
                Log.d(TAG, "✅ Reproducción del instructor iniciada");

                // 🎮 MOSTRAR CONTROLES DE VIDEO
                mediaController.show();

                // 🔧 CONTROL DIRECTO DEL MEDIAPLAYER PARA PAUSA CORRECTA
                mediaController.setOnClickListener(v -> {
                    if (mp.isPlaying()) {
                        mp.pause();
                        Log.d(TAG, "⏸️ PAUSA MANUAL - Audio y video DETENIDOS");
                    } else {
                        mp.start();
                        Log.d(TAG, "▶️ REPRODUCCIÓN MANUAL - Audio y video CONTINUANDO");
                    }
                });
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ Error en video del instructor - what: " + what + ", extra: " + extra);
                Toast.makeText(context, "Error al reproducir video: " + what, Toast.LENGTH_SHORT).show();
                return true;
            });

            videoView.setOnCompletionListener(mp -> {
                Log.d(TAG, "✅ Video del instructor completado");
                tempFile.delete();
                Log.d(TAG, "✅ Archivo temporal eliminado");
            });

            // Agregar VideoView al layout PRIMERO
            fullscreenLayout.addView(videoView);

            // Configurar el diálogo
            builder.setView(fullscreenLayout);
            builder.setCancelable(false);

            // Botón para cerrar (solo cuando se presione back)
            builder.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    if (videoView.isPlaying()) {
                        videoView.stopPlayback();
                    }
                    tempFile.delete();
                    dialog.dismiss();
                    return true;
                }
                return false;
            });

            AlertDialog dialog = builder.create();

            // ✅ CREAR BOTÓN X EN LA ESQUINA SUPERIOR DERECHA
            Button closeButton = new Button(context);

            // Convertir dp a pixels para tamaño consistente
            int sizeInDp = 50; // Reducido para que sea más pequeño y redondo
            float scale = context.getResources().getDisplayMetrics().density;
            int sizeInPx = (int) (sizeInDp * scale);

            FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                    sizeInPx, // Ancho fijo para que sea circular
                    sizeInPx  // Alto fijo para que sea circular
            );
            closeButtonParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            closeButtonParams.setMargins(24, 24, 24, 0); // Margen superior y derecho
            closeButton.setLayoutParams(closeButtonParams);

            // ✅ CONFIGURAR EL CÍRCULO ROJO COMO FONDO
            closeButton.setBackgroundResource(R.drawable.circle_red);

            // ✅ CONFIGURAR LA X EN EL CENTRO
            closeButton.setText("X"); // Letra X mayúscula
            closeButton.setTextSize(20); // Tamaño ajustado para el círculo más pequeño
            closeButton.setTextColor(Color.WHITE); // Color blanco para contraste
            closeButton.setTypeface(null, android.graphics.Typeface.BOLD); // Negrita
            closeButton.setGravity(Gravity.CENTER); // Centrar la X
            closeButton.setPadding(0, 0, 0, 0); // Sin padding para centrado perfecto
            closeButton.setAllCaps(false); // Evitar transformación de texto
            closeButton.setMinWidth(0); // Remover ancho mínimo
            closeButton.setMinHeight(0); // Remover alto mínimo
            closeButton.setElevation(12); // ✅ Mayor elevación para que flote sobre el video
            closeButton.setStateListAnimator(null); // Remover animaciones de estado

            closeButton.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.stopPlayback();
                }
                tempFile.delete();
                dialog.dismiss();
            });

            // Agregar el botón DESPUÉS del VideoView para que aparezca encima
            fullscreenLayout.addView(closeButton);

            // Configurar pantalla completa
            dialog.setOnShowListener(dialogInterface -> {
                if (context instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) context;
                    activity.getWindow().setFlags(
                            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                    );
                }
            });

            dialog.show();

            Log.d(TAG, "✅ Video del instructor en pantalla completa iniciado");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al mostrar video del instructor: " + e.getMessage(), e);
            showVideoNotAvailableDialog(topic);
        }

        Log.d(TAG, "=== FIN VIDEO DEL INSTRUCTOR ===");
    }

    /**
     * Obtiene el nombre del archivo de video para un tema
     */
    private String getVideoFileName(String topic) {
        String videoFile = TOPIC_VIDEOS.get(topic.toUpperCase());

        if (videoFile != null) {
            return videoFile;
        }

        for (Map.Entry<String, String> entry : TOPIC_VIDEOS.entrySet()) {
            if (topic.toUpperCase().contains(entry.getKey()) ||
                    entry.getKey().contains(topic.toUpperCase())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Muestra diálogo cuando el video no está disponible
     */
    private void showVideoNotAvailableDialog(String topic) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🎥 Video del Instructor");
        builder.setMessage("El video para el tema '" + topic + "' no está disponible en este momento.\n\n" +
                "Estamos trabajando para agregar más contenido multimedia.");
        builder.setPositiveButton("Entendido", null);
        builder.show();

        Toast.makeText(context, "Video no disponible para: " + topic, Toast.LENGTH_SHORT).show();
    }

    /**
     * Verifica si existe un video para un tema específico
     */
    public boolean hasVideoForTopic(String topic) {
        return getVideoFileName(topic) != null;
    }

    /**
     * Verifica si el archivo de video existe físicamente en assets
     */
    public boolean videoFileExists(String topic) {
        String videoFileName = getVideoFileName(topic);
        if (videoFileName == null) {
            return false;
        }

        try {
            context.getAssets().open(videoFileName).close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Archivo de video no encontrado: " + videoFileName);
            return false;
        }
    }

    /**
     * Obtiene la lista de temas que tienen videos disponibles
     */
    public String[] getAvailableVideoTopics() {
        return TOPIC_VIDEOS.keySet().toArray(new String[0]);
    }
}