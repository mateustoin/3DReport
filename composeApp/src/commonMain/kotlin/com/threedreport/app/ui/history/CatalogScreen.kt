package com.threedreport.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.ObserveUiNotices
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.format.toCurrencyText

/** Provisório: a próxima etapa troca esta tela pela do Catálogo na nova interface. */
@Composable
fun CatalogScreen(viewModel: QuoteHistoryViewModel, navigation: HistoryNavigation, onNewProduct: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveUiNotices(viewModel.notices)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Catálogo")
        state.rows.forEach { row ->
            ListRow(
                title = row.quote.name,
                onClick = { viewModel.startEditingDetails(row.quote) },
                actions = emptyList(),
                value = row.quote.totalWithServices.toCurrencyText(row.quote.currency),
            )
        }
    }
    HistoryDialogs(state, viewModel)
}
