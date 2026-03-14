package com.prumo.data.repository

import com.prumo.core.model.EstoqueItem
import com.prumo.core.model.EstoqueUpdateInput
import com.prumo.core.model.FornecedorSummary
import com.prumo.core.model.MaterialSummary
import com.prumo.core.model.ObraSummary
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.model.SessionToken
import com.prumo.core.model.SessionUser
import com.prumo.core.model.SupabaseConfig
import com.prumo.core.repository.AuthRepository
import com.prumo.core.repository.EstoqueRepository
import com.prumo.core.repository.ObrasRepository
import com.prumo.core.repository.PedidosRepository
import com.prumo.data.api.SupabaseAuthApi
import com.prumo.data.api.SupabaseRestClient
import com.prumo.data.model.AuthResponseDto
import com.prumo.data.model.EstoqueDto
import com.prumo.data.model.EstoquePatchDto
import com.prumo.data.model.FornecedorDto
import com.prumo.data.model.MaterialDto
import com.prumo.data.model.ObraDto
import com.prumo.data.model.PedidoDto
import com.prumo.data.model.PedidoUpsertDto
import com.prumo.data.model.ProfileDto
import com.prumo.data.model.UserRoleDto
import com.prumo.data.storage.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SupabaseAuthRepository(
    private val restClient: SupabaseRestClient,
    private val authApi: SupabaseAuthApi,
    private val sessionStore: SessionStore
) : AuthRepository {
    override suspend fun login(email: String, password: String): SessionToken {
        return withContext(Dispatchers.IO) {
            val auth = authApi.login(email, password)
            val user = loadUserContext(auth)
            val token = auth.toSessionToken(user)
            sessionStore.save(token)
            token
        }
    }

    override suspend fun currentSession(): SessionToken? = sessionStore.read()

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            sessionStore.clear()
        }
    }

    override suspend fun clearSession() {
        withContext(Dispatchers.IO) {
            sessionStore.clear()
        }
    }

    private suspend fun loadUserContext(auth: AuthResponseDto): SessionUser {
        val userId = auth.user.id

        val profiles = restClient.getList(
            path = "profiles",
            query = mapOf(
                "select" to "full_name,tenant_id,is_active",
                "user_id" to "eq.$userId",
                "limit" to "1"
            ),
            deserializer = ProfileDto.serializer()
        )

        val profile = profiles.firstOrNull() ?: error("Perfil nao encontrado para usuario autenticado")
        if (!profile.isActive) error("Usuario inativo")

        val roles = restClient.getList(
            path = "user_roles",
            query = mapOf(
                "select" to "role",
                "user_id" to "eq.$userId",
                "tenant_id" to "eq.${profile.tenantId}",
                "limit" to "1"
            ),
            deserializer = UserRoleDto.serializer()
        )

        val obras = restClient.getList(
            path = "obras",
            query = mapOf(
                "select" to "id,name,status,address,description",
                "order" to "name.asc"
            ),
            deserializer = ObraDto.serializer()
        )

        return SessionUser(
            userId = userId,
            email = auth.user.email,
            fullName = profile.fullName,
            role = roles.firstOrNull()?.role,
            tenantId = profile.tenantId,
            obraScope = obras.map { it.id }
        )
    }
}

class SupabaseObrasRepository(
    private val restClient: SupabaseRestClient
) : ObrasRepository {
    override suspend fun listObras(): List<ObraSummary> {
        return withContext(Dispatchers.IO) {
            restClient.getList(
                path = "obras",
                query = mapOf(
                    "select" to "id,name,status,address,description",
                    "deleted_at" to "is.null",
                    "order" to "name.asc"
                ),
                deserializer = ObraDto.serializer()
            ).map {
                ObraSummary(
                    id = it.id,
                    name = it.name,
                    status = it.status,
                    address = it.address,
                    description = it.description
                )
            }
        }
    }
}

