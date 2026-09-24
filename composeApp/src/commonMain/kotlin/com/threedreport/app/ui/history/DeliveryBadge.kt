package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.formatShortDate
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote

/**
 * Etiqueta do prazo de entrega nos cards do Histórico (lista e Kanban). É o ganho de guardar uma
 * data em vez de "N dias" (decisão 85): o app sabe quando o prazo venceu e mostra isso sem o
 * vendedor precisar conferir pedido por pedido.
 *
 * Três leituras diferentes, porque pedem ações diferentes:
 * - **atrasado** (aprovado, imprimindo ou pronto, com a data já passada): cor de erro, é cliente
 *   esperando;
 * - **prazo vencido** num orçamento ainda Orçado: neutro, é só um orçamento parado que precisa de
 *   data nova antes de ser reenviado;
 * - no prazo, ou já entregue: neutro.
 *
 * Não desenha nada sem prazo.
 */
@Composable
fun DeliveryBadge(savedQuote: SavedQuote, todayEpochDay: Long, modifier: Modifier = Modifier) {
    val deadline = savedQuote.deliveryDateEpochDay ?: return
    val overdue = savedQuote.isDeliveryOverdue(todayEpochDay)
    val date = formatShortDate(deadline)

    val (text, background, foreground) = when {
        overdue && savedQuote.status == OrderStatus.ORCADO -> Triple(
            "Prazo vencido ($date) — atualize antes de reenviar",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        overdue -> Triple(
            "Atrasado · entrega era $date",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        savedQuote.status == OrderStatus.ENTREGUE -> Triple(
            "Prazo era $date",
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        deadline == todayEpochDay -> Triple(
            "Entrega hoje",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        else -> Triple(
            "Entrega até $date",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
