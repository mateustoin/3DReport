package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_help_for
import _3dreport.composeapp.generated.resources.history_selection_count
import _3dreport.composeapp.generated.resources.history_selection_export_pdf
import _3dreport.composeapp.generated.resources.history_selection_none
import _3dreport.composeapp.generated.resources.history_selection_select_all
import _3dreport.composeapp.generated.resources.orders_action_new
import _3dreport.composeapp.generated.resources.orders_empty_filter
import _3dreport.composeapp.generated.resources.orders_empty_first
import _3dreport.composeapp.generated.resources.orders_kanban_cancelled
import _3dreport.composeapp.generated.resources.orders_kanban_cancelled_help
import _3dreport.composeapp.generated.resources.orders_title
import _3dreport.composeapp.generated.resources.orders_view_kanban
import _3dreport.composeapp.generated.resources.orders_view_list
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.data.OrdersView
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.components.TableColumn
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pedidos (decisões 129 e 132): blocos de resumo, filtros e, na visualização lembrada, o kanban ou a tabela ordenada
 * pelo prazo. Só desenha o [HistoryUiState] e envia eventos; "Novo orçamento" e as ações que abrem o Orçamento saem
 * pelos callbacks de [navigation] e [onNewQuote].
 */
@Composable
fun OrdersScreen(
    viewModel: QuoteHistoryViewModel,
    navigation: HistoryNavigation,
    onNewQuote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    ObserveHistory(viewModel, state.copiedId)
    val kanban = state.ordersView == OrdersView.KANBAN
    val columns = ordersColumns()
    val space = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        state.busy?.let { BusyBar(it) }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl),
        ) {
            item(key = "header") { OrdersHeader(state, viewModel, onNewQuote) }
            if (!state.hasItems) {
                item(key = "empty") {
                    EmptyState(stringResource(Res.string.orders_empty_first), actionLabel = stringResource(Res.string.orders_action_new), onAction = onNewQuote)
                }
            } else {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(space.md), modifier = Modifier.padding(bottom = space.md)) {
                        OrdersSummaryBlocks(state)
                        OrdersFilters(state, viewModel, showAll = !kanban)
                    }
                }
                if (kanban) kanbanItems(state, viewModel, navigation) else tableItems(state, columns, viewModel, navigation)
            }
        }
        if (state.selectionMode && !kanban) OrdersSelectionBar(state, viewModel)
    }
    HistoryDialogs(state, viewModel)
}

@Composable
private fun OrdersHeader(state: HistoryUiState, viewModel: QuoteHistoryViewModel, onNewQuote: () -> Unit) {
    ScreenHeader(stringResource(Res.string.orders_title)) {
        if (!state.hasItems) return@ScreenHeader
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SegmentedChoice(
                options = listOf(stringResource(Res.string.orders_view_list), stringResource(Res.string.orders_view_kanban)),
                selectedIndex = if (state.ordersView == OrdersView.KANBAN) 1 else 0,
                onSelect = { viewModel.setOrdersView(if (it == 1) OrdersView.KANBAN else OrdersView.LIST) },
            )
            AppButton(stringResource(Res.string.orders_action_new), onNewQuote, kind = ButtonKind.Primary, icon = AppIcons.Add)
        }
    }
}

private fun LazyListScope.kanbanItems(state: HistoryUiState, viewModel: QuoteHistoryViewModel, navigation: HistoryNavigation) {
    item(key = "kanban") {
        KanbanBoard(
            columns = state.columns,
            state = state,
            loadThumbnail = viewModel::photoThumbnail,
            callbacks = KanbanCallbacks(
                onStatusChange = viewModel::updateStatus,
                actionsFor = { row -> quoteActionsFor(row, viewModel, navigation, selectable = false) },
                onOpen = { row -> viewModel.startEditingDetails(row.quote) },
                onShowAllDelivered = viewModel::setShowAllDelivered,
            ),
        )
    }
    if (state.cancelledCount > 0) {
        item(key = "cancelled") {
            Row(
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            ) {
                Text(
                    pluralStringResource(Res.plurals.orders_kanban_cancelled, state.cancelledCount, state.cancelledCount),
                    style = MaterialTheme.appType.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HelpHint(HelpText(stringResource(Res.string.orders_kanban_cancelled_help)), stringResource(Res.string.orders_title))
            }
        }
    }
}

private fun LazyListScope.tableItems(state: HistoryUiState, columns: List<TableColumn>, viewModel: QuoteHistoryViewModel, navigation: HistoryNavigation) {
    if (state.rows.isEmpty()) {
        item(key = "no-match") { EmptyState(stringResource(Res.string.orders_empty_filter)) }
        return
    }
    item(key = "table-header") { OrdersTableHeader(columns, selectable = state.selectionMode) }
    // Lista preguiçosa: só as linhas visíveis são montadas, e cada uma só carrega o que precisa.
    items(state.rows, key = { it.quote.id }) { row ->
        OrdersTableRow(
            row = row,
            columns = columns,
            state = state,
            actions = quoteActionsFor(row, viewModel, navigation, selectable = true),
            onOpen = { viewModel.startEditingDetails(row.quote) },
            onToggleSelected = { viewModel.toggleSelection(row.quote.id) },
        )
    }
}

@Composable
private fun OrdersSelectionBar(state: HistoryUiState, viewModel: QuoteHistoryViewModel) {
    val count = state.selectedVisibleCount
    SelectionBar(
        summary = if (count > 0) pluralStringResource(Res.plurals.history_selection_count, count, count) else stringResource(Res.string.history_selection_none),
    ) {
        AppButton(stringResource(Res.string.history_selection_select_all), viewModel::selectAllVisible, enabled = count < state.rows.size)
        AppButton(
            stringResource(Res.string.history_selection_export_pdf),
            viewModel::exportSelectedPdf,
            icon = AppIcons.PictureAsPdf,
            enabled = count > 0 && state.busy == null,
        )
        AppButton(stringResource(Res.string.common_cancel), viewModel::clearSelection)
    }
}
