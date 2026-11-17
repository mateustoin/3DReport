package com.threedreport.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.threedreport.core.model.OrderStatus

/**
 * A cor da situação de um pedido (docs/design-system.md, "Situação do pedido e prazo"): cinza, azul, âmbar e verde
 * ao longo do fluxo, e `textMuted` pra o que já saiu dele (entregue, cancelado). Sempre vem com o nome da situação:
 * a cor nunca fala sozinha.
 */
@Composable
fun OrderStatus.statusColor(): Color {
    val colors = MaterialTheme.appColors
    return when (this) {
        OrderStatus.ORCADO -> colors.statusQuoted
        OrderStatus.APROVADO -> colors.statusApproved
        OrderStatus.EM_IMPRESSAO -> colors.statusPrinting
        OrderStatus.PRONTO -> colors.statusReady
        OrderStatus.ENTREGUE, OrderStatus.CANCELADO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
