package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jaymie.translateocr.data.repository.OCRRepository
import com.jaymie.translateocr.utils.PermissionUtils
import com.jaymie.translateocr.utils.ScreenCaptureManager
import com.jaymie.translateocr.utils.OverlayManager
import com.jaymie.translateocr.data.service.ScreenCaptureService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.mlkit.vision.text.Text
import com.jaymie.translateocr.data.model.TranslationService
import com.jaymie.translateocr.data.repository.TranslationRepository
import com.jaymie.translateocr.utils.Event
import com.jaymie.translateocr.utils.PreferencesManager

/**
 * ViewModel for handling translation operations and UI state in the Home screen
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val screenCaptureManager = ScreenCaptureManager(application)
    private val ocrRepository = OCRRepository(application)
    private val translationRepository = TranslationRepository()
    private val preferencesManager = PreferencesManager(application)

    // UI State
    private val _showFloatingButton = MutableLiveData<Boolean>()
    val showFloatingButton: LiveData<Boolean> = _showFloatingButton

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _ocrResult = MutableLiveData<List<Text.TextBlock>>()
    val ocrResult: LiveData<List<Text.TextBlock>> = _ocrResult

    // Language State
    private val _sourceLanguage = MutableLiveData<String>()
    val sourceLanguage: LiveData<String> = _sourceLanguage

    private val _targetLanguage = MutableLiveData<String?>()
    val targetLanguage: MutableLiveData<String?> = _targetLanguage

    private val _sourceLanguageText = MutableLiveData<String>()
    val sourceLanguageText: LiveData<String> = _sourceLanguageText

    private val _targetLanguageText = MutableLiveData<String?>()
    val targetLanguageText: MutableLiveData<String?> = _targetLanguageText

    // Service State
    private val _selectedService = MutableLiveData(TranslationService.GOOGLE_TRANSLATE)
    val selectedService: LiveData<TranslationService> = _selectedService

    private val _showApiKeyDialog = MutableLiveData<Event<TranslationService>>()
    val showApiKeyDialog: LiveData<Event<TranslationService>> = _showApiKeyDialog

    private val _validationResult = MutableLiveData<ValidationResult>()
    val validationResult: LiveData<ValidationResult> = _validationResult

    init {
        setDefaultLanguages()
    }

    fun startScreenCapture(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            try {
                ScreenCaptureService.startService(getApplication())
                delay(100)
                screenCaptureManager.startProjection(resultCode, data)
                _showFloatingButton.value = true
            } catch (e: Exception) {
                _toastMessage.postValue("Error starting screen capture: ${e.message}")
            }
        }
    }

    fun onFloatingButtonClicked() {
        viewModelScope.launch {
            try {
                OverlayManager.getInstance().showOverlay(getApplication())
                OverlayManager.getInstance().showLoading(true)
                
                val bitmap = screenCaptureManager.captureScreen()
                bitmap?.let { processScreenCapture(it) }
            } catch (e: Exception) {
                _toastMessage.postValue("Error: ${e.message}")
                OverlayManager.getInstance().showLoading(false)
            }
        }
    }

    private suspend fun processScreenCapture(bitmap: Bitmap) {
        val textBlocks = ocrRepository.performOCR(bitmap)
        
        if (textBlocks.isEmpty()) {
            _toastMessage.postValue("No text detected")
            OverlayManager.getInstance().showLoading(false)
            return
        }

        // Check API key requirements
        when (selectedService.value) {
            TranslationService.GOOGLE_TRANSLATE_API -> {
                val apiKey = preferencesManager.getGoogleApiKey()
                if (apiKey.isNullOrBlank()) {
                    _toastMessage.postValue("Google Translate API key is not set")
                    _showApiKeyDialog.value = Event(TranslationService.GOOGLE_TRANSLATE_API)
                    OverlayManager.getInstance().showLoading(false)
                    return
                }
                processTranslation(textBlocks)
            }
            TranslationService.DEEPL_API -> {
                val apiKey = preferencesManager.getDeepLApiKey()
                if (apiKey.isNullOrBlank()) {
                    _toastMessage.postValue("DeepL API key is not set")
                    _showApiKeyDialog.value = Event(TranslationService.DEEPL_API)
                    OverlayManager.getInstance().showLoading(false)
                    return
                }
                processTranslation(textBlocks)
            }
            else -> processTranslation(textBlocks)
        }
    }

    private suspend fun processTranslation(textBlocks: List<Text.TextBlock>) {
        when (selectedService.value) {
            TranslationService.GOOGLE_TRANSLATE -> translateWithGoogle(textBlocks)
            TranslationService.GOOGLE_TRANSLATE_API -> translateWithGoogle(textBlocks)
            TranslationService.DEEPL -> translateWithDeepL(textBlocks)
            TranslationService.DEEPL_API -> translateWithDeepL(textBlocks)
            else -> OverlayManager.getInstance().updateOverlayText(textBlocks)
        }
    }

    private suspend fun translateWithGoogle(textBlocks: List<Text.TextBlock>) {
        try {
            val isApiKeyService = selectedService.value == TranslationService.GOOGLE_TRANSLATE_API
            val validBlocks = OverlayManager.getInstance().getValidTextBlocks(textBlocks, getApplication())

            if (validBlocks.size > 5) {
                _toastMessage.postValue("Translating multiple text blocks...")
            }

            val translations = validBlocks.map { line ->
                translationRepository.translateWithGoogle(
                    text = line.text,
                    sourceLanguage = sourceLanguage.value ?: "en",
                    targetLanguage = targetLanguage.value ?: "ja",
                    apiKey = if (isApiKeyService) preferencesManager.getGoogleApiKey() else null,
                    isApiKeyService = isApiKeyService
                )
            }

            OverlayManager.getInstance().updateOverlayText(textBlocks, translations.joinToString("\n"))
        } catch (e: Exception) {
            handleTranslationError(e, textBlocks)
        } finally {
            OverlayManager.getInstance().showLoading(false)
        }
    }

    private suspend fun translateWithDeepL(textBlocks: List<Text.TextBlock>) {
        try {
            val isApiKeyService = selectedService.value == TranslationService.DEEPL_API
            val validBlocks = OverlayManager.getInstance().getValidTextBlocks(textBlocks, getApplication())

            if (validBlocks.size > 5) {
                _toastMessage.postValue("Translating multiple text blocks...")
            }

            val translations = validBlocks.map { line ->
                translationRepository.translateWithDeepL(
                    text = line.text,
                    sourceLanguage = sourceLanguage.value ?: "EN",
                    targetLanguage = targetLanguage.value ?: "JA",
                    apiKey = if (isApiKeyService) preferencesManager.getDeepLApiKey() else null,
                    isApiKeyService = isApiKeyService
                )
            }

            OverlayManager.getInstance().updateOverlayText(textBlocks, translations.joinToString("\n"))
        } catch (e: Exception) {
            handleTranslationError(e, textBlocks)
        } finally {
            OverlayManager.getInstance().showLoading(false)
        }
    }

    fun setTranslationService(service: TranslationService) {
        val previousService = selectedService.value ?: TranslationService.GOOGLE_TRANSLATE
        _selectedService.value = service

        when (service) {
            TranslationService.GOOGLE_TRANSLATE_API -> {
                if (preferencesManager.getGoogleApiKey().isNullOrBlank()) {
                    _showApiKeyDialog.value = Event(service)
                    _selectedService.value = previousService
                    return
                }
            }
            TranslationService.DEEPL_API -> {
                if (preferencesManager.getDeepLApiKey().isNullOrBlank()) {
                    _showApiKeyDialog.value = Event(service)
                    _selectedService.value = previousService
                    return
                }
            }
        }

        setDefaultLanguagesForService(service)
    }

    fun validateAndSaveApiKey(service: TranslationService, apiKey: String) {
        viewModelScope.launch {
            try {
                _validationResult.value = ValidationResult.Loading

                when (service) {
                    TranslationService.GOOGLE_TRANSLATE_API -> {
                        if (translationRepository.testGoogleTranslate(apiKey)) {
                            preferencesManager.saveGoogleApiKey(apiKey)
                            _selectedService.value = service
                            _validationResult.value = ValidationResult.Success
                        } else {
                            _validationResult.value = ValidationResult.Error("Invalid API key")
                        }
                    }
                    TranslationService.DEEPL_API -> {
                        if (translationRepository.testDeepL(apiKey)) {
                            preferencesManager.saveDeepLApiKey(apiKey)
                            _selectedService.value = service
                            _validationResult.value = ValidationResult.Success
                        } else {
                            _validationResult.value = ValidationResult.Error("Invalid API key")
                        }
                    }
                    else -> _validationResult.value = ValidationResult.Error("Invalid service type")
                }
            } catch (e: Exception) {
                _validationResult.value = ValidationResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setSourceLanguage(code: String, displayText: String) {
        _sourceLanguage.value = code
        _sourceLanguageText.value = displayText
    }

    fun setTargetLanguage(code: String, displayText: String) {
        _targetLanguage.value = code
        _targetLanguageText.value = displayText
    }

    fun swapLanguages() {
        val tempSourceCode = _sourceLanguage.value
        val tempSourceText = _sourceLanguageText.value
        
        _sourceLanguage.value = _targetLanguage.value
        _sourceLanguageText.value = _targetLanguageText.value
        
        _targetLanguage.value = tempSourceCode
        _targetLanguageText.value = tempSourceText
    }

    private fun setDefaultLanguages() {
        setSourceLanguage("en", "English")
        setTargetLanguage("ja", "Japanese")
    }

    private fun setDefaultLanguagesForService(service: TranslationService) {
        val isDeepL = service == TranslationService.DEEPL || service == TranslationService.DEEPL_API
        
        // DeepL uses uppercase, others use lowercase
        val sourceCode = if (isDeepL) "EN" else "en"
        val targetCode = if (isDeepL) "JA" else "ja"
        
        setSourceLanguage(sourceCode, "English")
        setTargetLanguage(targetCode, "Japanese")
    }

    private fun handleTranslationError(e: Exception, textBlocks: List<Text.TextBlock>) {
        _toastMessage.postValue("Translation failed: ${e.message}")
        OverlayManager.getInstance().updateOverlayText(textBlocks)
    }

    fun hasScreenRecordingPermission(): Boolean = PermissionUtils.hasScreenRecordingPermission()

    override fun onCleared() {
        super.onCleared()
        screenCaptureManager.stopProjection()
        getApplication<Application>().stopService(Intent(getApplication(), ScreenCaptureService::class.java))
    }

    sealed class ValidationResult {
        object Loading : ValidationResult()
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun onApiKeyDialogDismissed(service: TranslationService) {
        when (service) {
            TranslationService.GOOGLE_TRANSLATE_API -> {
                if (preferencesManager.getGoogleApiKey().isNullOrBlank()) {
                    _toastMessage.value = "Google Translate API key is not set"
                }
            }
            TranslationService.DEEPL_API -> {
                if (preferencesManager.getDeepLApiKey().isNullOrBlank()) {
                    _toastMessage.value = "DeepL API key is not set"
                }
            }
        }
    }
}