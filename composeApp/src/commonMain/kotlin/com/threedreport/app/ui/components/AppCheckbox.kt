package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Caixa de marcar do kit, com o [label] ao lado; a linha toda é o alvo do clique e do Tab. Marcada: fundo `primary`
 * com o visto em `onPrimary`. Desmarcada: borda `line` (`primary` ao passar o mouse). No foco, anel de 2.
 * Com [help], uma dica de ajuda ao lado do rótulo. O conteúdo que fica sob a caixa (campos de um serviço marcado) recua [AppDimens.checkboxIndent].
 */
@Composable
fun AppCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, help: HelpText? = null) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (focused) Modifier.border(AppDimens.focusRingWidth, primary, MaterialTheme.shapes.small) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .hoverable(interaction)
            .toggleable(value = checked, interactionSource = interaction, indication = null, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        CheckboxMark(checked, hovered)
        Text(label, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
        if (help != null) HelpHint(help, label)
    }
}

/**
 * Só o quadrado da caixa de marcar, sem rótulo nem clique (o de [AppCheckbox], e o da coluna de seleção de uma tabela,
 * onde a linha inteira é o alvo). Marcada: fundo `primary` com o visto; desmarcada: borda `line`, `primary` com [hovered].
 */
@Composable
fun CheckboxMark(checked: Boolean, hovered: Boolean = false, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val primary = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = modifier
            .size(AppDimens.checkboxSize)
            .background(if (checked) primary else colors.card, shape)
            .border(AppDimens.borderWidth, if (checked || hovered) primary else colors.line, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(AppIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(AppDimens.checkmarkSize))
        }
    }
}
