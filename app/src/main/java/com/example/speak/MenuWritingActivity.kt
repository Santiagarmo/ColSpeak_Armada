package com.example.speak

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Mapa principal del módulo de Writing (A1).
 * Similar a MenuReadingActivity pero las llaves de progreso provienen del módulo de Reading.
 */
class MenuWritingActivity : AppCompatActivity() {
    // Elementos del primer grupo (Start Writing)
    private var startWritingExpanded = false
    private var eButtonStartWriting: ImageView? = null
    private var imgWritingAlphabet: ImageView? = null
    private var imgWritingNumbers: ImageView? = null
    private var imgWritingColors: ImageView? = null
    private var imgWritingPronouns: ImageView? = null
    private var imgWritingPossessive: ImageView? = null

    // Segundo grupo (map 2)
    private var icMap2WritingExpanded = false
    private var imgIcMap2Writing: ImageView? = null
    private var imgWritingPrepositions: ImageView? = null
    private var imgWritingAdjectives: ImageView? = null

    // Return button
    private var eBtnReturnMenu: LinearLayout? = null

    // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
    // Modo de navegación libre (sin dependencias)
    private var isUnlockAllEnabled = false

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var readingHistoryMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null
    private var homeButton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_writing)

        // Inicializar vistas
        eButtonStartWriting = findViewById<ImageView>(R.id.eButtonStartWriting)
        imgWritingAlphabet = findViewById<ImageView>(R.id.imgWritingAlphabet)
        imgWritingNumbers = findViewById<ImageView>(R.id.imgWritingNumbers)
        imgWritingColors = findViewById<ImageView>(R.id.imgWritingColors)
        imgWritingPronouns = findViewById<ImageView>(R.id.imgWritingPronouns)
        imgWritingPossessive = findViewById<ImageView>(R.id.imgWritingPossessive)

        imgIcMap2Writing = findViewById<ImageView>(R.id.imgIcMap2Writing)
        imgWritingPrepositions = findViewById<ImageView>(R.id.imgWritingPrepositions)
        imgWritingAdjectives = findViewById<ImageView>(R.id.imgWritingAdjectives)

        eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)

        birdMenu = findViewById<ImageView?>(R.id.imgBirdMenu)
        quizMenu = findViewById<ImageView?>(R.id.imgQuizMenu)
        readingHistoryMenu = findViewById<ImageView?>(R.id.imgReadingHistoryMenu)
        eButtonProfile = findViewById<ImageView?>(R.id.btnProfile)
        homeButton = findViewById<ImageView?>(R.id.homeButton)

        // Leer modo libre desde Intent o SharedPreferences
        this.isUnlockAllEnabled = getIntent().getBooleanExtra("FREE_ROAM", false)
        if (!this.isUnlockAllEnabled) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            this.isUnlockAllEnabled = prefs.getBoolean("FREE_ROAM", false)
        }
        if (this.isUnlockAllEnabled) {
            val container = findViewById<View?>(R.id.freeRoamContainer)
            if (container != null) container.setVisibility(View.VISIBLE)
            val btn = findViewById<View?>(R.id.btnDisableFreeRoam)
            if (btn != null) {
                btn.setOnClickListener(View.OnClickListener { v: View? ->
                    val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("FREE_ROAM", false).apply()
                    if (container != null) container.setVisibility(View.GONE)
                    Toast.makeText(this, "Modo libre desactivado", Toast.LENGTH_SHORT).show()
                    // Refrescar candados/efectos al desactivar
                    applyWritingVisualLockEffects()
                })
            }
        }

        applyWritingVisualLockEffects()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)

        // Alphabet (requires Reading Alphabet passed)
        imgWritingAlphabet!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                0
            )
        })
        imgWritingNumbers!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                1
            )
        })
        imgWritingColors!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                2
            )
        })
        imgWritingPronouns!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                3
            )
        })
        imgWritingPossessive!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                4
            )
        })
        imgWritingPrepositions!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                5
            )
        })
        imgWritingAdjectives!!.setOnClickListener(View.OnClickListener { v: View? ->
            openTopicIfUnlocked(
                6
            )
        })

        // Start Writing toggle
        eButtonStartWriting!!.setOnClickListener(View.OnClickListener { v: View? ->
            startWritingExpanded = !startWritingExpanded
            if (startWritingExpanded) {
                fadeInView(imgWritingAlphabet)
                fadeInView(imgWritingNumbers)
                fadeInView(imgWritingColors)
                fadeInView(imgWritingPronouns)
                fadeInView(imgWritingPossessive)
            } else {
                fadeOutView(imgWritingAlphabet)
                fadeOutView(imgWritingNumbers)
                fadeOutView(imgWritingColors)
                fadeOutView(imgWritingPronouns)
                fadeOutView(imgWritingPossessive)
            }
        })

        // Map 2 toggle
        imgIcMap2Writing!!.setOnClickListener(View.OnClickListener { v: View? ->
            icMap2WritingExpanded = !icMap2WritingExpanded
            if (icMap2WritingExpanded) {
                fadeInView(imgWritingPrepositions)
                fadeInView(imgWritingAdjectives)
            } else {
                fadeOutView(imgWritingPrepositions)
                fadeOutView(imgWritingAdjectives)
            }
        })

        // Return button
        eBtnReturnMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
            ModuleTracker.clearLastModule(this)
            startActivity(Intent(this@MenuWritingActivity, MainActivity::class.java))
            Toast.makeText(
                this,
                "Has retornado al menú principal correctamente.",
                Toast.LENGTH_SHORT
            ).show()
        })

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuWritingActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuWritingActivity,
                        "Error opening profile: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (homeButton != null) {
            homeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuWritingActivity, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuWritingActivity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (quizMenu != null) {
            quizMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    val intent = Intent(this@MenuWritingActivity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuWritingActivity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Reading History button
        if (readingHistoryMenu != null) {
            readingHistoryMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent =
                        Intent(this@MenuWritingActivity, ReadingHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuWritingActivity,
                        "Error opening reading history: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Bird menu - Despliega opciones
        if (birdMenu != null) {
            birdMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                birdExpanded = !birdExpanded
                if (birdExpanded) {
                    birdMenu!!.setImageResource(R.drawable.bird1_menu)
                    fadeInView(quizMenu)
                    fadeInView(readingHistoryMenu)
                } else {
                    birdMenu!!.setImageResource(R.drawable.bird0_menu)
                    fadeInView(quizMenu)
                    fadeOutView(readingHistoryMenu)
                }
            })
        }
    }

    /**
     * Comprueba si el tema index está desbloqueado y, de ser así, abre WritingActivity.
     */
    private fun openTopicIfUnlocked(index: Int) {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val topic: String = TOPICS[index]

        if (this.isUnlockAllEnabled || isTopicUnlocked(prefs, index)) {
            try {
                val intent = Intent(this@MenuWritingActivity, WritingActivity::class.java)
                intent.putExtra("TOPIC", topic)
                intent.putExtra("LEVEL", "A1.1")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error opening writing topic: " + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "✏️ Para practicar " + topic + ", completa antes el tema previo en Writing.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Un tema está desbloqueado si: (1) se aprobó el tema correspondiente en Reading y
     * (2) se aprobó el tema anterior de Writing (excepto el primero).
     */
    private fun isTopicUnlocked(prefs: SharedPreferences, index: Int): Boolean {
        if (this.isUnlockAllEnabled) return true
        // El primer tema está siempre disponible al iniciar Writing
        if (index == 0) return true

        // Para los demás, debe haberse completado el tema previo de Writing
        return prefs.getBoolean(WRITING_KEYS[index - 1], false)
    }

    private fun applyWritingVisualLockEffects() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)

        val unlockedAll = this.isUnlockAllEnabled
        applyLockEffect(imgWritingAlphabet!!, unlockedAll || isTopicUnlocked(prefs, 0))
        applyLockEffect(imgWritingNumbers!!, unlockedAll || isTopicUnlocked(prefs, 1))
        applyLockEffect(imgWritingColors!!, unlockedAll || isTopicUnlocked(prefs, 2))
        applyLockEffect(imgWritingPronouns!!, unlockedAll || isTopicUnlocked(prefs, 3))
        applyLockEffect(imgWritingPossessive!!, unlockedAll || isTopicUnlocked(prefs, 4))
        applyLockEffect(imgWritingPrepositions!!, unlockedAll || isTopicUnlocked(prefs, 5))
        applyLockEffect(imgWritingAdjectives!!, unlockedAll || isTopicUnlocked(prefs, 6))
    }

    private fun applyLockEffect(imageView: ImageView, isUnlocked: Boolean) {
        if (isUnlocked) {
            imageView.clearColorFilter()
            imageView.setAlpha(1.0f)
            imageView.setTag(R.id.element_state, "enabled")
        } else {
            val grayFilter: ColorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            )
            imageView.setColorFilter(grayFilter)
            imageView.setAlpha(0.35f)
            imageView.setTag(R.id.element_state, "locked")
        }
    }

    private fun fadeInView(view: View?) {
        if (view == null) return
        view.setVisibility(View.VISIBLE)
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.setDuration(500)
        view.startAnimation(fadeIn)
    }

    private fun fadeOutView(view: View?) {
        if (view == null) return
        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.setDuration(500)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                view.setVisibility(View.GONE)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(fadeOut)
    }

    override fun onResume() {
        super.onResume()
        applyWritingVisualLockEffects()
    }

    companion object {
        private const val TAG = "MenuWritingActivity"

        // Secuencia de temas en orden y sus claves de progreso
        private val TOPICS = arrayOf<String>(
            "ALPHABET",
            "NUMBERS",
            "COLORS",
            "PERSONAL PRONOUNS",
            "POSSESSIVE ADJECTIVES",
            "PREPOSITIONS OF PLACE",
            "ADJECTIVES"
        )

        // Claves de escritura guardadas al completar un tema (se usan como prerequisito del siguiente)
        private val WRITING_KEYS = arrayOf<String?>(
            "PASSED_WRITING_ALPHABET",
            "PASSED_WRITING_NUMBERS",
            "PASSED_WRITING_COLORS",
            "PASSED_WRITING_PERSONAL_PRONOUNS",
            "PASSED_WRITING_POSSESSIVE_ADJECTIVES",
            "PASSED_WRITING_PREPOSITIONS_OF_PLACE",
            "PASSED_WRITING_ADJECTIVES"
        )
    }
}