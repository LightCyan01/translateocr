package com.jaymie.translateocr.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Manages ML Kit translation model downloads and state persistence.
 * Uses SharedPreferences to track downloaded models across app sessions.
 */
class ModelManager(private val context: Context) {
    private val downloadedModels = mutableSetOf<String>()
    private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

    init {
        // Load previously downloaded models from preferences
        prefs.getStringSet("downloaded_models", null)?.let {
            downloadedModels.addAll(it)
        }
    }

    /**
     * Synchronously checks if a model is marked as downloaded in local storage.
     * This does not verify with ML Kit if the model is actually available.
     * 
     * @param languageCode The language code to check
     * @return true if the model is marked as downloaded
     */
    fun isModelDownloadedSync(languageCode: String): Boolean {
        return downloadedModels.contains(languageCode)
    }

    /**
     * Downloads a translation model for the specified language.
     * Uses [kotlinx.coroutines.tasks.await] to handle ML Kit's Tasks API.
     * Updates local storage on successful download.
     * 
     * @param languageCode The language code to download
     * @return Result indicating success or failure
     */
    suspend fun downloadModel(languageCode: String): Result<Unit> {
        if (!hasStoragePermission()) {
            return Result.failure(Exception("Storage permission required to download models"))
        }

        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(languageCode) ?: languageCode)
                .build()
            val translator = Translation.getClient(options)

            translator.downloadModelIfNeeded().await()
            downloadedModels.add(languageCode)
            saveDownloadedModels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Persists the set of downloaded models to SharedPreferences.
     * Called after successful downloads or when models are removed.
     */
    private fun saveDownloadedModels() {
        prefs.edit().putStringSet("downloaded_models", downloadedModels.toSet()).apply()
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // No storage permission needed for Android 13+
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}