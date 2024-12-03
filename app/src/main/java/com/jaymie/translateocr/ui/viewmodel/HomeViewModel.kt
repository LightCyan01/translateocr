package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.content.Intent
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
import android.util.Log
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.withTimeoutOrNull

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _showFloatingButton = MutableLiveData<Boolean>()
    val showFloatingButton: LiveData<Boolean> = _showFloatingButton

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _ocrResult = MutableLiveData<List<Text.TextBlock>>()
    val ocrResult: LiveData<List<Text.TextBlock>> = _ocrResult

    private val screenCaptureManager = ScreenCaptureManager(application)
    private val ocrRepository = OCRRepository(application)

    fun startScreenCapture(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting screen capture service...")
                ScreenCaptureService.startService(getApplication())
                
                delay(100)
                Log.d(TAG, "Starting media projection...")
                screenCaptureManager.startProjection(resultCode, data)
                _showFloatingButton.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting screen capture", e)
                _toastMessage.postValue("Error starting screen capture: ${e.message}")
            }
        }
    }

    fun onFloatingButtonClicked() {
        viewModelScope.launch {
            try {
                OverlayManager.getInstance().showOverlay(getApplication())
                OverlayManager.getInstance().showLoading(true)
                
                withTimeoutOrNull(5000L) { // 5 second timeout
                    val bitmap = screenCaptureManager.captureScreen()
                    bitmap?.let {
                        val textBlocks = ocrRepository.performOCR(it)
                        OverlayManager.getInstance().updateOverlayText(textBlocks)
                    }
                } ?: run {
                    _toastMessage.postValue("OCR processing timed out")
                    OverlayManager.getInstance().showLoading(false)
                }
            } catch (e: Exception) {
                _toastMessage.postValue("Error capturing screen: ${e.message}")
                OverlayManager.getInstance().showLoading(false)
            }
        }
    }

    fun hasScreenRecordingPermission(): Boolean {
        return PermissionUtils.hasScreenRecordingPermission()
    }

    override fun onCleared() {
        super.onCleared()
        screenCaptureManager.stopProjection()
        getApplication<Application>().stopService(Intent(getApplication(), ScreenCaptureService::class.java))
    }
}
