package com.example.aasha.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.data.repository.PatientRepository
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
    val currentLanguage: String = "en",
    val pendingCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: MainRepository,
    private val patientRepository: PatientRepository,
    private val sessionManager: SessionManager,
    private val application: Application
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    
    private val _syncEvents = MutableSharedFlow<String>()
    val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    private val sessionFlow = combine(
        sessionManager.name,
        sessionManager.locality,
        sessionManager.language
    ) { name, locality, language -> Triple(name, locality, language) }

    private val syncStatusFlow = combine(
        patientRepository.pendingCount,
        patientRepository.lastSyncTime,
        _isSyncing
    ) { pending, lastSync, syncing -> Triple(pending, lastSync, syncing) }

    val uiState: StateFlow<ProfileUiState> = combine(
        patientRepository.patients,
        sessionFlow,
        syncStatusFlow
    ) { patients, session, syncStatus ->
        val (name, locality, language) = session
        val (pendingCount, lastSyncTime, isSyncing) = syncStatus
        
        ProfileUiState(
            name = name ?: "Savitri Devi",
            area = locality ?: "Bishnupur Village",
            totalPatients = patients.size,
            totalVisits = 0, 
            vaccinationsCompleted = 0, 
            medicinesDistributed = 0,
            currentLanguage = language,
            pendingCount = pendingCount,
            lastSyncTime = lastSyncTime,
            isSyncing = isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    init {
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        WorkManager.getInstance(application)
            .getWorkInfosForUniqueWorkFlow("patient_sync")
            .onEach { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@onEach
                
                val isRunning = workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED
                
                if (isRunning && !_isSyncing.value) {
                    _syncEvents.emit("Sync Started")
                } else if (!isRunning && _isSyncing.value) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        _syncEvents.emit("Sync Completed")
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        _syncEvents.emit("Sync Failed")
                    }
                }
                
                _isSyncing.value = isRunning
            }
            .launchIn(viewModelScope)
    }

    fun triggerSync() {
        Log.d("SYNC_DEBUG", "🔥 Profile UI triggered sync")
        patientRepository.triggerSync()
    }

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
