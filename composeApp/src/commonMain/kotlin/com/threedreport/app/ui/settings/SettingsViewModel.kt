package com.threedreport.app.ui.settings

import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da tela de Configurações gerais.
 *
 * Edição em rascunho: as mudanças só valem para o resto do app depois de
 * [save], que valida os campos e grava em [SettingsRepository].
 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) {

    /** O que está gravado e de onde o rascunho saiu; serve pra saber se há edição por salvar. */
    private var baseline: PricingSettings = settingsRepository.settings.value

    private val state = MutableStateFlow(baseline.toUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    /** O que está gravado agora, pra tela acompanhar mudanças feitas de fora (onboarding, backup). */
    val savedSettings: StateFlow<PricingSettings> = settingsRepository.settings

    /** Se o formulário tem alguma mudança ainda não salva. */
    val hasUnsavedChanges: Boolean
        get() = state.value.fieldsOnly() != baseline.toUiState()

    fun update(transform: (SettingsUiState) -> SettingsUiState) {
        state.value = transform(state.value).copy(savedConfirmation = false)
    }

    /**
     * Acompanha o que foi gravado por outro caminho (o onboarding grava direto no repositório). Com o
     * formulário sem edição, ele passa a mostrar os valores novos; antes, o formulário continuava com os
     * padrões da abertura do app, e o primeiro "Salvar" desfazia o onboarding em silêncio. Com edição
     * em andamento, o rascunho fica como está.
     */
    fun syncWith(saved: PricingSettings) {
        if (saved == baseline) return
        val wasClean = !hasUnsavedChanges
        baseline = saved
        if (wasClean) state.value = saved.toUiState()
    }

    /** Chamado depois de o aviso de sucesso aparecer, pra um novo salvamento poder avisar de novo. */
    fun consumeSavedConfirmation() {
        state.value = state.value.copy(savedConfirmation = false)
    }

    fun save() {
        val current = state.value
        val settings = runCatching {
            PricingSettings(
                energyPricePerKwh = current.energyPricePerKwhText.nonNegative("Preço do kWh", NumberKind.MEASURE),
                failureRate = current.failureRatePercentText.percent("Taxa de falhas"),
                finishingRate = current.finishingRatePercentText.percent("Taxa de acabamento"),
                laborRatePerHour = current.laborRatePerHourText.nonNegative("Valor da sua hora de trabalho"),
                monthlyFixedCost = current.monthlyFixedCostText.nonNegative("Custo fixo mensal"),
                productiveHoursPerMonth = current.productiveHoursPerMonthText.nonNegative("Horas de impressão por mês"),
                administrativeCost = current.administrativeCostText.nonNegative("Custo administrativo"),
                profitMargin = current.profitMarginPercentText.percent("Margem de lucro"),
                taxRate = current.taxRatePercentText.percent("Imposto sobre a venda", below100 = true),
            )
        }

        state.value = settings.fold(
            onSuccess = {
                settingsRepository.update(it)
                baseline = it
                it.toUiState().copy(savedConfirmation = true, warningMessage = it.warning())
            },
            onFailure = { current.copy(errorMessage = it.message, savedConfirmation = false) },
        )
    }
}

/** Número não negativo, com a mensagem dizendo qual campo corrigir (nunca o nome interno do modelo). */
private fun String.nonNegative(label: String, kind: NumberKind = NumberKind.AMOUNT): Double {
    val value = if (isBlank()) 0.0 else toRequiredDouble(label, kind)
    require(value >= 0) { "\"$label\" não pode ser negativo." }
    return value
}

/** Percentual digitado (15 = 15%) como fração (0,15). */
private fun String.percent(label: String, below100: Boolean = false): Double {
    val value = nonNegative(label, NumberKind.MEASURE)
    require(!below100 || value < 100) { "\"$label\" precisa ficar abaixo de 100%." }
    return value / 100.0
}

/** Custo fixo sem horas pra dividir não entra no preço: melhor avisar do que deixar a pessoa achar que entrou. */
private fun PricingSettings.warning(): String? =
    if (monthlyFixedCost > 0 && productiveHoursPerMonth <= 0) {
        "O custo fixo mensal só entra no preço quando as horas de impressão por mês estão preenchidas."
    } else {
        null
    }

private fun SettingsUiState.fieldsOnly() = copy(errorMessage = null, savedConfirmation = false, warningMessage = null)

private fun PricingSettings.toUiState() = SettingsUiState(
    energyPricePerKwhText = energyPricePerKwh.toInputText(),
    failureRatePercentText = (failureRate * 100).toInputText(),
    finishingRatePercentText = (finishingRate * 100).toInputText(),
    laborRatePerHourText = laborRatePerHour.toInputText(),
    monthlyFixedCostText = monthlyFixedCost.toInputText(),
    productiveHoursPerMonthText = productiveHoursPerMonth.toInputText(),
    administrativeCostText = administrativeCost.toInputText(),
    profitMarginPercentText = (profitMargin * 100).toInputText(),
    taxRatePercentText = (taxRate * 100).toInputText(),
)
