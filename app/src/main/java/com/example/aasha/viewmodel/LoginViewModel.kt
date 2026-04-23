package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.repository.AuthRepository
import com.example.aasha.data.repository.FirestoreRepository
import com.example.aasha.domain.model.User
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object RegistrationSuccess : LoginUiState()
    object MPinSetupSuccess : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signUp(aashaId: String, email: String, password: String, locality: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                // 1. Check if AashaID already exists
                if (firestoreRepository.isAashaIdTaken(aashaId)) {
                    _uiState.value = LoginUiState.Error("AashaID already taken")
                    return@launch
                }

                // 2. Create Firebase Auth user
                val authResult = authRepository.signUp(email, password)
                val uid = authResult.user?.uid ?: throw Exception("Failed to get UID")

                // 3. Store user in Firestore
                val user = User(
                    uid = uid,
                    aashaId = aashaId,
                    email = email,
                    locality = locality,
                    createdAt = Timestamp.now()
                )
                try {
                    firestoreRepository.saveUser(user)
                } catch (e: Exception) {
                    // 🔥 ROLLBACK AUTH USER
                    authRepository.deleteCurrentUser()
                    throw Exception("Failed to save user. Please try again.")
                }


                // 4. Save session locally
                sessionManager.saveSession(aashaId, email, locality)
                
                _uiState.value = LoginUiState.RegistrationSuccess
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-up failed")
            }
        }
    }

    fun loginWithAashaId(aashaId: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                // 1. Query Firestore to get email using AashaID
                val user = firestoreRepository.getUserByAashaId(aashaId)
                    ?: throw Exception("Aasha ID not found")

                // 2. Use retrieved email to login via Firebase Auth
                authRepository.signIn(user.email, password)

                // 3. Cache locally
                sessionManager.saveSession(user.aashaId, user.email, user.locality)
                
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Invalid Aasha ID or Password")
            }
        }
    }

    fun loginWithMPin(pin: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            if (sessionManager.verifyMpin(pin)) {
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error("Incorrect MPIN")
            }
        }
    }

    fun setupMPin(pin: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            if (pin.length == 4) {
                sessionManager.saveMpin(pin)
                _uiState.value = LoginUiState.MPinSetupSuccess
            } else {
                _uiState.value = LoginUiState.Error("MPIN must be 4 digits")
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = LoginUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            authRepository.signOut()
            _uiState.value = LoginUiState.Idle
        }
    }
}
