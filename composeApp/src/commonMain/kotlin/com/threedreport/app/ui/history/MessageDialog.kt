package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.message_action_copy
import _3dreport.composeapp.generated.resources.message_action_whatsapp
import _3dreport.composeapp.generated.resources.message_field_kind
import _3dreport.composeapp.generated.resources.message_field_text
import _3dreport.composeapp.generated.resources.message_include_pix
import _3dreport.composeapp.generated.resources.message_title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppTextArea
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.appType
import com.threedreport.core.message.MessageTemplateKind
import org.jetbrains.compose.resources.stringResource

/**
 * "Mensagem pro cliente" (decisão 148, item 23): o seletor de modelo, o texto já preenchido e editável, e os botões
 * "Copiar" e "Abrir no WhatsApp" (a ação principal). Não fecha com clique fora: o texto editado se perderia.
 */
@Composable
fun MessageDialog(editing: MessageEditing, viewModel: QuoteHistoryViewModel) {
    val labels = MessageTemplateKind.entries.associateWith { stringResource(it.labelRes()) }
    AppDialog(
        title = stringResource(Res.string.message_title),
        onDismiss = viewModel::closeMessage,
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_close), viewModel::closeMessage)
            AppButton(stringResource(Res.string.message_action_copy), viewModel::copyMessage, icon = AppIcons.ContentCopy)
            AppButton(stringResource(Res.string.message_action_whatsapp), viewModel::sendMessageOnWhatsApp, kind = ButtonKind.Primary, icon = AppIcons.Chat)
        },
    ) {
        AppDropdownField(
            label = stringResource(Res.string.message_field_kind),
            items = MessageTemplateKind.entries,
            selected = editing.kind,
            itemLabel = { labels.getValue(it) },
            displayText = { labels.getValue(it) },
            onSelect = viewModel::changeMessageKind,
        )
        AppTextArea(
            label = stringResource(Res.string.message_field_text),
            value = editing.text,
            onValueChange = viewModel::editMessage,
            minLines = 6,
            maxLines = 14,
        )
        // O Pix fica fora do texto editável: só leitura, embaixo, e vai no fim (sozinho na última linha) quando marcado.
        editing.pix?.let { pix ->
            AppCheckbox(stringResource(Res.string.message_include_pix), editing.includePix, viewModel::setIncludePix)
            if (editing.includePix) {
                Text(pix.header, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pix.code, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
