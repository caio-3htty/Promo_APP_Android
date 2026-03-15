package com.prumo.androidclient

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prumo.core.i18n.statusLabel
import com.prumo.core.i18n.t
import com.prumo.core.model.AccessAuditEntry
import com.prumo.core.model.AccessMode
import com.prumo.core.model.AccessRequestReviewData
import com.prumo.core.model.AccessReviewDecision
import com.prumo.core.model.AccessUserRecord
import com.prumo.core.model.AppRole
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.PedidoResumo
import com.prumo.core.model.PermissionCatalogItem
import com.prumo.core.model.PermissionScopeType
import com.prumo.core.model.RecebimentoInput
import com.prumo.core.model.UserAccessUpdateInput
import com.prumo.core.model.UserPermissionGrantDraft
import com.prumo.core.model.UserTypeRecord
import com.prumo.core.model.UserTypeUpsertInput
import com.prumo.core.repository.AuthRepository
import com.prumo.core.repository.EstoqueRepository
import com.prumo.core.repository.ObrasRepository
import com.prumo.core.repository.PedidosRepository
import com.prumo.core.repository.UsuariosRepository
import com.prumo.core.ui.AppPage
import com.prumo.core.ui.SectionCard
import com.prumo.core.ui.StateMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private enum class AccessSection {
    USERS,
    TYPES
}

private data class AccessUserDraft(
    val isActive: Boolean,
    val role: AppRole?,
    val accessMode: AccessMode,
    val userTypeId: String?,
    val obraIds: Set<String>,
    val grants: List<UserPermissionGrantDraft>
)

private data class UserTypeDraft(
    val id: String?,
    val name: String,
    val description: String,
    val baseRole: AppRole,
    val isActive: Boolean
)

