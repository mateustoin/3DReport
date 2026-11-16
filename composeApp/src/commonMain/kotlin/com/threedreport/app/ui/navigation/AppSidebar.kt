package com.threedreport.app.ui.navigation

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.BrandMark
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/** Largura da barra com rótulos e do trilho só com ícones. */
val SIDEBAR_WIDTH = AppDimens.sidebarWidth
val SIDEBAR_COMPACT_WIDTH = AppDimens.sidebarCompactWidth

/**
 * Janela mais estreita que isso fica com o trilho só de ícones: com a barra inteira, o Orçamento perdia
 * as duas colunas (decisão 111).
 */
val SIDEBAR_LABELS_MIN_WINDOW_WIDTH = 1280.dp

/**
 * Barra lateral agrupada (decisão 111) no azul-marinho do sistema de design, nos dois temas. Vendas e
 * Cadastros em cima; Configurações e Sobre no pé, com a versão embaixo. [compact] tira os rótulos e deixa o
 * nome de cada item numa dica ao passar o mouse.
 *
 * @param status texto curto ao lado do rótulo ("Editando"), por tela.
 * @param hasPendingChanges pontinho de "há alteração não salva aqui", por tela.
 */
@Composable
fun AppSidebar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    compact: Boolean,
    version: String,
    status: (AppDestination) -> String? = { null },
    hasPendingChanges: (AppDestination) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) SIDEBAR_COMPACT_WIDTH else SIDEBAR_WIDTH)
            .background(colors.navy)
            .padding(horizontal = if (compact) space.xs else space.sm, vertical = space.md),
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Brand(compact)
            listOf(DestinationGroup.SALES, DestinationGroup.REGISTRIES).forEach { group ->
                GroupTitle(group, compact)
                AppDestination.entries.filter { it.group == group }.forEach { destination ->
                    SidebarItem(destination, destination == selected, compact, status(destination), hasPendingChanges(destination)) {
                        onSelect(destination)
                    }
                }
            }
        }
        AppDestination.entries.filter { it.group == DestinationGroup.FOOTER }.forEach { destination ->
            SidebarItem(destination, destination == selected, compact, status(destination), hasPendingChanges(destination)) {
                onSelect(destination)
            }
        }
        Text(
            if (compact) "v$version" else "3DReport v$version",
            style = MaterialTheme.appType.label,
            color = colors.onNavyMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (compact) space.xxs else space.sm, top = space.xs),
        )
    }
}

/** A marca do kit, com o respiro da barra; no trilho estreito, só o "3D". */
@Composable
private fun Brand(compact: Boolean) {
    val space = MaterialTheme.spacing
    BrandMark(compact = compact, modifier = Modifier.fillMaxWidth().padding(horizontal = space.sm, vertical = space.xxs))
}

@Composable
private fun GroupTitle(group: DestinationGroup, compact: Boolean) {
    val title = group.title ?: return
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    if (compact) {
        // Sem espaço pro título: uma linha fina separa os grupos no trilho.
        Box(
            Modifier.fillMaxWidth().padding(horizontal = space.sm, vertical = space.sm).height(AppDimens.borderWidth)
                .background(colors.navyRaised),
        )
    } else {
        Text(
            title.uppercase(),
            style = MaterialTheme.appType.overline,
            color = colors.onNavyMuted,
            modifier = Modifier.padding(start = space.sm, end = space.sm, top = space.md, bottom = space.xs),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarItem(
    destination: AppDestination,
    selected: Boolean,
    compact: Boolean,
    status: String?,
    hasPendingChanges: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val raised = selected || hovered
    val textColor = when {
        selected -> Color.White
        hovered -> colors.onNavy
        else -> colors.onNavyMuted
    }
    val item: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.navItemHeight)
                .clip(MaterialTheme.shapes.small)
                .background(if (raised) colors.navyRaised else Color.Transparent)
                .then(if (focused) Modifier.border(AppDimens.focusRingWidth, colors.onNavyMuted, MaterialTheme.shapes.small) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
                .padding(horizontal = space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(space.sm),
        ) {
            Box {
                Icon(
                    if (selected) destination.selectedIcon else destination.icon,
                    contentDescription = if (compact) destination.label else null,
                    tint = if (selected) colors.amber else textColor,
                    modifier = Modifier.size(AppDimens.navIconSize),
                )
                if (hasPendingChanges) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = space.xxs, y = -space.xxs).size(AppDimens.pendingDotSize).clip(CircleShape).background(colors.onNavy))
                }
            }
            if (!compact) {
                Text(
                    destination.label,
                    style = MaterialTheme.appType.body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                status?.let {
                    Text(it, style = MaterialTheme.appType.label, color = colors.onNavyMuted, maxLines = 1)
                }
            }
        }
    }

    if (compact) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(listOfNotNull(destination.label, status).joinToString(" · ")) } },
            state = rememberTooltipState(),
        ) { item() }
    } else {
        item()
    }
}
