package com.nutriscan.data.repository

import com.nutriscan.domain.model.Food
import kotlinx.coroutines.flow.Flow

/**
 * Contrat du repository — la couche domaine ne connaît que cette interface.
 * L'implémentation (FoodRepositoryImpl) est injectée via Hilt.
 */
interface FoodRepository {

    /** Recherche distante (API) + fusion avec le cache. */
    suspend fun searchFoods(query: String): List<Food>

    /** Recherche hors-ligne uniquement (Room). */
    suspend fun searchFoodsOffline(query: String): List<Food>

    /** Récupère un aliment par ID (API puis cache). */
    suspend fun getFoodById(id: String): Food?

    /** Récupère depuis le cache Room uniquement. */
    suspend fun getCachedFood(id: String): Food?

    /** Sauvegarde un aliment dans le cache Room. */
    suspend fun cacheFood(food: Food)

    /** Ajoute un aliment à l'historique de consultations. */
    suspend fun addToHistory(food: Food)

    /** Flux de l'historique des consultations (max 20). */
    fun getHistory(): Flow<List<Food>>

    /** Bascule le statut favori, retourne le nouvel état. */
    suspend fun toggleFavorite(foodId: String): Boolean

    /** Flux de la liste des favoris. */
    fun getFavorites(): Flow<List<Food>>

    /** Recherche via code-barres (Open Food Facts). */
    suspend fun getFoodByBarcode(barcode: String): Food?

    /** Vide l'historique. */
    suspend fun clearHistory()
}
