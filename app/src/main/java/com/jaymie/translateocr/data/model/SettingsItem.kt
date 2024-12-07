package com.jaymie.translateocr.data.model

data class SettingsItem(
    val id: Int,
    val title: String,
    val type: SettingsType
)

enum class SettingsType {
    GOOGLE_API_KEY,
    DEEPL_API_KEY
} 