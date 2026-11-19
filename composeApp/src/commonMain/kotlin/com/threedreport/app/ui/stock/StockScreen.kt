package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.stock_action_manual_use
import _3dreport.composeapp.generated.resources.stock_action_register_purchase
import _3dreport.composeapp.generated.resources.stock_action_track
import _3dreport.composeapp.generated.resources.stock_action_weigh
import _3dreport.composeapp.generated.resources.stock_alert_banner
import _3dreport.composeapp.generated.resources.stock_empty_action
import _3dreport.composeapp.generated.resources.stock_empty_balance
import _3dreport.composeapp.generated.resources.stock_first_use
import _3dreport.composeapp.generated.resources.stock_purchases_empty
import _3dreport.composeapp.generated.resources.stock_row_limit
import _3dreport.composeapp.generated.resources.stock_row_low
import _3dreport.composeapp.generated.resources.stock_row_out
import _3dreport.composeapp.generated.resources.stock_row_reserved
import _3dreport.composeapp.generated.resources.stock_section_consumables
import _3dreport.composeapp.generated.resources.stock_section_filaments
import _3dreport.composeapp.generated.resources.stock_section_untracked
import _3dreport.composeapp.generated.resources.stock_shopping_buy
import _3dreport.composeapp.generated.resources.stock_shopping_copy
import _3dreport.composeapp.generated.resources.stock_shopping_empty
import _3dreport.composeapp.generated.resources.stock_shopping_empty_untracked
import _3dreport.composeapp.generated.resources.stock_shopping_out_row
import _3dreport.composeapp.generated.resources.stock_shopping_out_title
import _3dreport.composeapp.generated.resources.stock_shopping_reason_balance
import _3dreport.composeapp.generated.resources.stock_shopping_reason_limit
import _3dreport.composeapp.generated.resources.stock_shopping_reason_orders
import _3dreport.composeapp.generated.resources.stock_shopping_register
import _3dreport.composeapp.generated.resources.stock_shopping_see_balance
import _3dreport.composeapp.generated.resources.stock_title
import _3dreport.composeapp.generated.resources.stock_view_balance
import _3dreport.composeapp.generated.resources.stock_view_purchases
import _3dreport.composeapp.generated.resources.stock_view_shopping
import _3dreport.composeapp.generated.resources.stock_view_shopping_count
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.threedreport.app.platform.formatDate
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AttentionNotice
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.CollapsibleSection
import com.threedreport.app.ui.components.ColorSwatch
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.ListScreenLayout
import com.threedreport.app.ui.components.ObserveUiNotices
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.filaments.parseHexColor
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.report.StockState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tela Estoque (decisão 163): quanto sobra de cada cor e de cada insumo, as compras e a lista do que comprar, em três
 * vistas. O item ou a compra abertos ficam no painel à direita, como nos cadastros.
 */
@Composable
fun StockScreen(viewModel: StockViewModel, onRegisterFilament: () -> Unit = {}, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveUiNotices(viewModel.notices)

    val panel: (@Composable () -> Unit)? = state.panel?.let { open ->
        {
            when (open) {
                is StockPanel.Item -> StockItemPanelView(open.panel, viewModel)
                is StockPanel.Purchase -> StockPurchasePanelView(open.panel, state.today, viewModel)
            }
        }
    }
    ListScreenLayout(panel = panel, modifier = modifier) {
        ScreenHeader(stringResource(Res.string.stock_title)) {
            // Sem nenhum cadastro não há o que comprar: o botão âmbar sai, e o estado vazio leva a cadastrar (como em Filamentos).
            if (!state.hasNoItems) {
                AppButton(
                    stringResource(Res.string.stock_action_register_purchase),
                    onClick = viewModel::startPurchase,
                    kind = if (panel == null) ButtonKind.Primary else ButtonKind.Secondary,
                    icon = AppIcons.Add,
                )
            }
        }
        val shoppingLabel = if (state.shopping.size > 0) {
            stringResource(Res.string.stock_view_shopping_count, state.shopping.size)
        } else {
            stringResource(Res.string.stock_view_shopping)
        }
        SegmentedChoice(
            options = listOf(stringResource(Res.string.stock_view_balance), stringResource(Res.string.stock_view_purchases), shoppingLabel),
            selectedIndex = state.view.ordinal,
            onSelect = { viewModel.selectView(StockView.entries[it]) },
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.md),
        )
        when (state.view) {
            StockView.BALANCE -> BalanceView(state, viewModel, onRegisterFilament)
            StockView.PURCHASES -> PurchasesView(state, viewModel)
            StockView.SHOPPING -> ShoppingView(state, viewModel)
        }
    }

    state.dialog?.let { StockDialogView(it, viewModel) }
}

