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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.ui.components.ConfirmDialog

/**
 * Edição de um orçamento salvo, aberta a partir do Histórico (lista ou Kanban) sem sair da tela.
 * Reaproveita [QuoteScreen] inteiro dentro de um diálogo, com um [QuoteViewModel] só dele (decisão 108):
 * antes, editar usava o mesmo da aba Orçamento e apagava o rascunho que estivesse lá.
 *
 * Clique fora não fecha, e Esc ou "Cancelar edição" com alterações pedem confirmação: fechar um
 * formulário de cálculo inteiro por um clique distraído perdia tudo. Ctrl+S salva, como na aba.
 *
 * @param onSaved chamado depois de salvar, já com o diálogo fechado, pra quem abriu avisar que deu certo.
 */
@Composable
fun EditQuoteDialog(viewModel: QuoteViewModel, onDismiss: () -> Unit, onSaved: (name: String) -> Unit = {}) {
    var confirmingDiscard by remember { mutableStateOf(false) }
    val close = {
        viewModel.resetForm()
        onDismiss()
    }
    val attemptClose = { if (viewModel.hasUnsavedEdits) confirmingDiscard = true else close() }
    val save = {
        val name = viewModel.saveForm.value.name
        if (viewModel.saveCurrentQuote()) {
            onDismiss()
            onSaved(name)
        }
    }

    Dialog(onDismissRequest = attemptClose, properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        val input by viewModel.input.collectAsState()
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.onPreviewKeyEvent { event ->
                val accel = event.isCtrlPressed || event.isMetaPressed
                if (event.type == KeyEventType.KeyDown && accel && event.key == Key.S) {
                    save()
                    true
                } else {
                    false
                }
            },
        ) {
            Column(modifier = Modifier.width(820.dp).heightIn(max = 720.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (input.isProduct) "Editar produto" else "Editar pedido", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = attemptClose) { Text("Cancelar edição") }
                }
                HorizontalDivider()
                QuoteScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    onSave = save,
                )
            }
        }
    }

    if (confirmingDiscard) {
        ConfirmDialog(
            title = "Descartar as alterações?",
            message = "O que você mudou neste orçamento não foi salvo e vai se perder.",
            confirmLabel = "Descartar",
            onConfirm = {
                confirmingDiscard = false
                close()
            },
            onDismiss = { confirmingDiscard = false },
        )
    }
}
