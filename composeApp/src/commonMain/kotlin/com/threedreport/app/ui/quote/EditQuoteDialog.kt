package com.threedreport.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Edição de um orçamento salvo, aberta a partir do Histórico (lista ou Kanban) sem sair da tela.
 * Reaproveita [QuoteScreen] inteiro (mesmo formulário de cálculo + salvar já usado na aba
 * Orçamento) dentro de um diálogo, em vez de trocar de aba (ver decisão sobre a mudança) — assim
 * cancelar a edição não exige salvar antes nem perder de vista o Histórico por trás. O botão
 * "Cancelar edição" fica sempre visível no topo, sem precisar rolar até o fim do formulário pra
 * achar o já existente dentro de [SaveQuoteForm].
 */
@Composable
fun EditQuoteDialog(viewModel: QuoteViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { viewModel.resetForm(); onDismiss() }) {
        val input by viewModel.input.collectAsState()
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.width(760.dp).heightIn(max = 680.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (input.isProduct) "Editar produto" else "Editar orçamento salvo", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { viewModel.resetForm(); onDismiss() }) { Text("Cancelar edição") }
                }
                HorizontalDivider()
                QuoteScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    onEditingFinished = onDismiss,
                )
            }
        }
    }
}
