package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.history_menu_back
import _3dreport.composeapp.generated.resources.history_menu_convert
import _3dreport.composeapp.generated.resources.history_menu_copy_pix
import _3dreport.composeapp.generated.resources.history_menu_copy_text
import _3dreport.composeapp.generated.resources.history_menu_copy_to_catalog
import _3dreport.composeapp.generated.resources.history_menu_deadline_change
import _3dreport.composeapp.generated.resources.history_menu_deadline_set
import _3dreport.composeapp.generated.resources.history_menu_download_model
import _3dreport.composeapp.generated.resources.history_menu_download_photo
import _3dreport.composeapp.generated.resources.history_menu_duplicate
import _3dreport.composeapp.generated.resources.history_menu_edit_calculation
import _3dreport.composeapp.generated.resources.history_menu_edit_details
import _3dreport.composeapp.generated.resources.history_menu_export_pdf
import _3dreport.composeapp.generated.resources.history_menu_follow_up
import _3dreport.composeapp.generated.resources.history_menu_group_files
import _3dreport.composeapp.generated.resources.history_menu_group_organize
import _3dreport.composeapp.generated.resources.history_menu_group_send
import _3dreport.composeapp.generated.resources.history_menu_mark_cancelled
import _3dreport.composeapp.generated.resources.history_menu_message
import _3dreport.composeapp.generated.resources.history_menu_more
import _3dreport.composeapp.generated.resources.history_menu_move_to
import _3dreport.composeapp.generated.resources.history_menu_payments
import _3dreport.composeapp.generated.resources.history_menu_print_settings
import _3dreport.composeapp.generated.resources.history_menu_reprice
import _3dreport.composeapp.generated.resources.history_menu_save_image
import _3dreport.composeapp.generated.resources.history_menu_select
import _3dreport.composeapp.generated.resources.history_menu_sell
import _3dreport.composeapp.generated.resources.history_menu_status_current
import _3dreport.composeapp.generated.resources.history_menu_tracking_copy
import _3dreport.composeapp.generated.resources.history_menu_tracking_open
import _3dreport.composeapp.generated.resources.history_menu_whatsapp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppMenu
import com.threedreport.app.ui.components.AppMenuDivider
import com.threedreport.app.ui.components.AppMenuGroup
import com.threedreport.app.ui.components.AppMenuItem
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.statusColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.stringResource

/**
 * O botão de ícone "Mais ações" e o menu dele, agrupado em Enviar, Arquivos, Organizar e Excluir (decisão 108). "Mover
 * para" troca o conteúdo do menu pela lista de situações, em vez de abrir um segundo menu por cima: o submenu de verdade
 * some quando o mouse escorrega meio centímetro. As ações que não se aplicam ([QuoteActions] `null`) não aparecem.
 */
@Composable
fun QuoteActionsMenu(savedQuote: SavedQuote, actions: QuoteActions, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var choosingStatus by remember { mutableStateOf(false) }
    val close = {
        expanded = false
        choosingStatus = false
    }

    Box(modifier) {
        IconAction(AppIcons.MoreVert, stringResource(Res.string.history_menu_more), onClick = { expanded = true })
        AppMenu(expanded = expanded, onDismiss = close) {
            val onStatusChange = actions.onStatusChange
            if (choosingStatus && onStatusChange != null) {
                AppMenuItem(stringResource(Res.string.history_menu_back), { choosingStatus = false }, AppIcons.ArrowBack)
                AppMenuDivider()
                OrderStatus.entries.forEach { status ->
                    val name = stringResource(status.labelRes())
                    AppMenuItem(
                        text = if (status == savedQuote.status) stringResource(Res.string.history_menu_status_current, name) else name,
                        onClick = {
                            close()
                            onStatusChange(status)
                        },
                        enabled = status != savedQuote.status,
                    ) { Box(Modifier.size(AppDimens.legendSwatchSize).background(status.statusColor(), CircleShape)) }
                }
                return@AppMenu
            }
            StaleGroup(actions, close)
            SendGroup(actions, close)
            FilesGroup(actions, close)
            OrganizeGroup(savedQuote, actions, close, onChooseStatus = { choosingStatus = true })
            AppMenuDivider()
            AppMenuItem(stringResource(Res.string.common_delete), { close(); actions.onDelete() }, AppIcons.Delete, danger = true)
        }
    }
}

