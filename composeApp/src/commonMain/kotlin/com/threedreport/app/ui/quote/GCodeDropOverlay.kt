package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.app_drop_add
import _3dreport.composeapp.generated.resources.app_drop_help_empty
import _3dreport.composeapp.generated.resources.app_drop_replace
import _3dreport.composeapp.generated.resources.app_drop_title_empty
import _3dreport.composeapp.generated.resources.app_drop_title_filled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.threedreport.app.ui.components.DropZone
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** Onde um G-code pode ser solto quando o pedido já tem impressão preenchida (decisão 114). */
internal sealed interface GCodeDropTarget {
    data class Replace(val printId: Int) : GCodeDropTarget

    data object AddNew : GCodeDropTarget
}

/** Opacidade do véu do aviso: a tela de baixo continua aparecendo, mas o texto do aviso lê bem. */
private const val SCRIM_ALPHA = 0.92f

/**
 * Aviso por cima da janela enquanto um arquivo é arrastado: diz onde soltar e o que vai acontecer.
 * Sem ele, arrastar um G-code pra janela não dá nenhum sinal de que funciona até soltar.
 *
 * Com o pedido ainda em branco, a janela inteira é um lugar só. Com alguma impressão preenchida, o aviso se
 * divide: uma [DropZone] pra substituir cada impressão e outra pra adicionar o arquivo como impressão nova. Vários
 * arquivos soltos fora das áreas viram uma impressão cada.
 */
@Composable
internal fun GCodeDropOverlay(
    prints: List<PrintInput>,
    hovered: () -> GCodeDropTarget?,
    onZonePlaced: (GCodeDropTarget, Rect) -> Unit,
    onDispose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) { onDispose { onDispose() } }
    val space = MaterialTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = SCRIM_ALPHA))
            .padding(space.xxl)
            .border(AppDimens.focusRingWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
            .padding(space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(space.sm)) {
            Icon(AppIcons.RequestQuote, contentDescription = null, modifier = Modifier.size(AppDimens.dropOverlayIconSize), tint = MaterialTheme.colorScheme.primary)
            if (prints.none { !it.isBlank }) {
                Text(stringResource(Res.string.app_drop_title_empty), style = MaterialTheme.appType.panelTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(Res.string.app_drop_help_empty), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Text(stringResource(Res.string.app_drop_title_filled), style = MaterialTheme.appType.panelTitle, color = MaterialTheme.colorScheme.onSurface)
            prints.forEachIndexed { index, print ->
                val target = GCodeDropTarget.Replace(print.id)
                DropZone(
                    text = stringResource(Res.string.app_drop_replace, printTitle(index + 1, print.name).replaceFirstChar { it.lowercase() }),
                    highlighted = hovered() == target,
                    onPlaced = { onZonePlaced(target, it) },
                )
            }
            DropZone(
                text = stringResource(Res.string.app_drop_add),
                highlighted = hovered() == GCodeDropTarget.AddNew,
                onPlaced = { onZonePlaced(GCodeDropTarget.AddNew, it) },
            )
        }
    }
}
