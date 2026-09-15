package com.threedreport.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Tela de Configurações: parâmetros de custo da operação, editados em conjunto e salvos com um botão. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Energia", style = MaterialTheme.typography.titleMedium)
        LabeledField("Preço do kWh (R$)", state.energyPricePerKwhText) {
            viewModel.update { s -> s.copy(energyPricePerKwhText = it) }
        }
        LabeledField("Consumo da impressora (W)", state.printerPowerWattsText) {
            viewModel.update { s -> s.copy(printerPowerWattsText = it) }
        }

        Text("Manutenção e custos fixos", style = MaterialTheme.typography.titleMedium)
        LabeledField("Manutenção por hora (R$)", state.maintenanceCostPerHourText) {
            viewModel.update { s -> s.copy(maintenanceCostPerHourText = it) }
        }
        LabeledField("Custo administrativo por orçamento (R$)", state.administrativeCostText) {
            viewModel.update { s -> s.copy(administrativeCostText = it) }
        }

        Text("Falhas e acabamento", style = MaterialTheme.typography.titleMedium)
        LabeledField("Taxa de falhas (%)", state.failureRatePercentText) {
            viewModel.update { s -> s.copy(failureRatePercentText = it) }
        }
        LabeledField("Taxa de acabamento (%)", state.finishingRatePercentText) {
            viewModel.update { s -> s.copy(finishingRatePercentText = it) }
        }

        Text("Retorno do investimento na máquina", style = MaterialTheme.typography.titleMedium)
        LabeledField("Valor da máquina (R$)", state.machinePriceText) {
            viewModel.update { s -> s.copy(machinePriceText = it) }
        }
        LabeledField("Prazo de retorno (meses)", state.paybackMonthsText) {
            viewModel.update { s -> s.copy(paybackMonthsText = it) }
        }
        LabeledField("Dias de uso por mês", state.printingDaysPerMonthText) {
            viewModel.update { s -> s.copy(printingDaysPerMonthText = it) }
        }
        LabeledField("Horas de uso por dia", state.printingHoursPerDayText) {
            viewModel.update { s -> s.copy(printingHoursPerDayText = it) }
        }

        Text("Margem", style = MaterialTheme.typography.titleMedium)
        LabeledField("Margem de lucro (%)", state.profitMarginPercentText) {
            viewModel.update { s -> s.copy(profitMarginPercentText = it) }
        }

        Button(onClick = viewModel::save) { Text("Salvar") }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.savedConfirmation) {
            Text("Configurações salvas.", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
    )
}
