package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.orders_card_prints
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.threedreport.app.ui.components.cardFrame
import com.threedreport.app.ui.components.clickableCard
import com.threedreport.app.ui.components.rememberCardState
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
    compact: Boolean,
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
    val card = rememberCardState()
    val active = card.active
    // Na coluna estreita (modo compacto) o cartão é mais apertado: sem foto ao lado do nome, e o respiro cai pra 8.
    val padding = if (compact) space.xs else space.sm

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
            .cardFrame(card, highlighted = isDragging)
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
                .clickableCard(card, onClick = { callbacks.onOpen(row) })
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(space.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                if (!compact && quote.photoFileName != null) PhotoThumbnail(quote, loadThumbnail, Modifier.size(AppDimens.kanbanPhotoSize))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    Text(
                        quote.name,
                        style = MaterialTheme.appType.bodyStrong,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val prints = row.printCount?.let { pluralStringResource(Res.plurals.orders_card_prints, it, it) }
                    if (compact) {
                        // Coluna estreita: o cliente ganha a própria linha (e até duas), em vez de ser cortado depois do número.
                        Caption(listOfNotNull(quote.displayNumber, prints).joinToString(" · "), maxLines = 1)
                        Caption(quote.client?.name.orEmpty(), maxLines = 2)
                    } else {
                        Caption(listOfNotNull(quote.displayNumber, quote.client?.name, prints).joinToString(" · "), maxLines = 1)
                    }
                }
            }
            // O mesmo valor da tabela: o que o cliente paga. Na coluna estreita o selo e o valor se empilham; um FlowRow
            // aqui erra a altura intrínseca (a que iguala as colunas) quando quebra a linha, e corta o cartão.
            val value: @Composable () -> Unit = {
                NumericText(
                    quote.totalWithServices.toCurrencyText(quote.currency),
                    style = MaterialTheme.appType.bodyStrong,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    row.deadline?.let { DeadlineBadgeView(it) }
                    value()
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    row.deadline?.let { DeadlineBadgeView(it) }
                    Spacer(Modifier.weight(1f))
                    value()
                }
            }
            // Pagamento, orçamento parado e observação (Leva 11): só aparecem quando há o que dizer. Na coluna estreita os
            // selos se empilham, como o de prazo e o valor, pra nenhum ser cortado.
            val notes = quote.notes?.takeIf { it.isNotBlank() }
            if (row.paymentStatus != null || row.staleDays != null || notes != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                    val badges: @Composable () -> Unit = {
                        row.staleDays?.let { StaleBadgeView(it) }
                        row.paymentStatus?.let { PaymentBadgeView(it) }
                    }
                    if (compact) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(space.xxs)) { badges() }
                    } else {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(space.xs)) { badges() }
                    }
                    notes?.let { NotesIcon(it, onClick = { callbacks.onOpen(row) }) }
                }
            }
        }
        QuoteActionsMenu(
            quote,
            callbacks.actionsFor(row),
            modifier = Modifier.align(Alignment.TopEnd).alpha(if (active) 1f else 0f).background(colors.card, MaterialTheme.shapes.small),
        )
    }
}

@Composable
private fun Caption(text: String, maxLines: Int) {
    if (text.isEmpty()) return
    Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
