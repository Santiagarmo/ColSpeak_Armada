package com.example.speak;

import android.app.ProgressDialog;
//Intent: Permite la navegación entre actividades.
import android.content.Intent;
//Bundle: Se usa en el onCreate para recuperar el estado de la actividad.
import android.os.Bundle;
import android.util.Patterns;
//View: Representa la interfaz gráfica.
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
//Toast: Muestra mensajes emergentes en pantalla.
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
//Extiende AppCompatActivity, lo que permite utilizar componentes modernos de Android y mejorar la compatibilidad con versiones antiguas.
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;


import com.google.firebase.auth.AuthResult;
//FirebaseAuth: Maneja la autenticación de usuarios con Firebase.
import com.google.firebase.auth.FirebaseAuth;
//FirebaseUser: Representa al usuario autenticado.
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DatabaseReference;

//FirebaseDatabase: Permite la conexión con la base de datos en tiempo real de Firebase.
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashMap;

//SharedPreferences: Se usa para almacenar datos persistentes, como el estado de sesión.
import android.content.SharedPreferences;

//ConnectivityManager y NetworkInfo: Se utilizan para verificar la conexión a Internet.
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//Log: Sirve para depurar con mensajes en la consola.
import android.util.Log;

//MaterialButton: Botón con estilo de Material Design.
import com.google.android.material.button.MaterialButton;

//TextInputEditText y TextInputLayout: Mejoran los campos de entrada de texto con diseño moderno y validaciones.
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

//DatabaseHelper: Clase personalizada que maneja la base de datos SQLite para la autenticación offline.
import com.example.speak.database.DatabaseHelper;

import com.example.speak.quiz.QuizActivity;

