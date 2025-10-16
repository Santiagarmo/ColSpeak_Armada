package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speak.database.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TermsConditions : AppCompatActivity() {
    //Firebase validation
    var eUser: FirebaseUser? = null
    var eAuth: FirebaseAuth? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null
    private var dbHelper: DatabaseHelper? = null

    //Aceptación de terminos y condiciones
    private var eButtonAccept: Button? = null

    //End session
    private var eButtonEnd: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(com.example.speak.R.layout.term_condition)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(android.R.id.content),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        //Firebase
        eAuth = FirebaseAuth.getInstance()
        eUser = eAuth!!.getCurrentUser()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference("ColSpeak")

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // NUEVA FUNCIONALIDAD: Verificar si el usuario ya tiene progreso
        if (hasUserProgress()) {
            // Si tiene progreso, ir directamente al mapa apropiado
            redirectToLastActivity()
            return  // Salir del onCreate sin mostrar términos y condiciones
        }

        // Inicializar y configurar el botón de aceptar
        eButtonAccept = findViewById<Button>(com.example.speak.R.id.btnAccept)
        eButtonAccept!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // Verificar si es usuario invitado
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val isGuest = prefs.getBoolean("is_guest", false)

                if (isGuest) {
                    // Si es invitado, ir directamente al perfil
                    val intent = Intent(this@TermsConditions, HomePageActivity::class.java)
                    startActivity(intent)
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        val intent = Intent(this@TermsConditions, HomePageActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@TermsConditions,
                            "Por favor inicia sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@TermsConditions, LoginActivity::class.java))
                    }
                }
            }
        })

        // Lock the "Back" button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Leave empty to lock the back button
            }
        })

        //Log out
        eButtonEnd = findViewById<LinearLayout>(com.example.speak.R.id.btnEndSession)
        eButtonEnd!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                EndSession()
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
                Log.d("TermsConditions", "Progreso encontrado: " + key)
                return true // Tiene progreso
            }
        }

        Log.d("TermsConditions", "No se encontró progreso previo")
        return false // No tiene progreso
    }

    /**
     * Redirige al usuario al mapa o actividad apropiada basándose en su progreso
     */
    private fun redirectToLastActivity() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)

        Log.d("TermsConditions", "Redirigiendo usuario con progreso existente...")


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
            Log.d("TermsConditions", "Redirigiendo a Speaking map")
            intent = Intent(this, MenuSpeakingActivity::class.java)
        } else if (hasListeningProgress) {
            // Si tiene progreso en Listening, ir al mapa principal
            Log.d("TermsConditions", "Redirigiendo a Listening map")
            intent = Intent(this, MenuA1Activity::class.java)
        } else {
            // Fallback: ir al mapa principal
            Log.d("TermsConditions", "Redirigiendo a mapa principal (fallback)")
            intent = Intent(this, MenuA1Activity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Cerrar TermsConditions para que no se pueda volver
    }

    //We close the user session
    private fun EndSession() {
        eAuth!!.signOut()
        startActivity(Intent(this@TermsConditions, MajorActivity::class.java))
        Toast.makeText(
            this@TermsConditions,
            "Has finalizado sesión correctamente.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
