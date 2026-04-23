package com.nutriscan.data.api

import com.nutriscan.BuildConfig
import com.nutriscan.domain.model.*
import retrofit2.http.*

interface UsdaApi {
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 20,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("api_key") apiKey: String = BuildConfig.USDA_API_KEY
    ): UsdaSearchResponse

    @GET("food/{fdcId}")
    suspend fun getFoodDetail(
        @Path("fdcId") fdcId: String,
        @Query("format") format: String = "full",
        @Query("api_key") apiKey: String = BuildConfig.USDA_API_KEY
    ): UsdaFoodDetail
}

data class UsdaSearchResponse(
    val totalHits: Int = 0,
    val foods: List<UsdaFoodItem> = emptyList()
)
data class UsdaFoodItem(
    val fdcId: Int = 0,
    val description: String = "",
    val foodCategory: String? = null,
    val foodNutrients: List<UsdaNutrient> = emptyList()
)
data class UsdaFoodDetail(
    val fdcId: Int = 0,
    val description: String = "",
    val foodCategory: UsdaCategoryObj? = null,
    val foodNutrients: List<UsdaNutrientDetail> = emptyList()
)
data class UsdaCategoryObj(val description: String = "")
data class UsdaNutrient(val nutrientId: Int = 0, val value: Double = 0.0)
data class UsdaNutrientDetail(val nutrient: UsdaNutrientInfo = UsdaNutrientInfo(), val amount: Double = 0.0)
data class UsdaNutrientInfo(val id: Int = 0, val name: String = "", val unitName: String = "")

private object NID {
    const val ENERGY = 1008; const val PROTEIN = 1003; const val FAT = 1004
    const val CARBS = 1005; const val FIBER = 1079; const val SUGARS = 2000
    const val SAT_FAT = 1258; const val VIT_C = 1162; const val VIT_D = 1114
    const val VIT_A = 1106; const val VIT_B12 = 1178; const val VIT_B6 = 1175
    const val FOLATE = 1177; const val POTASSIUM = 1092; const val CALCIUM = 1087
    const val IRON = 1089; const val MAGNESIUM = 1090; const val SODIUM = 1093
    const val ZINC = 1095
}

fun UsdaFoodItem.toDomain(): Food {
    val n = foodNutrients.associate { it.nutrientId to it.value }
    return Food(
        id = fdcId.toString(), name = description, nameFr = description,
        category = mapCat(foodCategory),
        calories = n[NID.ENERGY] ?: 0.0, proteins = n[NID.PROTEIN] ?: 0.0,
        carbohydrates = n[NID.CARBS] ?: 0.0, fat = n[NID.FAT] ?: 0.0,
        fiber = n[NID.FIBER] ?: 0.0, sugars = n[NID.SUGARS] ?: 0.0,
        saturatedFat = n[NID.SAT_FAT] ?: 0.0,
        vitamins = buildVit(n), minerals = buildMin(n),
        benefits = genBen(n), downsides = genDown(n),
        source = DataSource.USDA
    )
}

fun UsdaFoodDetail.toDomain(): Food {
    val n = foodNutrients.associate { it.nutrient.id to it.amount }
    return Food(
        id = fdcId.toString(), name = description, nameFr = description,
        category = mapCat(foodCategory?.description),
        calories = n[NID.ENERGY] ?: 0.0, proteins = n[NID.PROTEIN] ?: 0.0,
        carbohydrates = n[NID.CARBS] ?: 0.0, fat = n[NID.FAT] ?: 0.0,
        fiber = n[NID.FIBER] ?: 0.0, sugars = n[NID.SUGARS] ?: 0.0,
        saturatedFat = n[NID.SAT_FAT] ?: 0.0,
        vitamins = buildVit(n), minerals = buildMin(n),
        benefits = genBen(n), downsides = genDown(n),
        source = DataSource.USDA
    )
}

private fun buildVit(n: Map<Int, Double>) = mapOf(
    Vitamin.C to (n[NID.VIT_C] ?: 0.0), Vitamin.D to (n[NID.VIT_D] ?: 0.0),
    Vitamin.A to (n[NID.VIT_A] ?: 0.0), Vitamin.B12 to (n[NID.VIT_B12] ?: 0.0),
    Vitamin.B6 to (n[NID.VIT_B6] ?: 0.0), Vitamin.B9 to (n[NID.FOLATE] ?: 0.0)
).filter { it.value > 0 }

