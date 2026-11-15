package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_cannot_save_field
import _3dreport.composeapp.generated.resources.quote_cannot_save_incomplete
import _3dreport.composeapp.generated.resources.quote_cannot_save_service
import _3dreport.composeapp.generated.resources.quote_field_name
import _3dreport.composeapp.generated.resources.quote_field_quantity
import _3dreport.composeapp.generated.resources.quote_help_details
import _3dreport.composeapp.generated.resources.quote_help_kind_order
import _3dreport.composeapp.generated.resources.quote_help_kind_product
import _3dreport.composeapp.generated.resources.quote_help_quantity
import _3dreport.composeapp.generated.resources.quote_help_quantity_detail
import _3dreport.composeapp.generated.resources.quote_kind_order
import _3dreport.composeapp.generated.resources.quote_kind_product
import _3dreport.composeapp.generated.resources.quote_op_copying
import _3dreport.composeapp.generated.resources.quote_op_copying_cancel
import _3dreport.composeapp.generated.resources.quote_op_copying_help
import _3dreport.composeapp.generated.resources.quote_op_duplicating
import _3dreport.composeapp.generated.resources.quote_op_duplicating_cancel
import _3dreport.composeapp.generated.resources.quote_op_duplicating_help_order
import _3dreport.composeapp.generated.resources.quote_op_duplicating_help_product
import _3dreport.composeapp.generated.resources.quote_op_editing
import _3dreport.composeapp.generated.resources.quote_op_editing_cancel
import _3dreport.composeapp.generated.resources.quote_op_editing_help_order
import _3dreport.composeapp.generated.resources.quote_op_editing_help_product
import _3dreport.composeapp.generated.resources.quote_op_editing_numbered
import _3dreport.composeapp.generated.resources.quote_op_selling
import _3dreport.composeapp.generated.resources.quote_op_selling_cancel
import _3dreport.composeapp.generated.resources.quote_op_selling_help
import _3dreport.composeapp.generated.resources.quote_save_changes
import _3dreport.composeapp.generated.resources.quote_save_order
import _3dreport.composeapp.generated.resources.quote_save_product
import _3dreport.composeapp.generated.resources.quote_saved_order
import _3dreport.composeapp.generated.resources.quote_saved_product
import _3dreport.composeapp.generated.resources.quote_section_details
import _3dreport.composeapp.generated.resources.quote_section_extras
import _3dreport.composeapp.generated.resources.quote_section_print
import _3dreport.composeapp.generated.resources.quote_section_prints
import _3dreport.composeapp.generated.resources.quote_section_sale
import _3dreport.composeapp.generated.resources.quote_title_order
import _3dreport.composeapp.generated.resources.quote_title_product
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ObserveUiNotices
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Currency
import com.threedreport.core.model.QuoteKind
import org.jetbrains.compose.resources.stringResource

/**
 * Tela de Orçamento: à esquerda o formulário em seções numeradas, à direita o painel de resultado fixo.
 * [onSave] é o botão Salvar: num orçamento novo, salva e limpa pra o próximo; editando um salvo
 * (decisão 113), salva e volta pra onde a edição começou.
 */
