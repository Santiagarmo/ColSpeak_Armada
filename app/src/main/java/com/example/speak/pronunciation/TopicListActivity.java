package com.example.speak.pronunciation;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.R;
import com.example.speak.database.DatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public class TopicListActivity extends AppCompatActivity {
    private ListView topicListView;
    private DatabaseHelper dbHelper;
    private String currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        // Obtener el nivel del intent
        currentLevel = getIntent().getStringExtra("LEVEL");
        if (currentLevel == null) {
            Toast.makeText(this, "Error: No se especificó el nivel", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Inicializar vistas
        topicListView = findViewById(R.id.topicListView);
        TextView levelTextView = findViewById(R.id.levelTextView);
        levelTextView.setText("Nivel: " + currentLevel);

        // Cargar temas
        loadTopics();
    }

    private void loadTopics() {
        Cursor cursor = dbHelper.getPronunciationTopics(currentLevel);
        List<String> topics = new ArrayList<>();
        final List<String> topicIds = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String topicId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC_ID));
                String topicName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC_NAME));
                String description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC_DESCRIPTION));
                
                topics.add(topicName + "\n" + description);
                topicIds.add(topicId);
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (topics.isEmpty()) {
            Toast.makeText(this, "No hay temas disponibles para este nivel", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, topics);
        topicListView.setAdapter(adapter);

        topicListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTopicId = topicIds.get(position);
            Intent intent = new Intent(TopicListActivity.this, TopicPronunciationActivity.class);
            intent.putExtra("TOPIC_ID", selectedTopicId);
            intent.putExtra("LEVEL", currentLevel);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
} 