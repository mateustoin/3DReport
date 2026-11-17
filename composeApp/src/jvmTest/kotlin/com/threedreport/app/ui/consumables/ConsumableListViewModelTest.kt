package com.threedreport.app.ui.consumables

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.consumables_error_cost_required
import _3dreport.composeapp.generated.resources.consumables_notice_archived
import _3dreport.composeapp.generated.resources.consumables_notice_deleted
import _3dreport.composeapp.generated.resources.consumables_notice_restored
import _3dreport.composeapp.generated.resources.consumables_notice_saved
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.pricing.PricingCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsumableListViewModelTest {

    // Unconfined: a lista acompanha o repositório e os avisos chegam na hora, sem esperar relógio nenhum.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModelOf(repository: ConsumableRepository) = ConsumableListViewModel(repository, scope)

    /** Os avisos emitidos daqui em diante (o fluxo não guarda os antigos). */
    private fun ConsumableListViewModel.collectNotices(): List<UiText> =
        mutableListOf<UiText>().also { received -> scope.launch { notices.collect { received += it } } }

    @Test
    fun savingANewConsumableIncludesUnitCostAndChargeModeAndClosesTheForm() {
        val repository = ConsumableRepository()
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Argola", unitCostText = "0,35", chargedPerOrder = false) }
        viewModel.save()

        val saved = repository.consumables.value.first { it.name == "Argola" }
        assertEquals(0.35, saved.unitCost)
        assertEquals(false, saved.chargedPerOrder)
        assertNull(viewModel.uiState.value.form)
        assertEquals(listOf(uiText(Res.string.consumables_notice_saved)), notices)
    }

    @Test
    fun theListFollowsTheRepositoryAndSplitsArchived() {
        val repository = ConsumableRepository()
        val viewModel = viewModelOf(repository)
        assertTrue(viewModel.uiState.value.isEmpty)

        repository.add(Consumable(id = "argola", name = "Argola", unitCost = 0.35))
        repository.add(Consumable(id = "caixa", name = "Caixa", unitCost = 2.0, archived = true))

        val state = viewModel.uiState.value
        assertEquals(listOf("argola"), state.active.map { it.id })
        assertEquals(listOf("caixa"), state.archived.map { it.id })
        assertTrue(!state.isEmpty)
    }

    @Test
    fun aBlankNameFailsOnTheNameFieldAndSavesNothing() {
        val repository = ConsumableRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "  ", unitCostText = "1") }
        viewModel.save()

        val form = viewModel.uiState.value.form!!
        assertEquals(uiText(Res.string.common_error_name_required), form.nameError)
        assertNull(form.unitCostError)
        assertTrue(repository.consumables.value.isEmpty())
    }

    @Test
    fun aNegativeUnitCostFailsOnTheCostFieldAndSavesNothing() {
        val repository = ConsumableRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Ímã", unitCostText = "-1") }
        viewModel.save()

        assertEquals(uiText(Res.string.common_error_negative), viewModel.uiState.value.form!!.unitCostError)
        assertTrue(repository.consumables.value.isEmpty())
    }

    @Test
    fun aMissingOrInvalidUnitCostHasItsOwnMessage() {
        val repository = ConsumableRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Ímã") }
        viewModel.save()
        assertEquals(uiText(Res.string.consumables_error_cost_required), viewModel.uiState.value.form!!.unitCostError)

        viewModel.updateForm { it.copy(unitCostText = "abc") }
        viewModel.save()
        assertEquals(uiText(Res.string.common_error_number_invalid), viewModel.uiState.value.form!!.unitCostError)
        assertTrue(repository.consumables.value.isEmpty())
    }

    @Test
    fun editingTheFormClearsTheErrors() {
        val viewModel = viewModelOf(ConsumableRepository())
        viewModel.startAdd()
        viewModel.save()
        assertNotNull(viewModel.uiState.value.form!!.nameError)

        viewModel.updateForm { it.copy(name = "Argola") }

        assertNull(viewModel.uiState.value.form!!.nameError)
        assertNull(viewModel.uiState.value.form!!.unitCostError)
    }

    @Test
    fun editingAConsumableRestoresItsFieldsAndSavingKeepsItArchived() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "caixa", name = "Caixa", unitCost = 2.0, chargedPerOrder = true, archived = true))
        val viewModel = viewModelOf(repository)

        viewModel.startEdit("caixa")

        val form = viewModel.uiState.value.form!!
        assertEquals("Caixa", form.name)
        assertEquals(true, form.chargedPerOrder)

        viewModel.updateForm { it.copy(unitCostText = "2,5") }
        viewModel.save()
        val saved = repository.consumables.value.single()
        assertEquals(2.5, saved.unitCost)
        assertTrue(saved.archived)
    }

    @Test
    fun cancelEditClosesTheForm() {
        val viewModel = viewModelOf(ConsumableRepository())
        viewModel.startAdd()

        viewModel.cancelEdit()

        assertNull(viewModel.uiState.value.form)
    }

    @Test
    fun usageCountsTheOrdersThatUseTheConsumable() {
        val filaments = FilamentRepository()
        val printers = PrinterRepository()
        val history = QuoteHistoryRepository()
        val consumables = ConsumableRepository()
        val argola = Consumable(id = "argola", name = "Argola", unitCost = 0.35)
        consumables.add(argola)

        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val printer = printers.printers.value.first()
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 60.0),
            printer = printer,
            settings = PricingSettings(energyPricePerKwh = 1.0, failureRate = 0.1, finishingRate = 0.1, profitMargin = 1.0),
        ).copy(consumables = listOf(QuotedConsumable.of(argola, quantity = 1.0)))
        history.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        val viewModel = ConsumableListViewModel(consumables, scope, history::quotesIncludingTrash)

        assertEquals(1, viewModel.usageCount("argola"))
        assertEquals(0, viewModel.usageCount("outro"))
    }

    @Test
    fun requestDeleteCarriesTheUsageAndWhetherItCanBeArchived() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "argola", name = "Argola", unitCost = 0.35))
        repository.add(Consumable(id = "caixa", name = "Caixa", unitCost = 2.0, archived = true))
        val viewModel = viewModelOf(repository)

        viewModel.requestDelete("argola")
        val pending = viewModel.uiState.value.pendingDelete!!
        assertEquals("Argola", pending.name)
        assertEquals(0, pending.usageCount)
        assertTrue(pending.canArchive)

        viewModel.requestDelete("caixa")
        assertEquals(false, viewModel.uiState.value.pendingDelete!!.canArchive)

        viewModel.dismissDelete()
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun archivingAndRestoringMoveTheItemAndWarn() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "argola", name = "Argola", unitCost = 0.35))
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()

        viewModel.setArchived("argola", true)
        assertEquals(listOf("argola"), viewModel.uiState.value.archived.map { it.id })
        assertTrue(viewModel.uiState.value.active.isEmpty())

        viewModel.setArchived("argola", false)
        assertEquals(listOf("argola"), viewModel.uiState.value.active.map { it.id })

        assertEquals(
            listOf(uiText(Res.string.consumables_notice_archived), uiText(Res.string.consumables_notice_restored)),
            notices,
        )
    }

    @Test
    fun deletingRemovesItClosesAnOpenFormForTheSameConsumableAndWarns() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "argola", name = "Argola", unitCost = 0.35))
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()
        viewModel.startEdit("argola")
        viewModel.requestDelete("argola")

        viewModel.delete("argola")

        assertNull(viewModel.uiState.value.form)
        assertNull(viewModel.uiState.value.pendingDelete)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(listOf(uiText(Res.string.consumables_notice_deleted)), notices)
    }
}
