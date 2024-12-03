package com.jaymie.translateocr.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object OcrRepository {
    private val _ocrResult = MutableLiveData<String>()
    val ocrResult: LiveData<String> get() = _ocrResult

    fun postOcrResult(result: String) {
        _ocrResult.postValue(result)
    }
}