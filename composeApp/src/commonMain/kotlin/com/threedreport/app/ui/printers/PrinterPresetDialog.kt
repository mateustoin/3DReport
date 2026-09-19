package com.threedreport.app.ui.printers

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.focus.tabToNavigate

/**
 * Diálogo com o catálogo pré-cadastrado de impressoras (decisão 54) — aberto
 * pelo botão "Escolher da lista" em Impressoras. Escolher um preset abre o
 * formulário de nova impressora com nome/consumo já preenchidos (continuam
 * editáveis); não salva nada sozinho. Busca (marca ou modelo, contains, sem
 * diferenciar maiúsculas/minúsculas) filtra a lista — útil com o catálogo
 * cobrindo várias marcas.
 */
@Composable
fun PrinterPresetDialog(onPick: (PrinterPreset) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = PRINTER_PRESETS.filter {
        query.isBlank() || it.brand.contains(query, ignoreCase = true) || it.model.contains(query, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.width(480.dp).heightIn(max = 560.dp).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Impressoras pré-cadastradas", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Consumo é a potência máxima/nominal do manual ou ficha técnica do fabricante, não o " +
                        "consumo médio real durante a impressão (costuma ser bem menor) — ajuste depois de " +
                        "escolher. Não achou a sua? Cadastre manualmente abaixo, com o consumo do manual dela.",
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().tabToNavigate(),
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar por marca ou modelo") },
                    singleLine = true,
                )

                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filtered.isEmpty()) {
                        EmptyState("Nenhuma impressora encontrada pra \"$query\".")
                    }
                    filtered.forEach { preset ->
                        PrinterPresetRow(preset = preset, onPick = { onPick(preset) })
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }
}

@Composable
private fun PrinterPresetRow(preset: PrinterPreset, onPick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("${preset.brand} ${preset.model}", style = MaterialTheme.typography.titleMedium)
                Text("${preset.ratedPowerWatts.toInt()} W (máxima, do manual/ficha técnica)", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onPick) { Text("Usar") }
        }
    }
}
