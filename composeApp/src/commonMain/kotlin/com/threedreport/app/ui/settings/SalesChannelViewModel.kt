package com.threedreport.app.ui.settings

import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
class SalesChannelViewModel(private val repository: SalesChannelRepository) {

    val channels: StateFlow<List<SalesChannel>> = repository.channels

    private val formState = MutableStateFlow(SalesChannelFormState())
    val form: StateFlow<SalesChannelFormState> = formState.asStateFlow()

    fun setName(text: String) = formState.update { it.copy(nameText = text, errorMessage = null) }
    fun setFeeRate(text: String) = formState.update { it.copy(feeRatePercentText = text, errorMessage = null) }

    fun startEditing(channel: SalesChannel) {
        formState.value = SalesChannelFormState(
            nameText = channel.name,
            feeRatePercentText = (channel.feeRate * 100).toString(),
            editingId = channel.id,
        )
    }

    fun cancelEditing() {
        formState.value = SalesChannelFormState()
    }

    fun save() {
        val current = formState.value
        val name = current.nameText.trim()
        val feeRate = (parseDecimal(current.feeRatePercentText) ?: 0.0) / 100.0

        val channel = runCatching {
            require(name.isNotBlank()) { "Dê um nome ao canal (ex.: Shopee, Cartão, Pix)." }
            require(feeRate >= 0 && feeRate < 1) { "A taxa precisa ficar entre 0% e 100%." }
            SalesChannel(id = current.editingId ?: name.lowercase().replace(" ", "-"), name = name, feeRate = feeRate)
        }

        formState.value = channel.fold(
            onSuccess = {
                if (current.editingId != null) repository.update(it) else repository.add(it)
                SalesChannelFormState()
            },
            onFailure = { current.copy(errorMessage = it.message) },
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value.editingId == id) formState.value = SalesChannelFormState()
    }
}
