package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import com.threedreport.app.ui.components.IconLabel
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.weekdayName
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.components.SubsectionTitle
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.services.ServiceChargeSelector
import com.threedreport.app.platform.encodeImageBitmapToPng
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.app.ui.viewer.rememberStl3DViewerState
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
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

/**
 * Abaixo disso a tela volta pra uma coluna só. O valor cobre a janela padrão do app com folga e
 * ainda deixa as duas colunas legíveis; espremer mais faria os campos e a nota ficarem estreitos
 * demais pra ler de relance, que é justamente o que o layout de duas colunas tenta resolver.
 */
private val TWO_COLUMN_MIN_WIDTH = 1100.dp

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
fun QuoteScreen(
    viewModel: QuoteViewModel,
    modifier: Modifier = Modifier,
    onEditingFinished: () -> Unit = {},
    onCancelOperation: () -> Unit = {},
) {
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
    val quote = result.quote

    // Em tela larga, entradas à esquerda e o dinheiro à direita, recalculando enquanto se digita:
    // antes era uma coluna só e não dava pra ver o preço e os campos ao mesmo tempo. Janela
    // estreita volta pra coluna única, que continua sendo o layout que sempre funcionou.
    Column(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (maxWidth >= TWO_COLUMN_MIN_WIDTH) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    QuoteInputs(viewModel, filaments, printers, services, salesChannels, settings, input, result, currency)
                    HorizontalDivider()
                    SaveQuoteFormSection(viewModel, saveForm, result, onEditingFinished)
                }
                VerticalDivider()
                Column(
                    modifier = Modifier.weight(0.8f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    QuoteResultSection(viewModel, filaments, allFilaments, printers, services, salesChannels, settings, input, result)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuoteInputs(viewModel, filaments, printers, services, salesChannels, settings, input, result, currency)
                HorizontalDivider()
                QuoteResultSection(viewModel, filaments, allFilaments, printers, services, salesChannels, settings, input, result)
                HorizontalDivider()
                SaveQuoteFormSection(viewModel, saveForm, result, onEditingFinished)
            }
        }
    }
    OperationBar(saveForm, isProduct = input.isProduct, onCancel = onCancelOperation)
    }
}

/**
 * Faixa fixa no rodapé enquanto o formulário veio de uma operação começada no Histórico (Vender,
 * Duplicar, Guardar no catálogo), com o jeito de desistir sempre à vista (decisão 102). Antes o
 * "Cancelar" ficava no meio do formulário, entre os campos, e era difícil de achar. O fundo
 * avermelhado diz que há algo em andamento, sem ser alarme: nada foi salvo ainda.
 */
@Composable
private fun OperationBar(form: SaveQuoteFormState, isProduct: Boolean, onCancel: () -> Unit) {
    val (text, cancelLabel) = when {
        form.soldFromProductName != null -> Pair(
            "Vendendo o produto \"${form.soldFromProductName}\": preencha o cliente e o prazo e salve o pedido. " +
                "O produto continua no catálogo, sem mudar nada.",
            "Cancelar venda",
        )
        form.copiedFromOrderName != null -> Pair(
            "Copiando o pedido \"${form.copiedFromOrderName}\" pro catálogo: revise e clique em \"Salvar no catálogo\". " +
                "Preço negociado, frete, cliente e prazo não vêm junto. O pedido não muda.",
            "Cancelar cópia",
        )
        form.duplicatedFromName != null -> Pair(
            "Duplicando \"${form.duplicatedFromName}\": revise os dados e salve pra criar " +
                (if (isProduct) "um produto novo." else "um pedido novo.") + " O original não muda.",
            "Cancelar duplicação",
        )
        else -> return
    }
    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(AppIcons.Info, contentDescription = null)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { IconLabel(AppIcons.Close, cancelLabel) }
        }
    }
}

