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
    fun savingANewFilamentIncludesBrandAndOneDefaultColor() {
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
            )
        }
        viewModel.updateForm { form -> form.copy(colors = form.colors.map { it.copy(name = "Vermelho Fosco", hex = "#E53935") }) }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "PLA Vermelho" }
        assertEquals("Voolt", saved.brand)
        assertEquals(1, saved.colors.size)
        assertEquals("Vermelho Fosco", saved.colors.first().name)
        assertEquals("#E53935", saved.colors.first().hex)
        assertTrue(saved.hasStockAvailable)
    }

    @Test
    fun savingANewFilamentIncludesMaterialTypeWhenSet() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(
                name = "PETG Cinza",
                pricePerKgText = "120",
                densityGPerCm3Text = "1.27",
                materialType = "PETG",
            )
        }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "PETG Cinza" }
        assertEquals("PETG", saved.materialType)
    }

    @Test
    fun newFilamentDefaultsToOneColorInStock() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Nylon", pricePerKgText = "150", densityGPerCm3Text = "1.14") }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "Nylon" }
        assertEquals(1, saved.colors.size)
        assertTrue(saved.hasStockAvailable)
    }

    @Test
    fun addColorRowAddsAnotherColorToTheDraft() {
        val viewModel = FilamentListViewModel(FilamentRepository())

        viewModel.startAdd()
        assertEquals(1, viewModel.form.value!!.colors.size)

        viewModel.addColorRow()
        assertEquals(2, viewModel.form.value!!.colors.size)
    }

    @Test
    fun removeColorRowKeepsAtLeastOne() {
        val viewModel = FilamentListViewModel(FilamentRepository())

        viewModel.startAdd()
        val onlyColorId = viewModel.form.value!!.colors.first().id

        viewModel.removeColorRow(onlyColorId)

        assertEquals(1, viewModel.form.value!!.colors.size)
    }

    @Test
    fun savingMultipleColorsUnderOneFilamentAvoidsDuplicateCatalogEntries() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "PLA Voolt", pricePerKgText = "100", densityGPerCm3Text = "1.24", brand = "Voolt") }
        viewModel.addColorRow()
        val (firstId, secondId) = viewModel.form.value!!.colors.map { it.id }
        viewModel.updateColorRow(firstId) { it.copy(name = "Vermelho") }
        viewModel.updateColorRow(secondId) { it.copy(name = "Azul") }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "PLA Voolt" }
        assertEquals(setOf("Vermelho", "Azul"), saved.colors.map { it.name }.toSet())
    }

    @Test
    fun toggleColorInStockFlipsOnlyThatColor() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository)
        val filament = repository.filaments.value.first()
        val colorId = filament.colors.first().id
        assertTrue(filament.hasStockAvailable)

        viewModel.toggleColorInStock(filament.id, colorId)
        assertEquals(false, repository.filaments.value.first { it.id == filament.id }.colors.first { it.id == colorId }.inStock)

        viewModel.toggleColorInStock(filament.id, colorId)
        assertTrue(repository.filaments.value.first { it.id == filament.id }.colors.first { it.id == colorId }.inStock)
    }
}
