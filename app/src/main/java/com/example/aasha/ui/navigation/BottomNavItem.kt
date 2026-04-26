package com.example.aasha.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, com.example.aasha.R.string.dashboard, Icons.Default.Home)
    object Add : BottomNavItem("add_modal", com.example.aasha.R.string.add, Icons.Default.Add)
    object Profile : BottomNavItem(Screen.Profile.route, com.example.aasha.R.string.profile, Icons.Default.Person)
}
