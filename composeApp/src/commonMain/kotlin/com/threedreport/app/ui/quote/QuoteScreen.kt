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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.platform.encodeImageBitmapToPng
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.app.ui.viewer.rememberStl3DViewerState
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.Service
import com.threedreport.core.stl.StlAnalyzer
import com.threedreport.core.stl.parseStl
import com.threedreport.core.stl.peekStlTriangleCount
import kotlin.math.abs
import kotlin.math.round

/**
 * Acima disso, o visualizador 3D não é exibido (só o STL é salvo, pra recuperar depois) — é um
 * limite heurístico, não medido com benchmark real: o rasterizador em `Canvas` (decisão 61)
 * recalcula a projeção de cada triângulo a cada frame durante o arrasto, então uma malha muito
 * densa travava a interface por completo em vez de só ficar mais lenta. Ajustar se um caso real
 * mostrar que o limite está conservador (ou generoso) demais.
 */
private const val MAX_RENDERABLE_STL_TRIANGLES = 500_000L

/** Opção padrão do seletor de canal: venda sem intermediário e sem taxa (Pix, dinheiro, entrega em mãos). */
private const val DIRECT_SALE_LABEL = "Venda direta (sem taxa)"

/** Arredonda pra 1 casa decimal, separador decimal brasileiro (vírgula) — mesmo estilo de `toWeightText()`. */
private fun Double.formatOneDecimal(): String {
    val tenths = round(this * 10).toLong()
    val whole = tenths / 10
    val decimal = abs(tenths % 10)
    return if (decimal == 0L) "$whole" else "$whole,$decimal"
}

/**
 * Tela de Orçamento: dados da peça (filamento, impressora, comprimento, tempo) e resultado
 * calculado. [onEditingFinished] é chamado quando uma edição de orçamento salvo (iniciada fora
 * daqui, ver [EditQuoteDialog]) termina — seja por cancelamento, seja por salvar com sucesso; não
 * tem efeito num orçamento novo (não editando nada).
 */
