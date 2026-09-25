package com.threedreport.app

import androidx.compose.foundation.layout.Arrangement
import com.threedreport.app.platform.fileDropTarget
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.platform.openFolder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.LocalSnackbarHostState
import com.threedreport.app.ui.dashboard.DashboardScreen
import com.threedreport.app.ui.dashboard.DashboardViewModel
import com.threedreport.app.ui.filaments.FilamentListScreen
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.history.QuoteHistoryScreen
import com.threedreport.app.ui.history.QuoteHistoryViewModel
import com.threedreport.app.ui.onboarding.OnboardingDialog
import com.threedreport.app.ui.onboarding.OnboardingPrinter
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.ui.printers.PrinterListScreen
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.EditQuoteDialog
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListScreen
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.app.ui.settings.BackupViewModel
import com.threedreport.app.ui.settings.BrandingViewModel
import com.threedreport.app.ui.settings.CurrencyViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SalesChannelViewModel
import com.threedreport.app.ui.settings.SettingsViewModel
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.app.ui.theme.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val GITHUB_URL = "https://github.com/mateustoin/3DReport"
private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/mateustoin"
private const val AUTHOR_NAME = "Mateus Antonio da Silva"

private enum class AppTab(val label: String) {
    QUOTE("Orçamento"),
    HISTORY("Histórico"),
    DASHBOARD("Dashboard"),
    FILAMENTS("Filamentos"),
    PRINTERS("Impressoras"),
    SERVICES("Serviços"),
    SETTINGS("Configurações");

    /**
     * Ícone contornado das abas inativas e o preenchido da aba ativa (padrão do Material 3). Por
     * `when`, e não no construtor, pra nenhum vetor ser montado antes de a barra aparecer.
     */
    val icon: ImageVector
        get() = when (this) {
            QUOTE -> AppIcons.RequestQuote
            HISTORY -> AppIcons.History
            DASHBOARD -> AppIcons.BarChart
            FILAMENTS -> AppIcons.Spool
            PRINTERS -> AppIcons.Printer3d
            SERVICES -> AppIcons.Handyman
            SETTINGS -> AppIcons.Settings
        }

    val selectedIcon: ImageVector
        get() = when (this) {
            QUOTE -> AppIcons.RequestQuoteFilled
            HISTORY -> AppIcons.HistoryFilled
            DASHBOARD -> AppIcons.BarChartFilled
            FILAMENTS -> AppIcons.SpoolFilled
            PRINTERS -> AppIcons.Printer3dFilled
            SERVICES -> AppIcons.HandymanFilled
            SETTINGS -> AppIcons.SettingsFilled
        }
}

