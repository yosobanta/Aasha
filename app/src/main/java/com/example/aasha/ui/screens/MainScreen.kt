package com.example.aasha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.ui.navigation.BottomNavItem
import com.example.aasha.ui.navigation.Screen
import com.example.aasha.ui.screens.dashboard.DashboardScreen
import com.example.aasha.ui.screens.login.LoginScreen
import com.example.aasha.ui.screens.login.ScreenMode
import com.example.aasha.ui.screens.profile.ProfileScreen
import com.example.aasha.ui.screens.splash.SplashScreen
import com.example.aasha.ui.screens.appointment.AppointmentScreen
import com.example.aasha.ui.screens.patient.AddPatientScreen
import com.example.aasha.ui.screens.patient.PatientDetailScreen
import com.example.aasha.ui.screens.patient.PatientListScreen
import com.example.aasha.ui.screens.vaccination.VaccinationScreen
import com.example.aasha.ui.screens.visit.VisitScreen
import com.example.aasha.viewmodel.PatientViewModel
import com.example.aasha.viewmodel.SessionViewModel
import androidx.compose.ui.res.stringResource
import com.example.aasha.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val patientViewModel: PatientViewModel = hiltViewModel()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val currentDestination = navBackStackEntry?.destination
    
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val bottomBarScreens = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Add,
        BottomNavItem.Profile
    )

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Dashboard.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        val title = stringResource(item.titleResId)
                        
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = selected,
                            onClick = {
                                if (item.route == "add_modal") {
                                    showBottomSheet = true
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToMpin = {
                        navController.navigate(Screen.LoginMpin.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    showMpinInitially = false
                )
            }
            composable(Screen.LoginMpin.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.LoginMpin.route) { inclusive = true }
                        }
                    },
                    showMpinInitially = true
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(Screen.Appointment.route) {
                AppointmentScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.AddPatient.route) {
                AddPatientScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = patientViewModel
                )
            }
            composable(Screen.PatientDetail.route) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                PatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    viewModel = patientViewModel
                )
            }
            composable(Screen.Vaccination.route) {
                VaccinationScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Visit.route) {
                VisitScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.PatientList.route) {
                PatientListScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    viewModel = patientViewModel
                )
            }
            composable(Screen.Calendar.route) {
                // CalendarScreen implementation
                com.example.aasha.ui.screens.appointment.CalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SetupMpin.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack()
                    },
                    showMpinInitially = false,
                    initialMode = ScreenMode.SetupMpin
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                AddOptionsContent(onOptionSelected = { option ->
                    showBottomSheet = false
                    when (option) {
                        "add_patient" -> navController.navigate(Screen.AddPatient.route)
                        "appointment" -> navController.navigate(Screen.Appointment.route)
                        "vaccination" -> navController.navigate(Screen.Vaccination.route)
                        "visit" -> navController.navigate(Screen.Visit.route)
                    }
                })
            }
        }
    }
}

@Composable
fun AddOptionsContent(onOptionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        AddOptionItem(stringResource(R.string.add_new_patient), Icons.Default.Add) { onOptionSelected("add_patient") }
        AddOptionItem(stringResource(R.string.new_appointment), Icons.Default.Add) { onOptionSelected("appointment") }
        AddOptionItem(stringResource(R.string.record_vaccination), Icons.Default.Add) { onOptionSelected("vaccination") }
        AddOptionItem(stringResource(R.string.add_visit), Icons.Default.Add) { onOptionSelected("visit") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOptionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontWeight = FontWeight.Medium)
        }
    }
}
