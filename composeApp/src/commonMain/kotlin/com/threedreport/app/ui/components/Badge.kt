package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.DeadlineColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Selo: texto curto em `label` semibold sobre um fundo de raio 6. É como o prazo aparece ("Hoje", "Atrasado 2
 * dias"): as cores vêm de [DeadlineColors] e o texto sempre acompanha, porque situação e prazo nunca dependem só da cor.
 */
@Composable
fun AppBadge(text: String, colors: DeadlineColors, modifier: Modifier = Modifier) {
    val space = MaterialTheme.spacing
    Text(
        text,
        style = MaterialTheme.appType.label,
        fontWeight = FontWeight.SemiBold,
        color = colors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(colors.background, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = space.xs, vertical = space.xxs),
    )
}

/** A situação de um pedido: o ponto na cor dela e o nome ao lado. */
@Composable
fun StatusLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Box(Modifier.size(AppDimens.pendingDotSize).background(color, CircleShape))
        Text(text, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
