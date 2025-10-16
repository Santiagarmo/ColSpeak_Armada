package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.speak.database.DatabaseHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//Animacion
class MainActivity : AppCompatActivity() {
    //Firebase validation
    var eUser: FirebaseUser? = null
    var eAuth: FirebaseAuth? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null
    private var dbHelper: DatabaseHelper? = null

    //Instantiate
    var eTextScore: TextView? = null

    private val pronunciationCard: ImageView? = null
    private val quizCard: ImageView? = null
    private val textToSpeechCard: ImageView? = null

    private var isGuestUser = false

    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var sliderAdapter: SliderAdapter? = null

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var pronunMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        //Firebase
        eAuth = FirebaseAuth.getInstance()
        eUser = eAuth!!.getCurrentUser()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference("ColSpeak")

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Inicializar ViewPager2 y TabLayout
        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        sliderAdapter = SliderAdapter(this)
        viewPager!!.setAdapter(sliderAdapter)

        // ==========================================
        // CONFIGURACIÓN DEL PAGETRANSFORMER
        // ==========================================

        // Configurar offscreenPageLimit para mostrar páginas adyacentes
        viewPager!!.setOffscreenPageLimit(1)

        // Agregar el PageTransformer para acercar las tarjetas
        viewPager!!.setPageTransformer(object : ViewPager2.PageTransformer {
            override fun transformPage(page: View, position: Float) {
                // Ajusta este valor para controlar la separación
                // -30 = más juntas, -20 = más separadas, -40 = muy juntas
                page.setTranslationX(-22 * position)

                // Opcional: agregar efecto de escala para destacar la tarjeta central
                // Puedes comentar estas líneas si no quieres el efecto de escala
//                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position) * 0.15f);
//                page.setScaleY(scaleFactor);
            }
        })

        // ==========================================
        // FIN DE LA CONFIGURACIÓN
        // ==========================================
        birdMenu = findViewById<ImageView>(R.id.imgBirdMenu)
        quizMenu = findViewById<ImageView>(R.id.imgQuizMenu)
        pronunMenu = findViewById<ImageView>(R.id.imgPronunMenu)

