package com.jaymie.translateocr.data.model

data class UserStats(
    val userId: String = "",
    val totalWordsTranslated: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) 