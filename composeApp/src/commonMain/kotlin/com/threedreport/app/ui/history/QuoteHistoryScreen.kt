package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.IconLabel
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.app.ui.quote.DeliveryDateDialog
import com.threedreport.app.ui.quote.PrintSettingsDialog
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.RepriceResult

private enum class HistoryViewMode { LIST, KANBAN }

private val KIND_SEGMENT_MIN_WIDTH = 168.dp

/** Tela de Histórico: orçamentos salvos, com o retrato dos valores no momento em que foram salvos. */
@Composable
fun QuoteHistoryScreen(
    viewModel: QuoteHistoryViewModel,
    onEditQuote: (SavedQuote) -> Unit,
    onDuplicateQuote: (SavedQuote) -> Unit,
    onSellProduct: (SavedQuote) -> Unit,
    onCopyToCatalog: (SavedQuote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val copiedId by viewModel.copiedId.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var pendingDelete by remember { mutableStateOf<SavedQuote?>(null) }
    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }
    val pendingExport by viewModel.pendingExport.collectAsState()
    val deliveryDateEditing by viewModel.deliveryDateEditing.collectAsState()
    val today = viewModel.currentEpochDay()
    val showingProducts = filter.kind == QuoteKind.PRODUCT
    val repricing by viewModel.repricing.collectAsState()
    // Coletados pra o aviso "Custos mudaram" dos produtos acompanhar os cadastros na hora.
    val filaments by viewModel.filaments.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val salesChannels by viewModel.salesChannels.collectAsState()
    // Produto não tem andamento, então não tem Kanban: a lista de produtos é sempre lista.
    val effectiveViewMode = if (showingProducts) HistoryViewMode.LIST else viewMode

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Histórico", style = MaterialTheme.typography.titleLarge)

        if (savedQuotes.isEmpty()) {
            Text(
                "Nada salvo ainda. Calcule uma peça na aba Orçamento e salve como pedido de cliente " +
                    "ou como produto do catálogo.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // Dois níveis (decisão 102): o segmentado escolhe o que ver, e "Exibir como", menor e
            // recuado embaixo, é um jeito de ver os pedidos. Na mesma linha, Lista/Kanban pareciam
            // uma terceira e quarta opção do mesmo nível que Pedidos e Produtos.
            val orderCount = savedQuotes.count { it.isOrder }
            SingleChoiceSegmentedButtonRow {
                // Largura mínima: sem ela, o check que entra no segmento escolhido corta o texto.
                SegmentedButton(
                    modifier = Modifier.widthIn(min = KIND_SEGMENT_MIN_WIDTH),
                    selected = !showingProducts,
                    onClick = { viewModel.setKindFilter(QuoteKind.ORDER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Pedidos ($orderCount)") }
                SegmentedButton(
                    modifier = Modifier.widthIn(min = KIND_SEGMENT_MIN_WIDTH),
                    selected = showingProducts,
                    onClick = { viewModel.setKindFilter(QuoteKind.PRODUCT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Produtos (${savedQuotes.size - orderCount})") }
            }
            if (!showingProducts) {
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Exibir como:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = viewMode == HistoryViewMode.LIST,
                        onClick = { viewMode = HistoryViewMode.LIST },
                        label = { IconLabel(AppIcons.ViewList, "Lista") },
                    )
                    FilterChip(
                        selected = viewMode == HistoryViewMode.KANBAN,
                        onClick = { viewMode = HistoryViewMode.KANBAN },
                        label = { IconLabel(AppIcons.ViewKanban, "Kanban") },
                    )
                }
            }

            if (showingProducts) {
                Text(
                    "Produtos são as peças que você oferece, com preço, sem cliente nem andamento. Quando " +
                        "alguém comprar, clique em \"Vender\" pra criar o pedido. \"Exportar catálogo\" leva " +
                        "todos os produtos da lista, ou só os que você marcar.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (viewMode == HistoryViewMode.LIST) {
                Text(
                    "Marque a caixinha de um ou mais orçamentos pra exportar todos juntos num PDF só.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Arraste um card pra outra coluna pra mudar o status (ou use o menu \"Ações\" do card).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // No Kanban, o status já é a própria organização em colunas — filtrar por status ali
            // deixaria as outras colunas vazias sem explicação, então esse filtro some nesse modo.
            // Produto também não tem status pra filtrar.
            HistoryFilterBar(
                filter = filter,
                viewModel = viewModel,
                showStatusFilter = effectiveViewMode == HistoryViewMode.LIST && !showingProducts,
                categories = if (showingProducts) viewModel.productCategories(savedQuotes) else emptyList(),
                hasUncategorized = savedQuotes.any { !it.isOrder && it.category == null },
            )
        }

        if (effectiveViewMode == HistoryViewMode.LIST) {
            val visibleQuotes = viewModel.visibleQuotes(savedQuotes, filter)

            if (selectedIds.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${selectedIds.size} selecionado(s)", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = viewModel::exportSelectedPdf) { IconLabel(AppIcons.PictureAsPdf, "Exportar selecionados (PDF)") }
                    Button(onClick = viewModel::exportCatalogPdf) { IconLabel(AppIcons.GridView, "Exportar catálogo (PDF)") }
                    TextButton(onClick = viewModel::clearSelection) { Text("Cancelar seleção") }
                }
            } else if (showingProducts && visibleQuotes.isNotEmpty()) {
                Button(onClick = viewModel::exportCatalogPdf) {
                    IconLabel(
                        AppIcons.GridView,
                        if (visibleQuotes.size == 1) "Exportar catálogo com 1 produto (PDF)" else "Exportar catálogo com os ${visibleQuotes.size} produtos (PDF)",
                    )
                }
            }

            if (savedQuotes.isNotEmpty() && visibleQuotes.isEmpty()) {
                val hasAnyOfThisKind = savedQuotes.any { it.kind == filter.kind }
                EmptyState(
                    when {
                        hasAnyOfThisKind && showingProducts -> "Nenhum produto encontrado com esse filtro."
                        hasAnyOfThisKind -> "Nenhum orçamento encontrado com esse filtro."
                        showingProducts -> "Nenhum produto no catálogo ainda. Na aba Orçamento, escolha " +
                            "\"Produto do catálogo\" antes de salvar, ou use \"Guardar no catálogo\" no menu " +
                            "\"Ações\" de um pedido."
                        else -> "Nenhum pedido ainda. Na aba Orçamento, escolha \"Pedido de cliente\" antes de " +
                            "salvar, ou clique em \"Vender\" num produto."
                    },
                )
            }

            visibleQuotes.forEach { savedQuote ->
                SavedQuoteRow(
                    savedQuote = savedQuote,
                    photoBytes = savedQuote.photoFileName?.let { viewModel.photoBytes(savedQuote) },
                    justCopied = copiedId == savedQuote.id,
                    selected = savedQuote.id in selectedIds,
                    onToggleSelected = { viewModel.toggleSelection(savedQuote.id) },
                    onDownloadPhoto = { viewModel.downloadPhoto(savedQuote) },
                    onDownloadStl = { viewModel.downloadStl(savedQuote) },
                    onExportPdf = { viewModel.exportPdf(savedQuote) },
                    onCopy = { viewModel.copyQuoteToClipboard(savedQuote) },
                    onEdit = { onEditQuote(savedQuote) },
                    onDuplicate = { onDuplicateQuote(savedQuote) },
                    onSell = { onSellProduct(savedQuote) },
                    onCopyToCatalog = { onCopyToCatalog(savedQuote) },
                    onConvertToOrder = if (viewModel.canConvertToOrder(savedQuote, savedQuotes)) {
                        { viewModel.convertToOrder(savedQuote.id) }
                    } else {
                        null
                    },
                    onDelete = { pendingDelete = savedQuote },
                    repriceResult = if (savedQuote.isOrder) {
                        null
                    } else {
                        viewModel.repriceFor(savedQuote, filaments, printers, settings, salesChannels)
                    },
                    onReprice = { viewModel.startRepricing(savedQuote) },
                    onStatusChange = { status -> viewModel.updateStatus(savedQuote.id, status) },
                    onUpdatePrintSettings = { settings -> viewModel.updatePrintSettings(savedQuote.id, settings) },
                    onOpenWhatsApp = { viewModel.openInWhatsApp(savedQuote) },
                    onSaveImage = { viewModel.saveShareableImage(savedQuote) },
                    todayEpochDay = today,
                    onEditDeliveryDate = { viewModel.startEditingDeliveryDate(savedQuote) },
                )
            }
        } else if (savedQuotes.isNotEmpty()) {
            val kanbanQuotes = viewModel.visibleQuotes(savedQuotes, filter.copy(status = null))
            KanbanBoard(
                quotes = kanbanQuotes,
                photoBytesFor = { savedQuote -> savedQuote.photoFileName?.let { viewModel.photoBytes(savedQuote) } },
                onStatusChange = viewModel::updateStatus,
                onEdit = onEditQuote,
                onDuplicate = onDuplicateQuote,
                onCopyToCatalog = onCopyToCatalog,
                onDelete = { pendingDelete = it },
                onUpdatePrintSettings = { savedQuote, settings -> viewModel.updatePrintSettings(savedQuote.id, settings) },
                todayEpochDay = today,
                onEditDeliveryDate = viewModel::startEditingDeliveryDate,
            )
        }
    }

    pendingDelete?.let { savedQuote ->
        ConfirmDialog(
            title = if (savedQuote.isOrder) "Excluir orçamento?" else "Excluir produto?",
            message = if (savedQuote.isOrder) {
                "\"${savedQuote.name}\" será removido do histórico, junto com a foto e o STL salvos (se houver). Essa ação não pode ser desfeita."
            } else {
                "\"${savedQuote.name}\" será removido do catálogo, junto com a foto e o STL salvos (se houver). " +
                    "Pedidos já vendidos a partir dele não mudam. Essa ação não pode ser desfeita."
            },
            onConfirm = {
                viewModel.delete(savedQuote.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    deliveryDateEditing?.let { savedQuote ->
        DeliveryDateDialog(
            quoteName = savedQuote.name,
            initial = savedQuote.deliveryDateEpochDay,
            onDismiss = viewModel::cancelEditingDeliveryDate,
            onSave = viewModel::saveDeliveryDate,
        )
    }

    pendingExport?.let { pending -> OverdueExportDialog(pending, today, viewModel) }

    repricing?.let { RepriceDialog(it, viewModel) }
}

/**
 * Aviso no card do produto quando o preço de hoje não é o guardado (decisão 102). Aparece só
 * quando há o que fazer: preço antigo com custos iguais continua certo e não gera aviso.
 */
@Composable
private fun RepriceNotice(result: RepriceResult, onReprice: () -> Unit) {
    when (result) {
        is RepriceResult.Repriced -> if (result.changed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Custos mudaram: hoje o preço calculado seria ${(result.quote.tableSalePrice ?: result.quote.salePrice).toMoney()}.",
                    style = MaterialTheme.typography.bodySmall,
                    // Âmbar da paleta (o app não define `tertiary`, que cairia no roxo padrão do M3).
                    color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(onClick = onReprice) { IconLabel(AppIcons.Sync, "Atualizar preço") }
            }
        }
        is RepriceResult.Unavailable -> Text(
            when (result.reason) {
                RepriceResult.Reason.FILAMENT_MISSING -> "O filamento ${result.missingName.orEmpty()} não está mais cadastrado"
                RepriceResult.Reason.PRINTER_MISSING -> result.missingName?.let { "A impressora $it não está mais cadastrada" }
                    ?: "Este produto não guardou a impressora"
                RepriceResult.Reason.CHANNEL_MISSING -> "O canal ${result.missingName.orEmpty()} não está mais cadastrado"
                RepriceResult.Reason.INVALID -> "Não deu pra recalcular com os cadastros de hoje"
            } + ": abra em Editar pra escolher de novo e conferir o preço.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Antes e depois de atualizar o preço de um produto, pra confirmar antes de gravar. */
@Composable
private fun RepriceDialog(repricing: Repricing, viewModel: QuoteHistoryViewModel) {
    val before = repricing.product.quote
    val after = repricing.newQuote
    AlertDialog(
        onDismissRequest = viewModel::cancelRepricing,
        title = { Text("Atualizar o preço de \"${repricing.product.name}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Recalculado com os filamentos, a impressora e as configurações de hoje, com as mesmas " +
                        "medidas da peça.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                BeforeAfterLine("Preço calculado", before.tableSalePrice ?: before.salePrice, after.tableSalePrice ?: after.salePrice)
                BeforeAfterLine("Custo de produção", before.productionCost, after.productionCost)
                BeforeAfterLine("Lucro", before.profit, after.profit)
                if (after.isNegotiated) {
                    Text(
                        "Preço anunciado: ${after.salePrice.toMoney()} (mantido). Pra mudar, abra o produto em Editar.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (after.profit < 0) {
                    Text(
                        "Com os custos de hoje, o preço anunciado dá prejuízo de ${(-after.profit).toMoney()}. " +
                            "O mínimo pra não sair no negativo é ${after.breakEvenSalePrice.toMoney()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::confirmRepricing) { Text("Atualizar") } },
        dismissButton = { TextButton(onClick = viewModel::cancelRepricing) { Text("Cancelar") } },
    )
}

@Composable
private fun BeforeAfterLine(label: String, before: Double, after: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${before.toMoney()} → ${after.toMoney()}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

/**
 * Aviso antes de mandar pro cliente um orçamento com prazo já vencido. Com um orçamento só, o
 * caminho principal é "Alterar prazo" (abre o diálogo de prazo); na exportação em lote não dá pra
 * editar vários prazos de uma vez, então o aviso lista quais estão vencidos e oferece só enviar
 * assim mesmo ou cancelar.
 */
@Composable
private fun OverdueExportDialog(pending: PendingExport, todayEpochDay: Long, viewModel: QuoteHistoryViewModel) {
    val overdue = pending.quotes.filter { it.isDeliveryOverdue(todayEpochDay) }
    val single = pending.quotes.singleOrNull()

    AlertDialog(
        onDismissRequest = viewModel::dismissPendingExport,
        title = { Text("O prazo de entrega já passou") },
        text = {
            Text(
                if (single != null) {
                    "\"${single.name}\" promete entrega até ${formatDate(single.deliveryDateEpochDay!!)}, e essa data já passou. " +
                        "Se enviar assim, o cliente recebe um prazo vencido."
                } else {
                    "${overdue.size} dos orçamentos selecionados têm prazo vencido: " +
                        overdue.joinToString { "\"${it.name}\" (${formatDate(it.deliveryDateEpochDay!!)})" } +
                        ". Se exportar assim, o cliente recebe esses prazos vencidos."
                },
            )
        },
        confirmButton = {
            if (single != null) {
                TextButton(onClick = viewModel::changeDateOfPendingExport) { Text("Alterar prazo") }
            } else {
                TextButton(onClick = viewModel::dismissPendingExport) { Text("Cancelar") }
            }
        },
        dismissButton = { TextButton(onClick = viewModel::confirmPendingExport) { Text("Enviar assim mesmo") } },
    )
}

@Composable
private fun HistoryFilterBar(
    filter: HistoryFilter,
    viewModel: QuoteHistoryViewModel,
    showStatusFilter: Boolean = true,
    categories: List<String> = emptyList(),
    hasUncategorized: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filter.query,
            onValueChange = viewModel::setSearchQuery,
            // Produto não tem cliente, então a busca dele é só pelo nome.
            label = { Text(if (filter.kind == QuoteKind.PRODUCT) "Buscar por nome" else "Buscar por nome ou cliente") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodPreset.entries.forEach { preset ->
                FilterChip(
                    selected = preset == filter.period,
                    onClick = { viewModel.setPeriodFilter(preset) },
                    label = { Text(preset.label) },
                )
            }
        }

        if (showStatusFilter) {
            StatusFilterDropdown(selected = filter.status, onSelect = viewModel::setStatusFilter)
        }

        // Categorias dos produtos (decisão 102). Só aparecem quando existe alguma; "Sem categoria"
        // só quando há produtos dos dois jeitos, senão seria igual a "Todas".
        if (categories.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = filter.category == CategoryFilter.All,
                    onClick = { viewModel.setCategoryFilter(CategoryFilter.All) },
                    label = { Text("Todas as categorias") },
                )
                categories.forEach { category ->
                    val option = CategoryFilter.Named(category)
                    FilterChip(
                        selected = (filter.category as? CategoryFilter.Named)?.name.equals(category, ignoreCase = true),
                        onClick = { viewModel.setCategoryFilter(option) },
                        label = { Text(category) },
                    )
                }
                if (hasUncategorized) {
                    FilterChip(
                        selected = filter.category == CategoryFilter.None,
                        onClick = { viewModel.setCategoryFilter(CategoryFilter.None) },
                        label = { Text("Sem categoria") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterDropdown(selected: OrderStatus?, onSelect: (OrderStatus?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selected?.label ?: "Todos os status",
            onValueChange = {},
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos os status") }, onClick = { onSelect(null); expanded = false })
            OrderStatus.entries.forEach { status ->
                DropdownMenuItem(text = { Text(status.label) }, onClick = { onSelect(status); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(status: OrderStatus, onStatusChange: (OrderStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(onClick = { expanded = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).background(status.progressColor(), CircleShape))
                Text("Status: ${status.label}")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OrderStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(option.progressColor(), CircleShape))
                            Text(option.label)
                        }
                    },
                    onClick = {
                        onStatusChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SavedQuoteRow(
    savedQuote: SavedQuote,
    photoBytes: ByteArray?,
    justCopied: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onDownloadPhoto: () -> Unit,
    onDownloadStl: () -> Unit,
    onExportPdf: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onSell: () -> Unit,
    onCopyToCatalog: () -> Unit,
    onConvertToOrder: (() -> Unit)?,
    onDelete: () -> Unit,
    repriceResult: RepriceResult?,
    onReprice: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    onUpdatePrintSettings: (PrintSettings?) -> Unit,
    onOpenWhatsApp: () -> Unit,
    onSaveImage: () -> Unit,
    todayEpochDay: Long,
    onEditDeliveryDate: () -> Unit,
) {
    var showPrintSettingsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val isProduct = !savedQuote.isOrder

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })

            if (photoBytes != null) {
                Image(
                    bitmap = decodeImageBitmap(photoBytes),
                    contentDescription = savedQuote.name,
                    modifier = Modifier.size(72.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(savedQuote.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatDateTime(savedQuote.savedAtEpochMillis), style = MaterialTheme.typography.bodySmall)
                    savedQuote.lastEditedEpochMillis?.let { editedAt ->
                        Text(
                            "· Editado em ${formatDateTime(editedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    buildAnnotatedString {
                        append("Peso: ")
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.filamentWeightGrams.toWeightText()) }
                        append(" · Produção: ")
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.productionCost.toMoney()) }
                        append(" · Venda: ")
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.salePrice.toMoney()) }
                        savedQuote.quote.tableSalePrice?.let { tablePrice ->
                            append(
                                when {
                                    isProduct -> " · Anunciado (calculado "
                                    savedQuote.soldAtCatalogPrice -> " · Preço do catálogo (calculado "
                                    savedQuote.quote.negotiatedDiscount < 0 -> " · Acima da tabela ("
                                    else -> " · Negociado (tabela "
                                },
                            )
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(tablePrice.toMoney()) }
                            append(")")
                        }
                        append(" · Lucro: ")
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.profit.toMoney()) }
                        if (savedQuote.quote.quantity > 1) {
                            append(" · Quantidade: ")
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("${savedQuote.quote.quantity}") }
                            append(" (")
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.unitSalePrice.toMoney()) }
                            append(" cada)")
                        }
                        savedQuote.quote.channelName?.let { channelName ->
                            append(" · Canal: ")
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(channelName) }
                        }
                        if (savedQuote.shippingCost > 0) {
                            append(" · Frete: ")
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.shippingCost.toMoney()) }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                savedQuote.category?.let { category ->
                    Text("Categoria: $category", style = MaterialTheme.typography.bodySmall)
                }
                repriceResult?.let { RepriceNotice(it, onReprice) }
                savedQuote.sourceLink?.let { link ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Link interno (não exportado):", style = MaterialTheme.typography.bodySmall)
                        LinkText(text = link, url = link)
                    }
                }
                savedQuote.client?.let { client ->
                    Text(
                        "Cliente (uso interno): ${client.name}" + (client.contact?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                savedQuote.quote.job.filamentColor?.let { color ->
                    Text("Cor: ${color.displayLabel()}", style = MaterialTheme.typography.bodySmall)
                }

                // Produto não tem andamento nem prazo (decisão 101).
                if (isProduct) {
                    Text("Produto do catálogo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDropdown(status = savedQuote.status, onStatusChange = onStatusChange)
                        DeliveryBadge(savedQuote, todayEpochDay)
                    }
                }

                // FlowRow: com o "Vender" do produto, os botões não cabem numa linha em janela estreita.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    // Vender é a ação principal do produto: um clique a partir do card.
                    if (isProduct) {
                        Button(onClick = onSell) { IconLabel(AppIcons.Sell, "Vender") }
                    }
                    TextButton(onClick = onExportPdf) { IconLabel(AppIcons.PictureAsPdf, "Exportar PDF") }
                    TextButton(onClick = onCopy) { IconLabel(AppIcons.ContentCopy, if (justCopied) "Copiado!" else "Copiar") }
                    TextButton(onClick = onEdit) { IconLabel(AppIcons.Edit, "Editar") }
                    Box {
                        TextButton(onClick = { showMenu = true }) { IconLabel(AppIcons.MoreVert, "Ações") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Duplicar") }, leadingIcon = { Icon(AppIcons.FileCopy, contentDescription = null) }, onClick = { showMenu = false; onDuplicate() })
                            if (isProduct) {
                                DropdownMenuItem(
                                    text = { Text("Atualizar preço") },
                                    leadingIcon = { Icon(AppIcons.Sync, contentDescription = null) },
                                    enabled = repriceResult is RepriceResult.Repriced,
                                    onClick = { showMenu = false; onReprice() },
                                )
                                onConvertToOrder?.let { convert ->
                                    DropdownMenuItem(
                                        text = { Text("Transformar em pedido") },
                                        leadingIcon = { Icon(AppIcons.RequestQuote, contentDescription = null) },
                                        onClick = { showMenu = false; convert() },
                                    )
                                }
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Guardar no catálogo") },
                                    leadingIcon = { Icon(AppIcons.Storefront, contentDescription = null) },
                                    onClick = { showMenu = false; onCopyToCatalog() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (savedQuote.deliveryDateEpochDay == null) "Definir prazo de entrega" else "Alterar prazo de entrega") },
                                    leadingIcon = { Icon(AppIcons.Event, contentDescription = null) },
                                    onClick = { showMenu = false; onEditDeliveryDate() },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (savedQuote.printSettings == null) "Adicionar configurações de impressão" else "Configurações de impressão") },
                                leadingIcon = { Icon(AppIcons.Tune, contentDescription = null) },
                                onClick = { showMenu = false; showPrintSettingsDialog = true },
                            )
                            if (photoBytes != null) {
                                DropdownMenuItem(text = { Text("Baixar foto") }, leadingIcon = { Icon(AppIcons.Image, contentDescription = null) }, onClick = { showMenu = false; onDownloadPhoto() })
                            }
                            if (savedQuote.stlFileName != null) {
                                DropdownMenuItem(text = { Text("Baixar STL") }, leadingIcon = { Icon(AppIcons.Download, contentDescription = null) }, onClick = { showMenu = false; onDownloadStl() })
                            }
                            DropdownMenuItem(
                                text = { Text("Abrir no WhatsApp") },
                                leadingIcon = { Icon(AppIcons.Chat, contentDescription = null) },
                                onClick = { showMenu = false; onOpenWhatsApp() },
                            )
                            DropdownMenuItem(
                                text = { Text(if (isProduct) "Salvar imagem pro WhatsApp" else "Salvar orçamento pro WhatsApp") },
                                leadingIcon = { Icon(AppIcons.AddPhotoAlternate, contentDescription = null) },
                                onClick = { showMenu = false; onSaveImage() },
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(AppIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                            )
                        }
                    }
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
