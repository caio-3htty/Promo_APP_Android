package com.prumo.feature.pedidos

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.model.PedidoInput
import com.prumo.core.state.ScreenState

@Composable
fun PedidosScreen(
    obraId: String,
    viewModel: PedidosViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(obraId) {
        viewModel.load(obraId)
    }

    var materialId by remember { mutableStateOf("") }
    var fornecedorId by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("1") }
    var preco by remember { mutableStateOf("0") }
    var codigoCompra by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
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

        Text("Novo pedido", modifier = Modifier.padding(top = 12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = materialId,
                onValueChange = { materialId = it },
                label = { Text("Material ID") }
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = fornecedorId,
                onValueChange = { fornecedorId = it },
                label = { Text("Fornecedor ID") }
            )
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

        Button(
            onClick = {
                val input = PedidoInput(
                    obraId = obraId,
                    materialId = materialId,
                    fornecedorId = fornecedorId,
                    quantidade = quantidade.toDoubleOrNull() ?: 0.0,
                    precoUnit = preco.toDoubleOrNull() ?: 0.0,
                    status = "pendente",
                    codigoCompra = codigoCompra.ifBlank { null }
                )
                viewModel.create(obraId, input) {
                    materialId = ""
                    fornecedorId = ""
                    quantidade = "1"
                    preco = "0"
                    codigoCompra = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = !state.creating
        ) {
            Text(if (state.creating) "Salvando..." else "Criar pedido")
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        when (val snapshot = state.screenState) {
            ScreenState.Loading -> Text("Carregando pedidos...", modifier = Modifier.padding(top = 12.dp))
            ScreenState.Empty -> Text("Nenhum pedido encontrado", modifier = Modifier.padding(top = 12.dp))
            is ScreenState.Error -> Text(snapshot.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            is ScreenState.Content -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshot.data) { pedido ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${pedido.materialNome ?: pedido.materialId} - ${pedido.fornecedorNome ?: pedido.fornecedorId}")
                                Text("Qtd: ${pedido.quantidade} | Unit: ${pedido.precoUnit} | Total: ${pedido.total}")
                                Text("Status: ${pedido.status} | Codigo: ${pedido.codigoCompra ?: "-"}")
                            }
                        }
                    }
                }
            }
        }
    }
}
