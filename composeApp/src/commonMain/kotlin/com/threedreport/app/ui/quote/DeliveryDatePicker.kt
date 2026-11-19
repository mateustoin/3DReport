package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.quote_deadline_days
import _3dreport.composeapp.generated.resources.quote_deadline_none
import _3dreport.composeapp.generated.resources.quote_deadline_title
import _3dreport.composeapp.generated.resources.quote_deadline_unset
import _3dreport.composeapp.generated.resources.quote_deadline_until
import _3dreport.composeapp.generated.resources.quote_result_deadline_overdue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.threedreport.app.platform.epochDayToUtcMillis
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.platform.weekdayName
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DateShortcut
import com.threedreport.app.ui.components.DateShortcutPicker
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Atalhos de prazo, em dias a partir de hoje. O vendedor pensa em "uma semana", não em "dia 30";
 * o app converte e guarda a data (decisão 85), que é o que o cliente lê e o que permite avisar
 * quando o prazo vence.
 */
private val QUICK_DAYS = listOf(3, 7, 15)

/**
 * Seletor de prazo de entrega: atalhos "+3 / +7 / +15 dias", "Escolher data…" (calendário) e
 * "Sem prazo". Usado no formulário de salvar da tela de Orçamento e no diálogo de prazo do
 * Histórico, pra os dois lugares funcionarem igual.
 *
 * @param epochDay prazo atual (dias desde 01/01/1970), ou `null` sem prazo.
 * @param daysLate quantos dias [epochDay] já passou (0 em dia), calculado pelo ViewModel ([deliveryDaysLate]).
 * @param today hoje, em dias desde 01/01/1970, de quem chama (o ViewModel), pra os atalhos e o calendário.
 * @param onChange chamado com o novo prazo, ou `null` pra remover.
 */
@Composable
fun DeliveryDatePicker(epochDay: Long?, daysLate: Int, today: Long, onChange: (Long?) -> Unit) {
    // Prometer entrega no passado não existe: o calendário só deixa escolher de hoje em diante.
    DateShortcutPicker(
        epochDay = epochDay,
        shortcuts = QUICK_DAYS.map { DateShortcut(stringResource(Res.string.quote_deadline_days, it), today + it) },
        today = today,
        onChange = onChange,
        allowPast = false,
        clearLabel = stringResource(Res.string.quote_deadline_none),
    ) { day ->
        when {
            day == null -> Text(
                stringResource(Res.string.quote_deadline_unset),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            daysLate > 0 -> Text(
                // A mesma frase do painel de resultado.
                pluralStringResource(Res.plurals.quote_result_deadline_overdue, daysLate, formatShortDate(day), weekdayName(day), daysLate),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Text(
                stringResource(Res.string.quote_deadline_until, formatDate(day), weekdayName(day)),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Diálogo "Prazo de entrega" do Histórico: muda só a data, sem abrir a edição completa do
 * orçamento é o caminho natural quando o cliente demorou a responder e o prazo venceu.
 */
@Composable
fun DeliveryDateDialog(quoteName: String, initial: Long?, daysLate: (Long?) -> Int, today: Long, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    var epochDay by remember { mutableStateOf(initial) }

    AppDialog(
        title = stringResource(Res.string.quote_deadline_title),
        onDismiss = onDismiss,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), onDismiss)
            AppButton(stringResource(Res.string.common_save), { onSave(epochDay) }, kind = ButtonKind.Primary)
        },
    ) {
        DialogText(quoteName)
        DeliveryDatePicker(epochDay = epochDay, daysLate = daysLate(epochDay), today = today, onChange = { epochDay = it })
    }
}
