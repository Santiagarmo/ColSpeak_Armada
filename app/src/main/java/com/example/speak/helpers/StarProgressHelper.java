package com.example.speak.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class StarProgressHelper {

    private static final String TAG = "StarProgressHelper";
    private static final String PREFS_NAME = "TrophyPrefs";
    private static final String KEY_STAR_POINTS = "star_points"; // puntos acumulados (10 por sesión)

    // Retorna puntos acumulados
    public static int getStarPoints(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_STAR_POINTS, 0);
    }

    // Agrega puntos por sesión (normalmente 10 si aprueba)
    public static void addSessionPoints(Context context, int points) {
        if (points <= 0) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_STAR_POINTS, 0);
        int updated = current + points;
        prefs.edit().putInt(KEY_STAR_POINTS, updated).apply();
        Log.d(TAG, "Star points updated: " + current + " -> " + updated);
    }

    // Estrellas contando sesiones (1 estrella = 10 puntos)
    public static int getStarCount(Context context) {
        return getStarPoints(context) / 10;
    }

    // Trofeos por estrellas (1 trofeo = 5 estrellas = 50 puntos)
    public static int getTrophyCountFromStars(Context context) {
        return getStarPoints(context) / 50;
    }
}