private fun buildMin(n: Map<Int, Double>) = mapOf(
    Mineral.POTASSIUM to (n[NID.POTASSIUM] ?: 0.0),
    Mineral.CALCIUM to (n[NID.CALCIUM] ?: 0.0),
    Mineral.IRON to (n[NID.IRON] ?: 0.0),
    Mineral.MAGNESIUM to (n[NID.MAGNESIUM] ?: 0.0),
    Mineral.SODIUM to (n[NID.SODIUM] ?: 0.0),
    Mineral.ZINC to (n[NID.ZINC] ?: 0.0)
).filter { it.value > 0 }

private fun genBen(n: Map<Int, Double>) = buildList {
    if ((n[NID.PROTEIN] ?: 0.0) > 15) add("Excellente source de protéines")
    if ((n[NID.FIBER] ?: 0.0) > 5) add("Riche en fibres alimentaires")
    if ((n[NID.VIT_C] ?: 0.0) > 20) add("Bonne source de vitamine C")
    if ((n[NID.IRON] ?: 0.0) > 3) add("Source de fer")
    if ((n[NID.POTASSIUM] ?: 0.0) > 300) add("Riche en potassium")
    if ((n[NID.ENERGY] ?: 0.0) < 60) add("Faible en calories")
    if (isEmpty()) add("Aliment nutritif")
}

private fun genDown(n: Map<Int, Double>) = buildList {
    if ((n[NID.ENERGY] ?: 0.0) > 400) add("Calorique — à consommer avec modération")
    if ((n[NID.SAT_FAT] ?: 0.0) > 10) add("Élevé en graisses saturées")
    if ((n[NID.SODIUM] ?: 0.0) > 400) add("Teneur en sodium élevée")
    if ((n[NID.SUGARS] ?: 0.0) > 20) add("Riche en sucres simples")
}

private fun mapCat(c: String?) = when {
    c == null -> FoodCategory.OTHER
    c.contains("fruit", true) -> FoodCategory.FRUIT
    c.contains("vegetable", true) -> FoodCategory.VEGETABLE
    c.contains("poultry", true) || c.contains("beef", true) || c.contains("fish", true) -> FoodCategory.PROTEIN
    c.contains("grain", true) || c.contains("cereal", true) -> FoodCategory.GRAIN
    c.contains("dairy", true) || c.contains("milk", true) -> FoodCategory.DAIRY
    c.contains("legume", true) || c.contains("bean", true) -> FoodCategory.LEGUME
    c.contains("nut", true) || c.contains("seed", true) -> FoodCategory.NUT
    else -> FoodCategory.OTHER
}

interface OpenFoodFactsApi {
    @GET("search")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("fields") fields: String = "code,product_name,product_name_fr,nutriments,categories,image_url"
    ): OffSearchResponse

    @GET("product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OffProductResponse
}

data class OffSearchResponse(val count: Int = 0, val products: List<OffProduct> = emptyList())
data class OffProductResponse(val status: Int = 0, val product: OffProduct? = null)
data class OffProduct(
    val code: String = "", val product_name: String = "",
    val product_name_fr: String = "", val categories: String = "",
    val image_url: String? = null, val nutriments: OffNutriments? = null
)
data class OffNutriments(
    val energy_100g: Double? = null, val proteins_100g: Double? = null,
    val carbohydrates_100g: Double? = null, val fat_100g: Double? = null,
    val fiber_100g: Double? = null, val sugars_100g: Double? = null,
    val saturated_fat_100g: Double? = null, val vitamin_c_100g: Double? = null,
    val potassium_100g: Double? = null, val calcium_100g: Double? = null,
    val iron_100g: Double? = null
)

fun OffProduct.toDomainOrNull(): Food? {
    val n = nutriments ?: return null
    val name = product_name_fr.ifBlank { product_name }.ifBlank { return null }
    return Food(
        id = code, name = name, nameFr = name, barcode = code,
        calories = (n.energy_100g ?: 0.0) / 4.184,
        proteins = n.proteins_100g ?: 0.0,
        carbohydrates = n.carbohydrates_100g ?: 0.0,
        fat = n.fat_100g ?: 0.0, fiber = n.fiber_100g ?: 0.0,
        sugars = n.sugars_100g ?: 0.0,
        saturatedFat = n.saturated_fat_100g ?: 0.0,
        imageUrl = image_url, source = DataSource.OPEN_FOOD_FACTS
    )
}
