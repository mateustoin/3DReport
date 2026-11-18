package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_notice_saved
import _3dreport.composeapp.generated.resources.settings_orders_error_days
import _3dreport.composeapp.generated.resources.settings_orders_error_deposit
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

/** Os campos de Configurações > Pedidos que a validação pode apontar. */
enum class OrderSettingsField { DEPOSIT, STALE_DAYS }

/**
 * Os campos de Pedidos como texto digitado (sinal padrão em %, dias, site de rastreio), o erro de cada um e se o
 * rascunho difere do que está gravado.
 */
data class OrderSettingsUiState(
    val depositPercentText: String = "",
    val staleDaysText: String = "",
    val trackingUrlText: String = "",
    val errors: Map<OrderSettingsField, UiText> = emptyMap(),
    val hasUnsavedChanges: Boolean = false,
)

/**
 * ViewModel de Configurações > Pedidos (Leva 11, item 36): sinal padrão (%), dias pra orçamento parado e site de
 * rastreio. Como Negócio e Documentos, é rascunho até o "Salvar" da barra "Alterações não salvas" ([validate], [save],
 * [discard]). Nada daqui muda preço.
 */
class OrderSettingsViewModel(
    private val repository: OrderSettingsRepository,
    scope: CoroutineScope,
    /** A moeda do app: os números são lidos com o separador decimal dela. */
    private val currency: () -> Currency = { Currency.BRL },
) {
    /** O que está gravado e de onde o rascunho saiu. */
    private var baseline: OrderSettings = repository.settings.value

    private val state = MutableStateFlow(baseline.toUiState())
    val uiState: StateFlow<OrderSettingsUiState> = state.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    init {
        // O que foi gravado por outro caminho (restaurar um backup): sem edição em andamento, o formulário mostra o novo.
        repository.settings.onEach { saved ->
            if (saved == baseline) return@onEach
            val wasClean = !hasUnsavedChanges
            baseline = saved
            publish(if (wasClean) saved.toUiState() else state.value)
        }.launchIn(scope)
    }

    val hasUnsavedChanges: Boolean
        get() = state.value.hasUnsavedChanges

    private fun OrderSettings.toUiState() = OrderSettingsUiState(
        depositPercentText = (defaultDepositRate * 100).toInputText(AppLocale.decimalSeparator),
        staleDaysText = staleQuoteDays.toString(),
        trackingUrlText = effectiveTrackingUrlTemplate(currency()),
    )

    private fun publish(next: OrderSettingsUiState) {
        val saved = baseline.toUiState()
        state.value = next.copy(
            hasUnsavedChanges = next.depositPercentText != saved.depositPercentText ||
                next.staleDaysText != saved.staleDaysText ||
                next.trackingUrlText.trim() != saved.trackingUrlText.trim(),
        )
    }

    fun setDepositPercent(text: String) = publish(state.value.copy(depositPercentText = text, errors = state.value.errors - OrderSettingsField.DEPOSIT))

    fun setStaleDays(text: String) = publish(state.value.copy(staleDaysText = text, errors = state.value.errors - OrderSettingsField.STALE_DAYS))

    fun setTrackingUrl(text: String) = publish(state.value.copy(trackingUrlText = text))

    /** "Usar o site dos Correios": o padrão do app volta pro rascunho. */
    fun useDefaultTracking() = setTrackingUrl(OrderSettings.defaultTrackingUrlTemplate(currency()))

    /** Volta o rascunho pro que está gravado ("Descartar" da barra de alterações). */
    fun discard() = publish(baseline.toUiState())

    private class Parsed(val settings: OrderSettings?, val errors: Map<OrderSettingsField, UiText>)

    private fun parse(form: OrderSettingsUiState): Parsed {
        val errors = mutableMapOf<OrderSettingsField, UiText>()
        val percent = if (form.depositPercentText.isBlank()) 0.0 else parseDecimal(form.depositPercentText, NumberKind.MEASURE, AppLocale.decimalSeparator)
        when {
            percent == null -> errors[OrderSettingsField.DEPOSIT] = uiText(Res.string.common_error_number_invalid)
            percent !in 0.0..100.0 -> errors[OrderSettingsField.DEPOSIT] = uiText(Res.string.settings_orders_error_deposit)
        }
        val days = if (form.staleDaysText.isBlank()) 0 else parseWholeNumber(form.staleDaysText)
        if (days == null || days !in 0..OrderSettings.MAX_STALE_QUOTE_DAYS) {
            errors[OrderSettingsField.STALE_DAYS] = uiText(Res.string.settings_orders_error_days, OrderSettings.MAX_STALE_QUOTE_DAYS)
        }
        if (errors.isNotEmpty()) return Parsed(null, errors)
        // O endereço dos Correios grava como "padrão do app" (nada gravado); texto vazio, como "sem link".
        val url = form.trackingUrlText.trim()
        return Parsed(
            OrderSettings(
                defaultDepositRate = percent!! / 100.0,
                staleQuoteDays = days!!,
                trackingUrlTemplate = url.takeUnless { it == OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE },
            ),
            errors,
        )
    }

    /** Confere os campos sem gravar: com erro, mostra no campo e devolve `false`. Quem salva mais de uma seção valida todas antes. */
    fun validate(): Boolean {
        val parsed = parse(state.value)
        if (parsed.settings == null) publish(state.value.copy(errors = parsed.errors))
        return parsed.settings != null
    }

    /** Valida e grava. Com [announce] falso o aviso de salvo fica com quem salva mais de uma seção de uma vez. */
    fun save(announce: Boolean = true) {
        val parsed = parse(state.value)
        val settings = parsed.settings
        if (settings == null) {
            publish(state.value.copy(errors = parsed.errors))
            return
        }
        repository.update(settings)
        baseline = settings
        publish(settings.toUiState())
        if (announce) noticeChannel.trySend(UiNotice(uiText(Res.string.settings_notice_saved)))
    }
}
