package com.example.speak.database;

// Importaciones necesarias para trabajar con SQLite
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.security.SecureRandom;
import java.util.regex.Pattern;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "speak.db";
    private static final int DATABASE_VERSION = 9;
    private Context context;

    // Nombres y columnas para la tabla de usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_SYNC_STATUS = "sync_status"; // 0: no sincronizado, 1: sincronizado
    public static final String COLUMN_LAST_LOGIN = "last_login";
    public static final String COLUMN_AUTH_TOKEN = "auth_token";
    public static final String COLUMN_OFFLINE_MODE = "offline_mode";
    public static final String COLUMN_DEVICE_ID = "device_id";
    public static final String COLUMN_IS_GUEST = "is_guest";

    // Tabla de respuestas de pronunciación
    public static final String TABLE_PRONUNCIATION = "pronunciation_results";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_REFERENCE_TEXT = "reference_text";
    public static final String COLUMN_SPOKEN_TEXT = "spoken_text";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_TOPIC = "topic";
    public static final String COLUMN_LEVEL = "level";

    // Tabla para resultados del quiz
    public static final String TABLE_QUIZ = "quiz_results";
    public static final String COLUMN_QUESTION = "question";
    public static final String COLUMN_CORRECT_ANSWER = "correct_answer";
    public static final String COLUMN_USER_ANSWER = "user_answer";
    public static final String COLUMN_IS_CORRECT = "is_correct";
    public static final String COLUMN_QUIZ_TYPE = "quiz_type";

    // Tabla para respuestas de escucha
    public static final String TABLE_LISTENING = "listening_answers";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_PITCH = "pitch";

    // Tabla para resultados de escritura
    public static final String TABLE_WRITING = "writing_results";
    public static final String COLUMN_WRITING_ID = "id";
    public static final String COLUMN_WRITING_USER_ID = "user_id";
    public static final String COLUMN_WRITING_QUESTION = "question";
    public static final String COLUMN_WRITING_USER_ANSWER = "user_answer";
    public static final String COLUMN_WRITING_IS_CORRECT = "is_correct";
    public static final String COLUMN_WRITING_SIMILARITY = "similarity";
    public static final String COLUMN_WRITING_TIMESTAMP = "timestamp";

    // Tabla para resultados de reading
    public static final String TABLE_READING = "reading_results";
    public static final String COLUMN_READING_ID = "id";
    public static final String COLUMN_READING_USER_ID = "user_id";
    public static final String COLUMN_READING_QUESTION = "question";
    public static final String COLUMN_READING_CORRECT_ANSWER = "correct_answer";
    public static final String COLUMN_READING_USER_ANSWER = "user_answer";
    public static final String COLUMN_READING_IS_CORRECT = "is_correct";
    public static final String COLUMN_READING_TIMESTAMP = "timestamp";
    public static final String COLUMN_READING_TOPIC = "topic";
    public static final String COLUMN_READING_LEVEL = "level";

    // Tabla para temas y niveles de pronunciación
    public static final String TABLE_PRONUNCIATION_TOPICS = "pronunciation_topics";
    public static final String COLUMN_TOPIC_ID = "topic_id";
    public static final String COLUMN_TOPIC_NAME = "topic_name";
    public static final String COLUMN_TOPIC_LEVEL = "topic_level";
    public static final String COLUMN_TOPIC_DESCRIPTION = "topic_description";
    public static final String COLUMN_TOPIC_QUESTIONS = "topic_questions";

    // SQL para crear tabla de pronunciación
    private static final String CREATE_TABLE_PRONUNCIATION = 
        "CREATE TABLE " + TABLE_PRONUNCIATION + "(" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_REFERENCE_TEXT + " TEXT NOT NULL, " +
        COLUMN_SPOKEN_TEXT + " TEXT NOT NULL, " +
        COLUMN_SCORE + " REAL NOT NULL, " +
        COLUMN_SYNC_STATUS + " INTEGER DEFAULT 0, " +
        COLUMN_TOPIC + " TEXT, " +
        COLUMN_LEVEL + " TEXT, " +
        COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    // SQL para crear tabla de resultados del quiz
    private static final String CREATE_TABLE_QUIZ = 
        "CREATE TABLE " + TABLE_QUIZ + "(" +
                COLUMN_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_QUESTION + " TEXT NOT NULL, " +
        COLUMN_CORRECT_ANSWER + " TEXT NOT NULL, " +
        COLUMN_USER_ANSWER + " TEXT NOT NULL, " +
        COLUMN_IS_CORRECT + " INTEGER NOT NULL, " +
        COLUMN_QUIZ_TYPE + " TEXT NOT NULL, " +
        COLUMN_TOPIC + " TEXT, " +
        COLUMN_LEVEL + " TEXT, " +
        COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    // SQL para crear tabla de usuarios
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, "
            + COLUMN_PASSWORD + " TEXT NOT NULL, "
            + COLUMN_SYNC_STATUS + " INTEGER DEFAULT 0, "
            + COLUMN_LAST_LOGIN + " DATETIME, "
            + COLUMN_AUTH_TOKEN + " TEXT, "
            + COLUMN_OFFLINE_MODE + " INTEGER DEFAULT 0, "
            + COLUMN_DEVICE_ID + " TEXT UNIQUE, "
            + COLUMN_IS_GUEST + " INTEGER DEFAULT 0)";

    // SQL para crear tabla de respuestas de escucha
    private static final String CREATE_TABLE_LISTENING = "CREATE TABLE " + TABLE_LISTENING + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_ID + " INTEGER NOT NULL, " +
            COLUMN_QUESTION + " TEXT NOT NULL, " +
            COLUMN_CORRECT_ANSWER + " TEXT NOT NULL, " +
            COLUMN_USER_ANSWER + " TEXT NOT NULL, " +
            COLUMN_IS_CORRECT + " INTEGER NOT NULL, " +
            COLUMN_SPEED + " REAL NOT NULL, " +
            COLUMN_PITCH + " REAL NOT NULL, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL)";

    // SQL para crear tabla de resultados de escritura
    private static final String CREATE_TABLE_WRITING = 
        "CREATE TABLE " + TABLE_WRITING + " (" +
        COLUMN_WRITING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_WRITING_USER_ID + " TEXT, " +
        COLUMN_WRITING_QUESTION + " TEXT, " +
        COLUMN_WRITING_USER_ANSWER + " TEXT, " +
        COLUMN_WRITING_IS_CORRECT + " INTEGER, " +
        COLUMN_WRITING_SIMILARITY + " REAL, " +
                    COLUMN_WRITING_TIMESTAMP + " INTEGER, " +
                    COLUMN_TOPIC + " TEXT, " +
                    COLUMN_LEVEL + " TEXT" +
                    ")";

    // SQL para crear tabla de resultados de reading
    private static final String CREATE_TABLE_READING = 
        "CREATE TABLE " + TABLE_READING + " (" +
        COLUMN_READING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_READING_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_READING_QUESTION + " TEXT NOT NULL, " +
        COLUMN_READING_CORRECT_ANSWER + " TEXT NOT NULL, " +
        COLUMN_READING_USER_ANSWER + " TEXT NOT NULL, " +
        COLUMN_READING_IS_CORRECT + " INTEGER NOT NULL, " +
        COLUMN_READING_TIMESTAMP + " INTEGER NOT NULL, " +
        COLUMN_READING_TOPIC + " TEXT, " +
        COLUMN_READING_LEVEL + " TEXT)";

    // SQL para crear tabla de temas de pronunciación
    private static final String CREATE_TABLE_PRONUNCIATION_TOPICS = 
        "CREATE TABLE " + TABLE_PRONUNCIATION_TOPICS + "(" +
        COLUMN_TOPIC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TOPIC_NAME + " TEXT NOT NULL, " +
        COLUMN_TOPIC_LEVEL + " TEXT NOT NULL, " +
        COLUMN_TOPIC_DESCRIPTION + " TEXT, " +
        COLUMN_TOPIC_QUESTIONS + " TEXT NOT NULL)";

    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    private static final String GUEST_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int GUEST_PASSWORD_LENGTH = 6;
    
    public static boolean isValidPassword(String password) {
        return pattern.matcher(password).matches();
    }
    
    private String generateGuestPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < GUEST_PASSWORD_LENGTH; i++) {
            int index = random.nextInt(GUEST_PASSWORD_CHARS.length());
            password.append(GUEST_PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // Se llama la primera vez que se crea la base de datos
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_PRONUNCIATION);
        db.execSQL(CREATE_TABLE_QUIZ);
        db.execSQL(CREATE_TABLE_LISTENING);
        db.execSQL(CREATE_TABLE_WRITING);
        db.execSQL(CREATE_TABLE_READING);
        db.execSQL(CREATE_TABLE_PRONUNCIATION_TOPICS);
    }

    // Se llama cuando se actualiza la versión de la base de datos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Agregar la columna quiz_type a la tabla quiz_results
            db.execSQL("ALTER TABLE " + TABLE_QUIZ + " ADD COLUMN " + COLUMN_QUIZ_TYPE + " TEXT NOT NULL DEFAULT 'Quiz'");
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_TABLE_PRONUNCIATION_TOPICS);
        }
        if (oldVersion < 8) {
            // Eliminar y recrear la tabla writing_results
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WRITING);
            db.execSQL(CREATE_TABLE_WRITING);
        }
        if (oldVersion < 9) {
            // Crear la tabla reading_results
            db.execSQL(CREATE_TABLE_READING);
        }
    }

    // Guarda un nuevo usuario
    public long saveUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_SYNC_STATUS, 0);
        values.put(COLUMN_LAST_LOGIN, System.currentTimeMillis());
        values.put(COLUMN_OFFLINE_MODE, 1); // por defecto en modo offline

        try {
            long id = db.insertOrThrow(TABLE_USERS, null, values);
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving user: " + e.getMessage());
            return -1;
        }
    }

    // Verifica si un usuario existe con email y contraseña
    // El método es público, devuelve un booleano (verdadero o falso)
    public boolean checkUser(String email, String password) {
        //Obtiene una instancia de la base de datos en modo solo lectura.
        SQLiteDatabase db = this.getReadableDatabase();
        //Define las columnas que se quieren recuperar del usuario: ID y modo sin conexión (offline).
        String[] columns = {COLUMN_ID, COLUMN_OFFLINE_MODE};
        //Construye una cláusula WHERE segura (evita inyecciones SQL).
        // Busca un usuario donde el correo y la contraseña coincidan con los introducidos.
        String selection = COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?";
        String[] selectionArgs = {email, password};

        //Ejecuta la consulta SQL. El resultado es un Cursor que apunta a los datos obtenidos.
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        //Comprueba si hay al menos un resultado (es decir, si el usuario existe con esos datos).
        if (cursor.moveToFirst()) {

            // Obtener el ID del usuario
            long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));

            // Guardar el ID en SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("user_id", userId);
            editor.apply();

            //Obtiene el valor del campo offlineMode del usuario
            // (aunque en este método no lo usa más adelante; quizás es para fines futuros).
            int offlineMode = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_OFFLINE_MODE));
            //Cierra el cursor (buena práctica para liberar recursos).
            cursor.close();

            //Llama a otro método llamado updateLastLogin(email)
            // para actualizar la fecha del último inicio de sesión del usuario.
            updateLastLogin(email);

            //usuario válido
            return true;
        }

        //Si no se encuentra ningún usuario, también se cierra el cursor y se devuelve false.
        cursor.close();
        return false;
    }

    // Obtiene el ID del usuario según su email
    public long getUserId(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    // Modificar el método savePronunciationResult para incluir tema y nivel
    public long savePronunciationResult(long userId, String referenceText, String spokenText, 
                                      double score, String topic, String level) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_REFERENCE_TEXT, referenceText);
        values.put(COLUMN_SPOKEN_TEXT, spokenText);
        values.put(COLUMN_SCORE, score);
        values.put(COLUMN_SYNC_STATUS, 0);
        values.put(COLUMN_TOPIC, topic);
        values.put(COLUMN_LEVEL, level);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        return db.insert(TABLE_PRONUNCIATION, null, values);
    }

    // Sobrecarga: guardar resultado de pronunciación con timestamp de sesión
    public long savePronunciationResult(long userId, String referenceText, String spokenText,
                                       double score, String topic, String level, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_REFERENCE_TEXT, referenceText);
        values.put(COLUMN_SPOKEN_TEXT, spokenText);
        values.put(COLUMN_SCORE, score);
        values.put(COLUMN_SYNC_STATUS, 0);
        values.put(COLUMN_TOPIC, topic);
        values.put(COLUMN_LEVEL, level);
        values.put(COLUMN_TIMESTAMP, timestamp);
        return db.insert(TABLE_PRONUNCIATION, null, values);
    }

    // Método para obtener el historial de pronunciación por tema
    public Cursor getPronunciationHistoryByTopic(String topic, String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_TOPIC + "=? AND " + COLUMN_LEVEL + "=?";
        String[] selectionArgs = {topic, level};
        return db.query(TABLE_PRONUNCIATION, null, selection, selectionArgs, 
                       null, null, COLUMN_TIMESTAMP + " DESC");
    }

    // Método para obtener el promedio de puntuación por tema
    public double getTopicAverageScore(String topic, String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_TOPIC + "=? AND " + COLUMN_LEVEL + "=?";
        String[] selectionArgs = {topic, level};
        
        Cursor cursor = db.query(TABLE_PRONUNCIATION, 
                               new String[]{"AVG(" + COLUMN_SCORE + ") as avg_score"}, 
                               selection, selectionArgs, null, null, null);
        
        double average = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            average = cursor.getDouble(cursor.getColumnIndex("avg_score"));
            cursor.close();
        }
        return average;
    }

    // Métodos para guardar resultados de quiz
    public void saveQuizResult(long userId, String question, String correctAnswer, 
                             String userAnswer, boolean isCorrect, String quizType,
                             String topic, String level, long sessionTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_CORRECT_ANSWER, correctAnswer);
        values.put(COLUMN_USER_ANSWER, userAnswer);
        values.put(COLUMN_IS_CORRECT, isCorrect ? 1 : 0);
        values.put(COLUMN_TIMESTAMP, sessionTimestamp);
        values.put(COLUMN_QUIZ_TYPE, quizType);
        values.put(COLUMN_TOPIC, topic);
        values.put(COLUMN_LEVEL, level);
        
        db.insert(TABLE_QUIZ, null, values);
    }

    // Método específico para el módulo de Image Identification Audio
    public void saveImageQuizResult(long userId, String topic, String level, String quizType, 
                                   int score, int totalQuestions, double percentage, long sessionTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_QUESTION, "Quiz completed");
        values.put(COLUMN_CORRECT_ANSWER, "Score: " + score + "/" + totalQuestions);
        values.put(COLUMN_USER_ANSWER, "Percentage: " + String.format("%.1f%%", percentage));
        values.put(COLUMN_IS_CORRECT, percentage >= 70 ? 1 : 0);
        values.put(COLUMN_QUIZ_TYPE, quizType);
        values.put(COLUMN_TOPIC, topic);
        values.put(COLUMN_LEVEL, level);
        values.put(COLUMN_TIMESTAMP, sessionTimestamp);

        try {
            db.insert(TABLE_QUIZ, null, values);
            Log.d("DatabaseHelper", "Image quiz result saved successfully");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving image quiz result: " + e.getMessage());
        }
    }

    // Método para verificar si una tabla existe
    private boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Método para obtener el último resultado de quiz de un usuario
    public Cursor getLastQuizResult(long userId) {
        SQLiteDatabase db = null;
        try {
            db = this.getReadableDatabase();
            String[] columns = {COLUMN_ID, COLUMN_USER_ID, COLUMN_IS_CORRECT};
            String selection = COLUMN_USER_ID + "=?";
            String[] selectionArgs = {String.valueOf(userId)};
            String orderBy = COLUMN_ID + " DESC";
            String limit = "1";
            return db.query(TABLE_QUIZ, columns, selection, selectionArgs, null, null, orderBy, limit);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener último resultado del quiz: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    // Nuevo método para verificar si existe un usuario por ID
    public boolean userExistsById(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_ID + "=?";
        String[] selectionArgs = {String.valueOf(userId)};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Comprueba si un usuario ha sido sincronizado
    public boolean isUserSynced(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_SYNC_STATUS};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS));
            cursor.close();
            return syncStatus == 1;
        }
        cursor.close();
        return false;
    }

    // Marca a un usuario como sincronizado
    public void markUserAsSynced(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNC_STATUS, 1);
        String whereClause = COLUMN_EMAIL + "=?";
        String[] whereArgs = {email};
        db.update(TABLE_USERS, values, whereClause, whereArgs);
    }

    // Verifica si un usuario existe por email
    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Limpieza de recursos
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    // Cierra conexiones abiertas
    @Override
    public synchronized void close() {
        try {
            if (getWritableDatabase().isOpen()) {
                getWritableDatabase().close();
            }
            if (getReadableDatabase().isOpen()) {
                getReadableDatabase().close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al cerrar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
        super.close();
    }

    // Obtener historial de pronunciación
    public Cursor getPronunciationHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_ID + "=?";
        String[] selectionArgs = {String.valueOf(getCurrentUserId())};
        return db.query(TABLE_PRONUNCIATION,
            null,
            null,
            null,
            null,
            null,
            COLUMN_TIMESTAMP + " DESC");
    }

    // Método para obtener el historial de quiz
    public Cursor getQuizHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_QUIZ, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
    }

    // Metodo, verifica si la sesión offline sigue siendo válida (menos de 24h desde último login)
    public boolean checkOfflineSession(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_LAST_LOGIN, COLUMN_OFFLINE_MODE};
        String selection = COLUMN_EMAIL + "=?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        if (cursor.moveToFirst()) {
            long lastLogin = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN));
            int offlineMode = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_OFFLINE_MODE));
            cursor.close();

            // Verificar si la sesión es válida (24 horas)
            long sessionTimeout = 24 * 60 * 60 * 1000; // 24 horas en milisegundos
            boolean isSessionValid = (System.currentTimeMillis() - lastLogin) < sessionTimeout;
            
            return isSessionValid && offlineMode == 1;
        }
        
        cursor.close();
        return false;
    }

    // Nuevo método para obtener resultados de pronunciación no sincronizados
    public Cursor getUnsyncedPronunciationResults() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_SYNC_STATUS + "=?";
        String[] selectionArgs = {"0"};
        return db.query(TABLE_PRONUNCIATION,
            null,
            selection,
            selectionArgs,
            null,
            null,
            COLUMN_TIMESTAMP + " DESC");
    }

    // Marca un resultado de pronunciación como sincronizado
    public void markPronunciationResultAsSynced(long resultId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNC_STATUS, 1);
        String whereClause = COLUMN_ID + "=?";
        String[] whereArgs = {String.valueOf(resultId)};
        db.update(TABLE_PRONUNCIATION, values, whereClause, whereArgs);
    }

    // Nuevo método para actualizar último login
    private void updateLastLogin(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_LOGIN, System.currentTimeMillis());
        String whereClause = COLUMN_EMAIL + "=?";
        String[] whereArgs = {email};
        db.update(TABLE_USERS, values, whereClause, whereArgs);
    }

    // Activa o desactiva el modo offline del usuario
    public void setUserOfflineMode(String email, boolean isOffline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OFFLINE_MODE, isOffline ? 1 : 0);
        db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{email});
        db.close();
    }

    public boolean isGuestUserExists(String deviceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_DEVICE_ID + " = ? AND " + COLUMN_IS_GUEST + " = 1",
                new String[]{deviceId},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long createGuestUser(String deviceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String guestEmail = "guest_" + deviceId + "@speak.app";
        String guestPassword = generateGuestPassword();
        
        values.put(COLUMN_EMAIL, guestEmail);
        values.put(COLUMN_PASSWORD, guestPassword);
        values.put(COLUMN_DEVICE_ID, deviceId);
        values.put(COLUMN_IS_GUEST, 1);
        values.put(COLUMN_OFFLINE_MODE, 1);
        
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public Cursor getGuestUser(String deviceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS,
                new String[]{COLUMN_ID, COLUMN_EMAIL, COLUMN_PASSWORD},
                COLUMN_DEVICE_ID + " = ? AND " + COLUMN_IS_GUEST + " = 1",
                new String[]{deviceId},
                null, null, null);
    }

    public void saveListeningAnswer(long userId, String question, String correctAnswer, 
            String selectedAnswer, boolean isCorrect, float speed, float pitch) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Verificar si el usuario existe
        if (!userExistsById(userId)) {
            Log.e("DatabaseHelper", "Error saving listening answer: User does not exist");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_CORRECT_ANSWER, correctAnswer);
        values.put(COLUMN_USER_ANSWER, selectedAnswer);
        values.put(COLUMN_IS_CORRECT, isCorrect ? 1 : 0);
        values.put(COLUMN_SPEED, speed);
        values.put(COLUMN_PITCH, pitch);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        
        long result = db.insert(TABLE_LISTENING, null, values);
        if (result == -1) {
            Log.e("DatabaseHelper", "Error saving listening answer");
        } else {
            Log.d("DatabaseHelper", "Listening answer saved successfully with ID: " + result);
        }
    }

    public List<Map<String, Object>> getListeningAnswers(long userId) {
        List<Map<String, Object>> answers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Verificar si el usuario existe
        if (!userExistsById(userId)) {
            Log.e("DatabaseHelper", "Error getting listening answers: User does not exist");
            return answers;
        }

        Cursor cursor = db.query(TABLE_LISTENING, null, COLUMN_USER_ID + " = ?", 
            new String[]{String.valueOf(userId)}, null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> answer = new HashMap<>();
                answer.put("question", cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION)));
                answer.put("correct_answer", cursor.getString(cursor.getColumnIndex(COLUMN_CORRECT_ANSWER)));
                answer.put("selected_answer", cursor.getString(cursor.getColumnIndex(COLUMN_USER_ANSWER)));
                answer.put("is_correct", cursor.getInt(cursor.getColumnIndex(COLUMN_IS_CORRECT)) == 1);
                answer.put("speed", cursor.getFloat(cursor.getColumnIndex(COLUMN_SPEED)));
                answer.put("pitch", cursor.getFloat(cursor.getColumnIndex(COLUMN_PITCH)));
                answer.put("timestamp", cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                answers.add(answer);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return answers;
    }

    public void saveWritingResult(String userId, String question, String userAnswer, 
            boolean isCorrect, double similarity, String topic, String level) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WRITING_USER_ID, userId);
        values.put(COLUMN_WRITING_QUESTION, question);
        values.put(COLUMN_WRITING_USER_ANSWER, userAnswer);
        values.put(COLUMN_WRITING_IS_CORRECT, isCorrect ? 1 : 0);
        values.put(COLUMN_WRITING_SIMILARITY, similarity);
        values.put(COLUMN_WRITING_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_TOPIC, topic);
        values.put(COLUMN_LEVEL, level);

        try {
        db.insert(TABLE_WRITING, null, values);
            Log.d("DatabaseHelper", "Writing result saved successfully");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving writing result: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getWritingResults(String userId) {
        List<Map<String, Object>> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WRITING, null, COLUMN_WRITING_USER_ID + " = ?", 
            new String[]{userId}, null, null, COLUMN_WRITING_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> result = new HashMap<>();
                result.put("question", cursor.getString(cursor.getColumnIndex(COLUMN_WRITING_QUESTION)));
                result.put("userAnswer", cursor.getString(cursor.getColumnIndex(COLUMN_WRITING_USER_ANSWER)));
                result.put("isCorrect", cursor.getInt(cursor.getColumnIndex(COLUMN_WRITING_IS_CORRECT)) == 1);
                result.put("similarity", cursor.getDouble(cursor.getColumnIndex(COLUMN_WRITING_SIMILARITY)));
                result.put("timestamp", cursor.getLong(cursor.getColumnIndex(COLUMN_WRITING_TIMESTAMP)));
                results.add(result);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    // Métodos para manejar temas de pronunciación
    public long savePronunciationTopic(String name, String level, String description, String questions) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TOPIC_NAME, name);
        values.put(COLUMN_TOPIC_LEVEL, level);
        values.put(COLUMN_TOPIC_DESCRIPTION, description);
        values.put(COLUMN_TOPIC_QUESTIONS, questions);
        return db.insert(TABLE_PRONUNCIATION_TOPICS, null, values);
    }

    public Cursor getPronunciationTopics(String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_TOPIC_LEVEL + "=?";
        String[] selectionArgs = {level};
        return db.query(TABLE_PRONUNCIATION_TOPICS, null, selection, selectionArgs, 
                       null, null, COLUMN_TOPIC_NAME + " ASC");
    }

    public Cursor getPronunciationTopic(String topicId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_TOPIC_ID + "=?";
        String[] selectionArgs = {topicId};
        return db.query(TABLE_PRONUNCIATION_TOPICS, null, selection, selectionArgs, 
                       null, null, null);
    }

    /**
     * Carga los temas de pronunciación desde el archivo de assets
     * @param context El contexto de la aplicación
     */
    public void loadTopicsFromFile(Context context) {
        try {
            InputStream is = context.getAssets().open("pronunciation_topics.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            String currentTopic = "";
            String currentLevel = "";
            String currentDescription = "";
            List<String> currentQuestions = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Topic:")) {
                    // Guardar el tema anterior si existe
                    if (!currentTopic.isEmpty() && !currentQuestions.isEmpty()) {
                        savePronunciationTopic(
                            currentTopic,
                            currentLevel,
                            currentDescription,
                            String.join(",", currentQuestions)
                        );
                    }
                    // Iniciar nuevo tema
                    currentTopic = line.substring(6).trim();
                    currentQuestions.clear();
                } else if (line.startsWith("Level:")) {
                    currentLevel = line.substring(6).trim();
                } else if (line.startsWith("Description:")) {
                    currentDescription = line.substring(12).trim();
                } else if (line.startsWith("Questions:")) {
                    String[] questions = line.substring(10).trim().split(",");
                    for (String question : questions) {
                        currentQuestions.add(question.trim());
                    }
                }
            }

            // Guardar el último tema
            if (!currentTopic.isEmpty() && !currentQuestions.isEmpty()) {
                savePronunciationTopic(
                    currentTopic,
                    currentLevel,
                    currentDescription,
                    String.join(",", currentQuestions)
                );
            }

            reader.close();
            is.close();
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error loading pronunciation topics", e);
        }
    }

    // Método para obtener el ID del usuario actual desde SharedPreferences
    public long getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("user_id", -1);
    }

    // Métodos específicos para el nuevo módulo de pronunciación
    public long getPronunciationUserId() {
        SharedPreferences prefs = context.getSharedPreferences("pronunciation_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("pronunciation_user_id", -1);
    }

    public void savePronunciationUserId(long userId) {
        SharedPreferences prefs = context.getSharedPreferences("pronunciation_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("pronunciation_user_id", userId);
        editor.apply();
    }

    public long getGuestUserId(String deviceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        long userId = -1;
        
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_DEVICE_ID + " = ? AND " + COLUMN_IS_GUEST + " = 1",
                new String[]{deviceId},
                null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
            cursor.close();
        }
        
        return userId;
    }

    public Cursor getQuizResultsBySession(long userId, String quizType, long sessionTimestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {
            COLUMN_QUESTION,
            COLUMN_CORRECT_ANSWER,
            COLUMN_USER_ANSWER,
            COLUMN_IS_CORRECT,
            COLUMN_TOPIC,
            COLUMN_LEVEL
        };
        
        String selection = COLUMN_USER_ID + " = ? AND " + 
                         COLUMN_QUIZ_TYPE + " = ? AND " + 
                         COLUMN_TIMESTAMP + " = ?";
        String[] selectionArgs = {
            String.valueOf(userId),
            quizType,
            String.valueOf(sessionTimestamp)
        };
        
        return db.query(TABLE_QUIZ, columns, selection, selectionArgs, null, null, null);
    }

    // Método para marcar un tema como completado
    public void markTopicAsPassed(long userId, String topic, String level) {
        try {
            // Guardar en SharedPreferences para compatibilidad con el sistema existente
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String key = "PASSED_" + topic.toUpperCase().replace(" ", "_");
            editor.putBoolean(key, true);
            editor.apply();
            
            Log.d("DatabaseHelper", "Topic marked as passed: " + topic + " for user: " + userId);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error marking topic as passed: " + e.getMessage());
        }
    }

    // Métodos para manejar resultados de reading
    public void saveReadingResult(long userId, String question, String correctAnswer, 
                                 String userAnswer, boolean isCorrect, String topic, String level) {
        saveReadingResult(userId, question, correctAnswer, userAnswer, isCorrect, topic, level, System.currentTimeMillis());
    }

    public void saveReadingResult(long userId, String question, String correctAnswer, 
                                 String userAnswer, boolean isCorrect, String topic, String level, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_READING_USER_ID, userId);
        values.put(COLUMN_READING_QUESTION, question);
        values.put(COLUMN_READING_CORRECT_ANSWER, correctAnswer);
        values.put(COLUMN_READING_USER_ANSWER, userAnswer);
        values.put(COLUMN_READING_IS_CORRECT, isCorrect ? 1 : 0);
        values.put(COLUMN_READING_TIMESTAMP, timestamp);
        values.put(COLUMN_READING_TOPIC, topic);
        values.put(COLUMN_READING_LEVEL, level);

        try {
            db.insert(TABLE_READING, null, values);
            Log.d("DatabaseHelper", "Reading result saved successfully with timestamp: " + timestamp);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving reading result: " + e.getMessage());
        }
    }

    public Cursor getReadingHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_READING_USER_ID + "=?";
        String[] selectionArgs = {String.valueOf(getCurrentUserId())};
        return db.query(TABLE_READING,
            null,
            selection,
            selectionArgs,
            null,
            null,
            COLUMN_READING_TIMESTAMP + " DESC");
    }

    public Cursor getReadingHistoryByTopic(String topic, String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_READING_USER_ID + "=? AND " + 
                          COLUMN_READING_TOPIC + "=? AND " + 
                          COLUMN_READING_LEVEL + "=?";
        String[] selectionArgs = {String.valueOf(getCurrentUserId()), topic, level};
        return db.query(TABLE_READING, null, selection, selectionArgs, 
                       null, null, COLUMN_READING_TIMESTAMP + " DESC");
    }

}
