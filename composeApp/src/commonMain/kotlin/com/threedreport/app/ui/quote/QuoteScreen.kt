package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.encodeImageBitmapToPng
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.components.ShowNotice
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.components.availableOrSelected
import com.threedreport.app.ui.filaments.displayText
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.channelFeeSummary
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.app.ui.viewer.rememberStl3DViewerState
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.showcasePriceSuggestion
import kotlin.math.abs
import kotlin.math.round

/** Opção padrão do seletor de canal: venda sem intermediário e sem taxa (Pix, dinheiro, entrega em mãos). */
private const val DIRECT_SALE_LABEL = "Venda direta (sem taxa)"

/**
 * Abaixo disso a tela volta pra uma coluna só (o painel de resultado vai pro fim). Com o painel de 380 e a
 * margem de 32 de cada lado, sobram uns 500 dp pra grade de três colunas do formulário.
 */
private val TWO_COLUMN_MIN_WIDTH = 960.dp

/** Arredonda pra 1 casa decimal, separador decimal brasileiro (vírgula) — mesmo estilo de `toWeightText()`. */
private fun Double.formatOneDecimal(): String {
    val tenths = round(this * 10).toLong()
    val whole = tenths / 10
    val decimal = abs(tenths % 10)
    return if (decimal == 0L) "$whole" else "$whole,$decimal"
}

/**
 * Tela de Orçamento: à esquerda o formulário em seções numeradas, à direita o painel de resultado fixo.
 * [onSave] é o botão Salvar: num orçamento novo, salva e limpa pra o próximo; editando um salvo
 * (decisão 113), salva e volta pra onde a edição começou.
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
    val consumables by viewModel.consumables.collectAsState()
    val salesChannels by viewModel.salesChannels.collectAsState()
    val input by viewModel.input.collectAsState()
    val saveForm by viewModel.saveForm.collectAsState()

    // Uma conta por mudança de entrada ou de cadastro, e não por redesenho: a mesma que o Ctrl+S salva.
    val result = remember(allFilaments, printers, settings, services, consumables, salesChannels, input, saveForm.operation) { viewModel.currentResult() }
    val comparison = remember(allFilaments, printers, settings, services, consumables, salesChannels, input) {
        viewModel.comparePrinters(allFilaments, printers, settings, services, input, salesChannels, consumables)
    }
    val currency = LocalCurrency.current
    val space = MaterialTheme.spacing

    val isEditing = saveForm.editingQuoteId != null
    val canSave = result.quote != null && !result.missingServicePrice && result.fieldErrors.isEmpty()
    val cannotSaveReason = when {
        result.fieldErrors.isNotEmpty() -> "Corrija o campo marcado em vermelho pra poder salvar."
        result.quote != null && result.missingServicePrice -> "Informe o valor de cada serviço marcado (ou desmarque) pra poder salvar."
        result.errorMessage != null -> result.errorMessage
        else -> "Preencha filamento, impressora, peso (ou comprimento) e tempo, ou arraste o G-code, pra poder salvar."
    }
    val saveLabel = when {
        isEditing -> "Salvar alterações"
        input.isProduct -> "Salvar no catálogo"
        else -> "Salvar pedido"
    }
    val cancelLabel = operationTexts(saveForm, input.isProduct)?.cancelLabel

    @Composable
    fun Form() = QuoteForm(viewModel, allFilaments, printers, services, consumables, salesChannels, settings, input, saveForm, result, comparison, currency)

    @Composable
    fun Panel(fixed: Boolean, panelModifier: Modifier) = QuoteResultPanel(
        result = result,
        input = input,
        saveForm = saveForm,
        settings = settings,
        allFilaments = allFilaments,
        printers = printers,
        comparison = comparison,
        canSave = canSave,
        cannotSaveReason = cannotSaveReason,
        saveLabel = saveLabel,
        cancelLabel = cancelLabel,
        fixed = fixed,
        onSave = onSave,
        onCancel = onCancelOperation,
        onUseShippingCoveringPrice = viewModel::useShippingCoveringPrice,
        modifier = panelModifier,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= TWO_COLUMN_MIN_WIDTH) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl),
                ) { Form() }
                Panel(fixed = true, panelModifier = Modifier.width(AppDimens.resultPanelWidth).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.padding(start = space.xl, end = space.xl, top = AppDimens.screenTopPadding, bottom = space.xl)) { Form() }
                Panel(fixed = false, panelModifier = Modifier.fillMaxWidth())
            }
        }
    }

    saveForm.blockedMessage?.let { message ->
        ShowSnackbarOnce(true, message, viewModel::consumeBlockedMessage)
    }
    ShowSnackbarOnce(
        saveForm.savedConfirmation,
        when {
            saveForm.savedAsProduct -> "Produto ${saveForm.savedNumber.orEmpty()} salvo no Catálogo.".replace("  ", " ")
            else -> "Pedido ${saveForm.savedNumber.orEmpty()} salvo em Pedidos.".replace("  ", " ")
        },
        viewModel::consumeSavedConfirmation,
    )
    val notice by viewModel.notice.collectAsState()
    ShowNotice(notice, viewModel::consumeNotice)
}

/** O que a tela está fazendo além de um orçamento novo: título, o detalhe (numa dica) e o texto de desistir. */
private data class OperationTexts(val title: String, val detail: String, val cancelLabel: String)

