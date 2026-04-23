package com.example.aasha.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.Vaccination
import com.example.aasha.domain.model.Visit
import com.example.aasha.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    var patient by remember { mutableStateOf<Patient?>(null) }
    val vaccinations by viewModel.getVaccinations(patientId).collectAsState()
    val visits by viewModel.getVisits(patientId).collectAsState()
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    LaunchedEffect(patientId) {
        // In a real app, fetch from viewModel
        // For now, find in the list of patients
        patient = viewModel.patients.value.find { it.id == patientId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit patient */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        if (patient == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val p = patient!!
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(text = p.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(text = "ID: ${p.id.take(8)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoRow("Age", "${p.age} years")
                            InfoRow("Gender", p.gender)
                            InfoRow("Village", p.village)
                            InfoRow("Phone", p.phone ?: "N/A")
                            InfoRow("Guardian", p.guardianName ?: "N/A")
                            if (p.isPregnant == true) {
                                InfoRow("Status", "Pregnant", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Text("Medical History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                if (visits.isEmpty() && vaccinations.isEmpty()) {
                    item {
                        Text("No recent visits or vaccinations recorded.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    // Visits Section
                    if (visits.isNotEmpty()) {
                        item {
                            Text("Recent Visits", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(visits.size) { index ->
                            val visit = visits[index]
                            HistoryCard(
                                title = visit.reason,
                                subtitle = visit.observations.ifBlank { "No notes" },
                                date = dateFormatter.format(Date(visit.visitDate))
                            )
                        }
                    }

                    // Vaccinations Section
                    if (vaccinations.isNotEmpty()) {
                        item {
                            Text("Vaccinations", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(vaccinations.size) { index ->
                            val vaccination = vaccinations[index]
                            HistoryCard(
                                title = vaccination.vaccineName,
                                subtitle = "Dose: ${vaccination.doseNumber}",
                                date = dateFormatter.format(Date(vaccination.dateAdministered)),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    title: String,
    subtitle: String,
    date: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = date, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = color)
    }
}