        // Conectar TabLayout con ViewPager2
        TabLayoutMediator(
            tabLayout!!, viewPager!!,
            TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int -> }
        ).attach()

        eButtonProfile = findViewById<ImageView>(R.id.btnProfile)
        eButtonProfile!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // Verificar si es usuario invitado
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val isGuest = prefs.getBoolean("is_guest", false)

                if (isGuest) {
                    // Si es invitado, ir directamente al perfil
                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Por favor inicia sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    }
                }
            }
        })

        quizMenu = findViewById<ImageView>(R.id.imgQuizMenu)
        quizMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // Verificar si es usuario invitado
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val isGuest = prefs.getBoolean("is_guest", false)

                if (isGuest) {
                    // Si es invitado, ir directamente al quiz history
                    val intent = Intent(this@MainActivity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        val intent = Intent(this@MainActivity, QuizHistoryActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Por favor inicia sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    }
                }
            }
        })

        pronunMenu = findViewById<ImageView>(R.id.imgPronunMenu)
        pronunMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // Verificar si es usuario invitado
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val isGuest = prefs.getBoolean("is_guest", false)

                if (isGuest) {
                    // Si es invitado, ir directamente al historial
                    val intent = Intent(this@MainActivity, PronunciationHistoryActivity::class.java)
                    startActivity(intent)
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        val intent =
                            Intent(this@MainActivity, PronunciationHistoryActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Por favor inicia sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    }
                }
            }
        })

        // Lock the "Back" button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Leave empty to lock the back button
            }
        })

        // Check if user is guest
        val deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
        if (dbHelper!!.isGuestUserExists(deviceId)) {
            isGuestUser = true
            showGuestUserInfo(deviceId)
        } else {
            UserData()
        }

        birdMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                birdExpanded = !birdExpanded

                if (birdExpanded) {
                    birdMenu!!.setImageResource(R.drawable.bird1_menu)
                    fadeInView(quizMenu!!)
                    fadeInView(pronunMenu!!)
                } else {
                    birdMenu!!.setImageResource(R.drawable.bird0_menu)
                    fadeOutView(quizMenu!!)
                    fadeOutView(pronunMenu!!)
                }
            }
        })

        // Redirigir automáticamente al último módulo si existe (solo en primer arranque)
        val lastModule = ModuleTracker.getLastModule(this)
        if (lastModule != null) {
            var redirectIntent: Intent? = null
            when (lastModule) {
                "listening" -> redirectIntent = Intent(this, MenuA1Activity::class.java)
                "speaking" -> redirectIntent = Intent(this, MenuSpeakingActivity::class.java)
                "reading" -> redirectIntent = Intent(this, MenuReadingActivity::class.java)
                "writing" -> redirectIntent = Intent(this, MenuWritingActivity::class.java)
                "word_order" -> redirectIntent = Intent(this, WordOrderActivity::class.java)
            }
            if (redirectIntent != null) {
                startActivity(redirectIntent)
                finish()
                return
            }
        }
    }

    private fun fadeInView(view: View) {
        view.setVisibility(View.VISIBLE)
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.setDuration(500)
        view.startAnimation(fadeIn)
    }

    private fun fadeOutView(view: View) {
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

    private fun showGuestUserInfo(deviceId: String?) {
        val cursor = dbHelper!!.getGuestUser(deviceId)
        if (cursor.moveToFirst()) {
            val email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))
            val password = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD))
        }
        cursor.close()
    }

    private fun UserData() {
        if (isGuestUser) {
            return
        }

        if (eUser != null) {
            val uid = eUser!!.getUid()
            val email = eUser!!.getEmail()

            databaseReference!!.child("Users").child(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val name =
                                dataSnapshot.child("name").getValue<String?>(String::class.java)
                            val date =
                                dataSnapshot.child("date").getValue<String?>(String::class.java)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al cargar datos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    override fun onStart() {
        LoggedUser()
        super.onStart()
    }

    private fun LoggedUser() {
        var prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isGuest = prefs.getBoolean("is_guest", false)
        var userId = prefs.getLong("user_id", -1)

        if (isGuest && userId != -1L) {
            Toast.makeText(this@MainActivity, "Modo Invitado", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
        if (deviceId != null) {
            if (dbHelper!!.isGuestUserExists(deviceId)) {
                userId = dbHelper!!.getGuestUserId(deviceId)
                if (userId != -1L) {
                    val editor = prefs.edit()
                    editor.putBoolean("is_guest", true)
                    editor.putLong("user_id", userId)
                    editor.apply()
                    Toast.makeText(this@MainActivity, "Modo Invitado", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                userId = dbHelper!!.createGuestUser(deviceId)
                if (userId != -1L) {
                    val editor = prefs.edit()
                    editor.putBoolean("is_guest", true)
                    editor.putLong("user_id", userId)
                    editor.apply()
                    Toast.makeText(this@MainActivity, "Modo Invitado", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val isOffline = prefs.getBoolean("is_offline", false)
        val userEmail = prefs.getString("user_email", null)

        if (isLoggedIn) {
            if (isOffline && userEmail != null) {
                if (dbHelper!!.checkOfflineSession(userEmail)) {
                    Toast.makeText(this@MainActivity, "Modo Offline", Toast.LENGTH_SHORT).show()
                } else {
                    clearLoginState()
                    startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
                    finish()
                }
            } else if (eUser != null) {
                Toast.makeText(this@MainActivity, "En Línea", Toast.LENGTH_SHORT).show()
            }
        } else {
            startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
            finish()
        }
    }

    private fun clearLoginState() {
        eAuth!!.signOut()

        val prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("user_id")
        editor.remove("user_email")
        editor.remove("is_logged_in")
        editor.remove("is_offline")
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
    }
}