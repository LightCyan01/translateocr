package com.jaymie.translateocr.utils

import com.jaymie.translateocr.data.model.Language

object DeepLConstants {
    val SUPPORTED_SOURCE_LANGUAGES = listOf(
        Language("EN", "English"),
        Language("ES", "Spanish"),
        Language("FR", "French"),
        Language("DE", "German"),
        Language("IT", "Italian"),
        Language("JA", "Japanese"),
        Language("ZH", "Chinese (Simplified)"),
        Language("PT", "Portuguese"),
        Language("NL", "Dutch"),
        Language("PL", "Polish"),
        Language("RU", "Russian"),
        Language("KO", "Korean"),
        Language("TR", "Turkish"),
        Language("SV", "Swedish"),
        Language("DA", "Danish"),
        Language("FI", "Finnish"),
        Language("CS", "Czech"),
        Language("EL", "Greek"),
        Language("BG", "Bulgarian"),
        Language("HU", "Hungarian")
    )

    val SUPPORTED_TARGET_LANGUAGES = listOf(
        Language("EN-US", "English (American)"),
        Language("EN-GB", "English (British)"),
        Language("ES", "Spanish"),
        Language("FR", "French"),
        Language("DE", "German"),
        Language("IT", "Italian"),
        Language("JA", "Japanese"),
        Language("ZH", "Chinese (Simplified)"),
        Language("PT-PT", "Portuguese (European)"),
        Language("PT-BR", "Portuguese (Brazilian)"),
        Language("NL", "Dutch"),
        Language("PL", "Polish"),
        Language("RU", "Russian"),
        Language("KO", "Korean"),
        Language("TR", "Turkish"),
        Language("SV", "Swedish"),
        Language("DA", "Danish"),
        Language("FI", "Finnish"),
        Language("CS", "Czech"),
        Language("EL", "Greek"),
        Language("BG", "Bulgarian"),
        Language("HU", "Hungarian")
    )
}