package com.threedreport.app

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.about_updates_announce
import _3dreport.composeapp.generated.resources.about_updates_see_news
import _3dreport.composeapp.generated.resources.app_discard_edit_message
import _3dreport.composeapp.generated.resources.app_discard_edit_title
import _3dreport.composeapp.generated.resources.app_discard_quote_message
import _3dreport.composeapp.generated.resources.app_discard_quote_title
import _3dreport.composeapp.generated.resources.app_end_editing_back
import _3dreport.composeapp.generated.resources.app_end_editing_confirm
import _3dreport.composeapp.generated.resources.app_end_editing_message
import _3dreport.composeapp.generated.resources.app_end_editing_title
import _3dreport.composeapp.generated.resources.app_saved_changes
import _3dreport.composeapp.generated.resources.app_status_editing
import _3dreport.composeapp.generated.resources.common_discard
import _3dreport.composeapp.generated.resources.common_got_it
import _3dreport.composeapp.generated.resources.common_open_folder
import _3dreport.composeapp.generated.resources.common_retry
import _3dreport.composeapp.generated.resources.storage_notice_migrated_cleanup
import _3dreport.composeapp.generated.resources.storage_notice_migrated_kept
import _3dreport.composeapp.generated.resources.storage_notice_migrated_message
import _3dreport.composeapp.generated.resources.storage_notice_migrated_title
import _3dreport.composeapp.generated.resources.storage_notice_newer_kept
import _3dreport.composeapp.generated.resources.storage_notice_newer_message
import _3dreport.composeapp.generated.resources.storage_notice_newer_recover
import _3dreport.composeapp.generated.resources.storage_notice_newer_title
import _3dreport.composeapp.generated.resources.storage_unreadable_empty
import _3dreport.composeapp.generated.resources.storage_unreadable_kept
import _3dreport.composeapp.generated.resources.storage_unreadable_recovered
import _3dreport.composeapp.generated.resources.storage_unreadable_title
import _3dreport.composeapp.generated.resources.storage_write_failed_file
import _3dreport.composeapp.generated.resources.storage_write_failed_message
import _3dreport.composeapp.generated.resources.storage_write_failed_title
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.data.Clock
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.platform.fileDropTarget
import com.threedreport.app.platform.openFolder
import com.threedreport.app.platform.openUrl
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.about.AboutScreen
import com.threedreport.app.ui.about.UpdateState
import com.threedreport.app.ui.about.UpdateViewModel
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.LocalSnackbarHostState
import com.threedreport.app.ui.consumables.ConsumableListScreen
import com.threedreport.app.ui.consumables.ConsumableListViewModel
import com.threedreport.app.ui.dashboard.DashboardScreen
import com.threedreport.app.ui.dashboard.DashboardViewModel
import com.threedreport.app.ui.filaments.FilamentListScreen
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.history.CatalogScreen
import com.threedreport.app.ui.history.HistoryNavigation
import com.threedreport.app.ui.history.OrdersScreen
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.navigation.AppSidebar
import com.threedreport.app.ui.navigation.SIDEBAR_LABELS_MIN_WINDOW_WIDTH
import com.threedreport.app.ui.navigation.destinationForShortcut
import com.threedreport.app.ui.onboarding.OnboardingDialog
import com.threedreport.app.ui.onboarding.OnboardingPrinter
import com.threedreport.app.ui.printers.PrinterListScreen
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.PrintInput
import com.threedreport.app.ui.quote.QuoteOperation
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.quote.printTitle
import com.threedreport.app.ui.services.ServiceListScreen
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.app.ui.settings.BackupViewModel
import com.threedreport.app.ui.settings.BrandingViewModel
import com.threedreport.app.ui.settings.CurrencyViewModel
import com.threedreport.app.ui.settings.SalesChannelViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SettingsSection
import com.threedreport.app.ui.settings.SettingsViewModel
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.QuoteKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    container: AppContainer,
    dataFolderNotice: DataFolderNotice? = null,
    // Os parâmetros abaixo só existem pro gerador de screenshots do site (decisão 120): abrir
    // direto numa aba, preencher o orçamento da aba com dados de exemplo e fixar o "hoje" de Pedidos, sem mudar o comportamento
    // padrão do app (valores default preservam o de sempre).
    startAt: AppDestination = AppDestination.QUOTE,
    prepareQuote: (QuoteViewModel) -> Unit = {},
    today: () -> Long = ::todayEpochDay,
    clock: Clock = Clock.System,
) {
    val settingsRepository = container.settings
    val printerRepository = container.printers
    val onboardingRepository = container.onboarding

    val appScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // O navegador precisa dos ViewModels do Orçamento, e o de histórico precisa do navegador (pra "ver
    // pedidos"): o `nav` é preenchido logo abaixo, antes de qualquer toque na tela.
    val (viewModels, navigator) = remember {
        lateinit var nav: AppNavigator
        val models = AppViewModels(container, appScope, showOrders = { nav.goTo(AppDestination.ORDERS) }, prepareQuote = prepareQuote, today = today, clock = clock)
        nav = AppNavigator(
            startAt,
            models.quoteViewModel,
            models.editQuoteViewModel,
            onEditSaved = { appScope.launch { snackbarHostState.showSnackbar(getString(Res.string.app_saved_changes)) } },
        )
        models to nav
    }
    val destination = navigator.destination
    // Fica aqui, e não na tela, pra voltar a Configurações na mesma seção.
    var settingsSection by remember { mutableStateOf(SettingsSection.BUSINESS) }

    val onboardingCompleted by onboardingRepository.completed.collectAsState()
    var draggingFile by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    // Onde está cada área do aviso de arrastar ("Substituir a impressão 2", "Adicionar como nova"), pra
    // saber em qual o arquivo foi solto (decisão 114).
    val dropZones = remember { mutableStateMapOf<DropZone, Rect>() }
    // Coletado pra recompor quando a edição abre ou fecha, e pro diálogo mostrar o nome do orçamento.
    val editState by viewModels.editQuoteViewModel.uiState.collectAsState()
    val editForm = editState.form
    val editing = editForm.editingQuoteId != null
    val editingLabel = stringResource(Res.string.app_status_editing)
    // O Orçamento na tela, do mesmo valor coletado que escolhe a tela: tudo o que depende dele (aviso de
    // arrastar, destino do G-code solto, Ctrl+S) concorda com o que está desenhado.
    val visibleQuoteViewModel = if (editing) viewModels.editQuoteViewModel else viewModels.quoteViewModel
    val tabState by viewModels.quoteViewModel.uiState.collectAsState()
    val visibleInput = (if (editing) editState else tabState).input
    val themeMode by viewModels.themeViewModel.mode.collectAsState()
    val currency by viewModels.currencyViewModel.currency.collectAsState()

    // Coletados aqui pro pontinho de "alteração não salva" da barra lateral acompanhar o que se digita.
    val settingsDraft by viewModels.settingsViewModel.uiState.collectAsState()
    val brandingDraft by viewModels.brandingViewModel.uiState.collectAsState()
    val settingsDirty = settingsDraft.hasUnsavedChanges || brandingDraft.hasUnsavedChanges

    AppTheme(themeMode) {
        CompositionLocalProvider(LocalCurrency provides currency, LocalSnackbarHostState provides snackbarHostState) {
            Surface(
                modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val accel = event.isCtrlPressed || event.isMetaPressed

                    val shortcut = if (accel) destinationForShortcut(shortcutNumberOf(event.key) ?: 0) else null
                    if (shortcut != null) {
                        navigator.goTo(shortcut)
                        return@onPreviewKeyEvent true
                    }

                    when {
                        accel && event.key == Key.S && navigator.destination == AppDestination.QUOTE -> {
                            navigator.saveCurrent()
                            true
                        }

                        accel && event.key == Key.N && navigator.destination == AppDestination.QUOTE -> {
                            navigator.newQuote()
                            true
                        }

                        // Esc fecha o formulário da aba que está na tela, e só ele: antes fechava os de
                        // todas as abas, inclusive um que a pessoa nem estava vendo.
                        event.key == Key.Escape -> when (navigator.destination) {
                            AppDestination.FILAMENTS -> viewModels.filamentListViewModel.let { vm -> vm.uiState.value.form?.let { vm.cancelEdit(); true } ?: false }
                            AppDestination.PRINTERS -> viewModels.printerListViewModel.let { vm -> vm.uiState.value.form?.let { vm.cancelEdit(); true } ?: false }
                            AppDestination.SERVICES -> viewModels.serviceListViewModel.let { vm -> vm.uiState.value.form?.let { vm.cancelEdit(); true } ?: false }
                            AppDestination.CONSUMABLES -> viewModels.consumableListViewModel.let { vm -> vm.uiState.value.form?.let { vm.cancelEdit(); true } ?: false }
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
                            navigator.goTo(AppDestination.QUOTE)
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
                                onSelect = navigator::goTo,
                                compact = compactSidebar,
                                version = APP_VERSION,
                                status = { if (it == AppDestination.QUOTE && editing) editingLabel else null },
                                hasPendingChanges = { it == AppDestination.SETTINGS && settingsDirty },
                            )
                            VerticalDivider()

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when (destination) {
                                    // A chave troca a tela inteira entre a edição e a aba, sem estado de uma vazar pra outra.
                                    AppDestination.QUOTE -> key(editing) {
                                        if (editing) {
                                            QuoteScreen(
                                                viewModels.editQuoteViewModel,
                                                onSave = navigator::saveEditing,
                                                onCancelOperation = navigator::cancelEditing,
                                            )
                                        } else {
                                            QuoteScreen(viewModels.quoteViewModel, onCancelOperation = navigator::cancelQuoteOperation)
                                        }
                                    }

                                    // Duplicar, Vender, Guardar no catálogo e os "Novo" começam um orçamento na aba: com um
                                    // rascunho lá, pergunta antes.
                                    AppDestination.ORDERS -> OrdersScreen(
                                        viewModels.ordersViewModel,
                                        navigation = HistoryNavigation(
                                            onEdit = navigator::editQuote,
                                            onDuplicate = navigator::duplicateQuote,
                                            onCopyToCatalog = navigator::copyToCatalog,
                                        ),
                                        onNewQuote = navigator::newOrder,
                                    )

                                    AppDestination.CATALOG -> CatalogScreen(
                                        viewModels.catalogViewModel,
                                        navigation = HistoryNavigation(
                                            onEdit = navigator::editQuote,
                                            onDuplicate = navigator::duplicateQuote,
                                            onSell = navigator::sellProduct,
                                        ),
                                        onNewProduct = navigator::newProduct,
                                    )

                                    AppDestination.DASHBOARD -> DashboardScreen(
                                        viewModels.dashboardViewModel,
                                        onNewQuote = navigator::newOrder,
                                        onShowOrders = { navigator.goTo(AppDestination.ORDERS) },
                                    )

                                    AppDestination.FILAMENTS -> FilamentListScreen(viewModels.filamentListViewModel)

                                    AppDestination.PRINTERS -> PrinterListScreen(viewModels.printerListViewModel)

                                    AppDestination.SERVICES -> ServiceListScreen(viewModels.serviceListViewModel)

                                    AppDestination.CONSUMABLES -> ConsumableListScreen(viewModels.consumableListViewModel)

                                    AppDestination.SETTINGS -> SettingsScreen(
                                        viewModels.settingsViewModel,
                                        viewModels.brandingViewModel,
                                        viewModels.templateListViewModel,
                                        viewModels.themeViewModel,
                                        viewModels.currencyViewModel,
                                        viewModels.backupViewModel,
                                        viewModels.salesChannelViewModel,
                                        section = settingsSection,
                                        onSectionChange = { settingsSection = it },
                                    )

                                    AppDestination.ABOUT -> AboutScreen(version = APP_VERSION, updateViewModel = viewModels.updateViewModel)
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
            val updateViewModel = viewModels.updateViewModel
            LaunchedEffect(Unit) { updateViewModel.checkOnStart() }
            val updateState by updateViewModel.uiState.collectAsState()
            val available = updateViewModel.unannounced(updateState.status)
            LaunchedEffect(available?.version) {
                if (available == null || navigator.destination == AppDestination.ABOUT) return@LaunchedEffect
                updateViewModel.markAnnounced(available)
                val result = snackbarHostState.showSnackbar(
                    getString(Res.string.about_updates_announce, available.version),
                    actionLabel = getString(Res.string.about_updates_see_news),
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

            if (navigator.confirmingEditDiscard) {
                ConfirmDialog(
                    title = stringResource(Res.string.app_discard_edit_title),
                    message = stringResource(Res.string.app_discard_edit_message),
                    confirmLabel = stringResource(Res.string.common_discard),
                    onConfirm = navigator::confirmEditDiscard,
                    onDismiss = navigator::keepEditing,
                )
            }

            if (navigator.pendingEndEditing != null) {
                ConfirmDialog(
                    title = stringResource(Res.string.app_end_editing_title),
                    message = stringResource(Res.string.app_end_editing_message, editForm.name),
                    confirmLabel = stringResource(Res.string.app_end_editing_confirm),
                    dismissLabel = stringResource(Res.string.app_end_editing_back),
                    onConfirm = navigator::confirmEndEditing,
                    onDismiss = navigator::dismissEndEditing,
                )
            }

            if (navigator.pendingDiscard != null) {
                ConfirmDialog(
                    title = stringResource(Res.string.app_discard_quote_title),
                    message = stringResource(Res.string.app_discard_quote_message),
                    confirmLabel = stringResource(Res.string.common_discard),
                    onConfirm = navigator::confirmDiscard,
                    onDismiss = navigator::dismissDiscard,
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
    val migrated = notice.kind == DataFolderNotice.Kind.MIGRATED
    AppDialog(
        title = stringResource(if (migrated) Res.string.storage_notice_migrated_title else Res.string.storage_notice_newer_title),
        onDismiss = {},
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_open_folder), { openFolder(notice.path) })
            AppButton(stringResource(Res.string.common_got_it), onDismiss, kind = ButtonKind.Primary)
        },
    ) {
        DialogText(stringResource(if (migrated) Res.string.storage_notice_migrated_message else Res.string.storage_notice_newer_message))
        DialogText(stringResource(if (migrated) Res.string.storage_notice_migrated_kept else Res.string.storage_notice_newer_kept))
        SelectionContainer { DialogText(notice.path) }
        DialogText(stringResource(if (migrated) Res.string.storage_notice_migrated_cleanup else Res.string.storage_notice_newer_recover), muted = true)
    }
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
        AppDialog(
            title = stringResource(Res.string.storage_unreadable_title),
            onDismiss = {},
            dismissOnClickOutside = false,
            buttons = { AppButton(stringResource(Res.string.common_got_it), container.storageHealth::dismissUnreadable, kind = ButtonKind.Primary) },
        ) {
            unreadable.forEach { file ->
                DialogText(
                    stringResource(if (file.recoveredFromBackup) Res.string.storage_unreadable_recovered else Res.string.storage_unreadable_empty, file.name),
                )
            }
            DialogText(stringResource(Res.string.storage_unreadable_kept), muted = true)
            SelectionContainer {
                Column { unreadable.forEach { DialogText(it.keptAs, muted = true) } }
            }
        }
    }

    if (writeFailures.isNotEmpty()) {
        AppDialog(
            title = stringResource(Res.string.storage_write_failed_title),
            onDismiss = {},
            dismissOnClickOutside = false,
            buttons = { AppButton(stringResource(Res.string.common_retry), container.pendingWrites::retry, kind = ButtonKind.Primary) },
        ) {
            DialogText(stringResource(Res.string.storage_write_failed_message))
            writeFailures.forEach { (file, reason) -> DialogText(stringResource(Res.string.storage_write_failed_file, file, reason), muted = true) }
        }
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