private fun operationTexts(form: SaveQuoteFormState, isProduct: Boolean): OperationTexts? = when (val operation = form.operation) {
    is QuoteOperation.Editing -> OperationTexts(
        "Editando " + listOfNotNull(operation.savedQuote.displayNumber?.takeIf { operation.savedQuote.isOrder }, "\"${operation.savedQuote.name}\"").joinToString(" "),
        "Salve pra atualizar " + (if (isProduct) "o produto." else "o pedido.") +
            " A data de criação original é mantida. O que estava sendo feito no Orçamento volta quando você terminar.",
        "Cancelar edição",
    )

    is QuoteOperation.Selling -> OperationTexts(
        "Vendendo \"${operation.product.name}\"",
        "Preencha o cliente e o prazo e salve o pedido. O produto continua no catálogo, sem mudar nada.",
        "Cancelar venda",
    )

    is QuoteOperation.CopyingToCatalog -> OperationTexts(
        "Copiando \"${operation.fromName}\" pro catálogo",
        "Revise e clique em \"Salvar no catálogo\". Preço negociado, frete, cliente e prazo não vêm junto. O pedido não muda.",
        "Cancelar cópia",
    )

    is QuoteOperation.Duplicating -> OperationTexts(
        "Duplicando \"${operation.fromName}\"",
        "Revise os dados, preencha o cliente e salve pra criar " + (if (isProduct) "um produto novo." else "um pedido novo.") + " O original não muda.",
        "Cancelar duplicação",
    )

    else -> null
}

