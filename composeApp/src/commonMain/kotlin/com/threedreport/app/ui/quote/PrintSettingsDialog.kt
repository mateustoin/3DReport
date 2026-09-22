package com.threedreport.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.PrintSettings
import kotlin.math.round

/** Arredonda pra 2 casas decimais e evita ".0"/"0" à toa, mesmo espírito de `QuoteViewModel.formatImportedNumber`. */
private fun Double.toSettingText(): String {
    val rounded = round(this * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

/**
 * Configurações de fatiamento (altura de camada, preenchimento, suporte) de uma peça — usado tanto
 * no cadastro do orçamento (preenche [initial] com o que veio do G-code, se houver, e o que o
 * criador digitar aqui fica no formulário de salvar) quanto no Histórico (edição rápida direto no
 * card salvo, sem precisar reabrir a edição completa do orçamento). Ver KDoc de [PrintSettings]
 * pro porquê desses dados existirem: só consulta/replicação futura, não afetam o cálculo do preço.
 */
@Composable
fun PrintSettingsDialog(initial: PrintSettings, onDismiss: () -> Unit, onSave: (PrintSettings) -> Unit) {
    var layerHeightText by remember { mutableStateOf(initial.layerHeightMm?.toSettingText().orEmpty()) }
    var infillText by remember { mutableStateOf(initial.infillPercentage?.toSettingText().orEmpty()) }
    var infillPattern by remember { mutableStateOf(initial.infillPattern.orEmpty()) }
    var supportsEnabled by remember { mutableStateOf(initial.supportsEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações de impressão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Guardado só pra você consultar e replicar o mesmo padrão numa impressão futura da mesma " +
                        "peça — não entra no cálculo do preço nem em nenhum export.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = layerHeightText,
                    onValueChange = { layerHeightText = it },
                    label = { Text("Altura de camada (mm)") },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = infillText,
                    onValueChange = { infillText = it },
                    label = { Text("Preenchimento (%)") },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = infillPattern,
                    onValueChange = { infillPattern = it },
                    label = { Text("Padrão de preenchimento (ex.: grid, gyroid)") },
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Suporte", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = supportsEnabled == true, onClick = { supportsEnabled = true }, label = { Text("Sim") })
                        FilterChip(selected = supportsEnabled == false, onClick = { supportsEnabled = false }, label = { Text("Não") })
                        FilterChip(selected = supportsEnabled == null, onClick = { supportsEnabled = null }, label = { Text("Não informado") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    PrintSettings(
                        layerHeightMm = parseDecimal(layerHeightText),
                        infillPercentage = parseDecimal(infillText),
                        infillPattern = infillPattern.trim().ifEmpty { null },
                        supportsEnabled = supportsEnabled,
                    ),
                )
                onDismiss()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
