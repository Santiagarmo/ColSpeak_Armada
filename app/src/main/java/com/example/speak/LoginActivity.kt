package com.example.speak

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speak.database.DatabaseHelper
import com.example.speak.quiz.QuizActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

//Intent: Permite la navegación entre actividades.
//Bundle: Se usa en el onCreate para recuperar el estado de la actividad.
//View: Representa la interfaz gráfica.
//Toast: Muestra mensajes emergentes en pantalla.
//Extiende AppCompatActivity, lo que permite utilizar componentes modernos de Android y mejorar la compatibilidad con versiones antiguas.
//FirebaseAuth: Maneja la autenticación de usuarios con Firebase.
//FirebaseUser: Representa al usuario autenticado.
//FirebaseDatabase: Permite la conexión con la base de datos en tiempo real de Firebase.
//SharedPreferences: Se usa para almacenar datos persistentes, como el estado de sesión.
//ConnectivityManager y NetworkInfo: Se utilizan para verificar la conexión a Internet.
//Log: Sirve para depurar con mensajes en la consola.
//MaterialButton: Botón con estilo de Material Design.
//TextInputEditText y TextInputLayout: Mejoran los campos de entrada de texto con diseño moderno y validaciones.
//DatabaseHelper: Clase personalizada que maneja la base de datos SQLite para la autenticación offline.
class LoginActivity : AppCompatActivity() {
    //We declare the variables
    //Login
    //EditText eLoginEmail, eLoginPassword;
    //Button eButtonLogin;
    //Login
    //Firebase
    //FirebaseAuth eAuth;
    var eDatase: FirebaseDatabase? = null

    var eProgress: ProgressDialog? = null

    //Variables de la Clase
    private var emailInputLayout: TextInputLayout? = null
    private var passwordInputLayout: TextInputLayout? = null
    private var eLoginEmail: TextInputEditText? = null
    private var eLoginPassword: TextInputEditText? = null
    private var eButtonLogin: MaterialButton? = null
    private var etextRegister: TextView? = null
    private var progressBar: View? = null
    private var eAuth: FirebaseAuth? = null
    private var dbHelper: DatabaseHelper? = null
    private var prefs: SharedPreferences? = null

    private val userEmail: String? = null

    //Método onCreate(Bundle savedInstanceState)
    //Configura la vista con el layout XML activity_login.xml
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        //Instantiate

        //Login
        //Asociación de Vistas con XML
        //Se enlazan los elementos de la UI con el código
        emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        eLoginEmail = findViewById<TextInputEditText>(R.id.LoginEmail)
        eLoginPassword = findViewById<TextInputEditText>(R.id.loginPassword)
        eButtonLogin = findViewById<MaterialButton>(R.id.btnLoginPlay)
        etextRegister = findViewById<TextView>(R.id.textRegister)
        progressBar = findViewById<View>(R.id.progressBar)

        //Login

