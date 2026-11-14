package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.components.availableOrSelected
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toInputText
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
import kotlin.math.round

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
) {
    val importing by viewModel.importing.collectAsState()
    // Arquivado sai das escolhas (decisão 115); um já escolhido continua aparecendo, marcado.
    val inStock = allFilaments.filter { it.hasStockAvailable && !it.archived }
    val space = MaterialTheme.spacing

    if (input.prints.size == 1) {
        val print = input.prints.single()
        PrintFields(viewModel, inStock, printers, print, result.prints.firstOrNull(), result, currency, showRuns = false)
        AppButton("Adicionar outra impressão", onClick = { viewModel.addPrint() }, icon = AppIcons.Add)
        return
    }

    val collapsed by viewModel.collapsedPrintIds.collectAsState()
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
    AppButton("Adicionar outra impressão", onClick = { viewModel.addPrint() }, icon = AppIcons.Add)
}

/** "Impressão 2 · Corpo", o nome que a impressão tem em toda a tela (e nunca "job", "placa" ou "parte"). */
internal fun printTitle(number: Int, name: String): String = "Impressão $number" + name.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()

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
                print.thumbnail?.let { Thumbnail(it.bytes) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(printTitle(number, print.name), style = MaterialTheme.appType.bodyStrong)
                    if (!expanded) {
                        Text(
                            printSummary(print, resolved, quoted, currency),
                            style = MaterialTheme.appType.caption,
                            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(if (expanded) "Recolher" else "Abrir", style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.primary)
                Icon(
                    if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.iconSize),
                )
            }
            IconAction(AppIcons.FileCopy, "Duplicar ${printTitle(number, print.name)}", onClick = { viewModel.duplicatePrint(print.id) })
            IconAction(AppIcons.Delete, "Remover ${printTitle(number, print.name)}", onClick = { viewModel.removePrint(print.id) })
        }
        if (!expanded) return@Column

        FormGrid {
            cell(2) {
                AppTextField(
                    label = "Nome da impressão (opcional)",
                    value = print.name,
                    onValueChange = { viewModel.setPrintName(it, print.id) },
                    placeholder = "Cabeça, corpo, base…",
                )
            }
        }
        PrintFields(viewModel, inStock, printers, print, resolved, result, currency, showRuns = true)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(AppDimens.iconSize), strokeWidth = AppDimens.focusRingWidth)
                Text("Lendo…", style = MaterialTheme.appType.caption)
            } else {
                AppButton("G-code desta impressão", onClick = { viewModel.pickAndImportGCode(print.id) }, icon = AppIcons.RequestQuote)
            }
            AppButton(
                if (print.settings.isEmpty) "Configurações de impressão" else "Editar configurações",
                onClick = { editingSettings = true },
            )
        }
        print.gcodeImportMessage?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (print.beforeGCode != null || print.createdByImport) {
            AppButton("Desfazer importação do G-code", onClick = { viewModel.undoGCodeImport(print.id) })
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
private fun printSummary(print: PrintInput, resolved: ResolvedPrint?, quoted: QuotedPrint?, currency: Currency): String = buildList {
    print.name.trim().takeIf { it.isNotEmpty() }?.let(::add)
    (resolved?.printer?.name ?: print.missingPrinterName)?.let(::add)
    print.printTimeMinutes?.let { add(it.minutesToDurationText() + if (print.runs > 1) " × ${print.runs}" else "") }
    // O peso sai dos campos, e não do cálculo: aparece mesmo com outra impressão ainda por preencher.
    val grams = print.filaments.zip(resolved?.filaments.orEmpty()).sumOf { (row, chosen) ->
        val meters = parseDecimal(row.lengthText, NumberKind.MEASURE) ?: 0.0
        chosen.filament?.weightGrams(meters) ?: 0.0
    }
    if (grams > 0) add(grams.toWeightText())
    quoted?.cost?.total?.let { add("custo ${it.toCurrencyText(currency)}") }
}.ifEmpty { listOf("Ainda sem dados") }.joinToString(" · ")

/** Os campos de uma impressão: filamentos, tempo, impressora e, nos blocos, quantas vezes a mesa roda. */
@Composable
private fun PrintFields(
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
                    error = result.fieldErrors[QuoteFields.length(print.id, row.id)],
                )
            }
        }
        AppButton("Adicionar filamento", onClick = { viewModel.addFilament(print.id) }, icon = AppIcons.Add)

        val timeError = result.fieldErrors[QuoteFields.printTime(print.id)]
        val runsError = result.fieldErrors[QuoteFields.runs(print.id)]
        FormGrid {
            cell {
                AppTextField(
                    label = "Tempo de impressão",
                    value = print.printTimeText,
                    onValueChange = { viewModel.setPrintTimeMinutes(it, print.id) },
                    placeholder = "3h20 ou 200 (minutos)",
                    error = timeError,
                )
            }
            cell(if (showRuns) 1 else 2) {
                AppDropdownField(
                    label = "Impressora",
                    items = printers.availableOrSelected { it.id == resolved?.printer?.id },
                    selected = resolved?.printer,
                    itemLabel = { it.name + if (it.archived) " (arquivada)" else "" },
                    displayText = { it.name + if (it.archived) " (arquivada)" else "" },
                    onSelect = { viewModel.selectPrinter(it.id, print.id) },
                    emptyText = print.missingPrinterName?.let { "$it (não cadastrada)" } ?: "Escolha a impressora",
                )
            }
            if (showRuns) {
                cell {
                    AppTextField(
                        label = "× vezes",
                        value = print.runsText,
                        onValueChange = { viewModel.setRuns(it, print.id) },
                        placeholder = "1",
                        error = runsError,
                        help = HelpText(
                            "Quantas vezes esta mesa roda no pedido: 4 mesas iguais de chaveiros é uma impressão × 4.",
                            "É diferente da quantidade de peças, que repete o pedido inteiro. Tempo e consumo aqui são de uma rodada.",
                        ),
                    )
                }
            }
        }
    }
}