@Composable
fun App(container: AppContainer, dataFolderNotice: DataFolderNotice? = null) {
    val filamentRepository = container.filaments
    val printerRepository = container.printers
    val settingsRepository = container.settings
    val historyRepository = container.quoteHistory
    val onboardingRepository = container.onboarding

    val appScope = rememberCoroutineScope()
    val newQuoteViewModel = {
        QuoteViewModel(
            filamentRepository, printerRepository, settingsRepository, container.services,
            container.salesChannels, historyRepository,
            clientRepository = container.clients,
            currency = { container.currency.currency.value },
            scope = appScope,
            background = Dispatchers.Default,
            main = Dispatchers.Main,
        )
    }
    val quoteViewModel = remember { newQuoteViewModel() }
    // Editar um orçamento salvo tem o próprio ViewModel (decisão 108): usar o da aba apagava o rascunho
    // que estivesse em andamento lá.
    val editQuoteViewModel = remember { newQuoteViewModel() }
    val historyViewModel = remember {
        QuoteHistoryViewModel(
            historyRepository, container.branding,
            filamentRepository, printerRepository, settingsRepository, container.salesChannels,
            clientRepository = container.clients,
            scope = appScope,
            background = Dispatchers.Default,
            main = Dispatchers.Main,
        )
    }
    val dashboardViewModel = remember { DashboardViewModel(historyRepository, settingsRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository, historyRepository, container.maintenance) }
    val serviceListViewModel = remember { ServiceListViewModel(container.services) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val brandingViewModel = remember {
        BrandingViewModel(container.branding, container.templates, currency = { container.currency.currency.value })
    }
    val templateListViewModel = remember { TemplateListViewModel(container.templates, container.branding) }
    val themeViewModel = remember { ThemeViewModel(container.theme) }
    val currencyViewModel = remember { CurrencyViewModel(container.currency) }
    val backupViewModel = remember { BackupViewModel(container.backup, container.preferences, container.pendingWrites, scope = appScope) }
    val salesChannelViewModel = remember { SalesChannelViewModel(container.salesChannels) }

    val onboardingCompleted by onboardingRepository.completed.collectAsState()
    var selectedTab by remember { mutableStateOf(AppTab.QUOTE) }
    var draggingFile by remember { mutableStateOf(false) }
    // Ação que jogaria fora o orçamento em andamento na aba, esperando a confirmação.
    var pendingDiscard by remember { mutableStateOf<(() -> Unit)?>(null) }
    val unlessDraft: (() -> Unit) -> Unit = { action -> if (quoteViewModel.hasDraft) pendingDiscard = action else action() }
    var showHelp by remember { mutableStateOf(false) }
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    AppTheme(themeMode) {
        CompositionLocalProvider(LocalCurrency provides currency, LocalSnackbarHostState provides snackbarHostState) {
            Surface(
                modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val accel = event.isCtrlPressed || event.isMetaPressed

                    val tabForKey = when {
                        accel && event.key == Key.One -> AppTab.QUOTE
                        accel && event.key == Key.Two -> AppTab.HISTORY
                        accel && event.key == Key.Three -> AppTab.DASHBOARD
                        accel && event.key == Key.Four -> AppTab.FILAMENTS
                        accel && event.key == Key.Five -> AppTab.PRINTERS
                        accel && event.key == Key.Six -> AppTab.SERVICES
                        accel && event.key == Key.Seven -> AppTab.SETTINGS
                        else -> null
                    }
                    if (tabForKey != null) {
                        selectedTab = tabForKey
                        return@onPreviewKeyEvent true
                    }

                    when {
                        accel && event.key == Key.S && selectedTab == AppTab.QUOTE -> {
                            quoteViewModel.saveCurrentQuote()
                            true
                        }
                        accel && event.key == Key.N && selectedTab == AppTab.QUOTE -> {
                            unlessDraft(quoteViewModel::resetForm)
                            true
                        }
                        // Esc fecha o formulário da aba que está na tela, e só ele: antes fechava os de
                        // todas as abas, inclusive um que a pessoa nem estava vendo.
                        event.key == Key.Escape -> when (selectedTab) {
                            AppTab.FILAMENTS -> filamentListViewModel.form.value?.let { filamentListViewModel.cancelEdit(); true } ?: false
                            AppTab.PRINTERS -> printerListViewModel.form.value?.let { printerListViewModel.cancelEdit(); true } ?: false
                            AppTab.SERVICES -> serviceListViewModel.form.value?.let { serviceListViewModel.cancelEdit(); true } ?: false
                            else -> false
                        }
                        else -> false
                    }
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().fileDropTarget(
                        onDragActive = { draggingFile = it },
                        onDrop = { dropped ->
                            // Seja qual for a aba aberta, o G-code vai pro Orçamento, que é onde o resultado aparece.
                            selectedTab = AppTab.QUOTE
                            quoteViewModel.importDropped(dropped)
                        },
                    ),
                ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AppTabBar(selected = selectedTab, onSelect = { selectedTab = it })

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            AppTab.QUOTE -> QuoteScreen(
                                quoteViewModel,
                                // Vender, Duplicar e Guardar no catálogo começam no Histórico:
                                // desistir devolve a pessoa pra lá, com o formulário limpo.
                                onCancelOperation = {
                                    quoteViewModel.resetForm()
                                    selectedTab = AppTab.HISTORY
                                },
                            )
                            AppTab.HISTORY -> QuoteHistoryScreen(
                                historyViewModel,
                                onEditQuote = { savedQuote -> editQuoteViewModel.loadForEditing(savedQuote) },
                                // As três começam um orçamento na aba: com um rascunho lá, pergunta antes.
                                onDuplicateQuote = { savedQuote ->
                                    unlessDraft {
                                        quoteViewModel.duplicateForNewQuote(savedQuote)
                                        selectedTab = AppTab.QUOTE
                                    }
                                },
                                onSellProduct = { product ->
                                    unlessDraft {
                                        quoteViewModel.sellFromProduct(product)
                                        selectedTab = AppTab.QUOTE
                                    }
                                },
                                onCopyToCatalog = { order ->
                                    unlessDraft {
                                        quoteViewModel.copyToCatalog(order)
                                        selectedTab = AppTab.QUOTE
                                    }
                                },
                            )
                            AppTab.DASHBOARD -> DashboardScreen(dashboardViewModel)
                            AppTab.FILAMENTS -> FilamentListScreen(filamentListViewModel)
                            AppTab.PRINTERS -> PrinterListScreen(printerListViewModel)
                            AppTab.SERVICES -> ServiceListScreen(serviceListViewModel)
                            AppTab.SETTINGS -> SettingsScreen(
                                settingsViewModel,
                                brandingViewModel,
                                templateListViewModel,
                                themeViewModel,
                                currencyViewModel,
                                backupViewModel,
                                salesChannelViewModel,
                            )
                        }
                    }

                    AppFooter(onHelpClick = { showHelp = true })
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
                )

                if (draggingFile) GCodeDropOverlay()
                }
            }

            if (showHelp) {
                HelpDialog(onDismiss = { showHelp = false })
            }

            var showOldDataNotice by remember { mutableStateOf(dataFolderNotice != null) }
            if (showOldDataNotice && dataFolderNotice != null) {
                DataFolderNoticeDialog(dataFolderNotice, onDismiss = { showOldDataNotice = false })
            }

            StorageHealthDialogs(container)

            // O aviso dos dados antigos vem antes: o onboarding só aparece depois de ele ser lido.
            if (!onboardingCompleted && !showOldDataNotice) {
                OnboardingDialog(
                    currentSettings = settingsRepository.settings.value,
                    onFinish = { settings, printer ->
                        settingsRepository.update(settings)
                        printer?.let { applyOnboardingPrinter(printerRepository, it) }
                        onboardingRepository.markCompleted()
                    },
                    onSkip = onboardingRepository::markCompleted,
                )
            }

            val editForm by editQuoteViewModel.saveForm.collectAsState()
            if (editForm.editingQuoteId != null) {
                EditQuoteDialog(
                    viewModel = editQuoteViewModel,
                    onDismiss = editQuoteViewModel::resetForm,
                    onSaved = { appScope.launch { snackbarHostState.showSnackbar("Alterações salvas.") } },
                )
            }

            pendingDiscard?.let { action ->
                ConfirmDialog(
                    title = "Descartar o orçamento em andamento?",
                    message = "O que está preenchido na aba Orçamento ainda não foi salvo e vai ser substituído.",
                    confirmLabel = "Descartar",
                    onConfirm = {
                        pendingDiscard = null
                        action()
                    },
                    onDismiss = { pendingDiscard = null },
                )
            }
        }
    }
}

