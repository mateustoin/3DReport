package com.threedreport.app.ui.navigation

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DestinationTextTest {

    @Test
    fun everyDestinationAndGroupHasItsNameInResources() = runBlocking {
        assertEquals(
            listOf("Orçamento", "Pedidos", "Catálogo", "Dashboard", "Filamentos", "Impressoras", "Serviços", "Insumos", "Configurações", "Sobre"),
            AppDestination.entries.map { getString(it.labelRes()) },
        )
        assertEquals("Vendas", getString(DestinationGroup.SALES.titleRes()!!))
        assertEquals("Cadastros", getString(DestinationGroup.REGISTRIES.titleRes()!!))
        assertNull(DestinationGroup.FOOTER.titleRes())
    }
}
