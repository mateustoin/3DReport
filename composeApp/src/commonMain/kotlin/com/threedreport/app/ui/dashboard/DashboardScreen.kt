package com.threedreport.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.format.toBrl

/** Tela de Dashboard: total vendido, lucro e filamento mais usado, recortados por um período rápido. */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val period by viewModel.period.collectAsState()
    val summary = viewModel.summarize(savedQuotes, period)

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodPreset.entries.forEach { preset ->
                FilterChip(
                    selected = preset == period,
                    onClick = { viewModel.setPeriod(preset) },
                    label = { Text(preset.label) },
                )
            }
        }

        if (summary.quoteCount == 0) {
            Text(
                "Nenhum orçamento salvo nesse período.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Orçamentos", summary.quoteCount.toString(), Modifier.weight(1f))
                StatCard("Total vendido", summary.totalSalePrice.toBrl(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Lucro", summary.totalProfit.toBrl(), Modifier.weight(1f))
                StatCard(
                    "Filamento mais usado",
                    summary.mostUsedFilamentName?.let { "$it (${summary.mostUsedFilamentCount}x)" } ?: "-",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
