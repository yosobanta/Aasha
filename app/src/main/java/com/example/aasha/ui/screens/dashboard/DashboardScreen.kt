package com.example.aasha.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.aasha.R
import com.example.aasha.domain.model.Patient
import com.example.aasha.ui.components.AashaCard
import com.example.aasha.ui.components.AashaTextField
import com.example.aasha.ui.components.PatientItem
import com.example.aasha.ui.navigation.Screen
import com.example.aasha.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var showQuickActions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncEvents.collectLatest { message ->
            val translatedMessage = when (message) {
                "Sync Started" -> context.getString(R.string.sync_started)
                "Sync Completed" -> context.getString(R.string.sync_completed)
                "Sync Failed" -> context.getString(R.string.sync_failed)
                else -> message
            }
            snackbarHostState.showSnackbar(translatedMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickActions = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Actions")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (showQuickActions) {
            QuickActionsBottomSheet(
                onDismiss = { showQuickActions = false },
                sheetState = sheetState,
                onAddPatient = { navController.navigate(Screen.AddPatient.route) },
                onRecordVaccination = { /* Navigate */ },
                onAddVisit = { /* Navigate */ },
                onScheduleAppt = { /* Navigate */ }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            item {
                HeaderSection(uiState.ashaName, uiState.area)
            }

            // High Visibility Sync Banner
            item {
                EnhancedSyncBanner(
                    isSyncing = uiState.isSyncing,
                    pendingCount = uiState.pendingCount,
                    onSyncClick = { viewModel.triggerSync() }
                )
            }

            // Key Statistics
            item {
                SectionTitle(stringResource(R.string.performance_summary))
                StatsGrid(
                    patients = uiState.totalPatients,
                    appointments = uiState.totalAppointments,
                    vaccinations = uiState.totalVaccinations,
                    visits = uiState.totalVisits,
                    onPatientsClick = { navController.navigate(Screen.PatientList.route) }
                )
            }

            // Search Bar
            item {
                AashaTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    label = stringResource(R.string.search_patients),
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.search_results)) }
                items(uiState.patients) { patient ->
                    PatientItem(patient) {
                        navController.navigate(Screen.PatientDetail.createRoute(patient.id))
                    }
                }
            } else {
                // Today's Tasks (Action Priority)
                item { SectionTitle(stringResource(R.string.todays_tasks)) }

                if (uiState.tasks.isEmpty()) {
                    item { EmptyTasksState() }
                } else {
                    items(uiState.tasks) { task ->
                        TaskItem(
                            name = task.patientName,
                            type = task.taskType,
                            isCompleted = task.isCompleted,
                            onToggle = { viewModel.toggleTask(task.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HeaderSection(name: String, area: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(R.string.namaste, name),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = area,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
private fun EnhancedSyncBanner(isSyncing: Boolean, pendingCount: Int, onSyncClick: () -> Unit) {
    val bgColor = if (isSyncing) MaterialTheme.colorScheme.primaryContainer
                 else if (pendingCount > 0) MaterialTheme.colorScheme.errorContainer
                 else MaterialTheme.colorScheme.secondaryContainer
    
    val contentColor = if (isSyncing) MaterialTheme.colorScheme.onPrimaryContainer
                     else if (pendingCount > 0) MaterialTheme.colorScheme.onErrorContainer
                     else MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = contentColor)
                } else {
                    Icon(
                        if (pendingCount > 0) Icons.Default.SyncProblem else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isSyncing) stringResource(R.string.syncing)
                               else if (pendingCount > 0) stringResource(R.string.items_pending, pendingCount)
                               else stringResource(R.string.all_data_synced),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    if (pendingCount > 0 && !isSyncing) {
                        Text(
                            text = "Tap to sync manually",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            if (!isSyncing && pendingCount > 0) {
                IconButton(onClick = onSyncClick) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync Now", tint = contentColor)
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(
    patients: Int,
    appointments: Int,
    vaccinations: Int,
    visits: Int,
    onPatientsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.patients), patients.toString(), Icons.Default.People, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onClick = onPatientsClick)
            StatCard(stringResource(R.string.appts), appointments.toString(), Icons.Default.Event, Color(0xFF8B5CF6), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.vaccine), vaccinations.toString(), Icons.Default.Vaccines, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            StatCard(stringResource(R.string.recent_visits), visits.toString(), Icons.Default.Home, Color(0xFFF59E0B), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {},
        enabled = onClick != null,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun TaskItem(name: String, type: String, isCompleted: Boolean, onToggle: () -> Unit) {
    AashaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Assignment,
                            contentDescription = null,
                            tint = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(text = type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun EmptyTasksState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "All tasks completed for today!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onAddPatient: () -> Unit,
    onRecordVaccination: () -> Unit,
    onAddVisit: () -> Unit,
    onScheduleAppt: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            QuickActionItem(
                label = stringResource(R.string.add_new_patient),
                icon = Icons.Default.PersonAdd,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onAddPatient(); onDismiss() }
            )
            QuickActionItem(
                label = stringResource(R.string.record_vaccination),
                icon = Icons.Default.Vaccines,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onRecordVaccination(); onDismiss() }
            )
            QuickActionItem(
                label = stringResource(R.string.record_visit),
                icon = Icons.Default.HomeWork,
                color = Color(0xFFF59E0B),
                onClick = { onAddVisit(); onDismiss() }
            )
            QuickActionItem(
                label = stringResource(R.string.schedule_appointment),
                icon = Icons.Default.Event,
                color = Color(0xFF8B5CF6),
                onClick = { onScheduleAppt(); onDismiss() }
            )
        }
    }
}

@Composable
private fun QuickActionItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