/** O formulário: cabeçalho, área de G-code e as seções numeradas. */
@Composable
private fun QuoteForm(
    viewModel: QuoteViewModel,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    services: List<Service>,
    consumables: List<Consumable>,
    salesChannels: List<SalesChannel>,
    settings: PricingSettings,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    result: QuoteResult,
    comparison: List<Pair<PrinterProfile, com.threedreport.core.model.Quote>>,
    currency: Currency,
) {
    val space = MaterialTheme.spacing
    val orphanServices = input.selectedServices.filterKeys { id -> services.none { it.id == id } }
    val offeredServices = services.availableOrSelected { it.id in input.selectedServices }
    val orphanConsumables = input.selectedConsumables.filterKeys { id -> consumables.none { it.id == id } }
    val offeredConsumables = consumables.availableOrSelected { it.id in input.selectedConsumables }
    val hasServices = offeredServices.isNotEmpty() || orphanServices.isNotEmpty()
    val hasConsumables = offeredConsumables.isNotEmpty() || orphanConsumables.isNotEmpty()
    val hasExtras = settings.laborRatePerHour > 0 || hasServices || hasConsumables
    var number = 0

    Column(modifier = Modifier.widthIn(max = AppDimens.formMaxWidth), verticalArrangement = Arrangement.spacedBy(space.xl)) {
        Header(viewModel, input, form)
        GCodeDropArea(viewModel, input)

        FormSection(++number, if (input.prints.size > 1) "Impressões" else "Impressão") {
            Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
                val quantityError = result.fieldErrors[QuoteFields.QUANTITY]
                FormGrid {
                    cell(2) {
                        AppTextField(
                            label = "Nome da peça (opcional)",
                            value = form.name,
                            onValueChange = viewModel::setSaveName,
                        )
                    }
                    cell {
                        AppTextField(
                            label = "Quantidade de peças",
                            value = input.quantityText,
                            onValueChange = viewModel::setQuantity,
                            placeholder = "1",
                            error = quantityError,
                            help = HelpText(
                                "Peso, comprimento e tempo são de UMA peça: o app multiplica pela quantidade.",
                                "Se você fatiou a mesa inteira de uma vez e os números já são do lote todo, deixe a quantidade em 1.",
                            ),
                        )
                    }
                }
                PrintsSection(viewModel, allFilaments, printers, input, result, currency)
            }
        }

        if (hasExtras) {
            FormSection(++number, "Trabalho e extras") {
                Column(verticalArrangement = Arrangement.spacedBy(space.md)) {
                    // Só aparece pra quem configurou quanto vale a própria hora: sem isso, o campo não teria
                    // efeito nenhum no preço. O tempo é do pedido inteiro (decisão 94).
                    if (settings.laborRatePerHour > 0) LaborField(viewModel, settings, input, result, currency)
                    if (hasServices) {
                        ExtrasList("Serviços", HelpText("O valor é deste pedido: marque o serviço e digite quanto vai cobrar por ele.")) {
                            // Serviço marcado num orçamento reaberto que já saiu do catálogo continua aparecendo.
                            offeredServices.forEach { service ->
                                ServiceRow(
                                    viewModel, service.id, service.name, service.suggestedPrice, input.selectedServices[service.id],
                                    input.quantity, result.fieldErrors[QuoteFields.serviceMinutes(service.id)],
                                )
                            }
                            orphanServices.forEach { (id, serviceInput) ->
                                ServiceRow(viewModel, id, serviceInput.name, null, serviceInput, input.quantity, result.fieldErrors[QuoteFields.serviceMinutes(id)])
                            }
                        }
                    }
                    if (hasConsumables) {
                        // Insumo argola, ímã, caixa: vale pra pedido e pra produto (decisão 122).
                        ExtrasList("Insumos", null) {
                            offeredConsumables.forEach { consumable ->
                                ConsumableRow(
                                    viewModel, consumable.id, consumable.name, consumable.unitCost, input.selectedConsumables[consumable.id],
                                    input.quantity, result.fieldErrors[QuoteFields.consumable(consumable.id)],
                                )
                            }
                            orphanConsumables.forEach { (id, consumableInput) ->
                                ConsumableRow(
                                    viewModel, id, "${consumableInput.name} (saiu do cadastro)", consumableInput.unitCost, consumableInput,
                                    input.quantity, result.fieldErrors[QuoteFields.consumable(id)],
                                )
                            }
                        }
                    }
                }
            }
        }

        FormSection(++number, "Venda") { SaleFields(viewModel, salesChannels, settings, input, form, result, currency) }

        FormSection(
            ++number,
            "Detalhes",
            help = HelpText("Foto e prazo saem no PDF e na mensagem. Contato, STL e link ficam só com você."),
        ) { DetailsFields(viewModel, printers, input, form, result) }
    }
}

@Composable
private fun Header(viewModel: QuoteViewModel, input: QuoteInputState, form: SaveQuoteFormState) {
    val operation = operationTexts(form, input.isProduct)
    // O tipo vem primeiro (decisão 108): ele esconde frete e negociação, então escolher depois de
    // preencher era jogar trabalho fora. Na edição, e quando o formulário veio de "Vender" ou de
    // "Guardar no catálogo", o destino já está decidido.
    val showKind = form.operation == null || form.operation is QuoteOperation.Duplicating
    val title = operation?.title ?: if (input.isProduct) "Novo produto" else "Novo orçamento"
    val help = operation?.detail ?: if (input.isProduct) {
        "Uma peça que você oferece, com preço, sem cliente. Quando alguém comprar, é só clicar em \"Vender\"."
    } else {
        "Um orçamento pra um cliente, que segue o andamento de Orçado até Entregue."
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Text(title, style = MaterialTheme.appType.headline, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f, fill = false))
            HelpHint(HelpText(help), title)
        }
        if (showKind) {
            SegmentedChoice(
                options = listOf("Pedido de cliente", "Produto do catálogo"),
                selectedIndex = if (input.isProduct) 1 else 0,
                onSelect = { viewModel.setKind(if (it == 1) QuoteKind.PRODUCT else QuoteKind.ORDER) },
            )
        }
    }
}