import android.provider.Settings;
import android.database.Cursor;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    //We declare the variables

    //Login
    //EditText eLoginEmail, eLoginPassword;
    //Button eButtonLogin;
    //Login

    //Firebase
    //FirebaseAuth eAuth;
    FirebaseDatabase eDatase;

    ProgressDialog eProgress;

    //Variables de la Clase
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText eLoginEmail, eLoginPassword;
    private MaterialButton eButtonLogin;
    private TextView etextRegister;
    private View progressBar;
    private FirebaseAuth eAuth;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private String userEmail;

    //Método onCreate(Bundle savedInstanceState)
    //Configura la vista con el layout XML activity_login.xml
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Instantiate

        //Login
        //Asociación de Vistas con XML
        //Se enlazan los elementos de la UI con el código
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        eLoginEmail = findViewById(R.id.LoginEmail);
        eLoginPassword = findViewById(R.id.loginPassword);
        eButtonLogin = findViewById(R.id.btnLoginPlay);
        etextRegister = findViewById(R.id.textRegister);
        progressBar = findViewById(R.id.progressBar);
        //Login

        // Inicializar Firebase Auth
        //Inicialización de Componentes
        //Se inicializan Firebase Auth, la base de datos local y las preferencias compartidas
        eAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE);


        eDatase = FirebaseDatabase.getInstance();

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
        eButtonLogin.setOnClickListener(v -> attemptLogin());

        etextRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        eProgress = new ProgressDialog( LoginActivity.this);
        eProgress.setMessage("Procesando datos...");
        eProgress.setCancelable(false);

    }

    //Método attemptLogin()
    private void attemptLogin() {
        String email = eLoginEmail.getText().toString().trim();
        String password = eLoginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            emailInputLayout.setError("El correo electrónico es requerido");
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("La contraseña es requerida");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Ingrese un correo electrónico válido");
            return;
        }

        if (password.length() < 6) {
            passwordInputLayout.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        setLoading(true);

        if (isNetworkAvailable()) {
            loginOnline(email, password);
        } else {
            loginOffline(email, password);
        }
    }

    //Método isNetworkAvailable()
    //Comprueba si hay conexión a Internet.
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Método loginOnline
    private void loginOnline(String email, String password) {
        //Autentica con Firebase.
        eAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    //Si la autenticación es exitosa, guarda el estado de sesión.
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso en Firebase
                        FirebaseUser user = eAuth.getCurrentUser();
                        if (user != null) {
                            // Guardar estado de inicio de sesión
                            saveLoginState(user.getUid(), email);

                            // Verificar si el usuario existe en la base de datos local (SQLite)
                            if (!dbHelper.userExists(email)) {
                                // Si no existe, guardarlo localmente (SQLite)
                                long localUserId = dbHelper.saveUser(email, password);
                                if (localUserId != -1) {
                                    dbHelper.markUserAsSynced(email);
                                }
                            }

                            // Quiz
                            startQuizActivity(email);

                            // Iniciar MainActivity
                            startMainActivity();
                        } else {
                            showError("Error al obtener información del usuario");
                            setLoading(false);
                        }
                    } else {
                        // Si falla el inicio de sesión en línea, intentar offline
                        Log.e(TAG, "Error de inicio de sesión: " + task.getException());
                        loginOffline(email, password);
                    }
                });
    }

    //Método loginOffline(String email, String password)
    //Verifica si el usuario está en SQLite y permite el inicio de sesión offline.
    private void loginOffline(String email, String password) {
        // Verificar credenciales en la base de datos local
        if (dbHelper.checkUser(email, password)) {
            // Obtener el ID del usuario local
            long userId = dbHelper.getUserId(email);
            if (userId != -1) {
                // Guardar estado de inicio de sesión
                saveLoginState(String.valueOf(userId), email);
                
                // Actualizar modo offline en la base de datos
                dbHelper.setUserOfflineMode(email, true);

                // Quiz
                startQuizActivity(email);

                // Iniciar MainActivity
                startMainActivity();
            } else {
                showError("Error al obtener el ID del usuario");
            }
        } else {
            showError("Credenciales incorrectas");
        }
    }

    //Método saveLoginState(String userId, String email)
    //Guarda datos de sesión en SharedPreferences
    private void saveLoginState(String userId, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("user_email", email);
        editor.putString("user_id", userId);
        editor.putBoolean("is_offline", !isNetworkAvailable());
        editor.putLong("last_sync", System.currentTimeMillis());
        editor.apply();
    }


    //Su objetivo es iniciar MainActivity después de que un usuario haya iniciado sesión.
    private void startMainActivity() {
        //Crea un Intent, que es un objeto que indica que queremos cambiar de Activity.
        //LoginActivity.this: Es el contexto actual (LoginActivity).
        //MainActivity.class: Es la actividad de destino.
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        //FLAG_ACTIVITY_NEW_TASK: Indica que la nueva Activity debe iniciarse como una nueva tarea.
        //FLAG_ACTIVITY_CLEAR_TASK: Elimina todas las actividades previas de la tarea actual, evitando que el usuario pueda regresar a LoginActivity con el botón "atrás".
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //Ejecuta el intent, iniciando MainActivity y finalizando LoginActivity.
        startActivity(intent);
    }

    //Quiz
    private void startQuizActivity(String email) {
        // Guardamos email en SharedPreferences para seguridad extra
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, QuizActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish(); // Opcional: para cerrar LoginActivity
    }


    //Este método se encarga de mostrar un mensaje de error al usuario y detener cualquier indicación de carga en la pantalla.
    private void showError(String message) {
        //Toast es una pequeña notificación emergente en Android que aparece en la parte inferior de la pantalla con un mensaje temporal.
        //para mostrarlo con la duración corta
        //this: Hace referencia a la actividad actual.
        //message: Es el texto que se mostrará en la notificación.
        //Toast.LENGTH_SHORT: Define la duración del mensaje (puede ser LENGTH_SHORT o LENGTH_LONG).
        //.show(): Muestra el mensaje en pantalla.
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        //Se llama el método setLoading(false);, que probablemente oculta un ProgressBar o algún indicador de carga.
        setLoading(false);
    }

    //Este método se usa para gestionar la interfaz de usuario mientras se está realizando el inicio de sesión.
    private void setLoading(boolean isLoading) {
        //Mostrar u ocultar la barra de progreso (progressBar)
        //Si isLoading es true, muestra la progressBar (View.VISIBLE).
        //Si isLoading es false, la oculta (View.GONE).
        //La progressBar sirve para indicar visualmente que una acción está en proceso, en este caso, el inicio de sesión.
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        //Habilitar o deshabilitar
        //Cuando isLoading es true, se deshabilitan los botones (setEnabled(false)).
        //Cuando isLoading es false, se vuelven a habilitar (setEnabled(true)).
        //Esto evita que el usuario presione los botones varias veces mientras el inicio de sesión está en proceso.
        eButtonLogin.setEnabled(!isLoading);

    }

    //Este método cierra la sesión del usuario y limpia cualquier rastro de su sesión en la app.
    public void clearLoginState() {
        // Limpiar Firebase Auth
        eAuth.signOut();

        // Limpiar SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.remove("user_email");
        editor.remove("is_logged_in");
        editor.apply();

        // Limpiar campos de entrada
        eLoginEmail.setText("");
        eLoginPassword.setText("");

        // Limpiar errores
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
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

    private void handleGuestLogin() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        if (dbHelper.isGuestUserExists(deviceId)) {
            // Usuario invitado ya existe, iniciar sesión
            Cursor cursor = dbHelper.getGuestUser(deviceId);
            if (cursor.moveToFirst()) {
                String email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL));
                String password = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
                loginOffline(email, password);
            }
            cursor.close();
        } else {
            // Crear nuevo usuario invitado
            long userId = dbHelper.createGuestUser(deviceId);
            if (userId != -1) {
                Cursor cursor = dbHelper.getGuestUser(deviceId);
                if (cursor.moveToFirst()) {
                    String email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL));
                    String password = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
                    loginOffline(email, password);
                }
                cursor.close();
            } else {
                showError("Error al crear usuario invitado");
            }
        }
    }

}