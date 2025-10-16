package com.example.speak

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.example.speak.database.DatabaseHelper
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Importaciones necesarias para trabajar con UI, base de datos y diseño

// Importación de una clase personalizada para acceder a la base de datos
/**
 * Clase que muestra el historial de preguntas de un quiz.
 * Extiende AppCompatActivity, lo que le permite ser una pantalla dentro de la app.
 */
class QuizHistoryActivity : AppCompatActivity() {
    // Referencias a la base de datos y al diseño (tabla)
    private var dbHelper: DatabaseHelper? = null
    private var tableLayout: TableLayout? = null
    private var tableLayoutHeader: TableLayout? = null
    private var headerScrollView: HorizontalScrollView? = null
    private var contentScrollView: HorizontalScrollView? = null
    private var currentUserId: Long = 0
    private var continueButton: Button? = null

    private var eBtnReturnMenu: ConstraintLayout? = null

    private var birdExpanded = false
    private var birdMenu: ImageView? = null
    private var quizMenu: ImageView? = null
    private var pronunMenu: ImageView? = null

    private var eButtonProfile: ImageView? = null
    private var homeButton: ImageView? = null

    /**
     * Método que se ejecuta al crear la actividad.
     * Inicializa el layout, la base de datos y carga el historial.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_history)

        dbHelper = DatabaseHelper(this)
        tableLayout = findViewById<TableLayout>(R.id.quizHistoryTable)
        tableLayoutHeader = findViewById<TableLayout>(R.id.quizHistoryTableHeader)
        headerScrollView = findViewById<HorizontalScrollView?>(R.id.headerScrollView)
        contentScrollView = findViewById<HorizontalScrollView?>(R.id.contentScrollView)
        val historyTitle = findViewById<TextView>(R.id.historyTitle)
        val exportButton = findViewById<LinearLayout>(R.id.exportButton)
        continueButton = findViewById<Button>(R.id.continueButton)

        // Sincronizar el scroll horizontal entre header y contenido
        setupScrollSync()

        //Declaramos las variables Menu
        birdMenu = findViewById<ImageView?>(R.id.imgBirdMenu)
        quizMenu = findViewById<ImageView?>(R.id.imgQuizMenu)
        pronunMenu = findViewById<ImageView?>(R.id.imgPronunMenu)
        eButtonProfile = findViewById<ImageView?>(R.id.btnProfile)
        homeButton = findViewById<ImageView?>(R.id.homeButton)

        // Obtener el ID del usuario actual
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = prefs.getLong("user_id", -1)
        Log.d("QuizHistory", "Current User ID: " + currentUserId)

        // Obtener extras del Intent
        val quizType = getIntent().getStringExtra("QUIZ_TYPE")
        val score = getIntent().getIntExtra("SCORE", 0)
        val totalQuestions = getIntent().getIntExtra("TOTAL_QUESTIONS", 0)
        val currentTopic = getIntent().getStringExtra("TOPIC") // Obtener el tema actual

        Log.d("QuizHistory", "Quiz Type: " + quizType)
        Log.d("QuizHistory", "Score: " + score)
        Log.d("QuizHistory", "Total Questions: " + totalQuestions)
        Log.d("QuizHistory", "Current Topic: " + currentTopic)

        // Actualizar el título según el tipo de quiz
        if (quizType != null) {
            historyTitle.setText("Resultados de " + quizType)
        } else {
            historyTitle.setText("Historial de Actividades")
        }

        try {
            // Usar el nuevo método loadQuizResults
            loadQuizResults()
        } catch (e: Exception) {
            Log.e("QuizHistory", "Error loading quiz history: " + e.message)
            Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show()
        }

        // Configurar el botón "Continuar"
        setupContinueButton(currentTopic, score, totalQuestions)

        // Configurar el botón de exportación
        exportButton.setOnClickListener(View.OnClickListener { v: View? -> exportToCSV() })

        if (eButtonProfile != null) {
            eButtonProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@QuizHistoryActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@QuizHistoryActivity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        if (homeButton != null) {
            homeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    val intent = Intent(this@QuizHistoryActivity, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@QuizHistoryActivity,
                        "Error opening activity: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        eBtnReturnMenu = findViewById<ConstraintLayout>(R.id.eBtnReturnMenu)
        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ReturnMenu()
            }
        })

        if (quizMenu != null) {
            quizMenu!!.setOnClickListener(View.OnClickListener { v: View? ->
                try {
                    //Intent intent = new Intent(MenuA1Activity.this, com.example.speak.quiz.QuizActivity.class);
                    val intent = Intent(this@QuizHistoryActivity, QuizHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@QuizHistoryActivity,
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
                        Intent(this@QuizHistoryActivity, PronunciationHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@QuizHistoryActivity,
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

    /**
     * Sincroniza el scroll horizontal entre el header y el contenido
     */
    private fun setupScrollSync() {
        if (headerScrollView != null && contentScrollView != null) {
            // Variable para evitar bucles infinitos de sincronización
            val isSyncing = booleanArrayOf(false)

            headerScrollView!!.setOnScrollChangeListener(View.OnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                if (!isSyncing[0]) {
                    isSyncing[0] = true
                    contentScrollView!!.scrollTo(scrollX, scrollY)
                    isSyncing[0] = false
                }
            })

            contentScrollView!!.setOnScrollChangeListener(View.OnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                if (!isSyncing[0]) {
                    isSyncing[0] = true
                    headerScrollView!!.scrollTo(scrollX, scrollY)
                    isSyncing[0] = false
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

    private fun showFullHistory(tableLayout: TableLayout, currentUserId: Long) {
        // Crear encabezado de la tabla
        tableLayoutHeader!!.removeAllViews()
        addTableHeader(tableLayoutHeader!!)

        val cursor = dbHelper!!.getQuizHistory()
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // Obtener los índices de las columnas una sola vez
                val userIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID)
                val questionIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION)
                val userAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER)
                val correctAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER)
                val isCorrectIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)
                val topicIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC)
                val levelIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL)

                do {
                    val recordUserId = cursor.getLong(userIdIndex)
                    if (recordUserId == currentUserId) {
                        addTableRow(tableLayout, cursor)
                    }
                } while (cursor.moveToNext())
            } catch (e: Exception) {
                Log.e("QuizHistory", "Error processing cursor: " + e.message)
                e.printStackTrace()
            } finally {
                cursor.close()
            }
        }
    }

    private fun showCurrentActivityResults(
        tableLayout: TableLayout,
        questions: ArrayList<ListeningQuestion?>?,
        score: Int,
        totalQuestions: Int
    ) {
        Log.d("QuizHistory", "Showing current activity results")

        // Crear encabezado de la tabla
        tableLayoutHeader!!.removeAllViews()
        addTableHeader(tableLayoutHeader!!)

        // Obtener el timestamp de sesión
        val sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1)
        val quizType = getIntent().getStringExtra("QUIZ_TYPE")

        Log.d("QuizHistory", "Session Timestamp: " + sessionTimestamp)
        Log.d("QuizHistory", "Quiz Type: " + quizType)

        if (sessionTimestamp == -1L) {
            Log.e("QuizHistory", "No session timestamp found")
            return
        }

        // Obtener las respuestas de la sesión actual
        val cursor = dbHelper!!.getQuizHistory()
        if (cursor == null) {
            Log.e("QuizHistory", "Cursor is null")
            return
        }

        if (!cursor.moveToFirst()) {
            Log.e("QuizHistory", "No data in cursor")
            cursor.close()
            return
        }

        try {
            // Obtener los índices de las columnas una sola vez
            val userIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID)
            val questionIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION)
            val userAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER)
            val correctAnswerIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER)
            val isCorrectIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)
            val topicIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC)
            val levelIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL)
            val timestampIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP)
            val quizTypeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE)

            // Verificar que todos los índices sean válidos
            if (userIdIndex == -1 || questionIndex == -1 || userAnswerIndex == -1 || correctAnswerIndex == -1 || isCorrectIndex == -1 || topicIndex == -1 || levelIndex == -1 || timestampIndex == -1 || quizTypeIndex == -1) {
                Log.e("QuizHistory", "Invalid column index")
                return
            }

            // Mostrar solo las respuestas de la sesión actual
            do {
                val recordUserId = cursor.getLong(userIdIndex)
                val recordQuizType = cursor.getString(quizTypeIndex)
                val recordTimestamp = cursor.getLong(timestampIndex)

                if (recordUserId == currentUserId &&
                    quizType == recordQuizType && recordTimestamp == sessionTimestamp
                ) {  // Comparación exacta del timestamp

                    val question = cursor.getString(questionIndex)
                    val userAnswer = cursor.getString(userAnswerIndex)
                    val correctAnswer = cursor.getString(correctAnswerIndex)
                    val isCorrect = cursor.getInt(isCorrectIndex) == 1
                    val topic = cursor.getString(topicIndex)
                    val level = cursor.getString(levelIndex)

                    addTableRow(tableLayout, cursor)
                }
            } while (cursor.moveToNext())
        } catch (e: Exception) {
            Log.e("QuizHistory", "Error processing cursor: " + e.message)
            e.printStackTrace()
        } finally {
            cursor.close()
        }
    }

    private fun showFilteredHistory(
        tableLayout: TableLayout,
        currentUserId: Long,
        quizType: String,
        showOnlyLast10: Boolean
    ) {
        // Crear encabezado de la tabla
        tableLayoutHeader!!.removeAllViews()
        addTableHeader(tableLayoutHeader!!)

        val cursor = dbHelper!!.getQuizHistory()
        if (cursor != null && cursor.moveToFirst()) {
            var count = 0
            do {
                val recordUserId =
                    cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID))
                val recordQuizType =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUIZ_TYPE))

                if (recordUserId == currentUserId && quizType == recordQuizType) {
                    addTableRow(tableLayout, cursor)
                    count++

                    if (showOnlyLast10 && count >= 10) {
                        break
                    }
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
    }


    private fun addTableHeader(table: TableLayout) {
        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(getResources().getColor(R.color.header_blue_table))

        val headers = arrayOf<String?>(
            "Date",
            "Question",
            "Correct Answer",
            "Your Answer",
            "Result",
            "Topic",
            "Level"
        )
        for (header in headers) {
            val textView = TextView(this)
            textView.setText(header)
            textView.setTextColor(Color.WHITE)
            textView.setTextSize(16f)
            textView.setPadding(16, 12, 16, 12)
            textView.setTypeface(null, Typeface.BOLD)
            textView.setMinWidth(220) // Aumenta el ancho mínimo
            textView.setMaxLines(1) // Solo una línea
            textView.setSingleLine(true) // Forzar una sola línea
            textView.setEllipsize(TextUtils.TruncateAt.END)
            headerRow.addView(textView)
        }
        table.addView(headerRow)
    }

    private fun addTableRow(table: TableLayout, cursor: Cursor) {
        val row = TableRow(this)
        row.setBackgroundColor(
            if (cursor.getPosition() % 2 == 0) getResources().getColor(R.color.white) else getResources().getColor(
                R.color.light_gray
            )
        )

        // Formatear la fecha
        val timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP))
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))

        // Obtener los datos
        val question = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION))
        val correctAnswer =
            cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER))
        val selectedAnswer =
            cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER))
        val isCorrect = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1
        val topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC))
        val level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL))

        // Crear las celdas
        val cellContents = arrayOf<String?>(
            date,
            question,
            correctAnswer,
            selectedAnswer,
            if (isCorrect) "✓" else "✗",
            topic,
            level
        )

        for (i in cellContents.indices) {
            val textView = TextView(this)
            textView.setText(cellContents[i])
            textView.setTextSize(14f)
            textView.setPadding(16, 12, 16, 12)
            textView.setMinWidth(200) // Ancho mínimo para cada columna
            textView.setMaxWidth(400) // Ancho máximo para cada columna
            textView.setEllipsize(TextUtils.TruncateAt.END)
            textView.setMaxLines(3)

            // Aplicar color rojo o verde para el resultado
            if (i == 4) { // La columna del resultado
                textView.setTextColor(if (isCorrect) Color.GREEN else Color.RED)
                textView.setTextSize(18f) // Hacer el símbolo más grande
                textView.setTypeface(null, Typeface.BOLD)
            }

            row.addView(textView)
        }

        table.addView(row)
    }

    /**
     * Muestra un mensaje al usuario usando Toast
     * @param message El mensaje a mostrar
     * @param isSuccess Si el mensaje es de éxito o error
     */
    private fun showMessage(message: String?, isSuccess: Boolean) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Configurar el botón "Continuar" según el progreso del usuario
     */
    private fun setupContinueButton(currentTopic: String?, score: Int, totalQuestions: Int) {
        // Si no tenemos el tema del Intent, intentar obtenerlo de la base de datos
        var currentTopic = currentTopic
        if (currentTopic == null || currentTopic.isEmpty()) {
            currentTopic = this.topicFromDatabase
        }

        // Calcular el porcentaje del score
        val finalScore =
            if (totalQuestions > 0) ((score / totalQuestions.toDouble()) * 100).toInt() else 0

        Log.d(
            TAG,
            "Setting up continue button - Topic: " + currentTopic + ", Score: " + finalScore + "%"
        )

        // Solo mostrar el botón si hay un tema válido y el usuario aprobó
        if (currentTopic != null && !currentTopic.isEmpty() && finalScore >= 70) {
            val nextTopic = ProgressionHelper.getNextTopic(this, currentTopic)

            if (nextTopic != null) {
                // Hay un siguiente tema disponible
                continueButton!!.setText(ProgressionHelper.getContinueButtonText(nextTopic))
                continueButton!!.setVisibility(View.VISIBLE)

                // Crear copia final de las variables para usar en la lambda
                val finalCurrentTopic: String? = currentTopic
                val finalScoreForLambda = finalScore

                // Configurar el click listener
                continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                    Log.d(TAG, "Continue button clicked")
                    Log.d(TAG, "Current topic: " + finalCurrentTopic)
                    Log.d(TAG, "Score: " + finalScoreForLambda + "%")

                    // Marcar tema actual como completado
                    ProgressionHelper.markTopicCompleted(
                        this,
                        finalCurrentTopic,
                        finalScoreForLambda
                    )
                    Log.d(TAG, "Topic marked as completed")

                    // Crear intent para continuar con el siguiente tema
                    val continueIntent =
                        ProgressionHelper.createContinueIntent(this, finalCurrentTopic, "")
                    Log.d(
                        TAG,
                        "Continue intent created: " + (if (continueIntent != null) continueIntent.getComponent() else "null")
                    )
                    if (continueIntent != null) {
                        Log.d(TAG, "Starting next activity")
                        startActivity(continueIntent)
                        finish()
                    } else {
                        Log.e(TAG, "Continue intent is null for topic: " + finalCurrentTopic)
                        Toast.makeText(this, "No hay más temas disponibles", Toast.LENGTH_SHORT)
                            .show()
                    }
                })

                Log.d(TAG, "Continue button configured for next topic: " + nextTopic)
            } else {
                // Es el último tema
                continueButton!!.setText("¡Nivel completado!")
                continueButton!!.setVisibility(View.VISIBLE)
                continueButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                    val intent = Intent(this, MenuA1Activity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                })

                Log.d(TAG, "Continue button configured as level completed")
            }
        } else {
            // No mostrar el botón si no aprobó o no hay tema válido
            continueButton!!.setVisibility(View.GONE)
            Log.d(
                TAG,
                "Continue button hidden - Topic: " + currentTopic + ", Score: " + finalScore + "%"
            )
        }
    }

    private val topicFromDatabase: String?
        /**
         * Obtener el tema de la base de datos basado en la sesión actual
         */
        get() {
            try {
                val sessionTimestamp =
                    getIntent().getLongExtra("SESSION_TIMESTAMP", -1)
                val quizType = getIntent().getStringExtra("QUIZ_TYPE")

                if (sessionTimestamp == -1L || quizType == null) {
                    Log.d(
                        TAG,
                        "No session timestamp or quiz type available"
                    )
                    return null
                }

                val query =
                    "SELECT DISTINCT " + DatabaseHelper.COLUMN_TOPIC +
                            " FROM quiz_results WHERE " +
                            DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_QUIZ_TYPE + " = ? AND " +
                            DatabaseHelper.COLUMN_TIMESTAMP + " = ? LIMIT 1"

                val cursor = dbHelper!!.getReadableDatabase().rawQuery(
                    query, arrayOf<String>(
                        currentUserId.toString(), quizType, sessionTimestamp.toString()
                    )
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val topicIndex =
                        cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC)
                    if (topicIndex != -1) {
                        val topic = cursor.getString(topicIndex)
                        cursor.close()
                        Log.d(
                            TAG,
                            "Topic obtained from database: " + topic
                        )
                        return topic
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error getting topic from database: " + e.message
                )
            }

            return null
        }

    /**
     * Se llama cuando la actividad se destruye. Cierra la base de datos para liberar recursos.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
    }

    private fun loadQuizResults() {
        try {
            val quizType = getIntent().getStringExtra("QUIZ_TYPE")
            val showCurrentActivityOnly =
                getIntent().getBooleanExtra("SHOW_CURRENT_ACTIVITY_ONLY", false)
            val sessionTimestamp = getIntent().getLongExtra("SESSION_TIMESTAMP", -1)

            Log.d(
                TAG,
                "Loading quiz results - Type: " + quizType + ", ShowCurrentOnly: " + showCurrentActivityOnly + ", Timestamp: " + sessionTimestamp
            )

            tableLayout!!.removeAllViews()
            tableLayoutHeader!!.removeAllViews()
            addTableHeader(tableLayoutHeader!!)

            if (quizType != null && quizType == "Writing") {
                Log.d(TAG, "Loading Writing results")
                var writingQuery = "SELECT * FROM " + DatabaseHelper.TABLE_WRITING + " WHERE " +
                        DatabaseHelper.COLUMN_WRITING_USER_ID + " = ?"
                var writingSelectionArgs: Array<String> = arrayOf(currentUserId.toString())

                if (showCurrentActivityOnly && sessionTimestamp != -1L) {
                    writingQuery += " AND " + DatabaseHelper.COLUMN_WRITING_TIMESTAMP + " = ?"
                    writingSelectionArgs =
                        arrayOf(currentUserId.toString(), sessionTimestamp.toString())
                }

                writingQuery += " ORDER BY " + DatabaseHelper.COLUMN_WRITING_TIMESTAMP + " DESC"
                Log.d(TAG, "Writing query: " + writingQuery)

                val writingCursor =
                    dbHelper!!.getReadableDatabase().rawQuery(writingQuery, writingSelectionArgs)

                // Verificar si el cursor tiene datos
                if (writingCursor == null) {
                    Log.e(TAG, "Writing cursor is null")
                    return
                }

                if (!writingCursor.moveToFirst()) {
                    Log.d(TAG, "No writing results found")
                    writingCursor.close()
                    return
                }

                // Obtener índices de columnas
                val timestampIndex =
                    writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_TIMESTAMP)
                val questionIndex =
                    writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_QUESTION)
                val userAnswerIndex =
                    writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_USER_ANSWER)
                val topicIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC)
                val levelIndex = writingCursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL)
                val similarityIndex =
                    writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_SIMILARITY)
                val isCorrectIndex =
                    writingCursor.getColumnIndex(DatabaseHelper.COLUMN_WRITING_IS_CORRECT)

                // Verificar que todos los índices sean válidos
                if (timestampIndex == -1 || questionIndex == -1 || userAnswerIndex == -1 || topicIndex == -1 || levelIndex == -1 || similarityIndex == -1 || isCorrectIndex == -1) {
                    Log.e(TAG, "Invalid column index in writing table")
                    writingCursor.close()
                    return
                }

                do {
                    try {
                        val row = TableRow(this)
                        row.setBackgroundColor(
                            if (writingCursor.getPosition() % 2 == 0) getResources().getColor(
                                R.color.white
                            ) else getResources().getColor(R.color.light_gray)
                        )

                        val timestamp = writingCursor.getLong(timestampIndex)
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(timestamp))

                        val question = writingCursor.getString(questionIndex)
                        val userAnswer = writingCursor.getString(userAnswerIndex)
                        val topic = writingCursor.getString(topicIndex)
                        val level = writingCursor.getString(levelIndex)
                        val similarity = writingCursor.getDouble(similarityIndex)
                        val similarityText = String.format("%.1f%%", similarity * 100)
                        val isCorrect = writingCursor.getInt(isCorrectIndex) == 1

                        val cellContents = arrayOf<String?>(
                            date,
                            question,
                            if (isCorrect) "✓" else "✗",
                            userAnswer,
                            similarityText,
                            topic,
                            level
                        )

                        for (i in cellContents.indices) {
                            val textView = TextView(this)
                            textView.setText(cellContents[i])
                            textView.setTextSize(14f)
                            textView.setPadding(16, 12, 16, 12)
                            textView.setMinWidth(220)
                            textView.setMaxLines(3)
                            textView.setEllipsize(TextUtils.TruncateAt.END)

                            // Aplicar color al símbolo de correcto/incorrecto
                            if (i == 2) { // La columna del símbolo ✓/✗
                                textView.setTextColor(if (isCorrect) Color.GREEN else Color.RED)
                                textView.setTextSize(18f)
                                textView.setTypeface(null, Typeface.BOLD)
                            } else if (i == 4) { // La columna del porcentaje
                                textView.setTextColor(
                                    if (similarity >= 0.7) Color.GREEN else if (similarity >= 0.5) Color.rgb(
                                        255,
                                        165,
                                        0
                                    ) else  // Naranja
                                        Color.RED
                                )
                                textView.setTextSize(16f)
                                textView.setTypeface(null, Typeface.BOLD)
                            }

                            row.addView(textView)
                        }

                        tableLayout!!.addView(row)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing writing row: " + e.message)
                    }
                } while (writingCursor.moveToNext())
                writingCursor.close()
            } else {
                Log.d(TAG, "Loading quiz_results")
                var query =
                    "SELECT * FROM quiz_results WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?"
                var selectionArgs: Array<String> = arrayOf(currentUserId.toString())

                if (showCurrentActivityOnly && sessionTimestamp != -1L) {
                    query += " AND " + DatabaseHelper.COLUMN_TIMESTAMP + " = ?"
                    selectionArgs =
                        arrayOf(currentUserId.toString(), sessionTimestamp.toString())
                } else if (quizType != null && !quizType.isEmpty()) {
                    query += " AND " + DatabaseHelper.COLUMN_QUIZ_TYPE + " = ?"
                    selectionArgs = arrayOf(currentUserId.toString(), quizType)
                }

                query += " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
                Log.d(TAG, "Quiz results query: " + query)

                val cursor = dbHelper!!.getReadableDatabase().rawQuery(query, selectionArgs)
                while (cursor.moveToNext()) {
                    addTableRow(tableLayout!!, cursor)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadQuizResults: " + e.message)
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar el historial: " + e.message, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun exportToCSV() {
        try {
            // Crear el directorio si no existe
            val exportDir = File(getExternalFilesDir(null), "SpeakExports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Crear el archivo CSV con timestamp
            val fileNameTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                Date()
            )
            val csvFile = File(exportDir, "quiz_history_" + fileNameTimestamp + ".csv")
            val writer = FileWriter(csvFile)

            // Escribir encabezados
            writer.append("Date,Question,Correct Answer,User Answer,Score,Topic,Level\n")

            // Obtener los datos de la base de datos
            val cursor = dbHelper!!.getReadableDatabase().query(
                DatabaseHelper.TABLE_QUIZ,
                null,
                DatabaseHelper.COLUMN_USER_ID + " = ?",
                arrayOf<String>(currentUserId.toString()),
                null,
                null,
                DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Obtener los valores de cada columna
                    val recordTimestamp =
                        cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP))
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(recordTimestamp))
                    var question =
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUESTION))
                    var correctAnswer =
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CORRECT_ANSWER))
                    var userAnswer =
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ANSWER))
                    val isCorrect =
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_CORRECT)) == 1
                    var topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC))
                    var level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL))

                    // Escapar comas y comillas en los campos
                    question = "\"" + question.replace("\"", "\"\"") + "\""
                    correctAnswer = "\"" + correctAnswer.replace("\"", "\"\"") + "\""
                    userAnswer = "\"" + userAnswer.replace("\"", "\"\"") + "\""
                    topic = "\"" + topic.replace("\"", "\"\"") + "\""
                    level = "\"" + level.replace("\"", "\"\"") + "\""

                    // Escribir la línea en el CSV
                    writer.append(
                        String.format(
                            "%s,%s,%s,%s,%s,%s,%s\n",
                            date,
                            question,
                            correctAnswer,
                            userAnswer,
                            if (isCorrect) "Correct" else "Incorrect",
                            topic,
                            level
                        )
                    )
                } while (cursor.moveToNext())
                cursor.close()
            }

            writer.flush()
            writer.close()

            // Mostrar mensaje de éxito con la ubicación del archivo
            val message = "Archivo CSV exportado exitosamente a:\n" + csvFile.getAbsolutePath()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

            // Compartir el archivo
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/csv")
            val csvUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                csvFile
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Compartir archivo CSV"))
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to CSV: " + e.message)
            Toast.makeText(this, "Error al exportar el archivo CSV", Toast.LENGTH_SHORT).show()
        }
    }

    //Return Menú
    private fun ReturnMenu() {
        startActivity(Intent(this@QuizHistoryActivity, MainActivity::class.java))
        Toast.makeText(
            this@QuizHistoryActivity,
            "Has retornado al menú correctamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val TAG = "QuizHistoryActivity"
    }
}