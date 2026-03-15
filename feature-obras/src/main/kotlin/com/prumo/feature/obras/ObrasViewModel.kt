package com.prumo.feature.obras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prumo.core.model.ObraSummary
import com.prumo.core.repository.ObrasRepository
import com.prumo.core.state.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ObrasViewModel(
    private val repository: ObrasRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<ObraSummary>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<ObraSummary>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            runCatching { repository.listObras() }
                .onSuccess { obras ->
                    _state.value = if (obras.isEmpty()) ScreenState.Empty else ScreenState.Content(obras)
                }
                .onFailure {
                    _state.value = ScreenState.Error(it.message ?: "Falha ao carregar obras")
                }
        }
    }
}