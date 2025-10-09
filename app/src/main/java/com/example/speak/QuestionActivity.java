package com.example.speak;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class QuestionActivity extends AppCompatActivity {
    private CardView optionA, optionB, optionC;
    private MaterialButton nextButton;
    private TextView questionNumber, scoreText, questionText, questionType;
    private int currentScore = 100;
    private String selectedAnswer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        // Inicializar vistas
        initializeViews();
        // Configurar listeners
        setupListeners();
        // Actualizar UI
        updateUI();
    }

    private void initializeViews() {
        // Inicializar opciones
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);

        // Inicializar botón siguiente
        nextButton = findViewById(R.id.nextButton);

        // Inicializar textos
        questionNumber = findViewById(R.id.questionNumber);
        scoreText = findViewById(R.id.scoreText);
        questionText = findViewById(R.id.questionText);
        questionType = findViewById(R.id.questionType);
    }

    private void setupListeners() {
        // Listener para opción A
        optionA.setOnClickListener(v -> selectOption(optionA, "A"));

        // Listener para opción B
        optionB.setOnClickListener(v -> selectOption(optionB, "B"));

        // Listener para opción C
        optionC.setOnClickListener(v -> selectOption(optionC, "C"));

        // Listener para botón siguiente
        nextButton.setOnClickListener(v -> {
            if (selectedAnswer == null) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
                return;
            }
            // Aquí puedes agregar la lógica para verificar la respuesta
            // y pasar a la siguiente pregunta
            checkAnswer();
        });
    }

    private void selectOption(CardView selectedCard, String option) {
        // Deseleccionar todas las opciones
        optionA.setSelected(false);
        optionB.setSelected(false);
        optionC.setSelected(false);

        // Seleccionar la opción elegida
        selectedCard.setSelected(true);
        selectedAnswer = option;

        // Actualizar UI
        updateUI();
    }

    private void checkAnswer() {
        // Aquí puedes agregar la lógica para verificar si la respuesta es correcta
        // Por ahora, solo mostraremos un mensaje
        String correctAnswer = "B"; // La respuesta correcta es 10 dedos
        if (selectedAnswer.equals(correctAnswer)) {
            Toast.makeText(this, "Correct! Well done!", Toast.LENGTH_SHORT).show();
            currentScore += 10;
        } else {
            Toast.makeText(this, "Incorrect. The correct answer is B (10 fingers)", Toast.LENGTH_SHORT).show();
            currentScore -= 5;
        }
        updateUI();
    }

    private void updateUI() {
        // Actualizar puntuación
        scoreText.setText("Score: " + currentScore);

        // Actualizar estado de las opciones
        updateOptionState(optionA, "A");
        updateOptionState(optionB, "B");
        updateOptionState(optionC, "C");
    }

    private void updateOptionState(CardView option, String optionLetter) {
        if (option.isSelected()) {
            option.setCardElevation(8f);
            option.setAlpha(1f);
        } else {
            option.setCardElevation(4f);
            option.setAlpha(0.8f);
        }
    }
} 