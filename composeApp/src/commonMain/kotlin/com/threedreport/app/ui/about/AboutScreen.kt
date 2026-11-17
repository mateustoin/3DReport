package com.threedreport.app.ui.about

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.about_does_orders
import _3dreport.composeapp.generated.resources.about_does_quote
import _3dreport.composeapp.generated.resources.about_does_settings
import _3dreport.composeapp.generated.resources.about_link_code
import _3dreport.composeapp.generated.resources.about_link_licenses
import _3dreport.composeapp.generated.resources.about_link_site
import _3dreport.composeapp.generated.resources.about_link_support
import _3dreport.composeapp.generated.resources.about_shortcut_close
import _3dreport.composeapp.generated.resources.about_shortcut_close_keys
import _3dreport.composeapp.generated.resources.about_shortcut_new
import _3dreport.composeapp.generated.resources.about_shortcut_new_keys
import _3dreport.composeapp.generated.resources.about_shortcut_save
import _3dreport.composeapp.generated.resources.about_shortcut_save_keys
import _3dreport.composeapp.generated.resources.about_shortcut_screens
import _3dreport.composeapp.generated.resources.about_shortcut_screens_keys
import _3dreport.composeapp.generated.resources.about_shortcuts_title
import _3dreport.composeapp.generated.resources.about_summary
import _3dreport.composeapp.generated.resources.about_title
import _3dreport.composeapp.generated.resources.about_version
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.openUrl
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.BrandMark
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.InfoRow
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

const val SITE_URL = "https://mateustoin.github.io/3DReport/"
const val GITHUB_URL = "https://github.com/mateustoin/3DReport"
const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/mateustoin"
const val LICENSES_URL = "https://github.com/mateustoin/3DReport/blob/main/THIRD-PARTY-NOTICES.md"
private const val AUTHOR_NAME = "Mateus Antonio da Silva"

/**
 * Sobre (decisão 111): o que ficava no rodapé de toda tela (versão, autor, links e apoio), o que o app faz, os atalhos
 * de teclado e a verificação de atualizações ([UpdateSection]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(version: String, updateViewModel: UpdateViewModel) {
    val space = MaterialTheme.spacing
    val updateState by updateViewModel.uiState.collectAsState()
    val updateAvailable = updateState.status is UpdateState.Available
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl)
                .widthIn(max = AppDimens.listMaxWidth)
                .fillMaxWidth(),
        ) {
            // O espaço depois do cabeçalho é o do próprio ScreenHeader, o mesmo dos cadastros.
            ScreenHeader(stringResource(Res.string.about_title))
            Column(verticalArrangement = Arrangement.spacedBy(space.xl)) {
                Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                    BrandMark(onNavy = false)
                    Text(stringResource(Res.string.about_version, version), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
                    Text(stringResource(Res.string.about_summary, AUTHOR_NAME), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
                    listOf(Res.string.about_does_quote, Res.string.about_does_orders, Res.string.about_does_settings).forEach {
                        Text(stringResource(it), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
                    // Um só âmbar por contexto: com versão nova, o âmbar é de "Ver novidades" (UpdateSection).
                    AppButton(
                        stringResource(Res.string.about_link_support),
                        { openUrl(BUY_ME_A_COFFEE_URL) },
                        kind = if (updateAvailable) ButtonKind.Secondary else ButtonKind.Primary,
                        icon = AppIcons.OpenInNew,
                    )
                    AppButton(stringResource(Res.string.about_link_site), { openUrl(SITE_URL) }, icon = AppIcons.OpenInNew)
                    AppButton(stringResource(Res.string.about_link_code), { openUrl(GITHUB_URL) }, icon = AppIcons.OpenInNew)
                    AppButton(stringResource(Res.string.about_link_licenses), { openUrl(LICENSES_URL) }, icon = AppIcons.OpenInNew)
                }

                UpdateSection(updateViewModel)

                FormSection(null, stringResource(Res.string.about_shortcuts_title)) {
                    Column {
                        InfoRow(stringResource(Res.string.about_shortcut_screens), stringResource(Res.string.about_shortcut_screens_keys))
                        InfoRow(stringResource(Res.string.about_shortcut_save), stringResource(Res.string.about_shortcut_save_keys))
                        InfoRow(stringResource(Res.string.about_shortcut_new), stringResource(Res.string.about_shortcut_new_keys))
                        InfoRow(stringResource(Res.string.about_shortcut_close), stringResource(Res.string.about_shortcut_close_keys))
                    }
                }
            }
        }
    }
}
