package com.threedreport.app.ui.quote

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.FieldHelp
import com.threedreport.app.ui.components.IconLabel
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toWeightText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuotedPrint
import kotlin.math.round

/**
 * As impressões do pedido (decisão 114). Com uma só, a tela é a de sempre, mais um "Adicionar outra
 * impressão" discreto: quem nunca clica nele não vê nada novo. Com duas ou mais, cada uma vira um cartão
 * "Impressão N", recolhível, com a impressora, os filamentos, o tempo, as vezes e o G-code dela. O que é
 * do pedido (quantidade, trabalho, serviços, venda) fica fora dos cartões, uma vez só.
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

    if (input.prints.size == 1) {
        val print = input.prints.single()
        GCodeImportCard(
            importing = importing,
            message = print.gcodeImportMessage,
            canUndo = print.beforeGCode != null,
            onPick = { viewModel.pickAndImportGCode(print.id) },
            onUndo = { viewModel.undoGCodeImport(print.id) },
        )
        SectionTitle(AppIcons.Spool, "A peça")
        PrintFields(viewModel, inStock, printers, print, result.prints.firstOrNull(), result, currency, showRuns = false)
        // Sem ajuda embaixo: quem nunca clica nele não vê nada novo. O que ele faz fica no Sobre.
        TextButton(onClick = { viewModel.addPrint() }) { Text("+ Adicionar outra impressão") }
        return
    }

    GCodeImportCard(
        importing = importing,
        message = null,
        canUndo = false,
        title = "Arraste os G-codes pra janela",
        detail = "Cada arquivo vira uma impressão. Ao soltar, dá pra escolher qual impressão ele substitui.",
        pickLabel = "Nova impressão de um G-code",
        onPick = { viewModel.pickGCodeAsNewPrint() },
        onUndo = {},
    )
    SectionTitle(AppIcons.Spool, "As impressões")
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
            )
        }
    }
    TextButton(onClick = { viewModel.addPrint() }) { Text("+ Adicionar outra impressão") }
}

/** "Impressão 2 · Corpo", o nome que a impressão tem em toda a tela (e nunca "job", "placa" ou "parte"). */
internal fun printTitle(number: Int, name: String): String = "Impressão $number" + name.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()

/**
 * Um cartão de impressão. Recolhido, mostra o resumo numa linha ("Corpo · K1 · 9h40 · 180 g · custo
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
) {
    var expanded by rememberSaveable(print.id) { mutableStateOf(true) }
    var editingSettings by remember { mutableStateOf(false) }
    val hasError = result.fieldErrors.keys.any { it.startsWith("time:${print.id}") || it.startsWith("runs:${print.id}") || it.startsWith("length:${print.id}:") }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    print.thumbnail?.let { Thumbnail(it.bytes, size = 40) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(printTitle(number, print.name), style = MaterialTheme.typography.titleSmall)
                        if (!expanded) {
                            Text(
                                printSummary(print, resolved, quoted, currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        if (expanded) "Recolher" else "Abrir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { viewModel.duplicatePrint(print.id) }) {
                    Icon(AppIcons.FileCopy, contentDescription = "Duplicar ${printTitle(number, print.name)}")
                }
                IconButton(onClick = { viewModel.removePrint(print.id) }) {
                    Icon(AppIcons.Delete, contentDescription = "Remover ${printTitle(number, print.name)}")
                }
            }
            if (!expanded) return@Column

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().tabToNavigate(),
                value = print.name,
                onValueChange = { viewModel.setPrintName(it, print.id) },
                label = { Text("Nome da impressão (opcional)") },
                placeholder = { Text("Cabeça, corpo, base…") },
                singleLine = true,
            )
            PrintFields(viewModel, inStock, printers, print, resolved, result, currency, showRuns = true)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (importing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Lendo…", style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedButton(onClick = { viewModel.pickAndImportGCode(print.id) }) { IconLabel(AppIcons.RequestQuote, "G-code desta impressão") }
                }
                TextButton(onClick = { editingSettings = true }) {
                    Text(if (print.settings.isEmpty) "Configurações de impressão" else "Editar configurações")
                }
            }
            print.gcodeImportMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (print.beforeGCode != null || print.createdByImport) {
                TextButton(onClick = { viewModel.undoGCodeImport(print.id) }) { Text("Desfazer importação do G-code") }
            }
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

/** Os campos de uma impressão: filamentos, impressora, tempo e, nos cartões, quantas vezes a mesa roda. */
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
    val multicolor = print.filaments.size > 1
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
    TextButton(onClick = { viewModel.addFilament(print.id) }) { Text("+ Adicionar filamento") }
    if (multicolor) {
        FieldHelp(
            "Peça multicolor: cada filamento com o próprio consumo e o próprio preço.",
            "Use o peso ou os metros que o fatiador informa pra cada filamento (a purga e a torre entram junto).",
        )
    }

    LabeledDropdown(
        label = "Impressora",
        items = printers.filter { !it.archived || it.id == resolved?.printer?.id },
        selected = resolved?.printer,
        itemLabel = { it.name + if (it.archived) " (arquivada)" else "" },
        displayText = { it.name + if (it.archived) " (arquivada)" else "" },
        onSelect = { viewModel.selectPrinter(it.id, print.id) },
        emptyText = print.missingPrinterName?.let { "$it (não cadastrada)" } ?: "Escolha a impressora",
    )

    val timeError = result.fieldErrors[QuoteFields.printTime(print.id)]
    val runsError = result.fieldErrors[QuoteFields.runs(print.id)]
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.weight(1f).tabToNavigate(),
            value = print.printTimeText,
            onValueChange = { viewModel.setPrintTimeMinutes(it, print.id) },
            label = { Text("Tempo de impressão") },
            placeholder = { Text("3h20 ou 200 (minutos)") },
            singleLine = true,
            isError = timeError != null,
            supportingText = if (timeError != null) ({ Text(timeError) }) else null,
        )
        if (showRuns) {
            OutlinedTextField(
                modifier = Modifier.width(120.dp).tabToNavigate(),
                value = print.runsText,
                onValueChange = { viewModel.setRuns(it, print.id) },
                label = { Text("× vezes") },
                placeholder = { Text("1") },
                singleLine = true,
                isError = runsError != null,
                supportingText = if (runsError != null) ({ Text(runsError) }) else null,
            )
        }
    }
    if (showRuns) {
        FieldHelp(
            "Quantas vezes esta mesa roda no pedido: 4 mesas iguais de chaveiros é uma impressão × 4.",
            "É diferente da quantidade de peças, que repete o pedido inteiro. Tempo e consumo aqui são de uma rodada.",
        )
    }
}

