package com.prumo.androidclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.prumo.core.i18n.t
import com.prumo.core.i18n.statusLabel
import com.prumo.core.model.AppRole
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.SessionUser
import com.prumo.core.repository.ObrasRepository
import com.prumo.core.ui.AppPage
import com.prumo.core.ui.SectionCard
import com.prumo.core.ui.StateMessage
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
    AppPage(title = t("app.name")) {
        StateMessage(t("home.role", "value" to (user.role?.wireValue ?: t("home.no_role"))))
        StateMessage(t("home.linked_works", "count" to user.obraScope.size))

        Button(onClick = onOpenObras, modifier = Modifier.fillMaxWidth()) { Text(t("home.works")) }
        Button(onClick = onOpenCadastros, modifier = Modifier.fillMaxWidth()) { Text(t("home.registers")) }
        if (user.role == AppRole.MASTER || user.role == AppRole.GESTOR) {
            Button(onClick = onOpenUsuarios, modifier = Modifier.fillMaxWidth()) { Text(t("home.users_access")) }
        }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text(t("home.logout")) }
    }
}

@Composable
fun SemAcessoScreen(
    user: SessionUser?,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    onLogout: () -> Unit
) {
    AppPage(title = t("access.no_operational")) {
        StateMessage(t("access.active", "value" to (user?.isActive ?: false)))
        StateMessage(t("home.role", "value" to (user?.role?.wireValue ?: t("access.undefined"))))
        StateMessage(t("home.linked_works", "count" to (user?.obraScope?.size ?: 0)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh) { Text(t("access.refresh")) }
            Button(onClick = onGoHome) { Text(t("access.home")) }
            Button(onClick = onLogout) { Text(t("home.logout")) }
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
    AppPage(title = obra?.name ?: t("dashboard.default"), subtitle = obra?.address.orEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackObras) { Text(t("dashboard.back_works")) }
            Button(onClick = onOpenCadastros) { Text(t("home.registers")) }
        }
        Button(onClick = onOpenPedidos, modifier = Modifier.fillMaxWidth()) { Text(t("dashboard.orders")) }
        Button(onClick = onOpenRecebimento, modifier = Modifier.fillMaxWidth()) { Text(t("dashboard.receipt")) }
        Button(onClick = onOpenEstoque, modifier = Modifier.fillMaxWidth()) { Text(t("dashboard.stock")) }
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

    AppPage(
        title = if (showTrash) t("obras.trash_title") else t("obras.title"),
        actions = {
            if (canManage) {
                Button(onClick = { showTrash = !showTrash }) {
                    Text(if (showTrash) t("common.active") else t("common.trash"))
                }
            }
        }
    ) {
        if (canManage && !showTrash) {
            SectionCard {
                OutlinedTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = { Text(t("obras.name")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formAddress,
                    onValueChange = { formAddress = it },
                    label = { Text(t("obras.address")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formDescription,
                    onValueChange = { formDescription = it },
                    label = { Text(t("obras.description")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formStatus,
                    onValueChange = { formStatus = it },
                    label = { Text(t("obras.status")) },
                    modifier = Modifier.fillMaxWidth()
                )
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
                }) { Text(if (editingId == null) t("obras.create") else t("obras.save")) }
            }
        }

        if (loading) {
            CircularProgressIndicator()
        }
        error?.let { StateMessage(it, isError = true) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { obra ->
                Card(onClick = { if (!showTrash) onOpenDashboard(obra) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(obra.name, fontWeight = FontWeight.SemiBold)
                        Text(t("obras.status_value", "value" to statusLabel(obra.status)))
                        obra.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showTrash) {
                                    Button(onClick = { scope.launch { repository.restoreObra(obra.id); load() } }) {
                                        Text(t("common.restore"))
                                    }
                                    Button(onClick = { scope.launch { repository.hardDeleteObra(obra.id); load() } }) {
                                        Text(t("common.delete"))
                                    }
                                } else {
                                    Button(onClick = {
                                        editingId = obra.id
                                        formName = obra.name
                                        formAddress = obra.address.orEmpty()
                                        formDescription = obra.description.orEmpty()
                                        formStatus = obra.status
                                    }) { Text(t("common.edit")) }
                                    Button(onClick = { scope.launch { repository.softDeleteObra(obra.id); load() } }) {
                                        Text(t("common.trash"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
