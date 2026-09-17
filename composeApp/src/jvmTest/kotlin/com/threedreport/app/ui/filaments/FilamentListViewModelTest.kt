package com.threedreport.app.ui.filaments

import com.threedreport.app.data.FilamentRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilamentListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingANewFilamentIncludesBrandAndColor() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(
                name = "PLA Vermelho",
                pricePerKgText = "100",
                densityGPerCm3Text = "1.24",
                diameterMmText = "1.75",
                brand = "Voolt",
                colorName = "Vermelho Fosco",
                colorHex = "#E53935",
            )
        }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "PLA Vermelho" }
        assertEquals("Voolt", saved.brand)
        assertEquals("Vermelho Fosco", saved.colorName)
        assertEquals("#E53935", saved.colorHex)
        assertTrue(saved.inStock)
    }

    @Test
    fun newFilamentDefaultsToInStock() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Nylon", pricePerKgText = "150", densityGPerCm3Text = "1.14") }
        viewModel.save()

        assertTrue(repository.filaments.value.first { it.name == "Nylon" }.inStock)
    }

    @Test
    fun toggleInStockFlipsTheFlag() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)
        val filament = repository.filaments.value.first()
        assertTrue(filament.inStock)

        viewModel.toggleInStock(filament.id)
        assertEquals(false, repository.filaments.value.first { it.id == filament.id }.inStock)

        viewModel.toggleInStock(filament.id)
        assertTrue(repository.filaments.value.first { it.id == filament.id }.inStock)
    }
}
