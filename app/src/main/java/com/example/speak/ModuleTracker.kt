package com.example.speak

import android.content.Context

/**
 * Helper para almacenar y recuperar el último módulo ("mundo") que el usuario visitó.
 */
object ModuleTracker {
    private const val PREFS = "ModulePrefs"
    private const val KEY_LAST_MODULE = "last_module"

    @JvmStatic
    fun setLastModule(ctx: Context, module: String?) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_MODULE, module).apply()
    }

    fun getLastModule(ctx: Context): String? {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_MODULE, null)
    }

    fun clearLastModule(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_MODULE).apply()
    }
}