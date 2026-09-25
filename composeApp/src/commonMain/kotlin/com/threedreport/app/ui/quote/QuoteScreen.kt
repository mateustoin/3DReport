package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.encodeImageBitmapToPng
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.weekdayName
import com.threedreport.app.ui.components.ClientField
import com.threedreport.app.ui.components.FieldHelp
import com.threedreport.app.ui.components.IconLabel
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.components.ShowNotice
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.components.SubsectionTitle
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.filaments.displayText
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.services.ServiceChargeSelector
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.app.ui.viewer.rememberStl3DViewerState
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.Service
import kotlin.math.abs
import kotlin.math.round

/** Opção padrão do seletor de canal: venda sem intermediário e sem taxa (Pix, dinheiro, entrega em mãos). */
private const val DIRECT_SALE_LABEL = "Venda direta (sem taxa)"

/**
 * Abaixo disso a tela volta pra uma coluna só. O valor cabe na janela padrão do app (1280) já descontada a
 * barra lateral com rótulos (decisão 111), e ainda deixa as duas colunas com uns 500 dp cada; espremer mais
 * faria os campos e a nota ficarem estreitos demais pra ler de relance, que é justamente o que o layout de
 * duas colunas tenta resolver.
 */
private val TWO_COLUMN_MIN_WIDTH = 1040.dp

/** Arredonda pra 1 casa decimal, separador decimal brasileiro (vírgula) — mesmo estilo de `toWeightText()`. */
private fun Double.formatOneDecimal(): String {
    val tenths = round(this * 10).toLong()
    val whole = tenths / 10
    val decimal = abs(tenths % 10)
    return if (decimal == 0L) "$whole" else "$whole,$decimal"
}

/**
 * Tela de Orçamento: dados da peça e resultado calculado. [onSave] é o botão Salvar: num orçamento novo,
 * salva e limpa pra o próximo; editando um salvo (decisão 113), salva e volta pra onde a edição começou.
 */
@Composable
fun QuoteScreen(
    viewModel: QuoteViewModel,
    modifier: Modifier = Modifier,
    onSave: () -> Unit = { viewModel.saveCurrentQuote() },
    onCancelOperation: () -> Unit = {},
) {
    val allFilaments by viewModel.filaments.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val services by viewModel.services.collectAsState()
    val salesChannels by viewModel.salesChannels.collectAsState()
    val input by viewModel.input.collectAsState()
    val saveForm by viewModel.saveForm.collectAsState()

    // Uma conta por mudança de entrada ou de cadastro, e não por redesenho: a mesma que o Ctrl+S salva.
    val result = remember(allFilaments, printers, settings, services, salesChannels, input, saveForm.operation) { viewModel.currentResult() }
    val comparison = remember(allFilaments, printers, settings, services, salesChannels, input) {
        viewModel.comparePrinters(allFilaments, printers, settings, services, input, salesChannels)
    }
    val currency = LocalCurrency.current

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
                        QuoteInputs(viewModel, allFilaments, printers, services, salesChannels, settings, input, saveForm, result, currency)
                        HorizontalDivider()
                        SaveQuoteFormSection(viewModel, saveForm, input, result, onSave)
                    }
                    VerticalDivider()
                    Column(
                        modifier = Modifier.weight(0.8f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        QuoteResultSection(viewModel, allFilaments, printers, input, saveForm, result, comparison)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    QuoteInputs(viewModel, allFilaments, printers, services, salesChannels, settings, input, saveForm, result, currency)
                    HorizontalDivider()
                    QuoteResultSection(viewModel, allFilaments, printers, input, saveForm, result, comparison)
                    HorizontalDivider()
                    SaveQuoteFormSection(viewModel, saveForm, input, result, onSave)
                }
            }
        }
        OperationBar(saveForm, isProduct = input.isProduct, onCancel = onCancelOperation)
    }

    saveForm.blockedMessage?.let { message ->
        ShowSnackbarOnce(true, message, viewModel::consumeBlockedMessage)
    }
    val notice by viewModel.notice.collectAsState()
    ShowNotice(notice, viewModel::consumeNotice)
}

/**
 * Faixa fixa no rodapé enquanto o formulário veio de uma operação começada em Pedidos ou no Catálogo
 * (Editar, Vender, Duplicar, Guardar no catálogo), com o jeito de desistir sempre à vista (decisões 102
 * e 113).
 *
 * Visual neutro com filete âmbar (decisão 103), a mesma assinatura das faixas de destaque do PDF
 * (decisão 35): diz que há algo em andamento sem parecer erro.
 */
