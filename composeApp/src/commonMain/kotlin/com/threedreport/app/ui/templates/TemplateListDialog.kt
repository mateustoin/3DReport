package com.threedreport.app.ui.templates

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.templates_action_load
import _3dreport.composeapp.generated.resources.templates_active
import _3dreport.composeapp.generated.resources.templates_delete_message
import _3dreport.composeapp.generated.resources.templates_delete_title
import _3dreport.composeapp.generated.resources.templates_empty
import _3dreport.composeapp.generated.resources.templates_help
import _3dreport.composeapp.generated.resources.templates_row_brand
import _3dreport.composeapp.generated.resources.templates_row_no_brand
import _3dreport.composeapp.generated.resources.templates_title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.QuoteTemplate
import org.jetbrains.compose.resources.stringResource

/**
 * Diálogo com a lista de templates salvos (fotos nomeadas da marca d'água e do rodapé), aberto a partir de Configurações,
 * Documentos pro cliente, que continua sendo o único editor ao vivo dessa config (ver [TemplateListViewModel]). Não é uma
 * aba própria: seu único uso é dentro dessa seção de Configurações. Clicar numa linha carrega o template.
 */
@Composable
fun TemplateListDialog(viewModel: TemplateListViewModel, onLoad: (QuoteTemplate) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    AppDialog(
        title = stringResource(Res.string.templates_title),
        onDismiss = onDismiss,
        scrollable = true,
        help = HelpText(stringResource(Res.string.templates_help)),
        buttons = { AppButton(stringResource(Res.string.common_close), onDismiss) },
    ) {
        if (state.rows.isEmpty()) DialogText(stringResource(Res.string.templates_empty), muted = true)
        state.rows.forEach { row -> key(row.template.id) { TemplateRow(row, onLoad = { onLoad(row.template) }, onDelete = { viewModel.requestDelete(row.template.id) }) } }
    }

    state.pendingDelete?.let { template ->
        ConfirmDialog(
            title = stringResource(Res.string.templates_delete_title),
            message = stringResource(Res.string.templates_delete_message, template.name),
            onConfirm = { viewModel.delete(template.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun TemplateRow(row: TemplateRowState, onLoad: () -> Unit, onDelete: () -> Unit) {
    val template = row.template
    ListRow(
        title = template.name,
        details = listOfNotNull(
            stringResource(Res.string.templates_active).takeIf { row.isActive },
            template.brandName?.let { stringResource(Res.string.templates_row_brand, it) } ?: stringResource(Res.string.templates_row_no_brand),
        ),
        selected = row.isActive,
        onClick = onLoad,
        actions = listOf(
            RowAction(AppIcons.Download, stringResource(Res.string.templates_action_load), onLoad),
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), onDelete, danger = true),
        ),
    )
}
