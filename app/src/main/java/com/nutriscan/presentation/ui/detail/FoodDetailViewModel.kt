package com.nutriscan.presentation.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import com.nutriscan.domain.usecase.GetFoodDetailUseCase
import com.nutriscan.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFoodDetailUseCase: GetFoodDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    private val foodId: String = checkNotNull(savedStateHandle["foodId"])

    val uiState: StateFlow<UiState<Food>> = getFoodDetailUseCase(foodId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    // Portion personnalisée (en grammes)
    private val _portionGrams = MutableStateFlow(100.0)
    val portionGrams = _portionGrams.asStateFlow()

    // Onglet actif (0=Nutrition, 1=Vitamines, 2=Bienfaits)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    fun onPortionChanged(grams: Double) {
        _portionGrams.value = grams.coerceIn(10.0, 1000.0)
    }

    fun onTabSelected(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            toggleFavoriteUseCase(foodId)
        }
    }

    /** Ajuste une valeur nutritionnelle selon la portion. */
    fun adjust(valuePer100g: Double): Double {
        return (valuePer100g * _portionGrams.value) / 100.0
    }
}
