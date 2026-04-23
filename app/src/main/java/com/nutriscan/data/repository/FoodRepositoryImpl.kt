package com.nutriscan.data.repository

import com.nutriscan.data.api.OpenFoodFactsApi
import com.nutriscan.data.api.UsdaApi
import com.nutriscan.data.api.toDomain
import com.nutriscan.data.api.toDomainOrNull
import com.nutriscan.data.db.FoodDao
import com.nutriscan.data.db.toDomain
import com.nutriscan.data.db.toEntity
import com.nutriscan.domain.model.Food
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val usdaApi: UsdaApi,
    private val offApi: OpenFoodFactsApi,
    private val dao: FoodDao
) : FoodRepository {

    override suspend fun searchFoods(query: String): List<Food> {
        return try {
            val resp = usdaApi.searchFoods(query)
            val foods = resp.foods.map { it.toDomain() }
            foods.forEach { dao.upsertFood(it.toEntity()) }
            foods
        } catch (e: Exception) {
            try {
                offApi.searchProducts(query).products.mapNotNull { it.toDomainOrNull() }
            } catch (e2: Exception) { emptyList() }
        }
    }

    override suspend fun searchFoodsOffline(query: String) =
        dao.searchFoods("%$query%").map { it.toDomain() }

    override suspend fun getFoodById(id: String): Food? {
        return try {
            usdaApi.getFoodDetail(id).toDomain()
        } catch (e: Exception) { getCachedFood(id) }
    }

    override suspend fun getCachedFood(id: String) =
        dao.getFoodById(id)?.toDomain()

    override suspend fun cacheFood(food: Food) =
        dao.upsertFood(food.toEntity())

    override suspend fun addToHistory(food: Food) {
        dao.upsertFood(food.toEntity().copy(
            lastViewedAt = System.currentTimeMillis(),
            isInHistory = true
        ))
        dao.trimHistory(20)
    }

    override fun getHistory(): Flow<List<Food>> =
        dao.getHistory().map { it.map { e -> e.toDomain() } }

    override suspend fun toggleFavorite(foodId: String): Boolean {
        val current = dao.getFoodById(foodId) ?: return false
        val newState = !current.isFavorite
        dao.updateFavorite(foodId, newState)
        return newState
    }

    override fun getFavorites(): Flow<List<Food>> =
        dao.getFavorites().map { it.map { e -> e.toDomain() } }

    override suspend fun getFoodByBarcode(barcode: String): Food? {
        return try {
            offApi.getProductByBarcode(barcode).product?.toDomainOrNull()
        } catch (e: Exception) { dao.getFoodByBarcode(barcode)?.toDomain() }
    }

    override suspend fun clearHistory() = dao.clearHistory()
}
