package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.settings_action_energy_calc
import _3dreport.composeapp.generated.resources.settings_energy_action_use
import _3dreport.composeapp.generated.resources.settings_energy_field_bill_kwh
import _3dreport.composeapp.generated.resources.settings_energy_field_bill_total
import _3dreport.composeapp.generated.resources.settings_energy_field_flag_surcharge
import _3dreport.composeapp.generated.resources.settings_energy_field_flag_tariff
import _3dreport.composeapp.generated.resources.settings_energy_field_period_price
import _3dreport.composeapp.generated.resources.settings_energy_field_period_share
import _3dreport.composeapp.generated.resources.settings_energy_help_bill
import _3dreport.composeapp.generated.resources.settings_energy_help_flag
import _3dreport.composeapp.generated.resources.settings_energy_help_white
import _3dreport.composeapp.generated.resources.settings_energy_mode_bill
import _3dreport.composeapp.generated.resources.settings_energy_mode_flag
import _3dreport.composeapp.generated.resources.settings_energy_mode_white
import _3dreport.composeapp.generated.resources.settings_energy_period_mid_peak
import _3dreport.composeapp.generated.resources.settings_energy_period_off_peak
import _3dreport.composeapp.generated.resources.settings_energy_period_peak
import _3dreport.composeapp.generated.resources.settings_energy_result
import _3dreport.composeapp.generated.resources.settings_energy_result_empty
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.FormSummary
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.pricing.EnergyTariff
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Os três jeitos de chegar no preço do kWh (decisão 126), ver [EnergyTariffDialog]. */
enum class EnergyTariffMode(val label: StringResource) {
    BILL(Res.string.settings_energy_mode_bill),
    FLAG(Res.string.settings_energy_mode_flag),
    WHITE(Res.string.settings_energy_mode_white),
}

/**
 * Rascunho da calculadora de kWh (decisão 126). Cada modo usa só os campos dele; os outros ficam
 * ignorados por [result], que é a lógica pura (sem Compose) testada em `EnergyTariffDialogTest`.
 */
data class EnergyTariffFormState(
    val mode: EnergyTariffMode = EnergyTariffMode.BILL,
    val billTotalText: String = "",
    val billConsumedKwhText: String = "",
    val flagTariffText: String = "",
    val flagSurchargeText: String = "",
    val offPeakPriceText: String = "",
    val offPeakShareText: String = "",
    val midPeakPriceText: String = "",
    val midPeakShareText: String = "",
    val peakPriceText: String = "",
    val peakShareText: String = "",
)

/** O preço do kWh calculado a partir do modo escolhido, ou `null` quando os campos não bastam. */
fun EnergyTariffFormState.result(decimalSeparator: Char = ','): Double? = when (mode) {
    EnergyTariffMode.BILL -> billResult(decimalSeparator)
    EnergyTariffMode.FLAG -> flagResult(decimalSeparator)
    EnergyTariffMode.WHITE -> whiteResult(decimalSeparator)
}

private fun EnergyTariffFormState.billResult(separator: Char): Double? {
    // Conta e consumo passam de mil ("1.250", "1.200 kWh"): o ponto seguido de três dígitos é milhar, como
    // o campo mostra na tela.
    val total = parseDecimal(billTotalText, NumberKind.AMOUNT, separator) ?: return null
    val consumed = parseDecimal(billConsumedKwhText, NumberKind.AMOUNT, AppLocale.decimalSeparator) ?: return null
    if (total < 0 || consumed <= 0) return null
    return EnergyTariff.fromBill(total, consumed)
}

private fun EnergyTariffFormState.flagResult(separator: Char): Double? {
    val tariff = parseDecimal(flagTariffText, NumberKind.MEASURE, separator) ?: return null
    val surcharge = if (flagSurchargeText.isBlank()) 0.0 else parseDecimal(flagSurchargeText, NumberKind.MEASURE, separator) ?: return null
    if (tariff < 0 || surcharge < 0) return null
    return EnergyTariff.withFlag(tariff, surcharge)
}

private fun EnergyTariffFormState.whiteResult(separator: Char): Double? {
    fun period(priceText: String, shareText: String): EnergyTariff.Period? {
        val price = if (priceText.isBlank()) 0.0 else parseDecimal(priceText, NumberKind.MEASURE, separator) ?: return null
        val share = if (shareText.isBlank()) 0.0 else parseDecimal(shareText, NumberKind.MEASURE, AppLocale.decimalSeparator) ?: return null
        if (price < 0 || share < 0) return null
        return EnergyTariff.Period(price, share)
    }
    val periods = listOfNotNull(
        period(offPeakPriceText, offPeakShareText),
        period(midPeakPriceText, midPeakShareText),
        period(peakPriceText, peakShareText),
    )
    if (periods.size < 3) return null
    return EnergyTariff.weightedAverage(periods)
}

