package com.prumo.feature.pedidos

import com.prumo.core.model.FornecedorSummary
import com.prumo.core.model.MaterialSummary
import com.prumo.core.model.PedidoInput
import com.prumo.core.model.PedidoResumo
import com.prumo.core.repository.PedidosRepository
import com.prumo.core.state.ScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PedidosViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load success emits content`() = runTest {
        val vm = PedidosViewModel(FakePedidosRepository())
        vm.load("obra-1")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.screenState is ScreenState.Content)
    }
}

private class FakePedidosRepository : PedidosRepository {
    override suspend fun listPedidos(obraId: String?, status: String?, search: String?): List<PedidoResumo> {
        return listOf(
            PedidoResumo(
                id = "p1",
                obraId = obraId ?: "obra-1",
                obraNome = "Obra A",
                materialId = "m1",
                materialNome = "Bloco",
                materialUnidade = "un",
                fornecedorId = "f1",
                fornecedorNome = "Fornecedor A",
                quantidade = 10.0,
                precoUnit = 8.0,
                total = 80.0,
                status = "pendente",
                codigoCompra = null,
                criadoEm = "2026-03-14T00:00:00Z",
                dataRecebimento = null,
                deletedAt = null
            )
        )
    }

    override suspend fun listMateriais(): List<MaterialSummary> = listOf(MaterialSummary("m1", "Bloco", "un"))

    override suspend fun listFornecedores(): List<FornecedorSummary> = listOf(FornecedorSummary("f1", "Fornecedor A"))

    override suspend fun listObras() = emptyList<com.prumo.core.model.ObraSummary>()

    override suspend fun createPedido(input: PedidoInput) = Unit

    override suspend fun updatePedido(id: String, input: PedidoInput) = Unit

    override suspend fun updatePedidoStatus(
        id: String,
        status: String,
        codigoCompra: String?,
        dataRecebimentoIso: String?,
        recebidoPor: String?
    ) = Unit

    override suspend fun softDeletePedido(id: String) = Unit
}
