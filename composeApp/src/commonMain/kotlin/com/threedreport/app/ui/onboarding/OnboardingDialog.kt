package com.threedreport.app.ui.onboarding

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.onboarding_action_back
import _3dreport.composeapp.generated.resources.onboarding_action_next
import _3dreport.composeapp.generated.resources.onboarding_action_skip
import _3dreport.composeapp.generated.resources.onboarding_action_start
import _3dreport.composeapp.generated.resources.onboarding_costs_title
import _3dreport.composeapp.generated.resources.onboarding_field_energy
import _3dreport.composeapp.generated.resources.onboarding_field_labor
import _3dreport.composeapp.generated.resources.onboarding_field_machine_price
import _3dreport.composeapp.generated.resources.onboarding_field_margin
import _3dreport.composeapp.generated.resources.onboarding_field_model
import _3dreport.composeapp.generated.resources.onboarding_help
import _3dreport.composeapp.generated.resources.onboarding_help_energy
import _3dreport.composeapp.generated.resources.onboarding_help_labor
import _3dreport.composeapp.generated.resources.onboarding_help_labor_detail
import _3dreport.composeapp.generated.resources.onboarding_help_machine_price
import _3dreport.composeapp.generated.resources.onboarding_help_margin
import _3dreport.composeapp.generated.resources.onboarding_help_margin_detail
import _3dreport.composeapp.generated.resources.onboarding_help_model_none
import _3dreport.composeapp.generated.resources.onboarding_help_model_none_detail
import _3dreport.composeapp.generated.resources.onboarding_help_model_preset
import _3dreport.composeapp.generated.resources.onboarding_help_model_preset_detail
import _3dreport.composeapp.generated.resources.onboarding_model_other
import _3dreport.composeapp.generated.resources.onboarding_printer_title
import _3dreport.composeapp.generated.resources.onboarding_step
import _3dreport.composeapp.generated.resources.onboarding_title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.printers.PRINTER_PRESETS
import com.threedreport.app.ui.printers.PrinterPreset
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appType
import com.threedreport.core.model.PricingSettings
import org.jetbrains.compose.resources.stringResource

/**
 * Perguntas da primeira execução, em dois passos curtos (a impressora; os custos e a margem), pra o app não abrir com a
 * conta calibrada pra outra pessoa. As respostas e a validação ficam no [OnboardingViewModel]. Dá pra pular: quem só quer ver
 * o app funcionando não deveria ser barrado por um formulário. Clicar fora não fecha: sem querer, a pessoa perderia o que
 * digitou e o onboarding não voltaria.
 */
@Composable
fun OnboardingDialog(
    currentSettings: PricingSettings,
    onFinish: (PricingSettings, OnboardingPrinter?) -> Unit,
    onSkip: () -> Unit,
) {
    val currency = LocalCurrency.current
    // Sem chave: as configurações mudarem com o diálogo aberto não pode recriar o ViewModel e apagar o que se digitou.
    val viewModel = remember { OnboardingViewModel(currentSettings, currency) }
    val state by viewModel.uiState.collectAsState()
    val symbol = currency.symbol
    val last = state.step == OnboardingStep.COSTS

    AppDialog(
        title = stringResource(Res.string.onboarding_title),
        onDismiss = onSkip,
        dismissOnClickOutside = false,
        help = HelpText(stringResource(Res.string.onboarding_help)),
        buttons = {
            AppButton(stringResource(Res.string.onboarding_action_skip), onSkip)
            if (last) AppButton(stringResource(Res.string.onboarding_action_back), viewModel::back)
            AppButton(
                stringResource(if (last) Res.string.onboarding_action_start else Res.string.onboarding_action_next),
                onClick = {
                    if (last) viewModel.finish()?.let { onFinish(it.settings, it.printer) } else viewModel.next()
                },
                kind = ButtonKind.Primary,
                enabled = state.canContinue,
            )
        },
    ) {
        Text(
            stringResource(Res.string.onboarding_step, state.stepNumber, OnboardingStep.entries.size),
            style = MaterialTheme.appType.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (state.step) {
            OnboardingStep.PRINTER -> FormSection(null, stringResource(Res.string.onboarding_printer_title)) {
                val other = stringResource(Res.string.onboarding_model_other)
                val preset = state.preset
                AppDropdownField(
                    label = stringResource(Res.string.onboarding_field_model),
                    items = listOf<PrinterPreset?>(null) + PRINTER_PRESETS,
                    selected = preset,
                    itemLabel = { it?.let { option -> "${option.brand} ${option.model}" } ?: other },
                    displayText = { it?.let { option -> "${option.brand} ${option.model}" } ?: other },
                    onSelect = viewModel::setPreset,
                    emptyText = other,
                    help = if (preset != null) {
                        HelpText(
                            stringResource(Res.string.onboarding_help_model_preset, preset.ratedPowerWatts.toInputText()),
                            stringResource(Res.string.onboarding_help_model_preset_detail),
                        )
                    } else {
                        HelpText(stringResource(Res.string.onboarding_help_model_none), stringResource(Res.string.onboarding_help_model_none_detail))
                    },
                )
                AppNumberField(
                    label = stringResource(Res.string.onboarding_field_machine_price),
                    value = state.machinePriceText,
                    onValueChange = viewModel::setMachinePrice,
                    kind = NumberKind.AMOUNT,
                    prefix = symbol,
                    placeholder = stringResource(Res.string.common_optional),
                    error = state.machinePriceError?.asString(),
                    help = HelpText(stringResource(Res.string.onboarding_help_machine_price)),
                )
            }

            OnboardingStep.COSTS -> FormSection(null, stringResource(Res.string.onboarding_costs_title)) {
                FormGrid {
                    cell(3) {
                        AppNumberField(
                            label = stringResource(Res.string.onboarding_field_energy),
                            value = state.energyText,
                            onValueChange = viewModel::setEnergy,
                            kind = NumberKind.MEASURE,
                            prefix = symbol,
                            error = state.energyError?.asString(),
                            help = HelpText(stringResource(Res.string.onboarding_help_energy)),
                        )
                    }
                    cell(3) {
                        AppNumberField(
                            label = stringResource(Res.string.onboarding_field_labor),
                            value = state.laborText,
                            onValueChange = viewModel::setLabor,
                            kind = NumberKind.AMOUNT,
                            prefix = symbol,
                            placeholder = stringResource(Res.string.common_optional),
                            error = state.laborError?.asString(),
                            help = HelpText(stringResource(Res.string.onboarding_help_labor), stringResource(Res.string.onboarding_help_labor_detail)),
                        )
                    }
                    cell(3) {
                        AppNumberField(
                            label = stringResource(Res.string.onboarding_field_margin),
                            value = state.marginText,
                            onValueChange = viewModel::setMargin,
                            kind = NumberKind.MEASURE,
                            error = state.marginError?.asString(),
                            help = HelpText(stringResource(Res.string.onboarding_help_margin), stringResource(Res.string.onboarding_help_margin_detail)),
                        )
                    }
                }
            }
        }
    }
}
