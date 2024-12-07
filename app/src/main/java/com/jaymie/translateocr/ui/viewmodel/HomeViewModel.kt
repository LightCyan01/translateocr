package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.text.Text
import com.jaymie.translateocr.data.model.Translation
import com.jaymie.translateocr.data.model.TranslationService
import com.jaymie.translateocr.data.repository.FirestoreRepository
import com.jaymie.translateocr.data.repository.OCRRepository
import com.jaymie.translateocr.data.repository.TranslationHistoryRepository
import com.jaymie.translateocr.data.repository.TranslationRepository
import com.jaymie.translateocr.data.service.ScreenCaptureService
import com.jaymie.translateocr.service.TranslateAccessibilityService
import com.jaymie.translateocr.utils.DeepLConstants
import com.jaymie.translateocr.utils.Event
import com.jaymie.translateocr.utils.FloatingButtonManager
import com.jaymie.translateocr.utils.GoogleLanguages
import com.jaymie.translateocr.utils.OverlayManager
import com.jaymie.translateocr.utils.PermissionUtils
import com.jaymie.translateocr.utils.PreferencesManager
import com.jaymie.translateocr.utils.ScreenCaptureManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for handling translation operations and UI state in the Home screen.
 * Uses:
 * - ViewBinding for UI interactions
 * - Coroutines for async operations
 * - LiveData for state management
 * - ML Kit for OCR and translation
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val screenCaptureManager = ScreenCaptureManager(application)
    private val ocrRepository = OCRRepository(application)
    private val translationRepository = TranslationRepository()
    private val preferencesManager = PreferencesManager(application)
    private val translationHistoryRepository = TranslationHistoryRepository()
    private val floatingButtonManager = FloatingButtonManager(application)
    private val firestoreRepository = FirestoreRepository()

    // UI State LiveData
    private val _showFloatingButton = MutableLiveData<Boolean>()
    val showFloatingButton: LiveData<Boolean> = _showFloatingButton

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

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

    private var isScreenCaptureActive = false

    private val _isHighPrecisionEnabled = MutableLiveData<Boolean>()
    val isHighPrecisionEnabled: LiveData<Boolean> = _isHighPrecisionEnabled

    private var isProcessingTranslation = false

    init {
        setDefaultLanguages()
        observeAccessibilityService()
    }

    /**
     * Starts screen capture service and projection.
     * Uses coroutines for async operations.
     */
    fun startScreenCapture(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            try {
                ScreenCaptureService.startService(getApplication())
                delay(100)
                screenCaptureManager.startProjection(resultCode, data)
                _showFloatingButton.value = true
                isScreenCaptureActive = true
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
        if (!isScreenCaptureActive) return

        try {
            val service = selectedService.value ?: TranslationService.GOOGLE_TRANSLATE
            val translations = when (service) {
                TranslationService.OFFLINE -> {
                    if (!isModelDownloaded()) {
                        throw Exception("Language model not downloaded. Please download it first.")
                    }
                    val validBlocks = OverlayManager.getInstance().getValidTextBlocks(textBlocks, getApplication())
                    validBlocks.map { line ->
                        translationRepository.translateOffline(
                            text = line.text,
                            sourceLanguage = sourceLanguage.value ?: "en",
                            targetLanguage = targetLanguage.value ?: "ja"
                        )
                    }
                }
                else -> when (service) {
                    TranslationService.GOOGLE_TRANSLATE -> translateWithGoogle(textBlocks)
                    TranslationService.GOOGLE_TRANSLATE_API -> translateWithGoogle(textBlocks)
                    TranslationService.DEEPL -> translateWithDeepL(textBlocks)
                    TranslationService.DEEPL_API -> translateWithDeepL(textBlocks)
                    else -> listOf()
                }
            }

            // Update overlay with translations
            if (isHighPrecisionEnabled.value == true) {
                // Handle high precision mode
                val accessibilityBlocks = textBlocks.map { block ->
                    TranslateAccessibilityService.TextBlock(
                        text = block.text,
                        bounds = block.boundingBox ?: android.graphics.Rect(),
                        isEditable = false
                    )
                }
                OverlayManager.getInstance().updateOverlayWithHighPrecision(accessibilityBlocks, translations)
            } else {
                OverlayManager.getInstance().updateOverlayText(textBlocks, translations.joinToString("\n"))
            }

            // Save translation to history
            val translation = Translation(
                id = System.currentTimeMillis().toString(),
                originalText = textBlocks.joinToString("\n") { it.text },
                translatedText = translations.joinToString("\n"),
                fromLanguage = sourceLanguage.value ?: "en",
                toLanguage = targetLanguage.value ?: "ja"
            )
            translationHistoryRepository.saveTranslation(translation)

            // After successful translation, increment the word count
            val wordCount = textBlocks.sumOf { block -> 
                block.text.split("\\s+".toRegex()).size 
            }
            preferencesManager.incrementTranslatedWords(wordCount)
            firestoreRepository.updateUserStats(wordCount)

        } catch (e: Exception) {
            handleTranslationError(e, textBlocks)
        }
    }

    private suspend fun translateWithGoogle(textBlocks: List<Text.TextBlock>): List<String> {
        return try {
            val isApiKeyService = selectedService.value == TranslationService.GOOGLE_TRANSLATE_API
            val validBlocks = OverlayManager.getInstance().getValidTextBlocks(textBlocks, getApplication())

            validBlocks.map { line ->
                translationRepository.translateWithGoogle(
                    text = line.text,
                    sourceLanguage = sourceLanguage.value ?: "en",
                    targetLanguage = targetLanguage.value ?: "ja",
                    apiKey = if (isApiKeyService) preferencesManager.getGoogleApiKey() else null,
                    isApiKeyService = isApiKeyService
                )
            }
        } catch (e: Exception) {
            handleTranslationError(e, textBlocks)
            emptyList()
        }
    }

    private suspend fun translateWithDeepL(textBlocks: List<Text.TextBlock>): List<String> {
        return try {
            val isApiKeyService = selectedService.value == TranslationService.DEEPL_API
            val validBlocks = OverlayManager.getInstance().getValidTextBlocks(textBlocks, getApplication())

            validBlocks.map { line ->
                translationRepository.translateWithDeepL(
                    text = line.text,
                    sourceLanguage = sourceLanguage.value ?: "EN",
                    targetLanguage = targetLanguage.value ?: "JA",
                    apiKey = if (isApiKeyService) preferencesManager.getDeepLApiKey() else null,
                    isApiKeyService = isApiKeyService
                )
            }
        } catch (e: Exception) {
            handleTranslationError(e, textBlocks)
            emptyList()
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
            TranslationService.GOOGLE_TRANSLATE,
            TranslationService.DEEPL,
            TranslationService.OFFLINE -> {
                // No API key validation needed for these services
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
        
        // Use the language's name property instead of code for display
        val sourceLanguage = if (isDeepL) {
            DeepLConstants.SUPPORTED_SOURCE_LANGUAGES.find { it.code == "EN" }
        } else {
            GoogleLanguages.SUPPORTED_LANGUAGES.find { it.code == "en" }
        }

        val targetLanguage = if (isDeepL) {
            DeepLConstants.SUPPORTED_TARGET_LANGUAGES.find { it.code == "JA" }
        } else {
            GoogleLanguages.SUPPORTED_LANGUAGES.find { it.code == "ja" }
        }

        sourceLanguage?.let { setSourceLanguage(it.code, it.name) }
        targetLanguage?.let { setTargetLanguage(it.code, it.name) }
    }

    private fun handleTranslationError(e: Exception, textBlocks: List<Text.TextBlock>) {
        _toastMessage.postValue("Translation failed: ${e.message}")
        OverlayManager.getInstance().updateOverlayText(textBlocks)
    }

    fun hasScreenRecordingPermission(): Boolean = PermissionUtils.hasScreenRecordingPermission()

    override fun onCleared() {
        super.onCleared()
        isProcessingTranslation = false
        if (isScreenCaptureActive) {
            clearTranslationState()
            floatingButtonManager.cleanup()
        }
        // Remove the observer properly
        accessibilityObserver?.let {
            TranslateAccessibilityService.screenText.removeObserver(it)
        }
        accessibilityObserver = null
    }

    sealed class ValidationResult {
        data object Loading : ValidationResult()
        data object Success : ValidationResult()
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
            TranslationService.GOOGLE_TRANSLATE,
            TranslationService.DEEPL,
            TranslationService.OFFLINE -> {
                // These services don't require API keys, so no action needed
            }
        }
    }

    fun clearTranslationState() {
        viewModelScope.launch {
            try {
                if (isScreenCaptureActive) {
                    isProcessingTranslation = false
                    screenCaptureManager.stopProjection()
                    isScreenCaptureActive = false
                    _showFloatingButton.value = false
                    floatingButtonManager.removeFloatingButton()
                    OverlayManager.getInstance().removeOverlay()
                    _ocrResult.value = emptyList()
                    _toastMessage.postValue(null)
                    getApplication<Application>().stopService(Intent(getApplication(), ScreenCaptureService::class.java))
                }
            } catch (e: Exception) {
                // Handle any cleanup errors silently
            }
        }
    }

    fun stopScreenCapture() {
        viewModelScope.launch {
            try {
                screenCaptureManager.stopProjection()
                isScreenCaptureActive = false
                _showFloatingButton.value = false
                floatingButtonManager.removeFloatingButton()
                getApplication<Application>().stopService(Intent(getApplication(), ScreenCaptureService::class.java))
            } catch (e: Exception) {
                _toastMessage.postValue("Error stopping screen capture: ${e.message}")
            }
        }
    }

    fun checkState() {
        if (!isScreenCaptureActive) {
            _showFloatingButton.value = false
        }
    }

    private fun processHighPrecisionText(textBlocks: List<TranslateAccessibilityService.TextBlock>) {
        if (!isScreenCaptureActive || isProcessingTranslation) return
        
        viewModelScope.launch {
            isProcessingTranslation = true
            try {
                val translations = translateTextBlocks(textBlocks)
                updateOverlayAndSaveHistory(textBlocks, translations)
            } catch (e: Exception) {
                if (isScreenCaptureActive) {
                    _toastMessage.postValue("Translation failed: ${e.message}")
                }
            } finally {
                isProcessingTranslation = false
            }
        }
    }

    private suspend fun translateTextBlocks(
        textBlocks: List<TranslateAccessibilityService.TextBlock>
    ): List<String> {
        return textBlocks.map { block ->
            when (selectedService.value) {
                TranslationService.OFFLINE -> translateOfflineBlock(block)
                TranslationService.GOOGLE_TRANSLATE,
                TranslationService.GOOGLE_TRANSLATE_API -> translateWithGoogleBlock(block)
                TranslationService.DEEPL,
                TranslationService.DEEPL_API -> translateWithDeepLBlock(block)
                else -> block.text
            }
        }
    }

    private suspend fun translateOfflineBlock(block: TranslateAccessibilityService.TextBlock): String {
        if (!isModelDownloaded()) {
            throw Exception("Language model not downloaded. Please download it first.")
        }
        return translationRepository.translateOffline(
            text = block.text,
            sourceLanguage = sourceLanguage.value ?: "en",
            targetLanguage = targetLanguage.value ?: "ja"
        )
    }

    private suspend fun translateWithGoogleBlock(block: TranslateAccessibilityService.TextBlock): String {
        return translationRepository.translateWithGoogle(
            text = block.text,
            sourceLanguage = sourceLanguage.value ?: "en",
            targetLanguage = targetLanguage.value ?: "ja",
            apiKey = if (selectedService.value == TranslationService.GOOGLE_TRANSLATE_API) 
                preferencesManager.getGoogleApiKey() else null,
            isApiKeyService = selectedService.value == TranslationService.GOOGLE_TRANSLATE_API
        )
    }

    private suspend fun translateWithDeepLBlock(block: TranslateAccessibilityService.TextBlock): String {
        return translationRepository.translateWithDeepL(
            text = block.text,
            sourceLanguage = sourceLanguage.value ?: "EN",
            targetLanguage = targetLanguage.value ?: "JA",
            apiKey = if (selectedService.value == TranslationService.DEEPL_API) 
                preferencesManager.getDeepLApiKey() else null,
            isApiKeyService = selectedService.value == TranslationService.DEEPL_API
        )
    }

    private suspend fun updateOverlayAndSaveHistory(
        textBlocks: List<TranslateAccessibilityService.TextBlock>,
        translations: List<String>
    ) {
        // Update overlay
        OverlayManager.getInstance().updateOverlayWithHighPrecision(textBlocks, translations)

        // Save translation history
        saveTranslationHistory(textBlocks, translations)

        // Update word count
        updateWordCount(textBlocks)
    }

    private suspend fun saveTranslationHistory(
        textBlocks: List<TranslateAccessibilityService.TextBlock>,
        translations: List<String>
    ) {
        val translation = Translation(
            id = System.currentTimeMillis().toString(),
            originalText = textBlocks.joinToString("\n") { it.text },
            translatedText = translations.joinToString("\n"),
            fromLanguage = sourceLanguage.value ?: "en",
            toLanguage = targetLanguage.value ?: "ja"
        )
        translationHistoryRepository.saveTranslation(translation)
    }

    private suspend fun updateWordCount(textBlocks: List<TranslateAccessibilityService.TextBlock>) {
        try {
            val wordCount = textBlocks.sumOf { block -> 
                block.text.split("\\s+".toRegex()).size 
            }
            preferencesManager.incrementTranslatedWords(wordCount)
            
            // Only update Firestore if word count is positive
            if (wordCount > 0) {
                firestoreRepository.updateUserStats(wordCount)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error updating word count", e)
            // Don't throw the exception to prevent translation failure
        }
    }

    private suspend fun isModelDownloaded(): Boolean {
        return translationRepository.isModelDownloaded(
            sourceLanguage = sourceLanguage.value ?: "en",
            targetLanguage = targetLanguage.value ?: "ja"
        )
    }

    // Store the observer reference for proper cleanup
    private var accessibilityObserver: ((List<TranslateAccessibilityService.TextBlock>) -> Unit)? = null

    private fun observeAccessibilityService() {
        accessibilityObserver = { textBlocks ->
            if (isHighPrecisionEnabled.value == true) {
                processHighPrecisionText(textBlocks)
            }
        }
        accessibilityObserver?.let {
            TranslateAccessibilityService.screenText.observeForever(it)
        }
    }

}

data class HomeUiState(
    val sourceLanguageText: String = "English",
    val targetLanguageText: String = "Japanese",
    val selectedServiceText: String = TranslationService.GOOGLE_TRANSLATE.displayName,
    val isHighPrecisionEnabled: Boolean = false,
    val showFloatingButton: Boolean = false,
    val isLoading: Boolean = false
)