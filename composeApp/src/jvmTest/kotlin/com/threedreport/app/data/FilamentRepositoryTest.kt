package com.threedreport.app.data

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que o catálogo de filamentos sobrevive a uma nova instância do repositório (persistência em disco). */
class FilamentRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsWithDefaultCatalog() {
        val filaments = FilamentRepository().filaments.value
        assertTrue(filaments.isNotEmpty())
    }

    @Test
    fun addedFilamentSurvivesNewRepositoryInstance() {
        val original = FilamentRepository()
        val newFilament = Filament(id = "custom", name = "Nylon", pricePerKg = 150.0, densityGPerCm3 = 1.14)
        original.add(newFilament)

        val reloaded = FilamentRepository().filaments.value
        assertTrue(reloaded.any { it.id == "custom" && it.name == "Nylon" })
    }

    @Test
    fun defaultFilamentsStartInStock() {
        assertTrue(FilamentRepository().filaments.value.all { it.hasStockAvailable })
    }

    @Test
    fun brandAndColorsSurviveNewRepositoryInstance() {
        val original = FilamentRepository()
        val newFilament = Filament(
            id = "custom-color",
            name = "PLA Vermelho",
            pricePerKg = 100.0,
            densityGPerCm3 = 1.24,
            brand = "Voolt",
            colors = listOf(
                FilamentColor(id = "red", name = "Vermelho Fosco", hex = "#E53935"),
                FilamentColor(id = "blue", name = "Azul Fosco", hex = "#1E88E5"),
            ),
        )
        original.add(newFilament)

        val reloaded = FilamentRepository().filaments.value.first { it.id == "custom-color" }
        assertEquals("Voolt", reloaded.brand)
        assertEquals(2, reloaded.colors.size)
        assertEquals("Vermelho Fosco", reloaded.colors.first { it.id == "red" }.name)
        assertEquals("#1E88E5", reloaded.colors.first { it.id == "blue" }.hex)
    }

    @Test
    fun perColorInStockFlagPersistsChange() {
        val repository = FilamentRepository()
        val filament = repository.filaments.value.first()
        val colorId = filament.colors.first().id

        repository.update(filament.copy(colors = filament.colors.map { it.copy(inStock = false) }))

        val reloaded = FilamentRepository().filaments.value.first { it.id == filament.id }
        assertEquals(false, reloaded.colors.first { it.id == colorId }.inStock)
        assertEquals(false, reloaded.hasStockAvailable)
    }

    @Test
    fun updatePersistsChange() {
        val repository = FilamentRepository()
        val filament = repository.filaments.value.first()

        repository.update(filament.copy(pricePerKg = 999.0))

        val reloaded = FilamentRepository().filaments.value.first { it.id == filament.id }
        assertEquals(999.0, reloaded.pricePerKg)
    }

    @Test
    fun deleteRemovesFromDisk() {
        val repository = FilamentRepository()
        val filament = repository.filaments.value.first()

        repository.delete(filament.id)

        val reloaded = FilamentRepository().filaments.value
        assertTrue(reloaded.none { it.id == filament.id })
    }
}
