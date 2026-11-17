package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_color
import _3dreport.composeapp.generated.resources.quote_color_n
import _3dreport.composeapp.generated.resources.quote_filament
import _3dreport.composeapp.generated.resources.quote_filament_add
import _3dreport.composeapp.generated.resources.quote_filament_choose
import _3dreport.composeapp.generated.resources.quote_filament_n
import _3dreport.composeapp.generated.resources.quote_filament_not_registered
import _3dreport.composeapp.generated.resources.quote_filament_remove
import _3dreport.composeapp.generated.resources.quote_help_multicolor
import _3dreport.composeapp.generated.resources.quote_help_multicolor_detail
import _3dreport.composeapp.generated.resources.quote_help_runs
import _3dreport.composeapp.generated.resources.quote_help_runs_detail
import _3dreport.composeapp.generated.resources.quote_help_weight_disabled
import _3dreport.composeapp.generated.resources.quote_length
import _3dreport.composeapp.generated.resources.quote_length_n
import _3dreport.composeapp.generated.resources.quote_mark_archived
import _3dreport.composeapp.generated.resources.quote_mark_archived_female
import _3dreport.composeapp.generated.resources.quote_mark_out_of_stock
import _3dreport.composeapp.generated.resources.quote_print_time
import _3dreport.composeapp.generated.resources.quote_print_time_placeholder
import _3dreport.composeapp.generated.resources.quote_printer
import _3dreport.composeapp.generated.resources.quote_printer_choose
import _3dreport.composeapp.generated.resources.quote_printer_not_registered
import _3dreport.composeapp.generated.resources.quote_runs
import _3dreport.composeapp.generated.resources.quote_weight
import _3dreport.composeapp.generated.resources.quote_weight_n
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FieldRowAction
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.components.availableOrSelected
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round

/** Os campos de uma impressão: filamentos, tempo, impressora e, nos blocos, quantas vezes a mesa roda. */
@Composable
internal fun PrintFields(
    viewModel: QuoteViewModel,
    inStock: List<Filament>,
    printers: List<PrinterProfile>,
    print: PrintInput,
    resolved: ResolvedPrint?,
    result: QuoteResult,
    currency: Currency,
    showRuns: Boolean,
) {
    val space = MaterialTheme.spacing
    val multicolor = print.filaments.size > 1
    val archivedMark = stringResource(Res.string.quote_mark_archived_female)
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        print.filaments.forEachIndexed { slot, row ->
            key(row.id) {
                FilamentRow(
                    viewModel = viewModel,
                    inStock = inStock,
                    printId = print.id,
                    row = row,
                    resolved = resolved?.filaments?.getOrNull(slot),
                    number = slot + 1,
                    multicolor = multicolor,
                    currency = currency,
                    error = result.errorText(QuoteFields.length(print.id, row.id)),
                )
            }
        }
        AppButton(stringResource(Res.string.quote_filament_add), onClick = { viewModel.addFilament(print.id) }, icon = AppIcons.Add)

        val timeError = result.errorText(QuoteFields.printTime(print.id))
        val runsError = result.errorText(QuoteFields.runs(print.id))
        FormGrid {
            cell {
                AppTextField(
                    label = stringResource(Res.string.quote_print_time),
                    value = print.printTimeText,
                    onValueChange = { viewModel.setPrintTimeMinutes(it, print.id) },
                    placeholder = stringResource(Res.string.quote_print_time_placeholder),
                    error = timeError,
                )
            }
            cell(if (showRuns) 1 else 2) {
                AppDropdownField(
                    label = stringResource(Res.string.quote_printer),
                    items = printers.availableOrSelected { it.id == resolved?.printer?.id },
                    selected = resolved?.printer,
                    itemLabel = { it.name + if (it.archived) " $archivedMark" else "" },
                    displayText = { it.name + if (it.archived) " $archivedMark" else "" },
                    onSelect = { viewModel.selectPrinter(it.id, print.id) },
                    emptyText = print.missingPrinterName?.let { stringResource(Res.string.quote_printer_not_registered, it) }
                        ?: stringResource(Res.string.quote_printer_choose),
                )
            }
            if (showRuns) {
                cell {
                    AppTextField(
                        label = stringResource(Res.string.quote_runs),
                        value = print.runsText,
                        onValueChange = { viewModel.setRuns(it, print.id) },
                        placeholder = "1",
                        error = runsError,
                        help = HelpText(stringResource(Res.string.quote_help_runs), stringResource(Res.string.quote_help_runs_detail)),
                    )
                }
            }
        }
    }
}

