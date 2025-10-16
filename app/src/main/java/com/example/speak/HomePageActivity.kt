package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.quiz.QuizActivity

class HomePageActivity : AppCompatActivity() {
    //Declaramos las variables
    private var eButtonBegin: Button? = null
    private var eButtonTest: Button? = null

    //Return Terminos
    private var returnContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // NUEVA FUNCIONALIDAD: Verificar si el usuario ya tiene progreso
        if (hasUserProgress()) {
            // Si tiene progreso, ir directamente al mapa apropiado
            redirectToLastActivity()
            return  // Salir del onCreate sin mostrar la pantalla de inicio
        }

        //Inicializamos las variables
        initializeViews()
        setupClickListeners()

        //Return Menu

        //Return Menu
        returnContainer = findViewById<LinearLayout>(R.id.returnContainer)
        returnContainer!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })
    }

    /**
     * Verifica si el usuario ya tiene progreso en alguna actividad
     * @return true si tiene progreso, false si es la primera vez
     */
    private fun hasUserProgress(): Boolean {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Lista de claves de progreso para verificar
        val progressKeys = arrayOf<String?>( // Listening topics
            "PASSED_ALPHABET", "PASSED_NUMBERS", "PASSED_COLORS",
            "PASSED_PERSONAL_PRONOUNS", "PASSED_POSSESSIVE_ADJECTIVES",
            "PASSED_PREPOSITIONS_OF_PLACE", "PASSED_ADJECTIVES",
            "PASSED_GUESS_PICTURE", "PASSED_LISTEN_GUESS_IMAGE",  // Speaking/Pronunciation topics

            "PASSED_PRON_ALPHABET", "PASSED_PRON_NUMBERS", "PASSED_PRON_COLORS",
            "PASSED_PRON_PERSONAL_PRONOUNS", "PASSED_PRON_POSSESSIVE_ADJECTIVES",
            "PASSED_PRON_PREPOSITIONS_OF_PLACE", "PASSED_PRON_ADJECTIVES",  // Other activities

            "PASSED_LEVEL_A1_1", "PASSED_LEVEL_A1_2", "PASSED_LEVEL_A2_1"
        )


        // Verificar si alguna clave tiene progreso
        for (key in progressKeys) {
            if (prefs.getBoolean(key, false)) {
                Log.d("HomePageActivity", "Progreso encontrado: " + key)
                return true // Tiene progreso
            }
        }

        Log.d("HomePageActivity", "No se encontró progreso previo")
        return false // No tiene progreso
    }

    /**
     * Redirige al usuario al mapa o actividad apropiada basándose en su progreso
     */
    private fun redirectToLastActivity() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)

        Log.d("HomePageActivity", "Redirigiendo usuario con progreso existente...")


        // Determinar qué mapa mostrar basándose en el progreso
        // 1. Verificar si tiene progreso en Speaking (más específico)
        val hasSpeakingProgress = prefs.getBoolean("PASSED_PRON_ALPHABET", false) ||
                prefs.getBoolean("PASSED_PRON_NUMBERS", false) ||
                prefs.getBoolean("PASSED_PRON_COLORS", false) ||
                prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)


        // 2. Verificar si tiene progreso en Listening
        val hasListeningProgress = prefs.getBoolean("PASSED_ALPHABET", false) ||
                prefs.getBoolean("PASSED_NUMBERS", false) ||
                prefs.getBoolean("PASSED_COLORS", false) ||
                prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)

        val intent: Intent?

        if (hasSpeakingProgress) {
            // Si tiene progreso en Speaking, ir al mapa de speaking
            Log.d("HomePageActivity", "Redirigiendo a Speaking map")
            intent = Intent(this, MenuSpeakingActivity::class.java)
        } else if (hasListeningProgress) {
            // Si tiene progreso en Listening, ir al mapa principal
            Log.d("HomePageActivity", "Redirigiendo a Listening map")
            intent = Intent(this, MenuA1Activity::class.java)
        } else {
            // Fallback: ir al mapa principal
            Log.d("HomePageActivity", "Redirigiendo a mapa principal (fallback)")
            intent = Intent(this, MenuA1Activity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Cerrar HomePageActivity para que no se pueda volver
    }

    private fun initializeViews() {
        try {
            eButtonBegin = findViewById<Button>(R.id.eButtonBegin)
            eButtonTest = findViewById<Button>(R.id.eButtonTest)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        //Configuramos el botón de start
        eButtonBegin!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@HomePageActivity, MenuA1Activity::class.java)
                startActivity(intent)
            }
        })

        eButtonTest!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@HomePageActivity, QuizActivity::class.java)
                startActivity(intent)
            }
        })
    }

    //Return Menú
    private fun ReturnMenu() {
        startActivity(Intent(this@HomePageActivity, TermsConditions::class.java))
        Toast.makeText(this@HomePageActivity, "Has retornado correctamente.", Toast.LENGTH_SHORT)
            .show()
    }
}
