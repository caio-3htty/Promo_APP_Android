@file:OptIn(ExperimentalLayoutApi::class)

package com.prumo.feature.pedidos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.i18n.statusLabel
import com.prumo.core.i18n.t
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.state.ScreenState
import com.prumo.core.ui.AppPage
import com.prumo.core.ui.SectionCard
import com.prumo.core.ui.StateMessage

private val statusOptions = listOf("pendente", "aprovado", "enviado", "entregue", "cancelado")

@Composable
fun PedidosScreen(
    obraId: String,
    viewModel: PedidosViewModel,
    canEditBase: Boolean,
    canApprove: Boolean,
    canDelete: Boolean
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(obraId) {
        viewModel.load(obraId)
    }

    var editing by remember { mutableStateOf<PedidoResumo?>(null) }
    var detailPedido by remember { mutableStateOf<PedidoResumo?>(null) }
    var showMaterialDialog by remember { mutableStateOf(false) }
    var showFornecedorDialog by remember { mutableStateOf(false) }
    var materialId by remember { mutableStateOf("") }
    var fornecedorId by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("1") }
    var preco by remember { mutableStateOf("0") }
    var codigoCompra by remember { mutableStateOf("") }
    var formStatus by remember { mutableStateOf("pendente") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun fillFrom(pedido: PedidoResumo?) {
        if (pedido == null) {
            editing = null
            materialId = ""
            fornecedorId = ""
            quantidade = "1"
            preco = "0"
            codigoCompra = ""
            formStatus = "pendente"
            return
        }
        editing = pedido
        materialId = pedido.materialId
        fornecedorId = pedido.fornecedorId
        quantidade = pedido.quantidade.toString()
        preco = pedido.precoUnit.toString()
        codigoCompra = pedido.codigoCompra.orEmpty()
        formStatus = pedido.status
    }

    val selectedMaterialName = state.materiais.firstOrNull { it.id == materialId }?.nome ?: "-"
    val selectedFornecedorName = state.fornecedores.firstOrNull { it.id == fornecedorId }?.nome ?: "-"
    val chooseMaterialError = t("orders.choose_material_first")
    val chooseSupplierError = t("orders.choose_supplier_first")

    AppPage(title = t("orders.title")) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.search,
                onValueChange = { viewModel.onSearch(it) },
                label = { Text(t("orders.search")) }
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.statusFilter,
                onValueChange = { viewModel.onStatusFilter(it) },
                label = { Text(t("orders.status")) }
            )
            Button(onClick = { viewModel.load(obraId) }) {
                Text(t("orders.filter"))
            }
        }

        if (canEditBase) {
            StateMessage(if (editing == null) t("orders.new") else t("orders.edit"))
            SectionCard {
                Text("${t("orders.select_material")}: $selectedMaterialName")
                Button(onClick = { showMaterialDialog = true }) { Text(t("orders.select_material")) }
            }
            SectionCard {
                Text("${t("orders.select_supplier")}: $selectedFornecedorName")
                Button(onClick = { showFornecedorDialog = true }) { Text(t("orders.select_supplier")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = quantidade,
                    onValueChange = { quantidade = it },
                    label = { Text(t("orders.quantity")) }
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = preco,
                    onValueChange = { preco = it },
                    label = { Text(t("orders.unit_price")) }
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = codigoCompra,
                onValueChange = { codigoCompra = it },
                label = { Text(t("orders.purchase_code")) }
            )

            if (canApprove) {
                Text(t("orders.order_status"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusOptions.forEach { status ->
                        Button(onClick = { formStatus = status }) {
                            val statusText = statusLabel(status)
                            Text(if (formStatus == status) "$statusText *" else statusText)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !state.creating,
                    onClick = {
                        formError = validatePedidoSelections(
                            materialId = materialId,
                            fornecedorId = fornecedorId,
                            materialError = chooseMaterialError,
                            fornecedorError = chooseSupplierError
                        )
                        if (formError != null) return@Button

                        val input = PedidoInput(
                            obraId = obraId,
                            materialId = materialId,
                            fornecedorId = fornecedorId,
                            quantidade = quantidade.toDoubleOrNull() ?: 0.0,
                            precoUnit = preco.toDoubleOrNull() ?: 0.0,
                            status = if (canApprove) formStatus else "pendente",
                            codigoCompra = codigoCompra.ifBlank { null }
                        )
                        if (editing == null) {
                            viewModel.create(obraId, input) { fillFrom(null) }
                        } else {
                            viewModel.update(obraId, editing!!.id, input) { fillFrom(null) }
                        }
                    }
                ) {
                    Text(
                        if (state.creating) t("orders.saving")
                        else if (editing == null) t("orders.create")
                        else t("orders.save_edit")
                    )
                }
                if (editing != null) {
                    Button(onClick = { fillFrom(null) }) { Text(t("orders.cancel")) }
                }
            }
            formError?.let { StateMessage(it, isError = true) }
        }

        state.error?.let { StateMessage(it, isError = true) }

        when (val snapshot = state.screenState) {
            ScreenState.Loading -> StateMessage(t("orders.loading"))
            ScreenState.Empty -> StateMessage(t("orders.empty"))
            is ScreenState.Error -> StateMessage(snapshot.message, isError = true)
            is ScreenState.Content -> {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshot.data) { pedido ->
                        PedidoCard(
                            pedido = pedido,
                            canEditBase = canEditBase,
                            canApprove = canApprove,
                            canDelete = canDelete,
                            onDetail = { detailPedido = pedido },
                            onEdit = { fillFrom(pedido) },
                            onApprove = { viewModel.updateStatus(obraId, pedido.id, "aprovado", pedido.codigoCompra) },
                            onCancel = { viewModel.updateStatus(obraId, pedido.id, "cancelado", pedido.codigoCompra) },
                            onDelete = { viewModel.softDelete(obraId, pedido.id) }
                        )
                    }
                }
            }
        }
    }

    if (showMaterialDialog) {
        SelectionDialog(
            title = t("orders.material_dialog"),
            options = state.materiais.map { it.id to it.nome },
            onSelect = {
                materialId = it
                showMaterialDialog = false
            },
            onDismiss = { showMaterialDialog = false }
        )
    }

    if (showFornecedorDialog) {
        SelectionDialog(
            title = t("orders.supplier_dialog"),
            options = state.fornecedores.map { it.id to it.nome },
            onSelect = {
                fornecedorId = it
                showFornecedorDialog = false
            },
            onDismiss = { showFornecedorDialog = false }
        )
    }

    detailPedido?.let { pedido ->
        AlertDialog(
            onDismissRequest = { detailPedido = null },
            title = { Text(t("orders.details_dialog")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t("orders.field_id", "value" to pedido.id))
                    Text(t("orders.field_material", "value" to (pedido.materialNome ?: pedido.materialId)))
                    Text(t("orders.field_supplier", "value" to (pedido.fornecedorNome ?: pedido.fornecedorId)))
                    Text(t("orders.field_quantity", "value" to pedido.quantidade))
                    Text(t("orders.field_unit_price", "value" to pedido.precoUnit))
                    Text(t("orders.field_total", "value" to pedido.total))
                    Text(t("orders.field_status", "value" to statusLabel(pedido.status)))
                    Text(t("orders.field_purchase_code", "value" to (pedido.codigoCompra ?: "-")))
                    Text(t("orders.field_created_at", "value" to (pedido.criadoEm ?: "-")))
                }
            },
            confirmButton = {
                Button(onClick = { detailPedido = null }) { Text(t("orders.cancel")) }
            }
        )
    }
}

