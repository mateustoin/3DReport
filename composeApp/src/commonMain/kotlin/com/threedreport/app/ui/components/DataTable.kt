package com.threedreport.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Uma coluna da tabela: [width] fixa ou, sem ela, elástica na proporção de [weight]. [end] alinha o conteúdo à direita
 * (o valor). O [title] sai no cabeçalho; coluna sem título (a miniatura, o botão da linha) o deixa vazio.
 */
class TableColumn(val title: String, val width: Dp? = null, val weight: Float = 1f, val end: Boolean = false)

/** Cabeçalho da tabela: os títulos das colunas em `label`, na cor de apoio, com 34 de altura. [selectable] reserva a coluna da caixa de marcar. */
@Composable
fun TableHeader(columns: List<TableColumn>, modifier: Modifier = Modifier, selectable: Boolean = false) {
    val space = MaterialTheme.spacing
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(AppDimens.tableHeaderHeight).padding(start = space.sm, end = space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.md),
        ) {
            if (selectable) Box(Modifier.width(AppDimens.checkboxSize))
            columns.forEach { column ->
                Text(
                    column.title,
                    style = MaterialTheme.appType.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (column.end) TextAlign.End else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (column.width != null) Modifier.width(column.width) else Modifier.weight(column.weight),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.appColors.line, thickness = AppDimens.borderWidth)
    }
}

/** Onde cada célula de [TableRow] vai: [Cell] põe o conteúdo na coluna [index], na largura e no alinhamento dela. */
class TableRowScope internal constructor(private val row: RowScope, private val columns: List<TableColumn>) {
    @Composable
    fun Cell(index: Int, content: @Composable () -> Unit) {
        val column = columns[index]
        with(row) {
            Box(
                modifier = if (column.width != null) Modifier.width(column.width) else Modifier.weight(column.weight),
                contentAlignment = if (column.end) Alignment.CenterEnd else Alignment.CenterStart,
            ) { content() }
        }
    }
}

/**
 * Linha da tabela, no molde da [ListRow] dos cadastros (não é cartão): mínimo de 52 de altura, divisor de 1 px embaixo,
 * as [actions] e o [extra] (um botão de menu) só aparecem com o mouse em cima ou o foco na linha, por cima do fim dela,
 * cobrindo o valor. Clicar na linha chama [onClick]. Com [selectionMark], a primeira coluna é a caixa de marcar
 * (só desenhada: o clique na linha é quem marca). [selected] usa o fundo `tint`. [revealActions] falso esconde as ações
 * e o menu (no modo de seleção, a linha só marca).
 */
@Composable
fun TableRow(
    columns: List<TableColumn>,
    onClick: () -> Unit,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMark: Boolean? = null,
    extra: @Composable RowScope.() -> Unit = {},
    revealActions: Boolean = true,
    revealInset: Dp = 0.dp,
    cells: @Composable TableRowScope.() -> Unit,
) {
    val space = MaterialTheme.spacing
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        RevealActions(actions = actions, selected = selected, extra = extra, enabled = revealActions, endInset = revealInset) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .then(if (focused) Modifier.border(AppDimens.focusRingWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                    .defaultMinSize(minHeight = AppDimens.tableRowHeight)
                    .padding(start = space.sm, end = space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.md),
            ) {
                if (selectionMark != null) CheckboxMark(selectionMark)
                TableRowScope(this, columns).cells()
            }
        }
        HorizontalDivider(color = MaterialTheme.appColors.line, thickness = AppDimens.borderWidth)
    }
}
