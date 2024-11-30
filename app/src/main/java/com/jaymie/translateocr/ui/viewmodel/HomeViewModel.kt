package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jaymie.translateocr.utils.PermissionUtils

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _showFloatingButton = MutableLiveData<Boolean>()
    val showFloatingButton: LiveData<Boolean> get() = _showFloatingButton

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    // TODO: Add LiveData for OCR results and other UI updates

    fun hasScreenRecordingPermission(): Boolean {
        return PermissionUtils.hasScreenRecordingPermission()
    }

    fun onFloatingButtonClicked() {
        // TODO: Implement the OCR and translation logic here
        // start a Coroutine to handle the asynchronous operation

        _toastMessage.postValue("Floating button clicked. Starting OCR process...")

        // Example:
        // startOcrProcess()
    }

    // TODO: Implement methods to handle OCR processing and post results to LiveData

    // Example method
    /*
    private fun startOcrProcess() {
        viewModelScope.launch {
            // Perform OCR and translation
            val result = performOcrAndTranslation()
            // Post results to LiveData
            _ocrResult.postValue(result)
        }
    }
    */
}