/**
 * Uma linha de filamento da impressão: filamento, cor, e o consumo em gramas ou em metros (decisão
 * 107). Na peça multicolor, cada linha ganha número e o botão de remover, e começa sem filamento quando
 * o G-code não disse qual é (decisão 105).
 */
@Composable
private fun FilamentRow(
    viewModel: QuoteViewModel,
    inStock: List<Filament>,
    printId: Int,
    row: FilamentInput,
    resolved: ResolvedFilament?,
    number: Int,
    multicolor: Boolean,
    currency: Currency,
    error: String?,
) {
    // Um filamento escolhido que acabou continua na lista (marcado), pra um pedido reaberto
    // não trocar de filamento sozinho; os outros que acabaram ficam de fora.
    val chosen = resolved?.filament
    val options = if (chosen != null && chosen !in inStock) inStock + chosen else inStock
    val filament = resolved?.filament
    val colors = filament?.colors.orEmpty().filter { it.inStock || it.id == resolved?.color?.id }
    val archivedMark = stringResource(Res.string.quote_mark_archived)
    val outOfStockMark = stringResource(Res.string.quote_mark_out_of_stock)

    fun Filament.availabilityMark(): String = when {
        archived -> " $archivedMark"
        hasStockAvailable -> ""
        else -> " $outOfStockMark"
    }
    // Peso e metros são o mesmo consumo: digitar um preenche o outro, pelo filamento escolhido.
    val derivedWeight = row.weightText ?: run {
        val separator = LocalCurrency.current.decimalSeparator
        val meters = parseDecimal(row.lengthText, NumberKind.MEASURE, separator)
        if (filament != null && meters != null) (round(filament.weightGrams(meters) * 100) / 100).toInputText(separator) else ""
    }
    val removeDescription = stringResource(Res.string.quote_filament_remove, number)
    FormGrid {
        cell {
            AppDropdownField(
                label = if (multicolor) stringResource(Res.string.quote_filament_n, number) else stringResource(Res.string.quote_filament),
                items = options,
                selected = chosen,
                itemLabel = { "${it.name} · ${it.pricePerKg.toCurrencyText(currency)}/kg" + it.availabilityMark() },
                displayText = { it.name + it.availabilityMark() },
                onSelect = { viewModel.selectFilament(it.id, printId, row.id) },
                emptyText = row.missingFilamentName?.let { stringResource(Res.string.quote_filament_not_registered, it) }
                    ?: stringResource(Res.string.quote_filament_choose),
                help = if (multicolor && number == 1) {
                    HelpText(stringResource(Res.string.quote_help_multicolor), stringResource(Res.string.quote_help_multicolor_detail))
                } else {
                    null
                },
            )
        }
        if (colors.size > 1) {
            cell {
                AppDropdownField(
                    label = if (multicolor) stringResource(Res.string.quote_color_n, number) else stringResource(Res.string.quote_color),
                    items = colors,
                    selected = resolved?.color,
                    itemLabel = { it.displayLabel() + if (it.inStock) "" else " $outOfStockMark" },
                    displayText = { it.displayLabel() },
                    onSelect = { viewModel.selectFilamentColor(it.id, printId, row.id) },
                )
            }
        }
        cell {
            AppTextField(
                label = if (multicolor) stringResource(Res.string.quote_weight_n, number) else stringResource(Res.string.quote_weight),
                value = derivedWeight,
                onValueChange = { viewModel.setWeightGrams(it, printId, row.id) },
                enabled = filament != null,
                help = if (filament == null) HelpText(stringResource(Res.string.quote_help_weight_disabled)) else null,
            )
        }
        cell {
            AppTextField(
                label = if (multicolor) stringResource(Res.string.quote_length_n, number) else stringResource(Res.string.quote_length),
                value = row.lengthText,
                onValueChange = { viewModel.setLengthMeters(it, printId, row.id) },
                error = error,
            )
        }
        if (multicolor) {
            cell {
                FieldRowAction {
                    IconAction(AppIcons.Close, removeDescription, onClick = { viewModel.removeFilament(row.id, printId) })
                }
            }
        }
    }
}
