package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_filter_period
import _3dreport.composeapp.generated.resources.orders_filter_status
import _3dreport.composeapp.generated.resources.orders_filter_status_all
import _3dreport.composeapp.generated.resources.orders_search
import _3dreport.composeapp.generated.resources.orders_search_hint
import _3dreport.composeapp.generated.resources.orders_summary_due_today
import _3dreport.composeapp.generated.resources.orders_summary_in_progress
import _3dreport.composeapp.generated.resources.orders_summary_overdue
import _3dreport.composeapp.generated.resources.orders_summary_receivable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.SummaryBlock
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.OrderStatus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Os quatro blocos de resumo de Pedidos, nas duas visualizações: em andamento, a receber, pra entregar hoje e atrasados. Os
 * dois últimos usam as cores de prazo (a legenda diz o que o número é, a cor só reforça).
 */
@Composable
fun OrdersSummaryBlocks(state: HistoryUiState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val summary = state.summary
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        SummaryBlock(summary.inProgress.toString(), stringResource(Res.string.orders_summary_in_progress), Modifier.weight(1f))
        SummaryBlock(summary.receivable.toMoneyText(), stringResource(Res.string.orders_summary_receivable), Modifier.weight(1f))
        SummaryBlock(
            summary.dueToday.toString(),
            stringResource(Res.string.orders_summary_due_today),
            Modifier.weight(1f),
            valueColor = colors.deadlineToday.content,
        )
        SummaryBlock(
            summary.overdue.toString(),
            pluralStringResource(Res.plurals.orders_summary_overdue, summary.overdue),
            Modifier.weight(1f),
            valueColor = colors.deadlineLate.content,
        )
    }
}

/**
 * Busca, situação e período numa linha só, com os campos do kit. No kanban só a busca: a situação já é a própria
 * organização em colunas, e o período não esconde pedido em andamento.
 */
@Composable
fun OrdersFilters(state: HistoryUiState, viewModel: QuoteHistoryViewModel, showAll: Boolean, modifier: Modifier = Modifier) {
    val filter = state.filter
    val statusAll = stringResource(Res.string.orders_filter_status_all)
    val statusNames = OrderStatus.entries.associateWith { stringResource(it.labelRes()) }
    val periodNames = PeriodPreset.entries.associateWith { stringResource(it.labelRes()) }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.Top) {
        AppTextField(
            label = stringResource(Res.string.orders_search),
            value = filter.query,
            onValueChange = viewModel::setSearchQuery,
            placeholder = stringResource(Res.string.orders_search_hint),
            modifier = Modifier.weight(1f, fill = false).widthIn(max = AppDimens.searchMaxWidth).fillMaxWidth(),
        )
        if (showAll) {
            AppDropdownField<OrderStatus?>(
                label = stringResource(Res.string.orders_filter_status),
                items = listOf(null) + OrderStatus.entries,
                selected = filter.status,
                itemLabel = { it?.let(statusNames::getValue) ?: statusAll },
                displayText = { it?.let(statusNames::getValue) ?: statusAll },
                onSelect = viewModel::setStatusFilter,
                emptyText = statusAll,
                modifier = Modifier.width(AppDimens.filterFieldWidth),
            )
            AppDropdownField(
                label = stringResource(Res.string.history_filter_period),
                items = PeriodPreset.entries,
                selected = filter.period,
                itemLabel = { periodNames.getValue(it) },
                displayText = { periodNames.getValue(it) },
                onSelect = viewModel::setPeriodFilter,
                modifier = Modifier.width(AppDimens.filterFieldWidth),
            )
        }
    }
}
