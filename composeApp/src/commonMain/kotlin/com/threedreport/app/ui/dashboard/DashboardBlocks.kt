package com.threedreport.app.ui.dashboard

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.dashboard_catalog_price_range
import _3dreport.composeapp.generated.resources.dashboard_catalog_products
import _3dreport.composeapp.generated.resources.dashboard_catalog_ranking_help
import _3dreport.composeapp.generated.resources.dashboard_catalog_ranking_title
import _3dreport.composeapp.generated.resources.dashboard_catalog_title
import _3dreport.composeapp.generated.resources.dashboard_col_per_hour
import _3dreport.composeapp.generated.resources.dashboard_col_piece
import _3dreport.composeapp.generated.resources.dashboard_col_price
import _3dreport.composeapp.generated.resources.dashboard_col_product
import _3dreport.composeapp.generated.resources.dashboard_col_profit
import _3dreport.composeapp.generated.resources.dashboard_col_sales
import _3dreport.composeapp.generated.resources.dashboard_discount_client
import _3dreport.composeapp.generated.resources.dashboard_discount_help
import _3dreport.composeapp.generated.resources.dashboard_discount_title
import _3dreport.composeapp.generated.resources.dashboard_discounts_given
import _3dreport.composeapp.generated.resources.dashboard_filament_value
import _3dreport.composeapp.generated.resources.dashboard_hours_configured
import _3dreport.composeapp.generated.resources.dashboard_hours_help_labor
import _3dreport.composeapp.generated.resources.dashboard_hours_labor
import _3dreport.composeapp.generated.resources.dashboard_hours_title
import _3dreport.composeapp.generated.resources.dashboard_most_used_filament
import _3dreport.composeapp.generated.resources.dashboard_most_used_title
import _3dreport.composeapp.generated.resources.dashboard_no_data
import _3dreport.composeapp.generated.resources.dashboard_open_conversion
import _3dreport.composeapp.generated.resources.dashboard_open_conversion_help
import _3dreport.composeapp.generated.resources.dashboard_open_help
import _3dreport.composeapp.generated.resources.dashboard_open_quotes
import _3dreport.composeapp.generated.resources.dashboard_open_title
import _3dreport.composeapp.generated.resources.dashboard_per_hour
import _3dreport.composeapp.generated.resources.dashboard_price_range
import _3dreport.composeapp.generated.resources.dashboard_ranking_help
import _3dreport.composeapp.generated.resources.dashboard_ranking_help_machine_hour
import _3dreport.composeapp.generated.resources.dashboard_ranking_title
import _3dreport.composeapp.generated.resources.dashboard_times
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.InfoRow
import com.threedreport.app.ui.components.TableColumn
import com.threedreport.app.ui.components.TableHeader
import com.threedreport.app.ui.components.TableStaticRow
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.core.report.CatalogSummary
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Uma linha de ranking já formatada: o nome e as três colunas de valor, na ordem da tabela. */
private class RankingRow(val name: String, val first: String, val second: String, val perHour: String)

/** "Peças que mais deram lucro": a tabela do kit, com o valor à direita e a ajuda sob demanda. */
@Composable
internal fun SalesRanking(state: DashboardUiState) {
    val noData = stringResource(Res.string.dashboard_no_data)
    val rows = state.summary.topProducts.map { product ->
        RankingRow(
            name = product.name,
            first = stringResource(Res.string.dashboard_times, product.orderCount),
            second = product.totalProfit.toMoney(),
            perHour = product.profitPerPrintHour?.let { stringResource(Res.string.dashboard_per_hour, it.toMoney()) } ?: noData,
        )
    }
    RankingTable(
        title = stringResource(Res.string.dashboard_ranking_title),
        help = HelpText(
            short = stringResource(Res.string.dashboard_ranking_help),
            detail = stringResource(Res.string.dashboard_ranking_help_machine_hour),
        ),
        columns = rankingColumns(
            nameTitle = stringResource(Res.string.dashboard_col_piece),
            firstTitle = stringResource(Res.string.dashboard_col_sales),
            secondTitle = stringResource(Res.string.dashboard_col_profit),
        ),
        rows = rows,
    )
}

/** "Qual vale a pena oferecer primeiro": os produtos do catálogo de quem ainda não vende (decisão 103). */
@Composable
internal fun CatalogRanking(catalog: CatalogSummary) {
    val noData = stringResource(Res.string.dashboard_no_data)
    val rows = catalog.products.map { product ->
        RankingRow(
            name = product.name,
            first = product.profit.toMoney(),
            second = product.price.toMoney(),
            perHour = product.profitPerPrintHour?.let { stringResource(Res.string.dashboard_per_hour, it.toMoney()) } ?: noData,
        )
    }
    RankingTable(
        title = stringResource(Res.string.dashboard_catalog_ranking_title),
        help = HelpText(stringResource(Res.string.dashboard_catalog_ranking_help)),
        columns = rankingColumns(
            nameTitle = stringResource(Res.string.dashboard_col_product),
            firstTitle = stringResource(Res.string.dashboard_col_profit),
            secondTitle = stringResource(Res.string.dashboard_col_price),
            firstIsMoney = true,
        ),
        rows = rows,
    )
}

