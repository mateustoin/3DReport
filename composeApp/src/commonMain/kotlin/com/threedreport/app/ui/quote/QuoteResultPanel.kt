package com.threedreport.app.ui.quote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.weekdayName
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.filaments.displayText
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PriceComposition
import com.threedreport.core.pricing.priceComposition
import kotlin.math.abs

/** O que o painel precisa pra um estado sem resultado: o texto e se é erro. */
internal data class NoResultMessage(val text: String, val isError: Boolean)

/** Por que não há preço ainda (campo inválido, cadastro faltando, dados por preencher). */
internal fun noResultMessage(
    result: QuoteResult,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    input: QuoteInputState,
): NoResultMessage = when {
    result.errorMessage != null -> NoResultMessage(result.errorMessage, true)

    result.fieldErrors.isNotEmpty() -> NoResultMessage("Corrija o campo marcado pra calcular: ${result.fieldErrors.values.first()}", true)

    allFilaments.none { it.hasStockAvailable } && allFilaments.isNotEmpty() ->
        NoResultMessage("Todos os filamentos cadastrados estão marcados como esgotados. Marque algum como \"Em estoque\" em Filamentos.", false)

    allFilaments.isEmpty() -> NoResultMessage("Cadastre um filamento em Filamentos.", false)

    printers.isEmpty() -> NoResultMessage("Cadastre uma impressora em Impressoras.", false)

    // Tudo arquivado (decisão 115): não há o que escolher, e pedir peso e tempo não ajudaria.
    allFilaments.none { it.hasStockAvailable && !it.archived } ->
        NoResultMessage("Todos os filamentos em estoque estão arquivados. Restaure um em Filamentos, na seção Arquivados.", false)

    printers.all { it.archived } -> NoResultMessage("Todas as impressoras estão arquivadas. Restaure uma em Impressoras, na seção Arquivados.", false)

    input.prints.size > 1 -> NoResultMessage("Preencha peso (ou comprimento) e tempo de cada impressão pra calcular, ou arraste os G-codes.", false)

    else -> NoResultMessage("Preencha peso (ou comprimento) e tempo pra calcular, ou arraste o G-code.", false)
}

/**
 * Painel de resultado: coluna em azul-marinho com o preço grande, o lucro e o mínimo sem prejuízo, a barra de
 * composição e, no pé, as ações. A conta item a item e a comparação de impressoras ficam recolhidas, sob demanda.
 *
 * @param fixed `true` na tela larga (coluna de altura fixa, o miolo rola e o pé fica); `false` na estreita,
 * onde o painel vai no fim da página.
 */
