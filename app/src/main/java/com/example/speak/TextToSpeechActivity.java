package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class TextToSpeechActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = "TextToSpeechActivity";
    private TextToSpeech textToSpeech;
    private EditText textInput;
    private Button speakButton;
    private Button stopButton;
    private SeekBar speedSeekBar;
    private SeekBar pitchSeekBar;
    private TextView speedValue;
    private TextView pitchValue;
    private float speed = 1.0f;
    private float pitch = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_speech);

        // Inicializar vistas
        textInput = findViewById(R.id.textInput);
        speakButton = findViewById(R.id.speakButton);
        stopButton = findViewById(R.id.stopButton);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedValue = findViewById(R.id.speedValue);
        pitchValue = findViewById(R.id.pitchValue);

        // Inicializar TextToSpeech en modo offline
        textToSpeech = new TextToSpeech(this, this, "com.google.android.tts");

        // Configurar botones
        speakButton.setOnClickListener(v -> speak());
        stopButton.setOnClickListener(v -> stopSpeaking());

        // Configurar SeekBars
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress / 100.0f;
                speedValue.setText(String.format("%.1fx", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pitch = progress / 100.0f;
                pitchValue.setText(String.format("%.1fx", pitch));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Configurar listener de progreso
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> {
                    speakButton.setEnabled(false);
                    stopButton.setEnabled(true);
                });
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    speakButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    speakButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    Toast.makeText(TextToSpeechActivity.this, 
                        "Error al pronunciar el texto", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Verificar si el motor TTS está disponible en modo offline
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma no soportado o datos faltantes");
                Toast.makeText(this, 
                    "Por favor, instala el motor de voz en inglés desde la configuración del sistema", 
                    Toast.LENGTH_LONG).show();
                // Abrir configuración de TTS
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            } else {
                // Configurar para modo offline
                textToSpeech.setSpeechRate(speed);
                textToSpeech.setPitch(pitch);
                speakButton.setEnabled(true);
            }
        } else {
            Log.e(TAG, "Inicialización fallida");
            Toast.makeText(this, "Error al inicializar el motor de voz", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak() {
        String text = textInput.getText().toString();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Por favor ingresa un texto", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configurar parámetros para modo offline
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts1");

        textToSpeech.setSpeechRate(speed);
        textToSpeech.setPitch(pitch);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts1");
    }

    private void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            speakButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
} 