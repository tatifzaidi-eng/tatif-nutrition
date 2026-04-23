package com.nutriscan.presentation.ui.compare

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import com.nutriscan.presentation.theme.NutriColors
import com.nutriscan.presentation.ui.components.FoodListItem
import com.nutriscan.presentation.ui.components.SearchBar
import kotlin.math.roundToInt

private val chartColors = listOf(
    Color(0xFF378ADD), Color(0xFF1D9E75), Color(0xFFBA7517)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(viewModel: CompareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Comparaison",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.selectedFoods.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearAll) {
                            Text("Effacer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.selectedFoods.size < 3) {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onClear = { viewModel.onSearchQueryChanged("") },
                        placeholder = "Ajouter un aliment à comparer…"
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Résultats de recherche ──────────────────────────────────────────
            if (state.isSearching) {
                SearchDropdown(results = searchResults, onFoodClick = viewModel::addFood)
            } else {
                // ── Slots sélectionnés ──────────────────────────────────────────
                if (state.selectedFoods.isEmpty()) {
                    EmptyCompareHint()
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        // Chips des aliments sélectionnés
                        item { SelectedFoodsRow(state.selectedFoods, viewModel::removeFood) }

                        // Graphique en barres
                        if (state.selectedFoods.size >= 2) {
                            item { CompareBarChart(state.selectedFoods) }
                            item { CompareTable(state.selectedFoods, state.highlights) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Aliments sélectionnés
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectedFoodsRow(foods: List<Food>, onRemove: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        foods.forEachIndexed { i, food ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = chartColors[i].copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.5.dp, chartColors[i].copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                        Text(food.category.emoji, fontSize = 26.sp, modifier = Modifier.align(Alignment.Center))
                        IconButton(
                            onClick = { onRemove(food.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        food.nameFr,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${food.calories.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Slot vide
        repeat(3 - foods.size) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Graphique en barres (Canvas natif)
// ─────────────────────────────────────────────────────────────────────────────

private data class NutrientDef(val label: String, val getValue: (Food) -> Double, val maxRef: Double)

private val nutrientDefs = listOf(
    NutrientDef("Cal.", { it.calories }, 600.0),
    NutrientDef("Prot.", { it.proteins }, 40.0),
    NutrientDef("Gluc.", { it.carbohydrates }, 60.0),
    NutrientDef("Lip.", { it.fat }, 40.0),
    NutrientDef("Fibres", { it.fiber }, 20.0)
)

@Composable
private fun CompareBarChart(foods: List<Food>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Comparaison nutritionnelle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text("Pour 100g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            val animProgress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "chart"
            )
            val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
            val onSurface = MaterialTheme.colorScheme.onSurface

            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val groupCount = nutrientDefs.size
                val groupWidth = size.width / groupCount
                val barWidth = (groupWidth * 0.65f) / foods.size
                val maxBarH = size.height - 40f
                val labelY = size.height - 10f

                nutrientDefs.forEachIndexed { gi, def ->
                    val groupX = gi * groupWidth
                    foods.forEachIndexed { fi, food ->
                        val value = def.getValue(food)
                        val barH = ((value / def.maxRef) * maxBarH * animProgress).toFloat().coerceAtMost(maxBarH)
                        val barX = groupX + (groupWidth - barWidth * foods.size) / 2 + fi * barWidth
                        val barY = size.height - 40f - barH
                        drawRoundRect(
                            color = chartColors[fi],
                            topLeft = Offset(barX + 2, barY),
                            size = Size(barWidth - 4, barH),
                            cornerRadius = CornerRadius(4f)
                        )
                        // Valeur au-dessus
                        if (barH > 20f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                value.roundToInt().toString(),
                                barX + barWidth / 2,
                                barY - 4f,
                                android.graphics.Paint().apply {
                                    textSize = 22f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    color = chartColors[fi].copy(alpha = 0.9f).hashCode()
                                }
                            )
                        }
                    }
                    // Label catégorie
                    drawContext.canvas.nativeCanvas.drawText(
                        def.label,
                        groupX + groupWidth / 2,
                        labelY,
                        android.graphics.Paint().apply {
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = onSurface.copy(alpha = 0.6f).hashCode()
                        }
                    )
                }
            }

            // Légende
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                foods.forEachIndexed { i, food ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(chartColors[i]))
                        Text(food.nameFr, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tableau de comparaison
// ─────────────────────────────────────────────────────────────────────────────

private data class TableRow(val label: String, val getVal: (Food) -> Double, val unit: String, val higherIsBetter: Boolean)

private val tableRows = listOf(
    TableRow("Calories", { it.calories }, "kcal", false),
    TableRow("Protéines", { it.proteins }, "g", true),
    TableRow("Glucides", { it.carbohydrates }, "g", false),
    TableRow("Lipides", { it.fat }, "g", false),
    TableRow("Fibres", { it.fiber }, "g", true),
    TableRow("Sucres", { it.sugars }, "g", false),
)

@Composable
private fun CompareTable(foods: List<Food>, highlights: Map<String, String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tableau détaillé", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))

            // En-têtes
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(80.dp))
                foods.forEachIndexed { i, food ->
                    Text(
                        text = food.nameFr,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = chartColors[i],
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            tableRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(80.dp)
                    )
                    val values = foods.map { row.getVal(it) }
                    val best = if (row.higherIsBetter) values.max() else values.min()
                    foods.forEachIndexed { i, food ->
                        val v = row.getVal(food)
                        val isWinner = v == best && foods.size > 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isWinner) Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(chartColors[i].copy(alpha = 0.12f))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${v.roundToInt()} ${row.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                                color = if (isWinner) chartColors[i] else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dropdown résultats de recherche
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchDropdown(results: UiState<List<Food>>, onFoodClick: (Food) -> Unit) {
    when (results) {
        is UiState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Success -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results.data, key = { it.id }) { food ->
                FoodListItem(food = food, onClick = { onFoodClick(food) })
            }
        }
        else -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Aucun résultat", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCompareHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Balance, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text("Comparez jusqu'à 3 aliments", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Utilisez la barre de recherche ci-dessus pour ajouter des aliments", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
