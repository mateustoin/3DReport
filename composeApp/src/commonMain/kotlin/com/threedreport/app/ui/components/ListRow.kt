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
import androidx.compose.foundation.layout.RowScope
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
 * [valueMuted] deixa só o valor em tom discreto ("A combinar"). [muted] apaga o nome (item arquivado). [selected] destaca a linha com o fundo `tint` (o item aberto no painel).
 * [footer] é um bloco opcional sob os detalhes (o estoque de cada cor, a situação da manutenção); o que houver nele
 * que seja clicável responde por si, sem abrir a edição.
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
    valueMuted: Boolean = false,
    selected: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        RevealActions(actions = actions, selected = selected) {
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
                    footer?.invoke()
                }
                if (value != null) {
                    NumericText(
                        value,
                        style = MaterialTheme.appType.body,
                        color = if (muted || valueMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        HorizontalDivider(color = colors.line, thickness = AppDimens.borderWidth)
    }
}

/**
 * Faixa com ações de ícone que só aparecem com o mouse em cima ou com o foco dentro (teclado), sem sair da ordem do
 * Tab. É o mecanismo de [ListRow], aberto pra outras listas (os componentes do diálogo de manutenção). As [actions]
 * (e o [extra], um botão que abre um menu, por exemplo) ficam por cima do fim do [content], com o fundo da faixa; quem põe algo no fim do conteúdo deve reservar o espaço
 * (veja [revealActionsWidth]). [content] recebe se a faixa está ativa. [selected] usa o fundo `tint`. Com [enabled] falso, a faixa não existe
 * (o modo de seleção da tabela: clicar só marca) e a linha também não muda de fundo no hover.
 */
@Composable
fun RevealActions(
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    extra: @Composable RowScope.() -> Unit = {},
    enabled: Boolean = true,
    content: @Composable (active: Boolean) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val active = hovered || focusWithin
    val actionsAlpha by animateFloatAsState(if (active) 1f else 0f, tween(AppDimens.MOTION_MILLIS))
    val rowBackground = when {
        selected -> colors.tint
        active && enabled -> colors.column
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(rowBackground)
            .onFocusChanged { focusWithin = it.hasFocus }
            .hoverable(interaction),
        contentAlignment = Alignment.CenterEnd,
    ) {
        content(active)
        if (enabled) {
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
                extra()
            }
        }
    }
}

/** Largura que [RevealActions] ocupa com [count] ações, pra reservar no fim do conteúdo. */
@Composable
fun revealActionsWidth(count: Int): androidx.compose.ui.unit.Dp =
    AppDimens.smallControlHeight * count + MaterialTheme.spacing.xxs * (count + 1)

private const val DETAIL_SEPARATOR = " · "
