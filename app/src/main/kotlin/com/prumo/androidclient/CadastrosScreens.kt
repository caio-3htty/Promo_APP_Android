package com.prumo.androidclient

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
import com.prumo.core.model.FornecedorSummary
import com.prumo.core.model.MaterialFornecedorRecord
import com.prumo.core.model.MaterialRecord
import com.prumo.core.repository.CadastrosRepository
import java.time.Instant
import kotlinx.coroutines.launch

@Composable
fun FornecedoresManagerScreen(
    repository: CadastrosRepository,
    canManage: Boolean
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var showTrash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<FornecedorSummary>>(emptyList()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var nome by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var contatos by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                repository.listFornecedores(
                    includeDeleted = showTrash,
                    deletedSinceIso = if (showTrash) Instant.now().minusSeconds(60L * 60L * 24L * 30L).toString() else null
                )
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

    LaunchedEffect(showTrash) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showTrash) "Lixeira de fornecedores" else "Fornecedores", style = MaterialTheme.typography.titleLarge)
            if (canManage) Button(onClick = { showTrash = !showTrash }) { Text(if (showTrash) "Ativos" else "Lixeira") }
        }

        if (canManage && !showTrash) {
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contatos, onValueChange = { contatos = it }, label = { Text("Contatos") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    runCatching {
                        repository.saveFornecedor(
                            FornecedorSummary(
                                id = editingId ?: "",
                                nome = nome,
                                cnpj = cnpj.ifBlank { null },
                                contatos = contatos.ifBlank { null },
                                entregaPropria = false,
                                deletedAt = null
                            )
                        )
                    }.onSuccess {
                        editingId = null
                        nome = ""
                        cnpj = ""
                        contatos = ""
                        load()
                    }.onFailure { error = it.message }
                }
            }) { Text(if (editingId == null) "Criar" else "Salvar") }
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.nome, fontWeight = FontWeight.SemiBold)
                        Text(item.cnpj.orEmpty())
                        Text(item.contatos.orEmpty())
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showTrash) {
                                    Button(onClick = { scope.launch { repository.restoreFornecedor(item.id); load() } }) { Text("Restaurar") }
                                    Button(onClick = { scope.launch { repository.hardDeleteFornecedor(item.id); load() } }) { Text("Excluir") }
                                } else {
                                    Button(onClick = {
                                        editingId = item.id
                                        nome = item.nome
                                        cnpj = item.cnpj.orEmpty()
                                        contatos = item.contatos.orEmpty()
                                    }) { Text("Editar") }
                                    Button(onClick = { scope.launch { repository.softDeleteFornecedor(item.id); load() } }) { Text("Lixeira") }
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
fun MateriaisManagerScreen(
    repository: CadastrosRepository,
    canManage: Boolean
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var showTrash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<MaterialRecord>>(emptyList()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var nome by remember { mutableStateOf("") }
    var unidade by remember { mutableStateOf("un") }
    var tempo by remember { mutableStateOf("0") }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                repository.listMateriais(
                    includeDeleted = showTrash,
                    deletedSinceIso = if (showTrash) Instant.now().minusSeconds(60L * 60L * 24L * 30L).toString() else null
                )
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

    LaunchedEffect(showTrash) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showTrash) "Lixeira de materiais" else "Materiais", style = MaterialTheme.typography.titleLarge)
            if (canManage) Button(onClick = { showTrash = !showTrash }) { Text(if (showTrash) "Ativos" else "Lixeira") }
        }

        if (canManage && !showTrash) {
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = unidade, onValueChange = { unidade = it }, label = { Text("Unidade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tempo, onValueChange = { tempo = it }, label = { Text("Tempo producao") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    runCatching {
                        repository.saveMaterial(
                            MaterialRecord(
                                id = editingId ?: "",
                                nome = nome,
                                unidade = unidade,
                                tempoProducaoPadrao = tempo.toIntOrNull(),
                                estoqueMinimo = 0.0,
                                deletedAt = null
                            )
                        )
                    }.onSuccess {
                        editingId = null
                        nome = ""
                        unidade = "un"
                        tempo = "0"
                        load()
                    }.onFailure { error = it.message }
                }
            }) { Text(if (editingId == null) "Criar" else "Salvar") }
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.nome, fontWeight = FontWeight.SemiBold)
                        Text("Unidade: ${item.unidade}")
                        Text("Tempo: ${item.tempoProducaoPadrao ?: "-"}")
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showTrash) {
                                    Button(onClick = { scope.launch { repository.restoreMaterial(item.id); load() } }) { Text("Restaurar") }
                                    Button(onClick = { scope.launch { repository.hardDeleteMaterial(item.id); load() } }) { Text("Excluir") }
                                } else {
                                    Button(onClick = {
                                        editingId = item.id
                                        nome = item.nome
                                        unidade = item.unidade
                                        tempo = item.tempoProducaoPadrao?.toString() ?: "0"
                                    }) { Text("Editar") }
                                    Button(onClick = { scope.launch { repository.softDeleteMaterial(item.id); load() } }) { Text("Lixeira") }
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
fun MaterialFornecedorManagerScreen(
    repository: CadastrosRepository,
    canManage: Boolean
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<MaterialFornecedorRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var materialId by remember { mutableStateOf("") }
    var fornecedorId by remember { mutableStateOf("") }
    var precoAtual by remember { mutableStateOf("") }
    var pedidoMinimo by remember { mutableStateOf("0") }
    var leadTime by remember { mutableStateOf("0") }
    var validade by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                repository.listMaterialFornecedor(showTrash, if (showTrash) Instant.now().minusSeconds(60L * 60L * 24L * 30L).toString() else null)
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

    LaunchedEffect(showTrash) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showTrash) "Lixeira Material x Fornecedor" else "Material x Fornecedor", style = MaterialTheme.typography.titleLarge)
            if (canManage) Button(onClick = { showTrash = !showTrash }) { Text(if (showTrash) "Ativos" else "Lixeira") }
        }

        if (canManage && !showTrash) {
            OutlinedTextField(value = materialId, onValueChange = { materialId = it }, label = { Text("Material ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fornecedorId, onValueChange = { fornecedorId = it }, label = { Text("Fornecedor ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = precoAtual, onValueChange = { precoAtual = it }, label = { Text("Preco atual") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pedidoMinimo, onValueChange = { pedidoMinimo = it }, label = { Text("Pedido minimo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = leadTime, onValueChange = { leadTime = it }, label = { Text("Lead time dias") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = validade, onValueChange = { validade = it }, label = { Text("Validade preco (yyyy-mm-dd)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    runCatching {
                        repository.saveMaterialFornecedor(
                            MaterialFornecedorRecord(
                                id = editingId ?: "",
                                materialId = materialId,
                                fornecedorId = fornecedorId,
                                precoAtual = precoAtual.toDoubleOrNull() ?: 0.0,
                                pedidoMinimo = pedidoMinimo.toDoubleOrNull() ?: 0.0,
                                leadTimeDias = leadTime.toIntOrNull() ?: 0,
                                validadePreco = validade.ifBlank { null },
                                materialNome = null,
                                materialUnidade = null,
                                fornecedorNome = null,
                                fornecedorCnpj = null,
                                deletedAt = null
                            )
                        )
                    }.onSuccess {
                        editingId = null
                        materialId = ""
                        fornecedorId = ""
                        precoAtual = ""
                        pedidoMinimo = "0"
                        leadTime = "0"
                        validade = ""
                        load()
                    }.onFailure { error = it.message }
                }
            }) { Text(if (editingId == null) "Criar vinculo" else "Salvar vinculo") }
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.materialNome ?: item.materialId} x ${item.fornecedorNome ?: item.fornecedorId}")
                        Text("Preco: ${item.precoAtual} | Min: ${item.pedidoMinimo} | Lead: ${item.leadTimeDias}")
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showTrash) {
                                    Button(onClick = { scope.launch { repository.restoreMaterialFornecedor(item.id); load() } }) { Text("Restaurar") }
                                    Button(onClick = { scope.launch { repository.hardDeleteMaterialFornecedor(item.id); load() } }) { Text("Excluir") }
                                } else {
                                    Button(onClick = {
                                        editingId = item.id
                                        materialId = item.materialId
                                        fornecedorId = item.fornecedorId
                                        precoAtual = item.precoAtual.toString()
                                        pedidoMinimo = item.pedidoMinimo.toString()
                                        leadTime = item.leadTimeDias.toString()
                                        validade = item.validadePreco.orEmpty()
                                    }) { Text("Editar") }
                                    Button(onClick = { scope.launch { repository.softDeleteMaterialFornecedor(item.id); load() } }) { Text("Lixeira") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
