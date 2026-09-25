package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PrinterMaintenance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manutenção de todas as impressoras: componentes com intervalo, diário e horas avulsas (decisão
 * 96). Editada pela aba Impressoras.
 */
interface MaintenanceRepository {
    val maintenance: StateFlow<PrinterMaintenance>
    fun addComponent(component: MaintenanceComponent)
    fun updateComponent(component: MaintenanceComponent)
    fun deleteComponent(id: String)

    /**
     * Grava [entry] no diário. Se ela tiver componente, o contador dele recomeça em
     * [printerHoursNow] (o total de horas da impressora agora).
     */
    fun logService(entry: MaintenanceLogEntry, printerHoursNow: Double)

    /** Tira do diário; sendo a manutenção mais recente de um componente, desfaz o zerar dele. */
    fun deleteLogEntry(id: String)
    fun addManualUsage(entry: ManualUsageEntry)
    fun deleteManualUsage(id: String)

    /** Remove tudo de [printerId]; chamado quando a impressora sai do catálogo. */
    fun deleteAllFor(printerId: String)
}

/**
 * Cada tipo de item numa lista de registros própria (com as datas de cada um, decisão 106), montados
 * juntos em [PrinterMaintenance] pra quem lê. As regras de zerar e desfazer continuam no `core`
 * ([PrinterMaintenance.withService] e [PrinterMaintenance.withoutLogEntry]).
 */
class StoredMaintenanceRepository(
    private val components: RecordCollection<MaintenanceComponent>,
    private val log: RecordCollection<MaintenanceLogEntry>,
    private val manualUsage: RecordCollection<ManualUsageEntry>,
) : MaintenanceRepository {

    private val state = MutableStateFlow(current())
    override val maintenance: StateFlow<PrinterMaintenance> = state.asStateFlow()

    private fun current() = PrinterMaintenance(components.items.value, log.items.value, manualUsage.items.value)

    private fun refresh() {
        state.value = current()
    }

    override fun addComponent(component: MaintenanceComponent) = components.add(component).also { refresh() }

    override fun updateComponent(component: MaintenanceComponent) {
        components.update(component)
        refresh()
    }

    override fun deleteComponent(id: String) {
        components.delete(id)
        refresh()
    }

    override fun logService(entry: MaintenanceLogEntry, printerHoursNow: Double) = apply(state.value.withService(entry, printerHoursNow))

    override fun deleteLogEntry(id: String) = apply(state.value.withoutLogEntry(id))

    override fun addManualUsage(entry: ManualUsageEntry) = manualUsage.add(entry).also { refresh() }

    override fun deleteManualUsage(id: String) {
        manualUsage.delete(id)
        refresh()
    }

    override fun deleteAllFor(printerId: String) = apply(state.value.withoutPrinter(printerId))

    /** Grava a diferença entre o estado atual e [target], item a item, e atualiza a leitura. */
    private fun apply(target: PrinterMaintenance) {
        val before = state.value
        sync(components, before.components, target.components) { it.id }
        sync(log, before.log, target.log) { it.id }
        sync(manualUsage, before.manualUsage, target.manualUsage) { it.id }
        refresh()
    }

    private fun <T> sync(collection: RecordCollection<T>, before: List<T>, after: List<T>, idOf: (T) -> String) {
        val afterById = after.associateBy(idOf)
        before.filter { idOf(it) !in afterById }.forEach { collection.delete(idOf(it)) }
        val beforeIds = before.mapTo(HashSet(), idOf)
        after.forEach { item -> if (idOf(item) in beforeIds) collection.update(item) else collection.add(item) }
    }
}