/**
 * Ajuda pra chegar no preço do kWh sem sair de Configurações (decisão 126): pela conta de luz (mais
 * exato), pela bandeira do mês, ou pela tarifa branca (média pesada pelas horas de impressão em cada
 * horário). O resultado só preenche o rascunho do preço do kWh; quem salva é o "Salvar" de sempre.
 */
@Composable
fun EnergyTariffDialog(onDismiss: () -> Unit, onUse: (Double) -> Unit) {
    var state by remember { mutableStateOf(EnergyTariffFormState()) }
    val separator = LocalCurrency.current.decimalSeparator
    val symbol = LocalCurrency.current.symbol
    val result = remember(state, separator) { state.result(separator) }
    val modes = EnergyTariffMode.entries

    AppDialog(
        title = stringResource(Res.string.settings_action_energy_calc),
        onDismiss = onDismiss,
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), onDismiss)
            AppButton(stringResource(Res.string.settings_energy_action_use), onClick = { result?.let(onUse) }, kind = ButtonKind.Primary, enabled = result != null)
        },
    ) {
        SegmentedChoice(
            options = modes.map { stringResource(it.label) },
            selectedIndex = modes.indexOf(state.mode),
            onSelect = { state = state.copy(mode = modes[it]) },
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xxs))

        when (state.mode) {
            EnergyTariffMode.BILL -> FormGrid {
                cell(1) {
                    AppNumberField(stringResource(Res.string.settings_energy_field_bill_total), state.billTotalText, { state = state.copy(billTotalText = it) }, NumberKind.AMOUNT, prefix = symbol)
                }
                cell(1) {
                    AppNumberField(
                        stringResource(Res.string.settings_energy_field_bill_kwh),
                        state.billConsumedKwhText,
                        { state = state.copy(billConsumedKwhText = it) },
                        NumberKind.AMOUNT,
                        money = false,
                        help = HelpText(stringResource(Res.string.settings_energy_help_bill)),
                    )
                }
            }

            EnergyTariffMode.FLAG -> FormGrid {
                cell(1) {
                    AppNumberField(stringResource(Res.string.settings_energy_field_flag_tariff), state.flagTariffText, { state = state.copy(flagTariffText = it) }, NumberKind.MEASURE, prefix = symbol)
                }
                cell(2) {
                    AppNumberField(
                        stringResource(Res.string.settings_energy_field_flag_surcharge),
                        state.flagSurchargeText,
                        { state = state.copy(flagSurchargeText = it) },
                        NumberKind.MEASURE,
                        prefix = symbol,
                        help = HelpText(stringResource(Res.string.settings_energy_help_flag)),
                    )
                }
            }

            EnergyTariffMode.WHITE -> {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xl)) {
                    PeriodFields(
                        title = stringResource(Res.string.settings_energy_period_off_peak),
                        price = state.offPeakPriceText,
                        share = state.offPeakShareText,
                        onPrice = { state = state.copy(offPeakPriceText = it) },
                        onShare = { state = state.copy(offPeakShareText = it) },
                        help = HelpText(stringResource(Res.string.settings_energy_help_white)),
                    )
                    PeriodFields(
                        title = stringResource(Res.string.settings_energy_period_mid_peak),
                        price = state.midPeakPriceText,
                        share = state.midPeakShareText,
                        onPrice = { state = state.copy(midPeakPriceText = it) },
                        onShare = { state = state.copy(midPeakShareText = it) },
                    )
                    PeriodFields(
                        title = stringResource(Res.string.settings_energy_period_peak),
                        price = state.peakPriceText,
                        share = state.peakShareText,
                        onPrice = { state = state.copy(peakPriceText = it) },
                        onShare = { state = state.copy(peakShareText = it) },
                    )
                }
            }
        }

        if (result != null) {
            FormSummary(stringResource(Res.string.settings_energy_result), result.toMoney())
        } else {
            DialogText(stringResource(Res.string.settings_energy_result_empty), muted = true)
        }
    }
}

/** Os dois campos de um horário da tarifa branca: o preço do kWh e a parte das horas de impressão que cai nele. */
@Composable
private fun PeriodFields(title: String, price: String, share: String, onPrice: (String) -> Unit, onShare: (String) -> Unit, help: HelpText? = null) {
    FormSection(null, title, help = help) {
        FormGrid {
            cell(2) { AppNumberField(stringResource(Res.string.settings_energy_field_period_price), price, onPrice, NumberKind.MEASURE, prefix = LocalCurrency.current.symbol) }
            cell(1) { AppNumberField(stringResource(Res.string.settings_energy_field_period_share), share, onShare, NumberKind.MEASURE, money = false) }
        }
    }
}