/**
 * A impressora respondida no onboarding vai pra primeira impressora cadastrada (a de exemplo, numa
 * instalação nova): o modelo troca nome e consumo, o valor troca o preço da máquina.
 */
private fun applyOnboardingPrinter(repository: PrinterRepository, choice: OnboardingPrinter) {
    val printer = repository.printers.value.firstOrNull() ?: return
    val preset = choice.preset
    repository.update(
        printer.copy(
            name = preset?.let { "${it.brand} ${it.model}" } ?: printer.name,
            printerPowerWatts = preset?.ratedPowerWatts ?: printer.printerPowerWatts,
            machineInvestment = choice.machinePrice?.let { printer.machineInvestment.copy(machinePrice = it) } ?: printer.machineInvestment,
        ),
    )
}

/** Rodapé fixo em toda tela: versão, autor, link do projeto e apoio via doação. */
@Composable
private fun AppFooter(onHelpClick: () -> Unit) {
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("3DReport v$APP_VERSION", style = MaterialTheme.typography.bodySmall)
        Text("·", style = MaterialTheme.typography.bodySmall)
        Text(AUTHOR_NAME, style = MaterialTheme.typography.bodySmall)
        Text("·", style = MaterialTheme.typography.bodySmall)
        LinkText(text = "GitHub", url = GITHUB_URL)
        Text("·", style = MaterialTheme.typography.bodySmall)
        LinkText(text = "☕ Apoie no Buy Me a Coffee", url = BUY_ME_A_COFFEE_URL)
        Text("·", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onHelpClick) { Text("Ajuda") }
    }
}

