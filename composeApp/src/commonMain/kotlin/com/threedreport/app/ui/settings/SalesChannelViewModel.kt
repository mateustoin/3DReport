package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_channels_error_fee_range
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_fee_number
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_fee_range
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_fixed_negative
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_fixed_number
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_limit_missing
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_limit_order
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_limit_zero
import _3dreport.composeapp.generated.resources.settings_channels_notice_archived
import _3dreport.composeapp.generated.resources.settings_channels_notice_deleted
import _3dreport.composeapp.generated.resources.settings_channels_notice_restored
import _3dreport.composeapp.generated.resources.settings_channels_notice_saved
import _3dreport.composeapp.generated.resources.settings_channels_tier_promoted
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.ui.components.PendingDelete
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.Currency
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
 * Formulário de cadastro de canal de venda. [editingId] não-nulo significa que o formulário está editando um canal
 * existente em vez de criar um novo. Cada campo tem o seu erro; [tiersError] é o das faixas.
 */
data class SalesChannelFormState(
    val nameText: String = "",
    val feeRatePercentText: String = "",
    val fixedFeePerItemText: String = "",
    val tiers: List<ChannelTierFormRow> = emptyList(),
    val tiersExpanded: Boolean = false,
    /** Aviso de uma mudança nas faixas que a pessoa pode não ter percebido (ver [SalesChannelViewModel.removeTierRow]). */
    val tierNotice: UiText? = null,
    val editingId: String? = null,
    val nameError: UiText? = null,
    val feeError: UiText? = null,
    val fixedFeeError: UiText? = null,
    val tiersError: UiText? = null,
)

/**
 * Tudo o que a seção Canais de venda mostra: a lista (ativos e arquivados), o formulário aberto (nulo quando nenhum) e a
 * pergunta de exclusão pendente (nula quando nenhuma).
 */
