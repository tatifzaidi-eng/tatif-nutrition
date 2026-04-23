package com.nutriscan.domain.usecase

import com.nutriscan.data.repository.FoodRepository
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchFoodsUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(query: String): Flow<UiState<List<Food>>> = flow {
        if (query.isBlank() || query.length < 2) { emit(UiState.Empty); return@flow }
        emit(UiState.Loading)
        try {
            val results = repository.searchFoods(query)
            if (results.isEmpty()) emit(UiState.Empty)
            else emit(UiState.Success(results))
        } catch (e: Exception) {
            val cached = repository.searchFoodsOffline(query)
            if (cached.isNotEmpty()) emit(UiState.Success(cached))
            else emit(UiState.Error(e.message ?: "Erreur de recherche"))
        }
    }
}

class GetFoodDetailUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(foodId: String): Flow<UiState<Food>> = flow {
        emit(UiState.Loading)
        try {
            val food = repository.getFoodById(foodId)
            if (food != null) {
                repository.cacheFood(food)
                repository.addToHistory(food)
                emit(UiState.Success(food))
            } else emit(UiState.Error("Aliment introuvable"))
        } catch (e: Exception) {
            val cached = repository.getCachedFood(foodId)
            if (cached != null) emit(UiState.Success(cached))
            else emit(UiState.Error(e.message ?: "Impossible de charger l'aliment"))
        }
    }
}

class GetSearchHistoryUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(): Flow<List<Food>> = repository.getHistory()
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    suspend operator fun invoke(foodId: String): Boolean =
        repository.toggleFavorite(foodId)
}

class GetFavoritesUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(): Flow<List<Food>> = repository.getFavorites()
}

class GetFoodByBarcodeUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(barcode: String): Flow<UiState<Food>> = flow {
        emit(UiState.Loading)
        try {
            val food = repository.getFoodByBarcode(barcode)
            if (food != null) emit(UiState.Success(food))
            else emit(UiState.Error("Produit non trouvé"))
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Erreur lecture code-barres"))
        }
    }
}

class CompareFoodsUseCase @Inject constructor() {
    operator fun invoke(foods: List<Food>): Map<String, String> {
        if (foods.size < 2) return emptyMap()
        return mapOf(
            "calories_min" to (foods.minByOrNull { it.calories }?.id ?: ""),
            "proteins_max" to (foods.maxByOrNull { it.proteins }?.id ?: ""),
            "fiber_max"    to (foods.maxByOrNull { it.fiber }?.id ?: ""),
            "fat_min"      to (foods.minByOrNull { it.fat }?.id ?: "")
        )
    }
}
