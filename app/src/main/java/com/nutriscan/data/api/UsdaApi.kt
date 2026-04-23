package com.nutriscan.data.api

import com.nutriscan.BuildConfig
import com.nutriscan.domain.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────────────────────
//  Retrofit interface — USDA FoodData Central
//  Doc : https://fdc.nal.usda.gov/api-guide.html
// ─────────────────────────────────────────────────────────────────────────────
interface UsdaApi {

    /**
     * Recherche d'aliments.
     * GET /v1/foods/search?query=apple&pageSize=20&api_key=...
     */
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageNumber") page: Int = 1,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("api_key") apiKey: String = BuildConfig.USDA_API_KEY
    ): UsdaSearchResponse

    /**
     * Détails complets d'un aliment.
     * GET /v1/food/{fdcId}?api_key=...
     */
    @GET("food/{fdcId}")
    suspend fun getFoodDetail(
        @Path("fdcId") fdcId: String,
        @Query("format") format: String = "full",
        @Query("api_key") apiKey: String = BuildConfig.USDA_API_KEY
    ): UsdaFoodDetail
}

// ─────────────────────────────────────────────────────────────────────────────
//  DTOs (Data Transfer Objects) — réponses brutes de l'API USDA
// ─────────────────────────────────────────────────────────────────────────────

data class UsdaSearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val foods: List<UsdaFoodItem> = emptyList()
)

data class UsdaFoodItem(
    val fdcId: Int,
    val description: String,
    val dataType: String = "",
    val brandOwner: String? = null,
    val foodCategory: String? = null,
    val foodNutrients: List<UsdaNutrient> = emptyList()
)

data class UsdaFoodDetail(
    val fdcId: Int,
    val description: String,
    val dataType: String = "",
    val publicationDate: String = "",
    val foodCategory: UsdaFoodCategory? = null,
    val foodNutrients: List<UsdaNutrientDetail> = emptyList()
)

data class UsdaFoodCategory(
    val id: Int = 0,
    val code: String = "",
    val description: String = ""
)

data class UsdaNutrient(
    val nutrientId: Int = 0,
    val nutrientName: String = "",
    val unitName: String = "",
    val value: Double = 0.0
)

data class UsdaNutrientDetail(
    val nutrient: UsdaNutrientInfo,
    val amount: Double = 0.0,
    val dataPoints: Int = 0
)

