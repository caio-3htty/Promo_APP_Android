package com.prumo.feature.estoque

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.state.ScreenState

@Composable
fun EstoqueScreen(
    obraId: String,
    viewModel: EstoqueViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editingValues = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(obraId) {
        viewModel.load(obraId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Estoque", style = MaterialTheme.typography.titleLarge)

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        when (val snapshot = state.state) {
            ScreenState.Loading -> Text("Carregando estoque...", modifier = Modifier.padding(top = 12.dp))
            ScreenState.Empty -> Text("Sem itens no estoque", modifier = Modifier.padding(top = 12.dp))
            is ScreenState.Error -> Text(snapshot.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            is ScreenState.Content -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshot.data) { item ->
                        val currentValue = editingValues[item.id] ?: item.estoqueAtual.toString()
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.materialNome ?: item.materialId)
                                Text("Atualizado em: ${item.atualizadoEm}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        modifier = Modifier.weight(1f),
                                        value = currentValue,
                                        onValueChange = { editingValues[item.id] = it },
                                        label = { Text("Estoque") }
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.update(obraId, item.id, (editingValues[item.id] ?: item.estoqueAtual.toString()).toDoubleOrNull() ?: item.estoqueAtual)
                                        },
                                        enabled = state.updatingId != item.id,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(if (state.updatingId == item.id) "..." else "Salvar")
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