@Composable
private fun LaborField(viewModel: QuoteViewModel, settings: PricingSettings, input: QuoteInputState, result: QuoteResult, currency: Currency) {
    val quantity = input.quantity
    val error = result.fieldErrors[QuoteFields.LABOR]
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        FormGrid {
            cell {
                AppTextField(
                    label = "Seu trabalho",
                    value = input.laborMinutesText,
                    onValueChange = viewModel::setLaborMinutes,
                    placeholder = "1h30 ou 90 (minutos)",
                    error = error,
                    help = HelpText(
                        (if (quantity > 1) "Total das $quantity peças" else "O pedido inteiro") +
                            ", cobrado a ${settings.laborRatePerHour.toCurrencyText(currency)}/h, sem a margem por cima." +
                            (if (quantity > 1) " Mudou a quantidade? Revise o tempo." else ""),
                        "Fora o tempo de máquina: fatiar, montar a mesa, tirar da mesa, remover suporte, lixar, pintar, " +
                            "embalar. O valor da hora é ajustável em Configurações. Se o acabamento já está no percentual " +
                            "de Configurações, não conte ele aqui.",
                    ),
                )
            }
        }
        // Lembrete, não erro: sem ele, configurar a hora e não ver o preço mudar parece defeito.
        if (input.isLaborTimeMissing) {
            Text("Seu trabalho ainda não entra no preço: informe o tempo deste pedido.", style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Um grupo de linhas marcáveis (serviços, insumos) com rótulo em `label`; é lista, não cartão. */
@Composable
private fun ExtrasList(label: String, help: HelpText?, content: @Composable () -> Unit) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
            Text(label, style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (help != null) HelpHint(help, label)
        }
        content()
    }
}

@Composable
private fun CheckRow(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Recuo do conteúdo de uma linha marcada, alinhado com o texto depois da caixa. */
@Composable
private fun indentUnderCheckbox(): Modifier = Modifier.padding(start = MaterialTheme.spacing.xxl + MaterialTheme.spacing.xs)

/**
 * Um serviço na lista do orçamento. Desmarcado, só o nome (e o valor sugerido, se houver). Marcado, ganha o
 * campo de valor deste pedido e, com mais de uma peça, a escolha entre cobrar por peça ou uma vez no pedido,
 * com o total ao lado pra conta nunca virar surpresa.
 */
@Composable
private fun ServiceRow(
    viewModel: QuoteViewModel,
    id: String,
    name: String,
    suggestedPrice: Double?,
    serviceInput: ServiceInput?,
    quantity: Int,
    minutesError: String? = null,
) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        CheckRow(serviceInput != null, if (serviceInput == null && suggestedPrice != null) "$name · sugerido ${suggestedPrice.toMoney()}" else name) {
            viewModel.toggleService(id)
        }
        if (serviceInput == null) return@Column

        val price = parseDecimal(serviceInput.priceText)?.takeIf { it >= 0 }
        Column(modifier = indentUnderCheckbox(), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            FormGrid {
                cell {
                    AppTextField(
                        label = if (quantity > 1 && !serviceInput.chargedPerOrder) "Valor por peça" else "Valor",
                        value = serviceInput.priceText,
                        onValueChange = { viewModel.setServicePrice(id, it) },
                        error = if (price == null) (if (serviceInput.priceText.isBlank()) "Informe o valor" else "Não é um número") else null,
                    )
                }
                // Opcional (decisão 123): sem tempo informado, o serviço continua sendo repasse, fora do lucro.
                cell {
                    AppTextField(
                        label = "Tempo (min)",
                        value = serviceInput.laborMinutesText,
                        onValueChange = { viewModel.setServiceLaborMinutes(id, it) },
                        placeholder = "30",
                        error = minutesError,
                    )
                }
            }
            if (quantity > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                    ServiceChargeSelector(
                        chargedPerOrder = serviceInput.chargedPerOrder,
                        onChange = { viewModel.setServiceChargedPerOrder(id, it) },
                    )
                    if (price != null) {
                        val total = if (serviceInput.chargedPerOrder) price else price * quantity
                        NumericText("= ${total.toMoney()}", style = MaterialTheme.appType.body)
                    }
                }
            }
        }
    }
}

