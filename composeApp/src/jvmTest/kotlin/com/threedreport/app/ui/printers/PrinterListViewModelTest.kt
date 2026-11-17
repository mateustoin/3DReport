package com.threedreport.app.ui.printers

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_whole_number
import _3dreport.composeapp.generated.resources.printers_error_whole_max
import _3dreport.composeapp.generated.resources.printers_notice_archived
import _3dreport.composeapp.generated.resources.printers_notice_deleted
import _3dreport.composeapp.generated.resources.printers_notice_restored
import _3dreport.composeapp.generated.resources.printers_notice_saved
import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.report.MaintenanceState
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

/** O estado único da tela de Impressoras: formulário, fila, manutenção, validação por campo, arquivar e excluir. */
class PrinterListViewModelTest {

    // Unconfined: a lista acompanha os repositórios e os avisos chegam na hora, sem esperar relógio nenhum.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun viewModelOf(printers: PrinterRepository, history: QuoteHistoryRepository = QuoteHistoryRepository(), maintenance: MaintenanceRepository = MaintenanceRepository()) =
        PrinterListViewModel(printers, history, maintenance, scope)

    /** Os avisos emitidos daqui em diante (o fluxo não guarda os antigos). */
    private fun PrinterListViewModel.collectNotices(): List<UiText> =
        mutableListOf<UiText>().also { received -> scope.launch { notices.collect { received += it } } }

    private val ender = PrinterProfile(
        id = "printer-1",
        name = "Ender 3",
        printerPowerWatts = 200.0,
        maintenanceCostPerHour = 0.1,
        machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
    )

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
    fun startAddFromPresetPrefillsNameAndPower() {
        val viewModel = viewModelOf(PrinterRepository())
        val preset = PRINTER_PRESETS.first()

        viewModel.startAddFromPreset(preset)

        val form = viewModel.uiState.value.form!!
        assertEquals("${preset.brand} ${preset.model}", form.name)
        assertEquals(preset.ratedPowerWatts.toInputText(), form.printerPowerWattsText)
        assertTrue(form.powerFromPreset, "o campo avisa que é a potência máxima")
    }

