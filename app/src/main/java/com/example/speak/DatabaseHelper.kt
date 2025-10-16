package com.example.speak

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, "SpeakApp.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "sync_status INTEGER DEFAULT 0," +
                    "last_login DATETIME," +
                    "auth_token TEXT," +
                    "offline_mode INTEGER DEFAULT 0," +
                    "device_id TEXT UNIQUE," +
                    "is_guest INTEGER DEFAULT 0" +
                    ")"
        )

        // Create listening_answers table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS listening_answers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id TEXT," +
                    "question TEXT," +
                    "correct_answer TEXT," +
                    "selected_answer TEXT," +
                    "is_correct INTEGER," +
                    "speed REAL," +
                    "pitch REAL," +
                    "timestamp INTEGER" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS listening_answers")
        onCreate(db)
    }

    fun getUserId(email: String?): Long {
        val db = this.getReadableDatabase()
        val columns = arrayOf<String?>("id")
        val selection = "email = ?"
        val selectionArgs = arrayOf<String?>(email)
        val cursor = db.query("users", columns, selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndex("id"))
            cursor.close()
            return id
        }
        cursor.close()
        return -1
    }

    fun saveListeningAnswer(
        userId: String?, question: String?, correctAnswer: String?,
        selectedAnswer: String?, isCorrect: Boolean, speed: Float, pitch: Float
    ) {
        val db = this.getWritableDatabase()
        val values = ContentValues()
        values.put("user_id", userId)
        values.put("question", question)
        values.put("correct_answer", correctAnswer)
        values.put("selected_answer", selectedAnswer)
        values.put("is_correct", if (isCorrect) 1 else 0)
        values.put("speed", speed)
        values.put("pitch", pitch)
        values.put("timestamp", System.currentTimeMillis())
        db.insert("listening_answers", null, values)
    }

    fun getListeningAnswers(userId: String?): MutableList<MutableMap<String?, Any?>?> {
        val answers: MutableList<MutableMap<String?, Any?>?> =
            ArrayList<MutableMap<String?, Any?>?>()
        val db = this.getReadableDatabase()
        val cursor = db.query(
            "listening_answers", null, "user_id = ?",
            arrayOf<String?>(userId), null, null, "timestamp DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val answer: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                answer.put("question", cursor.getString(cursor.getColumnIndex("question")))
                answer.put(
                    "correct_answer",
                    cursor.getString(cursor.getColumnIndex("correct_answer"))
                )
                answer.put(
                    "selected_answer",
                    cursor.getString(cursor.getColumnIndex("selected_answer"))
                )
                answer.put("is_correct", cursor.getInt(cursor.getColumnIndex("is_correct")) == 1)
                answer.put("speed", cursor.getFloat(cursor.getColumnIndex("speed")))
                answer.put("pitch", cursor.getFloat(cursor.getColumnIndex("pitch")))
                answer.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")))
                answers.add(answer)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return answers
    }
}