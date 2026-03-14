package com.prumo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: AuthUserDto
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String
)

@Serializable
data class ProfileDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class UserRoleDto(val role: String)

@Serializable
data class ObraDto(
    val id: String,
    val name: String,
    val status: String,
    val address: String? = null,
    val description: String? = null
)

@Serializable
data class MaterialDto(
    val id: String,
    val nome: String,
    val unidade: String
)

@Serializable
data class FornecedorDto(
    val id: String,
    val nome: String
)

@Serializable
data class NestedMaterialDto(
    val nome: String? = null,
    val unidade: String? = null
)

@Serializable
data class NestedFornecedorDto(
    val nome: String? = null
)

@Serializable
data class PedidoDto(
    val id: String,
    @SerialName("obra_id") val obraId: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("fornecedor_id") val fornecedorId: String,
    val quantidade: Double,
    @SerialName("preco_unit") val precoUnit: Double,
    val total: Double,
    val status: String,
    @SerialName("codigo_compra") val codigoCompra: String? = null,
    val materiais: NestedMaterialDto? = null,
    val fornecedores: NestedFornecedorDto? = null
)

@Serializable
data class EstoqueDto(
    val id: String,
    @SerialName("obra_id") val obraId: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("estoque_atual") val estoqueAtual: Double,
    @SerialName("atualizado_em") val atualizadoEm: String,
    val materiais: NestedMaterialDto? = null
)

@Serializable
data class PedidoUpsertDto(
    @SerialName("obra_id") val obraId: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("fornecedor_id") val fornecedorId: String,
    val quantidade: Double,
    @SerialName("preco_unit") val precoUnit: Double,
    val total: Double,
    val status: String,
    @SerialName("codigo_compra") val codigoCompra: String? = null
)

@Serializable
data class EstoquePatchDto(
    @SerialName("estoque_atual") val estoqueAtual: Double
)