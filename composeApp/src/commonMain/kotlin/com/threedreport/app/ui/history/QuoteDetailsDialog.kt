package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.ClientField
import com.threedreport.app.ui.components.matchingClients
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.quote.DeliveryDatePicker
import com.threedreport.core.model.Client
import com.threedreport.core.model.SavedQuote

/**
 * "Editar detalhes" (decisão 108): o que muda sem mexer no preço — nome, cliente, contato, foto, prazo,
 * link e categoria. É a edição mais comum (corrigir o telefone do cliente, trocar a foto) e antes exigia
 * reabrir o cálculo inteiro, que ainda reprecificava o pedido com os custos de hoje.
 *
 * Não fecha com clique fora: é um formulário, e perder o que foi digitado por um clique distraído é pior
 * do que um clique a mais em Cancelar.
 */
@Composable
fun QuoteDetailsDialog(savedQuote: SavedQuote, viewModel: QuoteHistoryViewModel, clients: List<Client>) {
    val isOrder = savedQuote.isOrder
    var form by remember(savedQuote.id) {
        mutableStateOf(
            DetailsForm(
                name = savedQuote.name.takeUnless { savedQuote.hasAutoName }.orEmpty(),
                clientName = savedQuote.client?.name.orEmpty(),
                clientContact = savedQuote.client?.contact.orEmpty(),
                clientId = savedQuote.client?.id,
                sourceLink = savedQuote.sourceLink.orEmpty(),
                photo = viewModel.photoFile(savedQuote),
                deliveryDateEpochDay = savedQuote.deliveryDateEpochDay,
                category = savedQuote.category.orEmpty(),
            ),
        )
    }
    var photoError by remember(savedQuote.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = viewModel::cancelEditingDetails,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Detalhes de \"${savedQuote.name}\"") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "O preço não muda aqui. Pra mudar peso, tempo, filamento ou preço, use \"Editar cálculo\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().tabToNavigate(),
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    label = { Text("Nome da peça") },
                    placeholder = { if (savedQuote.hasAutoName) Text(savedQuote.name) },
                    singleLine = true,
                )
                if (isOrder) {
                    ClientField(
                        name = form.clientName,
                        suggestions = matchingClients(form.clientName, clients),
                        onNameChange = { form = form.copy(clientName = it, clientId = null) },
                        onChoose = { form = form.copy(clientName = it.name, clientContact = it.contact.orEmpty(), clientId = it.id) },
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().tabToNavigate(),
                        value = form.clientContact,
                        onValueChange = { form = form.copy(clientContact = it) },
                        label = { Text("Contato do cliente") },
                        placeholder = { Text("WhatsApp: (11) 99999-0000") },
                        singleLine = true,
                    )
                    Text("Prazo de entrega", style = MaterialTheme.typography.labelLarge)
                    DeliveryDatePicker(epochDay = form.deliveryDateEpochDay, onChange = { form = form.copy(deliveryDateEpochDay = it) })
                } else {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().tabToNavigate(),
                        value = form.category,
                        onValueChange = { form = form.copy(category = it) },
                        label = { Text("Categoria") },
                        singleLine = true,
                    )
                }

                val photo = form.photo
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (photo != null) {
                        val bitmap = remember(photo) { runCatching { decodeImageBitmap(photo.bytes) }.getOrNull() }
                        bitmap?.let { Image(it, contentDescription = "Foto da peça", modifier = Modifier.size(56.dp)) }
                        OutlinedButton(onClick = { pickPhoto(viewModel) { picked, error -> picked?.let { form = form.copy(photo = it) }; photoError = error } }) {
                            Text("Trocar foto")
                        }
                        TextButton(onClick = { form = form.copy(photo = null) }) { Text("Remover foto") }
                    } else {
                        OutlinedButton(onClick = { pickPhoto(viewModel) { picked, error -> picked?.let { form = form.copy(photo = it) }; photoError = error } }) {
                            Text("Escolher foto")
                        }
                    }
                }
                photoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().tabToNavigate(),
                    value = form.sourceLink,
                    onValueChange = { form = form.copy(sourceLink = it) },
                    label = { Text("Link do modelo (uso interno)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.saveDetails(form) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = viewModel::cancelEditingDetails) { Text("Cancelar") } },
    )
}

private fun pickPhoto(viewModel: QuoteHistoryViewModel, onResult: (com.threedreport.app.platform.PickedFile?, String?) -> Unit) {
    when (val picked = viewModel.pickPhoto()) {
        is PhotoPick.Picked -> onResult(picked.file, null)
        PhotoPick.Cancelled -> Unit
        is PhotoPick.Failed -> onResult(null, picked.message)
    }
}
