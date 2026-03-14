package com.prumo.core.model

data class SessionUser(
    val userId: String,
    val email: String,
    val fullName: String?,
    val role: String?,
    val tenantId: String,
    val obraScope: List<String>
)

data class SessionToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val user: SessionUser
)

data class ObraSummary(
    val id: String,
    val name: String,
    val status: String,
    val address: String?,
    val description: String?
)

data class MaterialSummary(
    val id: String,
    val nome: String,
    val unidade: String
)

data class FornecedorSummary(
    val id: String,
    val nome: String
)

data class PedidoResumo(
    val id: String,
    val obraId: String,
    val materialId: String,
    val materialNome: String?,
    val fornecedorId: String,
    val fornecedorNome: String?,
    val quantidade: Double,
    val precoUnit: Double,
    val total: Double,
    val status: String,
    val codigoCompra: String?
)

data class PedidoInput(
    val obraId: String,
    val materialId: String,
    val fornecedorId: String,
    val quantidade: Double,
    val precoUnit: Double,
    val status: String = "pendente",
    val codigoCompra: String? = null
)

data class EstoqueItem(
    val id: String,
    val obraId: String,
    val materialId: String,
    val materialNome: String?,
    val unidade: String?,
    val estoqueAtual: Double,
    val atualizadoEm: String
)

data class EstoqueUpdateInput(
    val estoqueAtual: Double
)

data class SupabaseConfig(
    val baseUrl: String,
    val anonKey: String
)