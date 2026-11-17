package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_account_absorb
import _3dreport.composeapp.generated.resources.quote_account_and
import _3dreport.composeapp.generated.resources.quote_account_channel_both
import _3dreport.composeapp.generated.resources.quote_account_channel_fixed
import _3dreport.composeapp.generated.resources.quote_account_channel_rate
import _3dreport.composeapp.generated.resources.quote_account_channel_takes
import _3dreport.composeapp.generated.resources.quote_account_consumables
import _3dreport.composeapp.generated.resources.quote_account_free_shipping
import _3dreport.composeapp.generated.resources.quote_account_free_shipping_note
import _3dreport.composeapp.generated.resources.quote_account_keep_margin
import _3dreport.composeapp.generated.resources.quote_account_margin
import _3dreport.composeapp.generated.resources.quote_account_minus
import _3dreport.composeapp.generated.resources.quote_account_order_level
import _3dreport.composeapp.generated.resources.quote_account_print_line
import _3dreport.composeapp.generated.resources.quote_account_production
import _3dreport.composeapp.generated.resources.quote_account_profit
import _3dreport.composeapp.generated.resources.quote_account_rush
import _3dreport.composeapp.generated.resources.quote_account_service_below_rate
import _3dreport.composeapp.generated.resources.quote_account_service_times
import _3dreport.composeapp.generated.resources.quote_account_service_yield
import _3dreport.composeapp.generated.resources.quote_account_shipping
import _3dreport.composeapp.generated.resources.quote_account_tax
import _3dreport.composeapp.generated.resources.quote_account_use_price
import _3dreport.composeapp.generated.resources.quote_compare_all_on
import _3dreport.composeapp.generated.resources.quote_compare_caption_all
import _3dreport.composeapp.generated.resources.quote_compare_caption_one
import _3dreport.composeapp.generated.resources.quote_compare_cheapest
import _3dreport.composeapp.generated.resources.quote_compare_title
import _3dreport.composeapp.generated.resources.quote_composition_description
import _3dreport.composeapp.generated.resources.quote_composition_fees
import _3dreport.composeapp.generated.resources.quote_composition_labor
import _3dreport.composeapp.generated.resources.quote_composition_material
import _3dreport.composeapp.generated.resources.quote_composition_profit
import _3dreport.composeapp.generated.resources.quote_help_compare
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.clickableAction
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PriceComposition
import org.jetbrains.compose.resources.stringResource

/**
 * Onde o dinheiro do preço está: barra empilhada em quatro grupos mais legenda. O detalhamento item a item
 * fica na conta completa. Os grupos somam o que o cliente paga.
 */