@Composable
private fun rankingColumns(nameTitle: String, firstTitle: String, secondTitle: String, firstIsMoney: Boolean = false) = listOf(
    TableColumn(nameTitle),
    TableColumn(firstTitle, width = if (firstIsMoney) AppDimens.tableMoneyColumn else AppDimens.tableNumberColumn, end = true),
    TableColumn(secondTitle, width = AppDimens.tableMoneyColumn, end = true),
    TableColumn(stringResource(Res.string.dashboard_col_per_hour), width = AppDimens.tableMoneyColumn, end = true),
)

@Composable
private fun RankingTable(title: String, help: HelpText, columns: List<TableColumn>, rows: List<RankingRow>) {
    FormSection(number = null, title = title, help = help) {
        Column {
            TableHeader(columns)
            rows.forEach { row ->
                TableStaticRow(columns) {
                    Cell(0) { Text(row.name, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Cell(1) { Value(row.first) }
                    Cell(2) { Value(row.second) }
                    Cell(3) { Value(row.perHour) }
                }
            }
        }
    }
}

/** Um valor da tabela: `body`, algarismos tabulares, na cor do texto. */
@Composable
private fun Value(text: String) {
    NumericText(text, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
}

/** Os blocos da direita quando há venda no período. */
@Composable
internal fun SalesBlocks(state: DashboardUiState) {
    OpenBlock(state)
    HoursBlock(state)
    MostUsedBlock(state)
    DiscountBlock(state)
}

/** Os blocos da direita quando não há venda: o que está em aberto e o catálogo. */
@Composable
internal fun EmptyBlocks(state: DashboardUiState) {
    OpenBlock(state)
    state.catalog?.let { CatalogBlock(it) }
}

/** Orçamentos esperando resposta (quantidade e valor) e a parcela dos orçamentos que virou venda. */
@Composable
private fun OpenBlock(state: DashboardUiState) {
    val summary = state.summary
    if (summary.openQuoteCount == 0) return
    val help = HelpText(
        short = stringResource(Res.string.dashboard_open_help),
        detail = stringResource(Res.string.dashboard_open_conversion_help),
    )
    FormSection(number = null, title = stringResource(Res.string.dashboard_open_title), help = help) {
        Column {
            InfoRow(pluralStringResource(Res.plurals.dashboard_open_quotes, summary.openQuoteCount, summary.openQuoteCount), summary.openQuoteTotal.toMoney())
            summary.conversionRate?.let { InfoRow(stringResource(Res.string.dashboard_open_conversion), it.toPercentText()) }
        }
    }
}

/**
 * Hora de trabalho e, quando configurada, a sua hora de referência. A hora de máquina já está no resumo e no ranking, então
 * sem hora de trabalho o bloco não aparece.
 */
@Composable
private fun HoursBlock(state: DashboardUiState) {
    val perLaborHour = state.summary.earningsPerLaborHour ?: return
    FormSection(
        number = null,
        title = stringResource(Res.string.dashboard_hours_title),
        help = HelpText(stringResource(Res.string.dashboard_hours_help_labor)),
    ) {
        Column {
            InfoRow(stringResource(Res.string.dashboard_hours_labor), stringResource(Res.string.dashboard_per_hour, perLaborHour.toMoney()))
            if (state.laborRatePerHour > 0) {
                InfoRow(stringResource(Res.string.dashboard_hours_configured), stringResource(Res.string.dashboard_per_hour, state.laborRatePerHour.toMoney()))
            }
        }
    }
}

/** O filamento mais usado e os descontos dados no período. */
@Composable
private fun MostUsedBlock(state: DashboardUiState) {
    val summary = state.summary
    val filament = summary.mostUsedFilamentName
    if (filament == null && summary.negotiatedCount == 0) return
    FormSection(number = null, title = stringResource(Res.string.dashboard_most_used_title)) {
        Column {
            filament?.let {
                InfoRow(
                    stringResource(Res.string.dashboard_most_used_filament),
                    stringResource(Res.string.dashboard_filament_value, it, summary.mostUsedFilamentCount),
                )
            }
            if (summary.negotiatedCount > 0) {
                InfoRow(
                    pluralStringResource(Res.plurals.dashboard_discounts_given, summary.negotiatedCount, summary.negotiatedCount),
                    summary.totalNegotiatedDiscount.toMoney(),
                )
            }
        }
    }
}

/** Os clientes que mais receberam desconto, um por linha. */
@Composable
private fun DiscountBlock(state: DashboardUiState) {
    val clients = state.summary.topDiscountClients
    if (clients.isEmpty()) return
    FormSection(number = null, title = stringResource(Res.string.dashboard_discount_title), help = HelpText(stringResource(Res.string.dashboard_discount_help))) {
        Column {
            clients.forEach { client ->
                InfoRow(
                    pluralStringResource(Res.plurals.dashboard_discount_client, client.negotiatedCount, client.clientName, client.negotiatedCount),
                    client.totalDiscount.toMoney(),
                )
            }
        }
    }
}

/** Quantos produtos há no catálogo e a faixa de preço deles. */
@Composable
private fun CatalogBlock(catalog: CatalogSummary) {
    val min = catalog.minPrice ?: return
    val max = catalog.maxPrice ?: return
    FormSection(number = null, title = stringResource(Res.string.dashboard_catalog_title)) {
        Column {
            InfoRow(stringResource(Res.string.dashboard_catalog_products), catalog.productCount.toString())
            InfoRow(
                stringResource(Res.string.dashboard_catalog_price_range),
                if (min == max) min.toMoney() else stringResource(Res.string.dashboard_price_range, min.toMoney(), max.toMoney()),
            )
        }
    }
}
