package com.nutriscan.presentation.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.data.repository.FoodRepository
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.usecase.GetFavoritesUseCase
import com.nutriscan.domain.usecase.GetSearchHistoryUseCase
import com.nutriscan.presentation.ui.home.FoodCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistory: GetSearchHistoryUseCase,
    private val repo: FoodRepository
) : ViewModel() {
    val history: StateFlow<List<Food>> = getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun clearHistory() { viewModelScope.launch { repo.clearHistory() } }
}

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onFoodClick: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val history by vm.history.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Vider l'historique") },
            text = { Text("Supprimer tous les aliments consultés récemment ?") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); showDialog = false }) {
                    Text("Vider", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Historique", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (history.isNotEmpty()) Text(
                    "${history.size} aliment${if (history.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isNotEmpty()) {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (history.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🕘", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text("Aucun historique", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Les aliments consultés apparaîtront ici",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            val grouped = history.groupBy { food ->
                val cal = Calendar.getInstance().apply { timeInMillis = food.lastViewedAt }
                val today = Calendar.getInstance()
                val yest = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                when {
                    sameDay(cal, today) -> "Aujourd'hui"
                    sameDay(cal, yest)  -> "Hier"
                    else -> SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
                        .format(cal.time).replaceFirstChar { it.uppercase() }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (date, foods) ->
                    item(key = "d_$date") {
                        Text(
                            date,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(foods, key = { "h_${it.id}_${it.lastViewedAt}" }) { food ->
                        FoodCard(food) { onFoodClick(food.id) }
                    }
                }
            }
        }
    }
}

private fun sameDay(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFavorites: GetFavoritesUseCase
) : ViewModel() {
    val favorites: StateFlow<List<Food>> = getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun FavoritesScreen(
    contentPadding: PaddingValues,
    onFoodClick: (String) -> Unit,
    vm: FavoritesViewModel = hiltViewModel()
) {
    val favorites by vm.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Favoris", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (favorites.isNotEmpty()) Text(
                    "${favorites.size} aliment${if (favorites.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (favorites.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("❤️", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text("Aucun favori", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Appuyez sur ♡ dans la fiche d'un aliment pour l'ajouter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites, key = { it.id }) { food ->
                    FoodCard(food) { onFoodClick(food.id) }
                }
            }
        }
    }
}
