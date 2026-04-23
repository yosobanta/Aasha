package com.example.aasha.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.aasha.ui.navigation.Screen
import com.example.aasha.ui.screens.dashboard.PatientItem
import com.example.aasha.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val patients by viewModel.patients.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patients List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(patients) { patient ->
                PatientItem(patient) {
                    navController.navigate(Screen.PatientDetail.createRoute(patient.id))
                }
            }
        }
    }
}
