package com.example.speak;

/**
 * Clase que define los diferentes tipos de voz disponibles en la aplicación
 */
public class VoiceType {
    private String name;
    private String displayName;
    private float pitch;
    private float speed;
    private int iconResource;
    
    public VoiceType(String name, String displayName, float pitch, float speed, int iconResource) {
        this.name = name;
        this.displayName = displayName;
        this.pitch = pitch;
        this.speed = speed;
        this.iconResource = iconResource;
    }
    
    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public float getPitch() { return pitch; }
    public float getSpeed() { return speed; }
    public int getIconResource() { return iconResource; }
    
    // Tipos de voz predefinidos - Configuración más realista
    public static final VoiceType NIÑO = new VoiceType(
        "niño", 
        "Niño / Child", 
        1.5f,  // Pitch muy alto - voz de niño
        0.8f,  // Velocidad un poco más lenta
        R.drawable.child_icon
    );
    
    public static final VoiceType NIÑA = new VoiceType(
        "niña", 
        "Niña / Girl", 
        1.8f,  // Pitch más alto que niño - voz de niña
        0.8f,  // Velocidad un poco más lenta
        R.drawable.girl_icon
    );
    
    public static final VoiceType MUJER = new VoiceType(
        "mujer", 
        "Mujer / Woman", 
        1.2f,  // Pitch ligeramente alto - voz femenina adulta
        0.9f,  // Velocidad normal
        R.drawable.woman_icon
    );
    
    public static final VoiceType HOMBRE = new VoiceType(
        "hombre", 
        "Hombre / Man", 
        0.9f,  // Pitch bajo - voz masculina gruesa
        0.9f,  // Velocidad normal
        R.drawable.man_icon
    );
    
    public static final VoiceType ABUELO = new VoiceType(
        "abuelo", 
        "Abuelo / Grandfather", 
        0.6f,  // Pitch muy bajo - voz gruesa de abuelo
        0.8f,  // Velocidad más lenta - habla pausada
        R.drawable.grandfather_icon
    );
    
    public static final VoiceType ABUELA = new VoiceType(
        "abuela", 
        "Abuela / Grandmother", 
        0.8f,  // Pitch bajo - voz de abuela
        0.8f,  // Velocidad más lenta - habla pausada
        R.drawable.grandmother_icon
    );
    
    // Array con todos los tipos de voz
    public static final VoiceType[] ALL_VOICES = {
        NIÑO, NIÑA, MUJER, HOMBRE, ABUELO, ABUELA
    };
    
    // Método para obtener un tipo de voz por nombre
    public static VoiceType getByName(String name) {
        for (VoiceType voice : ALL_VOICES) {
            if (voice.getName().equals(name)) {
                return voice;
            }
        }
        return MUJER; // Valor por defecto
    }
}
