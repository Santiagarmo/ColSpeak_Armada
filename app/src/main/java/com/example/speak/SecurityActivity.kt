package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.helpers.SecurityManager

class SecurityActivity : AppCompatActivity() {
    private var passwordInput: EditText? = null
    private var verifyButton: Button? = null
    private var configButton: Button? = null
    private var titleText: TextView? = null
    private var descriptionText: TextView? = null
    private var attemptsText: TextView? = null

    private var securityManager: SecurityManager? = null
    private var isDeviceVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)


        // Inicializar SecurityManager
        securityManager = SecurityManager(this)


        // Verificar si el dispositivo ya fue verificado
        checkDeviceVerification()

        if (isDeviceVerified) {
            // Dispositivo ya verificado, continuar a la aplicación
            proceedToApp()
        } else {
            // Configurar la interfaz de verificación
            setupSecurityInterface()
        }
    }

    private fun checkDeviceVerification() {
        isDeviceVerified = securityManager!!.isDeviceAuthorized()
    }

    private fun setupSecurityInterface() {
        passwordInput = findViewById<EditText>(R.id.password_input)
        verifyButton = findViewById<Button>(R.id.verify_button)
        configButton = findViewById<Button>(R.id.config_button)
        titleText = findViewById<TextView>(R.id.security_title)
        descriptionText = findViewById<TextView>(R.id.security_description)
        attemptsText = findViewById<TextView>(R.id.attempts_text)


        // Configurar texto explicativo
        titleText!!.setText("Verificación de Seguridad")
        descriptionText!!.setText(
            "Esta aplicación requiere verificación temporal antes de su lanzamiento oficial.\n\n" +
                    "Ingrese la contraseña de acceso temporal para continuar.\n\n" +
                    "Esta verificación solo se requiere una vez por dispositivo."
        )


        // Actualizar información de intentos
        updateAttemptsInfo()

        verifyButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                verifyPassword()
            }
        })

        configButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                openSecurityConfig()
            }
        })


        // Permitir verificación al presionar Enter
        passwordInput!!.setOnEditorActionListener(OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            verifyPassword()
            true
        })
    }

    private fun verifyPassword() {
        // Verificar si se han excedido los intentos máximos
        if (securityManager!!.hasExceededMaxAttempts()) {
            Toast.makeText(
                this,
                "Demasiados intentos fallidos. Intente más tarde.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val enteredPassword = passwordInput!!.getText().toString().trim { it <= ' ' }

        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (securityManager!!.verifyPassword(enteredPassword)) {
            // Contraseña correcta
            Toast.makeText(this, "Verificación exitosa", Toast.LENGTH_SHORT).show()
            proceedToApp()
        } else {
            // Contraseña incorrecta
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            passwordInput!!.setText("")
            passwordInput!!.requestFocus()


            // Actualizar información de intentos
            updateAttemptsInfo()
        }
    }

    private fun updateAttemptsInfo() {
        val attempts = securityManager!!.getVerificationAttempts()
        val maxAttempts = 5 // Debe coincidir con MAX_VERIFICATION_ATTEMPTS en SecurityManager

        if (attempts > 0) {
            attemptsText!!.setText(String.format("Intentos: %d/%d", attempts, maxAttempts))
            attemptsText!!.setVisibility(View.VISIBLE)
        } else {
            attemptsText!!.setVisibility(View.GONE)
        }
    }

    private fun proceedToApp() {
        // Ir al splash screen después de la autenticación exitosa
        val intent = Intent(this@SecurityActivity, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevenir salida de la aplicación sin verificación
        if (!isDeviceVerified) {
            Toast.makeText(
                this,
                "Debe completar la verificación para continuar",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            super.onBackPressed()
        }
    }

    private fun openSecurityConfig() {
        val intent = Intent(this@SecurityActivity, SecurityConfigActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "SecurityActivity"
    }
}
