package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "Savitri Devi",
    val role: String = "ASHA Worker",
    val area: String = "Bishnupur Village",
    val totalPatients: Int = 0,
    val totalVisits: Int = 0,
    val vaccinationsCompleted: Int = 0,
    val medicinesDistributed: Int = 0,
    val currentLanguage: String = "en"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.patients,
        sessionManager.workerId,
        sessionManager.locality,
        sessionManager.language
    ) { patients, workerId, locality, language ->
        ProfileUiState(
            name = if (workerId != null) "($workerId)" else "Savitri Devi",
            area = locality ?: "Bishnupur Village",
            totalPatients = patients.size,
            totalVisits = 0, // Should be calculated from visit repository
            vaccinationsCompleted = 0, // Should be calculated from vaccination repository
            medicinesDistributed = 0,
            currentLanguage = language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun changeLanguage(langCode: String) {
        viewModelScope.launch {
            sessionManager.saveLanguage(langCode)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}
