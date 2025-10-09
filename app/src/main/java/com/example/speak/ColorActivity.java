package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import android.content.res.ColorStateList;
import androidx.core.view.ViewCompat;
import androidx.core.content.ContextCompat;

import com.example.speak.components.ReusableAudioPlayerCard;

public class ColorActivity extends AppCompatActivity {
    private static final String TAG = "ColorActivity";

    //Control de Audio reutilizable
    private ReusableAudioPlayerCard reusableAudioPlayerCard;

    //Declaramos las variables
    private Button eButtonListening;
    private TextView alphabetTextView;

    // Reproductor de audio


    //Return Map Listening
    private LinearLayout returnContainer;

    // TextToSpeech
    private String alphabetText = "Yellow   Blue   Red   Orange   Violet   Green   Pink   Brown   Gold   Gray   White   Black";

    // Control de reproducción

    // Posición actual en milisegundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);

        //Inicializamos las variables
        initializeViews();
        setupClickListeners();
        setupAudioPlayer();

        //Return Menu
        returnContainer = findViewById(R.id.returnContainer);
        returnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMapA1();
            }
        });
    }

    private void initializeViews() {
        try {
            eButtonListening = findViewById(R.id.eButtonListening);
            alphabetTextView = findViewById(R.id.alphabetTextView);


            // Reproductor de audio

            reusableAudioPlayerCard = findViewById(R.id.reusableAudioPlayerCard);

        } catch (Exception e) {
            //Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        //Configuramos el botón de start
        eButtonListening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ColorActivity.this, ListeningActivity.class);
                intent.putExtra("TOPIC", "COLORS");
                intent.putExtra("LEVEL", "A1.1");
                startActivity(intent);
            }
        });
    }

    private void setupAudioPlayer() {
        try {
            if (reusableAudioPlayerCard != null) {
                // Configurar el componente para la carpeta de alphabet
                reusableAudioPlayerCard.configure("audio_video/color", alphabetText);
                Log.d(TAG, "Audio player configured successfully for alphabet folder");
            } else {
                Log.w(TAG, "ReusableAudioPlayerCard is null, skipping audio setup");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up audio player: " + e.getMessage(), e);
            Toast.makeText(this, "Error configurando reproductor de audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Return Menú
    private void ReturnMapA1() {
        startActivity(new Intent(ColorActivity.this, MenuA1Activity.class));
        //Toast.makeText(AlphabetActivity.this, "Has retornado correctamente.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (reusableAudioPlayerCard != null) {
                reusableAudioPlayerCard.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up audio player: " + e.getMessage(), e);
        }
        Log.d(TAG, "Activity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
    }
}
