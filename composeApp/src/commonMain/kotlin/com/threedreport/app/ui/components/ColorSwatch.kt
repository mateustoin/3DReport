package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/** Círculo com a cor de um filamento. Sem [color] (hex vazio ou inválido), mostra só o contorno, no tom de campo desligado. */
@Composable
fun ColorSwatch(color: Color?, modifier: Modifier = Modifier, size: Dp = AppDimens.swatchSize, selected: Boolean = false) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(size)
            .background(color ?: colors.column, CircleShape)
            .border(
                if (selected) AppDimens.focusRingWidth else AppDimens.borderWidth,
                if (selected) MaterialTheme.colorScheme.primary else colors.line,
                CircleShape,
            ),
    )
}

/**
 * Amostra de cor que se escolhe com um clique (as cores prontas do formulário). É um botão só de imagem: [description]
 * vira o texto pro leitor de tela e a dica ao passar o mouse; anel de foco no teclado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwatchButton(color: Color, description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        var focused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(MaterialTheme.spacing.xxs / 2),
        ) {
            ColorSwatch(color, size = AppDimens.swatchSize, selected = selected || focused)
        }
    }
}

/**
 * Selo de uma cor do filamento na lista, que marca se tem em estoque: com amostra, nome e, quando acabou, o
 * [outOfStockLabel]. Clicar alterna ([onToggle]). Acabou fica apagado.
 */
@Composable
fun ColorChip(label: String, color: Color?, inStock: Boolean, outOfStockLabel: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    var focused by remember { mutableStateOf(false) }
    val description = if (inStock) label else "$label, $outOfStockLabel"
    Row(
        modifier = modifier
            .alpha(if (inStock) 1f else 0.6f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (inStock) colors.tint else Color.Transparent)
            .border(
                if (focused) AppDimens.focusRingWidth else AppDimens.borderWidth,
                if (focused) MaterialTheme.colorScheme.primary else colors.line,
                MaterialTheme.shapes.extraSmall,
            )
            .onFocusChanged { focused = it.isFocused }
            .toggleable(value = inStock, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = description }
            .padding(horizontal = space.xs, vertical = space.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xxs),
    ) {
        ColorSwatch(color, size = AppDimens.swatchSmallSize)
        Text(label, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        if (!inStock) Text(outOfStockLabel, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
