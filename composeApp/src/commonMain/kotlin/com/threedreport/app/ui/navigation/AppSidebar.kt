package com.threedreport.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Largura da barra com rótulos e do trilho só com ícones. */
val SIDEBAR_WIDTH = 208.dp
val SIDEBAR_COMPACT_WIDTH = 72.dp

/**
 * Janela mais estreita que isso fica com o trilho só de ícones: com a barra inteira, o Orçamento perdia
 * as duas colunas (decisão 111).
 */
val SIDEBAR_LABELS_MIN_WINDOW_WIDTH = 1280.dp

/**
 * Barra lateral agrupada (decisão 111). Vendas e Cadastros em cima; Configurações e Sobre no pé, com a
 * versão embaixo. [compact] tira os rótulos e deixa o nome de cada item numa dica ao passar o mouse.
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
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) SIDEBAR_COMPACT_WIDTH else SIDEBAR_WIDTH)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            listOf(DestinationGroup.SALES, DestinationGroup.REGISTRIES).forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(16.dp))
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (compact) 4.dp else 12.dp, top = 8.dp),
        )
    }
}

@Composable
private fun GroupTitle(group: DestinationGroup, compact: Boolean) {
    val title = group.title ?: return
    if (compact) {
        // Sem espaço pro título: uma linha fina separa os grupos no trilho.
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    } else {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
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
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    val item: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) colors.secondaryContainer else colors.surfaceContainerLow)
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(12.dp),
        ) {
            Box {
                Icon(
                    if (selected) destination.selectedIcon else destination.icon,
                    contentDescription = if (compact) destination.label else null,
                    tint = content,
                    modifier = Modifier.size(22.dp),
                )
                if (hasPendingChanges) {
                    Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(colors.tertiary))
                }
            }
            if (!compact) {
                Text(
                    destination.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                status?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = colors.tertiary, maxLines = 1)
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
