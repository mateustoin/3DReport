package com.threedreport.app.data

import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PrinterMaintenance
import kotlinx.coroutines.flow.StateFlow

/**
 * Manutenção de todas as impressoras: componentes com intervalo, diário e horas avulsas (decisão
 * 96). Editada pela aba Impressoras.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada plataforma), num arquivo
 * próprio, pra não mudar o formato do cadastro de impressoras.
 */
expect class MaintenanceRepository() {
    val maintenance: StateFlow<PrinterMaintenance>
    fun addComponent(component: MaintenanceComponent)
    fun updateComponent(component: MaintenanceComponent)
    fun deleteComponent(id: String)

    /**
     * Grava [entry] no diário. Se ela tiver componente, o contador dele recomeça em
     * [printerHoursNow] (o total de horas da impressora agora), na mesma gravação.
     */
    fun logService(entry: MaintenanceLogEntry, printerHoursNow: Double)

    /** Tira do diário; sendo a manutenção mais recente de um componente, desfaz o zerar dele. */
    fun deleteLogEntry(id: String)
    fun addManualUsage(entry: ManualUsageEntry)
    fun deleteManualUsage(id: String)

    /** Remove tudo de [printerId]; chamado quando a impressora sai do catálogo. */
    fun deleteAllFor(printerId: String)
}
