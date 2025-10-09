package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.speak.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TermsConditions extends AppCompatActivity {

    //Firebase validation
    FirebaseUser eUser;
    FirebaseAuth eAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private DatabaseHelper dbHelper;

    //Aceptación de terminos y condiciones
    private Button eButtonAccept;

    //End session
    private LinearLayout eButtonEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.term_condition);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Firebase
        eAuth = FirebaseAuth.getInstance();
        eUser = eAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("ColSpeak");

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // NUEVA FUNCIONALIDAD: Verificar si el usuario ya tiene progreso
        if (hasUserProgress()) {
            // Si tiene progreso, ir directamente al mapa apropiado
            redirectToLastActivity();
            return; // Salir del onCreate sin mostrar términos y condiciones
        }

        // Inicializar y configurar el botón de aceptar
        eButtonAccept = findViewById(R.id.btnAccept);
        eButtonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si es usuario invitado
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean isGuest = prefs.getBoolean("is_guest", false);

                if (isGuest) {
                    // Si es invitado, ir directamente al perfil
                    Intent intent = new Intent(TermsConditions.this, HomePageActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(TermsConditions.this, HomePageActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(TermsConditions.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TermsConditions.this, LoginActivity.class));
                    }
                }
            }


        });

        // Lock the "Back" button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Leave empty to lock the back button
            }
        });

        //Log out
        eButtonEnd = findViewById(R.id.btnEndSession);
        eButtonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EndSession();
            }
        });
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
                android.util.Log.d("TermsConditions", "Progreso encontrado: " + key);
                return true; // Tiene progreso
            }
        }
        
        android.util.Log.d("TermsConditions", "No se encontró progreso previo");
        return false; // No tiene progreso
    }

    /**
     * Redirige al usuario al mapa o actividad apropiada basándose en su progreso
     */
    private void redirectToLastActivity() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        android.util.Log.d("TermsConditions", "Redirigiendo usuario con progreso existente...");
        
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
            android.util.Log.d("TermsConditions", "Redirigiendo a Speaking map");
            intent = new Intent(this, MenuSpeakingActivity.class);
        } else if (hasListeningProgress) {
            // Si tiene progreso en Listening, ir al mapa principal
            android.util.Log.d("TermsConditions", "Redirigiendo a Listening map");
            intent = new Intent(this, MenuA1Activity.class);
        } else {
            // Fallback: ir al mapa principal
            android.util.Log.d("TermsConditions", "Redirigiendo a mapa principal (fallback)");
            intent = new Intent(this, MenuA1Activity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Cerrar TermsConditions para que no se pueda volver
    }

    //We close the user session
    private void EndSession() {
        eAuth.signOut();
        startActivity(new Intent(TermsConditions.this, MajorActivity.class));
        Toast.makeText(TermsConditions.this, "Has finalizado sesión correctamente.", Toast.LENGTH_SHORT).show();
    }

}
