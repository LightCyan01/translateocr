package com.jaymie.translateocr.data.model

data class Translation(
    val id: String = "",  // Timestamp as ID
    val originalText: String = "",
    val translatedText: String = "",
    val fromLanguage: String = "",
    val toLanguage: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return """
            {
                "id": "$id",
                "originalText": "$originalText",
                "translatedText": "$translatedText",
                "fromLanguage": "$fromLanguage",
                "toLanguage": "$toLanguage",
                "timestamp": $timestamp
            }
        """.trimIndent()
    }

    companion object {
        fun fromJson(json: String): Translation {
            val jsonObject = org.json.JSONObject(json)
            return Translation(
                id = jsonObject.getString("id"),
                originalText = jsonObject.getString("originalText"),
                translatedText = jsonObject.getString("translatedText"),
                fromLanguage = jsonObject.getString("fromLanguage"),
                toLanguage = jsonObject.getString("toLanguage"),
                timestamp = jsonObject.getLong("timestamp")
            )
        }
    }
} 