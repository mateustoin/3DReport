package com.threedreport.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.focus.tabToNavigate

/**
 * Tela de Configurações gerais: parâmetros do negócio, iguais para qualquer
 * impressora/orçamento (energia, falhas, acabamento, administrativo, margem),
 * e a personalização do PDF exportado (marca d'água).
 * O que é específico de cada impressora fica na tela de Impressoras.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, brandingViewModel: BrandingViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val branding by brandingViewModel.uiState.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Energia", style = MaterialTheme.typography.titleMedium)
        LabeledField("Preço do kWh (R$)", state.energyPricePerKwhText) {
            viewModel.update { s -> s.copy(energyPricePerKwhText = it) }
        }

        Text("Falhas e acabamento", style = MaterialTheme.typography.titleMedium)
        LabeledField("Taxa de falhas (%)", state.failureRatePercentText) {
            viewModel.update { s -> s.copy(failureRatePercentText = it) }
        }
        LabeledField("Taxa de acabamento (%)", state.finishingRatePercentText) {
            viewModel.update { s -> s.copy(finishingRatePercentText = it) }
        }

        Text("Custos administrativos", style = MaterialTheme.typography.titleMedium)
        LabeledField("Custo administrativo por orçamento (R$)", state.administrativeCostText) {
            viewModel.update { s -> s.copy(administrativeCostText = it) }
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

        HorizontalDivider()

        Text("Marca d'água do PDF", style = MaterialTheme.typography.titleMedium)
        LabeledField("Texto da marca d'água (opcional)", branding.watermarkTextInput, brandingViewModel::update)

        Button(onClick = brandingViewModel::save) { Text("Salvar") }

        if (branding.savedConfirmation) {
            Text("Marca d'água salva.", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
    )
}
