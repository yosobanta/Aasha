package com.example.aasha.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object LoginMpin : Screen("login_mpin")
    object Dashboard : Screen("dashboard")
    object PatientDetail : Screen("patient_detail/{patientId}") {
        fun createRoute(patientId: String) = "patient_detail/$patientId"
    }
    object Profile : Screen("profile")
    object Appointment : Screen("appointment")
    object AddPatient : Screen("add_patient")
    object Vaccination : Screen("vaccination")
    object Visit : Screen("visit")
    object PatientList : Screen("patient_list")
    object SetupMpin : Screen("setup_mpin")
}
