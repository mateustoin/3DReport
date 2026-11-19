package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_restore
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.settings_channels_action_new
import _3dreport.composeapp.generated.resources.settings_channels_delete_message
import _3dreport.composeapp.generated.resources.settings_channels_delete_title
import _3dreport.composeapp.generated.resources.settings_channels_delete_what
import _3dreport.composeapp.generated.resources.settings_channels_empty
import _3dreport.composeapp.generated.resources.settings_channels_field_fee
import _3dreport.composeapp.generated.resources.settings_channels_field_fixed
import _3dreport.composeapp.generated.resources.settings_channels_field_name
import _3dreport.composeapp.generated.resources.settings_channels_field_name_hint
import _3dreport.composeapp.generated.resources.settings_channels_form_edit
import _3dreport.composeapp.generated.resources.settings_channels_form_new
import _3dreport.composeapp.generated.resources.settings_channels_help
import _3dreport.composeapp.generated.resources.settings_channels_help_detail
import _3dreport.composeapp.generated.resources.settings_channels_help_fixed
import _3dreport.composeapp.generated.resources.settings_channels_help_fixed_detail
import _3dreport.composeapp.generated.resources.settings_channels_help_tiers_active
import _3dreport.composeapp.generated.resources.settings_channels_tier_add
import _3dreport.composeapp.generated.resources.settings_channels_tier_fee
import _3dreport.composeapp.generated.resources.settings_channels_tier_fixed
import _3dreport.composeapp.generated.resources.settings_channels_tier_last
import _3dreport.composeapp.generated.resources.settings_channels_tier_remove
import _3dreport.composeapp.generated.resources.settings_channels_tier_up_to
import _3dreport.composeapp.generated.resources.settings_channels_tiers_active
import _3dreport.composeapp.generated.resources.settings_channels_tiers_help
import _3dreport.composeapp.generated.resources.settings_channels_tiers_help_detail
import _3dreport.composeapp.generated.resources.settings_channels_tiers_toggle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.FieldError
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.TableColumn
import com.threedreport.app.ui.components.TableHeader
import com.threedreport.app.ui.components.TablePlainRow
import com.threedreport.app.ui.components.clickableAction
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.channelFeeSummary
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.SalesChannel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A seção Canais de venda: a lista no molde dos cadastros e o formulário num diálogo. Valem na hora, sem o "Salvar" da barra. */
@Composable
internal fun SalesChannelSection(viewModel: SalesChannelViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Column {
            if (state.active.isEmpty()) {
                EmptyState(stringResource(Res.string.settings_channels_empty), modifier = Modifier.padding(top = MaterialTheme.spacing.xxs))
            }
            state.active.forEach { key(it.id) { ChannelRow(it, viewModel) } }
            ArchivedSection(state.archived.size) { state.archived.forEach { key(it.id) { ChannelRow(it, viewModel) } } }
        }
        // A ajuda que ficava no título da seção vai junto do botão, o primeiro elemento que age sobre a lista.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
            val newLabel = stringResource(Res.string.settings_channels_action_new)
            AppButton(newLabel, viewModel::startAdd, icon = AppIcons.Add)
            HelpHint(HelpText(stringResource(Res.string.settings_channels_help), stringResource(Res.string.settings_channels_help_detail)), newLabel)
        }
    }

    state.form?.let { ChannelFormDialog(it, viewModel) }

    state.pendingDelete?.let { pending ->
        DeleteOrArchiveDialog(
            title = stringResource(Res.string.settings_channels_delete_title, pending.name),
            what = stringResource(Res.string.settings_channels_delete_what, pending.name),
            usageCount = pending.usageCount,
            deleteMessage = stringResource(Res.string.settings_channels_delete_message),
            onArchive = { viewModel.setArchived(pending.id, true) }.takeIf { pending.canArchive },
            onDelete = { viewModel.delete(pending.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun ChannelRow(channel: SalesChannel, viewModel: SalesChannelViewModel) {
    val archiveOrRestore = if (channel.archived) {
        RowAction(AppIcons.Unarchive, stringResource(Res.string.common_restore), { viewModel.setArchived(channel.id, false) })
    } else {
        RowAction(AppIcons.Archive, stringResource(Res.string.common_archive), { viewModel.setArchived(channel.id, true) })
    }
    ListRow(
        title = channel.name,
        value = channelFeeSummary(channel, LocalCurrency.current),
        muted = channel.archived,
        onClick = { viewModel.startEdit(channel) },
        actions = listOf(
            RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.startEdit(channel) }),
            archiveOrRestore,
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDelete(channel.id) }, danger = true),
        ),
    )
}

