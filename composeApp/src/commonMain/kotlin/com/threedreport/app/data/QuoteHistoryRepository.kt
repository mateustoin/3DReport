package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.data.store.RecordCollection
import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StatusChange
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Histórico de orçamentos salvos. Cada [SavedQuote] é um retrato congelado do [Quote] (e dos serviços
 * escolhidos) no momento em que foi salvo (ver KDoc de [SavedQuote]).
 */
interface QuoteHistoryRepository {
    val savedQuotes: StateFlow<List<SavedQuote>>

    /**
     * Salva [quote] com [name] (gera um nome genérico se vazio), os [services] escolhidos (retrato do
     * preço no momento), [photo], [stlFile], [sourceLink] e [client] opcionais. Nasce
     * [OrderStatus.ORCADO], com o próximo número da sequência e a mudança de status registrada.
     *
     * Os anexos vão pro [AttachmentStore] pelo conteúdo: reabrir, duplicar ou vender e salvar com a
     * mesma foto reaproveita o mesmo arquivo, e trocar a foto nunca mexe na de outro orçamento.
     *
     * [kind] = [QuoteKind.PRODUCT] salva um produto do catálogo (decisão 101): [client],
     * [shippingCost] e [deliveryDateEpochDay] são descartados mesmo se vierem preenchidos, porque
     * produto não tem venda. [sourceProductId] é o produto de onde um pedido nasceu pelo "Vender".
     * [category] é só de produto (decisão 102): num pedido, é descartada. [printSettings] vai pra
     * primeira impressão ([SavedQuote.withPrintSettings]).
     */
    fun save(
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        stlFile: PickedFile? = null,
        sourceLink: String?,
        client: Client? = null,
        printSettings: PrintSettings? = null,
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        kind: QuoteKind = QuoteKind.ORDER,
        sourceProductId: String? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
        currency: Currency = Currency.BRL,
    ): SavedQuote

    /**
     * Reabre e salva de novo o orçamento [id] (edição explícita) — troca o retrato por um novo,
     * preservando [SavedQuote.savedAtEpochMillis], o número, o andamento e a moeda, e marcando
     * [SavedQuote.lastEditedEpochMillis]. `null` de [photo]/[stlFile] remove o anexo. Retorna `null`
     * sem fazer nada se [id] não existir. O [SavedQuote.kind] não muda por aqui (produto continua
     * produto); mudar de tipo é pelo [convertToOrder].
     */
    fun update(
        id: String,
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        stlFile: PickedFile?,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings? = null,
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
    ): SavedQuote?

    /**
     * Troca só os dados que não mexem no preço ("Editar detalhes", decisão 108): nome, cliente, link,
     * foto, prazo e categoria. O cálculo congelado fica como está.
     */
    fun updateDetails(id: String, details: QuoteDetails): SavedQuote?

    /** Manda pra lixeira; [restore] desfaz. Os anexos só saem quando ninguém mais aponta pra eles. */
    fun delete(id: String)

    /** Desfaz um [delete] ainda na lixeira. */
    fun restore(id: String): SavedQuote?

    /**
     * "Transformar em pedido" (decisão 101): o produto [id] passa a ser pedido, nascendo
     * [OrderStatus.ORCADO] com a data da conversão. Sem categoria (é de produto), e o preço anunciado,
     * se houver, fica marcado como preço do catálogo (não conta como negociação). Não faz nada se [id]
     * não existir ou já for pedido.
     */
    fun convertToOrder(id: String)

    /**
     * "Atualizar preço" de um produto do catálogo (decisão 102): troca só o [quote] recalculado com
     * os cadastros de hoje e marca [SavedQuote.lastEditedEpochMillis], que passa a ser a data do
     * preço. Não faz nada se [id] não existir ou for pedido, que é retrato congelado da venda.
     */
    fun updateQuote(id: String, quote: Quote)

    /** Atualiza o andamento do pedido [id] e registra a mudança com o momento dela. */
    fun updateStatus(id: String, status: OrderStatus)

