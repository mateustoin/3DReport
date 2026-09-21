package com.threedreport.core.report

import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SavedQuote

/**
 * Quanto tempo cada impressora cadastrada está ocupada **agora** — soma do tempo de impressão dos
 * orçamentos salvos com status [OrderStatus.EM_IMPRESSAO] que usaram aquela impressora. Ajuda a
 * prometer prazo com mais segurança pro cliente (roadmap: "Fila de impressão / agenda da
 * impressora").
 *
 * @property queuedMinutes soma de `quote.job.printTimeMinutes` dos orçamentos em fila.
 */
data class PrinterQueueEntry(
    val printer: PrinterProfile,
    val queuedMinutes: Double,
    val queuedQuoteCount: Int,
)

/**
 * Função pura e sem estado, mesmo estilo de [QuoteReport]: mesma entrada, mesma saída. Não
 * considera o período (diferente do Dashboard) — fila de impressão é sobre o estado **atual**, não
 * uma janela de tempo passada.
 */
object PrintQueueReport {

    /**
     * Orçamentos salvos antes de `Quote.printerId` existir (decisão 64) não têm como saber qual
     * impressora usaram — não entram na fila de nenhuma impressora, mesmo que estejam "Em
     * impressão".
     */
    fun summarize(printers: List<PrinterProfile>, savedQuotes: List<SavedQuote>): List<PrinterQueueEntry> {
        val printingQuotes = savedQuotes.filter { it.status == OrderStatus.EM_IMPRESSAO }
        return printers.map { printer ->
            val queued = printingQuotes.filter { it.quote.printerId == printer.id }
            PrinterQueueEntry(
                printer = printer,
                queuedMinutes = queued.sumOf { it.quote.job.printTimeMinutes },
                queuedQuoteCount = queued.size,
            )
        }
    }
}
