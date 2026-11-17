package com.threedreport.app.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.AppBadge
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType

/**
 * O selo de prazo de um pedido ([DeadlineBadge]), nas cores de prazo do tema e sempre com o texto. De um pedido que já
 * saiu do fluxo (entregue, cancelado) o prazo é só lembrança: texto de apoio, sem selo.
 */
@Composable
fun DeadlineBadgeView(badge: DeadlineBadge, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val palette = when (badge.tone) {
        DeadlineTone.ON_TIME, DeadlineTone.STALE -> colors.deadlineOnTime
        DeadlineTone.TODAY -> colors.deadlineToday
        DeadlineTone.LATE -> colors.deadlineLate
        DeadlineTone.DONE -> null
    }
    if (palette != null) {
        AppBadge(badge.text.asString(), palette, modifier)
    } else {
        Text(
            badge.text.asString(),
            style = MaterialTheme.appType.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
