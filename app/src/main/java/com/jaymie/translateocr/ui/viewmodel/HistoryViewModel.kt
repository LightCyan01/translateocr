package com.jaymie.translateocr.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaymie.translateocr.data.model.Translation
import com.jaymie.translateocr.data.repository.TranslationHistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val translationHistoryRepository = TranslationHistoryRepository()
    
    private val _translations = MutableLiveData<List<Translation>>()
    val translations: LiveData<List<Translation>> = _translations

    fun loadTranslations() {
        viewModelScope.launch {
            try {
                val history = translationHistoryRepository.getTranslations()
                _translations.value = history
            } catch (e: Exception) {
                _translations.value = emptyList()
            }
        }
    }
}