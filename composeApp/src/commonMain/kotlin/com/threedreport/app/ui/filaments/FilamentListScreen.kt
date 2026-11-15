package com.threedreport.app.ui.filaments

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.common_restore
import _3dreport.composeapp.generated.resources.filaments_action_add_color
import _3dreport.composeapp.generated.resources.filaments_action_new
import _3dreport.composeapp.generated.resources.filaments_action_remove_color
import _3dreport.composeapp.generated.resources.filaments_color_block
import _3dreport.composeapp.generated.resources.filaments_color_out_of_stock
import _3dreport.composeapp.generated.resources.filaments_color_unnamed
import _3dreport.composeapp.generated.resources.filaments_delete_message
import _3dreport.composeapp.generated.resources.filaments_delete_title
import _3dreport.composeapp.generated.resources.filaments_delete_what
import _3dreport.composeapp.generated.resources.filaments_empty
import _3dreport.composeapp.generated.resources.filaments_field_brand
import _3dreport.composeapp.generated.resources.filaments_field_color_hex
import _3dreport.composeapp.generated.resources.filaments_field_color_hex_hint
import _3dreport.composeapp.generated.resources.filaments_field_color_name
import _3dreport.composeapp.generated.resources.filaments_field_color_name_hint
import _3dreport.composeapp.generated.resources.filaments_field_color_presets
import _3dreport.composeapp.generated.resources.filaments_field_color_stock
import _3dreport.composeapp.generated.resources.filaments_field_density
import _3dreport.composeapp.generated.resources.filaments_field_diameter
import _3dreport.composeapp.generated.resources.filaments_field_material
import _3dreport.composeapp.generated.resources.filaments_field_material_custom
import _3dreport.composeapp.generated.resources.filaments_field_name
import _3dreport.composeapp.generated.resources.filaments_field_name_hint
import _3dreport.composeapp.generated.resources.filaments_field_price
import _3dreport.composeapp.generated.resources.filaments_form_edit
import _3dreport.composeapp.generated.resources.filaments_form_new
import _3dreport.composeapp.generated.resources.filaments_help_colors
import _3dreport.composeapp.generated.resources.filaments_help_density
import _3dreport.composeapp.generated.resources.filaments_help_density_varies
import _3dreport.composeapp.generated.resources.filaments_in_stock
import _3dreport.composeapp.generated.resources.filaments_material_custom_option
import _3dreport.composeapp.generated.resources.filaments_out_of_stock
import _3dreport.composeapp.generated.resources.filaments_row_density
import _3dreport.composeapp.generated.resources.filaments_row_out_of_stock
import _3dreport.composeapp.generated.resources.filaments_row_price
import _3dreport.composeapp.generated.resources.filaments_section_basics
import _3dreport.composeapp.generated.resources.filaments_section_colors
import _3dreport.composeapp.generated.resources.filaments_section_price
import _3dreport.composeapp.generated.resources.filaments_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppSwatchField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ColorChip
import com.threedreport.app.ui.components.ColorSwatch
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.FormBlock
import com.threedreport.app.ui.components.FormPanel
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.ListScreenLayout
import com.threedreport.app.ui.components.ObserveNotices
import com.threedreport.app.ui.components.PanelSection
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Filament
import org.jetbrains.compose.resources.stringResource

