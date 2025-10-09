package com.example.speak.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.LinearLayout;

import com.example.speak.HelpActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpContentHelper {
    private static final String TAG = "HelpContentHelper";
    
    private Context context;
    private AssetManager assetManager;
    
    public HelpContentHelper(Context context) {
        this.context = context;
        this.assetManager = context.getAssets();
    }
    
    /**
     * Carga el contenido de ayuda para un tema específico
     */
    public List<HelpActivity.HelpSection> loadHelpContent(String topic, String level) {
        List<HelpActivity.HelpSection> helpSections = new ArrayList<>();
        
        Log.d(TAG, "Loading help content for topic: " + topic + ", level: " + level);
        
        try {
            // Determinar qué archivo cargar según el tema
            String fileName = getFileNameForTopic(topic, level);
            Log.d(TAG, "File to load: " + fileName);
            
            if (fileName != null) {
                helpSections = parseHelpFile(fileName, topic);
                Log.d(TAG, "Parsed " + helpSections.size() + " help sections");
            } else {
                Log.w(TAG, "No file found for topic: " + topic);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading help content", e);
        }
        
        return helpSections;
    }
    
    /**
     * Determina el nombre del archivo a cargar según el tema y nivel
     */
    private String getFileNameForTopic(String topic, String level) {
        if (topic == null) return null;
        
        switch (topic.toUpperCase()) {
            case "ALPHABET":
                return "help_content.txt";
            case "NUMBERS":
                return "help_content.txt";
            case "COLORS":
                return "help_content.txt";
            case "PERSONAL PRONOUNS":
                return "help_content.txt";
            case "POSSESSIVE ADJECTIVES":
                return "help_content.txt";
            case "DAYS OF THE WEEK":
                return "help_content.txt";
            case "FAMILY MEMBERS":
                return "help_content.txt";
            case "CLASSROOM OBJECTS":
                return "help_content.txt";
            case "GREETINGS":
                return "pronunciation_topics.txt";
            case "DAYS":
                return "pronunciation_topics.txt";
            case "FAMILY":
                return "pronunciation_topics.txt";
            case "GRAMMAR":
                return "grammar_topics.txt";
            case "VOCABULARY":
                return "vocabulary_topics.txt";
            case "PRONUNCIATION":
                return "pronunciation_topics.txt";
            case "CONVERSATION":
                return "conversation_topics.txt";
            default:
                return "help_content.txt";
        }
    }
    
    /**
     * Parsea el archivo de ayuda y extrae las secciones relevantes
     */
    private List<HelpActivity.HelpSection> parseHelpFile(String fileName, String topic) throws IOException {
        List<HelpActivity.HelpSection> helpSections = new ArrayList<>();
        
        Log.d(TAG, "Parsing file: " + fileName + " for topic: " + topic);
        
        InputStream inputStream = assetManager.open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        String line;
        String currentTopic = null;
        String currentLevel = null;
        List<String> currentQuestions = new ArrayList<>();
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.startsWith("Topic:")) {
                // Si tenemos un tema anterior, crear la sección de ayuda
                if (currentTopic != null && currentTopic.equalsIgnoreCase(topic) && !currentQuestions.isEmpty()) {
                    Log.d(TAG, "Creating help section for topic: " + currentTopic + " with " + currentQuestions.size() + " questions");
                    
                    if (currentTopic.equalsIgnoreCase("ALPHABET")) {
                        // Para el alfabeto, crear múltiples secciones
                        List<HelpActivity.HelpSection> alphabetSections = createAlphabetHelpSection(currentQuestions);
                        helpSections.addAll(alphabetSections);
                        Log.d(TAG, "Added " + alphabetSections.size() + " alphabet sections");
                    } else {
                        // Para otros temas, crear una sola sección
                        helpSections.add(createHelpSection(currentTopic, currentQuestions));
                    }
                }
                
                // Nuevo tema
                currentTopic = line.substring(6).trim();
                currentQuestions.clear();
                Log.d(TAG, "Found topic: " + currentTopic);
                
            } else if (line.startsWith("Level:")) {
                currentLevel = line.substring(6).trim();
                Log.d(TAG, "Found level: " + currentLevel);
                
            } else if (line.startsWith("Q:") && currentTopic != null) {
                String question = line.substring(2).trim();
                if (!question.isEmpty()) {
                    currentQuestions.add(question);
                    Log.d(TAG, "Added question: " + question);
                }
            }
        }
        
        // Agregar la última sección si corresponde
        if (currentTopic != null && currentTopic.equalsIgnoreCase(topic) && !currentQuestions.isEmpty()) {
            Log.d(TAG, "Creating final help section for topic: " + currentTopic + " with " + currentQuestions.size() + " questions");
            
            if (currentTopic.equalsIgnoreCase("ALPHABET")) {
                // Para el alfabeto, crear múltiples secciones
                List<HelpActivity.HelpSection> alphabetSections = createAlphabetHelpSection(currentQuestions);
                helpSections.addAll(alphabetSections);
                Log.d(TAG, "Added " + alphabetSections.size() + " alphabet sections");
            } else {
                // Para otros temas, crear una sola sección
                helpSections.add(createHelpSection(currentTopic, currentQuestions));
            }
        }
        
        reader.close();
        inputStream.close();
        
        Log.d(TAG, "Total help sections created: " + helpSections.size());
        return helpSections;
    }
    
    /**
     * Crea una sección de ayuda basada en el tema y las preguntas
     */
    private HelpActivity.HelpSection createHelpSection(String topic, List<String> questions) {
        Log.d(TAG, "Creating help section for topic: " + topic + " with " + questions.size() + " questions");
        
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        
        switch (topic.toUpperCase()) {
            case "ALPHABET":
                Log.d(TAG, "Creating ALPHABET help section");
                // Para el alfabeto, solo retornamos la primera sección
                // Las demás secciones se manejan en el método principal
                List<HelpActivity.HelpSection> alphabetSections = createAlphabetHelpSection(questions);
                if (!alphabetSections.isEmpty()) {
                    section = alphabetSections.get(0);
                }
                break;
            case "NUMBERS":
                Log.d(TAG, "Creating NUMBERS help section");
                section = createNumbersHelpSection(questions);
                break;
            case "COLORS":
                Log.d(TAG, "Creating COLORS help section");
                section = createColorsHelpSection(questions);
                break;
            case "PERSONAL PRONOUNS":
                Log.d(TAG, "Creating PERSONAL PRONOUNS help section");
                section = createGenericHelpSection(topic, questions);
                break;
            case "POSSESSIVE ADJECTIVES":
                Log.d(TAG, "Creating POSSESSIVE ADJECTIVES help section");
                section = createGenericHelpSection(topic, questions);
                break;
            case "DAYS OF THE WEEK":
                Log.d(TAG, "Creating DAYS OF THE WEEK help section");
                section = createGenericHelpSection(topic, questions);
                break;
            case "FAMILY MEMBERS":
                Log.d(TAG, "Creating FAMILY MEMBERS help section");
                section = createGenericHelpSection(topic, questions);
                break;
            case "CLASSROOM OBJECTS":
                Log.d(TAG, "Creating CLASSROOM OBJECTS help section");
                section = createGenericHelpSection(topic, questions);
                break;
            case "GRAMMAR":
                Log.d(TAG, "Creating GRAMMAR help section");
                section = createGrammarHelpSection(questions);
                break;
            case "VOCABULARY":
                Log.d(TAG, "Creating VOCABULARY help section");
                section = createVocabularyHelpSection(questions);
                break;
            case "PRONUNCIATION":
                Log.d(TAG, "Creating PRONUNCIATION help section");
                section = createPronunciationHelpSection(questions);
                break;
            case "CONVERSATION":
                Log.d(TAG, "Creating CONVERSATION help section");
                section = createConversationHelpSection(questions);
                break;
            default:
                Log.d(TAG, "Creating GENERIC help section for topic: " + topic);
                section = createGenericHelpSection(topic, questions);
                break;
        }
        
        Log.d(TAG, "Created section with title: " + section.title + ", letters: " + section.letters.length);
        return section;
    }
    
    /**
     * Crea secciones de ayuda para el alfabeto
     */
    private List<HelpActivity.HelpSection> createAlphabetHelpSection(List<String> questions) {
        Log.d(TAG, "Creating alphabet help sections with " + questions.size() + " questions");
        
        // Dividir las preguntas en 2 grupos para 2 secciones
        List<String> group1 = new ArrayList<>();
        List<String> group2 = new ArrayList<>();
        List<String> group3 = new ArrayList<>();
        List<String> group4 = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            if (question.contains("[") || question.length() <= 3) {
                //if (i < questions.size() / 2) {
                //group1.add(question);
                // Solo agregar A, H, J, K a la primera parte
                // Primera parte: A, H, J, K
                if (question.contains("A [ei]") ||
                        question.contains("H [eitʃ]") ||
                        question.contains("J [dʒei]") ||
                        question.contains("K [kei]")) {
                    group1.add(question);
                }
                // Segunda parte: B, C, D, E, G, P, T, V, Z
                else if (question.contains("B [bi:]") ||
                        question.contains("C [si:]") ||
                        question.contains("D [di:]") ||
                        question.contains("E [i:]") ||
                        question.contains("G [dʒi:]") ||
                        question.contains("P [pi:]") ||
                        question.contains("T [ti:]") ||
                        question.contains("V [vi:]") ||
                        question.contains("Z [zi:]/[zed]")) {
                    group2.add(question);
                }
                // Tercera parte: resto de letras
                else if (question.contains("F [ef]") ||
                        question.contains("L [el]") ||
                        question.contains("M [em]") ||
                        question.contains("N [en]") ||
                        question.contains("S [es]") ||
                        question.contains("X [eks]")) {
                    group3.add(question);
                }
                // Cuarta parte: resto de letras
                else if (question.contains("I [ai]") ||
                        question.contains("Y [wai")
                       ) {
                    group4.add(question);
                }
                else {
                    //group5.add(question);
                }
            }
        }
        
        // Crear 2 secciones separadas
        List<HelpActivity.HelpSection> sections = new ArrayList<>();
        
        // Sección 1: Primera mitad de letras
        HelpActivity.HelpSection section1 = new HelpActivity.HelpSection();
        section1.title = "Alfabeto - Parte 1 / Alphabet - Part 1";
        section1.centralSound = "A-H-J-K";
        section1.audioResource = "alphabet_help_ei";
        section1.hasSequentialImages = true;
        section1.imageResources = new String[]{
                "grammar_rule_1",
                "mouth_aei",
                "mouth_th",
                "mouth_ch_j_sh",
                "mouth_cdgknstxyz",
        };
        section1.imageDisplayOrder = 1;
        section1.imageDescription = "Regla gramatical 1 - Estructura del alfabeto";
        section1.currentImageIndex = 0;
        section1.letters = group1.toArray(new String[0]);
        sections.add(section1);
        
        // Sección 2: Segunda mitad de letras
        HelpActivity.HelpSection section2 = new HelpActivity.HelpSection();
        section2.title = "Alfabeto - Parte 2 / Alphabet - Part 2";
        section2.centralSound = "B-C-D-E-G-P-T-V-Z";
        section2.audioResource = "alphabet_help_i";
        section2.hasSequentialImages = true;
        section2.imageResources = new String[]{
                "grammar_rule_2",
                "mouth_bmp",
                "alphabet_part2_2", 
                "alphabet_part2_3",
                "alphabet_part2_4",
        };
        section2.imageDisplayOrder = 2;
        section2.imageDescription = "Regla gramatical 2 - Pronunciación fonética";
        section2.currentImageIndex = 0;
        section2.letters = group2.toArray(new String[0]);
        sections.add(section2);

    // Sección 3: Segunda mitad de letras
    HelpActivity.HelpSection section3 = new HelpActivity.HelpSection();
    section3.title = "Alfabeto - Parte 3 / Alphabet - Part 3";
    section3.centralSound = "F-L-M-N-S-X";
    section3.audioResource = "alphabet_help_e";
    section3.hasSequentialImages = true;
    section3.imageResources = new String[]{
        "grammar_rule_3",
    };
    section3.imageDisplayOrder = 3;
    section3.imageDescription = "Regla gramatical 3 - Pronunciación fonética";
    section3.currentImageIndex = 0;
    section3.letters = group3.toArray(new String[0]);
        sections.add(section3);

        // Sección 4: Cuarte parte de letras
        HelpActivity.HelpSection section4 = new HelpActivity.HelpSection();
        section4.title = "Alfabeto - Parte 4 / Alphabet - Part 4";
        section4.centralSound = "I-Y";
        section4.audioResource = "alphabet_help_ai";
        section4.hasSequentialImages = true;
        section4.imageResources = new String[]{
                "grammar_rule_4",
        };
        section4.imageDisplayOrder = 4;
        section4.imageDescription = "Regla gramatical 4 - Pronunciación fonética";
        section4.currentImageIndex = 0;
        section4.letters = group4.toArray(new String[0]);
        sections.add(section4);
        
        Log.d(TAG, "Created " + sections.size() + " alphabet sections");
        return sections;
    }
    
    /**
     * Crea sección de ayuda para números
     */
    private HelpActivity.HelpSection createNumbersHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Números / Numbers";
        section.centralSound = "1-20";
        section.audioResource = "numbers_help_1_10"; // Usar un audio específico del mapeo
        
        // Configurar imágenes secuenciales para números
        section.hasSequentialImages = true;
        section.imageResources = new String[]{"numbers_help_1", "numbers_help_2"};
        section.imageDisplayOrder = 2;
        section.imageDescription = "Números del 1 al 20 en inglés";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de números
        List<String> numbers = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().matches(".*\\d+.*") || 
                question.toLowerCase().matches(".*(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty).*")) {
                numbers.add(question);
            }
        }
        
        section.letters = numbers.toArray(new String[0]);
        return section;
    }
    
    /**
     * Crea sección de ayuda para colores
     */
    private HelpActivity.HelpSection createColorsHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Colores / Colors";
        section.centralSound = "colors";
        section.audioResource = "colors_help_basic"; // Usar un audio específico del mapeo
        
        // Configurar imágenes secuenciales para colores
        section.hasSequentialImages = true;
        section.imageResources = new String[]{"colors_help_1", "colors_help_2"};
        section.imageDisplayOrder = 3;
        section.imageDescription = "Colores básicos en inglés";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de colores
        List<String> colors = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().matches(".*(red|blue|green|yellow|black|white|purple|orange|brown|pink).*")) {
                colors.add(question);
            }
        }
        
        section.letters = colors.toArray(new String[0]);
        return section;
    }
    
    /**
     * Crea sección de ayuda genérica
     */
    private HelpActivity.HelpSection createGenericHelpSection(String topic, List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = topic + " / " + topic;
        section.centralSound = topic.substring(0, Math.min(3, topic.length())).toUpperCase();
        section.audioResource = topic.toLowerCase() + "_help";
        section.letters = questions.toArray(new String[0]);
        return section;
    }
    
    /**
     * Obtiene la lista de temas disponibles
     */
    public List<String> getAvailableTopics() {
        List<String> topics = new ArrayList<>();
        
        try {
            // Leer desde el archivo de temas básicos
            InputStream inputStream = assetManager.open("A1.1_Basic_English_Topics.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Topic:")) {
                    String topic = line.substring(6).trim();
                    if (!topics.contains(topic)) {
                        topics.add(topic);
                    }
                }
            }
            
            reader.close();
            inputStream.close();
            
            // Leer desde el nuevo archivo de ayuda
            inputStream = assetManager.open("help_content.txt");
            reader = new BufferedReader(new InputStreamReader(inputStream));
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Topic:")) {
                    String topic = line.substring(6).trim();
                    if (!topics.contains(topic)) {
                        topics.add(topic);
                    }
                }
            }
            
            reader.close();
            inputStream.close();
            
            // Leer desde el archivo de pronunciación
            inputStream = assetManager.open("pronunciation_topics.txt");
            reader = new BufferedReader(new InputStreamReader(inputStream));
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Topic:")) {
                    String topic = line.substring(6).trim();
                    if (!topics.contains(topic)) {
                        topics.add(topic);
                    }
                }
            }
            
            reader.close();
            inputStream.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading available topics", e);
        }
        
        return topics;
    }
    
    /**
     * Crea sección de ayuda para gramática con imágenes secuenciales
     */
    private HelpActivity.HelpSection createGrammarHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Gramática / Grammar";
        section.centralSound = "GRAM";
        section.audioResource = "grammar_help";
        
        // Configurar imágenes secuenciales para gramática
        section.hasSequentialImages = true;
        section.imageResources = new String[]{
            "grammar_rule_1",      // Imagen de la regla 1
                "mouth_aei",       // mouth_aei
                "mouth_th",       // mouth_th
                "mouth_ch_j_sh",       // mouth_ch_j_sh
                "mouth_cdgknstxyz",    // mouth_cdgknstxyz
            "grammar_rule_2",      // Imagen de la regla 2
            "grammar_rule_3",      // Imagen de la regla 3
                "grammar_rule_4",      // Imagen de la regla 4
            "grammar_example_1",   // Ejemplo visual 1
            "grammar_example_2"    // Ejemplo visual 2
        };
        section.imageDisplayOrder = 1;
        section.imageDescription = "Reglas gramaticales paso a paso / Grammar rules step by step";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de reglas gramaticales
        List<String> rules = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().contains("rule") || 
                question.toLowerCase().contains("grammar") ||
                question.toLowerCase().contains("tense")) {
                rules.add(question);
            }
        }
        
        section.letters = rules.toArray(new String[0]);
        return section;
    }
    
    /**
     * Crea sección de ayuda para vocabulario con imágenes secuenciales
     */
    private HelpActivity.HelpSection createVocabularyHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Vocabulario / Vocabulary";
        section.centralSound = "VOC";
        section.audioResource = "vocabulary_help";
        
        // Configurar imágenes secuenciales para vocabulario
        section.hasSequentialImages = true;
        section.imageResources = new String[]{
            "vocab_category_1",    // Categoría 1
            "vocab_category_2",    // Categoría 2
            "vocab_category_3",    // Categoría 3
            "vocab_context_1",     // Contexto de uso 1
            "vocab_context_2"      // Contexto de uso 2
        };
        section.imageDisplayOrder = 2;
        section.imageDescription = "Categorías de vocabulario / Vocabulary categories";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de vocabulario
        List<String> vocab = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().contains("word") || 
                question.toLowerCase().contains("vocabulary") ||
                question.toLowerCase().contains("meaning")) {
                vocab.add(question);
            }
        }
        
        section.letters = vocab.toArray(new String[0]);
        return section;
    }
    
    /**
     * Crea sección de ayuda para pronunciación con imágenes secuenciales
     */
    private HelpActivity.HelpSection createPronunciationHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Pronunciación / Pronunciation";
        section.centralSound = "PRO";
        section.audioResource = "pronunciation_help";
        
        // Configurar imágenes secuenciales para pronunciación
        section.hasSequentialImages = true;
        section.imageResources = new String[]{
            "pron_mouth_1",        // Posición de boca 1
            "pron_mouth_2",        // Posición de boca 2
            "pron_mouth_3",        // Posición de boca 3
            "pron_tongue_1",       // Posición de lengua 1
            "pron_tongue_2"        // Posición de lengua 2
        };
        section.imageDisplayOrder = 3;
        section.imageDescription = "Posiciones de boca y lengua / Mouth and tongue positions";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de pronunciación
        List<String> pronunciation = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().contains("pronounce") || 
                question.toLowerCase().contains("sound") ||
                question.toLowerCase().contains("accent")) {
                pronunciation.add(question);
            }
        }
        
        section.letters = pronunciation.toArray(new String[0]);
        return section;
    }
    
    /**
     * Crea sección de ayuda para conversación con imágenes secuenciales
     */
    private HelpActivity.HelpSection createConversationHelpSection(List<String> questions) {
        HelpActivity.HelpSection section = new HelpActivity.HelpSection();
        section.title = "Conversación / Conversation";
        section.centralSound = "CON";
        section.audioResource = "conversation_help";
        
        // Configurar imágenes secuenciales para conversación
        section.hasSequentialImages = true;
        section.imageResources = new String[]{
            "conv_situation_1",    // Situación 1
            "conv_situation_2",    // Situación 2
            "conv_situation_3",    // Situación 3
            "conv_gestures_1",     // Gestos 1
            "conv_gestures_2"      // Gestos 2
        };
        section.imageDisplayOrder = 4;
        section.imageDescription = "Situaciones de conversación / Conversation situations";
        section.currentImageIndex = 0;
        
        // Convertir preguntas a formato de conversación
        List<String> conversation = new ArrayList<>();
        for (String question : questions) {
            if (question.toLowerCase().contains("conversation") || 
                question.toLowerCase().contains("dialogue") ||
                question.toLowerCase().contains("speak")) {
                conversation.add(question);
            }
        }
        
        section.letters = conversation.toArray(new String[0]);
        return section;
    }
}
