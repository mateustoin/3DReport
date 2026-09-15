package com.threedreport.core.model

/**
 * Parâmetros de custo da operação, compartilhados entre orçamentos.
 *
 * Percentuais são frações decimais: 10% = `0.10`, 100% = `1.0`.
 *
 * @property energyPricePerKwh preço do kWh, em R$.
 * @property printerPowerWatts consumo médio da impressora, em W.
 * @property maintenanceCostPerHour custo de manutenção/desgaste por hora de impressão, em R$.
 * @property failureRate percentual sobre o custo de material reservado para falhas de impressão.
 * @property finishingRate percentual sobre o custo de material referente a acabamento.
 * @property administrativeCost custo fixo administrativo por orçamento (ex.: modelagem 3D), em R$.
 * @property machineInvestment dados para diluir o valor da máquina nas horas de impressão.
 * @property profitMargin margem de lucro aplicada sobre o custo de produção.
 */
data class PricingSettings(
    val energyPricePerKwh: Double,
    val printerPowerWatts: Double,
    val maintenanceCostPerHour: Double,
    val failureRate: Double,
    val finishingRate: Double,
    val administrativeCost: Double = 0.0,
    val machineInvestment: MachineInvestment,
    val profitMargin: Double,
) {
    init {
        require(energyPricePerKwh >= 0) { "energyPricePerKwh não pode ser negativo" }
        require(printerPowerWatts >= 0) { "printerPowerWatts não pode ser negativo" }
        require(maintenanceCostPerHour >= 0) { "maintenanceCostPerHour não pode ser negativo" }
        require(failureRate >= 0) { "failureRate não pode ser negativo" }
        require(finishingRate >= 0) { "finishingRate não pode ser negativo" }
        require(administrativeCost >= 0) { "administrativeCost não pode ser negativo" }
        require(profitMargin >= 0) { "profitMargin não pode ser negativo" }
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
