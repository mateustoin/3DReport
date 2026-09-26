package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.threedreport.core.model.Archivable
import com.threedreport.core.model.SavedQuote

/**
 * O que um seletor do Orçamento oferece (decisão 115): arquivado sai das escolhas, mas o que já está
 * escolhido continua aparecendo.
 */
fun <T : Archivable<T>> List<T>.availableOrSelected(isSelected: (T) -> Boolean): List<T> =
    filter { !it.archived || isSelected(it) }

/**
 * Quantos pedidos e produtos usam cada cadastro (decisão 115), contando pelos retratos salvos. Serve pra
 * confirmação de excluir sugerir arquivar quando o item está em uso. Recebe também os da lixeira, que
 * podem voltar e continuam usando o item.
 */
object CatalogUsage {
    fun filament(id: String, quotes: List<SavedQuote>) =
        quotes.count { saved -> saved.quote.prints.any { print -> print.job.filaments.any { it.filament.id == id } } }

    fun printer(id: String, quotes: List<SavedQuote>) = quotes.count { saved -> saved.quote.prints.any { it.printerId == id } }

    fun service(id: String, quotes: List<SavedQuote>) = quotes.count { saved -> saved.services.any { it.id == id } }

    fun channel(id: String, quotes: List<SavedQuote>) = quotes.count { it.quote.channelId == id }
}

/** "1 pedido ou produto", "3 pedidos ou produtos". */
fun usageText(count: Int): String = if (count == 1) "1 pedido ou produto" else "$count pedidos ou produtos"

/**
 * Confirmação de excluir um cadastro (decisão 115). Sem uso, é a confirmação de sempre. Em uso, diz em
 * quantos e põe "Arquivar" como o botão principal: arquivado some das escolhas do Orçamento e continua
 * valendo pra quem já usou, e excluir continua possível pra quem quer mesmo.
 *
 * @param what o que está sendo excluído, pra frase ("o filamento \"PLA\"").
 * @param deleteMessage o que acontece ao excluir, como a tela já explicava.
 * @param onArchive `null` pra um item que já está arquivado.
 */
@Composable
fun DeleteOrArchiveDialog(
    title: String,
    what: String,
    usageCount: Int,
    deleteMessage: String,
    onArchive: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (usageCount == 0 || onArchive == null) {
        ConfirmDialog(title = title, message = deleteMessage, onConfirm = onDelete, onDismiss = onDismiss)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${what.replaceFirstChar { it.uppercase() }} está em ${usageText(usageCount)}.")
                Text(
                    "Arquivar tira da lista de escolhas do Orçamento e mantém o cadastro: pedidos reabertos e " +
                        "produtos do catálogo continuam funcionando, e dá pra restaurar depois.",
                )
                Text(deleteMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onArchive) { Text("Arquivar") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Excluir mesmo assim", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

/** "Arquivados (n)" no fim de uma lista de cadastro, recolhido: o dia a dia é com os ativos. */
@Composable
fun ArchivedSection(count: Int, content: @Composable () -> Unit) {
    if (count == 0) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Arquivados ($count) ▴" else "Arquivados ($count) ▾")
    }
    if (expanded) content()
}
