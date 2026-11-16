package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_error_percent_max
import _3dreport.composeapp.generated.resources.settings_notice_saved
import _3dreport.composeapp.generated.resources.settings_notice_saved_fixed_cost
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel da seção Negócio de Configurações. Um [uiState] só e os [notices] (o aviso de salvo e o de custo fixo sem
 * horas).
 *
 * Edição em rascunho: as mudanças só valem para o resto do app depois de [save], que valida os campos e grava em
 * [SettingsRepository], ou somem com [discard]. O rascunho acompanha, dentro de [scope], o que for gravado por outro
 * caminho (o primeiro uso grava direto no repositório). Os números são lidos com o separador decimal da moeda de
 * [currency], o mesmo que o campo usa pra validar e mostrar a dica de leitura.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    private val currency: () -> Currency = { Currency.BRL },
) {

    /** O que está gravado e de onde o rascunho saiu; serve pra saber se há edição por salvar. */
    private var baseline: PricingSettings = settingsRepository.settings.value

    private val state = MutableStateFlow(baseline.toUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    init {
        settingsRepository.settings.onEach(::syncWith).launchIn(scope)
    }

    /** Se o formulário tem alguma mudança ainda não salva. */
    val hasUnsavedChanges: Boolean
        get() = state.value.hasUnsavedChanges

    fun update(transform: (SettingsUiState) -> SettingsUiState) {
        publish(transform(state.value).copy(errors = emptyMap()))
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
        publish(if (wasClean) saved.toUiState() else state.value)
    }

    /** Volta o formulário pro que está gravado ("Descartar" da barra de alterações, decisão 112). */
    fun discard() {
        publish(baseline.toUiState())
    }

    /**
     * Confere os campos sem gravar: com erro, mostra cada um no seu campo e devolve `false`. Quem salva mais de uma
     * seção de uma vez valida todas antes de gravar qualquer uma.
     */
    fun validate(): Boolean = parse(state.value).let { result ->
        if (result.settings == null) publish(state.value.copy(errors = result.errors))
        result.settings != null
    }

    /**
     * Valida e grava. Com [announce] falso o aviso de salvo fica com quem salva mais de uma seção de uma vez. Com custo
     * fixo e sem horas pra dividir, o aviso de salvo já diz que o custo fixo não entra no preço.
     */
    fun save(announce: Boolean = true) {
        val current = state.value
        val result = parse(current)
        val settings = result.settings
        if (settings == null) {
            publish(current.copy(errors = result.errors))
            return
        }
        settingsRepository.update(settings)
        baseline = settings
        publish(settings.toUiState())
        if (!announce) return
        // Custo fixo sem horas pra dividir não entra no preço: melhor avisar do que deixar a pessoa achar que entrou. O
        // aviso é um só (o kit não tem um tom de atenção, então fica no estilo que demora e ganha o "x").
        val warn = settings.hasFixedCostWithoutHours()
        noticeChannel.trySend(UiNotice(uiText(if (warn) Res.string.settings_notice_saved_fixed_cost else Res.string.settings_notice_saved), isError = warn))
    }

    private class ParseResult(val settings: PricingSettings?, val errors: Map<SettingsField, UiText>)

    private fun parse(current: SettingsUiState): ParseResult {
        val separator = currency().decimalSeparator
        val errors = mutableMapOf<SettingsField, UiText>()
        fun amount(field: SettingsField, text: String, kind: NumberKind = NumberKind.AMOUNT, below100: Boolean = false): Double {
            val value = if (text.isBlank()) 0.0 else parseDecimal(text, kind, separator)
            when {
                value == null -> errors[field] = uiText(Res.string.common_error_number_invalid)
                value < 0 -> errors[field] = uiText(Res.string.common_error_negative)
                below100 && value >= 100 -> errors[field] = uiText(Res.string.settings_error_percent_max)
            }
            return value ?: 0.0
        }
        val energy = amount(SettingsField.ENERGY, current.energyPricePerKwhText, NumberKind.MEASURE)
        val failure = amount(SettingsField.FAILURE_RATE, current.failureRatePercentText, NumberKind.MEASURE)
        val finishing = amount(SettingsField.FINISHING_RATE, current.finishingRatePercentText, NumberKind.MEASURE)
        val labor = amount(SettingsField.LABOR, current.laborRatePerHourText)
        val fixedCost = amount(SettingsField.MONTHLY_FIXED_COST, current.monthlyFixedCostText)
        val hours = amount(SettingsField.PRODUCTIVE_HOURS, current.productiveHoursPerMonthText)
        val administrative = amount(SettingsField.ADMINISTRATIVE_COST, current.administrativeCostText)
        val margin = amount(SettingsField.MARGIN, current.profitMarginPercentText, NumberKind.MEASURE)
        val tax = amount(SettingsField.TAX, current.taxRatePercentText, NumberKind.MEASURE, below100 = true)
        val minimum = amount(SettingsField.MINIMUM_PRICE, current.minimumOrderPriceText)
        val rush = amount(SettingsField.RUSH, current.rushSurchargeRatePercentText, NumberKind.MEASURE)
        if (errors.isNotEmpty()) return ParseResult(null, errors)
        return ParseResult(
            PricingSettings(
                energyPricePerKwh = energy,
                failureRate = failure / 100.0,
                finishingRate = finishing / 100.0,
                laborRatePerHour = labor,
                monthlyFixedCost = fixedCost,
                productiveHoursPerMonth = hours,
                administrativeCost = administrative,
                profitMargin = margin / 100.0,
                taxRate = tax / 100.0,
                minimumOrderPrice = minimum,
                rushSurchargeRate = rush / 100.0,
            ),
            errors,
        )
    }

    /**
     * Publica [next] com a marca de "há o que salvar" recalculada contra o que está gravado, e o aviso de custo fixo
     * sem horas, que descreve o que está gravado e não o que está sendo digitado.
     */
    private fun publish(next: SettingsUiState) {
        state.value = next.copy(
            hasUnsavedChanges = next.fieldsOnly() != baseline.toUiState().fieldsOnly(),
            fixedCostWithoutHours = baseline.hasFixedCostWithoutHours(),
        )
    }
}

/** Custo fixo configurado sem horas de impressão por mês pra dividir: ele não entra no preço. */
private fun PricingSettings.hasFixedCostWithoutHours() = monthlyFixedCost > 0 && productiveHoursPerMonth <= 0

private fun SettingsUiState.fieldsOnly() = copy(errors = emptyMap(), fixedCostWithoutHours = false, hasUnsavedChanges = false)

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
    minimumOrderPriceText = minimumOrderPrice.toInputText(),
    rushSurchargeRatePercentText = (rushSurchargeRate * 100).toInputText(),
    fixedCostWithoutHours = hasFixedCostWithoutHours(),
)
