package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.history_details_field_category
import _3dreport.composeapp.generated.resources.history_details_field_client
import _3dreport.composeapp.generated.resources.history_details_field_contact
import _3dreport.composeapp.generated.resources.history_details_field_link
import _3dreport.composeapp.generated.resources.history_details_field_name
import _3dreport.composeapp.generated.resources.history_details_field_photo
import _3dreport.composeapp.generated.resources.history_details_help
import _3dreport.composeapp.generated.resources.history_details_help_detail
import _3dreport.composeapp.generated.resources.history_details_photo_alt
import _3dreport.composeapp.generated.resources.history_details_photo_change
import _3dreport.composeapp.generated.resources.history_details_photo_pick
import _3dreport.composeapp.generated.resources.history_details_photo_remove
import _3dreport.composeapp.generated.resources.history_details_placeholder_contact
import _3dreport.composeapp.generated.resources.history_details_title_order
import _3dreport.composeapp.generated.resources.history_details_title_product
import _3dreport.composeapp.generated.resources.quote_deadline_title
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.matchingClients
import com.threedreport.app.ui.quote.DeliveryDatePicker
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.Client
import org.jetbrains.compose.resources.stringResource

/**
 * "Editar detalhes" (decisão 108): o que muda sem mexer no preço, como nome, cliente, contato, foto, prazo, link e
 * categoria. É a edição mais comum (corrigir o telefone do cliente, trocar a foto) e antes exigia reabrir o cálculo
 * inteiro, que ainda reprecificava o pedido com os custos de hoje. O formulário vive no ViewModel ([DetailsEditing]).
 *
 * Não fecha com clique fora: é um formulário, e perder o que foi digitado por um clique distraído é pior do que um
 * clique a mais em Cancelar.
 */
@Composable
fun QuoteDetailsDialog(editing: DetailsEditing, viewModel: QuoteHistoryViewModel, clients: List<Client>, today: Long) {
    val quote = editing.quote
    val form = editing.form
    AppDialog(
        title = stringResource(if (quote.isOrder) Res.string.history_details_title_order else Res.string.history_details_title_product),
        onDismiss = viewModel::cancelEditingDetails,
        dismissOnClickOutside = false,
        help = HelpText(stringResource(Res.string.history_details_help), stringResource(Res.string.history_details_help_detail)),
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), viewModel::cancelEditingDetails)
            AppButton(stringResource(Res.string.common_save), viewModel::saveDetails, kind = ButtonKind.Primary)
        },
    ) {
        AppTextField(
            label = stringResource(Res.string.history_details_field_name),
            value = form.name,
            onValueChange = { text -> viewModel.updateDetails { it.copy(name = text) } },
            placeholder = quote.name.takeIf { quote.autoNamed },
        )
        if (quote.isOrder) {
            AppSuggestField(
                label = stringResource(Res.string.history_details_field_client),
                value = form.clientName,
                onValueChange = { text -> viewModel.updateDetails { it.copy(clientName = text, clientId = null) } },
                suggestions = matchingClients(form.clientName, clients),
                suggestionLabel = { client -> listOfNotNull(client.name, client.contact).joinToString(" · ") },
                onChoose = { client -> viewModel.updateDetails { it.copy(clientName = client.name, clientContact = client.contact.orEmpty(), clientId = client.id) } },
            )
            AppTextField(
                label = stringResource(Res.string.history_details_field_contact),
                value = form.clientContact,
                onValueChange = { text -> viewModel.updateDetails { it.copy(clientContact = text) } },
                placeholder = stringResource(Res.string.history_details_placeholder_contact),
            )
            Text(stringResource(Res.string.quote_deadline_title), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DeliveryDatePicker(
                epochDay = form.deliveryDateEpochDay,
                daysLate = viewModel.deliveryDaysLate(form.deliveryDateEpochDay),
                today = today,
                onChange = { day -> viewModel.updateDetails { it.copy(deliveryDateEpochDay = day) } },
            )
        } else {
            AppTextField(
                label = stringResource(Res.string.history_details_field_category),
                value = form.category,
                onValueChange = { text -> viewModel.updateDetails { it.copy(category = text) } },
            )
        }
        PhotoRow(editing, viewModel)
        AppTextField(
            label = stringResource(Res.string.history_details_field_link),
            value = form.sourceLink,
            onValueChange = { text -> viewModel.updateDetails { it.copy(sourceLink = text) } },
        )
    }
}

@Composable
private fun PhotoRow(editing: DetailsEditing, viewModel: QuoteHistoryViewModel) {
    val photo = editing.form.photo
    Text(stringResource(Res.string.history_details_field_photo), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        if (photo != null) {
            val bitmap = remember(photo) { runCatching { decodeImageBitmap(photo.bytes) }.getOrNull() }
            bitmap?.let {
                Image(
                    it,
                    contentDescription = stringResource(Res.string.history_details_photo_alt),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AppDimens.thumbnailSize).clip(MaterialTheme.shapes.extraSmall),
                )
            }
            AppButton(stringResource(Res.string.history_details_photo_change), viewModel::chooseDetailsPhoto)
            AppButton(stringResource(Res.string.history_details_photo_remove), { viewModel.updateDetails { it.copy(photo = null) } })
        } else {
            AppButton(stringResource(Res.string.history_details_photo_pick), viewModel::chooseDetailsPhoto)
        }
    }
    editing.photoError?.let { Text(it.asString(), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.error) }
}
