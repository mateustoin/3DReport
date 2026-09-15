package com.threedreport.app.data

import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private val DEFAULT_PRINTERS = listOf(
    PrinterProfile(
        id = "default-printer",
        name = "Minha impressora",
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(
            machinePrice = 2700.0,
            paybackMonths = 12,
            printingDaysPerMonth = 25,
            printingHoursPerDay = 16.0,
        ),
    ),
)

actual class PrinterRepository actual constructor() {
    private val file = File(appDataDir(), "printers.json")
    private val state = MutableStateFlow(readJsonFile(file, DEFAULT_PRINTERS))

    actual val printers: StateFlow<List<PrinterProfile>> = state.asStateFlow()

    actual fun add(printer: PrinterProfile) {
        state.value = state.value + printer
        persist()
    }

    actual fun update(printer: PrinterProfile) {
        state.value = state.value.map { if (it.id == printer.id) printer else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}
