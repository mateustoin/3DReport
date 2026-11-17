package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_help_channel
import _3dreport.composeapp.generated.resources.quote_help_channel_detail
import _3dreport.composeapp.generated.resources.quote_help_client_private
import _3dreport.composeapp.generated.resources.quote_help_free_shipping
import _3dreport.composeapp.generated.resources.quote_help_shipping
import _3dreport.composeapp.generated.resources.quote_help_shipping_detail
import _3dreport.composeapp.generated.resources.quote_help_target_order
import _3dreport.composeapp.generated.resources.quote_help_target_order_detail
import _3dreport.composeapp.generated.resources.quote_help_target_product
import _3dreport.composeapp.generated.resources.quote_help_target_product_detail
import _3dreport.composeapp.generated.resources.quote_sale_announced_price
import _3dreport.composeapp.generated.resources.quote_sale_channel_deleted
import _3dreport.composeapp.generated.resources.quote_sale_channel_missing
import _3dreport.composeapp.generated.resources.quote_sale_contact_placeholder
import _3dreport.composeapp.generated.resources.quote_sale_direct
import _3dreport.composeapp.generated.resources.quote_sale_field_channel
import _3dreport.composeapp.generated.resources.quote_sale_field_client
import _3dreport.composeapp.generated.resources.quote_sale_field_contact
import _3dreport.composeapp.generated.resources.quote_sale_field_shipping
import _3dreport.composeapp.generated.resources.quote_sale_field_target_order
import _3dreport.composeapp.generated.resources.quote_sale_field_target_product
import _3dreport.composeapp.generated.resources.quote_sale_free_shipping
import _3dreport.composeapp.generated.resources.quote_sale_free_shipping_hint
import _3dreport.composeapp.generated.resources.quote_sale_round_to
import _3dreport.composeapp.generated.resources.quote_sale_rush
import _3dreport.composeapp.generated.resources.quote_sale_rush_suggest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.availableOrSelected
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.channelFeeSummaries
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Currency
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.pricing.showcasePriceSuggestion
import org.jetbrains.compose.resources.stringResource

