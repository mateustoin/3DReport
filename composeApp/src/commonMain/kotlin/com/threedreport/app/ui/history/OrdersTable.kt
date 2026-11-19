package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_action_copied
import _3dreport.composeapp.generated.resources.history_action_copy
import _3dreport.composeapp.generated.resources.history_action_export_pdf
import _3dreport.composeapp.generated.resources.orders_card_prints
import _3dreport.composeapp.generated.resources.orders_col_client
import _3dreport.composeapp.generated.resources.orders_col_deadline
import _3dreport.composeapp.generated.resources.orders_col_number
import _3dreport.composeapp.generated.resources.orders_col_payment
import _3dreport.composeapp.generated.resources.orders_col_piece
import _3dreport.composeapp.generated.resources.orders_col_status
import _3dreport.composeapp.generated.resources.orders_col_value
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.StatusLabel
import com.threedreport.app.ui.components.TableColumn
import com.threedreport.app.ui.components.TableHeader
import com.threedreport.app.ui.components.TableRow
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.shownName
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.app.ui.theme.statusColor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Que colunas a tabela de Pedidos tem: com [showPayment], a de pagamento; em janela estreita ([compact]) o cliente desce pra
 * baixo do nome da peça e o selo de pagamento pra baixo do prazo, pra peça ter largura de ler (na janela de 960, as colunas
 * fixas sozinhas passavam do espaço). O cabeçalho ([ordersColumns]) e as linhas ([OrdersTableRow]) usam o mesmo layout.
 */
data class OrdersTableLayout(val showPayment: Boolean, val compact: Boolean) {
    internal val hasClientColumn: Boolean get() = !compact
    internal val hasPaymentColumn: Boolean get() = showPayment && !compact
}

/** Abaixo desta largura (da área da tela) a tabela de Pedidos usa o layout estreito ([OrdersTableLayout.compact]). */
fun ordersTableIsCompact(availableWidth: androidx.compose.ui.unit.Dp): Boolean = availableWidth < AppDimens.ordersTableCompactBelow

/** As colunas da tabela de Pedidos (decisão 132): número, peça, cliente, situação, prazo, pagamento e valor à direita, conforme o [layout]. */
@Composable
fun ordersColumns(layout: OrdersTableLayout = OrdersTableLayout(showPayment = false, compact = false)): List<TableColumn> = listOfNotNull(
    TableColumn(stringResource(Res.string.orders_col_number), width = AppDimens.tableNumberColumn),
    TableColumn(stringResource(Res.string.orders_col_piece), weight = 2.2f),
    TableColumn(stringResource(Res.string.orders_col_client), weight = 1.2f).takeIf { layout.hasClientColumn },
    TableColumn(stringResource(Res.string.orders_col_status), width = AppDimens.tableStatusColumn),
    TableColumn(stringResource(Res.string.orders_col_deadline), width = AppDimens.tableDeadlineColumn),
    TableColumn(stringResource(Res.string.orders_col_payment), width = AppDimens.tableStatusColumn).takeIf { layout.hasPaymentColumn },
    TableColumn(stringResource(Res.string.orders_col_value), width = AppDimens.tableMoneyColumn, end = true),
)

/** O cabeçalho da tabela de Pedidos. */
@Composable
fun OrdersTableHeader(columns: List<TableColumn>, selectable: Boolean) = TableHeader(columns, selectable = selectable)

/**
 * Uma linha da tabela de Pedidos. Clicar abre os detalhes (ou, no modo de seleção, marca a linha); as ações de
 * exportar e copiar e o menu aparecem no hover ou no foco, cobrindo o valor.
 */
@Composable
fun OrdersTableRow(
    row: HistoryRow,
    columns: List<TableColumn>,
    /** O mesmo layout passado pra [ordersColumns]. */
    layout: OrdersTableLayout,
    state: HistoryUiState,
    actions: QuoteActions,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit,
) {
    val quote = row.quote
    val selected = quote.id in state.selectedIds
    val copied = state.copiedId == quote.id
    TableRow(
        columns = columns,
        onClick = if (state.selectionMode) onToggleSelected else onOpen,
        selected = state.selectionMode && selected,
        selectionMark = if (state.selectionMode) selected else null,
        actions = listOf(
            RowAction(AppIcons.PictureAsPdf, stringResource(Res.string.history_action_export_pdf), actions.onExportPdf),
            RowAction(
                if (copied) AppIcons.Check else AppIcons.ContentCopy,
                stringResource(if (copied) Res.string.history_action_copied else Res.string.history_action_copy),
                actions.onCopy,
            ),
        ),
        extra = { QuoteActionsMenu(quote, actions) },
        revealActions = !state.selectionMode,
    ) {
        // Cada célula pela posição que tem neste layout: o cliente e o pagamento só têm coluna própria na janela larga.
        var index = 0
        Cell(index++) { Muted(quote.displayNumber.orEmpty()) }
        Cell(index++) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(quote.shownName(), style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    row.printCount?.let { Muted(pluralStringResource(Res.plurals.orders_card_prints, it, it)) }
                    if (!layout.hasClientColumn) Muted(quote.client?.name.orEmpty())
                }
                quote.notes?.takeIf { it.isNotBlank() }?.let { NotesIcon(it) }
            }
        }
        if (layout.hasClientColumn) Cell(index++) { Muted(quote.client?.name.orEmpty()) }
        Cell(index++) {
            // O selo de parado fica embaixo da situação: cabe nos 52 da linha, sem alargar a coluna.
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                StatusLabel(stringResource(quote.status.labelRes()), quote.status.statusColor())
                row.staleDays?.let { StaleBadgeView(it) }
            }
        }
        Cell(index++) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                row.deadline?.let { DeadlineBadgeView(it) }
                if (!layout.hasPaymentColumn) row.paymentStatus?.let { PaymentBadgeView(it) }
            }
        }
        if (layout.hasPaymentColumn) Cell(index++) { row.paymentStatus?.let { PaymentBadgeView(it) } }
        Cell(index) {
            NumericText(
                quote.totalWithServices.toCurrencyText(quote.currency),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
