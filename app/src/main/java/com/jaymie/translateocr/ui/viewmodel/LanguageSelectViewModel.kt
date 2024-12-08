package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jaymie.translateocr.data.model.Language
import com.jaymie.translateocr.utils.DeepLConstants
import com.jaymie.translateocr.utils.Event
import com.jaymie.translateocr.utils.GoogleLanguages
import com.jaymie.translateocr.utils.ModelManager
import kotlinx.coroutines.launch

/**
 * ViewModel for the language selection screen.
 * Handles language list management and model downloads.
 */
class LanguageSelectViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    private val _uiState = MutableLiveData(LanguageSelectUiState())
    val uiState: LiveData<LanguageSelectUiState> = _uiState

    private val _downloadEvent = MutableLiveData<Event<DownloadEvent>>()
    val downloadEvent: LiveData<Event<DownloadEvent>> = _downloadEvent

    init {
        val isFromLanguage = true
        val isDeepL = false
        updateUiState { it.copy(
            isFromLanguage = isFromLanguage,
            isDeepL = isDeepL
        ) }
        loadLanguages()
    }

    fun onLanguageClick(language: Language) {
        _downloadEvent.value = Event(DownloadEvent.ReturnLanguage(language.code))
    }

    fun onDownloadClick(language: Language) {
        _downloadEvent.value = Event(DownloadEvent.StartDownload(language.code))
    }

    fun updateDownloadState(languageCode: String, isDownloaded: Boolean) {
        val currentLanguages = _uiState.value?.languages.orEmpty()
        val updatedLanguages = currentLanguages.map { language ->
            if (language.code == languageCode) {
                language.copy(isDownloaded = isDownloaded, isDownloading = false)
            } else language
        }
        updateUiState { it.copy(languages = updatedLanguages) }
    }

    private fun loadLanguages() {
        viewModelScope.launch {
            val languages = if (_uiState.value?.isDeepL == true) {
                if (_uiState.value?.isFromLanguage == true) {
                    DeepLConstants.SUPPORTED_SOURCE_LANGUAGES
                } else {
                    DeepLConstants.SUPPORTED_TARGET_LANGUAGES
                }
            } else {
                GoogleLanguages.SUPPORTED_LANGUAGES
            }

            val languagesWithState = languages.map { language ->
                language.copy(
                    isDownloaded = modelManager.isModelDownloadedSync(language.code)
                )
            }

            updateUiState { it.copy(languages = languagesWithState) }
        }
    }

    private fun updateUiState(update: (LanguageSelectUiState) -> LanguageSelectUiState) {
        _uiState.value = update(_uiState.value ?: LanguageSelectUiState())
    }
}

data class LanguageSelectUiState(
    val languages: List<Language> = emptyList(),
    val isFromLanguage: Boolean = true,
    val isDeepL: Boolean = false
)

sealed class DownloadEvent {
    data class StartDownload(val languageCode: String) : DownloadEvent()
    data class ReturnLanguage(val languageCode: String) : DownloadEvent()
} 