package com.nutriscan.data.db

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nutriscan.domain.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
//  Entity Room
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameFr: String,
    val nameAr: String = "",
    val categoryName: String,
    val calories: Double,
    val proteins: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double,
    val sugars: Double = 0.0,
    val saturatedFat: Double = 0.0,
    // Stockés en JSON (Gson)
    val vitaminsJson: String = "{}",
    val mineralsJson: String = "{}",
    val benefitsJson: String = "[]",
    val downsidesJson: String = "[]",
    val tipsJson: String = "[]",
    val imageUrl: String? = null,
    val barcode: String? = null,
    val sourceName: String = "USDA",
    val isFavorite: Boolean = false,
    val isInHistory: Boolean = false,
    val lastViewedAt: Long = 0L
)

// ─────────────────────────────────────────────────────────────────────────────
//  DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface FoodDao {

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFoodById(id: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodEntity?

    @Query("""
        SELECT * FROM foods 
        WHERE name LIKE :query OR nameFr LIKE :query 
        ORDER BY lastViewedAt DESC 
        LIMIT 30
    """)
    suspend fun searchFoods(query: String): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE isInHistory = 1 ORDER BY lastViewedAt DESC LIMIT 20")
    fun getHistory(): Flow<List<FoodEntity>>

    @Query("""
        DELETE FROM foods 
        WHERE isInHistory = 1 AND isFavorite = 0
        AND id NOT IN (
            SELECT id FROM foods 
            WHERE isInHistory = 1 
            ORDER BY lastViewedAt DESC 
            LIMIT :limit
        )
    """)
    suspend fun trimHistory(limit: Int)

    @Query("UPDATE foods SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT * FROM foods WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<FoodEntity>>

    @Query("UPDATE foods SET isInHistory = 0 WHERE isFavorite = 0")
    suspend fun clearHistory()

    @Query("DELETE FROM foods WHERE isFavorite = 0 AND isInHistory = 0")
    suspend fun cleanCache()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Base de données Room
// ─────────────────────────────────────────────────────────────────────────────

@Database(entities = [FoodEntity::class], version = 1, exportSchema = false)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mappers Entity ↔ Domain
// ─────────────────────────────────────────────────────────────────────────────

private val gson = Gson()

fun Food.toEntity(): FoodEntity = FoodEntity(
    id = id,
    name = name,
    nameFr = nameFr,
    nameAr = nameAr,
    categoryName = category.name,
    calories = calories,
    proteins = proteins,
    carbohydrates = carbohydrates,
    fat = fat,
    fiber = fiber,
    sugars = sugars,
    saturatedFat = saturatedFat,
    vitaminsJson = gson.toJson(vitamins.mapKeys { it.key.name }),
    mineralsJson = gson.toJson(minerals.mapKeys { it.key.name }),
    benefitsJson = gson.toJson(benefits),
    downsidesJson = gson.toJson(downsides),
    tipsJson = gson.toJson(tips),
    imageUrl = imageUrl,
    barcode = barcode,
    sourceName = source.name,
    isFavorite = isFavorite,
    lastViewedAt = lastViewedAt
)

fun FoodEntity.toDomain(): Food {
    val vitType = object : TypeToken<Map<String, Double>>() {}.type
    val listType = object : TypeToken<List<String>>() {}.type

    val rawVit: Map<String, Double> = gson.fromJson(vitaminsJson, vitType) ?: emptyMap()
    val rawMin: Map<String, Double> = gson.fromJson(mineralsJson, vitType) ?: emptyMap()
    val ben: List<String> = gson.fromJson(benefitsJson, listType) ?: emptyList()
    val down: List<String> = gson.fromJson(downsidesJson, listType) ?: emptyList()
    val tipsList: List<String> = gson.fromJson(tipsJson, listType) ?: emptyList()

    return Food(
        id = id,
        name = name,
        nameFr = nameFr,
        nameAr = nameAr,
        category = runCatching { FoodCategory.valueOf(categoryName) }.getOrDefault(FoodCategory.OTHER),
        calories = calories,
        proteins = proteins,
        carbohydrates = carbohydrates,
        fat = fat,
        fiber = fiber,
        sugars = sugars,
        saturatedFat = saturatedFat,
        vitamins = rawVit.mapNotNull { (k, v) ->
            runCatching { Vitamin.valueOf(k) to v }.getOrNull()
        }.toMap(),
        minerals = rawMin.mapNotNull { (k, v) ->
            runCatching { Mineral.valueOf(k) to v }.getOrNull()
        }.toMap(),
        benefits = ben,
        downsides = down,
        tips = tipsList,
        imageUrl = imageUrl,
        barcode = barcode,
        source = runCatching { DataSource.valueOf(sourceName) }.getOrDefault(DataSource.LOCAL),
        isFavorite = isFavorite,
        lastViewedAt = lastViewedAt
    )
}
