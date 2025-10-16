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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * Mapa principal del módulo de Writing (A1).
 * Similar a MenuReadingActivity pero las llaves de progreso provienen del módulo de Reading.
 */
public class MenuWritingActivity extends AppCompatActivity {
    private static final String TAG = "MenuWritingActivity";

    // Elementos del primer grupo (Start Writing)
    private boolean startWritingExpanded = false;
    private ImageView eButtonStartWriting;
    private ImageView imgWritingAlphabet;
    private ImageView imgWritingNumbers;
    private ImageView imgWritingColors;
    private ImageView imgWritingPronouns;
    private ImageView imgWritingPossessive;

    // Segundo grupo (map 2)
    private boolean icMap2WritingExpanded = false;
    private ImageView imgIcMap2Writing;
    private ImageView imgWritingPrepositions;
    private ImageView imgWritingAdjectives;

    // Return button
    private LinearLayout eBtnReturnMenu;

    // Modo de navegación libre (sin dependencias)
    private boolean freeRoamMode = false;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView readingHistoryMenu;

    private ImageView eButtonProfile;
    private ImageView homeButton;

    // Secuencia de temas en orden y sus claves de progreso
    private static final String[] TOPICS = {
            "ALPHABET",
            "NUMBERS",
            "COLORS",
            "PERSONAL PRONOUNS",
            "POSSESSIVE ADJECTIVES",
            "PREPOSITIONS OF PLACE",
            "ADJECTIVES"
    };

    // Claves de escritura guardadas al completar un tema (se usan como prerequisito del siguiente)
    private static final String[] WRITING_KEYS = {
            "PASSED_WRITING_ALPHABET",
            "PASSED_WRITING_NUMBERS",
            "PASSED_WRITING_COLORS",
            "PASSED_WRITING_PERSONAL_PRONOUNS",
            "PASSED_WRITING_POSSESSIVE_ADJECTIVES",
            "PASSED_WRITING_PREPOSITIONS_OF_PLACE",
            "PASSED_WRITING_ADJECTIVES"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_writing);

        // Inicializar vistas
        eButtonStartWriting = findViewById(R.id.eButtonStartWriting);
        imgWritingAlphabet = findViewById(R.id.imgWritingAlphabet);
        imgWritingNumbers = findViewById(R.id.imgWritingNumbers);
        imgWritingColors = findViewById(R.id.imgWritingColors);
        imgWritingPronouns = findViewById(R.id.imgWritingPronouns);
        imgWritingPossessive = findViewById(R.id.imgWritingPossessive);

