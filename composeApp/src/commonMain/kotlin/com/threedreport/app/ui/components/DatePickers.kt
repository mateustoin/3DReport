package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_date_pick
import _3dreport.composeapp.generated.resources.common_date_use
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
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * O calendário do kit: o `DatePickerDialog` do Material 3 com a forma e a cor do tema e os botões do kit ([AppButton]).
 * [allowPast] e [allowFuture] dizem o que dá pra escolher em relação a [today] (prazo de entrega: só o futuro; pagamento e
 * manutenção feita: só o passado). Hoje sempre vale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    epochDay: Long?,
    today: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    allowPast: Boolean = true,
    allowFuture: Boolean = true,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = epochDay?.let(::epochDayToUtcMillis),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcMillisToEpochDay(utcTimeMillis)
                return (allowPast || day >= today) && (allowFuture || day <= today)
            }
        },
    )
    val container = MaterialTheme.appColors.card
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        colors = DatePickerDefaults.colors(containerColor = container),
        confirmButton = {
            AppButton(
                stringResource(Res.string.common_date_use),
                onClick = { state.selectedDateMillis?.let { onPick(utcMillisToEpochDay(it)) } },
                kind = ButtonKind.Primary,
                enabled = state.selectedDateMillis != null,
            )
        },
        dismissButton = { AppButton(stringResource(Res.string.common_cancel), onClick = onDismiss) },
    ) {
        DatePicker(state = state, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = container))
    }
}

/** Um atalho do [DateShortcutPicker]: o rótulo ("Hoje", "+7 dias") e o dia que ele escolhe. */
class DateShortcut(val label: String, val epochDay: Long)

/**
 * Seletor de data por atalhos: os [shortcuts] e "Escolher data…" (o [AppDatePickerDialog]) num seletor segmentado só, e
 * embaixo, sempre como texto, a data escolhida ([summary]; por padrão a data por extenso curta). Com [clearLabel], um botão
 * tira a data (prazo "Sem prazo"). [onChange] recebe o dia novo, ou `null` ao tirar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateShortcutPicker(
    epochDay: Long?,
    shortcuts: List<DateShortcut>,
    today: Long,
    onChange: (Long?) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    allowPast: Boolean = true,
    allowFuture: Boolean = true,
    clearLabel: String? = null,
    summary: @Composable (Long?) -> Unit = { day -> if (day != null) Text(formatDate(day), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface) },
) {
    val space = MaterialTheme.spacing
    var showCalendar by remember { mutableStateOf(false) }
    val matching = shortcuts.indexOfFirst { it.epochDay == epochDay }
    val selected = when {
        matching >= 0 -> matching
        epochDay != null -> shortcuts.size
        else -> -1
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(space.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            SegmentedChoice(
                options = shortcuts.map { it.label } + stringResource(Res.string.common_date_pick),
                selectedIndex = selected,
                onSelect = { index -> if (index < shortcuts.size) onChange(shortcuts[index].epochDay) else showCalendar = true },
            )
            if (clearLabel != null && epochDay != null) AppButton(clearLabel, onClick = { onChange(null) })
        }
        summary(epochDay)
    }
    if (showCalendar) {
        AppDatePickerDialog(
            epochDay = epochDay,
            today = today,
            onDismiss = { showCalendar = false },
            onPick = {
                onChange(it)
                showCalendar = false
            },
            allowPast = allowPast,
            allowFuture = allowFuture,
        )
    }
}
