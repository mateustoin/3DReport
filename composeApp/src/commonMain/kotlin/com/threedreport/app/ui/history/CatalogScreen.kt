package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.catalog_action_export
import _3dreport.composeapp.generated.resources.catalog_action_new
import _3dreport.composeapp.generated.resources.catalog_empty_filter
import _3dreport.composeapp.generated.resources.catalog_empty_first
import _3dreport.composeapp.generated.resources.catalog_filter_category
import _3dreport.composeapp.generated.resources.catalog_filter_category_all
import _3dreport.composeapp.generated.resources.catalog_filter_category_none
import _3dreport.composeapp.generated.resources.catalog_search
import _3dreport.composeapp.generated.resources.catalog_search_hint
import _3dreport.composeapp.generated.resources.catalog_selection_all
import _3dreport.composeapp.generated.resources.catalog_selection_export_selected
import _3dreport.composeapp.generated.resources.catalog_title
import _3dreport.composeapp.generated.resources.catalog_view_grid
import _3dreport.composeapp.generated.resources.catalog_view_list
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_clear_filters
import _3dreport.composeapp.generated.resources.history_filter_period
import _3dreport.composeapp.generated.resources.history_selection_count
import _3dreport.composeapp.generated.resources.history_selection_none
import _3dreport.composeapp.generated.resources.history_selection_select_all
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.data.CatalogView
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.BusyBar
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.components.TableHeader
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * O Catálogo (decisão 132): grade com a foto em destaque ou lista, a que a pessoa escolheu por último. Busca, categoria
 * e período no topo; "Exportar catálogo" entra no modo de seleção, com o rodapé que exporta. Só desenha o
 * [HistoryUiState]; "Novo produto" e as ações que abrem o Orçamento saem pelos callbacks.
 */
@Composable
fun CatalogScreen(
    viewModel: QuoteHistoryViewModel,
    navigation: HistoryNavigation,
    onNewProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    ObserveHistory(viewModel, state.copiedId)
    val space = MaterialTheme.spacing
    val sides = PaddingValues(start = space.xxl, end = space.xxl, bottom = space.xxl)

    Column(modifier = modifier.fillMaxSize()) {
        state.busy?.let { BusyBar(it) }
        // O cabeçalho e os filtros ficam fora do que troca (vazio, grade, lista): se estivessem dentro, trocar de
        // contêiner (o filtro que zera a lista, por exemplo) recriaria o campo de busca e ele perderia o foco.
        CatalogTop(state, viewModel, onNewProduct, Modifier.padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding))
        val body = Modifier.weight(1f).fillMaxWidth()
        when {
            !state.hasItems || state.rows.isEmpty() -> LazyColumn(body, contentPadding = sides) {
                item(key = "empty") {
                    if (!state.hasItems) {
                        EmptyState(stringResource(Res.string.catalog_empty_first), actionLabel = stringResource(Res.string.catalog_action_new), onAction = onNewProduct)
                    } else {
                        EmptyState(
                            stringResource(Res.string.catalog_empty_filter),
                            actionLabel = stringResource(Res.string.common_clear_filters),
                            onAction = viewModel::clearFilters,
                            actionKind = ButtonKind.Secondary,
                        )
                    }
                }
            }

            state.catalogView == CatalogView.GRID -> LazyVerticalGrid(
                columns = GridCells.Adaptive(AppDimens.productCardMinWidth),
                modifier = body,
                contentPadding = sides,
                horizontalArrangement = Arrangement.spacedBy(space.md),
                verticalArrangement = Arrangement.spacedBy(space.md),
            ) {
                items(state.rows, key = { it.quote.id }) { row ->
                    ProductCard(
                        row = row,
                        state = state,
                        actions = quoteActionsFor(row, viewModel, navigation, selectable = true),
                        loadThumbnail = viewModel::photoThumbnail,
                        onOpen = { viewModel.startEditingDetails(row.quote) },
                        onToggleSelected = { viewModel.toggleSelection(row.quote.id) },
                        onReprice = { viewModel.startRepricing(row.quote) },
                    )
                }
            }

            else -> {
                val columns = catalogColumns()
                LazyColumn(body, contentPadding = sides) {
                    item(key = "table-header") { TableHeader(columns, selectable = state.selectionMode) }
                    items(state.rows, key = { it.quote.id }) { row ->
                        CatalogListRow(
                            row = row,
                            columns = columns,
                            state = state,
                            actions = quoteActionsFor(row, viewModel, navigation, selectable = true),
                            loadThumbnail = viewModel::photoThumbnail,
                            onOpen = { viewModel.startEditingDetails(row.quote) },
                            onToggleSelected = { viewModel.toggleSelection(row.quote.id) },
                            onReprice = { viewModel.startRepricing(row.quote) },
                        )
                    }
                }
            }
        }
        if (state.selectionMode) CatalogSelectionBar(state, viewModel)
    }
    HistoryDialogs(state, viewModel)
}

