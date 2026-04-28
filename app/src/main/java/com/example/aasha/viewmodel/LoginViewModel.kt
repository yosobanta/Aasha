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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
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

    private val _hasMpin = MutableStateFlow(false)
    val hasMpin: StateFlow<Boolean> = _hasMpin.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.lastWorkerId.collect { id ->
                _hasMpin.value = id != null && sessionManager.hasMpin(id)
            }
        }
    }

    fun signUp(name: String, aashaId: String, email: String, password: String, locality: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val normalizedAashaId = aashaId.trim().lowercase(Locale.ROOT)
            try {
                // 1. Check if AashaID already exists (Double check before Auth)
                if (firestoreRepository.isAashaIdTaken(normalizedAashaId)) {
                    _uiState.value = LoginUiState.Error("AashaID already taken")
                    return@launch
                }

                // 2. Create Firebase Auth user
                val authResult = authRepository.signUp(email, password)
                val uid = authResult.user?.uid ?: throw Exception("Failed to get UID")

                // 3. Store user in Firestore
                val user = User(
                    uid = uid,
                    name = name,
                    aashaId = normalizedAashaId,
                    email = email,
                    locality = locality,
                    createdAt = Timestamp.now()
                )
                try {
                    firestoreRepository.saveUser(user)
                } catch (e: Exception) {
                    // 🔥 ROLLBACK AUTH USER if Firestore write fails
                    authRepository.deleteCurrentUser()
                    throw e
                }

                // 4. Save session locally
                sessionManager.saveSession(normalizedAashaId, name, email, locality)
                sessionManager.savePasswordHash(password, normalizedAashaId)

                _uiState.value = LoginUiState.RegistrationSuccess
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-up failed")
            }
        }
    }

    fun loginWithAashaId(aashaId: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val normalizedAashaId = aashaId.trim().lowercase(Locale.ROOT)
            try {
                // 1. Try offline verification first if we have a saved workerId and password hash
                val savedLastWorkerId = sessionManager.lastWorkerId.first()
                if (savedLastWorkerId == normalizedAashaId && sessionManager.verifyPassword(password, normalizedAashaId)) {
                    // Locally verified
                    sessionManager.saveSession(
                        workerId = normalizedAashaId,
                        name = sessionManager.name.first() ?: "",
                        email = sessionManager.email.first() ?: "",
                        locality = sessionManager.locality.first() ?: ""
                    )
                    _uiState.value = LoginUiState.Success
                    return@launch
                }

                // 2. If offline verification fails or not available, try online
                // Query Firestore to get email using AashaID
                val user = firestoreRepository.getUserByAashaId(normalizedAashaId)
                    ?: throw Exception("Aasha ID not found locally or online")

                // Use retrieved email to login via Firebase Auth
                authRepository.signIn(user.email, password)

                // Cache locally
                sessionManager.saveSession(user.aashaId, user.name, user.email, user.locality)
                sessionManager.savePasswordHash(password, user.aashaId)

                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Invalid Aasha ID or Password")
            }
        }
    }

    fun loginWithMPin(pin: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val workerId = sessionManager.lastWorkerId.first()
            if (workerId != null && sessionManager.verifyMpin(pin, workerId)) {
                // Successfully verified MPIN for the last user
                sessionManager.setLoggedIn(true)
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error("Incorrect MPIN")
            }
        }
    }

    fun setupMPin(pin: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val workerId = sessionManager.workerId.first() ?: sessionManager.lastWorkerId.first()
            if (workerId == null) {
                _uiState.value = LoginUiState.Error("User session not found. Please login again.")
                return@launch
            }
            
            if (pin.length == 4) {
                sessionManager.saveMpin(pin, workerId)
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
