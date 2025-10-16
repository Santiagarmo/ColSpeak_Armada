package com.example.speak

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class SplashActivity : AppCompatActivity() {
    private var splashVideo: VideoView? = null
    private var videoLoaded = false
    private var tempVideoFile: File? = null
    private var splashHandler: Handler? = null
    private var splashRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar la barra de estado para pantalla completa
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Inicializar video
        splashVideo = findViewById<VideoView?>(R.id.splash_video)


        // Debug: verificar que el video se encuentra
        if (splashVideo == null) {
            Log.e("SplashActivity", "splashVideo es null")
        } else {
            Log.d("SplashActivity", "splashVideo encontrado")
        }

        // Configurar video
        setupVideo()

        // Configurar handler para el splash
        splashHandler = Handler(Looper.getMainLooper())
        splashRunnable = object : Runnable {
            override fun run() {
                val intent = Intent(this@SplashActivity, MajorActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupVideo() {
        if (splashVideo == null) {
            Log.e("SplashActivity", "VideoView no encontrado")
            return
        }

        try {
            // Verificar si el archivo existe en assets
            try {
                val testStream = getAssets().open("SplashColspeak.mp4")
                testStream.close()
                Log.d("SplashActivity", "✅ Archivo SplashColspeak.mp4 encontrado en assets")
            } catch (e: Exception) {
                Log.e(
                    "SplashActivity",
                    "❌ Error: No se puede abrir SplashColspeak.mp4 desde assets: " + e.message
                )
                // Intentar con el video de prueba
                try {
                    val testStream2 = getAssets().open("alphabet.mp4")
                    testStream2.close()
                    Log.d("SplashActivity", "✅ Usando video de prueba: alphabet.mp4")
                } catch (e2: Exception) {
                    Log.e(
                        "SplashActivity",
                        "❌ Error: No se puede abrir alphabet.mp4 desde assets: " + e2.message
                    )
                    return
                }
            }

            // Crear archivo temporal desde assets
            tempVideoFile = File.createTempFile("splash_", ".mp4", getCacheDir())


            // Intentar cargar SplashColspeak.mp4 primero, si no funciona usar alphabet.mp4
            var videoFileName = "SplashColspeak.mp4"
            try {
                val testStream = getAssets().open("SplashColspeak.mp4")
                testStream.close()
                Log.d("SplashActivity", "✅ Usando SplashColspeak.mp4")
            } catch (e: Exception) {
                videoFileName = "alphabet.mp4"
                Log.d("SplashActivity", "✅ Usando video de prueba: alphabet.mp4")
            }

            val inputStream = getAssets().open(videoFileName)
            val outputStream = FileOutputStream(tempVideoFile)

            val buffer = ByteArray(8192)
            var length: Int
            var totalBytes = 0
            while ((inputStream.read(buffer).also { length = it }) > 0) {
                outputStream.write(buffer, 0, length)
                totalBytes += length
            }

            inputStream.close()
            outputStream.close()

            Log.d(
                "SplashActivity",
                "✅ Archivo temporal creado: " + tempVideoFile!!.getAbsolutePath()
            )
            Log.d("SplashActivity", "✅ Tamaño del archivo: " + totalBytes + " bytes")

            // Verificar que el archivo temporal existe y tiene contenido
            if (tempVideoFile!!.exists() && tempVideoFile!!.length() > 0) {
                Log.d("SplashActivity", "✅ Archivo temporal verificado - existe y tiene contenido")
            } else {
                Log.e("SplashActivity", "❌ Error: Archivo temporal no existe o está vacío")
                return
            }

            // Cargar video desde archivo temporal
            splashVideo!!.setVideoPath(tempVideoFile!!.getAbsolutePath())
            Log.d("SplashActivity", "✅ Video cargado desde archivo temporal")

            // Configurar listeners del video
            splashVideo!!.setOnPreparedListener(object : OnPreparedListener {
                override fun onPrepared(mediaPlayer: MediaPlayer) {
                    Log.d("SplashActivity", "Video preparado para reproducción")
                    Log.d(
                        "SplashActivity",
                        "Duración del video: " + mediaPlayer.getDuration() + " ms"
                    )
                    Log.d(
                        "SplashActivity",
                        "Dimensiones del video: " + mediaPlayer.getVideoWidth() + "x" + mediaPlayer.getVideoHeight()
                    )
                    videoLoaded = true


                    // Configurar para reproducción en bucle
                    mediaPlayer.setLooping(true)


                    // Iniciar reproducción
                    splashVideo!!.start()
                    Log.d("SplashActivity", "Video iniciado - debería estar reproduciéndose ahora")


                    // Ajustar duración del splash basándose en la duración del video
                    val videoDuration = mediaPlayer.getDuration()
                    if (videoDuration > 0) {
                        // Usar la duración del video + 2 segundos extra para asegurar que se vea completo
                        val splashDuration = videoDuration + 2000
                        Log.d(
                            "SplashActivity",
                            "Duración del splash ajustada a: " + splashDuration + " ms"
                        )


                        // Programar el splash con la duración del video
                        splashHandler!!.postDelayed(splashRunnable!!, splashDuration.toLong())
                    } else {
                        // Fallback: usar duración por defecto
                        Log.d(
                            "SplashActivity",
                            "Usando duración por defecto: " + SPLASH_DURATION + " ms"
                        )
                        splashHandler!!.postDelayed(splashRunnable!!, SPLASH_DURATION.toLong())
                    }
                }
            })

            splashVideo!!.setOnErrorListener(object : MediaPlayer.OnErrorListener {
                override fun onError(mediaPlayer: MediaPlayer?, what: Int, extra: Int): Boolean {
                    Log.e("SplashActivity", "Error reproduciendo video: " + what + ", " + extra)
                    Log.e(
                        "SplashActivity",
                        "Detalles del error - what: " + what + ", extra: " + extra
                    )
                    videoLoaded = false
                    return true
                }
            })

            splashVideo!!.setOnCompletionListener(object : OnCompletionListener {
                override fun onCompletion(mediaPlayer: MediaPlayer?) {
                    Log.d("SplashActivity", "Video completado - reiniciando...")
                }
            })

            // Agregar listener para verificar si el video está realmente reproduciéndose
            splashVideo!!.setOnInfoListener(object : MediaPlayer.OnInfoListener {
                override fun onInfo(mediaPlayer: MediaPlayer?, what: Int, extra: Int): Boolean {
                    when (what) {
                        MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> Log.d(
                            "SplashActivity",
                            "Video comenzó a renderizarse"
                        )

                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> Log.d(
                            "SplashActivity",
                            "Video comenzó a cargar buffer"
                        )

                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> Log.d(
                            "SplashActivity",
                            "Video terminó de cargar buffer"
                        )
                    }
                    return false
                }
            })
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error configurando video: " + e.message)
            videoLoaded = false
        }
    }


    override fun onBackPressed() {
        // Deshabilitar el botón de retroceso en el splash
        // No hacer nada
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar handler
        if (splashHandler != null && splashRunnable != null) {
            splashHandler!!.removeCallbacks(splashRunnable!!)
        }
        // Limpiar recursos del video
        if (splashVideo != null) {
            splashVideo!!.stopPlayback()
            splashVideo = null
        }
        // Limpiar archivo temporal
        if (tempVideoFile != null && tempVideoFile!!.exists()) {
            tempVideoFile!!.delete()
            Log.d("SplashActivity", "Archivo temporal eliminado")
        }
    }

    companion object {
        private const val SPLASH_DURATION = 10000 // 10 segundos (ajustar según duración del video)
    }
}
