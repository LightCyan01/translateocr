package com.jaymie.translateocr.data.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jaymie.translateocr.data.model.UserStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userStatsRef = db.collection("user_stats")

    suspend fun updateUserStats(wordsTranslated: Int) {
        if (auth.currentUser == null) return  // Early return if no user

        try {
            val docRef = userStatsRef.document(auth.currentUser!!.uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStats = snapshot.toObject(UserStats::class.java)
                val newTotal = (currentStats?.totalWordsTranslated ?: 0) + wordsTranslated
                
                val updatedStats = UserStats(
                    userId = auth.currentUser!!.uid,
                    totalWordsTranslated = newTotal,
                    lastUpdated = System.currentTimeMillis()
                )
                
                transaction.set(docRef, updatedStats)
            }.await()
        } catch (e: Exception) {
            // Silently fail for non-critical updates
        }
    }

    fun getUserStats(): Flow<UserStats> = callbackFlow {
        if (auth.currentUser == null) {
            trySend(UserStats())  // Send empty stats for non-authenticated users
            close()
            return@callbackFlow
        }

        val listener = userStatsRef.document(auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val stats = snapshot?.toObject(UserStats::class.java) 
                    ?: UserStats(userId = auth.currentUser!!.uid)
                trySend(stats)
            }

        awaitClose { listener.remove() }
    }
}
