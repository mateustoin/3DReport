package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.quote_deadline_days
import _3dreport.composeapp.generated.resources.quote_deadline_none
import _3dreport.composeapp.generated.resources.quote_deadline_pick
import _3dreport.composeapp.generated.resources.quote_deadline_title
import _3dreport.composeapp.generated.resources.quote_deadline_unset
import _3dreport.composeapp.generated.resources.quote_deadline_until
import _3dreport.composeapp.generated.resources.quote_deadline_use
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
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.platform.weekdayName
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ButtonKind
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
 * @param onChange chamado com o novo prazo, ou `null` pra remover.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeliveryDatePicker(epochDay: Long?, daysLate: Int, onChange: (Long?) -> Unit) {
    val today = todayEpochDay()
    val space = MaterialTheme.spacing
    var showCalendar by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            // Os atalhos e "Escolher data…" formam um seletor só. Marca "Escolher data…" quando a data veio do
            // calendário e não bate com nenhum atalho.
            val picked = epochDay != null && QUICK_DAYS.none { epochDay == today + it }
            val selected = when {
                picked -> QUICK_DAYS.size
                else -> QUICK_DAYS.indexOfFirst { epochDay == today + it }
            }
            SegmentedChoice(
                options = QUICK_DAYS.map { stringResource(Res.string.quote_deadline_days, it) } + stringResource(Res.string.quote_deadline_pick),
                selectedIndex = selected,
                onSelect = { index -> if (index < QUICK_DAYS.size) onChange(today + QUICK_DAYS[index]) else showCalendar = true },
            )
            if (epochDay != null) AppButton(stringResource(Res.string.quote_deadline_none), onClick = { onChange(null) })
        }

        when {
            epochDay == null -> Text(
                stringResource(Res.string.quote_deadline_unset),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            daysLate > 0 -> Text(
                // A mesma frase do painel de resultado.
                pluralStringResource(Res.plurals.quote_result_deadline_overdue, daysLate, formatShortDate(epochDay), weekdayName(epochDay), daysLate),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Text(
                stringResource(Res.string.quote_deadline_until, formatDate(epochDay), weekdayName(epochDay)),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (showCalendar) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = epochDay?.let(::epochDayToUtcMillis),
            // Prometer entrega no passado não existe: o calendário nem deixa escolher.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcMillisToEpochDay(utcTimeMillis) >= today
            },
        )
        // O calendário é o do Material 3, com a forma e a cor do tema; os botões são os do kit.
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            shape = MaterialTheme.shapes.large,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.appColors.card),
            confirmButton = {
                AppButton(
                    stringResource(Res.string.quote_deadline_use),
                    onClick = {
                        state.selectedDateMillis?.let { onChange(utcMillisToEpochDay(it)) }
                        showCalendar = false
                    },
                    kind = ButtonKind.Primary,
                    enabled = state.selectedDateMillis != null,
                )
            },
            dismissButton = { AppButton(stringResource(Res.string.common_cancel), onClick = { showCalendar = false }) },
        ) {
            DatePicker(state = state, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.appColors.card))
        }
    }
}

/**
 * Diálogo "Prazo de entrega" do Histórico: muda só a data, sem abrir a edição completa do
 * orçamento é o caminho natural quando o cliente demorou a responder e o prazo venceu.
 */
@Composable
fun DeliveryDateDialog(quoteName: String, initial: Long?, daysLate: (Long?) -> Int, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
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
        DeliveryDatePicker(epochDay = epochDay, daysLate = daysLate(epochDay), onChange = { epochDay = it })
    }
}
