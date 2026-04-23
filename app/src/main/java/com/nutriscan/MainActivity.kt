package com.nutriscan

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nutriscan.presentation.navigation.NutriScanNavHost
import com.nutriscan.presentation.theme.NutriScanTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NutriScanApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val isDark by vm.isDarkMode.collectAsState()
            NutriScanTheme(darkTheme = isDark ?: isSystemInDarkTheme()) {
                NutriScanNavHost()
            }
        }
    }
}