data class SalesChannelsUiState(
    val active: List<SalesChannel> = emptyList(),
    val archived: List<SalesChannel> = emptyList(),
    val form: SalesChannelFormState? = null,
    val pendingDelete: PendingDelete? = null,
) {
    /** Nenhum canal cadastrado, nem arquivado. */
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

/**
 * ViewModel dos canais de venda. Um [uiState] só e os [notices]. Ver [SalesChannel] pro porquê de canal e forma de
 * pagamento serem o mesmo campo. A lista acompanha o repositório dentro de [scope].
 */
class SalesChannelViewModel(
    private val repository: SalesChannelRepository,
    scope: CoroutineScope,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada canal antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
    /** A moeda em uso: os números são lidos com o separador decimal dela, o mesmo que o campo usa pra validar. */
    private val currency: () -> Currency = { Currency.BRL },
) {

    private val state = MutableStateFlow(SalesChannelsUiState().withItems(repository.channels.value))
    val uiState: StateFlow<SalesChannelsUiState> = state.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    init {
        repository.channels.onEach { items -> state.update { it.withItems(items) } }.launchIn(scope)
    }

    fun startAdd() {
        state.update { it.copy(form = SalesChannelFormState()) }
    }

    fun startEdit(channel: SalesChannel) {
        state.update {
            it.copy(
                form = SalesChannelFormState(
                    nameText = channel.name,
                    feeRatePercentText = (channel.feeRate * 100).toInputText(),
                    // Sem valor fixo, o campo fica vazio (vazio vale zero), em vez de mostrar um "0" pra apagar.
                    fixedFeePerItemText = channel.fixedFeePerItem.takeIf { fee -> fee > 0 }?.toInputText().orEmpty(),
                    tiers = channel.tiers.map { tier ->
                        ChannelTierFormRow(
                            id = newTierId(),
                            upToUnitPriceText = tier.upToUnitPrice?.toInputText().orEmpty(),
                            feeRatePercentText = (tier.feeRate * 100).toInputText(),
                            fixedFeePerItemText = tier.fixedFeePerItem.takeIf { fee -> fee > 0 }?.toInputText().orEmpty(),
                        )
                    },
                    tiersExpanded = channel.tiers.isNotEmpty(),
                    editingId = channel.id,
                ),
            )
        }
    }

    fun cancelEdit() {
        state.update { it.copy(form = null) }
    }

    /** Muda o formulário aberto e apaga os erros, que já não valem pro texto novo. */
    private fun updateForm(transform: (SalesChannelFormState) -> SalesChannelFormState) {
        state.update { s ->
            s.copy(form = s.form?.let { transform(it).copy(nameError = null, feeError = null, fixedFeeError = null, tiersError = null) })
        }
    }

    fun setName(text: String) = updateForm { it.copy(nameText = text) }
    fun setFeeRate(text: String) = updateForm { it.copy(feeRatePercentText = text) }
    fun setFixedFeePerItem(text: String) = updateForm { it.copy(fixedFeePerItemText = text) }
    fun setTiersExpanded(expanded: Boolean) = state.update { s -> s.copy(form = s.form?.copy(tiersExpanded = expanded)) }

    /** A primeira faixa cria duas linhas: uma com limite pra preencher e a última, sem limite ("Acima disso"). */
    fun addTierRow() {
        updateForm { form ->
            val tiers = if (form.tiers.isEmpty()) {
                listOf(ChannelTierFormRow(id = newTierId()), ChannelTierFormRow(id = newTierId()))
            } else {
                form.tiers.toMutableList().apply { add(lastIndex, ChannelTierFormRow(id = newTierId())) }
            }
            form.copy(tiers = tiers, tiersExpanded = true, tierNotice = null)
        }
    }

    /**
     * Com só uma faixa restando, ela sozinha não faz sentido: volta pra taxa única (sem faixas). Remover a
     * última ("Acima disso") faz a anterior virar a última, e o limite dela deixa de valer: isso vira um
     * aviso, em vez de sumir sem ninguém ver.
     */
    fun removeTierRow(id: String) {
        updateForm { form ->
            val removedLast = form.tiers.lastOrNull()?.id == id
            val remaining = form.tiers.filterNot { it.id == id }
            if (remaining.size <= 1) return@updateForm form.copy(tiers = emptyList(), tierNotice = null)
            val promoted = remaining.last()
            val notice = if (removedLast && promoted.upToUnitPriceText.isNotBlank()) {
                uiText(Res.string.settings_channels_tier_promoted, promoted.upToUnitPriceText)
            } else {
                null
            }
            val tiers = if (removedLast) remaining.dropLast(1) + promoted.copy(upToUnitPriceText = "") else remaining
            form.copy(tiers = tiers, tierNotice = notice)
        }
    }

    fun updateTierRow(id: String, transform: (ChannelTierFormRow) -> ChannelTierFormRow) {
        updateForm { form -> form.copy(tiers = form.tiers.map { if (it.id == id) transform(it) else it }, tierNotice = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = state.value.form ?: return
        val name = current.nameText.trim()
        val separator = currency().decimalSeparator

        // Em branco é taxa zero (Pix, dinheiro); texto que não é número é erro, e não zero: um "20 por
        // cento" que virasse 0% tiraria a taxa do marketplace do preço sem ninguém ver.
        val feeRate = blankAsZero(current.feeRatePercentText, NumberKind.MEASURE, separator)
        val fixedFee = blankAsZero(current.fixedFeePerItemText, NumberKind.AMOUNT, separator)
        val nameError = uiText(Res.string.common_error_name_required).takeIf { name.isBlank() }
        val feeError = when {
            feeRate == null -> uiText(Res.string.common_error_number_invalid)
            feeRate < 0 || feeRate >= 100 -> uiText(Res.string.settings_channels_error_fee_range)
            else -> null
        }
        val fixedFeeError = when {
            fixedFee == null -> uiText(Res.string.common_error_number_invalid)
            fixedFee < 0 -> uiText(Res.string.common_error_negative)
            else -> null
        }
        val tiers = runCatching { current.tiers.toChannelFeeTiers(separator) }
        val tiersError = (tiers.exceptionOrNull() as? TierError)?.text
        if (nameError != null || feeError != null || fixedFeeError != null || tiersError != null || feeRate == null || fixedFee == null) {
            state.update {
                it.copy(
                    form = current.copy(
                        nameError = nameError,
                        feeError = feeError,
                        fixedFeeError = fixedFeeError,
                        tiersError = tiersError,
                        // O erro das faixas fica numa área que pode estar recolhida: abre pra a mensagem aparecer.
                        tiersExpanded = current.tiersExpanded || tiersError != null,
                    ),
                )
            }
            return
        }

        // Id aleatório, como o de todo cadastro: gerado do nome, renomear um canal e criar outro com o
        // nome antigo fazia os dois terem o mesmo id.
        val channel = SalesChannel(
            id = current.editingId ?: Uuid.random().toString(),
            name = name,
            feeRate = feeRate / 100.0,
            fixedFeePerItem = fixedFee,
            tiers = tiers.getOrThrow(),
        )
        if (current.editingId != null) repository.updateKeepingArchived(channel) else repository.add(channel)
        state.update { it.copy(form = null) }
        noticeChannel.trySend(UiNotice(uiText(Res.string.settings_channels_notice_saved)))
    }

    /** Pergunta se pode excluir o canal [id], com quantos pedidos e produtos o usam (decisão 115). */
    fun requestDelete(id: String) {
        val channel = repository.channels.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = PendingDelete(id, channel.name, usageCount(id), canArchive = !channel.archived)) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    /** Quantos pedidos e produtos usam o canal [id]. */
    fun usageCount(id: String): Int = CatalogUsage.channel(id, usageQuotes())

    /** Arquiva (ou restaura) o canal [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        repository.setArchived(id, archived)
        state.update { it.copy(pendingDelete = null) }
        noticeChannel.trySend(
            UiNotice(uiText(if (archived) Res.string.settings_channels_notice_archived else Res.string.settings_channels_notice_restored)),
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        state.update { s -> s.copy(form = s.form?.takeUnless { it.editingId == id }, pendingDelete = null) }
        noticeChannel.trySend(UiNotice(uiText(Res.string.settings_channels_notice_deleted)))
    }

    private fun SalesChannelsUiState.withItems(items: List<SalesChannel>): SalesChannelsUiState {
        val (archived, active) = items.partition { it.archived }
        return copy(active = active, archived = archived)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newTierId(): String = Uuid.random().toString()
}

/** Um erro nas faixas: o texto que a tela mostra embaixo da tabela. */
private class TierError(val text: UiText) : IllegalStateException()

/** O número digitado, em branco vale 0; `null` quando o texto não é um número. */
private fun blankAsZero(text: String, kind: NumberKind, separator: Char): Double? =
    if (text.isBlank()) 0.0 else parseDecimal(text, kind, separator)

/**
 * As linhas do rascunho como [ChannelFeeTier] (decisão 121): limite obrigatório e crescente em toda
 * faixa menos a última, que fica sem limite ("Acima disso").
 */
private fun List<ChannelTierFormRow>.toChannelFeeTiers(separator: Char): List<ChannelFeeTier> {
    if (isEmpty()) return emptyList()
    var previousLimit: Double? = null
    return mapIndexed { index, row ->
        val isLast = index == lastIndex
        val limit = if (isLast) {
            null
        } else {
            val value = parseDecimal(row.upToUnitPriceText, NumberKind.AMOUNT, separator) ?: throw TierError(uiText(Res.string.settings_channels_error_tier_limit_missing))
            if (value <= 0) throw TierError(uiText(Res.string.settings_channels_error_tier_limit_zero))
            if (previousLimit != null && value <= previousLimit) throw TierError(uiText(Res.string.settings_channels_error_tier_limit_order))
            previousLimit = value
            value
        }
        val feeRate = blankAsZero(row.feeRatePercentText, NumberKind.MEASURE, separator) ?: throw TierError(uiText(Res.string.settings_channels_error_tier_fee_number))
        if (feeRate < 0 || feeRate >= 100) throw TierError(uiText(Res.string.settings_channels_error_tier_fee_range))
        val fixedFee = blankAsZero(row.fixedFeePerItemText, NumberKind.AMOUNT, separator) ?: throw TierError(uiText(Res.string.settings_channels_error_tier_fixed_number))
        if (fixedFee < 0) throw TierError(uiText(Res.string.settings_channels_error_tier_fixed_negative))
        ChannelFeeTier(upToUnitPrice = limit, feeRate = feeRate / 100.0, fixedFeePerItem = fixedFee)
    }
}
