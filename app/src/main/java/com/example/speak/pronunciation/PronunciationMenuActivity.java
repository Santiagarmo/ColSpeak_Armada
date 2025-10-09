package com.example.speak.pronunciation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.speak.R;

public class PronunciationMenuActivity extends AppCompatActivity {
    private ImageButton btnA1;
    private ImageButton btnA2;
    private ImageButton btnB1;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pronunciation_menu);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnA1 = findViewById(R.id.btnA1);
        btnA2 = findViewById(R.id.btnA2);
        btnB1 = findViewById(R.id.btnB1);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void setupClickListeners() {
        btnA1.setOnClickListener(v -> {
            Intent intent = new Intent(PronunciationMenuActivity.this, TopicListActivity.class);
            intent.putExtra("LEVEL", "A1.1");
            startActivity(intent);
        });

        btnA2.setOnClickListener(v -> {
            Intent intent = new Intent(PronunciationMenuActivity.this, TopicListActivity.class);
            intent.putExtra("LEVEL", "A2.1");
            startActivity(intent);
        });

        btnB1.setOnClickListener(v -> {
            Intent intent = new Intent(PronunciationMenuActivity.this, TopicListActivity.class);
            intent.putExtra("LEVEL", "B1.1");
            startActivity(intent);
        });
    }
} 