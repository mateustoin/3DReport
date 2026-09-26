package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Perfil de uma impressora 3D específica. Quem faz orçamentos costuma ter
 * várias impressoras; cada uma tem seu próprio perfil salvo e um orçamento
 * escolhe qual delas usar, em vez de reconfigurar tudo a cada peça.
 *
 * @property id identificador único, atribuído por quem cria o perfil (UI).
 * @property name nome livre para identificação (ex.: "Ender 3", "Bambu A1").
 * @property printerPowerWatts consumo médio da impressora, em W.
 * @property maintenanceCostPerHour custo de manutenção/desgaste por hora de impressão, em R$.
 * @property machineInvestment dados para diluir o valor da máquina nas horas de impressão.
 * @property archived arquivado (decisão 115): some das escolhas de um orçamento novo, mas continua no
 *   cadastro pra quem já usou. Pedidos reabertos e produtos do catálogo continuam achando ele.
 */
@Serializable
data class PrinterProfile(
    val id: String,
    val name: String,
    val printerPowerWatts: Double,
    val maintenanceCostPerHour: Double,
    val machineInvestment: MachineInvestment,
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(printerPowerWatts >= 0) { "printerPowerWatts não pode ser negativo" }
        require(maintenanceCostPerHour >= 0) { "maintenanceCostPerHour não pode ser negativo" }
    }
}

/**
 * Retorno do investimento na impressora: quanto cada hora de impressão
 * deve pagar para quitar a máquina no prazo desejado.
 *
 * @property machinePrice valor pago na máquina, em R$.
 * @property paybackMonths prazo desejado para o retorno, em meses.
 * @property printingDaysPerMonth dias de uso da máquina por mês.
 * @property printingHoursPerDay horas de impressão por dia.
 */
@Serializable
data class MachineInvestment(
    val machinePrice: Double,
    val paybackMonths: Int,
    val printingDaysPerMonth: Int,
    val printingHoursPerDay: Double,
) {
    init {
        require(machinePrice >= 0) { "machinePrice não pode ser negativo" }
        require(paybackMonths > 0) { "paybackMonths deve ser positivo" }
        require(printingDaysPerMonth > 0) { "printingDaysPerMonth deve ser positivo" }
        require(printingHoursPerDay > 0) { "printingHoursPerDay deve ser positivo" }
    }

    /** Valor a adicionar por hora de impressão: preço ÷ (meses · dias/mês · horas/dia). */
    val costPerHour: Double
        get() = machinePrice / (paybackMonths * printingDaysPerMonth * printingHoursPerDay)
}