@Composable
private fun OperationBar(form: SaveQuoteFormState, isProduct: Boolean, onCancel: () -> Unit) {
    val (title, detail, cancelLabel) = when (val operation = form.operation) {
        is QuoteOperation.Editing -> Triple(
            "Editando " + listOfNotNull(operation.savedQuote.displayNumber?.takeIf { operation.savedQuote.isOrder }, "\"${operation.savedQuote.name}\"").joinToString(" "),
            "Salve pra atualizar " + (if (isProduct) "o produto." else "o pedido.") +
                " O que estava sendo feito no Orçamento volta quando você terminar.",
            "Cancelar edição",
        )
        is QuoteOperation.Selling -> Triple(
            "Vendendo \"${operation.product.name}\"",
            "Preencha o cliente e o prazo e salve o pedido. O produto continua no catálogo, sem mudar nada.",
            "Cancelar venda",
        )
        is QuoteOperation.CopyingToCatalog -> Triple(
            "Copiando \"${operation.fromName}\" pro catálogo",
            "Revise e clique em \"Salvar no catálogo\". Preço negociado, frete, cliente e prazo não vêm junto. O pedido não muda.",
            "Cancelar cópia",
        )
        is QuoteOperation.Duplicating -> Triple(
            "Duplicando \"${operation.fromName}\"",
            "Revise os dados, preencha o cliente e salve pra criar " + (if (isProduct) "um produto novo." else "um pedido novo.") +
                " O original não muda.",
            "Cancelar duplicação",
        )
        else -> return
    }
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
            Row(
                modifier = Modifier.weight(1f).padding(start = 20.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(AppIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onCancel) { IconLabel(AppIcons.Close, cancelLabel) }
            }
        }
    }
}

