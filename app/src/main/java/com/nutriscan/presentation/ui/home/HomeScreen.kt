package com.nutriscan.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import com.nutriscan.presentation.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFoodClick: (String) -> Unit,
    onBarcodeScan: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val history by viewModel.recentHistory.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ── En-tête ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NutriScan",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analysez vos aliments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Bouton scanner code-barres
                    IconButton(
                        onClick = onBarcodeScan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scanner un code-barres",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Barre de recherche ──────────────────────────────────────────
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClear = viewModel::clearSearch,
                    placeholder = "Rechercher un aliment…"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Résultats de recherche ou écran principal ───────────────────────
            AnimatedContent(
                targetState = searchQuery.isNotBlank(),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "search_content"
            ) { isSearching ->
                if (isSearching) {
                    SearchResultsContent(
                        state = searchResults,
                        onFoodClick = onFoodClick
                    )
                } else {
                    HomeContent(
                        history = history,
                        onFoodClick = onFoodClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    state: UiState<List<Food>>,
    onFoodClick: (String) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Empty -> EmptyView(message = "Aucun résultat trouvé")
        is UiState.Error -> ErrorView(message = state.message)
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${state.data.size} résultats",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(state.data, key = { it.id }) { food ->
                    FoodListItem(food = food, onClick = { onFoodClick(food.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    history: List<Food>,
    onFoodClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Accès rapide ────────────────────────────────────────────────────────
        item {
            SectionHeader(title = "Accès rapide")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quickAccessCategories) { cat ->
                    QuickCategoryChip(category = cat)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Historique récent ───────────────────────────────────────────────────
        if (history.isNotEmpty()) {
            item {
                SectionHeader(title = "Récemment consultés")
            }
            items(history.take(5), key = { "hist_${it.id}" }) { food ->
                FoodListItem(
                    food = food,
                    onClick = { onFoodClick(food.id) },
                    showTimestamp = true
                )
            }
        }

        // ── Suggestions populaires ──────────────────────────────────────────────
        item {
            SectionHeader(title = "Populaires")
        }
        items(popularFoods, key = { "pop_${it}" }) { name ->
            SuggestionItem(name = name, onClick = { /* Recherche directe */ })
        }
    }
}

private val quickAccessCategories = listOf(
    "🍎 Fruits", "🥦 Légumes", "🥩 Protéines",
    "🌾 Céréales", "🥛 Laitages", "🫘 Légumineuses"
)

private val popularFoods = listOf(
    "Avocat", "Saumon", "Épinards", "Quinoa",
    "Amandes", "Lentilles", "Banane", "Poulet grillé"
)

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
