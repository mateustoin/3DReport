package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_gcode_undo
import _3dreport.composeapp.generated.resources.quote_print_add
import _3dreport.composeapp.generated.resources.quote_print_collapse
import _3dreport.composeapp.generated.resources.quote_print_duplicate
import _3dreport.composeapp.generated.resources.quote_print_field_name
import _3dreport.composeapp.generated.resources.quote_print_gcode
import _3dreport.composeapp.generated.resources.quote_print_name_placeholder
import _3dreport.composeapp.generated.resources.quote_print_open
import _3dreport.composeapp.generated.resources.quote_print_remove
import _3dreport.composeapp.generated.resources.quote_print_settings
import _3dreport.composeapp.generated.resources.quote_print_settings_edit
import _3dreport.composeapp.generated.resources.quote_print_summary_cost
import _3dreport.composeapp.generated.resources.quote_print_summary_empty
import _3dreport.composeapp.generated.resources.quote_print_title
import _3dreport.composeapp.generated.resources.quote_print_title_named
import _3dreport.composeapp.generated.resources.quote_reading
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuotedPrint
import org.jetbrains.compose.resources.stringResource

/**
 * As impressões do pedido (decisão 114), o corpo da seção "Impressão". Com uma só, são os campos dela direto
 * na grade, mais um "Adicionar outra impressão" discreto. Com duas ou mais, cada uma vira um bloco
 * "Impressão N", recolhível, com a impressora, os filamentos, o tempo, as vezes e o G-code dela (um bloco
 * de verdade: cada impressão é independente das outras). O que é do pedido (nome, quantidade, trabalho,
 * serviços, venda) fica fora dos blocos, uma vez só.
 */
@Composable
internal fun PrintsSection(
    viewModel: QuoteViewModel,
    allFilaments: List<Filament>,
    printers: List<PrinterProfile>,
    input: QuoteInputState,
    result: QuoteResult,
    currency: Currency,
    importing: Boolean,
    collapsed: Set<Int>,
) {
    // Arquivado sai das escolhas (decisão 115); um já escolhido continua aparecendo, marcado.
    val inStock = allFilaments.filter { it.hasStockAvailable && !it.archived }
    val space = MaterialTheme.spacing

    if (input.prints.size == 1) {
        val print = input.prints.single()
        PrintFields(viewModel, inStock, printers, print, result.prints.firstOrNull(), result, currency, showRuns = false)
        AppButton(stringResource(Res.string.quote_print_add), onClick = { viewModel.addPrint() }, icon = AppIcons.Add)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        input.prints.forEachIndexed { index, print ->
            key(print.id) {
                PrintCard(
                    viewModel = viewModel,
                    inStock = inStock,
                    printers = printers,
                    number = index + 1,
                    print = print,
                    resolved = result.prints.getOrNull(index),
                    quoted = result.quote?.prints?.getOrNull(index),
                    result = result,
                    currency = currency,
                    importing = importing,
                    expanded = print.id !in collapsed,
                    onToggleExpanded = { viewModel.toggleCollapsed(print.id) },
                )
            }
        }
    }
    AppButton(stringResource(Res.string.quote_print_add), onClick = { viewModel.addPrint() }, icon = AppIcons.Add)
}

/** "Impressão 2 · Corpo", o nome que a impressão tem em toda a tela (e nunca "job", "placa" ou "parte"). */
@Composable
internal fun printTitle(number: Int, name: String): String {
    val trimmed = name.trim()
    return if (trimmed.isEmpty()) {
        stringResource(Res.string.quote_print_title, number)
    } else {
        stringResource(Res.string.quote_print_title_named, number, trimmed)
    }
}

/**
 * Um bloco de impressão. Recolhido, mostra o resumo numa linha ("Corpo · K1 · 9h40 · 180 g · custo
 * R$ 32,10"), pra conferir um pedido de seis mesas sem rolar tudo. O custo é o da mesa (material e
 * máquina); trabalho, falhas e administrativo são do pedido e aparecem no resultado.
 */
