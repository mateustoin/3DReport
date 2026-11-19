package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.data.store.RecordCollection
import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StatusChange
import com.threedreport.core.model.roundToCents
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
     * preço no momento), [photo], [stlFile] (o arquivo do modelo, STL ou 3MF, guardado com a extensão dele),
     * [sourceLink] e [client] opcionais. Nasce
     * [OrderStatus.ORCADO], com o próximo número da sequência e a mudança de status registrada. Um pedido novo nasce
     * sem pagamento, rastreio nem observação, e com o sinal do % padrão de hoje ([SavedQuote.initialDeposit]); é por
     * aqui que duplicar e vender passam, então nada disso é copiado do original.
     *
     * Os anexos vão pro [AttachmentStore] pelo conteúdo: reabrir, duplicar ou vender e salvar com a
     * mesma foto reaproveita o mesmo arquivo, e trocar a foto nunca mexe na de outro orçamento.
     *
     * [kind] = [QuoteKind.PRODUCT] salva um produto do catálogo (decisão 101): [client],
     * [shippingCost] e [deliveryDateEpochDay] são descartados mesmo se vierem preenchidos, porque
     * produto não tem venda. [sourceProductId] é o produto de onde um pedido nasceu pelo "Vender".
     * [category] é só de produto (decisão 102): num pedido, é descartada. As configurações de cada impressão
     * vêm no próprio [quote]; [printThumbnails] traz a miniatura de cada uma, na ordem de `quote.prints`,
     * guardada como anexo (decisão 114).
     *
     * Nome em branco ganha o nome automático e [SavedQuote.autoNamed]. [autoNamed] diz que [name] é um nome
     * automático trazido de outro registro sem ninguém mexer (duplicar, vender, guardar no catálogo): ele continua
     * automático. Um nome digitado nunca é, mesmo que pareça.
     */
    fun save(
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        stlFile: PickedFile? = null,
        sourceLink: String?,
        client: Client? = null,
        printThumbnails: List<PickedFile?> = emptyList(),
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        kind: QuoteKind = QuoteKind.ORDER,
        sourceProductId: String? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
        currency: Currency = Currency.BRL,
        shippingAbsorbed: Boolean = false,
        autoNamed: Boolean = false,
    ): SavedQuote

    /**
     * Reabre e salva de novo o orçamento [id] (edição explícita) — troca o retrato por um novo,
     * preservando [SavedQuote.savedAtEpochMillis], o número, o andamento e a moeda, e marcando
     * [SavedQuote.lastEditedEpochMillis]. Também ficam os pagamentos, o rastreio, as observações e o último "Retomar
     * conversa"; o sinal nunca é criado nem muda por aqui, só fica limitado ao total novo se ele cair abaixo do sinal
     * ([SavedQuote.withDepositLimitedToTotal]). `null` de [photo]/[stlFile] remove o anexo. Retorna `null`
     * sem fazer nada se [id] não existir. O [SavedQuote.kind] não muda por aqui (produto continua
     * produto); mudar de tipo é pelo [convertToOrder]. Nome em branco ganha um nome automático novo; [autoNamed] diz
     * que [name] é o automático que já estava no campo, sem ninguém mexer, e aí ele continua automático.
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
        printThumbnails: List<PickedFile?> = emptyList(),
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
        shippingAbsorbed: Boolean = false,
        autoNamed: Boolean = false,
    ): SavedQuote?

    /**
     * Troca só os dados que não mexem no preço ("Editar detalhes", decisão 108): nome, cliente, link,
     * foto, prazo e categoria. O cálculo congelado fica como está. Nome em branco mantém o atual (e, se era
     * automático, continua automático); um nome diferente do atual deixa de ser automático.
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

    /**
     * Registra um pagamento no fim da lista do pedido [id] (decisão 146) e devolve o [Payment] gravado. `null`, sem
     * gravar nada, se [id] não existe, é produto do catálogo ou [amount] em centavos ([roundToCents]) não é válido
     * ([Payment.isValidAmount]): "0,004" arredonda a zero e não é pagamento. O valor gravado é o arredondado. O
     * pagamento ganha o número fixo do recibo ([SavedQuote.nextPaymentNumber]). Não conta como edição do pedido
     * ([SavedQuote.lastEditedEpochMillis] fica como está).
     */
    fun addPayment(id: String, amount: Double, method: PaymentMethod, epochDay: Long): Payment?

    /** Tira o pagamento [paymentId] do pedido [id] e devolve o que saiu, ou `null` se não havia. */
    fun deletePayment(id: String, paymentId: String): Payment?

    /** Troca o sinal pedido do pedido [id], arredondado a centavos e entre zero e o total ([SavedQuote.withDeposit]). */
    fun updateDeposit(id: String, depositAmount: Double)

    /**
     * Grava agora como o último contato do "Retomar conversa" do pedido [id] (decisão 149): o destaque de parado some
     * até passar o prazo de novo ([SavedQuote.staleDays]).
     */
    fun recordFollowUp(id: String)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?

    /** Miniatura da foto de [savedQuote] (lado maior de [maxSizePx]), ou `null`. */
    fun photoThumbnail(savedQuote: SavedQuote, maxSizePx: Int): ByteArray?

    /**
     * Bytes do arquivo do modelo de [savedQuote] (STL ou 3MF, ver [SavedQuote.modelFileKind]), ou `null` se não
     * houver arquivo salvo.
     */
    fun modelBytes(savedQuote: SavedQuote): ByteArray?

    /** Bytes de um anexo pela chave (a miniatura de uma impressão, por exemplo), ou `null`. */
    fun attachmentBytes(key: String): ByteArray?

    /** Chaves de anexos em uso por algum orçamento (inclusive os na lixeira, que podem voltar). */
    fun referencedAttachments(): Set<String>

    /** Os orçamentos vivos e os na lixeira, que podem voltar (pra contar quem usa um cadastro, decisão 115). */
    fun quotesIncludingTrash(): List<SavedQuote>
}