@Composable
private fun ChannelFormDialog(form: SalesChannelFormState, viewModel: SalesChannelViewModel) {
    val symbol = LocalCurrency.current.symbol
    // Com faixas, é a taxa de cada faixa que vale (decisão 121): os campos únicos ficam desligados, pra ninguém mudar
    // um número que não muda nada.
    val hasTiers = form.tiers.isNotEmpty()
    AppDialog(
        title = stringResource(if (form.editingId == null) Res.string.settings_channels_form_new else Res.string.settings_channels_form_edit),
        onDismiss = viewModel::cancelEdit,
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), viewModel::cancelEdit)
            AppButton(stringResource(Res.string.common_save), viewModel::save, kind = ButtonKind.Primary)
        },
    ) {
        FormGrid {
            cell(3) {
                AppTextField(
                    label = stringResource(Res.string.settings_channels_field_name),
                    value = form.nameText,
                    onValueChange = viewModel::setName,
                    placeholder = stringResource(Res.string.settings_channels_field_name_hint),
                    error = form.nameError?.asString(),
                    autoFocus = true,
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_channels_field_fee),
                    value = form.feeRatePercentText,
                    onValueChange = viewModel::setFeeRate,
                    kind = NumberKind.MEASURE,
                    money = false,
                    enabled = !hasTiers,
                    error = form.feeError?.asString(),
                    help = HelpText(stringResource(Res.string.settings_channels_help_tiers_active), warning = true).takeIf { hasTiers },
                )
            }
            cell(2) {
                AppNumberField(
                    label = stringResource(Res.string.settings_channels_field_fixed),
                    value = form.fixedFeePerItemText,
                    onValueChange = viewModel::setFixedFeePerItem,
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    enabled = !hasTiers,
                    error = form.fixedFeeError?.asString(),
                    help = HelpText(stringResource(Res.string.settings_channels_help_fixed), stringResource(Res.string.settings_channels_help_fixed_detail)),
                )
            }
        }
        ChannelTiersEditor(form, viewModel)
    }
}

/**
 * Faixas de preço de um canal (decisão 121), recolhidas por padrão: só quem precisa (Shopee) vai atrás.
 * Editar um canal que já tem faixas abre a área expandida.
 */
@Composable
private fun ChannelTiersEditor(form: SalesChannelFormState, viewModel: SalesChannelViewModel) {
    val space = MaterialTheme.spacing
    val symbol = LocalCurrency.current.symbol
    val toggleLabel = stringResource(Res.string.settings_channels_tiers_toggle)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
        Row(
            modifier = Modifier
                .clickableAction(onClick = { viewModel.setTiersExpanded(!form.tiersExpanded) }, role = Role.Button)
                .padding(vertical = space.xxs, horizontal = space.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.xxs),
        ) {
            Text(toggleLabel, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                if (form.tiersExpanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AppDimens.iconSize),
            )
        }
        HelpHint(HelpText(stringResource(Res.string.settings_channels_tiers_help), stringResource(Res.string.settings_channels_tiers_help_detail)), toggleLabel)
    }
    form.tierNotice?.let { Text(it.asString(), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    if (!form.tiersExpanded) {
        // Recolhidas, as faixas continuam valendo: dizer quantas, pra não ficarem escondidas em silêncio.
        if (form.tiers.isNotEmpty()) {
            Text(
                pluralStringResource(Res.plurals.settings_channels_tiers_active, form.tiers.size, form.tiers.size),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val upToTitle = stringResource(Res.string.settings_channels_tier_up_to)
    val feeTitle = stringResource(Res.string.settings_channels_tier_fee)
    val fixedTitle = stringResource(Res.string.settings_channels_tier_fixed)
    val removeTitle = stringResource(Res.string.settings_channels_tier_remove)
    val columns = listOf(TableColumn(upToTitle), TableColumn(feeTitle), TableColumn(fixedTitle), TableColumn("", width = AppDimens.smallControlHeight))
    if (form.tiers.isNotEmpty()) {
        Column {
            TableHeader(columns)
            form.tiers.forEachIndexed { index, tier ->
                key(tier.id) {
                    TablePlainRow(columns, spacing = MaterialTheme.spacing.xs, verticalPadding = MaterialTheme.spacing.xxs) {
                        Cell(0) {
                            if (index == form.tiers.lastIndex) {
                                Text(stringResource(Res.string.settings_channels_tier_last), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                AppNumberField(
                                    label = upToTitle,
                                    value = tier.upToUnitPriceText,
                                    onValueChange = { text -> viewModel.updateTierRow(tier.id) { it.copy(upToUnitPriceText = text) } },
                                    kind = NumberKind.AMOUNT,
                                    prefix = symbol,
                                    labelHidden = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Cell(1) {
                            AppNumberField(
                                label = feeTitle,
                                value = tier.feeRatePercentText,
                                onValueChange = { text -> viewModel.updateTierRow(tier.id) { it.copy(feeRatePercentText = text) } },
                                kind = NumberKind.MEASURE,
                                money = false,
                                labelHidden = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Cell(2) {
                            AppNumberField(
                                label = fixedTitle,
                                value = tier.fixedFeePerItemText,
                                onValueChange = { text -> viewModel.updateTierRow(tier.id) { it.copy(fixedFeePerItemText = text) } },
                                kind = NumberKind.AMOUNT,
                                prefix = symbol,
                                labelHidden = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Cell(3) {
                            IconAction(AppIcons.Delete, removeTitle, { viewModel.removeTierRow(tier.id) }, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    FieldError(form.tiersError?.asString())
    AppButton(stringResource(Res.string.settings_channels_tier_add), viewModel::addTierRow, icon = AppIcons.Add)
}
