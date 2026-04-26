package com.nutriscan.presentation.ui.compare

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.*
import com.nutriscan.domain.usecase.*
import com.nutriscan.presentation.ui.home.FoodCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.roundToInt

private val chartColors = listOf(
    Color(0xFF378ADD), Color(0xFF1D9E75), Color(0xFFBA7517)
)

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val searchFoodsUseCase: SearchFoodsUseCase,
    private val compareFoodsUseCase: CompareFoodsUseCase
) : ViewModel() {

    private val _selected = MutableStateFlow<List<Food>>(emptyList())
    val selected = _selected.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val highlights: StateFlow<Map<String, String>> = _selected
        .map { compareFoodsUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<UiState<List<Food>>> = _query
        .debounce(300L).distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(UiState.Empty)
            else searchFoodsUseCase(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Empty)

    fun onQueryChange(q: String) { _query.value = q }

    fun addFood(food: Food) {
        val cur = _selected.value
        if (cur.size >= 3 || cur.any { it.id == food.id }) return
        _selected.value = cur + food
        _query.value = ""
    }

    fun removeFood(id: String) { _selected.update { it.filter { f -> f.id != id } } }
    fun clearAll() { _selected.value = emptyList(); _query.value = "" }
}

@Composable
fun CompareScreen(
    contentPadding: PaddingValues,
    vm: CompareViewModel = hiltViewModel()
) {
    val selected by vm.selected.collectAsState()
    val query by vm.query.collectAsState()
    val results by vm.searchResults.collectAsState()
    val highlights by vm.highlights.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Comparaison", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (selected.isNotEmpty()) TextButton(onClick = vm::clearAll) { Text("Effacer", color = MaterialTheme.colorScheme.error) }
            }
            if (selected.size < 3) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query, onValueChange = vm::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ajouter un aliment…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Close, null) } },
                    singleLine = true, shape = RoundedCornerShape(14.dp)
                )
            }
        }

        if (query.isNotBlank()) {
            when (val s = results) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Success -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.data, key = { it.id }) { FoodCard(it) { vm.addFood(it) } }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucun résultat", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else if (selected.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⚖️", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text("Comparez jusqu'à 3 aliments", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Utilisez la barre de recherche pour ajouter des aliments", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        selected.forEachIndexed { i, food ->
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = chartColors[i].copy(0.08f)), border = BorderStroke(1.5.dp, chartColors[i].copy(0.4f))) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                                        Text(food.category.emoji, fontSize = 26.sp, modifier = Modifier.align(Alignment.Center))
                                        IconButton(onClick = { vm.removeFood(food.id) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(food.nameFr.ifBlank { food.name }, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                                    Text("${food.calories.roundToInt()} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        repeat(3 - selected.size) {
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f))) {
                                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (selected.size >= 2) {
                    item { CompareBarChart(selected) }
                    item { CompareTable(selected, highlights) }
                }
            }
        }
    }
}

@Composable
private fun CompareBarChart(foods: List<Food>) {
    val defs = listOf(
        "Cal" to ({ f: Food -> f.calories } to 600.0),
        "Prot" to ({ f: Food -> f.proteins } to 40.0),
        "Gluc" to ({ f: Food -> f.carbohydrates } to 60.0),
        "Lip" to ({ f: Food -> f.fat } to 40.0),
        "Fib" to ({ f: Food -> f.fiber } to 20.0)
    )
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Comparaison (100g)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(180.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
                defs.forEach { (labelFn, maxRef) ->
                    val (label, getFn) = labelFn
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(150.dp)) {
                            foods.forEachIndexed { i, food ->
                                val v = getFn(food)
                                val h = ((v / maxRef) * 150).coerceIn(4.0, 150.0).dp
                                Box(modifier = Modifier.width(16.dp).height(h).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(chartColors[i]))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                foods.forEachIndexed { i, food ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(chartColors[i]))
                        Text(food.nameFr.ifBlank { food.name }, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareTable(foods: List<Food>, highlights: Map<String, String>) {
    val rows = listOf(
        "Calories"  to ({ f: Food -> f.calories }      to false),
        "Protéines" to ({ f: Food -> f.proteins }      to true),
        "Glucides"  to ({ f: Food -> f.carbohydrates } to false),
        "Lipides"   to ({ f: Food -> f.fat }           to false),
        "Fibres"    to ({ f: Food -> f.fiber }         to true)
    )
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tableau comparatif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(80.dp))
                foods.forEachIndexed { i, f ->
                    Text(f.nameFr.ifBlank { f.name }.take(10), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = chartColors[i], textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            rows.forEach { (label, pair) ->
                val (getFn, higherBetter) = pair
                val vals = foods.map { getFn(it) }
                val best = if (higherBetter) vals.max() else vals.min()
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                    foods.forEachIndexed { i, f ->
                        val v = getFn(f); val winner = v == best && foods.size > 1
                        Box(modifier = Modifier.weight(1f).then(if (winner) Modifier.clip(RoundedCornerShape(6.dp)).background(chartColors[i].copy(0.12f)) else Modifier), contentAlignment = Alignment.Center) {
                            Text("${v.roundToInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = if (winner) FontWeight.Bold else FontWeight.Normal, color = if (winner) chartColors[i] else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(3.dp))
                        }
                    }
                }
            }
        }
    }
}
