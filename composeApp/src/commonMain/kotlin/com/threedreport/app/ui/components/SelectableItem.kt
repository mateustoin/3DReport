package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Item de uma lista de opções ao lado do conteúdo (as seções de Configurações): 36 de altura, raio 8, texto em `body`.
 * Selecionado: fundo `tint` e texto `primary` em 600; com o mouse em cima, fundo `column`. [note] é um texto curto à
 * direita ("Não salvo"), em `label` e na cor de âmbar de texto, pra o aviso não depender só da cor. Foco no teclado:
 * anel de 2.
 */
@Composable
fun SelectableItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, note: String? = null) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.navItemHeight)
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    selected -> colors.tint
                    hovered -> colors.column
                    else -> Color.Transparent
                },
            )
            .then(if (focused) Modifier.border(AppDimens.focusRingWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Text(
            label,
            style = MaterialTheme.appType.body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (note != null) {
            Text(note, style = MaterialTheme.appType.label, color = colors.amberText, maxLines = 1)
        }
    }
}
