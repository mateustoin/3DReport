package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Conteúdo de botão com ícone à esquerda do texto, no tamanho e espaçamento que o Material 3 usa
 * em botões com ícone (18 dp + 8 dp). Vai dentro de qualquer `Button`/`OutlinedButton`/
 * `TextButton`, que continuam cuidando da cor. O texto nunca sai (decisão 87): nenhum botão do app
 * é só ícone.
 */
@Composable
fun IconLabel(icon: ImageVector, text: String, color: Color = Color.Unspecified) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (color == Color.Unspecified) LocalContentColor.current else color)
    Spacer(Modifier.width(8.dp))
    Text(text, color = color)
}
