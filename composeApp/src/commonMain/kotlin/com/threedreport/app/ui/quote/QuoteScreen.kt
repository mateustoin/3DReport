package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.toBrl

/** Tela de Orçamento: dados da peça (filamento, impressora, comprimento, tempo) e resultado calculado. */
@Composable
fun QuoteScreen(viewModel: QuoteViewModel, modifier: Modifier = Modifier) {
    val filaments by viewModel.filaments.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val services by viewModel.services.collectAsState()
    val input by viewModel.input.collectAsState()
    val saveForm by viewModel.saveForm.collectAsState()

    val result = viewModel.calculate(filaments, printers, settings, services, input)

    Column(
        modifier = modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LabeledDropdown(
            label = "Filamento",
            items = filaments,
            selected = result.filament,
            itemLabel = { "${it.name} · ${it.pricePerKg.toBrl()}/kg" },
            displayText = { it.name },
            onSelect = { viewModel.selectFilament(it.id) },
        )

        LabeledDropdown(
            label = "Impressora",
            items = printers,
            selected = result.printer,
            itemLabel = { it.name },
            displayText = { it.name },
            onSelect = { viewModel.selectPrinter(it.id) },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.lengthMetersText,
            onValueChange = viewModel::setLengthMeters,
            label = { Text("Comprimento de filamento (m)") },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.printTimeMinutesText,
            onValueChange = viewModel::setPrintTimeMinutes,
            label = { Text("Tempo de impressão (min)") },
        )

        if (services.isNotEmpty()) {
            Text("Serviços opcionais", style = MaterialTheme.typography.titleMedium)
            services.forEach { service ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = service.id in input.selectedServiceIds,
                        onCheckedChange = { viewModel.toggleService(service.id) },
                    )
                    Text("${service.name} · ${service.price.toBrl()}")
                }
            }
        }

        HorizontalDivider()

        Text("Resultado", style = MaterialTheme.typography.titleMedium)

        val quote = result.quote
        when {
            result.errorMessage != null -> Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
            quote != null -> {
                Text("Produção: ${quote.productionCost.toBrl()}")
                Text("Venda: ${quote.salePrice.toBrl()}")
                Text("Lucro: ${quote.profit.toBrl()}")
                if (result.selectedServices.isNotEmpty()) {
                    result.selectedServices.forEach { service ->
                        Text("${service.name}: ${service.price.toBrl()}")
                    }
                    Text(
                        "Total (venda + serviços): ${result.grandTotal!!.toBrl()}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            filaments.isEmpty() -> Text("Cadastre um filamento na aba Filamentos.", style = MaterialTheme.typography.bodyMedium)
            printers.isEmpty() -> Text("Cadastre uma impressora na aba Impressoras.", style = MaterialTheme.typography.bodyMedium)
            else -> Text("Preencha os campos acima para calcular.", style = MaterialTheme.typography.bodyMedium)
        }

        if (quote != null) {
            HorizontalDivider()
            SaveQuoteForm(saveForm, viewModel, onSave = { viewModel.saveQuote(quote, result.selectedServices) })
        }
    }
}

@Composable
private fun SaveQuoteForm(form: SaveQuoteFormState, viewModel: QuoteViewModel, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Salvar orçamento", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = viewModel::setSaveName,
            label = { Text("Nome (opcional)") },
        )

        val photo = form.photo
        if (photo != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    bitmap = decodeImageBitmap(photo.bytes),
                    contentDescription = photo.fileName,
                    modifier = Modifier.size(64.dp),
                )
                Text(photo.fileName, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = viewModel::clearPhoto) { Text("Remover foto") }
            }
        } else {
            OutlinedButton(onClick = viewModel::pickPhoto) { Text("Escolher foto (opcional)") }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.sourceLink,
            onValueChange = viewModel::setSourceLink,
            label = { Text("Link do modelo (opcional, uso interno)") },
        )

        Button(onClick = onSave) { Text("Salvar orçamento") }

        if (form.savedConfirmation) {
            Text("Orçamento salvo no histórico.", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    items: List<T>,
    selected: T?,
    itemLabel: (T) -> String,
    displayText: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selected?.let(displayText).orEmpty(),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