/** Dados da peça: o que o criador preenche pra o cálculo acontecer. */
@Composable
private fun QuoteInputs(
    viewModel: QuoteViewModel,
    filaments: List<Filament>,
    printers: List<PrinterProfile>,
    services: List<Service>,
    salesChannels: List<SalesChannel>,
    settings: PricingSettings,
    input: QuoteInputState,
    result: QuoteResult,
    currency: Currency,
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

    // Só aparece pra quem configurou quanto vale a própria hora: sem isso, o campo não teria
    // efeito nenhum no preço e seria só mais uma caixa pra ignorar. Fica depois da quantidade
    // porque o tempo é do pedido inteiro (decisão 94).
    if (settings.laborRatePerHour > 0) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.laborMinutesText,
            onValueChange = viewModel::setLaborMinutes,
            label = { Text("Seu tempo de trabalho no pedido (min)") },
        )
        val quantity = input.quantity
        val scope = if (quantity > 1) "Total das $quantity peças, fora o tempo de máquina" else "Fora o tempo de máquina"
        Text(
            "$scope: fatiar, montar a mesa, tirar da mesa, remover suporte, lixar, pintar, " +
                "embalar. Cobrado a ${settings.laborRatePerHour.toCurrencyText(currency)}/h " +
                "(ajustável em Configurações). Se o acabamento já está no percentual de " +
                "Configurações, não conte ele aqui." +
                (if (quantity > 1) " Mudou a quantidade? Revise o tempo." else ""),
            style = MaterialTheme.typography.bodySmall,
        )
        // Lembrete, não erro: sem ele, configurar a hora e não ver o preço mudar parece defeito.
        if (input.isLaborTimeMissing) {
            Text(
                "Seu trabalho ainda não entra no preço: informe quantos minutos este pedido te dá.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    // Serviço marcado num orçamento reaberto que já saiu do catálogo continua aparecendo, pra não
    // sumir do pedido ao salvar de novo.
    val orphanServices = input.selectedServices.filterKeys { id -> services.none { it.id == id } }
    if (services.isNotEmpty() || orphanServices.isNotEmpty()) {
        SectionTitle(AppIcons.Handyman, "Serviços opcionais")
        services.forEach { service ->
            ServiceRow(
                viewModel = viewModel,
                id = service.id,
                name = service.name,
                suggestedPrice = service.price,
                serviceInput = input.selectedServices[service.id],
                quantity = input.quantity,
            )
        }
        orphanServices.forEach { (id, serviceInput) ->
            ServiceRow(viewModel, id, serviceInput.name, suggestedPrice = null, serviceInput, input.quantity)
        }
        Text(
            "O valor é deste pedido: marque o serviço e digite quanto vai cobrar por ele.",
            style = MaterialTheme.typography.bodySmall,
        )
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

    // Produto do catálogo não tem frete (decisão 101): quem paga e pra onde vai só existe na venda.
    if (!input.isProduct) {
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
    }
}

/**
 * Um serviço na lista do orçamento. Desmarcado, só o nome (e o valor sugerido, se houver).
 * Marcado, ganha o campo de valor deste pedido e, com mais de uma peça, a escolha entre cobrar por
 * peça ou uma vez no pedido, com o total ao lado pra conta nunca virar surpresa.
 */
@Composable
private fun ServiceRow(
    viewModel: QuoteViewModel,
    id: String,
    name: String,
    suggestedPrice: Double?,
    serviceInput: ServiceInput?,
    quantity: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = serviceInput != null, onCheckedChange = { viewModel.toggleService(id) })
            Text(if (serviceInput == null && suggestedPrice != null) "$name · sugerido ${suggestedPrice.toMoney()}" else name)
        }
        if (serviceInput == null) return@Column

        val price = parseDecimal(serviceInput.priceText)?.takeIf { it >= 0 }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp).tabToNavigate(),
            value = serviceInput.priceText,
            onValueChange = { viewModel.setServicePrice(id, it) },
            label = { Text(if (quantity > 1 && !serviceInput.chargedPerOrder) "Valor por peça" else "Valor") },
            isError = price == null,
            supportingText = if (price == null) ({ Text("Informe o valor") }) else null,
            singleLine = true,
        )
        if (quantity > 1) {
            Row(
                modifier = Modifier.padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServiceChargeSelector(
                    chargedPerOrder = serviceInput.chargedPerOrder,
                    onChange = { viewModel.setServiceChargedPerOrder(id, it) },
                )
                if (price != null) {
                    val total = if (serviceInput.chargedPerOrder) price else price * quantity
                    NumericText("= ${total.toMoney()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** O lado do dinheiro: quanto cobrar, o que sobra e como isso muda numa negociação. */
@Composable
private fun QuoteResultSection(
    viewModel: QuoteViewModel,
    filaments: List<Filament>,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    services: List<Service>,
    salesChannels: List<SalesChannel>,
    settings: PricingSettings,
    input: QuoteInputState,
    result: QuoteResult,
) {
    SectionTitle(AppIcons.Calculate, "Resultado")

    val saveForm by viewModel.saveForm.collectAsState()

    val quote = result.quote
    when {
        result.errorMessage != null -> Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
        quote != null -> QuoteReceipt(
            quote = quote,
            selectedServices = result.selectedServices,
            grandTotal = result.grandTotal ?: quote.salePrice,
            deliveryDateEpochDay = saveForm.deliveryDateEpochDay.takeUnless { input.isProduct },
        )
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
            isProduct = input.isProduct,
            // Só com o campo vazio: depois de aplicado, ou com um anunciado próprio, o atalho sobrava
            // e podia até sugerir baixar o preço.
            showcaseSuggestion = if (input.isProduct && input.targetTotalText.isBlank()) {
                viewModel.showcasePriceSuggestion((quote.tableSalePrice ?: quote.salePrice) + result.servicesTotal + result.shippingCost)
            } else {
                null
            },
            onApplyShowcaseSuggestion = viewModel::applyShowcasePrice,
            announcedPiecePrice = input.announcedUnitPrice?.takeIf { input.targetTotalText.isBlank() }?.let { it * input.quantity },
        )

        val comparison = viewModel.comparePrinters(filaments, printers, settings, services, input, salesChannels)
        if (comparison.size > 1) {
            PrinterComparison(comparison = comparison, extras = result.servicesTotal + result.shippingCost)
        }
    }
}

@Composable
private fun SaveQuoteFormSection(
    viewModel: QuoteViewModel,
    saveForm: SaveQuoteFormState,
    result: QuoteResult,
    onEditingFinished: () -> Unit,
) {
    val quote = result.quote
    val input by viewModel.input.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val printer = quote?.let { q -> printers.firstOrNull { it.id == q.printerId } }
    val queueHint = if (quote != null && printer != null && !input.isProduct) {
        viewModel.queueAheadOf(printer, savedQuotes, saveForm.editingQuoteId)?.let { queue ->
            val orders = if (queue.queuedQuoteCount == 1) "1 pedido aprovado ou imprimindo" else "${queue.queuedQuoteCount} pedidos aprovados ou imprimindo"
            "Fila da ${printer.name}: ${queue.queuedMinutes.minutesToDurationText()} de impressão em $orders · " +
                "esta peça: ${(quote.job.printTimeMinutes * quote.quantity).minutesToDurationText()}."
        }
    } else {
        null
    }
    SaveQuoteForm(
        form = saveForm,
        isProduct = input.isProduct,
        viewModel = viewModel,
        canSave = quote != null && !result.missingServicePrice,
        cannotSaveReason = if (quote != null && result.missingServicePrice) {
            "Informe o valor de cada serviço marcado (ou desmarque) pra poder salvar."
        } else {
            "Preencha filamento, impressora, comprimento e tempo (ou importe do G-code) pra poder salvar."
        },
        queueHint = queueHint,
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
/**
 * Resultado do cálculo como uma "nota": o valor que de fato é cobrado do cliente
 * ([grandTotal] — venda + serviços) em destaque no topo, com a composição do preço (custo de
 * produção, lucro, cada serviço) como itens de nota abaixo. Antes disso era uma pilha de `Text`
 * do mesmo peso, sem indicar qual número é o que realmente importa pra fechar a venda.
 */
@Composable
private fun QuoteReceipt(quote: Quote, selectedServices: List<QuoteService>, grandTotal: Double, deliveryDateEpochDay: Long?) {
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
            // O mesmo prazo que vai sair em destaque no PDF, pra o vendedor ver na nota o que o
            // cliente vai ler, enquanto ainda está escolhendo.
            deliveryDateEpochDay?.let {
                Text(
                    "Entrega até ${formatDate(it)} (${weekdayName(it)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (it < todayEpochDay()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
            PriceCompositionBar(quote)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ReceiptLine("Custo de produção", quote.productionCost.toMoney())
            ReceiptLine("Lucro", quote.profit.toMoney())
            selectedServices.forEach { service ->
                val multiplied = quote.quantity > 1 && !service.chargedPerOrder
                val label = if (multiplied) "${service.name} (× ${quote.quantity})" else service.name
                ReceiptLine(label, service.total(quote.quantity).toMoney())
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
    isProduct: Boolean = false,
    showcaseSuggestion: Double? = null,
    onApplyShowcaseSuggestion: (Double) -> Unit = {},
    announcedPiecePrice: Double? = null,
) {
    // Produto do catálogo não tem cliente pra negociar (decisão 101), mas tem o preço que se
    // anuncia (decisão 102): o mesmo preço fechado, com outro nome. Calculado R$ 18,37, anunciado
    // R$ 18,90; o anunciado vai pro catálogo, e o calculado fica guardado pra comparar.
    if (isProduct) {
        SectionTitle(AppIcons.Sell, "Preço anunciado")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = targetTotalText,
            onValueChange = onTargetTotalChange,
            label = { Text("Preço anunciado no catálogo (opcional)") },
        )
        showcaseSuggestion?.let { suggestion ->
            AssistChip(onClick = { onApplyShowcaseSuggestion(suggestion) }, label = { Text("Arredondar pra ${suggestion.toMoney()}") })
        }
        Text(
            "Um valor redondo pra vitrine, no lugar do que a margem deu. É ele que sai no catálogo, " +
                "no PDF e na imagem, e é o preço que o pedido recebe quando você clica em \"Vender\". " +
                "Deixe vazio pra anunciar o preço calculado.",
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        SectionTitle(AppIcons.Handshake, "Negociação")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = targetTotalText,
            onValueChange = onTargetTotalChange,
            label = { Text("Preço fechado com o cliente (opcional)") },
        )
        announcedPiecePrice?.let { price ->
            Text(
                "Preço anunciado do produto: ${price.toMoney()}. Frete e serviços somam por fora. " +
                    "Digite um valor aqui pra fechar outro preço.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Digite aqui o valor que o cliente propôs e veja o que sobra. Ele passa a ser o preço de " +
                "verdade do orçamento: é o que vai pro PDF, pro histórico e pro Dashboard. Deixe vazio " +
                "pra usar o preço da sua margem.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

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

    SectionTitle(AppIcons.Printer3d, "Comparar impressoras")
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

/**
 * Onde o dinheiro do preço está, em barra empilhada mais legenda. Existe porque "custo de produção
 * + lucro" não conta o suficiente: ver que a máquina pesa mais que o plástico, ou que o próprio
 * trabalho é a maior fatia, é o que ensina a precificar e a saber onde mexer quando o preço ficar
 * alto demais pro cliente.
 *
 * Fatias zeradas são omitidas: `Modifier.weight` não aceita zero, e uma legenda cheia de "R$ 0,00"
 * só atrapalharia a leitura.
 */
@Composable
private fun PriceCompositionBar(quote: Quote) {
    val costs = quote.costs
    val slices = listOf(
        CompositionSlice("Material", costs.material, MaterialTheme.colorScheme.primary),
        CompositionSlice("Energia", costs.energy, MaterialTheme.colorScheme.tertiary),
        CompositionSlice(
            "Máquina",
            costs.maintenance + costs.investmentReturn + costs.fixedCost,
            MaterialTheme.colorScheme.secondary,
        ),
        CompositionSlice("Seu trabalho", costs.labor + costs.finishing, MaterialTheme.colorScheme.error),
        CompositionSlice("Reserva de falha", costs.failures, MaterialTheme.colorScheme.outline),
        CompositionSlice("Administrativo", costs.administrative, MaterialTheme.colorScheme.outlineVariant),
        CompositionSlice("Lucro", quote.profit, MaterialTheme.colorScheme.primaryContainer),
    ).filter { it.value > 0.0 }

    if (slices.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
    ) {
        slices.forEach { slice ->
            Box(modifier = Modifier.weight(slice.value.toFloat()).fillMaxHeight().background(slice.color))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(slice.color))
                Text(
                    "${slice.label} ${slice.value.toMoney()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class CompositionSlice(val label: String, val value: Double, val color: Color)

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
    isProduct: Boolean,
    viewModel: QuoteViewModel,
    canSave: Boolean,
    queueHint: String?,
    cannotSaveReason: String,
    onSave: () -> Unit,
    onEditingFinished: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val isEditing = form.editingQuoteId != null
        SectionTitle(
            AppIcons.Save,
            when {
                isEditing && isProduct -> "Editar produto"
                isEditing -> "Editar orçamento salvo"
                else -> "Salvar"
            },
        )
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isProduct) {
                        "Editando um produto do catálogo: a data de criação original é mantida."
                    } else {
                        "Editando um orçamento já salvo: a data de criação original é mantida."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { viewModel.resetForm(); onEditingFinished() }) { Text("Cancelar edição") }
            }
        }
        // O tipo só se escolhe ao criar: na edição, e quando o formulário veio de "Vender" ou de
        // "Guardar no catálogo", o destino já está decidido e o aviso acima diz qual é.
        if (!isEditing && form.soldFromProductName == null && form.copiedFromOrderName == null) {
            KindSelector(isProduct = isProduct, onSelect = viewModel::setKind)
        }

        SubsectionTitle(AppIcons.Visibility, "O que o cliente vê", modifier = Modifier.padding(top = 8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = viewModel::setSaveName,
            label = { Text("Nome (opcional)") },
        )
        if (isProduct) {
            val savedQuotes by viewModel.savedQuotes.collectAsState()
            CategoryField(value = form.category, suggestions = viewModel.knownCategories(savedQuotes), onValueChange = viewModel::setCategory)
        }

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

        if (!isProduct) {
            Text("Prazo de entrega (opcional)", style = MaterialTheme.typography.labelLarge)
            DeliveryDatePicker(epochDay = form.deliveryDateEpochDay, onChange = viewModel::setDeliveryDate)
            queueHint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // Daqui pra baixo nada vai pro cliente. O título separa as duas metades do formulário, em
        // vez de depender de cada rótulo dizer "(uso interno)".
        SubsectionTitle(AppIcons.Lock, "Só pra você (não sai no PDF nem na mensagem)", modifier = Modifier.padding(top = 8.dp))

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

        if (!isProduct) {
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
        }

        Button(onClick = onSave, enabled = canSave) {
            Text(
                when {
                    isEditing -> "Salvar alterações"
                    isProduct -> "Salvar no catálogo"
                    else -> "Salvar como pedido"
                },
            )
        }
        if (!canSave) {
            Text(cannotSaveReason, style = MaterialTheme.typography.bodySmall)
        }

        ShowSnackbarOnce(
            form.savedConfirmation,
            if (form.savedAsProduct) "Produto salvo no catálogo (Histórico, em Produtos)." else "Orçamento salvo no histórico.",
            viewModel::consumeSavedConfirmation,
        )
    }
}

/**
 * Pedido de cliente ou produto do catálogo (decisão 101). Vem antes dos campos de propósito: o que
 * não faz sentido pra produto (cliente, prazo, frete, preço negociado) some antes de a pessoa
 * gastar tempo preenchendo.
 */
@Composable
private fun KindSelector(isProduct: Boolean, onSelect: (QuoteKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isProduct, onClick = { onSelect(QuoteKind.ORDER) }, label = { Text("Pedido de cliente") })
            FilterChip(selected = isProduct, onClick = { onSelect(QuoteKind.PRODUCT) }, label = { Text("Produto do catálogo") })
        }
        Text(
            if (isProduct) {
                "Uma peça que você oferece, com preço, sem cliente nem andamento. Fica no Histórico, em " +
                    "Produtos, fora do Kanban e do Dashboard. Quando alguém comprar, é só clicar em \"Vender\"."
            } else {
                "Um orçamento pra um cliente. Fica no Histórico, em Pedidos, e segue o andamento de " +
                    "Orçado até Entregue."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Categoria do produto (decisão 102): texto livre com as já usadas sugeridas embaixo, filtradas pelo
 * que se digita. Sem tela de cadastro: a lista de categorias é o que já foi digitado antes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(value: String, suggestions: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val matches = suggestions.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }

    ExposedDropdownMenuBox(expanded = expanded && matches.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).tabToNavigate(),
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text("Categoria (opcional)") },
            placeholder = { Text("Ex.: Chaveiros, Decoração, Utilidades") },
            trailingIcon = if (suggestions.isNotEmpty()) {
                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && matches.isNotEmpty()) }
            } else {
                null
            },
            singleLine = true,
        )
        DropdownMenu(expanded = expanded && matches.isNotEmpty(), onDismissRequest = { expanded = false }) {
            matches.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = { onValueChange(category); expanded = false })
            }
        }
    }
    Text(
        "Separa a lista de produtos e o catálogo em PDF em seções.",
        style = MaterialTheme.typography.bodySmall,
    )
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
