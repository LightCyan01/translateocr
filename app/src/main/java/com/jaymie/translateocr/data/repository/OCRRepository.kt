package com.jaymie.translateocr.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OCRRepository(context: Context) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun performOCR(bitmap: Bitmap): List<Text.TextBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = textRecognizer.process(image).await()
            result.textBlocks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
} 