package com.threedreport.app.ui.settings

import com.threedreport.core.model.SavedQuote
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Formulário de cadastro de canal de venda. `editingId` não-nulo significa que o formulário está
 * editando um canal existente em vez de criar um novo.
 */
data class SalesChannelFormState(
    val nameText: String = "",
    val feeRatePercentText: String = "",
    val editingId: String? = null,
    val errorMessage: String? = null,
)

/** Ver [SalesChannel] pro porquê de canal e forma de pagamento serem o mesmo campo. */
class SalesChannelViewModel(
    private val repository: SalesChannelRepository,
    /** Pedidos e produtos salvos, pra saber quem usa cada canal antes de excluir (decisão 115). */
    val savedQuotes: StateFlow<List<SavedQuote>> = MutableStateFlow(emptyList()),
) {

    val channels: StateFlow<List<SalesChannel>> = repository.channels

    private val formState = MutableStateFlow(SalesChannelFormState())
    val form: StateFlow<SalesChannelFormState> = formState.asStateFlow()

    fun setName(text: String) = formState.update { it.copy(nameText = text, errorMessage = null) }
    fun setFeeRate(text: String) = formState.update { it.copy(feeRatePercentText = text, errorMessage = null) }

    fun startEditing(channel: SalesChannel) {
        formState.value = SalesChannelFormState(
            nameText = channel.name,
            feeRatePercentText = (channel.feeRate * 100).toInputText(),
            editingId = channel.id,
        )
    }

    fun cancelEditing() {
        formState.value = SalesChannelFormState()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value
        val name = current.nameText.trim()

        val channel = runCatching {
            require(name.isNotBlank()) { "Dê um nome ao canal (ex.: Shopee, Cartão, Pix)." }
            // Em branco é taxa zero (Pix, dinheiro); texto que não é número é erro, e não zero: um "20 por
            // cento" que virasse 0% tiraria a taxa do marketplace do preço sem ninguém ver.
            val feeRate = if (current.feeRatePercentText.isBlank()) {
                0.0
            } else {
                (parseDecimal(current.feeRatePercentText, NumberKind.MEASURE) ?: error("A taxa não é um número. Ex.: 20 ou 4,99.")) / 100.0
            }
            require(feeRate >= 0 && feeRate < 1) { "A taxa precisa ficar entre 0% e 100%." }
            // Id aleatório, como o de todo cadastro: gerado do nome, renomear um canal e criar outro com o
            // nome antigo fazia os dois terem o mesmo id.
            SalesChannel(id = current.editingId ?: Uuid.random().toString(), name = name, feeRate = feeRate)
        }

        formState.value = channel.fold(
            onSuccess = {
                val archived = repository.channels.value.find { channel -> channel.id == it.id }?.archived == true
                if (current.editingId != null) repository.update(it.copy(archived = archived)) else repository.add(it)
                SalesChannelFormState()
            },
            onFailure = { current.copy(errorMessage = it.message) },
        )
    }

    /** Quantos pedidos e produtos usam o canal [id]. */
    fun usageCount(id: String): Int = CatalogUsage.channel(id, savedQuotes.value)

    /** Arquiva (ou restaura) o canal [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        val channel = repository.channels.value.find { it.id == id } ?: return
        repository.update(channel.copy(archived = archived))
    }

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value.editingId == id) formState.value = SalesChannelFormState()
    }
}
