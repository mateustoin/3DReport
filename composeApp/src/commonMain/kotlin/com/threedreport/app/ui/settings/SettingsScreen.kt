package com.threedreport.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.templates.TemplateListDialog
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.Currency
import com.threedreport.core.model.ThemeMode

/**
 * Tela de Configurações gerais: parâmetros do negócio, iguais para qualquer
 * impressora/orçamento (energia, falhas, acabamento, administrativo, margem),
 * e a personalização do PDF exportado (marca d'água, com biblioteca de
 * templates salvos — ver [TemplateListDialog]).
 * O que é específico de cada impressora fica na tela de Impressoras.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    brandingViewModel: BrandingViewModel,
    templateListViewModel: TemplateListViewModel,
    themeViewModel: ThemeViewModel,
    currencyViewModel: CurrencyViewModel,
    backupViewModel: BackupViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val branding by brandingViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()
    var showTemplatesDialog by remember { mutableStateOf(false) }
    // Lido do rascunho (e não das configurações salvas) pra que o aviso sobre o acabamento mude
    // junto com o que está sendo digitado, não só depois de salvar.
    val chargesLaborByTime = (parseDecimal(state.laborRatePerHourText) ?: 0.0) > 0.0

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

        Text("Seu trabalho", style = MaterialTheme.typography.titleMedium)
        LabeledField("Valor da sua hora de trabalho (${LocalCurrency.current.symbol}/h)", state.laborRatePerHourText) {
            viewModel.update { s -> s.copy(laborRatePerHourText = it) }
        }
        Text(
            "Preparar o arquivo, fatiar, tirar a peça da mesa, remover suporte, lixar, pintar, " +
                "embalar e atender o cliente é trabalho seu, e some do preço se não for cobrado. " +
                "Informe quanto vale a sua hora aqui e, em cada orçamento, quantos minutos aquela " +
                "peça deu de trabalho. Deixe zero pra não cobrar mão de obra.",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Custos fixos do negócio", style = MaterialTheme.typography.titleMedium)
        LabeledField("Custo fixo mensal (${LocalCurrency.current.symbol})", state.monthlyFixedCostText) {
            viewModel.update { s -> s.copy(monthlyFixedCostText = it) }
        }
        LabeledField("Horas de impressão por mês (todas as impressoras)", state.productiveHoursPerMonthText) {
            viewModel.update { s -> s.copy(productiveHoursPerMonthText = it) }
        }
        Text(
            "Aluguel do espaço, internet, assinaturas e embalagem não aparecem em nenhuma peça " +
                "específica, mas você paga todo mês. O valor é dividido pelas horas de impressão " +
                "do mês e cada peça paga a parte dela. Deixe zero pra não usar.",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Falhas e acabamento", style = MaterialTheme.typography.titleMedium)
        LabeledField("Taxa de falhas (%)", state.failureRatePercentText) {
            viewModel.update { s -> s.copy(failureRatePercentText = it) }
        }
        Text(
            "Reserva pra quando uma impressão falha. Incide sobre tudo que você gasta de novo pra " +
                "refazer a peça (material, energia, manutenção, retorno da máquina, custo fixo e " +
                "mão de obra) — só o custo administrativo fica de fora, porque uma modelagem já " +
                "feita não precisa ser refeita.",
            style = MaterialTheme.typography.bodySmall,
        )
        LabeledField("Taxa de acabamento (%)", state.finishingRatePercentText) {
            viewModel.update { s -> s.copy(finishingRatePercentText = it) }
        }
        Text(
            if (chargesLaborByTime) {
                "Sem efeito no momento: com uma hora de trabalho configurada acima, o acabamento " +
                    "passa a ser cobrado pelos minutos informados em cada orçamento, e não mais " +
                    "por este percentual sobre o material."
            } else {
                "Percentual sobre o custo do material. Ele existe pra quem ainda não cobra por " +
                    "hora: assim que você informar o valor da sua hora de trabalho acima, o " +
                    "acabamento passa a ser cobrado por tempo e esta taxa deixa de ter efeito."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (chargesLaborByTime) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )

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

        if (branding.isSavingAsTemplate) {
            LabeledField("Nome do template (ex.: Formal, Simples)", branding.templateNameInput, brandingViewModel::updateTemplateName)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = brandingViewModel::confirmSaveAsTemplate) { Text("Salvar template") }
                TextButton(onClick = brandingViewModel::cancelSaveAsTemplate) { Text("Cancelar") }
            }
            branding.templateSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = brandingViewModel::startSaveAsTemplate) { Text("Salvar como template") }
                TextButton(onClick = { showTemplatesDialog = true }) { Text("Ver templates salvos") }
            }
        }
        if (branding.templateSavedConfirmation) {
            Text("Template salvo.", color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()

        BackupSection(backupViewModel)
    }

    if (showTemplatesDialog) {
        TemplateListDialog(templateListViewModel, onDismiss = { showTemplatesDialog = false })
    }
}

@Composable
private fun BackupSection(viewModel: BackupViewModel) {
    val state by viewModel.uiState.collectAsState()

    Text("Backup", style = MaterialTheme.typography.titleMedium)
    Text(
        "Seus orçamentos, clientes, catálogos, fotos e arquivos STL ficam só neste computador. " +
            "O backup junta tudo isso num arquivo .zip, pra você guardar em outro lugar ou levar " +
            "pra outra máquina.",
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::createBackup) { Text("Fazer backup") }
        TextButton(onClick = viewModel::pickBackupToRestore) { Text("Restaurar backup") }
    }
    state.message?.let { message ->
        Text(
            message,
            color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }

    state.fileNameToRestore?.let { fileName ->
        ConfirmDialog(
            title = "Restaurar este backup?",
            message = "Todos os dados atuais do app (orçamentos, clientes, catálogos, configurações, " +
                "fotos e STLs) serão substituídos pelo conteúdo de \"$fileName\". Uma cópia dos dados " +
                "atuais é guardada automaticamente, e o app será fechado ao final pra carregar os " +
                "dados restaurados.",
            confirmLabel = "Restaurar",
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }

    state.restoredFromPreviousDataAt?.let { previousDataPath ->
        AlertDialog(
            onDismissRequest = viewModel::closeApp,
            title = { Text("Backup restaurado") },
            text = {
                Text(
                    "O app precisa ser fechado agora pra carregar os dados restaurados. " +
                        "Os dados que existiam antes foram guardados em:\n\n$previousDataPath",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::closeApp) { Text("Fechar o app") } },
        )
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
