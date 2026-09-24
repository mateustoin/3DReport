package com.threedreport.core.report

import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import kotlin.math.max

/** Situação de um componente de manutenção. */
enum class MaintenanceState {
    OK,

    /** Falta pouco: [MaintenanceReport.DUE_SOON_FRACTION] do intervalo ou menos. */
    DUE_SOON,
    OVERDUE,
}

/**
 * @property hoursSinceService horas de uso desde a última manutenção (nunca negativo).
 * @property remainingHours horas até a próxima; negativo quando já venceu.
 */
data class ComponentStatus(
    val component: MaintenanceComponent,
    val hoursSinceService: Double,
    val remainingHours: Double,
    val state: MaintenanceState,
)

/**
 * Horas de uso e situação da manutenção de cada impressora (decisão 96). Função pura e sem estado,
 * mesmo estilo de [PrintQueueReport], de onde vem a mesma soma por impressora.
 */
object MaintenanceReport {

    /**
     * Pedidos que já passaram pela máquina. "Em impressão" fica de fora porque ainda não terminou,
     * e Orçado/Aprovado ainda nem começaram.
     */
    val PRINTED_STATUSES: Set<OrderStatus> = setOf(OrderStatus.PRONTO, OrderStatus.ENTREGUE)

    /** Abaixo desta fração do intervalo restante, o componente aparece como "perto". */
    const val DUE_SOON_FRACTION = 0.1

    /**
     * Total de horas de uso de [printerId] que o app conhece: tempo de máquina dos pedidos já
     * impressos nela (uma peça × quantidade) mais as horas avulsas dela. Voltar o status de um
     * pedido ou excluí-lo diminui esse total; quem usa o número protege com `max(0, …)`.
     */
    fun printerHours(printerId: String, savedQuotes: List<SavedQuote>, manualUsage: List<ManualUsageEntry>): Double {
        val printedMinutes = savedQuotes
            .filter { it.quote.printerId == printerId && it.status in PRINTED_STATUSES }
            .sumOf { it.totalPrintTimeMinutes }
        return printedMinutes / 60.0 + manualUsage.filter { it.printerId == printerId }.sumOf { it.hours }
    }

    fun componentStatus(component: MaintenanceComponent, currentHours: Double): ComponentStatus {
        val since = max(0.0, currentHours - component.hoursAtLastService)
        val remaining = component.intervalHours - since
        val state = when {
            remaining < 0 -> MaintenanceState.OVERDUE
            remaining <= component.intervalHours * DUE_SOON_FRACTION -> MaintenanceState.DUE_SOON
            else -> MaintenanceState.OK
        }
        return ComponentStatus(component, since, remaining, state)
    }

    /**
     * Onde o contador começa ao cadastrar um componente: o total atual menos as horas que a pessoa
     * diz que já rodaram desde a última vez (zero se não souber ou for novo).
     */
    fun hoursAtLastServiceFor(currentHours: Double, hoursSinceLastService: Double): Double =
        currentHours - max(0.0, hoursSinceLastService)

    /** O componente que pede atenção primeiro (menos horas restantes), ou `null` sem nenhum. */
    fun mostUrgent(statuses: List<ComponentStatus>): ComponentStatus? = statuses.minByOrNull { it.remainingHours }
}
