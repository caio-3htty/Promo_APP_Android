package com.prumo.feature.obras

import com.prumo.core.model.ObraSummary
import com.prumo.core.repository.ObrasRepository
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
class ObrasViewModelTest {
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
        val vm = ObrasViewModel(object : ObrasRepository {
            override suspend fun listObras(includeDeleted: Boolean, deletedSinceIso: String?) =
                listOf(ObraSummary("1", "Obra A", "ativa", null, null))

            override suspend fun saveObra(obra: ObraSummary) = Unit
            override suspend fun softDeleteObra(obraId: String) = Unit
            override suspend fun restoreObra(obraId: String) = Unit
            override suspend fun hardDeleteObra(obraId: String) = Unit
        })
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value is ScreenState.Content)
    }
}
