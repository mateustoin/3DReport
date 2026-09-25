package com.threedreport.app.ui.settings

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.interpretationHint
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.SalesChannel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.templates.TemplateListDialog
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.Currency
import com.threedreport.core.model.ThemeMode
import com.threedreport.core.model.UsageProfile

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
    salesChannelViewModel: SalesChannelViewModel,
    usageProfile: UsageProfile,
    onUsageProfileChange: (UsageProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val savedSettings by viewModel.savedSettings.collectAsState()
    LaunchedEffect(savedSettings) { viewModel.syncWith(savedSettings) }
    val savedBranding by brandingViewModel.savedBranding.collectAsState()
    LaunchedEffect(savedBranding) { brandingViewModel.syncWith(savedBranding) }
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()
    var showTemplatesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(AppIcons.Palette, "Aparência")
        ThemeModeSelector(selected = themeMode, onSelect = themeViewModel::setMode)

        SectionTitle(AppIcons.Badge, "Como você usa o app")
        UsageProfileSelector(selected = usageProfile, onSelect = onUsageProfileChange)
        Text(
            "Define o que já vem escolhido ao salvar (pedido de cliente ou produto do catálogo) e o que " +
                "o Histórico mostra primeiro. Tudo continua disponível nos dois.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle(AppIcons.Payments, "Moeda")
        CurrencySelector(selected = currency, onSelect = currencyViewModel::setCurrency)
        Text(
            "A moeda dos orçamentos novos: símbolo e formato dos valores na tela, no PDF e no " +
                "copiar/colar. Não afeta o cálculo. Cada orçamento salvo continua na moeda em que foi feito.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        SectionTitle(AppIcons.Bolt, "Energia")
        NumberField("Preço do kWh (${LocalCurrency.current.symbol})", state.energyPricePerKwhText, NumberKind.MEASURE) {
            viewModel.update { s -> s.copy(energyPricePerKwhText = it) }
        }

        SectionTitle(AppIcons.Schedule, "Seu trabalho")
        NumberField("Valor da sua hora de trabalho (${LocalCurrency.current.symbol}/h)", state.laborRatePerHourText, NumberKind.AMOUNT) {
            viewModel.update { s -> s.copy(laborRatePerHourText = it) }
        }
        Text(
            "Cobrado pelos minutos que você informa em cada orçamento (tirar da mesa, remover " +
                "suporte, lixar, pintar, embalar). Só soma ao preço. Deixe zero pra não cobrar mão " +
                "de obra.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle(AppIcons.Storefront, "Custos fixos do negócio")
        NumberField("Custo fixo mensal (${LocalCurrency.current.symbol})", state.monthlyFixedCostText, NumberKind.AMOUNT) {
            viewModel.update { s -> s.copy(monthlyFixedCostText = it) }
        }
        NumberField("Horas de impressão por mês (todas as impressoras)", state.productiveHoursPerMonthText, NumberKind.AMOUNT) {
            viewModel.update { s -> s.copy(productiveHoursPerMonthText = it) }
        }
        Text(
            "Aluguel do espaço, internet, assinaturas e embalagem não aparecem em nenhuma peça " +
                "específica, mas você paga todo mês. O valor é dividido pelas horas de impressão " +
                "do mês e cada peça paga a parte dela. Deixe zero pra não usar.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle(AppIcons.Build, "Falhas e acabamento")
        NumberField("Taxa de falhas (%)", state.failureRatePercentText, NumberKind.MEASURE) {
            viewModel.update { s -> s.copy(failureRatePercentText = it) }
        }
        Text(
            "Reserva pra quando uma impressão falha. Incide sobre tudo que você gasta de novo pra " +
                "refazer a peça (material, energia, manutenção, retorno da máquina, custo fixo, " +
                "mão de obra e acabamento). Só o custo administrativo fica de fora, porque uma " +
                "modelagem já feita não precisa ser refeita.",
            style = MaterialTheme.typography.bodySmall,
        )
        NumberField("Taxa de acabamento (%)", state.finishingRatePercentText, NumberKind.MEASURE) {
            viewModel.update { s -> s.copy(finishingRatePercentText = it) }
        }
        Text(
            "Percentual do material pra lixar e pintar. Se você já conta esse tempo nos minutos de " +
                "cada orçamento, deixe 0 pra não cobrar duas vezes.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle(AppIcons.ReceiptLong, "Custos administrativos")
        NumberField("Custo administrativo por orçamento (${LocalCurrency.current.symbol})", state.administrativeCostText, NumberKind.AMOUNT) {
            viewModel.update { s -> s.copy(administrativeCostText = it) }
        }

        SectionTitle(AppIcons.TrendingUp, "Margem")
        NumberField("Margem de lucro (%)", state.profitMarginPercentText, NumberKind.MEASURE) {
            viewModel.update { s -> s.copy(profitMarginPercentText = it) }
        }

        SectionTitle(AppIcons.AccountBalance, "Imposto")
        NumberField("Imposto sobre a venda (%)", state.taxRatePercentText, NumberKind.MEASURE) {
            viewModel.update { s -> s.copy(taxRatePercentText = it) }
        }
        Text(
            "Percentual que sai da venda, como o Simples Nacional. Se você é MEI, deixe zero aqui: " +
                "o DAS é um valor fixo por mês, então o lugar dele é o custo fixo mensal, logo acima.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(onClick = viewModel::save) { Text("Salvar") }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.warningMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium) }
        ShowSnackbarOnce(state.savedConfirmation, "Configurações salvas.", viewModel::consumeSavedConfirmation)

        HorizontalDivider()

        ClientDocumentsSection(brandingViewModel, onShowTemplates = { showTemplatesDialog = true })

        HorizontalDivider()

        SalesChannelSection(salesChannelViewModel)

        HorizontalDivider()

        BackupSection(backupViewModel)
    }

    if (showTemplatesDialog) {
        TemplateListDialog(templateListViewModel, onLoad = brandingViewModel::applyTemplate, onDismiss = { showTemplatesDialog = false })
    }
}

@Composable
private fun SalesChannelSection(viewModel: SalesChannelViewModel) {
    val channels by viewModel.channels.collectAsState()
    val form by viewModel.form.collectAsState()

    SectionTitle(AppIcons.ShoppingBag, "Canais de venda")
    Text(
        "Onde a venda acontece e quanto isso desconta do que você recebe: Shopee, Mercado Livre, " +
            "cartão, Pix. Em cada orçamento você escolhe o canal, e o preço sobe o suficiente pra " +
            "sua margem não mudar. Marketplace e forma de pagamento entram aqui juntos de propósito: " +
            "somar a taxa da Shopee com a do cartão cobraria em dobro, porque o marketplace já " +
            "embute o processamento do pagamento.",
        style = MaterialTheme.typography.bodySmall,
    )

    var pendingDelete by remember { mutableStateOf<SalesChannel?>(null) }
    channels.forEach { channel ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${channel.name} · ${channel.feeRate.toPercentText()}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { viewModel.startEditing(channel) }) { Text("Editar") }
            TextButton(onClick = { pendingDelete = channel }) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    pendingDelete?.let { channel ->
        ConfirmDialog(
            title = "Excluir o canal \"${channel.name}\"?",
            message = "Orçamentos já salvos continuam com a taxa que tinham. Produtos do catálogo desse canal " +
                "passam a pedir um canal novo ao atualizar o preço.",
            onConfirm = {
                viewModel.delete(channel.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
    if (channels.isEmpty()) {
        Text(
            "Nenhum canal cadastrado: todo orçamento sai como venda direta, sem taxa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    LabeledField("Nome do canal (ex.: Shopee, Cartão)", form.nameText, viewModel::setName)
    NumberField("Taxa do canal (%)", form.feeRatePercentText, NumberKind.MEASURE, viewModel::setFeeRate)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::save) { Text(if (form.editingId != null) "Salvar canal" else "Adicionar canal") }
        if (form.editingId != null) {
            TextButton(onClick = viewModel::cancelEditing) { Text("Cancelar") }
        }
    }
    form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun BackupSection(viewModel: BackupViewModel) {
    val state by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    SectionTitle(AppIcons.Inventory2, "Backup")
    Text(
        "Seus orçamentos, clientes, catálogos, fotos e arquivos STL ficam só neste computador. " +
            "O backup junta tudo isso num arquivo .zip, pra você guardar em outro lugar ou levar " +
            "pra outra máquina.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        preferences.lastBackupEpochMillis?.let { "Último backup: ${formatDateTime(it)}" }
            ?: "Nenhum backup feito neste computador ainda.",
        style = MaterialTheme.typography.bodyMedium,
        color = if (preferences.lastBackupEpochMillis == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = viewModel::createBackup, enabled = !state.busy) { Text("Fazer backup agora") }
        TextButton(onClick = viewModel::pickBackupToRestore, enabled = !state.busy) { Text("Restaurar backup") }
        if (state.busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Trabalhando…", style = MaterialTheme.typography.bodySmall)
        }
    }
    state.message?.takeIf { state.isError }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    ShowSnackbarOnce(state.message != null && !state.isError, state.message.orEmpty(), viewModel::consumeMessage)
    if (state.savedPath != null && !state.isError) {
        TextButton(onClick = viewModel::openSavedBackupFolder) { Text("Abrir a pasta do backup") }
    }

    CheckboxRow(
        "Fazer um backup por dia ao abrir o app (guarda os últimos 7)",
        checked = preferences.autoBackupEnabled,
        onCheckedChange = viewModel::setAutomaticBackup,
    )
    if (preferences.autoBackupEnabled) {
        SelectionContainer {
            Text("Pasta: ${viewModel.automaticBackupDirectory}", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = viewModel::chooseAutomaticBackupDirectory) { Text("Trocar pasta") }
            TextButton(onClick = viewModel::openAutomaticBackupDirectory) { Text("Abrir pasta") }
        }
        Text(
            "Dica: escolha uma pasta do Google Drive, do OneDrive ou do Dropbox, e a cópia vai pra nuvem sozinha.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    state.pathToRestore?.let { path ->
        ConfirmDialog(
            title = "Restaurar este backup?",
            message = "Todos os dados atuais do app (orçamentos, clientes, catálogos, configurações, " +
                "fotos e STLs) serão substituídos pelo conteúdo de \"${path.substringAfterLast('/').substringAfterLast('\\')}\". " +
                "Uma cópia dos dados atuais é guardada automaticamente, e o app será fechado ao final pra " +
                "carregar os dados restaurados.",
            confirmLabel = "Restaurar",
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }

    state.restoredFromPreviousDataAt?.let { previousDataPath ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
            title = { Text("Backup restaurado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O app precisa ser fechado agora pra carregar os dados restaurados. Os dados que existiam antes foram guardados em:")
                    SelectionContainer { Text(previousDataPath, style = MaterialTheme.typography.bodyMedium) }
                }
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
private fun UsageProfileSelector(selected: UsageProfile, onSelect: (UsageProfile) -> Unit) {
    val options = UsageProfile.entries
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, profile ->
            SegmentedButton(
                selected = profile == selected,
                onClick = { onSelect(profile) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(profile.label)
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
internal fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

/** Campo de texto de uma linha com rótulo. */
@Composable
internal fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
    )
}

/**
 * Campo de número (decisão 107). Embaixo dele aparece como o app entendeu o que foi digitado quando há
 * como ler de dois jeitos ("2.700" → "= 2.700"), e um aviso quando não é número, em vez de o valor virar
 * zero ou outro número em silêncio.
 */
@Composable
internal fun NumberField(label: String, value: String, kind: NumberKind, onValueChange: (String) -> Unit) {
    val separator = LocalCurrency.current.decimalSeparator
    val invalid = value.isNotBlank() && parseDecimal(value, kind, separator) == null
    val hint = interpretationHint(value, kind, separator)
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = invalid,
        supportingText = when {
            invalid -> ({ Text("Não é um número. Use vírgula nos centavos: 1.250,50") })
            hint != null -> ({ Text(hint) })
            else -> null
        },
    )
}
