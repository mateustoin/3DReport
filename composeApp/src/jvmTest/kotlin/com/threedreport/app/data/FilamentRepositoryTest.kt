package com.threedreport.app.data

import com.threedreport.core.model.Filament
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
