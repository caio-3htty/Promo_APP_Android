package com.prumo.androidclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.prumo.core.model.AccessMode
import com.prumo.core.model.AccessRequestReviewData
import com.prumo.core.model.AccessReviewDecision
import com.prumo.core.model.AccessUserRecord
import com.prumo.core.model.AppRole
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.PedidoResumo
import com.prumo.core.model.RecebimentoInput
import com.prumo.core.model.UserAccessUpdateInput
import com.prumo.core.repository.AuthRepository
import com.prumo.core.repository.EstoqueRepository
import com.prumo.core.repository.ObrasRepository
import com.prumo.core.repository.PedidosRepository
import com.prumo.core.repository.UsuariosRepository
import java.time.Instant
import kotlinx.coroutines.launch

private data class AccessUserDraft(
    val isActive: Boolean,
    val role: AppRole?,
    val accessMode: AccessMode,
    val obraIds: Set<String>
)

@Composable
fun RecebimentoManagerScreen(
    obraId: String,
    pedidosRepository: PedidosRepository,
    estoqueRepository: EstoqueRepository,
    userId: String?
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<PedidoResumo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var codigoById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var recebimentoDataById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                pedidosRepository.listPedidos(obraId = obraId, status = null, search = search)
                    .filter { it.status == "aprovado" || it.status == "enviado" || it.status == "entregue" }
            }.onSuccess {
                items = it
                loading = false
                error = null
            }.onFailure {
                loading = false
                error = it.message
            }
        }
    }

    LaunchedEffect(search) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recebimento", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Buscar") }, modifier = Modifier.fillMaxWidth())
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                val codigo = codigoById[item.id] ?: item.codigoCompra.orEmpty()
                val dataRecebimento = recebimentoDataById[item.id] ?: Instant.now().toString()
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.materialNome ?: item.materialId} - ${item.fornecedorNome ?: item.fornecedorId}")
                        Text("Qtd: ${item.quantidade} | Status: ${item.status}")
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigoById = codigoById + (item.id to it) },
                            label = { Text("Codigo compra") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dataRecebimento,
                            onValueChange = { recebimentoDataById = recebimentoDataById + (item.id to it) },
                            label = { Text("Data recebimento (ISO)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (item.status != "entregue") {
                            Button(onClick = {
                                val finalCode = (codigoById[item.id] ?: item.codigoCompra.orEmpty()).trim()
                                if (finalCode.isBlank()) {
                                    error = "Codigo compra obrigatorio para confirmar recebimento."
                                    return@Button
                                }
                                val finalDate = (recebimentoDataById[item.id] ?: Instant.now().toString()).trim()
                                if (finalDate.isBlank()) {
                                    error = "Data recebimento obrigatoria."
                                    return@Button
                                }
                                scope.launch {
                                    runCatching {
                                        pedidosRepository.updatePedidoStatus(
                                            id = item.id,
                                            status = "entregue",
                                            codigoCompra = finalCode,
                                            dataRecebimentoIso = finalDate,
                                            recebidoPor = userId
                                        )
                                        estoqueRepository.upsertFromRecebimento(
                                            RecebimentoInput(
                                                pedidoId = item.id,
                                                obraId = item.obraId,
                                                materialId = item.materialId,
                                                quantidade = item.quantidade,
                                                codigoCompra = finalCode,
                                                dataRecebimentoIso = finalDate,
                                                recebidoPor = userId
                                            )
                                        )
                                    }.onSuccess { load() }.onFailure { error = it.message }
                                }
                            }) { Text("Confirmar recebimento") }
                        } else {
                            Text("Recebido em ${item.dataRecebimento ?: "-"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UsuariosAcessosScreen(
    usuariosRepository: UsuariosRepository,
    obrasRepository: ObrasRepository
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var users by remember { mutableStateOf<List<AccessUserRecord>>(emptyList()) }
    var obras by remember { mutableStateOf<List<ObraSummary>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<Map<String, AccessUserDraft>>(emptyMap()) }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                Pair(
                    usuariosRepository.listAccessUsers(),
                    obrasRepository.listObras(includeDeleted = false, deletedSinceIso = null)
                )
            }.onSuccess {
                users = it.first
                obras = it.second
                drafts = it.first.associate { user ->
                    user.userId to AccessUserDraft(
                        isActive = user.isActive,
                        role = user.role,
                        accessMode = user.accessMode,
                        obraIds = user.obraIds.toSet()
                    )
                }
                loading = false
                error = null
            }.onFailure {
                loading = false
                error = it.message
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Usuarios e Acessos", style = MaterialTheme.typography.titleLarge)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                val draft = drafts[user.userId]
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(user.fullName, fontWeight = FontWeight.SemiBold)
                        Text(user.email ?: user.userId, style = MaterialTheme.typography.bodySmall)
                        Text("Perfil: ${draft?.role?.wireValue ?: user.role?.wireValue ?: "sem papel"}")
                        Text("Obras: ${(draft?.obraIds ?: user.obraIds.toSet()).size}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                val current = drafts[user.userId] ?: AccessUserDraft(
                                    isActive = user.isActive,
                                    role = user.role,
                                    accessMode = user.accessMode,
                                    obraIds = user.obraIds.toSet()
                                )
                                drafts = drafts + (user.userId to current.copy(isActive = !current.isActive))
                            }) { Text(if ((draft?.isActive ?: user.isActive)) "Inativar" else "Ativar") }

                            Button(onClick = {
                                scope.launch {
                                    val current = drafts[user.userId] ?: AccessUserDraft(
                                        isActive = user.isActive,
                                        role = user.role,
                                        accessMode = user.accessMode,
                                        obraIds = user.obraIds.toSet()
                                    )
                                    val obraIds = current.obraIds.ifEmpty { obras.take(1).map { it.id }.toSet() }
                                    runCatching {
                                        usuariosRepository.saveUserAccess(
                                            UserAccessUpdateInput(
                                                userId = user.userId,
                                                tenantId = user.tenantId,
                                                isActive = current.isActive,
                                                role = current.role,
                                                accessMode = current.accessMode,
                                                userTypeId = user.userTypeId,
                                                obraIds = obraIds.toList()
                                            )
                                        )
                                    }.onSuccess { load() }.onFailure { error = it.message }
                                }
                            }) { Text("Salvar") }
                        }

                        Text("Modo acesso", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = {
                                val current = drafts[user.userId] ?: AccessUserDraft(
                                    isActive = user.isActive,
                                    role = user.role,
                                    accessMode = user.accessMode,
                                    obraIds = user.obraIds.toSet()
                                )
                                drafts = drafts + (user.userId to current.copy(accessMode = AccessMode.TEMPLATE))
                            }) {
                                val mode = draft?.accessMode ?: user.accessMode
                                Text(if (mode == AccessMode.TEMPLATE) "Template *" else "Template")
                            }
                            Button(onClick = {
                                val current = drafts[user.userId] ?: AccessUserDraft(
                                    isActive = user.isActive,
                                    role = user.role,
                                    accessMode = user.accessMode,
                                    obraIds = user.obraIds.toSet()
                                )
                                drafts = drafts + (user.userId to current.copy(accessMode = AccessMode.CUSTOM))
                            }) {
                                val mode = draft?.accessMode ?: user.accessMode
                                Text(if (mode == AccessMode.CUSTOM) "Custom *" else "Custom")
                            }
                        }

                        Text("Perfil", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(AppRole.MASTER, AppRole.GESTOR, AppRole.ENGENHEIRO, AppRole.OPERACIONAL, AppRole.ALMOXARIFE).forEach { role ->
                                Button(onClick = {
                                    val current = drafts[user.userId] ?: AccessUserDraft(
                                        isActive = user.isActive,
                                        role = user.role,
                                        accessMode = user.accessMode,
                                        obraIds = user.obraIds.toSet()
                                    )
                                    drafts = drafts + (user.userId to current.copy(role = role))
                                }) {
                                    val selectedRole = draft?.role ?: user.role
                                    Text(if (selectedRole == role) "${role.wireValue} *" else role.wireValue)
                                }
                            }
                        }

                        Text("Obras vinculadas", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            obras.forEach { obra ->
                                val selected = (draft?.obraIds ?: user.obraIds.toSet()).contains(obra.id)
                                Button(onClick = {
                                    val current = drafts[user.userId] ?: AccessUserDraft(
                                        isActive = user.isActive,
                                        role = user.role,
                                        accessMode = user.accessMode,
                                        obraIds = user.obraIds.toSet()
                                    )
                                    val next = current.obraIds.toMutableSet()
                                    if (selected) next.remove(obra.id) else next.add(obra.id)
                                    drafts = drafts + (user.userId to current.copy(obraIds = next))
                                }) {
                                    Text(if (selected) "${obra.name} *" else obra.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessRequestReviewScreen(
    token: String,
    authRepository: AuthRepository,
    onBackLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf<AccessRequestReviewData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reviewUsername by remember { mutableStateOf("") }
    var reviewJob by remember { mutableStateOf("") }
    var reviewRole by remember { mutableStateOf(AppRole.OPERACIONAL.wireValue) }
    var reviewNotes by remember { mutableStateOf("") }

    LaunchedEffect(token) {
        scope.launch {
            loading = true
            runCatching { authRepository.getAccessRequest(token) }
                .onSuccess {
                    data = it
                    reviewUsername = it.requestedUsername
                    reviewJob = it.requestedJobTitle
                    reviewRole = it.requestedRole.wireValue
                    loading = false
                }
                .onFailure {
                    error = it.message
                    loading = false
                }
        }
    }

    fun review(decision: AccessReviewDecision) {
        scope.launch {
            runCatching {
                authRepository.reviewAccessRequest(
                    token = token,
                    decision = decision,
                    reviewedUsername = reviewUsername,
                    reviewedJobTitle = reviewJob,
                    reviewedRole = reviewRole,
                    reviewNotes = reviewNotes
                )
            }.onSuccess {
                if (it.ok) onBackLogin() else error = it.message
            }.onFailure { error = it.message }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revisao de solicitacao", style = MaterialTheme.typography.titleLarge)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        data?.let {
            Text("Empresa: ${it.companyName}")
            Text("Solicitante: ${it.applicantFullName} (${it.applicantEmail})")
            OutlinedTextField(value = reviewUsername, onValueChange = { v -> reviewUsername = v }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reviewJob, onValueChange = { v -> reviewJob = v }, label = { Text("Cargo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reviewRole, onValueChange = { v -> reviewRole = v }, label = { Text("Perfil") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reviewNotes, onValueChange = { v -> reviewNotes = v }, label = { Text("Observacoes") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { review(AccessReviewDecision.APPROVE) }) { Text("Aprovar") }
                Button(onClick = { review(AccessReviewDecision.EDIT) }) { Text("Aprovar com edicao") }
                Button(onClick = { review(AccessReviewDecision.REJECT) }) { Text("Rejeitar") }
            }
        }
    }
}
