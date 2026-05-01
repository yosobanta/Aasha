package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.data.repository.PatientRepository
import com.example.aasha.domain.model.Patient
import com.example.aasha.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.work.WorkInfo
import androidx.work.WorkManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class DashboardUiState(
    val ashaName: String = "Savitri Devi",
    val area: String = "Bishnupur Village",
    val isOnline: Boolean = false,
    val appointmentsToday: Int = 0,
    val vaccinationsDue: Int = 0,
    val ancCheckups: Int = 0,
    val patients: List<Patient> = emptyList(),
    val tasks: List<PatientTask> = emptyList(),
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val totalPatients: Int = 0,
    val totalAppointments: Int = 0,
    val totalVisits: Int = 0,
    val totalVaccinations: Int = 0
)

data class PatientTask(
    val id: String,
    val patientName: String,
    val taskType: String,
    val isCompleted: Boolean
)

data class DashboardStats(
    val totalPatients: Int = 0,
    val totalAppointments: Int = 0,
    val totalVisits: Int = 0,
    val totalVaccinations: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MainRepository,
    private val patientRepository: PatientRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSyncing = MutableStateFlow(false)

    private val _syncEvents = MutableSharedFlow<String>()
    val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    private val sessionFlow = combine(
        sessionManager.name,
        sessionManager.locality
    ) { name, locality -> name to locality }

    private val syncStatusFlow = combine(
        patientRepository.pendingCount,
        repository.pendingCount,
        patientRepository.lastSyncTime,
        _isSyncing
    ) { pPending, mPending, lastSync, syncing -> Triple(pPending + mPending, lastSync, syncing) }

    private val statsFlow = combine(
        patientRepository.patientCount,
        repository.appointmentCount,
        repository.visitCount,
        repository.vaccinationCount
    ) { p, a, v, vac -> 
        DashboardStats(p, a, v, vac)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) patientRepository.patients else patientRepository.searchPatients(query)
        },
        repository.appointments,
        sessionFlow,
        syncStatusFlow,
        statsFlow
    ) { patients, appointments, session, syncStatus, stats ->
        val (name, locality) = session
        val (pendingCount, lastSyncTime, isSyncing) = syncStatus

        DashboardUiState(
            patients = patients,
            ashaName = name ?: "Savitri Devi",
            area = locality ?: "Bishnupur Village",
            appointmentsToday = appointments.filter {
                val today = System.currentTimeMillis()
                it.dateTime in (today - 86400000..today + 86400000)
            }.size,
            tasks = appointments.map {
                PatientTask(it.id, it.patientName, "Follow-up", false)
            },
            pendingCount = pendingCount,
            lastSyncTime = lastSyncTime,
            isSyncing = isSyncing,
            isOnline = true,
            totalPatients = stats.totalPatients,
            totalAppointments = stats.totalAppointments,
            totalVisits = stats.totalVisits,
            totalVaccinations = stats.totalVaccinations
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        patientRepository.enqueuePeriodicSync()
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        WorkManager.getInstance(context)
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
        Log.d("SYNC_DEBUG", "🔥 UI triggered sync")
        patientRepository.triggerSync()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleTask(taskId: String) {
        // Logic to toggle task completion in Room
    }
}