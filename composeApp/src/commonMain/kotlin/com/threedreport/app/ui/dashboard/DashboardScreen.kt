package com.threedreport.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toMoney

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
            // Leitura em régua (número + rótulo, separados por uma linha fina), não cartões
            // brancos repetidos com sombra — o número já carrega peso visual suficiente sozinho.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                StatTicker("Orçamentos", summary.quoteCount.toString())
                StatDivider()
                StatTicker("Total vendido", summary.totalSalePrice.toMoney())
                StatDivider()
                StatTicker("Lucro", summary.totalProfit.toMoney())
                StatDivider()
                if (summary.negotiatedCount > 0) {
                    StatTicker(
                        label = "Descontos dados (${summary.negotiatedCount} " +
                            (if (summary.negotiatedCount == 1) "negociado)" else "negociados)"),
                        value = summary.totalNegotiatedDiscount.toMoney(),
                    )
                    StatDivider()
                }
                StatTicker(
                    label = "Filamento mais usado",
                    value = summary.mostUsedFilamentName?.let { "$it (${summary.mostUsedFilamentCount}x)" } ?: "—",
                    numeric = false,
                )
            }
        }
    }
}

@Composable
private fun StatTicker(label: String, value: String, numeric: Boolean = true, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(min = 160.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (numeric) {
            NumericText(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        } else {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatDivider() {
    VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp).height(48.dp))
}
