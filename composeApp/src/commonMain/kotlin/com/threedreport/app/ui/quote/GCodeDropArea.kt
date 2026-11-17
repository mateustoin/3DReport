package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_drop_many
import _3dreport.composeapp.generated.resources.quote_drop_one
import _3dreport.composeapp.generated.resources.quote_drop_pick
import _3dreport.composeapp.generated.resources.quote_gcode_hint_label
import _3dreport.composeapp.generated.resources.quote_gcode_undo
import _3dreport.composeapp.generated.resources.quote_help_gcode
import _3dreport.composeapp.generated.resources.quote_help_gcode_multi
import _3dreport.composeapp.generated.resources.quote_reading
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.clickableAction
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * O atalho principal da tela (decisão 108): arrastar o G-code ou escolher o arquivo. Vem primeiro,
 * antes de filamento e impressora, porque é ele que preenche os dois. A explicação do que o arquivo
 * preenche fica numa dica, e o resultado da leitura, numa linha embaixo.
 */
@Composable
internal fun GCodeDropArea(viewModel: QuoteViewModel, input: QuoteInputState, importing: Boolean) {
    val space = MaterialTheme.spacing
    val multi = input.prints.size > 1
    val single = input.prints.singleOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        DropArea(
            importing = importing,
            multi = multi,
            onPick = { if (multi) viewModel.pickGCodeAsNewPrint() else viewModel.pickAndImportGCode(single?.id) },
        )
        single?.gcodeImport?.let { Text(it.asText(), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (single?.beforeGCode != null) {
            AppButton(stringResource(Res.string.quote_gcode_undo), onClick = { viewModel.undoGCodeImport(single.id) })
        }
    }
}

@Composable
private fun DropArea(importing: Boolean, multi: Boolean, onPick: () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val shape = MaterialTheme.shapes.medium
    val dash = AppDimens.dashedBorderWidth
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.dropZoneHeight)
            .clip(shape)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, colors.line, style = Stroke(width = dash.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            .clickableAction(onClick = onPick, enabled = !importing, shape = shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs, Alignment.CenterHorizontally),
    ) {
        if (importing) {
            CircularProgressIndicator(modifier = Modifier.size(AppDimens.iconSize), strokeWidth = AppDimens.focusRingWidth)
            Text(stringResource(Res.string.quote_reading), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val prefix = stringResource(if (multi) Res.string.quote_drop_many else Res.string.quote_drop_one)
            val pick = stringResource(Res.string.quote_drop_pick)
            val pickColor = MaterialTheme.colorScheme.primary
            Text(
                buildAnnotatedString {
                    append("$prefix ")
                    withStyle(SpanStyle(color = pickColor, fontWeight = FontWeight.SemiBold)) { append(pick) }
                },
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HelpHint(
                HelpText(
                    stringResource(Res.string.quote_help_gcode),
                    if (multi) stringResource(Res.string.quote_help_gcode_multi) else null,
                ),
                label = stringResource(Res.string.quote_gcode_hint_label),
            )
        }
    }
}

/** A miniatura que o fatiador gravou no G-code, pra reconhecer a mesa de relance. */
@Composable
internal fun GCodeThumbnail(bytes: ByteArray) {
    val size = AppDimens.printThumbnailSize
    val bitmap = remember(bytes) { runCatching { decodeImageBitmap(bytes) }.getOrNull() } ?: return
    Box(modifier = Modifier.size(size).clip(MaterialTheme.shapes.extraSmall)) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
    }
}
