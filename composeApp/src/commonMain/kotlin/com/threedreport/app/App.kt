package com.threedreport.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.filaments.FilamentListScreen
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.history.QuoteHistoryScreen
import com.threedreport.app.ui.history.QuoteHistoryViewModel
import com.threedreport.app.ui.printers.PrinterListScreen
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SettingsViewModel

private enum class AppTab(val label: String) {
    QUOTE("Orçamento"),
    HISTORY("Histórico"),
    FILAMENTS("Filamentos"),
    PRINTERS("Impressoras"),
    SETTINGS("Configurações"),
}

@Composable
fun App() {
    val filamentRepository = remember { FilamentRepository() }
    val printerRepository = remember { PrinterRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val historyRepository = remember { QuoteHistoryRepository() }

    val quoteViewModel = remember {
        QuoteViewModel(filamentRepository, printerRepository, settingsRepository, historyRepository)
    }
    val historyViewModel = remember { QuoteHistoryViewModel(historyRepository) }
    val filamentListViewModel = remember { FilamentListViewModel(filamentRepository) }
    val printerListViewModel = remember { PrinterListViewModel(printerRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }

    var selectedTab by remember { mutableStateOf(AppTab.QUOTE) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    AppTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label) },
                        )
                    }
                }

                when (selectedTab) {
                    AppTab.QUOTE -> QuoteScreen(quoteViewModel)
                    AppTab.HISTORY -> QuoteHistoryScreen(historyViewModel)
                    AppTab.FILAMENTS -> FilamentListScreen(filamentListViewModel)
                    AppTab.PRINTERS -> PrinterListScreen(printerListViewModel)
                    AppTab.SETTINGS -> SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}
