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

class AlphabetActivity : AppCompatActivity() {
    private var eButtonListening: Button? = null
    private var alphabetTextView: TextView? = null
    private var returnContainer: LinearLayout? = null
    private var reusableAudioPlayerCard: ReusableAudioPlayerCard? = null
    private val alphabetText = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alphabet)

        initializeViews()
        setupClickListeners()
        setupAudioPlayer()

        returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
        returnContainer!!.setOnClickListener(View.OnClickListener { v: View? -> ReturnMapA1() })
    }

    private fun initializeViews() {
        try {
            eButtonListening = findViewById<Button>(R.id.eButtonListening)
            alphabetTextView = findViewById<TextView?>(R.id.alphabetTextView)
            returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
            reusableAudioPlayerCard =
                findViewById<ReusableAudioPlayerCard?>(R.id.reusableAudioPlayerCard)
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: " + e.message, e)
            Toast.makeText(this, "Error inicializando vista: " + e.message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun setupClickListeners() {
        eButtonListening!!.setOnClickListener(View.OnClickListener { v: View? ->
            try {
                val intent = Intent(this@AlphabetActivity, ListeningActivity::class.java)
                intent.putExtra("TOPIC", "ALPHABET")
                intent.putExtra("LEVEL", "A1.1")
                startActivity(intent)
                Log.d(TAG, "Starting ListeningActivity for ALPHABET")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting ListeningActivity", e)
                Toast.makeText(
                    this@AlphabetActivity,
                    "Error opening listening activity: " + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupAudioPlayer() {
        try {
            if (reusableAudioPlayerCard != null) {
                // Configurar el componente para la carpeta de alphabet
                reusableAudioPlayerCard!!.configure("audio_video/alphabet", alphabetText)
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

    private fun ReturnMapA1() {
        try {
            val intent = Intent(this@AlphabetActivity, MenuA1Activity::class.java)
            startActivity(intent)
            Toast.makeText(
                this@AlphabetActivity,
                "Has retornado al menú A1 correctamente.",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "Returned to MenuA1Activity")
        } catch (e: Exception) {
            Log.e(TAG, "Error returning to MenuA1Activity", e)
            Toast.makeText(
                this@AlphabetActivity,
                "Error returning to menu: " + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
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
        private const val TAG = "AlphabetActivity"
    }
}
