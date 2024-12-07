package com.jaymie.translateocr.data.model

data class Translation(
    val id: Long = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val service: TranslationService,
    val timestamp: Long = System.currentTimeMillis()
) 