// --------------------------------------------------------------------------------------------------------- saldo

@Composable
private fun BalanceView(state: StockUiState, viewModel: StockViewModel, onRegisterFilament: () -> Unit) {
    val space = MaterialTheme.spacing
    if (state.hasNoItems) {
        EmptyState(
            stringResource(Res.string.stock_empty_balance),
            actionLabel = stringResource(Res.string.stock_empty_action),
            onAction = onRegisterFilament,
            topPadding = false,
        )
        return
    }
    val openRef = (state.panel as? StockPanel.Item)?.panel?.entry?.ref
    if (state.alertCount > 0) {
        AttentionNotice(
            pluralStringResource(Res.plurals.stock_alert_banner, state.alertCount, state.alertCount),
            modifier = Modifier.padding(bottom = space.md),
        )
    }
    if (state.filamentGroups.isNotEmpty()) {
        SectionLabel(stringResource(Res.string.stock_section_filaments))
        state.filamentGroups.forEach { group ->
            key(group.rows.first().ref) {
                // Um filamento de uma cor só e sem nome é a própria linha: o nome dele no lugar do da cor.
                val flat = group.rows.size == 1 && group.rows.single().entry.name == null
                if (!flat) GroupLabel(group.filamentName)
                group.rows.forEach { row ->
                    key(row.ref) { TrackedRow(row, viewModel, title = if (flat) group.filamentName else row.entry.colorLabel(), selected = row.ref == openRef) }
                }
            }
        }
    }
    if (state.consumables.isNotEmpty()) {
        SectionLabel(stringResource(Res.string.stock_section_consumables), topSpace = state.filamentGroups.isNotEmpty())
        state.consumables.forEach { row -> key(row.ref) { TrackedRow(row, viewModel, title = row.entry.label(), selected = row.ref == openRef) } }
    }
    if (state.untracked.isNotEmpty()) {
        // Primeiro uso: nada controlado ainda, então a lista já vem aberta, com uma frase que diz o que fazer.
        val firstUse = state.filamentGroups.isEmpty() && state.consumables.isEmpty()
        if (firstUse) {
            Text(
                stringResource(Res.string.stock_first_use),
                style = MaterialTheme.appType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = space.xs),
            )
        }
        CollapsibleSection(stringResource(Res.string.stock_section_untracked, state.untracked.size), initiallyExpanded = firstUse) {
            state.untracked.forEach { entry ->
                key(entry.ref) {
                    ListRow(
                        title = entry.label(),
                        actions = emptyList(),
                        leading = entry.marker(),
                        trailing = {
                            AppButton(
                                stringResource(Res.string.stock_action_track),
                                onClick = { viewModel.startTrack(entry.ref) },
                                height = AppDimens.smallControlHeight,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Uma linha de item controlado: o saldo à direita, o reservado e o limite embaixo do nome, e "Pesei" e "Avulso" ao passar o mouse. */
@Composable
private fun TrackedRow(row: StockRowState, viewModel: StockViewModel, title: String, selected: Boolean) {
    val status = row.status
    val ref = row.ref
    val reserved = if (status.reserved > 0) stringResource(Res.string.stock_row_reserved, stockAmountText(ref, status.reserved)) else null
    val limit = status.item.lowThreshold
    val state = when (status.state) {
        StockState.OUT -> stringResource(Res.string.stock_row_out)
        StockState.LOW -> limit?.let { stringResource(Res.string.stock_row_low, stockAmountText(ref, it)) }
        StockState.OK -> limit?.let { stringResource(Res.string.stock_row_limit, stockAmountText(ref, it)) }
    }
    val actions = buildList {
        if (ref is StockItemRef.ColorRef) add(RowAction(AppIcons.Scale, stringResource(Res.string.stock_action_weigh), { viewModel.startWeigh(ref) }))
        add(RowAction(AppIcons.Outbox, stringResource(Res.string.stock_action_manual_use), { viewModel.startManualUse(ref) }))
    }
    ListRow(
        title = title,
        details = listOfNotNull(reserved, state),
        value = stockAmountText(ref, status.balance),
        valueError = status.state != StockState.OK,
        onClick = { viewModel.openItem(ref) },
        selected = selected,
        actions = actions,
        leading = row.entry.swatch(),
    )
}

/** A amostra da cor, só pra item de filamento; o insumo não leva nada. */
@Composable
private fun StockEntry.swatch(): (@Composable () -> Unit)? =
    if (ref is StockItemRef.ColorRef) {
        { ColorSwatch(parseHexColor(colorHex)) }
    } else {
        null
    }

/** Como [swatch], mas o insumo leva um espaço do tamanho da amostra: numa lista que mistura os dois, os nomes alinham. */
@Composable
private fun StockEntry.marker(): (@Composable () -> Unit)? =
    if (ref is StockItemRef.ColorRef) {
        { ColorSwatch(parseHexColor(colorHex)) }
    } else {
        { Spacer(Modifier.size(AppDimens.swatchSize)) }
    }

@Composable
private fun SectionLabel(text: String, topSpace: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.appType.sectionTitle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.then(if (topSpace) Modifier.padding(top = MaterialTheme.spacing.xl) else Modifier).padding(bottom = MaterialTheme.spacing.xxs),
    )
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.appType.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = MaterialTheme.spacing.sm, top = MaterialTheme.spacing.sm),
    )
}

// ------------------------------------------------------------------------------------------------------ compras

@Composable
private fun PurchasesView(state: StockUiState, viewModel: StockViewModel) {
    if (state.purchases.isEmpty()) {
        EmptyState(stringResource(Res.string.stock_purchases_empty), topPadding = false)
        return
    }
    state.purchases.forEach { purchase ->
        key(purchase.id) {
            val summary = purchase.lines.map { line -> "${line.entry.label()} ${stockPurchaseAmountText(line.entry.ref, line.amount)}" }.joinToString(", ")
            ListRow(
                title = summary,
                details = listOfNotNull(formatDate(purchase.dateEpochDay), purchase.supplier),
                value = purchase.total.toMoney(),
                selected = purchase.selected,
                onClick = { viewModel.openPurchase(purchase.id) },
                actions = listOf(
                    RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.openPurchase(purchase.id) }),
                    RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDeletePurchase(purchase.id) }, danger = true),
                ),
            )
        }
    }
}

