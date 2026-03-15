package com.prumo.feature.obras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prumo.core.i18n.statusLabel
import com.prumo.core.i18n.t
import com.prumo.core.model.ObraSummary
import com.prumo.core.state.ScreenState
import com.prumo.core.ui.AppPage
import com.prumo.core.ui.SectionCard
import com.prumo.core.ui.StateMessage

@Composable
fun ObrasScreen(
    viewModel: ObrasViewModel,
    onSelectObra: (ObraSummary) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    AppPage(title = t("obras.title")) {
        when (val snapshot = state) {
            is ScreenState.Loading -> StateMessage(t("obras.loading"))
            is ScreenState.Empty -> StateMessage(t("obras.empty"))
            is ScreenState.Error -> StateMessage(snapshot.message, isError = true)
            is ScreenState.Content -> {
                LazyColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snapshot.data) { obra ->
                        SectionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectObra(obra) }
                        ) {
                            Text(obra.name)
                            StateMessage(t("obras.status_value", "value" to statusLabel(obra.status)))
                            obra.address?.let { StateMessage(it) }
                        }
                    }
                }
            }
        }
    }
}
