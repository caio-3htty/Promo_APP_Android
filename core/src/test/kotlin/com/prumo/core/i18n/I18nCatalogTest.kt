package com.prumo.core.i18n

import com.prumo.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class I18nCatalogTest {

    @Test
    fun `falls back to pt BR when key is missing in selected language`() {
        val catalog = I18nCatalog(AppLanguage.EN)

        val value = catalog.t("i18n.test_pt_only")

        assertEquals("Somente PT", value)
    }

    @Test
    fun `interpolates placeholders in messages`() {
        val catalog = I18nCatalog(AppLanguage.PT_BR)

        val value = catalog.t("home.role", "value" to "gestor")

        assertEquals("Perfil: gestor", value)
    }
}
