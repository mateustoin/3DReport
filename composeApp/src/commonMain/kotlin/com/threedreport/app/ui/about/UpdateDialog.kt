package com.threedreport.app.ui.about

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.about_updates_see_news
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_retry
import _3dreport.composeapp.generated.resources.update_back
import _3dreport.composeapp.generated.resources.update_copy_command
import _3dreport.composeapp.generated.resources.update_downloading
import _3dreport.composeapp.generated.resources.update_error_checksum_mismatch
import _3dreport.composeapp.generated.resources.update_error_install
import _3dreport.composeapp.generated.resources.update_error_network
import _3dreport.composeapp.generated.resources.update_error_no_checksums
import _3dreport.composeapp.generated.resources.update_install_and_close
import _3dreport.composeapp.generated.resources.update_install_anyway
import _3dreport.composeapp.generated.resources.update_installing
import _3dreport.composeapp.generated.resources.update_manual_message
import _3dreport.composeapp.generated.resources.update_open_folder
import _3dreport.composeapp.generated.resources.update_password_cancelled
import _3dreport.composeapp.generated.resources.update_ready_linux
import _3dreport.composeapp.generated.resources.update_ready_linux_manual
import _3dreport.composeapp.generated.resources.update_ready_macos
import _3dreport.composeapp.generated.resources.update_ready_windows
import _3dreport.composeapp.generated.resources.update_title
import _3dreport.composeapp.generated.resources.update_unsaved
import _3dreport.composeapp.generated.resources.update_verifying
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.threedreport.app.platform.openUrl
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppProgressBar
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.app.update.InstallMethod
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A janela do "Atualizar agora" (decisão 143): baixando, conferindo, pronto pra instalar, o aviso de orçamento não
 * salvo, o comando do Linux sem `pkexec` e o erro. Um estado por passo; a conta e as regras são do [UpdateViewModel].
 */
@Composable
fun UpdateDialog(install: UpdateInstall, viewModel: UpdateViewModel) {
    val space = MaterialTheme.spacing
    val title = stringResource(Res.string.update_title, install.release.version)
    val seeNews = stringResource(Res.string.about_updates_see_news)
    when (install) {
        is UpdateInstall.Downloading -> AppDialog(
            title = title,
            onDismiss = viewModel::dismissUpdate,
            dismissOnClickOutside = false,
            buttons = { AppButton(stringResource(Res.string.common_cancel), viewModel::dismissUpdate) },
        ) {
            ProgressBlock(stringResource(Res.string.update_downloading), install.progress.takeIf { it > 0f })
        }

        is UpdateInstall.Verifying -> AppDialog(
            title = title,
            onDismiss = viewModel::dismissUpdate,
            dismissOnClickOutside = false,
            buttons = { AppButton(stringResource(Res.string.common_cancel), viewModel::dismissUpdate) },
        ) {
            ProgressBlock(stringResource(Res.string.update_verifying), null)
        }

        is UpdateInstall.Ready -> AppDialog(
            title = title,
            onDismiss = viewModel::dismissUpdate,
            buttons = {
                AppButton(stringResource(Res.string.common_cancel), viewModel::dismissUpdate)
                AppButton(
                    stringResource(if (install.update.plan.method == InstallMethod.LINUX_DEB_MANUAL) Res.string.update_open_folder else Res.string.update_install_and_close),
                    viewModel::installNow,
                    kind = ButtonKind.Primary,
                )
            },
        ) {
            DialogText(stringResource(install.update.plan.method.readyText()))
            if (install.passwordCancelled) DialogText(stringResource(Res.string.update_password_cancelled), muted = true)
        }

        is UpdateInstall.ConfirmUnsaved -> AppDialog(
            title = title,
            onDismiss = viewModel::backToReady,
            buttons = {
                AppButton(stringResource(Res.string.update_back), viewModel::backToReady)
                AppButton(stringResource(Res.string.update_install_anyway), viewModel::installAnyway, kind = ButtonKind.Danger)
            },
        ) {
            DialogText(stringResource(Res.string.update_unsaved))
        }

        is UpdateInstall.Installing -> AppDialog(
            title = title,
            onDismiss = {},
            dismissOnClickOutside = false,
            buttons = {},
        ) {
            ProgressBlock(stringResource(Res.string.update_installing), null)
        }

        is UpdateInstall.Manual -> AppDialog(
            title = title,
            onDismiss = viewModel::dismissUpdate,
            buttons = {
                AppButton(stringResource(Res.string.update_copy_command), viewModel::copyManualCommand, icon = AppIcons.ContentCopy)
                AppButton(stringResource(Res.string.common_close), viewModel::dismissUpdate, kind = ButtonKind.Primary)
            },
        ) {
            DialogText(stringResource(Res.string.update_manual_message))
            SelectionContainer {
                Text(
                    install.command,
                    style = MaterialTheme.appType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = space.xxs),
                )
            }
        }

        is UpdateInstall.Failed -> AppDialog(
            title = title,
            onDismiss = viewModel::dismissUpdate,
            buttons = {
                if (install.canRetry) {
                    AppButton(seeNews, { openUrl(install.release.url) }, icon = AppIcons.OpenInNew)
                    AppButton(stringResource(Res.string.common_retry), viewModel::retryUpdate, kind = ButtonKind.Primary)
                } else {
                    AppButton(stringResource(Res.string.common_close), viewModel::dismissUpdate)
                    AppButton(seeNews, { openUrl(install.release.url) }, kind = ButtonKind.Primary, icon = AppIcons.OpenInNew)
                }
            },
        ) {
            DialogText(stringResource(install.reason.text()))
        }
    }
}

/** A barra de progresso do passo e, embaixo, o que está sendo feito. Sem [progress], a barra anda sem fim. */
@Composable
private fun ProgressBlock(label: String, progress: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs), modifier = Modifier.fillMaxWidth()) {
        AppProgressBar(progress, modifier = Modifier.semantics { contentDescription = label })
        Text(label, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun InstallMethod.readyText(): StringResource = when (this) {
    InstallMethod.WINDOWS_MSI -> Res.string.update_ready_windows
    InstallMethod.LINUX_DEB -> Res.string.update_ready_linux
    InstallMethod.LINUX_DEB_MANUAL -> Res.string.update_ready_linux_manual
    InstallMethod.MACOS_DMG -> Res.string.update_ready_macos
}

private fun UpdateError.text(): StringResource = when (this) {
    UpdateError.NETWORK -> Res.string.update_error_network
    UpdateError.NO_CHECKSUMS -> Res.string.update_error_no_checksums
    UpdateError.CHECKSUM_MISMATCH -> Res.string.update_error_checksum_mismatch
    UpdateError.INSTALL -> Res.string.update_error_install
}
