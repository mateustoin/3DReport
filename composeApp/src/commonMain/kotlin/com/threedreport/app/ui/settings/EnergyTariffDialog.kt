package com.threedreport.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.SubsectionTitle
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.pricing.EnergyTariff

/** Os três jeitos de chegar no preço do kWh (decisão 126), ver [EnergyTariffDialog]. */
enum class EnergyTariffMode(val label: String) {
    BILL("Pela conta de luz"),
    FLAG("Bandeira do mês"),
    WHITE("Tarifa branca"),
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
    val consumed = parseDecimal(billConsumedKwhText, NumberKind.AMOUNT, separator) ?: return null
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
        val share = if (shareText.isBlank()) 0.0 else parseDecimal(shareText, NumberKind.MEASURE, separator) ?: return null
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyTariffDialog(onDismiss: () -> Unit, onUse: (Double) -> Unit) {
    var state by remember { mutableStateOf(EnergyTariffFormState()) }
    val separator = LocalCurrency.current.decimalSeparator
    val result = remember(state, separator) { state.result(separator) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calcular o kWh") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow {
                    EnergyTariffMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.mode == mode,
                            onClick = { state = state.copy(mode = mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = EnergyTariffMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }

                when (state.mode) {
                    EnergyTariffMode.BILL -> {
                        NumberField("Valor total da conta (${LocalCurrency.current.symbol})", state.billTotalText, NumberKind.AMOUNT) {
                            state = state.copy(billTotalText = it)
                        }
                        NumberField("Consumo do mês (kWh)", state.billConsumedKwhText, NumberKind.AMOUNT) {
                            state = state.copy(billConsumedKwhText = it)
                        }
                        Text(
                            "O jeito mais exato: a conta já inclui impostos e bandeira.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    EnergyTariffMode.FLAG -> {
                        NumberField("Tarifa (${LocalCurrency.current.symbol} por kWh)", state.flagTariffText, NumberKind.MEASURE) {
                            state = state.copy(flagTariffText = it)
                        }
                        NumberField(
                            "Acréscimo da bandeira (${LocalCurrency.current.symbol} por kWh)",
                            state.flagSurchargeText,
                            NumberKind.MEASURE,
                        ) { state = state.copy(flagSurchargeText = it) }
                        Text(
                            "O acréscimo aparece na conta de luz ou no site da sua distribuidora.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    EnergyTariffMode.WHITE -> {
                        SubsectionTitle(AppIcons.Bolt, "Fora de ponta")
                        NumberField("${LocalCurrency.current.symbol} por kWh", state.offPeakPriceText, NumberKind.MEASURE) {
                            state = state.copy(offPeakPriceText = it)
                        }
                        NumberField("% das horas de impressão", state.offPeakShareText, NumberKind.MEASURE) {
                            state = state.copy(offPeakShareText = it)
                        }
                        SubsectionTitle(AppIcons.Bolt, "Intermediário")
                        NumberField("${LocalCurrency.current.symbol} por kWh", state.midPeakPriceText, NumberKind.MEASURE) {
                            state = state.copy(midPeakPriceText = it)
                        }
                        NumberField("% das horas de impressão", state.midPeakShareText, NumberKind.MEASURE) {
                            state = state.copy(midPeakShareText = it)
                        }
                        SubsectionTitle(AppIcons.Bolt, "Ponta")
                        NumberField("${LocalCurrency.current.symbol} por kWh", state.peakPriceText, NumberKind.MEASURE) {
                            state = state.copy(peakPriceText = it)
                        }
                        NumberField("% das horas de impressão", state.peakShareText, NumberKind.MEASURE) {
                            state = state.copy(peakShareText = it)
                        }
                        Text(
                            "Informe quanto das horas de impressão cai em cada horário; o app faz a média.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text(
                    result?.let { "= ${it.toMoney()} por kWh" } ?: "Preencha os campos pra calcular.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = { result?.let(onUse) }, enabled = result != null) { Text("Usar este valor") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
