package com.prumo.feature.obras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.model.ObraSummary
import com.prumo.core.state.ScreenState

@Composable
fun ObrasScreen(
    viewModel: ObrasViewModel,
    onSelectObra: (ObraSummary) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    when (val snapshot = state) {
        is ScreenState.Loading -> Text("Carregando obras...", modifier = Modifier.padding(16.dp))
        is ScreenState.Empty -> Text("Nenhuma obra disponivel", modifier = Modifier.padding(16.dp))
        is ScreenState.Error -> Text("Erro: ${snapshot.message}", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        is ScreenState.Content -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapshot.data) { obra ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectObra(obra) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(obra.name, style = MaterialTheme.typography.titleMedium)
                            Text("Status: ${obra.status}", style = MaterialTheme.typography.bodyMedium)
                            obra.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}