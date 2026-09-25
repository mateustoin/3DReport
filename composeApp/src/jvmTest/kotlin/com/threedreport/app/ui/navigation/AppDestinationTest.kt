package com.threedreport.app.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDestinationTest {

    @Test
    fun shortcutsFollowTheSidebarOrderAndSkipAbout() {
        val expected = listOf(
            AppDestination.QUOTE, AppDestination.ORDERS, AppDestination.CATALOG, AppDestination.DASHBOARD,
            AppDestination.FILAMENTS, AppDestination.PRINTERS, AppDestination.SERVICES, AppDestination.SETTINGS,
        )
        assertEquals(expected, (1..8).map { destinationForShortcut(it) })
        assertNull(destinationForShortcut(9))
        assertNull(destinationForShortcut(0))
        assertNull(AppDestination.ABOUT.shortcutNumber)
    }

    @Test
    fun groupsAreContiguousSoTheSidebarReadsInShortcutOrder() {
        // A barra desenha grupo por grupo; se um item de Vendas aparecesse depois de Cadastros, o
        // atalho N deixaria de ser o N-ésimo item na tela.
        val groups = AppDestination.entries.map { it.group }
        assertEquals(groups.distinct(), groups.zipWithNext().filter { (a, b) -> a != b }.map { it.first } + groups.last())
    }
}
