package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        DropdownMenu(expanded = expanded, onDismissRequest = close) {
            val onStatusChange = actions.onStatusChange
            if (choosingStatus && onStatusChange != null) {
                MenuItem("Voltar", AppIcons.Close) { choosingStatus = false }
                HorizontalDivider()
                OrderStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(if (status == savedQuote.status) "${status.label} (atual)" else status.label) },
                        leadingIcon = { Box(Modifier.size(10.dp).background(status.progressColor(), CircleShape)) },
                        enabled = status != savedQuote.status,
                        onClick = {
                            close()
                            onStatusChange(status)
                        },
                    )
                }
                return@DropdownMenu
            }

            MenuGroup("Enviar pro cliente")
            MenuItem("Exportar PDF", AppIcons.PictureAsPdf) { close(); actions.onExportPdf() }
            MenuItem("Enviar texto no WhatsApp", AppIcons.Chat) { close(); actions.onOpenWhatsApp() }
            MenuItem("Salvar imagem pro WhatsApp", AppIcons.AddPhotoAlternate) { close(); actions.onSaveImage() }
            MenuItem("Copiar texto", AppIcons.ContentCopy) { close(); actions.onCopy() }

            HorizontalDivider()
            MenuGroup("Arquivos")
            actions.onDownloadPhoto?.let { MenuItem("Baixar foto", AppIcons.Image) { close(); it() } }
            actions.onDownloadStl?.let { MenuItem("Baixar STL", AppIcons.Download) { close(); it() } }
            actions.onPrintSettings?.let { onPrintSettings ->
                MenuItem(
                    if (savedQuote.printSettings == null) "Adicionar configurações de impressão" else "Configurações de impressão",
                    AppIcons.Tune,
                ) { close(); onPrintSettings() }
            }

            HorizontalDivider()
            MenuGroup("Organizar")
            MenuItem("Editar detalhes (nome, cliente, foto)", AppIcons.Edit) { close(); actions.onEditDetails() }
            MenuItem("Editar cálculo", AppIcons.Calculate) { close(); actions.onEdit() }
            MenuItem("Duplicar", AppIcons.FileCopy) { close(); actions.onDuplicate() }
            if (onStatusChange != null) {
                MenuItem("Mover para…", AppIcons.ViewKanban) { choosingStatus = true }
            }
            actions.onEditDeliveryDate?.let {
                MenuItem(if (savedQuote.deliveryDateEpochDay == null) "Definir prazo de entrega" else "Alterar prazo de entrega", AppIcons.Event) { close(); it() }
            }
            actions.onCopyToCatalog?.let { MenuItem("Guardar no catálogo", AppIcons.Storefront) { close(); it() } }
            actions.onReprice?.let { MenuItem("Atualizar preço", AppIcons.Sync) { close(); it() } }
            actions.onConvertToOrder?.let { MenuItem("Transformar em pedido", AppIcons.RequestQuote) { close(); it() } }

            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(AppIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { close(); actions.onDelete() },
            )
        }
    }
}

@Composable
private fun MenuGroup(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
