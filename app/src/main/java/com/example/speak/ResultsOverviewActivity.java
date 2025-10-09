package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.helpers.TrophyHelper;

public class ResultsOverviewActivity extends AppCompatActivity {
    
    private static final String TAG = "ResultsOverviewActivity";
    
    private TextView totalPoints;
    private TextView totalTrophies;
    private ProgressBar weeklyProgress;
    private TextView weeklyProgressText;
    private Button btnContinue;
    private LinearLayout btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_overview);
        
        initializeViews();
        setupClickListeners();
        loadResults();
        
        Log.d(TAG, "Vista de resultados creada");
    }
    
    private void initializeViews() {
        totalPoints = findViewById(R.id.totalPoints);
        totalTrophies = findViewById(R.id.totalTrophies);
        weeklyProgress = findViewById(R.id.weeklyProgress);
        weeklyProgressText = findViewById(R.id.weeklyProgressText);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Regresar al menú principal
                Intent intent = new Intent(ResultsOverviewActivity.this, MajorActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
    
    private void loadResults() {
        // Obtener datos usando TrophyHelper
        int trophies = TrophyHelper.getTrophyCount(this);
        int stars = TrophyHelper.getStarCount(this);
        
        // Calcular puntos totales (cada estrella = 10 puntos, cada trofeo = 50 puntos)
        int totalPointsValue = (stars * 10) + (trophies * 50);
        
        // Actualizar UI
        totalPoints.setText(String.valueOf(totalPointsValue));
        totalTrophies.setText(String.valueOf(trophies));
        
        // Simular progreso semanal (puedes implementar lógica real aquí)
        int weeklySessions = Math.min(stars, 7); // Máximo 7 sesiones por semana
        weeklyProgress.setMax(7);
        weeklyProgress.setProgress(weeklySessions);
        weeklyProgressText.setText(weeklySessions + "/7");
        
        Log.d(TAG, "Resultados cargados - Puntos: " + totalPointsValue + ", Trofeos: " + trophies + ", Estrellas: " + stars);
    }
}