        imgIcMap2Writing = findViewById(R.id.imgIcMap2Writing);
        imgWritingPrepositions = findViewById(R.id.imgWritingPrepositions);
        imgWritingAdjectives = findViewById(R.id.imgWritingAdjectives);

        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);

        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        readingHistoryMenu = findViewById(R.id.imgReadingHistoryMenu);
        eButtonProfile = findViewById(R.id.btnProfile);
        homeButton = findViewById(R.id.homeButton);

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
                    applyWritingVisualLockEffects();
                });
            }
        }

        applyWritingVisualLockEffects();
        setupClickListeners();
    }

    private void setupClickListeners() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);

        // Alphabet (requires Reading Alphabet passed)
        imgWritingAlphabet.setOnClickListener(v -> openTopicIfUnlocked(0));
        imgWritingNumbers.setOnClickListener(v -> openTopicIfUnlocked(1));
        imgWritingColors.setOnClickListener(v -> openTopicIfUnlocked(2));
        imgWritingPronouns.setOnClickListener(v -> openTopicIfUnlocked(3));
        imgWritingPossessive.setOnClickListener(v -> openTopicIfUnlocked(4));
        imgWritingPrepositions.setOnClickListener(v -> openTopicIfUnlocked(5));
        imgWritingAdjectives.setOnClickListener(v -> openTopicIfUnlocked(6));

        // Start Writing toggle
        eButtonStartWriting.setOnClickListener(v -> {
            startWritingExpanded = !startWritingExpanded;
            if (startWritingExpanded) {
                fadeInView(imgWritingAlphabet);
                fadeInView(imgWritingNumbers);
                fadeInView(imgWritingColors);
                fadeInView(imgWritingPronouns);
                fadeInView(imgWritingPossessive);
            } else {
                fadeOutView(imgWritingAlphabet);
                fadeOutView(imgWritingNumbers);
                fadeOutView(imgWritingColors);
                fadeOutView(imgWritingPronouns);
                fadeOutView(imgWritingPossessive);
            }
        });

        // Map 2 toggle
        imgIcMap2Writing.setOnClickListener(v -> {
            icMap2WritingExpanded = !icMap2WritingExpanded;
            if (icMap2WritingExpanded) {
                fadeInView(imgWritingPrepositions);
                fadeInView(imgWritingAdjectives);
            } else {
                fadeOutView(imgWritingPrepositions);
                fadeOutView(imgWritingAdjectives);
            }
        });

        // Return button
        eBtnReturnMenu.setOnClickListener(v -> {
            ModuleTracker.clearLastModule(this);
            startActivity(new Intent(MenuWritingActivity.this, MainActivity.class));
            Toast.makeText(this, "Has retornado al menú principal correctamente.", Toast.LENGTH_SHORT).show();
        });

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuWritingActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuWritingActivity.this, "Error opening profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuWritingActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuWritingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (quizMenu != null) {
            quizMenu.setOnClickListener(v -> {
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    Intent intent = new Intent(MenuWritingActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuWritingActivity.this, "Error opening activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Reading History button
        if (readingHistoryMenu != null) {
            readingHistoryMenu.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MenuWritingActivity.this, ReadingHistoryActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MenuWritingActivity.this, "Error opening reading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    /**
     * Comprueba si el tema index está desbloqueado y, de ser así, abre WritingActivity.
     */
    private void openTopicIfUnlocked(int index) {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        String topic = TOPICS[index];

        if (isUnlockAllEnabled() || isTopicUnlocked(prefs, index)) {
            try {
                Intent intent = new Intent(MenuWritingActivity.this, WritingActivity.class);
                intent.putExtra("TOPIC", topic);
                intent.putExtra("LEVEL", "A1.1");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Error opening writing topic: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "✏️ Para practicar " + topic + ", completa antes el tema previo en Writing.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Un tema está desbloqueado si: (1) se aprobó el tema correspondiente en Reading y
     * (2) se aprobó el tema anterior de Writing (excepto el primero).
     */
    private boolean isTopicUnlocked(SharedPreferences prefs, int index) {
        if (isUnlockAllEnabled()) return true;
        // El primer tema está siempre disponible al iniciar Writing
        if (index == 0) return true;

        // Para los demás, debe haberse completado el tema previo de Writing
        return prefs.getBoolean(WRITING_KEYS[index - 1], false);
    }

    private void applyWritingVisualLockEffects() {
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);

        boolean unlockedAll = isUnlockAllEnabled();
        applyLockEffect(imgWritingAlphabet, unlockedAll || isTopicUnlocked(prefs, 0));
        applyLockEffect(imgWritingNumbers, unlockedAll || isTopicUnlocked(prefs, 1));
        applyLockEffect(imgWritingColors, unlockedAll || isTopicUnlocked(prefs, 2));
        applyLockEffect(imgWritingPronouns, unlockedAll || isTopicUnlocked(prefs, 3));
        applyLockEffect(imgWritingPossessive, unlockedAll || isTopicUnlocked(prefs, 4));
        applyLockEffect(imgWritingPrepositions, unlockedAll || isTopicUnlocked(prefs, 5));
        applyLockEffect(imgWritingAdjectives, unlockedAll || isTopicUnlocked(prefs, 6));
    }

    private void applyLockEffect(ImageView imageView, boolean isUnlocked) {
        if (isUnlocked) {
            imageView.clearColorFilter();
            imageView.setAlpha(1.0f);
            imageView.setTag(R.id.element_state, "enabled");
        } else {
            ColorFilter grayFilter = new PorterDuffColorFilter(
                    ContextCompat.getColor(this, R.color.locked_gray),
                    PorterDuff.Mode.MULTIPLY);
            imageView.setColorFilter(grayFilter);
            imageView.setAlpha(0.35f);
            imageView.setTag(R.id.element_state, "locked");
        }
    }

    private boolean isUnlockAllEnabled() {
        // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
        return freeRoamMode;
    }

    private void fadeInView(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        view.startAnimation(fadeIn);
    }

    private void fadeOutView(View view) {
        if (view == null) return;
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) { view.setVisibility(View.GONE); }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(fadeOut);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyWritingVisualLockEffects();
    }
} 