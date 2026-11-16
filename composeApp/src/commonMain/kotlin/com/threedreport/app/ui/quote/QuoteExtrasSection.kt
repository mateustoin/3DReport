package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_consumable_field_quantity
import _3dreport.composeapp.generated.resources.quote_consumable_removed
import _3dreport.composeapp.generated.resources.quote_extras_consumables
import _3dreport.composeapp.generated.resources.quote_extras_services
import _3dreport.composeapp.generated.resources.quote_help_services
import _3dreport.composeapp.generated.resources.quote_labor_help_detail
import _3dreport.composeapp.generated.resources.quote_labor_help_many
import _3dreport.composeapp.generated.resources.quote_labor_help_one
import _3dreport.composeapp.generated.resources.quote_labor_label
import _3dreport.composeapp.generated.resources.quote_labor_missing
import _3dreport.composeapp.generated.resources.quote_labor_placeholder
import _3dreport.composeapp.generated.resources.quote_service_error_not_number
import _3dreport.composeapp.generated.resources.quote_service_error_required
import _3dreport.composeapp.generated.resources.quote_service_field_minutes
import _3dreport.composeapp.generated.resources.quote_service_field_value
import _3dreport.composeapp.generated.resources.quote_service_field_value_per_piece
import _3dreport.composeapp.generated.resources.quote_service_suggested
import _3dreport.composeapp.generated.resources.quote_service_total
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.availableOrSelected
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.Service
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * O que a seção "Trabalho e extras" oferece: o campo de trabalho (só pra quem configurou a própria hora), os
 * serviços e os insumos. Um serviço ou insumo marcado num orçamento reaberto que já saiu do cadastro continua
 * aparecendo ("órfão"), pra não sumir do preço sem ninguém ver.
 */
internal data class ExtrasOffer(
    val services: List<Service>,
    val orphanServices: Map<String, ServiceInput>,
    val consumables: List<Consumable>,
    val orphanConsumables: Map<String, ConsumableInput>,
    val showLabor: Boolean,
) {
    val hasServices: Boolean get() = services.isNotEmpty() || orphanServices.isNotEmpty()
    val hasConsumables: Boolean get() = consumables.isNotEmpty() || orphanConsumables.isNotEmpty()

    /** A seção só existe com algo a oferecer. */
    val isEmpty: Boolean get() = !showLabor && !hasServices && !hasConsumables
}

internal fun extrasOffer(settings: PricingSettings, services: List<Service>, consumables: List<Consumable>, input: QuoteInputState): ExtrasOffer =
    ExtrasOffer(
        services = services.availableOrSelected { it.id in input.selectedServices },
        orphanServices = input.selectedServices.filterKeys { id -> services.none { it.id == id } },
        consumables = consumables.availableOrSelected { it.id in input.selectedConsumables },
        orphanConsumables = input.selectedConsumables.filterKeys { id -> consumables.none { it.id == id } },
        // Sem a hora configurada, o campo não teria efeito nenhum no preço.
        showLabor = settings.laborRatePerHour > 0,
    )

/** Mão de obra, serviços e insumos: o corpo da seção "Trabalho e extras". */
@Composable
internal fun ExtrasFields(
    viewModel: QuoteViewModel,
    offer: ExtrasOffer,
    settings: PricingSettings,
    input: QuoteInputState,
    result: QuoteResult,
    currency: Currency,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        // O tempo é do pedido inteiro (decisão 94).
        if (offer.showLabor) LaborField(viewModel, settings, input, result, currency)
        if (offer.hasServices) {
            ExtrasList(stringResource(Res.string.quote_extras_services), HelpText(stringResource(Res.string.quote_help_services))) {
                // Serviço marcado num orçamento reaberto que já saiu do catálogo continua aparecendo.
                offer.services.forEach { service ->
                    ServiceRow(
                        viewModel, service.id, service.name, service.suggestedPrice, input.selectedServices[service.id],
                        input.quantity, result.errorText(QuoteFields.serviceMinutes(service.id)),
                    )
                }
                offer.orphanServices.forEach { (id, serviceInput) ->
                    ServiceRow(viewModel, id, serviceInput.name, null, serviceInput, input.quantity, result.errorText(QuoteFields.serviceMinutes(id)))
                }
            }
        }
        if (offer.hasConsumables) {
            // Insumo argola, ímã, caixa: vale pra pedido e pra produto (decisão 122).
            ExtrasList(stringResource(Res.string.quote_extras_consumables), null) {
                offer.consumables.forEach { consumable ->
                    ConsumableRow(
                        viewModel, consumable.id, consumable.name, consumable.unitCost, input.selectedConsumables[consumable.id],
                        input.quantity, result.errorText(QuoteFields.consumable(consumable.id)),
                    )
                }
                offer.orphanConsumables.forEach { (id, consumableInput) ->
                    ConsumableRow(
                        viewModel, id, stringResource(Res.string.quote_consumable_removed, consumableInput.name), consumableInput.unitCost, consumableInput,
                        input.quantity, result.errorText(QuoteFields.consumable(id)),
                    )
                }
            }
        }
    }
}

