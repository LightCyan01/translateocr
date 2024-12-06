package com.jaymie.translateocr.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jaymie.translateocr.data.model.Translation

class HistoryViewModel : ViewModel() {
    private val _translations = MutableLiveData<List<Translation>>()
    val translations: LiveData<List<Translation>> = _translations

    // TODO: Load translations from database
}