package com.threedreport.app.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.core.model.QuoteTemplate

/**
 * Diálogo com a lista de templates salvos (fotos nomeadas da marca d'água/
 * rodapé) — aberto a partir de Configurações → "Marca d'água do PDF", que
 * continua sendo o único editor ao vivo dessa config (ver [TemplateListViewModel]).
 * Não é uma aba própria: seu único uso é dentro dessa seção de Configurações.
 */
@Composable
fun TemplateListDialog(viewModel: TemplateListViewModel, onLoad: (QuoteTemplate) -> Unit, onDismiss: () -> Unit) {
    val templates by viewModel.templates.collectAsState()
    val branding by viewModel.activeBranding.collectAsState()
    val activeId = viewModel.activeTemplateId(templates, branding)
    var pendingDelete by remember { mutableStateOf<QuoteTemplate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.width(480.dp).heightIn(max = 520.dp).padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Templates salvos", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Fotos nomeadas da marca d'água/rodapé do PDF, criadas com \"Salvar como template\".",
                    style = MaterialTheme.typography.bodySmall,
                )

                if (templates.isEmpty()) {
                    EmptyState("Nenhum template salvo ainda. Use \"Salvar como template\" acima.")
                }

                templates.forEach { template ->
                    TemplateRow(
                        template = template,
                        isActive = template.id == activeId,
                        onLoad = { onLoad(template) },
                        onDelete = { pendingDelete = template },
                    )
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }

    pendingDelete?.let { template ->
        ConfirmDialog(
            title = "Excluir template?",
            message = "\"${template.name}\" será removido. Essa ação não pode ser desfeita.",
            onConfirm = {
                viewModel.delete(template.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TemplateRow(template: QuoteTemplate, isActive: Boolean, onLoad: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    if (isActive) {
                        Text("Ativo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    template.brandName?.let { "Marca d'água: \"$it\"" } ?: "Sem marca d'água configurada",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row {
                TextButton(onClick = onLoad) { Text("Carregar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
