package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
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
    private Button buttonGuestLogin;
    private TextView textRegister;
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
            buttonGuestLogin = findViewById(R.id.button_guest_login);
            textRegister = findViewById(R.id.textRegister);
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
