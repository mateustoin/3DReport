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
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.OnboardingRepository
import com.threedreport.app.data.UsageProfileRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.data.ThemeRepository
import com.threedreport.app.ui.icons.AppIcons
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
import com.threedreport.core.model.UsageProfile

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
fun App(oldDataPath: String? = null) {
    val filamentRepository = remember { FilamentRepository() }
    val printerRepository = remember { PrinterRepository() }
    val maintenanceRepository = remember { MaintenanceRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val historyRepository = remember { QuoteHistoryRepository() }
    val brandingRepository = remember { BrandingRepository() }
    val serviceRepository = remember { ServiceRepository() }
    val templateRepository = remember { TemplateRepository() }
    val themeRepository = remember { ThemeRepository() }
    val currencyRepository = remember { CurrencyRepository() }
    val backupRepository = remember { BackupRepository() }
    val salesChannelRepository = remember { SalesChannelRepository() }
    val onboardingRepository = remember { OnboardingRepository() }
    val usageProfileRepository = remember { UsageProfileRepository() }
    val defaultKind = { usageProfileRepository.profile.value.defaultKind }

    val quoteViewModel = remember {
        QuoteViewModel(
            filamentRepository, printerRepository, settingsRepository, serviceRepository,
            salesChannelRepository, historyRepository, defaultKind,
        )
    }
    val historyViewModel = remember {
        QuoteHistoryViewModel(
            historyRepository, brandingRepository, currencyRepository,
            filamentRepository, printerRepository, settingsRepository, salesChannelRepository,
            defaultKind = defaultKind,
        )
    }
    val dashboardViewModel = remember { DashboardViewModel(historyRepository, settingsRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository, historyRepository, maintenanceRepository) }
    val serviceListViewModel = remember { ServiceListViewModel(serviceRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val brandingViewModel = remember { BrandingViewModel(brandingRepository, templateRepository, currency = { currencyRepository.currency.value }) }
    val templateListViewModel = remember { TemplateListViewModel(templateRepository, brandingRepository) }
    val themeViewModel = remember { ThemeViewModel(themeRepository) }
    val currencyViewModel = remember { CurrencyViewModel(currencyRepository) }
    val backupViewModel = remember { BackupViewModel(backupRepository) }
    val salesChannelViewModel = remember { SalesChannelViewModel(salesChannelRepository) }

    val onboardingCompleted by onboardingRepository.completed.collectAsState()
    val usageProfile by usageProfileRepository.profile.collectAsState()
    // Perfil de uso (decisão 103): só muda o que vem escolhido, e vale na hora pro orçamento em
    // branco e pra lista do Histórico.
    val applyUsageProfile: (UsageProfile) -> Unit = { profile ->
        usageProfileRepository.update(profile)
        quoteViewModel.applyDefaultKindIfUntouched()
        historyViewModel.setKindFilter(profile.defaultKind)
    }
    var selectedTab by remember { mutableStateOf(AppTab.QUOTE) }
    var draggingFile by remember { mutableStateOf(false) }
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
                            quoteViewModel.resetForm()
                            true
                        }
                        event.key == Key.Escape -> {
                            var handled = false
                            if (filamentListViewModel.form.value != null) { filamentListViewModel.cancelEdit(); handled = true }
                            if (printerListViewModel.form.value != null) { printerListViewModel.cancelEdit(); handled = true }
                            if (serviceListViewModel.form.value != null) { serviceListViewModel.cancelEdit(); handled = true }
                            if (quoteViewModel.saveForm.value.editingQuoteId != null) { quoteViewModel.resetForm(); handled = true }
                            handled
                        }
                        else -> false
                    }
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().fileDropTarget(
                        onDragActive = { draggingFile = it },
                        onDrop = { file ->
                            // Seja qual for a aba aberta, o G-code vai pro Orçamento, que é onde o resultado aparece.
                            selectedTab = AppTab.QUOTE
                            quoteViewModel.importGCode(file)
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
                                onEditQuote = { savedQuote -> quoteViewModel.loadForEditing(savedQuote) },
                                onDuplicateQuote = { savedQuote ->
                                    quoteViewModel.duplicateForNewQuote(savedQuote)
                                    selectedTab = AppTab.QUOTE
                                },
                                onSellProduct = { product ->
                                    quoteViewModel.sellFromProduct(product)
                                    selectedTab = AppTab.QUOTE
                                },
                                onCopyToCatalog = { order ->
                                    quoteViewModel.copyToCatalog(order)
                                    selectedTab = AppTab.QUOTE
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
                                usageProfile = usageProfile,
                                onUsageProfileChange = applyUsageProfile,
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

            var showOldDataNotice by remember { mutableStateOf(oldDataPath != null) }
            if (showOldDataNotice && oldDataPath != null) {
                OldDataNoticeDialog(oldDataPath, onDismiss = { showOldDataNotice = false })
            }

            // O aviso dos dados antigos vem antes: o onboarding só aparece depois de ele ser lido.
            if (!onboardingCompleted && !showOldDataNotice) {
                OnboardingDialog(
                    currentSettings = settingsRepository.settings.value,
                    onFinish = { settings, profile ->
                        settingsRepository.update(settings)
                        applyUsageProfile(profile)
                        onboardingRepository.markCompleted()
                    },
                    onSkip = onboardingRepository::markCompleted,
                )
            }

            val quoteSaveForm by quoteViewModel.saveForm.collectAsState()
            if (quoteSaveForm.editingQuoteId != null) {
                EditQuoteDialog(viewModel = quoteViewModel, onDismiss = quoteViewModel::resetForm)
            }
        }
    }
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
 * Aviso único de que os dados de uma versão anterior foram guardados à parte (decisão 104): a 2.0
 * mudou o formato dos dados e não converte o que existia, então a pessoa precisa saber onde eles
 * ficaram e como recuperar.
 */
@Composable
private fun OldDataNoticeDialog(oldDataPath: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi") } },
        title = { Text("Formato de dados novo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Esta versão guarda os pedidos de um jeito novo, que aceita vários filamentos e várias " +
                        "impressões por pedido, e começa com os dados em branco.",
                )
                Text("O que você tinha foi guardado, sem apagar nada, em:")
                Text(oldDataPath, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Pra recuperar, reinstale a versão 1.44 e renomeie essa pasta de volta pra .3dreport.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
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
                    "• Orçamento: escolha filamento/impressora, informe comprimento e tempo, e calcule. " +
                        "Com uma hora de trabalho configurada, informe também o seu tempo de trabalho no pedido.",
                )
                Text("• Histórico: consulte, filtre, exporte em PDF ou copie orçamentos salvos (1 ou vários juntos).")
                Text("• Dashboard: total vendido, lucro e filamento mais usado no período.")
                Text("• Filamentos, Impressoras e Serviços: seus catálogos, usados na tela de Orçamento.")
                Text(
                    "• Configurações: aparência (tema), parâmetros de custo (incluindo o valor da sua hora de " +
                        "trabalho e o custo fixo mensal), marca d'água do PDF (com templates salvos — fotos " +
                        "nomeadas pra voltar rápido a uma configuração), taxa de marketplace e backup dos dados.",
                )
                Text("Atalhos de teclado", style = MaterialTheme.typography.titleSmall)
                Text("• Ctrl/Cmd+1 a 7: pula direto para cada aba, nessa ordem.")
                Text("• Ctrl/Cmd+S: salva o orçamento atual (aba Orçamento).")
                Text("• Ctrl/Cmd+N: limpa a tela de Orçamento pra começar um novo.")
                Text("• Esc: cancela o formulário aberto em Filamentos/Impressoras/Serviços.")
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