/** Canal, frete, cliente, preço combinado (ou anunciado) e as opções de frete grátis e urgência. */
@Composable
internal fun SaleFields(
    viewModel: QuoteViewModel,
    catalog: QuoteCatalog,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    result: QuoteResult,
    currency: Currency,
    today: Long,
) {
    val space = MaterialTheme.spacing
    val settings: PricingSettings = catalog.settings
    val quote = result.quote
    // Com o canal do orçamento excluído, o seletor aparece mesmo sem canais cadastrados: é nele que se
    // escolhe "Venda direta" pra liberar o cálculo.
    val offeredChannels: List<SalesChannel> = catalog.salesChannels.availableOrSelected { it.id == input.salesChannelId }
    val directSale = stringResource(Res.string.quote_sale_direct)
    val feeSummaries = channelFeeSummaries(offeredChannels, currency)
    val clientOnlyForYou = HelpText(stringResource(Res.string.quote_help_client_private))

    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        FormGrid {
            if (offeredChannels.isNotEmpty() || input.missingChannelName != null) {
                cell {
                    AppDropdownField(
                        label = stringResource(Res.string.quote_sale_field_channel),
                        items = listOf(null) + offeredChannels,
                        selected = result.salesChannel,
                        itemLabel = { it?.let { channel -> "${channel.name} · ${feeSummaries[channel.id].orEmpty()}" } ?: directSale },
                        displayText = { it?.name ?: directSale },
                        onSelect = { viewModel.selectSalesChannel(it?.id) },
                        emptyText = input.missingChannelName?.let { stringResource(Res.string.quote_sale_channel_deleted, it) } ?: directSale,
                        help = HelpText(stringResource(Res.string.quote_help_channel), stringResource(Res.string.quote_help_channel_detail)),
                    )
                }
            }
            // Produto do catálogo não tem frete (decisão 101): quem paga e pra onde vai só existe na venda.
            if (!input.isProduct) {
                cell {
                    AppTextField(
                        label = stringResource(Res.string.quote_sale_field_shipping, currency.symbol),
                        value = input.shippingCostText,
                        onValueChange = viewModel::setShippingCost,
                        error = result.errorText(QuoteFields.SHIPPING),
                        help = HelpText(stringResource(Res.string.quote_help_shipping), stringResource(Res.string.quote_help_shipping_detail)),
                    )
                }
                cell {
                    AppSuggestField(
                        label = stringResource(Res.string.quote_sale_field_client),
                        value = form.clientName,
                        onValueChange = viewModel::setClientName,
                        suggestions = viewModel.clientSuggestions(form.clientName, catalog.clients),
                        suggestionLabel = { client -> client.name + (client.contact?.let { " · $it" } ?: "") },
                        onChoose = viewModel::chooseClient,
                        help = clientOnlyForYou,
                    )
                }
                cell {
                    AppTextField(
                        label = stringResource(Res.string.quote_sale_field_contact),
                        value = form.clientContact,
                        onValueChange = viewModel::setClientContact,
                        placeholder = stringResource(Res.string.quote_sale_contact_placeholder),
                        help = clientOnlyForYou,
                    )
                }
            }
            // Produto do catálogo não tem cliente pra negociar (decisão 101), mas tem o preço que se anuncia
            // (decisão 102): o mesmo preço combinado, com outro nome.
            cell {
                AppTextField(
                    label = stringResource(if (input.isProduct) Res.string.quote_sale_field_target_product else Res.string.quote_sale_field_target_order),
                    value = input.targetTotalText,
                    onValueChange = viewModel::setTargetTotal,
                    error = result.errorText(QuoteFields.TARGET),
                    help = if (input.isProduct) {
                        HelpText(stringResource(Res.string.quote_help_target_product), stringResource(Res.string.quote_help_target_product_detail))
                    } else {
                        HelpText(stringResource(Res.string.quote_help_target_order), stringResource(Res.string.quote_help_target_order_detail))
                    },
                )
            }
        }

        input.missingChannelName?.let { name ->
            Text(
                stringResource(Res.string.quote_sale_channel_missing, name),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (quote != null) {
            // Só com o campo vazio: depois de aplicado, ou com um anunciado próprio, o atalho sobrava.
            val suggestion = if (input.isProduct && input.targetTotalText.isBlank()) {
                showcasePriceSuggestion((quote.tableSalePrice ?: quote.salePrice) + quote.extrasTotal)
            } else {
                null
            }
            suggestion?.let { AppButton(stringResource(Res.string.quote_sale_round_to, it.toMoney()), onClick = { viewModel.applyShowcasePrice(it) }) }
            input.announcedUnitPrice?.takeIf { input.targetTotalText.isBlank() && !input.isProduct }?.let { unit ->
                Text(
                    stringResource(Res.string.quote_sale_announced_price, (unit * input.quantity).toMoney()),
                    style = MaterialTheme.appType.caption,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (!input.isProduct) {
            // Só aparece com um frete digitado (decisão 124): marcado sem valor não faria nada.
            val shippingAmount = parseDecimal(input.shippingCostText, NumberKind.AMOUNT, LocalCurrency.current.decimalSeparator) ?: 0.0
            if (shippingAmount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
                    AppCheckbox(stringResource(Res.string.quote_sale_free_shipping), input.shippingAbsorbed, viewModel::setShippingAbsorbed)
                    HelpHint(HelpText(stringResource(Res.string.quote_help_free_shipping)), stringResource(Res.string.quote_sale_free_shipping_hint))
                }
            }
            // Urgência (decisão 125): só existe com o acréscimo configurado, senão a opção não teria efeito.
            if (settings.rushSurchargeRate > 0) {
                AppCheckbox(stringResource(Res.string.quote_sale_rush, settings.rushSurchargeRate.toPercentText()), input.rush, viewModel::setRush)
                val deadline = form.deliveryDateEpochDay
                if (!input.rush && deadline != null && deadline - today in 0..2) {
                    AppButton(stringResource(Res.string.quote_sale_rush_suggest), onClick = { viewModel.setRush(true) })
                }
            }
        }
    }
}
