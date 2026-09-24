package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.quote.PrintSettingsDialog
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
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
 *
 * As posições usadas pro hit-test do arrasto vêm de [LayoutCoordinates.positionInWindow] + o
 * tamanho medido, não de `boundsInWindow()` — esse último é recortado pela área visível de todos
 * os ancestrais (inclusive o scroll vertical da tela de Histórico), então uma coluna parcialmente
 * fora da área visível reportava um retângulo bem menor que sua altura real (só o título "cabia"),
 * e soltar o card mais abaixo na coluna não contava como estar "dentro" dela. Além disso, cada
 * coluna esticada pra altura da mais alta (`Row(Modifier.height(IntrinsicSize.Max))` +
 * `fillMaxHeight()` em cada [KanbanColumn]) — sem isso, uma coluna com poucos cards media só a
 * altura do próprio conteúdo, então soltar num espaço "vazio" da coluna (abaixo do último card, ou
 * numa coluna sem nenhum item) caía fora do retângulo conhecido.
 */
@Composable
fun KanbanBoard(
    quotes: List<SavedQuote>,
    photoBytesFor: (SavedQuote) -> ByteArray?,
    onStatusChange: (String, OrderStatus) -> Unit,
    onEdit: (SavedQuote) -> Unit,
    onDuplicate: (SavedQuote) -> Unit,
    onDelete: (SavedQuote) -> Unit,
    onUpdatePrintSettings: (SavedQuote, PrintSettings?) -> Unit,
    todayEpochDay: Long,
    onEditDeliveryDate: (SavedQuote) -> Unit,
) {
    // Posição+tamanho (em coordenadas de janela) de cada coluna, atualizado a cada posicionamento —
    // usado como referência comum (independente de qual composable está aninhado onde) pra saber
    // sobre qual coluna um card foi solto ou está sendo arrastado por cima.
    val columnBounds = remember { mutableStateMapOf<OrderStatus, Rect>() }
    val quotesByStatus = remember(quotes) { quotes.groupBy { it.status } }
    // Coluna sob o ponteiro durante um arrasto em andamento (null se nada está sendo arrastado, ou
    // se o ponteiro não está sobre nenhuma coluna) — usado só pra desenhar o indicador visual.
    var hoveredStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var draggedFromStatus by remember { mutableStateOf<OrderStatus?>(null) }

    // IntrinsicSize.Max faz o Row medir a altura da coluna mais alta e esticar as outras até lá
    // (fillMaxHeight em cada KanbanColumn) — sem isso, uma coluna com poucos cards tinha uma área
    // de soltar bem menor que as vizinhas mais cheias, mesmo ocupando a mesma largura na tela.
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OrderStatus.entries.forEach { status ->
            KanbanColumn(
                status = status,
                quotes = quotesByStatus[status].orEmpty(),
                photoBytesFor = photoBytesFor,
                columnBounds = columnBounds,
                onBoundsChanged = { bounds -> columnBounds[status] = bounds },
                isDropTarget = hoveredStatus == status && draggedFromStatus != status,
                onDragHover = { hovered -> hoveredStatus = hovered },
                onDragSourceChange = { source -> draggedFromStatus = source },
                onStatusChange = { savedQuote, newStatus -> onStatusChange(savedQuote.id, newStatus) },
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                onUpdatePrintSettings = onUpdatePrintSettings,
                todayEpochDay = todayEpochDay,
                onEditDeliveryDate = onEditDeliveryDate,
            )
        }
    }
}

private fun LayoutCoordinates.fullBoundsInWindow(): Rect = Rect(positionInWindow(), size.toSize())

@Composable
private fun KanbanColumn(
    status: OrderStatus,
    quotes: List<SavedQuote>,
    photoBytesFor: (SavedQuote) -> ByteArray?,
    columnBounds: Map<OrderStatus, Rect>,
    onBoundsChanged: (Rect) -> Unit,
    isDropTarget: Boolean,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    onStatusChange: (SavedQuote, OrderStatus) -> Unit,
    onEdit: (SavedQuote) -> Unit,
    onDuplicate: (SavedQuote) -> Unit,
    onDelete: (SavedQuote) -> Unit,
    onUpdatePrintSettings: (SavedQuote, PrintSettings?) -> Unit,
    todayEpochDay: Long,
    onEditDeliveryDate: (SavedQuote) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(
                if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
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
                "Nenhum orçamento aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        quotes.forEach { savedQuote ->
            KanbanCard(
                savedQuote = savedQuote,
                photoBytes = photoBytesFor(savedQuote),
                columnBounds = columnBounds,
                onDragHover = onDragHover,
                onDragSourceChange = onDragSourceChange,
                onStatusChange = { newStatus -> onStatusChange(savedQuote, newStatus) },
                onEdit = { onEdit(savedQuote) },
                onDuplicate = { onDuplicate(savedQuote) },
                onDelete = { onDelete(savedQuote) },
                onUpdatePrintSettings = { settings -> onUpdatePrintSettings(savedQuote, settings) },
                todayEpochDay = todayEpochDay,
                onEditDeliveryDate = { onEditDeliveryDate(savedQuote) },
            )
        }
        if (isDropTarget) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun KanbanCard(
    savedQuote: SavedQuote,
    photoBytes: ByteArray?,
    columnBounds: Map<OrderStatus, Rect>,
    onDragHover: (OrderStatus?) -> Unit,
    onDragSourceChange: (OrderStatus?) -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUpdatePrintSettings: (PrintSettings?) -> Unit,
    todayEpochDay: Long,
    onEditDeliveryDate: () -> Unit,
) {
    var dragOffset by remember(savedQuote.id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(savedQuote.id) { mutableStateOf(false) }
    var liveBounds by remember(savedQuote.id) { mutableStateOf<Rect?>(null) }
    var showMenu by remember(savedQuote.id) { mutableStateOf(false) }
    var showPrintSettingsDialog by remember(savedQuote.id) { mutableStateOf(false) }

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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (photoBytes != null) {
                    Image(
                        bitmap = decodeImageBitmap(photoBytes),
                        contentDescription = savedQuote.name,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(savedQuote.name, style = MaterialTheme.typography.titleSmall)
                    savedQuote.client?.let { client ->
                        Text(client.name, style = MaterialTheme.typography.bodySmall)
                    }
                    NumericText(savedQuote.totalWithServices.toMoney(), style = MaterialTheme.typography.bodyMedium)
                    DeliveryBadge(savedQuote, todayEpochDay)
                }
            }

            Box {
                TextButton(onClick = { showMenu = true }) { Text("⋮ Ações") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { showMenu = false; onDuplicate() })
                    DropdownMenuItem(
                        text = { Text(if (savedQuote.deliveryDateEpochDay == null) "Definir prazo de entrega" else "Alterar prazo de entrega") },
                        onClick = { showMenu = false; onEditDeliveryDate() },
                    )
                    DropdownMenuItem(
                        text = { Text("Configurações de impressão") },
                        onClick = { showMenu = false; showPrintSettingsDialog = true },
                    )
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }

    if (showPrintSettingsDialog) {
        PrintSettingsDialog(
            initial = savedQuote.printSettings ?: PrintSettings(),
            onDismiss = { showPrintSettingsDialog = false },
            onSave = { settings -> onUpdatePrintSettings(settings.takeUnless { it.isEmpty }) },
        )
    }
}
