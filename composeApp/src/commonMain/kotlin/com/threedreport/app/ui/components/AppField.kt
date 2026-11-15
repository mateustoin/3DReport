package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_help_for
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.TABULAR_FIGURES
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Ajuda sob demanda: uma frase curta e, opcionalmente, o detalhe. Aparece numa dica, nunca como parágrafo fixo.
 * Com [warning], o ícone de ajuda sai em âmbar: é um aviso que vale ler (o consumo que veio de um preset).
 */
data class HelpText(val short: String, val detail: String? = null, val warning: Boolean = false)

/**
 * Botão de ajuda ao lado de um rótulo: abre (ao passar o mouse ou ao clicar) a dica com o texto. Substitui
 * o parágrafo fixo embaixo do campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpHint(help: HelpText, label: String, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip {
                // O conteúdo do RichTooltip é uma Box: sem a Column, a frase curta e o detalhe se sobrepõem.
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                    Text(help.short, style = MaterialTheme.appType.caption)
                    help.detail?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        },
        state = state,
        modifier = modifier,
    ) {
        Icon(
            AppIcons.Info,
            contentDescription = stringResource(Res.string.common_help_for, label),
            tint = if (tint == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint,
            modifier = Modifier
                .size(AppDimens.iconSize)
                .clickable(role = Role.Button) { scope.launch { state.show() } },
        )
    }
}

@Composable
internal fun FieldLabel(label: String, help: HelpText?, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    // Altura fixa (a do ícone de ajuda): campos com e sem ajuda ficam alinhados na mesma linha da grade.
    Row(
        modifier = Modifier.height(AppDimens.iconSize),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
    ) {
        Text(label, style = MaterialTheme.appType.label, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (help != null) HelpHint(help, label, tint = if (help.warning) MaterialTheme.appColors.amberText else androidx.compose.ui.graphics.Color.Unspecified)
    }
}

/** Borda do campo: erro, foco (anel de 2), mouse em cima (cor `primary`) ou repouso (`line`). */
@Composable
private fun Modifier.fieldBox(hovered: Boolean, focused: Boolean, error: Boolean, enabled: Boolean, onNavy: Boolean): Modifier {
    val colors = MaterialTheme.appColors
    val borderColor = when {
        error -> if (onNavy) colors.onNavyDanger else MaterialTheme.colorScheme.error
        focused || hovered -> if (onNavy) colors.onNavyMuted else MaterialTheme.colorScheme.primary
        else -> if (onNavy) colors.navyRaised else colors.line
    }
    val fill = when {
        onNavy -> colors.navyRaised
        enabled -> colors.card
        else -> colors.column
    }
    return this
        .height(AppDimens.controlHeight)
        .background(fill, MaterialTheme.shapes.small)
        .border(if (focused) AppDimens.focusRingWidth else AppDimens.borderWidth, borderColor, MaterialTheme.shapes.small)
}

@Composable
private fun FieldError(error: String?, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error) {
    if (error != null) Text(error, style = MaterialTheme.appType.label, color = color)
}

/**
 * Campo de texto do kit: rótulo em `label` acima, caixa de 40 com borda `line` e fundo `card`; borda `primary`
 * ao passar o mouse e anel de 2 no foco; erro com borda `error` e mensagem curta abaixo. Tab avança e
 * Shift+Tab volta. [fieldModifier] vai na caixa (por exemplo, a âncora de um menu de sugestões). [trailing] fica
 * dentro da caixa, à direita do texto (a amostra de uma cor digitada, por exemplo). [onNavy] é a versão pra
 * dentro do painel azul-marinho (rótulo `onNavyMuted`, caixa `navyRaised`); [autoFocus] leva o foco pro campo
 * quando ele aparece. [prefix] é um texto fixo antes do valor (o símbolo da moeda), em cor de apoio.
 */
@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    help: HelpText? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onNavy: Boolean = false,
    autoFocus: Boolean = false,
    prefix: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val space = MaterialTheme.spacing
    val colors = MaterialTheme.appColors
    val textColor = if (onNavy) colors.onNavy else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (onNavy) colors.onNavyMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(modifier = modifier.alpha(if (enabled) 1f else 0.6f), verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        FieldLabel(label, help, color = if (onNavy) colors.onNavyMuted else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = fieldModifier.fillMaxWidth().fieldBox(hovered, focused, error != null, enabled, onNavy).hoverable(interaction),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.appType.body.copy(color = textColor, fontFeatureSettings = TABULAR_FIGURES),
                cursorBrush = SolidColor(if (onNavy) colors.amber else MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .tabToNavigate()
                    .semantics { contentDescription = label },
                decorationBox = { inner ->
                    Row(Modifier.padding(horizontal = space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                        if (prefix != null) Text(prefix, style = MaterialTheme.appType.body, color = mutedColor)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(placeholder, style = MaterialTheme.appType.body, color = mutedColor.copy(alpha = 0.7f), maxLines = 1)
                            }
                            inner()
                        }
                        trailing?.invoke()
                    }
                },
            )
        }
        FieldError(error, color = if (onNavy) colors.onNavyDanger else MaterialTheme.colorScheme.error)
    }
}