    /** Troca só as configurações de impressão do pedido [id] (`null` remove). */
    fun updatePrintSettings(id: String, printSettings: PrintSettings?)

    /** Troca só o prazo de entrega do pedido [id] (`null` remove). */
    fun updateDeliveryDate(id: String, deliveryDateEpochDay: Long?)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?

    /** Miniatura da foto de [savedQuote] (lado maior de [maxSizePx]), ou `null`. */
    fun photoThumbnail(savedQuote: SavedQuote, maxSizePx: Int): ByteArray?

    /** Bytes do arquivo STL de [savedQuote], ou `null` se não houver STL salvo. */
    fun stlBytes(savedQuote: SavedQuote): ByteArray?

    /** Chaves de anexos em uso por algum orçamento (inclusive os na lixeira, que podem voltar). */
    fun referencedAttachments(): Set<String>
}

/** Os dados de um orçamento que não mudam o preço (ver [QuoteHistoryRepository.updateDetails]). */
data class QuoteDetails(
    val name: String,
    val client: Client?,
    val sourceLink: String?,
    val photo: PickedFile?,
    val deliveryDateEpochDay: Long?,
    val category: String?,
)

@OptIn(ExperimentalUuidApi::class)
class StoredQuoteHistoryRepository(
    private val collection: RecordCollection<SavedQuote>,
    private val lastNumber: DocumentValue<Int>,
    private val attachments: AttachmentStore,
    private val clock: Clock,
    /** Nome automático ("Orçamento - 24/09/2026 14:30") pra quem salva com o nome em branco. */
    private val defaultName: () -> String,
) : QuoteHistoryRepository {

    override val savedQuotes: StateFlow<List<SavedQuote>> = collection.items

    override fun save(
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        stlFile: PickedFile?,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings?,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
        kind: QuoteKind,
        sourceProductId: String?,
        category: String?,
        soldAtCatalogPrice: Boolean,
        currency: Currency,
    ): SavedQuote {
        val isProduct = kind == QuoteKind.PRODUCT
        val now = clock.nowMillis()
        // O modelo valida no `init`; montar antes de guardar os anexos evita arquivo órfão quando algo
        // não passa (um frete negativo, por exemplo).
        val draft = SavedQuote(
            id = Uuid.random().toString(),
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            services = services,
            sourceLink = sourceLink.clean(),
            savedAtEpochMillis = now,
            client = if (isProduct) null else client,
            shippingCost = if (isProduct) 0.0 else shippingCost,
            deliveryDateEpochDay = if (isProduct) null else deliveryDateEpochDay,
            kind = kind,
            sourceProductId = sourceProductId,
            category = if (isProduct) category.normalizedCategory() else null,
            soldAtCatalogPrice = !isProduct && soldAtCatalogPrice,
            number = lastNumber.value.value + 1,
            statusHistory = listOf(StatusChange(OrderStatus.ORCADO, now)),
            currency = currency,
        ).withPrintSettings(printSettings)
        val saved = draft.copy(photoFileName = photo?.let(::store), stlFileName = stlFile?.let(::store))
        collection.add(saved)
        lastNumber.set(saved.number)
        return saved
    }

    override fun update(
        id: String,
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        stlFile: PickedFile?,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings?,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
        category: String?,
        soldAtCatalogPrice: Boolean,
    ): SavedQuote? {
        val existing = collection.find(id) ?: return null
        val isProduct = !existing.isOrder
        val draft = existing.copy(
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            services = services,
            sourceLink = sourceLink.clean(),
            client = if (isProduct) null else client,
            lastEditedEpochMillis = clock.nowMillis(),
            shippingCost = if (isProduct) 0.0 else shippingCost,
            deliveryDateEpochDay = if (isProduct) null else deliveryDateEpochDay,
            category = if (isProduct) category.normalizedCategory(excludingId = id) else null,
            soldAtCatalogPrice = !isProduct && soldAtCatalogPrice,
        ).withPrintSettings(printSettings)
        return collection.update(id) { draft.copy(photoFileName = photo?.let(::store), stlFileName = stlFile?.let(::store)) }
    }

    override fun updateDetails(id: String, details: QuoteDetails): SavedQuote? = collection.update(id) { existing ->
        val isProduct = !existing.isOrder
        existing.copy(
            name = details.name.trim().ifEmpty { existing.name },
            client = if (isProduct) null else details.client,
            sourceLink = details.sourceLink.clean(),
            photoFileName = details.photo?.let(::store),
            deliveryDateEpochDay = if (isProduct) null else details.deliveryDateEpochDay,
            category = if (isProduct) details.category.normalizedCategory(excludingId = id) else null,
            lastEditedEpochMillis = clock.nowMillis(),
        )
    }

    override fun delete(id: String) {
        collection.delete(id)
    }

    override fun restore(id: String): SavedQuote? = collection.restore(id)

    override fun convertToOrder(id: String) {
        val now = clock.nowMillis()
        collection.update(id) { existing ->
            if (existing.isOrder) {
                existing
            } else {
                // A data passa a ser a da conversão: é um pedido novo do ponto de vista da venda, e com a
                // data antiga do produto ele sumiria dos filtros de período e iria pro fim da lista.
                existing.copy(
                    kind = QuoteKind.ORDER,
                    status = OrderStatus.ORCADO,
                    statusHistory = listOf(StatusChange(OrderStatus.ORCADO, now)),
                    savedAtEpochMillis = now,
                    lastEditedEpochMillis = null,
                    category = null,
                    // O anunciado do produto vira o preço do pedido sem ser negociação com o cliente.
                    soldAtCatalogPrice = existing.quote.isNegotiated,
                )
            }
        }
    }

    override fun updateQuote(id: String, quote: Quote) {
        if (collection.find(id)?.isOrder != false) return
        collection.update(id) { it.copy(quote = quote, lastEditedEpochMillis = clock.nowMillis()) }
    }

    override fun updateStatus(id: String, status: OrderStatus) {
        collection.update(id) { it.withStatus(status, clock.nowMillis()) }
    }

    override fun updatePrintSettings(id: String, printSettings: PrintSettings?) {
        collection.update(id) { it.withPrintSettings(printSettings) }
    }

    override fun updateDeliveryDate(id: String, deliveryDateEpochDay: Long?) {
        collection.update(id) { if (it.isOrder) it.copy(deliveryDateEpochDay = deliveryDateEpochDay) else it }
    }

    override fun photoBytes(savedQuote: SavedQuote): ByteArray? = savedQuote.photoFileName?.let(attachments::read)

    override fun photoThumbnail(savedQuote: SavedQuote, maxSizePx: Int): ByteArray? =
        savedQuote.photoFileName?.let { attachments.thumbnail(it, maxSizePx) }

    override fun stlBytes(savedQuote: SavedQuote): ByteArray? = savedQuote.stlFileName?.let(attachments::read)

    override fun referencedAttachments(): Set<String> = collection.allRecords
        .mapNotNull { it.data }
        .flatMapTo(HashSet()) { saved ->
            listOfNotNull(saved.photoFileName, saved.stlFileName) + saved.quote.prints.mapNotNull { it.job.thumbnailFileName }
        }

    private fun store(file: PickedFile): String = attachments.put(file.bytes, file.fileName)

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

    /**
     * Categoria sem espaços nas pontas, vazia vira `null`, e com a grafia de uma que já existe
     * quando só muda maiúscula ("chaveiros" cai em "Chaveiros"), pra lista e PDF não ganharem duas
     * seções da mesma coisa. [excludingId] é o produto sendo editado: sem isso, a grafia antiga dele
     * mesmo venceria, e corrigir a maiúscula de uma categoria seria impossível.
     */
    private fun String?.normalizedCategory(excludingId: String? = null): String? {
        val trimmed = this?.trim()?.ifEmpty { null } ?: return null
        return savedQuotes.value
            .filter { it.id != excludingId }
            .firstNotNullOfOrNull { saved -> saved.category?.takeIf { it.equals(trimmed, ignoreCase = true) } } ?: trimmed
    }
}