/** Dados da peça: o que o criador preenche pra o cálculo acontecer. */
@Composable
private fun QuoteInputs(
    viewModel: QuoteViewModel,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    services: List<Service>,
    salesChannels: List<SalesChannel>,
    settings: PricingSettings,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    result: QuoteResult,
    currency: Currency,
) {
    // O tipo vem primeiro (decisão 108): ele esconde frete e negociação, então escolher depois de
    // preencher era jogar trabalho fora. Na edição, e quando o formulário veio de "Vender" ou de
    // "Guardar no catálogo", o destino já está decidido.
    if (form.operation == null || form.operation is QuoteOperation.Duplicating) {
        KindSelector(isProduct = input.isProduct, onSelect = viewModel::setKind)
    }

    PrintsSection(viewModel, allFilaments, printers, input, result, currency)

    val quantityError = result.fieldErrors[QuoteFields.QUANTITY]
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = input.quantityText,
        onValueChange = viewModel::setQuantity,
        label = { Text("Quantidade de peças") },
        placeholder = { Text("1") },
        singleLine = true,
        isError = quantityError != null,
        supportingText = if (quantityError != null) ({ Text(quantityError) }) else null,
    )
    FieldHelp(
        "Peso, comprimento e tempo acima são de UMA peça: o app multiplica pela quantidade.",
        "Se você fatiou a mesa inteira de uma vez e os números já são do lote todo, deixe a quantidade em 1.",
    )

    // Só aparece pra quem configurou quanto vale a própria hora: sem isso, o campo não teria
    // efeito nenhum no preço. Fica depois da quantidade porque o tempo é do pedido inteiro (decisão 94).
    if (settings.laborRatePerHour > 0) {
        val laborError = result.fieldErrors[QuoteFields.LABOR]
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.laborMinutesText,
            onValueChange = viewModel::setLaborMinutes,
            label = { Text("Seu tempo de trabalho no pedido") },
            placeholder = { Text("1h30 ou 90 (minutos)") },
            singleLine = true,
            isError = laborError != null,
            supportingText = if (laborError != null) ({ Text(laborError) }) else null,
        )
        val quantity = input.quantity
        FieldHelp(
            (if (quantity > 1) "Total das $quantity peças" else "O pedido inteiro") +
                ", cobrado a ${settings.laborRatePerHour.toCurrencyText(currency)}/h." +
                (if (quantity > 1) " Mudou a quantidade? Revise o tempo." else ""),
            "Fora o tempo de máquina: fatiar, montar a mesa, tirar da mesa, remover suporte, lixar, pintar, " +
                "embalar. O valor da hora é ajustável em Configurações. Se o acabamento já está no percentual " +
                "de Configurações, não conte ele aqui.",
        )
        // Lembrete, não erro: sem ele, configurar a hora e não ver o preço mudar parece defeito.
        if (input.isLaborTimeMissing) {
            Text(
                "Seu trabalho ainda não entra no preço: informe quanto tempo este pedido te dá.",
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
                suggestedPrice = service.suggestedPrice,
                serviceInput = input.selectedServices[service.id],
                quantity = input.quantity,
            )
        }
        orphanServices.forEach { (id, serviceInput) ->
            ServiceRow(viewModel, id, serviceInput.name, suggestedPrice = null, serviceInput, input.quantity)
        }
        FieldHelp("O valor é deste pedido: marque o serviço e digite quanto vai cobrar por ele.")
    }

    SectionTitle(AppIcons.Storefront, "A venda")
    // Com o canal do orçamento excluído, o seletor aparece mesmo sem canais cadastrados: é nele que se
    // escolhe "Venda direta" pra liberar o cálculo.
    if (salesChannels.isNotEmpty() || input.missingChannelName != null) {
        LabeledDropdown(
            label = "Canal de venda",
            items = listOf(null) + salesChannels,
            selected = result.salesChannel,
            itemLabel = { it?.let { channel -> "${channel.name} · ${channel.feeRate.toPercentText()}" } ?: DIRECT_SALE_LABEL },
            displayText = { it?.name ?: DIRECT_SALE_LABEL },
            onSelect = { viewModel.selectSalesChannel(it?.id) },
            emptyText = input.missingChannelName?.let { "$it (excluído)" } ?: DIRECT_SALE_LABEL,
        )
        FieldHelp(
            "A taxa do canal sai do que você recebe; o preço sobe o suficiente pra sua margem não mudar.",
            "A taxa incide sobre tudo o que o cliente paga, inclusive serviços e frete, como a maquininha e o " +
                "marketplace cobram de verdade. Cadastre os canais em Configurações.",
        )
    }

    input.missingChannelName?.let { name ->
        Text(
            "O canal \"$name\" deste orçamento não existe mais no cadastro. O preço salvo continua valendo; " +
                "pra recalcular, escolha outro canal ou \"Venda direta\" acima.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // Produto do catálogo não tem frete (decisão 101): quem paga e pra onde vai só existe na venda.
    if (!input.isProduct) {
        val shippingError = result.fieldErrors[QuoteFields.SHIPPING]
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = input.shippingCostText,
            onValueChange = viewModel::setShippingCost,
            label = { Text("Frete (${currency.symbol}, opcional)") },
            singleLine = true,
            isError = shippingError != null,
            supportingText = if (shippingError != null) ({ Text(shippingError) }) else null,
        )
        FieldHelp(
            "Linha própria no total, nunca embutido no preço da peça.",
            "Frete é repasse, não produto seu: não multiplica pela quantidade nem entra na margem. A taxa do " +
                "canal e o imposto incidem sobre ele, porque o cliente paga tudo junto.",
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
        // A linha inteira marca e desmarca (e o leitor de tela lê o nome junto da caixa).
        Row(
            modifier = Modifier.toggleable(value = serviceInput != null, role = Role.Checkbox, onValueChange = { viewModel.toggleService(id) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = serviceInput != null, onCheckedChange = null)
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
            supportingText = if (price == null) ({ Text(if (serviceInput.priceText.isBlank()) "Informe o valor" else "Não é um número") }) else null,
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
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    input: QuoteInputState,
    saveForm: SaveQuoteFormState,
    result: QuoteResult,
    comparison: List<Pair<PrinterProfile, Quote>>,
) {
    SectionTitle(AppIcons.Calculate, "Resultado")

    val quote = result.quote
    when {
        quote != null -> QuoteReceipt(
            quote = quote,
            selectedServices = result.selectedServices,
            grandTotal = result.grandTotal ?: quote.salePrice,
            shippingCost = result.shippingCost,
            deliveryDateEpochDay = saveForm.deliveryDateEpochDay.takeUnless { input.isProduct },
        )
        result.errorMessage != null -> Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
        result.fieldErrors.isNotEmpty() -> Text(
            "Corrija o campo marcado pra calcular: ${result.fieldErrors.values.first()}",
            color = MaterialTheme.colorScheme.error,
        )
        allFilaments.none { it.hasStockAvailable } && allFilaments.isNotEmpty() -> Text(
            "Todos os filamentos cadastrados estão marcados como esgotados. Marque algum como \"Em estoque\" em Filamentos.",
            style = MaterialTheme.typography.bodyMedium,
        )
        allFilaments.isEmpty() -> Text("Cadastre um filamento em Filamentos.", style = MaterialTheme.typography.bodyMedium)
        printers.isEmpty() -> Text("Cadastre uma impressora em Impressoras.", style = MaterialTheme.typography.bodyMedium)
        input.prints.size > 1 -> Text(
            "Preencha peso (ou comprimento) e tempo de cada impressão pra calcular, ou arraste os G-codes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        else -> Text("Preencha peso (ou comprimento) e tempo pra calcular, ou arraste o G-code.", style = MaterialTheme.typography.bodyMedium)
    }

    // Reabrindo um orçamento (decisão 108): o preço salvo fica até alguém mexer no que muda o preço, e a
    // tela diz o que os custos de hoje dariam, pra decisão de reprecificar ser de quem vende.
    if (quote != null && result.keepsOriginalPrice) {
        val today = result.todaysQuote
        Text(
            "Preço do orçamento salvo, mantido." + if (today != null && abs(today.customerTotal - quote.customerTotal) >= 0.005) {
                " Com os custos de hoje, sairia ${today.customerTotal.toMoney()}: mude qualquer valor da peça pra recalcular."
            } else {
                ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val original = result.originalQuote
    if (quote != null && original != null && abs(original.customerTotal - quote.customerTotal) >= 0.005) {
        Text(
            "Recalculado com os custos de hoje: era ${original.customerTotal.toMoney()} quando foi salvo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    if (quote != null) {
        NegotiationSection(
            quote = quote,
            targetTotalText = input.targetTotalText,
            targetError = result.fieldErrors[QuoteFields.TARGET],
            onTargetTotalChange = viewModel::setTargetTotal,
            isProduct = input.isProduct,
            // Só com o campo vazio: depois de aplicado, ou com um anunciado próprio, o atalho sobrava
            // e podia até sugerir baixar o preço.
            showcaseSuggestion = if (input.isProduct && input.targetTotalText.isBlank()) {
                viewModel.showcasePriceSuggestion((quote.tableSalePrice ?: quote.salePrice) + quote.extrasTotal)
            } else {
                null
            },
            onApplyShowcaseSuggestion = viewModel::applyShowcasePrice,
            announcedPiecePrice = input.announcedUnitPrice?.takeIf { input.targetTotalText.isBlank() }?.let { it * input.quantity },
        )

        if (comparison.size > 1) PrinterComparison(comparison = comparison, allPrints = input.prints.size > 1)
    }
}

@Composable
private fun SaveQuoteFormSection(
    viewModel: QuoteViewModel,
    saveForm: SaveQuoteFormState,
    input: QuoteInputState,
    result: QuoteResult,
    onSave: () -> Unit,
) {
    val quote = result.quote
    val printers by viewModel.printers.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    // Uma linha por impressora que o pedido usa: cada uma tem a própria fila.
    val queueHint = if (quote != null && !input.isProduct) {
        quote.printerIds.mapNotNull { printerId ->
            val printer = printers.firstOrNull { it.id == printerId } ?: return@mapNotNull null
            viewModel.queueAheadOf(printer, savedQuotes, saveForm.editingQuoteId)?.let { queue ->
                val orders = if (queue.queuedQuoteCount == 1) "1 pedido aprovado ou imprimindo" else "${queue.queuedQuoteCount} pedidos aprovados ou imprimindo"
                "Fila da ${printer.name}: ${queue.queuedMinutes.minutesToDurationText()} de impressão em $orders · " +
                    "este pedido: ${quote.printMinutesOn(printerId).minutesToDurationText()}."
            }
        }.joinToString("\n").ifEmpty { null }
    } else {
        null
    }
    SaveQuoteForm(
        form = saveForm,
        singlePrint = input.prints.singleOrNull(),
        isProduct = input.isProduct,
        viewModel = viewModel,
        canSave = quote != null && !result.missingServicePrice && result.fieldErrors.isEmpty(),
        cannotSaveReason = when {
            result.fieldErrors.isNotEmpty() -> "Corrija o campo marcado em vermelho pra poder salvar."
            quote != null && result.missingServicePrice -> "Informe o valor de cada serviço marcado (ou desmarque) pra poder salvar."
            result.errorMessage != null -> result.errorMessage
            else -> "Preencha filamento, impressora, peso (ou comprimento) e tempo, ou arraste o G-code, pra poder salvar."
        },
        queueHint = queueHint,
        onSave = onSave,
    )
}

/**
 * Resultado do cálculo como uma "nota": o valor que de fato é cobrado do cliente
 * ([grandTotal] — venda + serviços + frete) em destaque no topo, com a composição do preço (custo de
 * produção, lucro, cada serviço) como itens de nota abaixo.
 */
@Composable
private fun QuoteReceipt(
    quote: Quote,
    selectedServices: List<QuoteService>,
    grandTotal: Double,
    shippingCost: Double,
    deliveryDateEpochDay: Long?,
) {
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
                // A mesma conta do PDF e da mensagem: o preço de cada peça, com serviços e frete à parte.
                Text(
                    "${quote.quantity} × ${quote.unitSalePrice.toMoney()} = ${quote.salePrice.toMoney()}" +
                        when {
                            selectedServices.isNotEmpty() && shippingCost > 0 -> " + serviços e frete"
                            selectedServices.isNotEmpty() -> " + serviços"
                            shippingCost > 0 -> " + frete"
                            else -> ""
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Quanto sai de cada carretel, que é o que se confere no estoque.
            val filamentTotals = quote.filamentTotals()
            Text(
                "Consumo: " + filamentTotals.joinToString(" · ") { it.displayText() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Com várias impressões, o tempo total fica sempre à vista, mesmo com os cartões recolhidos.
            if (quote.prints.size > 1) {
                Text(
                    "${quote.prints.size} impressões · ${quote.totalPrintTimeMinutes.minutesToDurationText()} de máquina",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // O mesmo prazo que vai sair em destaque no PDF, pra o vendedor ver na nota o que o
            // cliente vai ler, enquanto ainda está escolhendo.
            deliveryDateEpochDay?.let {
                val overdue = it < todayEpochDay()
                Text(
                    "Entrega até ${formatDate(it)} (${weekdayName(it)})" + if (overdue) " — data já passou" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (quote.totalDeductionRate > 0.0) {
                val parts = buildList {
                    quote.channelName?.let { add("$it ${quote.channelFeeRate.toPercentText()}") }
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
            if (quote.prints.size > 1) PerPrintCosts(quote)
            ReceiptLine("Lucro", quote.profit.toMoney())
            selectedServices.forEach { service ->
                val multiplied = quote.quantity > 1 && !service.chargedPerOrder
                val label = if (multiplied) "${service.name} (× ${quote.quantity})" else service.name
                ReceiptLine(label, service.total(quote.quantity).toMoney())
            }
            if (shippingCost > 0) ReceiptLine("Frete", shippingCost.toMoney())
        }
    }
}

/**
 * "Por impressão", recolhido embaixo do custo de produção (decisão 114): o custo de cada mesa (material e
 * máquina) e, fechando a soma, o que é do pedido e não se divide entre elas (trabalho, falhas,
 * administrativo). Fica recolhido porque o que importa na conversa é o total.
 */
@Composable
private fun PerPrintCosts(quote: Quote) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(if (expanded) "Por impressão ▴" else "Por impressão ▾", style = MaterialTheme.typography.labelMedium)
    }
    if (!expanded) return
    Column(modifier = Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        quote.prints.forEachIndexed { index, print ->
            ReceiptLine("${printTitle(index + 1, print.job.name.orEmpty())} · ${print.printerName}", print.cost.total.toMoney())
        }
        val orderLevel = quote.productionCost - quote.prints.sumOf { it.cost.total }
        ReceiptLine("Do pedido (trabalho, falhas, administrativo)", orderLevel.toMoney())
    }
}

/**
 * Ferramenta de negociação: o vendedor digita o valor que o cliente propôs e vê na hora o que
 * sobra. O piso fica sempre à vista, porque é o número que ele precisa ter na cabeça no meio da conversa.
 */
@Composable
private fun NegotiationSection(
    quote: Quote,
    targetTotalText: String,
    targetError: String?,
    onTargetTotalChange: (String) -> Unit,
    isProduct: Boolean = false,
    showcaseSuggestion: Double? = null,
    onApplyShowcaseSuggestion: (Double) -> Unit = {},
    announcedPiecePrice: Double? = null,
) {
    // Produto do catálogo não tem cliente pra negociar (decisão 101), mas tem o preço que se
    // anuncia (decisão 102): o mesmo preço fechado, com outro nome.
    if (isProduct) {
        SectionTitle(AppIcons.Sell, "Preço anunciado")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = targetTotalText,
            onValueChange = onTargetTotalChange,
            label = { Text("Preço anunciado no catálogo (opcional)") },
            singleLine = true,
            isError = targetError != null,
            supportingText = if (targetError != null) ({ Text(targetError) }) else null,
        )
        showcaseSuggestion?.let { suggestion ->
            AssistChip(onClick = { onApplyShowcaseSuggestion(suggestion) }, label = { Text("Arredondar pra ${suggestion.toMoney()}") })
        }
        FieldHelp(
            "Um valor redondo pra vitrine, no lugar do que a margem deu.",
            "É ele que sai no catálogo, no PDF e na imagem, e é o preço que o pedido recebe quando você clica em " +
                "\"Vender\". Deixe vazio pra anunciar o preço calculado.",
        )
    } else {
        SectionTitle(AppIcons.Handshake, "Negociação")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = targetTotalText,
            onValueChange = onTargetTotalChange,
            label = { Text("Preço fechado com o cliente (opcional)") },
            singleLine = true,
            isError = targetError != null,
            supportingText = if (targetError != null) ({ Text(targetError) }) else null,
        )
        announcedPiecePrice?.let { price ->
            Text(
                "Preço anunciado do produto: ${price.toMoney()}. Frete e serviços somam por fora. " +
                    "Digite um valor aqui pra fechar outro preço.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        FieldHelp(
            "O total que o cliente vai pagar, se for diferente do calculado.",
            "Digite o valor que o cliente propôs e veja o que sobra. Ele passa a ser o preço de verdade do " +
                "orçamento: é o que vai pro PDF, pro histórico e pro Dashboard. Deixe vazio pra usar o preço da sua margem.",
        )
    }

    val breakEvenTotal = quote.breakEvenSalePrice + quote.extrasTotal
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

/**
 * Mesma peça calculada em cada impressora cadastrada: responde "em qual máquina sai mais barato". Com
 * preço fechado ou anunciado, a comparação usa o preço que a margem daria em cada uma (o fechado é o
 * mesmo em todas e marcaria a primeira como "mais barata" sem motivo).
 */
@Composable
private fun PrinterComparison(comparison: List<Pair<PrinterProfile, Quote>>, allPrints: Boolean) {
    fun Quote.comparable() = (tableSalePrice ?: salePrice) + extrasTotal
    val cheapest = comparison.minByOrNull { it.second.comparable() }?.first?.id

    SectionTitle(AppIcons.Printer3d, "Comparar impressoras")
    comparison.forEach { (printer, quote) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val name = if (allPrints) "Tudo na ${printer.name}" else printer.name
            Text(
                if (printer.id == cheapest) "$name (mais barata)" else name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (printer.id == cheapest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            NumericText(quote.comparable().toMoney(), style = MaterialTheme.typography.bodyMedium)
        }
    }
    FieldHelp(
        if (allPrints) {
            "Todas as impressões do pedido na mesma máquina, pelo preço que a sua margem dá em cada uma."
        } else {
            "Mesma peça, trocando só a máquina: pelo preço que a sua margem dá em cada uma."
        },
        "A diferença vem do consumo de energia, da manutenção e do retorno do investimento de cada impressora.",
    )
}

/**
 * Onde o dinheiro do preço está, em barra empilhada mais legenda. Ver que a máquina pesa mais que o
 * plástico, ou que o próprio trabalho é a maior fatia, é o que ensina a precificar. Canal e imposto
 * aparecem como fatia própria (a barra soma o preço inteiro). Fatias zeradas são omitidas.
 */
@Composable
private fun PriceCompositionBar(quote: Quote) {
    val costs = quote.costs
    val deductions = quote.salePrice * quote.totalDeductionRate
    val slices = listOf(
        CompositionSlice("Material", costs.material, MaterialTheme.colorScheme.primary),
        CompositionSlice("Energia", costs.energy, MaterialTheme.colorScheme.tertiary),
        CompositionSlice(
            "Máquina",
            costs.maintenance + costs.investmentReturn + costs.fixedCost,
            MaterialTheme.colorScheme.secondary,
        ),
        CompositionSlice("Seu trabalho", costs.labor + costs.finishing, MaterialTheme.colorScheme.tertiaryContainer),
        CompositionSlice("Reserva de falha", costs.failures, MaterialTheme.colorScheme.outline),
        CompositionSlice("Administrativo", costs.administrative, MaterialTheme.colorScheme.outlineVariant),
        CompositionSlice("Canal e imposto", deductions, MaterialTheme.colorScheme.secondaryContainer),
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
    /** A impressão, quando o pedido tem uma só: aí as configurações dela ficam aqui, como sempre ficaram. */
    singlePrint: PrintInput?,
    isProduct: Boolean,
    viewModel: QuoteViewModel,
    canSave: Boolean,
    queueHint: String?,
    cannotSaveReason: String,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val isEditing = form.editingQuoteId != null
        SectionTitle(
            AppIcons.Save,
            when {
                isEditing && isProduct -> "Editar produto"
                isEditing -> "Editar pedido"
                isProduct -> "Salvar no catálogo"
                else -> "Salvar pedido"
            },
        )
        if (isEditing) {
            Text(
                "A data de criação original é mantida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SubsectionTitle(AppIcons.Visibility, "O que o cliente vê", modifier = Modifier.padding(top = 8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = viewModel::setSaveName,
            label = { Text("Nome da peça (opcional)") },
            singleLine = true,
        )
        if (isProduct) {
            val savedQuotes by viewModel.savedQuotes.collectAsState()
            CategoryField(value = form.category, suggestions = viewModel.knownCategories(savedQuotes), onValueChange = viewModel::setCategory)
        }

        val photo = form.photo
        if (photo != null) {
            // Decodifica uma vez por foto, e não a cada tecla digitada em outro campo; uma imagem que não
            // abre vira aviso, e não derruba a tela.
            val bitmap = remember(photo) { runCatching { decodeImageBitmap(photo.bytes) }.getOrNull() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = "Foto da peça", modifier = Modifier.size(64.dp))
                } else {
                    Text("Não consegui mostrar essa imagem.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(if (form.photoFromGCode) "Miniatura do G-code" else photo.fileName, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = viewModel::clearPhoto) { Text("Remover foto") }
            }
        } else {
            OutlinedButton(onClick = viewModel::pickPhoto) { Text("Escolher foto (opcional)") }
        }
        form.photoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

        if (!isProduct) {
            Text("Prazo de entrega (opcional)", style = MaterialTheme.typography.labelLarge)
            DeliveryDatePicker(epochDay = form.deliveryDateEpochDay, onChange = viewModel::setDeliveryDate)
            queueHint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // Daqui pra baixo nada vai pro cliente. O título separa as duas metades do formulário, em
        // vez de depender de cada rótulo dizer "(uso interno)".
        SubsectionTitle(AppIcons.Lock, "Só pra você (não sai no PDF nem na mensagem)", modifier = Modifier.padding(top = 8.dp))

        if (!isProduct) {
            val clients by viewModel.clients.collectAsState()
            ClientField(
                name = form.clientName,
                suggestions = viewModel.clientSuggestions(form.clientName, clients),
                onNameChange = viewModel::setClientName,
                onChoose = viewModel::chooseClient,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().tabToNavigate(),
                value = form.clientContact,
                onValueChange = viewModel::setClientContact,
                label = { Text("Contato do cliente (opcional)") },
                placeholder = { Text("WhatsApp: (11) 99999-0000") },
                singleLine = true,
            )
        }

        StlAttachment(viewModel, form)

        // Com várias impressões, cada cartão tem as configurações da própria mesa (decisão 114).
        if (singlePrint != null) {
            var showPrintSettingsDialog by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { showPrintSettingsDialog = true }) {
                Text(if (singlePrint.settings.isEmpty) "Adicionar configurações de impressão" else "Editar configurações de impressão")
            }
            if (showPrintSettingsDialog) {
                PrintSettingsDialog(
                    initial = singlePrint.settings,
                    onDismiss = { showPrintSettingsDialog = false },
                    onSave = { viewModel.setPrintSettings(it, singlePrint.id) },
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.sourceLink,
            onValueChange = viewModel::setSourceLink,
            label = { Text("Link do modelo (opcional)") },
            singleLine = true,
        )
        if (form.sourceLink.isNotBlank()) {
            LinkText(text = "Abrir link no navegador", url = form.sourceLink)
        }

        Button(onClick = onSave, enabled = canSave) {
            Text(
                when {
                    isEditing -> "Salvar alterações"
                    isProduct -> "Salvar no catálogo"
                    else -> "Salvar pedido"
                },
            )
        }
        if (!canSave) {
            Text(cannotSaveReason, style = MaterialTheme.typography.bodySmall)
        }

        ShowSnackbarOnce(
            form.savedConfirmation,
            when {
                form.savedAsProduct -> "Produto ${form.savedNumber.orEmpty()} salvo no Catálogo.".replace("  ", " ")
                else -> "Pedido ${form.savedNumber.orEmpty()} salvo em Pedidos.".replace("  ", " ")
            },
            viewModel::consumeSavedConfirmation,
        )
    }
}

/** O STL anexado, com a prévia 3D e a análise calculadas fora do thread da tela ([QuoteViewModel.stlPreview]). */
@Composable
private fun StlAttachment(viewModel: QuoteViewModel, form: SaveQuoteFormState) {
    val stlFile = form.stlFile
    if (stlFile == null) {
        OutlinedButton(onClick = viewModel::pickStl) { Text("Anexar arquivo STL (opcional)") }
        FieldHelp("Guardado pra você baixar de novo em Pedidos ou no Catálogo e reaproveitar numa venda futura da mesma peça.")
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stlFile.fileName, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = viewModel::clearStlFile) { Text("Remover STL") }
    }
    val preview by viewModel.stlPreview.collectAsState()
    when (val current = preview) {
        null, StlPreview.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Preparando a prévia 3D…", style = MaterialTheme.typography.bodySmall)
        }
        is StlPreview.TooComplex -> Text(
            "Esse modelo é muito complexo pra pré-visualizar (~${current.triangleCount} triângulos) — " +
                "o arquivo é salvo normalmente, só sem a prévia 3D, pra não travar o app.",
            style = MaterialTheme.typography.bodySmall,
        )
        StlPreview.Unreadable -> Text(
            "Não consegui ler esse arquivo STL — pode estar corrompido ou num formato não suportado.",
            color = MaterialTheme.colorScheme.error,
        )
        is StlPreview.Ready -> {
            Text("Arraste pra girar, use a roda do mouse pra zoom.", style = MaterialTheme.typography.bodySmall)
            val viewerState = rememberStl3DViewerState(current.mesh)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Stl3DViewer(state = viewerState, mesh = current.mesh, modifier = Modifier.fillMaxWidth().height(280.dp))
            }
            val baseColor = MaterialTheme.colorScheme.primary
            OutlinedButton(onClick = {
                val snapshot = viewerState.captureSnapshot(baseColor, width = 1000, height = 1000)
                viewModel.setPhotoFromStlSnapshot(encodeImageBitmapToPng(snapshot))
            }) { Text("Capturar como foto do orçamento") }

            val analysis = current.analysis
            Text("Nível de dificuldade sugerido: ${analysis.difficulty.label}", style = MaterialTheme.typography.titleSmall)
            Text(
                "Área: ${(analysis.surfaceAreaMm2 / 100).formatOneDecimal()} cm² · " +
                    "Volume: ${(analysis.volumeMm3 / 1000).formatOneDecimal()} cm³ · " +
                    "Overhang: ${(analysis.overhangPercentage / 100).toPercentText()} · " +
                    "Peças no arquivo: ${analysis.disconnectedComponents}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!analysis.isManifold) {
                Text(
                    "Atenção: esse arquivo STL tem geometria com furos ou normais invertidas (não-manifold) " +
                        "— pode dar problema na hora de fatiar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FieldHelp(
                "Estimativas geométricas, só pra você decidir se cobra a mais pela complexidade.",
                "Nível de dificuldade e medidas não substituem o fatiador e não entram no PDF nem no copiar/colar.",
            )
        }
    }
}

/**
 * Pedido de cliente ou produto do catálogo (decisão 101), no topo do formulário (decisão 108): o que
 * não faz sentido pra produto (cliente, prazo, frete, preço negociado) some antes de a pessoa gastar
 * tempo preenchendo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindSelector(isProduct: Boolean, onSelect: (QuoteKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = !isProduct,
                onClick = { onSelect(QuoteKind.ORDER) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Pedido de cliente") }
            SegmentedButton(
                selected = isProduct,
                onClick = { onSelect(QuoteKind.PRODUCT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Produto do catálogo") }
        }
        FieldHelp(
            if (isProduct) {
                "Uma peça que você oferece, com preço, sem cliente. Quando alguém comprar, é só clicar em \"Vender\"."
            } else {
                "Um orçamento pra um cliente, que segue o andamento de Orçado até Entregue."
            },
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
    FieldHelp("Separa a lista de produtos e o catálogo em PDF em seções.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> LabeledDropdown(
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
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).tabToNavigate(),
            readOnly = true,
            value = selected?.let(displayText) ?: emptyText,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
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
