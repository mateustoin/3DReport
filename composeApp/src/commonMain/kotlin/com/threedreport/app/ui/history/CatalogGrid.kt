package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.catalog_action_sell
import _3dreport.composeapp.generated.resources.catalog_reprice_changed
import _3dreport.composeapp.generated.resources.catalog_reprice_unavailable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.CheckboxMark
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.cardFrame
import com.threedreport.app.ui.components.clickableAction
import com.threedreport.app.ui.components.clickableCard
import com.threedreport.app.ui.components.rememberCardState
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.stringResource

/**
 * Cartão de produto do Catálogo em grade (decisão 132): foto em destaque (ou o fundo `cardBlock` com o ícone, sem
 * ilustração), nome, categoria, preço e "Vender" como botão secundário. O menu das demais ações aparece no hover ou no
 * foco. No modo de seleção, o cartão ganha a caixa de marcar e clicar nele marca.
 */
@Composable
fun ProductCard(
    row: HistoryRow,
    state: HistoryUiState,
    actions: QuoteActions,
    loadThumbnail: (SavedQuote) -> ByteArray?,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit,
    onReprice: () -> Unit,
) {
    val quote = row.quote
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val card = rememberCardState()
    val active = card.active
    val selected = state.selectionMode && quote.id in state.selectedIds

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardFrame(card, selected = selected),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickableCard(card, onClick = if (state.selectionMode) onToggleSelected else onOpen),
        ) {
            PhotoThumbnail(
                quote,
                loadThumbnail,
                Modifier.fillMaxWidth().height(AppDimens.productPhotoHeight),
                placeholder = true,
                shape = RectangleShape,
            )
            Column(modifier = Modifier.padding(space.sm), verticalArrangement = Arrangement.spacedBy(space.xxs)) {
                Text(quote.name, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    quote.category.orEmpty(),
                    style = MaterialTheme.appType.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!state.selectionMode) RepriceHint(row, { value -> value.toCurrencyText(quote.currency) }, onReprice)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NumericText(quote.totalWithServices.toCurrencyText(quote.currency), style = MaterialTheme.appType.sectionTitle, color = MaterialTheme.colorScheme.onSurface)
                    actions.onSell?.takeUnless { state.selectionMode }?.let { sell -> AppButton(stringResource(Res.string.catalog_action_sell), sell, height = AppDimens.smallControlHeight) }
                }
            }
        }
        if (state.selectionMode) {
            CheckboxMark(selected, modifier = Modifier.align(Alignment.TopStart).padding(space.xs))
        } else {
            QuoteActionsMenu(
                quote,
                actions,
                modifier = Modifier.align(Alignment.TopEnd).padding(space.xs).alpha(if (active) 1f else 0f).background(colors.card, MaterialTheme.shapes.small),
            )
        }
    }
}

/**
 * O aviso do produto quando o preço de hoje não é o guardado (decisão 102), sem âmbar: "Custos mudaram" abre o "Atualizar
 * preço", e o produto que não dá pra recalcular diz o motivo numa dica. Só aparece quando há o que fazer.
 */
@Composable
fun RepriceHint(row: HistoryRow, money: (Double) -> String, onReprice: () -> Unit) {
    when (val notice = row.reprice) {
        null -> Unit

        is RepriceNotice.CostsChanged -> Row(
            modifier = Modifier.clickableAction(onReprice),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
        ) {
            Icon(AppIcons.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.iconSize))
            Text(
                stringResource(Res.string.catalog_reprice_changed, money(notice.newPrice)),
                style = MaterialTheme.appType.label,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        is RepriceNotice.Unavailable -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
            val title = stringResource(Res.string.catalog_reprice_unavailable)
            Text(title, style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HelpHint(HelpText(notice.reason.asString()), title)
        }
    }
}
