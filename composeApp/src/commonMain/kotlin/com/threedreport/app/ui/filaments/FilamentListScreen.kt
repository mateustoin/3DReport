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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
                onToggleColorInStock = { colorId -> viewModel.toggleColorInStock(filament.id, colorId) },
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
                onAddColor = viewModel::addColorRow,
                onRemoveColor = viewModel::removeColorRow,
                onColorChange = viewModel::updateColorRow,
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
private fun FilamentRow(
    filament: Filament,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleColorInStock: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().alpha(if (filament.hasStockAvailable) 1f else 0.5f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(filament.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append("${filament.pricePerKg.toBrl()}/kg · ${filament.densityGPerCm3} g/cm³")
                            filament.brand?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row {
                    TextButton(onClick = onEdit) { Text("Editar") }
                    TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filament.colors.forEach { color ->
                    ColorChip(
                        label = color.displayLabel(),
                        hex = color.hex,
                        inStock = color.inStock,
                        onClick = { onToggleColorInStock(color.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorChip(label: String, hex: String?, inStock: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .alpha(if (inStock) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parseHexColor(hex)?.let { ColorSwatch(color = it, size = 14.dp) }
        Text(label + if (inStock) "" else " (Acabou)", style = MaterialTheme.typography.bodySmall)
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
    onAddColor: () -> Unit,
    onRemoveColor: (String) -> Unit,
    onColorChange: (String, (FilamentColorFormState) -> FilamentColorFormState) -> Unit,
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

        HorizontalDivider()

        Text("Cores", style = MaterialTheme.typography.titleSmall)
        Text(
            "Cadastre mais de uma cor pra esse mesmo filamento (mesma marca/preço) em vez de duplicar o " +
                "cadastro — cada cor tem seu próprio controle de estoque.",
            style = MaterialTheme.typography.bodySmall,
        )

        form.colors.forEach { colorForm ->
            ColorRowEditor(
                colorForm = colorForm,
                canRemove = form.colors.size > 1,
                onChange = { transform -> onColorChange(colorForm.id, transform) },
                onRemove = { onRemoveColor(colorForm.id) },
            )
        }
        TextButton(onClick = onAddColor) { Text("+ Adicionar cor") }

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@Composable
private fun ColorRowEditor(
    colorForm: FilamentColorFormState,
    canRemove: Boolean,
    onChange: ((FilamentColorFormState) -> FilamentColorFormState) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f).tabToNavigate(),
                    value = colorForm.name,
                    onValueChange = { text -> onChange { it.copy(name = text) } },
                    label = { Text("Nome da cor (opcional, ex.: Vermelho Fosco)") },
                )
                if (canRemove) {
                    TextButton(onClick = onRemove) { Text("Remover", color = MaterialTheme.colorScheme.error) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f).tabToNavigate(),
                    value = colorForm.hex.orEmpty(),
                    onValueChange = { text -> onChange { it.copy(hex = text.ifBlank { null }) } },
                    label = { Text("Cor personalizada (hex, opcional, ex.: #E53935)") },
                )
                val previewColor = parseHexColor(colorForm.hex)
                ColorSwatch(color = previewColor ?: MaterialTheme.colorScheme.surfaceVariant, size = 32.dp)
            }

            Text("Ou escolha uma cor pronta:", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FILAMENT_COLOR_PRESETS.forEach { (label, hex) ->
                    parseHexColor(hex)?.let { color ->
                        ColorSwatch(
                            color = color,
                            size = 28.dp,
                            selected = colorForm.hex == hex,
                            onClick = {
                                onChange { it.copy(hex = hex, name = it.name.ifBlank { label }) }
                            },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = colorForm.inStock, onCheckedChange = { checked -> onChange { it.copy(inStock = checked) } })
                Text(if (colorForm.inStock) "Em estoque" else "Acabou")
            }
        }
    }
}
