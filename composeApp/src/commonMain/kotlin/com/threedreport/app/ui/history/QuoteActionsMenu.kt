package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.AppMenu
import com.threedreport.app.ui.components.AppMenuGroup
import com.threedreport.app.ui.components.AppMenuItem
import com.threedreport.app.ui.components.IconLabel
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.progressColor
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote

/**
 * Tudo o que dá pra fazer com um orçamento salvo. Um objeto só, montado pela tela, pra lista e Kanban
 * oferecerem exatamente as mesmas ações (antes cada um tinha a sua lista, e o Kanban não tinha metade).
 * Ação `null` não se aplica a este orçamento e some do menu.
 */
class QuoteActions(
    val onEdit: () -> Unit,
    val onEditDetails: () -> Unit,
    val onDuplicate: () -> Unit,
    val onSell: (() -> Unit)?,
    val onCopyToCatalog: (() -> Unit)?,
    val onConvertToOrder: (() -> Unit)?,
    val onReprice: (() -> Unit)?,
    val onStatusChange: ((OrderStatus) -> Unit)?,
    val onEditDeliveryDate: (() -> Unit)?,
    /** `null` com várias impressões: cada uma tem as suas, e elas se editam no Orçamento (decisão 114). */
    val onPrintSettings: (() -> Unit)?,
    val onDownloadPhoto: (() -> Unit)?,
    val onDownloadStl: (() -> Unit)?,
    val onExportPdf: () -> Unit,
    val onCopy: () -> Unit,
    val onOpenWhatsApp: () -> Unit,
    val onSaveImage: () -> Unit,
    val onDelete: () -> Unit,
)

/**
 * O botão "Ações" e o menu, agrupado em Enviar / Arquivos / Organizar / Excluir (decisão 108). "Mover
 * para" troca o conteúdo do menu pela lista de status, em vez de abrir um segundo menu por cima: o
 * submenu de verdade some quando o mouse escorrega meio centímetro.
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
        TextButton(onClick = { expanded = true }) { IconLabel(AppIcons.MoreVert, "Ações") }
        AppMenu(expanded = expanded, onDismiss = close) {
            val onStatusChange = actions.onStatusChange
            if (choosingStatus && onStatusChange != null) {
                AppMenuItem("Voltar", { choosingStatus = false }, AppIcons.Close)
                HorizontalDivider()
                OrderStatus.entries.forEach { status ->
                    AppMenuItem(
                        text = if (status == savedQuote.status) "${status.label} (atual)" else status.label,
                        onClick = {
                            close()
                            onStatusChange(status)
                        },
                        enabled = status != savedQuote.status,
                    ) { Box(Modifier.size(10.dp).background(status.progressColor(), CircleShape)) }
                }
                return@AppMenu
            }

            AppMenuGroup("Enviar pro cliente")
            AppMenuItem("Exportar PDF", { close(); actions.onExportPdf() }, AppIcons.PictureAsPdf)
            AppMenuItem("Enviar texto no WhatsApp", { close(); actions.onOpenWhatsApp() }, AppIcons.Chat)
            AppMenuItem("Salvar imagem pro WhatsApp", { close(); actions.onSaveImage() }, AppIcons.AddPhotoAlternate)
            AppMenuItem("Copiar texto", { close(); actions.onCopy() }, AppIcons.ContentCopy)

            HorizontalDivider()
            AppMenuGroup("Arquivos")
            actions.onDownloadPhoto?.let { AppMenuItem("Baixar foto", { close(); it() }, AppIcons.Image) }
            actions.onDownloadStl?.let { AppMenuItem("Baixar STL", { close(); it() }, AppIcons.Download) }
            actions.onPrintSettings?.let { onPrintSettings ->
                AppMenuItem(
                    if (savedQuote.printSettings == null) "Adicionar configurações de impressão" else "Configurações de impressão",
                    { close(); onPrintSettings() },
                    AppIcons.Tune,
                )
            }

            HorizontalDivider()
            AppMenuGroup("Organizar")
            AppMenuItem("Editar detalhes (nome, cliente, foto)", { close(); actions.onEditDetails() }, AppIcons.Edit)
            AppMenuItem("Editar cálculo", { close(); actions.onEdit() }, AppIcons.Calculate)
            AppMenuItem("Duplicar", { close(); actions.onDuplicate() }, AppIcons.FileCopy)
            if (onStatusChange != null) {
                AppMenuItem("Mover para…", { choosingStatus = true }, AppIcons.ViewKanban)
            }
            actions.onEditDeliveryDate?.let {
                AppMenuItem(if (savedQuote.deliveryDateEpochDay == null) "Definir prazo de entrega" else "Alterar prazo de entrega", { close(); it() }, AppIcons.Event)
            }
            actions.onCopyToCatalog?.let { AppMenuItem("Guardar no catálogo", { close(); it() }, AppIcons.Storefront) }
            actions.onReprice?.let { AppMenuItem("Atualizar preço", { close(); it() }, AppIcons.Sync) }
            actions.onConvertToOrder?.let { AppMenuItem("Transformar em pedido", { close(); it() }, AppIcons.RequestQuote) }

            HorizontalDivider()
            AppMenuItem("Excluir", { close(); actions.onDelete() }, AppIcons.Delete, danger = true)
        }
    }
}
