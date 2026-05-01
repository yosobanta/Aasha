package com.example.aasha.ui.screens.vaccination

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.Vaccination
import com.example.aasha.viewmodel.PatientViewModel
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.aasha.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationScreen(
    onBack: () -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val patients by patientViewModel.patients.collectAsState()
    
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var vaccineName by remember { mutableStateOf("") }
    var doseNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var requiresBooster by remember { mutableStateOf(false) }
    var boosterDate by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Pair<Int, Int>?>(null) } // hour, minute

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return utcTimeMillis > calendar.timeInMillis
            }
        }
    )

    val timePickerState = rememberTimePickerState()

    val vaccineList = listOf("BCG", "OPV", "DPT", "Hepatitis B", "Measles", "Pentavalent","Japanese Encephalitis (JE)","DPT Booster-1", "OPV Booster", "MR-2")

    val isBoosterValid = !requiresBooster || (boosterDate != null && reminderTime != null)
    val canSave = selectedPatient != null && vaccineName.isNotBlank() && isBoosterValid
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.record_vaccination)) },
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
            Text(stringResource(R.string.select_patient), style = MaterialTheme.typography.titleMedium)
            PatientDropdown(
                patients = patients,
                selectedPatient = selectedPatient,
                onPatientSelected = { selectedPatient = it }
            )

            Text(stringResource(R.string.vaccine_info), style = MaterialTheme.typography.titleMedium)
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = vaccineName,
                    onValueChange = { vaccineName = it },
                    label = { Text(stringResource(R.string.vaccine_name)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    vaccineList.forEach { vaccine ->
                        DropdownMenuItem(
                            text = { Text(vaccine) },
                            onClick = {
                                vaccineName = vaccine
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = doseNumber,
                onValueChange = { doseNumber = it },
                label = { Text(stringResource(R.string.dose_number)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.additional_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.requires_booster), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = requiresBooster,
                    onCheckedChange = { requiresBooster = it }
                )
            }

            if (requiresBooster) {
                val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(boosterDate?.let { stringResource(R.string.booster_date_label, sdf.format(Date(it))) } ?: stringResource(R.string.select_booster_date))
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(reminderTime?.let { (h, m) -> stringResource(R.string.reminder_time_label, String.format("%02d:%02d", h, m)) } ?: stringResource(R.string.select_reminder_time))
                }
                
                if (boosterDate == null || reminderTime == null) {
                    Text(
                        text = stringResource(R.string.booster_selection_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = stringResource(R.string.reminder_helper_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            boosterDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            reminderTime = Pair(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val patient = selectedPatient
                    if (patient != null && vaccineName.isNotBlank() && isBoosterValid) {
                        val reminderTimestamp = if (requiresBooster && boosterDate != null && reminderTime != null) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = boosterDate!!
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            cal.set(Calendar.HOUR_OF_DAY, reminderTime!!.first)
                            cal.set(Calendar.MINUTE, reminderTime!!.second)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        } else null

                        val vaccination = Vaccination(
                            patientId = patient.id,
                            patientName = patient.name,
                            vaccineName = vaccineName,
                            doseNumber = doseNumber,
                            dateAdministered = System.currentTimeMillis(),
                            remarks = notes,
                            requiresBooster = requiresBooster,
                            boosterDate = boosterDate,
                            reminderTime = reminderTimestamp
                        )
                        patientViewModel.addVaccination(vaccination)
                        onBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_vaccination))
            }
        }
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
