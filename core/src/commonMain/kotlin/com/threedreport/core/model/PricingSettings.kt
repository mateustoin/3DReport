package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Parâmetros de custo do negócio, compartilhados entre todos os orçamentos e
 * impressoras (o que é específico de cada impressora fica em [PrinterProfile]).
 *
 * Percentuais são frações decimais: 10% = `0.10`, 100% = `1.0`.
 *
 * @property energyPricePerKwh preço do kWh, em R$.
 * @property failureRate percentual reservado para falhas de impressão. Incide sobre **todo** o
 *   custo que se paga de novo ao reimprimir a peça (material, energia, manutenção, retorno da
 *   máquina, custo fixo, mão de obra e acabamento) — só [administrativeCost] fica de fora, porque
 *   uma modelagem já feita não precisa ser refeita quando a impressão falha.
 * @property finishingRate percentual sobre o custo de material referente a acabamento.
 *   **Legado:** só é usado quando [laborRatePerHour] é zero. Acabamento é trabalho, e trabalho
 *   escala com tempo, não com gramas de plástico — quem configura uma taxa de mão de obra passa a
 *   cobrar acabamento pelos minutos informados em cada orçamento, e este percentual deixa de ter
 *   efeito (ver `pricing/PricingCalculator`).
 * @property laborRatePerHour quanto vale uma hora do seu trabalho, em R$/h. Cobre preparar o
 *   arquivo, fatiar, tirar a peça da mesa, remover suporte, lixar, pintar, embalar e atender o
 *   cliente — o serviço que a peça dá, e que não aparece em nenhum outro custo. Zero (padrão)
 *   mantém o comportamento antigo, sem cobrar mão de obra e usando [finishingRate].
 * @property monthlyFixedCost custo fixo mensal do negócio, em R$ (aluguel do espaço, internet,
 *   assinaturas, embalagem). Diluído por hora de impressão junto com [productiveHoursPerMonth];
 *   sem isso, quem precifica só o custo variável descobre tarde que o mês não fecha.
 * @property productiveHoursPerMonth quantas horas suas impressoras rodam por mês, somando todas —
 *   é a base pra diluir [monthlyFixedCost] em cada hora de impressão. Zero (padrão) desliga o
 *   custo fixo por completo.
 * @property administrativeCost custo fixo administrativo por orçamento (ex.: modelagem 3D), em R$.
 * @property profitMargin margem de lucro aplicada sobre o custo de produção.
 * @property marketplaceFeeRate percentual que um marketplace (ex.: Shopee) desconta da venda,
 *   quando o orçamento marcar que a peça será vendida por lá. Diferente de um serviço extra: não
 *   é somado ao total cobrado do cliente, é descontado do que o criador recebe — por isso o valor
 *   de venda aumenta o suficiente para que a margem de lucro real não mude (ver
 *   `pricing/PricingCalculator`).
 */
@Serializable
data class PricingSettings(
    val energyPricePerKwh: Double,
    val failureRate: Double,
    val finishingRate: Double,
    val laborRatePerHour: Double = 0.0,
    val monthlyFixedCost: Double = 0.0,
    val productiveHoursPerMonth: Double = 0.0,
    val administrativeCost: Double = 0.0,
    val profitMargin: Double,
    val marketplaceFeeRate: Double = 0.0,
) {
    init {
        require(energyPricePerKwh >= 0) { "energyPricePerKwh não pode ser negativo" }
        require(failureRate >= 0) { "failureRate não pode ser negativo" }
        require(finishingRate >= 0) { "finishingRate não pode ser negativo" }
        require(laborRatePerHour >= 0) { "laborRatePerHour não pode ser negativo" }
        require(monthlyFixedCost >= 0) { "monthlyFixedCost não pode ser negativo" }
        require(productiveHoursPerMonth >= 0) { "productiveHoursPerMonth não pode ser negativo" }
        require(administrativeCost >= 0) { "administrativeCost não pode ser negativo" }
        require(profitMargin >= 0) { "profitMargin não pode ser negativo" }
        require(marketplaceFeeRate >= 0 && marketplaceFeeRate < 1) {
            "marketplaceFeeRate deve estar entre 0 (inclusive) e 1 (exclusive)"
        }
    }

    /**
     * Quanto de [monthlyFixedCost] cada hora de impressão precisa pagar. Mesma ideia do
     * [MachineInvestment.costPerHour], mas pro negócio inteiro em vez de uma máquina só. Zero
     * quando [productiveHoursPerMonth] não foi informado — sem saber em quantas horas diluir, o
     * custo fixo simplesmente não entra na conta.
     */
    val fixedCostPerHour: Double
        get() = if (productiveHoursPerMonth > 0) monthlyFixedCost / productiveHoursPerMonth else 0.0

    /**
     * Se o acabamento deve ser cobrado pelo tempo de trabalho ([laborRatePerHour]) em vez do
     * percentual sobre o material ([finishingRate]) — ver KDoc dos dois campos.
     */
    val chargesLaborByTime: Boolean
        get() = laborRatePerHour > 0
}
