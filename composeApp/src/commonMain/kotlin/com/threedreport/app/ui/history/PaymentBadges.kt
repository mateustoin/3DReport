package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.orders_notes_description
import _3dreport.composeapp.generated.resources.orders_stale
import _3dreport.composeapp.generated.resources.payment_status_deposit_paid
import _3dreport.composeapp.generated.resources.payment_status_not_paid
import _3dreport.composeapp.generated.resources.payment_status_paid
import _3dreport.composeapp.generated.resources.payment_status_partially_paid
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppBadge
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.core.model.PaymentStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** O nome da situação do pagamento ("Sinal pago"), o mesmo no selo e no diálogo de pagamentos. */
fun PaymentStatus.labelRes(): StringResource = when (this) {
    PaymentStatus.NOT_PAID -> Res.string.payment_status_not_paid
    PaymentStatus.DEPOSIT_PAID -> Res.string.payment_status_deposit_paid
    PaymentStatus.PARTIALLY_PAID -> Res.string.payment_status_partially_paid
    PaymentStatus.PAID -> Res.string.payment_status_paid
}

/** O selo de pagamento (decisão 146, item 14). "Não pago" pede atenção; as outras situações ficam neutras, sempre com o texto. */
@Composable
fun PaymentBadgeView(status: PaymentStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val palette = if (status == PaymentStatus.NOT_PAID) colors.deadlineToday else colors.deadlineOnTime
    AppBadge(stringResource(status.labelRes()), palette, modifier)
}

/** "Parado há N dias" no tom de atenção, não de erro (decisão 149). */
@Composable
fun StaleBadgeView(days: Int, modifier: Modifier = Modifier) {
    AppBadge(pluralStringResource(Res.plurals.orders_stale, days, days), MaterialTheme.appColors.deadlineToday, modifier)
}

/** O ícone de observação (decisão 151): 16, só de leitura e sem foco próprio, com o texto na dica. Clicar no cartão ou na linha abre o "Editar detalhes". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesIcon(notes: String, modifier: Modifier = Modifier) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(notes) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Icon(
            AppIcons.Description,
            contentDescription = stringResource(Res.string.orders_notes_description, notes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppDimens.noteIconSize),
        )
    }
}
