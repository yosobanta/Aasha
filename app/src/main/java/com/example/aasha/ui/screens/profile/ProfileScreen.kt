package com.example.aasha.ui.screens.profile

import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.res.stringResource
import com.example.aasha.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: androidx.navigation.NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncEvents.collectLatest { message ->
            val translatedMessage = when(message) {
                "Sync Started" -> context.getString(R.string.sync_started)
                "Sync Completed" -> context.getString(R.string.sync_completed)
                "Sync Failed" -> context.getString(R.string.sync_failed)
                else -> message
            }
            snackbarHostState.showSnackbar(translatedMessage)
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
                        Text(stringResource(R.string.performance_summary), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem(stringResource(R.string.patients), uiState.totalPatients.toString(), Modifier.weight(1f))
                            StatItem(stringResource(R.string.visits), uiState.totalVisits.toString(), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem(stringResource(R.string.vaccines), uiState.vaccinationsCompleted.toString(), Modifier.weight(1f))
                            StatItem(stringResource(R.string.meds), uiState.medicinesDistributed.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            // Settings / Options
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(stringResource(R.string.language_selection), Icons.Default.Language) { showLanguageDialog = true }

                    val mPinLabel = if (uiState.hasMpin) stringResource(R.string.change_mpin) else stringResource(R.string.set_mpin)
                    SettingRow(mPinLabel, Icons.Default.Lock) {
                        navController.navigate(com.example.aasha.ui.navigation.Screen.SetupMpin.route)
                    }

                    SettingRow(
                        label = stringResource(R.string.sync_status),
                        icon = Icons.Default.Sync,
                        badge = if (uiState.pendingCount > 0) uiState.pendingCount.toString() else null
                    ) { showSyncDialog = true }
                    SettingRow(stringResource(R.string.notification_settings), Icons.Default.Notifications) { }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingRow(stringResource(R.string.logout), Icons.Default.ExitToApp, color = Color.Red) { showLogoutDialog = true }
                }
            }
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text(stringResource(R.string.logout), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
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
                                .clickable { viewModel.changeLanguage(langCode) }
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
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text(stringResource(R.string.sync_status)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.last_synced))
                        Text(
                            text = if (uiState.lastSyncTime > 0) getRelativeTime(uiState.lastSyncTime, context) else stringResource(R.string.never),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pending_items))
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
                            Text(stringResource(R.string.syncing_in_progress), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text(if (uiState.isSyncing) stringResource(R.string.syncing) else stringResource(R.string.sync_now))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text(stringResource(R.string.close))
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

private fun getRelativeTime(timestamp: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val diffMinutes = diff / (60 * 1000)
    val diffHours = diff / (60 * 60 * 1000)
    
    return when {
        diff < 60 * 1000 -> context.getString(R.string.just_now)
        diff < 60 * 60 * 1000 -> context.getString(R.string.mins_ago, diffMinutes.toInt())
        diff < 36 * 60 * 60 * 1000 -> context.getString(R.string.hours_ago, diffHours.toInt())
        else -> {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

