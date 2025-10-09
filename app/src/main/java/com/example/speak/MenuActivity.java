package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.speak.pronunciation.TopicListActivity;

public class MenuActivity extends AppCompatActivity {
    private ImageButton btnListening;
    private ImageButton btnPronunciation;
    private ImageButton btnWriting;
    private ImageButton btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnListening = findViewById(R.id.btnListening);
        btnPronunciation = findViewById(R.id.btnPronunciation);
        btnWriting = findViewById(R.id.btnWriting);
        btnProfile = findViewById(R.id.btnProfile);
    }

    private void setupClickListeners() {
        btnListening.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ListeningActivity.class);
            startActivity(intent);
        });

        btnPronunciation.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, TopicListActivity.class);
            startActivity(intent);
        });

        btnWriting.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, WritingActivity.class);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }
} 