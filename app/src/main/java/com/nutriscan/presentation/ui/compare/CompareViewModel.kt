package com.nutriscan.presentation.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import com.nutriscan.domain.usecase.CompareFoodsUseCase
import com.nutriscan.domain.usecase.SearchFoodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  State
// ─────────────────────────────────────────────────────────────────────────────

data class CompareUiState(
    val selectedFoods: List<Food> = emptyList(),      // Max 3
    val searchQuery: String = "",
    val searchResults: UiState<List<Food>> = UiState.Empty,
    val highlights: Map<String, String> = emptyMap(),  // nutrient -> winning food ID
    val isSearching: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val searchFoodsUseCase: SearchFoodsUseCase,
    private val compareFoodsUseCase: CompareFoodsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CompareUiState())
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<UiState<List<Food>>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(UiState.Empty)
            else searchFoodsUseCase(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Empty)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun addFood(food: Food) {
        val current = _state.value.selectedFoods
        if (current.size >= 3 || current.any { it.id == food.id }) return
        val updated = current + food
        _state.update {
            it.copy(
                selectedFoods = updated,
                highlights = compareFoodsUseCase(updated),
                searchQuery = "",
                isSearching = false
            )
        }
        _searchQuery.value = ""
    }

    fun removeFood(foodId: String) {
        val updated = _state.value.selectedFoods.filter { it.id != foodId }
        _state.update {
            it.copy(
                selectedFoods = updated,
                highlights = compareFoodsUseCase(updated)
            )
        }
    }

    fun clearAll() {
        _state.update { CompareUiState() }
    }
}
