package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.components.ReusableAudioPlayerCard

class PossessiveAdjectActivity : AppCompatActivity() {
    //Control de Audio reutilizable
    private var reusableAudioPlayerCard: ReusableAudioPlayerCard? = null

    //Declaramos las variables
    private var eButtonListening: Button? = null
    private var alphabetTextView: TextView? = null


    // Reproductor de audio
    //Return Map Listening
    private var returnContainer: LinearLayout? = null

    // TextToSpeech
    private val alphabetText =
        "ADJECTIVE: MY YOUR OUR THEIR HIS HER ITS / NOUN: MINE YOURS OURS THEIRS HIS HERS"

    // Control de reproducción
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_possessive_adj)

        //Inicializamos las variables
        initializeViews()
        setupClickListeners()
        setupAudioPlayer()

        //Return Menu
        returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
        returnContainer!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMapA1()
            }
        })
    }

    private fun initializeViews() {
        try {
            eButtonListening = findViewById<Button>(R.id.eButtonListening)
            alphabetTextView = findViewById<TextView?>(R.id.alphabetTextView)

            // Reproductor de audio
            reusableAudioPlayerCard =
                findViewById<ReusableAudioPlayerCard?>(R.id.reusableAudioPlayerCard)
        } catch (e: Exception) {
            //Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show();
            finish()
        }
    }

    private fun setupClickListeners() {
        //Configuramos el botón de start
        eButtonListening!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@PossessiveAdjectActivity, ListeningActivity::class.java)
                intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                intent.putExtra("LEVEL", "A1.1")
                startActivity(intent)
            }
        })
    }

    private fun setupAudioPlayer() {
        try {
            if (reusableAudioPlayerCard != null) {
                // Configurar el componente para la carpeta de alphabet
                reusableAudioPlayerCard!!.configure("audio_video/possesive_adjetives", alphabetText)
                Log.d(TAG, "Audio player configured successfully for alphabet folder")
            } else {
                Log.w(TAG, "ReusableAudioPlayerCard is null, skipping audio setup")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up audio player: " + e.message, e)
            Toast.makeText(
                this,
                "Error configurando reproductor de audio: " + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Return Menú
    private fun ReturnMapA1() {
        startActivity(Intent(this@PossessiveAdjectActivity, MenuA1Activity::class.java))
        //Toast.makeText(AlphabetActivity.this, "Has retornado correctamente.", Toast.LENGTH_SHORT).show();
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (reusableAudioPlayerCard != null) {
                reusableAudioPlayerCard!!.cleanup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up audio player: " + e.message, e)
        }
        Log.d(TAG, "Activity destroyed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }

    companion object {
        private const val TAG = "PossessiveAdjectActivity"
    }
}
