package com.example.speak

import android.content.Intent
import android.content.SharedPreferences
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

class MenuA1Activity : AppCompatActivity() {
    //Declaramos las variables Start
    private var startExpanded = false
    private var eButtonStart: ImageView? = null
    private var imgABC: ImageView? = null
    private var imgNumbers: ImageView? = null
    private var imgColors: ImageView? = null
    private var imgPronouns: ImageView? = null
    private var imgPossesive: ImageView? = null

    //Declaramos las variables icMap2
    private var icMap2Expanded = false
    private var imgIcMap2: ImageView? = null
    private var imgPrepos: ImageView? = null
    private var imgAdject: ImageView? = null

    // Botones de identificación de imágenes
    private var btnImageIdentification: ImageView? = null
    private var btnImageIdentificationAudio: ImageView? = null

    // Image blocked
    private var map_blocked_icMap2: ImageView? = null
    private var map_blocked_icMap3: ImageView? = null
    private var map_blocked_icMap4: ImageView? = null
    private var map_blocked_icMap5: ImageView? = null

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var pronunMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null
    private var homeButton: ImageView? = null

    //Return Menú
    private var eBtnReturnMenu: LinearLayout? = null

    // En modo libre se ignoran dependencias; por defecto se respeta la secuencia
    // Modo de navegación libre (sin dependencias)
    private var isUnlockAllEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_a1)

        try {
            //Inicializamos las variables Start
            eButtonStart = findViewById<ImageView?>(R.id.eButtonStart)
            imgABC = findViewById<ImageView?>(R.id.imgABC)
            imgNumbers = findViewById<ImageView?>(R.id.imgNumbers)
            imgColors = findViewById<ImageView?>(R.id.imgColors)
            imgPronouns = findViewById<ImageView?>(R.id.imgPronouns)
            imgPossesive = findViewById<ImageView?>(R.id.imgPossesive)

            //Inicializamos las variables imgIcMap2
            imgIcMap2 = findViewById<ImageView?>(R.id.imgIcMap2)
            imgPrepos = findViewById<ImageView?>(R.id.imgPrepos)
            imgAdject = findViewById<ImageView?>(R.id.imgAdject)
            //Inicializamos los botones de identificación de imágenes
            btnImageIdentification = findViewById<ImageView?>(R.id.btnImageIdentification)
            btnImageIdentificationAudio = findViewById<ImageView?>(R.id.btnImageIdentificationAudio)

            //Blocked
            map_blocked_icMap2 = findViewById<ImageView>(R.id.map_blocked_icMap2)
            map_blocked_icMap3 = findViewById<ImageView>(R.id.map_blocked_icMap3)
            map_blocked_icMap4 = findViewById<ImageView>(R.id.map_blocked_icMap4)
            map_blocked_icMap5 = findViewById<ImageView>(R.id.map_blocked_icMap5)

            //Declaramos las variables Menu
            birdMenu = findViewById<ImageView?>(R.id.imgBirdMenu)
            quizMenu = findViewById<ImageView?>(R.id.imgQuizMenu)
            pronunMenu = findViewById<ImageView?>(R.id.imgPronunMenu)
            eButtonProfile = findViewById<ImageView?>(R.id.btnProfile)
            homeButton = findViewById<ImageView?>(R.id.homeButton)


            // Verificar progreso por niveles y desbloquear temas
            checkLevelProgressAndUnlockTopics()


            // Aplicar efectos visuales de bloqueo
            applyVisualLockEffects()

            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing views: " + e.message, Toast.LENGTH_LONG).show()
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
                    this.isUnlockAllEnabled = false // Actualizar la variable local
                    refreshVisualEffects()
                })
            }
        }

        applyBlock(map_blocked_icMap2!!, "DEP_MAP2_ACT1", "DEP_MAP2_ACT2")
        applyBlock(map_blocked_icMap3!!, "DEP_MAP3_ACT1", "DEP_MAP3_ACT2")
        applyBlock(map_blocked_icMap4!!, "DEP_MAP4_ACT1", "DEP_MAP4_ACT2")
        applyBlock(map_blocked_icMap5!!, "DEP_MAP5_ACT1", "DEP_MAP5_ACT2")

        //Return Menu
        eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)
        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })
    }

    // Verificar progreso por niveles y desbloquear temas automáticamente
    private fun checkLevelProgressAndUnlockTopics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)


        // Verificar si se completó el nivel A1.1 (70% o más en el quiz)
        val passedA1_1 = prefs.getBoolean("PASSED_LEVEL_A1_1", false)
        val scoreA1_1 = prefs.getInt("SCORE_LEVEL_A1_1", 0)

        if (passedA1_1 || scoreA1_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A1.1
            unlockRemainingA1_1Topics()
            Log.d("MenuA1Activity", "Temas restantes de A1.1 desbloqueados automáticamente")
        }


        // Verificar si se completó el nivel A1.2
        val passedA1_2 = prefs.getBoolean("PASSED_LEVEL_A1_2", false)
        val scoreA1_2 = prefs.getInt("SCORE_LEVEL_A1_2", 0)

        if (passedA1_2 || scoreA1_2 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A1.2
            unlockRemainingA1_2Topics()
            Log.d("MenuA1Activity", "Temas restantes de A1.2 desbloqueados automáticamente")
        }


        // Verificar si se completó el nivel A2.1
        val passedA2_1 = prefs.getBoolean("PASSED_LEVEL_A2_1", false)
        val scoreA2_1 = prefs.getInt("SCORE_LEVEL_A2_1", 0)

        if (passedA2_1 || scoreA2_1 >= 70) {
            // Desbloquear solo los temas que aún no están desbloqueados del nivel A2.1
            unlockRemainingA2_1Topics()
            Log.d("MenuA1Activity", "Temas restantes de A2.1 desbloqueados automáticamente")
        }
    }

    // Desbloquear solo los temas restantes del nivel A1.1 (respeta progresión)
    private fun unlockRemainingA1_1Topics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()


        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_ALPHABET", false)) {
            editor.putBoolean("PASSED_ALPHABET", true)
            Log.d("MenuA1Activity", "ALPHABET desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_NUMBERS", false)) {
            editor.putBoolean("PASSED_NUMBERS", true)
            Log.d("MenuA1Activity", "NUMBERS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_COLORS", false)) {
            editor.putBoolean("PASSED_COLORS", true)
            Log.d("MenuA1Activity", "COLORS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)) {
            editor.putBoolean("PASSED_PERSONAL_PRONOUNS", true)
            Log.d("MenuA1Activity", "PERSONAL PRONOUNS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false)) {
            editor.putBoolean("PASSED_POSSESSIVE_ADJECTIVES", true)
            Log.d("MenuA1Activity", "POSSESSIVE ADJECTIVES desbloqueado automáticamente")
        }

        // NO desbloquear PREPOSITIONS automáticamente - requiere completar writing imgIcMap2
// if (!prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false)) {
//     editor.putBoolean("PASSED_PREPOSITIONS_OF_PLACE", true);
// }

        // No se desbloquean temas avanzados (Prepositions, Adjectives, Guess Picture...) aquí; 
        // esos se habilitarán solo después de completar el Start del siguiente mundo.
        editor.apply()
    }

    // Desbloquear solo los temas restantes del nivel A1.2 (respeta progresión)
    private fun unlockRemainingA1_2Topics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()


        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_VERBS_TO_BE", false)) {
            editor.putBoolean("PASSED_VERBS_TO_BE", true)
            Log.d("MenuA1Activity", "VERBS TO BE desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_PRESENT_SIMPLE", false)) {
            editor.putBoolean("PASSED_PRESENT_SIMPLE", true)
            Log.d("MenuA1Activity", "PRESENT SIMPLE desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_DAILY_ROUTINES", false)) {
            editor.putBoolean("PASSED_DAILY_ROUTINES", true)
            Log.d("MenuA1Activity", "DAILY ROUTINES desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_FOOD_AND_DRINKS", false)) {
            editor.putBoolean("PASSED_FOOD_AND_DRINKS", true)
            Log.d("MenuA1Activity", "FOOD AND DRINKS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_FAMILY_MEMBERS", false)) {
            editor.putBoolean("PASSED_FAMILY_MEMBERS", true)
            Log.d("MenuA1Activity", "FAMILY MEMBERS desbloqueado automáticamente")
        }

        editor.apply()
    }

    // Desbloquear solo los temas restantes del nivel A2.1 (respeta progresión)
    private fun unlockRemainingA2_1Topics() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()


        // Solo desbloquear si no están ya desbloqueados
        if (!prefs.getBoolean("PASSED_PAST_SIMPLE", false)) {
            editor.putBoolean("PASSED_PAST_SIMPLE", true)
            Log.d("MenuA1Activity", "PAST SIMPLE desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_IRREGULAR_VERBS", false)) {
            editor.putBoolean("PASSED_IRREGULAR_VERBS", true)
            Log.d("MenuA1Activity", "IRREGULAR VERBS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_HOBBIES_AND_INTERESTS", false)) {
            editor.putBoolean("PASSED_HOBBIES_AND_INTERESTS", true)
            Log.d("MenuA1Activity", "HOBBIES AND INTERESTS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_WEATHER_AND_SEASONS", false)) {
            editor.putBoolean("PASSED_WEATHER_AND_SEASONS", true)
            Log.d("MenuA1Activity", "WEATHER AND SEASONS desbloqueado automáticamente")
        }
        if (!prefs.getBoolean("PASSED_TRAVEL_AND_TRANSPORTATION", false)) {
            editor.putBoolean("PASSED_TRAVEL_AND_TRANSPORTATION", true)
            Log.d("MenuA1Activity", "TRAVEL AND TRANSPORTATION desbloqueado automáticamente")
        }

        editor.apply()
    }

    private fun setupClickListeners() {
        if (imgABC != null) {
            imgABC!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuA1Activity, AlphabetActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuA1Activity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (imgNumbers != null) {
            imgNumbers!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean("PASSED_ALPHABET", false)
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, NumberActivity::class.java)
                    intent.putExtra("TOPIC", "NUMBERS")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión NUMBERS, Completa ALPHABET.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (imgColors != null) {
            imgColors!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean("PASSED_NUMBERS", false)
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, ColorActivity::class.java)
                    intent.putExtra("TOPIC", "COLORS")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión COLORS, Completa NUMBERS.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (imgPronouns != null) {
            imgPronouns!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean("PASSED_COLORS", false)
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, PronounsActivity::class.java)
                    intent.putExtra("TOPIC", "PERSONAL PRONOUNS")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión PERSONAL PRONOUNS, Completa COLORS.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (imgPossesive != null) {
            imgPossesive!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, PossessiveAdjectActivity::class.java)
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión POSSESSIVE ADJECTIVES Completa PERSONAL PRONOUNS.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (eButtonStart != null) {
            eButtonStart!!.setOnClickListener(View.OnClickListener { v: View? ->
                startExpanded = !startExpanded
                if (startExpanded) {
                    eButtonStart!!.setImageResource(R.drawable.start)
                    fadeInView(imgABC)
                    fadeInView(imgNumbers)
                    fadeInView(imgColors)
                    fadeInView(imgPronouns)
                    fadeInView(imgPossesive)
                } else {
                    eButtonStart!!.setImageResource(R.drawable.start)
                    fadeInView(imgABC)
                    fadeInView(imgNumbers)
                    fadeInView(imgColors)
                    fadeOutView(imgPronouns)
                    fadeOutView(imgPossesive)
                }
            })
        }

        if (imgPrepos != null) {
            imgPrepos!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                /*boolean passedAdjectives = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
                if (passedAdjectives) {
                    Intent intent = new Intent(MenuA1Activity.this, ListeningActivity.class);
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
                    intent.putExtra("LEVEL", "A1.1");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Para empezar la sesión PREPOSITIONS OF PLACE Completa POSSESSIVE ADJECTIVES.", Toast.LENGTH_LONG).show();
                }*/
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean(
                    "PASSED_WRITING_POSSESSIVE_ADJECTIVES",
                    false
                )
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, ListeningActivity::class.java)
                    intent.putExtra("TOPIC", "PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión PREPOSITIONS OF PLACE, completa POSSESSIVE ADJECTIVES en Writing primero.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (imgAdject != null) {
            imgAdject!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked = this.isUnlockAllEnabled || prefs.getBoolean(
                    "PASSED_PREPOSITIONS_OF_PLACE",
                    false
                )
                if (unlocked) {
                    val intent = Intent(this@MenuA1Activity, ListeningActivity::class.java)
                    intent.putExtra("TOPIC", "ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)")
                    intent.putExtra("LEVEL", "A1.1")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión ADJECTIVES Completa PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // Click listener para el botón de identificación de imágenes
        if (btnImageIdentification != null) {
            btnImageIdentification!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_ADJECTIVES", false)
                if (unlocked) {
                    val intent =
                        Intent(this@MenuA1Activity, ImageIdentificationActivity::class.java)
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                    intent.putExtra("LEVEL", "A1.1")
                    intent.putExtra("SOURCE_MAP", "LISTENING")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión GUESS PICTURE Completa ADJECTIVES (FEELINGS).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        // Click listener para el botón de identificación de imágenes con audio
        if (btnImageIdentificationAudio != null) {
            btnImageIdentificationAudio!!.setOnClickListener(View.OnClickListener { v: View? ->
                val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)
                val unlocked =
                    this.isUnlockAllEnabled || prefs.getBoolean("PASSED_GUESS_PICTURE", false)
                if (unlocked) {
                    val intent =
                        Intent(this@MenuA1Activity, ImageIdentificationAudioActivity::class.java)
                    intent.putExtra("TOPIC", "POSSESSIVE ADJECTIVES")
                    intent.putExtra("LEVEL", "A1.1")
                    intent.putExtra("SOURCE_MAP", "LISTENING")
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Para empezar la sesión LISTEN GUESS Completa GUESS PICTURE.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        if (imgIcMap2 != null) {
            imgIcMap2!!.setOnClickListener(View.OnClickListener { v: View? ->
                icMap2Expanded = !icMap2Expanded
                if (icMap2Expanded) {
                    imgIcMap2!!.setImageResource(R.drawable.icon_map2)
                    fadeInView(imgPrepos)
                    fadeInView(imgAdject)
                    fadeInView(btnImageIdentification)
                    fadeInView(btnImageIdentificationAudio)
                } else {
                    imgIcMap2!!.setImageResource(R.drawable.icon_map2)
                    fadeOutView(imgPrepos)
                    fadeOutView(imgAdject)
                    fadeInView(btnImageIdentification)
                    fadeInView(btnImageIdentificationAudio)
                }
            })
        }

        if (eButtonProfile != null) {
            eButtonProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuA1Activity, ProfileActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuA1Activity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (homeButton != null) {
            homeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@MenuA1Activity, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuA1Activity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)
        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })

        if (quizMenu != null) {
            quizMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    val intent = Intent(this@MenuA1Activity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuA1Activity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (pronunMenu != null) {
            pronunMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent =
                        Intent(this@MenuA1Activity, PronunciationHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MenuA1Activity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (birdMenu != null) {
            birdMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                birdExpanded = !birdExpanded
                if (birdExpanded) {
                    birdMenu!!.setImageResource(R.drawable.bird1_menu)
                    fadeInView(quizMenu)
                    fadeInView(pronunMenu)
                } else {
                    birdMenu!!.setImageResource(R.drawable.bird0_menu)
                    fadeOutView(quizMenu)
                    fadeOutView(pronunMenu)
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
        ModuleTracker.clearLastModule(this)
        startActivity(Intent(this@MenuA1Activity, MainActivity::class.java))
        Toast.makeText(
            this@MenuA1Activity,
            "Has retornado al menú correctamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Aplicar efectos visuales de bloqueo a todos los elementos del menú
    private fun applyVisualLockEffects() {
        val prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE)

        val unlockedAll = this.isUnlockAllEnabled
        val speakingStartCompleted = unlockedAll || areAllSpeakingStartTopicsCompleted(prefs)


        // Aplicar efectos de bloqueo basados en el progreso
        applyLockEffect(imgABC, true) // ABC siempre desbloqueado

        val passedAlphabet = prefs.getBoolean("PASSED_ALPHABET", false)
        applyLockEffect(imgNumbers, unlockedAll || passedAlphabet)

        val passedNumbers = prefs.getBoolean("PASSED_NUMBERS", false)
        applyLockEffect(imgColors, unlockedAll || passedNumbers)

        val passedColors = prefs.getBoolean("PASSED_COLORS", false)
        applyLockEffect(imgPronouns, unlockedAll || passedColors)

        val passedPronouns = prefs.getBoolean("PASSED_PERSONAL_PRONOUNS", false)
        applyLockEffect(imgPossesive, unlockedAll || passedPronouns)

        val passedPossessive = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false)
        applyLockEffect(imgPrepos, unlockedAll || (passedPossessive && speakingStartCompleted))

        val passedPrepositions = prefs.getBoolean("PASSED_PREPOSITIONS_OF_PLACE", false)
        applyLockEffect(imgAdject, unlockedAll || (passedPrepositions && speakingStartCompleted))

        /*boolean passedAdjectives = prefs.getBoolean("PASSED_POSSESSIVE_ADJECTIVES", false);
        applyLockEffect(imgAdject, passedAdjectives);*/
        // PREPOSITIONS en listening requiere completar POSSESSIVE ADJECTIVES en Writing
        val passedWritingPossessive =
            prefs.getBoolean("PASSED_WRITING_POSSESSIVE_ADJECTIVES", false)
        applyLockEffect(
            imgPrepos,
            unlockedAll || (passedWritingPossessive && speakingStartCompleted)
        )

        val passedAdjFeelings = prefs.getBoolean("PASSED_ADJECTIVES", false)
        applyLockEffect(imgAdject, unlockedAll || passedAdjFeelings)

        val passedGuessPict = prefs.getBoolean("PASSED_GUESS_PICTURE", false)
        applyLockEffect(imgAdject, unlockedAll || passedGuessPict)


        // CAMBIO: Aplicar bloqueo por color según progreso real
        // btnImageIdentification (GUESS PICTURE) - requiere ADJECTIVES completado
        applyLockEffect(
            btnImageIdentification,
            unlockedAll || (passedAdjFeelings && speakingStartCompleted)
        )


        // btnImageIdentificationAudio (LISTEN GUESS) - requiere GUESS PICTURE completado
        val passedGuessPicture = prefs.getBoolean("PASSED_GUESS_PICTURE", false)
        applyLockEffect(
            btnImageIdentificationAudio,
            unlockedAll || (passedGuessPicture && speakingStartCompleted)
        )

        Log.d("MenuA1Activity", "Speaking start completed: " + speakingStartCompleted)
        Log.d("MenuA1Activity", "Efectos visuales de bloqueo aplicados")
        Log.d("MenuA1Activity", "GUESS PICTURE desbloqueado: " + passedAdjFeelings)
        Log.d("MenuA1Activity", "LISTEN GUESS desbloqueado: " + passedGuessPicture)
    }

    // Aplicar efecto visual de bloqueo o desbloqueado a un ImageView
    private fun applyLockEffect(imageView: ImageView?, isUnlocked: Boolean) {
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

    // Método para refrescar los efectos visuales cuando cambie el progreso
    private fun refreshVisualEffects() {
        applyVisualLockEffects()
    }

    // Comprueba si los 5 temas básicos de Speaking están completados
    private fun areAllSpeakingStartTopicsCompleted(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean("PASSED_PRON_ALPHABET", false) &&
                prefs.getBoolean("PASSED_PRON_NUMBERS", false) &&
                prefs.getBoolean("PASSED_PRON_COLORS", false) &&
                prefs.getBoolean("PASSED_PRON_PERSONAL_PRONOUNS", false) &&
                prefs.getBoolean("PASSED_PRON_POSSESSIVE_ADJECTIVES", false)
    }
}