@Composable
fun QuoteScreen(viewModel: QuoteViewModel, modifier: Modifier = Modifier, onEditingFinished: () -> Unit = {}) {
    val allFilaments by viewModel.filaments.collectAsState()
    val filaments = allFilaments.filter { it.hasStockAvailable }
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val services by viewModel.services.collectAsState()
    val salesChannels by viewModel.salesChannels.collectAsState()
    val input by viewModel.input.collectAsState()
    val saveForm by viewModel.saveForm.collectAsState()

    val result = viewModel.calculate(filaments, printers, settings, services, input, salesChannels)
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

        // Só aparece pra quem configurou quanto vale a própria hora: sem isso, o campo não teria
        // efeito nenhum no preço e seria só mais uma caixa pra ignorar.
        if (settings.chargesLaborByTime) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().tabToNavigate(),
                value = input.laborMinutesText,
                onValueChange = viewModel::setLaborMinutes,
                label = { Text("Seu tempo de trabalho por peça (min)") },
            )
            Text(
                "Quanto cada peça dá de trabalho seu, fora o tempo de máquina: tirar da mesa, " +
                    "remover suporte, lixar, pintar, embalar. Cobrado a " +
                    "${settings.laborRatePerHour.toCurrencyText(currency)}/h (ajustável em Configurações).",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.quantityText,
            onValueChange = viewModel::setQuantity,
            label = { Text("Quantidade de peças") },
        )
        Text(
            "Comprimento e tempo acima são de UMA peça: o app multiplica pela quantidade. Se você " +
                "fatiou a mesa inteira de uma vez e os números já são do lote todo, deixe a " +
                "quantidade em 1. Vazio conta como 1.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (settings.chargesLaborByTime) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().tabToNavigate(),
                value = input.setupMinutesText,
                onValueChange = viewModel::setSetupMinutes,
                label = { Text("Preparo do pedido (min)") },
            )
            Text(
                "O que você faz uma vez só, não importa quantas peças: preparar o arquivo, fatiar, " +
                    "montar a mesa. É isso que faz a peça sair mais barata no lote, sem precisar " +
                    "inventar desconto.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

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

        if (salesChannels.isNotEmpty()) {
            LabeledDropdown(
                label = "Canal de venda",
                items = listOf(null) + salesChannels,
                selected = result.salesChannel,
                itemLabel = { it?.let { channel -> "${channel.name} · ${channel.feeRate.toPercentText()}" } ?: DIRECT_SALE_LABEL },
                displayText = { it?.name ?: DIRECT_SALE_LABEL },
                onSelect = { viewModel.selectSalesChannel(it?.id) },
                emptyText = DIRECT_SALE_LABEL,
            )
            Text(
                "A taxa do canal é descontada do que você recebe, então o preço de venda sobe o " +
                    "suficiente pra sua margem não mudar. Cadastre os canais em Configurações.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.shippingCostText,
            onValueChange = viewModel::setShippingCost,
            label = { Text("Frete (${currency.symbol}, opcional)") },
        )
        Text(
            "Somado ao total como linha própria, nunca embutido no preço da peça: frete é repasse, " +
                "não produto seu. Não multiplica pela quantidade nem entra na margem.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Resultado", style = MaterialTheme.typography.titleMedium)

        val quote = result.quote
        when {
            result.errorMessage != null -> Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
            quote != null -> QuoteReceipt(quote = quote, selectedServices = result.selectedServices, grandTotal = result.grandTotal ?: quote.salePrice)
            filaments.isEmpty() && allFilaments.isNotEmpty() -> Text(
                "Todos os filamentos cadastrados estão marcados como esgotados. Marque algum como \"Em estoque\" na aba Filamentos.",
                style = MaterialTheme.typography.bodyMedium,
            )
            filaments.isEmpty() -> Text("Cadastre um filamento na aba Filamentos.", style = MaterialTheme.typography.bodyMedium)
            printers.isEmpty() -> Text("Cadastre uma impressora na aba Impressoras.", style = MaterialTheme.typography.bodyMedium)
            else -> Text("Preencha os campos acima para calcular.", style = MaterialTheme.typography.bodyMedium)
        }

        if (quote != null) {
            NegotiationSection(
                quote = quote,
                extras = result.servicesTotal + result.shippingCost,
                targetTotalText = input.targetTotalText,
                onTargetTotalChange = viewModel::setTargetTotal,
            )

            val comparison = viewModel.comparePrinters(filaments, printers, settings, services, input, salesChannels)
            if (comparison.size > 1) {
                PrinterComparison(comparison = comparison, extras = result.servicesTotal + result.shippingCost)
            }
        }

        HorizontalDivider()
        SaveQuoteForm(
            form = saveForm,
            viewModel = viewModel,
            canSave = quote != null,
            onSave = {
                quote?.let {
                    val wasEditing = saveForm.editingQuoteId != null
                    viewModel.saveQuote(it, result.selectedServices)
                    if (wasEditing) onEditingFinished()
                }
            },
            onEditingFinished = onEditingFinished,
        )
    }
}

/**
 * Resultado do cálculo como uma "nota": o valor que de fato é cobrado do cliente
 * ([grandTotal] — venda + serviços) em destaque no topo, com a composição do preço (custo de
 * produção, lucro, cada serviço) como itens de nota abaixo. Antes disso era uma pilha de `Text`
 * do mesmo peso, sem indicar qual número é o que realmente importa pra fechar a venda.
 */
