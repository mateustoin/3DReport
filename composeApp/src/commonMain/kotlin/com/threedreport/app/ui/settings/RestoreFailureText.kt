package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_backup_restore_apply_failed_kept
import _3dreport.composeapp.generated.resources.settings_backup_restore_apply_failed_moved
import _3dreport.composeapp.generated.resources.settings_backup_restore_keep_failed
import _3dreport.composeapp.generated.resources.settings_backup_restore_newer_version
import _3dreport.composeapp.generated.resources.settings_backup_restore_no_data_folder
import _3dreport.composeapp.generated.resources.settings_backup_restore_not_a_backup
import _3dreport.composeapp.generated.resources.settings_backup_restore_old_format
import _3dreport.composeapp.generated.resources.settings_backup_restore_unreadable
import com.threedreport.app.data.RestoreFailure
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText

/** O texto pra quem lê de cada motivo de a restauração falhar, em recurso traduzível. */
fun RestoreFailure.text(): UiText = when (this) {
    RestoreFailure.NoDataFolder -> uiText(Res.string.settings_backup_restore_no_data_folder)
    RestoreFailure.UnreadableFile -> uiText(Res.string.settings_backup_restore_unreadable)
    RestoreFailure.NotABackup -> uiText(Res.string.settings_backup_restore_not_a_backup)
    RestoreFailure.NewerVersion -> uiText(Res.string.settings_backup_restore_newer_version)
    RestoreFailure.OldFormat -> uiText(Res.string.settings_backup_restore_old_format)
    RestoreFailure.CouldNotKeepCurrentData -> uiText(Res.string.settings_backup_restore_keep_failed)
    RestoreFailure.ApplyFailedKept -> uiText(Res.string.settings_backup_restore_apply_failed_kept)
    is RestoreFailure.CurrentDataMoved -> uiText(Res.string.settings_backup_restore_apply_failed_moved, keptAt, dataFolderName)
}
