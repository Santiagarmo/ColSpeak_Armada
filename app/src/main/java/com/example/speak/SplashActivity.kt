package com.example.speak;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 10000; // 10 segundos (ajustar según duración del video)
    private VideoView splashVideo;
    private boolean videoLoaded = false;
    private File tempVideoFile;
    private Handler splashHandler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ocultar la barra de estado para pantalla completa
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | 
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Inicializar video
        splashVideo = findViewById(R.id.splash_video);
        
        // Debug: verificar que el video se encuentra
        if (splashVideo == null) {
            Log.e("SplashActivity", "splashVideo es null");
        } else {
            Log.d("SplashActivity", "splashVideo encontrado");
        }

        // Configurar video
        setupVideo();

        // Configurar handler para el splash
        splashHandler = new Handler(Looper.getMainLooper());
        splashRunnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MajorActivity.class);
                startActivity(intent);
                finish();
            }
        };
    }

    private void setupVideo() {
        if (splashVideo == null) {
            Log.e("SplashActivity", "VideoView no encontrado");
            return;
        }

        try {
            // Verificar si el archivo existe en assets
            try {
                InputStream testStream = getAssets().open("SplashColspeak.mp4");
                testStream.close();
                Log.d("SplashActivity", "✅ Archivo SplashColspeak.mp4 encontrado en assets");
            } catch (Exception e) {
                Log.e("SplashActivity", "❌ Error: No se puede abrir SplashColspeak.mp4 desde assets: " + e.getMessage());
                // Intentar con el video de prueba
                try {
                    InputStream testStream2 = getAssets().open("alphabet.mp4");
                    testStream2.close();
                    Log.d("SplashActivity", "✅ Usando video de prueba: alphabet.mp4");
                } catch (Exception e2) {
                    Log.e("SplashActivity", "❌ Error: No se puede abrir alphabet.mp4 desde assets: " + e2.getMessage());
                    return;
                }
            }

            // Crear archivo temporal desde assets
            tempVideoFile = File.createTempFile("splash_", ".mp4", getCacheDir());
            
            // Intentar cargar SplashColspeak.mp4 primero, si no funciona usar alphabet.mp4
            String videoFileName = "SplashColspeak.mp4";
            try {
                InputStream testStream = getAssets().open("SplashColspeak.mp4");
                testStream.close();
                Log.d("SplashActivity", "✅ Usando SplashColspeak.mp4");
            } catch (Exception e) {
                videoFileName = "alphabet.mp4";
                Log.d("SplashActivity", "✅ Usando video de prueba: alphabet.mp4");
            }
            
            InputStream inputStream = getAssets().open(videoFileName);
            FileOutputStream outputStream = new FileOutputStream(tempVideoFile);

            byte[] buffer = new byte[8192];
            int length;
            int totalBytes = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalBytes += length;
            }

            inputStream.close();
            outputStream.close();

            Log.d("SplashActivity", "✅ Archivo temporal creado: " + tempVideoFile.getAbsolutePath());
            Log.d("SplashActivity", "✅ Tamaño del archivo: " + totalBytes + " bytes");

            // Verificar que el archivo temporal existe y tiene contenido
            if (tempVideoFile.exists() && tempVideoFile.length() > 0) {
                Log.d("SplashActivity", "✅ Archivo temporal verificado - existe y tiene contenido");
            } else {
                Log.e("SplashActivity", "❌ Error: Archivo temporal no existe o está vacío");
                return;
            }

            // Cargar video desde archivo temporal
            splashVideo.setVideoPath(tempVideoFile.getAbsolutePath());
            Log.d("SplashActivity", "✅ Video cargado desde archivo temporal");

            // Configurar listeners del video
            splashVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d("SplashActivity", "Video preparado para reproducción");
                    Log.d("SplashActivity", "Duración del video: " + mediaPlayer.getDuration() + " ms");
                    Log.d("SplashActivity", "Dimensiones del video: " + mediaPlayer.getVideoWidth() + "x" + mediaPlayer.getVideoHeight());
                    videoLoaded = true;
                    
                    // Configurar para reproducción en bucle
                    mediaPlayer.setLooping(true);
                    
                    // Iniciar reproducción
                    splashVideo.start();
                    Log.d("SplashActivity", "Video iniciado - debería estar reproduciéndose ahora");
                    
                    // Ajustar duración del splash basándose en la duración del video
                    int videoDuration = mediaPlayer.getDuration();
                    if (videoDuration > 0) {
                        // Usar la duración del video + 2 segundos extra para asegurar que se vea completo
                        int splashDuration = videoDuration + 2000;
                        Log.d("SplashActivity", "Duración del splash ajustada a: " + splashDuration + " ms");
                        
                        // Programar el splash con la duración del video
                        splashHandler.postDelayed(splashRunnable, splashDuration);
                    } else {
                        // Fallback: usar duración por defecto
                        Log.d("SplashActivity", "Usando duración por defecto: " + SPLASH_DURATION + " ms");
                        splashHandler.postDelayed(splashRunnable, SPLASH_DURATION);
                    }
                }
            });

            splashVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    Log.e("SplashActivity", "Error reproduciendo video: " + what + ", " + extra);
                    Log.e("SplashActivity", "Detalles del error - what: " + what + ", extra: " + extra);
                    videoLoaded = false;
                    return true;
                }
            });

            splashVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.d("SplashActivity", "Video completado - reiniciando...");
                }
            });

            // Agregar listener para verificar si el video está realmente reproduciéndose
            splashVideo.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.d("SplashActivity", "Video comenzó a renderizarse");
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            Log.d("SplashActivity", "Video comenzó a cargar buffer");
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            Log.d("SplashActivity", "Video terminó de cargar buffer");
                            break;
                    }
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e("SplashActivity", "Error configurando video: " + e.getMessage());
            videoLoaded = false;
        }
    }


    @Override
    public void onBackPressed() {
        // Deshabilitar el botón de retroceso en el splash
        // No hacer nada
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar handler
        if (splashHandler != null && splashRunnable != null) {
            splashHandler.removeCallbacks(splashRunnable);
        }
        // Limpiar recursos del video
        if (splashVideo != null) {
            splashVideo.stopPlayback();
            splashVideo = null;
        }
        // Limpiar archivo temporal
        if (tempVideoFile != null && tempVideoFile.exists()) {
            tempVideoFile.delete();
            Log.d("SplashActivity", "Archivo temporal eliminado");
        }
    }
}
