package com.prumo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prumo.core.model.AppRole
import com.prumo.core.model.SignupMode
import com.prumo.core.model.SignupRequestInput
import com.prumo.core.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isSignUp: Boolean = false,
    val signupMode: SignupMode = SignupMode.COMPANY_OWNER,
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val companyName: String = "",
    val username: String = "",
    val jobTitle: String = "",
    val requestedRole: AppRole = AppRole.OPERACIONAL,
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val success: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun onFullNameChange(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value, error = null)
    }

    fun onCompanyNameChange(value: String) {
        _uiState.value = _uiState.value.copy(companyName = value, error = null)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onJobTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(jobTitle = value, error = null)
    }

    fun onSignupModeChange(mode: SignupMode) {
        _uiState.value = _uiState.value.copy(signupMode = mode, error = null, message = null)
    }

    fun onRequestedRoleChange(role: AppRole) {
        _uiState.value = _uiState.value.copy(requestedRole = role, error = null)
    }

    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUp = !_uiState.value.isSignUp,
            error = null,
            message = null
        )
    }

    fun submit(origin: String) {
        if (_uiState.value.isSignUp) {
            signup(origin)
        } else {
            login()
        }
    }

    private fun login() {
        val snapshot = _uiState.value
        if (snapshot.email.isBlank() || snapshot.password.isBlank()) {
            _uiState.value = snapshot.copy(error = "Informe e-mail e senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = snapshot.copy(loading = true, error = null)
            runCatching { authRepository.login(snapshot.email.trim(), snapshot.password) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        success = true,
                        message = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Falha no login")
                }
        }
    }

    private fun signup(origin: String) {
        val snapshot = _uiState.value
        if (
            snapshot.email.isBlank() ||
            snapshot.password.length < 6 ||
            snapshot.fullName.isBlank() ||
            snapshot.companyName.isBlank() ||
            snapshot.username.isBlank() ||
            snapshot.jobTitle.isBlank()
        ) {
            _uiState.value = snapshot.copy(error = "Preencha todos os campos e use senha com 6+ caracteres.")
            return
        }

        viewModelScope.launch {
            _uiState.value = snapshot.copy(loading = true, error = null, message = null)
            runCatching {
                authRepository.signup(
                    SignupRequestInput(
                        mode = snapshot.signupMode,
                        email = snapshot.email.trim().lowercase(),
                        password = snapshot.password,
                        fullName = snapshot.fullName.trim(),
                        companyName = snapshot.companyName.trim(),
                        username = snapshot.username.trim(),
                        jobTitle = snapshot.jobTitle.trim(),
                        requestedRole = if (snapshot.signupMode == SignupMode.COMPANY_OWNER) AppRole.MASTER else snapshot.requestedRole,
                        origin = origin
                    )
                )
            }.onSuccess { result ->
                if (result.ok) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = result.message,
                        error = null,
                        isSignUp = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Falha ao enviar solicitacao")
            }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }
}
