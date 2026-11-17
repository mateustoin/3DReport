package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Uma área de soltar do aviso de arrastar arquivo: um retângulo com o que vai acontecer se o arquivo for solto nele. Quem
 * desenha o aviso diz qual zona está sob o mouse ([highlighted]) e recebe em [onPlaced] onde ela está na janela, pra saber
 * em qual o arquivo foi solto.
 *
 * Em cima, a zona ganha fundo `tint`, borda de 2 em `primary` (no lugar da de 1 em `line`) e um ícone de soltar antes do
 * texto, então o estado não depende só da cor. A borda é desenhada sem mexer nas medidas: a zona não pula.
 */
@Composable
fun DropZone(text: String, highlighted: Boolean, onPlaced: (Rect) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val shape = MaterialTheme.shapes.large
    val contentColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .widthIn(min = AppDimens.dropZoneMinWidth)
            .onGloballyPositioned { onPlaced(it.boundsInRoot()) }
            .background(if (highlighted) colors.tint else colors.column, shape)
            .border(
                if (highlighted) AppDimens.focusRingWidth else AppDimens.borderWidth,
                if (highlighted) MaterialTheme.colorScheme.primary else colors.line,
                shape,
            )
            .padding(horizontal = space.xl, vertical = space.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
            if (highlighted) Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(AppDimens.iconSize), tint = contentColor)
            Text(text, style = MaterialTheme.appType.bodyStrong, color = contentColor)
        }
    }
}
