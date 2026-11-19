package com.threedreport.app.ui

import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.consumables.ConsumableListViewModel
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Cadastros e Orçamento leem o dinheiro com o separador decimal da moeda escolhida (Leva 10B, parte 7), como
 * Configurações, canais e primeiro uso: "1,250" é mil duzentos e cinquenta em dólar e um e vinte e cinco em real.
 * O que volta pro campo sai com o mesmo separador, pra ser lido de volta igual. Peso, densidade, horas e percentual
 * seguem o idioma do app e não a moeda (ver NumberLanguageTest).
 */
class DecimalSeparatorTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val usd = { Currency.USD }
    private val brl = { Currency.BRL }

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    // ---- Impressoras

    private fun printerViewModel(repository: PrinterRepository, currency: () -> Currency) =
        PrinterListViewModel(repository, QuoteHistoryRepository(), MaintenanceRepository(), scope, currency)

    private fun savePrinter(name: String, currency: () -> Currency, price: String, hours: String, maintenance: String): PrinterProfile {
        val repository = PrinterRepository()
        val viewModel = printerViewModel(repository, currency)
        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(name = name, printerPowerWattsText = "350", machinePriceText = price, printingHoursPerDayText = hours, maintenanceCostPerHourText = maintenance)
        }
        viewModel.save()
        assertNull(viewModel.uiState.value.form, "salvou")
        return repository.printers.value.single { it.name == name }
    }

    @Test
    fun printerNumbersFollowTheCurrency() {
        val inDollars = savePrinter("Em dólar", usd, price = "2,700", hours = "16.5", maintenance = "0.25")
        assertEquals(2700.0, inDollars.machineInvestment.machinePrice)
        assertEquals(16.5, inDollars.machineInvestment.printingHoursPerDay)
        assertEquals(0.25, inDollars.maintenanceCostPerHour)

        val inReais = savePrinter("Em real", brl, price = "2.700", hours = "16,5", maintenance = "0,25")
        assertEquals(2700.0, inReais.machineInvestment.machinePrice)
        assertEquals(16.5, inReais.machineInvestment.printingHoursPerDay)
        assertEquals(0.25, inReais.maintenanceCostPerHour)
    }

    @Test
    fun aPrinterOpensWithTheCurrencySeparatorAndThePreviewReadsIt() {
        val repository = PrinterRepository()
        repository.add(PrinterProfile("p", "Ender", 200.0, 0.17, MachineInvestment(1500.5, 12, 25, 7.5)))

        val inDollars = printerViewModel(repository, usd).apply { startEdit("p") }.uiState.value.form!!
        assertEquals("1500.5", inDollars.machinePriceText)
        assertEquals("0.17", inDollars.maintenanceCostPerHourText)
        assertEquals("7,5", inDollars.printingHoursPerDayText)
        val expected = MachineInvestment(1500.5, 12, 25, 7.5).costPerHour + 0.17
        assertEquals(expected, inDollars.machineCostPerHour!!, 1e-9)
        assertEquals("0.17", printerViewModel(repository, usd).apply { startAdd() }.uiState.value.form!!.maintenanceCostPerHourText)

        val inReais = printerViewModel(repository, brl).apply { startEdit("p") }.uiState.value.form!!
        assertEquals("1500,5", inReais.machinePriceText)
        assertEquals("0,17", inReais.maintenanceCostPerHourText)
        assertEquals(expected, inReais.machineCostPerHour!!, 1e-9)
    }

    // ---- Filamentos

    @Test
    fun filamentNumbersFollowTheCurrency() {
        val repository = FilamentRepository()
        val viewModel = FilamentListViewModel(repository, scope, currency = usd)
        viewModel.startAdd()
        assertEquals("1,75", viewModel.uiState.value.form!!.diameterMmText)
        viewModel.updateForm { it.copy(name = "PETG X", pricePerKgText = "1,250", densityGPerCm3Text = "1.27") }
        viewModel.save()

        val saved = repository.filaments.value.single { it.name == "PETG X" }
        assertEquals(1250.0, saved.pricePerKg)
        assertEquals(1.27, saved.densityGPerCm3)
        assertEquals(1.75, saved.diameterMm)

        viewModel.startEdit(saved.id)
        assertEquals("1,27", viewModel.uiState.value.form!!.densityGPerCm3Text)
    }

    @Test
    fun filamentInReaisReadsAsBefore() {
        val repository = FilamentRepository()
        repository.add(Filament(id = "f", name = "PLA Y", pricePerKg = 99.9, densityGPerCm3 = 1.24))
        val viewModel = FilamentListViewModel(repository, scope, currency = brl)

        viewModel.startEdit("f")
        val form = viewModel.uiState.value.form!!
        assertEquals("99,9", form.pricePerKgText)
        assertEquals("1,75", form.diameterMmText)
        viewModel.updateForm { it.copy(pricePerKgText = "1.250") }
        viewModel.save()

        assertEquals(1250.0, repository.filaments.value.single { it.id == "f" }.pricePerKg)
    }

    // ---- Serviços e insumos

    @Test
    fun serviceAndConsumableValuesFollowTheCurrency() {
        val services = ServiceRepository()
        val serviceViewModel = ServiceListViewModel(services, scope, currency = usd)
        serviceViewModel.startAdd()
        serviceViewModel.updateForm { it.copy(name = "Pintura X", priceText = "1,250") }
        serviceViewModel.save()
        val service = services.services.value.single { it.name == "Pintura X" }
        assertEquals(1250.0, service.suggestedPrice)

        services.update(service.copy(suggestedPrice = 12.5))
        serviceViewModel.startEdit(service.id)
        assertEquals("12.5", serviceViewModel.uiState.value.form!!.priceText)

        val consumables = ConsumableRepository()
        val consumableViewModel = ConsumableListViewModel(consumables, scope, currency = usd)
        consumableViewModel.startAdd()
        consumableViewModel.updateForm { it.copy(name = "Argola X", unitCostText = "1,250") }
        consumableViewModel.save()
        val consumable = consumables.consumables.value.single { it.name == "Argola X" }
        assertEquals(1250.0, consumable.unitCost)

        consumables.update(consumable.copy(unitCost = 0.35))
        consumableViewModel.startEdit(consumable.id)
        assertEquals("0.35", consumableViewModel.uiState.value.form!!.unitCostText)
    }

    @Test
    fun serviceAndConsumableInReaisReadAsBefore() {
        val services = ServiceRepository()
        services.add(Service(id = "s", name = "Pintura Y", suggestedPrice = 12.5))
        val serviceViewModel = ServiceListViewModel(services, scope, currency = brl)
        serviceViewModel.startEdit("s")
        assertEquals("12,5", serviceViewModel.uiState.value.form!!.priceText)
        serviceViewModel.updateForm { it.copy(priceText = "1,250") }
        serviceViewModel.save()
        assertEquals(1.25, services.services.value.single { it.id == "s" }.suggestedPrice)

        val consumables = ConsumableRepository()
        consumables.add(Consumable(id = "c", name = "Argola Y", unitCost = 0.35))
        val consumableViewModel = ConsumableListViewModel(consumables, scope, currency = brl)
        consumableViewModel.startEdit("c")
        assertEquals("0,35", consumableViewModel.uiState.value.form!!.unitCostText)
    }

    // ---- Orçamento

    private fun quoteViewModel(history: QuoteHistoryRepository, currency: () -> Currency) = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), history,
        currency = currency,
    ).apply {
        setLengthMeters("12.5".replace('.', currency().decimalSeparator))
        setPrintTimeMinutes("190")
    }

    @Test
    fun theQuoteReadsShippingAndLengthWithTheCurrencyAndReopensTheSameWay() {
        val history = QuoteHistoryRepository()
        val viewModel = quoteViewModel(history, usd)
        viewModel.setShippingCost("1,200")

        val result = viewModel.uiState.value.result
        assertEquals(1200.0, result.shippingCost)
        assertEquals(12.5, result.quote!!.prints.single().job.filaments.single().lengthMeters)

        viewModel.setSaveName("Peça em dólar")
        viewModel.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()
        assertEquals(1200.0, saved.shippingCost)

        val editing = quoteViewModel(QuoteHistoryRepository(), usd)
        editing.loadForEditing(saved)
        assertEquals("1200", editing.uiState.value.input.shippingCostText)
        assertEquals("12,5", editing.uiState.value.input.prints.single().filaments.single().lengthText)
        assertFalse(editing.hasUnsavedEdits, "reabrir não cria alteração não salva")
        assertEquals(saved.quote.salePrice, editing.uiState.value.result.quote!!.salePrice, 1e-9)
    }

    @Test
    fun theQuoteInReaisReadsAsBefore() {
        val history = QuoteHistoryRepository()
        val viewModel = quoteViewModel(history, brl)
        viewModel.setShippingCost("1,200")

        assertEquals(1.2, viewModel.uiState.value.result.shippingCost, 1e-9)
        assertEquals(12.5, viewModel.uiState.value.result.quote!!.prints.single().job.filaments.single().lengthMeters)

        viewModel.setSaveName("Peça em real")
        viewModel.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()
        val editing = quoteViewModel(QuoteHistoryRepository(), brl)
        editing.loadForEditing(saved)
        assertEquals("1,2", editing.uiState.value.input.shippingCostText)
        assertEquals("12,5", editing.uiState.value.input.prints.single().filaments.single().lengthText)
        assertFalse(editing.hasUnsavedEdits)
    }
}
