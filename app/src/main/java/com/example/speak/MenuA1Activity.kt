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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.example.speak.MainActivity;
import com.example.speak.pronunciation.PronunciationActivity;

public class MenuA1Activity extends AppCompatActivity {

    //Declaramos las variables Start
    private boolean startExpanded = false;
    private ImageView eButtonStart;
    private ImageView imgABC;
    private ImageView imgNumbers;
    private ImageView imgColors;
    private ImageView imgPronouns;
    private ImageView imgPossesive;

    //Declaramos las variables icMap2
    private boolean icMap2Expanded = false;
    private ImageView imgIcMap2;
    private ImageView imgPrepos;
    private ImageView imgAdject;
    // Botones de identificación de imágenes
    private ImageView btnImageIdentification;
    private ImageView btnImageIdentificationAudio;

    // Image blocked
    private ImageView map_blocked_icMap2;
    private ImageView map_blocked_icMap3;
    private ImageView map_blocked_icMap4;
    private ImageView map_blocked_icMap5;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    //Return Menú
    private LinearLayout eBtnReturnMenu;

    // Modo de navegación libre (sin dependencias)
    private boolean freeRoamMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_a1);

        try {
            //Inicializamos las variables Start
            eButtonStart = findViewById(R.id.eButtonStart);
            imgABC = findViewById(R.id.imgABC);
            imgNumbers = findViewById(R.id.imgNumbers);
            imgColors = findViewById(R.id.imgColors);
            imgPronouns = findViewById(R.id.imgPronouns);
            imgPossesive = findViewById(R.id.imgPossesive);

            //Inicializamos las variables imgIcMap2
            imgIcMap2 = findViewById(R.id.imgIcMap2);
            imgPrepos = findViewById(R.id.imgPrepos);
            imgAdject = findViewById(R.id.imgAdject);
            //Inicializamos los botones de identificación de imágenes
            btnImageIdentification = findViewById(R.id.btnImageIdentification);
            btnImageIdentificationAudio = findViewById(R.id.btnImageIdentificationAudio);

            //Blocked
            map_blocked_icMap2 = findViewById(R.id.map_blocked_icMap2);
            map_blocked_icMap3 = findViewById(R.id.map_blocked_icMap3);
            map_blocked_icMap4 = findViewById(R.id.map_blocked_icMap4);
            map_blocked_icMap5 = findViewById(R.id.map_blocked_icMap5);

            //Declaramos las variables Menu
            birdMenu = findViewById(R.id.imgBirdMenu);
            quizMenu = findViewById(R.id.imgQuizMenu);
            pronunMenu = findViewById(R.id.imgPronunMenu);
            eButtonProfile = findViewById(R.id.btnProfile);
            homeButton = findViewById(R.id.homeButton);



            // Verificar progreso por niveles y desbloquear temas
            checkLevelProgressAndUnlockTopics();
            
            // Aplicar efectos visuales de bloqueo
            applyVisualLockEffects();
            
            setupClickListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    freeRoamMode = false; // Actualizar la variable local
                    refreshVisualEffects();
                });
            }
        }

        applyBlock(map_blocked_icMap2, "DEP_MAP2_ACT1", "DEP_MAP2_ACT2");
        applyBlock(map_blocked_icMap3, "DEP_MAP3_ACT1", "DEP_MAP3_ACT2");
        applyBlock(map_blocked_icMap4, "DEP_MAP4_ACT1", "DEP_MAP4_ACT2");
        applyBlock(map_blocked_icMap5, "DEP_MAP5_ACT1", "DEP_MAP5_ACT2");

        //Return Menu
        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });
    }

    // Verificar progreso por niveles y desbloquear temas automáticamente
    private void checkLevelProgressAndUnlockTopics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Verificar si se completó el nivel A1.1 (70% o más en el quiz)
        boolean passedA1_1 = prefs.getBoolean("PASSED_LEVEL_A1_1", false);
        int scoreA1_1 = prefs.getInt("SCORE_LEVEL_A1_1", 0);
        
        if (passedA1_1 || scoreA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A1.1
            unlockRemainingA1_1Topics();
            Log.d("MenuA1Activity", "Temas restantes de A1.1 desbloqueados automáticamente");
        }
        
        // Verificar si se completó el nivel A1.2
        boolean passedA1_2 = prefs.getBoolean("PASSED_LEVEL_A1_2", false);
        int scoreA1_2 = prefs.getInt("SCORE_LEVEL_A1_2", 0);
        
        if (passedA1_2 || scoreA1_2 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A1.2
            unlockRemainingA1_2Topics();
            Log.d("MenuA1Activity", "Temas restantes de A1.2 desbloqueados automáticamente");
        }
        
        // Verificar si se completó el nivel A2.1
        boolean passedA2_1 = prefs.getBoolean("PASSED_LEVEL_A2_1", false);
        int scoreA2_1 = prefs.getInt("SCORE_LEVEL_A2_1", 0);
        
        if (passedA2_1 || scoreA2_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A2.1
            unlockRemainingA2_1Topics();
            Log.d("MenuA1Activity", "Temas restantes de A2.1 desbloqueados automáticamente");
        }
    }

    // Desbloquear solo los temas restantes del nivel A1.1 (respeta progresión)
    private void unlockRemainingA1_1Topics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_ALPHABET", false)) {
            editor.putBoolean("PASSED_ALPHABET", true);
            Log.d("MenuA1Activity", "ALPHABET desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_NUMBERS", false)) {
            editor.putBoolean("PASSED_NUMBERS", true);
            Log.d("MenuA1Activity", "NUMBERS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_COLORS", false)) {
            editor.putBoolean("PASSED_COLORS", true);
            Log.d("MenuA1Activity", "COLORS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_PERSONAL_PRONOUNS", true);
            Log.d("MenuA1Activity", "PERSONAL PRONOUNS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_POSSESSIVE_ADJECTIVES", true);
            Log.d("MenuA1Activity", "POSSESSIVE ADJECTIVES desbloqueado automáticamente");
        }

        // NO desbloquear PREPOSITIONS automáticamente - requiere completar writing imgIcMap2