        // Inicializar Firebase Auth
        //Inicialización de Componentes
        //Se inicializan Firebase Auth, la base de datos local y las preferencias compartidas
        eAuth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)
        prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE)


        eDatase = FirebaseDatabase.getInstance()

        //Click Event Listener to the button
        //In the button we are going to pass the string

        //Login
        /*eButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = eLoginEmail.getText().toString();
                String password = eLoginPassword.getText().toString();
                //LoginUser(email, password);
                login();
            }
        });*/
        //Login

        //Listener para el boton
        //loginButton: Llama a attemptLogin(), que inicia sesión
        eButtonLogin!!.setOnClickListener(View.OnClickListener { v: View? -> attemptLogin() })

        etextRegister!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                startActivity(intent)
                //finish();
            }
        })

        eProgress = ProgressDialog(this@LoginActivity)
        eProgress!!.setMessage("Procesando datos...")
        eProgress!!.setCancelable(false)
    }

    //Método attemptLogin()
    private fun attemptLogin() {
        val email = eLoginEmail!!.getText().toString().trim { it <= ' ' }
        val password = eLoginPassword!!.getText().toString().trim { it <= ' ' }

        if (email.isEmpty()) {
            emailInputLayout!!.setError("El correo electrónico es requerido")
            return
        }

        if (password.isEmpty()) {
            passwordInputLayout!!.setError("La contraseña es requerida")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout!!.setError("Ingrese un correo electrónico válido")
            return
        }

        if (password.length < 6) {
            passwordInputLayout!!.setError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        emailInputLayout!!.setError(null)
        passwordInputLayout!!.setError(null)
        setLoading(true)

        if (this.isNetworkAvailable) {
            loginOnline(email, password)
        } else {
            loginOffline(email, password)
        }
    }

    private val isNetworkAvailable: Boolean
        //Método isNetworkAvailable()
        get() {
            val connectivityManager =
                getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
            return activeNetworkInfo != null && activeNetworkInfo.isConnected()
        }

    //Método loginOnline
    private fun loginOnline(email: String, password: String) {
        //Autentica con Firebase.
        eAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, OnCompleteListener { task: Task<AuthResult?>? ->
                //Si la autenticación es exitosa, guarda el estado de sesión.
                if (task!!.isSuccessful()) {
                    // Inicio de sesión exitoso en Firebase
                    val user = eAuth!!.getCurrentUser()
                    if (user != null) {
                        // Guardar estado de inicio de sesión
                        saveLoginState(user.getUid(), email)

                        // Verificar si el usuario existe en la base de datos local (SQLite)
                        if (!dbHelper!!.userExists(email)) {
                            // Si no existe, guardarlo localmente (SQLite)
                            val localUserId = dbHelper!!.saveUser(email, password)
                            if (localUserId != -1L) {
                                dbHelper!!.markUserAsSynced(email)
                            }
                        }

                        // Quiz
                        startQuizActivity(email)

                        // Iniciar MainActivity
                        startMainActivity()
                    } else {
                        showError("Error al obtener información del usuario")
                        setLoading(false)
                    }
                } else {
                    // Si falla el inicio de sesión en línea, intentar offline
                    Log.e(TAG, "Error de inicio de sesión: " + task.getException())
                    loginOffline(email, password)
                }
            })
    }

    //Método loginOffline(String email, String password)
    //Verifica si el usuario está en SQLite y permite el inicio de sesión offline.
    private fun loginOffline(email: String?, password: String?) {
        // Verificar credenciales en la base de datos local
        if (dbHelper!!.checkUser(email, password)) {
            // Obtener el ID del usuario local
            val userId = dbHelper!!.getUserId(email)
            if (userId != -1L) {
                // Guardar estado de inicio de sesión
                saveLoginState(userId.toString(), email)


                // Actualizar modo offline en la base de datos
                dbHelper!!.setUserOfflineMode(email, true)

                // Quiz
                startQuizActivity(email)

                // Iniciar MainActivity
                startMainActivity()
            } else {
                showError("Error al obtener el ID del usuario")
            }
        } else {
            showError("Credenciales incorrectas")
        }
    }

    //Método saveLoginState(String userId, String email)
    //Guarda datos de sesión en SharedPreferences
    private fun saveLoginState(userId: String?, email: String?) {
        val editor = prefs!!.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.putString("user_id", userId)
        editor.putBoolean("is_offline", !this.isNetworkAvailable)
        editor.putLong("last_sync", System.currentTimeMillis())
        editor.apply()
    }


    //Su objetivo es iniciar MainActivity después de que un usuario haya iniciado sesión.
    private fun startMainActivity() {
        //Crea un Intent, que es un objeto que indica que queremos cambiar de Activity.
        //LoginActivity.this: Es el contexto actual (LoginActivity).
        //MainActivity.class: Es la actividad de destino.
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        //FLAG_ACTIVITY_NEW_TASK: Indica que la nueva Activity debe iniciarse como una nueva tarea.
        //FLAG_ACTIVITY_CLEAR_TASK: Elimina todas las actividades previas de la tarea actual, evitando que el usuario pueda regresar a LoginActivity con el botón "atrás".
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        //Ejecuta el intent, iniciando MainActivity y finalizando LoginActivity.
        startActivity(intent)
    }

    //Quiz
    private fun startQuizActivity(email: String?) {
        // Guardamos email en SharedPreferences para seguridad extra
        val editor = prefs!!.edit()
        editor.putString("user_email", email)
        editor.apply()

        val intent = Intent(this@LoginActivity, QuizActivity::class.java)
        intent.putExtra("email", email)
        startActivity(intent)
        finish() // Opcional: para cerrar LoginActivity
    }


    //Este método se encarga de mostrar un mensaje de error al usuario y detener cualquier indicación de carga en la pantalla.
    private fun showError(message: String?) {
        //Toast es una pequeña notificación emergente en Android que aparece en la parte inferior de la pantalla con un mensaje temporal.
        //para mostrarlo con la duración corta
        //this: Hace referencia a la actividad actual.
        //message: Es el texto que se mostrará en la notificación.
        //Toast.LENGTH_SHORT: Define la duración del mensaje (puede ser LENGTH_SHORT o LENGTH_LONG).
        //.show(): Muestra el mensaje en pantalla.
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        //Se llama el método setLoading(false);, que probablemente oculta un ProgressBar o algún indicador de carga.
        setLoading(false)
    }

    //Este método se usa para gestionar la interfaz de usuario mientras se está realizando el inicio de sesión.
    private fun setLoading(isLoading: Boolean) {
        //Mostrar u ocultar la barra de progreso (progressBar)
        //Si isLoading es true, muestra la progressBar (View.VISIBLE).
        //Si isLoading es false, la oculta (View.GONE).
        //La progressBar sirve para indicar visualmente que una acción está en proceso, en este caso, el inicio de sesión.
        progressBar!!.setVisibility(if (isLoading) View.VISIBLE else View.GONE)
        //Habilitar o deshabilitar
        //Cuando isLoading es true, se deshabilitan los botones (setEnabled(false)).
        //Cuando isLoading es false, se vuelven a habilitar (setEnabled(true)).
        //Esto evita que el usuario presione los botones varias veces mientras el inicio de sesión está en proceso.
        eButtonLogin!!.setEnabled(!isLoading)
    }

    //Este método cierra la sesión del usuario y limpia cualquier rastro de su sesión en la app.
    fun clearLoginState() {
        // Limpiar Firebase Auth
        eAuth!!.signOut()

        // Limpiar SharedPreferences
        val editor = prefs!!.edit()
        editor.remove("user_id")
        editor.remove("user_email")
        editor.remove("is_logged_in")
        editor.apply()

        // Limpiar campos de entrada
        eLoginEmail!!.setText("")
        eLoginPassword!!.setText("")

        // Limpiar errores
        emailInputLayout!!.setError(null)
        passwordInputLayout!!.setError(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
    }

    /*
    private void login() {
        String email = eLoginEmail.getText().toString();
        String password = eLoginPassword.getText().toString();

        if (!email.isEmpty() && !password.isEmpty()) {
            if (password.length() >= 6) {
                eProgress.show();
                //Validation if you fill in the email and password fields
                eAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    eProgress.dismiss();
                                    //Let's say that FirebaseUser eUser, validate it as a recurring user
                                    FirebaseUser eUser = eAuth.getCurrentUser();
                                    //We add a counter and leave it at 0, it is to leave an error if the user cannot access
                                    int counter = 0;
                                    //Error if user is not null
                                    assert eUser != null;

                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    Toast.makeText(LoginActivity.this, "Acceso correcto.", Toast.LENGTH_SHORT).show();
                                    finish();

                                } else {
                                    eProgress.dismiss();
                                    Toast.makeText(LoginActivity.this, "Acceso incorrecto.", Toast.LENGTH_SHORT).show();

                                }
                            }
                        })

                        //In case user registration fails, to get the error
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText( LoginActivity.this,"Es necesario ingresar tanto la contraseña como el correo electrónico", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText( LoginActivity.this,"Es necesario ingresar tanto la contraseña como el correo electrónico", Toast.LENGTH_SHORT).show();
        }
    }
     */
    //Login
    /*
    private void LoginUser(String email, String password) {
        eProgress.show();

        //Validation if the email and password fields are not filled in
        if (email.isEmpty() && password.isEmpty()){
            Toast.makeText( LoginActivity.this, "Introduce tus datos para acceder.", Toast.LENGTH_SHORT).show();
            eProgress.dismiss();
        } else {

            //Validation if you fill in the email and password fields
            eAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                eProgress.dismiss();
                                //Let's say that FirebaseUser eUser, validate it as a recurring user
                                FirebaseUser eUser = eAuth.getCurrentUser();
                                //We add a counter and leave it at 0, it is to leave an error if the user cannot access
                                int counter = 0;
                                //Error if user is not null
                                assert eUser != null;

                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                Toast.makeText(LoginActivity.this, "Acceso correcto.", Toast.LENGTH_SHORT).show();
                                finish();

                            } else {
                                eProgress.dismiss();
                                Toast.makeText(LoginActivity.this, "Acceso incorrecto.", Toast.LENGTH_SHORT).show();

                            }
                        }
                    })

                    //In case user registration fails, to get the error
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }


    }
     */
    //Login
    private fun handleGuestLogin() {
        val deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)

        if (dbHelper!!.isGuestUserExists(deviceId)) {
            // Usuario invitado ya existe, iniciar sesión
            val cursor = dbHelper!!.getGuestUser(deviceId)
            if (cursor.moveToFirst()) {
                val email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))
                val password =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD))
                loginOffline(email, password)
            }
            cursor.close()
        } else {
            // Crear nuevo usuario invitado
            val userId = dbHelper!!.createGuestUser(deviceId)
            if (userId != -1L) {
                val cursor = dbHelper!!.getGuestUser(deviceId)
                if (cursor.moveToFirst()) {
                    val email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))
                    val password =
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD))
                    loginOffline(email, password)
                }
                cursor.close()
            } else {
                showError("Error al crear usuario invitado")
            }
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}