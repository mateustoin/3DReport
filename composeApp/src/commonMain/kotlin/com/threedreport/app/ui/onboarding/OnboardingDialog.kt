package com.threedreport.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.printers.PRINTER_PRESETS
import com.threedreport.app.ui.printers.PrinterPreset
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.UsageProfile

/**
 * A impressora respondida no onboarding, aplicada na impressora de exemplo (a que o primeiro orçamento
 * usa): o modelo troca nome e consumo, o valor troca o preço da máquina. `null` em cada um mantém o do
 * exemplo.
 */
data class OnboardingPrinter(val preset: PrinterPreset?, val machinePrice: Double?)

/**
 * Cinco perguntas na primeira execução, pra o app não abrir com a conta calibrada pra outra pessoa.
 *
 * A primeira é como a pessoa usa o app (decisão 103): quem ainda não vende começa salvando no
 * catálogo e vendo Produtos primeiro no Histórico. Só muda o que vem escolhido; nada some.
 *
 * Depois, os números que mais mudam o preço e que ninguém adivinha por padrão: a impressora e quanto
 * ela custou (decisão 108: o exemplo de R$ 2.700 e 380 W errava pra quase todo mundo e ia direto pro
 * primeiro orçamento), energia, o valor da própria hora (que nasce zerado e, sem preencher, faz o
 * trabalho sumir do preço) e a margem. Filamento fica de fora: o cadastro padrão já é utilizável e a tela
 * tem catálogo pronto.
 *
 * Dá pra pular: quem só quer ver o app funcionando não deveria ser barrado por um formulário. Clicar fora
 * não fecha: sem querer, a pessoa perderia o que digitou e o onboarding não voltaria.
 */
@Composable
fun OnboardingDialog(
    currentSettings: PricingSettings,
    onFinish: (PricingSettings, UsageProfile, OnboardingPrinter?) -> Unit,
    onSkip: () -> Unit,
) {
    val currency = LocalCurrency.current
    var energyText by remember { mutableStateOf(currentSettings.energyPricePerKwh.toInputText()) }
    var laborText by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(UsageProfile.SELLER) }
    var marginText by remember { mutableStateOf((currentSettings.profitMargin * 100).toInputText()) }
    var preset by remember { mutableStateOf<PrinterPreset?>(null) }
    var machinePriceText by remember { mutableStateOf("") }

    val energy = parseDecimal(energyText)
    val labor = if (laborText.isBlank()) 0.0 else parseDecimal(laborText)
    val margin = parseDecimal(marginText)
    val machinePrice = if (machinePriceText.isBlank()) null else parseDecimal(machinePriceText)
    val energyError = when {
        energy == null -> "Não é um número."
        energy < 0 -> "Não pode ser negativo."
        else -> null
    }
    val laborError = when {
        labor == null -> "Não é um número."
        labor < 0 -> "Não pode ser negativo."
        else -> null
    }
    val marginError = when {
        margin == null -> "Não é um número."
        margin < 0 -> "Não pode ser negativa."
        else -> null
    }
    val machinePriceError = when {
        machinePriceText.isNotBlank() && machinePrice == null -> "Não é um número."
        machinePrice != null && machinePrice < 0 -> "Não pode ser negativo."
        else -> null
    }
    val valid = listOf(energyError, laborError, marginError, machinePriceError).all { it == null }

    AlertDialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Bem-vindo ao 3DReport") },
        text = {
            // Rola: em tela de notebook (768 px de altura) as perguntas não cabem inteiras.
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Cinco perguntas rápidas pra o app sair do jeito do seu negócio. Dá pra mudar " +
                        "tudo depois em Configurações e Impressoras.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text("Como você vai usar o 3DReport?", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    UsageProfile.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = option == profile, onClick = { profile = option }, role = Role.RadioButton)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == profile, onClick = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Text("Qual a sua impressora?", style = MaterialTheme.typography.titleSmall)
                PresetDropdown(selected = preset, onSelect = { preset = it })
                OnboardingField(
                    label = "Quanto ela custou (${currency.symbol}, opcional)",
                    value = machinePriceText,
                    error = machinePriceError,
                    help = "A máquina se paga aos poucos: uma parte do valor entra em cada orçamento.",
                ) { machinePriceText = it }

                OnboardingField(
                    label = "Preço do kWh (${currency.symbol})",
                    value = energyText,
                    error = energyError,
                    help = "Está na sua conta de luz, na linha de consumo. Varia por estado e por distribuidora.",
                ) { energyText = it }

                OnboardingField(
                    label = "Valor da sua hora de trabalho (${currency.symbol}/h)",
                    value = laborText,
                    error = laborError,
                    help = "Preparar o arquivo, tirar da mesa, remover suporte, lixar, pintar, embalar. É o custo que " +
                        "mais some da conta de quem vende impressão 3D. Deixe em branco pra não cobrar por enquanto.",
                ) { laborText = it }

                OnboardingField(
                    label = "Margem de lucro (%)",
                    value = marginText,
                    error = marginError,
                    help = "Quanto você quer ganhar em cima do custo. 100% significa cobrar o dobro do que a peça custou pra produzir.",
                ) { marginText = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onFinish(
                        currentSettings.copy(
                            energyPricePerKwh = energy ?: currentSettings.energyPricePerKwh,
                            laborRatePerHour = labor ?: 0.0,
                            profitMargin = (margin ?: (currentSettings.profitMargin * 100)) / 100.0,
                        ),
                        profile,
                        OnboardingPrinter(preset, machinePrice).takeIf { preset != null || machinePrice != null },
                    )
                },
            ) { Text("Começar") }
        },
        dismissButton = { TextButton(onClick = onSkip) { Text("Pular") } },
    )
}

@Composable
private fun OnboardingField(label: String, value: String, error: String?, help: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { Text(error ?: help) },
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdown(selected: PrinterPreset?, onSelect: (PrinterPreset?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).tabToNavigate(),
            readOnly = true,
            value = selected?.let { "${it.brand} ${it.model}" } ?: "Outra, ou escolho depois",
            onValueChange = {},
            label = { Text("Modelo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = {
                Text(
                    if (selected != null) {
                        "Consumo: ${selected.ratedPowerWatts.toInputText()} W, a potência máxima do fabricante. Se medir o consumo real, ajuste em Impressoras."
                    } else {
                        "Sem modelo, o app usa a impressora de exemplo. Dá pra cadastrar a sua em Impressoras."
                    },
                )
            },
            singleLine = true,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Outra, ou escolho depois") }, onClick = { onSelect(null); expanded = false })
            PRINTER_PRESETS.forEach { preset ->
                DropdownMenuItem(text = { Text("${preset.brand} ${preset.model}") }, onClick = { onSelect(preset); expanded = false })
            }
        }
    }
}
