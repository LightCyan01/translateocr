package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jaymie.translateocr.data.model.SettingsItem
import com.jaymie.translateocr.data.model.SettingsType
import com.jaymie.translateocr.utils.PreferencesManager
import com.jaymie.translateocr.data.repository.TranslationRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing settings and API key validation
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val translationRepository = TranslationRepository()

    private val _settingsItems = MutableLiveData<List<SettingsItem>>()
    val settingsItems: LiveData<List<SettingsItem>> = _settingsItems

    private val _validationResult = MutableLiveData<ValidationResult>()
    val validationResult: LiveData<ValidationResult> = _validationResult

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val items = listOf(
            SettingsItem(
                id = 1,
                title = "Edit Google Translate API Key",
                type = SettingsType.GOOGLE_API_KEY
            ),
            SettingsItem(
                id = 2,
                title = "Edit DeepL API Key",
                type = SettingsType.DEEPL_API_KEY
            )
        )
        _settingsItems.value = items
    }

    /**
     * Validates and saves the API key for the specified service
     * @param type The type of service (Google or DeepL)
     * @param apiKey The API key to validate and save
     */
    fun validateAndSaveApiKey(type: SettingsType, apiKey: String) {
        viewModelScope.launch {
            try {
                _validationResult.value = ValidationResult.Loading

                when (type) {
                    SettingsType.GOOGLE_API_KEY -> validateGoogleApiKey(apiKey)
                    SettingsType.DEEPL_API_KEY -> validateDeepLApiKey(apiKey)
                }
            } catch (e: Exception) {
                _validationResult.value = ValidationResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun validateGoogleApiKey(apiKey: String) {
        if (translationRepository.testGoogleTranslate(apiKey)) {
            preferencesManager.saveGoogleApiKey(apiKey)
            _validationResult.value = ValidationResult.Success
        } else {
            _validationResult.value = ValidationResult.Error("Invalid API key")
        }
    }

    private suspend fun validateDeepLApiKey(apiKey: String) {
        if (translationRepository.testDeepL(apiKey)) {
            preferencesManager.saveDeepLApiKey(apiKey)
            _validationResult.value = ValidationResult.Success
        } else {
            _validationResult.value = ValidationResult.Error("Invalid API key")
        }
    }

    /**
     * Gets the stored Google Translate API key
     */
    fun getGoogleApiKey(): String? = preferencesManager.getGoogleApiKey()

    /**
     * Gets the stored DeepL API key
     */
    fun getDeepLApiKey(): String? = preferencesManager.getDeepLApiKey()

    /**
     * Represents the state of API key validation
     */
    sealed class ValidationResult {
        /** Loading state while validating API key */
        data object Loading : ValidationResult()

        /** Success state when API key is valid and saved */
        data object Success : ValidationResult()

        /** Error state with error message */
        data class Error(val message: String) : ValidationResult()
    }
}