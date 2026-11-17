package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.overdue_send_anyway
import _3dreport.composeapp.generated.resources.overdue_send_batch
import _3dreport.composeapp.generated.resources.overdue_send_change_date
import _3dreport.composeapp.generated.resources.overdue_send_single
import _3dreport.composeapp.generated.resources.overdue_send_title
import androidx.compose.runtime.Composable
import com.threedreport.app.platform.formatDate
import org.jetbrains.compose.resources.stringResource

/** Um orçamento de prazo vencido no aviso de envio: o nome e o prazo (dias desde 01/01/1970). */
data class OverdueItem(val name: String, val deadlineEpochDay: Long)

/**
 * Aviso antes de mandar pro cliente um orçamento com prazo já vencido, o mesmo em Pedidos e no Orçamento. Com
 * um orçamento só, o caminho principal é "Alterar prazo" ([onChangeDate]); no envio em lote não dá pra editar
 * vários prazos de uma vez, então o aviso lista quais estão vencidos e oferece só "Enviar assim mesmo" ou
 * cancelar ([onCancel]).
 */
@Composable
fun OverdueExportDialog(
    overdue: List<OverdueItem>,
    onChangeDate: () -> Unit,
    onSendAnyway: () -> Unit,
    onCancel: () -> Unit,
) {
    val single = overdue.singleOrNull()
    AppDialog(
        title = stringResource(Res.string.overdue_send_title),
        onDismiss = onCancel,
        buttons = {
            AppButton(stringResource(Res.string.overdue_send_anyway), onSendAnyway)
            if (single != null) {
                AppButton(stringResource(Res.string.overdue_send_change_date), onChangeDate, kind = ButtonKind.Primary)
            } else {
                AppButton(stringResource(Res.string.common_cancel), onCancel, kind = ButtonKind.Primary)
            }
        },
    ) {
        DialogText(
            if (single != null) {
                stringResource(Res.string.overdue_send_single, single.name, formatDate(single.deadlineEpochDay))
            } else {
                stringResource(
                    Res.string.overdue_send_batch,
                    overdue.size,
                    overdue.joinToString { "\"${it.name}\" (${formatDate(it.deadlineEpochDay)})" },
                )
            },
        )
    }
}
