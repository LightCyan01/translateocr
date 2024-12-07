package com.jaymie.translateocr.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.jaymie.translateocr.data.model.Translation
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets
import com.google.firebase.FirebaseApp

/**
 * Repository for managing translation history using Firebase Storage.
 * Handles saving and retrieving translation records as JSON files.
 * Each user's translations are stored in a separate directory.
 */
class TranslationHistoryRepository {
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    private fun getUserTranslationsPath(userId: String) = "users/$userId/translations"

    /**
     * Saves a translation as a JSON file in Firebase Storage.
     * Uses [kotlinx.coroutines.tasks.await] to handle Firebase Tasks.
     * 
     * @param translation The translation to save
     * Files are stored as: users/{userId}/translations/{translationId}.json
     */
    suspend fun saveTranslation(translation: Translation) {
        auth.currentUser?.let { user ->
            val fileName = "${translation.id}.json"
            val translationRef = storage.child("${getUserTranslationsPath(user.uid)}/$fileName")
            
            translationRef.putBytes(translation.toJson().toByteArray(StandardCharsets.UTF_8)).await()
        }
    }

    /**
     * Retrieves all translations for the current user from Firebase Storage.
     * Downloads and parses JSON files into Translation objects.
     * Results are sorted by timestamp in descending order.
     * 
     * @return List of translations sorted by timestamp
     */
    suspend fun getTranslations(): List<Translation> {
        return auth.currentUser?.let { user ->
            val translationsRef = storage.child(getUserTranslationsPath(user.uid))
            val result = translationsRef.listAll().await()
            
            result.items.mapNotNull { item ->
                try {
                    val bytes = item.getBytes(Long.MAX_VALUE).await()
                    val jsonString = String(bytes, StandardCharsets.UTF_8)
                    Translation.fromJson(jsonString)
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { translation -> translation.timestamp }
        } ?: emptyList()
    }
} 