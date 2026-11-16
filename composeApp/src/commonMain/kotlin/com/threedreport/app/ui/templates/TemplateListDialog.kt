package com.threedreport.app.ui.templates

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.settings_docs_template_save
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
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.QuoteTemplate
import org.jetbrains.compose.resources.stringResource

/**
 * Diálogo com a lista de modelos salvos, aberto a partir de Configurações, Documentos pro cliente, que continua sendo o
 * único editor dessa config (ver [TemplateListViewModel]). Clicar na linha não faz nada: "Carregar" (no hover ou no foco)
 * entrega o modelo a [onLoad], que o põe no rascunho, e fecha o diálogo. Sem modelos, [onSaveAsTemplate] leva pra
 * "Salvar como modelo".
 */
@Composable
fun TemplateListDialog(
    viewModel: TemplateListViewModel,
    onLoad: (QuoteTemplate) -> Unit,
    onSaveAsTemplate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    AppDialog(
        title = stringResource(Res.string.templates_title),
        onDismiss = onDismiss,
        help = HelpText(stringResource(Res.string.templates_help)),
        buttons = { AppButton(stringResource(Res.string.common_close), onDismiss) },
    ) {
        if (state.rows.isEmpty()) {
            EmptyState(
                message = stringResource(Res.string.templates_empty),
                actionLabel = stringResource(Res.string.settings_docs_template_save),
                onAction = onSaveAsTemplate,
            )
        }
        state.rows.forEach { row ->
            key(row.template.id) {
                TemplateRow(row, onLoad = { onLoad(row.template); onDismiss() }, onDelete = { viewModel.requestDelete(row.template.id) })
            }
        }
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
        actions = listOf(
            RowAction(AppIcons.Download, stringResource(Res.string.templates_action_load), onLoad),
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), onDelete, danger = true),
        ),
    )
}
