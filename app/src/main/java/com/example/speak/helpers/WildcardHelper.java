package com.example.speak.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Html;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.speak.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper class para manejar el sistema de comodines y ayudas en todos los módulos
 * IMPORTANTE: Las ayudas NO afectan la evaluación final del usuario
 */
public class WildcardHelper {
    
    private static final String TAG = "WildcardHelper";
    private static final String WILDCARD_PREFS = "wildcard_prefs";
    private static final String WILDCARD_COUNT_KEY = "wildcard_count_";
    private static final String CHANGED_QUESTIONS_KEY = "changed_questions_";
    
    private final Context context;
    private final String moduleName;
    private final String topicName;
    private final int maxWildcards = 5;
    private int remainingWildcards;
    private VideoHelper videoHelper;
    
    // Callbacks para las diferentes ayudas
    public interface WildcardCallbacks {
        void onChangeQuestion();
        void onShowContentImage();
        void onShowInstructorVideo();
        void onShowFiftyFifty();
        void onShowCreativeHelp();
        void onShowWildcardInfo(); // Nueva opción para mostrar información
    }
    
    private WildcardCallbacks callbacks;
    
    public WildcardHelper(Context context, String moduleName, String topicName) {
        this.context = context;
        this.moduleName = moduleName;
        this.topicName = topicName;
        this.remainingWildcards = getRemainingWildcards();
        this.videoHelper = new VideoHelper(context);
    }
    
    public void setCallbacks(WildcardCallbacks callbacks) {
        this.callbacks = callbacks;
    }
    
    /**
     * Muestra el menú de comodines
     */
    public void showWildcardMenu() {
        if (remainingWildcards <= 0) {
            Toast.makeText(context, "No tienes más comodines disponibles", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_wildcard_menu, null);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseWildcardModal);

        // Configurar título y contador
        TextView titleText = dialogView.findViewById(R.id.wildcardTitle);
        TextView countText = dialogView.findViewById(R.id.wildcardCount);
        
        titleText.setText("Comodines");
        countText.setText("Disponibles: " + remainingWildcards);
        
        // Configurar botones de ayuda
        setupHelpButtons(dialogView);
        
        builder.setView(dialogView);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
    }
    
    /**
     * Configura los botones de ayuda
     */
    private void setupHelpButtons(View dialogView) {
        // Ayuda 1: Cambiar pregunta
        Button help1Btn = dialogView.findViewById(R.id.help1Button);

        String htmlText1 ="🔄 Cambiar pregunta <b>/ Change question</b>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            help1Btn.setText(Html.fromHtml(htmlText1, Html.FROM_HTML_MODE_LEGACY));
        } else {
            help1Btn.setText(Html.fromHtml(htmlText1));
        }

        // 👇 Forzar fondo blanco y texto negro desde código
        help1Btn.setBackgroundResource(R.drawable.button_rounded);
        help1Btn.setBackgroundTintList(null); // Elimina el tinte que pone el tema
        help1Btn.setTextColor(ContextCompat.getColor(context, R.color.black));

