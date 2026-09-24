package com.threedreport.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.epochDayToUtcMillis
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.platform.weekdayName

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
 * @param onChange chamado com o novo prazo, ou `null` pra remover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDatePicker(epochDay: Long?, onChange: (Long?) -> Unit) {
    val today = todayEpochDay()
    var showCalendar by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_DAYS.forEach { days ->
                FilterChip(
                    selected = epochDay == today + days,
                    onClick = { onChange(today + days) },
                    label = { Text("+$days dias") },
                )
            }
            FilterChip(
                // Marcado quando a data veio do calendário e não bate com nenhum atalho.
                selected = epochDay != null && QUICK_DAYS.none { epochDay == today + it },
                onClick = { showCalendar = true },
                label = { Text("Escolher data…") },
            )
            if (epochDay != null) {
                FilterChip(selected = false, onClick = { onChange(null) }, label = { Text("Sem prazo") })
            }
        }

        when {
            epochDay == null -> Text(
                "Sem prazo definido — não aparece no PDF nem na mensagem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            epochDay < today -> Text(
                "Entrega até ${formatDate(epochDay)} (${weekdayName(epochDay)}) — essa data já passou.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            else -> Text(
                "Entrega até ${formatDate(epochDay)} (${weekdayName(epochDay)})",
                style = MaterialTheme.typography.bodyMedium,
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
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(utcMillisToEpochDay(it)) }
                        showCalendar = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("Usar esta data") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/**
 * Diálogo "Prazo de entrega" do Histórico: muda só a data, sem abrir a edição completa do
 * orçamento — é o caminho natural quando o cliente demorou a responder e o prazo venceu.
 */
@Composable
fun DeliveryDateDialog(quoteName: String, initial: Long?, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    var epochDay by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prazo de entrega") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(quoteName, style = MaterialTheme.typography.bodyMedium)
                DeliveryDatePicker(epochDay = epochDay, onChange = { epochDay = it })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(epochDay) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
