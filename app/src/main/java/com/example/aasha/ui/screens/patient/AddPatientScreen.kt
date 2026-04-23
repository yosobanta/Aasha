package com.example.aasha.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.domain.model.Patient
import com.example.aasha.viewmodel.PatientViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var isPregnant by remember { mutableStateOf<Boolean?>(null) }
    
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicatePatient by remember { mutableStateOf<Patient?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                label = { Text("Age *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Gender *", fontWeight = FontWeight.Medium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = gender == "Male",
                    onClick = { 
                        gender = "Male"
                        isPregnant = null
                    },
                    label = { Text("Male") }
                )
                FilterChip(
                    selected = gender == "Female",
                    onClick = { gender = "Female" },
                    label = { Text("Female") }
                )
                FilterChip(
                    selected = gender == "Other",
                    onClick = { 
                        gender = "Other"
                        isPregnant = null
                    },
                    label = { Text("Other") }
                )
            }

            if (gender == "Female") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPregnant == true,
                        onCheckedChange = { isPregnant = it }
                    )
                    Text("Is Pregnant?")
                }
            }

            OutlinedTextField(
                value = village,
                onValueChange = { village = it },
                label = { Text("Village/Area *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phone = it },
                label = { Text("Phone Number (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = guardianName,
                onValueChange = { guardianName = it },
                label = { Text("Guardian Name (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val ageInt = age.toIntOrNull() ?: 0
                    if (name.isBlank() || ageInt <= 0 || gender.isBlank() || village.isBlank()) {
                        // Validation failed
                        return@Button
                    }
                    
                    scope.launch {
                        val existing = viewModel.isDuplicate(name, village, ageInt)
                        if (existing != null) {
                            duplicatePatient = existing
                            showDuplicateDialog = true
                        } else {
                            savePatient(viewModel, name, ageInt, gender, village, phone, guardianName, isPregnant, onBack)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && age.isNotBlank() && gender.isNotBlank() && village.isNotBlank()
            ) {
                Text("Save Patient")
            }
        }
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Possible Duplicate Found") },
            text = { Text("A similar patient (${duplicatePatient?.name}, ${duplicatePatient?.age} yrs) already exists in ${duplicatePatient?.village}. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    savePatient(viewModel, name, age.toInt(), gender, village, phone, guardianName, isPregnant, onBack)
                }) {
                    Text("Continue Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun savePatient(
    viewModel: PatientViewModel,
    name: String,
    age: Int,
    gender: String,
    village: String,
    phone: String,
    guardianName: String,
    isPregnant: Boolean?,
    onSuccess: () -> Unit
) {
    val patient = Patient(
        name = name,
        age = age,
        gender = gender,
        village = village,
        phone = phone.takeIf { it.isNotBlank() },
        guardianName = guardianName.takeIf { it.isNotBlank() },
        isPregnant = isPregnant
    )
    viewModel.addPatient(patient)
    onSuccess()
}
