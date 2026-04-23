package com.nutriscan.data.repository

import com.nutriscan.data.api.OpenFoodFactsApi
import com.nutriscan.data.api.UsdaApi
import com.nutriscan.data.db.FoodDao
import com.nutriscan.data.db.FoodEntity
import com.nutriscan.data.db.toEntity
import com.nutriscan.data.db.toDomain
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

    // ── Recherche ──────────────────────────────────────────────────────────────

    override suspend fun searchFoods(query: String): List<Food> {
        return try {
            val response = usdaApi.searchFoods(query = query, pageSize = 20)
            val foods = response.foods.map { it.toDomain() }
            // Met à jour le cache silencieusement
            foods.forEach { dao.upsertFood(it.toEntity()) }
            foods
        } catch (e: Exception) {
            // Fallback Open Food Facts
            val offResponse = offApi.searchProducts(query)
            offResponse.products.mapNotNull { it.toDomainOrNull() }
        }
    }

    override suspend fun searchFoodsOffline(query: String): List<Food> {
        return dao.searchFoods("%$query%").map { it.toDomain() }
    }

    // ── Détail ─────────────────────────────────────────────────────────────────

    override suspend fun getFoodById(id: String): Food? {
        return try {
            val response = usdaApi.getFoodDetail(fdcId = id)
            response.toDomain()
        } catch (e: Exception) {
            getCachedFood(id)
        }
    }

    override suspend fun getCachedFood(id: String): Food? {
        return dao.getFoodById(id)?.toDomain()
    }

    override suspend fun cacheFood(food: Food) {
        dao.upsertFood(food.toEntity())
    }

    // ── Historique ─────────────────────────────────────────────────────────────

    override suspend fun addToHistory(food: Food) {
        val entity = food.toEntity().copy(
            lastViewedAt = System.currentTimeMillis(),
            isInHistory = true
        )
        dao.upsertFood(entity)
        // Limite l'historique à 20 entrées
        dao.trimHistory(20)
    }

    override fun getHistory(): Flow<List<Food>> {
        return dao.getHistory().map { list -> list.map { it.toDomain() } }
    }

    // ── Favoris ────────────────────────────────────────────────────────────────

    override suspend fun toggleFavorite(foodId: String): Boolean {
        val current = dao.getFoodById(foodId) ?: return false
        val newState = !current.isFavorite
        dao.updateFavorite(foodId, newState)
        return newState
    }

    override fun getFavorites(): Flow<List<Food>> {
        return dao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    // ── Code-barres ────────────────────────────────────────────────────────────

    override suspend fun getFoodByBarcode(barcode: String): Food? {
        return try {
            val response = offApi.getProductByBarcode(barcode)
            response.product?.toDomainOrNull()
        } catch (e: Exception) {
            dao.getFoodByBarcode(barcode)?.toDomain()
        }
    }

    override suspend fun clearHistory() {
        dao.clearHistory()
    }
}
