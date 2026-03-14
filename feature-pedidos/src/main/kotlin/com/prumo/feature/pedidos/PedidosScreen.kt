@file:OptIn(ExperimentalLayoutApi::class)

package com.prumo.feature.pedidos

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
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.state.ScreenState

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
    var materialId by remember { mutableStateOf("") }
    var fornecedorId by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("1") }
    var preco by remember { mutableStateOf("0") }
    var codigoCompra by remember { mutableStateOf("") }
    var formStatus by remember { mutableStateOf("pendente") }

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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pedidos", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.search,
                onValueChange = { viewModel.onSearch(it) },
                label = { Text("Busca") }
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.statusFilter,
                onValueChange = { viewModel.onStatusFilter(it) },
                label = { Text("Status") }
            )
            Button(onClick = { viewModel.load(obraId) }) {
                Text("Filtrar")
            }
        }

        if (canEditBase) {
            Text(if (editing == null) "Novo pedido" else "Editar pedido", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = materialId,
                onValueChange = { materialId = it },
                label = { Text("Material ID") }
            )
            if (state.materiais.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.materiais.take(8).forEach { material ->
                        Button(onClick = { materialId = material.id }) {
                            Text(material.nome)
                        }
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fornecedorId,
                onValueChange = { fornecedorId = it },
                label = { Text("Fornecedor ID") }
            )
            if (state.fornecedores.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.fornecedores.take(8).forEach { fornecedor ->
                        Button(onClick = { fornecedorId = fornecedor.id }) {
                            Text(fornecedor.nome)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = quantidade,
                    onValueChange = { quantidade = it },
                    label = { Text("Quantidade") }
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = preco,
                    onValueChange = { preco = it },
                    label = { Text("Preco unitario") }
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = codigoCompra,
                onValueChange = { codigoCompra = it },
                label = { Text("Codigo compra") }
            )

            if (canApprove) {
                Text("Status do pedido", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusOptions.forEach { status ->
                        Button(onClick = { formStatus = status }) {
                            Text(if (formStatus == status) "$status *" else status)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !state.creating,
                    onClick = {
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
                    Text(if (state.creating) "Salvando..." else if (editing == null) "Criar pedido" else "Salvar edicao")
                }
                if (editing != null) {
                    Button(onClick = { fillFrom(null) }) { Text("Cancelar") }
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        when (val snapshot = state.screenState) {
            ScreenState.Loading -> Text("Carregando pedidos...")
            ScreenState.Empty -> Text("Nenhum pedido encontrado")
            is ScreenState.Error -> Text(snapshot.message, color = MaterialTheme.colorScheme.error)
            is ScreenState.Content -> {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshot.data) { pedido ->
                        PedidoCard(
                            pedido = pedido,
                            canEditBase = canEditBase,
                            canApprove = canApprove,
                            canDelete = canDelete,
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
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PedidoCard(
    pedido: PedidoResumo,
    canEditBase: Boolean,
    canApprove: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${pedido.materialNome ?: pedido.materialId} - ${pedido.fornecedorNome ?: pedido.fornecedorId}")
            Text("Qtd: ${pedido.quantidade} | Unit: ${pedido.precoUnit} | Total: ${pedido.total}")
            Text("Status: ${pedido.status} | Codigo: ${pedido.codigoCompra ?: "-"}")
            Text("Criado em: ${pedido.criadoEm ?: "-"}")

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canEditBase) {
                    Button(onClick = onEdit) { Text("Editar") }
                }
                if (canApprove && pedido.status != "entregue" && pedido.status != "cancelado") {
                    Button(onClick = onApprove) { Text("Aprovar") }
                    Button(onClick = onCancel) { Text("Cancelar") }
                }
                if (canDelete) {
                    Button(onClick = onDelete) { Text("Excluir") }
                }
            }
        }
    }
}
