package com.example.aasha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aasha.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashRoute {
    object Idle : SplashRoute()
    object Login : SplashRoute()
    object MpinVerification : SplashRoute()
    object Dashboard : SplashRoute()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _route = MutableStateFlow<SplashRoute>(SplashRoute.Idle)
    val route: StateFlow<SplashRoute> = _route.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn.first()
            if (isLoggedIn) {
                _route.value = SplashRoute.MpinVerification
            } else {
                _route.value = SplashRoute.Login
            }
        }
    }
}
