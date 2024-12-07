package com.jaymie.translateocr.data.repository

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Repository handling translation operations for different translation services.
 * Supports:
 * - Google Translate (Free and API)
 * - DeepL (Free and API)
 * - ML Kit Offline Translation
 *
 * All translation operations are performed in a coroutine context using [Dispatchers.IO]
 * for network and disk operations.
 */
class TranslationRepository {
    companion object {
        private const val GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2"
        private const val DEEPL_URL = "https://api-free.deepl.com/v2/translate"
        private const val GOOGLE_API_KEY = "AIzaSyBbqNXjC71FoTXuyRrhF-rzLts65qArJyc"
        private const val DEEPL_API_KEY = "09993180-00e7-4bc8-abe3-c63a71e1d05d:fx"
        private const val TEST_TEXT = "Hello"
        private const val TIMEOUT_MS = 10000
    }

    /**
     * Performs offline translation using ML Kit.
     * Uses [kotlinx.coroutines.tasks.await] to handle ML Kit's Tasks API.
     *
     * @param text Text to translate
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @throws Exception if model is not downloaded or translation fails
     */
    suspend fun translateOffline(text: String, sourceLanguage: String, targetLanguage: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage) ?: sourceLanguage)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage) ?: targetLanguage)
            .build()
        val translator = Translation.getClient(options)

        return try {
            // Awaits ML Kit's Task completion
            translator.translate(text).await()
        } catch (e: Exception) {
            throw Exception("Offline translation failed: ${e.message}")
        }
    }

    /**
     * Checks if an ML Kit translation model is downloaded.
     * Executes in the coroutine context provided by the caller.
     *
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @return true if model is downloaded and ready to use
     */
    suspend fun isModelDownloaded(sourceLanguage: String, targetLanguage: String): Boolean {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage) ?: sourceLanguage)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage) ?: targetLanguage)
            .build()
        val translator = Translation.getClient(options)
        return try {
            translator.downloadModelIfNeeded().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Translates text using Google Translate
     * @param text Text to translate
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @param apiKey Optional API key for Google Translate API service
     * @param isApiKeyService Whether this is an API key service request
     */
    suspend fun translateWithGoogle(
        text: String, 
        sourceLanguage: String, 
        targetLanguage: String,
        apiKey: String? = null,
        isApiKeyService: Boolean = false
    ): String {
        val translationKey = if (isApiKeyService) {
            requireNotNull(apiKey) { "Google Translate API key is required" }
            apiKey
        } else {
            GOOGLE_API_KEY
        }
        return performTranslation(text, sourceLanguage, targetLanguage, translationKey)
    }

    /**
     * Translates text using DeepL
     * @param text Text to translate
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @param apiKey Optional API key for DeepL API service
     * @param isApiKeyService Whether this is an API key service request
     */
    suspend fun translateWithDeepL(
        text: String, 
        sourceLanguage: String, 
        targetLanguage: String,
        apiKey: String? = null,
        isApiKeyService: Boolean = false
    ): String {
        val translationKey = if (isApiKeyService) {
            requireNotNull(apiKey) { "DeepL API key is required" }
            apiKey
        } else {
            DEEPL_API_KEY
        }
        return performDeepLTranslation(text, sourceLanguage, targetLanguage, translationKey)
    }

    private suspend fun performTranslation(
        text: String, 
        sourceLanguage: String, 
        targetLanguage: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val requestUrl = "$GOOGLE_TRANSLATE_URL?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("q", text)
            put("source", sourceLanguage.lowercase())
            put("target", targetLanguage.lowercase())
        }

        val connection = createConnection(requestUrl).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            outputStream.use { it.write(jsonBody.toString().toByteArray()) }
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            response.getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText")
        } else {
            throw Exception("HTTP error code: ${connection.responseCode}")
        }
    }

    private suspend fun performDeepLTranslation(
        text: String, 
        sourceLanguage: String, 
        targetLanguage: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val connection = createConnection(DEEPL_URL).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "DeepL-Auth-Key $apiKey")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            doOutput = true

            val postData = buildString {
                append("text=").append(URLEncoder.encode(text, "UTF-8"))
                append("&source_lang=").append(sourceLanguage.uppercase())
                append("&target_lang=").append(targetLanguage.uppercase())
            }
            outputStream.use { it.write(postData.toByteArray()) }
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            response.getJSONArray("translations")
                .getJSONObject(0)
                .getString("text")
        } else {
            throw Exception("HTTP error code: ${connection.responseCode}")
        }
    }

    /**
     * Tests if a Google Translate API key is valid
     */
    suspend fun testGoogleTranslate(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        
        try {
            val requestUrl = "$GOOGLE_TRANSLATE_URL?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("q", TEST_TEXT)
                put("source", "en")
                put("target", "ja")
            }

            val connection = createConnection(requestUrl).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                outputStream.use { it.write(jsonBody.toString().toByteArray()) }
            }

            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Tests if a DeepL API key is valid
     */
    suspend fun testDeepL(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        
        try {
            val connection = createConnection(DEEPL_URL).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "DeepL-Auth-Key $apiKey")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                
                val postData = "text=${URLEncoder.encode(TEST_TEXT, "UTF-8")}&source_lang=EN&target_lang=JA"
                outputStream.use { it.write(postData.toByteArray()) }
            }

            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }

    private fun createConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
    }
}
