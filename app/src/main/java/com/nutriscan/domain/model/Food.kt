package com.nutriscan.domain.model

data class Food(
    val id: String,
    val name: String,
    val nameFr: String = "",
    val nameAr: String = "",
    val category: FoodCategory = FoodCategory.OTHER,
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val sugars: Double = 0.0,
    val fat: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val fiber: Double = 0.0,
    val vitamins: Map<Vitamin, Double> = emptyMap(),
    val minerals: Map<Mineral, Double> = emptyMap(),
    val benefits: List<String> = emptyList(),
    val downsides: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageUrl: String? = null,
    val barcode: String? = null,
    val source: DataSource = DataSource.USDA,
    val isFavorite: Boolean = false,
    val lastViewedAt: Long = 0L
)

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
    B1("Vitamine B1", "mg", 1.2),
    B2("Vitamine B2", "mg", 1.3),
    B3("Vitamine B3", "mg", 16.0),
    B6("Vitamine B6", "mg", 1.7),
    B9("Vitamine B9", "µg", 400.0),
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

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>()
}