@Composable
internal fun QuoteResultPanel(
    result: QuoteResult,
    input: QuoteInputState,
    saveForm: SaveQuoteFormState,
    settings: PricingSettings,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    comparison: List<Pair<PrinterProfile, Quote>>,
    canSave: Boolean,
    cannotSaveReason: String,
    saveLabel: String,
    cancelLabel: String?,
    fixed: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onUseShippingCoveringPrice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val quote = result.quote
    Column(
        modifier = modifier
            .background(colors.navy)
            .padding(space.xl),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        val bodyModifier = if (fixed) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier
        Column(modifier = bodyModifier, verticalArrangement = Arrangement.spacedBy(space.lg)) {
            if (quote != null) {
                PriceHeader(quote, result, input, saveForm)
                StatBlocks(quote)
                NegotiationNotes(quote, settings.minimumOrderPrice)
                CompositionBar(quote.priceComposition(result.chargedShipping))
                Expander("Ver a conta completa") {
                    FullAccount(quote, result, settings, onUseShippingCoveringPrice)
                }
                if (comparison.size > 1) {
                    Expander("Comparar impressoras") { PrinterComparison(comparison, allPrints = input.prints.size > 1) }
                }
            } else {
                val message = noResultMessage(result, allFilaments, printers, input)
                Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    Text("VOCÊ COBRA", style = MaterialTheme.appType.overline, color = colors.onNavyMuted)
                    Text("—", style = MaterialTheme.appType.displayPrice, color = colors.onNavyMuted)
                    Text(
                        message.text,
                        style = MaterialTheme.appType.caption,
                        color = if (message.isError) colors.onNavyDanger else colors.onNavyMuted,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
            if (!canSave) Text(cannotSaveReason, style = MaterialTheme.appType.caption, color = colors.onNavyMuted)
            AppButton(
                saveLabel,
                onClick = onSave,
                kind = ButtonKind.Primary,
                enabled = canSave,
                height = AppDimens.primaryActionHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            if (cancelLabel != null) {
                AppButton(cancelLabel, onClick = onCancel, kind = ButtonKind.OnNavy, icon = AppIcons.Close, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PriceHeader(quote: Quote, result: QuoteResult, input: QuoteInputState, saveForm: SaveQuoteFormState) {
    val colors = MaterialTheme.appColors
    val grandTotal = result.grandTotal ?: quote.salePrice
    val caption = MaterialTheme.appType.caption
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Text("VOCÊ COBRA", style = MaterialTheme.appType.overline, color = colors.onNavyMuted)
        BasicText(
            grandTotal.toMoney(),
            style = MaterialTheme.appType.displayPrice.copy(color = Color.White),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 52.sp, stepSize = 2.sp),
        )
        fun line(text: String, color: Color = colors.onNavyMuted, weight: FontWeight? = null) = text to (color to weight)
        val lines = buildList {
            if (quote.quantity > 1) {
                // A mesma conta do PDF e da mensagem: o preço de cada peça, com serviços e frete à parte.
                val extras = when {
                    result.selectedServices.isNotEmpty() && result.chargedShipping > 0 -> " + serviços e frete"
                    result.selectedServices.isNotEmpty() -> " + serviços"
                    result.chargedShipping > 0 -> " + frete"
                    else -> ""
                }
                add(line("${quote.quantity} unidades · ${quote.unitSalePrice.toMoney()} cada$extras"))
            }
            // Quanto sai de cada carretel, que é o que se confere no estoque.
            add(line("Consumo: " + quote.filamentTotals().joinToString(" · ") { it.displayText() }))
            if (quote.prints.size > 1) {
                add(line("${quote.prints.size} impressões · ${quote.totalPrintTimeMinutes.minutesToDurationText()} de máquina"))
            }
            // O mesmo prazo que sai no PDF, pra ver na nota o que o cliente vai ler.
            saveForm.deliveryDateEpochDay.takeUnless { input.isProduct }?.let {
                val overdue = it < todayEpochDay()
                add(
                    line(
                        "Entrega até ${formatDate(it)} (${weekdayName(it)})" + if (overdue) ", data já passou" else "",
                        if (overdue) colors.onNavyDanger else colors.onNavy,
                        FontWeight.Medium,
                    ),
                )
            }
            if (quote.minimumPriceApplied) add(line("Preço mínimo do pedido aplicado (a conta dava ${quote.priceBeforeMinimum!!.toMoney()})"))
            // Reabrindo um orçamento (decisão 108): o preço salvo fica até mexerem no que muda o preço.
            if (result.keepsOriginalPrice) {
                val today = result.todaysQuote
                add(
                    line(
                        "Preço do orçamento salvo, mantido." + if (today != null && abs(today.customerTotal - quote.customerTotal) >= 0.005) {
                            " Com os custos de hoje, sairia ${today.customerTotal.toMoney()}: mude qualquer valor da peça pra recalcular."
                        } else {
                            ""
                        },
                    ),
                )
            }
            val original = result.originalQuote
            if (original != null && abs(original.customerTotal - quote.customerTotal) >= 0.005) {
                add(line("Recalculado com os custos de hoje: era ${original.customerTotal.toMoney()} quando foi salvo.", colors.onNavy))
            }
        }
        lines.forEach { (text, style) -> Text(text, style = caption, color = style.first, fontWeight = style.second) }
    }
}

@Composable
private fun StatBlocks(quote: Quote) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val breakEven = quote.breakEvenSalePrice + quote.extrasTotal
    Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
        StatBlock("Seu lucro", quote.profit.toMoney(), if (quote.profit < 0) colors.onNavyDanger else colors.amber, Modifier.weight(1f))
        StatBlock("Mínimo sem prejuízo", breakEven.toMoney(), Color.White, Modifier.weight(1f))
    }
}

@Composable
private fun StatBlock(label: String, value: String, valueColor: Color, modifier: Modifier) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(
        modifier = modifier.background(colors.navyRaised, MaterialTheme.shapes.medium).padding(horizontal = space.md, vertical = space.sm),
    ) {
        Text(label, style = MaterialTheme.appType.label, color = colors.onNavyMuted, maxLines = 1)
        Text(value, style = MaterialTheme.appType.statValue, color = valueColor, maxLines = 1)
    }
}

/** Avisos de negociação: prejuízo e preço abaixo do mínimo do pedido (aviso, não bloqueio: decisão 125). */
@Composable
private fun NegotiationNotes(quote: Quote, minimumOrderPrice: Double) {
    val colors = MaterialTheme.appColors
    if (quote.profit < 0) {
        Text(
            "Prejuízo: nesse valor você paga ${(-quote.profit).toMoney()} pra imprimir.",
            style = MaterialTheme.appType.caption,
            color = colors.onNavyDanger,
        )
    }
    if (minimumOrderPrice > 0 && quote.isNegotiated && quote.salePrice < minimumOrderPrice) {
        Text("Abaixo do seu preço mínimo do pedido (${minimumOrderPrice.toMoney()}).", style = MaterialTheme.appType.caption, color = colors.amber)
    }
}

/**
 * Onde o dinheiro do preço está: barra empilhada em quatro grupos mais legenda. O detalhamento item a item
 * fica na conta completa. Os grupos somam o que o cliente paga.
 */
@Composable
private fun CompositionBar(composition: PriceComposition) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val palette = colors.composition
    val groups = listOf(
        Triple("Material e máquina", composition.materialAndMachine, palette.materialAndMachine),
        Triple("Trabalho e serviços", composition.laborAndServices, palette.laborAndServices),
        Triple("Taxas e impostos", composition.feesAndTaxes, palette.feesAndTaxes),
        Triple("Lucro", composition.profit, palette.profit),
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.compositionBarHeight)
                .clip(CircleShape)
                .semantics { contentDescription = "Composição do preço" },
            horizontalArrangement = Arrangement.spacedBy(AppDimens.compositionGap),
        ) {
            groups.filter { it.second > 0.0 }.forEach { (_, value, color) ->
                Box(Modifier.weight(value.toFloat()).fillMaxHeight().background(color))
            }
        }
        Column(modifier = Modifier.padding(top = space.sm)) {
            groups.filter { it.second > 0.005 || it.first == "Lucro" }.forEach { (label, value, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = space.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                        Box(Modifier.size(AppDimens.legendSwatchSize).background(color, MaterialTheme.shapes.extraSmall))
                        Text(label, style = MaterialTheme.appType.body, color = colors.onNavyMuted)
                    }
                    NumericText(value.toMoney(), style = MaterialTheme.appType.body, color = colors.onNavy)
                }
            }
        }
    }
}

