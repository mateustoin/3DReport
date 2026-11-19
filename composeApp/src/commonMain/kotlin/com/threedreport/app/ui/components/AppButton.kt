package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Os botões do sistema de design: um principal por tela (âmbar), o secundário, o que vai sobre o azul-marinho
 * e o de ação destrutiva (excluir, descartar), que só aparece em diálogo de confirmação.
 */
enum class ButtonKind { Primary, Secondary, OnNavy, Danger }

/**
 * Botão do kit. [Primary][ButtonKind.Primary]: fundo âmbar, texto `onAmber` em 600. [Secondary][ButtonKind.Secondary]:
 * fundo `card` com borda `line`. [OnNavy][ButtonKind.OnNavy]: transparente com borda `navyRaised`, e com [active] fundo `navyRaised`. O secundário
 * ativo ([active]) ganha fundo `tint` e borda `primary`.
 * [Danger][ButtonKind.Danger]: como o secundário, com texto `error`. Ícone opcional
 * à esquerda do texto (nunca só ícone).
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = AppDimens.controlHeight,
    active: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val background: Color
    val content: Color
    val border: Color
    when (kind) {
        ButtonKind.Primary -> {
            background = colors.amber
            content = colors.onAmber
            border = colors.amber
        }

        ButtonKind.Secondary -> {
            // Ativo (o modo que o botão liga está ligado): fundo `tint` e borda `primary`.
            background = if (active) colors.tint else colors.card
            content = MaterialTheme.colorScheme.onSurface
            border = if ((hovered && enabled) || active) MaterialTheme.colorScheme.primary else colors.line
        }

        ButtonKind.OnNavy -> {
            // Ativo (o campo que o botão abre está aberto): fundo `navyRaised`.
            background = if (active) colors.navyRaised else Color.Transparent
            content = colors.onNavy
            border = if (hovered && enabled) colors.onNavyMuted else colors.navyRaised
        }

        ButtonKind.Danger -> {
            background = colors.card
            content = MaterialTheme.colorScheme.error
            border = if (hovered && enabled) MaterialTheme.colorScheme.error else colors.line
        }
    }
    val ring = when (kind) {
        ButtonKind.OnNavy -> colors.onNavyMuted
        ButtonKind.Primary -> colors.onAmber
        ButtonKind.Secondary -> MaterialTheme.colorScheme.primary
        ButtonKind.Danger -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .defaultMinSize(minHeight = height)
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .border(if (focused) AppDimens.focusRingWidth else AppDimens.borderWidth, if (focused) ring else border, MaterialTheme.shapes.small)
            .onFocusChanged { focused = it.isFocused }
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(AppDimens.iconSize))
        Text(
            text,
            style = if (kind == ButtonKind.Primary) MaterialTheme.appType.bodyStrong else MaterialTheme.appType.body,
            fontWeight = if (kind == ButtonKind.Primary) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
    }
}

/** Ação só de ícone: sempre com descrição pro leitor de tela, dica ao passar o mouse e anel de foco no teclado. [onNavy] é a versão pro painel azul-marinho. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onNavy: Boolean = false,
    tabStop: Boolean = true,
) {
    androidx.compose.material3.TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description) } },
        state = androidx.compose.material3.rememberTooltipState(),
        modifier = modifier,
    ) {
        var focused by remember { mutableStateOf(false) }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(AppDimens.smallControlHeight)
                .clip(MaterialTheme.shapes.small)
                .then(if (focused) Modifier.border(AppDimens.focusRingWidth, if (onNavy) MaterialTheme.appColors.onNavy else MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .focusProperties { canFocus = tabStop }
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = when {
                    tint != Color.Unspecified -> tint
                    onNavy -> MaterialTheme.appColors.onNavyMuted
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(AppDimens.iconSize),
            )
        }
    }
}
