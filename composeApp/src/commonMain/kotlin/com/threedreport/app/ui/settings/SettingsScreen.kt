package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.settings_action_discard
import _3dreport.composeapp.generated.resources.settings_backup_busy
import _3dreport.composeapp.generated.resources.settings_section_appearance
import _3dreport.composeapp.generated.resources.settings_section_backup
import _3dreport.composeapp.generated.resources.settings_section_business
import _3dreport.composeapp.generated.resources.settings_section_channels
import _3dreport.composeapp.generated.resources.settings_section_documents
import _3dreport.composeapp.generated.resources.settings_section_pending
import _3dreport.composeapp.generated.resources.settings_title
import _3dreport.composeapp.generated.resources.settings_unsaved
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDivider
import com.threedreport.app.ui.components.BusyBar
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ObserveUiNotices
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.components.SelectableItem
import com.threedreport.app.ui.templates.TemplateListDialog
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.text.uiText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Configurações em seções (decisão 112): uma lista à esquerda e uma seção por vez, com o conteúdo em até 760 de largura.
 * Custos e documentos são rascunho, e a barra "Alterações não salvas" embaixo salva ou descarta os dois
 * juntos; trocar de seção ou de tela não perde nada. Tema, moeda, canais e backup valem na hora.
 * O que é de cada impressora fica em Impressoras.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    brandingViewModel: BrandingViewModel,
    templateListViewModel: TemplateListViewModel,
    themeViewModel: ThemeViewModel,
    currencyViewModel: CurrencyViewModel,
    backupViewModel: BackupViewModel,
    salesChannelViewModel: SalesChannelViewModel,
    section: SettingsSection,
    onSectionChange: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val branding by brandingViewModel.uiState.collectAsState()
    val backup by backupViewModel.uiState.collectAsState()
    ObserveUiNotices(viewModel.notices)
    ObserveUiNotices(brandingViewModel.notices)
    ObserveUiNotices(salesChannelViewModel.notices)
    ObserveUiNotices(backupViewModel.notices)
    val space = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        if (backup.busy) BusyBar(uiText(Res.string.settings_backup_busy))
        Box(Modifier.padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding)) {
            ScreenHeader(stringResource(Res.string.settings_title))
        }
        Row(modifier = Modifier.weight(1f)) {
            SectionList(
                selected = section,
                onSelect = onSectionChange,
                hasPendingChanges = { (it == SettingsSection.BUSINESS && state.hasUnsavedChanges) || (it == SettingsSection.DOCUMENTS && branding.hasUnsavedChanges) },
            )
            // Uma rolagem por seção: trocar de seção começa do alto.
            key(section) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier
                            .padding(start = space.xl, end = space.xxl, bottom = space.xxl)
                            .widthIn(max = AppDimens.formMaxWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(space.xl),
                    ) {
                        when (section) {
                            SettingsSection.BUSINESS -> BusinessSection(state, viewModel)
                            SettingsSection.CHANNELS -> SalesChannelSection(salesChannelViewModel)
                            SettingsSection.DOCUMENTS -> ClientDocumentsSection(brandingViewModel, templateListViewModel)
                            SettingsSection.APPEARANCE -> AppearanceSection(themeViewModel, currencyViewModel)
                            SettingsSection.BACKUP -> BackupSection(backupViewModel)
                        }
                    }
                }
            }
        }

        if (state.hasUnsavedChanges || branding.hasUnsavedChanges) {
            UnsavedChangesBar(
                onDiscard = {
                    viewModel.discard()
                    brandingViewModel.discard()
                },
                // Um erro leva pra seção dele.
                onSave = { saveAllSettings(viewModel, brandingViewModel)?.let(onSectionChange) },
            )
        }
    }
}

@Composable
private fun SectionList(selected: SettingsSection, onSelect: (SettingsSection) -> Unit, hasPendingChanges: (SettingsSection) -> Boolean) {
    val space = MaterialTheme.spacing
    Column(
        modifier = Modifier.width(AppDimens.sectionListWidth + space.xxl).fillMaxHeight().padding(start = space.xxl),
        verticalArrangement = Arrangement.spacedBy(space.xxs),
    ) {
        val pending = stringResource(Res.string.settings_section_pending)
        SettingsSection.entries.forEach { section ->
            SelectableItem(
                label = stringResource(section.titleRes()),
                selected = section == selected,
                onClick = { onSelect(section) },
                note = pending.takeIf { hasPendingChanges(section) },
            )
        }
    }
}

private fun SettingsSection.titleRes() = when (this) {
    SettingsSection.BUSINESS -> Res.string.settings_section_business
    SettingsSection.CHANNELS -> Res.string.settings_section_channels
    SettingsSection.DOCUMENTS -> Res.string.settings_section_documents
    SettingsSection.APPEARANCE -> Res.string.settings_section_appearance
    SettingsSection.BACKUP -> Res.string.settings_section_backup
}

/**
 * Barra fixa embaixo enquanto há rascunho (decisão 112). No lugar dos dois "Salvar" soltos no meio das
 * seções, que ficavam fora da vista com a tela rolada.
 */
@Composable
private fun UnsavedChangesBar(onDiscard: () -> Unit, onSave: () -> Unit) {
    val space = MaterialTheme.spacing
    AppDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.card)
            .padding(horizontal = space.xxl, vertical = space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Text(
            stringResource(Res.string.settings_unsaved),
            style = MaterialTheme.appType.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        AppButton(stringResource(Res.string.settings_action_discard), onDiscard)
        AppButton(stringResource(Res.string.common_save), onSave, kind = ButtonKind.Primary)
    }
}
