package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.filaments_color_unnamed
import _3dreport.composeapp.generated.resources.stock_amount_units
import _3dreport.composeapp.generated.resources.stock_item_removed
import _3dreport.composeapp.generated.resources.stock_movement_adjustment
import _3dreport.composeapp.generated.resources.stock_movement_order
import _3dreport.composeapp.generated.resources.stock_movement_order_plain
import _3dreport.composeapp.generated.resources.stock_movement_purchase
import _3dreport.composeapp.generated.resources.stock_movement_start
import _3dreport.composeapp.generated.resources.stock_reason_failure
import _3dreport.composeapp.generated.resources.stock_reason_other
import _3dreport.composeapp.generated.resources.stock_reason_own_use
import _3dreport.composeapp.generated.resources.stock_reason_test
import androidx.compose.runtime.Composable
import com.threedreport.app.ui.format.toPurchaseGramsText
import com.threedreport.app.ui.format.toUnitsNumberText
import com.threedreport.app.ui.format.toWholeGramsText
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovementKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** O saldo de um item no texto da tela: gramas sem casa decimal ("640 g", "-40 g") ou unidades ("8 un"). */
@Composable
fun stockAmountText(ref: StockItemRef, amount: Double): String = when (ref) {
    is StockItemRef.ColorRef -> amount.toWholeGramsText()
    is StockItemRef.ConsumableRef -> stringResource(Res.string.stock_amount_units, amount.toUnitsNumberText())
}

/** Como [stockAmountText], mas com o sinal à frente ("+1000 g", "-100 g"): a quantidade de um movimento. */
@Composable
fun stockSignedAmountText(ref: StockItemRef, amount: Double): String =
    (if (amount > 0) "+" else "") + stockAmountText(ref, amount)

/** A quantidade de uma linha de compra: gramas viram quilos a partir de mil ("1 kg", "500 g"). */
@Composable
fun stockPurchaseAmountText(ref: StockItemRef, amount: Double): String = when (ref) {
    is StockItemRef.ColorRef -> amount.toPurchaseGramsText()
    is StockItemRef.ConsumableRef -> stringResource(Res.string.stock_amount_units, amount.toUnitsNumberText())
}

/** O nome de uma cor na linha de um filamento: o nome, o hex ou, sem nenhum dos dois, "Sem nome". */
@Composable
fun StockEntry.colorLabel(): String = name ?: stringResource(Res.string.filaments_color_unnamed)

/** O nome do item pra frases e seletores: o do cadastro, e "(removido)" pra quem já saiu dele. */
@Composable
fun StockEntry.label(): String = if (removed) stringResource(Res.string.stock_item_removed) else displayName

/** O que um movimento é, em uma palavra: "Compra", "Pedido #0042", "Falha", "Ajuste". */
@Composable
fun MovementRowState.title(): String = when (kind) {
    StockMovementKind.START -> stringResource(Res.string.stock_movement_start)
    StockMovementKind.PURCHASE -> stringResource(Res.string.stock_movement_purchase)
    StockMovementKind.ORDER -> orderLabel?.let { stringResource(Res.string.stock_movement_order, it) } ?: stringResource(Res.string.stock_movement_order_plain)
    StockMovementKind.MANUAL_USE -> stringResource((reason ?: ManualUseReason.OTHER).labelRes())
    StockMovementKind.ADJUSTMENT -> stringResource(Res.string.stock_movement_adjustment)
}

/** O nome do motivo de um consumo avulso. */
fun ManualUseReason.labelRes(): StringResource = when (this) {
    ManualUseReason.FAILURE -> Res.string.stock_reason_failure
    ManualUseReason.TEST -> Res.string.stock_reason_test
    ManualUseReason.OWN_USE -> Res.string.stock_reason_own_use
    ManualUseReason.OTHER -> Res.string.stock_reason_other
}