/**
 * Um insumo na lista do orçamento (decisão 122): argola, ímã, caixa. Desmarcado, só o nome. Marcado, ganha a
 * quantidade (por peça ou pelo pedido, com mais de uma peça) e o custo unitário do cadastro, só de referência.
 */
@Composable
private fun ConsumableRow(
    viewModel: QuoteViewModel,
    id: String,
    name: String,
    unitCost: Double,
    consumableInput: ConsumableInput?,
    orderQuantity: Int,
    quantityError: String?,
) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        CheckRow(consumableInput != null, name) { viewModel.toggleConsumable(id) }
        if (consumableInput == null) return@Column

        Column(modifier = indentUnderCheckbox(), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            FormGrid {
                cell {
                    AppTextField(
                        label = "Qtd. (${unitCost.toMoney()} cada)",
                        value = consumableInput.quantityText,
                        onValueChange = { viewModel.setConsumableQuantity(id, it) },
                        placeholder = "1",
                        error = quantityError,
                    )
                }
            }
            if (orderQuantity > 1) {
                ServiceChargeSelector(
                    chargedPerOrder = consumableInput.chargedPerOrder,
                    onChange = { viewModel.setConsumableChargedPerOrder(id, it) },
                )
            }
        }
    }
}

/** Canal, frete, cliente, preço fechado (ou anunciado) e as opções de frete grátis e urgência. */
@Composable
private fun SaleFields(
    viewModel: QuoteViewModel,
    salesChannels: List<SalesChannel>,
    settings: PricingSettings,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    result: QuoteResult,
    currency: Currency,
) {
    val space = MaterialTheme.spacing
    val quote = result.quote
    // Com o canal do orçamento excluído, o seletor aparece mesmo sem canais cadastrados: é nele que se
    // escolhe "Venda direta" pra liberar o cálculo.
    val offeredChannels = salesChannels.availableOrSelected { it.id == input.salesChannelId }
    val clients by viewModel.clients.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        FormGrid {
            if (offeredChannels.isNotEmpty() || input.missingChannelName != null) {
                cell {
                    AppDropdownField(
                        label = "Canal de venda",
                        items = listOf(null) + offeredChannels,
                        selected = result.salesChannel,
                        itemLabel = { it?.let { channel -> "${channel.name} · ${channelFeeSummary(channel, currency)}" } ?: DIRECT_SALE_LABEL },
                        displayText = { it?.name ?: DIRECT_SALE_LABEL },
                        onSelect = { viewModel.selectSalesChannel(it?.id) },
                        emptyText = input.missingChannelName?.let { "$it (excluído)" } ?: DIRECT_SALE_LABEL,
                        help = HelpText(
                            "A taxa do canal sai do que você recebe; o preço sobe o suficiente pra sua margem não mudar.",
                            "A taxa incide sobre tudo o que o cliente paga, inclusive serviços e frete, como a maquininha e o " +
                                "marketplace cobram de verdade. Cadastre os canais em Configurações.",
                        ),
                    )
                }
            }
            // Produto do catálogo não tem frete (decisão 101): quem paga e pra onde vai só existe na venda.
            if (!input.isProduct) {
                cell {
                    AppTextField(
                        label = "Frete (${currency.symbol}, opcional)",
                        value = input.shippingCostText,
                        onValueChange = viewModel::setShippingCost,
                        error = result.fieldErrors[QuoteFields.SHIPPING],
                        help = HelpText(
                            "Linha própria no total, nunca embutido no preço da peça.",
                            "Frete é repasse, não produto seu: não multiplica pela quantidade nem entra na margem. A taxa do " +
                                "canal e o imposto incidem sobre ele, porque o cliente paga tudo junto.",
                        ),
                    )
                }
                cell {
                    AppSuggestField(
                        label = "Cliente (opcional)",
                        value = form.clientName,
                        onValueChange = viewModel::setClientName,
                        suggestions = viewModel.clientSuggestions(form.clientName, clients),
                        suggestionLabel = { client -> client.name + (client.contact?.let { " · $it" } ?: "") },
                        onChoose = viewModel::chooseClient,
                        help = HelpText("Só pra você: não sai no PDF nem na mensagem."),
                    )
                }
                cell {
                    AppTextField(
                        label = "Contato do cliente (opcional)",
                        value = form.clientContact,
                        onValueChange = viewModel::setClientContact,
                        placeholder = "WhatsApp: (11) 99999-0000",
                        help = HelpText("Só pra você: não sai no PDF nem na mensagem."),
                    )
                }
            }
            // Produto do catálogo não tem cliente pra negociar (decisão 101), mas tem o preço que se anuncia
            // (decisão 102): o mesmo preço fechado, com outro nome.
            cell {
                AppTextField(
                    label = if (input.isProduct) "Preço anunciado (opcional)" else "Preço fechado (opcional)",
                    value = input.targetTotalText,
                    onValueChange = viewModel::setTargetTotal,
                    error = result.fieldErrors[QuoteFields.TARGET],
                    help = if (input.isProduct) {
                        HelpText(
                            "Um valor redondo pra vitrine, no lugar do que a margem deu.",
                            "É ele que sai no catálogo, no PDF e na imagem, e é o preço que o pedido recebe quando você clica em " +
                                "\"Vender\". Deixe vazio pra anunciar o preço calculado.",
                        )
                    } else {
                        HelpText(
                            "O total que o cliente vai pagar, se for diferente do calculado.",
                            "Digite o valor que o cliente propôs e veja o que sobra. Ele passa a ser o preço de verdade do " +
                                "orçamento: é o que vai pro PDF, pro histórico e pro Dashboard. Deixe vazio pra usar o preço da sua margem.",
                        )
                    },
                )
            }
        }

        input.missingChannelName?.let { name ->
            Text(
                "O canal \"$name\" deste orçamento não existe mais no cadastro. O preço salvo continua valendo; " +
                    "pra recalcular, escolha outro canal ou \"Venda direta\" acima.",
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (quote != null) {
            // Só com o campo vazio: depois de aplicado, ou com um anunciado próprio, o atalho sobrava.
            val suggestion = if (input.isProduct && input.targetTotalText.isBlank()) {
                showcasePriceSuggestion((quote.tableSalePrice ?: quote.salePrice) + quote.extrasTotal)
            } else {
                null
            }
            suggestion?.let { AppButton("Arredondar pra ${it.toMoney()}", onClick = { viewModel.applyShowcasePrice(it) }) }
            input.announcedUnitPrice?.takeIf { input.targetTotalText.isBlank() && !input.isProduct }?.let { unit ->
                Text(
                    "Preço anunciado do produto: ${(unit * input.quantity).toMoney()}. Frete e serviços somam por fora. " +
                        "Digite um valor em Preço fechado pra fechar outro preço.",
                    style = MaterialTheme.appType.caption,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (!input.isProduct) {
            // Só aparece com um frete digitado (decisão 124): marcado sem valor não faria nada.
            val shippingAmount = parseDecimal(input.shippingCostText) ?: 0.0
            if (shippingAmount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
                    CheckRow(input.shippingAbsorbed, "Frete grátis pro cliente (você paga)", viewModel::setShippingAbsorbed)
                    HelpHint(HelpText("O cliente vê \"Frete grátis\" e o valor sai do seu lucro."), "Frete grátis")
                }
            }
            // Urgência (decisão 125): só existe com o acréscimo configurado, senão a opção não teria efeito.
            if (settings.rushSurchargeRate > 0) {
                CheckRow(input.rush, "Pedido urgente (+${settings.rushSurchargeRate.toPercentText()})", viewModel::setRush)
                val deadline = form.deliveryDateEpochDay
                if (!input.rush && deadline != null && deadline - todayEpochDay() in 0..2) {
                    AppButton("Prazo curto: marcar como urgente?", onClick = { viewModel.setRush(true) })
                }
            }
        }
    }
}

/** Prazo, categoria, link, foto, STL e configurações de impressão: o que acompanha o pedido ao salvar. */
@Composable
private fun DetailsFields(
    viewModel: QuoteViewModel,
    printers: List<PrinterProfile>,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    result: QuoteResult,
) {
    val space = MaterialTheme.spacing
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val quote = result.quote
    // Uma linha por impressora que o pedido usa: cada uma tem a própria fila.
    val queueHint = if (quote != null && !input.isProduct) {
        quote.printerIds.mapNotNull { printerId ->
            val printer = printers.firstOrNull { it.id == printerId } ?: return@mapNotNull null
            viewModel.queueAheadOf(printer, savedQuotes, form.editingQuoteId)?.let { queue ->
                val orders = if (queue.queuedQuoteCount == 1) "1 pedido aprovado ou imprimindo" else "${queue.queuedQuoteCount} pedidos aprovados ou imprimindo"
                "Fila da ${printer.name}: ${queue.queuedMinutes.minutesToDurationText()} de impressão em $orders · " +
                    "este pedido: ${quote.printMinutesOn(printerId).minutesToDurationText()}."
            }
        }.joinToString("\n").ifEmpty { null }
    } else {
        null
    }

    Column(verticalArrangement = Arrangement.spacedBy(space.md)) {
        if (!input.isProduct) {
            Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
                Text("Prazo de entrega (opcional)", style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DeliveryDatePicker(epochDay = form.deliveryDateEpochDay, onChange = viewModel::setDeliveryDate)
                queueHint?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        FormGrid {
            if (input.isProduct) {
                cell {
                    CategoryField(form.category, viewModel.knownCategories(savedQuotes), viewModel::setCategory)
                }
            }
            cell(2) {
                AppTextField(
                    label = "Link do modelo (opcional)",
                    value = form.sourceLink,
                    onValueChange = viewModel::setSourceLink,
                )
            }
        }
        if (form.sourceLink.isNotBlank()) LinkText(text = "Abrir link no navegador", url = form.sourceLink)

        PhotoRow(viewModel, form)
        StlAttachment(viewModel, form)

        // Com várias impressões, cada bloco tem as configurações da própria mesa (decisão 114).
        val singlePrint = input.prints.singleOrNull()
        if (singlePrint != null) {
            var showDialog by remember { mutableStateOf(false) }
            AppButton(
                if (singlePrint.settings.isEmpty) "Adicionar configurações de impressão" else "Editar configurações de impressão",
                onClick = { showDialog = true },
            )
            if (showDialog) {
                PrintSettingsDialog(
                    initial = singlePrint.settings,
                    onDismiss = { showDialog = false },
                    onSave = { viewModel.setPrintSettings(it, singlePrint.id) },
                )
            }
        }
    }
}

@Composable
private fun PhotoRow(viewModel: QuoteViewModel, form: SaveQuoteFormState) {
    val space = MaterialTheme.spacing
    val photo = form.photo
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        if (photo != null) {
            // Decodifica uma vez por foto, e não a cada tecla digitada em outro campo; uma imagem que não
            // abre vira aviso, e não derruba a tela.
            val bitmap = remember(photo) { runCatching { decodeImageBitmap(photo.bytes) }.getOrNull() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Foto da peça",
                        modifier = Modifier.size(AppDimens.thumbnailSize).clip(MaterialTheme.shapes.small),
                    )
                } else {
                    Text("Não consegui mostrar essa imagem.", style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.error)
                }
                Text(if (form.photoFromGCode) "Miniatura do G-code" else photo.fileName, style = MaterialTheme.appType.body)
                AppButton("Remover foto", onClick = viewModel::clearPhoto)
            }
        } else {
            AppButton("Escolher foto (opcional)", onClick = viewModel::pickPhoto, icon = AppIcons.AddPhotoAlternate)
        }
        form.photoError?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.error) }
    }
}

