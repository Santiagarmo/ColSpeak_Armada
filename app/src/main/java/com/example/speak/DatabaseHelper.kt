package com.example.speak;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "SpeakApp.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "email TEXT UNIQUE NOT NULL," +
            "password TEXT NOT NULL," +
            "sync_status INTEGER DEFAULT 0," +
            "last_login DATETIME," +
            "auth_token TEXT," +
            "offline_mode INTEGER DEFAULT 0," +
            "device_id TEXT UNIQUE," +
            "is_guest INTEGER DEFAULT 0" +
            ")");

        // Create listening_answers table
        db.execSQL("CREATE TABLE IF NOT EXISTS listening_answers (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id TEXT," +
            "question TEXT," +
            "correct_answer TEXT," +
            "selected_answer TEXT," +
            "is_correct INTEGER," +
            "speed REAL," +
            "pitch REAL," +
            "timestamp INTEGER" +
            ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS listening_answers");
        onCreate(db);
    }

    public long getUserId(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"id"};
        String selection = "email = ?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query("users", columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndex("id"));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    public void saveListeningAnswer(String userId, String question, String correctAnswer, 
            String selectedAnswer, boolean isCorrect, float speed, float pitch) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("question", question);
        values.put("correct_answer", correctAnswer);
        values.put("selected_answer", selectedAnswer);
        values.put("is_correct", isCorrect ? 1 : 0);
        values.put("speed", speed);
        values.put("pitch", pitch);
        values.put("timestamp", System.currentTimeMillis());
        db.insert("listening_answers", null, values);
    }

    public List<Map<String, Object>> getListeningAnswers(String userId) {
        List<Map<String, Object>> answers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("listening_answers", null, "user_id = ?", 
            new String[]{userId}, null, null, "timestamp DESC");

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> answer = new HashMap<>();
                answer.put("question", cursor.getString(cursor.getColumnIndex("question")));
                answer.put("correct_answer", cursor.getString(cursor.getColumnIndex("correct_answer")));
                answer.put("selected_answer", cursor.getString(cursor.getColumnIndex("selected_answer")));
                answer.put("is_correct", cursor.getInt(cursor.getColumnIndex("is_correct")) == 1);
                answer.put("speed", cursor.getFloat(cursor.getColumnIndex("speed")));
                answer.put("pitch", cursor.getFloat(cursor.getColumnIndex("pitch")));
                answer.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
                answers.add(answer);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return answers;
    }
} 