/** Tela de Filamentos: catálogo salvo, escolhido depois na tela de Orçamento. Mesmo molde da tela de Insumos. */
@Composable
fun FilamentListScreen(viewModel: FilamentListViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveNotices(viewModel.notices)

    val form = state.form
    val panel: (@Composable () -> Unit)? = form?.let { open ->
        { FilamentFormPanel(open, viewModel) }
    }
    ListScreenLayout(panel = panel, modifier = modifier) {
        ScreenHeader(stringResource(Res.string.filaments_title)) {
            if (!state.isEmpty) {
                AppButton(
                    stringResource(Res.string.filaments_action_new),
                    onClick = viewModel::startAdd,
                    kind = if (form == null) ButtonKind.Primary else ButtonKind.Secondary,
                    icon = AppIcons.Add,
                )
            }
        }
        if (state.active.isEmpty()) {
            EmptyState(
                stringResource(Res.string.filaments_empty),
                actionLabel = stringResource(Res.string.filaments_action_new).takeIf { state.isEmpty },
                onAction = viewModel::startAdd.takeIf { state.isEmpty },
            )
        }
        state.active.forEach { key(it.id) { FilamentRow(it, viewModel, selectedId = form?.id) } }
        ArchivedSection(state.archived.size) { state.archived.forEach { key(it.id) { FilamentRow(it, viewModel, selectedId = form?.id) } } }
    }

    state.pendingDelete?.let { pending ->
        DeleteOrArchiveDialog(
            title = stringResource(Res.string.filaments_delete_title),
            what = stringResource(Res.string.filaments_delete_what, pending.name),
            usageCount = pending.usageCount,
            deleteMessage = stringResource(Res.string.filaments_delete_message, pending.name),
            onArchive = { viewModel.setArchived(pending.id, true) }.takeIf { pending.canArchive },
            onDelete = { viewModel.delete(pending.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilamentRow(filament: Filament, viewModel: FilamentListViewModel, selectedId: String?) {
    val archiveOrRestore = if (filament.archived) {
        RowAction(AppIcons.Unarchive, stringResource(Res.string.common_restore), { viewModel.setArchived(filament.id, false) })
    } else {
        RowAction(AppIcons.Archive, stringResource(Res.string.common_archive), { viewModel.setArchived(filament.id, true) })
    }
    val unnamed = stringResource(Res.string.filaments_color_unnamed)
    val outOfStock = stringResource(Res.string.filaments_color_out_of_stock)
    ListRow(
        title = filament.name,
        details = listOfNotNull(
            filament.materialType,
            filament.brand,
            stringResource(Res.string.filaments_row_density, filament.densityGPerCm3.toInputText()),
            stringResource(Res.string.filaments_row_out_of_stock).takeIf { !filament.archived && !filament.hasStockAvailable },
        ),
        value = stringResource(Res.string.filaments_row_price, filament.pricePerKg.toMoney()),
        muted = filament.archived,
        selected = selectedId == filament.id,
        onClick = { viewModel.startEdit(filament.id) },
        actions = listOf(
            RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.startEdit(filament.id) }),
            archiveOrRestore,
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDelete(filament.id) }, danger = true),
        ),
        // Uma cor só, sem nome e em estoque não diz nada: o selo some e a linha fica mais baixa.
        footer = if (filament.colors.size == 1 && filament.colors[0].let { it.labelOrNull() == null && it.inStock }) {
            null
        } else {
            {
                FlowRow(
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                ) {
                    filament.colors.forEach { color ->
                        key(color.id) {
                            ColorChip(
                                label = color.labelOrNull() ?: unnamed,
                                color = parseHexColor(color.hex),
                                inStock = color.inStock,
                                outOfStockLabel = outOfStock,
                                onToggle = { viewModel.toggleColorInStock(filament.id, color.id) },
                                onSelectedRow = selectedId == filament.id,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun FilamentFormPanel(form: FilamentFormState, viewModel: FilamentListViewModel) {
    val symbol = LocalCurrency.current.symbol
    FormPanel(
        title = stringResource(if (form.id == null) Res.string.filaments_form_new else Res.string.filaments_form_edit),
        onSave = viewModel::save,
        onCancel = viewModel::cancelEdit,
    ) {
        PanelSection(1, stringResource(Res.string.filaments_section_basics)) {
            AppTextField(
                label = stringResource(Res.string.filaments_field_name),
                value = form.name,
                onValueChange = { text -> viewModel.updateForm { it.copy(name = text) } },
                placeholder = stringResource(Res.string.filaments_field_name_hint),
                error = form.nameError?.asString(),
            )
            val customOption = stringResource(Res.string.filaments_material_custom_option)
            AppDropdownField(
                label = stringResource(Res.string.filaments_field_material),
                items = MATERIAL_OPTIONS,
                selected = form.materialOption,
                itemLabel = { option -> if (option is MaterialOption.Preset) option.preset.label else customOption },
                displayText = { option -> if (option is MaterialOption.Preset) option.preset.label else customOption },
                onSelect = { option -> viewModel.chooseMaterialType((option as? MaterialOption.Preset)?.preset) },
                emptyText = stringResource(Res.string.common_optional),
            )
            if (form.isCustomMaterialType) {
                AppTextField(
                    label = stringResource(Res.string.filaments_field_material_custom),
                    value = form.materialType,
                    onValueChange = { text -> viewModel.updateForm { it.copy(materialType = text) } },
                )
            }
            AppSuggestField(
                label = stringResource(Res.string.filaments_field_brand),
                value = form.brand,
                onValueChange = { text -> viewModel.updateForm { it.copy(brand = text) } },
                suggestions = form.brandSuggestions,
                suggestionLabel = { it },
                onChoose = { brand -> viewModel.updateForm { it.copy(brand = brand) } },
                placeholder = stringResource(Res.string.common_optional),
            )
        }
        PanelSection(2, stringResource(Res.string.filaments_section_price)) {
            AppTextField(
                label = stringResource(Res.string.filaments_field_price, symbol),
                value = form.pricePerKgText,
                onValueChange = { text -> viewModel.updateForm { it.copy(pricePerKgText = text) } },
                error = form.priceError?.asString(),
            )
            AppTextField(
                label = stringResource(Res.string.filaments_field_density),
                value = form.densityGPerCm3Text,
                onValueChange = { text -> viewModel.updateForm { it.copy(densityGPerCm3Text = text) } },
                error = form.densityError?.asString(),
                help = HelpText(
                    stringResource(if (form.densityNeedsCheck) Res.string.filaments_help_density_varies else Res.string.filaments_help_density),
                    warning = form.densityNeedsCheck,
                ),
            )
            AppTextField(
                label = stringResource(Res.string.filaments_field_diameter),
                value = form.diameterMmText,
                onValueChange = { text -> viewModel.updateForm { it.copy(diameterMmText = text) } },
                error = form.diameterError?.asString(),
            )
        }
        PanelSection(3, stringResource(Res.string.filaments_section_colors), help = HelpText(stringResource(Res.string.filaments_help_colors))) {
            form.colors.forEachIndexed { index, colorForm ->
                key(colorForm.id) {
                    ColorBlock(index + 1, colorForm, canRemove = form.colors.size > 1, viewModel = viewModel)
                }
            }
            AppButton(stringResource(Res.string.filaments_action_add_color), viewModel::addColorRow, icon = AppIcons.Add)
        }
    }
}

@Composable
private fun ColorBlock(number: Int, colorForm: FilamentColorFormState, canRemove: Boolean, viewModel: FilamentListViewModel) {
    val presetNames = FILAMENT_COLOR_PRESETS.associate { it.hex to stringResource(it.name) }
    FormBlock(
        title = stringResource(Res.string.filaments_color_block, number),
        action = RowAction(
            AppIcons.Delete,
            stringResource(Res.string.filaments_action_remove_color),
            { viewModel.removeColorRow(colorForm.id) },
            danger = true,
        ).takeIf { canRemove },
    ) {
        AppTextField(
            label = stringResource(Res.string.filaments_field_color_name),
            value = colorForm.name,
            onValueChange = { text -> viewModel.updateColorRow(colorForm.id) { it.copy(name = text) } },
            placeholder = stringResource(Res.string.filaments_field_color_name_hint),
        )
        AppTextField(
            label = stringResource(Res.string.filaments_field_color_hex),
            value = colorForm.hex.orEmpty(),
            onValueChange = { text -> viewModel.updateColorRow(colorForm.id) { it.copy(hex = text.ifBlank { null }) } },
            placeholder = stringResource(Res.string.filaments_field_color_hex_hint),
            trailing = { ColorSwatch(parseHexColor(colorForm.hex)) },
        )
        AppSwatchField(
            label = stringResource(Res.string.filaments_field_color_presets),
            options = FILAMENT_COLOR_PRESETS,
            optionLabel = { presetNames.getValue(it.hex) },
            optionColor = { it.color },
            isSelected = { it.hex == colorForm.hex },
            onSelect = { preset -> viewModel.applyColorPreset(colorForm.id, presetNames.getValue(preset.hex), preset.hex) },
        )
        AppChoiceField(
            label = stringResource(Res.string.filaments_field_color_stock),
            options = listOf(stringResource(Res.string.filaments_in_stock), stringResource(Res.string.filaments_out_of_stock)),
            selectedIndex = if (colorForm.inStock) 0 else 1,
            onSelect = { index -> viewModel.updateColorRow(colorForm.id) { it.copy(inStock = index == 0) } },
        )
    }
}
