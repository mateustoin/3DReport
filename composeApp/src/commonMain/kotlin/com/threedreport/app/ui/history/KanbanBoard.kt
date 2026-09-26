package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.threedreport.app.platform.HorizontalScrollbarFor
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import kotlin.math.roundToInt

/**
 * Quadro Kanban do Histórico: uma coluna por etapa do fluxo ([OrderStatus.PIPELINE]), arrastando o
 * card entre colunas pra mudar o status — visão alternativa à lista, sobre o mesmo dado. O menu "Ações"
 * de cada card é o mesmo da lista ([QuoteActionsMenu]), com "Mover para" pra quem não quer arrastar.
 *
 * Sem scroll vertical próprio: a rolagem é a mesma da tela de Histórico por trás (evita rolagem
 * aninhada). Na horizontal, uma barra de rolagem visível: com a roda do mouse rolando na vertical, quem
 * não tem trackpad não descobria as colunas da direita.
 *
 * As posições usadas pro hit-test do arrasto vêm de [LayoutCoordinates.positionInWindow] + o
 * tamanho medido, não de `boundsInWindow()` — esse último é recortado pela área visível de todos
 * os ancestrais (inclusive o scroll vertical da tela de Histórico), então uma coluna parcialmente
 * fora da área visível reportava um retângulo bem menor que sua altura real, e soltar o card mais
 * abaixo na coluna não contava como estar "dentro" dela. Cada coluna é esticada pra altura da mais alta
 * (`IntrinsicSize.Max` + `fillMaxHeight()`), pra soltar num espaço vazio da coluna também contar.
 *
 * @param deliveredFooter embaixo da coluna Entregue: o "ver mais" dos entregues antigos.
 */
@Composable
fun KanbanBoard(
    quotes: List<SavedQuote>,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    onStatusChange: (String, OrderStatus) -> Unit,
    actionsFor: (SavedQuote) -> QuoteActions,
    todayEpochDay: Long,
    deliveredFooter: @Composable () -> Unit = {},
) {
    // Posição+tamanho (em coordenadas de janela) de cada coluna, pra saber sobre qual coluna um card
    // foi solto ou está sendo arrastado por cima.
    val columnBounds = remember { mutableStateMapOf<OrderStatus, Rect>() }
    val quotesByStatus = remember(quotes) { quotes.groupBy { it.status } }
    var hoveredStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var draggedFromStatus by remember { mutableStateOf<OrderStatus?>(null) }
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OrderStatus.PIPELINE.forEach { status ->
                KanbanColumn(
                    status = status,
                    quotes = quotesByStatus[status].orEmpty(),
                    loadThumbnail = loadThumbnail,
                    columnBounds = columnBounds,
                    onBoundsChanged = { bounds -> columnBounds[status] = bounds },
                    isDropTarget = hoveredStatus == status && draggedFromStatus != status,
                    // A coluna de onde o card saiu fica por cima das outras enquanto ele é arrastado:
                    // senão o card passava por baixo das colunas à direita.
                    isDragSource = draggedFromStatus == status,
                    onDragHover = { hovered -> hoveredStatus = hovered },
                    onDragSourceChange = { source -> draggedFromStatus = source },
                    onStatusChange = { savedQuote, newStatus -> onStatusChange(savedQuote.id, newStatus) },
                    actionsFor = actionsFor,
                    todayEpochDay = todayEpochDay,
                    footer = if (status == OrderStatus.ENTREGUE) deliveredFooter else ({}),
                )
            }
        }
        HorizontalScrollbarFor(scrollState, modifier = Modifier.fillMaxWidth())
    }
}

private fun LayoutCoordinates.fullBoundsInWindow(): Rect = Rect(positionInWindow(), size.toSize())

@Composable
private fun KanbanColumn(
    status: OrderStatus,
    quotes: List<SavedQuote>,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    columnBounds: Map<OrderStatus, Rect>,
    onBoundsChanged: (Rect) -> Unit,
    isDropTarget: Boolean,
    isDragSource: Boolean,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    onStatusChange: (SavedQuote, OrderStatus) -> Unit,
    actionsFor: (SavedQuote) -> QuoteActions,
    todayEpochDay: Long,
    footer: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .zIndex(if (isDragSource) 1f else 0f)
            .width(260.dp)
            .fillMaxHeight()
            .background(
                if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(8.dp),
            )
            .padding(6.dp)
            .onGloballyPositioned { onBoundsChanged(it.fullBoundsInWindow()) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(status.progressColor(), CircleShape))
            Text("${status.label} (${quotes.size})", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
        if (quotes.isEmpty()) {
            Text(
                "Nenhum pedido aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        quotes.forEach { savedQuote ->
            androidx.compose.runtime.key(savedQuote.id) {
                KanbanCard(
                    savedQuote = savedQuote,
                    loadThumbnail = loadThumbnail,
                    columnBounds = columnBounds,
                    onDragHover = onDragHover,
                    onDragSourceChange = onDragSourceChange,
                    onStatusChange = { newStatus -> onStatusChange(savedQuote, newStatus) },
                    actions = actionsFor(savedQuote),
                    todayEpochDay = todayEpochDay,
                )
            }
        }
        if (isDropTarget) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
        }
        footer()
    }
}

@Composable
private fun KanbanCard(
    savedQuote: SavedQuote,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    columnBounds: Map<OrderStatus, Rect>,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    actions: QuoteActions,
    todayEpochDay: Long,
) {
    var dragOffset by remember(savedQuote.id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(savedQuote.id) { mutableStateOf(false) }
    var liveBounds by remember(savedQuote.id) { mutableStateOf<Rect?>(null) }

    fun targetStatusUnderPointer(): OrderStatus? {
        val point = liveBounds?.center ?: return null
        return columnBounds.entries.firstOrNull { it.value.contains(point) }?.key
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned { liveBounds = it.fullBoundsInWindow() }
            .pointerInput(savedQuote.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragSourceChange(savedQuote.status)
                        onDragHover(savedQuote.status)
                    },
                    onDragEnd = {
                        isDragging = false
                        val targetStatus = targetStatusUnderPointer()
                        if (targetStatus != null && targetStatus != savedQuote.status) {
                            onStatusChange(targetStatus)
                        }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoThumbnail(savedQuote, size = 48.dp, load = loadThumbnail)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(savedQuote.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    Text(
                        listOfNotNull(savedQuote.displayNumber, savedQuote.client?.name, printCountLabel(savedQuote)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // O mesmo valor da lista: o que o cliente paga.
                    NumericText(savedQuote.totalWithServices.toCurrencyText(savedQuote.currency), style = MaterialTheme.typography.bodyMedium)
                    DeliveryBadge(savedQuote, todayEpochDay)
                }
            }
            QuoteActionsMenu(savedQuote, actions)
        }
    }
}