        help1Btn.setOnClickListener(v -> {
            if (useWildcard()) {
                if (callbacks != null) {
                    callbacks.onChangeQuestion();
                }
                Toast.makeText(context, "Ayuda 1: Cambiando pregunta...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ayuda 2: Mostrar imagen de contenido
        Button help2Btn = dialogView.findViewById(R.id.help2Button);

        String htmlText2 ="🖼️ Ver imagen de contenido <b>/ Look content picture</b>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            help2Btn.setText(Html.fromHtml(htmlText2, Html.FROM_HTML_MODE_LEGACY));
        } else {
            help2Btn.setText(Html.fromHtml(htmlText2));
        }

        // 👇 Forzar fondo blanco y texto negro desde código
        help2Btn.setBackgroundResource(R.drawable.button_rounded);
        help2Btn.setBackgroundTintList(null); // Elimina el tinte que pone el tema
        help2Btn.setTextColor(ContextCompat.getColor(context, R.color.black));

        help2Btn.setOnClickListener(v -> {
            if (useWildcard()) {
                if (callbacks != null) {
                    callbacks.onShowContentImage();
                }
                Toast.makeText(context, "Ayuda 2: Mostrando imagen de contenido...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ayuda 3: Ver video del instructor
        Button help3Btn = dialogView.findViewById(R.id.help3Button);

        String htmlText = "🎥 Video instructivo <b>/ Instruction video</b>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            help3Btn.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        } else {
            help3Btn.setText(Html.fromHtml(htmlText));
        }

        // 👇 Forzar fondo blanco y texto negro desde código
        help3Btn.setBackgroundResource(R.drawable.button_rounded);
        help3Btn.setBackgroundTintList(null); // Elimina el tinte que pone el tema
        help3Btn.setTextColor(ContextCompat.getColor(context, R.color.black));

        help3Btn.setOnClickListener(v -> {
            if (useWildcard()) {
                if (callbacks != null) {
                    callbacks.onShowInstructorVideo();
                }
                Toast.makeText(context, "Ayuda 3: Mostrando video del instructor...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ayuda 4: 50/50 - Mostrar solo 2 opciones
        Button help4Btn = dialogView.findViewById(R.id.help4Button);

        String htmlText4 ="🎯 50% a 50% <b>/ Two options</b>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            help4Btn.setText(Html.fromHtml(htmlText4, Html.FROM_HTML_MODE_LEGACY));
        } else {
            help4Btn.setText(Html.fromHtml(htmlText4));
        }

        help4Btn.setBackgroundResource(R.drawable.button_rounded);
        help4Btn.setBackgroundTintList(null); // Elimina el tinte que pone el tema
        help4Btn.setTextColor(ContextCompat.getColor(context, R.color.black));

        help4Btn.setOnClickListener(v -> {
            if (useWildcard()) {
                if (callbacks != null) {
                    callbacks.onShowFiftyFifty();
                }
                Toast.makeText(context, "Ayuda 4: Aplicando 50/50...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ayuda 5: Ayuda creativa
        Button help5Btn = dialogView.findViewById(R.id.help5Button);

        String htmlText5 ="💡 Ayuda creativa <b>/ Creative help</b>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            help5Btn.setText(Html.fromHtml(htmlText5, Html.FROM_HTML_MODE_LEGACY));
        } else {
            help5Btn.setText(Html.fromHtml(htmlText5));
        }

        help5Btn.setBackgroundResource(R.drawable.button_rounded);
        help5Btn.setBackgroundTintList(null); // Elimina el tinte que pone el tema
        help5Btn.setTextColor(ContextCompat.getColor(context, R.color.black));

        help5Btn.setOnClickListener(v -> {
            if (useWildcard()) {
                if (callbacks != null) {
                    callbacks.onShowCreativeHelp();
                }
                Toast.makeText(context, "Ayuda 5: Aplicando ayuda creativa...", Toast.LENGTH_SHORT).show();
            }
        });
        

    }
    
    /**
     * Usa un comodín y actualiza el contador
     */
    private boolean useWildcard() {
        if (remainingWildcards > 0) {
            remainingWildcards--;
            saveRemainingWildcards();
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene el número de comodines restantes
     */
    private int getRemainingWildcards() {
        SharedPreferences prefs = context.getSharedPreferences(WILDCARD_PREFS, Context.MODE_PRIVATE);
        String key = WILDCARD_COUNT_KEY + moduleName + "_" + topicName;
        return prefs.getInt(key, maxWildcards);
    }
    
    /**
     * Guarda el número de comodines restantes
     */
    private void saveRemainingWildcards() {
        SharedPreferences prefs = context.getSharedPreferences(WILDCARD_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = WILDCARD_COUNT_KEY + moduleName + "_" + topicName;
        editor.putInt(key, remainingWildcards);
        editor.apply();
    }
    
    /**
     * Resetea los comodines para un tema específico
     */
    public void resetWildcards() {
        remainingWildcards = maxWildcards;
        saveRemainingWildcards();
    }
    
    /**
     * Obtiene el número de comodines restantes
     */
    public int getRemainingWildcardsCount() {
        return remainingWildcards;
    }
    
    /**
     * Verifica si hay comodines disponibles
     */
    public boolean hasWildcardsAvailable() {
        return remainingWildcards > 0;
    }
    
    /**
     * Obtiene el VideoHelper para uso directo en las actividades
     */
    public VideoHelper getVideoHelper() {
        return videoHelper;
    }
    
    /**
     * Marca una pregunta como cambiada para evitar duplicación de puntos
     */
    public void markQuestionAsChanged(int questionIndex) {
        SharedPreferences prefs = context.getSharedPreferences(WILDCARD_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = CHANGED_QUESTIONS_KEY + moduleName + "_" + topicName + "_" + questionIndex;
        editor.putBoolean(key, true);
        editor.apply();
    }
    
    /**
     * Verifica si una pregunta fue cambiada
     */
    public boolean wasQuestionChanged(int questionIndex) {
        SharedPreferences prefs = context.getSharedPreferences(WILDCARD_PREFS, Context.MODE_PRIVATE);
        String key = CHANGED_QUESTIONS_KEY + moduleName + "_" + topicName + "_" + questionIndex;
        return prefs.getBoolean(key, false);
    }
    
    /**
     * Limpia el registro de preguntas cambiadas
     */
    public void clearChangedQuestions() {
        SharedPreferences prefs = context.getSharedPreferences(WILDCARD_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Limpiar todas las claves de preguntas cambiadas para este módulo y tema
        for (int i = 0; i < 100; i++) { // Asumimos máximo 100 preguntas por tema
            String key = CHANGED_QUESTIONS_KEY + moduleName + "_" + topicName + "_" + i;
            editor.remove(key);
        }
        editor.apply();
    }
    
    /**
     * Implementación por defecto de la ayuda 50/50
     * Elimina 2 opciones incorrectas aleatoriamente
     * IMPORTANTE: Esta ayuda NO afecta la evaluación final
     */
    public List<String> applyFiftyFifty(List<String> allOptions, String correctAnswer) {
        if (allOptions.size() <= 2) {
            return allOptions; // No se puede aplicar 50/50
        }
        
        List<String> result = new ArrayList<>();
        result.add(correctAnswer); // Siempre incluir la respuesta correcta
        
        // Agregar una opción incorrecta aleatoria
        List<String> incorrectOptions = new ArrayList<>();
        for (String option : allOptions) {
            if (!option.equals(correctAnswer)) {
                incorrectOptions.add(option);
            }
        }
        
        if (!incorrectOptions.isEmpty()) {
            Random random = new Random();
            String randomIncorrect = incorrectOptions.get(random.nextInt(incorrectOptions.size()));
            result.add(randomIncorrect);
        }
        
        return result;
    }
    
    /**
     * Implementación por defecto de la ayuda creativa
     * Proporciona una pista contextual
     */
    public String getCreativeHelp(String question, String correctAnswer) {
        // Lógica básica de pistas - puede ser personalizada por cada módulo
        if (question.toLowerCase().contains("pronoun") || question.toLowerCase().contains("pronombre")) {
            return "💡 Pista: Piensa en si se refiere a una persona, objeto o grupo";
        } else if (question.toLowerCase().contains("color") || question.toLowerCase().contains("color")) {
            return "💡 Pista: Observa el objeto en la imagen";
        } else if (question.toLowerCase().contains("number") || question.toLowerCase().contains("número")) {
            return "💡 Pista: Cuenta cuidadosamente los elementos";
        } else {
            return "💡 Pista: Lee la pregunta con atención y piensa en el contexto";
        }
    }
    
    /**
     * Obtiene una pregunta alternativa para la ayuda "Cambiar Pregunta"
     * IMPORTANTE: Esta pregunta NO se incluye en la evaluación final
     */
    public String getAlternativeQuestion(String currentQuestion, String topic) {
        // Generar una pregunta alternativa basada en el tema
        switch (topic.toUpperCase()) {
            case "ALPHABET":
                return "¿Puedes identificar otra letra del alfabeto?";
            case "NUMBERS":
                return "¿Puedes contar elementos diferentes?";
            case "COLORS":
                return "¿Puedes identificar otros colores en tu entorno?";
            case "PERSONAL PRONOUNS":
                return "¿Puedes usar pronombres en una oración diferente?";
            case "POSSESSIVE ADJECTIVES":
                return "¿Puedes describir posesiones de otra persona?";
            default:
                return "¿Puedes pensar en otro ejemplo relacionado con este tema?";
        }
    }
    
    /**
     * Muestra el video del instructor para el tema actual
     */
    public void showInstructorVideo() {
        if (videoHelper != null) {
            videoHelper.showInstructorVideo(topicName);
        }
    }
}
