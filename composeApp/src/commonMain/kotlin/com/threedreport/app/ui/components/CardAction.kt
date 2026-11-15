package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors

/**
 * O que um cartão clicável sabe de si: o mouse em cima, o foco do próprio cartão (o Tab chegou nele) e o foco em algo
 * dentro dele (o botão do menu, por exemplo). [active] junta o mouse e o foco dentro, e é o que mostra o menu de ações
 * do cartão. Criado por [rememberCardState] e passado a [cardFrame] e [clickableCard].
 */
@Stable
class CardState internal constructor(internal val interaction: MutableInteractionSource) {
    var hovered by mutableStateOf(false)
        internal set
    var focused by mutableStateOf(false)
        internal set
    var focusWithin by mutableStateOf(false)
        internal set

    val active: Boolean get() = hovered || focusWithin
}

@Composable
fun rememberCardState(): CardState {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val state = remember { CardState(interaction) }
    state.hovered = hovered
    return state
}

/**
 * A moldura do cartão do kit (de pedido no kanban, de produto na grade): raio 10, fundo `card` (`tint` quando
 * [selected]) e borda `line`, que vira `primary` com o mouse em cima, o foco ou a seleção, e de 2 de espessura quando o
 * foco é do próprio cartão. [highlighted] força a borda `primary` (o cartão sendo arrastado).
 */
@Composable
fun Modifier.cardFrame(state: CardState, selected: Boolean = false, highlighted: Boolean = false): Modifier {
    val colors = MaterialTheme.appColors
    val shape = MaterialTheme.shapes.medium
    return this
        .clip(shape)
        .background(if (selected) colors.tint else colors.card)
        .border(
            if (state.focused) AppDimens.focusRingWidth else AppDimens.borderWidth,
            if (state.focused || state.active || selected || highlighted) MaterialTheme.colorScheme.primary else colors.line,
            shape,
        )
        .onFocusChanged { state.focusWithin = it.hasFocus }
        .hoverable(state.interaction)
}

/**
 * A área clicável do conteúdo de um cartão: variante de [clickableAction] pro cartão, cujo anel de foco é a borda de
 * [cardFrame] (e não um anel por dentro). Clica com [role] e conta o foco em [CardState.focused]. Use no lugar de
 * `Modifier.clickable` solto.
 */
fun Modifier.clickableCard(state: CardState, onClick: () -> Unit, role: Role = Role.Button): Modifier = this
    .onFocusChanged { state.focused = it.isFocused }
    .clickable(interactionSource = state.interaction, indication = null, role = role, onClick = onClick)
