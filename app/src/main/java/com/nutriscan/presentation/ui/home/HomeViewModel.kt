package com.nutriscan.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.domain.model.Food
import com.nutriscan.domain.model.UiState
import com.nutriscan.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchFoodsUseCase: SearchFoodsUseCase,
    private val getHistoryUseCase: GetSearchHistoryUseCase,
) : ViewModel() {

    // ── État de la recherche ───────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<UiState<List<Food>>> = _searchQuery
        .debounce(300L)           // anti-rebond 300ms
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(UiState.Empty)
            else searchFoodsUseCase(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Empty
        )

    // ── Historique ─────────────────────────────────────────────────────────────

    val recentHistory: StateFlow<List<Food>> = getHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Actions ────────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}
