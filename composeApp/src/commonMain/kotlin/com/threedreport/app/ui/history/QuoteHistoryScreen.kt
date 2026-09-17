package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.toBrl
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote

/** Tela de Histórico: orçamentos salvos, com o retrato dos valores no momento em que foram salvos. */
@Composable
fun QuoteHistoryScreen(viewModel: QuoteHistoryViewModel, modifier: Modifier = Modifier) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val copiedId by viewModel.copiedId.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var pendingDelete by remember { mutableStateOf<SavedQuote?>(null) }

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
            Text(
                "Marque a caixinha de um ou mais orçamentos pra exportar todos juntos num PDF só.",
                style = MaterialTheme.typography.bodySmall,
            )

            HistoryFilterBar(filter = filter, viewModel = viewModel)
        }

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
                onExportPdf = { viewModel.exportPdf(savedQuote) },
                onCopy = { viewModel.copyQuoteToClipboard(savedQuote) },
                onDelete = { pendingDelete = savedQuote },
                onStatusChange = { status -> viewModel.updateStatus(savedQuote.id, status) },
            )
        }
    }

    pendingDelete?.let { savedQuote ->
        ConfirmDialog(
            title = "Excluir orçamento?",
            message = "\"${savedQuote.name}\" será removido do histórico, junto com a foto salva (se houver). Essa ação não pode ser desfeita.",
            onConfirm = {
                viewModel.delete(savedQuote.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun HistoryFilterBar(filter: HistoryFilter, viewModel: QuoteHistoryViewModel) {
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

        StatusFilterDropdown(selected = filter.status, onSelect = viewModel::setStatusFilter)
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
        TextButton(onClick = { expanded = true }) { Text("Status: ${status.label}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OrderStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
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
    onExportPdf: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
) {
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
                Text(formatDateTime(savedQuote.savedAtEpochMillis), style = MaterialTheme.typography.bodySmall)
                Text(
                    "Peso: ${savedQuote.quote.filamentWeightGrams.toWeightText()} · " +
                        "Produção: ${savedQuote.quote.productionCost.toBrl()} · Venda: ${savedQuote.quote.salePrice.toBrl()} · " +
                        "Lucro: ${savedQuote.quote.profit.toBrl()}",
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
                    if (photoBytes != null) {
                        TextButton(onClick = onDownloadPhoto) { Text("Baixar foto") }
                    }
                    TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
