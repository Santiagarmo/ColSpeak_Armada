package com.example.speak.quiz;

// Importación de clases necesarias para operaciones con ficheros y logs
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuestionGenerator {
    private static final String TAG = "QuestionGenerator"; // Etiqueta para logs
    private final Context context; // Contexto de la aplicación
    private final List<Question> questions; // Lista de todas las preguntas cargadas
    private final Random random; // Generador aleatorio para mezclar preguntas

    // Clase interna que representa una pregunta
    public static class Question {
        private final String question; // Texto de la pregunta
        private final String correctAnswer; // Respuesta correcta
        private final List<String> options; // Lista de opciones de respuesta
        private final String topic; // Tema de la pregunta
        private final String level; // Nivel de la pregunta

        // Constructor para inicializar todos los campos de una pregunta
        public Question(String question, String correctAnswer, List<String> options, String topic, String level) {
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.options = options;
            this.topic = topic;
            this.level = level;
        }

        // Getters para acceder a los datos de la pregunta
        public String getQuestion() { return question; }
        public String getCorrectAnswer() { return correctAnswer; }
        public List<String> getOptions() { return options; }
        public String getTopic() { return topic; }
        public String getLevel() { return level; }
    }

    // Constructor del generador, recibe el contexto y carga las preguntas al inicializar
    public QuestionGenerator(Context context) {
        this.context = context;
        this.questions = new ArrayList<>();
        this.random = new Random();
        loadQuestions(); // Llama al método que carga las preguntas desde un archivo
    }

    // Método privado para cargar las preguntas desde un archivo de texto plano
    private void loadQuestions() {
        try {
            // Abre el archivo ubicado en la carpeta assets
            InputStream inputStream = context.getAssets().open("PLACEMENT_TEST_ A1_TO_B1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String currentQuestion = null;
            String currentAnswer = null;
            List<String> currentOptions = null;
            String currentTopic = null;
            String currentLevel = null;

            // Lee el archivo línea por línea
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Elimina espacios en blanco
                
                if (line.startsWith("Topic:")) {
                    // Si ya había una pregunta previa, la añade a la lista
                    if (currentQuestion != null && currentAnswer != null && currentOptions != null) {
                        questions.add(new Question(currentQuestion, currentAnswer, currentOptions, 
                            currentTopic != null ? currentTopic : "General", 
                            currentLevel != null ? currentLevel : "A1.1"));
                    }
                    // Iniciar nueva pregunta
                    currentTopic = line.substring(6).trim(); // Quita el prefijo "Topic:"
                    currentQuestion = null;
                    currentAnswer = null;
                    currentOptions = null;
                    currentLevel = null;
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim(); // Quita el prefijo "Level:"
                } else if (line.startsWith("Q:")) {
                    currentQuestion = line.substring(2).trim(); // Quita el prefijo "Q:"
                } else if (line.startsWith("A:")) {
                    // Manejar formato A:a), A:b), etc.
                    String answerLine = line.substring(2).trim(); // Quita el prefijo "A:"
                    if (answerLine.contains(")")) {
                        // Formato A:a) It's -> extraer solo "It's"
                        int parenthesisIndex = answerLine.indexOf(")");
                        if (parenthesisIndex != -1 && parenthesisIndex + 1 < answerLine.length()) {
                            currentAnswer = answerLine.substring(parenthesisIndex + 1).trim();
                        } else {
                            currentAnswer = answerLine;
                        }
                    } else {
                        currentAnswer = answerLine;
                    }
                } else if (line.startsWith("O:")) {
                    // Divide las opciones separadas por '|'
                    String optionsLine = line.substring(2).trim(); // Quita el prefijo "O:"
                    String[] options = optionsLine.split("\\|");
                    currentOptions = new ArrayList<>();
                    for (String option : options) {
                        // Manejar formato a) It's, b) Are, etc.
                        String cleanOption = option.trim();
                        if (cleanOption.contains(")")) {
                            int parenthesisIndex = cleanOption.indexOf(")");
                            if (parenthesisIndex != -1 && parenthesisIndex + 1 < cleanOption.length()) {
                                cleanOption = cleanOption.substring(parenthesisIndex + 1).trim();
                            }
                        }
                        currentOptions.add(cleanOption);
                    }
                }
            }

            // Añade la última pregunta si estaba completa
            if (currentQuestion != null && currentAnswer != null && currentOptions != null) {
                questions.add(new Question(currentQuestion, currentAnswer, currentOptions, 
                    currentTopic != null ? currentTopic : "General", 
                    currentLevel != null ? currentLevel : "A1.1"));
            }
            
            reader.close(); // Cierra el lector
            
            // Log para verificar que se cargaron las preguntas
            Log.d(TAG, "Total preguntas cargadas: " + questions.size());
            for (int i = 0; i < Math.min(3, questions.size()); i++) {
                Question q = questions.get(i);
                Log.d(TAG, "Pregunta " + (i+1) + ": " + q.getQuestion());
                Log.d(TAG, "Respuesta correcta: " + q.getCorrectAnswer());
                Log.d(TAG, "Opciones: " + q.getOptions());
                Log.d(TAG, "Tema: " + q.getTopic());
                Log.d(TAG, "Nivel: " + q.getLevel());
            }
            
        } catch (IOException e) {
            // En caso de error al leer el archivo
            Log.e(TAG, "Error loading questions file", e);
        }
    }

    // Genera una lista de preguntas aleatorias, con una cantidad determinada
    public List<Question> generateQuestions(int count) {
        if (questions.isEmpty()) {
            Log.e(TAG, "No hay preguntas cargadas");
            return new ArrayList<>(); // Devuelve lista vacía si no hay preguntas
        }
        
        List<Question> selectedQuestions = new ArrayList<>();
        List<Question> shuffledQuestions = new ArrayList<>(questions);

        // Mezcla las preguntas con aleatoriedad
        Collections.shuffle(shuffledQuestions, random);
        
        // Seleccionar el número solicitado de preguntas
        // Añade el número de preguntas solicitado
        for (int i = 0; i < count && i < shuffledQuestions.size(); i++) {
            selectedQuestions.add(shuffledQuestions.get(i));
        }
        
        return selectedQuestions; // Devuelve la lista final
    }

    // Obtener todas las preguntas cargadas
    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }
} 