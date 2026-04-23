package com.nutriscan.data.api

import com.nutriscan.domain.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────────────────────
//  Open Food Facts API — pour la recherche par code-barres (gratuite)
//  Doc : https://openfoodfacts.github.io/openfoodfacts-server/api/
// ─────────────────────────────────────────────────────────────────────────────
interface OpenFoodFactsApi {

    /** Recherche par nom. */
    @GET("search")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("fields") fields: String = "code,product_name,nutriments,categories,image_url"
    ): OffSearchResponse

    /** Récupère un produit par code-barres. */
    @GET("product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OffProductResponse
}

// ─────────────────────────────────────────────────────────────────────────────
//  DTOs Open Food Facts
// ─────────────────────────────────────────────────────────────────────────────

data class OffSearchResponse(
    val count: Int = 0,
    val products: List<OffProduct> = emptyList()
)

data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null
)

data class OffProduct(
    val code: String = "",
    val product_name: String = "",
    val product_name_fr: String = "",
    val categories: String = "",
    val image_url: String? = null,
    val nutriments: OffNutriments? = null
)

data class OffNutriments(
    val energy_100g: Double? = null,
    val proteins_100g: Double? = null,
    val carbohydrates_100g: Double? = null,
    val fat_100g: Double? = null,
    val fiber_100g: Double? = null,
    val sugars_100g: Double? = null,
    val salt_100g: Double? = null,
    val saturated_fat_100g: Double? = null,
    val vitamin_c_100g: Double? = null,
    val potassium_100g: Double? = null,
    val calcium_100g: Double? = null,
    val iron_100g: Double? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  Mappers Open Food Facts → Domaine
// ─────────────────────────────────────────────────────────────────────────────

fun OffProduct.toDomainOrNull(): Food? {
    val n = nutriments ?: return null
    val name = product_name_fr.ifBlank { product_name }.ifBlank { return null }
    return Food(
        id = code,
        name = name,
        nameFr = name,
        barcode = code,
        category = mapOffCategory(categories),
        // L'API OFF renvoie l'énergie en kJ, on convertit en kcal (÷ 4.184)
        calories = (n.energy_100g ?: 0.0) / 4.184,
        proteins = n.proteins_100g ?: 0.0,
        carbohydrates = n.carbohydrates_100g ?: 0.0,
        fat = n.fat_100g ?: 0.0,
        fiber = n.fiber_100g ?: 0.0,
        sugars = n.sugars_100g ?: 0.0,
        saturatedFat = n.saturated_fat_100g ?: 0.0,
        vitamins = buildMap {
            n.vitamin_c_100g?.takeIf { it > 0 }?.let { put(Vitamin.C, it * 1000) } // mg
        },
        minerals = buildMap {
            n.potassium_100g?.takeIf { it > 0 }?.let { put(Mineral.POTASSIUM, it * 1000) }
            n.calcium_100g?.takeIf { it > 0 }?.let { put(Mineral.CALCIUM, it * 1000) }
            n.iron_100g?.takeIf { it > 0 }?.let { put(Mineral.IRON, it * 1000) }
        },
        imageUrl = image_url,
        source = DataSource.OPEN_FOOD_FACTS
    )
}

private fun mapOffCategory(categories: String): FoodCategory {
    val lower = categories.lowercase()
    return when {
        "fruit" in lower -> FoodCategory.FRUIT
        "légume" in lower || "vegetable" in lower -> FoodCategory.VEGETABLE
        "viande" in lower || "meat" in lower || "poisson" in lower || "fish" in lower -> FoodCategory.PROTEIN
        "céréale" in lower || "grain" in lower || "bread" in lower -> FoodCategory.GRAIN
        "lait" in lower || "dairy" in lower || "fromage" in lower -> FoodCategory.DAIRY
        "légumineuse" in lower || "legume" in lower -> FoodCategory.LEGUME
        "noix" in lower || "nut" in lower || "graine" in lower -> FoodCategory.NUT
        else -> FoodCategory.OTHER
    }
}
