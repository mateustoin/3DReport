package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_orders_deposit
import _3dreport.composeapp.generated.resources.settings_orders_deposit_help
import _3dreport.composeapp.generated.resources.settings_orders_stale
import _3dreport.composeapp.generated.resources.settings_orders_stale_help
import _3dreport.composeapp.generated.resources.settings_orders_tracking
import _3dreport.composeapp.generated.resources.settings_orders_tracking_default
import _3dreport.composeapp.generated.resources.settings_orders_tracking_help
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.text.asString
import com.threedreport.core.model.OrderSettings
import org.jetbrains.compose.resources.stringResource

/**
 * A seção Pedidos de Configurações: sinal padrão, quando um orçamento conta como parado e o site de rastreio. É uma
 * seção única, então sem título; cada campo explica o que faz numa dica. É rascunho até o "Salvar" da barra de alterações.
 */
@Composable
internal fun OrdersSection(viewModel: OrderSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    FormGrid {
        cell(1) {
            AppNumberField(
                label = stringResource(Res.string.settings_orders_deposit),
                value = state.depositPercentText,
                onValueChange = viewModel::setDepositPercent,
                kind = NumberKind.MEASURE,
                money = false,
                error = state.errors[OrderSettingsField.DEPOSIT]?.asString(),
                help = HelpText(stringResource(Res.string.settings_orders_deposit_help)),
            )
        }
        cell(1) {
            AppNumberField(
                label = stringResource(Res.string.settings_orders_stale),
                value = state.staleDaysText,
                onValueChange = viewModel::setStaleDays,
                kind = NumberKind.AMOUNT,
                error = state.errors[OrderSettingsField.STALE_DAYS]?.asString(),
                help = HelpText(stringResource(Res.string.settings_orders_stale_help)),
            )
        }
        cell(3) {
            AppTextField(
                label = stringResource(Res.string.settings_orders_tracking),
                value = state.trackingUrlText,
                onValueChange = viewModel::setTrackingUrl,
                help = HelpText(stringResource(Res.string.settings_orders_tracking_help)),
            )
        }
    }
    if (state.trackingUrlText.trim() != OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE) {
        AppButton(stringResource(Res.string.settings_orders_tracking_default), viewModel::useDefaultTracking)
    }
}
