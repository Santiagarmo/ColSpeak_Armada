package com.example.speak

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.database.DatabaseHelper

class MajorActivity : AppCompatActivity() {
    //Declaramos las variables
    private lateinit var eButtonLogin: Button
    private lateinit var eButtonLogin2: Button
    private lateinit var buttonGuestLogin: Button
    private lateinit var textRegister: TextView
    private lateinit var textRegister2: TextView
    private lateinit var textAppDescription: TextView
    private lateinit var textLearnAnywhere: TextView
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_major)

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(this)

        //Inicializamos las variables
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        try {
            eButtonLogin = findViewById(R.id.eButtonLogin)
            eButtonLogin2 = findViewById(R.id.eButtonLogin2)
            buttonGuestLogin = findViewById(R.id.button_guest_login)
            textRegister = findViewById(R.id.textRegister)
            textRegister2 = findViewById(R.id.textRegister2)
            textAppDescription = findViewById(R.id.textAppDescription)
            textLearnAnywhere = findViewById(R.id.textLearnAnywhere)

            // Aplicar formato HTML al texto de descripción
            val htmlText = "Una app de <b>aprendizaje offline y online</b>"
            textAppDescription.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)

            // Aplicar formato al texto "Aprende donde quieras y cuando quieras."
            val fullText = "Aprende donde quieras y cuando quieras."
            val spannableString = SpannableString(fullText)

            // "Aprende donde quieras" - posición 0 a 22 - color #173446
            val startFirst = 0
            val endFirst = 22 // "Aprende donde quieras"
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#173446")),
                startFirst,
                endFirst,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // "y cuando quieras." - posición 22 a 41 - color #008CB9 y Bold
            val startSecond = 22
            val endSecond = fullText.length
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#008CB9")),
                startSecond,
                endSecond,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                startSecond,
                endSecond,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            textLearnAnywhere.text = spannableString
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        //Configuramos el botón de inicio de sesión
        eButtonLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        //Configuramos el botón de invitado
        buttonGuestLogin.setOnClickListener {
            handleGuestLogin()
        }

        //Configuramos el texto de registro
        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //Configuramos el segundo botón de inicio de sesión (misma funcionalidad)
        eButtonLogin2.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        //Configuramos el segundo texto de registro (misma funcionalidad)
        textRegister2.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleGuestLogin() {
        // Obtener el ID del dispositivo
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        if (deviceId != null) {
            // Verificar si ya existe un usuario invitado
            if (dbHelper.isGuestUserExists(deviceId)) {
                // Usuario invitado existe, obtener sus datos
                val userId = dbHelper.getGuestUserId(deviceId)
                if (userId != -1L) {
                    // Guardar el ID del usuario en SharedPreferences
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().apply {
                        putLong("user_id", userId)
                        putString("device_id", deviceId)
                        putBoolean("is_guest", true)
                        apply()
                    }

                    // Ir al menú principal
                    startActivity(Intent(this, TermsConditions::class.java))
                    finish()
                }
            } else {
                // Crear nuevo usuario invitado
                val userId = dbHelper.createGuestUser(deviceId)
                if (userId != -1L) {
                    // Guardar el ID del usuario en SharedPreferences
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().apply {
                        putLong("user_id", userId)
                        putString("device_id", deviceId)
                        putBoolean("is_guest", true)
                        apply()
                    }

                    // Ir al menú principal
                    startActivity(Intent(this, TermsConditions::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error al crear usuario invitado", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Error al obtener ID del dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
