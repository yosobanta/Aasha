package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.Vaccination
import com.example.aasha.domain.model.Visit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val patients: StateFlow<List<Patient>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.patients
            } else {
                repository.searchPatients(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun getVaccinations(patientId: String): StateFlow<List<Vaccination>> {
        return repository.getVaccinationsByPatient(patientId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun getVisits(patientId: String): StateFlow<List<Visit>> {
        return repository.getVisitsByPatient(patientId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun addPatient(patient: Patient) {
        viewModelScope.launch {
            repository.savePatient(patient)
        }
    }

    fun addVaccination(vaccination: Vaccination) {
        viewModelScope.launch {
            repository.saveVaccination(vaccination)
        }
    }

    fun addVisit(visit: Visit) {
        viewModelScope.launch {
            repository.saveVisit(visit)
        }
    }

    suspend fun isDuplicate(name: String, village: String, age: Int): Patient? {
        return repository.isDuplicate(name, village, age)
    }
}
