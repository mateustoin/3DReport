package com.threedreport.app.data

import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PrinterMaintenance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class MaintenanceRepository actual constructor() {
    private val file = File(appDataDir(), "maintenance.json")
    private val state = MutableStateFlow(readJsonFile(file, PrinterMaintenance()))

    actual val maintenance: StateFlow<PrinterMaintenance> = state.asStateFlow()

    actual fun addComponent(component: MaintenanceComponent) = edit { it.copy(components = it.components + component) }

    actual fun updateComponent(component: MaintenanceComponent) = edit { current ->
        current.copy(components = current.components.map { if (it.id == component.id) component else it })
    }

    actual fun deleteComponent(id: String) = edit { current -> current.copy(components = current.components.filterNot { it.id == id }) }

    actual fun logService(entry: MaintenanceLogEntry, printerHoursNow: Double) = edit { current ->
        current.copy(
            log = current.log + entry,
            components = current.components.map {
                if (it.id == entry.componentId) it.copy(hoursAtLastService = printerHoursNow) else it
            },
        )
    }

    actual fun deleteLogEntry(id: String) = edit { current -> current.copy(log = current.log.filterNot { it.id == id }) }

    actual fun addManualUsage(entry: ManualUsageEntry) = edit { it.copy(manualUsage = it.manualUsage + entry) }

    actual fun deleteManualUsage(id: String) = edit { current ->
        current.copy(manualUsage = current.manualUsage.filterNot { it.id == id })
    }

    actual fun deleteAllFor(printerId: String) = edit { it.withoutPrinter(printerId) }

    private fun edit(transform: (PrinterMaintenance) -> PrinterMaintenance) {
        state.value = transform(state.value)
        writeJsonFile(file, state.value)
    }
}
