package com.threedreport.app.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDestinationTest {

    @Test
    fun shortcutsFollowTheSidebarOrderUpToStock() {
        val expected = listOf(
            AppDestination.QUOTE, AppDestination.ORDERS, AppDestination.CATALOG, AppDestination.DASHBOARD,
            AppDestination.FILAMENTS, AppDestination.PRINTERS, AppDestination.SERVICES, AppDestination.CONSUMABLES,
            AppDestination.STOCK,
        )
        assertEquals(expected, (1..9).map { destinationForShortcut(it) })
        assertNull(destinationForShortcut(10))
        assertNull(destinationForShortcut(0))
    }

    @Test
    fun settingsMovedToTheCommaShortcutAndAboutHasNone() {
        // Ctrl/Cmd+vírgula é o atalho comum de preferências (decisão 163): Configurações deixou de ter número.
        assertNull(AppDestination.SETTINGS.shortcutNumber)
        assertTrue(AppDestination.SETTINGS.isPreferences)
        assertNull(AppDestination.ABOUT.shortcutNumber)
        assertEquals(setOf(AppDestination.SETTINGS), AppDestination.entries.filter { it.isPreferences }.toSet())
    }

    @Test
    fun stockSitsInRegistriesRightAfterConsumables() {
        assertEquals(DestinationGroup.REGISTRIES, AppDestination.STOCK.group)
        assertEquals(AppDestination.CONSUMABLES.ordinal + 1, AppDestination.STOCK.ordinal)
    }

    @Test
    fun groupsAreContiguousSoTheSidebarReadsInShortcutOrder() {
        // A barra desenha grupo por grupo; se um item de Vendas aparecesse depois de Cadastros, o
        // atalho N deixaria de ser o N-ésimo item na tela.
        val groups = AppDestination.entries.map { it.group }
        assertEquals(groups.distinct(), groups.zipWithNext().filter { (a, b) -> a != b }.map { it.first } + groups.last())
    }
}
