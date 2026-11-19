package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.stock_action_add_item
import _3dreport.composeapp.generated.resources.stock_action_adjust
import _3dreport.composeapp.generated.resources.stock_action_buy_again
import _3dreport.composeapp.generated.resources.stock_action_delete_purchase
import _3dreport.composeapp.generated.resources.stock_action_remove_item
import _3dreport.composeapp.generated.resources.stock_action_stop
import _3dreport.composeapp.generated.resources.stock_cost_per_kg
import _3dreport.composeapp.generated.resources.stock_cost_per_unit
import _3dreport.composeapp.generated.resources.stock_date_today
import _3dreport.composeapp.generated.resources.stock_date_yesterday
import _3dreport.composeapp.generated.resources.stock_field_amount
import _3dreport.composeapp.generated.resources.stock_field_amount_plain
import _3dreport.composeapp.generated.resources.stock_field_date
import _3dreport.composeapp.generated.resources.stock_field_item
import _3dreport.composeapp.generated.resources.stock_field_link
import _3dreport.composeapp.generated.resources.stock_field_note
import _3dreport.composeapp.generated.resources.stock_field_price
import _3dreport.composeapp.generated.resources.stock_field_shipping
import _3dreport.composeapp.generated.resources.stock_field_spool
import _3dreport.composeapp.generated.resources.stock_field_supplier
import _3dreport.composeapp.generated.resources.stock_field_threshold
import _3dreport.composeapp.generated.resources.stock_help_spool
import _3dreport.composeapp.generated.resources.stock_help_threshold
import _3dreport.composeapp.generated.resources.stock_help_untracked_color
import _3dreport.composeapp.generated.resources.stock_help_untracked_consumable
import _3dreport.composeapp.generated.resources.stock_item_choose
import _3dreport.composeapp.generated.resources.stock_item_n
import _3dreport.composeapp.generated.resources.stock_panel_available
import _3dreport.composeapp.generated.resources.stock_panel_average
import _3dreport.composeapp.generated.resources.stock_panel_balance
import _3dreport.composeapp.generated.resources.stock_panel_movements
import _3dreport.composeapp.generated.resources.stock_panel_reserved
import _3dreport.composeapp.generated.resources.stock_per_kg
import _3dreport.composeapp.generated.resources.stock_per_unit
import _3dreport.composeapp.generated.resources.stock_purchase_edit
import _3dreport.composeapp.generated.resources.stock_purchase_new
import _3dreport.composeapp.generated.resources.stock_quick_1kg
import _3dreport.composeapp.generated.resources.stock_quick_500g
import _3dreport.composeapp.generated.resources.stock_section_items
import _3dreport.composeapp.generated.resources.stock_section_purchase
import _3dreport.composeapp.generated.resources.stock_section_values
import _3dreport.composeapp.generated.resources.stock_total
import _3dreport.composeapp.generated.resources.stock_unit_g
import _3dreport.composeapp.generated.resources.stock_unit_un
import _3dreport.composeapp.generated.resources.stock_update_consumable
import _3dreport.composeapp.generated.resources.stock_update_filament
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.localEpochDayOf
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.DateShortcut
import com.threedreport.app.ui.components.DateShortcutPicker
import com.threedreport.app.ui.components.FieldError
import com.threedreport.app.ui.components.FormBlock
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormPanel
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.FormSummary
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.InfoRow
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.PanelSection
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.suggestionsFor
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.StockItemRef
import org.jetbrains.compose.resources.stringResource

/** A unidade do saldo, pra rótulo de campo: "g" na cor de filamento, "un" no insumo. */
@Composable
private fun StockItemRef.unitLabel(): String =
    stringResource(if (this is StockItemRef.ColorRef) Res.string.stock_unit_g else Res.string.stock_unit_un)

/** "R$ 97,40/kg" na cor de filamento e "R$ 0,26/un" no insumo: o custo na unidade do cadastro. */
@Composable
private fun catalogCostText(ref: StockItemRef, cost: Double): String =
    stringResource(if (ref is StockItemRef.ColorRef) Res.string.stock_per_kg else Res.string.stock_per_unit, cost.toMoney())

// ------------------------------------------------------------------------------------------ painel do item

/**
 * O painel de um item controlado: saldo, reservado, disponível e a média das compras, "Comprar de novo", os dois campos que se
 * salvam ("Avisar abaixo de" e o carretel vazio), o histórico de movimentos e as ações que mexem no saldo.
 */
