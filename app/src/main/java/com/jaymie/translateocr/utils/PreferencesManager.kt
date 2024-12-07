package com.jaymie.translateocr.utils

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "translate_ocr_prefs"
        private const val GOOGLE_API_KEY = "google_api_key"
        private const val DEEPL_API_KEY = "deepl_api_key"
    }

    fun saveGoogleApiKey(key: String) {
        prefs.edit().putString(GOOGLE_API_KEY, key).apply()
    }

    fun saveDeepLApiKey(key: String) {
        prefs.edit().putString(DEEPL_API_KEY, key).apply()
    }

    fun getGoogleApiKey(): String? = prefs.getString(GOOGLE_API_KEY, null)
    fun getDeepLApiKey(): String? = prefs.getString(DEEPL_API_KEY, null)
} 