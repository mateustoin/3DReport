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
 * @property queuedMinutes soma do tempo de impressão dos orçamentos em fila **nesta** impressora,
 *   já multiplicado pelas rodadas e pela quantidade de cada um: um pedido de 10 peças de 30 min
 *   ocupa a impressora por 300 min, não por 30. Um pedido com impressões em duas máquinas soma em
 *   cada uma só o tempo das impressões dela.
 * @property queuedQuoteCount pedidos com pelo menos uma impressão nesta impressora.
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
     * @param statuses quais status contam como fila. O padrão é só [OrderStatus.EM_IMPRESSAO] (o
     *   que ocupa a máquina agora, usado na aba Impressoras). A dica de prazo da tela de Orçamento
     *   passa também [OrderStatus.APROVADO], porque um pedido aprovado ainda não impresso também
     *   está na frente da peça nova.
     */
    fun summarize(
        printers: List<PrinterProfile>,
        savedQuotes: List<SavedQuote>,
        statuses: Set<OrderStatus> = setOf(OrderStatus.EM_IMPRESSAO),
    ): List<PrinterQueueEntry> {
        // Produto do catálogo (decisão 101) não ocupa máquina: o status dele fica parado em Orçado.
        val printingQuotes = savedQuotes.filter { it.isOrder && it.status in statuses }
        return printers.map { printer ->
            val queued = printingQuotes.filter { printer.id in it.quote.printerIds }
            PrinterQueueEntry(
                printer = printer,
                queuedMinutes = queued.sumOf { it.quote.printMinutesOn(printer.id) },
                queuedQuoteCount = queued.size,
            )
        }
    }
}
