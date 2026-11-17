package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_period_all
import _3dreport.composeapp.generated.resources.history_period_last_30_days
import _3dreport.composeapp.generated.resources.history_period_last_7_days
import _3dreport.composeapp.generated.resources.history_period_last_month
import _3dreport.composeapp.generated.resources.history_period_this_month
import _3dreport.composeapp.generated.resources.history_period_this_year
import _3dreport.composeapp.generated.resources.orders_status_approved
import _3dreport.composeapp.generated.resources.orders_status_cancelled
import _3dreport.composeapp.generated.resources.orders_status_delivered
import _3dreport.composeapp.generated.resources.orders_status_printing
import _3dreport.composeapp.generated.resources.orders_status_quoted
import _3dreport.composeapp.generated.resources.orders_status_ready
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.OrderStatus
import org.jetbrains.compose.resources.StringResource

/** O nome da situação do pedido, em recurso traduzível (o `core` só guarda o nome gravado em disco). */
fun OrderStatus.labelRes(): StringResource = when (this) {
    OrderStatus.ORCADO -> Res.string.orders_status_quoted
    OrderStatus.APROVADO -> Res.string.orders_status_approved
    OrderStatus.EM_IMPRESSAO -> Res.string.orders_status_printing
    OrderStatus.PRONTO -> Res.string.orders_status_ready
    OrderStatus.ENTREGUE -> Res.string.orders_status_delivered
    OrderStatus.CANCELADO -> Res.string.orders_status_cancelled
}

fun OrderStatus.labelText(): UiText = uiText(labelRes())

/** O nome do período do filtro, em recurso traduzível. */
fun PeriodPreset.labelRes(): StringResource = when (this) {
    PeriodPreset.ALL -> Res.string.history_period_all
    PeriodPreset.LAST_7_DAYS -> Res.string.history_period_last_7_days
    PeriodPreset.LAST_30_DAYS -> Res.string.history_period_last_30_days
    PeriodPreset.THIS_MONTH -> Res.string.history_period_this_month
    PeriodPreset.LAST_MONTH -> Res.string.history_period_last_month
    PeriodPreset.THIS_YEAR -> Res.string.history_period_this_year
}

/** As somas por moeda em um texto só, "R$ 300,00 · US$ 40,00": uma por moeda, a do app primeiro. */
fun List<CurrencyTotal>.toMoneyText(): String = joinToString(" · ") { it.amount.toCurrencyText(it.currency) }