@Composable
internal fun StockItemPanelView(panel: StockItemPanel, viewModel: StockViewModel) {
    val ref = panel.entry.ref
    val unit = ref.unitLabel()
    val status = panel.status
    FormPanel(title = panel.entry.label(), onSave = viewModel::saveItem, onCancel = viewModel::closePanel) {
        Column {
            InfoRow(stringResource(Res.string.stock_panel_balance), stockAmountText(ref, status.balance))
            if (status.reserved > 0) {
                InfoRow(stringResource(Res.string.stock_panel_reserved), stockAmountText(ref, status.reserved))
                InfoRow(stringResource(Res.string.stock_panel_available), stockAmountText(ref, status.available))
            }
            panel.averageCost?.let { InfoRow(stringResource(Res.string.stock_panel_average), catalogCostText(ref, it)) }
        }
        if (panel.buyAgainLink != null) {
            AppButton(stringResource(Res.string.stock_action_buy_again), { viewModel.buyAgain(ref) }, icon = AppIcons.OpenInNew)
        }
        AppNumberField(
            label = stringResource(Res.string.stock_field_threshold, unit),
            value = panel.thresholdText,
            onValueChange = viewModel::setThreshold,
            kind = NumberKind.AMOUNT,
            placeholder = stringResource(Res.string.common_optional),
            error = panel.thresholdError?.asString(),
            help = HelpText(stringResource(Res.string.stock_help_threshold)),
            money = false,
        )
        if (panel.isFilament) {
            AppNumberField(
                label = stringResource(Res.string.stock_field_spool),
                value = panel.spoolText,
                onValueChange = viewModel::setSpool,
                kind = NumberKind.AMOUNT,
                placeholder = stringResource(Res.string.common_optional),
                error = panel.spoolError?.asString(),
                help = HelpText(stringResource(Res.string.stock_help_spool)),
                money = false,
            )
        }
        ItemActions(panel, viewModel)
        FormSection(number = null, title = stringResource(Res.string.stock_panel_movements), modifier = Modifier.padding(top = MaterialTheme.spacing.sm)) {
            Column {
                panel.movements.forEach { movement ->
                    key(movement.id) {
                        ListRow(
                            title = movement.title(),
                            details = listOfNotNull(formatDate(localEpochDayOf(movement.epochMillis)), movement.note),
                            value = stockSignedAmountText(ref, movement.amount),
                            actions = if (movement.removable) {
                                listOf(RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDeleteMovement(movement.id) }, danger = true))
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemActions(panel: StockItemPanel, viewModel: StockViewModel) {
    val space = MaterialTheme.spacing
    FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
        AppButton(stringResource(Res.string.stock_action_adjust), { viewModel.startAdjust(panel.entry.ref) })
        AppButton(stringResource(Res.string.stock_action_stop), { viewModel.requestStopTracking(panel.entry.ref) })
    }
}

// ---------------------------------------------------------------------------------------- painel da compra

/**
 * O formulário de compra: data, fornecedor e link; as linhas (item, quantidade, preço pago e o custo por kg ou por unidade com o
 * frete rateado); frete e total; e as caixas "Atualizar o preço", marcadas, que se pode desmarcar.
 */
@Composable
internal fun StockPurchasePanelView(panel: PurchasePanel, today: Long, viewModel: StockViewModel) {
    val form = panel.form
    FormPanel(
        title = stringResource(if (panel.isNew) Res.string.stock_purchase_new else Res.string.stock_purchase_edit),
        onSave = viewModel::savePurchase,
        onCancel = viewModel::closePanel,
    ) {
        PanelSection(1, stringResource(Res.string.stock_section_purchase)) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                    Text(
                        stringResource(Res.string.stock_field_date),
                        style = MaterialTheme.appType.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DateShortcutPicker(
                        epochDay = form.dateEpochDay,
                        shortcuts = listOf(
                            DateShortcut(stringResource(Res.string.stock_date_today), today),
                            DateShortcut(stringResource(Res.string.stock_date_yesterday), today - 1),
                        ),
                        today = today,
                        onChange = { day -> if (day != null) viewModel.updatePurchase { it.copy(dateEpochDay = day) } },
                        allowFuture = false,
                    )
                }
                AppSuggestField(
                    label = stringResource(Res.string.stock_field_supplier),
                    value = form.supplier,
                    onValueChange = { text -> viewModel.updatePurchase { it.copy(supplier = text) } },
                    suggestions = panel.suppliers.suggestionsFor(form.supplier),
                    suggestionLabel = { it },
                    onChoose = { supplier -> viewModel.updatePurchase { it.copy(supplier = supplier) } },
                    placeholder = stringResource(Res.string.common_optional),
                )
                AppTextField(
                    label = stringResource(Res.string.stock_field_link),
                    value = form.link,
                    onValueChange = { text -> viewModel.updatePurchase { it.copy(link = text) } },
                    placeholder = stringResource(Res.string.common_optional),
                )
            }
        }
        PanelSection(2, stringResource(Res.string.stock_section_items)) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                form.lines.forEachIndexed { index, line ->
                    key(line.id) {
                        PurchaseLineBlock(number = index + 1, line = line, view = panel.lines[index], panel = panel, viewModel = viewModel)
                    }
                }
                FieldError(form.linesError?.asString())
                AppButton(stringResource(Res.string.stock_action_add_item), viewModel::addPurchaseLine, icon = AppIcons.Add)
            }
        }
        PanelSection(3, stringResource(Res.string.stock_section_values)) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                AppNumberField(
                    label = stringResource(Res.string.stock_field_shipping),
                    value = form.shippingText,
                    onValueChange = { text -> viewModel.updatePurchase { it.copy(shippingText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = LocalCurrency.current.symbol,
                    placeholder = stringResource(Res.string.common_optional),
                    error = form.shippingError?.asString(),
                )
                FormSummary(stringResource(Res.string.stock_total), panel.total.toMoney())
                panel.updates.forEach { update ->
                    key(update.key) {
                        AppCheckbox(
                            label = if (update.isFilament) {
                                stringResource(Res.string.stock_update_filament, update.name, update.currentValue.toMoney(), update.newValue.toMoney())
                            } else {
                                stringResource(Res.string.stock_update_consumable, update.name, update.currentValue.toMoney(), update.newValue.toMoney())
                            },
                            checked = update.checked,
                            onChange = { viewModel.toggleUpdate(update.key) },
                        )
                    }
                }
                AppTextField(
                    label = stringResource(Res.string.stock_field_note),
                    value = form.note,
                    onValueChange = { text -> viewModel.updatePurchase { it.copy(note = text) } },
                    placeholder = stringResource(Res.string.common_optional),
                )
            }
        }
        if (!panel.isNew) {
            AppButton(stringResource(Res.string.stock_action_delete_purchase), { viewModel.requestDeletePurchase() }, icon = AppIcons.Delete)
        }
    }
}

/** Uma linha da compra: o item, a quantidade e o preço pago, os atalhos de quilo e o custo por kg (ou por unidade). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseLineBlock(number: Int, line: PurchaseLineForm, view: PurchaseLineView, panel: PurchasePanel, viewModel: StockViewModel) {
    val space = MaterialTheme.spacing
    val entry = view.entry?.takeIf { !it.removed }
    val ref = view.entry?.ref
    FormBlock(
        title = stringResource(Res.string.stock_item_n, number),
        action = RowAction(AppIcons.Delete, stringResource(Res.string.stock_action_remove_item, number), { viewModel.removePurchaseLine(line.id) }, danger = true),
    ) {
        AppDropdownField(
            label = stringResource(Res.string.stock_field_item),
            items = panel.choices,
            selected = entry,
            itemLabel = { it.displayName },
            displayText = { it.displayName },
            onSelect = { choice -> viewModel.updatePurchaseLine(line.id) { it.copy(ref = choice.ref) } },
            emptyText = stringResource(Res.string.stock_item_choose),
            error = line.refError?.asString(),
            help = if (ref != null && !view.tracked) {
                HelpText(stringResource(if (ref is StockItemRef.ColorRef) Res.string.stock_help_untracked_color else Res.string.stock_help_untracked_consumable), warning = true)
            } else {
                null
            },
        )
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = if (ref != null) stringResource(Res.string.stock_field_amount, ref.unitLabel()) else stringResource(Res.string.stock_field_amount_plain),
                    value = line.amountText,
                    onValueChange = { text -> viewModel.updatePurchaseLine(line.id) { it.copy(amountText = text) } },
                    kind = NumberKind.AMOUNT,
                    error = line.amountError?.asString(),
                    money = false,
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.stock_field_price),
                    value = line.priceText,
                    onValueChange = { text -> viewModel.updatePurchaseLine(line.id) { it.copy(priceText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = LocalCurrency.current.symbol,
                    error = line.priceError?.asString(),
                )
            }
        }
        if (ref is StockItemRef.ColorRef) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
                AppButton(stringResource(Res.string.stock_quick_1kg), { viewModel.fillLineGrams(line.id, KILOGRAM_GRAMS) }, height = AppDimens.smallControlHeight)
                AppButton(stringResource(Res.string.stock_quick_500g), { viewModel.fillLineGrams(line.id, HALF_KILOGRAM_GRAMS) }, height = AppDimens.smallControlHeight)
            }
        }
        view.catalogUnitCost?.let { cost ->
            if (ref != null) {
                FormSummary(
                    stringResource(if (ref is StockItemRef.ColorRef) Res.string.stock_cost_per_kg else Res.string.stock_cost_per_unit),
                    cost.toMoney(),
                )
            }
        }
    }
}

private const val KILOGRAM_GRAMS = 1000.0
private const val HALF_KILOGRAM_GRAMS = 500.0