/**
 * Os dados de um orçamento que não mudam o preço (ver [QuoteHistoryRepository.updateDetails]). [trackingCode] e
 * [notes] são só de pedido (decisões 150 e 151): num produto do catálogo são descartados. Em branco vira `null`.
 */
data class QuoteDetails(
    val name: String,
    val client: Client?,
    val sourceLink: String?,
    val photo: PickedFile?,
    val deliveryDateEpochDay: Long?,
    val category: String?,
    val trackingCode: String?,
    val notes: String?,
)

@OptIn(ExperimentalUuidApi::class)
class StoredQuoteHistoryRepository(
    private val collection: RecordCollection<SavedQuote>,
    private val lastNumber: DocumentValue<Int>,
    private val attachments: AttachmentStore,
    private val clock: Clock,
    /** Nome automático ("Orçamento - 24/09/2026 14:30") no momento dado, pra quem salva com o nome em branco. */
    private val defaultName: (epochMillis: Long) -> String,
    /** Sinal padrão de hoje, em fração (decisão 146), lido a cada pedido novo. */
    private val defaultDepositRate: () -> Double = { 0.0 },
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
        printThumbnails: List<PickedFile?>,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
        kind: QuoteKind,
        sourceProductId: String?,
        category: String?,
        soldAtCatalogPrice: Boolean,
        currency: Currency,
        shippingAbsorbed: Boolean,
        autoNamed: Boolean,
    ): SavedQuote {
        val isProduct = kind == QuoteKind.PRODUCT
        val now = clock.nowMillis()
        val typedName = name.trim()
        // O modelo valida no `init`; montar antes de guardar os anexos evita arquivo órfão quando algo
        // não passa (um frete negativo, por exemplo).
        val draft = SavedQuote(
            id = Uuid.random().toString(),
            name = typedName.ifEmpty { defaultName(now) },
            autoNamed = typedName.isEmpty() || autoNamed,
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
            shippingAbsorbed = !isProduct && shippingAbsorbed,
        ).let { if (isProduct) it else it.copy(depositAmount = SavedQuote.initialDeposit(it.totalWithServices, defaultDepositRate())) }
        val saved = draft.copy(
            quote = draft.quote.withThumbnails(printThumbnails),
            photoFileName = photo?.let(::store),
            modelFileName = stlFile?.let(::store),
        )
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
        printThumbnails: List<PickedFile?>,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
        category: String?,
        soldAtCatalogPrice: Boolean,
        shippingAbsorbed: Boolean,
        autoNamed: Boolean,
    ): SavedQuote? {
        val existing = collection.find(id) ?: return null
        val isProduct = !existing.isOrder
        val now = clock.nowMillis()
        val typedName = name.trim()
        val draft = existing.copy(
            name = typedName.ifEmpty { defaultName(now) },
            autoNamed = typedName.isEmpty() || autoNamed,
            quote = quote,
            services = services,
            sourceLink = sourceLink.clean(),
            client = if (isProduct) null else client,
            lastEditedEpochMillis = now,
            shippingCost = if (isProduct) 0.0 else shippingCost,
            deliveryDateEpochDay = if (isProduct) null else deliveryDateEpochDay,
            category = if (isProduct) category.normalizedCategory(excludingId = id) else null,
            soldAtCatalogPrice = !isProduct && soldAtCatalogPrice,
            shippingAbsorbed = !isProduct && shippingAbsorbed,
            // Pagamentos, rastreio, observações e último contato vêm do `existing` como estão.
        ).withDepositLimitedToTotal()
        return collection.update(id) {
            draft.copy(
                quote = draft.quote.withThumbnails(printThumbnails),
                photoFileName = photo?.let(::store),
                modelFileName = stlFile?.let(::store),
            )
        }
    }

    override fun updateDetails(id: String, details: QuoteDetails): SavedQuote? = collection.update(id) { existing ->
        val isProduct = !existing.isOrder
        val name = details.name.trim().ifEmpty { existing.name }
        existing.copy(
            name = name,
            autoNamed = existing.autoNamed && name == existing.name,
            client = if (isProduct) null else details.client,
            sourceLink = details.sourceLink.clean(),
            photoFileName = details.photo?.let(::store),
            deliveryDateEpochDay = if (isProduct) null else details.deliveryDateEpochDay,
            category = if (isProduct) details.category.normalizedCategory(excludingId = id) else null,
            trackingCode = if (isProduct) null else details.trackingCode.clean(),
            notes = if (isProduct) null else details.notes.clean(),
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
                ).let { it.copy(depositAmount = SavedQuote.initialDeposit(it.totalWithServices, defaultDepositRate())) }
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

    override fun addPayment(id: String, amount: Double, method: PaymentMethod, epochDay: Long): Payment? {
        val existing = collection.find(id)
        // Valida o valor já em centavos, que é o que fica gravado: "0,004" arredonda a zero e não é pagamento.
        val cents = roundToCents(amount)
        if (!Payment.isValidAmount(cents) || existing?.isOrder != true) return null
        val payment = Payment(
            id = Uuid.random().toString(),
            amount = cents,
            method = method,
            epochDay = epochDay,
            number = existing.nextPaymentNumber,
        )
        collection.update(id) { it.copy(payments = it.payments + payment, lastPaymentNumber = maxOf(it.lastPaymentNumber, payment.number)) }
        return payment
    }

    override fun deletePayment(id: String, paymentId: String): Payment? {
        val found = collection.find(id) ?: return null
        val payment = found.payments.firstOrNull { it.id == paymentId } ?: return null
        // O número do recibo que sai não volta (decisão 147): o maior já usado fica guardado, nunca diminui.
        val issued = found.paymentNumber(paymentId) ?: 0
        collection.update(id) { existing ->
            existing.copy(payments = existing.payments.filterNot { it.id == paymentId }, lastPaymentNumber = maxOf(existing.lastPaymentNumber, issued))
        }
        return payment
    }

    override fun updateDeposit(id: String, depositAmount: Double) {
        collection.update(id) { if (it.isOrder) it.withDeposit(depositAmount) else it }
    }

    override fun recordFollowUp(id: String) {
        val now = clock.nowMillis()
        collection.update(id) { if (it.isOrder) it.copy(lastFollowUpEpochMillis = now) else it }
    }

    override fun photoBytes(savedQuote: SavedQuote): ByteArray? = savedQuote.photoFileName?.let(attachments::read)

    override fun photoThumbnail(savedQuote: SavedQuote, maxSizePx: Int): ByteArray? =
        savedQuote.photoFileName?.let { attachments.thumbnail(it, maxSizePx) }

    override fun modelBytes(savedQuote: SavedQuote): ByteArray? = savedQuote.modelFileName?.let(attachments::read)

    override fun attachmentBytes(key: String): ByteArray? = attachments.read(key)

    override fun referencedAttachments(): Set<String> = collection.allRecords
        .mapNotNull { it.data }
        .flatMapTo(HashSet()) { saved ->
            listOfNotNull(saved.photoFileName, saved.modelFileName) + saved.quote.prints.mapNotNull { it.job.thumbnailFileName }
        }

    override fun quotesIncludingTrash(): List<SavedQuote> = collection.allRecords.mapNotNull { it.data }

    private fun store(file: PickedFile): String = attachments.put(file.bytes, file.fileName)

    /** A miniatura de cada impressão, na ordem de `prints`; impressão sem miniatura fica sem. */
    private fun Quote.withThumbnails(thumbnails: List<PickedFile?>): Quote = copy(
        prints = prints.mapIndexed { index, print ->
            print.copy(job = print.job.copy(thumbnailFileName = thumbnails.getOrNull(index)?.let(::store)))
        },
    )

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
