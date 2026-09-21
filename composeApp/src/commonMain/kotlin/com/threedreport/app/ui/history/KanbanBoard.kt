package com.threedreport.app.ui.history

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.threedreport.app.ui.format.toMoney
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import kotlin.math.roundToInt

/**
 * Quadro Kanban do Histórico: uma coluna por [OrderStatus], arrastando o card entre colunas pra
 * mudar o status — visão alternativa à lista, sobre o mesmo dado (não a substitui). O menu "⋮" de
 * cada card cobre o mesmo caso caso o arrasto não seja preciso o bastante numa tela/mouse
 * específico — ambos os caminhos levam à mesma mudança de status.
 *
 * Sem scroll vertical próprio: a rolagem é a mesma da tela de Histórico por trás (evita rolagem
 * aninhada, mais simples e já suficiente pro volume de orçamentos esperado).
 */
@Composable
fun KanbanBoard(
    quotes: List<SavedQuote>,
    onStatusChange: (String, OrderStatus) -> Unit,
    onEdit: (SavedQuote) -> Unit,
    onDuplicate: (SavedQuote) -> Unit,
    onDelete: (SavedQuote) -> Unit,
) {
    // boundsInWindow() de cada coluna, atualizado a cada posicionamento — usado como referência
    // comum (independente de qual composable está aninhado onde) pra saber sobre qual coluna um
    // card foi solto.
    val columnBounds = remember { mutableStateMapOf<OrderStatus, Rect>() }
    val quotesByStatus = remember(quotes) { quotes.groupBy { it.status } }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OrderStatus.entries.forEach { status ->
            KanbanColumn(
                status = status,
                quotes = quotesByStatus[status].orEmpty(),
                columnBounds = columnBounds,
                onBoundsChanged = { bounds -> columnBounds[status] = bounds },
                onStatusChange = { savedQuote, newStatus -> onStatusChange(savedQuote.id, newStatus) },
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    status: OrderStatus,
    quotes: List<SavedQuote>,
    columnBounds: Map<OrderStatus, Rect>,
    onBoundsChanged: (Rect) -> Unit,
    onStatusChange: (SavedQuote, OrderStatus) -> Unit,
    onEdit: (SavedQuote) -> Unit,
    onDuplicate: (SavedQuote) -> Unit,
    onDelete: (SavedQuote) -> Unit,
) {
    Column(
        modifier = Modifier.width(260.dp).onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("${status.label} (${quotes.size})", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider()
        if (quotes.isEmpty()) {
            Text(
                "Nenhum orçamento aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        quotes.forEach { savedQuote ->
            KanbanCard(
                savedQuote = savedQuote,
                columnBounds = columnBounds,
                onStatusChange = { newStatus -> onStatusChange(savedQuote, newStatus) },
                onEdit = { onEdit(savedQuote) },
                onDuplicate = { onDuplicate(savedQuote) },
                onDelete = { onDelete(savedQuote) },
            )
        }
    }
}

@Composable
private fun KanbanCard(
    savedQuote: SavedQuote,
    columnBounds: Map<OrderStatus, Rect>,
    onStatusChange: (OrderStatus) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var dragOffset by remember(savedQuote.id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(savedQuote.id) { mutableStateOf(false) }
    var liveBounds by remember(savedQuote.id) { mutableStateOf<Rect?>(null) }
    var showMenu by remember(savedQuote.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned { liveBounds = it.boundsInWindow() }
            .pointerInput(savedQuote.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        val center = liveBounds?.center
                        val targetStatus = center?.let { point ->
                            columnBounds.entries.firstOrNull { it.value.contains(point) }?.key
                        }
                        if (targetStatus != null && targetStatus != savedQuote.status) {
                            onStatusChange(targetStatus)
                        }
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(savedQuote.name, style = MaterialTheme.typography.titleSmall)
            savedQuote.client?.let { client ->
                Text(client.name, style = MaterialTheme.typography.bodySmall)
            }
            Text(savedQuote.totalWithServices.toMoney(), style = MaterialTheme.typography.bodyMedium)

            Box {
                TextButton(onClick = { showMenu = true }) { Text("⋮ Ações") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { showMenu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}
