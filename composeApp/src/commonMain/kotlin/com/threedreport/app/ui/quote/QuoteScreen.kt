package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedCard
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
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.core.stl.parseStl

/** Tela de Orçamento: dados da peça (filamento, impressora, comprimento, tempo) e resultado calculado. */
@Composable
fun QuoteScreen(viewModel: QuoteViewModel, modifier: Modifier = Modifier) {
    val allFilaments by viewModel.filaments.collectAsState()
    val filaments = allFilaments.filter { it.hasStockAvailable }
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val services by viewModel.services.collectAsState()
    val input by viewModel.input.collectAsState()
    val saveForm by viewModel.saveForm.collectAsState()

    val result = viewModel.calculate(filaments, printers, settings, services, input)
    val currency = LocalCurrency.current

    Column(
        modifier = modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LabeledDropdown(
            label = "Filamento",
            items = filaments,
            selected = result.filament,
            itemLabel = { "${it.name} · ${it.pricePerKg.toCurrencyText(currency)}/kg" },
            displayText = { it.name },
            onSelect = { viewModel.selectFilament(it.id) },
        )

        val availableColors = result.filament?.colors?.filter { it.inStock }.orEmpty()
        if (availableColors.size > 1) {
            LabeledDropdown(
                label = "Cor",
                items = availableColors,
                selected = result.filamentColor,
                itemLabel = { it.displayLabel() },
                displayText = { it.displayLabel() },
                onSelect = { viewModel.selectFilamentColor(it.id) },
            )
        }

        LabeledDropdown(
            label = "Impressora",
            items = printers,
            selected = result.printer,
            itemLabel = { it.name },
            displayText = { it.name },
            onSelect = { viewModel.selectPrinter(it.id) },
        )

        OutlinedButton(onClick = viewModel::pickAndImportGCode) { Text("Preencher a partir do G-code") }
        input.gcodeImportMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = viewModel::undoGCodeImport) { Text("Desfazer importação do G-code") }
        }

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
                    Text("${service.name} · ${service.price.toMoney()}")
                }
            }
        }

        if (settings.marketplaceFeeRate > 0.0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = input.appliesMarketplaceFee,
                    onCheckedChange = viewModel::setAppliesMarketplaceFee,
                )
                Text("Vender por marketplace (taxa de ${settings.marketplaceFeeRate.toPercentText()})")
            }
        }

        HorizontalDivider()

        Text("Resultado", style = MaterialTheme.typography.titleMedium)

        val quote = result.quote
        when {
            result.errorMessage != null -> Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
            quote != null -> {
                Text("Produção: ${quote.productionCost.toMoney()}")
                Text("Venda: ${quote.salePrice.toMoney()}")
                if (quote.marketplaceFeeRate > 0.0) {
                    Text(
                        "Já inclui a taxa de marketplace (${quote.marketplaceFeeRate.toPercentText()}) — " +
                            "o cliente paga esse valor normalmente.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("Lucro: ${quote.profit.toMoney()}")
                if (result.selectedServices.isNotEmpty()) {
                    result.selectedServices.forEach { service ->
                        Text("${service.name}: ${service.price.toMoney()}")
                    }
                    Text(
                        "Total (venda + serviços): ${result.grandTotal!!.toMoney()}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            filaments.isEmpty() && allFilaments.isNotEmpty() -> Text(
                "Todos os filamentos cadastrados estão marcados como esgotados. Marque algum como \"Em estoque\" na aba Filamentos.",
                style = MaterialTheme.typography.bodyMedium,
            )
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

        val stlFile = form.stlFile
        if (stlFile != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stlFile.fileName, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = viewModel::clearStlFile) { Text("Remover STL") }
            }
            val mesh = remember(stlFile) { runCatching { parseStl(stlFile.bytes) }.getOrNull() }
            if (mesh != null) {
                Text(
                    "Arraste pra girar, use a roda do mouse pra zoom.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Stl3DViewer(mesh = mesh, modifier = Modifier.fillMaxWidth().height(280.dp))
                }
            } else {
                Text(
                    "Não consegui ler esse arquivo STL — pode estar corrompido ou num formato não suportado.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            OutlinedButton(onClick = viewModel::pickStl) { Text("Anexar arquivo STL (opcional)") }
        }
        Text(
            "Guardado pra você recuperar depois no Histórico e reaproveitar numa venda futura da " +
                "mesma peça — não entra no PDF nem no copiar/colar.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.sourceLink,
            onValueChange = viewModel::setSourceLink,
            label = { Text("Link do modelo (opcional, uso interno)") },
        )
        if (form.sourceLink.isNotBlank()) {
            LinkText(text = "Abrir link no navegador", url = form.sourceLink)
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.clientName,
            onValueChange = viewModel::setClientName,
            label = { Text("Cliente (opcional, uso interno)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.clientContact,
            onValueChange = viewModel::setClientContact,
            label = { Text("Contato do cliente (opcional)") },
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
