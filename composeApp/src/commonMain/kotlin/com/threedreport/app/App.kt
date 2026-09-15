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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.FilamentListScreen
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.history.QuoteHistoryScreen
import com.threedreport.app.ui.history.QuoteHistoryViewModel
import com.threedreport.app.ui.printers.PrinterListScreen
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListScreen
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.app.ui.settings.BrandingViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SettingsViewModel

private const val GITHUB_URL = "https://github.com/mateustoin/3DReport"
private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/mateustoin"
private const val AUTHOR_NAME = "Mateus Antonio da Silva"

private enum class AppTab(val label: String) {
    QUOTE("Orçamento"),
    HISTORY("Histórico"),
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

    val quoteViewModel = remember {
        QuoteViewModel(filamentRepository, printerRepository, settingsRepository, serviceRepository, historyRepository)
    }
    val historyViewModel = remember { QuoteHistoryViewModel(historyRepository, brandingRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository) }
    val serviceListViewModel = remember { ServiceListViewModel(serviceRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val brandingViewModel = remember { BrandingViewModel(brandingRepository) }

    var selectedTab by remember { mutableStateOf(AppTab.QUOTE) }
    var showHelp by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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
                        AppTab.HISTORY -> QuoteHistoryScreen(historyViewModel)
                        AppTab.FILAMENTS -> FilamentListScreen(filamentListViewModel)
                        AppTab.PRINTERS -> PrinterListScreen(printerListViewModel)
                        AppTab.SERVICES -> ServiceListScreen(serviceListViewModel)
                        AppTab.SETTINGS -> SettingsScreen(settingsViewModel, brandingViewModel)
                    }
                }

                AppFooter(onHelpClick = { showHelp = true })
            }
        }

        if (showHelp) {
            HelpDialog(onDismiss = { showHelp = false })
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
                Text("• Orçamento: escolha filamento/impressora, informe comprimento e tempo, e calcule.")
                Text("• Histórico: consulte, exporte em PDF ou copie orçamentos salvos (1 ou vários juntos).")
                Text("• Filamentos, Impressoras e Serviços: seus catálogos, usados na tela de Orçamento.")
                Text("• Configurações: parâmetros de custo, marca d'água do PDF e taxa de marketplace.")
                LinkText(text = "Ver código-fonte no GitHub", url = GITHUB_URL)
                LinkText(text = "☕ Apoiar o projeto no Buy Me a Coffee", url = BUY_ME_A_COFFEE_URL)
            }
        },
    )
}
