package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.history_reprice_after
import _3dreport.composeapp.generated.resources.history_reprice_announced
import _3dreport.composeapp.generated.resources.history_reprice_before
import _3dreport.composeapp.generated.resources.history_reprice_confirm
import _3dreport.composeapp.generated.resources.history_reprice_cost
import _3dreport.composeapp.generated.resources.history_reprice_help
import _3dreport.composeapp.generated.resources.history_reprice_help_detail
import _3dreport.composeapp.generated.resources.history_reprice_loss
import _3dreport.composeapp.generated.resources.history_reprice_price
import _3dreport.composeapp.generated.resources.history_reprice_profit
import _3dreport.composeapp.generated.resources.history_reprice_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.shownName
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** Antes e depois de atualizar o preço de um produto (decisão 102), pra confirmar antes de gravar. */
@Composable
fun RepriceDialog(repricing: Repricing, viewModel: QuoteHistoryViewModel) {
    val currency = repricing.product.currency
    fun money(value: Double) = value.toCurrencyText(currency)
    AppDialog(
        title = stringResource(Res.string.history_reprice_title, repricing.product.shownName()),
        onDismiss = viewModel::cancelRepricing,
        help = HelpText(stringResource(Res.string.history_reprice_help), stringResource(Res.string.history_reprice_help_detail)),
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), viewModel::cancelRepricing)
            AppButton(stringResource(Res.string.history_reprice_confirm), viewModel::confirmRepricing, kind = ButtonKind.Primary)
        },
    ) {
        ComparisonRow(null, stringResource(Res.string.history_reprice_before), stringResource(Res.string.history_reprice_after))
        ComparisonRow(stringResource(Res.string.history_reprice_price), money(repricing.priceBefore), money(repricing.priceAfter))
        ComparisonRow(stringResource(Res.string.history_reprice_cost), money(repricing.costBefore), money(repricing.costAfter))
        ComparisonRow(stringResource(Res.string.history_reprice_profit), money(repricing.profitBefore), money(repricing.profitAfter))
        repricing.announcedPrice?.let { DialogText(stringResource(Res.string.history_reprice_announced, money(it)), muted = true) }
        val loss = repricing.loss
        val breakEven = repricing.breakEven
        if (loss != null && breakEven != null) {
            Text(
                stringResource(Res.string.history_reprice_loss, money(loss), money(breakEven)),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Uma linha do antes e depois: o rótulo à esquerda e os dois valores em colunas, alinhados à direita. Sem rótulo, é o cabeçalho. */
@Composable
private fun ComparisonRow(label: String?, before: String, after: String) {
    val header = label == null
    val style = if (header) MaterialTheme.appType.label else MaterialTheme.appType.body
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Text(label.orEmpty(), style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        NumericText(before, style = style, color = muted, textAlign = TextAlign.End, modifier = Modifier.width(AppDimens.tableMoneyColumn))
        NumericText(
            after,
            style = if (header) style else MaterialTheme.appType.bodyStrong,
            color = if (header) muted else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(AppDimens.tableMoneyColumn),
        )
    }
}
