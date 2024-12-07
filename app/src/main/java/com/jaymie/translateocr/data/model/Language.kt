package com.jaymie.translateocr.data.model

data class Language(
    val code: String,
    val name: String,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false
) {
}