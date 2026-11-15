package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_delete
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Diálogo do kit: fundo `card`, raio 12, título em `panelTitle`, o [content] e os [buttons] no pé, alinhados à
 * direita (o último é o que confirma). Com [scrollable], o corpo rola (diálogo longo, como a manutenção). Com [dismissOnClickOutside] desligado, só os botões fecham (formulário: perder o que foi digitado por um clique distraído é pior que um clique a mais). [help] vira uma dica ao lado do título, no lugar de um parágrafo no corpo. Base do [ConfirmDialog] e do [DeleteOrArchiveDialog].
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    scrollable: Boolean = false,
    help: HelpText? = null,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val space = MaterialTheme.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = dismissOnClickOutside),
        containerColor = MaterialTheme.appColors.card,
        shape = MaterialTheme.shapes.large,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                Text(title, style = MaterialTheme.appType.panelTitle, color = MaterialTheme.colorScheme.onSurface)
                if (help != null) HelpHint(help, title)
            }
        },
        text = {
            Column(
                modifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                verticalArrangement = Arrangement.spacedBy(space.xs),
                content = content,
            )
        },
        confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(space.xs), content = buttons) },
    )
}

/** Texto do corpo de um diálogo ([AppDialog]). */
@Composable
fun DialogText(text: String, muted: Boolean = false) {
    Text(
        text,
        style = if (muted) MaterialTheme.appType.caption else MaterialTheme.appType.body,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

/** Confirmação antes de uma ação destrutiva (excluir um cadastro, descartar um rascunho): Cancelar e a ação, em vermelho. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(Res.string.common_delete),
    dismissLabel: String = stringResource(Res.string.common_cancel),
) {
    AppDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            AppButton(dismissLabel, onDismiss)
            AppButton(confirmLabel, onConfirm, kind = ButtonKind.Danger)
        },
    ) { DialogText(message) }
}
