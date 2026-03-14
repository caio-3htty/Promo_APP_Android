package com.prumo.androidclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.SessionToken
import com.prumo.core.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val bootDone: Boolean = false,
    val session: SessionToken? = null,
    val selectedObra: ObraSummary? = null
)

class MainViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun bootstrap() {
        viewModelScope.launch {
            val current = authRepository.currentSession()
            _state.value = _state.value.copy(bootDone = true, session = current)
        }
    }

    fun onLoggedIn(token: SessionToken) {
        _state.value = _state.value.copy(session = token)
    }

    fun selectObra(obra: ObraSummary) {
        _state.value = _state.value.copy(selectedObra = obra)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = MainUiState(bootDone = true)
        }
    }
}