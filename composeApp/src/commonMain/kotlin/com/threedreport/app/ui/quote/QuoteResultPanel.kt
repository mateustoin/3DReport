package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_compare_title
import _3dreport.composeapp.generated.resources.quote_result_all_out_of_stock
import _3dreport.composeapp.generated.resources.quote_result_below_minimum
import _3dreport.composeapp.generated.resources.quote_result_break_even
import _3dreport.composeapp.generated.resources.quote_result_consumption
import _3dreport.composeapp.generated.resources.quote_result_deadline
import _3dreport.composeapp.generated.resources.quote_result_deadline_overdue
import _3dreport.composeapp.generated.resources.quote_result_extras_both
import _3dreport.composeapp.generated.resources.quote_result_extras_services
import _3dreport.composeapp.generated.resources.quote_result_extras_shipping
import _3dreport.composeapp.generated.resources.quote_result_filaments_archived
import _3dreport.composeapp.generated.resources.quote_result_fill_many
import _3dreport.composeapp.generated.resources.quote_result_fill_one
import _3dreport.composeapp.generated.resources.quote_result_fix_field
import _3dreport.composeapp.generated.resources.quote_result_full_account
import _3dreport.composeapp.generated.resources.quote_result_kept
import _3dreport.composeapp.generated.resources.quote_result_kept_with_today
import _3dreport.composeapp.generated.resources.quote_result_loss
import _3dreport.composeapp.generated.resources.quote_result_minimum_applied
import _3dreport.composeapp.generated.resources.quote_result_no_filament
import _3dreport.composeapp.generated.resources.quote_result_no_price
import _3dreport.composeapp.generated.resources.quote_result_no_printer
import _3dreport.composeapp.generated.resources.quote_result_overline
import _3dreport.composeapp.generated.resources.quote_result_printers_archived
import _3dreport.composeapp.generated.resources.quote_result_prints
import _3dreport.composeapp.generated.resources.quote_result_profit
import _3dreport.composeapp.generated.resources.quote_result_recalculated
import _3dreport.composeapp.generated.resources.quote_result_units
import _3dreport.composeapp.generated.resources.quote_result_units_extras
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
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.displayPriceAutoSize
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PriceComposition
import com.threedreport.core.pricing.priceComposition
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** O que o painel precisa pra um estado sem resultado: o texto e se é erro. */
internal data class NoResultMessage(val text: String, val isError: Boolean)

/** Por que não há preço ainda (campo inválido, cadastro faltando, dados por preencher). */
@Composable
internal fun noResultMessage(
    result: QuoteResult,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    input: QuoteInputState,
): NoResultMessage = when {
    result.errorMessage != null -> NoResultMessage(result.errorMessage.asString(), true)

    result.fieldErrors.isNotEmpty() ->
        NoResultMessage(stringResource(Res.string.quote_result_fix_field, result.fieldErrors.values.first().asString()), true)

    allFilaments.none { it.hasStockAvailable } && allFilaments.isNotEmpty() ->
        NoResultMessage(stringResource(Res.string.quote_result_all_out_of_stock), false)

    allFilaments.isEmpty() -> NoResultMessage(stringResource(Res.string.quote_result_no_filament), false)

    printers.isEmpty() -> NoResultMessage(stringResource(Res.string.quote_result_no_printer), false)

    // Tudo arquivado (decisão 115): não há o que escolher, e pedir peso e tempo não ajudaria.
    allFilaments.none { it.hasStockAvailable && !it.archived } ->
        NoResultMessage(stringResource(Res.string.quote_result_filaments_archived), false)

    printers.all { it.archived } -> NoResultMessage(stringResource(Res.string.quote_result_printers_archived), false)

    input.prints.size > 1 -> NoResultMessage(stringResource(Res.string.quote_result_fill_many), false)

    else -> NoResultMessage(stringResource(Res.string.quote_result_fill_one), false)
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
    negotiating: Boolean,
    onNegotiatingChange: (Boolean) -> Unit,
    canSave: Boolean,
    cannotSaveReason: String,
    saveLabel: String,
    cancelLabel: String?,
    fixed: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onUseShippingCoveringPrice: () -> Unit,
    onTargetTotalChange: (String) -> Unit,
    send: ClientSendActions,
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
                Expander(stringResource(Res.string.quote_result_full_account)) {
                    FullAccount(quote, result, settings, onUseShippingCoveringPrice)
                }
                if (comparison.size > 1) {
                    Expander(stringResource(Res.string.quote_compare_title)) { PrinterComparison(comparison, allPrints = input.prints.size > 1) }
                }
            } else {
                val message = noResultMessage(result, allFilaments, printers, input)
                Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    Text(stringResource(Res.string.quote_result_overline).uppercase(), style = MaterialTheme.appType.overline, color = colors.onNavyMuted)
                    Text(stringResource(Res.string.quote_result_no_price), style = MaterialTheme.appType.displayPrice, color = colors.onNavyMuted)
                    Text(
                        message.text,
                        style = MaterialTheme.appType.caption,
                        color = if (message.isError) colors.onNavyDanger else colors.onNavyMuted,
                    )
                }
            }
        }
        ResultFooter(
            result = result,
            input = input,
            canSave = canSave,
            cannotSaveReason = cannotSaveReason,
            saveLabel = saveLabel,
            cancelLabel = cancelLabel,
            onSave = onSave,
            onCancel = onCancel,
            onTargetTotalChange = onTargetTotalChange,
            negotiating = negotiating,
            onNegotiatingChange = onNegotiatingChange,
            send = send,
        )
    }
}

