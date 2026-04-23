package com.nutriscan.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nutriscan.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  Settings Screen — Thème, langue, cache
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    var selectedLanguage by remember { mutableStateOf("fr") }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Vider le cache") },
            text = { Text("Les données hors-ligne seront supprimées. Vos favoris resteront intacts.") },
            confirmButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Vider", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {

            // ── Apparence ────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Apparence")
                SettingsToggleRow(
                    icon = if (isDarkMode == true) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    title = "Mode sombre",
                    subtitle = "Adapter l'affichage à la luminosité",
                    checked = isDarkMode ?: false,
                    onToggle = { settingsViewModel.toggleDarkMode() }
                )
            }

            // ── Langue ───────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Langue")
                listOf("fr" to "Français 🇫🇷", "ar" to "العربية 🇲🇦").forEach { (code, label) ->
                    SettingsRadioRow(
                        icon = Icons.Filled.Language,
                        title = label,
                        selected = selectedLanguage == code,
                        onSelect = { selectedLanguage = code }
                    )
                }
            }

            // ── Données ──────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Données")
                SettingsActionRow(
                    icon = Icons.Filled.Storage,
                    title = "Vider le cache",
                    subtitle = "Libérer l'espace disque",
                    onClick = { showClearCacheDialog = true }
                )
                SettingsInfoRow(
                    icon = Icons.Filled.Info,
                    title = "Sources de données",
                    subtitle = "USDA FoodData Central + Open Food Facts"
                )
            }

            // ── À propos ─────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("À propos")
                SettingsInfoRow(
                    icon = Icons.Filled.Apps,
                    title = "NutriScan",
                    subtitle = "Version 1.0.0"
                )
                SettingsInfoRow(
                    icon = Icons.Filled.Shield,
                    title = "Confidentialité",
                    subtitle = "Aucune donnée personnelle collectée"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Composants atomiques
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRadioRow(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            RadioButton(selected = selected, onClick = onSelect)
        },
        modifier = Modifier.then(
            if (!selected) Modifier else Modifier
        )
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier
)
