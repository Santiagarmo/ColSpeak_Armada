package com.example.speak;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WordOrderActivity extends AppCompatActivity {
    private RecyclerView wordsRecyclerView;
    private WordAdapter wordAdapter;
    private TextView questionTextView;
    private TextView scoreTextView;
    private Button checkAnswerButton;
    private List<String> currentWords;
    private String correctAnswer;
    private int currentScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_order);

        // Inicializar vistas
        wordsRecyclerView = findViewById(R.id.wordsRecyclerView);
        questionTextView = findViewById(R.id.questionTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        checkAnswerButton = findViewById(R.id.checkAnswerButton);

        // Mostrar badge si FREE_ROAM está activo
        boolean freeRoam = getIntent().getBooleanExtra("FREE_ROAM", false);
        if (!freeRoam) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            freeRoam = prefs.getBoolean("FREE_ROAM", false);
        }
        if (freeRoam) {
            View container = findViewById(R.id.freeRoamContainer);
            if (container != null) container.setVisibility(View.VISIBLE);
            View btn = findViewById(R.id.btnDisableFreeRoam);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("FREE_ROAM", false).apply();
                    if (container != null) container.setVisibility(View.GONE);
                    Toast.makeText(this, "Modo libre desactivado", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // Configurar RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        wordsRecyclerView.setLayoutManager(layoutManager);
        wordAdapter = new WordAdapter();
        wordsRecyclerView.setAdapter(wordAdapter);

        // Configurar ItemTouchHelper para drag and drop
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | 
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                wordAdapter.moveItem(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No necesitamos implementar esto
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.5f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(wordsRecyclerView);

        // Configurar botón de verificación
        checkAnswerButton.setOnClickListener(v -> checkAnswer());

        // Cargar primera pregunta
        loadNewQuestion();
    }

    private void loadNewQuestion() {
        try {
            // Leer el archivo de preguntas
            InputStream is = getAssets().open("prayer_A1.1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> questions = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Q: ")) {
                    questions.add(line.substring(3));
                }
            }
            reader.close();

            // Seleccionar una pregunta aleatoria
            if (!questions.isEmpty()) {
                Random random = new Random();
                String question = questions.get(random.nextInt(questions.size()));
                correctAnswer = question;
                
                // Dividir la pregunta en palabras y mezclarlas
                currentWords = new ArrayList<>();
                String[] words = question.split("\\s+");
                Collections.addAll(currentWords, words);
                Collections.shuffle(currentWords);

                // Actualizar la UI
                wordAdapter.setWords(currentWords);
                questionTextView.setText("Ordena las palabras para formar una oración correcta");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar las preguntas", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAnswer() {
        StringBuilder userAnswer = new StringBuilder();
        for (String word : wordAdapter.getWords()) {
            userAnswer.append(word).append(" ");
        }
        String userAnswerStr = userAnswer.toString().trim();
        
        // Calcular similitud
        double similarity = calculateSimilarity(userAnswerStr, correctAnswer);
        currentScore = (int) (similarity * 100);
        
        // Actualizar puntuación
        scoreTextView.setText("Puntuación: " + currentScore + "%");
        
        // Mostrar resultado
        if (currentScore == 100) {
            Toast.makeText(this, "¡Correcto! Orden perfecto", Toast.LENGTH_SHORT).show();
            // Cargar nueva pregunta después de un breve delay
            wordsRecyclerView.postDelayed(this::loadNewQuestion, 1500);
        } else {
            Toast.makeText(this, "Intenta de nuevo. Similitud: " + currentScore + "%", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateSimilarity(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        if (s1.equals(s2)) return 1.0;
        
        // Contar palabras correctas en la posición correcta
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        int correctWords = 0;
        for (int i = 0; i < Math.min(words1.length, words2.length); i++) {
            if (words1[i].equals(words2[i])) {
                correctWords++;
            }
        }
        
        return (double) correctWords / Math.max(words1.length, words2.length);
    }

    private class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {
        private List<String> words = new ArrayList<>();

        public void setWords(List<String> words) {
            this.words = words;
            notifyDataSetChanged();
        }

        public List<String> getWords() {
            return words;
        }

        public void moveItem(int fromPos, int toPos) {
            if (fromPos < toPos) {
                for (int i = fromPos; i < toPos; i++) {
                    Collections.swap(words, i, i + 1);
                }
            } else {
                for (int i = fromPos; i > toPos; i--) {
                    Collections.swap(words, i, i - 1);
                }
            }
            notifyItemMoved(fromPos, toPos);
        }

        @NonNull
        @Override
        public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_word, parent, false);
            return new WordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
            holder.wordTextView.setText(words.get(position));
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class WordViewHolder extends RecyclerView.ViewHolder {
            TextView wordTextView;

            WordViewHolder(@NonNull View itemView) {
                super(itemView);
                wordTextView = itemView.findViewById(R.id.wordTextView);
            }
        }
    }
} 