@Composable
private fun PriceHeader(quote: Quote, result: QuoteResult, input: QuoteInputState, saveForm: SaveQuoteFormState) {
    val colors = MaterialTheme.appColors
    val grandTotal = result.grandTotal ?: quote.salePrice
    val caption = MaterialTheme.appType.caption
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Text(stringResource(Res.string.quote_result_overline).uppercase(), style = MaterialTheme.appType.overline, color = colors.onNavyMuted)
        BasicText(
            grandTotal.toMoney(),
            style = MaterialTheme.appType.displayPrice.copy(color = Color.White),
            maxLines = 1,
            autoSize = displayPriceAutoSize,
        )
        fun line(text: String, color: Color = colors.onNavyMuted, weight: FontWeight? = null) = text to (color to weight)
        val unitsLine = if (quote.quantity > 1) {
            // A mesma conta do PDF e da mensagem: o preço de cada peça, com serviços e frete à parte.
            val unit = quote.unitSalePrice.toMoney()
            when {
                result.selectedServices.isNotEmpty() && result.chargedShipping > 0 ->
                    pluralStringResource(Res.plurals.quote_result_units_extras, quote.quantity, quote.quantity, unit, stringResource(Res.string.quote_result_extras_both))

                result.selectedServices.isNotEmpty() ->
                    pluralStringResource(Res.plurals.quote_result_units_extras, quote.quantity, quote.quantity, unit, stringResource(Res.string.quote_result_extras_services))

                result.chargedShipping > 0 ->
                    pluralStringResource(Res.plurals.quote_result_units_extras, quote.quantity, quote.quantity, unit, stringResource(Res.string.quote_result_extras_shipping))

                else -> pluralStringResource(Res.plurals.quote_result_units, quote.quantity, quote.quantity, unit)
            }
        } else {
            null
        }
        val consumption = stringResource(Res.string.quote_result_consumption, quote.filamentTotals().joinToString(" · ") { it.displayText() })
        val printsLine = if (quote.prints.size > 1) {
            stringResource(Res.string.quote_result_prints, quote.prints.size, quote.totalPrintTimeMinutes.minutesToDurationText())
        } else {
            null
        }
        // O mesmo prazo que sai no PDF, pra ver na nota o que o cliente vai ler.
        val deadlineLine = saveForm.deliveryDateEpochDay.takeUnless { input.isProduct }?.let {
            val overdue = it < todayEpochDay()
            line(
                stringResource(if (overdue) Res.string.quote_result_deadline_overdue else Res.string.quote_result_deadline, formatDate(it), weekdayName(it)),
                if (overdue) colors.onNavyDanger else colors.onNavy,
                FontWeight.Medium,
            )
        }
        val minimumLine = if (quote.minimumPriceApplied) stringResource(Res.string.quote_result_minimum_applied, quote.priceBeforeMinimum!!.toMoney()) else null
        // Reabrindo um orçamento (decisão 108): o preço salvo fica até mexerem no que muda o preço.
        val keptLine = if (result.keepsOriginalPrice) {
            val today = result.todaysQuote
            if (today != null && abs(today.customerTotal - quote.customerTotal) >= 0.005) {
                stringResource(Res.string.quote_result_kept_with_today, today.customerTotal.toMoney())
            } else {
                stringResource(Res.string.quote_result_kept)
            }
        } else {
            null
        }
        val original = result.originalQuote
        val recalculatedLine = if (original != null && abs(original.customerTotal - quote.customerTotal) >= 0.005) {
            stringResource(Res.string.quote_result_recalculated, original.customerTotal.toMoney())
        } else {
            null
        }
        val lines = buildList {
            unitsLine?.let { add(line(it)) }
            // Quanto sai de cada carretel, que é o que se confere no estoque.
            add(line(consumption))
            printsLine?.let { add(line(it)) }
            deadlineLine?.let { add(it) }
            minimumLine?.let { add(line(it)) }
            keptLine?.let { add(line(it)) }
            recalculatedLine?.let { add(line(it, colors.onNavy)) }
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
        StatBlock(stringResource(Res.string.quote_result_profit), quote.profit.toMoney(), if (quote.profit < 0) colors.onNavyDanger else colors.amber, Modifier.weight(1f))
        StatBlock(stringResource(Res.string.quote_result_break_even), breakEven.toMoney(), Color.White, Modifier.weight(1f))
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
            stringResource(Res.string.quote_result_loss, (-quote.profit).toMoney()),
            style = MaterialTheme.appType.caption,
            color = colors.onNavyDanger,
        )
    }
    if (minimumOrderPrice > 0 && quote.isNegotiated && quote.salePrice < minimumOrderPrice) {
        Text(stringResource(Res.string.quote_result_below_minimum, minimumOrderPrice.toMoney()), style = MaterialTheme.appType.caption, color = colors.amber)
    }
}
