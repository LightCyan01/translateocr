package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jaymie.translateocr.data.repository.OcrRepository
import com.jaymie.translateocr.data.service.ScreenCaptureService
import com.jaymie.translateocr.utils.ScreenCaptureManager


class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    val ocrResult: LiveData<String> = OcrRepository.ocrResult

    // TODO: LiveData for translation results
    // private val _translationResult = MutableLiveData<String>()
    // val translationResult: LiveData<String> get() = _translationResult

    private var screenCaptureManager: ScreenCaptureManager? = null

    /**
     * Initialize the screen capture service with the given result code and data.
     */
    fun initScreenCaptureService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(getApplication(), ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_INIT
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_DATA, data)
        }
        getApplication<Application>().startForegroundService(serviceIntent)
    }

    /**
     * Command the service to start screen capture and OCR processing.
     */
    fun captureScreen() {
        val serviceIntent = Intent(getApplication(), ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_CAPTURE_SCREEN
        }
        getApplication<Application>().startService(serviceIntent)
    }


    fun onFloatingButtonClicked() {
        _toastMessage.postValue("Floating button clicked. Starting screen capture...")
        // The ViewModel cannot request permissions, so the Fragment handles it
    }

    // TODO: Implement translation logic
    /*
    private suspend fun translateText(text: String) {
        // Perform translation in a coroutine
        val translatedText = withContext(Dispatchers.IO) {
            // Call your translation API or library here
            // Example: translationService.translate(text)
        }

        // Post the translated text to LiveData
        _translationResult.postValue(translatedText)
    }
    */

    /**
     * Stop the screen capture service.
     */
    fun stopScreenCaptureService() {
        val serviceIntent = Intent(getApplication(), ScreenCaptureService::class.java)
        getApplication<Application>().stopService(serviceIntent)
    }
}
