package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_negotiate
import _3dreport.composeapp.generated.resources.quote_negotiate_clear
import _3dreport.composeapp.generated.resources.quote_negotiate_field
import _3dreport.composeapp.generated.resources.quote_send
import _3dreport.composeapp.generated.resources.quote_send_image
import _3dreport.composeapp.generated.resources.quote_send_pdf
import _3dreport.composeapp.generated.resources.quote_send_whatsapp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * O que "Enviar pro cliente" faz (decisão 131): [onSend] salva o orçamento e devolve se o menu pode abrir (falso
 * quando o salvar foi bloqueado); os outros três entregam o pedido salvo.
 */
internal class ClientSendActions(
    val onSend: () -> Boolean,
    val onPdf: () -> Unit,
    val onImage: () -> Unit,
    val onWhatsApp: () -> Unit,
    val exporting: Boolean,
)

/**
 * O pé do painel de resultado: o botão principal, "Negociar" e "Enviar pro cliente" lado a lado, e o texto de
 * desistir de uma operação. "Negociar" abre o preço combinado, o mesmo valor do campo da seção Venda
 * ([onTargetTotalChange]); os dois botões não valem pro produto do catálogo, que não tem cliente.
 */
@Composable
internal fun ResultFooter(
    result: QuoteResult,
    input: QuoteInputState,
    canSave: Boolean,
    cannotSaveReason: String,
    saveLabel: String,
    cancelLabel: String?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onTargetTotalChange: (String) -> Unit,
    negotiating: Boolean,
    onNegotiatingChange: (Boolean) -> Unit,
    send: ClientSendActions,
) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        if (!canSave) Text(cannotSaveReason, style = MaterialTheme.appType.caption, color = colors.onNavyMuted)
        if (negotiating && !input.isProduct) {
            NegotiationField(
                value = input.targetTotalText,
                error = result.errorText(QuoteFields.TARGET),
                onChange = onTargetTotalChange,
                onClose = { onNegotiatingChange(false) },
            )
        }
        AppButton(
            saveLabel,
            onClick = onSave,
            kind = ButtonKind.Primary,
            enabled = canSave,
            height = AppDimens.primaryActionHeight,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!input.isProduct) {
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                AppButton(
                    stringResource(Res.string.quote_negotiate),
                    onClick = { onNegotiatingChange(!negotiating) },
                    kind = ButtonKind.OnNavy,
                    // Com um preço combinado digitado, a conta pode estar sem preço por causa dele: o campo precisa continuar abrindo.
                    enabled = result.quote != null || input.targetTotalText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                SendButton(send, Modifier.weight(1f))
            }
        }
        if (cancelLabel != null) {
            AppButton(cancelLabel, onClick = onCancel, kind = ButtonKind.OnNavy, icon = AppIcons.Close, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** O campo "Preço combinado": o preço, o lucro e o mínimo do painel mudam enquanto se digita. Esc fecha. */
@Composable
private fun NegotiationField(value: String, error: String?, onChange: (String) -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.appColors
    AppTextField(
        label = stringResource(Res.string.quote_negotiate_field),
        value = value,
        onValueChange = onChange,
        error = error,
        onNavy = true,
        autoFocus = true,
        modifier = Modifier.onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                onClose()
                true
            } else {
                false
            }
        },
        trailing = if (value.isBlank()) {
            null
        } else {
            {
                Text(
                    stringResource(Res.string.quote_negotiate_clear),
                    style = MaterialTheme.appType.label,
                    color = colors.onNavyMuted,
                    modifier = Modifier.clickable(role = Role.Button) { onChange("") },
                )
            }
        },
    )
}

/** "Enviar pro cliente": salva como pedido e abre PDF, imagem e texto no WhatsApp, os mesmos de Pedidos. */
@Composable
private fun SendButton(send: ClientSendActions, modifier: Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val close = { open = false }
    Box(modifier) {
        AppButton(
            stringResource(Res.string.quote_send),
            onClick = { open = send.onSend() },
            kind = ButtonKind.OnNavy,
            enabled = !send.exporting,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = open, onDismissRequest = close) {
            SendItem(stringResource(Res.string.quote_send_whatsapp), AppIcons.Chat) { close(); send.onWhatsApp() }
            SendItem(stringResource(Res.string.quote_send_image), AppIcons.AddPhotoAlternate) { close(); send.onImage() }
            SendItem(stringResource(Res.string.quote_send_pdf), AppIcons.PictureAsPdf) { close(); send.onPdf() }
        }
    }
}

@Composable
private fun SendItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
