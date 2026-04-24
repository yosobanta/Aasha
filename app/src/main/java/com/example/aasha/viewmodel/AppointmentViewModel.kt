package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.repository.MainRepository
import com.example.aasha.data.repository.PatientRepository
import com.example.aasha.domain.model.Appointment
import com.example.aasha.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookingResult {
    object Success : BookingResult()
    data class Error(val message: String) : BookingResult()
}

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val repository: MainRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {
    
    val appointments: StateFlow<List<Appointment>> = repository.appointments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val patients: StateFlow<List<Patient>> = patientRepository.patients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _bookingResult = MutableStateFlow<BookingResult?>(null)
    val bookingResult: StateFlow<BookingResult?> = _bookingResult.asStateFlow()

    fun bookAppointment(patientId: String, patientName: String, dateTime: Long) {
        viewModelScope.launch {
            val isSlotTaken = appointments.value.any { it.dateTime == dateTime }
            if (isSlotTaken) {
                _bookingResult.value = BookingResult.Error("Slot already booked")
                return@launch
            }
            
            val appointment = Appointment(
                patientId = patientId,
                patientName = patientName,
                dateTime = dateTime
            )
            repository.saveAppointment(appointment)
            _bookingResult.value = BookingResult.Success
        }
    }

    fun isSlotAvailable(dateTime: Long): Boolean {
        return appointments.value.none { it.dateTime == dateTime }
    }
    
    fun clearBookingResult() {
        _bookingResult.value = null
    }
}
