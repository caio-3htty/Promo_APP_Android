package com.prumo.feature.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prumo.core.model.FornecedorSummary
import com.prumo.core.model.MaterialSummary
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.repository.PedidosRepository
import com.prumo.core.state.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PedidosUiState(
    val screenState: ScreenState<List<PedidoResumo>> = ScreenState.Loading,
    val materiais: List<MaterialSummary> = emptyList(),
    val fornecedores: List<FornecedorSummary> = emptyList(),
    val search: String = "",
    val statusFilter: String = "",
    val creating: Boolean = false,
    val error: String? = null
)

class PedidosViewModel(
    private val repository: PedidosRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PedidosUiState())
    val state: StateFlow<PedidosUiState> = _state.asStateFlow()

    fun onSearch(value: String) {
        _state.value = _state.value.copy(search = value)
    }

    fun onStatusFilter(value: String) {
        _state.value = _state.value.copy(statusFilter = value)
    }

    fun load(obraId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(screenState = ScreenState.Loading)
            runCatching {
                val pedidos = repository.listPedidos(
                    obraId = obraId,
                    status = _state.value.statusFilter.ifBlank { null },
                    search = _state.value.search.ifBlank { null }
                )
                val materiais = repository.listMateriais()
                val fornecedores = repository.listFornecedores()
                Triple(pedidos, materiais, fornecedores)
            }.onSuccess { (pedidos, materiais, fornecedores) ->
                _state.value = _state.value.copy(
                    screenState = if (pedidos.isEmpty()) ScreenState.Empty else ScreenState.Content(pedidos),
                    materiais = materiais,
                    fornecedores = fornecedores,
                    error = null
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    screenState = ScreenState.Error(it.message ?: "Falha ao carregar pedidos"),
                    error = it.message
                )
            }
        }
    }

    fun create(obraId: String, input: PedidoInput, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(creating = true, error = null)
            runCatching { repository.createPedido(input.copy(obraId = obraId)) }
                .onSuccess {
                    _state.value = _state.value.copy(creating = false)
                    load(obraId)
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(creating = false, error = it.message ?: "Falha ao criar pedido")
                }
        }
    }

    fun update(obraId: String, id: String, input: PedidoInput, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(creating = true, error = null)
            runCatching { repository.updatePedido(id, input.copy(obraId = obraId)) }
                .onSuccess {
                    _state.value = _state.value.copy(creating = false)
                    load(obraId)
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(creating = false, error = it.message ?: "Falha ao atualizar pedido")
                }
        }
    }
}