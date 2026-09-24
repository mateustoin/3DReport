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
 * @property finishingRate percentual sobre o custo de material pra lixar e pintar. Sempre soma,
 *   com ou sem [laborRatePerHour]: quem já conta o acabamento nos minutos de trabalho de cada
 *   orçamento deixa em zero, pra não cobrar o mesmo trabalho duas vezes.
 * @property laborRatePerHour quanto vale uma hora do seu trabalho, em R$/h. Cobre preparar o
 *   arquivo, fatiar, tirar a peça da mesa, remover suporte, lixar, pintar, embalar e atender o
 *   cliente — o serviço que a peça dá, e que não aparece em nenhum outro custo. Zero (padrão) não
 *   cobra mão de obra. Só **soma** ao preço, independente de [finishingRate].
 * @property monthlyFixedCost custo fixo mensal do negócio, em R$ (aluguel do espaço, internet,
 *   assinaturas, embalagem). Diluído por hora de impressão junto com [productiveHoursPerMonth];
 *   sem isso, quem precifica só o custo variável descobre tarde que o mês não fecha.
 * @property productiveHoursPerMonth quantas horas suas impressoras rodam por mês, somando todas —
 *   é a base pra diluir [monthlyFixedCost] em cada hora de impressão. Zero (padrão) desliga o
 *   custo fixo por completo.
 * @property administrativeCost custo fixo administrativo por orçamento (ex.: modelagem 3D), em R$.
 * @property profitMargin margem de lucro aplicada sobre o custo de produção.
 * @property taxRate percentual de imposto sobre a venda (ex.: Simples Nacional), descontado do
 *   que você recebe junto com a taxa do canal. **MEI não entra aqui:** o DAS é um valor fixo por
 *   mês, então o lugar dele é [monthlyFixedCost], não este percentual.
 * @property marketplaceFeeRate **legado.** Era a taxa única de marketplace, hoje substituída pelo
 *   catálogo de canais de venda ([SalesChannel]), escolhido por orçamento. Mantido só pra
 *   converter a configuração de quem já usava o app (a primeira execução cria um canal com esse
 *   valor) e pra não perder o dado de orçamentos antigos; o cálculo não usa mais este campo.
 */
@Serializable
data class PricingSettings(
    val energyPricePerKwh: Double,
    val failureRate: Double,
    val finishingRate: Double,
    val laborRatePerHour: Double = 0.0,
    val monthlyFixedCost: Double = 0.0,
    val productiveHoursPerMonth: Double = 0.0,
    val taxRate: Double = 0.0,
    val administrativeCost: Double = 0.0,
    val profitMargin: Double,
    val marketplaceFeeRate: Double = 0.0,
    val schemaVersion: Int = 1,
) {
    init {
        require(energyPricePerKwh >= 0) { "energyPricePerKwh não pode ser negativo" }
        require(failureRate >= 0) { "failureRate não pode ser negativo" }
        require(finishingRate >= 0) { "finishingRate não pode ser negativo" }
        require(laborRatePerHour >= 0) { "laborRatePerHour não pode ser negativo" }
        require(monthlyFixedCost >= 0) { "monthlyFixedCost não pode ser negativo" }
        require(productiveHoursPerMonth >= 0) { "productiveHoursPerMonth não pode ser negativo" }
        require(taxRate >= 0 && taxRate < 1) { "taxRate deve estar entre 0 (inclusive) e 1 (exclusive)" }
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
     * Traz uma configuração salva por uma versão anterior do app pro formato atual. Até a versão 1
     * do esquema, uma hora de trabalho configurada desligava sozinha o [finishingRate]; hoje os dois
     * somam (decisão 93). Pra quem já tinha a hora configurada, o percentual é zerado **uma vez**,
     * que é exatamente o que o cálculo antigo fazia, então o preço não muda ao atualizar. Depois de
     * salva na versão atual, uma taxa digitada de propósito nunca mais é mexida.
     */
    fun migrated(): PricingSettings = when {
        schemaVersion >= CURRENT_SCHEMA_VERSION -> this
        laborRatePerHour > 0 -> copy(finishingRate = 0.0, schemaVersion = CURRENT_SCHEMA_VERSION)
        else -> copy(schemaVersion = CURRENT_SCHEMA_VERSION)
    }

    companion object {
        /**
         * Versão do formato salvo. Um arquivo sem o campo é da versão 1 (ver [migrated]); só muda
         * quando o significado de um campo muda, não a cada campo novo com valor padrão.
         */
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
