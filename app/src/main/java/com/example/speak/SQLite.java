package com.example.speak;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.annotation.Nullable;

//Class: Creating a local database
public class SQLite extends SQLiteOpenHelper {
    public SQLite(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int level){
        super(context, name, factory, level);
    }

    //We assign the name to the local database
    @Override
    public void onCreate(SQLiteDatabase ColSpeakDB){
        //We instantiate the database and create a score table
        ColSpeakDB.execSQL("create table scores(name text, score int)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldLevel, int newLevel) {

    }
}
