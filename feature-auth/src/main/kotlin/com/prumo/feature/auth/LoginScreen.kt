package com.prumo.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.model.AppRole
import com.prumo.core.model.SignupMode

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    origin: String,
    onLoggedIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) {
            viewModel.consumeSuccess()
            onLoggedIn()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PRUMO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            if (state.isSignUp) "Criar conta / solicitar acesso" else "Entrar",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        if (state.isSignUp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.onSignupModeChange(SignupMode.COMPANY_OWNER) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.loading
                ) {
                    Text(if (state.signupMode == SignupMode.COMPANY_OWNER) "Conta empresa *" else "Conta empresa")
                }
                Button(
                    onClick = { viewModel.onSignupModeChange(SignupMode.COMPANY_INTERNAL) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.loading
                ) {
                    Text(if (state.signupMode == SignupMode.COMPANY_INTERNAL) "Conta interna *" else "Conta interna")
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Nome completo") },
                enabled = !state.loading
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = state.companyName,
                onValueChange = viewModel::onCompanyNameChange,
                label = { Text("Nome da empresa") },
                enabled = !state.loading
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Usuario (nome exibido)") },
                enabled = !state.loading
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = state.jobTitle,
                onValueChange = viewModel::onJobTitleChange,
                label = { Text("Cargo") },
                enabled = !state.loading
            )
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (state.isSignUp) 12.dp else 0.dp),
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("E-mail corporativo") },
            enabled = !state.loading
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.loading
        )

        if (state.isSignUp && state.signupMode == SignupMode.COMPANY_INTERNAL) {
            RolePicker(
                selected = state.requestedRole,
                enabled = !state.loading,
                onSelect = viewModel::onRequestedRoleChange
            )
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        state.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        Button(
            onClick = { viewModel.submit(origin) },
            enabled = !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            if (state.loading) {
                CircularProgressIndicator()
            } else {
                Text(if (state.isSignUp) "Enviar solicitacao" else "Entrar")
            }
        }

        Button(
            onClick = viewModel::toggleSignUpMode,
            enabled = !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(if (state.isSignUp) "Ja tenho conta" else "Criar conta")
        }
    }
}

@Composable
private fun RolePicker(
    selected: AppRole,
    enabled: Boolean,
    onSelect: (AppRole) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("Perfil de acesso", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                AppRole.GESTOR,
                AppRole.ENGENHEIRO,
                AppRole.OPERACIONAL,
                AppRole.ALMOXARIFE
            ).forEach { role ->
                Button(
                    onClick = { onSelect(role) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (selected == role) "${role.wireValue} *" else role.wireValue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