@Composable
private fun PrintCard(
    viewModel: QuoteViewModel,
    inStock: List<Filament>,
    printers: List<PrinterProfile>,
    number: Int,
    print: PrintInput,
    resolved: ResolvedPrint?,
    quoted: QuotedPrint?,
    result: QuoteResult,
    currency: Currency,
    importing: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    var editingSettings by remember { mutableStateOf(false) }
    val title = printTitle(number, print.name)
    val summary = if (expanded) "" else printSummary(print, resolved, quoted, currency)
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val hasError = result.fieldErrors.keys.any { key ->
        key == QuoteFields.printTime(print.id) || key == QuoteFields.runs(print.id) ||
            print.filaments.any { key == QuoteFields.length(print.id, it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, MaterialTheme.shapes.medium)
            .border(AppDimens.borderWidth, colors.line, MaterialTheme.shapes.medium)
            .padding(space.md),
        verticalArrangement = Arrangement.spacedBy(space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
            Row(
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                print.thumbnail?.let { GCodeThumbnail(it.bytes) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.appType.bodyStrong)
                    if (!expanded) {
                        Text(
                            summary,
                            style = MaterialTheme.appType.caption,
                            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(stringResource(if (expanded) Res.string.quote_print_collapse else Res.string.quote_print_open), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.primary)
                Icon(
                    if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.iconSize),
                )
            }
            IconAction(AppIcons.FileCopy, stringResource(Res.string.quote_print_duplicate, title), onClick = { viewModel.duplicatePrint(print.id) })
            IconAction(AppIcons.Delete, stringResource(Res.string.quote_print_remove, title), onClick = { viewModel.removePrint(print.id) })
        }
        if (!expanded) return@Column

        FormGrid {
            cell(2) {
                AppTextField(
                    label = stringResource(Res.string.quote_print_field_name),
                    value = print.name,
                    onValueChange = { viewModel.setPrintName(it, print.id) },
                    placeholder = stringResource(Res.string.quote_print_name_placeholder),
                )
            }
        }
        PrintFields(viewModel, inStock, printers, print, resolved, result, currency, showRuns = true)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(AppDimens.iconSize), strokeWidth = AppDimens.focusRingWidth)
                Text(stringResource(Res.string.quote_reading), style = MaterialTheme.appType.caption)
            } else {
                AppButton(stringResource(Res.string.quote_print_gcode), onClick = { viewModel.pickAndImportGCode(print.id) }, icon = AppIcons.RequestQuote)
            }
            AppButton(
                stringResource(if (print.settings.isEmpty) Res.string.quote_print_settings else Res.string.quote_print_settings_edit),
                onClick = { editingSettings = true },
            )
        }
        print.gcodeImport?.let { Text(it.asText(), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (print.beforeGCode != null || print.createdByImport) {
            AppButton(stringResource(Res.string.quote_gcode_undo), onClick = { viewModel.undoGCodeImport(print.id) })
        }
    }

    if (editingSettings) {
        PrintSettingsDialog(
            initial = print.settings,
            onDismiss = { editingSettings = false },
            onSave = { viewModel.setPrintSettings(it, print.id) },
        )
    }
}

/** "Corpo · K1 · 9h40 × 2 · 180 g · custo R$ 32,10", com o que já dá pra dizer. */
@Composable
private fun printSummary(print: PrintInput, resolved: ResolvedPrint?, quoted: QuotedPrint?, currency: Currency): String {
    val cost = quoted?.cost?.total?.let { stringResource(Res.string.quote_print_summary_cost, it.toCurrencyText(currency)) }
    val empty = stringResource(Res.string.quote_print_summary_empty)
    return buildList {
        print.name.trim().takeIf { it.isNotEmpty() }?.let(::add)
        (resolved?.printer?.name ?: print.missingPrinterName)?.let(::add)
        print.printTimeMinutes?.let { add(it.minutesToDurationText() + if (print.runs > 1) " × ${print.runs}" else "") }
        // O peso sai dos campos, e não do cálculo: aparece mesmo com outra impressão ainda por preencher.
        val grams = print.filaments.zip(resolved?.filaments.orEmpty()).sumOf { (row, chosen) ->
            val meters = parseDecimal(row.lengthText, NumberKind.MEASURE) ?: 0.0
            chosen.filament?.weightGrams(meters) ?: 0.0
        }
        if (grams > 0) add(grams.toWeightText())
        cost?.let(::add)
    }.ifEmpty { listOf(empty) }.joinToString(" · ")
}
