package com.example.speak;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.example.speak.helpers.HelpContentHelper;
import com.example.speak.helpers.HelpAudioHelper;
import com.example.speak.helpers.SequentialImageHelper;
import com.example.speak.VoiceType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpActivity extends AppCompatActivity {
    private static final String TAG = "HelpActivity";
    
    private TextView titleTextView;
    private RecyclerView helpRecyclerView;
    private HelpAdapter helpAdapter;
    private List<HelpSection> helpSections;
    
    private String currentTopic;
    private String currentLevel;
    private String filterSectionTitle;
    private HelpAudioHelper audioHelper;
    private SequentialImageHelper imageHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        
        // Obtener datos del intent
        Intent intent = getIntent();
        currentTopic = intent.getStringExtra("topic");
        currentLevel = intent.getStringExtra("level");
        filterSectionTitle = intent.getStringExtra("section_title");
        
        initViews();
        loadHelpContent();
        
        // Inicializar audio helper
        audioHelper = new HelpAudioHelper(this);
        
        // Inicializar image helper
        imageHelper = new SequentialImageHelper(this);
    }
    
    private void initViews() {
        titleTextView = findViewById(R.id.helpTitleTextView);
        helpRecyclerView = findViewById(R.id.helpRecyclerView);
        
        // Configurar título
        if (currentTopic != null) {
            titleTextView.setText("Ayuda / Help\n" + currentTopic);
        } else {
            titleTextView.setText("Ayuda / Help\nPronunciación del alfabeto / The alphabet pronunciation");
        }
        
        // Configurar RecyclerView
        helpRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        helpSections = new ArrayList<>();
        helpAdapter = new HelpAdapter(helpSections, this);
        helpRecyclerView.setAdapter(helpAdapter);
        
        Log.d(TAG, "Views initialized, RecyclerView adapter set");
    }
    
    private void loadHelpContent() {
        Log.d(TAG, "Loading help content for topic: " + currentTopic + ", level: " + currentLevel);
        
        // Usar el helper para cargar contenido dinámicamente
        HelpContentHelper contentHelper = new HelpContentHelper(this);
        List<HelpSection> newSections = contentHelper.loadHelpContent(currentTopic, currentLevel);
        
        // Filtrar por sección si se solicitó
        if (filterSectionTitle != null && !filterSectionTitle.trim().isEmpty()) {
            List<HelpSection> filtered = new ArrayList<>();
            for (HelpSection s : newSections) {
                if (matchesSection(s, filterSectionTitle)) filtered.add(s);
            }
            if (!filtered.isEmpty()) {
                newSections = filtered;
            } else {
                // Fallback: si no encontramos secciones en el helper, usar las secciones por defecto del alfabeto
                if ("ALPHABET".equalsIgnoreCase(currentTopic)) {
                    List<HelpSection> defaultAlphabet = createDefaultAlphabetSections();
                    List<HelpSection> filteredDefault = new ArrayList<>();
                    for (HelpSection s : defaultAlphabet) if (matchesSection(s, filterSectionTitle)) filteredDefault.add(s);
                    if (!filteredDefault.isEmpty()) {
                        newSections = filteredDefault;
                    }
                }
            }
        }
        
        Log.d(TAG, "Loaded " + newSections.size() + " help sections");
        
        // Si no hay contenido específico, cargar ayuda por defecto
        if (newSections.isEmpty()) {
            Log.w(TAG, "No help sections found, loading default help");
            loadDefaultHelp();
        } else {
            Log.d(TAG, "Help sections loaded successfully");
            for (int i = 0; i < newSections.size(); i++) {
                HelpSection section = newSections.get(i);
                Log.d(TAG, "Section " + i + ": title=" + section.title + ", letters=" + section.letters.length);
            }
            
            // Actualizar la lista del adapter
            helpSections.clear();
            helpSections.addAll(newSections);
        }
        
        helpAdapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter notified of data change, total sections: " + helpSections.size());
    }

    // Permite abrir esta actividad filtrando explícitamente por un subtema/clave
    public static void startFiltered(Context context, String topic, String level) {
        Intent i = new Intent(context, HelpActivity.class);
        i.putExtra("topic", topic);
        i.putExtra("level", level);
        context.startActivity(i);
    }

    public static void startFilteredSection(Context context, String topic, String level, String sectionTitle) {
        Intent i = new Intent(context, HelpActivity.class);
        i.putExtra("topic", topic);
        i.putExtra("level", level);
        i.putExtra("section_title", sectionTitle);
        context.startActivity(i);
    }

    // Crea la lista por defecto de secciones del alfabeto (sin tocar el adapter)
    private List<HelpSection> createDefaultAlphabetSections() {
        List<HelpSection> list = new ArrayList<>();
        HelpSection section1 = new HelpSection();
        section1.title = "Letras con sonido /ei/ /\n Letters with /ei/ sound";
        section1.letters = new String[]{"H [eitʃ]", "J [dʒei]", "A [ei]", "K [kei]"};
        section1.centralSound = "ei";
        section1.audioResource = "alphabet_help_ei";
        list.add(section1);

        HelpSection section2 = new HelpSection();
        section2.title = "Letras con sonido /i/ /\n Letters with /i/ sound";
        section2.letters = new String[]{"B [bi:]", "C [si:]", "D [di:]", "E [i:]", "G [dʒi:]", "P [pi:]", "T [ti:]", "V [vi:]", "Z [zi:]/[zed]"};
        section2.centralSound = "i";
        section2.audioResource = "alphabet_help_i";
        list.add(section2);

        HelpSection section3 = new HelpSection();
        section3.title = "Letras con sonido /e/ /\n Letters with /e/ sound";
        section3.letters = new String[]{"F [ef]", "L [el]", "M [em]", "N [en]", "S [es]", "X [eks]"};
        section3.centralSound = "e";
        section3.audioResource = "alphabet_help_e";
        list.add(section3);

        HelpSection section4 = new HelpSection();
        section4.title = "Letras con sonido /ai/ /\n Letters with /ai/ sound";
        section4.letters = new String[]{"I [ai]", "Y [wai]"};
        section4.centralSound = "ai";
        section4.audioResource = "alphabet_help_ai";
        list.add(section4);

        HelpSection section5 = new HelpSection();
        section5.title = "Letras con sonido /ou/ /\n Letters with /ou/ sound";
        section5.letters = new String[]{"O [ou]"};
        section5.centralSound = "ou";
        section5.audioResource = "alphabet_help_ou";
        list.add(section5);

        HelpSection section6 = new HelpSection();
        section6.title = "Letras con sonido /ju/ /\n Letters with /ju/ sound";
        section6.letters = new String[]{"Q [kju:]", "U [ju:]", "W ['dʌbəl.ju:]"};
        section6.centralSound = "ju";
        section6.audioResource = "alphabet_help_ju";
        list.add(section6);

        HelpSection section7 = new HelpSection();
        section7.title = "Letras con sonido /ar/ /\n Letters with /ar/ sound";
        section7.letters = new String[]{"R [ar]"};
        section7.centralSound = "ar";
        section7.audioResource = "alphabet_help_ar";
        list.add(section7);

        return list;
    }

    private boolean matchesSection(HelpSection s, String key) {
        if (s == null || key == null) return false;
        String k = normalize(key);
        String title = normalize(s.title);
        String sound = normalize(s.centralSound);
        // Coincidencia por igualdad o inclusión
        if (k.equals(sound) || k.equals(title)) return true;
        if (!title.isEmpty() && title.contains(k)) return true;
        // Coincidencias comunes de alfabeto: /ei/, /i/, /e/ en títulos
        if (k.equals("ei") && title.contains("/ei/")) return true;
        if (k.equals("i") && title.contains("/i/")) return true;
        if (k.equals("e") && title.contains("/e/")) return true;
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        s = s.toLowerCase().trim();
        s = s.replace("[", "").replace("]", "");
        s = s.replace("/", "");
        s = s.replace("  ", " ");
        return s;
    }
    
    private void loadDefaultHelp() {
        // Cargar ayuda general por defecto
        loadAlphabetHelp();
    }
    
    private void loadAlphabetHelp() {
        helpSections.clear();
        
        // Sección 1: Letras con sonido /ei/
        HelpSection section1 = new HelpSection();
        section1.title = "Letras con sonido /ei/ / Letters with /ei/ sound";
        section1.letters = new String[]{"H [eitʃ]", "J [dʒei]", "A [ei]", "K [kei]"};
        section1.centralSound = "ei";
        section1.audioResource = "alphabet_help_ei"; // Recurso de audio
        helpSections.add(section1);
        
        // Sección 2: Letras con sonido /i/
        HelpSection section2 = new HelpSection();
        section2.title = "Letras con sonido /i/ / Letters with /i/ sound";
        section2.letters = new String[]{"B [bi:]", "C [si:]", "D [di:]", "E [i:]", "G [dʒi:]", "P [pi:]", "T [ti:]", "V [vi:]", "Z [zi:]/[zed]"};
        section2.centralSound = "i";
        section2.audioResource = "alphabet_help_i";
        helpSections.add(section2);
        
        // Sección 3: Letras con sonido /e/
        HelpSection section3 = new HelpSection();
        section3.title = "Letras con sonido /e/ / Letters with /e/ sound";
        section3.letters = new String[]{"F [ef]", "L [el]", "M [em]", "N [en]", "S [es]", "X [eks]"};
        section3.centralSound = "e";
        section3.audioResource = "alphabet_help_e";
        helpSections.add(section3);
        
        // Sección 4: Letras con sonido /ai/
        HelpSection section4 = new HelpSection();
        section4.title = "Letras con sonido /ai/ / Letters with /ai/ sound";
        section4.letters = new String[]{"I [ai]", "Y [wai]"};
        section4.centralSound = "ai";
        section4.audioResource = "alphabet_help_ai";
        helpSections.add(section4);
        
        // Sección 5: Letras con sonido /ou/
        HelpSection section5 = new HelpSection();
        section5.title = "Letras con sonido /ou/ / Letters with /ou/ sound";
        section5.letters = new String[]{"O [ou]"};
        section5.centralSound = "ou";
        section5.audioResource = "alphabet_help_ou";
        helpSections.add(section5);
        
        // Sección 6: Letras con sonido /ju/
        HelpSection section6 = new HelpSection();
        section6.title = "Letras con sonido /ju/ / Letters with /ju/ sound";
        section6.letters = new String[]{"Q [kju:]", "U [ju:]", "W ['dʌbəl.ju:]"};
        section6.centralSound = "ju";
        section6.audioResource = "alphabet_help_ju";
        helpSections.add(section6);
        
        // Sección 7: Letras con sonido /ar/
        HelpSection section7 = new HelpSection();
        section7.title = "Letras con sonido /ar/ / Letters with /ar/ sound";
        section7.letters = new String[]{"R [ar]"};
        section7.centralSound = "ar";
        section7.audioResource = "alphabet_help_ar";
        helpSections.add(section7);
        
        helpAdapter.notifyDataSetChanged();
    }
    
    private void loadNumbersHelp() {
        helpSections.clear();
        
        // Sección para números del 1-10
        HelpSection section1 = new HelpSection();
        section1.title = "Números del 1 al 10 / Numbers 1 to 10";
        section1.letters = new String[]{"One [wʌn]", "Two [tu:]", "Three [θri:]", "Four [fɔ:]", "Five [faiv]"};
        section1.centralSound = "1-10";
        section1.audioResource = "numbers_help_1_10";
        helpSections.add(section1);
        
        // Sección para números del 11-20
        HelpSection section2 = new HelpSection();
        section2.title = "Números del 11 al 20 / Numbers 11 to 20";
        section2.letters = new String[]{"Eleven [ɪˈlevən]", "Twelve [twelv]", "Thirteen [θɜːˈtiːn]", "Fourteen [fɔːˈtiːn]", "Fifteen [fɪfˈtiːn]"};
        section2.centralSound = "11-20";
        section2.audioResource = "numbers_help_11_20";
        helpSections.add(section2);
        
        helpAdapter.notifyDataSetChanged();
    }
    
    private void loadColorsHelp() {
        helpSections.clear();
        
        // Sección para colores básicos
        HelpSection section1 = new HelpSection();
        section1.title = "Colores básicos / Basic colors";
        section1.letters = new String[]{"Red [red]", "Blue [blu:]", "Green [gri:n]", "Yellow [ˈjeləʊ]", "Black [blæk]"};
        section1.centralSound = "colors";
        section1.audioResource = "colors_help_basic";
        helpSections.add(section1);
        
        // Sección para colores adicionales
        HelpSection section2 = new HelpSection();
        section2.title = "Colores adicionales / Additional colors";
        section2.letters = new String[]{"White [waɪt]", "Purple [ˈpɜːpl]", "Orange [ˈɒrɪndʒ]", "Brown [braʊn]", "Pink [pɪŋk]"};
        section2.centralSound = "colors+";
        section2.audioResource = "colors_help_additional";
        helpSections.add(section2);
        
        helpAdapter.notifyDataSetChanged();
    }
    
    private void loadGeneralHelp() {
        // Cargar ayuda general por defecto
        loadAlphabetHelp();
    }
    
    // Clase para representar una sección de ayuda
    public static class HelpSection {
        public String title;
        public String[] letters;
        public String centralSound;
        public String audioResource;
        public String[] imageResources;        // Recursos de imágenes secuenciales
        public int imageDisplayOrder;          // Orden de visualización
        public String imageDescription;        // Descripción de la imagen
        public boolean hasSequentialImages;    // Si tiene imágenes secuenciales
        public int currentImageIndex;          // Índice de imagen actual
    }
    
    // Adapter para el RecyclerView
    private static class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.HelpViewHolder> {
        private List<HelpSection> helpSections;
        private Context context;
        
        public HelpAdapter(List<HelpSection> helpSections, Context context) {
            this.helpSections = helpSections;
            this.context = context;
        }
        
        @Override
        public HelpViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d("HelpAdapter", "onCreateViewHolder called for viewType: " + viewType);
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_help_section, parent, false);
            return new HelpViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(HelpViewHolder holder, int position) {
            Log.d("HelpAdapter", "onBindViewHolder called for position: " + position);
            HelpSection section = helpSections.get(position);
            holder.bind(section);
        }
        
        @Override
        public int getItemCount() {
            Log.d("HelpAdapter", "getItemCount called, returning: " + helpSections.size());
            return helpSections.size();
        }
        
        class HelpViewHolder extends RecyclerView.ViewHolder {
            private TextView titleTextView;
            private LinearLayout diagramContainer;
            private MaterialCardView audioPlayerCard;
            private ImageButton playButton;
            
            // Controles para imágenes secuenciales
            private ImageView mainImageView;
            private ImageButton previousImageButton;
            private ImageButton nextImageButton;
            private TextView imageCounterText;
            private TextView imageDescriptionText;
            
            // Controles de audio para progreso y tiempo
            private TextView speedIndicator;
            private com.example.speak.AudioPlayerView audioPlayerView;
            private TextView currentTimeText;
            private TextView totalTimeText;
            
            // Botones de tipos de voz (solo 4 opciones)
            private Button helpVoiceChildButton;
            private Button helpVoiceGirlButton;
            private Button helpVoiceWomanButton;
            private Button helpVoiceManButton;
            
            // Control de idioma
            private boolean isSpanishMode = true; // Por defecto español
            private Button helpLanguageSpanishButton;
            private Button helpLanguageEnglishButton;
            
            // Control de panel de configuración
            private ImageButton helpConfigButton;
            private MaterialCardView voiceTypeCard;
            private boolean isConfigPanelVisible = false;
            
            public HelpViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.helpSectionTitle);
                diagramContainer = itemView.findViewById(R.id.diagramContainer);
                audioPlayerCard = itemView.findViewById(R.id.audioPlayerCard);
                playButton = itemView.findViewById(R.id.playButton);
                
                // Inicializar controles de imagen
                mainImageView = itemView.findViewById(R.id.mainImageView);
                previousImageButton = itemView.findViewById(R.id.previousImageButton);
                nextImageButton = itemView.findViewById(R.id.nextImageButton);
                imageCounterText = itemView.findViewById(R.id.imageCounterText);
                imageDescriptionText = itemView.findViewById(R.id.imageDescriptionText);
                
                // Solo el texto de velocidad para el diseño compacto
                // Controles de audio para progreso y tiempo
                speedIndicator = itemView.findViewById(R.id.speedIndicator);
                audioPlayerView = itemView.findViewById(R.id.audioPlayerView);
                currentTimeText = itemView.findViewById(R.id.currentTimeText);
                totalTimeText = itemView.findViewById(R.id.totalTimeText);
                
                // Inicializar botones de tipos de voz (solo 4 opciones)
                helpVoiceChildButton = itemView.findViewById(R.id.helpVoiceChildButton);
                helpVoiceGirlButton = itemView.findViewById(R.id.helpVoiceGirlButton);
                helpVoiceWomanButton = itemView.findViewById(R.id.helpVoiceWomanButton);
                helpVoiceManButton = itemView.findViewById(R.id.helpVoiceManButton);
                
                // Inicializar botones de idioma
                helpLanguageSpanishButton = itemView.findViewById(R.id.helpLanguageSpanishButton);
                helpLanguageEnglishButton = itemView.findViewById(R.id.helpLanguageEnglishButton);
                
                // Inicializar controles de configuración
                helpConfigButton = itemView.findViewById(R.id.helpConfigButton);
                voiceTypeCard = itemView.findViewById(R.id.voiceTypeCard);
            }
            
            public void bind(HelpSection section) {
                Log.d("HelpAdapter", "Binding section: " + section.title + " with " + section.letters.length + " letters");
                
                titleTextView.setText(section.title);
                
                // Crear diagrama visual
                createDiagram(section);
                
                // Configurar audio player
                setupAudioPlayer(section);
                
                // Configurar imágenes secuenciales si existen
                Log.d("HelpAdapter", "About to setup sequential images");
                setupSequentialImages(section);
                
                // Configurar botones de tipos de voz
                setupVoiceTypeButtons();
                
                // Configurar botones de idioma
                setupLanguageButtons();
                
                // Configurar botón de configuración
                setupConfigButton();
            }
            
            private void createDiagram(HelpSection section) {
                Log.d("HelpAdapter", "Creating diagram for section: " + section.title);
                Log.d("HelpAdapter", "Central sound: " + section.centralSound);
                Log.d("HelpAdapter", "Letters count: " + section.letters.length);
                
                diagramContainer.removeAllViews();
                
                // Crear círculo central
                TextView centralCircle = new TextView(context);
                centralCircle.setText(section.centralSound);
                centralCircle.setBackgroundResource(R.drawable.circle_background_c);
                centralCircle.setTextColor(context.getResources().getColor(android.R.color.white));
                centralCircle.setTextSize(18);
                centralCircle.setPadding(20, 20, 20, 20);
                centralCircle.setGravity(android.view.Gravity.CENTER);
                
                LinearLayout.LayoutParams centralParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                centralParams.gravity = android.view.Gravity.CENTER;
                centralParams.setMargins(0, 20, 0, 20);
                centralCircle.setLayoutParams(centralParams);
                
                diagramContainer.addView(centralCircle);
                Log.d("HelpAdapter", "Added central circle with text: " + section.centralSound);
                
                // Crear letras conectadas
                LinearLayout lettersContainer = new LinearLayout(context);
                lettersContainer.setOrientation(LinearLayout.HORIZONTAL);
                lettersContainer.setGravity(android.view.Gravity.CENTER);
                
                for (String letter : section.letters) {
                    Log.d("HelpAdapter", "Creating letter view for: " + letter);
                    TextView letterView = new TextView(context);
                    letterView.setText(letter);
                    letterView.setTextColor(context.getResources().getColor(android.R.color.white));
                    letterView.setTextSize(14);
                    letterView.setPadding(10, 5, 10, 5);
                    letterView.setBackgroundResource(R.drawable.rounded_background);
                    
                    LinearLayout.LayoutParams letterParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    letterParams.setMargins(8, 0, 8, 0);
                    letterView.setLayoutParams(letterParams);
                    
                    lettersContainer.addView(letterView);
                    Log.d("HelpAdapter", "Added letter view: " + letter);
                }
                
                diagramContainer.addView(lettersContainer);
                Log.d("HelpAdapter", "Added letters container with " + section.letters.length + " letters");
            }
            
            private void setupAudioPlayer(HelpSection section) {
                // Configurar botón de reproducción/pausa
                playButton.setOnClickListener(v -> {
                    if (context instanceof HelpActivity) {
                        HelpActivity activity = (HelpActivity) context;
                        if (activity.audioHelper != null) {
                            if (activity.audioHelper.isPlaying()) {
                                // Pausar
                                activity.audioHelper.pauseAudio();
                                playButton.setImageResource(R.drawable.reproduce);
                                
                                // Detener ondas y progreso
                                if (audioPlayerView != null) {
                                    audioPlayerView.setPlaying(false);
                                }
                                
                                // Detener actualización del progreso
                                stopTimeUpdate();
                            } else if (activity.audioHelper.isPaused()) {
                                // Reanudar desde donde se pausó
                                activity.audioHelper.resumeAudio();
                                playButton.setImageResource(R.drawable.pause);
                                
                                // Reanudar ondas y progreso
                                if (audioPlayerView != null) {
                                    audioPlayerView.setPlaying(true);
                                }
                                
                                // Reanudar actualización del progreso
                                startTimeUpdate(section);
                            } else {
                                // Reproducir audio desde el principio
                                if (isSpanishMode) {
                                    // Usar archivo MP3 original en español
                                    activity.audioHelper.playAudio(section.audioResource);
                                    playButton.setImageResource(R.drawable.pause);
                                    
                                    // Configurar duración del archivo MP3
                                    int duration = activity.audioHelper.getTotalDuration();
                                    if (duration > 0 && audioPlayerView != null) {
                                        Log.d("HelpActivity", "Setting MP3 duration: " + duration + "ms");
                                        audioPlayerView.setDuration(duration);
                                        audioPlayerView.setPlaying(true);
                                    }
                                    
                                    // Iniciar actualización del progreso
                                    startTimeUpdate(section);
                                } else {
                                    // Usar TextToSpeech para inglés con tipos de voz
                                    String textToSpeak = generateEnglishTextFromSection(section);
                                    activity.audioHelper.speakText(textToSpeak);
                                    playButton.setImageResource(R.drawable.pause);
                                    
                                    // Configurar duración estimada para TextToSpeech
                                    int estimatedDuration = estimateTextDuration(textToSpeak);
                                    if (audioPlayerView != null) {
                                        Log.d("HelpActivity", "Setting TTS duration: " + estimatedDuration + "ms");
                                        audioPlayerView.setDuration(estimatedDuration);
                                        audioPlayerView.setPlaying(true);
                                    }
                                    
                                    // Iniciar actualización del progreso
                                    startTimeUpdate(section);
                                }
                            }
                        }
                    }
                });
                
                // Configurar control de velocidad funcional
                if (speedIndicator != null) {
                    speedIndicator.setOnClickListener(v -> {
                        // Ciclar entre 1x, 1.5x, 2x
                        String currentSpeed = speedIndicator.getText().toString();
                        String newSpeed;
                        float speedValue;
                        
                        switch (currentSpeed) {
                            case "x1":
                                newSpeed = "x1.5";
                                speedValue = 1.5f;
                                break;
                            case "x1.5":
                                newSpeed = "x2";
                                speedValue = 2.0f;
                                break;
                            case "x2":
                                newSpeed = "x1";
                                speedValue = 1.0f;
                                break;
                            default:
                                newSpeed = "x1";
                                speedValue = 1.0f;
                        }
                        
                        speedIndicator.setText(newSpeed);
                        
                        // Aplicar la velocidad al audio
                        if (context instanceof HelpActivity) {
                            HelpActivity activity = (HelpActivity) context;
                            if (activity.audioHelper != null) {
                                activity.audioHelper.setPlaybackSpeed(speedValue);
                            }
                        }
                    });
                    
                    // Establecer velocidad inicial
                    speedIndicator.setText("x1");
                }
                
                // Configurar AudioPlayerView integrado
                if (audioPlayerView != null) {
                    Log.d("HelpActivity", "Setting up AudioPlayerView listener");
                    
                    // Configurar listener del reproductor
                    audioPlayerView.setOnProgressChangeListener(new com.example.speak.AudioPlayerView.OnProgressChangeListener() {
                        @Override
                        public void onProgressChanged(float progress) {
                            // Actualizar tiempo cuando cambie el progreso
                            // progress viene en milisegundos (como en ListeningActivity)
                            Log.d("HelpActivity", "Progress changed: " + progress + "ms");
                            if (context instanceof HelpActivity) {
                                HelpActivity activity = (HelpActivity) context;
                                if (activity.audioHelper != null) {
                                    int duration = activity.audioHelper.getTotalDuration();
                                    if (duration > 0) {
                                        updateTimeDisplay((int)progress, duration);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onPlayPause(boolean isPlaying) {
                            Log.d("HelpActivity", "Play/Pause: " + isPlaying);
                            // No necesitamos hacer nada aquí
                        }

                        @Override
                        public void onSeek(float position) {
                            // Aplicar seek cuando el usuario toque la barra
                            // position viene en milisegundos (como en ListeningActivity)
                            Log.d("HelpActivity", "Seek to: " + position + "ms");
                            if (context instanceof HelpActivity) {
                                HelpActivity activity = (HelpActivity) context;
                                if (activity.audioHelper != null) {
                                    activity.audioHelper.seekTo((int)position);
                                }
                            }
                        }
                    });
                }
                
                // Inicializar valores por defecto
                if (currentTimeText != null) currentTimeText.setText("0:00");
                if (totalTimeText != null) totalTimeText.setText("0:00");
                
                // Configurar actualización de tiempo en tiempo real
                startTimeUpdate(section);
            }
            

            /**
             * Actualiza la visualización del tiempo
             */
            private void updateTimeDisplay(int currentPosition, int totalDuration) {
                if (currentTimeText != null && totalTimeText != null) {
                    String currentTime = formatTime(currentPosition);
                    String totalTime = formatTime(totalDuration);
                    currentTimeText.setText(currentTime);
                    totalTimeText.setText(totalTime);
                }
            }
            
            /**
             * Formatea el tiempo en formato MM:SS
             */
            private String formatTime(int milliseconds) {
                int seconds = (milliseconds / 1000) % 60;
                int minutes = (milliseconds / (1000 * 60)) % 60;
                return String.format("%d:%02d", minutes, seconds);
            }
            
            /**
             * Inicia la actualización del tiempo en tiempo real
             */
            private android.os.Handler progressHandler;
            private Runnable progressRunnable;
            
            private void startTimeUpdate(HelpSection section) {
                Log.d("HelpActivity", "startTimeUpdate called for section: " + section.title);
                if (context instanceof HelpActivity) {
                    HelpActivity activity = (HelpActivity) context;
                    if (activity.audioHelper != null && audioPlayerView != null) {
                        // Detener actualización anterior si existe
                        stopTimeUpdate();
                        
                        // Configurar duración del AudioPlayerView
                        int totalDuration = activity.audioHelper.getTotalDuration();
                        Log.d("HelpActivity", "Setting duration: " + totalDuration + "ms");
                        if (totalDuration > 0) {
                            audioPlayerView.setDuration(totalDuration);
                        }
                        
                        // Crear un Handler dedicado para esta sección (como en ListeningActivity)
                        progressHandler = new android.os.Handler();
                        progressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (activity.audioHelper.isPlaying()) {
                                    int currentPosition = activity.audioHelper.getCurrentPosition();
                                    int totalDuration = activity.audioHelper.getTotalDuration();
                                    
                                    Log.d("HelpActivity", "Update - Position: " + currentPosition + "ms, Duration: " + totalDuration + "ms, Playing: true");
                                    
                                    if (totalDuration > 0) {
                                        // Actualizar AudioPlayerView con posición en milisegundos (como en ListeningActivity)
                                        audioPlayerView.setProgress(currentPosition);
                                        audioPlayerView.setPlaying(true);
                                        
                                        // Actualizar tiempo
                                        updateTimeDisplay(currentPosition, totalDuration);
                                        
                                        // Continuar actualizando si aún está reproduciendo
                                        if (currentPosition < totalDuration) {
                                            progressHandler.postDelayed(this, 100);
                                        } else {
                                            // Audio terminó
                                            audioPlayerView.setPlaying(false);
                                            Log.d("HelpActivity", "Audio playback completed");
                                        }
                                    } else {
                                        audioPlayerView.setPlaying(false);
                                    }
                                } else {
                                    Log.d("HelpActivity", "Update - Not playing, setting playing: false");
                                    audioPlayerView.setPlaying(false);
                                }
                            }
                        };
                        
                        // Iniciar la actualización del progreso
                        progressHandler.post(progressRunnable);
                    }
                }
            }
            
            private void stopTimeUpdate() {
                if (progressHandler != null && progressRunnable != null) {
                    progressHandler.removeCallbacks(progressRunnable);
                    progressHandler = null;
                    progressRunnable = null;
                }
            }
            

            
            private void setupSequentialImages(HelpSection section) {
                Log.d("HelpAdapter", "setupSequentialImages called for section: " + section.title);
                Log.d("HelpAdapter", "hasSequentialImages: " + section.hasSequentialImages);
                Log.d("HelpAdapter", "imageResources: " + (section.imageResources != null ? section.imageResources.length : "null"));
                
                if (section.hasSequentialImages && section.imageResources != null && section.imageResources.length > 0) {
                    Log.d("HelpAdapter", "Showing image controls for " + section.imageResources.length + " images");
                    // Mostrar controles de imagen
                    mainImageView.setVisibility(View.VISIBLE);
                    previousImageButton.setVisibility(View.VISIBLE);
                    nextImageButton.setVisibility(View.VISIBLE);
                    imageCounterText.setVisibility(View.VISIBLE);
                    imageDescriptionText.setVisibility(View.VISIBLE);
                    
                    // Configurar imagen inicial
                    section.currentImageIndex = 0;
                    Log.d("HelpAdapter", "Setting initial image index to 0");
                    updateImageDisplay(section);
                    
                    // Configurar botones de navegación
                    previousImageButton.setOnClickListener(v -> {
                        Log.d("HelpAdapter", "Previous button clicked, current index: " + section.currentImageIndex);
                        if (section.currentImageIndex > 0) {
                            section.currentImageIndex--;
                            updateImageDisplay(section);
                        }
                    });
                    
                    nextImageButton.setOnClickListener(v -> {
                        Log.d("HelpAdapter", "Next button clicked, current index: " + section.currentImageIndex);
                        if (section.currentImageIndex < section.imageResources.length - 1) {
                            section.currentImageIndex++;
                            updateImageDisplay(section);
                        }
                    });
                    
                    // Configurar estado inicial de botones
                    updateNavigationButtons(section);
                    Log.d("HelpAdapter", "Image controls setup completed");
                    
                } else {
                    // Ocultar controles de imagen si no hay imágenes
                    mainImageView.setVisibility(View.GONE);
                    previousImageButton.setVisibility(View.GONE);
                    nextImageButton.setVisibility(View.GONE);
                    imageCounterText.setVisibility(View.GONE);
                    imageDescriptionText.setVisibility(View.GONE);
                }
            }
            
            private void updateImageDisplay(HelpSection section) {
                Log.d("HelpAdapter", "updateImageDisplay called, currentIndex: " + section.currentImageIndex);
                if (section.imageResources != null && section.currentImageIndex < section.imageResources.length) {
                    String imageResource = section.imageResources[section.currentImageIndex];
                    Log.d("HelpAdapter", "Loading image: " + imageResource);
                    
                    // Usar el SequentialImageHelper para cargar la imagen
                    if (context instanceof HelpActivity) {
                        HelpActivity activity = (HelpActivity) context;
                        if (activity.imageHelper != null) {
                            Log.d("HelpAdapter", "Using imageHelper to load image");
                            activity.imageHelper.loadImage(imageResource, mainImageView);
                        } else {
                            Log.w("HelpAdapter", "imageHelper is null!");
                        }
                    }
                    
                    // Actualizar contador
                    imageCounterText.setText(String.format("%d/%d", 
                        section.currentImageIndex + 1, section.imageResources.length));
                    
                    // Actualizar descripción usando el helper
                    if (context instanceof HelpActivity) {
                        HelpActivity activity = (HelpActivity) context;
                        if (activity.imageHelper != null) {
                            String description = activity.imageHelper.getImageDescription(imageResource);
                            imageDescriptionText.setText(description);
                        }
                    }
                    
                    // Actualizar estado de botones
                    updateNavigationButtons(section);
                }
            }
            
            private void updateNavigationButtons(HelpSection section) {
                // Botón anterior
                previousImageButton.setEnabled(section.currentImageIndex > 0);
                previousImageButton.setAlpha(section.currentImageIndex > 0 ? 1.0f : 0.5f);
                
                // Botón siguiente
                nextImageButton.setEnabled(section.currentImageIndex < section.imageResources.length - 1);
                nextImageButton.setAlpha(section.currentImageIndex < section.imageResources.length - 1 ? 1.0f : 0.5f);
            }
            
            private void loadImageFromAssets(String imageName) {
                // Implementar carga desde assets si es necesario
                // Por ahora usamos imagen por defecto
                mainImageView.setImageResource(R.drawable.help_icon);
            }
            
            private void setupVoiceTypeButtons() {
                if (context instanceof HelpActivity) {
                    HelpActivity activity = (HelpActivity) context;
                    if (activity.audioHelper != null) {
                        // Configurar listeners para los botones de tipos de voz
                        helpVoiceChildButton.setOnClickListener(v -> {
                            activity.audioHelper.setVoiceType(VoiceType.NIÑO);
                            updateVoiceTypeButtons();
                        });
                        
                        helpVoiceGirlButton.setOnClickListener(v -> {
                            activity.audioHelper.setVoiceType(VoiceType.NIÑA);
                            updateVoiceTypeButtons();
                        });
                        
                        helpVoiceWomanButton.setOnClickListener(v -> {
                            activity.audioHelper.setVoiceType(VoiceType.MUJER);
                            updateVoiceTypeButtons();
                        });
                        
                        helpVoiceManButton.setOnClickListener(v -> {
                            activity.audioHelper.setVoiceType(VoiceType.HOMBRE);
                            updateVoiceTypeButtons();
                        });
                        
                        // Marcar el botón inicial como seleccionado
                        updateVoiceTypeButtons();
                    }
                }
            }
            
            private void updateVoiceTypeButtons() {
                if (context instanceof HelpActivity) {
                    HelpActivity activity = (HelpActivity) context;
                    if (activity.audioHelper != null) {
                        VoiceType currentVoice = activity.audioHelper.getCurrentVoiceType();
                        
                        // Resetear todos los botones
                        resetVoiceTypeButtons();
                        
                        // Marcar el botón seleccionado (emoji con efecto de escala y color)
                        switch (currentVoice.getName()) {
                            case "niño":
                                helpVoiceChildButton.setScaleX(1.2f);
                                helpVoiceChildButton.setScaleY(1.2f);
                                helpVoiceChildButton.setTextColor(ContextCompat.getColor(context, R.color.selected_voice));
                                break;
                            case "niña":
                                helpVoiceGirlButton.setScaleX(1.2f);
                                helpVoiceGirlButton.setScaleY(1.2f);
                                helpVoiceGirlButton.setTextColor(ContextCompat.getColor(context, R.color.selected_voice));
                                break;
                            case "mujer":
                                helpVoiceWomanButton.setScaleX(1.2f);
                                helpVoiceWomanButton.setScaleY(1.2f);
                                helpVoiceWomanButton.setTextColor(ContextCompat.getColor(context, R.color.selected_voice));
                                break;
                            case "hombre":
                                helpVoiceManButton.setScaleX(1.2f);
                                helpVoiceManButton.setScaleY(1.2f);
                                helpVoiceManButton.setTextColor(ContextCompat.getColor(context, R.color.selected_voice));
                                break;
                        }
                    }
                }
            }
            
            private void resetVoiceTypeButtons() {
                // Resetear escala y color de todos los botones
                helpVoiceChildButton.setScaleX(1.0f);
                helpVoiceChildButton.setScaleY(1.0f);
                helpVoiceChildButton.setTextColor(ContextCompat.getColor(context, R.color.help_audio_player_background));
                
                helpVoiceGirlButton.setScaleX(1.0f);
                helpVoiceGirlButton.setScaleY(1.0f);
                helpVoiceGirlButton.setTextColor(ContextCompat.getColor(context, R.color.help_audio_player_background));
                
                helpVoiceWomanButton.setScaleX(1.0f);
                helpVoiceWomanButton.setScaleY(1.0f);
                helpVoiceWomanButton.setTextColor(ContextCompat.getColor(context, R.color.help_audio_player_background));
                
                helpVoiceManButton.setScaleX(1.0f);
                helpVoiceManButton.setScaleY(1.0f);
                helpVoiceManButton.setTextColor(ContextCompat.getColor(context, R.color.help_audio_player_background));
            }
            
            private void setupLanguageButtons() {
                // Configurar listeners para los botones de idioma
                helpLanguageSpanishButton.setOnClickListener(v -> {
                    isSpanishMode = true;
                    updateLanguageButtons();
                });
                
                helpLanguageEnglishButton.setOnClickListener(v -> {
                    isSpanishMode = false;
                    updateLanguageButtons();
                });
                
                // Marcar el botón inicial como seleccionado
                updateLanguageButtons();
            }
            
            private void updateLanguageButtons() {
                // Resetear todos los botones de idioma
                helpLanguageSpanishButton.setBackgroundTintList(null);
                helpLanguageEnglishButton.setBackgroundTintList(null);
                
                // Marcar el botón seleccionado
                if (isSpanishMode) {
                    helpLanguageSpanishButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.selected_voice)));
                } else {
                    helpLanguageEnglishButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.selected_voice)));
                }
            }
            
            private void setupConfigButton() {
                helpConfigButton.setOnClickListener(v -> {
                    toggleConfigPanel();
                });
            }
            
            private void toggleConfigPanel() {
                if (isConfigPanelVisible) {
                    // Ocultar panel
                    voiceTypeCard.setVisibility(View.GONE);
                    isConfigPanelVisible = false;
                    helpConfigButton.setImageResource(R.drawable.settings);
                } else {
                    // Mostrar panel
                    voiceTypeCard.setVisibility(View.VISIBLE);
                    isConfigPanelVisible = true;
                    helpConfigButton.setImageResource(R.drawable.settings);
                }
            }
            
            private String generateSpanishTextFromSection(HelpSection section) {
                StringBuilder text = new StringBuilder();
                text.append("Letras con sonido ").append(section.centralSound).append(": ");
                
                for (int i = 0; i < section.letters.length; i++) {
                    text.append(section.letters[i]);
                    if (i < section.letters.length - 1) {
                        text.append(", ");
                    }
                }
                
                text.append(". Repite después de mí: ");
                for (String letter : section.letters) {
                    text.append(letter).append(" ");
                }
                
                return text.toString();
            }
            
            private String generateEnglishTextFromSection(HelpSection section) {
                StringBuilder text = new StringBuilder();
                text.append("Letters with sound ").append(section.centralSound).append(": ");
                
                for (int i = 0; i < section.letters.length; i++) {
                    text.append(section.letters[i]);
                    if (i < section.letters.length - 1) {
                        text.append(", ");
                    }
                }
                
                text.append(". Repeat after me: ");
                for (String letter : section.letters) {
                    text.append(letter).append(" ");
                }
                
                return text.toString();
            }
            
            private int estimateTextDuration(String text) {
                // Estimación aproximada: 150 palabras por minuto = 2.5 palabras por segundo
                // Cada palabra promedio tiene 5 caracteres, así que dividimos por 5
                int wordCount = text.split("\\s+").length;
                int estimatedSeconds = Math.max(3, wordCount / 2); // Mínimo 3 segundos
                return estimatedSeconds * 1000; // Convertir a milisegundos
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioHelper != null) {
            audioHelper.release();
        }
    }
}
