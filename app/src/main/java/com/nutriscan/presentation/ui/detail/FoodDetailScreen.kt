package com.nutriscan.presentation.ui.detail

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.*
import com.nutriscan.domain.usecase.GetFoodDetailUseCase
import com.nutriscan.domain.usecase.ToggleFavoriteUseCase
import com.nutriscan.presentation.theme.NutriColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetail: GetFoodDetailUseCase,
    private val toggleFav: ToggleFavoriteUseCase
) : ViewModel() {
    private val foodId: String = checkNotNull(savedStateHandle["foodId"])
    val uiState: StateFlow<UiState<Food>> = getDetail(foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
    private val _portion = MutableStateFlow(100.0)
    val portion = _portion.asStateFlow()
    private val _tab = MutableStateFlow(0)
    val tab = _tab.asStateFlow()
    fun setTab(t: Int) { _tab.value = t }
    fun setPortion(g: Double) { _portion.value = g.coerceIn(10.0, 1000.0) }
    fun toggleFavorite() { viewModelScope.launch { toggleFav(foodId) } }
    fun adj(v: Double) = v * _portion.value / 100.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    vm: FoodDetailViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val portion by vm.portion.collectAsState()
    val tab by vm.tab.collectAsState()

    when (val s = state) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Empty -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Introuvable")
            }
        }
        is UiState.Success -> {
            val food = s.data
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                food.nameFr.ifBlank { food.name },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        },
                        actions = {
                            IconButton(onClick = vm::toggleFavorite) {
                                Icon(
                                    if (food.isFavorite) Icons.Filled.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                    null,
                                    tint = if (food.isFavorite) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            ) { pad ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(food.category.emoji, fontSize = 34.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    food.category.labelFr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                                )
                                Text(
                                    food.nameFr.ifBlank { food.name },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${vm.adj(food.calories).roundToInt()}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        " kcal  •  ${portion.roundToInt()}g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Macros grid
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Protéines", vm.adj(food.proteins), "g"),
                            Triple("Glucides", vm.adj(food.carbohydrates), "g"),
                            Triple("Lipides", vm.adj(food.fat), "g"),
                            Triple("Fibres", vm.adj(food.fiber), "g")
                        ).forEach { (label, value, unit) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${value.roundToInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        unit,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Macro ring
                    MacroRing(food, vm::adj)

                    // Portion slider
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Portion : ${portion.roundToInt()}g",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = portion.toFloat(),
                            onValueChange = { vm.setPortion(it.toDouble()) },
                            valueRange = 10f..500f,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(50.0, 100.0, 150.0, 200.0).forEach { g ->
                                FilterChip(
                                    selected = portion == g,
                                    onClick = { vm.setPortion(g) },
                                    label = { Text("${g.roundToInt()}g", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Tabs
                    TabRow(selectedTabIndex = tab) {
                        listOf("Nutrition", "Vitamines", "Bienfaits").forEachIndexed { i, t ->
                            Tab(
                                selected = tab == i,
                                onClick = { vm.setTab(i) },
                                text = { Text(t, fontSize = 13.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    when (tab) {
                        0 -> NutritionTab(food, vm::adj)
                        1 -> VitaminsTab(food, vm::adj)
                        2 -> BenefitsTab(food)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MacroRing(food: Food, adj: (Double) -> Double) {
    val prot = adj(food.proteins)
    val carbs = adj(food.carbohydrates)
    val fat = adj(food.fat)
    val total = (prot + carbs + fat).takeIf { it > 0 } ?: return
    val anim by animateFloatAsState(1f, tween(900), label = "ring")

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                    var start = -90f
                    listOf(
                        prot to NutriColors.Proteins,
                        carbs to NutriColors.Carbs,
                        fat to NutriColors.Fat
                    ).forEach { (v, c) ->
                        val sweep = (v / total * 360f * anim).toFloat()
                        drawArc(c, start, sweep, false, style = stroke)
                        start += sweep
                    }
                }
                Text(
                    "${(prot + carbs + fat).roundToInt()}g",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Protéines", prot, NutriColors.Proteins),
                    Triple("Glucides", carbs, NutriColors.Carbs),
                    Triple("Lipides", fat, NutriColors.Fat)
                ).forEach { (lbl, v, c) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                        Text(lbl, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                        Text("${v.roundToInt()}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("${(v / total * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionTab(food: Food, adj: (Double) -> Double) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("Calories", adj(food.calories) to 800.0, "kcal" to NutriColors.Calories),
            Triple("Protéines", adj(food.proteins) to 35.0, "g" to NutriColors.Proteins),
            Triple("Glucides", adj(food.carbohydrates) to 50.0, "g" to NutriColors.Carbs),
            Triple("Lipides", adj(food.fat) to 30.0, "g" to NutriColors.Fat),
            Triple("Fibres", adj(food.fiber) to 15.0, "g" to NutriColors.Fiber)
        ).forEach { (label, valuePair, unitColor) ->
            val (value, ref) = valuePair
            val (unit, color) = unitColor
            val prog by animateFloatAsState(
                (value / ref).coerceIn(0.0, 1.0).toFloat(),
                tween(600),
                label = label
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(90.dp)
                )
                LinearProgressIndicator(
                    progress = { prog },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "${value.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(52.dp).padding(start = 8.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun VitaminsTab(food: Food, adj: (Double) -> Double) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (food.vitamins.isNotEmpty()) {
            Text("Vitamines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.vitamins.forEach { (v, value) ->
                val a = adj(value)
                val pct = (a / v.dailyValue * 100).coerceIn(0.0, 150.0)
                val prog by animateFloatAsState(
                    (pct / 100f).toFloat().coerceIn(0f, 1f),
                    label = "v${v.name}"
                )
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(v.fullName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${a.roundToInt()} ${v.unit}  ${pct.roundToInt()}% AJR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NutriColors.Vitamins,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
        if (food.minerals.isNotEmpty()) {
            Text("Minéraux", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.minerals.forEach { (m, value) ->
                val a = adj(value)
                val pct = (a / m.dailyValue * 100).coerceIn(0.0, 150.0)
                val prog by animateFloatAsState(
                    (pct / 100f).toFloat().coerceIn(0f, 1f),
                    label = "m${m.name}"
                )
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.fullName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${a.roundToInt()} ${m.unit}  ${pct.roundToInt()}% AJR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NutriColors.Minerals,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
        if (food.vitamins.isEmpty() && food.minerals.isEmpty()) {
            Text("Données non disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BenefitsTab(food: Food) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (food.benefits.isNotEmpty()) {
            Text("Bienfaits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.benefits.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = NutriColors.LevelLow,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(item, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (food.downsides.isNotEmpty()) {
            Text("Points d'attention", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.downsides.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        null,
                        tint = NutriColors.LevelHigh,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(item, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