// if (!prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false)) {
//     editor.putBoolean("PASSED_PREPOSITIONS_OF_PLACE", true);
// }

        // No se desbloquean temas avanzados (Prepositions, Adjectives, Guess Picture...) aquí; 
        // esos se habilitarán solo después de completar el Start del siguiente mundo.
        
        editor.apply();
    }

    // Desbloquear solo los temas restantes del nivel A1.2 (respeta progresión)
    private void unlockRemainingA1_2Topics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_VERBS_TO_BE", false)) {
            editor.putBoolean("PASSED_VERBS_TO_BE", true);
            Log.d("MenuA1Activity", "VERBS TO BE desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PRESENT_SIMPLE", false)) {
            editor.putBoolean("PASSED_PRESENT_SIMPLE", true);
            Log.d("MenuA1Activity", "PRESENT SIMPLE desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_DAILY_ROUTINES", false)) {
            editor.putBoolean("PASSED_DAILY_ROUTINES", true);
            Log.d("MenuA1Activity", "DAILY ROUTINES desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_FOOD_AND_DRINKS", false)) {
            editor.putBoolean("PASSED_FOOD_AND_DRINKS", true);
            Log.d("MenuA1Activity", "FOOD AND DRINKS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_FAMILY_MEMBERS", false)) {
            editor.putBoolean("PASSED_FAMILY_MEMBERS", true);
            Log.d("MenuA1Activity", "FAMILY MEMBERS desbloqueado automáticamente");
        }
        
        editor.apply();
    }

    // Desbloquear solo los temas restantes del nivel A2.1 (respeta progresión)
    private void unlockRemainingA2_1Topics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_PAST_SIMPLE", false)) {
            editor.putBoolean("PASSED_PAST_SIMPLE", true);
            Log.d("MenuA1Activity", "PAST SIMPLE desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_IRREGULAR_VERBS", false)) {
            editor.putBoolean("PASSED_IRREGULAR_VERBS", true);
            Log.d("MenuA1Activity", "IRREGULAR VERBS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_HOBBIES_AND_INTERESTS", false)) {
            editor.putBoolean("PASSED_HOBBIES_AND_INTERESTS", true);
            Log.d("MenuA1Activity", "HOBBIES AND INTERESTS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_WEATHER_AND_SEASONS", false)) {
            editor.putBoolean("PASSED_WEATHER_AND_SEASONS", true);
            Log.d("MenuA1Activity", "WEATHER AND SEASONS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_TRAVEL_AND_TRANSPORTATION", false)) {
            editor.putBoolean("PASSED_TRAVEL_AND_TRANSPORTATION", true);
            Log.d("MenuA1Activity", "TRAVEL AND TRANSPORTATION desbloqueado automáticamente");
        }
        
        editor.apply();
    }

    private void setupClickListeners() {
        if (imgABC != null) {
            imgABC.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuA1Activity.this, AlphabetActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuA1Activity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (imgNumbers != null) {
            imgNumbers.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_ALPHABET", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, NumberActivity.class);
                    intent.putExtra("TOPIC", "NUMBERS");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión NUMBERS, Completa ALPHABET.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (imgColors != null) {
            imgColors.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_NUMBERS", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, ColorActivity.class);
                    intent.putExtra("TOPIC", "COLORS");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión COLORS, Completa NUMBERS.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (imgPronouns != null) {
            imgPronouns.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_COLORS", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, PronounsActivity.class);
                    intent.putExtra("TOPIC", "PERSONAL PRONOUNS");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión PERSONAL PRONOUNS, Completa COLORS.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (imgPossesive != null) {
            imgPossesive.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, PossessiveAdjectActivity.class);
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión POSSESSIVE ADJECTIVES Completa PERSONAL PRONOUNS.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (eButtonStart != null) {
            eButtonStart.setOnClickListener(v -> {
                startExpanded = !startExpanded;
                if (startExpanded) {
                    eButtonStart.setImageResource(R.drawable.start);
                    fadeInView(imgABC);
                    fadeInView(imgNumbers);
                    fadeInView(imgColors);
                    fadeInView(imgPronouns);
                    fadeInView(imgPossesive);
                } else {
                    eButtonStart.setImageResource(R.drawable.start);
                    fadeInView(imgABC);
                    fadeInView(imgNumbers);
                    fadeInView(imgColors);
                    fadeOutView(imgPronouns);
                    fadeOutView(imgPossesive);
                }
            });
        }

        if (imgPrepos != null) {
            imgPrepos.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                /*boolean passedAdjectives = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
                if (passedAdjectives) {
                    Intent intent = new Intent(MenuA1Activity.this, ListeningActivity.class);
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión PREPOSITIONS OF PLACE Completa POSSESSIVE ADJECTIVES.", Toast.LENGTH_LONG).show();
                }*/
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, ListeningActivity.class);
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión PREPOSITIONS OF PLACE, completa POSSESSIVE ADJECTIVES en Writing primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (imgAdject != null) {
            imgAdject.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, ListeningActivity.class);
                    intent.putExtra("TOPIC", "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión ADJECTIVES Completa PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // Click listener para el botón de identificación de imágenes
        if (btnImageIdentification != null) {
            btnImageIdentification.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_ADJECTIVES", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, ImageIdentificationActivity.class);
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES");
                    intent.putExtra("LEVEL", "A1.1");
                    intent.putExtra("SOURCE_MAP", "LISTENING");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión GUESS PICTURE Completa ADJECTIVES (FEELINGS).", Toast.LENGTH_LONG).show();
                }
            });
        }

        // Click listener para el botón de identificación de imágenes con audio
        if (btnImageIdentificationAudio != null) {
            btnImageIdentificationAudio.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean unlocked = isUnlockAllEnabled() || prefs.getBoolean("PASSED_GUESS_PICTURE", false);
                if (unlocked) {
                    Intent intent = new Intent(MenuA1Activity.this, ImageIdentificationAudioActivity.class);
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES");
                    intent.putExtra("LEVEL", "A1.1");
                    intent.putExtra("SOURCE_MAP", "LISTENING");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión LISTEN GUESS Completa GUESS PICTURE.", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (imgIcMap2 != null) {
            imgIcMap2.setOnClickListener(v -> {
                icMap2Expanded = !icMap2Expanded;
                if (icMap2Expanded) {
                    imgIcMap2.setImageResource(R.drawable.icon_map2);
                    fadeInView(imgPrepos);
                    fadeInView(imgAdject);
                    fadeInView(btnImageIdentification);
                    fadeInView(btnImageIdentificationAudio);
                } else {
                    imgIcMap2.setImageResource(R.drawable.icon_map2);
                    fadeOutView(imgPrepos);
                    fadeOutView(imgAdject);
                    fadeInView(btnImageIdentification);
                    fadeInView(btnImageIdentificationAudio);
                }
            });
        }

        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuA1Activity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuA1Activity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuA1Activity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuA1Activity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });

        if (quizMenu != null) {
            quizMenu.setOnClickListener(v -> {
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    Intent intent = new Intent(MenuA1Activity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuA1Activity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (pronunMenu != null) {
            pronunMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuA1Activity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuA1Activity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (birdMenu != null) {
            birdMenu.setOnClickListener(v -> {
                birdExpanded = !birdExpanded;
                if (birdExpanded) {
                    birdMenu.setImageResource(R.drawable.bird1_menu);
                    fadeInView(quizMenu);
                    fadeInView(pronunMenu);
                } else {
                    birdMenu.setImageResource(R.drawable.bird0_menu);
                    fadeOutView(quizMenu);
                    fadeOutView(pronunMenu);
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
        ModuleTracker.clearLastModule(this);
        startActivity(new Intent(MenuA1Activity.this, MainActivity.class));
        Toast.makeText(MenuA1Activity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }

    // Aplicar efectos visuales de bloqueo a todos los elementos del menú
    private void applyVisualLockEffects() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);

        boolean unlockedAll = isUnlockAllEnabled();
        boolean speakingStartCompleted = unlockedAll || areAllSpeakingStartTopicsCompleted(prefs);
        
        // Aplicar efectos de bloqueo basados en el progreso
        applyLockEffect(imgABC, true); // ABC siempre desbloqueado
        
        boolean passedAlphabet = prefs.getBoolean("PASSED_ALPHABET", false);
        applyLockEffect(imgNumbers, unlockedAll || passedAlphabet);
        
        boolean passedNumbers = prefs.getBoolean("PASSED_NUMBERS", false);
        applyLockEffect(imgColors, unlockedAll || passedNumbers);
        
        boolean passedColors = prefs.getBoolean("PASSED_COLORS", false);
        applyLockEffect(imgPronouns, unlockedAll || passedColors);
        
        boolean passedPronouns = prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false);
        applyLockEffect(imgPossesive, unlockedAll || passedPronouns);
        
        boolean passedPossessive = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
        applyLockEffect(imgPrepos, unlockedAll || (passedPossessive && speakingStartCompleted));
        
        boolean passedPrepositions = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false);
        applyLockEffect(imgAdject, unlockedAll || (passedPrepositions && speakingStartCompleted));

        /*boolean passedAdjectives = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
        applyLockEffect(imgAdject, passedAdjectives);*/
        // PREPOSITIONS en listening requiere completar POSSESSIVE ADJECTIVES en Writing
        boolean passedWritingPossessive = prefs.getBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", false);
        applyLockEffect(imgPrepos, unlockedAll || (passedWritingPossessive && speakingStartCompleted));

        boolean passedAdjFeelings = prefs.getBoolean("PASSED_ADJECTIVES", false);
        applyLockEffect(imgAdject, unlockedAll || passedAdjFeelings);

        boolean passedGuessPict = prefs.getBoolean("PASSED_GUESS_PICTURE", false);
        applyLockEffect(imgAdject, unlockedAll || passedGuessPict);
        
        // CAMBIO: Aplicar bloqueo por color según progreso real
        // btnImageIdentification (GUESS PICTURE) - requiere ADJECTIVES completado
        applyLockEffect(btnImageIdentification, unlockedAll || (passedAdjFeelings && speakingStartCompleted));
        
        // btnImageIdentificationAudio (LISTEN GUESS) - requiere GUESS PICTURE completado
        boolean passedGuessPicture = prefs.getBoolean("PASSED_GUESS_PICTURE", false);
        applyLockEffect(btnImageIdentificationAudio, unlockedAll || (passedGuessPicture && speakingStartCompleted));

        Log.d("MenuA1Activity", "Speaking start completed: " + speakingStartCompleted);
        Log.d("MenuA1Activity", "Efectos visuales de bloqueo aplicados");
        Log.d("MenuA1Activity", "GUESS PICTURE desbloqueado: " + passedAdjFeelings);
        Log.d("MenuA1Activity", "LISTEN GUESS desbloqueado: " + passedGuessPicture);
    }

    // Aplicar efecto visual de bloqueo o desbloqueado a un ImageView
    private void applyLockEffect(ImageView imageView, boolean isUnlocked) {
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

    // Método para refrescar los efectos visuales cuando cambie el progreso
    private void refreshVisualEffects() {
        applyVisualLockEffects();
    }

    private boolean isUnlockAllEnabled() {
        // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
        return freeRoamMode;
    }

    // Comprueba si los 5 temas básicos de Speaking están completados
    private boolean areAllSpeakingStartTopicsCompleted(SharedPreferences prefs) {
        return prefs.getBoolean("PASSED_PRON_ALPHABET", false) &&
               prefs.getBoolean("PASSED_PRON_NUMBERS", false) &&
               prefs.getBoolean("PASSED_PRON_COLORS", false) &&
               prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false) &&
               prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false);
    }
}