// ------------------------------------------------------------------------------------------- lista de compras

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingView(state: StockUiState, viewModel: StockViewModel) {
    val shopping = state.shopping
    if (shopping.isEmpty) {
        EmptyState(
            message = stringResource(if (shopping.hasTracked) Res.string.stock_shopping_empty else Res.string.stock_shopping_empty_untracked),
            actionLabel = stringResource(Res.string.stock_shopping_see_balance).takeIf { !shopping.hasTracked },
            onAction = { viewModel.selectView(StockView.BALANCE) }.takeIf { !shopping.hasTracked },
            actionKind = ButtonKind.Secondary,
            topPadding = false,
        )
        return
    }
    val space = MaterialTheme.spacing
    if (shopping.lines.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(bottom = space.xs),
            horizontalArrangement = Arrangement.spacedBy(space.xs),
            verticalArrangement = Arrangement.spacedBy(space.xs),
        ) {
            AppButton(stringResource(Res.string.stock_shopping_copy), viewModel::copyShoppingList, icon = AppIcons.ContentCopy)
            AppButton(stringResource(Res.string.stock_shopping_register), viewModel::startPurchaseFromShopping)
        }
        shopping.lines.forEach { row ->
            key(row.entry.ref) {
                val ref = row.entry.ref
                val line = row.line
                ListRow(
                    title = row.entry.label(),
                    details = listOfNotNull(
                        stringResource(Res.string.stock_shopping_reason_orders, stockAmountText(ref, line.reserved)).takeIf { line.reserved > 0 },
                        stringResource(Res.string.stock_shopping_reason_balance, stockAmountText(ref, line.balance)),
                        line.lowThreshold?.let { stringResource(Res.string.stock_shopping_reason_limit, stockAmountText(ref, it)) },
                    ),
                    value = stringResource(Res.string.stock_shopping_buy, stockAmountText(ref, row.amount)),
                    actions = emptyList(),
                    leading = row.entry.marker(),
                )
            }
        }
    }
    if (shopping.outOfStock.isNotEmpty()) {
        Column(modifier = if (shopping.lines.isNotEmpty()) Modifier.padding(top = space.xl) else Modifier) {
            SectionLabel(stringResource(Res.string.stock_shopping_out_title))
            shopping.outOfStock.forEach { row ->
                key(row.entry.ref) {
                    ListRow(
                        title = row.entry.label(),
                        details = listOf(pluralStringResource(Res.plurals.stock_shopping_out_row, row.orderCount, row.orderCount)),
                        actions = emptyList(),
                        leading = row.entry.marker(),
                    )
                }
            }
        }
    }
}
