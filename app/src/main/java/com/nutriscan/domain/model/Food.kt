package com.nutriscan.domain.model

/**
 * Modèle principal représentant un aliment avec ses valeurs nutritionnelles complètes.
 * Utilisé dans la couche domaine (indépendant de la source de données).
 */
data class Food(
    val id: String,
    val name: String,
    val nameFr: String,
    val nameAr: String = "",
    val description: String = "",
    val category: FoodCategory,
    val portion: Portion = Portion(),

    // ─── Macronutriments (pour 100g) ──────────────────────────────────────────
    val calories: Double,
    val proteins: Double,       // grammes
    val carbohydrates: Double,  // grammes
    val sugars: Double = 0.0,   // grammes
    val fat: Double,            // grammes
    val saturatedFat: Double = 0.0,
    val fiber: Double = 0.0,

    // ─── Micronutriments ──────────────────────────────────────────────────────
    val vitamins: Map<Vitamin, Double> = emptyMap(),
    val minerals: Map<Mineral, Double> = emptyMap(),

    // ─── Métadonnées ──────────────────────────────────────────────────────────
    val benefits: List<String> = emptyList(),
    val downsides: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageUrl: String? = null,
    val barcode: String? = null,
    val source: DataSource = DataSource.USDA,
    val isFavorite: Boolean = false,
    val lastViewedAt: Long = 0L
)

data class Portion(
    val amount: Double = 100.0,
    val unit: PortionUnit = PortionUnit.GRAM,
    val description: String = "100g"
)

enum class PortionUnit(val label: String) {
    GRAM("g"), MILLILITER("ml"), PIECE("pièce"), CUP("tasse"), TABLESPOON("c.à.s")
}

enum class FoodCategory(val labelFr: String, val emoji: String) {
    FRUIT("Fruits", "🍎"),
    VEGETABLE("Légumes", "🥦"),
    PROTEIN("Protéines", "🥩"),
    GRAIN("Céréales", "🌾"),
    DAIRY("Produits laitiers", "🥛"),
    LEGUME("Légumineuses", "🫘"),
    NUT("Noix & graines", "🥜"),
    FAT("Corps gras", "🫙"),
    BEVERAGE("Boissons", "🥤"),
    SWEET("Sucreries", "🍫"),
    OTHER("Autre", "🍽️")
}

enum class Vitamin(val fullName: String, val unit: String, val dailyValue: Double) {
    A("Vitamine A", "µg", 900.0),
    C("Vitamine C", "mg", 90.0),
    D("Vitamine D", "µg", 20.0),
    E("Vitamine E", "mg", 15.0),
    K("Vitamine K", "µg", 120.0),
    B1("Vitamine B1 (Thiamine)", "mg", 1.2),
    B2("Vitamine B2 (Riboflavine)", "mg", 1.3),
    B3("Vitamine B3 (Niacine)", "mg", 16.0),
    B6("Vitamine B6", "mg", 1.7),
    B9("Vitamine B9 (Folate)", "µg", 400.0),
    B12("Vitamine B12", "µg", 2.4)
}

enum class Mineral(val fullName: String, val unit: String, val dailyValue: Double) {
    CALCIUM("Calcium", "mg", 1300.0),
    IRON("Fer", "mg", 18.0),
    MAGNESIUM("Magnésium", "mg", 420.0),
    PHOSPHORUS("Phosphore", "mg", 1250.0),
    POTASSIUM("Potassium", "mg", 4700.0),
    SODIUM("Sodium", "mg", 2300.0),
    ZINC("Zinc", "mg", 11.0),
    SELENIUM("Sélénium", "µg", 55.0)
}

enum class DataSource { USDA, OPEN_FOOD_FACTS, LOCAL }

/**
 * Wrapper pour les états de l'UI — Loading, Success, Error.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>()
}

/**
 * Résultat de comparaison entre plusieurs aliments.
 */
data class ComparisonResult(
    val foods: List<Food>,
    val highlights: Map<String, ComparisonHighlight> // key = nutrient name
)

data class ComparisonHighlight(
    val winner: Food,
    val values: Map<String, Double>
)
