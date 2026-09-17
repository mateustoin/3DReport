package com.threedreport.app.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.core.model.QuoteTemplate

/** Tela de Templates: presets nomeados de marca d'água/rodapé do PDF, pra trocar rápido a config ativa em Configurações. */
@Composable
fun TemplateListScreen(viewModel: TemplateListViewModel, modifier: Modifier = Modifier) {
    val templates by viewModel.templates.collectAsState()
    val form by viewModel.form.collectAsState()
    var pendingDelete by remember { mutableStateOf<QuoteTemplate?>(null) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Templates", style = MaterialTheme.typography.titleLarge)
        Text(
            "Presets nomeados de marca d'água/rodapé do PDF. \"Usar este\" aplica o preset na " +
                "configuração atual (Configurações → Marca d'água do PDF), que é a que vale pra " +
                "todo export — não precisa reescrever o texto toda vez que quiser trocar o visual.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (templates.isEmpty()) {
            EmptyState("Nenhum template cadastrado ainda. Cadastre o primeiro abaixo.")
        }

        templates.forEach { template ->
            TemplateRow(
                template = template,
                onApply = { viewModel.applyToActive(template.id) },
                onEdit = { viewModel.startEdit(template) },
                onDelete = { pendingDelete = template },
            )
        }

        val currentForm = form
        if (currentForm == null) {
            Button(onClick = viewModel::startAdd) { Text("+ Novo template") }
        } else {
            HorizontalDivider()
            TemplateForm(
                form = currentForm,
                onChange = viewModel::updateForm,
                onSave = viewModel::save,
                onCancel = viewModel::cancelEdit,
            )
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
private fun TemplateRow(template: QuoteTemplate, onApply: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    template.watermarkText?.let { "Marca d'água: \"$it\"" } ?: "Sem marca d'água configurada",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row {
                TextButton(onClick = onApply) { Text("Usar este") }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TemplateForm(
    form: TemplateFormState,
    onChange: ((TemplateFormState) -> TemplateFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.id == null) "Novo template" else "Editar template", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome (ex.: Formal, Simples)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.watermarkText,
            onValueChange = { text -> onChange { it.copy(watermarkText = text) } },
            label = { Text("Texto da marca d'água (opcional)") },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.showWatermark, onCheckedChange = { checked -> onChange { it.copy(showWatermark = checked) } })
            Text("Marca d'água diagonal no PDF")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.showFooter, onCheckedChange = { checked -> onChange { it.copy(showFooter = checked) } })
            Text("Rodapé com o nome no PDF")
        }

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
