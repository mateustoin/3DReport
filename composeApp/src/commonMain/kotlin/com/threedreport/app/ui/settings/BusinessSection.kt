package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_action_energy_calc
import _3dreport.composeapp.generated.resources.settings_energy_title
import _3dreport.composeapp.generated.resources.settings_failures_title
import _3dreport.composeapp.generated.resources.settings_field_admin_cost
import _3dreport.composeapp.generated.resources.settings_field_energy_price
import _3dreport.composeapp.generated.resources.settings_field_failure_rate
import _3dreport.composeapp.generated.resources.settings_field_finishing_rate
import _3dreport.composeapp.generated.resources.settings_field_fixed_cost
import _3dreport.composeapp.generated.resources.settings_field_labor_rate
import _3dreport.composeapp.generated.resources.settings_field_margin
import _3dreport.composeapp.generated.resources.settings_field_minimum_price
import _3dreport.composeapp.generated.resources.settings_field_productive_hours
import _3dreport.composeapp.generated.resources.settings_field_rush
import _3dreport.composeapp.generated.resources.settings_field_tax
import _3dreport.composeapp.generated.resources.settings_fixed_title
import _3dreport.composeapp.generated.resources.settings_help_failure
import _3dreport.composeapp.generated.resources.settings_help_failure_detail
import _3dreport.composeapp.generated.resources.settings_help_finishing
import _3dreport.composeapp.generated.resources.settings_help_finishing_detail
import _3dreport.composeapp.generated.resources.settings_help_fixed
import _3dreport.composeapp.generated.resources.settings_help_fixed_detail
import _3dreport.composeapp.generated.resources.settings_help_labor
import _3dreport.composeapp.generated.resources.settings_help_labor_detail
import _3dreport.composeapp.generated.resources.settings_help_minimum
import _3dreport.composeapp.generated.resources.settings_help_minimum_detail
import _3dreport.composeapp.generated.resources.settings_help_rush
import _3dreport.composeapp.generated.resources.settings_help_rush_detail
import _3dreport.composeapp.generated.resources.settings_help_tax
import _3dreport.composeapp.generated.resources.settings_help_tax_detail
import _3dreport.composeapp.generated.resources.settings_labor_title
import _3dreport.composeapp.generated.resources.settings_margin_title
import _3dreport.composeapp.generated.resources.settings_order_title
import _3dreport.composeapp.generated.resources.settings_warning_fixed_cost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import org.jetbrains.compose.resources.stringResource

/** A seção Negócio: os custos e a margem que entram em todo preço. Rascunho até o "Salvar" da barra. */
@Composable
internal fun BusinessSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val currency = LocalCurrency.current
    val symbol = currency.symbol
    var showTariffDialog by remember { mutableStateOf(false) }

    FormSection(1, stringResource(Res.string.settings_energy_title)) {
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_energy_price),
                    value = state.energyPricePerKwhText,
                    onValueChange = { text -> viewModel.update { it.copy(energyPricePerKwhText = text) } },
                    kind = NumberKind.MEASURE,
                    prefix = symbol,
                    error = state.errors[SettingsField.ENERGY]?.asString(),
                )
            }
        }
        AppButton(stringResource(Res.string.settings_action_energy_calc), onClick = { showTariffDialog = true }, icon = AppIcons.Calculate)
    }

    FormSection(2, stringResource(Res.string.settings_labor_title)) {
        FormGrid {
            cell(2) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_labor_rate),
                    value = state.laborRatePerHourText,
                    onValueChange = { text -> viewModel.update { it.copy(laborRatePerHourText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    error = state.errors[SettingsField.LABOR]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_labor), stringResource(Res.string.settings_help_labor_detail)),
                )
            }
        }
    }

    FormSection(3, stringResource(Res.string.settings_fixed_title)) {
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_fixed_cost),
                    value = state.monthlyFixedCostText,
                    onValueChange = { text -> viewModel.update { it.copy(monthlyFixedCostText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    error = state.errors[SettingsField.MONTHLY_FIXED_COST]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_fixed), stringResource(Res.string.settings_help_fixed_detail)),
                )
            }
            cell(2) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_productive_hours),
                    value = state.productiveHoursPerMonthText,
                    onValueChange = { text -> viewModel.update { it.copy(productiveHoursPerMonthText = text) } },
                    kind = NumberKind.AMOUNT,
                    error = state.errors[SettingsField.PRODUCTIVE_HOURS]?.asString(),
                    // Custo fixo sem horas pra dividir não entra no preço: o ícone de ajuda fica âmbar e explica.
                    help = HelpText(stringResource(Res.string.settings_warning_fixed_cost), warning = true).takeIf { state.fixedCostWithoutHours },
                )
            }
        }
    }

    FormSection(4, stringResource(Res.string.settings_failures_title)) {
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_failure_rate),
                    value = state.failureRatePercentText,
                    onValueChange = { text -> viewModel.update { it.copy(failureRatePercentText = text) } },
                    kind = NumberKind.MEASURE,
                    error = state.errors[SettingsField.FAILURE_RATE]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_failure), stringResource(Res.string.settings_help_failure_detail)),
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_finishing_rate),
                    value = state.finishingRatePercentText,
                    onValueChange = { text -> viewModel.update { it.copy(finishingRatePercentText = text) } },
                    kind = NumberKind.MEASURE,
                    error = state.errors[SettingsField.FINISHING_RATE]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_finishing), stringResource(Res.string.settings_help_finishing_detail)),
                )
            }
        }
    }

    FormSection(5, stringResource(Res.string.settings_margin_title)) {
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_margin),
                    value = state.profitMarginPercentText,
                    onValueChange = { text -> viewModel.update { it.copy(profitMarginPercentText = text) } },
                    kind = NumberKind.MEASURE,
                    error = state.errors[SettingsField.MARGIN]?.asString(),
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_tax),
                    value = state.taxRatePercentText,
                    onValueChange = { text -> viewModel.update { it.copy(taxRatePercentText = text) } },
                    kind = NumberKind.MEASURE,
                    error = state.errors[SettingsField.TAX]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_tax), stringResource(Res.string.settings_help_tax_detail)),
                )
            }
        }
    }

    FormSection(6, stringResource(Res.string.settings_order_title)) {
        FormGrid {
            cell(2) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_admin_cost),
                    value = state.administrativeCostText,
                    onValueChange = { text -> viewModel.update { it.copy(administrativeCostText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    error = state.errors[SettingsField.ADMINISTRATIVE_COST]?.asString(),
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_minimum_price),
                    value = state.minimumOrderPriceText,
                    onValueChange = { text -> viewModel.update { it.copy(minimumOrderPriceText = text) } },
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    error = state.errors[SettingsField.MINIMUM_PRICE]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_minimum), stringResource(Res.string.settings_help_minimum_detail)),
                )
            }
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_field_rush),
                    value = state.rushSurchargeRatePercentText,
                    onValueChange = { text -> viewModel.update { it.copy(rushSurchargeRatePercentText = text) } },
                    kind = NumberKind.MEASURE,
                    error = state.errors[SettingsField.RUSH]?.asString(),
                    help = HelpText(stringResource(Res.string.settings_help_rush), stringResource(Res.string.settings_help_rush_detail)),
                )
            }
        }
    }

    if (showTariffDialog) {
        EnergyTariffDialog(
            onDismiss = { showTariffDialog = false },
            onUse = { price ->
                viewModel.update { it.copy(energyPricePerKwhText = price.toInputText(currency.decimalSeparator)) }
                showTariffDialog = false
            },
        )
    }
}
