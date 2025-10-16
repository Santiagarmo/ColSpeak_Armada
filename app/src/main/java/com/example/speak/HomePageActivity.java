package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.quiz.QuizActivity;

public class HomePageActivity extends AppCompatActivity {

    //Declaramos las variables
    private Button eButtonBegin;
    private Button eButtonTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // NUEVA FUNCIONALIDAD: Verificar si el usuario ya tiene progreso
        if (hasUserProgress()) {
            // Si tiene progreso, ir directamente al mapa apropiado
            redirectToLastActivity();
            return; // Salir del onCreate sin mostrar la pantalla de inicio
        }

        //Inicializamos las variables
        initializeViews();
        setupClickListeners();
    }

    /**
     * Verifica si el usuario ya tiene progreso en alguna actividad
     * @return true si tiene progreso, false si es la primera vez
     */
    private boolean hasUserProgress() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Lista de claves de progreso para verificar
        String[] progressKeys = {
            // Listening topics
            "PASSED_ALPHABET", "PASSED_NUMBERS", "PASSED_COLORS",
            "PASSED_PERSONAL_PRONOUNS", "PASSED_POSSESSIVE_ADJECTIVES",
            "PASSED_PREPOSITIONS_OF_PLACE", "PASSED_ADJECTIVES",
            "PASSED_GUESS_PICTURE", "PASSED_LISTEN_GUESS_IMAGE",
            
            // Speaking/Pronunciation topics
            "PASSED_PRON_ALPHABET", "PASSED_PRON_NUMBERS", "PASSED_PRON_COLORS",
            "PASSED_PRON_PERSONAL_PRONOUNS", "PASSED_PRON_POSSESSIVE_ADJECTIVES",
            "PASSED_PRON_PREPOSITIONS_OF_PLACE", "PASSED_PRON_ADJECTIVES",
            
            // Other activities
            "PASSED_LEVEL_A1_1", "PASSED_LEVEL_A1_2", "PASSED_LEVEL_A2_1"
        };
        
        // Verificar si alguna clave tiene progreso
        for (String key : progressKeys) {
            if (prefs.getBoolean(key, false)) {
                android.util.Log.d("HomePageActivity", "Progreso encontrado: " + key);
                return true; // Tiene progreso
            }
        }
        
        android.util.Log.d("HomePageActivity", "No se encontró progreso previo");
        return false; // No tiene progreso
    }

    /**
     * Redirige al usuario al mapa o actividad apropiada basándose en su progreso
     */
    private void redirectToLastActivity() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        android.util.Log.d("HomePageActivity", "Redirigiendo usuario con progreso existente...");
        
        // Determinar qué mapa mostrar basándose en el progreso
        // 1. Verificar si tiene progreso en Speaking (más específico)
        boolean hasSpeakingProgress = prefs.getBoolean("PASSED_PRON_ALPHABET", false) ||
                                     prefs.getBoolean("PASSED_PRON_NUMBERS", false) ||
                                     prefs.getBoolean("PASSED_PRON_COLORS", false) ||
                                     prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false);
        
        // 2. Verificar si tiene progreso en Listening
        boolean hasListeningProgress = prefs.getBoolean("PASSED_ALPHABET", false) ||
                                      prefs.getBoolean("PASSED_NUMBERS", false) ||
                                      prefs.getBoolean("PASSED_COLORS", false) ||
                                      prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false);
        
        Intent intent;
        
        if (hasSpeakingProgress) {
            // Si tiene progreso en Speaking, ir al mapa de speaking
            android.util.Log.d("HomePageActivity", "Redirigiendo a Speaking map");
            intent = new Intent(this, MenuSpeakingActivity.class);
        } else if (hasListeningProgress) {
            // Si tiene progreso en Listening, ir al mapa principal
            android.util.Log.d("HomePageActivity", "Redirigiendo a Listening map");
            intent = new Intent(this, MenuA1Activity.class);
        } else {
            // Fallback: ir al mapa principal
            android.util.Log.d("HomePageActivity", "Redirigiendo a mapa principal (fallback)");
            intent = new Intent(this, MenuA1Activity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Cerrar HomePageActivity para que no se pueda volver
    }

    private void initializeViews() {
        try {
            eButtonBegin = findViewById(R.id.eButtonBegin);
            eButtonTest = findViewById(R.id.eButtonTest);

        } catch (Exception e) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        //Configuramos el botón de start
        eButtonBegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, MenuA1Activity.class);
                startActivity(intent);
            }
        });

        eButtonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, QuizActivity.class);
                startActivity(intent);
            }
        });

    }

}
