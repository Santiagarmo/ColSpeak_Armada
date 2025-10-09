package com.example.speak;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper para almacenar y recuperar el último módulo ("mundo") que el usuario visitó.
 */
public class ModuleTracker {
    private static final String PREFS = "ModulePrefs";
    private static final String KEY_LAST_MODULE = "last_module";

    public static void setLastModule(Context ctx, String module) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_MODULE, module).apply();
    }

    public static String getLastModule(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_MODULE, null);
    }

    public static void clearLastModule(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LAST_MODULE).apply();
    }
} 