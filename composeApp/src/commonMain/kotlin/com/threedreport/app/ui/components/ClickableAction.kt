package com.threedreport.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors

/**
 * Área clicável do kit pra uma ação que é texto ou uma linha (recolher um bloco, abrir "Ver a conta completa", a área
 * de soltar um arquivo): recorta em [shape], clica com [role] e, no foco do teclado, desenha o anel de 2. [onNavy]
 * troca o anel `primary` (que some no azul-marinho) por `onNavy`. Use no lugar de `Modifier.clickable` solto, que não
 * mostra onde o Tab está.
 */
@Composable
fun Modifier.clickableAction(
    onClick: () -> Unit,
    onNavy: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    role: Role = Role.Button,
    enabled: Boolean = true,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val ring = if (onNavy) MaterialTheme.appColors.onNavy else MaterialTheme.colorScheme.primary
    return this
        .clip(shape)
        .then(if (focused) Modifier.border(AppDimens.focusRingWidth, ring, shape) else Modifier)
        .onFocusChanged { focused = it.isFocused }
        .clickable(enabled = enabled, role = role, onClick = onClick)
}
