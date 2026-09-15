package com.threedreport.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.format.toBrl

/** Tela de Orçamento: dados da peça (filamento, comprimento, tempo) e resultado calculado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(viewModel: QuoteViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var filamentMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = filamentMenuExpanded,
            onExpandedChange = { filamentMenuExpanded = it },
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                value = state.selectedFilament?.name.orEmpty(),
                onValueChange = {},
                label = { Text("Filamento") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filamentMenuExpanded) },
            )
            DropdownMenu(
                expanded = filamentMenuExpanded,
                onDismissRequest = { filamentMenuExpanded = false },
            ) {
                state.filaments.forEach { filament ->
                    DropdownMenuItem(
                        text = { Text("${filament.name} · ${filament.pricePerKg.toBrl()}/kg") },
                        onClick = {
                            viewModel.selectFilament(filament)
                            filamentMenuExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.lengthMetersText,
            onValueChange = viewModel::setLengthMeters,
            label = { Text("Comprimento de filamento (m)") },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.printTimeMinutesText,
            onValueChange = viewModel::setPrintTimeMinutes,
            label = { Text("Tempo de impressão (min)") },
        )

        HorizontalDivider()

        Text("Resultado", style = MaterialTheme.typography.titleMedium)

        val quote = state.quote
        when {
            state.errorMessage != null -> Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            quote != null -> {
                Text("Produção: ${quote.productionCost.toBrl()}")
                Text("Venda: ${quote.salePrice.toBrl()}")
                Text("Lucro: ${quote.profit.toBrl()}")
            }
            else -> Text("Preencha os campos acima para calcular.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
