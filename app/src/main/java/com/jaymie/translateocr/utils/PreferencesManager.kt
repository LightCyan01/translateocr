package com.jaymie.translateocr.utils

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Add configuration change handling
    private var lastConfigChange = 0L
    private val configChangeThreshold = 500L

    companion object {
        private const val PREFS_NAME = "translate_ocr_prefs"
        private const val GOOGLE_API_KEY = "google_api_key"
        private const val DEEPL_API_KEY = "deepl_api_key"
        private const val KEY_USERNAME = "username"
        private const val KEY_PROFILE_PICTURE = "profile_picture"
        private const val KEY_TRANSLATED_WORDS = "translated_words"
    }

    fun saveGoogleApiKey(key: String) {
        if (isConfigurationChange()) return
        prefs.edit().putString(GOOGLE_API_KEY, key).apply()
    }

    fun saveDeepLApiKey(key: String) {
        if (isConfigurationChange()) return
        prefs.edit().putString(DEEPL_API_KEY, key).apply()
    }

    private fun isConfigurationChange(): Boolean {
        val currentTime = System.currentTimeMillis()
        val isConfigChange = (currentTime - lastConfigChange) < configChangeThreshold
        lastConfigChange = currentTime
        return isConfigChange
    }

    fun getGoogleApiKey(): String? = prefs.getString(GOOGLE_API_KEY, null)
    fun getDeepLApiKey(): String? = prefs.getString(DEEPL_API_KEY, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    
    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getProfilePictureUri(): String? = prefs.getString(KEY_PROFILE_PICTURE, null)
    
    fun saveProfilePictureUri(uri: String) {
        prefs.edit().putString(KEY_PROFILE_PICTURE, uri).apply()
    }

    fun getTranslatedWordsCount(): Int = prefs.getInt(KEY_TRANSLATED_WORDS, 0)
    
    fun incrementTranslatedWords(count: Int = 1) {
        val currentCount = getTranslatedWordsCount()
        prefs.edit().putInt(KEY_TRANSLATED_WORDS, currentCount + count).apply()
    }
} 