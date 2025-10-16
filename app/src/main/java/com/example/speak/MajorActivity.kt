package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.example.speak.database.DatabaseHelper;

public class MajorActivity extends AppCompatActivity {

    //Declaramos las variables
    private Button eButtonLogin;
    private Button eButtonLogin2;
    private Button buttonGuestLogin;
    private TextView textRegister;
    private TextView textRegister2;
    private TextView textAppDescription;
    private TextView textLearnAnywhere;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_major);

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        //Inicializamos las variables
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        try {
            eButtonLogin = findViewById(R.id.eButtonLogin);
            eButtonLogin2 = findViewById(R.id.eButtonLogin2);
            buttonGuestLogin = findViewById(R.id.button_guest_login);
            textRegister = findViewById(R.id.textRegister);
            textRegister2 = findViewById(R.id.textRegister2);
            textAppDescription = findViewById(R.id.textAppDescription);
            textLearnAnywhere = findViewById(R.id.textLearnAnywhere);

            // Aplicar formato HTML al texto de descripción
            if (textAppDescription != null) {
                String htmlText = "Una app de <b>aprendizaje offline y online</b>";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    textAppDescription.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY));
                } else {
                    textAppDescription.setText(android.text.Html.fromHtml(htmlText));
                }
            }

            // Aplicar formato al texto "Aprende donde quieras y cuando quieras."
            if (textLearnAnywhere != null) {
                String fullText = "Aprende donde quieras y cuando quieras.";
                SpannableString spannableString = new SpannableString(fullText);

                // "Aprende donde quieras" - posición 0 a 22 - color #173446
                int startFirst = 0;
                int endFirst = 22; // "Aprende donde quieras"
                spannableString.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#173446")),
                    startFirst,
                    endFirst,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // "y cuando quieras." - posición 22 a 41 - color #008CB9 y Bold
                int startSecond = 22;
                int endSecond = fullText.length();
                spannableString.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#008CB9")),
                    startSecond,
                    endSecond,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannableString.setSpan(
                    new StyleSpan(android.graphics.Typeface.BOLD),
                    startSecond,
                    endSecond,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                textLearnAnywhere.setText(spannableString);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        //Configuramos el botón de inicio de sesión
        eButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MajorActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        //Configuramos el botón de invitado
        buttonGuestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGuestLogin();
            }
        });

        //Configuramos el texto de registro
        textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MajorActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        //Configuramos el segundo botón de inicio de sesión (misma funcionalidad)
        eButtonLogin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MajorActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        //Configuramos el segundo texto de registro (misma funcionalidad)
        textRegister2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MajorActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleGuestLogin() {
        // Obtener el ID del dispositivo
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        if (deviceId != null) {
            // Verificar si ya existe un usuario invitado
            if (dbHelper.isGuestUserExists(deviceId)) {
                // Usuario invitado existe, obtener sus datos
                long userId = dbHelper.getGuestUserId(deviceId);
                if (userId != -1) {
                    // Guardar el ID del usuario en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("user_id", userId);
                    editor.putString("device_id", deviceId);
                    editor.putBoolean("is_guest", true);
                    editor.apply();

                    // Ir al menú principal
                    startActivity(new Intent(MajorActivity.this, TermsConditions.class));
                    finish();
                }
            } else {
                // Crear nuevo usuario invitado
                long userId = dbHelper.createGuestUser(deviceId);
                if (userId != -1) {
                    // Guardar el ID del usuario en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("user_id", userId);
                    editor.putString("device_id", deviceId);
                    editor.putBoolean("is_guest", true);
                    editor.apply();

                    // Ir al menú principal
                    startActivity(new Intent(MajorActivity.this, TermsConditions.class));
                    finish();
                } else {
                    Toast.makeText(this, "Error al crear usuario invitado", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Error al obtener ID del dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
