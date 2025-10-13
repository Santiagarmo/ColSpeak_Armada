package com.example.speak.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper para manejar imágenes secuenciales y ordenadas
 * Soporta carga desde drawables y assets
 */
public class SequentialImageHelper {
    private static final String TAG = "SequentialImageHelper";
    
    private Context context;
    private AssetManager assetManager;
    private Map<String, String> drawableResourceMap;
    private Map<String, String> assetImageMap;
    
    public SequentialImageHelper(Context context) {
        this.context = context;
        this.assetManager = context.getAssets();
        initializeImageMappings();
    }
    
    /**
     * Inicializa el mapeo de recursos de imágenes
     */
    private void initializeImageMappings() {
        // Mapeo para drawables
        drawableResourceMap = new HashMap<>();
        
        // Imágenes de gramática
        drawableResourceMap.put("grammar_rule_1", "grammar_rule_1");
        drawableResourceMap.put("mouth_aei", "mouth_aei");
        drawableResourceMap.put("mouth_th", "mouth_th");
        drawableResourceMap.put("mouth_ch_j_sh", "mouth_ch_j_sh");
        drawableResourceMap.put("mouth_cdgknstxyz", "mouth_cdgknstxyz");
        drawableResourceMap.put("grammar_rule_2", "grammar_rule_2");
        drawableResourceMap.put("mouth_bmp", "mouth_bmp");
        drawableResourceMap.put("mouth_fv", "mouth_fv");
        drawableResourceMap.put("grammar_rule_3", "grammar_rule_3");
        drawableResourceMap.put("mouth_l", "mouth_l");
        drawableResourceMap.put("grammar_rule_4", "grammar_rule_4");
        drawableResourceMap.put("grammar_rule_5", "grammar_rule_5");
        drawableResourceMap.put("grammar_rule_6", "grammar_rule_6");
        drawableResourceMap.put("mouth_qw", "mouth_qw");
        drawableResourceMap.put("mouth_u", "mouth_u");
        drawableResourceMap.put("grammar_rule_7", "grammar_rule_7");
        drawableResourceMap.put("grammar_example_1", "help_icon");
        drawableResourceMap.put("grammar_example_2", "help_icon");
        
        // Imágenes de vocabulario
        drawableResourceMap.put("vocab_category_1", "help_icon");
        drawableResourceMap.put("vocab_category_2", "help_icon");
        drawableResourceMap.put("vocab_category_3", "help_icon");
        drawableResourceMap.put("vocab_context_1", "help_icon");
        drawableResourceMap.put("vocab_context_2", "help_icon");
        
        // Imágenes de pronunciación
        drawableResourceMap.put("pron_mouth_1", "help_icon");
        drawableResourceMap.put("pron_mouth_2", "help_icon");
        drawableResourceMap.put("pron_mouth_3", "help_icon");
        drawableResourceMap.put("pron_tongue_1", "help_icon");
        drawableResourceMap.put("pron_tongue_2", "help_icon");
        
        // Imágenes de conversación
        drawableResourceMap.put("conv_situation_1", "help_icon");
        drawableResourceMap.put("conv_situation_2", "help_icon");
        drawableResourceMap.put("conv_situation_3", "help_icon");
        drawableResourceMap.put("conv_gestures_1", "help_icon");
        drawableResourceMap.put("conv_gestures_2", "help_icon");
        
        // Imágenes del alfabeto
        drawableResourceMap.put("alphabet_help_1", "abc");
        drawableResourceMap.put("alphabet_help_2", "alphabet");
        drawableResourceMap.put("alphabet_help_3", "help_icon");
        drawableResourceMap.put("alphabet_part2_1", "alphabet_part2_1");
        drawableResourceMap.put("alphabet_part2_2", "alphabet_part2_2");
        drawableResourceMap.put("alphabet_part2_3", "alphabet_part2_3");
        drawableResourceMap.put("alphabet_part2_4", "alphabet_part2_4");
        
        // Imágenes de números
        drawableResourceMap.put("numbers_help_1", "numbers");
        drawableResourceMap.put("numbers_help_2", "ordinal");
        
        // Imágenes de colores
        drawableResourceMap.put("colors_help_1", "colors");
        drawableResourceMap.put("colors_help_2", "table_color");
        
        // Mapeo para assets
        assetImageMap = new HashMap<>();
        
        // Imágenes desde assets (si existen)
        assetImageMap.put("grammar_rule_1", "grammar/rule_1.png");
        assetImageMap.put("grammar_rule_2", "grammar/rule_2.png");
        assetImageMap.put("grammar_rule_3", "grammar/rule_3.png");
        assetImageMap.put("grammar_rule_4", "grammar/rule_4.png");
        assetImageMap.put("grammar_rule_5", "grammar/rule_5.png");
        assetImageMap.put("grammar_rule_6", "grammar/rule_6.png");
        assetImageMap.put("mouth_fv", "pronunciation/mouth_fv.png");
        assetImageMap.put("mouth_l", "pronunciation/mouth_l.png");
        assetImageMap.put("vocab_category_1", "vocabulary/category_1.png");
        assetImageMap.put("vocab_category_2", "vocabulary/category_2.png");
        assetImageMap.put("pron_mouth_1", "pronunciation/mouth_1.png");
        assetImageMap.put("pron_mouth_2", "pronunciation/mouth_2.png");
        assetImageMap.put("conv_situation_1", "conversation/situation_1.png");
        assetImageMap.put("conv_situation_2", "conversation/situation_2.png");
    }
    
