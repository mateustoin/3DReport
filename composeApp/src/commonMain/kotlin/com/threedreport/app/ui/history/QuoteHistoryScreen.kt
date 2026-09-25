package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.IconLabel
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.ShowNotice
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.filaments.displayText
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.quote.DeliveryDateDialog
import com.threedreport.app.ui.quote.PrintSettingsDialog
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.RepriceResult
import kotlinx.coroutines.delay

private enum class HistoryViewMode { LIST, KANBAN }

/**
 * Pedidos ou Catálogo (decisão 111): a mesma tela, cada uma com o próprio [QuoteHistoryViewModel] e o tipo
 * fixo nele. Mostra o que foi salvo com o retrato dos valores do momento em que foi salvo.
 */
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
    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }
    var showAllDelivered by remember { mutableStateOf(false) }
    var printSettingsFor by remember { mutableStateOf<SavedQuote?>(null) }
    val pendingExport by viewModel.pendingExport.collectAsState()
    val deliveryDateEditing by viewModel.deliveryDateEditing.collectAsState()
    val detailsEditing by viewModel.detailsEditing.collectAsState()
    val repricing by viewModel.repricing.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val today = viewModel.currentEpochDay()
    val showingProducts = filter.kind == QuoteKind.PRODUCT
    // Coletados pra o aviso "Custos mudaram" dos produtos acompanhar os cadastros na hora.
    val filaments by viewModel.filaments.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val salesChannels by viewModel.salesChannels.collectAsState()
    // Produto não tem andamento, então não tem Kanban: a lista de produtos é sempre lista.
    val effectiveViewMode = if (showingProducts) HistoryViewMode.LIST else viewMode

    // Recalculado quando a lista ou o filtro mudam, e não a cada redesenho (decisão 108).
    val visibleQuotes = remember(savedQuotes, filter) { viewModel.visibleQuotes(savedQuotes, filter) }
    val categories = remember(savedQuotes) { viewModel.productCategories(savedQuotes) }
    val soldProductIds = remember(savedQuotes) { viewModel.soldProductIds(savedQuotes) }
    val repriced = remember(savedQuotes, filaments, printers, settings, salesChannels) {
        savedQuotes.filterNot { it.isOrder }.associate { it.id to viewModel.repriceFor(it, filaments, printers, settings, salesChannels) }
    }
    val selectedVisibleCount = visibleQuotes.count { it.id in selectedIds }

    // A categoria escolhida sumiu (o último produto dela foi excluído ou mudou de categoria): volta pra
    // "Todas", senão a lista ficaria vazia com um filtro que nem aparece mais nos chips.
    LaunchedEffect(categories, filter.category) {
        val chosen = filter.category as? CategoryFilter.Named ?: return@LaunchedEffect
        if (categories.none { it.equals(chosen.name, ignoreCase = true) }) viewModel.setCategoryFilter(CategoryFilter.All)
    }
    LaunchedEffect(copiedId) {
        if (copiedId == null) return@LaunchedEffect
        delay(2000)
        viewModel.clearCopied()
    }

    fun actionsFor(savedQuote: SavedQuote): QuoteActions {
        val isProduct = !savedQuote.isOrder
        return QuoteActions(
            onEdit = { onEditQuote(savedQuote) },
            onEditDetails = { viewModel.startEditingDetails(savedQuote) },
            onDuplicate = { onDuplicateQuote(savedQuote) },
            onSell = if (isProduct) ({ onSellProduct(savedQuote) }) else null,
            onCopyToCatalog = if (isProduct) null else ({ onCopyToCatalog(savedQuote) }),
            onConvertToOrder = if (viewModel.canConvertToOrder(savedQuote, soldProductIds)) ({ viewModel.convertToOrder(savedQuote.id) }) else null,
            onReprice = (repriced[savedQuote.id] as? RepriceResult.Repriced)?.takeIf { it.changed }?.let { { viewModel.startRepricing(savedQuote) } },
            onStatusChange = if (isProduct) null else ({ status -> viewModel.updateStatus(savedQuote.id, status) }),
            onEditDeliveryDate = if (isProduct) null else ({ viewModel.startEditingDeliveryDate(savedQuote) }),
            onPrintSettings = { printSettingsFor = savedQuote },
            onDownloadPhoto = savedQuote.photoFileName?.let { { viewModel.downloadPhoto(savedQuote) } },
            onDownloadStl = savedQuote.stlFileName?.let { { viewModel.downloadStl(savedQuote) } },
            onExportPdf = { viewModel.exportPdf(savedQuote) },
            onCopy = { viewModel.copyQuoteToClipboard(savedQuote) },
            onOpenWhatsApp = { viewModel.openInWhatsApp(savedQuote) },
            onSaveImage = { viewModel.saveShareableImage(savedQuote) },
            onDelete = { viewModel.delete(savedQuote.id) },
        )
    }

    val header: @Composable () -> Unit = {
        HistoryHeader(
            viewModel = viewModel,
            savedQuotes = savedQuotes,
            filter = filter,
            showingProducts = showingProducts,
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            categories = if (showingProducts) categories else emptyList(),
            effectiveViewMode = effectiveViewMode,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        busy?.let { message ->
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (effectiveViewMode == HistoryViewMode.LIST) {
                // Lista preguiçosa: só os cards visíveis são montados, e cada um só carrega a própria
                // miniatura. Com algumas centenas de pedidos com foto, a lista antiga travava a cada tecla.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") { header() }
                    if (showingProducts && visibleQuotes.isNotEmpty() && selectedIds.isEmpty()) {
                        item(key = "catalog") {
                            Button(onClick = viewModel::exportCatalogPdf, enabled = busy == null) {
                                IconLabel(
                                    AppIcons.GridView,
                                    if (visibleQuotes.size == 1) "Exportar catálogo com 1 produto (PDF)" else "Exportar catálogo com os ${visibleQuotes.size} produtos (PDF)",
                                )
                            }
                        }
                    }
                    if (savedQuotes.any { it.kind == filter.kind } && visibleQuotes.isEmpty()) {
                        item(key = "empty") { EmptyHistory(showingProducts) }
                    }
                    items(visibleQuotes, key = { it.id }) { savedQuote ->
                        SavedQuoteRow(
                            savedQuote = savedQuote,
                            loadThumbnail = viewModel::photoThumbnail,
                            justCopied = copiedId == savedQuote.id,
                            selected = savedQuote.id in selectedIds,
                            onToggleSelected = { viewModel.toggleSelection(savedQuote.id) },
                            actions = actionsFor(savedQuote),
                            repriceResult = repriced[savedQuote.id],
                            todayEpochDay = today,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    header()
                    val nowMillis = remember(savedQuotes) { viewModel.nowMillis() }
                    val kanbanQuotes = remember(savedQuotes, filter, showAllDelivered) {
                        viewModel.kanbanQuotes(savedQuotes, filter, showAllDelivered, nowMillis)
                    }
                    val hiddenDelivered = remember(savedQuotes, filter, kanbanQuotes) {
                        viewModel.kanbanQuotes(savedQuotes, filter, true, nowMillis).count { it.status == OrderStatus.ENTREGUE } -
                            kanbanQuotes.count { it.status == OrderStatus.ENTREGUE }
                    }
                    KanbanBoard(
                        quotes = kanbanQuotes,
                        loadThumbnail = viewModel::photoThumbnail,
                        onStatusChange = viewModel::updateStatus,
                        actionsFor = ::actionsFor,
                        todayEpochDay = today,
                        deliveredFooter = {
                            when {
                                hiddenDelivered > 0 -> TextButton(onClick = { showAllDelivered = true }) {
                                    Text("Ver mais $hiddenDelivered entregues (mais de ${QuoteHistoryViewModel.RECENT_DELIVERED_DAYS} dias)")
                                }
                                showAllDelivered -> TextButton(onClick = { showAllDelivered = false }) {
                                    Text("Mostrar só os últimos ${QuoteHistoryViewModel.RECENT_DELIVERED_DAYS} dias")
                                }
                            }
                        },
                    )
                    val cancelled = savedQuotes.count { it.isOrder && it.status == OrderStatus.CANCELADO }
                    if (cancelled > 0) {
                        Text(
                            "$cancelled cancelado(s) ficam fora do quadro. Na lista, filtre pelo status Cancelado pra ver.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Barra de seleção fixa no rodapé: com a lista rolada, os botões de exportar continuavam
        // lá em cima, fora da vista.
        if (effectiveViewMode == HistoryViewMode.LIST && selectedVisibleCount > 0) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (selectedVisibleCount == 1) "1 selecionado" else "$selectedVisibleCount selecionados",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (showingProducts) {
                    Button(onClick = viewModel::exportCatalogPdf, enabled = busy == null) { IconLabel(AppIcons.GridView, "Exportar catálogo (PDF)") }
                } else {
                    Button(onClick = viewModel::exportSelectedPdf, enabled = busy == null) { IconLabel(AppIcons.PictureAsPdf, "Exportar selecionados (PDF)") }
                }
                TextButton(onClick = viewModel::clearSelection) { Text("Cancelar seleção") }
            }
        }
    }

    deliveryDateEditing?.let { savedQuote ->
        DeliveryDateDialog(
            quoteName = savedQuote.name,
            initial = savedQuote.deliveryDateEpochDay,
            onDismiss = viewModel::cancelEditingDeliveryDate,
            onSave = viewModel::saveDeliveryDate,
        )
    }

    detailsEditing?.let { savedQuote -> QuoteDetailsDialog(savedQuote, viewModel, clients) }

    printSettingsFor?.let { savedQuote ->
        PrintSettingsDialog(
            initial = savedQuote.printSettings ?: PrintSettings(),
            onDismiss = { printSettingsFor = null },
            onSave = { settings -> viewModel.updatePrintSettings(savedQuote.id, settings.takeUnless { it.isEmpty }) },
        )
    }

    pendingExport?.let { pending -> OverdueExportDialog(pending, today, viewModel) }

    repricing?.let { RepriceDialog(it, viewModel) }

    ShowNotice(notice, viewModel::consumeNotice)
}

@Composable
private fun HistoryHeader(
    viewModel: QuoteHistoryViewModel,
    savedQuotes: List<SavedQuote>,
    filter: HistoryFilter,
    showingProducts: Boolean,
    viewMode: HistoryViewMode,
    onViewModeChange: (HistoryViewMode) -> Unit,
    categories: List<String>,
    effectiveViewMode: HistoryViewMode,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (showingProducts) "Catálogo" else "Pedidos", style = MaterialTheme.typography.titleLarge)

        if (savedQuotes.none { it.kind == filter.kind }) {
            Text(
                if (showingProducts) {
                    "Nenhum produto no catálogo ainda. No Orçamento, escolha \"Produto do catálogo\" antes de " +
                        "salvar, ou use \"Guardar no catálogo\" no menu \"Ações\" de um pedido."
                } else {
                    "Nenhum pedido ainda. No Orçamento, escolha \"Pedido de cliente\" antes de salvar, ou " +
                        "clique em \"Vender\" num produto do Catálogo."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
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
                    onClick = { onViewModeChange(HistoryViewMode.LIST) },
                    label = { IconLabel(AppIcons.ViewList, "Lista") },
                )
                FilterChip(
                    selected = viewMode == HistoryViewMode.KANBAN,
                    onClick = { onViewModeChange(HistoryViewMode.KANBAN) },
                    label = { IconLabel(AppIcons.ViewKanban, "Kanban") },
                )
            }
        }

        Text(
            when {
                showingProducts -> "Produtos são as peças que você oferece, com preço, sem cliente nem andamento. Quando " +
                    "alguém comprar, clique em \"Vender\" pra criar o pedido."
                viewMode == HistoryViewMode.LIST -> "Marque a caixinha de um ou mais orçamentos pra exportar todos juntos num PDF só."
                else -> "Arraste um card pra outra coluna pra mudar o status, ou use \"Ações\" › \"Mover para\"."
            },
            style = MaterialTheme.typography.bodySmall,
        )

        // No Kanban, o status já é a própria organização em colunas, e o período não esconde pedido em
        // andamento; os dois filtros somem nesse modo. Produto também não tem status pra filtrar.
        HistoryFilterBar(
            filter = filter,
            viewModel = viewModel,
            showStatusFilter = effectiveViewMode == HistoryViewMode.LIST && !showingProducts,
            showPeriodFilter = effectiveViewMode == HistoryViewMode.LIST,
            categories = categories,
            hasUncategorized = savedQuotes.any { !it.isOrder && it.category == null },
        )
    }
}

@Composable
private fun EmptyHistory(showingProducts: Boolean) {
    EmptyState(if (showingProducts) "Nenhum produto encontrado com esse filtro." else "Nenhum pedido encontrado com esse filtro.")
}

/**
 * Aviso no card do produto quando o preço de hoje não é o guardado (decisão 102). Aparece só
 * quando há o que fazer: preço antigo com custos iguais continua certo e não gera aviso.
 */
@Composable
private fun RepriceNotice(result: RepriceResult, currencyText: (Double) -> String, onReprice: () -> Unit) {
    when (result) {
        is RepriceResult.Repriced -> if (result.changed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Custos mudaram: hoje o preço calculado seria ${currencyText(result.quote.tableSalePrice ?: result.quote.salePrice)}.",
                    style = MaterialTheme.typography.bodySmall,
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
            } + ": abra em \"Editar cálculo\" pra escolher de novo e conferir o preço.",
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
    val currency = repricing.product.currency
    val money: (Double) -> String = { it.toCurrencyText(currency) }
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
                BeforeAfterLine("Preço calculado", money(before.tableSalePrice ?: before.salePrice), money(after.tableSalePrice ?: after.salePrice))
                BeforeAfterLine("Custo de produção", money(before.productionCost), money(after.productionCost))
                BeforeAfterLine("Lucro", money(before.profit), money(after.profit))
                if (after.isNegotiated) {
                    Text(
                        "Preço anunciado: ${money(after.salePrice)} (mantido). Pra mudar, abra o produto em \"Editar cálculo\".",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (after.profit < 0) {
                    Text(
                        "Com os custos de hoje, o preço anunciado dá prejuízo de ${money(-after.profit)}. " +
                            "O mínimo pra não sair no negativo é ${money(after.breakEvenSalePrice)}.",
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
private fun BeforeAfterLine(label: String, before: String, after: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$before → $after", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
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
    showStatusFilter: Boolean,
    showPeriodFilter: Boolean,
    categories: List<String>,
    hasUncategorized: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filter.query,
            onValueChange = viewModel::setSearchQuery,
            // Produto não tem cliente, então a busca dele é só pelo nome.
            label = { Text(if (filter.kind == QuoteKind.PRODUCT) "Buscar por nome" else "Buscar por nome, cliente ou número") },
            singleLine = true,
        )

        if (showPeriodFilter) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PeriodPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = preset == filter.period,
                        onClick = { viewModel.setPeriodFilter(preset) },
                        label = { Text(preset.label) },
                    )
                }
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
            singleLine = true,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos os status") }, onClick = { onSelect(null); expanded = false })
            OrderStatus.entries.forEach { status ->
                DropdownMenuItem(text = { Text(status.label) }, onClick = { onSelect(status); expanded = false })
            }
        }
    }
}

@Composable
private fun StatusDropdown(status: OrderStatus, onStatusChange: (OrderStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
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
    loadThumbnail: (SavedQuote) -> ByteArray?,
    justCopied: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    actions: QuoteActions,
    repriceResult: RepriceResult?,
    todayEpochDay: Long,
) {
    val isProduct = !savedQuote.isOrder
    val currency = savedQuote.currency
    val money: (Double) -> String = { it.toCurrencyText(currency) }
    val quote = savedQuote.quote

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelected() },
                modifier = Modifier.semantics { contentDescription = "Selecionar \"${savedQuote.name}\"" },
            )

            PhotoThumbnail(savedQuote, size = 72.dp, load = loadThumbnail)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(savedQuote.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    savedQuote.displayNumber?.takeIf { savedQuote.isOrder }?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                // O valor em destaque é o que o cliente paga, o mesmo do Kanban, do PDF e da mensagem.
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericText(money(savedQuote.totalWithServices), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (quote.quantity > 1) {
                        Text("${quote.quantity} peças · ${money(quote.unitSalePrice)} cada", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    buildList {
                        add("Produção ${money(quote.productionCost)}")
                        add("Lucro ${money(quote.profit)}")
                        add("Peso ${quote.filamentWeightGrams.toWeightText()}")
                        quote.tableSalePrice?.let { tablePrice ->
                            add(
                                when {
                                    isProduct -> "Anunciado (calculado ${money(tablePrice)})"
                                    savedQuote.soldAtCatalogPrice -> "Preço do catálogo (calculado ${money(tablePrice)})"
                                    quote.negotiatedDiscount < 0 -> "Acima da tabela (${money(tablePrice)})"
                                    else -> "Negociado (tabela ${money(tablePrice)})"
                                },
                            )
                        }
                        quote.channelName?.let { add("Canal $it") }
                        if (savedQuote.shippingCost > 0) add("Frete ${money(savedQuote.shippingCost)}")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                savedQuote.category?.let { category ->
                    Text("Categoria: $category", style = MaterialTheme.typography.bodySmall)
                }
                repriceResult?.let { result -> RepriceNotice(result, money) { actions.onReprice?.invoke() } }
                savedQuote.sourceLink?.let { link ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Link interno:", style = MaterialTheme.typography.bodySmall)
                        LinkText(text = link, url = link)
                    }
                }
                savedQuote.client?.let { client ->
                    Text(
                        "Cliente: ${client.name}" + (client.contact?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                // Peça multicolor mostra o consumo de cada filamento, que é o que se confere no estoque.
                val filamentTotals = quote.filamentTotals()
                if (filamentTotals.size > 1) {
                    Text("Filamentos: ${filamentTotals.joinToString(", ") { it.displayText() }}", style = MaterialTheme.typography.bodySmall)
                } else {
                    filamentTotals.singleOrNull()?.color?.let { color ->
                        Text("Cor: ${color.displayLabel()}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Produto não tem andamento nem prazo (decisão 101).
                if (isProduct) {
                    Text("Produto do catálogo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        actions.onStatusChange?.let { StatusDropdown(status = savedQuote.status, onStatusChange = it) }
                        DeliveryBadge(savedQuote, todayEpochDay)
                    }
                }

                // FlowRow: com o "Vender" do produto, os botões não cabem numa linha em janela estreita.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    // Vender é a ação principal do produto: um clique a partir do card.
                    actions.onSell?.let { sell -> Button(onClick = sell) { IconLabel(AppIcons.Sell, "Vender") } }
                    TextButton(onClick = actions.onExportPdf) { IconLabel(AppIcons.PictureAsPdf, "Exportar PDF") }
                    TextButton(onClick = actions.onCopy) { IconLabel(AppIcons.ContentCopy, if (justCopied) "Copiado!" else "Copiar") }
                    TextButton(onClick = actions.onEdit) { IconLabel(AppIcons.Edit, "Editar") }
                    QuoteActionsMenu(savedQuote, actions)
                }
            }
        }
    }
}
