package com.example.speak

import android.content.Intent
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// Added import for TextView
class MenuReadingActivity : AppCompatActivity() {
    //Declaramos las variables Start Reading
    private var startReadingExpanded = false
    private var eButtonStartReading: ImageView? = null
    private var imgReadingTranslation: ImageView? = null
    private var imgReadingComprehension: ImageView? = null
    private var imgReadingVocabulary: ImageView? = null
    private var imgReadingGrammar: ImageView? = null
    private var imgReadingStories: ImageView? = null

    //Declaramos las variables icMap2 Reading
    private var icMap2ReadingExpanded = false
    private var imgIcMap2Reading: ImageView? = null
    private var imgReadingAdvanced: ImageView? = null
    private var imgReadingPractice: ImageView? = null

    // Image blocked
    private var map_blocked_2Reading: ImageView? = null
    private var map_blocked_3Reading: ImageView? = null
    private var map_blocked_4Reading: ImageView? = null
    private var map_blocked_5Reading: ImageView? = null

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var readingHistoryMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null
    private var homeButton: ImageView? = null

    //Return Menú
    private var eBtnReturnMenu: LinearLayout? = null

    // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
    // Modo de navegación libre (sin dependencias)
    private var isUnlockAllEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_reading)

        try {
            //Inicializamos las variables Start Reading
            eButtonStartReading = findViewById<ImageView?>(R.id.eButtonStartReading)
            imgReadingTranslation = findViewById<ImageView?>(R.id.imgReadingTranslation)
            imgReadingComprehension = findViewById<ImageView?>(R.id.imgReadingComprehension)
            imgReadingVocabulary = findViewById<ImageView?>(R.id.imgReadingVocabulary)
            imgReadingGrammar = findViewById<ImageView?>(R.id.imgReadingGrammar)
            imgReadingStories = findViewById<ImageView?>(R.id.imgReadingStories)

            //Inicializamos las variables imgIcMap2 Reading
            imgIcMap2Reading = findViewById<ImageView?>(R.id.imgIcMap2Reading)
            imgReadingAdvanced = findViewById<ImageView?>(R.id.imgReadingAdvanced)
            imgReadingPractice = findViewById<ImageView?>(R.id.imgReadingPractice)

            //Blocked
            map_blocked_2Reading = findViewById<ImageView>(R.id.map_blocked_2Reading)
            map_blocked_3Reading = findViewById<ImageView>(R.id.map_blocked_3Reading)
            map_blocked_4Reading = findViewById<ImageView>(R.id.map_blocked_4Reading)
            map_blocked_5Reading = findViewById<ImageView>(R.id.map_blocked_5Reading)

            //Declaramos las variables Menu
            birdMenu = findViewById<ImageView?>(R.id.imgBirdMenu)
            quizMenu = findViewById<ImageView?>(R.id.imgQuizMenu)
            readingHistoryMenu = findViewById<ImageView?>(R.id.imgReadingHistoryMenu)
            eButtonProfile = findViewById<ImageView?>(R.id.btnProfile)
            homeButton = findViewById<ImageView?>(R.id.homeButton)

            // Verificar progreso de reading y aplicar efectos visuales
            checkReadingProgressAndUnlockTopics()
            applyReadingVisualLockEffects()

            setupReadingClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing reading menu: " + e.message, Toast.LENGTH_LONG)
                .show()
            e.printStackTrace()
        }

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
                    refreshReadingVisualEffects()
                })
            }
        }

        applyBlock(map_blocked_2Reading!!, "DEP_MAP2_ACT1", "DEP_MAP2_ACT2")
        applyBlock(map_blocked_3Reading!!, "DEP_MAP3_ACT1", "DEP_MAP3_ACT2")
        applyBlock(map_blocked_4Reading!!, "DEP_MAP4_ACT1", "DEP_MAP4_ACT2")
        applyBlock(map_blocked_5Reading!!, "DEP_MAP5_ACT1", "DEP_MAP5_ACT2")

        //Return Menu
        eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)
        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })

        // MÉTODO TEMPORAL PARA TESTING - Agregar long click al botón de retorno para marcar temas como completados
        // TODO: Remover esto cuando se implementen todas las actividades de reading
        eBtnReturnMenu!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            markAllReadingTopicsForTesting()
            true
        })
    }

    // Verificar progreso de reading y desbloquear temas automáticamente
    private fun checkReadingProgressAndUnlockTopics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Verificar si se completó el reading de nivel A1.1 (70% o más)
        val passedReadingA1_1 = prefs.getBoolean("PASSED_READING_LEVEL_A1_1", false)
        val scoreReadingA1_1 = prefs.getInt("SCORE_READING_LEVEL_A1_1", 0)

        if (passedReadingA1_1 || scoreReadingA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del reading A1.1
            unlockRemainingReadingA1_1Topics()
            Log.d(TAG, "Temas restantes de Reading A1.1 desbloqueados automáticamente")
        }
    }

    // Desbloquear solo los temas restantes del reading nivel A1.1
    private fun unlockRemainingReadingA1_1Topics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()


        // Solo desbloquear si no están ya desbloqueados - Usamos claves específicas para reading
        if (!prefs.getBoolean("PASSED_READING_ALPHABET", false)) {
            editor.putBoolean("PASSED_READING_ALPHABET", true)
            Log.d(TAG, "Reading ALPHABET desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_READING_NUMBERS", false)) {
            editor.putBoolean("PASSED_READING_NUMBERS", true)
            Log.d(TAG, "Reading NUMBERS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_READING_COLORS", false)) {
            editor.putBoolean("PASSED_READING_COLORS", true)
            Log.d(TAG, "Reading COLORS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_READING_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_READING_PERSONAL_PRONOUNS", true)
            Log.d(TAG, "Reading PERSONAL PRONOUNS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", true)
            Log.d(TAG, "Reading POSSESSIVE ADJECTIVES desbloqueado automáticamente")
        }

        /*if (!prefs.getBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", false)) {
            editor.putBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", true);
            Log.d(TAG, "Reading PREPOSITIONS OF PLACE desbloqueado automáticamente");
        }
        if (!prefs.getBoolean("PASSED_READING_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_READING_ADJECTIVES", true);
            Log.d(TAG, "Reading ADJECTIVES desbloqueado automáticamente");
        }*/
        editor.apply()
    }

    private fun setupReadingClickListeners() {
        // READING - ALPHABET (antes Translation) - Requiere completar Speaking Alphabet
        if (imgReadingTranslation != null) {
            imgReadingTranslation!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent =
                        Intent(this@MenuReadingActivity, ImageIdentificationActivity::class.java)
                    intent.putExtra("TOPIC", "ALPHABET")
                    intent.putExtra("LEVEL", "A1.1")
                    intent.putExtra("SOURCE_MAP", "READING")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuReadingActivity,
                        "Error opening reading alphabet: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // READING - NUMBERS (antes Comprehension) - Requiere completar Speaking Numbers
        if (imgReadingComprehension != null) {
            imgReadingComprehension!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_READING_ALPHABET", false)
                if (unlocked) {
                    try {
                        val intent = Intent(
                            this@MenuReadingActivity,
                            ImageIdentificationAudioActivity::class.java
                        )
                        intent.putExtra("TOPIC", "NUMBERS")
                        intent.putExtra("LEVEL", "A1.1")
                        intent.putExtra("SOURCE_MAP", "READING")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading numbers: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar NUMBERS speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // READING - COLORS (antes Vocabulary) - Requiere completar Speaking Colors
        if (imgReadingVocabulary != null) {
            imgReadingVocabulary!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_READING_NUMBERS", false)
                if (unlocked) {
                    try {
                        val intent = Intent(
                            this@MenuReadingActivity,
                            ImageIdentificationActivity::class.java
                        )
                        intent.putExtra("TOPIC", "COLORS")
                        intent.putExtra("LEVEL", "A1.1")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading colors: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar COLORS speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // READING - PERSONAL PRONOUNS (antes Grammar) - Requiere completar Speaking Personal Pronouns
        if (imgReadingGrammar != null) {
            imgReadingGrammar!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_READING_COLORS", false)
                if (unlocked) {
                    try {
                        val intent =
                            Intent(this@MenuReadingActivity, TranslationReadingActivity::class.java)
                        intent.putExtra("TOPIC", "PERSONAL PRONOUNS")
                        intent.putExtra("LEVEL", "A1.1")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading personal pronouns: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar PERSONAL PRONOUNS speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // READING - POSSESSIVE ADJECTIVES (antes Stories) - Requiere completar Speaking Possessive Adjectives
        if (imgReadingStories != null) {
            imgReadingStories!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean(
                    "PASSED_READING_PERSONAL_PRONOUNS",
                    false
                )
                if (unlocked) {
                    try {
                        val intent =
                            Intent(this@MenuReadingActivity, TranslationReadingActivity::class.java)
                        intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                        intent.putExtra("LEVEL", "A1.1")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading possessive adjectives: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar POSSESSIVE ADJECTIVES speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // READING - PREPOSITIONS OF PLACE (antes Advanced) - Requiere completar Speaking Prepositions
        if (imgReadingAdvanced != null) {
            imgReadingAdvanced!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean(
                    "PASSED_READING_POSSESSIVE_ADJECTIVES",
                    false
                )
                if (unlocked) {
                    try {
                        val intent =
                            Intent(this@MenuReadingActivity, TranslationReadingActivity::class.java)
                        intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE")
                        intent.putExtra("LEVEL", "A1.1")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading prepositions: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar PREPOSITIONS speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // READING - ADJECTIVES (antes Practice) - Requiere completar Speaking Adjectives
        if (imgReadingPractice != null) {
            imgReadingPractice!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean(
                    "PASSED_READING_PREPOSITIONS_OF_PLACE",
                    false
                )
                if (unlocked) {
                    try {
                        val intent =
                            Intent(this@MenuReadingActivity, TranslationReadingActivity::class.java)
                        intent.putExtra("TOPIC", "ADJECTIVES")
                        intent.putExtra("LEVEL", "A1.1")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MenuReadingActivity,
                            "Error opening reading adjectives: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "📖 Para practicar ADJECTIVES speaking primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // Botón Start Reading - Despliega los primeros elementos
        if (eButtonStartReading != null) {
            eButtonStartReading!!.setOnClickListener(View.OnClickListener { v: View? ->
                startReadingExpanded = !startReadingExpanded
                if (startReadingExpanded) {
                    eButtonStartReading!!.setImageResource(R.drawable.start)
                    fadeInView(imgReadingTranslation)
                    fadeInView(imgReadingComprehension)
                    fadeInView(imgReadingVocabulary)
                    fadeInView(imgReadingGrammar)
                    fadeInView(imgReadingStories)
                } else {
                    eButtonStartReading!!.setImageResource(R.drawable.start)
                    fadeOutView(imgReadingTranslation)
                    fadeOutView(imgReadingComprehension)
                    fadeOutView(imgReadingVocabulary)
                    fadeOutView(imgReadingGrammar)
                    fadeOutView(imgReadingStories)
                }
            })
        }

        // Map Icon 2 Reading - Despliega elementos avanzados
        if (imgIcMap2Reading != null) {
            imgIcMap2Reading!!.setOnClickListener(View.OnClickListener { v: View? ->
                icMap2ReadingExpanded = !icMap2ReadingExpanded
                if (icMap2ReadingExpanded) {
                    imgIcMap2Reading!!.setImageResource(R.drawable.icon_map3)
                    fadeInView(imgReadingAdvanced)
                    fadeInView(imgReadingPractice)
                } else {
                    imgIcMap2Reading!!.setImageResource(R.drawable.icon_map3)
                    fadeOutView(imgReadingAdvanced)
                    fadeOutView(imgReadingPractice)
                }
            })
        }

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuReadingActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuReadingActivity,
                        "Error opening profile: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (homeButton != null) {
            homeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuReadingActivity, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuReadingActivity,
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
                    val intent = Intent(this@MenuReadingActivity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuReadingActivity,
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
                        Intent(this@MenuReadingActivity, ReadingHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuReadingActivity,
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

    private fun fadeInView(view: View?) {
        if (view == null) return

        view.setVisibility(View.VISIBLE)
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.setDuration(500)
        view.startAnimation(fadeIn)
    }

    //Método para aplicar bloqueo
    private fun applyBlock(candado: ImageView, vararg dependencias: String?) {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        var blocked = !this.isUnlockAllEnabled
        if (!blocked) {
            candado.setVisibility(View.GONE)
            return
        }
        for (dep in dependencias) {
            if (prefs.getBoolean(dep, false)) {
                blocked = false
                break
            }
        }
        candado.setVisibility(if (blocked) View.VISIBLE else View.GONE)
    }

    //Return Menú
    private fun ReturnMenu() {
        // Limpiar seguimiento de módulo
        ModuleTracker.clearLastModule(this)
        startActivity(Intent(this@MenuReadingActivity, MainActivity::class.java))
        Toast.makeText(
            this@MenuReadingActivity,
            "Has retornado al menú principal correctamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Aplicar efectos visuales de bloqueo para reading
    private fun applyReadingVisualLockEffects() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Verificar progreso de speaking y aplicar efectos a reading
        val passedSpeakingAlphabet = prefs.getBoolean("PASSED_READING_ALPHABET", false)
        val passedSpeakingNumbers = prefs.getBoolean("PASSED_READING_NUMBERS", false)
        val passedSpeakingColors = prefs.getBoolean("PASSED_READING_COLORS", false)
        val passedSpeakingPronouns = prefs.getBoolean("PASSED_READING_PERSONAL_PRONOUNS", false)
        val passedSpeakingPossessive =
            prefs.getBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", false)
        val passedSpeakingPrepositions =
            prefs.getBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", false)
        val passedSpeakingAdjectives = prefs.getBoolean("PASSED_READING_ADJECTIVES", false)


        // Aplicar efectos de bloqueo basados en progreso de speaking
        val unlockedAll = this.isUnlockAllEnabled
        applyReadingLockEffect(
            imgReadingComprehension,
            unlockedAll || passedSpeakingAlphabet
        ) // 2do tema
        applyReadingLockEffect(
            imgReadingVocabulary,
            unlockedAll || passedSpeakingNumbers
        ) // 3er tema
        applyReadingLockEffect(imgReadingGrammar, unlockedAll || passedSpeakingColors) // 4to tema
        applyReadingLockEffect(imgReadingStories, unlockedAll || passedSpeakingPronouns) // 5to tema
        applyReadingLockEffect(
            imgReadingAdvanced,
            unlockedAll || passedSpeakingPossessive
        ) // 6to tema
        applyReadingLockEffect(
            imgReadingPractice,
            unlockedAll || passedSpeakingPrepositions
        ) // 7mo tema

        Log.d(TAG, "Efectos visuales de bloqueo aplicados a reading")
        Log.d(
            TAG,
            "Translation: " + passedSpeakingAlphabet + ", Comprehension: " + passedSpeakingNumbers +
                    ", Vocabulary: " + passedSpeakingColors + ", Grammar: " + passedSpeakingPronouns +
                    ", Stories: " + passedSpeakingPossessive + ", Advanced: " + passedSpeakingPrepositions +
                    ", Practice: " + passedSpeakingAdjectives
        )
    }

    private fun applyReadingLockEffect(imageView: ImageView?, isUnlocked: Boolean) {
        if (imageView == null) return

        if (isUnlocked) {
            // Elemento desbloqueado - apariencia normal
            imageView.clearColorFilter()
            imageView.setAlpha(1.0f)
            imageView.setElevation(0f)
            imageView.setTag(R.id.element_state, "enabled")
        } else {
            // Elemento bloqueado - aplicar efecto gris y transparente
            val grayFilter: ColorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            )
            imageView.setColorFilter(grayFilter)
            imageView.setAlpha(0.35f)
            imageView.setElevation(2f) // Elevación sutil para efecto de sombra
            imageView.setTag(R.id.element_state, "locked")
        }
    }

    private fun refreshReadingVisualEffects() {
        applyReadingVisualLockEffects()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar efectos visuales al regresar a la actividad
        refreshReadingVisualEffects()
    }

    // MÉTODO TEMPORAL PARA TESTING - Marcar todos los temas de reading como completados
    private fun markAllReadingTopicsForTesting() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putBoolean("PASSED_READING_ALPHABET", true)
        editor.putBoolean("PASSED_READING_NUMBERS", true)
        editor.putBoolean("PASSED_READING_COLORS", true)
        editor.putBoolean("PASSED_READING_PERSONAL_PRONOUNS", true)
        editor.putBoolean("PASSED_READING_POSSESSIVE_ADJECTIVES", true)
        editor.putBoolean("PASSED_READING_PREPOSITIONS_OF_PLACE", true)
        editor.putBoolean("PASSED_READING_ADJECTIVES", true)

        editor.apply()
        Toast.makeText(
            this,
            "Todos los temas de Reading marcados como completados para testing.",
            Toast.LENGTH_SHORT
        ).show()
        refreshReadingVisualEffects() // Actualizar efectos visuales
    }

    companion object {
        private const val TAG = "MenuReadingActivity"
    }
}