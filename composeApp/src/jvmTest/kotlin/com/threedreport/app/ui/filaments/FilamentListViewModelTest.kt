package com.threedreport.app.ui.filaments

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.common_error_positive
import _3dreport.composeapp.generated.resources.common_error_required
import _3dreport.composeapp.generated.resources.filaments_notice_archived
import _3dreport.composeapp.generated.resources.filaments_notice_deleted
import _3dreport.composeapp.generated.resources.filaments_notice_restored
import _3dreport.composeapp.generated.resources.filaments_notice_saved
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Filament
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O estado único da tela de Filamentos: formulário, cores, validação por campo, arquivar e excluir. */
class FilamentListViewModelTest {

    // Unconfined: a lista acompanha o repositório e os avisos chegam na hora, sem esperar relógio nenhum.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun viewModelOf(repository: FilamentRepository) = FilamentListViewModel(repository, scope)

    /** Os avisos emitidos daqui em diante (o fluxo não guarda os antigos). */
    private fun FilamentListViewModel.collectNotices(): List<UiText> =
        mutableListOf<UiText>().also { received -> scope.launch { notices.collect { received += it } } }

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingANewFilamentIncludesBrandAndOneDefaultColor() {
        val repository = FilamentRepository()
        val viewModel = viewModelOf(repository)

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
        val viewModel = viewModelOf(repository)

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
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Nylon", pricePerKgText = "150", densityGPerCm3Text = "1.14") }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "Nylon" }
        assertEquals(1, saved.colors.size)
        assertTrue(saved.hasStockAvailable)
    }

    @Test
    fun addColorRowAddsAnotherColorToTheDraft() {
        val viewModel = viewModelOf(FilamentRepository())

        viewModel.startAdd()
        assertEquals(1, viewModel.uiState.value.form!!.colors.size)

        viewModel.addColorRow()
        assertEquals(2, viewModel.uiState.value.form!!.colors.size)
    }

    @Test
    fun removeColorRowKeepsAtLeastOne() {
        val viewModel = viewModelOf(FilamentRepository())

        viewModel.startAdd()
        val onlyColorId = viewModel.uiState.value.form!!.colors.first().id

        viewModel.removeColorRow(onlyColorId)

        assertEquals(1, viewModel.uiState.value.form!!.colors.size)
    }

