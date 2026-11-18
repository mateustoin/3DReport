package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_orders_error_days
import _3dreport.composeapp.generated.resources.settings_orders_error_deposit
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Os campos de Configurações > Pedidos que a validação pode apontar. */
enum class OrderSettingsField { DEPOSIT, STALE_DAYS }

/** Os campos de Pedidos como texto digitado (sinal padrão em %, dias, site de rastreio) e o erro de cada um. */
data class OrderSettingsUiState(
    val depositPercentText: String = "",
    val staleDaysText: String = "",
    val trackingUrlText: String = "",
    val errors: Map<OrderSettingsField, UiText> = emptyMap(),
)

/**
 * ViewModel de Configurações > Pedidos (Leva 11): sinal padrão (%), dias pra orçamento parado e site de rastreio. Como
 * Canais de venda, vale na hora: cada campo grava assim que o texto é válido, e um texto inválido mostra o erro no
 * campo sem gravar nada. Nada daqui muda preço.
 */
class OrderSettingsViewModel(
    private val repository: OrderSettingsRepository,
    scope: CoroutineScope,
    /** A moeda do app: os números são lidos com o separador decimal dela. */
    private val currency: () -> Currency = { Currency.BRL },
) {
    private val state = MutableStateFlow(repository.settings.value.toUiState(currency().decimalSeparator))
    val uiState: StateFlow<OrderSettingsUiState> = state.asStateFlow()

    init {
        // O que foi gravado por outro caminho (restaurar um backup) vira o que o formulário mostra.
        repository.settings.onEach { saved ->
            if (saved != current()) state.value = saved.toUiState(currency().decimalSeparator)
        }.launchIn(scope)
    }

    /** As configurações que o texto atual dos campos representa, ou `null` se algum campo está inválido. */
    private fun current(): OrderSettings? {
        val form = state.value
        val deposit = parseDeposit(form.depositPercentText) ?: return null
        val days = parseDays(form.staleDaysText) ?: return null
        return OrderSettings(deposit, days, form.trackingUrlText.toStored())
    }

    private fun parseDeposit(text: String): Double? {
        if (text.isBlank()) return 0.0
        val percent = parseDecimal(text, NumberKind.MEASURE, currency().decimalSeparator) ?: return null
        return (percent / 100.0).takeIf { it in 0.0..1.0 }
    }

    private fun parseDays(text: String): Int? {
        if (text.isBlank()) return 0
        return parseWholeNumber(text)?.takeIf { it in 0..OrderSettings.MAX_STALE_QUOTE_DAYS }
    }

    fun setDepositPercent(text: String) {
        val rate = parseDeposit(text)
        val error = if (rate == null) numberError(text, uiText(Res.string.settings_orders_error_deposit)) else null
        state.value = state.value.copy(depositPercentText = text, errors = state.value.errors.withField(OrderSettingsField.DEPOSIT, error))
        if (rate != null) repository.update(repository.settings.value.copy(defaultDepositRate = rate))
    }

    fun setStaleDays(text: String) {
        val days = parseDays(text)
        val error = if (days == null) numberError(text, uiText(Res.string.settings_orders_error_days, OrderSettings.MAX_STALE_QUOTE_DAYS)) else null
        state.value = state.value.copy(staleDaysText = text, errors = state.value.errors.withField(OrderSettingsField.STALE_DAYS, error))
        if (days != null) repository.update(repository.settings.value.copy(staleQuoteDays = days))
    }

    /** O site de rastreio: o endereço dos Correios grava como "padrão do app" (nada gravado), texto vazio como "sem link". */
    fun setTrackingUrl(text: String) {
        state.value = state.value.copy(trackingUrlText = text)
        repository.update(repository.settings.value.copy(trackingUrlTemplate = text.toStored()))
    }

    /** "Usar o site dos Correios": volta ao padrão do app. */
    fun useDefaultTracking() = setTrackingUrl(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE)

    private fun numberError(text: String, rangeError: UiText): UiText =
        if (parseDecimal(text, NumberKind.MEASURE, currency().decimalSeparator) == null) uiText(Res.string.common_error_number_invalid) else rangeError

    private fun Map<OrderSettingsField, UiText>.withField(field: OrderSettingsField, error: UiText?) =
        if (error == null) this - field else this + (field to error)

    private fun String.toStored(): String? = trim().takeUnless { it == OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE }?.let { this.trim() }
}

private fun OrderSettings.toUiState(separator: Char) = OrderSettingsUiState(
    depositPercentText = (defaultDepositRate * 100).toInputText(separator),
    staleDaysText = staleQuoteDays.toString(),
    trackingUrlText = effectiveTrackingUrlTemplate,
)
