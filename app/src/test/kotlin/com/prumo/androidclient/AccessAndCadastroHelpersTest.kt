package com.prumo.androidclient

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessAndCadastroHelpersTest {

    @Test
    fun `resolveAuditUserLabel returns mapped friendly name`() {
        val map = mapOf("u1" to "Maria Silva")

        val resolved = resolveAuditUserLabel("u1", map)

        assertEquals("Maria Silva", resolved)
    }

    @Test
    fun `resolveAuditUserLabel falls back to id when user is unknown`() {
        val resolved = resolveAuditUserLabel("missing-user", emptyMap())

        assertEquals("missing-user", resolved)
    }

    @Test
    fun `validateMaterialFornecedorSelection requires material first`() {
        val error = validateMaterialFornecedorSelection(
            materialId = "",
            fornecedorId = "forn-1",
            requiredMaterialMessage = "material required",
            requiredFornecedorMessage = "supplier required"
        )

        assertEquals("material required", error)
    }

    @Test
    fun `validateMaterialFornecedorSelection requires supplier when material exists`() {
        val error = validateMaterialFornecedorSelection(
            materialId = "mat-1",
            fornecedorId = "",
            requiredMaterialMessage = "material required",
            requiredFornecedorMessage = "supplier required"
        )

        assertEquals("supplier required", error)
    }
}
