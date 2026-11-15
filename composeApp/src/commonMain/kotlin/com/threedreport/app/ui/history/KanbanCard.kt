package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.orders_card_prints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.math.roundToInt

/**
 * Cartão de pedido do kanban: fundo `card`, borda `line` (`primary` com o mouse em cima ou o foco), nome em `bodyStrong`,
 * número, cliente e quantidade de impressões em `caption`, o selo de prazo e o valor, com a foto pequena quando houver.
 * O botão de menu só aparece no hover ou no foco. Clicar abre os detalhes; arrastar muda a situação.
 */
@Composable
fun KanbanCard(
    row: HistoryRow,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    columnBounds: Map<OrderStatus, Rect>,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    callbacks: KanbanCallbacks,
) {
    val quote = row.quote
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    var dragOffset by remember(quote.id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(quote.id) { mutableStateOf(false) }
    var liveBounds by remember(quote.id) { mutableStateOf<Rect?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    var focusWithin by remember { mutableStateOf(false) }
    val active = hovered || focusWithin

    fun targetStatusUnderPointer(): OrderStatus? {
        val point = liveBounds?.center ?: return null
        return columnBounds.entries.firstOrNull { it.value.contains(point) }?.key
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned { liveBounds = Rect(it.positionInWindow(), it.size.toSize()) }
            .clip(MaterialTheme.shapes.medium)
            .background(colors.card)
            .border(
                if (focused) AppDimens.focusRingWidth else AppDimens.borderWidth,
                if (focused || active || isDragging) MaterialTheme.colorScheme.primary else colors.line,
                MaterialTheme.shapes.medium,
            )
            .onFocusChanged { focusWithin = it.hasFocus }
            .hoverable(interaction)
            .pointerInput(quote.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragSourceChange(quote.status)
                        onDragHover(quote.status)
                    },
                    onDragEnd = {
                        isDragging = false
                        val target = targetStatusUnderPointer()
                        if (target != null && target != quote.status) callbacks.onStatusChange(quote.id, target)
                        dragOffset = Offset.Zero
                        onDragHover(null)
                        onDragSourceChange(null)
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                        onDragHover(null)
                        onDragSourceChange(null)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        onDragHover(targetStatusUnderPointer())
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) { callbacks.onOpen(row) }
                .padding(space.sm),
            verticalArrangement = Arrangement.spacedBy(space.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                if (quote.photoFileName != null) PhotoThumbnail(quote, loadThumbnail, Modifier.size(AppDimens.kanbanPhotoSize))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    Text(
                        quote.name,
                        style = MaterialTheme.appType.bodyStrong,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val prints = row.printCount?.let { pluralStringResource(Res.plurals.orders_card_prints, it, it) }
                    Text(
                        listOfNotNull(quote.displayNumber, quote.client?.name, prints).joinToString(" · "),
                        style = MaterialTheme.appType.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(space.xxs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                // O mesmo valor da tabela: o que o cliente paga.
                row.deadline?.let { DeadlineBadgeView(it, Modifier.padding(end = space.xs)) }
                NumericText(
                    quote.totalWithServices.toCurrencyText(quote.currency),
                    style = MaterialTheme.appType.sectionTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        QuoteActionsMenu(
            quote,
            callbacks.actionsFor(row),
            modifier = Modifier.align(Alignment.TopEnd).alpha(if (active) 1f else 0f).background(colors.card, MaterialTheme.shapes.small),
        )
    }
}
