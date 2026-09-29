package com.threedreport.app.ui.settings

import com.threedreport.core.model.SavedQuote
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Uma linha de faixa de preço no rascunho (decisão 121): a última da lista é sempre a que não tem
 * limite ("Acima disso"), então a posição na lista é que decide, não um campo à parte.
 */
data class ChannelTierFormRow(
    val id: String,
    val upToUnitPriceText: String = "",
    val feeRatePercentText: String = "",
    val fixedFeePerItemText: String = "",
)

/**
 * Formulário de cadastro de canal de venda. `editingId` não-nulo significa que o formulário está
 * editando um canal existente em vez de criar um novo.
 */
data class SalesChannelFormState(
    val nameText: String = "",
    val feeRatePercentText: String = "",
    val fixedFeePerItemText: String = "",
    val tiers: List<ChannelTierFormRow> = emptyList(),
    val tiersExpanded: Boolean = false,
    val editingId: String? = null,
    val errorMessage: String? = null,
)

/** Ver [SalesChannel] pro porquê de canal e forma de pagamento serem o mesmo campo. */
class SalesChannelViewModel(
    private val repository: SalesChannelRepository,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada canal antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
) {

    val channels: StateFlow<List<SalesChannel>> = repository.channels

    private val formState = MutableStateFlow(SalesChannelFormState())
    val form: StateFlow<SalesChannelFormState> = formState.asStateFlow()

    fun setName(text: String) = formState.update { it.copy(nameText = text, errorMessage = null) }
    fun setFeeRate(text: String) = formState.update { it.copy(feeRatePercentText = text, errorMessage = null) }
    fun setFixedFeePerItem(text: String) = formState.update { it.copy(fixedFeePerItemText = text, errorMessage = null) }
    fun setTiersExpanded(expanded: Boolean) = formState.update { it.copy(tiersExpanded = expanded) }

    /** A primeira faixa cria duas linhas: uma com limite pra preencher e a última, sem limite ("Acima disso"). */
    @OptIn(ExperimentalUuidApi::class)
    fun addTierRow() {
        formState.update { form ->
            val tiers = if (form.tiers.isEmpty()) {
                listOf(ChannelTierFormRow(id = newTierId()), ChannelTierFormRow(id = newTierId()))
            } else {
                form.tiers.toMutableList().apply { add(lastIndex, ChannelTierFormRow(id = newTierId())) }
            }
            form.copy(tiers = tiers, tiersExpanded = true, errorMessage = null)
        }
    }

    /** Com só uma faixa restando, ela sozinha não faz sentido: volta pra taxa única (sem faixas). */
    fun removeTierRow(id: String) {
        formState.update { form ->
            val remaining = form.tiers.filterNot { it.id == id }
            form.copy(tiers = if (remaining.size <= 1) emptyList() else remaining, errorMessage = null)
        }
    }

    fun updateTierRow(id: String, transform: (ChannelTierFormRow) -> ChannelTierFormRow) {
        formState.update { form -> form.copy(tiers = form.tiers.map { if (it.id == id) transform(it) else it }, errorMessage = null) }
    }

    fun startEditing(channel: SalesChannel) {
        formState.value = SalesChannelFormState(
            nameText = channel.name,
            feeRatePercentText = (channel.feeRate * 100).toInputText(),
            // Sem valor fixo, o campo fica vazio (vazio vale zero), em vez de mostrar um "0" pra apagar.
            fixedFeePerItemText = channel.fixedFeePerItem.takeIf { it > 0 }?.toInputText().orEmpty(),
            tiers = channel.tiers.map {
                ChannelTierFormRow(
                    id = newTierId(),
                    upToUnitPriceText = it.upToUnitPrice?.toInputText().orEmpty(),
                    feeRatePercentText = (it.feeRate * 100).toInputText(),
                    fixedFeePerItemText = it.fixedFeePerItem.takeIf { fee -> fee > 0 }?.toInputText().orEmpty(),
                )
            },
            tiersExpanded = channel.tiers.isNotEmpty(),
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
            val feeRate = current.feeRatePercentText.blankAsZeroPercent("A taxa")
            require(feeRate >= 0 && feeRate < 1) { "A taxa precisa ficar entre 0% e 100%." }
            val fixedFeePerItem = current.fixedFeePerItemText.blankAsZeroAmount("O valor fixo por item")
            require(fixedFeePerItem >= 0) { "O valor fixo por item não pode ser negativo." }
            val tiers = current.tiers.toChannelFeeTiers()
            // Id aleatório, como o de todo cadastro: gerado do nome, renomear um canal e criar outro com o
            // nome antigo fazia os dois terem o mesmo id.
            SalesChannel(
                id = current.editingId ?: Uuid.random().toString(),
                name = name,
                feeRate = feeRate,
                fixedFeePerItem = fixedFeePerItem,
                tiers = tiers,
            )
        }

        formState.value = channel.fold(
            onSuccess = {
                if (current.editingId != null) repository.updateKeepingArchived(it) else repository.add(it)
                SalesChannelFormState()
            },
            onFailure = { current.copy(errorMessage = it.message) },
        )
    }

    /** Quantos pedidos e produtos usam o canal [id]. */
    fun usageCount(id: String): Int = CatalogUsage.channel(id, usageQuotes())

    /** Arquiva (ou restaura) o canal [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) = repository.setArchived(id, archived)

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value.editingId == id) formState.value = SalesChannelFormState()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newTierId(): String = Uuid.random().toString()
}

/** Percentual digitado, em branco vale 0 (Pix, dinheiro); texto que não é número é erro, não zero. */
private fun String.blankAsZeroPercent(label: String): Double =
    if (isBlank()) 0.0 else (parseDecimal(this, NumberKind.MEASURE) ?: error("$label não é um número. Ex.: 20 ou 4,99.")) / 100.0

/** Valor em R$ digitado, em branco vale 0. */
private fun String.blankAsZeroAmount(label: String): Double =
    if (isBlank()) 0.0 else parseDecimal(this, NumberKind.AMOUNT) ?: error("$label não é um número.")

/**
 * As linhas do rascunho como [ChannelFeeTier] (decisão 121): limite obrigatório e crescente em toda
 * faixa menos a última, que fica sem limite ("Acima disso").
 */
private fun List<ChannelTierFormRow>.toChannelFeeTiers(): List<ChannelFeeTier> {
    if (isEmpty()) return emptyList()
    var previousLimit: Double? = null
    return mapIndexed { index, row ->
        val isLast = index == lastIndex
        val limit = if (isLast) {
            null
        } else {
            val value = parseDecimal(row.upToUnitPriceText, NumberKind.AMOUNT) ?: error("Preencha o limite de todas as faixas, menos a última.")
            require(value > 0) { "O limite da faixa precisa ser maior que zero." }
            require(previousLimit == null || value > previousLimit) {
                "Os limites das faixas precisam subir de uma faixa pra outra."
            }
            previousLimit = value
            value
        }
        val feeRate = row.feeRatePercentText.blankAsZeroPercent("A taxa da faixa")
        require(feeRate >= 0 && feeRate < 1) { "A taxa da faixa precisa ficar entre 0% e 100%." }
        val fixedFeePerItem = row.fixedFeePerItemText.blankAsZeroAmount("O valor fixo da faixa")
        require(fixedFeePerItem >= 0) { "O valor fixo da faixa não pode ser negativo." }
        ChannelFeeTier(upToUnitPrice = limit, feeRate = feeRate, fixedFeePerItem = fixedFeePerItem)
    }
}