    /**
     * Carga una imagen en el ImageView especificado
     */
    public void loadImage(String imageName, ImageView imageView) {
        Log.d(TAG, "loadImage called for: " + imageName);
        try {
            // Primero intentar cargar desde drawables
            String drawableName = drawableResourceMap.get(imageName);
            Log.d(TAG, "Drawable name from map: " + drawableName);
            if (drawableName != null) {
                int drawableId = context.getResources().getIdentifier(
                    drawableName, "drawable", context.getPackageName());
                Log.d(TAG, "Drawable ID: " + drawableId);
                if (drawableId != 0) {
                    Log.d(TAG, "Setting drawable resource: " + drawableId);
                    imageView.setImageResource(drawableId);
                    return;
                }
            }
            
            // Si no está en drawables, intentar desde assets
            String assetPath = assetImageMap.get(imageName);
            if (assetPath != null) {
                loadImageFromAssets(assetPath, imageView);
                return;
            }
            
            // Fallback: usar imagen por defecto
            int fallbackId = context.getResources().getIdentifier(
                "help_icon", "drawable", context.getPackageName());
            if (fallbackId != 0) {
                imageView.setImageResource(fallbackId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + imageName, e);
            // Fallback a imagen por defecto
            try {
                int fallbackId = context.getResources().getIdentifier(
                    "help_icon", "drawable", context.getPackageName());
                if (fallbackId != 0) {
                    imageView.setImageResource(fallbackId);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error loading fallback image", ex);
            }
        }
    }
    
    /**
     * Carga imagen desde assets
     */
    private void loadImageFromAssets(String assetPath, ImageView imageView) {
        try {
            InputStream inputStream = assetManager.open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from assets: " + assetPath, e);
            // Fallback a imagen por defecto
            try {
                int fallbackId = context.getResources().getIdentifier(
                    "help_icon", "drawable", context.getPackageName());
                if (fallbackId != 0) {
                    imageView.setImageResource(fallbackId);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error loading fallback image", ex);
            }
        }
    }
    
    /**
     * Verifica si una imagen existe
     */
    public boolean imageExists(String imageName) {
        return drawableResourceMap.containsKey(imageName) || 
               assetImageMap.containsKey(imageName);
    }
    
    /**
     * Obtiene la descripción de una imagen
     */
    public String getImageDescription(String imageName) {
        // Mapeo de descripciones específicas
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("grammar_rule_1", "Pronunciación A [ei], es similar a las pronunciaciones de las letras H [eitʃ], J [dʒei], K [kei]");
        descriptions.put("mouth_aei", "Posición de la boca para sonidos A E I");
        descriptions.put("mouth_th", "Posición de la boca para sonidos T H");
        descriptions.put("mouth_ch_j_sh", "Posición de la boca para sonidos CH J SH");
        descriptions.put("mouth_cdgknstxyz", "Posición de la boca para sonidos C D G K N S T X Y Z");
        descriptions.put("grammar_rule_2", "Pronunciación E [i:], es similar a las pronunciaciones de las letras B [bi:], C [si:], D [di:], G [dʒi:], P [pi:], T [ti:], V [vi:], Z [zi:]/[zed]");
        descriptions.put("mouth_bmp", "Posición de la boca para sonidos B M P");
        descriptions.put("mouth_cdgknstxyz", "Posición de la boca para sonidos C D G K N S T X Y Z");
        descriptions.put("mouth_aei", "Posición de la boca para sonidos A E I");
        descriptions.put("mouth_fv", "Posición de la boca para sonidos F V");
        descriptions.put("grammar_rule_3", "Pronunciación [e], es similar a las pronunciaciones de las letras F [ef], L [el], M [em], N [en], S [es], X [eks]");
        descriptions.put("mouth_fv", "Posición de la boca para sonidos F V");
        descriptions.put("mouth_cdgknstxyz", "Posición de la boca para sonidos C D G K N S T X Y Z");
        descriptions.put("mouth_l", "Posición de la boca para sonidos L");
        descriptions.put("mouth_bmp", "Posición de la boca para sonidos B M P");
        descriptions.put("grammar_rule_4", "Pronunciación [ai], es similar a las pronunciaciones de las letras I [ai], Y [wai]");
        descriptions.put("mouth_aei", "Posición de la boca para sonidos A E I");
        descriptions.put("mouth_cdgknstxyz", "Posición de la boca para sonidos C D G K N S T X Y Z");
        descriptions.put("grammar_rule_5", "Pronunciación [ou], es similar a las pronunciaciones de las letras O [ou]");
        descriptions.put("grammar_rule_6", "Pronunciación [ju], es similar a las pronunciaciones de las letras Q [kju:], U [ju:], W ['dʌbəl.ju:]");
        descriptions.put("mouth_qw", "Posición de la boca para sonidos Q W");
        descriptions.put("mouth_u", "Posición de la boca para sonidos U");
        descriptions.put("grammar_rule_7", "Pronunciación [ar], es similar a las pronunciaciones de las letras R [ar]");
        descriptions.put("vocab_category_1", "Business Terms - Professional vocabulary");
        descriptions.put("vocab_category_2", "Academic Words - Formal language");
        descriptions.put("pron_mouth_1", "Mouth Position 1 - /θ/ sound (think)");
        descriptions.put("pron_mouth_2", "Mouth Position 2 - /ð/ sound (this)");
        descriptions.put("conv_situation_1", "Formal Meeting - Professional conversation");
        descriptions.put("conv_situation_2", "Casual Chat - Informal dialogue");
        
        // Descripciones para alfabeto, números y colores
        descriptions.put("alphabet_help_1", "Alfabeto inglés - Letras A-M");
        descriptions.put("alphabet_help_2", "Alfabeto inglés - Letras N-Z");
        descriptions.put("alphabet_help_3", "Pronunciación fonética del alfabeto");
        descriptions.put("alphabet_part2_1", "Letras N-P - Sonidos nasales y labiales");
        descriptions.put("alphabet_part2_2", "Letras Q-T - Sonidos oclusivos y fricativos");
        descriptions.put("alphabet_part2_3", "Letras U-W - Sonidos vocálicos y semivocálicos");
        descriptions.put("alphabet_part2_4", "Letras X-Z - Sonidos fricativos y africados");
        descriptions.put("numbers_help_1", "Números del 1 al 10 en inglés");
        descriptions.put("numbers_help_2", "Números del 11 al 20 en inglés");
        descriptions.put("colors_help_1", "Colores básicos en inglés");
        descriptions.put("colors_help_2", "Colores adicionales en inglés");
        
        return descriptions.getOrDefault(imageName, "Image description not available");
    }
    
    /**
     * Obtiene el orden de visualización para un conjunto de imágenes
     */
    public int getImageDisplayOrder(String imageName) {
        // Mapeo de orden de visualización
        Map<String, Integer> displayOrder = new HashMap<>();
        
        // Gramática: orden lógico de reglas
        displayOrder.put("grammar_rule_1", 1);
        displayOrder.put("grammar_rule_2", 2);
        displayOrder.put("grammar_rule_3", 3);
        displayOrder.put("grammar_rule_4", 4);
        displayOrder.put("grammar_rule_5", 5);
        displayOrder.put("grammar_rule_6", 6);
        displayOrder.put("grammar_example_1", 4);
        displayOrder.put("grammar_example_2", 5);
        
        // Vocabulario: orden de categorías
        displayOrder.put("vocab_category_1", 1);
        displayOrder.put("vocab_category_2", 2);
        displayOrder.put("vocab_category_3", 3);
        displayOrder.put("vocab_context_1", 4);
        displayOrder.put("vocab_context_2", 5);
        
        // Pronunciación: orden de posiciones
        displayOrder.put("pron_mouth_1", 1);
        displayOrder.put("pron_mouth_2", 2);
        displayOrder.put("pron_mouth_3", 3);
        displayOrder.put("pron_tongue_1", 4);
        displayOrder.put("pron_tongue_2", 5);
        
        // Conversación: orden de situaciones
        displayOrder.put("conv_situation_1", 1);
        displayOrder.put("conv_situation_2", 2);
        displayOrder.put("conv_situation_3", 3);
        displayOrder.put("conv_gestures_1", 4);
        displayOrder.put("conv_gestures_2", 5);
        
        // Alfabeto, números y colores: orden secuencial
        displayOrder.put("alphabet_help_1", 1);
        displayOrder.put("alphabet_help_2", 2);
        displayOrder.put("alphabet_help_3", 3);
        displayOrder.put("numbers_help_1", 4);
        displayOrder.put("numbers_help_2", 5);
        displayOrder.put("colors_help_1", 6);
        displayOrder.put("colors_help_2", 7);
        
        return displayOrder.getOrDefault(imageName, 0);
    }
    
    /**
     * Obtiene el número total de imágenes para un tema
     */
    public int getTotalImagesForTopic(String topic) {
        switch (topic.toUpperCase()) {
            case "GRAMMAR":
                return 5; // 5 imágenes de reglas gramaticales
            case "VOCABULARY":
                return 5; // 5 imágenes de categorías
            case "PRONUNCIATION":
                return 5; // 5 imágenes de posiciones
            case "CONVERSATION":
                return 5; // 5 imágenes de situaciones
            case "ALPHABET":
                return 3; // 3 imágenes del alfabeto
            case "NUMBERS":
                return 2; // 2 imágenes de números
            case "COLORS":
                return 2; // 2 imágenes de colores
            default:
                return 0;
        }
    }
    
    /**
     * Obtiene la lista de imágenes para un tema específico
     */
    public String[] getImagesForTopic(String topic) {
        switch (topic.toUpperCase()) {
            case "GRAMMAR":
                return new String[]{
                    "grammar_rule_1", "grammar_rule_2", "grammar_rule_3", "grammar_rule_4", "grammar_rule_5","grammar_rule_6",
                    "grammar_example_1", "grammar_example_2"
                };
            case "VOCABULARY":
                return new String[]{
                    "vocab_category_1", "vocab_category_2", "vocab_category_3",
                    "vocab_context_1", "vocab_context_2"
                };
            case "PRONUNCIATION":
                return new String[]{
                    "pron_mouth_1", "pron_mouth_2", "pron_mouth_3",
                    "pron_tongue_1", "pron_tongue_2"
                };
            case "CONVERSATION":
                return new String[]{
                    "conv_situation_1", "conv_situation_2", "conv_situation_3",
                    "conv_gestures_1", "conv_gestures_2"
                };
            case "ALPHABET":
                return new String[]{
                    "alphabet_help_1", "alphabet_help_2", "alphabet_help_3"
                };
            case "NUMBERS":
                return new String[]{
                    "numbers_help_1", "numbers_help_2"
                };
            case "COLORS":
                return new String[]{
                    "colors_help_1", "colors_help_2"
                };
            default:
                return new String[0];
        }
    }
}
