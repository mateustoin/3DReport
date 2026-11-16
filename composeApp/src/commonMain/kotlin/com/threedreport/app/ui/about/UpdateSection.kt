package com.threedreport.app.ui.about

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.about_updates_available
import _3dreport.composeapp.generated.resources.about_updates_check
import _3dreport.composeapp.generated.resources.about_updates_checking
import _3dreport.composeapp.generated.resources.about_updates_failed
import _3dreport.composeapp.generated.resources.about_updates_help
import _3dreport.composeapp.generated.resources.about_updates_help_detail
import _3dreport.composeapp.generated.resources.about_updates_see_news
import _3dreport.composeapp.generated.resources.about_updates_title
import _3dreport.composeapp.generated.resources.about_updates_toggle
import _3dreport.composeapp.generated.resources.about_updates_up_to_date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.threedreport.app.platform.openUrl
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * "Atualizações" no Sobre (decisão 116): a opção, o resultado em texto e a ação que resolve. O app só avisa e abre a
 * página da versão; não instala nada.
 */
@Composable
fun UpdateSection(viewModel: UpdateViewModel) {
    val state by viewModel.uiState.collectAsState()
    val space = MaterialTheme.spacing

    FormSection(null, stringResource(Res.string.about_updates_title)) {
        AppCheckbox(
            stringResource(Res.string.about_updates_toggle),
            state.enabled,
            viewModel::setEnabled,
            help = HelpText(stringResource(Res.string.about_updates_help), stringResource(Res.string.about_updates_help_detail)),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            when (val status = state.status) {
                is UpdateState.Available -> {
                    AppButton(stringResource(Res.string.about_updates_see_news), { openUrl(status.release.url) }, icon = AppIcons.OpenInNew)
                    StatusText(stringResource(Res.string.about_updates_available, status.release.version))
                }

                else -> {
                    AppButton(stringResource(Res.string.about_updates_check), viewModel::checkNow, enabled = status != UpdateState.Checking, icon = AppIcons.Sync)
                    when (status) {
                        UpdateState.Checking -> StatusText(stringResource(Res.string.about_updates_checking))
                        UpdateState.UpToDate -> StatusText(stringResource(Res.string.about_updates_up_to_date))
                        UpdateState.Failed -> StatusText(stringResource(Res.string.about_updates_failed))
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
