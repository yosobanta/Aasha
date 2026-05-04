package com.example.aasha.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.R
import com.example.aasha.domain.model.Patient
import com.example.aasha.ui.components.AashaButton
import com.example.aasha.ui.components.AashaButtonStyle
import com.example.aasha.ui.components.AashaCard
import com.example.aasha.ui.components.AashaTextField
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(patientId) {
        patient = viewModel.patients.value.find { it.id == patientId }
    }

    if (showDeleteDialog) {
        DeletePatientDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { pw ->
                if (viewModel.verifyPassword(pw)) {
                    viewModel.deletePatient(patientId)
                    showDeleteDialog = false
                    onBack()
                    true
                } else false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.patient_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit patient */ }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (patient == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val p = patient!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Patient Header
                item {
                    PatientProfileHeader(p)
                }

                // Personal Info Section
                item {
                    SectionTitle(stringResource(R.string.personal_info))
                    AashaCard(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(Icons.Default.Cake, stringResource(R.string.age), stringResource(R.string.yrs_gender, p.age, p.gender))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        InfoRow(Icons.Default.LocationOn, stringResource(R.string.village), p.village)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        InfoRow(Icons.Default.Phone, stringResource(R.string.phone), p.phone ?: "Not Provided")
                        if (p.guardianName != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            InfoRow(Icons.Default.SupervisorAccount, stringResource(R.string.guardian), p.guardianName!!)
                        }
                    }
                }

                // Medical History Section
                item {
                    SectionTitle(stringResource(R.string.medical_history))
                }

                if (visits.isEmpty() && vaccinations.isEmpty()) {
                    item { EmptyHistoryState() }
                } else {
                    if (visits.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.recent_visits),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(visits) { visit ->
                            HistoryCard(
                                title = visit.reason,
                                subtitle = visit.observations.ifBlank { stringResource(R.string.no_notes) },
                                date = dateFormatter.format(Date(visit.visitDate)),
                                icon = Icons.Default.MedicalServices,
                                iconColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (vaccinations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.vaccinations),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(vaccinations) { vaccination ->
                            HistoryCard(
                                title = vaccination.vaccineName,
                                subtitle = stringResource(R.string.dose_prefix, vaccination.doseNumber),
                                date = dateFormatter.format(Date(vaccination.dateAdministered)),
                                icon = Icons.Default.Vaccines,
                                iconColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun PatientProfileHeader(patient: Patient) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = patient.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (patient.isPregnant == true) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.pregnant).uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.yrs_gender, patient.age, patient.gender),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryCard(title: String, subtitle: String, date: String, icon: ImageVector, iconColor: Color) {
    AashaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.no_records), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = "Use Quick Actions to add visits or vaccinations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun DeletePatientDialog(onDismiss: () -> Unit, onConfirm: (String) -> Boolean) {
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_patient), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.delete_patient_confirm))
                Spacer(modifier = Modifier.height(20.dp))
                AashaTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = stringResource(R.string.enter_password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null
                )
                if (passwordError != null) {
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onConfirm(password)) {
                        passwordError = "Incorrect password"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
