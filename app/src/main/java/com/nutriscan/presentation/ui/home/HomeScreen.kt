package com.nutriscan.presentation.ui.home

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.*
import com.nutriscan.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchFoodsUseCase: SearchFoodsUseCase,
    private val getHistoryUseCase: GetSearchHistoryUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<UiState<List<Food>>> = _query
        .debounce(300L).distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(UiState.Empty)
            else searchFoodsUseCase(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Empty)

    val history: StateFlow<List<Food>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }
    fun clearQuery() { _query.value = "" }
}

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onFoodClick: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val query by vm.query.collectAsState()
    val results by vm.searchResults.collectAsState()
    val history by vm.history.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NutriScan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Analysez vos aliments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text("🥗", fontSize = 20.sp) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Rechercher un aliment…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = vm::clearQuery) {
                        Icon(Icons.Filled.Close, null)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                )
            )
        }

        AnimatedContent(targetState = query.isNotBlank(), label = "search") { isSearching ->
            if (isSearching) {
                when (val s = results) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucun résultat", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = MaterialTheme.colorScheme.error) }
                    is UiState.Success -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(s.data, key = { it.id }) { FoodCard(it) { onFoodClick(it.id) } }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (history.isNotEmpty()) {
                        item { Text("Récemment consultés", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                        items(history.take(5), key = { "h${it.id}" }) { FoodCard(it) { onFoodClick(it.id) } }
                    }
                    item { Text("Populaires", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                    items(listOf("Avocat 🥑", "Saumon 🐟", "Épinards 🥬", "Quinoa 🌾", "Amandes 🥜", "Lentilles 🫘")) { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(16.dp))
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodCard(food: Food, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text(food.category.emoji, fontSize = 26.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(food.nameFr.ifBlank { food.name }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val kcal = food.calories.roundToInt()
                    val (bg, fg) = when {
                        kcal < 60 -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                        kcal < 200 -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary)
                        else -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
                    }
                    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                        Text("$kcal kcal", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Medium)
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                        Text("P ${food.proteins.roundToInt()}g", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(20.dp))
        }
    }
}
