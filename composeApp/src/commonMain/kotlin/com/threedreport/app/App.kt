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
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.quote.QuoteScreen
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.settings.SettingsScreen
import com.threedreport.app.ui.settings.SettingsViewModel

private enum class AppTab(val label: String) {
    QUOTE("Orçamento"),
    SETTINGS("Configurações"),
}

@Composable
fun App() {
    val filamentRepository = remember { FilamentRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val quoteViewModel = remember { QuoteViewModel(filamentRepository, settingsRepository) }
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
                    AppTab.SETTINGS -> SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}