@Composable
private fun LaborField(viewModel: QuoteViewModel, settings: PricingSettings, input: QuoteInputState, result: QuoteResult, currency: Currency) {
    val quantity = input.quantity
    val rate = settings.laborRatePerHour.toCurrencyText(currency)
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        FormGrid {
            cell {
                AppTextField(
                    label = stringResource(Res.string.quote_labor_label),
                    value = input.laborMinutesText,
                    onValueChange = viewModel::setLaborMinutes,
                    placeholder = stringResource(Res.string.quote_labor_placeholder),
                    error = result.errorText(QuoteFields.LABOR),
                    help = HelpText(
                        if (quantity > 1) {
                            stringResource(Res.string.quote_labor_help_many, quantity, rate)
                        } else {
                            stringResource(Res.string.quote_labor_help_one, rate)
                        },
                        stringResource(Res.string.quote_labor_help_detail),
                    ),
                )
            }
        }
        // Lembrete, não erro: sem ele, configurar a hora e não ver o preço mudar parece defeito.
        if (input.isLaborTimeMissing) {
            Text(stringResource(Res.string.quote_labor_missing), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Um grupo de linhas marcáveis (serviços, insumos) com rótulo em `label`; é lista, não cartão. */
@Composable
private fun ExtrasList(label: String, help: HelpText?, content: @Composable () -> Unit) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
            Text(label, style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (help != null) HelpHint(help, label)
        }
        content()
    }
}

/** Recuo do conteúdo de uma linha marcada, alinhado com o texto depois da caixa. */
private fun indentUnderCheckbox(): Modifier = Modifier.padding(start = AppDimens.checkboxIndent)

/**
 * Um serviço na lista do orçamento. Desmarcado, só o nome (e o valor sugerido, se houver). Marcado, ganha o
 * campo de valor deste pedido e, com mais de uma peça, a escolha entre cobrar por peça ou uma vez no pedido,
 * com o total ao lado pra conta nunca virar surpresa.
 */
@Composable
private fun ServiceRow(
    viewModel: QuoteViewModel,
    id: String,
    name: String,
    suggestedPrice: Double?,
    serviceInput: ServiceInput?,
    quantity: Int,
    minutesError: String? = null,
) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        val checkLabel = if (serviceInput == null && suggestedPrice != null) {
            stringResource(Res.string.quote_service_suggested, name, suggestedPrice.toMoney())
        } else {
            name
        }
        AppCheckbox(checkLabel, serviceInput != null, { viewModel.toggleService(id) })
        if (serviceInput == null) return@Column

        val price = parseDecimal(serviceInput.priceText, NumberKind.AMOUNT, LocalCurrency.current.decimalSeparator)?.takeIf { it >= 0 }
        Column(modifier = indentUnderCheckbox(), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            FormGrid {
                cell {
                    AppTextField(
                        label = stringResource(
                            if (quantity > 1 && !serviceInput.chargedPerOrder) Res.string.quote_service_field_value_per_piece else Res.string.quote_service_field_value,
                        ),
                        value = serviceInput.priceText,
                        onValueChange = { viewModel.setServicePrice(id, it) },
                        error = if (price == null) {
                            stringResource(if (serviceInput.priceText.isBlank()) Res.string.quote_service_error_required else Res.string.quote_service_error_not_number)
                        } else {
                            null
                        },
                    )
                }
                // Opcional (decisão 123): sem tempo informado, o serviço continua sendo repasse, fora do lucro.
                cell {
                    AppTextField(
                        label = stringResource(Res.string.quote_service_field_minutes),
                        value = serviceInput.laborMinutesText,
                        onValueChange = { viewModel.setServiceLaborMinutes(id, it) },
                        placeholder = "30",
                        error = minutesError,
                    )
                }
            }
            if (quantity > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                    ServiceChargeSelector(
                        chargedPerOrder = serviceInput.chargedPerOrder,
                        onChange = { viewModel.setServiceChargedPerOrder(id, it) },
                    )
                    if (price != null) {
                        val total = if (serviceInput.chargedPerOrder) price else price * quantity
                        NumericText(stringResource(Res.string.quote_service_total, total.toMoney()), style = MaterialTheme.appType.body)
                    }
                }
            }
        }
    }
}

/**
 * Um insumo na lista do orçamento (decisão 122): argola, ímã, caixa. Desmarcado, só o nome. Marcado, ganha a
 * quantidade (por peça ou pelo pedido, com mais de uma peça) e o custo unitário do cadastro, só de referência.
 */
@Composable
private fun ConsumableRow(
    viewModel: QuoteViewModel,
    id: String,
    name: String,
    unitCost: Double,
    consumableInput: ConsumableInput?,
    orderQuantity: Int,
    quantityError: String?,
) {
    val space = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        AppCheckbox(name, consumableInput != null, { viewModel.toggleConsumable(id) })
        if (consumableInput == null) return@Column

        Column(modifier = indentUnderCheckbox(), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            FormGrid {
                cell {
                    AppTextField(
                        label = stringResource(Res.string.quote_consumable_field_quantity, unitCost.toMoney()),
                        value = consumableInput.quantityText,
                        onValueChange = { viewModel.setConsumableQuantity(id, it) },
                        placeholder = "1",
                        error = quantityError,
                    )
                }
            }
            if (orderQuantity > 1) {
                ServiceChargeSelector(
                    chargedPerOrder = consumableInput.chargedPerOrder,
                    onChange = { viewModel.setConsumableChargedPerOrder(id, it) },
                )
            }
        }
    }
}
