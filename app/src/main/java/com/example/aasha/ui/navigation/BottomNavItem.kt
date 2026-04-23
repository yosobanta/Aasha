package com.example.aasha.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Home)
    object Add : BottomNavItem("add_modal", "Add", Icons.Default.Add)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}