/**
 * Aviso único sobre a pasta de dados ao abrir (decisões 104 e 106): dados de outro formato guardados à
 * parte ou convertidos. O caminho pode ser selecionado e copiado, e o aviso não some com um clique fora,
 * porque é a única vez que ele aparece.
 */
@Composable
private fun DataFolderNoticeDialog(notice: DataFolderNotice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false),
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi") } },
        dismissButton = { TextButton(onClick = { openFolder(notice.path) }) { Text("Abrir pasta") } },
        title = {
            Text(
                when (notice.kind) {
                    DataFolderNotice.Kind.MOVED_FROM_NEWER -> "Dados de uma versão mais nova"
                    DataFolderNotice.Kind.MIGRATED -> "Seus dados foram atualizados"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (notice.kind) {
                        DataFolderNotice.Kind.MOVED_FROM_NEWER ->
                            "Os dados eram de uma versão mais nova do 3DReport, que esta não consegue ler. Pra não " +
                                "estragar nada, esta versão começa com os dados em branco."
                        DataFolderNotice.Kind.MIGRATED ->
                            "Esta versão guarda os dados de um jeito novo, e os seus foram convertidos. Está tudo aqui."
                    },
                )
                Text(
                    if (notice.kind == DataFolderNotice.Kind.MIGRATED) {
                        "Uma cópia de como estava antes ficou guardada, sem mudar nada, em:"
                    } else {
                        "O que você tinha foi guardado, sem apagar nada, em:"
                    },
                )
                SelectionContainer {
                    Text(notice.path, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Text(
                    when (notice.kind) {
                        DataFolderNotice.Kind.MOVED_FROM_NEWER -> "Pra recuperar, atualize o 3DReport e renomeie essa pasta de volta pra .3dreport."
                        DataFolderNotice.Kind.MIGRATED -> "Depois de conferir que está tudo certo, dá pra apagar essa cópia."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

/**
 * Avisos do armazenamento (decisão 108). Um arquivo de dados que não abriu foi guardado à parte (e,
 * quando deu, os dados vieram da cópia anterior); uma gravação que falhou fica numa faixa que não some
 * até dar certo, porque o que mudou está só na memória.
 */
@Composable
private fun StorageHealthDialogs(container: AppContainer) {
    val unreadable by container.storageHealth.unreadableFiles.collectAsState()
    val writeFailures by container.storageHealth.writeFailures.collectAsState()

    if (unreadable.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = { TextButton(onClick = container.storageHealth::dismissUnreadable) { Text("Entendi") } },
            title = { Text("Um arquivo de dados não abriu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unreadable.forEach { file ->
                        Text(
                            if (file.recoveredFromBackup) {
                                "${file.name}: recuperado da cópia anterior. Só a última mudança antes de fechar pode ter se perdido."
                            } else {
                                "${file.name}: não havia cópia anterior que abrisse, e ele começou vazio. Restaure um backup em " +
                                    "Configurações → Backup pra recuperar."
                            },
                        )
                    }
                    Text("Nada foi apagado. O arquivo com problema ficou guardado em:", style = MaterialTheme.typography.bodySmall)
                    SelectionContainer {
                        Column { unreadable.forEach { Text(it.keptAs, style = MaterialTheme.typography.bodySmall) } }
                    }
                }
            },
        )
    }

    if (writeFailures.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = { TextButton(onClick = container.pendingWrites::retry) { Text("Tentar de novo") } },
            title = { Text("Não consegui gravar suas mudanças") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "O que você mudou está só na memória por enquanto. Feche programas que possam estar usando a " +
                            "pasta de dados (antivírus, OneDrive, backup), confira se o disco tem espaço e tente de novo.",
                    )
                    writeFailures.forEach { (file, reason) -> Text("• $file: $reason", style = MaterialTheme.typography.bodySmall) }
                }
            },
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        title = { Text("3DReport v$APP_VERSION") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Aplicativo gratuito e de código aberto para orçamentos de impressão 3D.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "• Orçamento: arraste o G-code pra janela (ou use \"Escolher arquivo\") e peso, tempo, foto, " +
                        "impressora e filamento vêm preenchidos. Escolha se é pedido de cliente ou produto do catálogo, " +
                        "o canal de venda e os serviços, e salve.",
                )
                Text(
                    "• Histórico: pedidos e produtos salvos, em lista ou Kanban. Exporte PDF, imagem pro WhatsApp " +
                        "ou o texto; \"Ações\" tem mover de etapa, editar detalhes, duplicar e vender.",
                )
                Text("• Dashboard: vendas pela data em que o cliente fechou, lucro por hora e o que mais vende.")
                Text("• Filamentos, Impressoras e Serviços: seus cadastros, usados na tela de Orçamento.")
                Text(
                    "• Configurações: custos (energia, sua hora, margem, imposto), canais de venda com a taxa de cada " +
                        "um, documentos pro cliente (marca, logo, contato), tema, moeda e backup " +
                        "(com cópia automática diária).",
                )
                Text("Atalhos de teclado", style = MaterialTheme.typography.titleSmall)
                Text("• Ctrl/Cmd+1 a 7: pula direto para cada aba, nessa ordem.")
                Text("• Ctrl/Cmd+S: salva o orçamento (na aba Orçamento e na edição).")
                Text("• Ctrl/Cmd+N: começa um orçamento novo (pergunta antes se houver algo preenchido).")
                Text("• Esc: fecha o formulário aberto na aba (Filamentos, Impressoras, Serviços) ou a edição.")
                LinkText(text = "Ver código-fonte no GitHub", url = GITHUB_URL)
                LinkText(text = "☕ Apoiar o projeto no Buy Me a Coffee", url = BUY_ME_A_COFFEE_URL)
            }
        },
    )
}

