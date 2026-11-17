package com.threedreport.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Menu do kit (o de ações de um pedido, o de envio pro cliente): fundo `card`, raio 8, borda `line` e a sombra
 * que o sistema de design reserva pra menu. Os itens são [AppMenuItem]; os grupos, [AppMenuGroup].
 */
@Composable
fun AppMenu(expanded: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.appColors.card,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        shadowElevation = AppDimens.menuElevation,
        border = BorderStroke(AppDimens.borderWidth, MaterialTheme.appColors.line),
        content = content,
    )
}

/** Um item de [AppMenu], com 40 de altura (`AppDimens.menuItemHeight`): texto em `body` e, se houver, o ícone à esquerda. [danger] pinta de `error` (excluir). */
@Composable
fun AppMenuItem(text: String, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true, danger: Boolean = false) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    AppMenuItem(text, onClick, enabled = enabled, danger = danger) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (danger) tint else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** [AppMenuItem] com um [leading] qualquer à esquerda (a bolinha da cor de uma situação, por exemplo). */
@Composable
fun AppMenuItem(text: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false, leading: @Composable () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(text, style = MaterialTheme.appType.body, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        },
        leadingIcon = leading,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.height(AppDimens.menuItemHeight),
    )
}

/** O divisor entre grupos de um [AppMenu]: 1 px em `line`, com um respiro de 4 em cima e embaixo. */
@Composable
fun AppMenuDivider() = AppDivider(Modifier.padding(vertical = MaterialTheme.spacing.xxs))

/** O título de um grupo de itens do [AppMenu], em `overline`. */
@Composable
fun AppMenuGroup(title: String) {
    val space = MaterialTheme.spacing
    Text(
        title.uppercase(),
        style = MaterialTheme.appType.overline,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = space.sm, top = space.xs, end = space.sm, bottom = space.xxs),
    )
}
