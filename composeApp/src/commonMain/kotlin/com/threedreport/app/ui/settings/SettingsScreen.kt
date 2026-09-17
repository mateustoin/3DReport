package com.threedreport.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.Currency
import com.threedreport.core.model.ThemeMode

/**
 * Tela de Configurações gerais: parâmetros do negócio, iguais para qualquer
 * impressora/orçamento (energia, falhas, acabamento, administrativo, margem),
 * e a personalização do PDF exportado (marca d'água).
 * O que é específico de cada impressora fica na tela de Impressoras.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    brandingViewModel: BrandingViewModel,
    themeViewModel: ThemeViewModel,
    currencyViewModel: CurrencyViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val branding by brandingViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Aparência", style = MaterialTheme.typography.titleMedium)
        ThemeModeSelector(selected = themeMode, onSelect = themeViewModel::setMode)

        Text("Moeda", style = MaterialTheme.typography.titleMedium)
        CurrencySelector(selected = currency, onSelect = currencyViewModel::setCurrency)
        Text(
            "Muda o símbolo e o formato dos valores em toda a interface, no PDF exportado e no " +
                "copiar/colar — não afeta o cálculo, só a exibição.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Energia", style = MaterialTheme.typography.titleMedium)
        LabeledField("Preço do kWh (${LocalCurrency.current.symbol})", state.energyPricePerKwhText) {
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
        LabeledField("Custo administrativo por orçamento (${LocalCurrency.current.symbol})", state.administrativeCostText) {
            viewModel.update { s -> s.copy(administrativeCostText = it) }
        }

        Text("Margem", style = MaterialTheme.typography.titleMedium)
        LabeledField("Margem de lucro (%)", state.profitMarginPercentText) {
            viewModel.update { s -> s.copy(profitMarginPercentText = it) }
        }

        Text("Marketplace", style = MaterialTheme.typography.titleMedium)
        LabeledField("Taxa de marketplace (%, ex.: Shopee)", state.marketplaceFeeRatePercentText) {
            viewModel.update { s -> s.copy(marketplaceFeeRatePercentText = it) }
        }
        Text(
            "Marcada por orçamento na aba Orçamento, quando aquela venda for por um marketplace. " +
                "O valor de venda sobe o suficiente pra sua margem de lucro real não mudar.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(onClick = viewModel::save) { Text("Salvar") }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.savedConfirmation) {
            Text("Configurações salvas.", color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()

        Text("Marca d'água do PDF", style = MaterialTheme.typography.titleMedium)
        LabeledField("Texto da marca d'água (opcional)", branding.watermarkTextInput, brandingViewModel::update)

        CheckboxRow("Marca d'água diagonal no PDF", branding.showWatermark, brandingViewModel::setShowWatermark)
        CheckboxRow("Rodapé com o nome no PDF", branding.showFooter, brandingViewModel::setShowFooter)

        Button(onClick = brandingViewModel::save) { Text("Salvar") }

        branding.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (branding.savedConfirmation) {
            Text("Marca d'água salva.", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = ThemeMode.entries
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(mode.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelector(selected: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = "${selected.code} (${selected.symbol})",
            onValueChange = {},
            label = { Text("Moeda") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} (${currency.symbol})") },
                    onClick = { onSelect(currency); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
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