@Composable
fun RecebimentoManagerScreen(
    obraId: String,
    pedidosRepository: PedidosRepository,
    estoqueRepository: EstoqueRepository,
    userId: String?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<PedidoResumo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var codigoById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var recebimentoDateById by remember { mutableStateOf<Map<String, LocalDate>>(emptyMap()) }
    val requiredCodeMessage = t("receipt.required_code")

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

    AppPage(title = t("receipt.title")) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(t("receipt.search")) },
            modifier = Modifier.fillMaxWidth()
        )
        if (loading) CircularProgressIndicator()
        error?.let { StateMessage(it, isError = true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                val codigo = codigoById[item.id] ?: item.codigoCompra.orEmpty()
                val selectedDate = recebimentoDateById[item.id]
                    ?: parseDate(item.dataRecebimento)
                    ?: LocalDate.now()
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${item.materialNome ?: item.materialId} - ${item.fornecedorNome ?: item.fornecedorId}")
                        Text(
                            t(
                                "receipt.item_summary",
                                "qty" to item.quantidade,
                                "status" to statusLabel(item.status)
                            )
                        )
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigoById = codigoById + (item.id to it) },
                            label = { Text(t("receipt.purchase_code")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = selectedDate.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(t("receipt.received_date")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            val picker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    recebimentoDateById = recebimentoDateById + (
                                        item.id to LocalDate.of(year, month + 1, dayOfMonth)
                                        )
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            )
                            picker.show()
                        }) { Text(t("receipt.pick_date")) }

                        if (item.status != "entregue") {
                            Button(onClick = {
                                val finalCode = (codigoById[item.id] ?: item.codigoCompra.orEmpty()).trim()
                                if (finalCode.isBlank()) {
                                    error = requiredCodeMessage
                                    return@Button
                                }
                                val date = recebimentoDateById[item.id]
                                    ?: parseDate(item.dataRecebimento)
                                    ?: LocalDate.now()
                                val finalDateIso = date.atStartOfDay(ZoneOffset.UTC).toInstant().toString()
                                scope.launch {
                                    runCatching {
                                        pedidosRepository.updatePedidoStatus(
                                            id = item.id,
                                            status = "entregue",
                                            codigoCompra = finalCode,
                                            dataRecebimentoIso = finalDateIso,
                                            recebidoPor = userId
                                        )
                                        estoqueRepository.upsertFromRecebimento(
                                            RecebimentoInput(
                                                pedidoId = item.id,
                                                obraId = item.obraId,
                                                materialId = item.materialId,
                                                quantidade = item.quantidade,
                                                codigoCompra = finalCode,
                                                dataRecebimentoIso = finalDateIso,
                                                recebidoPor = userId
                                            )
                                        )
                                    }.onSuccess { load() }.onFailure { error = it.message }
                                }
                            }) { Text(t("receipt.confirm")) }
                        } else {
                            Text(t("receipt.received_at", "value" to (item.dataRecebimento ?: "-")))
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
    obrasRepository: ObrasRepository,
    currentUserId: String?
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var users by remember { mutableStateOf<List<AccessUserRecord>>(emptyList()) }
    var obras by remember { mutableStateOf<List<ObraSummary>>(emptyList()) }
    var permissionCatalog by remember { mutableStateOf<List<PermissionCatalogItem>>(emptyList()) }
    var userTypes by remember { mutableStateOf<List<UserTypeRecord>>(emptyList()) }
    var accessAuditLog by remember { mutableStateOf<List<AccessAuditEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeSection by remember { mutableStateOf(AccessSection.USERS) }
    var drafts by remember { mutableStateOf<Map<String, AccessUserDraft>>(emptyMap()) }
    var userTypeDraft by remember {
        mutableStateOf(
            UserTypeDraft(
                id = null,
                name = "",
                description = "",
                baseRole = AppRole.OPERACIONAL,
                isActive = true
            )
        )
    }
    val requiredTypeNameMessage = t("validation.required_field", "field" to t("types.name"))

    fun resetTypeDraft() {
        userTypeDraft = UserTypeDraft(
            id = null,
            name = "",
            description = "",
            baseRole = AppRole.OPERACIONAL,
            isActive = true
        )
    }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                val loadedUsers = usuariosRepository.listAccessUsers()
                val loadedObras = obrasRepository.listObras(includeDeleted = false, deletedSinceIso = null)
                val loadedCatalog = usuariosRepository.listPermissionCatalog()
                val loadedTypes = usuariosRepository.listUserTypes()
                val loadedAudit = usuariosRepository.listAccessAuditLog(limit = 50)
                Quintuple(loadedUsers, loadedObras, loadedCatalog, loadedTypes, loadedAudit)
            }.onSuccess { loaded ->
                users = loaded.first
                obras = loaded.second
                permissionCatalog = loaded.third
                userTypes = loaded.fourth
                accessAuditLog = loaded.fifth
                drafts = users.associate { user ->
                    user.userId to AccessUserDraft(
                        isActive = user.isActive,
                        role = user.role,
                        accessMode = user.accessMode,
                        userTypeId = user.userTypeId,
                        obraIds = user.obraIds.toSet(),
                        grants = user.grants
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
    val userDisplayById = remember(users) {
        users.associate { user ->
            user.userId to user.fullName.ifBlank { user.email ?: user.userId }
        }
    }

    fun baseDraft(user: AccessUserRecord): AccessUserDraft {
        return drafts[user.userId] ?: AccessUserDraft(
            isActive = user.isActive,
            role = user.role,
            accessMode = user.accessMode,
            userTypeId = user.userTypeId,
            obraIds = user.obraIds.toSet(),
            grants = user.grants
        )
    }

    fun updateDraft(user: AccessUserRecord, updater: (AccessUserDraft) -> AccessUserDraft) {
        val current = baseDraft(user)
        drafts = drafts + (user.userId to updater(current))
    }

    fun upsertGrant(
        current: AccessUserDraft,
        permissionKey: String,
        checked: Boolean,
        obraScoped: Boolean
    ): AccessUserDraft {
        val hasGrant = current.grants.any { it.permissionKey == permissionKey }
        if (!checked && hasGrant) {
            return current.copy(grants = current.grants.filterNot { it.permissionKey == permissionKey })
        }
        if (!checked || hasGrant) return current
        return current.copy(
            grants = current.grants + UserPermissionGrantDraft(
                permissionKey = permissionKey,
                scopeType = if (obraScoped) PermissionScopeType.ALL_OBRAS else PermissionScopeType.TENANT,
                obraIds = emptyList()
            )
        )
    }

    AppPage(title = t("users.title")) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { activeSection = AccessSection.USERS }) {
                Text(if (activeSection == AccessSection.USERS) "${t("users.tab_users")} *" else t("users.tab_users"))
            }
            Button(onClick = { activeSection = AccessSection.TYPES }) {
                Text(if (activeSection == AccessSection.TYPES) "${t("users.tab_types")} *" else t("users.tab_types"))
            }
        }

        if (loading) CircularProgressIndicator()
        error?.let { StateMessage(it, isError = true) }

        if (activeSection == AccessSection.USERS) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    val draft = baseDraft(user)
                    val selectedType = userTypes.firstOrNull { it.id == draft.userTypeId }
                    val effectiveRole = selectedType?.baseRole ?: draft.role
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user.fullName, fontWeight = FontWeight.SemiBold)
                            Text(user.email ?: user.userId, style = MaterialTheme.typography.bodySmall)
                            Text(t("home.role", "value" to (effectiveRole?.wireValue ?: t("home.no_role"))))
                            Text(t("home.linked_works", "count" to draft.obraIds.size))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    updateDraft(user) { current -> current.copy(isActive = !current.isActive) }
                                }) { Text(if (draft.isActive) t("users.deactivate") else t("users.activate")) }

                                Button(onClick = {
                                    scope.launch {
                                        val current = baseDraft(user)
                                        val obraIds = current.obraIds.ifEmpty { obras.take(1).map { it.id }.toSet() }
                                        val typeBaseRole = userTypes.firstOrNull { it.id == current.userTypeId }?.baseRole
                                        runCatching {
                                            usuariosRepository.saveUserAccess(
                                                UserAccessUpdateInput(
                                                    userId = user.userId,
                                                    tenantId = user.tenantId,
                                                    isActive = current.isActive,
                                                    role = typeBaseRole ?: current.role,
                                                    accessMode = current.accessMode,
                                                    userTypeId = current.userTypeId,
                                                    obraIds = obraIds.toList(),
                                                    grants = if (current.accessMode == AccessMode.CUSTOM) current.grants else emptyList(),
                                                    changedByUserId = currentUserId
                                                )
                                            )
                                        }.onSuccess { load() }.onFailure { error = it.message }
                                    }
                                }) { Text(t("users.save")) }
                            }

                            Text(t("users.user_type"), style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = {
                                    updateDraft(user) { current -> current.copy(userTypeId = null) }
                                }) {
                                    val selected = draft.userTypeId == null
                                    Text(if (selected) "${t("users.no_type")} *" else t("users.no_type"))
                                }
                                userTypes.forEach { type ->
                                    Button(onClick = {
                                        updateDraft(user) { current -> current.copy(userTypeId = type.id) }
                                    }) {
                                        val selected = draft.userTypeId == type.id
                                        Text(if (selected) "${type.name} *" else type.name)
                                    }
                                }
                            }

                            Text(t("users.access_mode"), style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = {
                                    updateDraft(user) { current ->
                                        current.copy(accessMode = AccessMode.TEMPLATE, grants = emptyList())
                                    }
                                }) {
                                    Text(if (draft.accessMode == AccessMode.TEMPLATE) "${t("users.template")} *" else t("users.template"))
                                }
                                Button(onClick = {
                                    updateDraft(user) { current ->
                                        current.copy(accessMode = AccessMode.CUSTOM)
                                    }
                                }) {
                                    Text(if (draft.accessMode == AccessMode.CUSTOM) "${t("users.custom")} *" else t("users.custom"))
                                }
                            }

                            Text(t("users.profile"), style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(AppRole.MASTER, AppRole.GESTOR, AppRole.ENGENHEIRO, AppRole.OPERACIONAL, AppRole.ALMOXARIFE).forEach { role ->
                                    Button(onClick = {
                                        updateDraft(user) { current -> current.copy(role = role) }
                                    }) {
                                        Text(if (draft.role == role) "${role.wireValue} *" else role.wireValue)
                                    }
                                }
                            }

                            Text(t("users.works"), style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                obras.forEach { obra ->
                                    val selected = draft.obraIds.contains(obra.id)
                                    Button(onClick = {
                                        updateDraft(user) { current ->
                                            val next = current.obraIds.toMutableSet()
                                            if (selected) next.remove(obra.id) else next.add(obra.id)
                                            current.copy(obraIds = next)
                                        }
                                    }) {
                                        Text(if (selected) "${obra.name} *" else obra.name)
                                    }
                                }
                            }

                            if (draft.accessMode == AccessMode.CUSTOM) {
                                Text(t("users.custom_permissions"), style = MaterialTheme.typography.labelLarge)
                                permissionCatalog.forEach { permission ->
                                    val grant = draft.grants.firstOrNull { it.permissionKey == permission.key }
                                    val checked = grant != null
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("${permission.labelPt} (${permission.area})")
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(onClick = {
                                                    updateDraft(user) { current ->
                                                        upsertGrant(current, permission.key, !checked, permission.obraScoped)
                                                    }
                                                }) {
                                                    Text(if (checked) t("users.permission_remove") else t("users.permission_add"))
                                                }
                                            }

                                            if (checked && grant != null) {
                                                Text(t("users.scope"), style = MaterialTheme.typography.labelMedium)
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(onClick = {
                                                        updateDraft(user) { current ->
                                                            current.copy(
                                                                grants = current.grants.map {
                                                                    if (it.permissionKey == permission.key) it.copy(
                                                                        scopeType = PermissionScopeType.TENANT,
                                                                        obraIds = emptyList()
                                                                    ) else it
                                                                }
                                                            )
                                                        }
                                                    }) {
                                                        Text(
                                                            if (grant.scopeType == PermissionScopeType.TENANT) {
                                                                "${t("users.scope_tenant")} *"
                                                            } else {
                                                                t("users.scope_tenant")
                                                            }
                                                        )
                                                    }
                                                    if (permission.obraScoped) {
                                                        Button(onClick = {
                                                            updateDraft(user) { current ->
                                                                current.copy(
                                                                    grants = current.grants.map {
                                                                        if (it.permissionKey == permission.key) it.copy(
                                                                            scopeType = PermissionScopeType.ALL_OBRAS,
                                                                            obraIds = emptyList()
                                                                        ) else it
                                                                    }
                                                                )
                                                            }
                                                        }) {
                                                            Text(
                                                                if (grant.scopeType == PermissionScopeType.ALL_OBRAS) {
                                                                    "${t("users.scope_all_works")} *"
                                                                } else {
                                                                    t("users.scope_all_works")
                                                                }
                                                            )
                                                        }
                                                        Button(onClick = {
                                                            updateDraft(user) { current ->
                                                                current.copy(
                                                                    grants = current.grants.map {
                                                                        if (it.permissionKey == permission.key) it.copy(
                                                                            scopeType = PermissionScopeType.SELECTED_OBRAS
                                                                        ) else it
                                                                    }
                                                                )
                                                            }
                                                        }) {
                                                            Text(
                                                                if (grant.scopeType == PermissionScopeType.SELECTED_OBRAS) {
                                                                    "${t("users.scope_selected_works")} *"
                                                                } else {
                                                                    t("users.scope_selected_works")
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                if (grant.scopeType == PermissionScopeType.SELECTED_OBRAS) {
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        obras.forEach { obra ->
                                                            val obraSelected = grant.obraIds.contains(obra.id)
                                                            Button(onClick = {
                                                                updateDraft(user) { current ->
                                                                    current.copy(
                                                                        grants = current.grants.map { item ->
                                                                            if (item.permissionKey != permission.key) return@map item
                                                                            val next = item.obraIds.toMutableSet()
                                                                            if (obraSelected) next.remove(obra.id) else next.add(obra.id)
                                                                            item.copy(obraIds = next.toList())
                                                                        }
                                                                    )
                                                                }
                                                            }) {
                                                                Text(if (obraSelected) "${obra.name} *" else obra.name)
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
                    }
                }
                item {
                    Text(t("users.audit_log"), style = MaterialTheme.typography.titleMedium)
                    SectionCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (accessAuditLog.isEmpty()) {
                                Text(t("users.audit_empty"))
                            } else {
                                accessAuditLog.forEach { entry ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "${formatAuditDate(entry.createdAt)} | ${entry.entityTable} | ${entry.action}",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${t("users.audit_target")}: ${
                                                resolveAuditUserLabel(entry.targetUserId, userDisplayById)
                                            }"
                                        )
                                        Text(
                                            "${t("users.audit_author")}: ${
                                                resolveAuditUserLabel(entry.changedBy, userDisplayById)
                                            }"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(t("types.title"), style = MaterialTheme.typography.titleMedium)
                            Text(t("types.subtitle"), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = userTypeDraft.name,
                                onValueChange = { userTypeDraft = userTypeDraft.copy(name = it) },
                                label = { Text(t("types.name")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = userTypeDraft.description,
                                onValueChange = { userTypeDraft = userTypeDraft.copy(description = it) },
                                label = { Text(t("types.description")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(t("types.base_role"), style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(AppRole.MASTER, AppRole.GESTOR, AppRole.ENGENHEIRO, AppRole.OPERACIONAL, AppRole.ALMOXARIFE).forEach { role ->
                                    Button(onClick = { userTypeDraft = userTypeDraft.copy(baseRole = role) }) {
                                        Text(
                                            if (userTypeDraft.baseRole == role) "${role.wireValue} *" else role.wireValue
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    userTypeDraft = userTypeDraft.copy(isActive = !userTypeDraft.isActive)
                                }) {
                                    Text(if (userTypeDraft.isActive) t("users.deactivate") else t("users.activate"))
                                }
                                Button(onClick = {
                                    scope.launch {
                                        if (userTypeDraft.name.isBlank()) {
                                            error = requiredTypeNameMessage
                                            return@launch
                                        }
                                        runCatching {
                                            usuariosRepository.saveUserType(
                                                UserTypeUpsertInput(
                                                    id = userTypeDraft.id,
                                                    name = userTypeDraft.name,
                                                    description = userTypeDraft.description.ifBlank { null },
                                                    baseRole = userTypeDraft.baseRole,
                                                    isActive = userTypeDraft.isActive,
                                                    createdByUserId = currentUserId
                                                )
                                            )
                                        }.onSuccess {
                                            resetTypeDraft()
                                            load()
                                        }.onFailure { error = it.message }
                                    }
                                }) { Text(t("users.save")) }
                                if (userTypeDraft.id != null) {
                                    Button(onClick = { resetTypeDraft() }) { Text(t("orders.cancel")) }
                                }
                            }
                        }
                    }
                }
                items(userTypes) { userType ->
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(userType.name, fontWeight = FontWeight.SemiBold)
                            Text(userType.description ?: t("types.no_description"))
                            Text(t("types.base_role_value", "value" to userType.baseRole.wireValue))
                            Text(
                                if (userType.isActive) t("types.status_active") else t("types.status_inactive")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    userTypeDraft = UserTypeDraft(
                                        id = userType.id,
                                        name = userType.name,
                                        description = userType.description.orEmpty(),
                                        baseRole = userType.baseRole,
                                        isActive = userType.isActive
                                    )
                                }) { Text(t("types.edit")) }
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

    AppPage(title = t("access_review.title"), modifier = Modifier.padding(4.dp)) {
        if (loading) CircularProgressIndicator()
        error?.let { StateMessage(it, isError = true) }
        data?.let {
            StateMessage(t("access_review.company", "value" to it.companyName))
            StateMessage(
                t(
                    "access_review.requester",
                    "name" to it.applicantFullName,
                    "email" to it.applicantEmail
                )
            )
            OutlinedTextField(
                value = reviewUsername,
                onValueChange = { v -> reviewUsername = v },
                label = { Text(t("access_review.username")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = reviewJob,
                onValueChange = { v -> reviewJob = v },
                label = { Text(t("access_review.job_title")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = reviewRole,
                onValueChange = { v -> reviewRole = v },
                label = { Text(t("access_review.role")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = reviewNotes,
                onValueChange = { v -> reviewNotes = v },
                label = { Text(t("access_review.notes")) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { review(AccessReviewDecision.APPROVE) }) { Text(t("access_review.approve")) }
                Button(onClick = { review(AccessReviewDecision.EDIT) }) { Text(t("access_review.approve_edit")) }
                Button(onClick = { review(AccessReviewDecision.REJECT) }) { Text(t("access_review.reject")) }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

internal fun resolveAuditUserLabel(
    userId: String?,
    displayById: Map<String, String>
): String {
    if (userId.isNullOrBlank()) return "-"
    return displayById[userId] ?: userId
}

private fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun formatAuditDate(value: String): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return value
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC)
    return formatter.format(instant)
}
