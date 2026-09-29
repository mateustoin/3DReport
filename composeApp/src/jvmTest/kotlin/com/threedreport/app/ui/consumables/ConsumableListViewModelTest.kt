package com.threedreport.app.ui.consumables

import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsumableListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingANewConsumableIncludesUnitCostAndChargeMode() {
        val repository = ConsumableRepository()
        val viewModel = ConsumableListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Argola", unitCostText = "0,35", chargedPerOrder = false) }
        viewModel.save()

        val saved = repository.consumables.value.first { it.name == "Argola" }
        assertEquals(0.35, saved.unitCost)
        assertEquals(false, saved.chargedPerOrder)
    }

    @Test
    fun aBlankNameFailsAndSavesNothing() {
        val repository = ConsumableRepository()
        val viewModel = ConsumableListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "  ", unitCostText = "1") }
        viewModel.save()

        assertNotNull(viewModel.form.value!!.errorMessage)
        assertTrue(repository.consumables.value.isEmpty())
    }

    @Test
    fun aNegativeUnitCostFailsAndSavesNothing() {
        val repository = ConsumableRepository()
        val viewModel = ConsumableListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Ímã", unitCostText = "-1") }
        viewModel.save()

        assertNotNull(viewModel.form.value!!.errorMessage)
        assertTrue(repository.consumables.value.isEmpty())
    }

    @Test
    fun editingAConsumableRestoresItsFields() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "caixa", name = "Caixa", unitCost = 2.0, chargedPerOrder = true))
        val viewModel = ConsumableListViewModel(repository)

        viewModel.startEdit(repository.consumables.value.single())

        val form = viewModel.form.value!!
        assertEquals("Caixa", form.name)
        assertEquals(true, form.chargedPerOrder)
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

        val viewModel = ConsumableListViewModel(consumables, history::quotesIncludingTrash)

        assertEquals(1, viewModel.usageCount("argola"))
        assertEquals(0, viewModel.usageCount("outro"))
    }

    @Test
    fun deletingClearsAnOpenFormForTheSameConsumable() {
        val repository = ConsumableRepository()
        repository.add(Consumable(id = "argola", name = "Argola", unitCost = 0.35))
        val viewModel = ConsumableListViewModel(repository)
        viewModel.startEdit(repository.consumables.value.single())

        viewModel.delete("argola")

        assertNull(viewModel.form.value)
    }
}
