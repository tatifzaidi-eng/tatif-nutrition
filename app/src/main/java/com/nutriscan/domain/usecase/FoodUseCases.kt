package com.nutriscan.domain.usecase

import com.nutriscan.data.repository.FoodRepository
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  SearchFoodsUseCase
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Recherche des aliments par nom — combine API + cache local.
 * Émet Loading → Success ou Error.
 */
class SearchFoodsUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(query: String): Flow<UiState<List<Food>>> = flow {
        if (query.isBlank() || query.length < 2) {
            emit(UiState.Empty)
            return@flow
        }
        emit(UiState.Loading)
        try {
            val results = repository.searchFoods(query)
            if (results.isEmpty()) emit(UiState.Empty)
            else emit(UiState.Success(results))
        } catch (e: Exception) {
            // Tentative de lecture du cache local en cas d'erreur réseau
            val cached = repository.searchFoodsOffline(query)
            if (cached.isNotEmpty()) {
                emit(UiState.Success(cached))
            } else {
                emit(UiState.Error(e.message ?: "Erreur de recherche", retryable = true))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GetFoodDetailUseCase
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Récupère les détails complets d'un aliment par ID.
 * Cache en local après chaque consultation.
 */
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
            } else {
                emit(UiState.Error("Aliment introuvable"))
            }
        } catch (e: Exception) {
            val cached = repository.getCachedFood(foodId)
            if (cached != null) emit(UiState.Success(cached))
            else emit(UiState.Error(e.message ?: "Impossible de charger l'aliment"))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GetSearchHistoryUseCase
// ─────────────────────────────────────────────────────────────────────────────
class GetSearchHistoryUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(): Flow<List<Food>> = repository.getHistory()
}

// ─────────────────────────────────────────────────────────────────────────────
//  ToggleFavoriteUseCase
// ─────────────────────────────────────────────────────────────────────────────
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    suspend operator fun invoke(foodId: String): Boolean {
        return repository.toggleFavorite(foodId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GetFavoritesUseCase
// ─────────────────────────────────────────────────────────────────────────────
class GetFavoritesUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(): Flow<List<Food>> = repository.getFavorites()
}

// ─────────────────────────────────────────────────────────────────────────────
//  GetFoodByBarcodeUseCase
// ─────────────────────────────────────────────────────────────────────────────
class GetFoodByBarcodeUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(barcode: String): Flow<UiState<Food>> = flow {
        emit(UiState.Loading)
        try {
            val food = repository.getFoodByBarcode(barcode)
            if (food != null) emit(UiState.Success(food))
            else emit(UiState.Error("Produit non trouvé pour ce code-barres"))
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Erreur lors de la lecture du code-barres"))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CompareFoodsUseCase
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Calcule les highlights de comparaison nutritionnelle entre N aliments.
 */
class CompareFoodsUseCase @Inject constructor() {
    operator fun invoke(foods: List<Food>): Map<String, String> {
        if (foods.size < 2) return emptyMap()
        val highlights = mutableMapOf<String, String>()

        // Trouver le "gagnant" de chaque nutriment
        val maxCalFood = foods.maxByOrNull { it.calories }
        val minCalFood = foods.minByOrNull { it.calories }
        val maxProtFood = foods.maxByOrNull { it.proteins }
        val maxFiberFood = foods.maxByOrNull { it.fiber }
        val minFatFood = foods.minByOrNull { it.fat }

        maxCalFood?.let { highlights["calories_max"] = it.id }
        minCalFood?.let { highlights["calories_min"] = it.id }
        maxProtFood?.let { highlights["proteins_max"] = it.id }
        maxFiberFood?.let { highlights["fiber_max"] = it.id }
        minFatFood?.let { highlights["fat_min"] = it.id }

        return highlights
    }
}
