package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_backup_action_create
import _3dreport.composeapp.generated.resources.settings_backup_action_restore
import _3dreport.composeapp.generated.resources.settings_backup_auto
import _3dreport.composeapp.generated.resources.settings_backup_auto_title
import _3dreport.composeapp.generated.resources.settings_backup_folder
import _3dreport.composeapp.generated.resources.settings_backup_folder_change
import _3dreport.composeapp.generated.resources.settings_backup_folder_help
import _3dreport.composeapp.generated.resources.settings_backup_folder_open
import _3dreport.composeapp.generated.resources.settings_backup_help
import _3dreport.composeapp.generated.resources.settings_backup_help_detail
import _3dreport.composeapp.generated.resources.settings_backup_last
import _3dreport.composeapp.generated.resources.settings_backup_none
import _3dreport.composeapp.generated.resources.settings_backup_restore_confirm
import _3dreport.composeapp.generated.resources.settings_backup_restore_message
import _3dreport.composeapp.generated.resources.settings_backup_restore_title
import _3dreport.composeapp.generated.resources.settings_backup_restored_close
import _3dreport.composeapp.generated.resources.settings_backup_restored_message
import _3dreport.composeapp.generated.resources.settings_backup_restored_title
import _3dreport.composeapp.generated.resources.settings_backup_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.formatDateTime
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.FieldLabel
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** A seção Backup: o backup de agora, a restauração e o backup automático diário. Valem na hora, sem o "Salvar" da barra. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackupSection(viewModel: BackupViewModel) {
    val state by viewModel.uiState.collectAsState()
    val space = MaterialTheme.spacing

    FormSection(
        1,
        stringResource(Res.string.settings_backup_title),
        help = HelpText(stringResource(Res.string.settings_backup_help), stringResource(Res.string.settings_backup_help_detail)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
            FieldLabel(stringResource(Res.string.settings_backup_last), help = null)
            Text(
                state.lastBackupEpochMillis?.let(::formatDateTime) ?: stringResource(Res.string.settings_backup_none),
                style = MaterialTheme.appType.body,
                color = if (state.lastBackupEpochMillis == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            AppButton(stringResource(Res.string.settings_backup_action_create), viewModel::createBackup, enabled = !state.busy, icon = AppIcons.Save)
            AppButton(stringResource(Res.string.settings_backup_action_restore), viewModel::pickBackupToRestore, enabled = !state.busy, icon = AppIcons.Download)
        }
    }

    FormSection(2, stringResource(Res.string.settings_backup_auto_title)) {
        AppCheckbox(stringResource(Res.string.settings_backup_auto), state.autoBackupEnabled, viewModel::setAutomaticBackup)
        if (state.autoBackupEnabled) {
            Column(
                modifier = Modifier.padding(start = AppDimens.checkboxIndent),
                verticalArrangement = Arrangement.spacedBy(space.xs),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    FieldLabel(stringResource(Res.string.settings_backup_folder), HelpText(stringResource(Res.string.settings_backup_folder_help)))
                    SelectionContainer {
                        Text(state.automaticBackupDirectory, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                val pickerTitle = stringResource(Res.string.settings_backup_folder)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
                    AppButton(stringResource(Res.string.settings_backup_folder_change), onClick = { viewModel.chooseAutomaticBackupDirectory(pickerTitle) })
                    AppButton(stringResource(Res.string.settings_backup_folder_open), viewModel::openAutomaticBackupDirectory)
                }
            }
        }
    }

    state.pathToRestore?.let {
        ConfirmDialog(
            title = stringResource(Res.string.settings_backup_restore_title),
            message = stringResource(Res.string.settings_backup_restore_message, state.fileNameToRestore.orEmpty()),
            confirmLabel = stringResource(Res.string.settings_backup_restore_confirm),
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }

    state.restoredFromPreviousDataAt?.let { previousDataPath ->
        // Sem saída: o app precisa fechar pra carregar os dados restaurados, e clicar fora ou apertar Esc não faz nada.
        AppDialog(
            title = stringResource(Res.string.settings_backup_restored_title),
            onDismiss = {},
            dismissOnClickOutside = false,
            buttons = { AppButton(stringResource(Res.string.settings_backup_restored_close), viewModel::closeApp, kind = com.threedreport.app.ui.components.ButtonKind.Primary) },
        ) {
            DialogText(stringResource(Res.string.settings_backup_restored_message))
            SelectionContainer { DialogText(previousDataPath, muted = true) }
        }
    }
}
