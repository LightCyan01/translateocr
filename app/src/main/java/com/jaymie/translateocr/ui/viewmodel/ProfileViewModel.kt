package com.jaymie.translateocr.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.jaymie.translateocr.data.repository.FirestoreRepository
import com.jaymie.translateocr.utils.PreferencesManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val firestoreRepository = FirestoreRepository()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _translatedWords = MutableLiveData<Int>()
    val translatedWords: LiveData<Int> = _translatedWords

    private val _profilePictureState = MutableLiveData<ProfilePictureState>()
    val profilePictureState: LiveData<ProfilePictureState> = _profilePictureState

    init {
        loadUserData()
        loadProfilePicture()
        observeUserStats()
    }

    private fun loadUserData() {
        // current user's email as usernae
        auth.currentUser?.let { user ->
            _username.value = user.email ?: "User"
        }
        
        // Load translated words count
        _translatedWords.value = preferencesManager.getTranslatedWordsCount()
    }

    private fun getUserStoragePath(userId: String) = "users/$userId"
    private fun getProfilePicturePath(userId: String) = "${getUserStoragePath(userId)}/profile_pictures"

    private fun loadProfilePicture() {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val imageRef = storageRef.child("${getProfilePicturePath(user.uid)}/profile.jpg")
                    val url = imageRef.downloadUrl.await()
                    _profilePictureState.value = ProfilePictureState.Success(url)
                } catch (e: Exception) {
                    // Silently fail on initial load
                }
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _profilePictureState.value = ProfilePictureState.Loading

                    // Compress and resize image before uploading
                    val bitmap = getApplication<Application>().contentResolver
                        .openInputStream(uri)?.use { input ->
                            val originalBitmap = android.graphics.BitmapFactory.decodeStream(input)
                            resizeAndCompressBitmap(originalBitmap)
                        } ?: throw Exception("Failed to process image")

                    // Convert bitmap to byte array
                    val baos = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, baos)
                    val data = baos.toByteArray()

                    // Upload to Firebase
                    val imageRef = storageRef.child("${getProfilePicturePath(user.uid)}/profile.jpg")
                    imageRef.putBytes(data).await()
                    
                    val downloadUrl = imageRef.downloadUrl.await()
                    _profilePictureState.value = ProfilePictureState.Success(downloadUrl)
                } catch (e: Exception) {
                    _profilePictureState.value = ProfilePictureState.Error(
                        e.message ?: "Failed to upload profile picture"
                    )
                }
            }
        }
    }

    private fun resizeAndCompressBitmap(original: android.graphics.Bitmap): android.graphics.Bitmap {
        val maxDimension = 1024 // Max width or height
        val width = original.width
        val height = original.height

        val ratio = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = if (ratio < 1) (width * ratio).toInt() else width
        val newHeight = if (ratio < 1) (height * ratio).toInt() else height

        return android.graphics.Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    fun signOut() {
        auth.signOut()
    }

    private fun observeUserStats() {
        viewModelScope.launch {
            try {
                firestoreRepository.getUserStats()
                    .catch { }
                    .collect { stats ->
                        _translatedWords.value = stats.totalWordsTranslated
                    }
            } catch (e: Exception) {
                // Silently catch errors
            }
        }
    }

    sealed class ProfilePictureState {
        data object Loading : ProfilePictureState()
        data class Success(val uri: Uri) : ProfilePictureState()
        data class Error(val message: String) : ProfilePictureState()
    }
}