@Composable
private fun PedidoCard(
    pedido: PedidoResumo,
    canEditBase: Boolean,
    canApprove: Boolean,
    canDelete: Boolean,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetail() }
    ) {
        Text("${pedido.materialNome ?: pedido.materialId} - ${pedido.fornecedorNome ?: pedido.fornecedorId}")
        Text(
            t(
                "orders.card_summary",
                "qty" to pedido.quantidade,
                "unit" to pedido.precoUnit,
                "total" to pedido.total
            )
        )
        Text(
            t(
                "orders.card_status",
                "status" to statusLabel(pedido.status),
                "code" to (pedido.codigoCompra ?: "-")
            )
        )
        Text(t("orders.field_created_at", "value" to (pedido.criadoEm ?: "-")))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onDetail) { Text(t("orders.details")) }
            if (canEditBase) {
                Button(onClick = onEdit) { Text(t("orders.edit")) }
            }
            if (canApprove && pedido.status != "entregue" && pedido.status != "cancelado") {
                Button(onClick = onApprove) { Text(t("orders.approve")) }
                Button(onClick = onCancel) { Text(t("orders.reject")) }
            }
            if (canDelete) {
                Button(onClick = onDelete) { Text(t("orders.delete")) }
            }
        }
    }
}

internal fun validatePedidoSelections(
    materialId: String,
    fornecedorId: String,
    materialError: String,
    fornecedorError: String
): String? {
    if (materialId.isBlank()) return materialError
    if (fornecedorId.isBlank()) return fornecedorError
    return null
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options) { option ->
                    SectionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.first) }
                    ) {
                        Text(option.second, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(t("orders.cancel")) }
        }
    )
}
