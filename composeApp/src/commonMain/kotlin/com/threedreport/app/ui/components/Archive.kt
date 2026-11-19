package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_archived_section
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_delete_anyway
import _3dreport.composeapp.generated.resources.common_delete_in_use
import _3dreport.composeapp.generated.resources.common_delete_in_use_hint
import _3dreport.composeapp.generated.resources.common_orders_or_products
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Archivable
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * O que um seletor do Orçamento oferece (decisão 115): arquivado sai das escolhas, mas o que já está
 * escolhido continua aparecendo.
 */
fun <T : Archivable<T>> List<T>.availableOrSelected(isSelected: (T) -> Boolean): List<T> =
    filter { !it.archived || isSelected(it) }

/**
 * Quantos pedidos e produtos usam cada cadastro (decisão 115), contando pelos retratos salvos. Serve pra
 * confirmação de excluir sugerir arquivar quando o item está em uso. Recebe também os da lixeira, que
 * podem voltar e continuam usando o item.
 */
object CatalogUsage {
    fun filament(id: String, quotes: List<SavedQuote>) =
        quotes.count { saved -> saved.quote.prints.any { print -> print.job.filaments.any { it.filament.id == id } } }

    fun printer(id: String, quotes: List<SavedQuote>) = quotes.count { saved -> saved.quote.prints.any { it.printerId == id } }

    fun service(id: String, quotes: List<SavedQuote>) = quotes.count { saved -> saved.services.any { it.id == id } }

    fun channel(id: String, quotes: List<SavedQuote>) = quotes.count { it.quote.channelId == id }

    fun consumable(id: String, quotes: List<SavedQuote>) = quotes.count { saved -> saved.quote.consumables.any { it.id == id } }
}

/**
 * Pergunta de exclusão pendente, no estado de uma tela de cadastro: qual item ([id], [name]), em quantos pedidos e
 * produtos ele está ([usageCount], decisão 115) e se dá pra arquivar em vez de excluir ([canArchive]: não, se já
 * está arquivado). O ViewModel monta; a tela mostra o [DeleteOrArchiveDialog].
 */
data class PendingDelete(val id: String, val name: String, val usageCount: Int, val canArchive: Boolean)

/**
 * Confirmação de excluir um cadastro (decisão 115). Sem uso, é a confirmação de sempre. Em uso, diz em
 * quantos e põe "Arquivar" como o botão principal: arquivado some das escolhas do Orçamento e continua
 * valendo pra quem já usou, e excluir continua possível pra quem quer mesmo.
 *
 * @param what o que está sendo excluído, pra frase ("o filamento \"PLA\"").
 * @param deleteMessage o que acontece ao excluir, como a tela já explicava.
 * @param onArchive `null` pra um item que já está arquivado.
 */
@Composable
fun DeleteOrArchiveDialog(
    title: String,
    what: String,
    usageCount: Int,
    deleteMessage: String,
    onArchive: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (usageCount == 0 || onArchive == null) {
        ConfirmDialog(title = title, message = deleteMessage, onConfirm = onDelete, onDismiss = onDismiss)
        return
    }
    AppDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), onDismiss)
            AppButton(stringResource(Res.string.common_delete_anyway), onDelete, kind = ButtonKind.Danger)
            AppButton(stringResource(Res.string.common_archive), onArchive, kind = ButtonKind.Primary)
        },
    ) {
        val usage = pluralStringResource(Res.plurals.common_orders_or_products, usageCount, usageCount)
        DialogText(stringResource(Res.string.common_delete_in_use, what.replaceFirstChar { it.uppercase() }, usage))
        DialogText(stringResource(Res.string.common_delete_in_use_hint))
        DialogText(deleteMessage, muted = true)
    }
}

/** "Arquivados (n)" no fim de uma lista de cadastro, recolhido: o dia a dia é com os ativos. */
@Composable
fun ArchivedSection(count: Int, content: @Composable () -> Unit) {
    if (count == 0) return
    CollapsibleSection(stringResource(Res.string.common_archived_section, count), content)
}

/**
 * Seção recolhida no fim de uma lista, com o [title] em `label` e a seta que abre e fecha ("Arquivados (3)", "Sem controle
 * de saldo (6)"). Começa fechada; a escolha de abrir fica enquanto a tela existe.
 */
@Composable
fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.md)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
    ) {
        Text(
            title,
            style = MaterialTheme.appType.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppDimens.iconSize),
        )
    }
    if (expanded) content()
}
