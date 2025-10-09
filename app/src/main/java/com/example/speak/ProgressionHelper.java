package com.example.speak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProgressionHelper {
    
    private static final String PREFS_NAME = "ProgressPrefs";
    
    // Definir los temas básicos del eButtonStart
    private static final String[] EBUTTON_START_TOPICS = {
        "ALPHABET",
        "NUMBERS", 
        "COLORS",
        "PERSONAL PRONOUNS",
        "POSSESSIVE ADJECTIVES"
    };

    // Temas básicos del Start de Speaking (Pronunciation)
    private static final String[] SPEAKING_START_TOPICS = {
        "PRON_ALPHABET",
        "PRON_NUMBERS",
        "PRON_COLORS",
        "PRON_PERSONAL_PRONOUNS",
        "PRON_POSSESSIVE_ADJECTIVES"
    };

    // Temas básicos del Start de Reading
    private static final String[] READING_START_TOPICS = {
        "READING_ALPHABET",
        "READING_NUMBERS", 
        "READING_COLORS",
        "READING_PERSONAL_PRONOUNS",
        "READING_POSSESSIVE_ADJECTIVES",
        "READING_PREPOSITIONS"
    };

    // Definir la secuencia de progresión de temas del nivel A1.1
    private static final LinkedHashMap<String, String> TOPIC_PROGRESSION = new LinkedHashMap<String, String>() {{
        put("ALPHABET", "NUMBERS");
        put("NUMBERS", "COLORS");
        put("COLORS", "PERSONAL PRONOUNS");
        put("PERSONAL PRONOUNS", "POSSESSIVE ADJECTIVES");
        put("POSSESSIVE ADJECTIVES", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
        put("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION", "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
        // Al completar ADJECTIVES, se considera completado el nivel A1.1
    }};
    
    // Mapear nombres de temas a sus claves de progreso
    private static final LinkedHashMap<String, String> TOPIC_TO_PROGRESS_KEY = new LinkedHashMap<String, String>() {{
        put("ALPHABET", "PASSED_ALPHABET");
        put("NUMBERS", "PASSED_NUMBERS");
        put("COLORS", "PASSED_COLORS");
        put("PERSONAL PRONOUNS", "PASSED_PERSONAL_PRONOUNS");
        put("POSSESSIVE ADJECTIVES", "PASSED_POSSESSIVE_ADJECTIVES");
        put("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION", "PASSED_PREPOSITIONS_OF_PLACE");
        put("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)", "PASSED_ADJECTIVES");

        // Pronunciation (Speaking) progress keys
        put("PRON_ALPHABET", "PASSED_PRON_ALPHABET");
        put("PRON_NUMBERS", "PASSED_PRON_NUMBERS");
        put("PRON_COLORS", "PASSED_PRON_COLORS");
        put("PRON_PERSONAL_PRONOUNS", "PASSED_PRON_PERSONAL_PRONOUNS");
        put("PRON_POSSESSIVE_ADJECTIVES", "PASSED_PRON_POSSESSIVE_ADJECTIVES");

        // Reading progress keys
        put("READING_ALPHABET", "PASSED_READING_ALPHABET");
        put("READING_NUMBERS", "PASSED_READING_NUMBERS");
        put("READING_COLORS", "PASSED_READING_COLORS");
        put("READING_PERSONAL_PRONOUNS", "PASSED_READING_PERSONAL_PRONOUNS");
        put("READING_POSSESSIVE_ADJECTIVES", "PASSED_READING_POSSESSIVE_ADJECTIVES");
        put("READING_PREPOSITIONS", "PASSED_READING_PREPOSITIONS");

        // Writing progress keys
        put("WRITING_ALPHABET", "PASSED_WRITING_ALPHABET");
        put("WRITING_NUMBERS", "PASSED_WRITING_NUMBERS");
        put("WRITING_COLORS", "PASSED_WRITING_COLORS");
        put("WRITING_PERSONAL_PRONOUNS", "PASSED_WRITING_PERSONAL_PRONOUNS");
        put("WRITING_POSSESSIVE_ADJECTIVES", "PASSED_WRITING_POSSESSIVE_ADJECTIVES");
    }};

    // Comprueba si se han completado todas las actividades básicas de Speaking
    private static boolean isSpeakingStartCompleted(SharedPreferences prefs) {
        for (String speakingTopic : SPEAKING_START_TOPICS) {
            String key = TOPIC_TO_PROGRESS_KEY.get(speakingTopic);
            if (key == null || !prefs.getBoolean(key, false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica si un tema pertenece al eButtonStart
     * @param topic Tema a verificar
     * @return true si es parte del eButtonStart
     */
    public static boolean isEButtonStartTopic(String topic) {
        for (String eButtonTopic : EBUTTON_START_TOPICS) {
            if (eButtonTopic.equals(topic)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica si se ha completado el 70% de los temas del eButtonStart
     * @param context Contexto de la aplicación
     * @return true si se ha completado el 70% o más
     */
    public static boolean isEButtonStart70PercentCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        int completedCount = 0;
        for (String topic : EBUTTON_START_TOPICS) {
            String progressKey = TOPIC_TO_PROGRESS_KEY.get(topic);
            if (progressKey != null && prefs.getBoolean(progressKey, false)) {
                completedCount++;
            }
        }
        
        // Desbloquear speaking solo cuando TODOS los temas del Start estén completados
        return completedCount == EBUTTON_START_TOPICS.length;
    }
    
    /**
     * Verifica si después de completar este tema se alcanza el 70% del eButtonStart
     * @param context Contexto de la aplicación
     * @param justCompletedTopic Tema que se acaba de completar
     * @return true si este tema hace que se alcance el 70%
     */
    public static boolean doesTopicUnlockSpeaking(Context context, String justCompletedTopic) {
        // Solo aplica para temas del eButtonStart
        if (!isEButtonStartTopic(justCompletedTopic)) {
            return false;
        }
        
        // Simular que el tema actual ya está completado
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        for (String topic : EBUTTON_START_TOPICS) {
            String progressKey = TOPIC_TO_PROGRESS_KEY.get(topic);
            boolean isCompleted = topic.equals(justCompletedTopic) ||
                                (progressKey != null && prefs.getBoolean(progressKey, false));
            if (!isCompleted) {
                // En cuanto encontramos un tema sin completar, no se desbloquea Speaking
                return false;
            }
        }

        // Si hemos llegado aquí, todos los temas están completados explícitamente
        return true;
    }

    /**
     * Verifica si al completar el tema actual se desbloquea Reading.
     */
    public static boolean doesTopicUnlockReading(Context context, String justCompletedTopic) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!justCompletedTopic.startsWith("PRON_")) {
            return false; // Solo los temas de pronunciation cuentan
        }

        // Marcar el tema actual como completado de forma provisional y comprobar si todos están completos
        SharedPreferences.Editor editor = prefs.edit();
        String progressKey = TOPIC_TO_PROGRESS_KEY.get(justCompletedTopic);
        if (progressKey != null) editor.putBoolean(progressKey, true).apply();

        boolean completed = isSpeakingStartCompleted(prefs);

        // Revertir si no debería haberse marcado todavía
        if (!completed && progressKey != null && !prefs.getBoolean(progressKey, false)) {
            editor.putBoolean(progressKey, false).apply();
        }
        return completed;
    }

    /**
     * Obtiene el siguiente tema en la progresión
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @return Siguiente tema o null si no hay más temas
     */
    public static String getNextTopic(Context context, String currentTopic) {
        // Buscar el siguiente tema en la progresión
        String nextTopic = TOPIC_PROGRESSION.get(currentTopic);
        
        if (nextTopic != null) {
            // Verificar si el siguiente tema ya está desbloqueado
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String progressKey = TOPIC_TO_PROGRESS_KEY.get(currentTopic);
            
            if (progressKey != null && prefs.getBoolean(progressKey, false)) {
                return nextTopic;
            }
        }
        
        return nextTopic; // Retorna el siguiente tema incluso si no está desbloqueado aún
    }
    
    /**
     * Verifica si hay un siguiente tema disponible
     * @param currentTopic Tema actual
     * @return true si hay siguiente tema, false si es el último
     */
    public static boolean hasNextTopic(String currentTopic) {
        return TOPIC_PROGRESSION.containsKey(currentTopic);
    }
    
    /**
     * Marca un tema como completado y desbloquea el siguiente
     * @param context Contexto de la aplicación
     * @param topic Tema completado
     * @param score Puntuación obtenida (debe ser >= 70 para aprobar)
     */
    public static void markTopicCompleted(Context context, String topic, int score) {
        if (score >= 70) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Manejar temas de Writing específicamente
            String progressKey;
            if (isWritingTopic(topic)) {
                progressKey = "PASSED_WRITING_" + topic.toUpperCase().replace(" ", "_");
            } else {
                progressKey = TOPIC_TO_PROGRESS_KEY.get(topic);
            }
            
            if (progressKey != null) {
                editor.putBoolean(progressKey, true);
                editor.apply();
            }
        }
    }

    /**
     * Verifica si un tema es de Writing
     * @param topic Tema a verificar
     * @return true si es un tema de Writing
     */
    private static boolean isWritingTopic(String topic) {
        return "ALPHABET".equals(topic) || 
               "NUMBERS".equals(topic) || 
               "COLORS".equals(topic) || 
               "PERSONAL PRONOUNS".equals(topic) || 
               "POSSESSIVE ADJECTIVES".equals(topic);
    }
    
    /**
     * Crea un Intent para continuar con el siguiente tema (vista principal de contenido)
     * @param context Contexto actual
     * @param currentTopic Tema actual completado
     * @param activityType Tipo de actividad (no se usa, mantenido por compatibilidad)
     * @return Intent para la actividad principal del siguiente tema o null si no hay más temas
     */
    public static Intent createContinueIntent(Context context, String currentTopic, String activityType) {
        // Paso 1: si desbloquea Speaking
        if (doesTopicUnlockSpeaking(context, currentTopic)) {
            Intent speakingIntent = new Intent(context, MenuSpeakingActivity.class);
            speakingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return speakingIntent;
        }

        // Paso 2: si desbloquea Reading (después de Speaking Start completado)
        if (doesTopicUnlockReading(context, currentTopic)) {
            Intent readingIntent = new Intent(context, MenuReadingActivity.class);
            readingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return readingIntent;
        }
        
        // LÓGICA ORIGINAL: Continuar con el siguiente tema de listening
        String nextTopic = getNextTopic(context, currentTopic);
        
        if (nextTopic == null) {
            return null; // No hay más temas
        }
        
        Intent intent = null;
        
        // Crear intent hacia la actividad principal de contenido del siguiente tema
        switch (nextTopic) {
            case "ALPHABET":
                intent = new Intent(context, AlphabetActivity.class);
                break;
            case "NUMBERS":
                intent = new Intent(context, NumberActivity.class);
                break;
            case "COLORS":
                intent = new Intent(context, ColorActivity.class);
                break;
            case "PERSONAL PRONOUNS":
                intent = new Intent(context, PronounsActivity.class);
                break;
            case "POSSESSIVE ADJECTIVES":
                intent = new Intent(context, PossessiveAdjectActivity.class);
                break;
            // Para temas que no tienen actividad principal específica, ir al menú
            case "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION":
                intent = new Intent(context, MenuA1Activity.class);
                break;
            case "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)":
                intent = new Intent(context, MenuA1Activity.class);
                break;
            default:
                // Por defecto, ir al menú principal A1
                intent = new Intent(context, MenuA1Activity.class);
                break;
        }
        
        if (intent != null) {
            // Para las actividades principales, no necesitamos pasar TOPIC/LEVEL como extra
            // ya que cada actividad tiene su contenido específico
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        
        return intent;
    }
    
    /**
     * Obtiene un mensaje descriptivo para el botón de continuar
     * @param nextTopic Siguiente tema
     * @return Mensaje descriptivo
     */
    public static String getContinueButtonText(String nextTopic) {
        if (nextTopic == null) {
            return "¡Nivel completado!";
        }
        
        // Simplificar nombres largos para el botón
        String simplifiedName = nextTopic;
        if (nextTopic.contains("PREPOSITIONS")) {
            simplifiedName = "PREPOSITIONS";
        } else if (nextTopic.contains("ADJECTIVES")) {
            simplifiedName = "ADJECTIVES";
        }
        
        //return "Continuar: " + simplifiedName;
        return "Continuar";
    }
    
    /**
     * Obtiene un mensaje descriptivo para el botón de continuar (versión mejorada)
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @return Mensaje descriptivo personalizado
     */
    public static String getContinueButtonTextEnhanced(Context context, String currentTopic) {
        // Si este tema desbloquea Speaking
        if (doesTopicUnlockSpeaking(context, currentTopic)) {
            return "🗣️ ¡Desbloquear Speaking!";
        }

        // Si desbloquea Reading
        if (doesTopicUnlockReading(context, currentTopic)) {
            return "📖 ¡Desbloquear Reading!";
        }
        
        // Usar lógica original para otros casos
        String nextTopic = getNextTopic(context, currentTopic);
        return getContinueButtonText(nextTopic);
    }

    /**
     * Obtiene el siguiente tema en la secuencia de Reading
     * @param currentTopic Tema actual
     * @return Siguiente tema o null si es el último
     */
    public static String getNextReadingTopic(String currentTopic) {
        if ("ALPHABET".equals(currentTopic)) {
            return "NUMBERS";
        } else if ("NUMBERS".equals(currentTopic)) {
            return "COLORS";
        } else if ("COLORS".equals(currentTopic)) {
            return "PERSONAL PRONOUNS";
        } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
            return "POSSESSIVE ADJECTIVES";
        }
        // POSSESSIVE ADJECTIVES es el último tema de Reading
        return null;
    }

    /**
     * Verifica si un tema desbloquea Writing
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @return true si desbloquea Writing
     */
    public static boolean doesTopicUnlockWriting(Context context, String currentTopic) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return "POSSESSIVE ADJECTIVES".equals(currentTopic) && 
               prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false);
    }

    /**
     * Obtiene el siguiente tema en la secuencia de Writing
     * @param currentTopic Tema actual
     * @return Siguiente tema o null si es el último
     */
    public static String getNextWritingTopic(String currentTopic) {
        if ("ALPHABET".equals(currentTopic)) {
            return "NUMBERS";
        } else if ("NUMBERS".equals(currentTopic)) {
            return "COLORS";
        } else if ("COLORS".equals(currentTopic)) {
            return "PERSONAL PRONOUNS";
        } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
            return "POSSESSIVE ADJECTIVES";
        }
        // POSSESSIVE ADJECTIVES es el último tema de Writing
        return null;
    }

    /**
     * Verifica si un tema de Writing desbloquea el siguiente módulo (Listening)
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @return true si desbloquea el siguiente módulo
     */
    public static boolean doesWritingTopicUnlockNextModule(Context context, String currentTopic) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return "POSSESSIVE ADJECTIVES".equals(currentTopic) && 
               prefs.getBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", false);
    }

    // Temas de Image Identification
    private static final String[] IMAGE_IDENTIFICATION_TOPICS = {
        "ALPHABET",
        "NUMBERS",
        "COLORS",
        "PERSONAL PRONOUNS",
        "POSSESSIVE ADJECTIVES"
    };

    /**
     * Obtiene el siguiente tema en la secuencia de Image Identification
     * @param currentTopic Tema actual
     * @return Siguiente tema o null si es el último
     */
    public static String getNextImageIdentificationTopic(String currentTopic) {
        if ("ALPHABET".equals(currentTopic)) {
            return "NUMBERS";
        } else if ("NUMBERS".equals(currentTopic)) {
            return "COLORS";
        } else if ("COLORS".equals(currentTopic)) {
            return "PERSONAL PRONOUNS";
        } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
            return "POSSESSIVE ADJECTIVES";
        }
        // Si llegó aquí, ya completó todos los temas de Image Identification
        return null;
    }

    /**
     * Verifica si un tema de Image Identification desbloquea el siguiente módulo
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @return true si desbloquea el siguiente módulo
     */
    public static boolean doesImageIdentificationTopicUnlockNextModule(Context context, String currentTopic) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return "POSSESSIVE ADJECTIVES".equals(currentTopic) && 
               prefs.getBoolean("PASSED_IMAGE_IDENTIFICATION_POSSESSIVE_ADJECTIVES", false);
    }

    /**
     * Obtiene el siguiente tema en la secuencia de Image Identification según el mapa de origen
     * @param currentTopic Tema actual
     * @param sourceMap Mapa desde donde se accedió ("LISTENING" o "READING")
     * @return Siguiente tema o null si es el último
     */
    public static String getNextImageIdentificationTopicBySource(String currentTopic, String sourceMap) {
        if ("LISTENING".equals(sourceMap)) {
            // Progresión del mapa de Listening
            if ("ALPHABET".equals(currentTopic)) {
                return "NUMBERS";
            } else if ("NUMBERS".equals(currentTopic)) {
                return "COLORS";
            } else if ("COLORS".equals(currentTopic)) {
                return "PERSONAL PRONOUNS";
            } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
                return "POSSESSIVE ADJECTIVES";
            } else if ("POSSESSIVE ADJECTIVES".equals(currentTopic)) {
                return "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION";
            } else if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equals(currentTopic)) {
                return "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)";
            }
        } else if ("READING".equals(sourceMap)) {
            // Progresión del mapa de Reading
            if ("ALPHABET".equals(currentTopic)) {
                return "NUMBERS";
            } else if ("NUMBERS".equals(currentTopic)) {
                return "COLORS";
            } else if ("COLORS".equals(currentTopic)) {
                return "PERSONAL PRONOUNS";
            } else if ("PERSONAL PRONOUNS".equals(currentTopic)) {
                return "POSSESSIVE ADJECTIVES";
            }
            // POSSESSIVE ADJECTIVES es el último tema de Reading
        }
        // Si llegó aquí, ya completó todos los temas del mapa correspondiente
        return null;
    }

    /**
     * Determina qué actividad usar para el siguiente tema en el mapa de Reading
     * @param nextTopic Siguiente tema
     * @return Clase de la actividad a usar
     */
    public static Class<?> getReadingActivityClass(String nextTopic) {
        if ("ALPHABET".equals(nextTopic)) {
            return ImageIdentificationActivity.class;
        } else if ("NUMBERS".equals(nextTopic)) {
            return ImageIdentificationAudioActivity.class;
        } else if ("COLORS".equals(nextTopic)) {
            return ImageIdentificationActivity.class;
        } else if ("PERSONAL PRONOUNS".equals(nextTopic)) {
            return TranslationReadingActivity.class;
        } else if ("POSSESSIVE ADJECTIVES".equals(nextTopic)) {
            return TranslationReadingActivity.class;
        }
        return TranslationReadingActivity.class; // Default fallback
    }

    /**
     * Determina qué actividad usar para el siguiente tema en el mapa de Writing
     * @param nextTopic Siguiente tema
     * @return Clase de la actividad a usar
     */
    public static Class<?> getWritingActivityClass(String nextTopic) {
        // Para Writing, todos los temas usan WritingActivity
        return WritingActivity.class;
    }

    /**
     * Verifica si un tema de Image Identification desbloquea el siguiente módulo según el mapa de origen
     * @param context Contexto de la aplicación
     * @param currentTopic Tema actual completado
     * @param sourceMap Mapa desde donde se accedió ("LISTENING" o "READING")
     * @return true si desbloquea el siguiente módulo
     */
    public static boolean doesImageIdentificationTopicUnlockNextModuleBySource(Context context, String currentTopic, String sourceMap) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        if ("LISTENING".equals(sourceMap)) {
            // En Listening, el último tema es ADJECTIVES
            return "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(currentTopic) && 
                   prefs.getBoolean("PASSED_IMAGE_IDENTIFICATION_ADJECTIVES", false);
        } else if ("READING".equals(sourceMap)) {
            // En Reading, el último tema es POSSESSIVE ADJECTIVES
            return "POSSESSIVE ADJECTIVES".equals(currentTopic) && 
                   prefs.getBoolean("PASSED_IMAGE_IDENTIFICATION_POSSESSIVE_ADJECTIVES", false);
        }
        return false;
    }

    /**
     * Obtiene la clase del mapa de destino según el mapa de origen
     * @param sourceMap Mapa desde donde se accedió ("LISTENING" o "READING")
     * @return Clase del mapa de destino
     */
    public static Class<?> getDestinationMapClass(String sourceMap) {
        if ("LISTENING".equals(sourceMap)) {
            return MenuA1Activity.class;
        } else if ("READING".equals(sourceMap)) {
            return MenuReadingActivity.class;
        }
        return MenuA1Activity.class; // Default fallback
    }

    /**
     * Obtiene el siguiente módulo en la progresión cuando se completa el último tema de un módulo
     * @param sourceMap Mapa actual ("LISTENING", "SPEAKING", "READING", "WRITING")
     * @return Clase del siguiente módulo
     */
    public static Class<?> getNextModuleClass(String sourceMap) {
        if ("LISTENING".equals(sourceMap)) {
            return MenuSpeakingActivity.class; // Listening → Speaking
        } else if ("SPEAKING".equals(sourceMap)) {
            return MenuReadingActivity.class; // Speaking → Reading
        } else if ("READING".equals(sourceMap)) {
            return MenuWritingActivity.class; // Reading → Writing
        } else if ("WRITING".equals(sourceMap)) {
            return MenuA1Activity.class; // Writing → Volver al inicio (Listening)
        }
        return MenuA1Activity.class; // Default fallback
    }

    /**
     * Obtiene el siguiente tema específico cuando se completa el último tema de Writing
     * @return El tema específico a desbloquear en Listening
     */
    public static String getNextTopicAfterWriting() {
        return "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION";
    }

    /**
     * Verifica si es el último tema de un módulo y debe desbloquear el siguiente módulo
     * @param currentTopic Tema actual
     * @param sourceMap Mapa actual
     * @return true si es el último tema del módulo
     */
    public static boolean isLastTopicOfModule(String currentTopic, String sourceMap) {
        if ("LISTENING".equals(sourceMap)) {
            // En Listening, el último tema es ADJECTIVES
            return "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(currentTopic);
        } else if ("SPEAKING".equals(sourceMap)) {
            // En Speaking, el último tema es POSSESSIVE ADJECTIVES
            return "POSSESSIVE ADJECTIVES".equals(currentTopic);
        } else if ("READING".equals(sourceMap)) {
            // En Reading, el último tema es POSSESSIVE ADJECTIVES
            return "POSSESSIVE ADJECTIVES".equals(currentTopic);
        } else if ("WRITING".equals(sourceMap)) {
            // En Writing, el último tema es POSSESSIVE ADJECTIVES
            return "POSSESSIVE ADJECTIVES".equals(currentTopic);
        }
        return false;
    }
}

