# Componente ReusableAudioPlayerCard - Guía de Uso

## Descripción
El `ReusableAudioPlayerCard` es un componente reutilizable basado en el control de audio de la vista de ayuda. Permite reproducir archivos de audio desde diferentes carpetas de assets y seleccionar diferentes tipos de voces (niño, niña, hombre, mujer).

## Características
- ✅ Reproducción de archivos MP3 desde assets
- ✅ Soporte para TextToSpeech con diferentes tipos de voz
- ✅ Control de velocidad de reproducción (1x, 1.5x, 2x)
- ✅ Selector de idioma (Español/English)
- ✅ Selector de tipos de voz (👦 Niño, 👧 Niña, 👨 Hombre, 👩 Mujer)
- ✅ Interfaz compacta y moderna
- ✅ Completamente reutilizable

## Estructura de Archivos
```
app/src/main/
├── res/layout/
│   └── audio_player_card.xml          # Layout del componente
├── java/com/example/speak/
│   ├── components/
│   │   └── ReusableAudioPlayerCard.java  # Clase del componente
│   └── helpers/
│       └── ReusableAudioHelper.java      # Helper para manejo de audio
```

## Cómo Usar

### 1. Agregar al Layout
```xml
<com.example.speak.components.ReusableAudioPlayerCard
    android:id="@+id/reusableAudioPlayerCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="16dp"
    android:layout_marginTop="8dp" />
```

### 2. Configurar en Java
```java
public class MiActivity extends AppCompatActivity {
    private ReusableAudioPlayerCard reusableAudioPlayerCard;
    private String textoParaReproducir = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_actividad);
        
        // Inicializar el componente
        reusableAudioPlayerCard = findViewById(R.id.reusableAudioPlayerCard);
        
        // Configurar para una carpeta específica
        reusableAudioPlayerCard.configure("audio_video/alphabet", textoParaReproducir);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos
        if (reusableAudioPlayerCard != null) {
            reusableAudioPlayerCard.cleanup();
        }
    }
}
```

## Configuración por Carpeta

### Alphabet
```java
reusableAudioPlayerCard.configure("audio_video/alphabet", "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z");
```

### Numbers
```java
reusableAudioPlayerCard.configure("audio_video/number", "1 2 3 4 5 6 7 8 9 10");
```

### Colors
```java
reusableAudioPlayerCard.configure("audio_video/color", "Red Blue Green Yellow Orange Purple");
```

## Estructura de Archivos de Audio Esperada

Para que funcione correctamente, los archivos de audio deben estar organizados así:

```
app/src/main/assets/
└── audio_video/
    ├── alphabet/
    │   ├── child.mp3          # Voz de niño
    │   ├── little_girl.mp3    # Voz de niña
    │   ├── man.mp3           # Voz de hombre
    │   └── woman.mp3         # Voz de mujer
    ├── number/
    │   ├── child.mp3
    │   ├── little_girl.mp3
    │   ├── man.mp3
    │   └── woman.mp3
    └── color/
        ├── child.mp3
        ├── little_girl.mp3
        ├── man.mp3
        └── woman.mp3
```

## Tipos de Voz Disponibles

| Emoji | Nombre | Archivo |
|-------|--------|---------|
| 👦 | Niño | `child.mp3` |
| 👧 | Niña | `little_girl.mp3` |
| 👨 | Hombre | `man.mp3` |
| 👩 | Mujer | `woman.mp3` |

## Funcionalidades

### Modo Español (Original)
- Reproduce archivos MP3 originales desde assets
- No requiere TextToSpeech
- Mejor calidad de audio

### Modo English (TTS)
- Usa TextToSpeech con diferentes tipos de voz
- Configuración automática de pitch y velocidad
- Soporte para diferentes velocidades de reproducción

### Controles
- ▶️ **Play/Pause**: Reproducir o pausar audio
- ⚙️ **Configuración**: Mostrar/ocultar panel de configuración
- 🎚️ **Velocidad**: Cambiar velocidad (x1, x1.5, x2)
- 🌍 **Idioma**: Cambiar entre Español e English
- 👤 **Voz**: Seleccionar tipo de voz

## Métodos Públicos

```java
// Configuración
void configure(String assetsFolder, String text)
void setText(String text)
void setAssetsFolder(String folder)

// Estado
boolean isPlaying()
boolean isPaused()
String getCurrentVoiceType()
boolean isSpanishMode()
float getCurrentSpeed()

// Limpieza
void cleanup()
```

## Ejemplo Completo - Activity Numbers

```java
public class NumberActivity extends AppCompatActivity {
    private ReusableAudioPlayerCard reusableAudioPlayerCard;
    private String numbersText = "1 2 3 4 5 6 7 8 9 10";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);
        
        reusableAudioPlayerCard = findViewById(R.id.reusableAudioPlayerCard);
        
        // Configurar para números
        reusableAudioPlayerCard.configure("audio_video/number", numbersText);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reusableAudioPlayerCard != null) {
            reusableAudioPlayerCard.cleanup();
        }
    }
}
```

## Ventajas del Componente

1. **Reutilizable**: Un solo componente para todas las actividades
2. **Consistente**: Misma interfaz en toda la aplicación
3. **Configurable**: Fácil cambio de carpeta y texto
4. **Mantenible**: Cambios centralizados en un solo lugar
5. **Moderno**: Diseño basado en Material Design
6. **Flexible**: Soporte para MP3 y TTS

## Migración desde Controles Existentes

Para migrar una actividad existente:

1. Reemplazar el layout del control de audio con el componente
2. Eliminar código relacionado con audio en la Activity
3. Agregar configuración del componente
4. Agregar limpieza en `onDestroy()`

¡El componente está listo para usar en cualquier vista que necesite control de audio!