@Composable
private fun SendGroup(actions: QuoteActions, close: () -> Unit) {
    AppMenuGroup(stringResource(Res.string.history_menu_group_send))
    AppMenuItem(stringResource(Res.string.history_menu_export_pdf), { close(); actions.onExportPdf() }, AppIcons.PictureAsPdf)
    AppMenuItem(stringResource(Res.string.history_menu_whatsapp), { close(); actions.onOpenWhatsApp() }, AppIcons.Chat)
    AppMenuItem(stringResource(Res.string.history_menu_save_image), { close(); actions.onSaveImage() }, AppIcons.AddPhotoAlternate)
    AppMenuItem(stringResource(Res.string.history_menu_copy_text), { close(); actions.onCopy() }, AppIcons.ContentCopy)
    actions.onMessage?.let { AppMenuItem(stringResource(Res.string.history_menu_message), { close(); it() }, AppIcons.Chat) }
    actions.onCopyPix?.let { AppMenuItem(stringResource(Res.string.history_menu_copy_pix), { close(); it() }, AppIcons.Payments) }
}

@Composable
private fun FilesGroup(actions: QuoteActions, close: () -> Unit) {
    if (actions.onDownloadPhoto == null && actions.onDownloadModelFile == null && actions.onPrintSettings == null) return
    AppMenuDivider()
    AppMenuGroup(stringResource(Res.string.history_menu_group_files))
    actions.onDownloadPhoto?.let { AppMenuItem(stringResource(Res.string.history_menu_download_photo), { close(); it() }, AppIcons.Image) }
    actions.onDownloadModelFile?.let { AppMenuItem(stringResource(Res.string.history_menu_download_model), { close(); it() }, AppIcons.Download) }
    actions.onPrintSettings?.let { onPrintSettings ->
        AppMenuItem(stringResource(Res.string.history_menu_print_settings), { close(); onPrintSettings() }, AppIcons.Tune)
    }
}

@Composable
private fun OrganizeGroup(savedQuote: SavedQuote, actions: QuoteActions, close: () -> Unit, onChooseStatus: () -> Unit) {
    AppMenuDivider()
    AppMenuGroup(stringResource(Res.string.history_menu_group_organize))
    actions.onSell?.let { AppMenuItem(stringResource(Res.string.history_menu_sell), { close(); it() }, AppIcons.Sell) }
    actions.onPayments?.let { AppMenuItem(stringResource(Res.string.history_menu_payments), { close(); it() }, AppIcons.Payments) }
    actions.onOpenTracking?.let { AppMenuItem(stringResource(Res.string.history_menu_tracking_open), { close(); it() }, AppIcons.OpenInNew) }
    actions.onCopyTracking?.let { AppMenuItem(stringResource(Res.string.history_menu_tracking_copy), { close(); it() }, AppIcons.ContentCopy) }
    AppMenuItem(stringResource(Res.string.history_menu_edit_details), { close(); actions.onEditDetails() }, AppIcons.Edit)
    AppMenuItem(stringResource(Res.string.history_menu_edit_calculation), { close(); actions.onEdit() }, AppIcons.Calculate)
    AppMenuItem(stringResource(Res.string.history_menu_duplicate), { close(); actions.onDuplicate() }, AppIcons.FileCopy)
    if (actions.onStatusChange != null) AppMenuItem(stringResource(Res.string.history_menu_move_to), onChooseStatus, AppIcons.ViewKanban)
    actions.onEditDeliveryDate?.let {
        val label = if (savedQuote.deliveryDateEpochDay == null) Res.string.history_menu_deadline_set else Res.string.history_menu_deadline_change
        AppMenuItem(stringResource(label), { close(); it() }, AppIcons.Event)
    }
    actions.onCopyToCatalog?.let { AppMenuItem(stringResource(Res.string.history_menu_copy_to_catalog), { close(); it() }, AppIcons.Storefront) }
    actions.onReprice?.let { AppMenuItem(stringResource(Res.string.history_menu_reprice), { close(); it() }, AppIcons.Sync) }
    actions.onConvertToOrder?.let { AppMenuItem(stringResource(Res.string.history_menu_convert), { close(); it() }, AppIcons.RequestQuote) }
    actions.onSelect?.let { AppMenuItem(stringResource(Res.string.history_menu_select), { close(); it() }, AppIcons.Check) }
}

/** O alto do menu de um orçamento parado: retomar a conversa ou desistir dele (decisão 149). Sem as duas ações, nada aparece. */
@Composable
private fun StaleGroup(actions: QuoteActions, close: () -> Unit) {
    if (actions.onFollowUp == null && actions.onMarkCancelled == null) return
    actions.onFollowUp?.let { AppMenuItem(stringResource(Res.string.history_menu_follow_up), { close(); it() }, AppIcons.Chat) }
    actions.onMarkCancelled?.let { AppMenuItem(stringResource(Res.string.history_menu_mark_cancelled), { close(); it() }, AppIcons.Close) }
    AppMenuDivider()
}