/**
 * O atalho principal da tela (decisão 108): arrastar o G-code ou escolher o arquivo. Vem primeiro,
 * antes de filamento e impressora, porque é ele que preenche os dois; antes ficava no meio dos campos, e
 * o arrastar nem aparecia na tela.
 */
@Composable
private fun GCodeImportCard(
    importing: Boolean,
    message: String?,
    canUndo: Boolean,
    onPick: () -> Unit,
    onUndo: () -> Unit,
    title: String = "Arraste o G-code pra janela",
    detail: String = "Peso, tempo, foto, impressora e filamento vêm do arquivo do fatiador.",
    pickLabel: String = "Escolher arquivo",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.RequestQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Lendo…", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedButton(onClick = onPick) { Text(pickLabel) }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (canUndo) {
            TextButton(onClick = onUndo) { Text("Desfazer importação do G-code") }
        }
    }
}

/** A miniatura que o fatiador gravou no G-code, pra reconhecer a mesa de relance. */
@Composable
private fun Thumbnail(bytes: ByteArray, size: Int) {
    val bitmap = remember(bytes) { runCatching { decodeImageBitmap(bytes) }.getOrNull() } ?: return
    Box(modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp))) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp))
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                // Um filamento escolhido que esgotou continua na lista (marcado), pra um pedido reaberto
                // não trocar de filamento sozinho; os outros esgotados ficam de fora.
                val chosen = resolved?.filament
                val options = if (chosen != null && chosen !in inStock) inStock + chosen else inStock
                LabeledDropdown(
                    label = if (multicolor) "Filamento $number" else "Filamento",
                    items = options,
                    selected = chosen,
                    itemLabel = { "${it.name} · ${it.pricePerKg.toCurrencyText(currency)}/kg" + it.availabilityMark() },
                    displayText = { it.name + it.availabilityMark() },
                    onSelect = { viewModel.selectFilament(it.id, printId, row.id) },
                    emptyText = row.missingFilamentName?.let { "$it (não cadastrado)" } ?: "Escolha o filamento",
                )
            }
            if (multicolor) {
                IconButton(onClick = { viewModel.removeFilament(row.id, printId) }) {
                    Icon(AppIcons.Close, contentDescription = "Remover filamento $number")
                }
            }
        }
        val filament = resolved?.filament
        val colors = filament?.colors.orEmpty().filter { it.inStock || it.id == resolved?.color?.id }
        if (colors.size > 1) {
            LabeledDropdown(
                label = if (multicolor) "Cor $number" else "Cor",
                items = colors,
                selected = resolved?.color,
                itemLabel = { it.displayLabel() + if (it.inStock) "" else " (acabou)" },
                displayText = { it.displayLabel() },
                onSelect = { viewModel.selectFilamentColor(it.id, printId, row.id) },
            )
        }
        // Peso e metros são o mesmo consumo: digitar um preenche o outro, pelo filamento escolhido.
        val derivedWeight = row.weightText ?: run {
            val meters = parseDecimal(row.lengthText, NumberKind.MEASURE)
            if (filament != null && meters != null) (round(filament.weightGrams(meters) * 100) / 100).toInputText() else ""
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f).tabToNavigate(),
                value = derivedWeight,
                onValueChange = { viewModel.setWeightGrams(it, printId, row.id) },
                label = { Text(if (multicolor) "Peso $number (g)" else "Peso da peça (g)") },
                enabled = filament != null,
                singleLine = true,
                isError = error != null,
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f).tabToNavigate(),
                value = row.lengthText,
                onValueChange = { viewModel.setLengthMeters(it, printId, row.id) },
                label = { Text(if (multicolor) "Metros $number" else "Comprimento (m)") },
                singleLine = true,
                isError = error != null,
            )
        }
        when {
            error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            filament == null -> FieldHelp("Escolha o filamento pra digitar o peso em gramas: a conversão usa a densidade dele.")
        }
    }
}

/** " (arquivado)" ou " (esgotado)" ao lado do nome, quando é o caso. */
private fun Filament.availabilityMark(): String = when {
    archived -> " (arquivado)"
    hasStockAvailable -> ""
    else -> " (esgotado)"
}
