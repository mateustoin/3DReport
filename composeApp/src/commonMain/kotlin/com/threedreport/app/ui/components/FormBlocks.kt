package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/** Cores prontas pra escolher com um clique: rótulo em `label` acima, as amostras em linha que quebra. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> AppSwatchField(
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    optionColor: (T) -> Color,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val space = MaterialTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        FieldLabel(label, help = null)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xxs), verticalArrangement = Arrangement.spacedBy(space.xxs)) {
            options.forEach { option ->
                SwatchButton(optionColor(option), optionLabel(option), selected = isSelected(option), onClick = { onSelect(option) })
            }
        }
    }
}

/**
 * Bloco de um item repetido dentro de uma seção (cada cor do filamento): fundo `cardBlock`, sem borda, pra não
 * virar cartão dentro do painel. [title] em `bodyStrong` e, se houver, uma [action] só de ícone (remover) à direita.
 */
@Composable
fun FormBlock(title: String, modifier: Modifier = Modifier, action: RowAction? = null, content: @Composable ColumnScope.() -> Unit) {
    val space = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.appColors.cardBlock, MaterialTheme.shapes.medium).padding(space.sm),
        verticalArrangement = Arrangement.spacedBy(space.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (action != null) IconAction(action.icon, action.description, action.onClick, tint = if (action.danger) MaterialTheme.colorScheme.error else Color.Unspecified)
        }
        content()
    }
}

/**
 * Resultado que o formulário calcula enquanto se preenche (o custo por hora da máquina): [label] e [value] numa faixa `cardBlock`.
 * Com [inBlock] (dentro de um `FormBlock`, que já é `cardBlock`) a faixa perde o fundo e o recuo: vira só a linha, alinhada aos campos
 * do bloco, sem bloco dentro de bloco.
 */
@Composable
fun FormSummary(label: String, value: String, modifier: Modifier = Modifier, help: HelpText? = null, inBlock: Boolean = false) {
    val space = MaterialTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().then(
            if (inBlock) Modifier else Modifier.background(MaterialTheme.appColors.cardBlock, MaterialTheme.shapes.medium).padding(horizontal = space.sm, vertical = space.sm),
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xxs),
    ) {
        Text(label, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (help != null) HelpHint(help, label)
        Spacer(Modifier.weight(1f))
        NumericText(value, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Aviso curto no alto de um formulário (os valores vieram de uma lista de referência): faixa `cardBlock` com [text] em
 * `caption` e, se houver, a explicação sob demanda no [HelpHint]. [help] é o texto da dica.
 */
@Composable
fun FormNotice(text: String, modifier: Modifier = Modifier, help: HelpText? = null) {
    val space = MaterialTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.appColors.cardBlock, MaterialTheme.shapes.medium).padding(space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (help != null) HelpHint(help, text)
    }
}

/**
 * As sugestões que combinam com o que foi digitado: todas com o campo vazio; com texto, as que o contêm (sem diferenciar
 * maiúsculas) e ainda não são o próprio texto. Pro menu do [AppSuggestField].
 */
fun List<String>.suggestionsFor(typed: String): List<String> {
    val text = typed.trim()
    return filter { text.isEmpty() || (it.contains(text, ignoreCase = true) && !it.equals(text, ignoreCase = true)) }
}

/**
 * Aviso de atenção, no tom de atenção do sistema (o mesmo do selo "Hoje": fundo e texto de `deadlineToday`, nunca o
 * vermelho de erro): o ícone de alerta e uma frase em `caption`, numa faixa de raio 10. Avisa, não impede nada: o estoque
 * que não cobre um pedido, itens acabando. Vale no tema claro e no escuro, porque as cores já vêm dos dois.
 */
@Composable
fun AttentionNotice(text: String, modifier: Modifier = Modifier) {
    val space = MaterialTheme.spacing
    val palette = MaterialTheme.appColors.deadlineToday
    Row(
        modifier = modifier.fillMaxWidth().background(palette.background, MaterialTheme.shapes.medium).padding(space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Icon(AppIcons.Warning, contentDescription = null, tint = palette.content, modifier = Modifier.size(AppDimens.iconSize))
        Text(text, style = MaterialTheme.appType.caption, color = palette.content, modifier = Modifier.weight(1f))
    }
}