data class UsdaNutrientInfo(
    val id: Int,
    val number: String = "",
    val name: String = "",
    val unitName: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
//  Mappers USDA → Domaine
// ─────────────────────────────────────────────────────────────────────────────

/** IDs USDA des nutriments clés */
private object NutrientId {
    const val ENERGY = 1008
    const val PROTEIN = 1003
    const val FAT = 1004
    const val CARBS = 1005
    const val FIBER = 1079
    const val SUGARS = 2000
    const val SATURATED_FAT = 1258
    const val VIT_C = 1162
    const val VIT_D = 1114
    const val VIT_A = 1106
    const val VIT_B12 = 1178
    const val VIT_B6 = 1175
    const val FOLATE = 1177
    const val POTASSIUM = 1092
    const val CALCIUM = 1087
    const val IRON = 1089
    const val MAGNESIUM = 1090
    const val SODIUM = 1093
    const val ZINC = 1095
}

fun UsdaFoodItem.toDomain(): Food {
    val nutrients = foodNutrients.associate { it.nutrientId to it.value }
    return Food(
        id = fdcId.toString(),
        name = description,
        nameFr = description,
        category = mapCategory(foodCategory),
        calories = nutrients[NutrientId.ENERGY] ?: 0.0,
        proteins = nutrients[NutrientId.PROTEIN] ?: 0.0,
        carbohydrates = nutrients[NutrientId.CARBS] ?: 0.0,
        fat = nutrients[NutrientId.FAT] ?: 0.0,
        fiber = nutrients[NutrientId.FIBER] ?: 0.0,
        sugars = nutrients[NutrientId.SUGARS] ?: 0.0,
        saturatedFat = nutrients[NutrientId.SATURATED_FAT] ?: 0.0,
        vitamins = buildVitaminMap(nutrients),
        minerals = buildMineralMap(nutrients),
        source = DataSource.USDA
    )
}

fun UsdaFoodDetail.toDomain(): Food {
    val nutrients = foodNutrients.associate { it.nutrient.id to it.amount }
    return Food(
        id = fdcId.toString(),
        name = description,
        nameFr = description,
        category = mapCategory(foodCategory?.description),
        calories = nutrients[NutrientId.ENERGY] ?: 0.0,
        proteins = nutrients[NutrientId.PROTEIN] ?: 0.0,
        carbohydrates = nutrients[NutrientId.CARBS] ?: 0.0,
        fat = nutrients[NutrientId.FAT] ?: 0.0,
        fiber = nutrients[NutrientId.FIBER] ?: 0.0,
        sugars = nutrients[NutrientId.SUGARS] ?: 0.0,
        saturatedFat = nutrients[NutrientId.SATURATED_FAT] ?: 0.0,
        vitamins = buildVitaminMap(nutrients),
        minerals = buildMineralMap(nutrients),
        benefits = generateBenefits(nutrients),
        downsides = generateDownsides(nutrients),
        source = DataSource.USDA
    )
}

private fun buildVitaminMap(n: Map<Int, Double>): Map<Vitamin, Double> = mapOf(
    Vitamin.C to (n[NutrientId.VIT_C] ?: 0.0),
    Vitamin.D to (n[NutrientId.VIT_D] ?: 0.0),
    Vitamin.A to (n[NutrientId.VIT_A] ?: 0.0),
    Vitamin.B12 to (n[NutrientId.VIT_B12] ?: 0.0),
    Vitamin.B6 to (n[NutrientId.VIT_B6] ?: 0.0),
    Vitamin.B9 to (n[NutrientId.FOLATE] ?: 0.0)
).filter { it.value > 0 }

private fun buildMineralMap(n: Map<Int, Double>): Map<Mineral, Double> = mapOf(
    Mineral.POTASSIUM to (n[NutrientId.POTASSIUM] ?: 0.0),
    Mineral.CALCIUM to (n[NutrientId.CALCIUM] ?: 0.0),
    Mineral.IRON to (n[NutrientId.IRON] ?: 0.0),
    Mineral.MAGNESIUM to (n[NutrientId.MAGNESIUM] ?: 0.0),
    Mineral.SODIUM to (n[NutrientId.SODIUM] ?: 0.0),
    Mineral.ZINC to (n[NutrientId.ZINC] ?: 0.0)
).filter { it.value > 0 }

private fun generateBenefits(n: Map<Int, Double>): List<String> {
    val list = mutableListOf<String>()
    if ((n[NutrientId.PROTEIN] ?: 0.0) > 15) list.add("Excellente source de protéines")
    if ((n[NutrientId.FIBER] ?: 0.0) > 5) list.add("Riche en fibres alimentaires")
    if ((n[NutrientId.VIT_C] ?: 0.0) > 20) list.add("Bonne source de vitamine C")
    if ((n[NutrientId.IRON] ?: 0.0) > 3) list.add("Source de fer")
    if ((n[NutrientId.POTASSIUM] ?: 0.0) > 300) list.add("Riche en potassium")
    if ((n[NutrientId.ENERGY] ?: 0.0) < 60) list.add("Faible en calories")
    return list.ifEmpty { listOf("Aliment nutritif") }
}

private fun generateDownsides(n: Map<Int, Double>): List<String> {
    val list = mutableListOf<String>()
    if ((n[NutrientId.ENERGY] ?: 0.0) > 400) list.add("Calorique — à consommer avec modération")
    if ((n[NutrientId.SATURATED_FAT] ?: 0.0) > 10) list.add("Élevé en graisses saturées")
    if ((n[NutrientId.SODIUM] ?: 0.0) > 400) list.add("Teneur en sodium élevée")
    if ((n[NutrientId.SUGARS] ?: 0.0) > 20) list.add("Riche en sucres simples")
    return list
}

private fun mapCategory(category: String?): FoodCategory {
    return when {
        category == null -> FoodCategory.OTHER
        category.contains("fruit", true) -> FoodCategory.FRUIT
        category.contains("vegetable", true) || category.contains("légume", true) -> FoodCategory.VEGETABLE
        category.contains("poultry", true) || category.contains("beef", true) ||
        category.contains("fish", true) || category.contains("seafood", true) -> FoodCategory.PROTEIN
        category.contains("grain", true) || category.contains("cereal", true) -> FoodCategory.GRAIN
        category.contains("dairy", true) || category.contains("milk", true) -> FoodCategory.DAIRY
        category.contains("legume", true) || category.contains("bean", true) -> FoodCategory.LEGUME
        category.contains("nut", true) || category.contains("seed", true) -> FoodCategory.NUT
        else -> FoodCategory.OTHER
    }
}