/** Recolhe um bloco sob um título clicável com seta: o que é detalhe fica sob demanda. */
@Composable
private fun Expander(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { open = !open }
                .padding(vertical = space.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.xs),
        ) {
            Text(title, style = MaterialTheme.appType.body, fontWeight = FontWeight.Medium, color = colors.onNavy)
            Icon(
                if (open) AppIcons.ExpandLess else AppIcons.ExpandMore,
                contentDescription = null,
                tint = colors.onNavyMuted,
                modifier = Modifier.size(AppDimens.iconSize),
            )
        }
        if (open) content()
    }
}

@Composable
private fun AccountLine(label: String, value: String, muted: Boolean = false) {
    val colors = MaterialTheme.appColors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.appType.caption, color = colors.onNavyMuted, modifier = Modifier.weight(1f))
        NumericText(value, style = MaterialTheme.appType.caption, color = if (muted) colors.onNavyMuted else colors.onNavy)
    }
}

/** A conta item a item: tudo o que a nota sempre mostrou, agora sob demanda. */
@Composable
private fun FullAccount(quote: Quote, result: QuoteResult, settings: PricingSettings, onUseShippingCoveringPrice: () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        // O que o canal cobra aparece sempre que ele cobra algo: percentual, valor fixo por item (decisão 121) ou os dois.
        val channelCharges = quote.channelName != null && (quote.channelFeeRate > 0.0 || quote.channelFixedFee > 0.0)
        if (channelCharges || quote.taxRate > 0.0) {
            val parts = buildList {
                if (channelCharges) {
                    val perItem = (quote.channelFixedFee / quote.quantity).toMoney()
                    add(
                        when {
                            quote.channelFixedFee <= 0.0 -> "${quote.channelName} ${quote.channelFeeRate.toPercentText()}"
                            quote.channelFeeRate <= 0.0 -> "${quote.channelName} ($perItem por item)"
                            else -> "${quote.channelName} (${quote.channelFeeRate.toPercentText()} + $perItem por item)"
                        },
                    )
                }
                if (quote.taxRate > 0.0) add("imposto ${quote.taxRate.toPercentText()}")
            }
            Text(
                "Preço já elevado pra absorver ${parts.joinToString(" e ")}: o cliente paga esse valor normalmente, " +
                    "e o lucro já é o que sobra pra você.",
                style = MaterialTheme.appType.caption,
                color = colors.onNavyMuted,
            )
        }
        if (channelCharges) AccountLine("${quote.channelName} leva", quote.channelFeeAmount.toMoney())
        AccountLine("Custo de produção", quote.productionCost.toMoney())
        if (quote.prints.size > 1) {
            quote.prints.forEachIndexed { index, print ->
                AccountLine("   ${printTitle(index + 1, print.job.name.orEmpty())} · ${print.printerName}", print.cost.total.toMoney(), muted = true)
            }
            val orderLevel = quote.productionCost - quote.prints.sumOf { it.cost.total }
            AccountLine("   Do pedido (trabalho, falhas, administrativo)", orderLevel.toMoney(), muted = true)
        }
        if (quote.costs.consumables > 0.0) AccountLine("Insumos", quote.costs.consumables.toMoney())
        AccountLine("Lucro", quote.profit.toMoney())
        AccountLine("Margem obtida neste preço", quote.actualProfitMargin.toPercentText())
        // Só o valor: o percentual configurado já aparece no "Pedido urgente (+X%)".
        if (quote.rush) AccountLine("Urgência", quote.rushSurcharge.toMoney())
        result.selectedServices.forEach { service ->
            val multiplied = quote.quantity > 1 && !service.chargedPerOrder
            AccountLine(if (multiplied) "${service.name} (× ${quote.quantity})" else service.name, service.total(quote.quantity).toMoney())
            if (service.laborMinutes > 0) {
                val totalMinutes = service.totalLaborMinutes(quote.quantity)
                val hourly = service.total(quote.quantity) / (totalMinutes / 60.0)
                Text("· ${totalMinutes.minutesToDurationText()} · rende ${hourly.toMoney()}/h", style = MaterialTheme.appType.caption, color = colors.onNavyMuted)
                if (settings.laborRatePerHour > 0.0 && hourly < settings.laborRatePerHour) {
                    Text("paga menos que a sua hora (${settings.laborRatePerHour.toMoney()}/h)", style = MaterialTheme.appType.caption, color = colors.onNavyDanger)
                }
            }
        }
        if (result.chargedShipping > 0) AccountLine("Frete", result.chargedShipping.toMoney())
        if (result.shippingAbsorbed && result.shippingCost > 0) {
            AccountLine("Frete grátis (você paga)", "− ${result.shippingCost.toMoney()}")
            Text("Sai do seu lucro.", style = MaterialTheme.appType.caption, color = colors.onNavyMuted)
            val covering = quote.shippingCoveringSalePrice
            if (covering != null && !quote.isNegotiated) {
                Text("Pra manter a margem, cobre ${(covering + result.servicesTotal).toMoney()}", style = MaterialTheme.appType.caption, color = colors.onNavy)
                AppButton("Usar esse preço", onClick = onUseShippingCoveringPrice, kind = ButtonKind.OnNavy)
            }
        }
    }
}