/**
 * Lista de escolha do kit (uma opção por vez), com a mesma caixa do [AppTextField] e a seta à direita. O menu
 * abre ao clicar ou com Enter, e acompanha a largura do campo.
 */
@Composable
fun <T> AppDropdownField(
    label: String,
    items: List<T>,
    selected: T?,
    itemLabel: (T) -> String,
    displayText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "",
    help: HelpText? = null,
    error: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var boxWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val space = MaterialTheme.spacing
    val text = selected?.let(displayText) ?: emptyText
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        FieldLabel(label, help)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { boxWidth = with(density) { it.width.toDp() } }
                    .fieldBox(hovered, focused || expanded, error != null, true, onNavy = false)
                    .onFocusChanged { focused = it.isFocused }
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null, role = Role.DropdownList) { expanded = true }
                    .semantics { contentDescription = "$label: $text" }
                    .padding(start = space.sm, end = space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text,
                    style = MaterialTheme.appType.body,
                    color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(AppIcons.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AppDimens.iconSize))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(boxWidth.coerceAtLeast(AppDimens.sidebarWidth)),
                containerColor = MaterialTheme.appColors.card,
                border = androidx.compose.foundation.BorderStroke(AppDimens.borderWidth, MaterialTheme.appColors.line),
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemLabel(item), style = MaterialTheme.appType.body, maxLines = 2) },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        },
                    )
                }
            }
        }
        FieldError(error)
    }
}

/** Seletor segmentado de poucas opções, com borda `line`; a opção ativa tem fundo `primary`. */
@Composable
fun SegmentedChoice(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Row(
        modifier = modifier
            .background(colors.card, MaterialTheme.shapes.small)
            .border(AppDimens.borderWidth, colors.line, MaterialTheme.shapes.small)
            .padding(AppDimens.borderWidth),
    ) {
        options.forEachIndexed { index, option ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(AppDimens.controlHeight - AppDimens.borderWidth * 2)
                    .background(if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.shapes.extraSmall)
                    .clickable(role = Role.RadioButton) { onSelect(index) }
                    .padding(horizontal = space.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.appType.body,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** [SegmentedChoice] com o rótulo em `label` acima (e a ajuda, se houver), como os outros campos do formulário. */
@Composable
fun AppChoiceField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    help: HelpText? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        FieldLabel(label, help)
        SegmentedChoice(options, selectedIndex, onSelect)
    }
}

/**
 * Seção de formulário: marcador redondo numerado (fundo `tint`, número em `primary`) e título em
 * `sectionTitle`, sem ícone decorativo. Entre o título e os campos, 12.
 */
@Composable
fun FormSection(number: Int?, title: String, modifier: Modifier = Modifier, help: HelpText? = null, content: @Composable () -> Unit) {
    val space = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
            // Sem número quando as seções não são passos em sequência (ex.: diálogo de manutenção).
            if (number != null) {
                Box(
                    modifier = Modifier.size(AppDimens.sectionMarkerSize).background(MaterialTheme.appColors.tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(number.toString(), style = MaterialTheme.appType.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(title, style = MaterialTheme.appType.sectionTitle, color = MaterialTheme.colorScheme.onSurface)
            if (help != null) HelpHint(help, title)
        }
        content()
    }
}

class FormGridScope {
    internal val cells = mutableListOf<Pair<Int, @Composable () -> Unit>>()

    /** Uma célula que ocupa [span] das três colunas. */
    fun cell(span: Int = 1, content: @Composable () -> Unit) {
        cells += span.coerceIn(1, FORM_COLUMNS) to content
    }
}

private const val FORM_COLUMNS = 3

/** Grade de três colunas do formulário, com 12 entre campos. As células entram em ordem e quebram a linha quando não cabem. */
@Composable
fun FormGrid(modifier: Modifier = Modifier, content: FormGridScope.() -> Unit) {
    val gap = MaterialTheme.spacing.sm
    val cells = FormGridScope().apply(content).cells
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val column: Dp = (maxWidth - gap * (FORM_COLUMNS - 1)) / FORM_COLUMNS
        val rows = mutableListOf<MutableList<Pair<Int, @Composable () -> Unit>>>()
        var used = 0
        cells.forEach { cell ->
            if (rows.isEmpty() || used + cell.first > FORM_COLUMNS) {
                rows += mutableListOf<Pair<Int, @Composable () -> Unit>>()
                used = 0
            }
            rows.last() += cell
            used += cell.first
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (span, cell) ->
                        Box(Modifier.width(column * span + gap * (span - 1))) { cell() }
                    }
                }
            }
        }
    }
}

/**
 * [AppTextField] com sugestões num menu embaixo (cliente já cadastrado, categoria já usada). O menu abre sem
 * tirar o foco do campo, então dá pra continuar digitando com as sugestões na tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppSuggestField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<T>,
    suggestionLabel: (T) -> String,
    onChoose: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    help: HelpText? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        AppTextField(
            label = label,
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            fieldModifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryEditable),
            placeholder = placeholder,
            help = help,
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.appColors.card,
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestionLabel(suggestion), style = MaterialTheme.appType.body) },
                    onClick = {
                        onChoose(suggestion)
                        expanded = false
                    },
                )
            }
        }
    }
}