@Composable
fun QuoteScreen(
    viewModel: QuoteViewModel,
    modifier: Modifier = Modifier,
    onSave: () -> Unit = { viewModel.saveCurrentQuote() },
    onCancelOperation: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val catalog = state.catalog
    val input = state.input
    val saveForm = state.form
    // A conta é do ViewModel: a mesma que o Ctrl+S salva.
    val result = state.result
    val currency = LocalCurrency.current
    val space = MaterialTheme.spacing

    val isEditing = saveForm.editingQuoteId != null
    val canSave = result.quote != null && !result.missingServicePrice && result.fieldErrors.isEmpty()
    val errorMessage = result.errorMessage?.asString()
    val cannotSaveReason = when {
        result.fieldErrors.isNotEmpty() -> stringResource(Res.string.quote_cannot_save_field)
        result.quote != null && result.missingServicePrice -> stringResource(Res.string.quote_cannot_save_service)
        errorMessage != null -> errorMessage
        else -> stringResource(Res.string.quote_cannot_save_incomplete)
    }
    val saveLabel = stringResource(
        when {
            isEditing -> Res.string.quote_save_changes
            input.isProduct -> Res.string.quote_save_product
            else -> Res.string.quote_save_order
        },
    )
    val cancelLabel = operationTexts(saveForm, input.isProduct)?.cancelLabel

    @Composable
    fun Form() = QuoteForm(viewModel, state, currency)

    @Composable
    fun Panel(fixed: Boolean, panelModifier: Modifier) = QuoteResultPanel(
        result = result,
        input = input,
        saveForm = saveForm,
        settings = catalog.settings,
        allFilaments = catalog.filaments,
        printers = catalog.printers,
        comparison = state.comparison,
        negotiating = state.negotiating,
        onNegotiatingChange = viewModel::setNegotiating,
        canSave = canSave,
        cannotSaveReason = cannotSaveReason,
        saveLabel = saveLabel,
        cancelLabel = cancelLabel,
        fixed = fixed,
        onSave = onSave,
        onCancel = onCancelOperation,
        onUseShippingCoveringPrice = viewModel::useShippingCoveringPrice,
        onTargetTotalChange = viewModel::setTargetTotal,
        send = ClientSendActions(
            onSend = viewModel::sendToClient,
            onPdf = viewModel::sendPdf,
            onImage = viewModel::sendImage,
            onWhatsApp = viewModel::sendWhatsApp,
            onCopy = viewModel::sendCopy,
            exporting = state.exporting,
        ),
        modifier = panelModifier,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= AppDimens.twoColumnMinWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl),
                ) { Form() }
                Panel(fixed = true, panelModifier = Modifier.width(AppDimens.resultPanelWidth).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.padding(start = space.xl, end = space.xl, top = AppDimens.screenTopPadding, bottom = space.xl)) { Form() }
                Panel(fixed = false, panelModifier = Modifier.fillMaxWidth())
            }
        }
    }

    saveForm.blockedMessage?.let { message ->
        ShowSnackbarOnce(true, message.asString(), viewModel::consumeBlockedMessage)
    }
    val savedText = stringResource(
        if (saveForm.savedAsProduct) Res.string.quote_saved_product else Res.string.quote_saved_order,
        saveForm.savedNumber.orEmpty(),
    ).replace("  ", " ")
    ShowSnackbarOnce(saveForm.savedConfirmation, savedText, viewModel::consumeSavedConfirmation)
    ObserveUiNotices(viewModel.notices)
}

/** O que a tela está fazendo além de um orçamento novo: título, o detalhe (numa dica) e o texto de desistir. */
private data class OperationTexts(val title: String, val detail: String, val cancelLabel: String)

@Composable
private fun operationTexts(form: SaveQuoteFormState, isProduct: Boolean): OperationTexts? = when (val operation = form.operation) {
    is QuoteOperation.Editing -> {
        val saved = operation.savedQuote
        val number = saved.displayNumber?.takeIf { saved.isOrder }
        OperationTexts(
            if (number != null) {
                stringResource(Res.string.quote_op_editing_numbered, number, saved.name)
            } else {
                stringResource(Res.string.quote_op_editing, saved.name)
            },
            stringResource(if (isProduct) Res.string.quote_op_editing_help_product else Res.string.quote_op_editing_help_order),
            stringResource(Res.string.quote_op_editing_cancel),
        )
    }

    is QuoteOperation.Selling -> OperationTexts(
        stringResource(Res.string.quote_op_selling, operation.product.name),
        stringResource(Res.string.quote_op_selling_help),
        stringResource(Res.string.quote_op_selling_cancel),
    )

    is QuoteOperation.CopyingToCatalog -> OperationTexts(
        stringResource(Res.string.quote_op_copying, operation.fromName),
        stringResource(Res.string.quote_op_copying_help),
        stringResource(Res.string.quote_op_copying_cancel),
    )

    is QuoteOperation.Duplicating -> OperationTexts(
        stringResource(Res.string.quote_op_duplicating, operation.fromName),
        stringResource(if (isProduct) Res.string.quote_op_duplicating_help_product else Res.string.quote_op_duplicating_help_order),
        stringResource(Res.string.quote_op_duplicating_cancel),
    )

    else -> null
}

