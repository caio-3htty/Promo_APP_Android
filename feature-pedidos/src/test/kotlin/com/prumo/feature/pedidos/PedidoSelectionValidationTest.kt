package com.prumo.feature.pedidos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PedidoSelectionValidationTest {

    @Test
    fun `returns material error when material is missing`() {
        val result = validatePedidoSelections(
            materialId = "",
            fornecedorId = "forn-1",
            materialError = "material required",
            fornecedorError = "supplier required"
        )

        assertEquals("material required", result)
    }

    @Test
    fun `returns supplier error when supplier is missing`() {
        val result = validatePedidoSelections(
            materialId = "mat-1",
            fornecedorId = "",
            materialError = "material required",
            fornecedorError = "supplier required"
        )

        assertEquals("supplier required", result)
    }

    @Test
    fun `returns null when both assisted selections exist`() {
        val result = validatePedidoSelections(
            materialId = "mat-1",
            fornecedorId = "forn-1",
            materialError = "material required",
            fornecedorError = "supplier required"
        )

        assertNull(result)
    }
}