@Composable
private fun QuoteReceipt(quote: Quote, selectedServices: List<Service>, grandTotal: Double) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Você cobra",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumericText(
                text = grandTotal.toMoney(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (quote.quantity > 1) {
                Text(
                    "${quote.quantity} peças · ${(grandTotal / quote.quantity).toMoney()} cada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (quote.totalDeductionRate > 0.0) {
                val parts = buildList {
                    quote.channelName?.let { add("$it ${quote.marketplaceFeeRate.toPercentText()}") }
                    if (quote.taxRate > 0.0) add("imposto ${quote.taxRate.toPercentText()}")
                }
                Text(
                    "Preço já elevado pra absorver ${parts.joinToString(" e ")}: o cliente paga esse " +
                        "valor normalmente, e o lucro abaixo já é o que sobra pra você.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ReceiptLine("Custo de produção", quote.productionCost.toMoney())
            ReceiptLine("Lucro", quote.profit.toMoney())
            selectedServices.forEach { service ->
                val label = if (quote.quantity > 1) "${service.name} (× ${quote.quantity})" else service.name
                ReceiptLine(label, (service.price * quote.quantity).toMoney())
            }
        }
    }
}

/**
 * Ferramenta de negociação: o vendedor digita o valor que o cliente propôs e vê na hora o que
 * sobra. O piso (venda + extras que não passam pela margem) fica sempre à vista, porque é o
 * número que ele precisa ter na cabeça no meio da conversa.
 *
 * [extras] são serviços e frete: entram no total cobrado, mas não no preço da peça.
 */
@Composable
private fun NegotiationSection(
    quote: Quote,
    extras: Double,
    targetTotalText: String,
    onTargetTotalChange: (String) -> Unit,
) {
    Text("Negociação", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = targetTotalText,
        onValueChange = onTargetTotalChange,
        label = { Text("Preço fechado com o cliente (opcional)") },
    )
    Text(
        "Digite aqui o valor que o cliente propôs e veja o que sobra. Ele passa a ser o preço de " +
            "verdade do orçamento: é o que vai pro PDF, pro histórico e pro Dashboard. Deixe vazio " +
            "pra usar o preço da sua margem.",
        style = MaterialTheme.typography.bodySmall,
    )

    val breakEvenTotal = quote.breakEvenSalePrice + extras
    if (quote.profit < 0) {
        Text(
            "Prejuízo: nesse valor você paga ${(-quote.profit).toMoney()} pra imprimir. " +
                "O mínimo pra não sair no negativo é ${breakEvenTotal.toMoney()}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        Text(
            "Mínimo pra não ter prejuízo: ${breakEvenTotal.toMoney()}. " +
                "Margem obtida neste preço: ${quote.actualProfitMargin.toPercentText()}.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Mesma peça calculada em cada impressora cadastrada: responde "em qual máquina sai mais barato". */
@Composable
private fun PrinterComparison(comparison: List<Pair<PrinterProfile, Quote>>, extras: Double) {
    val cheapest = comparison.minByOrNull { it.second.salePrice }?.first?.id

    Text("Comparar impressoras", style = MaterialTheme.typography.titleMedium)
    comparison.forEach { (printer, quote) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (printer.id == cheapest) "${printer.name} (mais barata)" else printer.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (printer.id == cheapest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            NumericText((quote.salePrice + extras).toMoney(), style = MaterialTheme.typography.bodyMedium)
        }
    }
    Text(
        "Mesma peça, mesmas configurações, trocando só a máquina. A diferença vem do consumo de " +
            "energia, da manutenção e do retorno do investimento de cada uma.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumericText(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SaveQuoteForm(
    form: SaveQuoteFormState,
    viewModel: QuoteViewModel,
    canSave: Boolean,
    onSave: () -> Unit,
    onEditingFinished: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.editingQuoteId != null) "Editar orçamento salvo" else "Salvar orçamento", style = MaterialTheme.typography.titleMedium)
        if (form.editingQuoteId != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Editando um orçamento já salvo — a data de criação original é mantida.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { viewModel.resetForm(); onEditingFinished() }) { Text("Cancelar edição") }
            }
        }
        form.duplicatedFromName?.let { originalName ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Duplicado de \"$originalName\" — revise os dados e clique em Salvar pra criar um orçamento novo.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = viewModel::resetForm) { Text("Cancelar") }
            }
        }

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
            val triangleCount = remember(stlFile) { runCatching { peekStlTriangleCount(stlFile.bytes) }.getOrDefault(0L) }
            if (triangleCount > MAX_RENDERABLE_STL_TRIANGLES) {
                Text(
                    "Esse modelo é muito complexo pra pré-visualizar (~$triangleCount triângulos) — " +
                        "o arquivo foi salvo normalmente, mas sem prévia 3D nesta versão, pra não travar o app.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                val mesh = remember(stlFile) { runCatching { parseStl(stlFile.bytes) }.getOrNull() }
                if (mesh != null) {
                    Text(
                        "Arraste pra girar, use a roda do mouse pra zoom.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val viewerState = rememberStl3DViewerState(mesh)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Stl3DViewer(state = viewerState, mesh = mesh, modifier = Modifier.fillMaxWidth().height(280.dp))
                    }
                    val baseColor = MaterialTheme.colorScheme.primary
                    OutlinedButton(onClick = {
                        val snapshot = viewerState.captureSnapshot(baseColor, width = 1000, height = 1000)
                        viewModel.setPhotoFromStlSnapshot(encodeImageBitmapToPng(snapshot))
                    }) { Text("Capturar como foto do orçamento") }

                    val analysis = remember(mesh) { StlAnalyzer.analyze(mesh) }
                    Text(
                        "Nível de dificuldade sugerido: ${analysis.difficulty.label}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Área: ${(analysis.surfaceAreaMm2 / 100).formatOneDecimal()} cm² · " +
                            "Volume: ${(analysis.volumeMm3 / 1000).formatOneDecimal()} cm³ · " +
                            "Overhang: ${(analysis.overhangPercentage / 100).toPercentText()} · " +
                            "Peças no arquivo: ${analysis.disconnectedComponents}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!analysis.isManifold) {
                        Text(
                            "⚠ Esse arquivo STL tem geometria com furos ou normais invertidas (não-manifold) " +
                                "— pode dar problema na hora de fatiar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "Nível de dificuldade e medidas são estimativas (não substituem o fatiador real) e uso " +
                            "só interno — não entram no PDF nem no copiar-colar; servem só pra decidir se cobra " +
                            "uma margem extra por complexidade.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "Não consegui ler esse arquivo STL — pode estar corrompido ou num formato não suportado.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            OutlinedButton(onClick = viewModel::pickStl) { Text("Anexar arquivo STL (opcional)") }
        }
        Text(
            "Guardado pra você recuperar depois no Histórico e reaproveitar numa venda futura da " +
                "mesma peça — não entra no PDF nem no copiar/colar.",
            style = MaterialTheme.typography.bodySmall,
        )

        var showPrintSettingsDialog by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showPrintSettingsDialog = true }) {
            Text(if (form.printSettings.isEmpty) "Adicionar configurações de impressão" else "Editar configurações de impressão")
        }
        if (showPrintSettingsDialog) {
            PrintSettingsDialog(
                initial = form.printSettings,
                onDismiss = { showPrintSettingsDialog = false },
                onSave = viewModel::setPrintSettings,
            )
        }

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

        Button(onClick = onSave, enabled = canSave) { Text(if (form.editingQuoteId != null) "Salvar alterações" else "Salvar orçamento") }
        if (!canSave) {
            Text(
                "Preencha filamento, impressora, comprimento e tempo (ou importe do G-code) pra poder salvar.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

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
    // Pro seletor de canal, `null` não é "ainda não escolheu": é a venda direta, uma opção de
    // verdade que precisa aparecer escrita em vez de deixar o campo em branco.
    emptyText: String = "",
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selected?.let(displayText) ?: emptyText,
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
