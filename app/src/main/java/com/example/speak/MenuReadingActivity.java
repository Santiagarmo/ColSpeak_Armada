package com.example.speak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView; // Added import for TextView

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public class MenuReadingActivity extends AppCompatActivity {

    private static final String TAG = "MenuReadingActivity";

    //Declaramos las variables Start Reading
    private boolean startReadingExpanded = false;
    private ImageView eButtonStartReading;
    private ImageView imgReadingTranslation;
    private ImageView imgReadingComprehension;
    private ImageView imgReadingVocabulary;
    private ImageView imgReadingGrammar;
    private ImageView imgReadingStories;

    //Declaramos las variables icMap2 Reading
    private boolean icMap2ReadingExpanded = false;
    private ImageView imgIcMap2Reading;
    private ImageView imgReadingAdvanced;
    private ImageView imgReadingPractice;

    // Image blocked
    private ImageView map_blocked_2Reading;
    private ImageView map_blocked_3Reading;
    private ImageView map_blocked_4Reading;
    private ImageView map_blocked_5Reading;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView readingHistoryMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    //Return Menú
    private LinearLayout eBtnReturnMenu;

    // Modo de navegación libre (sin dependencias)
    private boolean freeRoamMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_reading);

        try {
            //Inicializamos las variables Start Reading
            eButtonStartReading = findViewById(R.id.eButtonStartReading);
            imgReadingTranslation = findViewById(R.id.imgReadingTranslation);
            imgReadingComprehension = findViewById(R.id.imgReadingComprehension);
            imgReadingVocabulary = findViewById(R.id.imgReadingVocabulary);
            imgReadingGrammar = findViewById(R.id.imgReadingGrammar);
            imgReadingStories = findViewById(R.id.imgReadingStories);

            //Inicializamos las variables imgIcMap2 Reading
            imgIcMap2Reading = findViewById(R.id.imgIcMap2Reading);
            imgReadingAdvanced = findViewById(R.id.imgReadingAdvanced);
            imgReadingPractice = findViewById(R.id.imgReadingPractice);

            //Blocked
            map_blocked_2Reading = findViewById(R.id.map_blocked_2Reading);
            map_blocked_3Reading = findViewById(R.id.map_blocked_3Reading);
            map_blocked_4Reading = findViewById(R.id.map_blocked_4Reading);
            map_blocked_5Reading = findViewById(R.id.map_blocked_5Reading);

            //Declaramos las variables Menu
            birdMenu = findViewById(R.id.imgBirdMenu);
            quizMenu = findViewById(R.id.imgQuizMenu);
            readingHistoryMenu = findViewById(R.id.imgReadingHistoryMenu);
            eButtonProfile = findViewById(R.id.btnProfile);
            homeButton = findViewById(R.id.homeButton);

            // Verificar progreso de reading y aplicar efectos visuales
            checkReadingProgressAndUnlockTopics();
            applyReadingVisualLockEffects();
            
            setupReadingClickListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing reading menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        // Leer modo libre desde Intent o SharedPreferences
        freeRoamMode = getIntent().getBooleanExtra("FREE_ROAM", false);
        if (!freeRoamMode) {
            SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
            freeRoamMode = prefs.getBoolean("FREE_ROAM", false);
        }
        if (freeRoamMode) {
            View container = findViewById(R.id.freeRoamContainer);
            if (container != null) container.setVisibility(View.VISIBLE);
            View btn = findViewById(R.id.btnDisableFreeRoam);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("FREE_ROAM", false).apply();
                    if (container != null) container.setVisibility(View.GONE);
                    Toast.makeText(this, "Modo libre desactivado", Toast.LENGTH_SHORT).show();
                    // Refrescar candados/efectos al desactivar
                    refreshReadingVisualEffects();
                });
            }
        }

        // Desbloqueos secuenciales del mapa de Reading basados en progreso real
        // ALPHABET aprobado desbloquea NUMBERS, etc.
        applyBlock(map_blocked_2Reading, "PASSED_READING_ALPHABET");
        applyBlock(map_blocked_3Reading, "PASSED_READING_NUMBERS");
        applyBlock(map_blocked_4Reading, "PASSED_READING_COLORS");
        applyBlock(map_blocked_5Reading, "PASSED_READING_PERSONAL_PRONOUNS");

        //Return Menu
        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });

        // MÉTODO TEMPORAL PARA TESTING - Agregar long click al botón de retorno para marcar temas como completados
        // TODO: Remover esto cuando se implementen todas las actividades de reading
        eBtnReturnMenu.setOnLongClickListener(v -> {
            markAllReadingTopicsForTesting();
            return true;
        });
    }

    // Verificar progreso de reading y desbloquear temas automáticamente
    private void checkReadingProgressAndUnlockTopics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Verificar si se completó el reading de nivel A1.1 (70% o más)
        boolean passedReadingA1_1 = prefs.getBoolean("PASSED_READING_LEVEL_A1_1", false);
        int scoreReadingA1_1 = prefs.getInt("SCORE_READING_LEVEL_A1_1", 0);
        
        if (passedReadingA1_1 || scoreReadingA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del reading A1.1
            unlockRemainingReadingA1_1Topics();
            Log.d(TAG, "Temas restantes de Reading A1.1 desbloqueados automáticamente");
        }
    }

    // Desbloquear solo los temas restantes del reading nivel A1.1
    private void unlockRemainingReadingA1_1Topics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Solo desbloquear si no están ya desbloqueados - Usamos claves específicas para reading
        if (!prefs.getBoolean("PASSED_READING_ALPHABET", false)) {
            editor.putBoolean("PASSED_READING_ALPHABET", true);
            Log.d(TAG, "Reading ALPHABET desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_NUMBERS", false)) {
            editor.putBoolean("PASSED_READING_NUMBERS", true);
            Log.d(TAG, "Reading NUMBERS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_COLORS", false)) {
            editor.putBoolean("PASSED_READING_COLORS", true);
            Log.d(TAG, "Reading COLORS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_READING_PERSONAL_PRONOUNS", true);
            Log.d(TAG, "Reading PERSONAL PRONOUNS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", true);
            Log.d(TAG, "Reading POSSESSIVE ADJECTIVES desbloqueado automáticamente");
        }
        /*if (!prefs.getBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", false)) {
            editor.putBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", true);
            Log.d(TAG, "Reading PREPOSITIONS OF PLACE desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_READING_ADJECTIVES", true);
            Log.d(TAG, "Reading ADJECTIVES desbloqueado automáticamente");
        }*/
        
        editor.apply();
    }

    private void setupReadingClickListeners() {
        // READING - ALPHABET (antes Translation) - Requiere completar Speaking Alphabet
        if (imgReadingTranslation != null) {
            imgReadingTranslation.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, ImageIdentificationActivity.class);
                        intent.putExtra("TOPIC", "ALPHABET");
                        intent.putExtra("LEVEL", "A1.1");
                        intent.putExtra("SOURCE_MAP", "READING");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading alphabet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            });
        }

        // READING - NUMBERS (antes Comprehension) - Requiere completar Speaking Numbers
        if (imgReadingComprehension != null) {
            imgReadingComprehension.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_ALPHABET", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, ImageIdentificationAudioActivity.class);
                        intent.putExtra("TOPIC", "NUMBERS");
                        intent.putExtra("LEVEL", "A1.1");
                        intent.putExtra("SOURCE_MAP", "READING");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading numbers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar NUMBERS speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // READING - COLORS (antes Vocabulary) - Requiere completar Speaking Colors
        if (imgReadingVocabulary != null) {
            imgReadingVocabulary.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_NUMBERS", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, ImageIdentificationActivity.class);
                        intent.putExtra("TOPIC", "COLORS");
                        intent.putExtra("LEVEL", "A1.1");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading colors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar COLORS speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // READING - PERSONAL PRONOUNS (antes Grammar) - Requiere completar Speaking Personal Pronouns
        if (imgReadingGrammar != null) {
            imgReadingGrammar.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_COLORS", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, TranslationReadingActivity.class);
                        intent.putExtra("TOPIC", "PERSONAL PRONOUNS");
                        intent.putExtra("LEVEL", "A1.1");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading personal pronouns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar PERSONAL PRONOUNS speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // READING - POSSESSIVE ADJECTIVES (antes Stories) - Requiere completar Speaking Possessive Adjectives
        if (imgReadingStories != null) {
            imgReadingStories.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_PERSONAL_PRONOUNS", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, TranslationReadingActivity.class);
                        intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES");
                        intent.putExtra("LEVEL", "A1.1");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading possessive adjectives: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar POSSESSIVE ADJECTIVES speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // READING - PREPOSITIONS OF PLACE (antes Advanced) - Requiere completar Speaking Prepositions
        if (imgReadingAdvanced != null) {
            imgReadingAdvanced.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, TranslationReadingActivity.class);
                        intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE");
                        intent.putExtra("LEVEL", "A1.1");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading prepositions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar PREPOSITIONS speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // READING - ADJECTIVES (antes Practice) - Requiere completar Speaking Adjectives
        if (imgReadingPractice != null) {
            imgReadingPractice.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", false);
                if (unlocked) {
                    try {
                        Intent intent = new Intent(MenuReadingActivity.this, TranslationReadingActivity.class);
                        intent.putExtra("TOPIC", "ADJECTIVES");
                        intent.putExtra("LEVEL", "A1.1");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MenuReadingActivity.this, "Error opening reading adjectives: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "📖 Para practicar ADJECTIVES speaking primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // Botón Start Reading - Despliega los primeros elementos
        if (eButtonStartReading != null) {
            eButtonStartReading.setOnClickListener(v -> {
                startReadingExpanded = !startReadingExpanded;
                if (startReadingExpanded) {
                    eButtonStartReading.setImageResource(R.drawable.start);
                    fadeInView(imgReadingTranslation);
                    fadeInView(imgReadingComprehension);
                    fadeInView(imgReadingVocabulary);
                    fadeInView(imgReadingGrammar);
                    fadeInView(imgReadingStories);
                } else {
                    eButtonStartReading.setImageResource(R.drawable.start);
                    fadeOutView(imgReadingTranslation);
                    fadeOutView(imgReadingComprehension);
                    fadeOutView(imgReadingVocabulary);
                    fadeOutView(imgReadingGrammar);
                    fadeOutView(imgReadingStories);
                }
            });
        }

        // Map Icon 2 Reading - Despliega elementos avanzados
        if (imgIcMap2Reading != null) {
            imgIcMap2Reading.setOnClickListener(v -> {
                icMap2ReadingExpanded = !icMap2ReadingExpanded;
                if (icMap2ReadingExpanded) {
                    imgIcMap2Reading.setImageResource(R.drawable.icon_map3);
                    fadeInView(imgReadingAdvanced);
                    fadeInView(imgReadingPractice);
                } else {
                    imgIcMap2Reading.setImageResource(R.drawable.icon_map3);
                    fadeOutView(imgReadingAdvanced);
                    fadeOutView(imgReadingPractice);
                }
            });
        }

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuReadingActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuReadingActivity.this, "Error opening profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuReadingActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuReadingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (quizMenu != null) {
            quizMenu.setOnClickListener(v -> {
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    Intent intent = new Intent(MenuReadingActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuReadingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Reading History button
        if (readingHistoryMenu != null) {
            readingHistoryMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuReadingActivity.this, ReadingHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuReadingActivity.this, "Error opening reading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Bird menu - Despliega opciones
        if (birdMenu != null) {
            birdMenu.setOnClickListener(v -> {
                birdExpanded = !birdExpanded;
                if (birdExpanded) {
                    birdMenu.setImageResource(R.drawable.bird1_menu);
                    fadeInView(quizMenu);
                    fadeInView(readingHistoryMenu);
                } else {
                    birdMenu.setImageResource(R.drawable.bird0_menu);
                    fadeInView(quizMenu);
                    fadeOutView(readingHistoryMenu);
                }
            });
        }
    }

    private void fadeOutView(final View view) {
        if (view == null) return;
        
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(fadeOut);
    }

    private void fadeInView(final View view) {
        if (view == null) return;
        
        view.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        view.startAnimation(fadeIn);
    }

    //Método para aplicar bloqueo
    private void applyBlock(ImageView candado, String... dependencias) {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        boolean blocked = !isUnlockAllEnabled();
        if (!blocked) {
            candado.setVisibility(View.GONE);
            return;
        }
        for (String dep : dependencias) {
            if (prefs.getBoolean(dep, false)) {
                blocked = false;
                break;
            }
        }
        candado.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    //Return Menú
    private void ReturnMenu() {
        // Limpiar seguimiento de módulo
        ModuleTracker.clearLastModule(this);
        startActivity(new Intent(MenuReadingActivity.this, MainActivity.class));
        Toast.makeText(MenuReadingActivity.this, "Has retornado al menú principal correctamente.", Toast.LENGTH_SHORT).show();
    }

    // Aplicar efectos visuales de bloqueo para reading
    private void applyReadingVisualLockEffects() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Verificar progreso de speaking y aplicar efectos a reading
        boolean passedSpeakingAlphabet = prefs.getBoolean("PASSED_READING_ALPHABET", false);
        boolean passedSpeakingNumbers = prefs.getBoolean("PASSED_READING_NUMBERS", false);
        boolean passedSpeakingColors = prefs.getBoolean("PASSED_READING_COLORS", false);
        boolean passedSpeakingPronouns = prefs.getBoolean("PASSED_READING_PERSONAL_PRONOUNS", false);
        boolean passedSpeakingPossessive = prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false);
        boolean passedSpeakingPrepositions = prefs.getBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", false);
        boolean passedSpeakingAdjectives = prefs.getBoolean("PASSED_READING_ADJECTIVES", false);
        
        // Aplicar efectos de bloqueo basados en progreso de speaking
        boolean unlockedAll = isUnlockAllEnabled();
        applyReadingLockEffect(imgReadingComprehension, unlockedAll || passedSpeakingAlphabet);     // 2do tema
        applyReadingLockEffect(imgReadingVocabulary, unlockedAll || passedSpeakingNumbers);         // 3er tema
        applyReadingLockEffect(imgReadingGrammar, unlockedAll || passedSpeakingColors);          // 4to tema
        applyReadingLockEffect(imgReadingStories, unlockedAll || passedSpeakingPronouns);        // 5to tema
        applyReadingLockEffect(imgReadingAdvanced, unlockedAll || passedSpeakingPossessive);     // 6to tema
        applyReadingLockEffect(imgReadingPractice, unlockedAll || passedSpeakingPrepositions);       // 7mo tema
        
        Log.d(TAG, "Efectos visuales de bloqueo aplicados a reading");
        Log.d(TAG, "Translation: " + passedSpeakingAlphabet + ", Comprehension: " + passedSpeakingNumbers +
                  ", Vocabulary: " + passedSpeakingColors + ", Grammar: " + passedSpeakingPronouns +
                  ", Stories: " + passedSpeakingPossessive + ", Advanced: " + passedSpeakingPrepositions +
                  ", Practice: " + passedSpeakingAdjectives);
    }

    private void applyReadingLockEffect(ImageView imageView, boolean isUnlocked) {
        if (imageView == null) return;
        
        if (isUnlocked) {
            // Elemento desbloqueado - apariencia normal
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setElevation(0f);
            imageView.setTag(R.id.element_state, "enabled");
        } else {
            // Elemento bloqueado - aplicar efecto gris y transparente
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setElevation(2f); // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked");
        }
    }

    private void refreshReadingVisualEffects() {
        applyReadingVisualLockEffects();
    }

    private boolean isUnlockAllEnabled() {
        // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
        return freeRoamMode;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar efectos visuales al regresar a la actividad
        refreshReadingVisualEffects();
        // Reaplicar bloqueos/desbloqueos de candados según el progreso actualizado
        applyBlock(map_blocked_2Reading, "PASSED_READING_ALPHABET");
        applyBlock(map_blocked_3Reading, "PASSED_READING_NUMBERS");
        applyBlock(map_blocked_4Reading, "PASSED_READING_COLORS");
        applyBlock(map_blocked_5Reading, "PASSED_READING_PERSONAL_PRONOUNS");
    }

    // MÉTODO TEMPORAL PARA TESTING - Marcar todos los temas de reading como completados
    private void markAllReadingTopicsForTesting() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putBoolean("PASSED_READING_ALPHABET", true);
        editor.putBoolean("PASSED_READING_NUMBERS", true);
        editor.putBoolean("PASSED_READING_COLORS", true);
        editor.putBoolean("PASSED_READING_PERSONAL_PRONOUNS", true);
        editor.putBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", true);
        editor.putBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", true);
        editor.putBoolean("PASSED_READING_ADJECTIVES", true);
        
        editor.apply();
        Toast.makeText(this, "Todos los temas de Reading marcados como completados para testing.", Toast.LENGTH_SHORT).show();
        refreshReadingVisualEffects(); // Actualizar efectos visuales
    }
} 