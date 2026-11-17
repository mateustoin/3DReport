package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_error_photo_unreadable
import _3dreport.composeapp.generated.resources.history_notice_details_saved
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.QuoteDetails
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.text
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Client
import com.threedreport.core.model.SavedQuote

/**
 * O "Editar detalhes" (decisão 108): nome, cliente, contato, foto, prazo, link e categoria, sem recalcular nada. O
 * formulário vive em [HistoryUiState.details]; aqui ficam os eventos dele.
 */
internal class HistoryDetailsEditor(
    private val repository: QuoteHistoryRepository,
    private val clientRepository: ClientRepository?,
    private val platform: PlatformServices,
    private val state: () -> HistoryUiState,
    private val change: ((HistoryUiState) -> HistoryUiState) -> Unit,
    private val notify: (UiNotice) -> Unit,
) {
    fun start(savedQuote: SavedQuote) {
        val form = DetailsForm(
            name = savedQuote.name.takeUnless { savedQuote.autoNamed }.orEmpty(),
            clientName = savedQuote.client?.name.orEmpty(),
            clientContact = savedQuote.client?.contact.orEmpty(),
            clientId = savedQuote.client?.id,
            sourceLink = savedQuote.sourceLink.orEmpty(),
            photo = savedQuote.photoFileName?.let { fileName -> repository.photoBytes(savedQuote)?.let { PickedFile(fileName, it) } },
            deliveryDateEpochDay = savedQuote.deliveryDateEpochDay,
            category = savedQuote.category.orEmpty(),
        )
        change { it.copy(details = DetailsEditing(savedQuote, form)) }
    }

    fun cancel() = change { it.copy(details = null) }

    fun update(transform: (DetailsForm) -> DetailsForm) =
        change { s -> s.copy(details = s.details?.let { it.copy(form = transform(it.form), photoError = null) }) }

    /** Escolhe uma foto nova. Arquivo que não abre como imagem vira o aviso no diálogo, e a foto antiga fica. */
    fun choosePhoto() {
        when (val picked = platform.pickFile(FileKind.IMAGE)) {
            PickResult.Cancelled -> Unit

            is PickResult.Deferred -> Unit

            is PickResult.Failed -> showPhotoError(picked.text())

            is PickResult.Picked ->
                if (runCatching { decodeImageBitmap(picked.file.bytes) }.isSuccess) {
                    update { it.copy(photo = picked.file) }
                } else {
                    showPhotoError(uiText(Res.string.history_error_photo_unreadable, picked.file.fileName))
                }
        }
    }

    private fun showPhotoError(error: UiText) = change { s -> s.copy(details = s.details?.copy(photoError = error)) }

    /** Grava o formulário. O cliente digitado é achado ou criado no cadastro, como ao salvar o pedido. */
    fun save() {
        val editing = state().details ?: return
        val form = editing.form
        val clientName = form.clientName.trim()
        val client = if (!editing.quote.isOrder || clientName.isEmpty()) {
            null
        } else {
            val typed = Client(name = clientName, contact = form.clientContact.trim().ifEmpty { null }, id = form.clientId)
            clientRepository?.resolve(typed) ?: typed
        }
        val saved = repository.updateDetails(
            editing.quote.id,
            QuoteDetails(
                name = form.name,
                client = client,
                sourceLink = form.sourceLink,
                photo = form.photo,
                deliveryDateEpochDay = form.deliveryDateEpochDay,
                category = form.category,
            ),
        )
        change { it.copy(details = null) }
        if (saved != null) notify(UiNotice(uiText(Res.string.history_notice_details_saved, saved.name)))
    }
}
