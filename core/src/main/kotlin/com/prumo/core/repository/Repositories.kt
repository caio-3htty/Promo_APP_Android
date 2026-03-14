package com.prumo.core.repository

import com.prumo.core.model.EstoqueItem
import com.prumo.core.model.EstoqueUpdateInput
import com.prumo.core.model.FornecedorSummary
import com.prumo.core.model.MaterialSummary
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.model.SessionToken

interface SessionProvider {
    suspend fun currentSession(): SessionToken?
    suspend fun clearSession()
}

interface AuthRepository : SessionProvider {
    suspend fun login(email: String, password: String): SessionToken
    suspend fun logout()
}

interface ObrasRepository {
    suspend fun listObras(): List<ObraSummary>
}

interface PedidosRepository {
    suspend fun listPedidos(obraId: String, status: String?, search: String?): List<PedidoResumo>
    suspend fun listMateriais(): List<MaterialSummary>
    suspend fun listFornecedores(): List<FornecedorSummary>
    suspend fun createPedido(input: PedidoInput)
    suspend fun updatePedido(id: String, input: PedidoInput)
}

interface EstoqueRepository {
    suspend fun listEstoque(obraId: String): List<EstoqueItem>
    suspend fun updateEstoque(itemId: String, input: EstoqueUpdateInput)
}