/** O formulário: cabeçalho, área de G-code e as seções numeradas. */
@Composable
private fun QuoteForm(
    viewModel: QuoteViewModel,
    state: QuoteUiState,
    currency: Currency,
) {
    val space = MaterialTheme.spacing
    val catalog = state.catalog
    val input = state.input
    val form = state.form
    val result = state.result
    val extras = extrasOffer(catalog.settings, catalog.services, catalog.consumables, input)
    var number = 0

    Column(modifier = Modifier.widthIn(max = AppDimens.formMaxWidth), verticalArrangement = Arrangement.spacedBy(space.xl)) {
        Header(viewModel, input, form)
        GCodeDropArea(viewModel, input, state.importing)

        FormSection(++number, stringResource(if (input.prints.size > 1) Res.string.quote_section_prints else Res.string.quote_section_print)) {
            Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
                FormGrid {
                    cell(2) {
                        AppTextField(
                            label = stringResource(Res.string.quote_field_name),
                            value = form.name,
                            onValueChange = viewModel::setSaveName,
                        )
                    }
                    cell {
                        AppTextField(
                            label = stringResource(Res.string.quote_field_quantity),
                            value = input.quantityText,
                            onValueChange = viewModel::setQuantity,
                            placeholder = "1",
                            error = result.errorText(QuoteFields.QUANTITY),
                            help = HelpText(stringResource(Res.string.quote_help_quantity), stringResource(Res.string.quote_help_quantity_detail)),
                        )
                    }
                }
                PrintsSection(viewModel, catalog.filaments, catalog.printers, input, result, currency, state.importing, state.collapsedPrintIds)
            }
        }

        if (!extras.isEmpty) {
            FormSection(++number, stringResource(Res.string.quote_section_extras)) {
                ExtrasFields(viewModel, extras, catalog.settings, input, result, currency)
            }
        }

        FormSection(++number, stringResource(Res.string.quote_section_sale)) { SaleFields(viewModel, catalog, input, form, result, currency) }

        FormSection(
            ++number,
            stringResource(Res.string.quote_section_details),
            help = HelpText(stringResource(Res.string.quote_help_details)),
        ) { DetailsFields(viewModel, catalog, state.stlPreview, input, form, state.queueHints) }
    }
}

@Composable
private fun Header(viewModel: QuoteViewModel, input: QuoteInputState, form: SaveQuoteFormState) {
    val operation = operationTexts(form, input.isProduct)
    // O tipo vem primeiro (decisão 108): ele esconde frete e negociação, então escolher depois de
    // preencher era jogar trabalho fora. Na edição, e quando o formulário veio de "Vender" ou de
    // "Guardar no catálogo", o destino já está decidido.
    val showKind = form.operation == null || form.operation is QuoteOperation.Duplicating
    val title = operation?.title ?: stringResource(if (input.isProduct) Res.string.quote_title_product else Res.string.quote_title_order)
    val help = operation?.detail ?: stringResource(if (input.isProduct) Res.string.quote_help_kind_product else Res.string.quote_help_kind_order)
    val kindOptions = listOf(stringResource(Res.string.quote_kind_order), stringResource(Res.string.quote_kind_product))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Text(title, style = MaterialTheme.appType.headline, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f, fill = false))
            HelpHint(HelpText(help), title)
        }
        if (showKind) {
            SegmentedChoice(
                options = kindOptions,
                selectedIndex = if (input.isProduct) 1 else 0,
                onSelect = { viewModel.setKind(if (it == 1) QuoteKind.PRODUCT else QuoteKind.ORDER) },
            )
        }
    }
}

/** O texto do erro do campo [key], ou `null` se ele está válido. */
@Composable
internal fun QuoteResult.errorText(key: String): String? = fieldErrors[key]?.asString()
