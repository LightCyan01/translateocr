package com.jaymie.translateocr.data.model

/**
 * Enum representing available translation services
 * @property displayName The user-friendly name of the service
 */
enum class TranslationService(val displayName: String) {
    GOOGLE_TRANSLATE("Google Translate"),
    GOOGLE_TRANSLATE_API("Google Translate (API Key)"),
    DEEPL("DeepL"),
    DEEPL_API("DeepL (API Key)"),
    OFFLINE("Offline Translation");

    companion object {
        /**
         * Get services that require API keys
         */
        val apiKeyServices = setOf(GOOGLE_TRANSLATE_API, DEEPL_API)

        /**
         * Get services that use default keys
         */
        val defaultServices = setOf(GOOGLE_TRANSLATE, DEEPL)
    }

    /**
     * Check if this service requires an API key
     */
    fun requiresApiKey(): Boolean = this in apiKeyServices

    /**
     * Check if this service uses default key
     */
    fun usesDefaultKey(): Boolean = this in defaultServices

    override fun toString(): String = displayName
}