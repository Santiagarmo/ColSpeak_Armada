package com.example.speak

import android.R
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.speak.AudioPlayerView.OnProgressChangeListener
import com.example.speak.HelpActivity.HelpAdapter.HelpViewHolder
import com.example.speak.helpers.HelpAudioHelper
import com.example.speak.helpers.HelpContentHelper
import com.example.speak.helpers.SequentialImageHelper
import com.google.android.material.card.MaterialCardView
import java.util.Locale
import kotlin.math.max

class HelpActivity : AppCompatActivity() {
    private var titleTextView: TextView? = null
    private var helpRecyclerView: RecyclerView? = null
    private var helpAdapter: HelpAdapter? = null
    private var helpSections: MutableList<HelpSection>? = null

    private var currentTopic: String? = null
    private var currentLevel: String? = null
    private var filterSectionTitle: String? = null
    private var audioHelper: HelpAudioHelper? = null
    private var imageHelper: SequentialImageHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)


        // Obtener datos del intent
        val intent = getIntent()
        currentTopic = intent.getStringExtra("topic")
        currentLevel = intent.getStringExtra("level")
        filterSectionTitle = intent.getStringExtra("section_title")

        initViews()
        loadHelpContent()


        // Inicializar audio helper
        audioHelper = HelpAudioHelper(this)


        // Inicializar image helper
        imageHelper = SequentialImageHelper(this)
    }

    private fun initViews() {
        titleTextView = findViewById<TextView>(R.id.helpTitleTextView)
        helpRecyclerView = findViewById<RecyclerView>(R.id.helpRecyclerView)


        // Configurar título
        if (currentTopic != null) {
            titleTextView!!.setText("Ayuda / Help\n" + currentTopic)
        } else {
            titleTextView!!.setText("Ayuda / Help\nPronunciación del alfabeto / The alphabet pronunciation")
        }


        // Configurar RecyclerView
        helpRecyclerView!!.setLayoutManager(LinearLayoutManager(this))
        helpSections = ArrayList<HelpSection>()
        helpAdapter = HelpAdapter(helpSections!!, this)
        helpRecyclerView!!.setAdapter(helpAdapter)

        Log.d(TAG, "Views initialized, RecyclerView adapter set")
    }

    private fun loadHelpContent() {
        Log.d(TAG, "Loading help content for topic: " + currentTopic + ", level: " + currentLevel)


        // Usar el helper para cargar contenido dinámicamente
        val contentHelper = HelpContentHelper(this)
        var newSections = contentHelper.loadHelpContent(currentTopic, currentLevel)


        // Filtrar por sección si se solicitó
        if (filterSectionTitle != null && !filterSectionTitle!!.trim { it <= ' ' }.isEmpty()) {
            val filtered: MutableList<HelpSection> = ArrayList<HelpSection>()
            for (s in newSections) {
                if (matchesSection(s, filterSectionTitle)) filtered.add(s)
            }
            if (!filtered.isEmpty()) {
                newSections = filtered
            } else {
                // Fallback: si no encontramos secciones en el helper, usar las secciones por defecto del alfabeto
                if ("ALPHABET".equals(currentTopic, ignoreCase = true)) {
                    val defaultAlphabet = createDefaultAlphabetSections()
                    val filteredDefault: MutableList<HelpSection> = ArrayList<HelpSection>()
                    for (s in defaultAlphabet) if (matchesSection(
                            s,
                            filterSectionTitle
                        )
                    ) filteredDefault.add(
                        s!!
                    )
                    if (!filteredDefault.isEmpty()) {
                        newSections = filteredDefault
                    }
                }
            }
        }

        Log.d(TAG, "Loaded " + newSections.size + " help sections")


        // Si no hay contenido específico, cargar ayuda por defecto
        if (newSections.isEmpty()) {
            Log.w(TAG, "No help sections found, loading default help")
            loadDefaultHelp()
        } else {
            Log.d(TAG, "Help sections loaded successfully")
            for (i in newSections.indices) {
                val section = newSections.get(i)
                Log.d(
                    TAG,
                    "Section " + i + ": title=" + section.title + ", letters=" + section.letters.size
                )
            }


            // Actualizar la lista del adapter
            helpSections!!.clear()
            helpSections!!.addAll(newSections)
        }

        helpAdapter!!.notifyDataSetChanged()
        Log.d(TAG, "Adapter notified of data change, total sections: " + helpSections!!.size)
    }

    // Crea la lista por defecto de secciones del alfabeto (sin tocar el adapter)
    private fun createDefaultAlphabetSections(): MutableList<HelpSection?> {
        val list: MutableList<HelpSection?> = ArrayList<HelpSection?>()
        val section1 = HelpSection()
        section1.title = "Letras con sonido /ei/ /\n Letters with /ei/ sound"
        section1.letters = arrayOf<String>("H [eitʃ]", "J [dʒei]", "A [ei]", "K [kei]")
        section1.centralSound = "ei"
        section1.audioResource = "alphabet_help_ei"
        list.add(section1)

        val section2 = HelpSection()
        section2.title = "Letras con sonido /i/ /\n Letters with /i/ sound"
        section2.letters = arrayOf<String>(
            "B [bi:]",
            "C [si:]",
            "D [di:]",
            "E [i:]",
            "G [dʒi:]",
            "P [pi:]",
            "T [ti:]",
            "V [vi:]",
            "Z [zi:]/[zed]"
        )
        section2.centralSound = "i"
        section2.audioResource = "alphabet_help_i"
        list.add(section2)

        val section3 = HelpSection()
        section3.title = "Letras con sonido /e/ /\n Letters with /e/ sound"
        section3.letters =
            arrayOf<String>("F [ef]", "L [el]", "M [em]", "N [en]", "S [es]", "X [eks]")
        section3.centralSound = "e"
        section3.audioResource = "alphabet_help_e"
        list.add(section3)

        val section4 = HelpSection()
        section4.title = "Letras con sonido /ai/ /\n Letters with /ai/ sound"
        section4.letters = arrayOf<String>("I [ai]", "Y [wai]")
        section4.centralSound = "ai"
        section4.audioResource = "alphabet_help_ai"
        list.add(section4)

        val section5 = HelpSection()
        section5.title = "Letras con sonido /ou/ /\n Letters with /ou/ sound"
        section5.letters = arrayOf<String>("O [ou]")
        section5.centralSound = "ou"
        section5.audioResource = "alphabet_help_ou"
        list.add(section5)

        val section6 = HelpSection()
        section6.title = "Letras con sonido /ju/ /\n Letters with /ju/ sound"
        section6.letters = arrayOf<String>("Q [kju:]", "U [ju:]", "W ['dʌbəl.ju:]")
        section6.centralSound = "ju"
        section6.audioResource = "alphabet_help_ju"
        list.add(section6)

        val section7 = HelpSection()
        section7.title = "Letras con sonido /ar/ /\n Letters with /ar/ sound"
        section7.letters = arrayOf<String>("R [ar]")
        section7.centralSound = "ar"
        section7.audioResource = "alphabet_help_ar"
        list.add(section7)

        return list
    }

    private fun matchesSection(s: HelpSection?, key: String?): Boolean {
        if (s == null || key == null) return false
        val k = normalize(key)
        val title = normalize(s.title)
        val rawTitle = if (s.title == null) "" else s.title!!.lowercase(Locale.getDefault())
        val sound = normalize(s.centralSound)
        // Coincidencia por igualdad o inclusión
        if (k == sound || k == title) return true
        // No usar contains(k) en títulos normalizados para claves cortas como "e" (causa falsos positivos)
        // Coincidencia para secciones de NUMBERS por rango
        if (k == "1-10" && (title.contains("1-10") || sound == "1-10" || normalize(s.title).contains(
                "numbers 1-10"
            ))
        ) return true
        if (k == "11-20" && (title.contains("11-20") || sound == "11-20" || normalize(s.title).contains(
                "numbers 11-20"
            ))
        ) return true

        // Mapeos robustos para ALPHABET por parte o sonido central
        if (k == "ei") {
            if (title.contains("parte 1") || title.contains("part 1")) return true
            if (sound.contains("a-h-j-k")) return true
            if (rawTitle.contains("/ei/")) return true
        }
        if (k == "i") {
            if (title.contains("parte 2") || title.contains("part 2")) return true
            if (sound.contains("b-c-d-e-g-p-t-v-z")) return true
            if (rawTitle.contains("/i/")) return true
        }
        if (k == "e") {
            if (title.contains("parte 3") || title.contains("part 3")) return true
            if (sound.contains("f-l-m-n-s-x")) return true
            if (rawTitle.contains("/e/")) return true
        }
        if (k == "ai") {
            if (title.contains("parte 4") || title.contains("part 4")) return true
            if (sound.contains("i-y")) return true
            if (rawTitle.contains("/ai/")) return true
        }
        if (k == "ou") {
            if (title.contains("parte 5") || title.contains("part 5")) return true
            if (sound == "o") return true
            if (rawTitle.contains("/ou/")) return true
        }
        if (k == "ju") {
            if (title.contains("parte 6") || title.contains("part 6")) return true
            if (sound.contains("q-u-w")) return true
            if (rawTitle.contains("/ju/")) return true
        }
        if (k == "ar") {
            if (title.contains("parte 7") || title.contains("part 7")) return true
            if (sound == "ar" || sound.contains("r")) return true
            if (rawTitle.contains("/ar/")) return true
        }
        return false
    }

    private fun normalize(s: String?): String {
        var s = s
        if (s == null) return ""
        s = s.lowercase(Locale.getDefault()).trim { it <= ' ' }
        s = s.replace("[", "").replace("]", "")
        s = s.replace("/", "")
        s = s.replace("  ", " ")
        return s
    }

    private fun loadDefaultHelp() {
        // Cargar ayuda general por defecto
        loadAlphabetHelp()
    }

    private fun loadAlphabetHelp() {
        helpSections!!.clear()


        // Sección 1: Letras con sonido /ei/
        val section1 = HelpSection()
        section1.title = "Letras con sonido /ei/ / Letters with /ei/ sound"
        section1.letters = arrayOf<String>("H [eitʃ]", "J [dʒei]", "A [ei]", "K [kei]")
        section1.centralSound = "ei"
        section1.audioResource = "alphabet_help_ei" // Recurso de audio
        helpSections!!.add(section1)


        // Sección 2: Letras con sonido /i/
        val section2 = HelpSection()
        section2.title = "Letras con sonido /i/ / Letters with /i/ sound"
        section2.letters = arrayOf<String>(
            "B [bi:]",
            "C [si:]",
            "D [di:]",
            "E [i:]",
            "G [dʒi:]",
            "P [pi:]",
            "T [ti:]",
            "V [vi:]",
            "Z [zi:]/[zed]"
        )
        section2.centralSound = "i"
        section2.audioResource = "alphabet_help_i"
        helpSections!!.add(section2)


        // Sección 3: Letras con sonido /e/
        val section3 = HelpSection()
        section3.title = "Letras con sonido /e/ / Letters with /e/ sound"
        section3.letters =
            arrayOf<String>("F [ef]", "L [el]", "M [em]", "N [en]", "S [es]", "X [eks]")
        section3.centralSound = "e"
        section3.audioResource = "alphabet_help_e"
        helpSections!!.add(section3)


        // Sección 4: Letras con sonido /ai/
        val section4 = HelpSection()
        section4.title = "Letras con sonido /ai/ / Letters with /ai/ sound"
        section4.letters = arrayOf<String>("I [ai]", "Y [wai]")
        section4.centralSound = "ai"
        section4.audioResource = "alphabet_help_ai"
        helpSections!!.add(section4)


        // Sección 5: Letras con sonido /ou/
        val section5 = HelpSection()
        section5.title = "Letras con sonido /ou/ / Letters with /ou/ sound"
        section5.letters = arrayOf<String>("O [ou]")
        section5.centralSound = "ou"
        section5.audioResource = "alphabet_help_ou"
        helpSections!!.add(section5)


        // Sección 6: Letras con sonido /ju/
        val section6 = HelpSection()
        section6.title = "Letras con sonido /ju/ / Letters with /ju/ sound"
        section6.letters = arrayOf<String>("Q [kju:]", "U [ju:]", "W ['dʌbəl.ju:]")
        section6.centralSound = "ju"
        section6.audioResource = "alphabet_help_ju"
        helpSections!!.add(section6)


        // Sección 7: Letras con sonido /ar/
        val section7 = HelpSection()
        section7.title = "Letras con sonido /ar/ / Letters with /ar/ sound"
        section7.letters = arrayOf<String>("R [ar]")
        section7.centralSound = "ar"
        section7.audioResource = "alphabet_help_ar"
        helpSections!!.add(section7)

        helpAdapter!!.notifyDataSetChanged()
    }

    private fun loadNumbersHelp() {
        helpSections!!.clear()


        // Sección para números del 1-10
        val section1 = HelpSection()
        section1.title = "Números del 1 al 10 / Numbers 1 to 10"
        section1.letters = arrayOf<String>(
            "One [wʌn]",
            "Two [tu:]",
            "Three [θri:]",
            "Four [fɔ:]",
            "Five [faiv]",
            "Six [sɪks]",
            "Seven [ˈsevən]",
            "Eight [eɪt]",
            "Nine [naɪn]",
            "Ten [ten]"
        )
        section1.centralSound = "1-10"
        section1.audioResource = "numbers_help_1_10"
        helpSections!!.add(section1)


        // Sección para números del 11-20
        val section2 = HelpSection()
        section2.title = "Números del 11 al 20 / Numbers 11 to 20"
        section2.letters = arrayOf<String>(
            "Eleven [ɪˈlevən]",
            "Twelve [twelv]",
            "Thirteen [θɜːˈtiːn]",
            "Fourteen [fɔːˈtiːn]",
            "Fifteen [fɪfˈtiːn]",
            "Sixteen [sɪksˈtiːn]",
            "Seventeen [sevənˈtiːn]",
            "Eighteen [eɪˈtiːn]",
            "Nineteen [naɪnˈtiːn]",
            "Twenty [ˈtwenti]"
        )
        section2.centralSound = "11-20"
        section2.audioResource = "numbers_help_11_20"
        helpSections!!.add(section2)

        helpAdapter!!.notifyDataSetChanged()
    }

    private fun loadColorsHelp() {
        helpSections!!.clear()


        // Sección para colores básicos
        val section1 = HelpSection()
        section1.title = "Colores básicos / Basic colors"
        section1.letters = arrayOf<String>(
            "Red [red]",
            "Blue [blu:]",
            "Green [gri:n]",
            "Yellow [ˈjeləʊ]",
            "Black [blæk]"
        )
        section1.centralSound = "colors"
        section1.audioResource = "colors_help_basic"
        helpSections!!.add(section1)


        // Sección para colores adicionales
        val section2 = HelpSection()
        section2.title = "Colores adicionales / Additional colors"
        section2.letters = arrayOf<String>(
            "White [waɪt]",
            "Purple [ˈpɜːpl]",
            "Orange [ˈɒrɪndʒ]",
            "Brown [braʊn]",
            "Pink [pɪŋk]"
        )
        section2.centralSound = "colors+"
        section2.audioResource = "colors_help_additional"
        helpSections!!.add(section2)

        helpAdapter!!.notifyDataSetChanged()
    }

    private fun loadGeneralHelp() {
        // Cargar ayuda general por defecto
        loadAlphabetHelp()
    }

    // Clase para representar una sección de ayuda
    class HelpSection {
        @JvmField
        var title: String? = null
        @JvmField
        var letters: Array<String?>
        @JvmField
        var centralSound: String? = null
        @JvmField
        var audioResource: String? = null
        @JvmField
        var imageResources: Array<String?>? // Recursos de imágenes secuenciales
        @JvmField
        var imageDisplayOrder: Int = 0 // Orden de visualización
        @JvmField
        var imageDescription: String? = null // Descripción de la imagen
        @JvmField
        var hasSequentialImages: Boolean = false // Si tiene imágenes secuenciales
        @JvmField
        var currentImageIndex: Int = 0 // Índice de imagen actual
    }

    // Adapter para el RecyclerView
    private class HelpAdapter(
        private val helpSections: MutableList<HelpSection>,
        private val context: Context
    ) : RecyclerView.Adapter<HelpViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
            Log.d("HelpAdapter", "onCreateViewHolder called for viewType: " + viewType)
            val view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_help_section, parent, false)
            return HelpViewHolder(view)
        }

        override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
            Log.d("HelpAdapter", "onBindViewHolder called for position: " + position)
            val section = helpSections.get(position)
            holder.bind(section)
        }

        override fun getItemCount(): Int {
            Log.d("HelpAdapter", "getItemCount called, returning: " + helpSections.size)
            return helpSections.size
        }

        inner class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView
            private val diagramContainer: LinearLayout
            private val audioPlayerCard: MaterialCardView?
            private val playButton: ImageButton

            // Controles para imágenes secuenciales
            private val mainImageView: ImageView
            private val previousImageButton: ImageButton
            private val nextImageButton: ImageButton
            private val imageCounterText: TextView
            private val imageDescriptionText: TextView

            // Controles de audio para progreso y tiempo
            private val speedIndicator: TextView?
            private val audioPlayerView: AudioPlayerView?
            private val currentTimeText: TextView?
            private val totalTimeText: TextView?

            // Botones de tipos de voz (solo 4 opciones)
            private val helpVoiceChildButton: Button
            private val helpVoiceGirlButton: Button
            private val helpVoiceWomanButton: Button
            private val helpVoiceManButton: Button

            // Control de idioma
            private var isSpanishMode = true // Por defecto español
            private val helpLanguageSpanishButton: Button
            private val helpLanguageEnglishButton: Button

            // Control de panel de configuración
            private val helpConfigButton: ImageButton
            private val voiceTypeCard: MaterialCardView
            private var isConfigPanelVisible = false

            fun bind(section: HelpSection) {
                Log.d(
                    "HelpAdapter",
                    "Binding section: " + section.title + " with " + section.letters.size + " letters"
                )

                titleTextView.setText(section.title)


                // Crear diagrama visual
                createDiagram(section)


                // Configurar audio player
                setupAudioPlayer(section)


                // Configurar imágenes secuenciales si existen
                Log.d("HelpAdapter", "About to setup sequential images")
                setupSequentialImages(section)


                // Configurar botones de tipos de voz
                setupVoiceTypeButtons()


                // Configurar botones de idioma
                setupLanguageButtons()


                // Configurar botón de configuración
                setupConfigButton()
            }

            private fun createDiagram(section: HelpSection) {
                Log.d("HelpAdapter", "Creating diagram for section: " + section.title)
                Log.d("HelpAdapter", "Central sound: " + section.centralSound)
                Log.d("HelpAdapter", "Letters count: " + section.letters.size)

                diagramContainer.removeAllViews()


                // Crear círculo central
                val centralCircle = TextView(context)
                centralCircle.setText(section.centralSound)
                centralCircle.setBackgroundResource(R.drawable.circle_background_c)
                centralCircle.setTextColor(context.getResources().getColor(R.color.white))
                centralCircle.setTextSize(18f)
                centralCircle.setPadding(20, 20, 20, 20)
                centralCircle.setGravity(Gravity.CENTER)

                val centralParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                centralParams.gravity = Gravity.CENTER
                centralParams.setMargins(0, 20, 0, 20)
                centralCircle.setLayoutParams(centralParams)

                diagramContainer.addView(centralCircle)
                Log.d("HelpAdapter", "Added central circle with text: " + section.centralSound)


                // Crear letras conectadas
                val lettersContainer = LinearLayout(context)
                lettersContainer.setOrientation(LinearLayout.HORIZONTAL)
                lettersContainer.setGravity(Gravity.CENTER)

                for (letter in section.letters) {
                    Log.d("HelpAdapter", "Creating letter view for: " + letter)
                    val letterView = TextView(context)
                    letterView.setText(letter)
                    letterView.setTextColor(context.getResources().getColor(R.color.white))
                    letterView.setTextSize(14f)
                    letterView.setPadding(10, 5, 10, 5)
                    letterView.setBackgroundResource(com.example.speak.R.drawable.rounded_background)

                    val letterParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    letterParams.setMargins(8, 0, 8, 0)
                    letterView.setLayoutParams(letterParams)

                    lettersContainer.addView(letterView)
                    Log.d("HelpAdapter", "Added letter view: " + letter)
                }

                diagramContainer.addView(lettersContainer)
                Log.d(
                    "HelpAdapter",
                    "Added letters container with " + section.letters.size + " letters"
                )
            }

            private fun setupAudioPlayer(section: HelpSection) {
                // Configurar botón de reproducción/pausa
                playButton.setOnClickListener(View.OnClickListener { v: View? ->
                    if (context is HelpActivity) {
                        val activity = context
                        if (activity.audioHelper != null) {
                            if (activity.audioHelper!!.isPlaying()) {
                                // Pausar
                                activity.audioHelper!!.pauseAudio()
                                playButton.setImageResource(com.example.speak.R.drawable.reproduce)


                                // Detener ondas y progreso
                                if (audioPlayerView != null) {
                                    audioPlayerView.setPlaying(false)
                                }


                                // Detener actualización del progreso
                                stopTimeUpdate()
                            } else if (activity.audioHelper!!.isPaused()) {
                                // Reanudar desde donde se pausó
                                activity.audioHelper!!.resumeAudio()
                                playButton.setImageResource(com.example.speak.R.drawable.pause)


                                // Reanudar ondas y progreso
                                if (audioPlayerView != null) {
                                    audioPlayerView.setPlaying(true)
                                }


                                // Reanudar actualización del progreso
                                startTimeUpdate(section)
                            } else {
                                // Reproducir audio desde el principio
                                if (isSpanishMode) {
                                    // Usar archivo MP3 original en español
                                    activity.audioHelper!!.playAudio(section.audioResource)
                                    playButton.setImageResource(com.example.speak.R.drawable.pause)


                                    // Configurar duración del archivo MP3
                                    val duration = activity.audioHelper!!.getTotalDuration()
                                    if (duration > 0 && audioPlayerView != null) {
                                        Log.d(
                                            "HelpActivity",
                                            "Setting MP3 duration: " + duration + "ms"
                                        )
                                        audioPlayerView.setDuration(duration.toFloat())
                                        audioPlayerView.setPlaying(true)
                                    }


                                    // Iniciar actualización del progreso
                                    startTimeUpdate(section)
                                } else {
                                    // Usar TextToSpeech para inglés con tipos de voz
                                    val textToSpeak = generateEnglishTextFromSection(section)
                                    activity.audioHelper!!.speakText(textToSpeak)
                                    playButton.setImageResource(com.example.speak.R.drawable.pause)


                                    // Configurar duración estimada para TextToSpeech
                                    val estimatedDuration = estimateTextDuration(textToSpeak)
                                    if (audioPlayerView != null) {
                                        Log.d(
                                            "HelpActivity",
                                            "Setting TTS duration: " + estimatedDuration + "ms"
                                        )
                                        audioPlayerView.setDuration(estimatedDuration.toFloat())
                                        audioPlayerView.setPlaying(true)
                                    }


                                    // Iniciar actualización del progreso
                                    startTimeUpdate(section)
                                }
                            }
                        }
                    }
                })


                // Configurar control de velocidad funcional
                if (speedIndicator != null) {
                    speedIndicator.setOnClickListener(View.OnClickListener { v: View? ->
                        // Ciclar entre 1x, 1.5x, 2x
                        val currentSpeed = speedIndicator.getText().toString()
                        val newSpeed: String?
                        val speedValue: Float

                        when (currentSpeed) {
                            "x1" -> {
                                newSpeed = "x1.5"
                                speedValue = 1.5f
                            }

                            "x1.5" -> {
                                newSpeed = "x2"
                                speedValue = 2.0f
                            }

                            "x2" -> {
                                newSpeed = "x1"
                                speedValue = 1.0f
                            }

                            else -> {
                                newSpeed = "x1"
                                speedValue = 1.0f
                            }
                        }

                        speedIndicator.setText(newSpeed)


                        // Aplicar la velocidad al audio
                        if (context is HelpActivity) {
                            val activity = context
                            if (activity.audioHelper != null) {
                                activity.audioHelper!!.setPlaybackSpeed(speedValue)
                            }
                        }
                    })


                    // Establecer velocidad inicial
                    speedIndicator.setText("x1")
                }


                // Configurar AudioPlayerView integrado
                if (audioPlayerView != null) {
                    Log.d("HelpActivity", "Setting up AudioPlayerView listener")


                    // Configurar listener del reproductor
                    audioPlayerView.setOnProgressChangeListener(object : OnProgressChangeListener {
                        override fun onProgressChanged(progress: Float) {
                            // Actualizar tiempo cuando cambie el progreso
                            // progress viene en milisegundos (como en ListeningActivity)
                            Log.d("HelpActivity", "Progress changed: " + progress + "ms")
                            if (context is HelpActivity) {
                                val activity = context
                                if (activity.audioHelper != null) {
                                    val duration = activity.audioHelper!!.getTotalDuration()
                                    if (duration > 0) {
                                        updateTimeDisplay(progress.toInt(), duration)
                                    }
                                }
                            }
                        }

                        override fun onPlayPause(isPlaying: Boolean) {
                            Log.d("HelpActivity", "Play/Pause: " + isPlaying)
                            // No necesitamos hacer nada aquí
                        }

                        override fun onSeek(position: Float) {
                            // Aplicar seek cuando el usuario toque la barra
                            // position viene en milisegundos (como en ListeningActivity)
                            Log.d("HelpActivity", "Seek to: " + position + "ms")
                            if (context is HelpActivity) {
                                val activity = context
                                if (activity.audioHelper != null) {
                                    activity.audioHelper!!.seekTo(position.toInt())
                                }
                            }
                        }
                    })
                }


                // Inicializar valores por defecto
                if (currentTimeText != null) currentTimeText.setText("0:00")
                if (totalTimeText != null) totalTimeText.setText("0:00")


                // Configurar actualización de tiempo en tiempo real
                startTimeUpdate(section)
            }


            /**
             * Actualiza la visualización del tiempo
             */
            private fun updateTimeDisplay(currentPosition: Int, totalDuration: Int) {
                if (currentTimeText != null && totalTimeText != null) {
                    val currentTime = formatTime(currentPosition)
                    val totalTime = formatTime(totalDuration)
                    currentTimeText.setText(currentTime)
                    totalTimeText.setText(totalTime)
                }
            }

            /**
             * Formatea el tiempo en formato MM:SS
             */
            private fun formatTime(milliseconds: Int): String {
                val seconds = (milliseconds / 1000) % 60
                val minutes = (milliseconds / (1000 * 60)) % 60
                return String.format("%d:%02d", minutes, seconds)
            }

            /**
             * Inicia la actualización del tiempo en tiempo real
             */
            private var progressHandler: Handler? = null
            private var progressRunnable: Runnable? = null

            init {
                titleTextView =
                    itemView.findViewById<TextView>(com.example.speak.R.id.helpSectionTitle)
                diagramContainer =
                    itemView.findViewById<LinearLayout>(com.example.speak.R.id.diagramContainer)
                audioPlayerCard =
                    itemView.findViewById<MaterialCardView?>(com.example.speak.R.id.audioPlayerCard)
                playButton = itemView.findViewById<ImageButton>(com.example.speak.R.id.playButton)


                // Inicializar controles de imagen
                mainImageView =
                    itemView.findViewById<ImageView>(com.example.speak.R.id.mainImageView)
                previousImageButton =
                    itemView.findViewById<ImageButton>(com.example.speak.R.id.previousImageButton)
                nextImageButton =
                    itemView.findViewById<ImageButton>(com.example.speak.R.id.nextImageButton)
                imageCounterText =
                    itemView.findViewById<TextView>(com.example.speak.R.id.imageCounterText)
                imageDescriptionText =
                    itemView.findViewById<TextView>(com.example.speak.R.id.imageDescriptionText)


                // Solo el texto de velocidad para el diseño compacto
                // Controles de audio para progreso y tiempo
                speedIndicator =
                    itemView.findViewById<TextView?>(com.example.speak.R.id.speedIndicator)
                audioPlayerView =
                    itemView.findViewById<AudioPlayerView?>(com.example.speak.R.id.audioPlayerView)
                currentTimeText =
                    itemView.findViewById<TextView?>(com.example.speak.R.id.currentTimeText)
                totalTimeText =
                    itemView.findViewById<TextView?>(com.example.speak.R.id.totalTimeText)


                // Inicializar botones de tipos de voz (solo 4 opciones)
                helpVoiceChildButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpVoiceChildButton)
                helpVoiceGirlButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpVoiceGirlButton)
                helpVoiceWomanButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpVoiceWomanButton)
                helpVoiceManButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpVoiceManButton)


                // Inicializar botones de idioma
                helpLanguageSpanishButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpLanguageSpanishButton)
                helpLanguageEnglishButton =
                    itemView.findViewById<Button>(com.example.speak.R.id.helpLanguageEnglishButton)


                // Inicializar controles de configuración
                helpConfigButton =
                    itemView.findViewById<ImageButton>(com.example.speak.R.id.helpConfigButton)
                voiceTypeCard =
                    itemView.findViewById<MaterialCardView>(com.example.speak.R.id.voiceTypeCard)
            }

            private fun startTimeUpdate(section: HelpSection) {
                Log.d("HelpActivity", "startTimeUpdate called for section: " + section.title)
                if (context is HelpActivity) {
                    val activity = context
                    if (activity.audioHelper != null && audioPlayerView != null) {
                        // Detener actualización anterior si existe
                        stopTimeUpdate()


                        // Configurar duración del AudioPlayerView
                        val totalDuration = activity.audioHelper!!.getTotalDuration()
                        Log.d("HelpActivity", "Setting duration: " + totalDuration + "ms")
                        if (totalDuration > 0) {
                            audioPlayerView.setDuration(totalDuration.toFloat())
                        }


                        // Crear un Handler dedicado para esta sección (como en ListeningActivity)
                        progressHandler = Handler()
                        progressRunnable = object : Runnable {
                            override fun run() {
                                if (activity.audioHelper!!.isPlaying()) {
                                    val currentPosition =
                                        activity.audioHelper!!.getCurrentPosition()
                                    val totalDuration = activity.audioHelper!!.getTotalDuration()

                                    Log.d(
                                        "HelpActivity",
                                        "Update - Position: " + currentPosition + "ms, Duration: " + totalDuration + "ms, Playing: true"
                                    )

                                    if (totalDuration > 0) {
                                        // Actualizar AudioPlayerView con posición en milisegundos (como en ListeningActivity)
                                        audioPlayerView.setProgress(currentPosition.toFloat())
                                        audioPlayerView.setPlaying(true)


                                        // Actualizar tiempo
                                        updateTimeDisplay(currentPosition, totalDuration)


                                        // Continuar actualizando si aún está reproduciendo
                                        if (currentPosition < totalDuration) {
                                            progressHandler!!.postDelayed(this, 100)
                                        } else {
                                            // Audio terminó
                                            audioPlayerView.setPlaying(false)
                                            Log.d("HelpActivity", "Audio playback completed")
                                        }
                                    } else {
                                        audioPlayerView.setPlaying(false)
                                    }
                                } else {
                                    Log.d(
                                        "HelpActivity",
                                        "Update - Not playing, setting playing: false"
                                    )
                                    audioPlayerView.setPlaying(false)
                                }
                            }
                        }


                        // Iniciar la actualización del progreso
                        progressHandler!!.post(progressRunnable!!)
                    }
                }
            }

            private fun stopTimeUpdate() {
                if (progressHandler != null && progressRunnable != null) {
                    progressHandler!!.removeCallbacks(progressRunnable!!)
                    progressHandler = null
                    progressRunnable = null
                }
            }


            private fun setupSequentialImages(section: HelpSection) {
                Log.d("HelpAdapter", "setupSequentialImages called for section: " + section.title)
                Log.d("HelpAdapter", "hasSequentialImages: " + section.hasSequentialImages)
                Log.d(
                    "HelpAdapter",
                    "imageResources: " + (if (section.imageResources != null) section.imageResources!!.size else "null")
                )

                if (section.hasSequentialImages && section.imageResources != null && section.imageResources!!.size > 0) {
                    Log.d(
                        "HelpAdapter",
                        "Showing image controls for " + section.imageResources!!.size + " images"
                    )
                    // Mostrar controles de imagen
                    mainImageView.setVisibility(View.VISIBLE)
                    previousImageButton.setVisibility(View.VISIBLE)
                    nextImageButton.setVisibility(View.VISIBLE)
                    imageCounterText.setVisibility(View.VISIBLE)
                    imageDescriptionText.setVisibility(View.VISIBLE)


                    // Configurar imagen inicial
                    section.currentImageIndex = 0
                    Log.d("HelpAdapter", "Setting initial image index to 0")
                    updateImageDisplay(section)


                    // Configurar botones de navegación
                    previousImageButton.setOnClickListener(View.OnClickListener { v: View? ->
                        Log.d(
                            "HelpAdapter",
                            "Previous button clicked, current index: " + section.currentImageIndex
                        )
                        if (section.currentImageIndex > 0) {
                            section.currentImageIndex--
                            updateImageDisplay(section)
                        }
                    })

                    nextImageButton.setOnClickListener(View.OnClickListener { v: View? ->
                        Log.d(
                            "HelpAdapter",
                            "Next button clicked, current index: " + section.currentImageIndex
                        )
                        if (section.currentImageIndex < section.imageResources!!.size - 1) {
                            section.currentImageIndex++
                            updateImageDisplay(section)
                        }
                    })


                    // Configurar estado inicial de botones
                    updateNavigationButtons(section)
                    Log.d("HelpAdapter", "Image controls setup completed")
                } else {
                    // Ocultar controles de imagen si no hay imágenes
                    mainImageView.setVisibility(View.GONE)
                    previousImageButton.setVisibility(View.GONE)
                    nextImageButton.setVisibility(View.GONE)
                    imageCounterText.setVisibility(View.GONE)
                    imageDescriptionText.setVisibility(View.GONE)
                }
            }

            private fun updateImageDisplay(section: HelpSection) {
                Log.d(
                    "HelpAdapter",
                    "updateImageDisplay called, currentIndex: " + section.currentImageIndex
                )
                if (section.imageResources != null && section.currentImageIndex < section.imageResources!!.size) {
                    val imageResource = section.imageResources!![section.currentImageIndex]
                    Log.d("HelpAdapter", "Loading image: " + imageResource)


                    // Usar el SequentialImageHelper para cargar la imagen
                    if (context is HelpActivity) {
                        val activity = context
                        if (activity.imageHelper != null) {
                            Log.d("HelpAdapter", "Using imageHelper to load image")
                            activity.imageHelper!!.loadImage(imageResource, mainImageView)
                        } else {
                            Log.w("HelpAdapter", "imageHelper is null!")
                        }
                    }


                    // Actualizar contador
                    imageCounterText.setText(
                        String.format(
                            "%d/%d",
                            section.currentImageIndex + 1, section.imageResources!!.size
                        )
                    )


                    // Actualizar descripción usando el helper
                    if (context is HelpActivity) {
                        val activity = context
                        if (activity.imageHelper != null) {
                            val description =
                                activity.imageHelper!!.getImageDescription(imageResource)
                            imageDescriptionText.setText(description)
                        }
                    }


                    // Actualizar estado de botones
                    updateNavigationButtons(section)
                }
            }

            private fun updateNavigationButtons(section: HelpSection) {
                // Botón anterior
                previousImageButton.setEnabled(section.currentImageIndex > 0)
                previousImageButton.setAlpha(if (section.currentImageIndex > 0) 1.0f else 0.5f)


                // Botón siguiente
                nextImageButton.setEnabled(section.currentImageIndex < section.imageResources!!.size - 1)
                nextImageButton.setAlpha(if (section.currentImageIndex < section.imageResources!!.size - 1) 1.0f else 0.5f)
            }

            private fun loadImageFromAssets(imageName: String?) {
                // Implementar carga desde assets si es necesario
                // Por ahora usamos imagen por defecto
                mainImageView.setImageResource(com.example.speak.R.drawable.help_icon)
            }

            private fun setupVoiceTypeButtons() {
                if (context is HelpActivity) {
                    val activity = context
                    if (activity.audioHelper != null) {
                        // Configurar listeners para los botones de tipos de voz
                        helpVoiceChildButton.setOnClickListener(View.OnClickListener { v: View? ->
                            activity.audioHelper!!.setVoiceType(VoiceType.NIÑO)
                            updateVoiceTypeButtons()
                        })

                        helpVoiceGirlButton.setOnClickListener(View.OnClickListener { v: View? ->
                            activity.audioHelper!!.setVoiceType(VoiceType.NIÑA)
                            updateVoiceTypeButtons()
                        })

                        helpVoiceWomanButton.setOnClickListener(View.OnClickListener { v: View? ->
                            activity.audioHelper!!.setVoiceType(VoiceType.MUJER)
                            updateVoiceTypeButtons()
                        })

                        helpVoiceManButton.setOnClickListener(View.OnClickListener { v: View? ->
                            activity.audioHelper!!.setVoiceType(VoiceType.HOMBRE)
                            updateVoiceTypeButtons()
                        })


                        // Marcar el botón inicial como seleccionado
                        updateVoiceTypeButtons()
                    }
                }
            }

            private fun updateVoiceTypeButtons() {
                if (context is HelpActivity) {
                    val activity = context
                    if (activity.audioHelper != null) {
                        val currentVoice = activity.audioHelper!!.getCurrentVoiceType()


                        // Resetear todos los botones
                        resetVoiceTypeButtons()


                        // Marcar el botón seleccionado (emoji con efecto de escala y color)
                        when (currentVoice.getName()) {
                            "niño" -> {
                                helpVoiceChildButton.setScaleX(1.2f)
                                helpVoiceChildButton.setScaleY(1.2f)
                                helpVoiceChildButton.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        com.example.speak.R.color.selected_voice
                                    )
                                )
                            }

                            "niña" -> {
                                helpVoiceGirlButton.setScaleX(1.2f)
                                helpVoiceGirlButton.setScaleY(1.2f)
                                helpVoiceGirlButton.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        com.example.speak.R.color.selected_voice
                                    )
                                )
                            }

                            "mujer" -> {
                                helpVoiceWomanButton.setScaleX(1.2f)
                                helpVoiceWomanButton.setScaleY(1.2f)
                                helpVoiceWomanButton.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        com.example.speak.R.color.selected_voice
                                    )
                                )
                            }

                            "hombre" -> {
                                helpVoiceManButton.setScaleX(1.2f)
                                helpVoiceManButton.setScaleY(1.2f)
                                helpVoiceManButton.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        com.example.speak.R.color.selected_voice
                                    )
                                )
                            }
                        }
                    }
                }
            }

            private fun resetVoiceTypeButtons() {
                // Resetear escala y color de todos los botones
                helpVoiceChildButton.setScaleX(1.0f)
                helpVoiceChildButton.setScaleY(1.0f)
                helpVoiceChildButton.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.example.speak.R.color.help_audio_player_background
                    )
                )

                helpVoiceGirlButton.setScaleX(1.0f)
                helpVoiceGirlButton.setScaleY(1.0f)
                helpVoiceGirlButton.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.example.speak.R.color.help_audio_player_background
                    )
                )

                helpVoiceWomanButton.setScaleX(1.0f)
                helpVoiceWomanButton.setScaleY(1.0f)
                helpVoiceWomanButton.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.example.speak.R.color.help_audio_player_background
                    )
                )

                helpVoiceManButton.setScaleX(1.0f)
                helpVoiceManButton.setScaleY(1.0f)
                helpVoiceManButton.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.example.speak.R.color.help_audio_player_background
                    )
                )
            }

            private fun setupLanguageButtons() {
                // Configurar listeners para los botones de idioma
                helpLanguageSpanishButton.setOnClickListener(View.OnClickListener { v: View? ->
                    isSpanishMode = true
                    updateLanguageButtons()
                })

                helpLanguageEnglishButton.setOnClickListener(View.OnClickListener { v: View? ->
                    isSpanishMode = false
                    updateLanguageButtons()
                })


                // Marcar el botón inicial como seleccionado
                updateLanguageButtons()
            }

            private fun updateLanguageButtons() {
                // Resetear todos los botones de idioma
                helpLanguageSpanishButton.setBackgroundTintList(null)
                helpLanguageEnglishButton.setBackgroundTintList(null)


                // Marcar el botón seleccionado
                if (isSpanishMode) {
                    helpLanguageSpanishButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                com.example.speak.R.color.selected_voice
                            )
                        )
                    )
                } else {
                    helpLanguageEnglishButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                com.example.speak.R.color.selected_voice
                            )
                        )
                    )
                }
            }

            private fun setupConfigButton() {
                helpConfigButton.setOnClickListener(View.OnClickListener { v: View? ->
                    toggleConfigPanel()
                })
            }

            private fun toggleConfigPanel() {
                if (isConfigPanelVisible) {
                    // Ocultar panel
                    voiceTypeCard.setVisibility(View.GONE)
                    isConfigPanelVisible = false
                    helpConfigButton.setImageResource(com.example.speak.R.drawable.settings)
                } else {
                    // Mostrar panel
                    voiceTypeCard.setVisibility(View.VISIBLE)
                    isConfigPanelVisible = true
                    helpConfigButton.setImageResource(com.example.speak.R.drawable.settings)
                }
            }

            private fun generateSpanishTextFromSection(section: HelpSection): String {
                val text = StringBuilder()
                text.append("Letras con sonido ").append(section.centralSound).append(": ")

                for (i in section.letters.indices) {
                    text.append(section.letters[i])
                    if (i < section.letters.size - 1) {
                        text.append(", ")
                    }
                }

                text.append(". Repite después de mí: ")
                for (letter in section.letters) {
                    text.append(letter).append(" ")
                }

                return text.toString()
            }

            private fun generateEnglishTextFromSection(section: HelpSection): String {
                val text = StringBuilder()
                text.append("Letters with sound ").append(section.centralSound).append(": ")

                for (i in section.letters.indices) {
                    val cleaned = cleanForTTS(section.letters[i])
                    if (!cleaned.isEmpty()) {
                        text.append(cleaned)
                        if (i < section.letters.size - 1) {
                            text.append(", ")
                        }
                    }
                }

                text.append(". Repeat after me: ")
                for (letter in section.letters) {
                    val cleaned = cleanForTTS(letter)
                    if (!cleaned.isEmpty()) {
                        text.append(cleaned).append(". ")
                    }
                }

                return text.toString().trim { it <= ' ' }
            }

            private fun cleanForTTS(s: String?): String {
                if (s == null) return ""
                // Eliminar contenido IPA entre corchetes, barras y caracteres no verbales que pueden cortar TTS
                var withoutIPA = s.replace("\\s*\\[[^\\]]*\\]".toRegex(), "")
                withoutIPA = withoutIPA.replace("/", " ")
                // Normalizar espacios
                withoutIPA = withoutIPA.replace("\\s+".toRegex(), " ").trim { it <= ' ' }
                return withoutIPA
            }

            private fun estimateTextDuration(text: String): Int {
                // Estimación aproximada: 150 palabras por minuto = 2.5 palabras por segundo
                // Cada palabra promedio tiene 5 caracteres, así que dividimos por 5
                val wordCount =
                    text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size
                val estimatedSeconds = max(3, wordCount / 2) // Mínimo 3 segundos
                return estimatedSeconds * 1000 // Convertir a milisegundos
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioHelper != null) {
            audioHelper!!.release()
        }
    }

    companion object {
        private const val TAG = "HelpActivity"

        // Permite abrir esta actividad filtrando explícitamente por un subtema/clave
        @JvmStatic
        fun startFiltered(context: Context, topic: String?, level: String?) {
            val i = Intent(context, HelpActivity::class.java)
            i.putExtra("topic", topic)
            i.putExtra("level", level)
            context.startActivity(i)
        }

        @JvmStatic
        fun startFilteredSection(
            context: Context,
            topic: String?,
            level: String?,
            sectionTitle: String?
        ) {
            val i = Intent(context, HelpActivity::class.java)
            i.putExtra("topic", topic)
            i.putExtra("level", level)
            i.putExtra("section_title", sectionTitle)
            context.startActivity(i)
        }
    }
}
