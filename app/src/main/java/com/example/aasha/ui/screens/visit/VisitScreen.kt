package com.example.aasha.ui.screens.visit

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
import com.example.aasha.domain.model.Visit
import com.example.aasha.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitScreen(
    onBack: () -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val patients by patientViewModel.patients.collectAsState()
    
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var visitReason by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var treatmentProvided by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Patient Visit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Select Patient", style = MaterialTheme.typography.titleMedium)
            com.example.aasha.ui.screens.vaccination.PatientDropdown(
                patients = patients,
                selectedPatient = selectedPatient,
                onPatientSelected = { selectedPatient = it }
            )

            OutlinedTextField(
                value = visitReason,
                onValueChange = { visitReason = it },
                label = { Text("Reason for Visit") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = observations,
                onValueChange = { observations = it },
                label = { Text("Observations / Symptoms") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = treatmentProvided,
                onValueChange = { treatmentProvided = it },
                label = { Text("Treatment / Advice Given") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val patient = selectedPatient
                    if (patient != null && visitReason.isNotBlank()) {
                        val visit = Visit(
                            patientId = patient.id,
                            patientName = patient.name,
                            visitDate = System.currentTimeMillis(),
                            reason = visitReason,
                            observations = observations,
                            treatment = treatmentProvided
                        )
                        patientViewModel.addVisit(visit)
                        onBack()
                    }
                },
                enabled = selectedPatient != null && visitReason.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Visit Record")
            }
        }
    }
}
