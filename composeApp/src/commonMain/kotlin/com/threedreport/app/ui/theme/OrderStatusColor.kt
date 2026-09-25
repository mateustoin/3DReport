package com.threedreport.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.threedreport.core.model.OrderStatus

/**
 * Cor de progresso de um pedido: fria (recém orçado) até quente (entregue) — o status vira
 * informação visual (o pedido "esquenta" conforme avança) em vez de decoração arbitrária. Interpola
 * entre `primary` (azul petróleo) e `secondary` (âmbar, já usados no resto do app — decisão 35),
 * então acompanha o tema claro/escuro automaticamente em vez de fixar hexadecimais novos.
 *
 * Cancelado sai do fluxo e fica cinza (`outline`): não é um passo depois de Entregue.
 */
@Composable
fun OrderStatus.progressColor(): Color {
    if (this == OrderStatus.CANCELADO) return MaterialTheme.colorScheme.outline
    val cold = MaterialTheme.colorScheme.primary
    val warm = MaterialTheme.colorScheme.secondary
    val steps = OrderStatus.PIPELINE.size - 1
    val t = OrderStatus.PIPELINE.indexOf(this).toFloat() / steps
    return lerp(cold, warm, t)
}