@Composable
private fun CatalogTop(state: HistoryUiState, viewModel: QuoteHistoryViewModel, onNewProduct: () -> Unit, modifier: Modifier = Modifier) = Column(modifier) {
    val space = MaterialTheme.spacing
    ScreenHeader(stringResource(Res.string.catalog_title)) {
        if (!state.hasItems) return@ScreenHeader
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm), verticalAlignment = Alignment.CenterVertically) {
            SegmentedChoice(
                options = listOf(stringResource(Res.string.catalog_view_grid), stringResource(Res.string.catalog_view_list)),
                selectedIndex = if (state.catalogView == CatalogView.GRID) 0 else 1,
                onSelect = { viewModel.setCatalogView(if (it == 0) CatalogView.GRID else CatalogView.LIST) },
            )
            AppButton(
                stringResource(Res.string.catalog_action_export),
                { viewModel.startSelection() },
                icon = AppIcons.PictureAsPdf,
                enabled = state.rows.isNotEmpty() && state.busy == null,
                active = state.selectionMode,
            )
            AppButton(stringResource(Res.string.catalog_action_new), onNewProduct, kind = ButtonKind.Primary, icon = AppIcons.Add, enabled = !state.selectionMode)
        }
    }
    if (state.hasItems) CatalogFilters(state, viewModel, Modifier.padding(bottom = space.md))
}

/** Busca, categoria e período numa linha só, com os campos do kit. A categoria só aparece quando algum produto tem uma. */
@Composable
private fun CatalogFilters(state: HistoryUiState, viewModel: QuoteHistoryViewModel, modifier: Modifier = Modifier) {
    val filter = state.filter
    val allLabel = stringResource(Res.string.catalog_filter_category_all)
    val noneLabel = stringResource(Res.string.catalog_filter_category_none)
    val periodNames = PeriodPreset.entries.associateWith { stringResource(it.labelRes()) }
    val categoryOptions = listOf<CategoryFilter>(CategoryFilter.All) + state.categories.map(CategoryFilter::Named) +
        listOfNotNull(CategoryFilter.None.takeIf { state.hasUncategorized })
    val categoryLabel: (CategoryFilter) -> String = { option ->
        when (option) {
            CategoryFilter.All -> allLabel
            CategoryFilter.None -> noneLabel
            is CategoryFilter.Named -> option.name
        }
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.Top) {
        AppTextField(
            label = stringResource(Res.string.catalog_search),
            value = filter.query,
            onValueChange = viewModel::setSearchQuery,
            placeholder = stringResource(Res.string.catalog_search_hint),
            modifier = Modifier.weight(1f, fill = false).widthIn(max = AppDimens.searchMaxWidth).fillMaxWidth(),
        )
        if (state.categories.isNotEmpty()) {
            AppDropdownField(
                label = stringResource(Res.string.catalog_filter_category),
                items = categoryOptions,
                selected = filter.category,
                itemLabel = categoryLabel,
                displayText = categoryLabel,
                onSelect = viewModel::setCategoryFilter,
                modifier = Modifier.width(AppDimens.filterFieldWidth),
            )
        }
        AppDropdownField(
            label = stringResource(Res.string.history_filter_period),
            items = PeriodPreset.entries,
            selected = filter.period,
            itemLabel = { periodNames.getValue(it) },
            displayText = { periodNames.getValue(it) },
            onSelect = viewModel::setPeriodFilter,
            modifier = Modifier.width(AppDimens.filterFieldWidth),
        )
    }
}

/** O rodapé do modo de seleção: sem nenhum marcado, exporta tudo o que o filtro mostra; com marcados, só eles. */
@Composable
private fun CatalogSelectionBar(state: HistoryUiState, viewModel: QuoteHistoryViewModel) {
    val count = state.selectedVisibleCount
    SelectionBar(
        summary = if (count > 0) pluralStringResource(Res.plurals.history_selection_count, count, count) else stringResource(Res.string.history_selection_none),
    ) {
        AppButton(stringResource(Res.string.history_selection_select_all), viewModel::selectAllVisible, enabled = count < state.rows.size)
        AppButton(
            if (count > 0) {
                stringResource(Res.string.catalog_selection_export_selected)
            } else {
                pluralStringResource(Res.plurals.catalog_selection_all, state.rows.size, state.rows.size)
            },
            viewModel::exportCatalogPdf,
            icon = AppIcons.PictureAsPdf,
            enabled = state.busy == null,
        )
        AppButton(stringResource(Res.string.common_cancel), viewModel::clearSelection)
    }
}
