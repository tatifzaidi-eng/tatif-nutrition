package com.nutriscan.data.repository

import com.nutriscan.domain.model.Food
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    suspend fun searchFoods(query: String): List<Food>
    suspend fun searchFoodsOffline(query: String): List<Food>
    suspend fun getFoodById(id: String): Food?
    suspend fun getCachedFood(id: String): Food?
    suspend fun cacheFood(food: Food)
    suspend fun addToHistory(food: Food)
    fun getHistory(): Flow<List<Food>>
    suspend fun toggleFavorite(foodId: String): Boolean
    fun getFavorites(): Flow<List<Food>>
    suspend fun getFoodByBarcode(barcode: String): Food?
    suspend fun clearHistory()
}
