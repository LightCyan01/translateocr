package com.jaymie.translateocr.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jaymie.translateocr.data.model.Translation
import com.jaymie.translateocr.data.repository.TranslationHistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val translationHistoryRepository = TranslationHistoryRepository()

    private val _translations = MutableLiveData<List<Translation>>()
    val translations: LiveData<List<Translation>> = _translations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        if (auth.currentUser != null) {
            loadTranslations()
        }
    }

    fun loadTranslations() {
        if (auth.currentUser == null) {
            _translations.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val history = translationHistoryRepository.getTranslations()
                _translations.value = history
            } catch (e: Exception) {
                _translations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isUserLoggedIn() = auth.currentUser != null
}