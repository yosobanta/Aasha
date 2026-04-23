package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.domain.model.Patient
import com.example.aasha.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val ashaName: String = "Savitri Devi",
    val area: String = "Bishnupur Village",
    val isOnline: Boolean = false,
    val appointmentsToday: Int = 0,
    val vaccinationsDue: Int = 0,
    val ancCheckups: Int = 0,
    val patients: List<Patient> = emptyList(),
    val tasks: List<PatientTask> = emptyList()
)

data class PatientTask(
    val id: String,
    val patientName: String,
    val taskType: String,
    val isCompleted: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<DashboardUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) repository.patients else repository.searchPatients(query)
        },
        repository.appointments,
        sessionManager.workerId,
        sessionManager.locality
    ) { patients, appointments, workerId, locality ->
        DashboardUiState(
            patients = patients,
            ashaName = if (workerId != null) "Aasha Worker ($workerId)" else "Savitri Devi",
            area = locality ?: "Bishnupur Village",
            appointmentsToday = appointments.filter { 
                val today = System.currentTimeMillis()
                it.dateTime in (today - 86400000..today + 86400000)
            }.size,
            tasks = appointments.map { 
                PatientTask(it.id, it.patientName, "Follow-up", false) 
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleTask(taskId: String) {
        // Logic to toggle task completion in Room
    }
}
