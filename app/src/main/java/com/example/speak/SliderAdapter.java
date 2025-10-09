package com.example.speak;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speak.pronunciation.PronunciationActivity;
import com.example.speak.pronunciation.PronunciationMenuActivity;
import com.example.speak.quiz.QuizActivity;
import com.example.speak.TextToSpeechActivity;
import com.example.speak.MenuReadingActivity;
import com.example.speak.MenuWritingActivity;

// Imports para el sistema de bloqueo
import android.content.SharedPreferences;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {
    private Context context;
    private int[] images = {
            R.drawable.test,        // 0
            R.drawable.listening,   // 1
            R.drawable.speaking,    // 2
            R.drawable.reading,     // 3
            R.drawable.writing,     // 4
            R.drawable.word_order,  // 5
            R.drawable.practice_pronun // 6
    };

    public SliderAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.slide_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        // Configurar la primera imagen del slide actual
        int firstImageIndex = position * 2;
        holder.imageView1.setImageResource(images[firstImageIndex]);
        
        // Aplicar bloqueo visual a los módulos correspondientes
        applySpeakingModuleLock(holder.imageView1, firstImageIndex == 2);
        applyReadingModuleLock(holder.imageView1, firstImageIndex == 3);
        applyWritingModuleLock(holder.imageView1, firstImageIndex == 4);
        applyWordOrderModuleLock(holder.imageView1, firstImageIndex == 5);
        
        holder.imageView1.setOnClickListener(v -> {
            // Verificar si es el módulo de speaking y está bloqueado
            if (!isFreeRoamEnabled() && firstImageIndex == 2 && !isSpeakingModuleEnabled(holder.imageView1)) {
                showSpeakingProgressDetails();
                return;
            }
            
            // Verificar si es el módulo de reading y está bloqueado
            if (!isFreeRoamEnabled() && firstImageIndex == 3 && !isReadingModuleEnabled(holder.imageView1)) {
                Toast.makeText(context, "Para acceder al módulo de reading, debes completar speaking", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Verificar si es el módulo de writing y está bloqueado
            if (!isFreeRoamEnabled() && firstImageIndex == 4 && !isWritingModuleEnabled(holder.imageView1)) {
                Toast.makeText(context, "Para acceder al módulo de writing, debes completar 70% de reading básico", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Verificar si es el módulo de word order y está bloqueado
            if (!isFreeRoamEnabled() && firstImageIndex == 5 && !isWordOrderModuleEnabled(holder.imageView1)) {
                Toast.makeText(context, "Para acceder al módulo de word order, debes completar writing", Toast.LENGTH_LONG).show();
                return;
            }
            
            Intent intent;
            switch (firstImageIndex) {
                case 0: // Test
                    intent = new Intent(context, QuizActivity.class);
                    break;
                case 1: // Listening
                    intent = new Intent(context, MenuA1Activity.class);
                    break;
                case 2: // Speaking
                    intent = new Intent(context, MenuSpeakingActivity.class);
                    break;
                case 3: // Reading
                    intent = new Intent(context, MenuReadingActivity.class);
                    break;
                case 4: // Writing
                    intent = new Intent(context, MenuWritingActivity.class);
                    break;
                case 5: // Word Order
                    intent = new Intent(context, WordOrderActivity.class);
                    break;
                case 6: // Practice Pronun
                    intent = new Intent(context, TextToSpeechActivity.class);
                    break;
                default:
                    return;
            }
            if (isFreeRoamEnabled()) {
                intent.putExtra("FREE_ROAM", true);
            }
            // Guardar el módulo visitado
            saveModuleSelection(firstImageIndex);
            context.startActivity(intent);
        });

        // Configurar la segunda imagen del slide actual
        int secondImageIndex = firstImageIndex + 1;
        if (secondImageIndex < images.length) {
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setImageResource(images[secondImageIndex]);
            
            // Aplicar bloqueo visual a los módulos correspondientes
            applySpeakingModuleLock(holder.imageView2, secondImageIndex == 2);
            applyReadingModuleLock(holder.imageView2, secondImageIndex == 3);
            applyWritingModuleLock(holder.imageView2, secondImageIndex == 4);
            applyWordOrderModuleLock(holder.imageView2, secondImageIndex == 5);
            
            holder.imageView2.setOnClickListener(v -> {
                // Verificar si es el módulo de speaking y está bloqueado
                if (!isFreeRoamEnabled() && secondImageIndex == 2 && !isSpeakingModuleEnabled(holder.imageView2)) {
                    showSpeakingProgressDetails();
                    return;
                }
                
                // Verificar si es el módulo de reading y está bloqueado
                if (!isFreeRoamEnabled() && secondImageIndex == 3 && !isReadingModuleEnabled(holder.imageView2)) {
                    Toast.makeText(context, "Para acceder al módulo de reading, debes completar speaking", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Verificar si es el módulo de writing y está bloqueado
                if (!isFreeRoamEnabled() && secondImageIndex == 4 && !isWritingModuleEnabled(holder.imageView2)) {
                    Toast.makeText(context, "Para acceder al módulo de writing, debes completar 70% de reading básico", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Verificar si es el módulo de word order y está bloqueado
                if (!isFreeRoamEnabled() && secondImageIndex == 5 && !isWordOrderModuleEnabled(holder.imageView2)) {
                    Toast.makeText(context, "Para acceder al módulo de word order, debes completar writing", Toast.LENGTH_LONG).show();
                    return;
                }
                
                Intent intent;
                switch (secondImageIndex) {
                    case 1: // Listening
                        intent = new Intent(context, MenuA1Activity.class);
                        break;
                    case 2: // Speaking
                        intent = new Intent(context, MenuSpeakingActivity.class);
                        break;
                    case 3: // Reading
                        intent = new Intent(context, MenuReadingActivity.class);
                        break;
                    case 4: // Writing
                        intent = new Intent(context, MenuWritingActivity.class);
                        break;
                    case 5: // Word Order
                        intent = new Intent(context, WordOrderActivity.class);
                        break;
                    case 6: // Practice Pronun
                        intent = new Intent(context, TextToSpeechActivity.class);
                        break;
                    default:
                        return;
                }
                if (isFreeRoamEnabled()) {
                    intent.putExtra("FREE_ROAM", true);
                }
                saveModuleSelection(secondImageIndex);
                context.startActivity(intent);
            });
        } else {
            holder.imageView2.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // Calculamos cuántos slides necesitamos (2 imágenes por slide)
        return (images.length + 1) / 2;
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView1;
        ImageView imageView2;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView1 = itemView.findViewById(R.id.slideImage1);
            imageView2 = itemView.findViewById(R.id.slideImage2);
        }
    }

    // Verificar si se ha completado el 70% de los temas básicos de listening (eButtonStart)
    private boolean isListeningModule70PercentCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        
        // Temas del eButtonStart (listening básico) - los primeros en desbloquearse
        String[] basicListeningTopics = {
            "PASSED_ALPHABET",           // imgABC
            "PASSED_NUMBERS",            // imgNumbers  
            "PASSED_COLORS",             // imgColors
            "PASSED_PERSONAL_PRONOUNS",  // imgPronouns
            "PASSED_POSSESSIVE_ADJECTIVES" // imgPossesive
        };
        
        // Contar temas completados
        int completedTopics = 0;
        for (String topic : basicListeningTopics) {
            if (prefs.getBoolean(topic, false)) {
                completedTopics++;
            }
        }
        
        // Calcular porcentaje
        double completionPercentage = (double) completedTopics / basicListeningTopics.length * 100;
        
        Log.d("SliderAdapter", "=== VERIFICACIÓN DESBLOQUEO SPEAKING ===");
        Log.d("SliderAdapter", "Temas completados: " + completedTopics + "/" + basicListeningTopics.length);
        Log.d("SliderAdapter", "¿Desbloquear speaking? " + (completedTopics == basicListeningTopics.length));
        
        // Detalles por tema
        for (String topic : basicListeningTopics) {
            boolean passed = prefs.getBoolean(topic, false);
            Log.d("SliderAdapter", "  " + topic + ": " + (passed ? "✅ COMPLETADO" : "❌ PENDIENTE"));
        }
        
        return completedTopics == basicListeningTopics.length;
    }

    // Verificar si todos los temas de listening del nivel A1.1 han sido completados (para reading)
    private boolean areAllA1_1ListeningTopicsCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        
        // Lista COMPLETA de todos los temas de listening del nivel A1.1 (para reading)
        boolean passedAlphabet = prefs.getBoolean("PASSED_ALPHABET", false);
        boolean passedNumbers = prefs.getBoolean("PASSED_NUMBERS", false);
        boolean passedColors = prefs.getBoolean("PASSED_COLORS", false);
        boolean passedPronouns = prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false);
        boolean passedPossessive = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
        boolean passedPrepositions = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false);
        boolean passedAdjectives = prefs.getBoolean("PASSED_ADJECTIVES", false);
        
        // También verificar los temas adicionales del nivel A1.1 si existen
        boolean passedOrdinal = prefs.getBoolean("PASSED_ORDINAL", false);
        boolean passedVerb = prefs.getBoolean("PASSED_VERB_TO_BE", false);
        
        // Todos los temas básicos deben estar completados
        boolean allBasicCompleted = passedAlphabet && passedNumbers && passedColors && 
                                   passedPronouns && passedPossessive && passedPrepositions && 
                                   passedAdjectives;
        
        // También incluir los temas adicionales para un bloqueo más estricto
        boolean allCompleted = allBasicCompleted && passedOrdinal && passedVerb;
        
        Log.d("SliderAdapter", "Estado COMPLETO de temas A1.1 para reading - Básicos: " + allBasicCompleted + ", Todos: " + allCompleted);
        Log.d("SliderAdapter", "Detalles: ABC=" + passedAlphabet + ", NUM=" + passedNumbers + ", COL=" + passedColors + 
              ", PRON=" + passedPronouns + ", POSS=" + passedPossessive + ", PREP=" + passedPrepositions + 
              ", ADJ=" + passedAdjectives + ", ORD=" + passedOrdinal + ", VERB=" + passedVerb);
        
        return allCompleted;
    }

    // Aplicar bloqueo visual al módulo de speaking
    private void applySpeakingModuleLock(ImageView imageView, boolean isSpeakingModule) {
        if (!isSpeakingModule) return;
        if (isFreeRoamEnabled()) {
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            return;
        }
        
        // CAMBIO: Usar 70% en lugar de 100% para desbloquear speaking
        boolean listening70PercentCompleted = isListeningModule70PercentCompleted();
        
        if (listening70PercentCompleted) {
            // Módulo desbloqueado - apariencia normal
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            Log.d("SliderAdapter", "🗣️ Módulo de speaking DESBLOQUEADO (70% listening completado)");
        } else {
            // Módulo bloqueado - apariencia gris y transparente
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setElevation(2f); // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked");
            Log.d("SliderAdapter", "🔒 Módulo de speaking BLOQUEADO (necesita 70% listening completado)");
        }
    }

    // Verificar si el módulo de speaking está habilitado
    private boolean isSpeakingModuleEnabled(ImageView imageView) {
        Object state = imageView.getTag(R.id.element_state);
        return "enabled".equals(state);
    }

    // Verificar si el módulo de pronunciación ha sido completado
    private boolean isPronunciationModuleCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        
        // CAMBIO: Verificar progreso real de speaking en lugar de estar temporalmente desbloqueado
        // Reading se desbloquea cuando se completa un 70% del módulo de speaking
        int speakingCompletedTopics = 0;
        int totalSpeakingTopics = 7; // Total de temas de speaking disponibles
        
        // Contar temas de speaking completados
        if (prefs.getBoolean("PASSED_PRON_ALPHABET", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_NUMBERS", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_COLORS", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false)) speakingCompletedTopics++;
        if (prefs.getBoolean("PASSED_PRON_ADJECTIVES", false)) speakingCompletedTopics++;
        
        // Calcular porcentaje de completado
        double completionPercentage = (speakingCompletedTopics / (double) totalSpeakingTopics) * 100;
        boolean isCompleted = completionPercentage >= 70; // 70% o más para desbloquear reading
        
        Log.d("SliderAdapter", "Speaking progress: " + speakingCompletedTopics + "/" + totalSpeakingTopics + 
              " (" + String.format("%.1f", completionPercentage) + "%) - Reading " + 
              (isCompleted ? "DESBLOQUEADO" : "BLOQUEADO"));
        
        return isCompleted;
    }

    // Verificar si los 5 temas básicos de Writing están completados
    private boolean isWritingStartCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);

        return prefs.getBoolean("PASSED_WRITING_ALPHABET", false) &&
               prefs.getBoolean("PASSED_WRITING_NUMBERS", false) &&
               prefs.getBoolean("PASSED_WRITING_COLORS", false) &&
               prefs.getBoolean("PASSED_WRITING_PERSONAL_PRONOUNS", false) &&
               prefs.getBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", false);
    }

    // Aplicar bloqueo visual al módulo de reading
    private void applyReadingModuleLock(ImageView imageView, boolean isReadingModule) {
        if (!isReadingModule) return;
        if (isFreeRoamEnabled()) {
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            return;
        }
        
        boolean speakingStartCompleted = isPronunciationModuleCompleted();
        
        if (speakingStartCompleted) {
            // Módulo desbloqueado - apariencia normal
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            Log.d("SliderAdapter", "Módulo de reading desbloqueado (Speaking Start completado)");
        } else {
            // Módulo bloqueado - apariencia gris y transparente
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setElevation(2f); // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked");
            Log.d("SliderAdapter", "Módulo de reading bloqueado (falta completar Speaking Start)");
        }
    }

    // Verificar si el módulo de reading está habilitado
    private boolean isReadingModuleEnabled(ImageView imageView) {
        Object state = imageView.getTag(R.id.element_state);
        return "enabled".equals(state);
    }

    // Verificar si el módulo de writing está habilitado
    private boolean isWritingModuleEnabled(ImageView imageView) {
        Object state = imageView.getTag(R.id.element_state);
        return "enabled".equals(state);
    }

    // Verificar si el módulo de word order está habilitado
    private boolean isWordOrderModuleEnabled(ImageView imageView) {
        Object state = imageView.getTag(R.id.element_state);
        return "enabled".equals(state);
    }

    // Verificar si se ha completado el 70% de los temas básicos de reading (eButtonStartReading)
    private boolean isReadingModule70PercentCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        
        // Temas del eButtonStartReading (reading básico) - los primeros en desbloquearse
        String[] basicReadingTopics = {
            "PASSED_READING_ALPHABET",           // imgReadingTranslation (Alphabet)
            "PASSED_READING_NUMBERS",            // imgReadingComprehension (Numbers)
            "PASSED_READING_COLORS",             // imgReadingVocabulary (Colors)
            "PASSED_READING_PERSONAL_PRONOUNS",  // imgReadingGrammar  (Personal Pronouns)
            "PASSED_READING_POSSESSIVE_ADJECTIVES" // imgReadingStories (Possessive Adjectives)
        };
        
        // Contar temas completados
        int completedTopics = 0;
        for (String topic : basicReadingTopics) {
            if (prefs.getBoolean(topic, false)) {
                completedTopics++;
            }
        }
        
        // Calcular porcentaje
        double completionPercentage = (double) completedTopics / basicReadingTopics.length * 100;
        
        Log.d("SliderAdapter", "=== VERIFICACIÓN DESBLOQUEO WRITING ===");
        Log.d("SliderAdapter", "Temas completados: " + completedTopics + "/" + basicReadingTopics.length);
        Log.d("SliderAdapter", "Porcentaje completado: " + String.format("%.1f%%", completionPercentage));
        Log.d("SliderAdapter", "¿Desbloquear writing? " + (completionPercentage >= 70.0));
        
        // Detalles por tema
        for (String topic : basicReadingTopics) {
            boolean passed = prefs.getBoolean(topic, false);
            Log.d("SliderAdapter", "  " + topic + ": " + (passed ? "✅ COMPLETADO" : "❌ PENDIENTE"));
        }
        
        return completionPercentage >= 70.0;
    }

    // Aplicar bloqueo visual al módulo de writing
    private void applyWritingModuleLock(ImageView imageView, boolean isWritingModule) {
        if (!isWritingModule) return;
        if (isFreeRoamEnabled()) {
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            return;
        }
        
        // CAMBIO: Writing se desbloquea cuando se completa 70% de reading básico
        boolean reading70PercentCompleted = isReadingModule70PercentCompleted();
        
        if (reading70PercentCompleted) {
            // Módulo desbloqueado - apariencia normal
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            Log.d("SliderAdapter", "📝 Módulo de writing DESBLOQUEADO (70% reading completado)");
        } else {
            // Módulo bloqueado - apariencia gris y transparente
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setElevation(2f); // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked");
            Log.d("SliderAdapter", "🔒 Módulo de writing BLOQUEADO (necesita 70% reading completado)");
        }
    }

    // Aplicar bloqueo visual al módulo de word order
    private void applyWordOrderModuleLock(ImageView imageView, boolean isWordOrderModule) {
        if (!isWordOrderModule) return;
        if (isFreeRoamEnabled()) {
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            return;
        }
        // Por ahora, Word Order está bloqueado (salvo modo libre)
        boolean isEnabled = false;
        
        if (isEnabled) {
            // Módulo desbloqueado - apariencia normal
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
            Log.d("SliderAdapter", "Módulo de word order desbloqueado");
        } else {
            // Módulo bloqueado - apariencia gris y transparente
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setElevation(2f); // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked");
            Log.d("SliderAdapter", "Módulo de word order bloqueado (en desarrollo)");
        }
    }

    private boolean isFreeRoamEnabled() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("FREE_ROAM", false);
    }

    // Mostrar progreso detallado del módulo de speaking
    private void showSpeakingProgressDetails() {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        
        String[] basicTopics = {
            "PASSED_ALPHABET",
            "PASSED_NUMBERS", 
            "PASSED_COLORS",
            "PASSED_PERSONAL_PRONOUNS",
            "PASSED_POSSESSIVE_ADJECTIVES"
        };
        
        String[] topicNames = {
            "Alphabet",
            "Numbers",
            "Colors", 
            "Personal Pronouns",
            "Possessive Adjectives"
        };
        
        int completed = 0;
        StringBuilder progressMessage = new StringBuilder("🗣️ Progreso para Speaking:\n");
        
        for (int i = 0; i < basicTopics.length; i++) {
            boolean isPassed = prefs.getBoolean(basicTopics[i], false);
            if (isPassed) completed++;
            
            progressMessage.append(isPassed ? "✅ " : "❌ ")
                          .append(topicNames[i])
                          .append("\n");
        }
        
        double percentage = (double) completed / basicTopics.length * 100;
        progressMessage.append("\nProgreso: ")
                      .append(completed)
                      .append("/")
                      .append(basicTopics.length)
                      .append(" (")
                      .append(String.format("%.0f%%", percentage))
                      .append(")\n")
                      .append("Necesitas: 70% para desbloquear Speaking");
        
        Toast.makeText(context, progressMessage.toString(), Toast.LENGTH_LONG).show();
    }

    // Guarda en SharedPreferences qué módulo se abrió
    private void saveModuleSelection(int imageIndex) {
        String module = null;
        switch (imageIndex) {
            case 1: module = "listening"; break;
            case 2: module = "speaking"; break;
            case 3: module = "reading"; break;
            case 4: module = "writing"; break;
            case 5: module = "word_order"; break;
        }
        if (module != null) {
            ModuleTracker.setLastModule(context, module);
        }
    }
} 