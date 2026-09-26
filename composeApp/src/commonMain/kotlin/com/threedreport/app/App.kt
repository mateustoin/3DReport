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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import com.threedreport.app.ui.about.AboutScreen
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.navigation.AppSidebar
import com.threedreport.app.ui.navigation.SIDEBAR_LABELS_MIN_WINDOW_WIDTH
import com.threedreport.app.ui.navigation.destinationForShortcut
import com.threedreport.core.model.QuoteKind
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.platform.openFolder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.IO
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import com.threedreport.app.platform.openUrl
import com.threedreport.app.ui.about.UpdateViewModel
import com.threedreport.app.ui.about.UpdateState
import com.threedreport.app.ui.about.UpdateSection
import com.threedreport.app.ui.quote.printTitle
import com.threedreport.app.ui.quote.PrintInput
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.components.ConfirmDialog
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
import com.threedreport.app.ui.quote.QuoteOperation
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListScreen
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.app.ui.settings.BackupViewModel
import com.threedreport.app.ui.settings.BrandingViewModel
import com.threedreport.app.ui.settings.CurrencyViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SettingsSection
import com.threedreport.app.ui.settings.hasUnsavedSettings
import com.threedreport.app.ui.settings.SalesChannelViewModel
import com.threedreport.app.ui.settings.SettingsViewModel
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.app.ui.theme.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun App(container: AppContainer, dataFolderNotice: DataFolderNotice? = null) {
    val filamentRepository = container.filaments
    val printerRepository = container.printers
    val settingsRepository = container.settings
    val historyRepository = container.quoteHistory
    val onboardingRepository = container.onboarding

    val appScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
    // que estivesse em andamento lá. A edição aparece no próprio Orçamento (decisão 113), e o rascunho
    // da aba volta quando ela termina.
    val editQuoteViewModel = remember { newQuoteViewModel() }
    var destination by remember { mutableStateOf(AppDestination.QUOTE) }
    // Fica aqui, e não na tela, pra voltar a Configurações na mesma seção.
    var settingsSection by remember { mutableStateOf(SettingsSection.BUSINESS) }
    // Pedidos e Catálogo são a mesma tela com o tipo fixo (decisão 111): cada uma com o seu ViewModel,
    // pra busca, filtros e seleção de uma não vazarem pra outra.
    val newHistoryViewModel = { kind: QuoteKind ->
        QuoteHistoryViewModel(
            historyRepository, container.branding,
            filamentRepository, printerRepository, settingsRepository, container.salesChannels,
            clientRepository = container.clients,
            scope = appScope,
            background = Dispatchers.Default,
            main = Dispatchers.Main,
            kind = kind,
            showOrders = { destination = AppDestination.ORDERS },
        )
    }
    val ordersViewModel = remember { newHistoryViewModel(QuoteKind.ORDER) }
    val catalogViewModel = remember { newHistoryViewModel(QuoteKind.PRODUCT) }
    val dashboardViewModel = remember { DashboardViewModel(historyRepository, settingsRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository, historyRepository::quotesIncludingTrash) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository, historyRepository, container.maintenance) }
    val serviceListViewModel = remember { ServiceListViewModel(container.services, historyRepository::quotesIncludingTrash) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val brandingViewModel = remember {
        BrandingViewModel(container.branding, container.templates, currency = { container.currency.currency.value })
    }
    val templateListViewModel = remember { TemplateListViewModel(container.templates, container.branding) }
    val themeViewModel = remember { ThemeViewModel(container.theme) }
    val currencyViewModel = remember { CurrencyViewModel(container.currency) }
    val backupViewModel = remember { BackupViewModel(container.backup, container.preferences, container.pendingWrites, scope = appScope) }
    val updateViewModel = remember {
        UpdateViewModel(container.preferences, container.releases, APP_VERSION, scope = appScope, background = Dispatchers.IO, main = Dispatchers.Main)
    }
    val salesChannelViewModel = remember { SalesChannelViewModel(container.salesChannels, historyRepository::quotesIncludingTrash) }

    val onboardingCompleted by onboardingRepository.completed.collectAsState()
    var draggingFile by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    // Onde está cada área do aviso de arrastar ("Substituir a impressão 2", "Adicionar como nova"), pra
    // saber em qual o arquivo foi solto (decisão 114).
    val dropZones = remember { mutableStateMapOf<DropZone, Rect>() }
    // Ação que jogaria fora o orçamento em andamento na aba, esperando a confirmação.
    var pendingDiscard by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Ação que precisa da aba do Orçamento livre, esperando a pessoa encerrar a edição em andamento.
    var pendingEndEditing by remember { mutableStateOf<(() -> Unit)?>(null) }
    val editForm by editQuoteViewModel.saveForm.collectAsState()
    val editing = editForm.editingQuoteId != null
    // O Orçamento que está na tela: a edição, quando há uma, ou a aba.
    val visibleQuoteViewModel = if (editing) editQuoteViewModel else quoteViewModel
    val visibleInput by visibleQuoteViewModel.input.collectAsState()
    val originOf = { operation: QuoteOperation? ->
        if (operation?.originKind == QuoteKind.PRODUCT) AppDestination.CATALOG else AppDestination.ORDERS
    }
    // Duplicar, Vender, Guardar no catálogo e Ctrl+N carregam no ViewModel da aba, que está por trás da
    // edição: primeiro a edição termina (sem alteração, termina sozinha), depois pergunta do rascunho.
    val unlessEditing: (() -> Unit) -> Unit = { action ->
        when {
            !editing -> action()
            editQuoteViewModel.hasUnsavedEdits -> pendingEndEditing = action
            else -> {
                editQuoteViewModel.resetForm()
                action()
            }
        }
    }
    val unlessDraft: (() -> Unit) -> Unit = { action ->
        unlessEditing { if (quoteViewModel.hasDraft) pendingDiscard = action else action() }
    }
    var confirmingEditDiscard by remember { mutableStateOf(false) }
    val endEditing = {
        val origin = originOf(editQuoteViewModel.saveForm.value.operation)
        editQuoteViewModel.resetForm()
        destination = origin
    }
    val cancelEditing = { if (editQuoteViewModel.hasUnsavedEdits) confirmingEditDiscard = true else endEditing() }
    val saveEditing = {
        if (editQuoteViewModel.saveCurrentQuote()) {
            endEditing()
            appScope.launch { snackbarHostState.showSnackbar("Alterações salvas.") }
        }
    }
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()

    // Coletados aqui pro pontinho de "alteração não salva" da barra lateral acompanhar o que se digita.
    val settingsDraft by settingsViewModel.uiState.collectAsState()
    val brandingDraft by brandingViewModel.uiState.collectAsState()
    val settingsDirty = remember(settingsDraft, brandingDraft) { hasUnsavedSettings(settingsViewModel, brandingViewModel) }

    AppTheme(themeMode) {
        CompositionLocalProvider(LocalCurrency provides currency, LocalSnackbarHostState provides snackbarHostState) {
            Surface(
                modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val accel = event.isCtrlPressed || event.isMetaPressed

                    val shortcut = if (accel) destinationForShortcut(shortcutNumberOf(event.key) ?: 0) else null
                    if (shortcut != null) {
                        destination = shortcut
                        return@onPreviewKeyEvent true
                    }

                    when {
                        accel && event.key == Key.S && destination == AppDestination.QUOTE -> {
                            if (editing) saveEditing() else quoteViewModel.saveCurrentQuote()
                            true
                        }
                        accel && event.key == Key.N && destination == AppDestination.QUOTE -> {
                            unlessDraft(quoteViewModel::resetForm)
                            true
                        }
                        // Esc fecha o formulário da aba que está na tela, e só ele: antes fechava os de
                        // todas as abas, inclusive um que a pessoa nem estava vendo.
                        event.key == Key.Escape -> when (destination) {
                            AppDestination.FILAMENTS -> filamentListViewModel.form.value?.let { filamentListViewModel.cancelEdit(); true } ?: false
                            AppDestination.PRINTERS -> printerListViewModel.form.value?.let { printerListViewModel.cancelEdit(); true } ?: false
                            AppDestination.SERVICES -> serviceListViewModel.form.value?.let { serviceListViewModel.cancelEdit(); true } ?: false
                            else -> false
                        }
                        else -> false
                    }
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().fileDropTarget(
                        onDragActive = {
                            draggingFile = it
                            if (!it) dragPosition = null
                        },
                        onDragMoved = { dragPosition = it },
                        onDrop = { dropped, position ->
                            // Seja qual for a aba aberta, o G-code vai pro Orçamento, que é onde o resultado aparece.
                            destination = AppDestination.QUOTE
                            val zone = position?.let { point -> dropZones.entries.firstOrNull { it.value.contains(point) }?.key }
                            when (zone) {
                                is DropZone.Replace -> visibleQuoteViewModel.importDroppedInto(zone.printId, dropped)
                                DropZone.AddNew -> dropped.forEach(visibleQuoteViewModel::importDroppedAsNewPrint)
                                null -> visibleQuoteViewModel.importDroppedFiles(dropped)
                            }
                        },
                    ),
                ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compactSidebar = maxWidth < SIDEBAR_LABELS_MIN_WINDOW_WIDTH
                Row(modifier = Modifier.fillMaxSize()) {
                    AppSidebar(
                        selected = destination,
                        onSelect = { destination = it },
                        compact = compactSidebar,
                        version = APP_VERSION,
                        status = { if (it == AppDestination.QUOTE && editing) "Editando" else null },
                        hasPendingChanges = { it == AppDestination.SETTINGS && settingsDirty },
                    )
                    VerticalDivider()

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (destination) {
                            // A chave troca a tela inteira entre a edição e a aba, sem estado de uma vazar pra outra.
                            AppDestination.QUOTE -> key(editing) {
                                if (editing) {
                                    QuoteScreen(editQuoteViewModel, onSave = saveEditing, onCancelOperation = cancelEditing)
                                } else {
                                    QuoteScreen(
                                        quoteViewModel,
                                        // Vender, Duplicar e Guardar no catálogo começam em Pedidos ou no Catálogo:
                                        // desistir devolve a pessoa pra lá, com o formulário limpo.
                                        onCancelOperation = {
                                            val origin = originOf(quoteViewModel.saveForm.value.operation)
                                            quoteViewModel.resetForm()
                                            destination = origin
                                        },
                                    )
                                }
                            }
                            AppDestination.ORDERS, AppDestination.CATALOG -> QuoteHistoryScreen(
                                if (destination == AppDestination.ORDERS) ordersViewModel else catalogViewModel,
                                onEditQuote = { savedQuote ->
                                    unlessEditing {
                                        editQuoteViewModel.loadForEditing(savedQuote)
                                        destination = AppDestination.QUOTE
                                    }
                                },
                                // As três começam um orçamento na aba: com um rascunho lá, pergunta antes.
                                onDuplicateQuote = { savedQuote ->
                                    unlessDraft {
                                        quoteViewModel.duplicateForNewQuote(savedQuote)
                                        destination = AppDestination.QUOTE
                                    }
                                },
                                onSellProduct = { product ->
                                    unlessDraft {
                                        quoteViewModel.sellFromProduct(product)
                                        destination = AppDestination.QUOTE
                                    }
                                },
                                onCopyToCatalog = { order ->
                                    unlessDraft {
                                        quoteViewModel.copyToCatalog(order)
                                        destination = AppDestination.QUOTE
                                    }
                                },
                            )
                            AppDestination.DASHBOARD -> DashboardScreen(dashboardViewModel)
                            AppDestination.FILAMENTS -> FilamentListScreen(filamentListViewModel)
                            AppDestination.PRINTERS -> PrinterListScreen(printerListViewModel)
                            AppDestination.SERVICES -> ServiceListScreen(serviceListViewModel)
                            AppDestination.SETTINGS -> SettingsScreen(
                                settingsViewModel,
                                brandingViewModel,
                                templateListViewModel,
                                themeViewModel,
                                currencyViewModel,
                                backupViewModel,
                                salesChannelViewModel,
                                section = settingsSection,
                                onSectionChange = { settingsSection = it },
                            )
                            AppDestination.ABOUT -> AboutScreen(version = APP_VERSION) { UpdateSection(updateViewModel) }
                        }
                    }
                }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )

                if (draggingFile) {
                    GCodeDropOverlay(
                        prints = visibleInput.prints,
                        // Lido dentro do aviso: cada movimento do mouse redesenha só ele, e não a tela inteira.
                        hovered = { dragPosition?.let { point -> dropZones.entries.firstOrNull { it.value.contains(point) }?.key } },
                        onZonePlaced = { zone, bounds -> dropZones[zone] = bounds },
                        onDispose = { dropZones.clear() },
                    )
                }
                }
            }

            var showOldDataNotice by remember { mutableStateOf(dataFolderNotice != null) }
            if (showOldDataNotice && dataFolderNotice != null) {
                DataFolderNoticeDialog(dataFolderNotice, onDismiss = { showOldDataNotice = false })
            }

            StorageHealthDialogs(container)

            // Com a opção ligada, uma verificação ao abrir, e o aviso uma vez por versão nova (decisão 116).
            LaunchedEffect(Unit) { updateViewModel.checkOnStart() }
            val updateState by updateViewModel.state.collectAsState()
            val available = updateViewModel.unannounced(updateState)
            LaunchedEffect(available?.version) {
                if (available == null || destination == AppDestination.ABOUT) return@LaunchedEffect
                updateViewModel.markAnnounced(available)
                val result = snackbarHostState.showSnackbar(
                    "Versão ${available.version} do 3DReport disponível.",
                    actionLabel = "Ver novidades",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) openUrl(available.url)
            }

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

            if (confirmingEditDiscard) {
                ConfirmDialog(
                    title = "Descartar as alterações?",
                    message = "O que você mudou neste orçamento não foi salvo e vai se perder.",
                    confirmLabel = "Descartar",
                    onConfirm = {
                        confirmingEditDiscard = false
                        endEditing()
                    },
                    onDismiss = { confirmingEditDiscard = false },
                )
            }

            pendingEndEditing?.let { action ->
                ConfirmDialog(
                    title = "Descartar a edição em andamento?",
                    message = "Você está editando \"${editForm.name}\" no Orçamento e ainda não salvou. Pra continuar, " +
                        "as alterações dessa edição se perdem.",
                    confirmLabel = "Descartar e continuar",
                    dismissLabel = "Voltar pra edição",
                    onConfirm = {
                        pendingEndEditing = null
                        editQuoteViewModel.resetForm()
                        action()
                    },
                    onDismiss = {
                        pendingEndEditing = null
                        destination = AppDestination.QUOTE
                    },
                )
            }

            pendingDiscard?.let { action ->
                ConfirmDialog(
                    title = "Descartar o orçamento em andamento?",
                    message = "O que está preenchido no Orçamento ainda não foi salvo e vai ser substituído.",
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
                                    "Configurações → Dados pra recuperar."
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

/** O número de Ctrl/Cmd+1 a 9, ou `null` pra outra tecla. */
private fun shortcutNumberOf(key: Key): Int? = when (key) {
    Key.One -> 1
    Key.Two -> 2
    Key.Three -> 3
    Key.Four -> 4
    Key.Five -> 5
    Key.Six -> 6
    Key.Seven -> 7
    Key.Eight -> 8
    Key.Nine -> 9
    else -> null
}

/** Onde um G-code pode ser solto quando o pedido já tem impressão preenchida (decisão 114). */
private sealed interface DropZone {
    data class Replace(val printId: Int) : DropZone

    data object AddNew : DropZone
}

/**
 * Aviso por cima da janela enquanto um arquivo é arrastado: diz onde soltar e o que vai acontecer.
 * Sem ele, arrastar um G-code pra janela não dá nenhum sinal de que funciona até soltar.
 *
 * Com o pedido ainda em branco, a janela inteira é um lugar só. Com alguma impressão preenchida, o aviso se
 * divide: uma área pra substituir cada impressão e outra pra adicionar o arquivo como impressão nova. Vários
 * arquivos soltos fora das áreas viram uma impressão cada.
 */
@Composable
private fun GCodeDropOverlay(
    prints: List<PrintInput>,
    hovered: () -> DropZone?,
    onZonePlaced: (DropZone, Rect) -> Unit,
    onDispose: () -> Unit,
) {
    DisposableEffect(Unit) { onDispose { onDispose() } }
    val filled = prints.any { !it.isBlank }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(32.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.RequestQuote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            if (!filled) {
                Text("Solte o G-code pra montar o orçamento", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Comprimento, tempo, foto, impressora e filamento vêm do arquivo, quando batem com o que está cadastrado. " +
                        "Vários arquivos de uma vez viram uma impressão cada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Text("Solte o G-code em cima do que ele vai fazer", style = MaterialTheme.typography.titleLarge)
            prints.forEachIndexed { index, print ->
                val zone = DropZone.Replace(print.id)
                DropZoneBox(
                    text = "Substituir a ${printTitle(index + 1, print.name).replaceFirstChar { it.lowercase() }}",
                    highlighted = hovered() == zone,
                    onPlaced = { onZonePlaced(zone, it) },
                )
            }
            DropZoneBox(
                text = "Adicionar como nova impressão",
                highlighted = hovered() == DropZone.AddNew,
                onPlaced = { onZonePlaced(DropZone.AddNew, it) },
            )
        }
    }
}

@Composable
private fun DropZoneBox(text: String, highlighted: Boolean, onPlaced: (Rect) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .widthIn(min = 360.dp)
            .onGloballyPositioned { onPlaced(it.boundsInRoot()) }
            .background(if (highlighted) colors.primaryContainer else colors.surfaceContainer, RoundedCornerShape(12.dp))
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = if (highlighted) colors.onPrimaryContainer else colors.onSurface)
    }
}
