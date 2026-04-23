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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nutriscan.domain.model.*
import com.nutriscan.presentation.theme.NutriColors
import com.nutriscan.presentation.ui.components.ErrorView
import com.nutriscan.presentation.ui.components.LoadingView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    onAddToCompare: (String) -> Unit,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val portionGrams by viewModel.portionGrams.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error   -> ErrorView(message = state.message)
        is UiState.Empty   -> ErrorView(message = "Aliment introuvable")
        is UiState.Success -> {
            val food = state.data
            DetailContent(
                food = food,
                portionGrams = portionGrams,
                selectedTab = selectedTab,
                onBack = onBack,
                onToggleFavorite = viewModel::toggleFavorite,
                onPortionChanged = viewModel::onPortionChanged,
                onTabSelected = viewModel::onTabSelected,
                onAddToCompare = { onAddToCompare(food.id) },
                adjust = viewModel::adjust
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    food: Food,
    portionGrams: Double,
    selectedTab: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPortionChanged: (Double) -> Unit,
    onTabSelected: (Int) -> Unit,
    onAddToCompare: () -> Unit,
    adjust: (Double) -> Double
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(food.nameFr, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onAddToCompare) {
                        Icon(Icons.Filled.Balance, contentDescription = "Comparer", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (food.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (food.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Carte héro ──────────────────────────────────────────────────────
            HeroCard(food = food, portionGrams = portionGrams, adjust = adjust)

            // ── Sélecteur de portion ────────────────────────────────────────────
            PortionSelector(
                portionGrams = portionGrams,
                onPortionChanged = onPortionChanged
            )

            // ── Macros en cercle ────────────────────────────────────────────────
            MacroRing(food = food, portionGrams = portionGrams, adjust = adjust)

            // ── Onglets ─────────────────────────────────────────────────────────
            val tabs = listOf("Nutrition", "Vitamines", "Bienfaits")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { onTabSelected(i) },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Contenu par onglet ──────────────────────────────────────────────
            when (selectedTab) {
                0 -> NutritionTab(food = food, adjust = adjust)
                1 -> VitaminsTab(food = food, adjust = adjust)
                2 -> BenefitsTab(food = food)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Carte héro
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(food: Food, portionGrams: Double, adjust: (Double) -> Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji catégorie
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = food.category.emoji, fontSize = 34.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.nameFr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = food.category.labelFr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${adjust(food.calories).roundToInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = " kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = "Pour ${portionGrams.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sélecteur de portion
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortionSelector(portionGrams: Double, onPortionChanged: (Double) -> Unit) {
    val presets = listOf(50.0, 100.0, 150.0, 200.0, 300.0)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Portion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { grams ->
                FilterChip(
                    selected = portionGrams == grams,
                    onClick = { onPortionChanged(grams) },
                    label = { Text("${grams.roundToInt()}g", fontSize = 12.sp) }
                )
            }
        }
        Slider(
            value = portionGrams.toFloat(),
            onValueChange = { onPortionChanged(it.toDouble()) },
            valueRange = 10f..500f,
            steps = 97,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Graphique en anneau des macros
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MacroRing(food: Food, portionGrams: Double, adjust: (Double) -> Double) {
    val prot = adjust(food.proteins)
    val carbs = adjust(food.carbohydrates)
    val fat = adjust(food.fat)
    val total = prot + carbs + fat
    if (total <= 0) return

    val protPct = (prot / total * 100).roundToInt()
    val carbsPct = (carbs / total * 100).roundToInt()
    val fatPct = (fat / total * 100).roundToInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Anneau Canvas
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                val animProgress by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "ring_anim"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    val diameter = size.minDimension
                    val offset = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f, (size.height - diameter) / 2f
                    )
                    val size2 = androidx.compose.ui.geometry.Size(diameter, diameter)
                    var startAngle = -90f
                    // Protéines
                    val protSweep = (protPct / 100f) * 360f * animProgress
                    drawArc(color = NutriColors.Proteins, startAngle, protSweep, false, topLeft = offset, size = size2, style = stroke)
                    startAngle += protSweep
                    // Glucides
                    val carbSweep = (carbsPct / 100f) * 360f * animProgress
                    drawArc(color = NutriColors.Carbs, startAngle, carbSweep, false, topLeft = offset, size = size2, style = stroke)
                    startAngle += carbSweep
                    // Lipides
                    val fatSweep = (fatPct / 100f) * 360f * animProgress
                    drawArc(color = NutriColors.Fat, startAngle, fatSweep, false, topLeft = offset, size = size2, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${total.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Légende
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroLegendItem("Protéines", prot, "%${protPct}", NutriColors.Proteins)
                MacroLegendItem("Glucides", carbs, "$carbsPct%", NutriColors.Carbs)
                MacroLegendItem("Lipides", fat, "$fatPct%", NutriColors.Fat)
            }
        }
    }
}

@Composable
private fun MacroLegendItem(label: String, value: Double, pct: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
        Text("${value.roundToInt()}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(pct, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Onglet Nutrition — barres de progression
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NutritionTab(food: Food, adjust: (Double) -> Double) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NutrientBar("Calories", adjust(food.calories), food.calories, 800.0, "kcal", NutriColors.Calories)
        NutrientBar("Protéines", adjust(food.proteins), food.proteins, 35.0, "g", NutriColors.Proteins)
        NutrientBar("Glucides", adjust(food.carbohydrates), food.carbohydrates, 50.0, "g", NutriColors.Carbs)
        NutrientBar("Sucres", adjust(food.sugars), food.sugars, 25.0, "g", NutriColors.Carbs.copy(alpha = 0.6f))
        NutrientBar("Lipides", adjust(food.fat), food.fat, 30.0, "g", NutriColors.Fat)
        NutrientBar("G. saturées", adjust(food.saturatedFat), food.saturatedFat, 15.0, "g", NutriColors.Fat.copy(alpha = 0.6f))
        NutrientBar("Fibres", adjust(food.fiber), food.fiber, 15.0, "g", NutriColors.Fiber)

        // Badge de niveau
        Spacer(Modifier.height(8.dp))
        NutrientLevelBadge(food)
    }
}

@Composable
private fun NutrientBar(
    label: String,
    adjustedValue: Double,
    valuePer100g: Double,
    dailyRef: Double,
    unit: String,
    color: Color
) {
    val progress by animateFloatAsState(
        targetValue = (adjustedValue / dailyRef).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(600),
        label = "bar_$label"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "${adjustedValue.roundToInt()}$unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp).padding(start = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun NutrientLevelBadge(food: Food) {
    val (text, color, bgColor) = when {
        food.calories < 60 -> Triple("Faible en calories", NutriColors.LevelLow, MaterialTheme.colorScheme.primaryContainer)
        food.calories < 200 -> Triple("Calories modérées", NutriColors.LevelMedium, MaterialTheme.colorScheme.tertiaryContainer)
        else -> Triple("Élevé en calories", NutriColors.LevelHigh, MaterialTheme.colorScheme.errorContainer)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Onglet Vitamines & Minéraux
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VitaminsTab(food: Food, adjust: (Double) -> Double) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (food.vitamins.isNotEmpty()) {
            Text("Vitamines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.vitamins.forEach { (vitamin, value) ->
                val adjusted = (adjust(value))
                val pct = (adjusted / vitamin.dailyValue * 100).coerceIn(0.0, 150.0)
                MicroNutrientRow(
                    name = vitamin.fullName,
                    value = "${adjusted.roundToInt()} ${vitamin.unit}",
                    dailyPct = pct.roundToInt(),
                    color = NutriColors.Vitamins
                )
            }
        }
        if (food.minerals.isNotEmpty()) {
            Text("Minéraux", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            food.minerals.forEach { (mineral, value) ->
                val adjusted = adjust(value)
                val pct = (adjusted / mineral.dailyValue * 100).coerceIn(0.0, 150.0)
                MicroNutrientRow(
                    name = mineral.fullName,
                    value = "${adjusted.roundToInt()} ${mineral.unit}",
                    dailyPct = pct.roundToInt(),
                    color = NutriColors.Minerals
                )
            }
        }
        if (food.vitamins.isEmpty() && food.minerals.isEmpty()) {
            Text(
                "Données micronutritionnelles non disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MicroNutrientRow(name: String, value: String, dailyPct: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text("$dailyPct% AJR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        val progress by animateFloatAsState(targetValue = (dailyPct / 100f).coerceIn(0f, 1f), label = "micro")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Onglet Bienfaits
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BenefitsTab(food: Food) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (food.benefits.isNotEmpty()) {
            BenefitSection(
                title = "Bienfaits",
                items = food.benefits,
                iconColor = NutriColors.LevelLow,
                icon = Icons.Filled.CheckCircle
            )
        }
        if (food.downsides.isNotEmpty()) {
            BenefitSection(
                title = "Points d'attention",
                items = food.downsides,
                iconColor = NutriColors.LevelHigh,
                icon = Icons.Filled.Warning
            )
        }
        if (food.tips.isNotEmpty()) {
            BenefitSection(
                title = "Conseils",
                items = food.tips,
                iconColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Filled.Lightbulb
            )
        }
    }
}

@Composable
private fun BenefitSection(
    title: String,
    items: List<String>,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Text(item, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
            }
        }
    }
}
