package com.prumo.feature.estoque

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.prumo.core.i18n.t
import com.prumo.core.ui.AppPage
import com.prumo.core.ui.SectionCard
import com.prumo.core.ui.StateMessage
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

    AppPage(title = t("stock.title")) {
        state.error?.let { StateMessage(it, isError = true) }

        when (val snapshot = state.state) {
            ScreenState.Loading -> StateMessage(t("stock.loading"), modifier = Modifier.padding(top = 12.dp))
            ScreenState.Empty -> StateMessage(t("stock.empty"), modifier = Modifier.padding(top = 12.dp))
            is ScreenState.Error -> StateMessage(snapshot.message, modifier = Modifier.padding(top = 12.dp), isError = true)
            is ScreenState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snapshot.data) { item ->
                        val currentValue = editingValues[item.id] ?: item.estoqueAtual.toString()
                        SectionCard {
                            Text(item.materialNome ?: item.materialId)
                            StateMessage(t("stock.updated_at", "value" to item.atualizadoEm))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = currentValue,
                                    onValueChange = { editingValues[item.id] = it },
                                    label = { Text(t("stock.value_label")) }
                                )
                                Button(
                                    onClick = {
                                        viewModel.update(
                                            obraId,
                                            item.id,
                                            (editingValues[item.id] ?: item.estoqueAtual.toString()).toDoubleOrNull()
                                                ?: item.estoqueAtual
                                        )
                                    },
                                    enabled = state.updatingId != item.id,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(if (state.updatingId == item.id) t("stock.saving") else t("common.save"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
