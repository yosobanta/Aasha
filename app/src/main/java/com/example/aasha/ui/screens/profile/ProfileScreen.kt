package com.example.aasha.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.aasha.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Info
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.name.take(1),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = uiState.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = uiState.role, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(text = uiState.area, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Performance Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Performance Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem("Patients", uiState.totalPatients.toString(), Modifier.weight(1f))
                            StatItem("Visits", uiState.totalVisits.toString(), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem("Vaccines", uiState.vaccinationsCompleted.toString(), Modifier.weight(1f))
                            StatItem("Meds", uiState.medicinesDistributed.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            // Settings / Options
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow("Language Selection", Icons.Default.Language) { showLanguageDialog = true }
                    SettingRow(
                        label = "Sync Status", 
                        icon = Icons.Default.Sync,
                        badge = if (uiState.pendingCount > 0) uiState.pendingCount.toString() else null
                    ) { showSyncDialog = true }
                    SettingRow("Notification Settings", Icons.Default.Notifications) { }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingRow("Logout", Icons.Default.ExitToApp, color = Color.Red) { showLogoutDialog = true }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? You will need to enter your MPIN to login again.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    val languages = listOf(
                        "English" to "en",
                        "Hindi (हिन्दी)" to "hi",
                        "Bengali (বাংলা)" to "bn",
                        "Marathi (मराठी)" to "mr"
                    )
                    languages.forEach { (langName, langCode) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = uiState.currentLanguage == langCode, 
                                onClick = { viewModel.changeLanguage(langCode) }
                            )
                            Text(langName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Last Synced:")
                        Text(
                            text = if (uiState.lastSyncTime > 0) getRelativeTime(uiState.lastSyncTime) else "Never",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pending Items:")
                        Text(
                            text = uiState.pendingCount.toString(),
                            color = if (uiState.pendingCount > 0) Color(0xFFF57C00) else Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (uiState.isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Syncing in progress...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text(if (uiState.isSyncing) "Syncing..." else "Sync Now")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingRow(
    label: String, 
    icon: ImageVector, 
    color: Color = MaterialTheme.colorScheme.onSurface,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontSize = 16.sp, color = color)
            Spacer(modifier = Modifier.weight(1f))
            if (badge != null) {
                Surface(
                    color = Color(0xFFF57C00),
                    shape = CircleShape,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val diffMinutes = diff / (60 * 1000)
    val diffHours = diff / (60 * 60 * 1000)
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "$diffMinutes mins ago"
        diff < 36 * 60 * 60 * 1000 -> "$diffHours hours ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
