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

public class AlphabetActivity extends AppCompatActivity {
    private static final String TAG = "AlphabetActivity";

    private Button eButtonListening;
    private TextView alphabetTextView;
    private LinearLayout returnContainer;
    private ReusableAudioPlayerCard reusableAudioPlayerCard;
    private String alphabetText = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alphabet);

        initializeViews();
        setupClickListeners();
        setupAudioPlayer();

        returnContainer = findViewById(R.id.returnContainer);
        returnContainer.setOnClickListener(v -> ReturnMapA1());
    }

    private void initializeViews() {
        try {
            eButtonListening = findViewById(R.id.eButtonListening);
            alphabetTextView = findViewById(R.id.alphabetTextView);
            returnContainer = findViewById(R.id.returnContainer);
            reusableAudioPlayerCard = findViewById(R.id.reusableAudioPlayerCard);
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error inicializando vista: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        eButtonListening.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AlphabetActivity.this, ListeningActivity.class);
                intent.putExtra("TOPIC", "ALPHABET");
                intent.putExtra("LEVEL", "A1.1");
                startActivity(intent);
                Log.d(TAG, "Starting ListeningActivity for ALPHABET");
            } catch (Exception e) {
                Log.e(TAG, "Error starting ListeningActivity", e);
                Toast.makeText(AlphabetActivity.this, "Error opening listening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAudioPlayer() {
        try {
            if (reusableAudioPlayerCard != null) {
                // Configurar el componente para la carpeta de alphabet
                reusableAudioPlayerCard.configure("audio_video/alphabet", alphabetText);
                Log.d(TAG, "Audio player configured successfully for alphabet folder");
            } else {
                Log.w(TAG, "ReusableAudioPlayerCard is null, skipping audio setup");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up audio player: " + e.getMessage(), e);
            Toast.makeText(this, "Error configurando reproductor de audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void ReturnMapA1() {
        try {
            Intent intent = new Intent(AlphabetActivity.this, MenuA1Activity.class);
            startActivity(intent);
            Toast.makeText(AlphabetActivity.this, "Has retornado al menú A1 correctamente.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Returned to MenuA1Activity");
        } catch (Exception e) {
            Log.e(TAG, "Error returning to MenuA1Activity", e);
            Toast.makeText(AlphabetActivity.this, "Error returning to menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
