package com.threedreport.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/** Uma ação da linha (editar, arquivar, excluir): vira um botão de ícone, com [description] pro leitor de tela e pra dica. */
class RowAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    /** Ação destrutiva: o ícone sai na cor de erro. */
    val danger: Boolean = false,
)

/**
 * Linha de lista dos cadastros (não é cartão): nome em `bodyStrong`, [details] em `caption` separados por " · "
 * e o [value] alinhado à direita com algarismos tabulares. As [actions] só aparecem com o mouse em cima ou com o
 * foco na linha (teclado), e continuam na ordem do Tab. Clicar na linha chama [onClick]. Divisor de 1 px embaixo.
 * [muted] apaga o nome (item arquivado). [selected] destaca a linha com o fundo `tint` (o item aberto no painel).
 * As ações ficam por cima do fim da linha, com o fundo dela, e cobrem o valor enquanto estão visíveis.
 */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
    details: List<String> = emptyList(),
    value: String? = null,
    muted: Boolean = false,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val active = hovered || focusWithin
    val actionsAlpha by animateFloatAsState(if (active) 1f else 0f, tween(AppDimens.MOTION_MILLIS))

    val rowBackground = when {
        selected -> colors.tint
        active -> colors.column
        else -> Color.Transparent
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(rowBackground)
                .onFocusChanged { focusWithin = it.hasFocus }
                .hoverable(interaction),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .then(if (focused) Modifier.border(AppDimens.focusRingWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                    .padding(start = space.sm, end = space.xs, top = space.sm, bottom = space.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.appType.bodyStrong,
                        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (details.isNotEmpty()) {
                        Text(
                            details.joinToString(DETAIL_SEPARATOR),
                            style = MaterialTheme.appType.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (value != null) {
                    NumericText(
                        value,
                        style = MaterialTheme.appType.body,
                        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .alpha(actionsAlpha)
                    .background(if (selected) colors.tint else colors.column, MaterialTheme.shapes.small)
                    .padding(horizontal = space.xxs),
                horizontalArrangement = Arrangement.spacedBy(space.xxs),
            ) {
                actions.forEach { action ->
                    IconAction(
                        icon = action.icon,
                        description = action.description,
                        onClick = action.onClick,
                        tint = if (action.danger) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        }
        HorizontalDivider(color = colors.line, thickness = AppDimens.borderWidth)
    }
}

private const val DETAIL_SEPARATOR = " · "
