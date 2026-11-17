package com.threedreport.app.data

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Visualização lembrada de Pedidos e do Catálogo (decisões 129 e 132): nasce em kanban e em grade, sobrevive
 * a reabrir o app, e arquivo de preferências antigo ou com valor desconhecido abre no padrão sem perder o resto.
 */
class PreferencesRepositoryTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun aNewInstallOpensOrdersInKanbanAndCatalogInGrid() {
        val preferences = PreferencesRepository().preferences.value
        assertEquals(OrdersView.KANBAN, preferences.ordersView)
        assertEquals(CatalogView.GRID, preferences.catalogView)
    }

    @Test
    fun theChosenViewsSurviveReopening() {
        val repository = PreferencesRepository()
        repository.setOrdersView(OrdersView.LIST)
        repository.setCatalogView(CatalogView.LIST)
        assertEquals(OrdersView.LIST, repository.preferences.value.ordersView)

        val reopened = PreferencesRepository().preferences.value
        assertEquals(OrdersView.LIST, reopened.ordersView)
        assertEquals(CatalogView.LIST, reopened.catalogView)
    }

    @Test
    fun changingOneViewKeepsTheOtherPreferences() {
        val repository = PreferencesRepository()
        repository.update(repository.preferences.value.copy(autoBackupEnabled = false, checkForUpdates = true))
        repository.setOrdersView(OrdersView.LIST)
        repository.setOrdersView(OrdersView.KANBAN)

        val reopened = PreferencesRepository().preferences.value
        assertEquals(OrdersView.KANBAN, reopened.ordersView)
        assertFalse(reopened.autoBackupEnabled)
        assertTrue(reopened.checkForUpdates)
    }

    @Test
    fun aFileFromBeforeOpensWithTheDefaultViews() {
        File(dataDir, "preferences.json").writeText(
            """{"data":{"autoBackupEnabled":false,"backupDirectory":"/backups","lastBackupEpochMillis":5,"checkForUpdates":true},"updatedAtEpochMillis":1}""",
        )

        val preferences = PreferencesRepository().preferences.value
        assertEquals(OrdersView.KANBAN, preferences.ordersView)
        assertEquals(CatalogView.GRID, preferences.catalogView)
        assertFalse(preferences.autoBackupEnabled)
        assertEquals("/backups", preferences.backupDirectory)
        assertTrue(preferences.checkForUpdates)
    }

    @Test
    fun anUnknownViewFallsBackToTheDefaultWithoutLosingTheFile() {
        File(dataDir, "preferences.json").writeText(
            """{"data":{"autoBackupEnabled":false,"ordersView":"TIMELINE","catalogView":3},"updatedAtEpochMillis":1}""",
        )

        val preferences = PreferencesRepository().preferences.value
        assertEquals(OrdersView.KANBAN, preferences.ordersView)
        assertEquals(CatalogView.GRID, preferences.catalogView)
        // O arquivo abriu: o resto das preferências continua, e nada foi posto de lado como ilegível.
        assertFalse(preferences.autoBackupEnabled)
        assertEquals(emptyList(), dataDir.list()!!.filter { "ilegivel" in it })
    }

    @Test
    fun theViewsAreWrittenWithStableNames() {
        PreferencesRepository().apply {
            setOrdersView(OrdersView.LIST)
            setCatalogView(CatalogView.GRID)
        }
        val text = File(dataDir, "preferences.json").readText()
        assertTrue("\"ordersView\": \"LIST\"" in text, text)
        assertTrue("\"catalogView\": \"GRID\"" in text, text)
    }
}
