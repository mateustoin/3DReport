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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.SavedQuote

/** Tela de Histórico: orçamentos salvos, com o retrato dos valores no momento em que foram salvos. */
@Composable
fun QuoteHistoryScreen(viewModel: QuoteHistoryViewModel, modifier: Modifier = Modifier) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()

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
        }

        savedQuotes.sortedByDescending { it.savedAtEpochMillis }.forEach { savedQuote ->
            SavedQuoteRow(
                savedQuote = savedQuote,
                photoBytes = savedQuote.photoFileName?.let { viewModel.photoBytes(savedQuote) },
                onDownloadPhoto = { viewModel.downloadPhoto(savedQuote) },
                onDelete = { viewModel.delete(savedQuote.id) },
            )
        }
    }
}

@Composable
private fun SavedQuoteRow(
    savedQuote: SavedQuote,
    photoBytes: ByteArray?,
    onDownloadPhoto: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    "Produção: ${savedQuote.quote.productionCost.toBrl()} · Venda: ${savedQuote.quote.salePrice.toBrl()} · " +
                        "Lucro: ${savedQuote.quote.profit.toBrl()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                savedQuote.sourceLink?.let {
                    Text("Link interno (não exportado): $it", style = MaterialTheme.typography.bodySmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (photoBytes != null) {
                        TextButton(onClick = onDownloadPhoto) { Text("Baixar foto") }
                    }
                    TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
