package com.wahab.pengamatbaterai

import android.content.Context

object Prefs {
    private const val NAME = "pengamat_baterai_prefs"

    private const val KEY_MAX_PERCENT = "max_percent"
    private const val KEY_MIN_PERCENT = "min_percent"
    private const val KEY_MAX_TEXT = "max_text"
    private const val KEY_MIN_TEXT = "min_text"
    private const val KEY_ENABLED = "enabled"

    const val DEFAULT_MAX_PERCENT = 100
    const val DEFAULT_MIN_PERCENT = 15
    const val DEFAULT_MAX_TEXT = "Baterai sudah penuh"
    const val DEFAULT_MIN_TEXT = "Baterai lemah, segera diisi"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getMaxPercent(context: Context): Int =
        prefs(context).getInt(KEY_MAX_PERCENT, DEFAULT_MAX_PERCENT)

    fun getMinPercent(context: Context): Int =
        prefs(context).getInt(KEY_MIN_PERCENT, DEFAULT_MIN_PERCENT)

    fun getMaxText(context: Context): String =
        prefs(context).getString(KEY_MAX_TEXT, DEFAULT_MAX_TEXT) ?: DEFAULT_MAX_TEXT

    fun getMinText(context: Context): String =
        prefs(context).getString(KEY_MIN_TEXT, DEFAULT_MIN_TEXT) ?: DEFAULT_MIN_TEXT

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun save(
        context: Context,
        maxPercent: Int,
        minPercent: Int,
        maxText: String,
        minText: String,
        enabled: Boolean
    ) {
        prefs(context).edit()
            .putInt(KEY_MAX_PERCENT, maxPercent)
            .putInt(KEY_MIN_PERCENT, minPercent)
            .putString(KEY_MAX_TEXT, maxText)
            .putString(KEY_MIN_TEXT, minText)
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
