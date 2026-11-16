package com.threedreport.app.ui.dashboard

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.dashboard_action_see_orders
import _3dreport.composeapp.generated.resources.dashboard_block_profit
import _3dreport.composeapp.generated.resources.dashboard_block_profit_per_machine_hour
import _3dreport.composeapp.generated.resources.dashboard_block_sales
import _3dreport.composeapp.generated.resources.dashboard_block_total
import _3dreport.composeapp.generated.resources.dashboard_empty_no_sales
import _3dreport.composeapp.generated.resources.dashboard_empty_none
import _3dreport.composeapp.generated.resources.dashboard_no_data
import _3dreport.composeapp.generated.resources.dashboard_per_hour
import _3dreport.composeapp.generated.resources.dashboard_period
import _3dreport.composeapp.generated.resources.dashboard_title
import _3dreport.composeapp.generated.resources.orders_action_new
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AdaptiveChoice
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.components.SummaryBlock
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Dashboard em blocos (decisão 133): o período no cabeçalho, quatro números em destaque, o ranking de peças à esquerda
 * e, à direita, os blocos de pares rótulo e valor. Só desenha o [DashboardUiState] e envia eventos; "Novo orçamento" e
 * "Ver pedidos" (os estados vazios) saem pelos callbacks. A tela não tem ação principal, então não há botão âmbar.
 * Abaixo de [AppDimens.twoColumnMinWidth] as duas colunas viram uma e o período vira lista suspensa.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNewQuote: () -> Unit,
    onShowOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val space = MaterialTheme.spacing
    val periodNames = DashboardPeriods.map { stringResource(it.dashboardLabelRes()) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= AppDimens.twoColumnMinWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl),
        ) {
            ScreenHeader(stringResource(Res.string.dashboard_title)) {
                AdaptiveChoice(
                    label = stringResource(Res.string.dashboard_period),
                    options = periodNames,
                    selectedIndex = DashboardPeriods.indexOf(state.period),
                    onSelect = { viewModel.setPeriod(DashboardPeriods[it]) },
                    compact = !wide,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(space.xl)) {
                DashboardBody(state, wide, onNewQuote, onShowOrders)
            }
        }
    }
}

/** O corpo da tela, conforme o [DashboardMode]. */
@Composable
private fun DashboardBody(state: DashboardUiState, wide: Boolean, onNewQuote: () -> Unit, onShowOrders: () -> Unit) {
    when (state.mode) {
        DashboardMode.SALES -> {
            SummaryBlocks(state, wide)
            val ranking: (@Composable ColumnScope.() -> Unit)? = if (state.summary.topProducts.isEmpty()) null else { { SalesRanking(state) } }
            Columns(wide = wide, left = ranking, right = { SalesBlocks(state) })
        }

        DashboardMode.NO_SALES -> Empty(
            state = state,
            wide = wide,
            message = stringResource(Res.string.dashboard_empty_no_sales),
            actionLabel = stringResource(Res.string.dashboard_action_see_orders),
            onAction = onShowOrders,
        )

        DashboardMode.NOTHING_SAVED -> Empty(
            state = state,
            wide = wide,
            message = stringResource(Res.string.dashboard_empty_none),
            actionLabel = stringResource(Res.string.orders_action_new),
            onAction = onNewQuote,
        )
    }
}

/** O estado vazio (uma frase e a ação) e, abaixo dele, o que ainda há pra mostrar: o que está em aberto e o catálogo. */
@Composable
private fun Empty(state: DashboardUiState, wide: Boolean, message: String, actionLabel: String, onAction: () -> Unit) {
    // O próprio EmptyState já traz o respiro de baixo, então este conjunto não leva o espaço entre seções.
    Column {
        EmptyState(message, actionLabel = actionLabel, onAction = onAction, actionKind = ButtonKind.Secondary)
        val catalog = state.catalog
        val ranking: (@Composable ColumnScope.() -> Unit)? = if (catalog == null) null else { { CatalogRanking(catalog) } }
        Columns(wide = wide, left = ranking, right = { EmptyBlocks(state) })
    }
}

/** Os quatro números em destaque: numa linha só quando cabe, em duas de duas quando a janela é estreita. */
@Composable
private fun SummaryBlocks(state: DashboardUiState, wide: Boolean) {
    val summary = state.summary
    val noData = stringResource(Res.string.dashboard_no_data)
    val blocks: List<@Composable (Modifier) -> Unit> = listOf(
        { m -> SummaryBlock(summary.quoteCount.toString(), stringResource(Res.string.dashboard_block_sales), m) },
        { m -> SummaryBlock(summary.totalSalePrice.toMoney(), stringResource(Res.string.dashboard_block_total), m) },
        { m ->
            SummaryBlock(
                summary.totalProfit.toMoney(),
                stringResource(Res.string.dashboard_block_profit),
                m,
                valueColor = MaterialTheme.appColors.amberText,
            )
        },
        { m ->
            SummaryBlock(
                summary.profitPerPrintHour?.let { stringResource(Res.string.dashboard_per_hour, it.toMoney()) } ?: noData,
                stringResource(Res.string.dashboard_block_profit_per_machine_hour),
                m,
            )
        },
    )
    val gap = MaterialTheme.spacing.sm
    if (wide) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            blocks.forEach { it(Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            blocks.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    pair.forEach { it(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Duas colunas quando [wide]: [left] ocupa a sobra e [right] tem a largura do painel lateral. Estreita, uma coluna só.
 * Sem [left], o [right] fica sozinho, na largura do painel.
 */
@Composable
private fun Columns(wide: Boolean, left: (@Composable ColumnScope.() -> Unit)?, right: @Composable ColumnScope.() -> Unit) {
    val gap = MaterialTheme.spacing.xl
    when {
        !wide -> Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            left?.invoke(this)
            right()
        }

        left == null -> Column(modifier = Modifier.widthIn(max = AppDimens.sidePanelWidth), verticalArrangement = Arrangement.spacedBy(gap)) { right() }

        else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) { left() }
            Column(modifier = Modifier.width(AppDimens.sidePanelWidth), verticalArrangement = Arrangement.spacedBy(gap)) { right() }
        }
    }
}