/**
 * O atalho principal da tela (decisão 108): arrastar o G-code ou escolher o arquivo. Vem primeiro,
 * antes de filamento e impressora, porque é ele que preenche os dois. A explicação do que o arquivo
 * preenche fica numa dica, e o resultado da leitura, numa linha embaixo.
 */
@Composable
internal fun GCodeDropArea(viewModel: QuoteViewModel, input: QuoteInputState) {
    val importing by viewModel.importing.collectAsState()
    val space = MaterialTheme.spacing
    val multi = input.prints.size > 1
    val single = input.prints.singleOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        DropArea(
            importing = importing,
            multi = multi,
            onPick = { if (multi) viewModel.pickGCodeAsNewPrint() else viewModel.pickAndImportGCode(single?.id) },
        )
        single?.gcodeImportMessage?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (single?.beforeGCode != null) {
            AppButton("Desfazer importação do G-code", onClick = { viewModel.undoGCodeImport(single.id) })
        }
    }
}

@Composable
private fun DropArea(importing: Boolean, multi: Boolean, onPick: () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val shape = MaterialTheme.shapes.medium
    val dash = AppDimens.dashedBorderWidth
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.dropZoneHeight)
            .clip(shape)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, colors.line, style = Stroke(width = dash.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            .clickable(enabled = !importing, role = Role.Button, onClick = onPick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs, Alignment.CenterHorizontally),
    ) {
        if (importing) {
            CircularProgressIndicator(modifier = Modifier.size(AppDimens.iconSize), strokeWidth = AppDimens.focusRingWidth)
            Text("Lendo…", style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                buildAnnotatedString {
                    append(if (multi) "Arraste os G-codes aqui ou " else "Arraste um G-code aqui ou ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) { append("escolha o arquivo") }
                },
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HelpHint(
                HelpText(
                    "Peso, tempo, foto, impressora e filamento vêm do arquivo do fatiador.",
                    if (multi) "Cada arquivo vira uma impressão. Ao soltar, dá pra escolher qual impressão ele substitui." else null,
                ),
                label = "G-code",
            )
        }
    }
}

/** A miniatura que o fatiador gravou no G-code, pra reconhecer a mesa de relance. */
@Composable
private fun Thumbnail(bytes: ByteArray) {
    val size = AppDimens.printThumbnailSize
    val bitmap = remember(bytes) { runCatching { decodeImageBitmap(bytes) }.getOrNull() } ?: return
    Box(modifier = Modifier.size(size).clip(MaterialTheme.shapes.extraSmall)) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
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
    // Um filamento escolhido que esgotou continua na lista (marcado), pra um pedido reaberto
    // não trocar de filamento sozinho; os outros esgotados ficam de fora.
    val chosen = resolved?.filament
    val options = if (chosen != null && chosen !in inStock) inStock + chosen else inStock
    val filament = resolved?.filament
    val colors = filament?.colors.orEmpty().filter { it.inStock || it.id == resolved?.color?.id }
    // Peso e metros são o mesmo consumo: digitar um preenche o outro, pelo filamento escolhido.
    val derivedWeight = row.weightText ?: run {
        val meters = parseDecimal(row.lengthText, NumberKind.MEASURE)
        if (filament != null && meters != null) (round(filament.weightGrams(meters) * 100) / 100).toInputText() else ""
    }
    FormGrid {
        cell {
            AppDropdownField(
                label = if (multicolor) "Filamento $number" else "Filamento",
                items = options,
                selected = chosen,
                itemLabel = { "${it.name} · ${it.pricePerKg.toCurrencyText(currency)}/kg" + it.availabilityMark() },
                displayText = { it.name + it.availabilityMark() },
                onSelect = { viewModel.selectFilament(it.id, printId, row.id) },
                emptyText = row.missingFilamentName?.let { "$it (não cadastrado)" } ?: "Escolha o filamento",
                help = if (multicolor && number == 1) {
                    HelpText(
                        "Peça multicolor: cada filamento com o próprio consumo e o próprio preço.",
                        "Use o peso ou os metros que o fatiador informa pra cada filamento (a purga e a torre entram junto).",
                    )
                } else {
                    null
                },
            )
        }
        if (colors.size > 1) {
            cell {
                AppDropdownField(
                    label = if (multicolor) "Cor $number" else "Cor",
                    items = colors,
                    selected = resolved?.color,
                    itemLabel = { it.displayLabel() + if (it.inStock) "" else " (acabou)" },
                    displayText = { it.displayLabel() },
                    onSelect = { viewModel.selectFilamentColor(it.id, printId, row.id) },
                )
            }
        }
        cell {
            AppTextField(
                label = if (multicolor) "Peso $number (g)" else "Peso (g)",
                value = derivedWeight,
                onValueChange = { viewModel.setWeightGrams(it, printId, row.id) },
                enabled = filament != null,
                help = if (filament == null) HelpText("Escolha o filamento pra digitar o peso em gramas: a conversão usa a densidade dele.") else null,
            )
        }
        cell {
            AppTextField(
                label = if (multicolor) "Metros $number" else "Comprimento (m)",
                value = row.lengthText,
                onValueChange = { viewModel.setLengthMeters(it, printId, row.id) },
                error = error,
            )
        }
        if (multicolor) {
            cell {
                Box(Modifier.padding(top = MaterialTheme.spacing.md, start = MaterialTheme.spacing.xxs)) {
                    IconAction(AppIcons.Close, "Remover filamento $number", onClick = { viewModel.removeFilament(row.id, printId) })
                }
            }
        }
    }
}

/** " (arquivado)" ou " (esgotado)" ao lado do nome, quando é o caso. */
private fun Filament.availabilityMark(): String = when {
    archived -> " (arquivado)"
    hasStockAvailable -> ""
    else -> " (esgotado)"
}
