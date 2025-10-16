package com.example.speak

import android.content.Intent
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.speak.pronunciation.PronunciationActivity

class MenuSpeakingActivity : AppCompatActivity() {
    //Declaramos las variables Start Speaking
    private var startSpeakingExpanded = false
    private var eButtonStartSpeaking: ImageView? = null
    private var imgSpeakingABC: ImageView? = null
    private var imgSpeakingNumbers: ImageView? = null
    private var imgSpeakingColors: ImageView? = null
    private var imgSpeakingPronouns: ImageView? = null
    private var imgSpeakingPossessive: ImageView? = null

    //Declaramos las variables icMap2 Speaking
    private var icMap2SpeakingExpanded = false
    private var imgIcMap2Speaking: ImageView? = null
    private var imgSpeakingPrepos: ImageView? = null
    private var imgSpeakingAdjectives: ImageView? = null

    // Image blocked
    private var map_blocked_2Speaking: ImageView? = null
    private var map_blocked_3Speaking: ImageView? = null
    private var map_blocked_4Speaking: ImageView? = null
    private var map_blocked_5Speaking: ImageView? = null

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var pronunHistoryMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null
    private var homeButton: ImageView? = null

    //Return Menú
    private var eBtnReturnMenu: LinearLayout? = null

    // Modo libre
    private var freeRoamMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_speaking)

        try {
            //Inicializamos las variables Start Speaking
            eButtonStartSpeaking = findViewById<ImageView?>(R.id.eButtonStartSpeaking)
            imgSpeakingABC = findViewById<ImageView?>(R.id.imgSpeakingABC)
            imgSpeakingNumbers = findViewById<ImageView?>(R.id.imgSpeakingNumbers)
            imgSpeakingColors = findViewById<ImageView?>(R.id.imgSpeakingColors)
            imgSpeakingPronouns = findViewById<ImageView?>(R.id.imgSpeakingPronouns)
            imgSpeakingPossessive = findViewById<ImageView?>(R.id.imgSpeakingPossessive)

            //Inicializamos las variables imgIcMap2 Speaking
            imgIcMap2Speaking = findViewById<ImageView?>(R.id.imgIcMap2Speaking)
            imgSpeakingPrepos = findViewById<ImageView?>(R.id.imgSpeakingPrepos)
            imgSpeakingAdjectives = findViewById<ImageView?>(R.id.imgSpeakingAdjectives)

            //Blocked
            map_blocked_2Speaking = findViewById<ImageView>(R.id.map_blocked_2Speaking)
            map_blocked_3Speaking = findViewById<ImageView>(R.id.map_blocked_3Speaking)
            map_blocked_4Speaking = findViewById<ImageView>(R.id.map_blocked_4Speaking)
            map_blocked_5Speaking = findViewById<ImageView>(R.id.map_blocked_5Speaking)

            //Declaramos las variables Menu
            birdMenu = findViewById<ImageView?>(R.id.imgBirdMenu)
            quizMenu = findViewById<ImageView?>(R.id.imgQuizMenu)
            pronunHistoryMenu = findViewById<ImageView?>(R.id.imgPronunHistoryMenu)
            eButtonProfile = findViewById<ImageView?>(R.id.btnProfile)
            homeButton = findViewById<ImageView?>(R.id.homeButton)

            // Verificar progreso de speaking y aplicar efectos visuales
            checkSpeakingProgressAndUnlockTopics()
            applySpeakingVisualLockEffects()

            setupSpeakingClickListeners()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error initializing speaking menu: " + e.message,
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }

        applyBlock(map_blocked_2Speaking!!, "DEP_MAP2_ACT1", "DEP_MAP2_ACT2")
        applyBlock(map_blocked_3Speaking!!, "DEP_MAP3_ACT1", "DEP_MAP3_ACT2")
        applyBlock(map_blocked_4Speaking!!, "DEP_MAP4_ACT1", "DEP_MAP4_ACT2")
        applyBlock(map_blocked_5Speaking!!, "DEP_MAP5_ACT1", "DEP_MAP5_ACT2")

        // Leer modo libre desde Intent o SharedPreferences
        freeRoamMode = getIntent().getBooleanExtra("FREE_ROAM", false)
        if (!freeRoamMode) {
            val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
            freeRoamMode = prefs.getBoolean("FREE_ROAM", false)
        }
        if (freeRoamMode) {
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
                    refreshSpeakingVisualEffects()
                })
            }
        }

        //Return Menu
        eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)
        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })
    }

    // Verificar progreso de speaking y desbloquear temas automáticamente
    private fun checkSpeakingProgressAndUnlockTopics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Verificar si se completó el speaking de nivel A1.1 (70% o más)
        val passedSpeakingA1_1 = prefs.getBoolean("PASSED_SPEAKING_LEVEL_A1_1", false)
        val scoreSpeakingA1_1 = prefs.getInt("SCORE_SPEAKING_LEVEL_A1_1", 0)

        if (passedSpeakingA1_1 || scoreSpeakingA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del speaking A1.1
            unlockRemainingSpeakingA1_1Topics()
            Log.d(TAG, "Temas restantes de Speaking A1.1 desbloqueados automáticamente")
        }
    }

    // Desbloquear solo los temas restantes del speaking nivel A1.1
    private fun unlockRemainingSpeakingA1_1Topics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()


        // Solo desbloquear si no están ya desbloqueados - Usamos claves específicas para speaking
        if (!prefs.getBoolean("PASSED_PRON_ALPHABET", false)) {
            editor.putBoolean("PASSED_PRON_ALPHABET", true)
            Log.d(TAG, "Speaking ALPHABET desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PRON_NUMBERS", false)) {
            editor.putBoolean("PASSED_PRON_NUMBERS", true)
            Log.d(TAG, "Speaking NUMBERS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PRON_COLORS", false)) {
            editor.putBoolean("PASSED_PRON_COLORS", true)
            Log.d(TAG, "Speaking COLORS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_PRON_PERSONAL_PRONOUNS", true)
            Log.d(TAG, "Speaking PERSONAL PRONOUNS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", true)
            Log.d(TAG, "Speaking POSSESSIVE ADJECTIVES desbloqueado automáticamente")
        }
        // NO desbloquear PREPOSITIONS automáticamente - requiere completar listening imgIcMap2
        /*if (!prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false)) {
            editor.putBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", true);
            Log.d(TAG, "Speaking PREPOSITIONS OF PLACE desbloqueado automáticamente");
        }*/
        if (!prefs.getBoolean("PASSED_PRON_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_PRON_ADJECTIVES", true)
            Log.d(TAG, "Speaking ADJECTIVES desbloqueado automáticamente")
        }

        editor.apply()
    }

    private fun setupSpeakingClickListeners() {
        // ALPHABET SPEAKING - Siempre disponible
        if (imgSpeakingABC != null) {
            imgSpeakingABC!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuSpeakingActivity, PronAlphabetActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuSpeakingActivity,
                        "Error opening speaking activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // NUMBERS SPEAKING - Requiere completar Alphabet Speaking
        if (imgSpeakingNumbers != null) {
            imgSpeakingNumbers!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val passedPronAlphabet = prefs.getBoolean("PASSED_PRON_ALPHABET", false)
                if (passedPronAlphabet) {
                    val intent = Intent(this@MenuSpeakingActivity, PronNumberActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar NUMBERS, completa ALPHABET pronunciation primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // COLORS SPEAKING - Requiere completar Numbers Speaking
        if (imgSpeakingColors != null) {
            imgSpeakingColors!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val passedPronNumbers = prefs.getBoolean("PASSED_PRON_NUMBERS", false)
                if (passedPronNumbers) {
                    val intent = Intent(this@MenuSpeakingActivity, PronColorActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar COLORS, completa NUMBERS pronunciation primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // PERSONAL PRONOUNS SPEAKING - Requiere completar Colors Speaking
        if (imgSpeakingPronouns != null) {
            imgSpeakingPronouns!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val passedPronColors = prefs.getBoolean("PASSED_PRON_COLORS", false)
                if (passedPronColors) {
                    val intent = Intent(this@MenuSpeakingActivity, PronPersProActivity::class.java)
                    intent.putExtra("TOPIC", "PERSONAL PRONOUNS")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar PERSONAL PRONOUNS, completa COLORS pronunciation primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // POSSESSIVE ADJECTIVES SPEAKING - Requiere completar Personal Pronouns Speaking
        if (imgSpeakingPossessive != null) {
            imgSpeakingPossessive!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val passedPronPronouns = prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)
                if (passedPronPronouns) {
                    val intent =
                        Intent(this@MenuSpeakingActivity, PronPosseAdjectActivity::class.java)
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar POSSESSIVE ADJECTIVES, completa PERSONAL PRONOUNS pronunciation primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // PREPOSITIONS SPEAKING - Requiere completar Possessive Adjectives Speaking
        if (imgSpeakingPrepos != null) {
            imgSpeakingPrepos!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                //boolean passedPronPossessive = prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false);
                //if (passedPronPossessive) {
                val passedListeningIcMap2 =
                    prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false) &&
                            prefs.getBoolean("PASSED_ADJECTIVES", false) &&
                            prefs.getBoolean("PASSED_GUESS_PICTURE", false)
                if (passedListeningIcMap2) {
                    val intent =
                        Intent(this@MenuSpeakingActivity, PronunciationActivity::class.java)
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    //Toast.makeText(this, "🗣️ Para practicar PREPOSITIONS, completa POSSESSIVE ADJECTIVES pronunciation primero.", Toast.LENGTH_LONG).show();
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar PREPOSITIONS, completa todo el módulo avanzado de listening primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // ADJECTIVES SPEAKING - Requiere completar Prepositions Speaking
        if (imgSpeakingAdjectives != null) {
            imgSpeakingAdjectives!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val passedPronPrepositions =
                    prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false)
                if (passedPronPrepositions) {
                    val intent =
                        Intent(this@MenuSpeakingActivity, PronunciationActivity::class.java)
                    intent.putExtra("TOPIC", "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "🗣️ Para practicar ADJECTIVES, completa PREPOSITIONS pronunciation primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // Botón Start Speaking - Despliega los primeros elementos
        if (eButtonStartSpeaking != null) {
            eButtonStartSpeaking!!.setOnClickListener(View.OnClickListener { v: View? ->
                startSpeakingExpanded = !startSpeakingExpanded
                if (startSpeakingExpanded) {
                    eButtonStartSpeaking!!.setImageResource(R.drawable.start)
                    fadeInView(imgSpeakingABC)
                    fadeInView(imgSpeakingNumbers)
                    fadeInView(imgSpeakingColors)
                    fadeInView(imgSpeakingPronouns)
                    fadeInView(imgSpeakingPossessive)
                } else {
                    eButtonStartSpeaking!!.setImageResource(R.drawable.start)
                    fadeInView(imgSpeakingABC)
                    fadeInView(imgSpeakingNumbers)
                    fadeInView(imgSpeakingColors)
                    fadeOutView(imgSpeakingPronouns)
                    fadeOutView(imgSpeakingPossessive)
                }
            })
        }

        // Map Icon 2 Speaking - Despliega elementos avanzados
        if (imgIcMap2Speaking != null) {
            imgIcMap2Speaking!!.setOnClickListener(View.OnClickListener { v: View? ->
                icMap2SpeakingExpanded = !icMap2SpeakingExpanded
                if (icMap2SpeakingExpanded) {
                    imgIcMap2Speaking!!.setImageResource(R.drawable.icon_map2)
                    fadeInView(imgSpeakingPrepos)
                    fadeInView(imgSpeakingAdjectives)
                } else {
                    imgIcMap2Speaking!!.setImageResource(R.drawable.icon_map2)
                    fadeOutView(imgSpeakingPrepos)
                    fadeOutView(imgSpeakingAdjectives)
                }
            })
        }

        // Profile button
        if (eButtonProfile != null) {
            eButtonProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuSpeakingActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuSpeakingActivity,
                        "Error opening profile: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (homeButton != null) {
            homeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuSpeakingActivity, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuSpeakingActivity,
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
                    val intent = Intent(this@MenuSpeakingActivity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuSpeakingActivity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Pronunciation History button
        if (pronunHistoryMenu != null) {
            pronunHistoryMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent =
                        Intent(this@MenuSpeakingActivity, PronunciationHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuSpeakingActivity,
                        "Error opening pronunciation history: " + e.message,
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
                    fadeInView(pronunHistoryMenu)
                } else {
                    birdMenu!!.setImageResource(R.drawable.bird0_menu)
                    fadeInView(quizMenu)
                    fadeOutView(pronunHistoryMenu)
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

    //Return Menú
    private fun ReturnMenu() {
        ModuleTracker.clearLastModule(this)
        startActivity(Intent(this@MenuSpeakingActivity, MainActivity::class.java))
        Toast.makeText(
            this@MenuSpeakingActivity,
            "Has retornado al menú principal correctamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Aplicar efectos visuales de bloqueo para speaking
    private fun applySpeakingVisualLockEffects() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Verificar progreso y aplicar efectos
        val passedPronAlphabet = prefs.getBoolean("PASSED_PRON_ALPHABET", false)
        val passedPronNumbers = prefs.getBoolean("PASSED_PRON_NUMBERS", false)
        val passedPronColors = prefs.getBoolean("PASSED_PRON_COLORS", false)
        val passedPronPronouns = prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false)
        val passedPronPossessive = prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false)
        val passedPronPrepositions = prefs.getBoolean("PASSED_PRON_PREPOSITIONS_OF_PLACE", false)


        // Aplicar efectos de bloqueo
        applySpeakingLockEffect(imgSpeakingNumbers, passedPronAlphabet)
        applySpeakingLockEffect(imgSpeakingColors, passedPronNumbers)
        applySpeakingLockEffect(imgSpeakingPronouns, passedPronColors)
        applySpeakingLockEffect(imgSpeakingPossessive, passedPronPronouns)
        //applySpeakingLockEffect(imgSpeakingPrepos, passedPronPossessive);
        // PREPOSITIONS en speaking requiere completar todo el módulo imgIcMap2 de listening
        val passedListeningIcMap2 = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false) &&
                prefs.getBoolean("PASSED_ADJECTIVES", false) &&
                prefs.getBoolean("PASSED_GUESS_PICTURE", false)
        applySpeakingLockEffect(imgSpeakingPrepos, passedListeningIcMap2)
        applySpeakingLockEffect(imgSpeakingAdjectives, passedPronPrepositions)
    }

    private fun applySpeakingLockEffect(imageView: ImageView?, isUnlocked: Boolean) {
        if (imageView == null) return

        if (!isUnlocked) {
            // Aplicar filtro gris y reducir opacidad
            val grayFilter: ColorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.locked_gray),
                PorterDuff.Mode.MULTIPLY
            )
            imageView.setColorFilter(grayFilter)
            imageView.setAlpha(0.5f)
        } else {
            // Remover filtros si está desbloqueado
            imageView.setColorFilter(null)
            imageView.setAlpha(1.0f)
        }
    }

    private fun refreshSpeakingVisualEffects() {
        applySpeakingVisualLockEffects()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar efectos visuales al regresar a la actividad
        refreshSpeakingVisualEffects()
    }

    //Método para aplicar bloqueo
    private fun applyBlock(candado: ImageView, vararg dependencias: String?) {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        var blocked = true
        for (dep in dependencias) {
            if (prefs.getBoolean(dep, false)) {
                blocked = false
                break
            }
        }
        candado.setVisibility(if (blocked) View.VISIBLE else View.GONE)
    }

    companion object {
        private const val TAG = "MenuSpeakingActivity"
    }
}