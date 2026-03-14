package com.prumo.androidclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prumo.core.model.AppRole
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.SessionUser
import com.prumo.core.repository.ObrasRepository
import java.time.Instant
import kotlinx.coroutines.launch

@Composable
fun IndexScreen(
    user: SessionUser,
    onOpenObras: () -> Unit,
    onOpenCadastros: () -> Unit,
    onOpenUsuarios: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("PRUMO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Perfil: ${user.role?.wireValue ?: "sem papel"}")
        Text("Obras vinculadas: ${user.obraScope.size}")

        Button(onClick = onOpenObras, modifier = Modifier.fillMaxWidth()) { Text("Obras") }
        Button(onClick = onOpenCadastros, modifier = Modifier.fillMaxWidth()) { Text("Cadastros") }
        if (user.role == AppRole.MASTER || user.role == AppRole.GESTOR) {
            Button(onClick = onOpenUsuarios, modifier = Modifier.fillMaxWidth()) { Text("Usuarios e Acessos") }
        }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
    }
}

@Composable
fun SemAcessoScreen(
    user: SessionUser?,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Sem acesso operacional", style = MaterialTheme.typography.headlineSmall)
        Text("Ativo: ${user?.isActive ?: false}")
        Text("Perfil: ${user?.role?.wireValue ?: "indefinido"}")
        Text("Obras vinculadas: ${user?.obraScope?.size ?: 0}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh) { Text("Atualizar status") }
            Button(onClick = onGoHome) { Text("Inicio") }
            Button(onClick = onLogout) { Text("Sair") }
        }
    }
}

@Composable
fun DashboardScreen(
    obra: ObraSummary?,
    onOpenPedidos: () -> Unit,
    onOpenRecebimento: () -> Unit,
    onOpenEstoque: () -> Unit,
    onOpenCadastros: () -> Unit,
    onBackObras: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(obra?.name ?: "Dashboard da obra", style = MaterialTheme.typography.headlineSmall)
        Text(obra?.address ?: "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackObras) { Text("Voltar obras") }
            Button(onClick = onOpenCadastros) { Text("Cadastros") }
        }
        Button(onClick = onOpenPedidos, modifier = Modifier.fillMaxWidth()) { Text("Pedidos") }
        Button(onClick = onOpenRecebimento, modifier = Modifier.fillMaxWidth()) { Text("Recebimento") }
        Button(onClick = onOpenEstoque, modifier = Modifier.fillMaxWidth()) { Text("Estoque") }
    }
}

@Composable
fun ObrasManagerScreen(
    repository: ObrasRepository,
    canManage: Boolean,
    onOpenDashboard: (ObraSummary) -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<ObraSummary>>(emptyList()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var formName by remember { mutableStateOf("") }
    var formAddress by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formStatus by remember { mutableStateOf("ativa") }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                repository.listObras(
                    includeDeleted = showTrash,
                    deletedSinceIso = if (showTrash) Instant.now().minusSeconds(60L * 60L * 24L * 30L).toString() else null
                )
            }.onSuccess {
                items = it
                loading = false
            }.onFailure {
                error = it.message
                loading = false
            }
        }
    }

    LaunchedEffect(showTrash) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showTrash) "Lixeira de obras" else "Obras", style = MaterialTheme.typography.titleLarge)
            if (canManage) {
                Button(onClick = { showTrash = !showTrash }) { Text(if (showTrash) "Ativos" else "Lixeira") }
            }
        }

        if (canManage && !showTrash) {
            OutlinedTextField(value = formName, onValueChange = { formName = it }, label = { Text("Nome obra") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formAddress, onValueChange = { formAddress = it }, label = { Text("Endereco") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formDescription, onValueChange = { formDescription = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formStatus, onValueChange = { formStatus = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    runCatching {
                        repository.saveObra(
                            ObraSummary(
                                id = editingId ?: "",
                                name = formName,
                                status = formStatus,
                                address = formAddress.ifBlank { null },
                                description = formDescription.ifBlank { null }
                            )
                        )
                    }.onSuccess {
                        editingId = null
                        formName = ""
                        formAddress = ""
                        formDescription = ""
                        formStatus = "ativa"
                        load()
                    }.onFailure { error = it.message }
                }
            }) { Text(if (editingId == null) "Criar obra" else "Salvar obra") }
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { obra ->
                Card(onClick = { if (!showTrash) onOpenDashboard(obra) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(obra.name, fontWeight = FontWeight.SemiBold)
                        Text("Status: ${obra.status}")
                        obra.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showTrash) {
                                    Button(onClick = { scope.launch { repository.restoreObra(obra.id); load() } }) { Text("Restaurar") }
                                    Button(onClick = { scope.launch { repository.hardDeleteObra(obra.id); load() } }) { Text("Excluir") }
                                } else {
                                    Button(onClick = {
                                        editingId = obra.id
                                        formName = obra.name
                                        formAddress = obra.address.orEmpty()
                                        formDescription = obra.description.orEmpty()
                                        formStatus = obra.status
                                    }) { Text("Editar") }
                                    Button(onClick = { scope.launch { repository.softDeleteObra(obra.id); load() } }) { Text("Lixeira") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
