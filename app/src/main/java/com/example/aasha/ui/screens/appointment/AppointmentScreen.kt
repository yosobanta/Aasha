package com.example.aasha.ui.screens.appointment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.domain.model.Patient
import com.example.aasha.viewmodel.AppointmentViewModel
import com.example.aasha.viewmodel.BookingResult
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.aasha.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    onBack: () -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel()
) {
    val patients by viewModel.patients.collectAsState()
    val bookingResult by viewModel.bookingResult.collectAsState()
    
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply { 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis) }
    var selectedTimeSlot by remember { mutableStateOf<Long?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    LaunchedEffect(bookingResult) {
        if (bookingResult is BookingResult.Success) {
            showConfirmation = true
            viewModel.clearBookingResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_appointment)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Selection
            Text(stringResource(R.string.select_patient), style = MaterialTheme.typography.titleMedium)
            PatientDropdown(
                patients = patients,
                selectedPatient = selectedPatient,
                onPatientSelected = { selectedPatient = it }
            )

            // Date Selection
            Text(stringResource(R.string.select_date), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)))
            }

            // Time Slot Selection
            Text(stringResource(R.string.select_time_slot), style = MaterialTheme.typography.titleMedium)
            TimeSlotGrid(
                selectedDate = selectedDate,
                selectedTimeSlot = selectedTimeSlot,
                onTimeSlotSelected = { selectedTimeSlot = it },
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val patient = selectedPatient
                    val slot = selectedTimeSlot
                    if (patient != null && slot != null) {
                        viewModel.bookAppointment(patient.id, patient.name, slot)
                    }
                },
                enabled = selectedPatient != null && selectedTimeSlot != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.confirm_booking))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = it
                        selectedTimeSlot = null // Reset time slot when date changes
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.success)) },
            text = { Text(stringResource(R.string.appointment_success)) },
            confirmButton = {
                TextButton(onClick = { 
                    showConfirmation = false
                    onBack()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    if (bookingResult is BookingResult.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBookingResult() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text((bookingResult as BookingResult.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearBookingResult() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDropdown(
    patients: List<Patient>,
    selectedPatient: Patient?,
    onPatientSelected: (Patient) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedPatient?.name ?: stringResource(R.string.select_a_patient),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            patients.forEach { patient ->
                DropdownMenuItem(
                    text = { Text(patient.name) },
                    onClick = {
                        onPatientSelected(patient)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimeSlotGrid(
    selectedDate: Long,
    selectedTimeSlot: Long?,
    onTimeSlotSelected: (Long) -> Unit,
    viewModel: AppointmentViewModel
) {
    val timeSlots = remember(selectedDate) {
        val slots = mutableListOf<Long>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        
        for (i in 0 until 16) { // 9 AM to 5 PM, every 30 mins
            slots.add(calendar.timeInMillis)
            calendar.add(Calendar.MINUTE, 30)
        }
        slots
    }

    Box(modifier = Modifier.height(250.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(timeSlots) { slot ->
                val isAvailable = viewModel.isSlotAvailable(slot)
                val isSelected = selectedTimeSlot == slot
                val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(slot))
                
                FilterChip(
                    selected = isSelected,
                    onClick = { if (isAvailable) onTimeSlotSelected(slot) else {} },
                    label = { Text(timeString) },
                    enabled = isAvailable,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
