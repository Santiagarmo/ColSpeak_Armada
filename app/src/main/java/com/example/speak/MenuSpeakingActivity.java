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

import com.example.speak.pronunciation.PronunciationActivity;

public class MenuSpeakingActivity extends AppCompatActivity {

    private static final String TAG = "MenuSpeakingActivity";

    //Declaramos las variables Start Speaking
    private boolean startSpeakingExpanded = false;
    private ImageView eButtonStartSpeaking;
    private ImageView imgSpeakingABC;
    private ImageView imgSpeakingNumbers;
    private ImageView imgSpeakingColors;
    private ImageView imgSpeakingPronouns;
    private ImageView imgSpeakingPossessive;

    //Declaramos las variables icMap2 Speaking
    private boolean icMap2SpeakingExpanded = false;
    private ImageView imgIcMap2Speaking;
    private ImageView imgSpeakingPrepos;
    private ImageView imgSpeakingAdjectives;

    // Image blocked
    private ImageView map_blocked_2Speaking;
    private ImageView map_blocked_3Speaking;
    private ImageView map_blocked_4Speaking;
    private ImageView map_blocked_5Speaking;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunHistoryMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    //Return Menú
    private LinearLayout eBtnReturnMenu;
    // Modo libre
    private boolean freeRoamMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_speaking);

        try {
            //Inicializamos las variables Start Speaking
            eButtonStartSpeaking = findViewById(R.id.eButtonStartSpeaking);
            imgSpeakingABC = findViewById(R.id.imgSpeakingABC);
            imgSpeakingNumbers = findViewById(R.id.imgSpeakingNumbers);
            imgSpeakingColors = findViewById(R.id.imgSpeakingColors);
            imgSpeakingPronouns = findViewById(R.id.imgSpeakingPronouns);
            imgSpeakingPossessive = findViewById(R.id.imgSpeakingPossessive);

            //Inicializamos las variables imgIcMap2 Speaking
            imgIcMap2Speaking = findViewById(R.id.imgIcMap2Speaking);
            imgSpeakingPrepos = findViewById(R.id.imgSpeakingPrepos);
            imgSpeakingAdjectives = findViewById(R.id.imgSpeakingAdjectives);

            //Blocked
            map_blocked_2Speaking = findViewById(R.id.map_blocked_2Speaking);
            map_blocked_3Speaking = findViewById(R.id.map_blocked_3Speaking);
            map_blocked_4Speaking = findViewById(R.id.map_blocked_4Speaking);
            map_blocked_5Speaking = findViewById(R.id.map_blocked_5Speaking);

            //Declaramos las variables Menu
            birdMenu = findViewById(R.id.imgBirdMenu);
            quizMenu = findViewById(R.id.imgQuizMenu);
            pronunHistoryMenu = findViewById(R.id.imgPronunHistoryMenu);
            eButtonProfile = findViewById(R.id.btnProfile);
            homeButton = findViewById(R.id.homeButton);

            // Verificar progreso de speaking y aplicar efectos visuales
            checkSpeakingProgressAndUnlockTopics();
            applySpeakingVisualLockEffects();
            
            setupSpeakingClickListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing speaking menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        applyBlock(map_blocked_2Speaking, "DEP_MAP2_ACT1", "DEP_MAP2_ACT2");
        applyBlock(map_blocked_3Speaking, "DEP_MAP3_ACT1", "DEP_MAP3_ACT2");
        applyBlock(map_blocked_4Speaking, "DEP_MAP4_ACT1", "DEP_MAP4_ACT2");
        applyBlock(map_blocked_5Speaking, "DEP_MAP5_ACT1", "DEP_MAP5_ACT2");

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
                    refreshSpeakingVisualEffects();
                });
            }
        }

        //Return Menu
        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });
    }

    // Verificar progreso de speaking y desbloquear temas automáticamente
    private void checkSpeakingProgressAndUnlockTopics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Verificar si se completó el speaking de nivel A1.1 (70% o más)
        boolean passedSpeakingA1_1 = prefs.getBoolean("PASSED_SPEAKING_LEVEL_A1_1", false);
        int scoreSpeakingA1_1 = prefs.getInt("SCORE_SPEAKING_LEVEL_A1_1", 0);
        
        if (passedSpeakingA1_1 || scoreSpeakingA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del speaking A1.1
            unlockRemainingSpeakingA1_1Topics();
            Log.d(TAG, "Temas restantes de Speaking A1.1 desbloqueados automáticamente");
        }
    }

    // Desbloquear solo los temas restantes del speaking nivel A1.1
    private void unlockRemainingSpeakingA1_1Topics() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Solo desbloquear si no están ya desbloqueados - Usamos claves específicas para speaking
        if (!prefs.getBoolean("PASSED_PRON_ALPHABET", false)) {
            editor.putBoolean("PASSED_PRON_ALPHABET", true);
            Log.d(TAG, "Speaking ALPHABET desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PRON_NUMBERS", false)) {
            editor.putBoolean("PASSED_PRON_NUMBERS", true);
            Log.d(TAG, "Speaking NUMBERS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PRON_COLORS", false)) {
            editor.putBoolean("PASSED_PRON_COLORS", true);
            Log.d(TAG, "Speaking COLORS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_PRON_PERSONAL_PRONOUNS", true);
            Log.d(TAG, "Speaking PERSONAL PRONOUNS desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", true);
            Log.d(TAG, "Speaking POSSESSIVE ADJECTIVES desbloqueado automáticamente");
        }
        // NO desbloquear PREPOSITIONS automáticamente - requiere completar listening imgIcMap2
        /*if (!prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false)) {
            editor.putBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", true);
            Log.d(TAG, "Speaking PREPOSITIONS OF PLACE desbloqueado automáticamente");
        }*/
        if (!prefs.getBoolean("PASSED_PRON_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_PRON_ADJECTIVES", true);
            Log.d(TAG, "Speaking ADJECTIVES desbloqueado automáticamente");
        }
        
        editor.apply();
    }

    private void setupSpeakingClickListeners() {
        // ALPHABET SPEAKING - Siempre disponible
        if (imgSpeakingABC != null) {
            imgSpeakingABC.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronAlphabetActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuSpeakingActivity.this, "Error opening speaking activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // NUMBERS SPEAKING - Requiere completar Alphabet Speaking
        if (imgSpeakingNumbers != null) {
            imgSpeakingNumbers.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean passedPronAlphabet = prefs.getBoolean("PASSED_PRON_ALPHABET", false);
                if (passedPronAlphabet) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronNumberActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "🗣️ Para practicar NUMBERS, completa ALPHABET pronunciation primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // COLORS SPEAKING - Requiere completar Numbers Speaking
        if (imgSpeakingColors != null) {
            imgSpeakingColors.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean passedPronNumbers = prefs.getBoolean("PASSED_PRON_NUMBERS", false);
                if (passedPronNumbers) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronColorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "🗣️ Para practicar COLORS, completa NUMBERS pronunciation primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // PERSONAL PRONOUNS SPEAKING - Requiere completar Colors Speaking
        if (imgSpeakingPronouns != null) {
            imgSpeakingPronouns.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean passedPronColors = prefs.getBoolean("PASSED_PRON_COLORS", false);
                if (passedPronColors) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronPersProActivity.class);
                    intent.putExtra("TOPIC", "PERSONAL PRONOUNS");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "🗣️ Para practicar PERSONAL PRONOUNS, completa COLORS pronunciation primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // POSSESSIVE ADJECTIVES SPEAKING - Requiere completar Personal Pronouns Speaking
        if (imgSpeakingPossessive != null) {
            imgSpeakingPossessive.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean passedPronPronouns = prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false);
                if (passedPronPronouns) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronPosseAdjectActivity.class);
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "🗣️ Para practicar POSSESSIVE ADJECTIVES, completa PERSONAL PRONOUNS pronunciation primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // PREPOSITIONS SPEAKING - Requiere completar Possessive Adjectives Speaking
        if (imgSpeakingPrepos != null) {
            imgSpeakingPrepos.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                //boolean passedPronPossessive = prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false);
                //if (passedPronPossessive) {
                boolean passedListeningIcMap2 = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false) &&
                        prefs.getBoolean("PASSED_ADJECTIVES", false) &&
                        prefs.getBoolean("PASSED_GUESS_PICTURE", false);
                if (passedListeningIcMap2) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronunciationActivity.class);
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    //Toast.makeText(this, "🗣️ Para practicar PREPOSITIONS, completa POSSESSIVE ADJECTIVES pronunciation primero.", Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "🗣️ Para practicar PREPOSITIONS, completa todo el módulo avanzado de listening primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // ADJECTIVES SPEAKING - Requiere completar Prepositions Speaking
        if (imgSpeakingAdjectives != null) {
            imgSpeakingAdjectives.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
                boolean passedPronPrepositions = prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false);
                if (passedPronPrepositions) {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronunciationActivity.class);
                    intent.putExtra("TOPIC", "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "🗣️ Para practicar ADJECTIVES, completa PREPOSITIONS pronunciation primero.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // Botón Start Speaking - Despliega los primeros elementos
        if (eButtonStartSpeaking != null) {
            eButtonStartSpeaking.setOnClickListener(v -> {
                startSpeakingExpanded = !startSpeakingExpanded;
                if (startSpeakingExpanded) {
                    eButtonStartSpeaking.setImageResource(R.drawable.start);
                    fadeInView(imgSpeakingABC);
                    fadeInView(imgSpeakingNumbers);
                    fadeInView(imgSpeakingColors);
                    fadeInView(imgSpeakingPronouns);
                    fadeInView(imgSpeakingPossessive);
                } else {
                    eButtonStartSpeaking.setImageResource(R.drawable.start);
                    fadeInView(imgSpeakingABC);
                    fadeInView(imgSpeakingNumbers);
                    fadeInView(imgSpeakingColors);
                    fadeOutView(imgSpeakingPronouns);
                    fadeOutView(imgSpeakingPossessive);
                }
            });
        }

        // Map Icon 2 Speaking - Despliega elementos avanzados
        if (imgIcMap2Speaking != null) {
            imgIcMap2Speaking.setOnClickListener(v -> {
                icMap2SpeakingExpanded = !icMap2SpeakingExpanded;
                if (icMap2SpeakingExpanded) {
                    imgIcMap2Speaking.setImageResource(R.drawable.icon_map2);
                    fadeInView(imgSpeakingPrepos);
                    fadeInView(imgSpeakingAdjectives);
                } else {
                    imgIcMap2Speaking.setImageResource(R.drawable.icon_map2);
                    fadeOutView(imgSpeakingPrepos);
                    fadeOutView(imgSpeakingAdjectives);
                }
            });
        }

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuSpeakingActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuSpeakingActivity.this, "Error opening profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuSpeakingActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuSpeakingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (quizMenu != null) {
            quizMenu.setOnClickListener(v -> {
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    Intent intent = new Intent(MenuSpeakingActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuSpeakingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Pronunciation History button
        if (pronunHistoryMenu != null) {
            pronunHistoryMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuSpeakingActivity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuSpeakingActivity.this, "Error opening pronunciation history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    fadeInView(pronunHistoryMenu);
                } else {
                    birdMenu.setImageResource(R.drawable.bird0_menu);
                    fadeInView(quizMenu);
                    fadeOutView(pronunHistoryMenu);
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

    //Return Menú
    private void ReturnMenu() {
        ModuleTracker.clearLastModule(this);
        startActivity(new Intent(MenuSpeakingActivity.this, MainActivity.class));
        Toast.makeText(MenuSpeakingActivity.this, "Has retornado al menú principal correctamente.", Toast.LENGTH_SHORT).show();
    }

    // Aplicar efectos visuales de bloqueo para speaking
    private void applySpeakingVisualLockEffects() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        
        // Verificar progreso y aplicar efectos
        boolean passedPronAlphabet = prefs.getBoolean("PASSED_PRON_ALPHABET", false);
        boolean passedPronNumbers = prefs.getBoolean("PASSED_PRON_NUMBERS", false);
        boolean passedPronColors = prefs.getBoolean("PASSED_PRON_COLORS", false);
        boolean passedPronPronouns = prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false);
        boolean passedPronPossessive = prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false);
        boolean passedPronPrepositions = prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false);
        
        // Aplicar efectos de bloqueo
        applySpeakingLockEffect(imgSpeakingNumbers, passedPronAlphabet);
        applySpeakingLockEffect(imgSpeakingColors, passedPronNumbers);
        applySpeakingLockEffect(imgSpeakingPronouns, passedPronColors);
        applySpeakingLockEffect(imgSpeakingPossessive, passedPronPronouns);
        //applySpeakingLockEffect(imgSpeakingPrepos, passedPronPossessive);
        // PREPOSITIONS en speaking requiere completar todo el módulo imgIcMap2 de listening
        boolean passedListeningIcMap2 = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false) &&
                prefs.getBoolean("PASSED_ADJECTIVES", false) &&
                prefs.getBoolean("PASSED_GUESS_PICTURE", false);
        applySpeakingLockEffect(imgSpeakingPrepos, passedListeningIcMap2);
        applySpeakingLockEffect(imgSpeakingAdjectives, passedPronPrepositions);
    }

    private void applySpeakingLockEffect(ImageView imageView, boolean isUnlocked) {
        if (imageView == null) return;
        
        if (!isUnlocked) {
            // Aplicar filtro gris y reducir opacidad
            ColorFilter grayFilter = new PorterDuffColorFilter(
                ContextCompat.getColor(this, android.R.color.darker_gray),
                PorterDuff.Mode.MULTIPLY
            );
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.5f);
        } else {
            // Remover filtros si está desbloqueado
            imageView.setColorFilter(null);
            imageView.setAlpha(1.0f);
        }
    }

    private void refreshSpeakingVisualEffects() {
        applySpeakingVisualLockEffects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar efectos visuales al regresar a la actividad
        refreshSpeakingVisualEffects();
    }

    //Método para aplicar bloqueo
    private void applyBlock(ImageView candado, String... dependencias) {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        boolean blocked = true;
        for (String dep : dependencias) {
            if (prefs.getBoolean(dep, false)) {
                blocked = false;
                break;
            }
        }
        candado.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }
} 