package com.prumo.feature.estoque

import com.prumo.core.model.EstoqueItem
import com.prumo.core.model.EstoqueUpdateInput
import com.prumo.core.repository.EstoqueRepository
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
class EstoqueViewModelTest {
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
        val vm = EstoqueViewModel(FakeEstoqueRepository())
        vm.load("obra-1")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.state is ScreenState.Content)
    }
}

private class FakeEstoqueRepository : EstoqueRepository {
    override suspend fun listEstoque(obraId: String): List<EstoqueItem> {
        return listOf(
            EstoqueItem(
                id = "e1",
                obraId = obraId,
                materialId = "m1",
                materialNome = "Bloco",
                unidade = "un",
                estoqueAtual = 5.0,
                atualizadoEm = "2026-03-14T00:00:00Z"
            )
        )
    }

    override suspend fun updateEstoque(itemId: String, input: EstoqueUpdateInput) = Unit
}
