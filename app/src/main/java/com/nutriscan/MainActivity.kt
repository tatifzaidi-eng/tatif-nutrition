package com.nutriscan

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.presentation.navigation.NutriScanNavHost
import com.nutriscan.presentation.theme.NutriScanTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  Application Hilt
// ─────────────────────────────────────────────────────────────────────────────

@HiltAndroidApp
class NutriScanApp : Application()

// ─────────────────────────────────────────────────────────────────────────────
//  DataStore pour les préférences utilisateur
// ─────────────────────────────────────────────────────────────────────────────

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "nutriscan_prefs")

private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

class SettingsRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SettingsViewModel
// ─────────────────────────────────────────────────────────────────────────────

@dagger.hilt.android.lifecycle.HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean?> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDarkMode() {
        viewModelScope.launch {
            settingsRepository.setDarkMode(!(isDarkMode.value ?: false))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Extend content into system bars (edge-to-edge)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkModePref by settingsViewModel.isDarkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()

            // Si la préférence n'est pas encore chargée, on utilise le thème système
            val useDark = isDarkModePref ?: systemDark

            NutriScanTheme(darkTheme = useDark) {
                NutriScanNavHost()
            }
        }
    }
}
