package com.nutriscan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "nutriscan_prefs")

private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

@Singleton
class SettingsRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_MODE_KEY] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {
    val isDarkMode: StateFlow<Boolean?> = repo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDarkMode() {
        viewModelScope.launch {
            repo.setDarkMode(!(isDarkMode.value ?: false))
        }
    }
}
