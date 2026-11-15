package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.orders_kanban_empty
import _3dreport.composeapp.generated.resources.orders_kanban_recent_only
import _3dreport.composeapp.generated.resources.orders_kanban_see_more
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.threedreport.app.platform.HorizontalScrollbarFor
import com.threedreport.app.ui.components.clickableAction
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.app.ui.theme.statusColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** O que o kanban pede à tela, junto: a situação nova de um cartão arrastado, as ações de cada um e o "ver mais" dos entregues. */
class KanbanCallbacks(
    val onStatusChange: (String, OrderStatus) -> Unit,
    val actionsFor: (HistoryRow) -> QuoteActions,
    val onOpen: (HistoryRow) -> Unit,
    val onShowAllDelivered: (Boolean) -> Unit,
)

/**
 * Quadro kanban de Pedidos: uma coluna por etapa do fluxo ([OrderStatus.PIPELINE]), arrastando o cartão entre colunas
 * pra mudar a situação. O menu de cada cartão tem "Mover para" pra quem não quer arrastar. As colunas dividem a largura
 * por igual e, abaixo da largura mínima, rolam na horizontal (com a barra visível: a roda do mouse rola na vertical, e
 * quem não tem trackpad não descobria as colunas da direita).
 *
 * As posições usadas pro hit-test do arrasto vêm de [LayoutCoordinates.positionInWindow] mais o tamanho medido, e não de
 * `boundsInWindow()`: esse é recortado pela área visível de todos os ancestrais, e uma coluna parcialmente fora da tela
 * reportava um retângulo bem menor que a altura real. Cada coluna é esticada à altura da mais alta (`IntrinsicSize.Max`
 * e `fillMaxHeight()`), pra soltar num espaço vazio da coluna também contar.
 */
@Composable
fun KanbanBoard(
    columns: List<KanbanColumnState>,
    state: HistoryUiState,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    callbacks: KanbanCallbacks,
    modifier: Modifier = Modifier,
) {
    // Posição e tamanho (em coordenadas de janela) de cada coluna, pra saber sobre qual um cartão foi solto.
    val columnBounds = remember { mutableStateMapOf<OrderStatus, Rect>() }
    var hoveredStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var draggedFromStatus by remember { mutableStateOf<OrderStatus?>(null) }
    val scrollState = rememberScrollState()
    val space = MaterialTheme.spacing

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fit = (maxWidth - space.sm * (columns.size - 1)) / columns.size
        val columnWidth = maxOf(fit, AppDimens.kanbanColumnMinWidth)
        Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                columns.forEach { column ->
                    KanbanColumn(
                        column = column,
                        width = columnWidth,
                        state = state,
                        loadThumbnail = loadThumbnail,
                        isDropTarget = hoveredStatus == column.status && draggedFromStatus != column.status,
                        // A coluna de onde o cartão saiu fica por cima das outras enquanto ele é arrastado.
                        isDragSource = draggedFromStatus == column.status,
                        columnBounds = columnBounds,
                        compact = columnWidth < AppDimens.kanbanRowMinWidth,
                        onBoundsChanged = { bounds -> columnBounds[column.status] = bounds },
                        onDragHover = { hovered -> hoveredStatus = hovered },
                        onDragSourceChange = { source -> draggedFromStatus = source },
                        callbacks = callbacks,
                    )
                }
            }
            if (fit < AppDimens.kanbanColumnMinWidth) HorizontalScrollbarFor(scrollState, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun LayoutCoordinates.fullBoundsInWindow(): Rect = Rect(positionInWindow(), size.toSize())

@Composable
private fun KanbanColumn(
    column: KanbanColumnState,
    width: Dp,
    state: HistoryUiState,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    isDropTarget: Boolean,
    isDragSource: Boolean,
    columnBounds: Map<OrderStatus, Rect>,
    compact: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    callbacks: KanbanCallbacks,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .zIndex(if (isDragSource) 1f else 0f)
            .width(width)
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.large)
            .background(if (isDropTarget) colors.tint else colors.column)
            .onGloballyPositioned { onBoundsChanged(it.fullBoundsInWindow()) },
    ) {
        Box(Modifier.fillMaxWidth().height(AppDimens.statusBandHeight).background(column.status.statusColor()))
        val padding = if (compact) space.xs else space.sm
        Column(modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(column.status.labelRes()), style = MaterialTheme.appType.sectionTitle, color = MaterialTheme.colorScheme.onSurface)
                NumericText(
                    column.total.toMoneyText(),
                    style = MaterialTheme.appType.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (column.cards.isEmpty()) {
                Text(stringResource(Res.string.orders_kanban_empty), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            column.cards.forEach { row ->
                key(row.quote.id) {
                    KanbanCard(
                        row = row,
                        loadThumbnail = loadThumbnail,
                        columnBounds = columnBounds,
                        compact = compact,
                        onDragHover = onDragHover,
                        onDragSourceChange = onDragSourceChange,
                        callbacks = callbacks,
                    )
                }
            }
            if (isDropTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.dropZoneHeight)
                        .border(AppDimens.focusRingWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                )
            }
            if (column.status == OrderStatus.ENTREGUE) DeliveredFooter(state, callbacks)
        }
    }
}

/** Embaixo da coluna Entregue: "ver mais" dos entregues antigos, e voltar a só os recentes. */
@Composable
private fun DeliveredFooter(state: HistoryUiState, callbacks: KanbanCallbacks) {
    val padding = Modifier.padding(horizontal = MaterialTheme.spacing.xs, vertical = MaterialTheme.spacing.xxs)
    when {
        state.hiddenDelivered > 0 -> Text(
            pluralStringResource(Res.plurals.orders_kanban_see_more, state.hiddenDelivered, state.hiddenDelivered),
            style = MaterialTheme.appType.label,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickableAction({ callbacks.onShowAllDelivered(true) }).then(padding),
        )

        state.showAllDelivered -> Text(
            stringResource(Res.string.orders_kanban_recent_only, RECENT_DELIVERED_DAYS),
            style = MaterialTheme.appType.label,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickableAction({ callbacks.onShowAllDelivered(false) }).then(padding),
        )
    }
}
