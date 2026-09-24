package com.threedreport.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.QuoteSummary

/**
 * Tela de Dashboard: o que foi vendido no período, quanto cada hora de máquina e de trabalho
 * rendeu, e quais peças e clientes puxam o resultado pra cima ou pra baixo (decisão 95). Venda é o
 * que o cliente aprovou; orçamento ainda "Orçado" aparece só numa linha à parte.
 */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val period by viewModel.period.collectAsState()
    val settings by viewModel.settings.collectAsState()
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

        when {
            summary.quoteCount == 0 && summary.openQuoteCount == 0 -> Text(
                "Nenhum orçamento salvo nesse período.",
                style = MaterialTheme.typography.bodyMedium,
            )
            summary.quoteCount == 0 -> {
                Text(
                    "Nenhuma venda nesse período. Um orçamento conta como venda quando o cliente aprova " +
                        "(status Aprovado em diante, no Histórico).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OpenQuotesLine(summary)
            }
            else -> {
                SalesRuler(summary)
                OpenQuotesLine(summary)
                HourlyRuler(summary, configuredLaborRate = settings.laborRatePerHour)
                ProductRankingSection(summary)
                DiscountRankingSection(summary)
            }
        }
    }
}

/**
 * Leitura em régua (número + rótulo, separados por uma linha fina), não cartões brancos repetidos
 * com sombra — o número já carrega peso visual suficiente sozinho.
 */
@Composable
private fun SalesRuler(summary: QuoteSummary) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        StatTicker("Vendas", summary.quoteCount.toString())
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

/** Orçamentos que o cliente ainda não fechou: fora das somas, mas à vista, com a conversão. */
@Composable
private fun OpenQuotesLine(summary: QuoteSummary) {
    if (summary.openQuoteCount == 0) return
    val count = summary.openQuoteCount
    val conversion = summary.conversionRate?.let { " · ${it.toPercentText()} dos orçamentos do período viraram venda" } ?: ""
    Text(
        "$count ${if (count == 1) "orçamento em aberto" else "orçamentos em aberto"} " +
            "(${summary.openQuoteTotal.toMoney()}), fora das somas acima$conversion",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Quanto cada hora rendeu. A hora de trabalho vem com a hora configurada ao lado, que é a
 * comparação que interessa: "configurei R$ 50, na prática levei R$ 72".
 */
@Composable
private fun HourlyRuler(summary: QuoteSummary, configuredLaborRate: Double) {
    val perPrintHour = summary.profitPerPrintHour
    val perLaborHour = summary.earningsPerLaborHour
    if (perPrintHour == null && perLaborHour == null) return

    SectionTitle(AppIcons.Schedule, "Quanto cada hora rendeu", modifier = Modifier.padding(top = 12.dp))
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        if (perPrintHour != null) {
            StatTicker("Lucro por hora de máquina", "${perPrintHour.toMoney()}/h")
        }
        if (perPrintHour != null && perLaborHour != null) StatDivider()
        if (perLaborHour != null) {
            StatTicker(
                label = "Seu trabalho rendeu por hora" +
                    if (configuredLaborRate > 0) " (sua hora: ${configuredLaborRate.toMoney()})" else "",
                value = "${perLaborHour.toMoney()}/h",
            )
        }
    }
    Text(
        buildString {
            if (perPrintHour != null) append("Máquina: lucro dividido pelas horas de impressão das vendas. ")
            if (perLaborHour != null) {
                append("Trabalho: lucro mais a sua mão de obra, dividido pelo seu tempo, só nas vendas com tempo de trabalho informado.")
            }
        }.trim(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProductRankingSection(summary: QuoteSummary) {
    if (summary.topProducts.isEmpty()) return
    SectionTitle(AppIcons.TrendingUp, "Peças que mais deram lucro", modifier = Modifier.padding(top = 12.dp))
    RankingHeader("Peça", "Vendas", "Lucro", "Por hora de máquina")
    summary.topProducts.forEach { product ->
        RankingRow(
            name = product.name,
            count = "${product.orderCount}x",
            amount = product.totalProfit.toMoney(),
            extra = product.profitPerPrintHour?.let { "${it.toMoney()}/h" } ?: "—",
        )
    }
    Text(
        "Pedidos com o mesmo nome contam como a mesma peça. Orçamentos salvos sem nome ficam de fora.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DiscountRankingSection(summary: QuoteSummary) {
    if (summary.topDiscountClients.isEmpty()) return
    SectionTitle(AppIcons.Handshake, "Clientes que mais puxam o preço pra baixo", modifier = Modifier.padding(top = 12.dp))
    RankingHeader("Cliente", "Negociados", "Desconto", null)
    summary.topDiscountClients.forEach { client ->
        RankingRow(
            name = client.clientName,
            count = client.negotiatedCount.toString(),
            amount = client.totalDiscount.toMoney(),
            extra = null,
        )
    }
}

@Composable
private fun RankingHeader(name: String, count: String, amount: String, extra: String?) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = style, color = color, modifier = Modifier.weight(1f))
        Text(count, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(RANK_COUNT_WIDTH))
        Text(amount, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(RANK_AMOUNT_WIDTH))
        if (extra != null) {
            Text(extra, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(RANK_EXTRA_WIDTH))
        }
    }
    HorizontalDivider()
}

@Composable
private fun RankingRow(name: String, count: String, amount: String, extra: String?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        NumericText(count, modifier = Modifier.width(RANK_COUNT_WIDTH), textAlign = TextAlign.End)
        NumericText(amount, modifier = Modifier.width(RANK_AMOUNT_WIDTH), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
        if (extra != null) {
            NumericText(extra, modifier = Modifier.width(RANK_EXTRA_WIDTH), textAlign = TextAlign.End)
        }
    }
}

private val RANK_COUNT_WIDTH = 96.dp
private val RANK_AMOUNT_WIDTH = 140.dp
private val RANK_EXTRA_WIDTH = 180.dp

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
