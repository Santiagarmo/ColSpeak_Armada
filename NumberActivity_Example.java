// EJEMPLO DE IMPLEMENTACIÓN DEL COMPONENTE REUTILIZABLE EN NUMBERACTIVITY
// Este es un ejemplo de cómo migrar NumberActivity para usar ReusableAudioPlayerCard

package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.components.ReusableAudioPlayerCard;

public class NumberActivity extends AppCompatActivity {
    private static final String TAG = "NumberActivity";

    // Declaramos las variables
    private Button eButtonListening;
    private TextView numberTextView;
    private LinearLayout returnContainer;
    
    // Componente reutilizable de audio
    private ReusableAudioPlayerCard reusableAudioPlayerCard;
    
    // Texto de los números
    private String numbersText = "1 2 3 4 5 6 7 8 9 10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);

        // Inicializamos las variables
        initializeViews();
        setupClickListeners();
        setupAudioPlayer();

        // Return Menu
        returnContainer = findViewById(R.id.returnContainer);
        returnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMapA1();
            }
        });
    }

    private void initializeViews() {
        eButtonListening = findViewById(R.id.eButtonListening);
        numberTextView = findViewById(R.id.numberTextView); // Ajustar ID según el layout
        returnContainer = findViewById(R.id.returnContainer);
        reusableAudioPlayerCard = findViewById(R.id.reusableAudioPlayerCard);
    }

    private void setupClickListeners() {
        // Botón para empezar actividad
        eButtonListening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(NumberActivity.this, ListeningActivity.class);
                    intent.putExtra("TOPIC", "NUMBERS");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                    Log.d(TAG, "Starting ListeningActivity for NUMBERS");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting ListeningActivity", e);
                    Toast.makeText(NumberActivity.this, "Error opening listening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupAudioPlayer() {
        // Configurar el componente reutilizable de audio
        if (reusableAudioPlayerCard != null) {
            // Configurar para la carpeta de números
            reusableAudioPlayerCard.configure("audio_video/number", numbersText);
            Log.d(TAG, "Audio player configured for numbers folder");
        } else {
            Log.e(TAG, "ReusableAudioPlayerCard is null");
        }
    }

    // Return Map Listening
    private void ReturnMapA1() {
        try {
            Intent intent = new Intent(NumberActivity.this, MenuA1Activity.class);
            startActivity(intent);
            Toast.makeText(NumberActivity.this, "Has retornado al menú A1 correctamente.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Returned to MenuA1Activity");
        } catch (Exception e) {
            Log.e(TAG, "Error returning to MenuA1Activity", e);
            Toast.makeText(NumberActivity.this, "Error returning to menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos del componente de audio
        if (reusableAudioPlayerCard != null) {
            reusableAudioPlayerCard.cleanup();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar audio si está reproduciéndose
        if (reusableAudioPlayerCard != null && reusableAudioPlayerCard.isPlaying()) {
            // El componente manejará la pausa automáticamente
            Log.d(TAG, "Activity paused, audio will be handled by component");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
    }
}

/*
PASOS PARA MIGRAR NUMBERACTIVITY:

1. MODIFICAR EL LAYOUT (activity_number.xml):
   - Reemplazar todo el control de audio existente con:
   
   <com.example.speak.components.ReusableAudioPlayerCard
       android:id="@+id/reusableAudioPlayerCard"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:layout_marginHorizontal="16dp"
       android:layout_marginTop="8dp" />

2. SIMPLIFICAR LA ACTIVITY:
   - Eliminar todas las variables relacionadas con audio
   - Eliminar métodos de configuración de audio
   - Eliminar TextToSpeech y MediaPlayer
   - Agregar configuración del componente reutilizable

3. AGREGAR LIMPIEZA:
   - Llamar a cleanup() en onDestroy()

4. CONFIGURAR CARPETA:
   - Usar "audio_video/number" como carpeta de assets
   - Configurar texto apropiado para números

VENTAJAS DE LA MIGRACIÓN:
- ✅ Código más limpio y mantenible
- ✅ Funcionalidad consistente entre actividades
- ✅ Menos código duplicado
- ✅ Mejor manejo de recursos
- ✅ Interfaz moderna y compacta
*/
