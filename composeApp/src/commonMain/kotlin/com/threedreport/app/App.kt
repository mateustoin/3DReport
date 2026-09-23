package com.threedreport.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OnboardingRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.data.ThemeRepository
import com.threedreport.app.ui.components.LinkText
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
    SETTINGS("Configurações"),
}

@Composable
fun App() {
    val filamentRepository = remember { FilamentRepository() }
    val printerRepository = remember { PrinterRepository() }
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

    val quoteViewModel = remember {
        QuoteViewModel(
            filamentRepository, printerRepository, settingsRepository, serviceRepository,
            salesChannelRepository, historyRepository,
        )
    }
    val historyViewModel = remember { QuoteHistoryViewModel(historyRepository, brandingRepository, currencyRepository) }
    val dashboardViewModel = remember { DashboardViewModel(historyRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository, historyRepository) }
    val serviceListViewModel = remember { ServiceListViewModel(serviceRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val brandingViewModel = remember { BrandingViewModel(brandingRepository, templateRepository) }
    val templateListViewModel = remember { TemplateListViewModel(templateRepository, brandingRepository) }
    val themeViewModel = remember { ThemeViewModel(themeRepository) }
    val currencyViewModel = remember { CurrencyViewModel(currencyRepository) }
    val backupViewModel = remember { BackupViewModel(backupRepository) }
    val salesChannelViewModel = remember { SalesChannelViewModel(salesChannelRepository) }

    val onboardingCompleted by onboardingRepository.completed.collectAsState()
    var selectedTab by remember { mutableStateOf(AppTab.QUOTE) }
    var showHelp by remember { mutableStateOf(false) }
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()

    AppTheme(themeMode) {
        CompositionLocalProvider(LocalCurrency provides currency) {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                        AppTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.label) },
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            AppTab.QUOTE -> QuoteScreen(quoteViewModel)
                            AppTab.HISTORY -> QuoteHistoryScreen(
                                historyViewModel,
                                onEditQuote = { savedQuote -> quoteViewModel.loadForEditing(savedQuote) },
                                onDuplicateQuote = { savedQuote ->
                                    quoteViewModel.duplicateForNewQuote(savedQuote)
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
                            )
                        }
                    }

                    AppFooter(onHelpClick = { showHelp = true })
                }
            }

            if (showHelp) {
                HelpDialog(onDismiss = { showHelp = false })
            }

            if (!onboardingCompleted) {
                OnboardingDialog(
                    currentSettings = settingsRepository.settings.value,
                    onFinish = {
                        settingsRepository.update(it)
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
                        "Com uma hora de trabalho configurada, informe também os minutos de trabalho da peça.",
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
