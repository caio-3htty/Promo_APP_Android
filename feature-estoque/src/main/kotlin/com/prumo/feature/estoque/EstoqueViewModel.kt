package com.prumo.feature.estoque

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prumo.core.model.EstoqueItem
import com.prumo.core.model.EstoqueUpdateInput
import com.prumo.core.repository.EstoqueRepository
import com.prumo.core.state.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstoqueUiState(
    val state: ScreenState<List<EstoqueItem>> = ScreenState.Loading,
    val updatingId: String? = null,
    val error: String? = null
)

class EstoqueViewModel(
    private val repository: EstoqueRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EstoqueUiState())
    val uiState: StateFlow<EstoqueUiState> = _uiState.asStateFlow()

    fun load(obraId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(state = ScreenState.Loading)
            runCatching { repository.listEstoque(obraId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(state = if (it.isEmpty()) ScreenState.Empty else ScreenState.Content(it), error = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(state = ScreenState.Error(it.message ?: "Falha ao carregar estoque"), error = it.message)
                }
        }
    }

    fun update(obraId: String, itemId: String, novoEstoque: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingId = itemId, error = null)
            runCatching { repository.updateEstoque(itemId, EstoqueUpdateInput(novoEstoque)) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(updatingId = null)
                    load(obraId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(updatingId = null, error = it.message ?: "Falha ao atualizar estoque")
                }
        }
    }
}