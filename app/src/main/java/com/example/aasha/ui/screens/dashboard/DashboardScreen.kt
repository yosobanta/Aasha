package com.example.aasha.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.aasha.domain.model.Patient
import com.example.aasha.ui.navigation.Screen
import com.example.aasha.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController, 
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.syncEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                HeaderSection(
                    uiState.ashaName, 
                    uiState.area, 
                    uiState.isSyncing
                )
            }

            // Sync Status Details
            item {
                SyncInfoSection(uiState.pendingCount, uiState.lastSyncTime)
            }

            // Stats Cards
            item {
                StatsSection(
                    uiState.appointmentsToday, 
                    uiState.vaccinationsDue, 
                    uiState.patients.size,
                    onPatientsClick = { navController.navigate(Screen.PatientList.route) }
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search Patients") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (searchQuery.isNotEmpty()) {
                item {
                    Text("Search Results", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                items(uiState.patients) { patient ->
                    PatientItem(patient) {
                        navController.navigate(Screen.PatientDetail.createRoute(patient.id))
                    }
                }
            } else {
                // Quick Actions
                item {
                    QuickActionsSection(navController, uiState.isSyncing) {
                        viewModel.triggerSync()
                    }
                }

                // Today's Tasks
                item {
                    Text("Today's Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                items(uiState.tasks) { task ->
                    TaskItem(
                        task.patientName,
                        task.taskType,
                        task.isCompleted,
                        onToggle = { viewModel.toggleTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PatientItem(patient: Patient, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(patient.name.take(1))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(patient.name, fontWeight = FontWeight.Bold)
                Text("${patient.age} yrs • ${patient.gender}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HeaderSection(name: String, area: String, isSyncing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Namaste, $name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = area, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        
        // Sync Status Indicator
        Surface(
            color = if (isSyncing) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Green, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSyncing) "Syncing..." else "Synced",
                    fontSize = 12.sp,
                    color = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun SyncInfoSection(pendingCount: Int, lastSyncTime: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudUpload, 
                    contentDescription = null, 
                    tint = if (pendingCount > 0) Color(0xFFF57C00) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (pendingCount > 0) "$pendingCount items pending" else "All data synced",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = if (lastSyncTime > 0) "Last: ${formatTimestamp(lastSyncTime)}" else "Never synced",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun StatsSection(appointments: Int, vaccinations: Int, patients: Int, onPatientsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Appts", appointments.toString(), Color(0xFFE3F2FD), Modifier.weight(1f))
        StatCard("Vaccine", vaccinations.toString(), Color(0xFFF3E5F5), Modifier.weight(1f))
        StatCard("Patients", patients.toString(), Color(0xFFE8F5E9), Modifier.weight(1f), onClick = onPatientsClick)
    }
}

@Composable
fun StatCard(label: String, value: String, bgColor: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = label, fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController, isSyncing: Boolean, onSyncClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton("New Patient", Icons.Default.Add, Modifier.weight(1f)) {
            navController.navigate(Screen.AddPatient.route)
        }
        QuickActionButton(
            label = if (isSyncing) "Syncing..." else "Sync Now", 
            icon = Icons.Default.Sync, 
            modifier = Modifier.weight(1f),
            enabled = !isSyncing,
            onClick = onSyncClick
        )
    }
}

@Composable
fun QuickActionButton(
    label: String, 
    icon: ImageVector, 
    modifier: Modifier, 
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun TaskItem(name: String, type: String, isCompleted: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = type, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCompleted) Color.Green else Color.Gray
                )
            }
        }
    }
}
