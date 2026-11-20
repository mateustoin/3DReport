package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItemRef

/**
 * O histórico [this] com a baixa do estoque ligada (item 4 da Leva 12): depois de cada mudança, compara a lista de
 * pedidos com a última vista e chama [StockRepository.reconcileOrders]. A lista de quando o histórico é montado é só a
 * base: abrir o app não cria nem tira movimento. [filaments] dá o cadastro de agora (pra cor `null` de filamento de uma
 * cor só).
 */
fun QuoteHistoryRepository.reconcilingStock(stock: StockRepository, filaments: () -> List<Filament>): QuoteHistoryRepository =
    StockReconcilingQuoteHistoryRepository(this, stock, filaments)

/**
 * O cadastro de filamentos [this] apagando o controle de saldo da cor que sai do filamento (item 9): a cor não tem
 * lixeira. Excluir o filamento inteiro não apaga o controle, porque ele vai pra lixeira e pode ser restaurado; enquanto
 * está lá, o `core` já o deixa de fora do estoque, dos alertas e da lista de compras. Insumo excluído segue a mesma
 * regra, por isso o cadastro de insumos não precisa de embrulho.
 */
fun FilamentRepository.deletingStockWith(stock: StockRepository): FilamentRepository = StockCleaningFilamentRepository(this, stock)

/**
 * Ver [reconcilingStock]. A baixa roda logo depois de cada chamada que muda o histórico, na mesma thread (a da tela):
 * o movimento é pedido pra gravação junto com o pedido, sem esperar uma coleta em segundo plano que poderia não rodar
 * antes de o app fechar. Como a comparação é com a última lista vista, uma mudança que não passe por aqui é acertada
 * na próxima que passar.
 */
private class StockReconcilingQuoteHistoryRepository(
    private val inner: QuoteHistoryRepository,
    private val stock: StockRepository,
    private val filaments: () -> List<Filament>,
) : QuoteHistoryRepository by inner {

    private var seen: List<SavedQuote> = inner.savedQuotes.value

    private fun <T> reconciling(change: () -> T): T = change().also { reconcile() }

    private fun reconcile() {
        val current = inner.savedQuotes.value
        if (current === seen) return
        val previous = seen
        seen = current
        stock.reconcileOrders(previous, current, filaments())
    }

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
    ): SavedQuote = reconciling {
        inner.save(
            name, quote, services, photo, stlFile, sourceLink, client, printThumbnails, shippingCost, deliveryDateEpochDay, kind,
            sourceProductId, category, soldAtCatalogPrice, currency, shippingAbsorbed, autoNamed,
        )
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
    ): SavedQuote? = reconciling {
        inner.update(
            id, name, quote, services, photo, stlFile, sourceLink, client, printThumbnails, shippingCost, deliveryDateEpochDay, category,
            soldAtCatalogPrice, shippingAbsorbed, autoNamed,
        )
    }

    override fun updateDetails(id: String, details: QuoteDetails): SavedQuote? = reconciling { inner.updateDetails(id, details) }

    override fun delete(id: String) = reconciling { inner.delete(id) }

    override fun restore(id: String): SavedQuote? = reconciling { inner.restore(id) }

    override fun convertToOrder(id: String) = reconciling { inner.convertToOrder(id) }

    override fun updateQuote(id: String, quote: Quote) = reconciling { inner.updateQuote(id, quote) }

    override fun updateStatus(id: String, status: OrderStatus) = reconciling { inner.updateStatus(id, status) }

    override fun updatePrintSettings(id: String, printSettings: PrintSettings?) = reconciling { inner.updatePrintSettings(id, printSettings) }

    override fun updateDeliveryDate(id: String, deliveryDateEpochDay: Long?) = reconciling { inner.updateDeliveryDate(id, deliveryDateEpochDay) }

    override fun addPayment(id: String, amount: Double, method: PaymentMethod, epochDay: Long): Payment? =
        reconciling { inner.addPayment(id, amount, method, epochDay) }

    override fun deletePayment(id: String, paymentId: String): Payment? = reconciling { inner.deletePayment(id, paymentId) }

    override fun updateDeposit(id: String, depositAmount: Double) = reconciling { inner.updateDeposit(id, depositAmount) }

    override fun recordFollowUp(id: String) = reconciling { inner.recordFollowUp(id) }
}

/** Ver [deletingStockWith]. */
private class StockCleaningFilamentRepository(
    private val inner: FilamentRepository,
    private val stock: StockRepository,
) : FilamentRepository by inner {

    override fun update(item: Filament) {
        val before = inner.items.value.find { it.id == item.id }
        inner.update(item)
        if (before == null) return
        val kept = item.colors.mapTo(HashSet()) { it.id }
        before.colors.filter { it.id !in kept }.forEach { stock.deleteAllFor(StockItemRef.ColorRef(item.id, it.id)) }
    }
}