/**
 * Abaixo disso, as sete abas não cabem com ícone sem quebrar o rótulo no meio da palavra
 * ("Orçament/o"), e a barra volta a ser só texto. O texto é o principal e o ícone é apoio
 * (decisão 87), então é o ícone que sai quando falta espaço.
 */
private val TAB_ICONS_MIN_WIDTH = 1160.dp

@Composable
private fun AppTabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showIcons = maxWidth >= TAB_ICONS_MIN_WIDTH
        PrimaryTabRow(selectedTabIndex = selected.ordinal) {
            AppTab.entries.forEach { tab ->
                if (showIcons) {
                    // Ícone à esquerda, e não em cima, pra barra continuar com 48 dp de altura.
                    LeadingIconTab(
                        selected = selected == tab,
                        onClick = { onSelect(tab) },
                        text = { Text(tab.label, maxLines = 1) },
                        icon = {
                            Icon(
                                if (selected == tab) tab.selectedIcon else tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                } else {
                    Tab(selected = selected == tab, onClick = { onSelect(tab) }, text = { Text(tab.label) })
                }
            }
        }
    }
}

/**
 * Aviso por cima da janela enquanto um arquivo é arrastado: diz onde soltar e o que vai acontecer.
 * Sem ele, arrastar um G-code pra janela não dá nenhum sinal de que funciona até soltar.
 */
@Composable
private fun GCodeDropOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(32.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.RequestQuote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Solte o G-code pra montar o orçamento", style = MaterialTheme.typography.titleLarge)
            Text(
                "Comprimento, tempo, foto, impressora e filamento vêm do arquivo, quando batem com o que está cadastrado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
