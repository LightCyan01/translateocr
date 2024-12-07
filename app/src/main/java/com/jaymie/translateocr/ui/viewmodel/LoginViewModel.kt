package com.jaymie.translateocr.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jaymie.translateocr.utils.Event
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _loginResult = MutableLiveData<Event<LoginResult>>()
    val loginResult: LiveData<Event<LoginResult>> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var email = MutableLiveData("")
    var password = MutableLiveData("")

    fun onLoginClick() {
        val emailValue = email.value ?: ""
        val passwordValue = password.value ?: ""

        if (!validateInput(emailValue, passwordValue)) return
        login(emailValue, passwordValue)
    }

    fun onRegisterClick() {
        val emailValue = email.value ?: ""
        val passwordValue = password.value ?: ""

        if (!validateInput(emailValue, passwordValue)) return
        register(emailValue, passwordValue)
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = Event(LoginResult.Error("Please fill in all fields"))
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginResult.value = Event(LoginResult.Error("Please enter a valid email address"))
            return false
        }

        if (password.length < 6) {
            _loginResult.value = Event(LoginResult.Error("Password must be at least 6 characters"))
            return false
        }

        return true
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loginResult.value = Event(LoginResult.Loading)
                
                auth.signInWithEmailAndPassword(email, password).await()
                _loginResult.value = Event(LoginResult.Success)
            } catch (e: Exception) {
                _loginResult.value = Event(LoginResult.Error(e.message ?: "Login failed"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loginResult.value = Event(LoginResult.Loading)

                // Check if user exists with invalid password
                try {
                    auth.signInWithEmailAndPassword(email, "dummy_password").await()
                    // If we reach here, the email exists
                    _loginResult.value = Event(LoginResult.Error("An account with this email already exists"))
                    return@launch
                } catch (e: Exception) {
                    // If the error is about wrong password, the email exists
                    if (e.message?.contains("password is invalid") == true) {
                        _loginResult.value = Event(LoginResult.Error("An account with this email already exists"))
                        return@launch
                    }
                    // proceed with registration
                }

                // Create new user
                auth.createUserWithEmailAndPassword(email, password).await()
                _loginResult.value = Event(LoginResult.Success)
            } catch (e: Exception) {
                _loginResult.value = Event(LoginResult.Error(e.message ?: "Registration failed"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class LoginResult {
        data object Loading : LoginResult()
        data object Success : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}