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
    
    val vaccineList = listOf("BCG", "OPV", "DPT", "Hepatitis B", "Measles", "Vitamin A")

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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val patient = selectedPatient
                    if (patient != null && vaccineName.isNotBlank()) {
                        val vaccination = Vaccination(
                            patientId = patient.id,
                            patientName = patient.name,
                            vaccineName = vaccineName,
                            doseNumber = doseNumber,
                            dateAdministered = System.currentTimeMillis(),
                            remarks = notes
                        )
                        patientViewModel.addVaccination(vaccination)
                        onBack()
                    }
                },
                enabled = selectedPatient != null && vaccineName.isNotBlank(),
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
