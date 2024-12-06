package com.jaymie.translateocr.utils

import com.jaymie.translateocr.data.model.Language

object GoogleLanguages {
    val SUPPORTED_LANGUAGES = listOf(
        Language("en", "English"),
        Language("zh", "Chinese (Simplified)"),
        Language("zh-TW", "Chinese (Traditional)"),
        Language("es", "Spanish"),
        Language("ar", "Arabic"),
        Language("fr", "French"),
        Language("de", "German"),
        Language("hi", "Hindi"),
        Language("it", "Italian"),
        Language("ja", "Japanese"),
        Language("ko", "Korean"),
        Language("pt", "Portuguese (Brazil)"),
        Language("ru", "Russian"),
        Language("vi", "Vietnamese"),
        Language("id", "Indonesian"),
        Language("tr", "Turkish"),
        Language("th", "Thai"),
        Language("nl", "Dutch"),
        Language("sv", "Swedish"),
        Language("no", "Norwegian"),
        Language("da", "Danish"),
        Language("pl", "Polish"),
        Language("uk", "Ukrainian"),
        Language("he", "Hebrew"),
        Language("ms", "Malay")
    )
}