/**
 * Mesma peça calculada em cada impressora cadastrada: responde "em qual máquina sai mais barato". Com
 * preço fechado ou anunciado, a comparação usa o preço que a margem daria em cada uma.
 */
@Composable
private fun PrinterComparison(comparison: List<Pair<PrinterProfile, Quote>>, allPrints: Boolean) {
    val colors = MaterialTheme.appColors
    fun Quote.comparable() = (tableSalePrice ?: salePrice) + extrasTotal
    val cheapest = comparison.minByOrNull { it.second.comparable() }?.first?.id
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        comparison.forEach { (printer, quote) ->
            val name = if (allPrints) "Tudo na ${printer.name}" else printer.name
            val best = printer.id == cheapest
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (best) "$name (mais barata)" else name,
                    style = MaterialTheme.appType.caption,
                    fontWeight = if (best) FontWeight.SemiBold else null,
                    color = if (best) colors.onNavy else colors.onNavyMuted,
                    modifier = Modifier.weight(1f),
                )
                NumericText(quote.comparable().toMoney(), style = MaterialTheme.appType.caption, color = colors.onNavy)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (allPrints) "Todas as impressões na mesma máquina, pelo preço que a sua margem dá." else "Mesma peça, trocando só a máquina.",
                style = MaterialTheme.appType.caption,
                color = colors.onNavyMuted,
                modifier = Modifier.weight(1f, fill = false),
            )
            HelpHint(
                HelpText("A diferença vem do consumo de energia, da manutenção e do retorno do investimento de cada impressora."),
                label = "Comparar impressoras",
                tint = colors.onNavyMuted,
                modifier = Modifier.padding(start = MaterialTheme.spacing.xxs),
            )
        }
    }
}