    @Test
    fun savingMultipleColorsUnderOneFilamentAvoidsDuplicateCatalogEntries() {
        val repository = FilamentRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "PLA Voolt", pricePerKgText = "100", densityGPerCm3Text = "1.24", brand = "Voolt") }
        viewModel.addColorRow()
        val (firstId, secondId) = viewModel.uiState.value.form!!.colors.map { it.id }
        viewModel.updateColorRow(firstId) { it.copy(name = "Vermelho") }
        viewModel.updateColorRow(secondId) { it.copy(name = "Azul") }
        viewModel.save()

        val saved = repository.filaments.value.first { it.name == "PLA Voolt" }
        assertEquals(setOf("Vermelho", "Azul"), saved.colors.map { it.name }.toSet())
    }

    @Test
    fun toggleColorInStockFlipsOnlyThatColor() {
        val repository = FilamentRepository()
        val viewModel = viewModelOf(repository)
        val filament = repository.filaments.value.first()
        val colorId = filament.colors.first().id
        assertTrue(filament.hasStockAvailable)

        viewModel.toggleColorInStock(filament.id, colorId)
        assertEquals(false, repository.filaments.value.first { it.id == filament.id }.colors.first { it.id == colorId }.inStock)

        viewModel.toggleColorInStock(filament.id, colorId)
        assertTrue(repository.filaments.value.first { it.id == filament.id }.colors.first { it.id == colorId }.inStock)
    }

    @Test
    fun savingClosesTheFormAndWarns() {
        val repository = FilamentRepository()
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "PLA", pricePerKgText = "100", densityGPerCm3Text = "1,24") }
        viewModel.save()

        assertNull(viewModel.uiState.value.form)
        assertEquals(listOf(uiText(Res.string.filaments_notice_saved)), notices)
    }

    @Test
    fun invalidFieldsFailEachOnTheirOwnFieldAndSaveNothing() {
        val repository = FilamentRepository()
        val before = repository.filaments.value.size
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(pricePerKgText = "-5", densityGPerCm3Text = "abc", diameterMmText = "0") }
        viewModel.save()

        val form = viewModel.uiState.value.form!!
        assertEquals(uiText(Res.string.common_error_name_required), form.nameError)
        assertEquals(uiText(Res.string.common_error_negative), form.priceError)
        assertEquals(uiText(Res.string.common_error_number_invalid), form.densityError)
        assertEquals(uiText(Res.string.common_error_positive), form.diameterError)
        assertEquals(before, repository.filaments.value.size)

        viewModel.updateForm { it.copy(pricePerKgText = "") }
        viewModel.save()
        assertEquals(uiText(Res.string.common_error_required), viewModel.uiState.value.form!!.priceError)
    }

    @Test
    fun editingAFieldClearsItsError() {
        val viewModel = viewModelOf(FilamentRepository())
        viewModel.startAdd()
        viewModel.save()
        assertTrue(viewModel.uiState.value.form!!.nameError != null)

        viewModel.updateForm { it.copy(name = "PLA") }

        assertNull(viewModel.uiState.value.form!!.nameError)
    }

    @Test
    fun choosingAMaterialTypeFillsTheDensityAndCustomLeavesItAlone() {
        val viewModel = viewModelOf(FilamentRepository())
        viewModel.startAdd()

        viewModel.chooseMaterialType(FILAMENT_MATERIAL_TYPE_PRESETS.first { it.label == "PETG" })
        var form = viewModel.uiState.value.form!!
        assertEquals("PETG", form.materialType)
        assertEquals("1,27", form.densityGPerCm3Text)
        assertEquals(MaterialOption.Preset(FILAMENT_MATERIAL_TYPE_PRESETS.first { it.label == "PETG" }), form.materialOption)

        // Compósito sem densidade padrão: o campo fica como estava e a tela pede pra conferir a ficha do rolo.
        viewModel.chooseMaterialType(FILAMENT_MATERIAL_TYPE_PRESETS.first { it.label == "PLA-CF" })
        form = viewModel.uiState.value.form!!
        assertEquals("1,27", form.densityGPerCm3Text)
        assertTrue(form.densityNeedsCheck)

        viewModel.chooseMaterialType(null)
        form = viewModel.uiState.value.form!!
        assertTrue(form.isCustomMaterialType)
        assertEquals("", form.materialType)
        assertEquals(MaterialOption.Custom, form.materialOption)
    }

    @Test
    fun aColorPresetSetsTheHexAndNamesOnlyAnUnnamedColor() {
        val viewModel = viewModelOf(FilamentRepository())
        viewModel.startAdd()
        val colorId = viewModel.uiState.value.form!!.colors.single().id

        viewModel.applyColorPreset(colorId, "Vermelho", "#E53935")
        assertEquals("Vermelho", viewModel.uiState.value.form!!.colors.single().name)
        assertEquals("#E53935", viewModel.uiState.value.form!!.colors.single().hex)

        viewModel.applyColorPreset(colorId, "Azul", "#1E88E5")
        assertEquals("Vermelho", viewModel.uiState.value.form!!.colors.single().name)
        assertEquals("#1E88E5", viewModel.uiState.value.form!!.colors.single().hex)
    }

    @Test
    fun brandSuggestionsFollowWhatIsTyped() {
        val viewModel = viewModelOf(FilamentRepository())
        viewModel.startAdd()
        assertTrue(viewModel.uiState.value.form!!.brandSuggestions.size > 10)

        viewModel.updateForm { it.copy(brand = "poly") }
        assertEquals(listOf("Polymaker"), viewModel.uiState.value.form!!.brandSuggestions)

        viewModel.updateForm { it.copy(brand = "Polymaker") }
        assertEquals(emptyList(), viewModel.uiState.value.form!!.brandSuggestions)
    }

    @Test
    fun theListFollowsTheRepositoryAndSplitsArchived() {
        val repository = FilamentRepository()
        repository.filaments.value.forEach { repository.delete(it.id) }
        val viewModel = viewModelOf(repository)
        assertTrue(viewModel.uiState.value.isEmpty)

        repository.add(Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24))
        repository.add(Filament(id = "abs", name = "ABS", pricePerKg = 100.0, densityGPerCm3 = 1.04, archived = true))

        assertEquals(listOf("pla"), viewModel.uiState.value.active.map { it.id })
        assertEquals(listOf("abs"), viewModel.uiState.value.archived.map { it.id })
    }

    @Test
    fun requestDeleteCarriesTheUsageAndWhetherItCanBeArchived() {
        val repository = FilamentRepository()
        repository.add(Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24))
        repository.add(Filament(id = "abs", name = "ABS", pricePerKg = 100.0, densityGPerCm3 = 1.04, archived = true))
        val viewModel = viewModelOf(repository)

        viewModel.requestDelete("pla")
        assertTrue(viewModel.uiState.value.pendingDelete!!.canArchive)

        viewModel.requestDelete("abs")
        assertEquals(false, viewModel.uiState.value.pendingDelete!!.canArchive)

        viewModel.dismissDelete()
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun archivingRestoringAndDeletingWarn() {
        val repository = FilamentRepository()
        repository.add(Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24))
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()
        viewModel.startEdit("pla")

        viewModel.setArchived("pla", true)
        viewModel.setArchived("pla", false)
        viewModel.delete("pla")

        assertNull(viewModel.uiState.value.form)
        assertEquals(
            listOf(
                uiText(Res.string.filaments_notice_archived),
                uiText(Res.string.filaments_notice_restored),
                uiText(Res.string.filaments_notice_deleted),
            ),
            notices,
        )
    }
}
