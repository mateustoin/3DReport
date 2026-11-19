package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.catalog_action_sell
import _3dreport.composeapp.generated.resources.catalog_col_category
import _3dreport.composeapp.generated.resources.catalog_col_price
import _3dreport.composeapp.generated.resources.catalog_col_product
import _3dreport.composeapp.generated.resources.catalog_col_profit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.TableColumn
import com.threedreport.app.ui.components.TableRow
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.shownName
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.stringResource

/**
 * As colunas do Catálogo em lista, no molde dos cadastros: miniatura, produto (que absorve a sobra), categoria, lucro e
 * preço, os dois últimos à direita e de largura fixa, e o "Vender" sempre visível na última coluna. O menu aparece no
 * hover ou no foco por cima da coluna do preço, sem cobrir o "Vender".
 */
@Composable
fun catalogColumns(): List<TableColumn> = listOf(
    TableColumn("", width = AppDimens.printThumbnailSize),
    TableColumn(stringResource(Res.string.catalog_col_product), weight = 1f),
    TableColumn(stringResource(Res.string.catalog_col_category), width = AppDimens.tableCategoryColumn),
    TableColumn(stringResource(Res.string.catalog_col_profit), width = AppDimens.tableMoneyColumn, end = true),
    TableColumn(stringResource(Res.string.catalog_col_price), width = AppDimens.tableMoneyColumn, end = true),
    TableColumn("", width = AppDimens.tableActionColumn, end = true),
)

/**
 * Uma linha do Catálogo em lista: clicar abre os detalhes (ou, no modo de seleção, marca). "Vender" fica sempre visível (menos no
 * modo de seleção); o menu das demais ações vem no hover ou no foco.
 */
@Composable
fun CatalogListRow(
    row: HistoryRow,
    columns: List<TableColumn>,
    state: HistoryUiState,
    actions: QuoteActions,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit,
    onReprice: () -> Unit,
) {
    val quote = row.quote
    val selected = quote.id in state.selectedIds
    TableRow(
        columns = columns,
        onClick = if (state.selectionMode) onToggleSelected else onOpen,
        actions = emptyList(),
        selected = state.selectionMode && selected,
        selectionMark = if (state.selectionMode) selected else null,
        extra = {
            QuoteActionsMenu(quote, actions)
        },
        revealActions = !state.selectionMode,
        revealInset = AppDimens.tableActionColumn + MaterialTheme.spacing.md + MaterialTheme.spacing.xs,
    ) {
        Cell(0) { PhotoThumbnail(quote, loadThumbnail, Modifier.size(AppDimens.printThumbnailSize), placeholder = true, shape = MaterialTheme.shapes.small) }
        Cell(1) {
            Column {
                Text(quote.shownName(), style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!state.selectionMode) RepriceHint(row, { value -> value.toCurrencyText(quote.currency) }, onReprice)
            }
        }
        Cell(2) { Muted(quote.category.orEmpty()) }
        Cell(3) { NumericText(quote.quote.profit.toCurrencyText(quote.currency), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Cell(4) {
            NumericText(quote.totalWithServices.toCurrencyText(quote.currency), style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface)
        }
        Cell(5) {
            if (!state.selectionMode) {
                actions.onSell?.let { sell -> AppButton(stringResource(Res.string.catalog_action_sell), sell, height = AppDimens.smallControlHeight) }
            }
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