@Composable
internal fun CompositionBar(composition: PriceComposition) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val palette = colors.composition
    val profitLabel = stringResource(Res.string.quote_composition_profit)
    val groups = listOf(
        Triple(stringResource(Res.string.quote_composition_material), composition.materialAndMachine, palette.materialAndMachine),
        Triple(stringResource(Res.string.quote_composition_labor), composition.laborAndServices, palette.laborAndServices),
        Triple(stringResource(Res.string.quote_composition_fees), composition.feesAndTaxes, palette.feesAndTaxes),
        Triple(profitLabel, composition.profit, palette.profit),
    )
    val barDescription = stringResource(Res.string.quote_composition_description)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.compositionBarHeight)
                .clip(CircleShape)
                .semantics { contentDescription = barDescription },
            horizontalArrangement = Arrangement.spacedBy(AppDimens.compositionGap),
        ) {
            groups.filter { it.second > 0.0 }.forEach { (_, value, color) ->
                Box(Modifier.weight(value.toFloat()).fillMaxHeight().background(color))
            }
        }
        Column(modifier = Modifier.padding(top = space.sm)) {
            groups.filter { it.second > 0.005 || it.first == profitLabel }.forEach { (label, value, color) ->
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
internal fun Expander(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        Row(
            modifier = Modifier
                .clickableAction(onClick = { open = !open }, onNavy = true)
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
private fun AccountLine(label: String, value: String, muted: Boolean = false, indented: Boolean = false) {
    val colors = MaterialTheme.appColors
    Row(modifier = Modifier.fillMaxWidth().then(if (indented) Modifier.padding(start = MaterialTheme.spacing.sm) else Modifier), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.appType.caption, color = colors.onNavyMuted, modifier = Modifier.weight(1f))
        NumericText(value, style = MaterialTheme.appType.caption, color = if (muted) colors.onNavyMuted else colors.onNavy)
    }
}

/** A conta item a item: tudo o que a nota sempre mostrou, agora sob demanda. */
@Composable
internal fun FullAccount(quote: Quote, result: QuoteResult, settings: PricingSettings, onUseShippingCoveringPrice: () -> Unit) {
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
                            quote.channelFixedFee <= 0.0 -> stringResource(Res.string.quote_account_channel_rate, quote.channelName.orEmpty(), quote.channelFeeRate.toPercentText())
                            quote.channelFeeRate <= 0.0 -> stringResource(Res.string.quote_account_channel_fixed, quote.channelName.orEmpty(), perItem)
                            else -> stringResource(Res.string.quote_account_channel_both, quote.channelName.orEmpty(), quote.channelFeeRate.toPercentText(), perItem)
                        },
                    )
                }
                if (quote.taxRate > 0.0) add(stringResource(Res.string.quote_account_tax, quote.taxRate.toPercentText()))
            }
            Text(
                stringResource(Res.string.quote_account_absorb, parts.joinToString(" ${stringResource(Res.string.quote_account_and)} ")),
                style = MaterialTheme.appType.caption,
                color = colors.onNavyMuted,
            )
        }
        if (channelCharges) AccountLine(stringResource(Res.string.quote_account_channel_takes, quote.channelName.orEmpty()), quote.channelFeeAmount.toMoney())
        AccountLine(stringResource(Res.string.quote_account_production), quote.productionCost.toMoney())
        if (quote.prints.size > 1) {
            quote.prints.forEachIndexed { index, print ->
                AccountLine(
                    stringResource(Res.string.quote_account_print_line, printTitle(index + 1, print.job.name.orEmpty()), print.printerName),
                    print.cost.total.toMoney(),
                    muted = true,
                    indented = true,
                )
            }
            val orderLevel = quote.productionCost - quote.prints.sumOf { it.cost.total }
            AccountLine(stringResource(Res.string.quote_account_order_level), orderLevel.toMoney(), muted = true, indented = true)
        }
        if (quote.costs.consumables > 0.0) AccountLine(stringResource(Res.string.quote_account_consumables), quote.costs.consumables.toMoney())
        AccountLine(stringResource(Res.string.quote_account_profit), quote.profit.toMoney())
        AccountLine(stringResource(Res.string.quote_account_margin), quote.actualProfitMargin.toPercentText())
        // Só o valor: o percentual configurado já aparece no "Pedido urgente (+X%)".
        if (quote.rush) AccountLine(stringResource(Res.string.quote_account_rush), quote.rushSurcharge.toMoney())
        result.selectedServices.forEach { service ->
            val multiplied = quote.quantity > 1 && !service.chargedPerOrder
            AccountLine(
                if (multiplied) stringResource(Res.string.quote_account_service_times, service.name, quote.quantity) else service.name,
                service.total(quote.quantity).toMoney(),
            )
            if (service.laborMinutes > 0) {
                val totalMinutes = service.totalLaborMinutes(quote.quantity)
                val hourly = service.total(quote.quantity) / (totalMinutes / 60.0)
                Text(
                    stringResource(Res.string.quote_account_service_yield, totalMinutes.minutesToDurationText(), hourly.toMoney()),
                    style = MaterialTheme.appType.caption,
                    color = colors.onNavyMuted,
                )
                if (settings.laborRatePerHour > 0.0 && hourly < settings.laborRatePerHour) {
                    Text(
                        stringResource(Res.string.quote_account_service_below_rate, settings.laborRatePerHour.toMoney()),
                        style = MaterialTheme.appType.caption,
                        color = colors.onNavyDanger,
                    )
                }
            }
        }
        if (result.chargedShipping > 0) AccountLine(stringResource(Res.string.quote_account_shipping), result.chargedShipping.toMoney())
        if (result.shippingAbsorbed && result.shippingCost > 0) {
            AccountLine(stringResource(Res.string.quote_account_free_shipping), stringResource(Res.string.quote_account_minus, result.shippingCost.toMoney()))
            Text(stringResource(Res.string.quote_account_free_shipping_note), style = MaterialTheme.appType.caption, color = colors.onNavyMuted)
            val covering = quote.shippingCoveringSalePrice
            if (covering != null && !quote.isNegotiated) {
                Text(
                    stringResource(Res.string.quote_account_keep_margin, (covering + result.servicesTotal).toMoney()),
                    style = MaterialTheme.appType.caption,
                    color = colors.onNavy,
                )
                AppButton(stringResource(Res.string.quote_account_use_price), onClick = onUseShippingCoveringPrice, kind = ButtonKind.OnNavy)
            }
        }
    }
}

/**
 * Mesma peça calculada em cada impressora cadastrada: responde "em qual máquina sai mais barato". Com
 * preço fechado ou anunciado, a comparação usa o preço que a margem daria em cada uma.
 */
@Composable
internal fun PrinterComparison(comparison: List<Pair<PrinterProfile, Quote>>, allPrints: Boolean) {
    val colors = MaterialTheme.appColors
    fun Quote.comparable() = (tableSalePrice ?: salePrice) + extrasTotal
    val cheapest = comparison.minByOrNull { it.second.comparable() }?.first?.id
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        comparison.forEach { (printer, quote) ->
            val name = if (allPrints) stringResource(Res.string.quote_compare_all_on, printer.name) else printer.name
            val best = printer.id == cheapest
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (best) stringResource(Res.string.quote_compare_cheapest, name) else name,
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
                stringResource(if (allPrints) Res.string.quote_compare_caption_all else Res.string.quote_compare_caption_one),
                style = MaterialTheme.appType.caption,
                color = colors.onNavyMuted,
                modifier = Modifier.weight(1f, fill = false),
            )
            HelpHint(
                HelpText(stringResource(Res.string.quote_help_compare)),
                label = stringResource(Res.string.quote_compare_title),
                tint = colors.onNavyMuted,
                modifier = Modifier.padding(start = MaterialTheme.spacing.xxs),
            )
        }
    }
}