/** O STL anexado, com a prévia 3D e a análise calculadas fora do thread da tela ([QuoteViewModel.stlPreview]). */
@Composable
private fun StlAttachment(viewModel: QuoteViewModel, form: SaveQuoteFormState) {
    val space = MaterialTheme.spacing
    val stlFile = form.stlFile
    if (stlFile == null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
            AppButton("Anexar arquivo STL (opcional)", onClick = viewModel::pickStl)
            HelpHint(
                HelpText("Guardado pra você baixar de novo em Pedidos ou no Catálogo e reaproveitar numa venda futura da mesma peça."),
                "Arquivo STL",
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            Text(stlFile.fileName, style = MaterialTheme.appType.body)
            AppButton("Remover STL", onClick = viewModel::clearStlFile)
        }
        val preview by viewModel.stlPreview.collectAsState()
        when (val current = preview) {
            null, StlPreview.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                CircularProgressIndicator(modifier = Modifier.size(AppDimens.iconSize), strokeWidth = AppDimens.focusRingWidth)
                Text("Preparando a prévia 3D…", style = MaterialTheme.appType.caption)
            }

            is StlPreview.TooComplex -> Text(
                "Esse modelo é muito complexo pra pré-visualizar (~${current.triangleCount} triângulos). " +
                    "O arquivo é salvo normalmente, só sem a prévia 3D, pra não travar o app.",
                style = MaterialTheme.appType.caption,
            )

            StlPreview.Unreadable -> Text(
                "Não consegui ler esse arquivo STL: pode estar corrompido ou num formato não suportado.",
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.error,
            )

            is StlPreview.Ready -> {
                Text("Arraste pra girar, use a roda do mouse pra zoom.", style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val viewerState = rememberStl3DViewerState(current.mesh)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium),
                ) {
                    Stl3DViewer(state = viewerState, mesh = current.mesh, modifier = Modifier.fillMaxWidth().height(AppDimens.stlViewerHeight))
                }
                val baseColor = MaterialTheme.colorScheme.primary
                AppButton("Capturar como foto do orçamento", onClick = {
                    val snapshot = viewerState.captureSnapshot(baseColor, width = 1000, height = 1000)
                    viewModel.setPhotoFromStlSnapshot(encodeImageBitmapToPng(snapshot))
                })

                val analysis = current.analysis
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
                    Text("Nível de dificuldade sugerido: ${analysis.difficulty.label}", style = MaterialTheme.appType.bodyStrong)
                    HelpHint(
                        HelpText(
                            "Estimativas geométricas, só pra você decidir se cobra a mais pela complexidade.",
                            "Nível de dificuldade e medidas não substituem o fatiador e não entram no PDF nem no copiar/colar.",
                        ),
                        "Análise do STL",
                    )
                }
                Text(
                    "Área: ${(analysis.surfaceAreaMm2 / 100).formatOneDecimal()} cm² · " +
                        "Volume: ${(analysis.volumeMm3 / 1000).formatOneDecimal()} cm³ · " +
                        "Overhang: ${(analysis.overhangPercentage / 100).toPercentText()} · " +
                        "Peças no arquivo: ${analysis.disconnectedComponents}",
                    style = MaterialTheme.appType.caption,
                )
                if (!analysis.isManifold) {
                    Text(
                        "Atenção: esse arquivo STL tem geometria com furos ou normais invertidas (não-manifold). " +
                            "Pode dar problema na hora de fatiar.",
                        style = MaterialTheme.appType.caption,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * Categoria do produto (decisão 102): texto livre com as já usadas sugeridas embaixo, filtradas pelo
 * que se digita. Sem tela de cadastro: a lista de categorias é o que já foi digitado antes.
 */
@Composable
private fun CategoryField(value: String, suggestions: List<String>, onValueChange: (String) -> Unit) {
    val matches = suggestions.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }
    AppSuggestField(
        label = "Categoria (opcional)",
        value = value,
        onValueChange = onValueChange,
        suggestions = matches,
        suggestionLabel = { it },
        onChoose = onValueChange,
        placeholder = "Ex.: Chaveiros, Decoração",
        help = HelpText("Separa a lista de produtos e o catálogo em PDF em seções."),
    )
}
