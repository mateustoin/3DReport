package com.threedreport.app.ui.filaments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.Filament

/** Tela de Filamentos: catálogo salvo, escolhido depois na tela de Orçamento. */
@Composable
fun FilamentListScreen(viewModel: FilamentListViewModel, modifier: Modifier = Modifier) {
    val filaments by viewModel.filaments.collectAsState()
    val form by viewModel.form.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filamentos", style = MaterialTheme.typography.titleLarge)

        filaments.forEach { filament ->
            FilamentRow(
                filament = filament,
                onEdit = { viewModel.startEdit(filament) },
                onDelete = { viewModel.delete(filament.id) },
            )
        }

        val currentForm = form
        if (currentForm == null) {
            Button(onClick = viewModel::startAdd) { Text("+ Novo filamento") }
        } else {
            HorizontalDivider()
            FilamentForm(
                form = currentForm,
                onChange = viewModel::updateForm,
                onSave = viewModel::save,
                onCancel = viewModel::cancelEdit,
            )
        }
    }
}

@Composable
private fun FilamentRow(filament: Filament, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(filament.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${filament.pricePerKg.toBrl()}/kg · ${filament.densityGPerCm3} g/cm³",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun FilamentForm(
    form: FilamentFormState,
    onChange: ((FilamentFormState) -> FilamentFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.id == null) "Novo filamento" else "Editar filamento", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = form.pricePerKgText,
            onValueChange = { text -> onChange { it.copy(pricePerKgText = text) } },
            label = { Text("Preço por kg (R$)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = form.densityGPerCm3Text,
            onValueChange = { text -> onChange { it.copy(densityGPerCm3Text = text) } },
            label = { Text("Densidade (g/cm³)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = form.diameterMmText,
            onValueChange = { text -> onChange { it.copy(diameterMmText = text) } },
            label = { Text("Diâmetro (mm)") },
        )

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