class SupabasePedidosRepository(
    private val restClient: SupabaseRestClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : PedidosRepository {
    override suspend fun listPedidos(obraId: String, status: String?, search: String?): List<PedidoResumo> {
        return withContext(Dispatchers.IO) {
            val query = mutableMapOf(
                "select" to "id,obra_id,material_id,fornecedor_id,quantidade,preco_unit,total,status,codigo_compra,materiais(nome,unidade),fornecedores(nome)",
                "obra_id" to "eq.$obraId",
                "deleted_at" to "is.null",
                "order" to "criado_em.desc"
            )

            if (!status.isNullOrBlank()) query["status"] = "eq.$status"
            if (!search.isNullOrBlank()) {
                val encoded = search.replace(",", " ")
                query["or"] = "codigo_compra.ilike.*$encoded*,status.ilike.*$encoded*"
            }

            restClient.getList(
                path = "pedidos_compra",
                query = query,
                deserializer = PedidoDto.serializer()
            ).map {
                PedidoResumo(
                    id = it.id,
                    obraId = it.obraId,
                    materialId = it.materialId,
                    materialNome = it.materiais?.nome,
                    fornecedorId = it.fornecedorId,
                    fornecedorNome = it.fornecedores?.nome,
                    quantidade = it.quantidade,
                    precoUnit = it.precoUnit,
                    total = it.total,
                    status = it.status,
                    codigoCompra = it.codigoCompra
                )
            }
        }
    }

    override suspend fun listMateriais(): List<MaterialSummary> {
        return withContext(Dispatchers.IO) {
            restClient.getList(
                path = "materiais",
                query = mapOf(
                    "select" to "id,nome,unidade",
                    "deleted_at" to "is.null",
                    "order" to "nome.asc"
                ),
                deserializer = MaterialDto.serializer()
            ).map { MaterialSummary(it.id, it.nome, it.unidade) }
        }
    }

    override suspend fun listFornecedores(): List<FornecedorSummary> {
        return withContext(Dispatchers.IO) {
            restClient.getList(
                path = "fornecedores",
                query = mapOf(
                    "select" to "id,nome",
                    "deleted_at" to "is.null",
                    "order" to "nome.asc"
                ),
                deserializer = FornecedorDto.serializer()
            ).map { FornecedorSummary(it.id, it.nome) }
        }
    }

    override suspend fun createPedido(input: PedidoInput) {
        withContext(Dispatchers.IO) {
            val payload = PedidoUpsertDto(
                obraId = input.obraId,
                materialId = input.materialId,
                fornecedorId = input.fornecedorId,
                quantidade = input.quantidade,
                precoUnit = input.precoUnit,
                total = input.quantidade * input.precoUnit,
                status = input.status,
                codigoCompra = input.codigoCompra
            )
            restClient.post("pedidos_compra", json.encodeToString(PedidoUpsertDto.serializer(), payload))
        }
    }

    override suspend fun updatePedido(id: String, input: PedidoInput) {
        withContext(Dispatchers.IO) {
            val payload = PedidoUpsertDto(
                obraId = input.obraId,
                materialId = input.materialId,
                fornecedorId = input.fornecedorId,
                quantidade = input.quantidade,
                precoUnit = input.precoUnit,
                total = input.quantidade * input.precoUnit,
                status = input.status,
                codigoCompra = input.codigoCompra
            )
            restClient.patch(
                path = "pedidos_compra",
                query = mapOf("id" to "eq.$id"),
                bodyJson = json.encodeToString(PedidoUpsertDto.serializer(), payload)
            )
        }
    }
}

class SupabaseEstoqueRepository(
    private val restClient: SupabaseRestClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EstoqueRepository {
    override suspend fun listEstoque(obraId: String): List<EstoqueItem> {
        return withContext(Dispatchers.IO) {
            restClient.getList(
                path = "estoque_obra_material",
                query = mapOf(
                    "select" to "id,obra_id,material_id,estoque_atual,atualizado_em,materiais(nome,unidade)",
                    "obra_id" to "eq.$obraId",
                    "order" to "atualizado_em.desc"
                ),
                deserializer = EstoqueDto.serializer()
            ).map {
                EstoqueItem(
                    id = it.id,
                    obraId = it.obraId,
                    materialId = it.materialId,
                    materialNome = it.materiais?.nome,
                    unidade = it.materiais?.unidade,
                    estoqueAtual = it.estoqueAtual,
                    atualizadoEm = it.atualizadoEm
                )
            }
        }
    }

    override suspend fun updateEstoque(itemId: String, input: EstoqueUpdateInput) {
        withContext(Dispatchers.IO) {
            val payload = EstoquePatchDto(input.estoqueAtual)
            restClient.patch(
                path = "estoque_obra_material",
                query = mapOf("id" to "eq.$itemId"),
                bodyJson = json.encodeToString(EstoquePatchDto.serializer(), payload)
            )
        }
    }
}

class AppContainer(
    config: SupabaseConfig,
    sessionStore: SessionStore
) {
    private val authApi = SupabaseAuthApi(config)
    private val restClient = SupabaseRestClient(config, sessionStore, authApi)

    val authRepository: AuthRepository = SupabaseAuthRepository(restClient, authApi, sessionStore)
    val obrasRepository: ObrasRepository = SupabaseObrasRepository(restClient)
    val pedidosRepository: PedidosRepository = SupabasePedidosRepository(restClient)
    val estoqueRepository: EstoqueRepository = SupabaseEstoqueRepository(restClient)
}

private fun AuthResponseDto.toSessionToken(user: SessionUser): SessionToken {
    val now = System.currentTimeMillis() / 1000L
    return SessionToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochSeconds = now + expiresIn,
        user = user
    )
}
