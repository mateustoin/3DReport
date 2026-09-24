package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Uma peça ou tarefa de manutenção de uma impressora, com o próprio intervalo (decisão 96): trocar
 * o bico a cada X h, lubrificar os eixos a cada Y h. Cada componente tem o seu contador, em vez de
 * um limiar único por impressora.
 *
 * O contador é uma **fotografia do total de horas**, e não uma soma por data: as horas da máquina
 * vêm dos pedidos impressos e das horas avulsas (ver
 * [com.threedreport.core.report.MaintenanceReport.printerHours]), e o modelo não sabe quando cada
 * pedido foi impresso. Então fazer a manutenção guarda o total daquele momento em
 * [hoursAtLastService], e as horas desde então são o total atual menos esse número.
 *
 * @property printerId impressora a que pertence ([PrinterProfile.id]).
 * @property intervalHours de quantas em quantas horas de uso a manutenção deve ser feita.
 * @property hoursAtLastService total de horas da impressora quando a manutenção foi feita pela
 *   última vez. Pode ser negativo: quem cadastra um componente numa máquina que já rodou antes do
 *   app informa "horas desde a última vez", e o app guarda o total atual menos isso.
 */
@Serializable
data class MaintenanceComponent(
    val id: String,
    val printerId: String,
    val name: String,
    val intervalHours: Double,
    val hoursAtLastService: Double,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(intervalHours > 0) { "intervalHours deve ser positivo: $intervalHours" }
    }
}

/**
 * Uma linha do diário de manutenção de uma impressora: o que foi trocado, ajustado ou consertado,
 * e quando. Só registro, não entra em nenhum cálculo de preço.
 *
 * @property dateEpochDay data em dias desde 01/01/1970 (mesma unidade do prazo de entrega).
 * @property componentId componente que esta manutenção zerou, ou `null` quando é só um registro
 *   (ex.: "Nivelada a mesa" sem componente cadastrado).
 * @property previousHoursAtLastService onde o contador de [componentId] estava antes desta
 *   manutenção zerá-lo. É o que permite desfazer: excluir a entrada mais recente de um componente
 *   devolve o contador a esse valor, então um "Feito hoje" clicado por engano não apaga as horas
 *   acumuladas. `null` sem componente e em entradas gravadas antes deste campo existir.
 */
@Serializable
data class MaintenanceLogEntry(
    val id: String,
    val printerId: String,
    val dateEpochDay: Long,
    val description: String,
    val componentId: String? = null,
    val previousHoursAtLastService: Double? = null,
) {
    init {
        require(description.isNotBlank()) { "description não pode ser vazia" }
    }
}

/**
 * Horas de uso da impressora que não vieram de nenhum orçamento: teste de calibração, reimpressão
 * de peça que falhou, uso próprio. Sem elas, o alerta de manutenção chegaria atrasado.
 */
@Serializable
data class ManualUsageEntry(
    val id: String,
    val printerId: String,
    val dateEpochDay: Long,
    val hours: Double,
    val reason: String,
) {
    init {
        require(hours > 0) { "hours deve ser positivo: $hours" }
    }
}

/**
 * Tudo de manutenção, de todas as impressoras, num arquivo só (`maintenance.json`), separado do
 * cadastro de impressoras pra não mudar o formato de `printers.json`.
 */
@Serializable
data class PrinterMaintenance(
    val components: List<MaintenanceComponent> = emptyList(),
    val log: List<MaintenanceLogEntry> = emptyList(),
    val manualUsage: List<ManualUsageEntry> = emptyList(),
) {
    /** O mesmo conjunto, só com o que é de [printerId]. */
    fun forPrinter(printerId: String) = PrinterMaintenance(
        components = components.filter { it.printerId == printerId },
        log = log.filter { it.printerId == printerId },
        manualUsage = manualUsage.filter { it.printerId == printerId },
    )

    /**
     * Grava [entry] e, se ela tiver componente, recomeça o contador dele em [printerHoursNow],
     * guardando na entrada onde o contador estava (ver [MaintenanceLogEntry.previousHoursAtLastService]).
     */
    fun withService(entry: MaintenanceLogEntry, printerHoursNow: Double): PrinterMaintenance {
        val component = components.find { it.id == entry.componentId }
        return copy(
            log = log + entry.copy(previousHoursAtLastService = component?.hoursAtLastService),
            components = components.map { if (it.id == component?.id) it.copy(hoursAtLastService = printerHoursNow) else it },
        )
    }

    /**
     * Tira a entrada [id] do diário. Se ela for a manutenção **mais recente** do componente (pela
     * ordem de registro, não pela data, que pode ter sido escolhida no passado), o contador volta
     * pra onde estava antes dela. Uma entrada mais antiga só sai do diário: o contador atual já
     * veio de uma manutenção posterior.
     */
    fun withoutLogEntry(id: String): PrinterMaintenance {
        val entry = log.find { it.id == id } ?: return this
        val previous = entry.previousHoursAtLastService
        val isLatestForComponent = entry.componentId != null && log.last { it.componentId == entry.componentId }.id == id
        return copy(
            log = log.filterNot { it.id == id },
            components = if (previous != null && isLatestForComponent) {
                components.map { if (it.id == entry.componentId) it.copy(hoursAtLastService = previous) else it }
            } else {
                components
            },
        )
    }

    /** O mesmo conjunto sem nada de [printerId]: usado quando a impressora é excluída. */
    fun withoutPrinter(printerId: String) = PrinterMaintenance(
        components = components.filterNot { it.printerId == printerId },
        log = log.filterNot { it.printerId == printerId },
        manualUsage = manualUsage.filterNot { it.printerId == printerId },
    )
}
