package com.example.speak.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class TrophyHelper {
    
    private static final String TAG = "TrophyHelper";
    private static final String PREFS_NAME = "TrophyPrefs";
    
    // Temas del eButtonStart (primeras 5 sesiones)
    private static final List<String> EBUTTON_START_TOPICS = Arrays.asList(
        "ALPHABET",
        "NUMBERS", 
        "COLORS",
        "PERSONAL PRONOUNS",
        "POSSESSIVE ADJECTIVES"
    );
    
    // Claves de progreso para los temas del eButtonStart
    private static final List<String> EBUTTON_START_PROGRESS_KEYS = Arrays.asList(
        "PASSED_ALPHABET",
        "PASSED_NUMBERS",
        "PASSED_COLORS", 
        "PASSED_PERSONAL_PRONOUNS",
        "PASSED_POSSESSIVE_ADJECTIVES"
    );
    
    /**
     * Obtiene el número de trofeos (temas del eButtonStart completados)
     */
    public static int getTrophyCount(Context context) {
        // Usar el mismo SharedPreferences que el sistema existente
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        int trophyCount = 0;
        
        Log.d(TAG, "=== VERIFICANDO TROFEOS ===");
        
        // Verificar cada tema del eButtonStart individualmente usando las claves del sistema existente
        String[] progressKeys = {
            "PASSED_ALPHABET",
            "PASSED_NUMBERS", 
            "PASSED_COLORS",
            "PASSED_PERSONAL_PRONOUNS",
            "PASSED_POSSESSIVE_ADJECTIVES"
        };
        
        for (int i = 0; i < EBUTTON_START_TOPICS.size(); i++) {
            String topic = EBUTTON_START_TOPICS.get(i);
            String progressKey = progressKeys[i];
            boolean isPassed = prefs.getBoolean(progressKey, false);
            
            Log.d(TAG, "Tema: " + topic + " | Clave: " + progressKey + " | Completado: " + isPassed);
            
            if (isPassed) {
                trophyCount++;
                Log.d(TAG, "✅ Trofeo confirmado para: " + topic);
            } else {
                Log.d(TAG, "❌ No completado: " + topic);
            }
        }
        
        Log.d(TAG, "=== RESULTADO FINAL: " + trophyCount + "/" + EBUTTON_START_TOPICS.size() + " trofeos ===");
        return trophyCount;
    }
    
    /**
     * Obtiene el número de estrellas (sesiones completadas)
     */
    public static int getStarCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("star_count", 0);
    }
    
    /**
     * Incrementa el contador de estrellas cuando se completa una sesión
     */
    public static void incrementStarCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentStars = prefs.getInt("star_count", 0);
        int newStars = currentStars + 1;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("star_count", newStars);
        editor.apply();
        
        Log.d(TAG, "Estrellas incrementadas: " + currentStars + " -> " + newStars);
    }
    
    /**
     * Verifica si un tema es del eButtonStart y otorga trofeo si es completado
     */
    public static void checkAndAwardTrophy(Context context, String topic, int score) {
        Log.d(TAG, "Verificando trofeo - Tema: " + topic + ", Puntuación: " + score);
        
        // Solo otorgar trofeo si es un tema del eButtonStart y la puntuación es >= 70
        if (score >= 70 && EBUTTON_START_TOPICS.contains(topic)) {
            Log.d(TAG, "¡Tema del eButtonStart completado! Tema: " + topic + ", Puntuación: " + score);
            // No necesitamos guardar nada aquí porque ya se guarda en ProgressPrefs
            // Solo logueamos que se otorgó el trofeo
        } else {
            Log.d(TAG, "No se otorga trofeo - Tema: " + topic + " no es del eButtonStart o puntuación insuficiente: " + score);
        }
    }
    
    /**
     * Obtiene la clave de progreso para un tema específico
     */
    private static String getProgressKeyForTopic(String topic) {
        switch (topic) {
            case "ALPHABET":
                return "PASSED_ALPHABET";
            case "NUMBERS":
                return "PASSED_NUMBERS";
            case "COLORS":
                return "PASSED_COLORS";
            case "PERSONAL PRONOUNS":
                return "PASSED_PERSONAL_PRONOUNS";
            case "POSSESSIVE ADJECTIVES":
                return "PASSED_POSSESSIVE_ADJECTIVES";
            default:
                return null;
        }
    }
    
    /**
     * Verifica si se ha completado el 100% de los temas del eButtonStart
     */
    public static boolean isEButtonStartFullyCompleted(Context context) {
        return getTrophyCount(context) == EBUTTON_START_TOPICS.size();
    }
    
    /**
     * Obtiene el progreso de trofeos como porcentaje
     */
    public static int getTrophyProgressPercentage(Context context) {
        int trophyCount = getTrophyCount(context);
        return (trophyCount * 100) / EBUTTON_START_TOPICS.size();
    }
    
    /**
     * Resetea todos los contadores (útil para testing)
     */
    public static void resetAllCounters(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Resetear trofeos
        for (String progressKey : EBUTTON_START_PROGRESS_KEYS) {
            editor.putBoolean(progressKey, false);
        }
        
        // Resetear estrellas
        editor.putInt("star_count", 0);
        editor.apply();
        
        Log.d(TAG, "Todos los contadores han sido reseteados");
    }
    
    /**
     * Limpia trofeos incorrectos - solo mantiene los del eButtonStart
     */
    public static void cleanIncorrectTrophies(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        Log.d(TAG, "=== LIMPIANDO TROFEOS INCORRECTOS ===");
        
        // Obtener todas las claves que empiezan con "PASSED_"
        java.util.Map<String, ?> allPrefs = prefs.getAll();
        int cleanedCount = 0;
        
        for (String key : allPrefs.keySet()) {
            if (key.startsWith("PASSED_")) {
                // Verificar si es una clave válida del eButtonStart
                boolean isValidKey = false;
                for (String validKey : EBUTTON_START_PROGRESS_KEYS) {
                    if (key.equals(validKey)) {
                        isValidKey = true;
                        break;
                    }
                }
                
                if (!isValidKey) {
                    editor.remove(key);
                    cleanedCount++;
                    Log.d(TAG, "Clave incorrecta removida: " + key);
                }
            }
        }
        
        editor.apply();
        Log.d(TAG, "Limpieza completada. Claves removidas: " + cleanedCount);
    }
}