    @Test
    fun printQueueReflectsQuotesCurrentlyPrintingOnEachPrinter() {
        val printerRepository = PrinterRepository()
        val historyRepository = QuoteHistoryRepository()
        val printer = PrinterProfile(
            id = "printer-1",
            name = "Ender 3",
            printerPowerWatts = 200.0,
            maintenanceCostPerHour = 0.1,
            machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
        )
        printerRepository.add(printer)

        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 120.0),
            printer = printer,
            settings = com.threedreport.core.model.PricingSettings(
                energyPricePerKwh = 1.0,
                failureRate = 0.1,
                finishingRate = 0.1,
                profitMargin = 1.0,
            ),
        )
        val saved = historyRepository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        historyRepository.updateStatus(saved.id, OrderStatus.EM_IMPRESSAO)

        val viewModel = viewModelOf(printerRepository, historyRepository)
        val row = viewModel.uiState.value.active.single { it.printer.id == "printer-1" }

        assertEquals(120.0, row.queuedMinutes)
        assertEquals(1, row.queuedQuoteCount)
    }

    private fun printerWithFinishedOrder(printMinutes: Double): Triple<PrinterRepository, QuoteHistoryRepository, PrinterProfile> {
        val printerRepository = PrinterRepository()
        val historyRepository = QuoteHistoryRepository()
        val printer = PrinterProfile(
            id = "printer-1",
            name = "Ender 3",
            printerPowerWatts = 200.0,
            maintenanceCostPerHour = 0.1,
            machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
        )
        printerRepository.add(printer)
        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = printMinutes),
            printer = printer,
            settings = com.threedreport.core.model.PricingSettings(energyPricePerKwh = 1.0, failureRate = 0.1, finishingRate = 0.1, profitMargin = 1.0),
        )
        val saved = historyRepository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        historyRepository.updateStatus(saved.id, OrderStatus.PRONTO)
        return Triple(printerRepository, historyRepository, printer)
    }

    @Test
    fun newComponentStartsCountingFromHoursAlreadyRunAndServiceRestartsIt() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 360.0)
        val viewModel = viewModelOf(printers, history)

        // Bico trocado há 52 h, a cada 50 h: o contador começa em 52, apesar de o app só conhecer 6 h.
        assertNull(viewModel.addComponent(printer.id, "Bico", "50", "52"))
        viewModel.openMaintenance(printer.id)
        val before = viewModel.uiState.value.maintenance!!.statuses.single()
        assertEquals(MaintenanceState.OVERDUE, before.state)
        assertEquals(52.0, before.hoursSinceService, 1e-9)

        viewModel.markServiced(before.component)

        val after = viewModel.uiState.value.maintenance!!.statuses.single()
        assertEquals(0.0, after.hoursSinceService, 1e-9)
        assertEquals("Bico", viewModel.uiState.value.maintenance!!.data.log.single().description)
        assertEquals(MaintenanceState.OK, viewModel.uiState.value.active.single { it.printer.id == printer.id }.maintenance!!.state)
    }

    @Test
    fun theMaintenanceDialogAndTheLogUseTheInjectedDay() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val maintenance = MaintenanceRepository()
        val viewModel = PrinterListViewModel(printers, history, maintenance, scope, today = { 19_000L })
        assertNull(viewModel.addComponent(printer.id, "Bico", "50", "0"))
        viewModel.openMaintenance(printer.id)
        val dialog = viewModel.uiState.value.maintenance!!
        assertEquals(19_000L, dialog.today)

        viewModel.markServiced(dialog.statuses.single().component)

        assertEquals(19_000L, viewModel.uiState.value.maintenance!!.data.log.single().dateEpochDay)
    }

    @Test
    fun manualUsageAddsToPrinterHours() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val viewModel = viewModelOf(printers, history)

        assertNull(viewModel.addManualUsage(printer.id, "2,5", "Calibração"))

        viewModel.openMaintenance(printer.id)
        assertEquals(3.5, viewModel.uiState.value.maintenance!!.hours, 1e-9)
    }

    @Test
    fun invalidMaintenanceInputReturnsAMessageAndSavesNothing() {
        val viewModel = viewModelOf(PrinterRepository())

        assertNotNull(viewModel.addComponent("p", "Bico", "0", ""))
        assertNotNull(viewModel.addComponent("p", " ", "50", ""))
        assertNotNull(viewModel.addManualUsage("p", "abc", "Teste"))
        assertNotNull(viewModel.logService("p", 20_000, " ", null))

        val maintenance = MaintenanceRepository().maintenance.value
        assertTrue(maintenance.components.isEmpty() && maintenance.manualUsage.isEmpty() && maintenance.log.isEmpty())
    }

    @Test
    fun deletingPrinterDeletesItsMaintenance() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val viewModel = viewModelOf(printers, history)
        viewModel.addComponent(printer.id, "Bico", "50", "")
        viewModel.addManualUsage(printer.id, "1", "Teste")

        viewModel.delete(printer.id)

        val reloaded = MaintenanceRepository().maintenance.value
        assertTrue(reloaded.components.isEmpty() && reloaded.manualUsage.isEmpty())
    }

    @Test
    fun theRowShowsTheMachineCostPerHourAndTheMostUrgentComponent() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 360.0)
        val viewModel = viewModelOf(printers, history)
        assertNull(viewModel.uiState.value.active.single { it.printer.id == printer.id }.maintenance)

        assertNull(viewModel.addComponent(printer.id, "Correias", "100", ""))
        assertNull(viewModel.addComponent(printer.id, "Bico", "50", "52"))
        assertNull(viewModel.addComponent(printer.id, "Mesa", "40", "50"))

        val row = viewModel.uiState.value.active.single { it.printer.id == printer.id }
        // 2000 / (12 * 25 * 8) + 0,10 por hora, a mesma prévia do formulário.
        assertEquals(2000.0 / (12 * 25 * 8) + 0.1, row.machineCostPerHour, 1e-9)
        assertEquals("Mesa", row.maintenance!!.componentName)
        assertEquals(MaintenanceState.OVERDUE, row.maintenance!!.state)
        assertEquals(1, row.maintenance!!.otherOverdue)
    }

    @Test
    fun theMaintenanceDialogFollowsTheRepositoryAndClosesWhenThePrinterIsDeleted() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val viewModel = viewModelOf(printers, history)

        viewModel.openMaintenance(printer.id)
        assertNull(viewModel.addManualUsage(printer.id, "2", "Calibração"))
        assertEquals(3.0, viewModel.uiState.value.maintenance!!.hours, 1e-9)

        viewModel.closeMaintenance()
        assertNull(viewModel.uiState.value.maintenance)

        viewModel.openMaintenance(printer.id)
        viewModel.delete(printer.id)
        assertNull(viewModel.uiState.value.maintenance)
    }

    @Test
    fun savingANewPrinterClosesTheFormAndWarns() {
        val printers = PrinterRepository()
        val viewModel = viewModelOf(printers)
        val notices = viewModel.collectNotices()

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Prusa MK4", printerPowerWattsText = "240", machinePriceText = "6000") }
        viewModel.save()

        assertNull(viewModel.uiState.value.form)
        val saved = printers.printers.value.first { it.name == "Prusa MK4" }
        assertEquals(240.0, saved.printerPowerWatts)
        assertEquals(12, saved.machineInvestment.paybackMonths)
        assertEquals(listOf(uiText(Res.string.printers_notice_saved)), notices)
    }

    @Test
    fun invalidFieldsFailEachOnTheirOwnFieldAndSaveNothing() {
        val printers = PrinterRepository()
        val before = printers.printers.value.size
        val viewModel = viewModelOf(printers)

        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(printerPowerWattsText = "-1", paybackMonthsText = "1,5", printingDaysPerMonthText = "40")
        }
        viewModel.save()

        val form = viewModel.uiState.value.form!!
        assertEquals(uiText(Res.string.common_error_name_required), form.nameError)
        assertEquals(uiText(Res.string.common_error_negative), form.powerError)
        assertEquals(uiText(Res.string.common_error_whole_number), form.paybackError)
        assertEquals(uiText(Res.string.printers_error_whole_max, 31), form.daysError)
        assertEquals(before, printers.printers.value.size)

        viewModel.updateForm { it.copy(name = "X") }
        assertNull(viewModel.uiState.value.form!!.nameError)
    }

    @Test
    fun paybackMonthsHaveACeilingAndTheMessageNamesIt() {
        val printers = PrinterRepository()
        val before = printers.printers.value.size
        val viewModel = viewModelOf(printers)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "X", paybackMonthsText = "601") }
        viewModel.save()

        assertEquals(uiText(Res.string.printers_error_whole_max, 600), viewModel.uiState.value.form!!.paybackError)
        assertEquals(before, printers.printers.value.size)
    }

    @Test
    fun editingAnArchivedPrinterKeepsItArchivedAndTheListSplitsThem() {
        val printers = PrinterRepository()
        printers.add(ender.copy(archived = true))
        val viewModel = viewModelOf(printers)
        assertEquals(listOf("printer-1"), viewModel.uiState.value.archived.map { it.printer.id })

        viewModel.startEdit("printer-1")
        viewModel.updateForm { it.copy(name = "Ender 3 V2") }
        viewModel.save()

        val saved = printers.printers.value.single { it.id == "printer-1" }
        assertEquals("Ender 3 V2", saved.name)
        assertTrue(saved.archived)
    }

    @Test
    fun requestDeleteArchiveRestoreAndDeleteWarn() {
        val printers = PrinterRepository()
        printers.add(ender)
        val viewModel = viewModelOf(printers)
        val notices = viewModel.collectNotices()

        viewModel.requestDelete("printer-1")
        assertTrue(viewModel.uiState.value.pendingDelete!!.canArchive)
        viewModel.dismissDelete()
        assertNull(viewModel.uiState.value.pendingDelete)

        viewModel.setArchived("printer-1", true)
        viewModel.setArchived("printer-1", false)
        viewModel.delete("printer-1")

        assertEquals(
            listOf(
                uiText(Res.string.printers_notice_archived),
                uiText(Res.string.printers_notice_restored),
                uiText(Res.string.printers_notice_deleted),
            ),
            notices,
        )
    }

    @Test
    fun thePresetPickerOpensAndClosesAndPickingOneClosesIt() {
        val viewModel = viewModelOf(PrinterRepository())

        viewModel.openPresetPicker()
        assertTrue(viewModel.uiState.value.presetPickerOpen)
        viewModel.closePresetPicker()
        assertEquals(false, viewModel.uiState.value.presetPickerOpen)

        viewModel.openPresetPicker()
        viewModel.startAddFromPreset(PRINTER_PRESETS.first())
        assertEquals(false, viewModel.uiState.value.presetPickerOpen)
        assertNotNull(viewModel.uiState.value.form)
    }
}
