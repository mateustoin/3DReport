package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.app.ui.quote.PrintSettingsDialog
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.SavedQuote

private enum class HistoryViewMode { LIST, KANBAN }

/** Tela de Histórico: orçamentos salvos, com o retrato dos valores no momento em que foram salvos. */
@Composable
fun QuoteHistoryScreen(
    viewModel: QuoteHistoryViewModel,
    onEditQuote: (SavedQuote) -> Unit,
    onDuplicateQuote: (SavedQuote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val copiedId by viewModel.copiedId.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var pendingDelete by remember { mutableStateOf<SavedQuote?>(null) }
    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Histórico de orçamentos", style = MaterialTheme.typography.titleLarge)

        if (savedQuotes.isEmpty()) {
            Text(
                "Nenhum orçamento salvo ainda. Calcule um na aba Orçamento e clique em \"Salvar orçamento\".",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = viewMode == HistoryViewMode.LIST, onClick = { viewMode = HistoryViewMode.LIST }, label = { Text("Lista") })
                FilterChip(
                    selected = viewMode == HistoryViewMode.KANBAN,
                    onClick = { viewMode = HistoryViewMode.KANBAN },
                    label = { Text("Kanban") },
                )
            }

            if (viewMode == HistoryViewMode.LIST) {
                Text(
                    "Marque a caixinha de um ou mais orçamentos pra exportar todos juntos num PDF só.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Arraste um card pra outra coluna pra mudar o status (ou use o menu \"⋮\" do card).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // No Kanban, o status já é a própria organização em colunas — filtrar por status ali
            // deixaria as outras colunas vazias sem explicação, então esse filtro some nesse modo.
            HistoryFilterBar(filter = filter, viewModel = viewModel, showStatusFilter = viewMode == HistoryViewMode.LIST)
        }

        if (viewMode == HistoryViewMode.LIST) {
            if (selectedIds.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${selectedIds.size} selecionado(s)", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = viewModel::exportSelectedPdf) { Text("Exportar selecionados (PDF)") }
                    Button(onClick = viewModel::exportCatalogPdf) { Text("Exportar catálogo (PDF)") }
                    TextButton(onClick = viewModel::clearSelection) { Text("Cancelar seleção") }
                }
            }

            val visibleQuotes = viewModel.visibleQuotes(savedQuotes, filter)
            if (savedQuotes.isNotEmpty() && visibleQuotes.isEmpty()) {
                EmptyState("Nenhum orçamento encontrado com esse filtro.")
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
                    onDelete = { pendingDelete = savedQuote },
                    onStatusChange = { status -> viewModel.updateStatus(savedQuote.id, status) },
                    onUpdatePrintSettings = { settings -> viewModel.updatePrintSettings(savedQuote.id, settings) },
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
                onDelete = { pendingDelete = it },
                onUpdatePrintSettings = { savedQuote, settings -> viewModel.updatePrintSettings(savedQuote.id, settings) },
            )
        }
    }

    pendingDelete?.let { savedQuote ->
        ConfirmDialog(
            title = "Excluir orçamento?",
            message = "\"${savedQuote.name}\" será removido do histórico, junto com a foto e o STL salvos (se houver). Essa ação não pode ser desfeita.",
            onConfirm = {
                viewModel.delete(savedQuote.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun HistoryFilterBar(filter: HistoryFilter, viewModel: QuoteHistoryViewModel, showStatusFilter: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filter.query,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("Buscar por nome ou cliente") },
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
    onDelete: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    onUpdatePrintSettings: (PrintSettings?) -> Unit,
) {
    var showPrintSettingsDialog by remember { mutableStateOf(false) }

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
                        append(" · Lucro: ")
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(savedQuote.quote.profit.toMoney()) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
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

                StatusDropdown(status = savedQuote.status, onStatusChange = onStatusChange)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onExportPdf) { Text("Exportar PDF") }
                    TextButton(onClick = onCopy) { Text(if (justCopied) "Copiado!" else "Copiar") }
                    TextButton(onClick = onEdit) { Text("Editar") }
                    TextButton(onClick = onDuplicate) { Text("Duplicar") }
                    TextButton(onClick = { showPrintSettingsDialog = true }) {
                        Text(if (savedQuote.printSettings == null) "Adicionar configurações de impressão" else "Configurações de impressão")
                    }
                    if (photoBytes != null) {
                        TextButton(onClick = onDownloadPhoto) { Text("Baixar foto") }
                    }
                    if (savedQuote.stlFileName != null) {
                        TextButton(onClick = onDownloadStl) { Text("Baixar STL") }
                    }
                    TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
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
