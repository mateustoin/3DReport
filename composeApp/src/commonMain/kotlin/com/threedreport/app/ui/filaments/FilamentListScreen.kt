package com.threedreport.app.ui.filaments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.Filament

/** Tela de Filamentos: catálogo salvo, escolhido depois na tela de Orçamento. */
@Composable
fun FilamentListScreen(viewModel: FilamentListViewModel, modifier: Modifier = Modifier) {
    val filaments by viewModel.filaments.collectAsState()
    val form by viewModel.form.collectAsState()
    var pendingDelete by remember { mutableStateOf<Filament?>(null) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filamentos", style = MaterialTheme.typography.titleLarge)

        if (filaments.isEmpty()) {
            EmptyState("Nenhum filamento cadastrado ainda. Cadastre o primeiro abaixo.")
        }

        filaments.forEach { filament ->
            FilamentRow(
                filament = filament,
                onEdit = { viewModel.startEdit(filament) },
                onDelete = { pendingDelete = filament },
                onToggleInStock = { viewModel.toggleInStock(filament.id) },
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

    pendingDelete?.let { filament ->
        ConfirmDialog(
            title = "Excluir filamento?",
            message = "\"${filament.name}\" será removido do catálogo. Essa ação não pode ser desfeita.",
            onConfirm = {
                viewModel.delete(filament.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun FilamentRow(filament: Filament, onEdit: () -> Unit, onDelete: () -> Unit, onToggleInStock: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().alpha(if (filament.inStock) 1f else 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                parseHexColor(filament.colorHex)?.let { ColorSwatch(color = it, size = 20.dp) }
                Column {
                    Text(filament.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append("${filament.pricePerKg.toBrl()}/kg · ${filament.densityGPerCm3} g/cm³")
                            filament.brand?.let { append(" · $it") }
                            filament.colorName?.let { append(" · $it") }
                            if (!filament.inStock) append(" · Acabou")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Row {
                TextButton(onClick = onToggleInStock) { Text(if (filament.inStock) "Marcar esgotado" else "Marcar em estoque") }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, size: Dp, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
            .border(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    )
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
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.pricePerKgText,
            onValueChange = { text -> onChange { it.copy(pricePerKgText = text) } },
            label = { Text("Preço por kg (R$)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.densityGPerCm3Text,
            onValueChange = { text -> onChange { it.copy(densityGPerCm3Text = text) } },
            label = { Text("Densidade (g/cm³)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.diameterMmText,
            onValueChange = { text -> onChange { it.copy(diameterMmText = text) } },
            label = { Text("Diâmetro (mm)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.brand,
            onValueChange = { text -> onChange { it.copy(brand = text) } },
            label = { Text("Marca (opcional)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.colorName,
            onValueChange = { text -> onChange { it.copy(colorName = text) } },
            label = { Text("Nome da cor (opcional, ex.: Vermelho Fosco)") },
        )

        Text("Cor visual (opcional)", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FILAMENT_COLOR_PRESETS.forEach { (label, hex) ->
                parseHexColor(hex)?.let { color ->
                    ColorSwatch(
                        color = color,
                        size = 28.dp,
                        selected = form.colorHex == hex,
                        onClick = {
                            onChange {
                                it.copy(colorHex = hex, colorName = it.colorName.ifBlank { label })
                            }
                        },
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = form.inStock, onCheckedChange = { checked -> onChange { it.copy(inStock = checked) } })
            Text(if (form.inStock) "Em estoque" else "Acabou")
        }

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
