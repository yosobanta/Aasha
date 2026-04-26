package com.example.aasha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import com.example.aasha.ui.screens.MainScreen
import com.example.aasha.ui.theme.AashaTheme
import com.example.aasha.util.LocaleHelper
import com.example.aasha.viewmodel.SessionViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val language by sessionViewModel.language.collectAsState()
            val context = LocalContext.current
            
            // Apply locale when language changes
            val wrappedContext = androidx.compose.runtime.remember(language) {
                LocaleHelper.wrapContext(context, language)
            }
            
            CompositionLocalProvider(LocalContext provides wrappedContext) {
                AashaTheme {
                    MainScreen()
                }
            }
        }
    }
}
