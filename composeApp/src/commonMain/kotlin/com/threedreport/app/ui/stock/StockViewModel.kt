package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.common_error_positive
import _3dreport.composeapp.generated.resources.common_error_required
import _3dreport.composeapp.generated.resources.stock_amount_grams
import _3dreport.composeapp.generated.resources.stock_amount_units
import _3dreport.composeapp.generated.resources.stock_error_below_spool
import _3dreport.composeapp.generated.resources.stock_error_no_items
import _3dreport.composeapp.generated.resources.stock_error_pick_item
import _3dreport.composeapp.generated.resources.stock_notice_adjusted
import _3dreport.composeapp.generated.resources.stock_notice_copy_failed
import _3dreport.composeapp.generated.resources.stock_notice_item_saved
import _3dreport.composeapp.generated.resources.stock_notice_link_failed
import _3dreport.composeapp.generated.resources.stock_notice_list_copied
import _3dreport.composeapp.generated.resources.stock_notice_movement_deleted
import _3dreport.composeapp.generated.resources.stock_notice_purchase_deleted
import _3dreport.composeapp.generated.resources.stock_notice_purchase_saved
import _3dreport.composeapp.generated.resources.stock_notice_stopped
import _3dreport.composeapp.generated.resources.stock_notice_tracking
import _3dreport.composeapp.generated.resources.stock_notice_use_recorded
import _3dreport.composeapp.generated.resources.stock_notice_weighed
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.StockRepository
import com.threedreport.app.data.recordPurchase
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.filaments.labelOrNull
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toMoneyInputText
import com.threedreport.app.ui.format.toUnitsNumberText
import com.threedreport.app.ui.format.toWholeGramsNumber
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.CatalogPriceUpdate
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovement
import com.threedreport.core.report.StockLedger
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.math.ceil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela Estoque (decisão 163). Um [uiState] só e os [notices]. Os números (saldo, reservado, alertas, lista de
 * compras, médias) são do `core` ([StockReport]); aqui se monta o que a tela mostra e se encaminha cada evento pro
 * [StockRepository]. O estado acompanha os repositórios dentro de [scope]; o rascunho dos painéis e diálogos muda na hora.
 */
class StockViewModel(
    private val stock: StockRepository,
    private val filamentRepository: FilamentRepository,
    private val consumableRepository: ConsumableRepository,
    private val historyRepository: QuoteHistoryRepository,
    scope: CoroutineScope,
    private val platform: PlatformServices = defaultPlatform,
    /** A moeda em uso: preço e frete da compra são lidos e escritos com o separador decimal dela. */
    private val currency: () -> Currency = { Currency.BRL },
    /** Hoje, em dias desde 01/01/1970: a data de uma compra nova. */
    private val today: () -> Long = ::todayEpochDay,
    /** Onde roda o que espera o texto de um recurso (a lista copiada); os testes passam o padrão. */
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
    private val newId: () -> String = ::randomId,
) {
    private val scope: CoroutineScope = scope

    /** O que é rascunho: a vista, o item ou a compra aberta no painel e o diálogo. O resto sai dos repositórios. */
    private data class Local(
        val view: StockView = StockView.BALANCE,
        val itemRef: StockItemRef? = null,
        val thresholdText: String = "",
        val spoolText: String = "",
        val thresholdError: UiText? = null,
        val spoolError: UiText? = null,
        val purchase: PurchaseFormState? = null,
        /** Como o formulário de compra nasceu: diferente dele, há alteração não salva (ver [purchaseIsDirty]). */
        val purchaseBaseline: PurchaseFormState? = null,
        /** O que a pessoa pediu enquanto a compra tinha alteração não salva; espera a resposta do diálogo de descartar. */
        val pendingSwitch: PanelSwitch? = null,
        val dialog: StockDialog? = null,
    )

    /** Uma troca de painel que descartaria a compra aberta. */
    private sealed interface PanelSwitch {
        data class Item(val ref: StockItemRef) : PanelSwitch

        data object NewPurchase : PanelSwitch

        data object PurchaseFromShopping : PanelSwitch

        data class Purchase(val id: String) : PanelSwitch
    }

    private var local = Local()

    private lateinit var data: Data

    /** Quantas vezes o estoque foi recalculado ([rebuildData]): o teste confere que digitar não recalcula nada. */
    internal var dataBuilds: Int = 0
        private set

    private val state = MutableStateFlow(StockUiState())

    /** Tudo o que a tela mostra. */
    val uiState: StateFlow<StockUiState> = state.asStateFlow()

    private val noticeFlow = MutableSharedFlow<UiNotice>(extraBufferCapacity = NOTICE_BUFFER)

    /** Os avisos de uma frase ("Compra registrada"); o de erro fica mais tempo na tela. */
    val notices: SharedFlow<UiNotice> = noticeFlow.asSharedFlow()

    private val decimal: Char get() = AppLocale.decimalSeparator
    private val moneyDecimal: Char get() = currency().decimalSeparator

    init {
        rebuildData()
        publish()
        combine(
            listOf(
                stock.items, stock.movements, stock.purchases,
                filamentRepository.filaments, consumableRepository.consumables, historyRepository.savedQuotes,
            ),
        ) { }.onEach {
            rebuildData()
            publish()
        }.launchIn(scope)
    }

    // ---------------------------------------------------------------------------------------------------- vistas

    fun selectView(view: StockView) = changeLocal { it.copy(view = view) }

    fun closePanel() = changeLocal { it.copy(itemRef = null, purchase = null, purchaseBaseline = null, thresholdError = null, spoolError = null) }

    /** A compra aberta tem alteração não salva: trocar de painel sem perguntar jogaria o que foi digitado fora. */
    private val purchaseIsDirty: Boolean get() = local.purchase != null && local.purchase != local.purchaseBaseline

    /** Faz a troca de painel [switch], ou, com uma compra pela metade, pergunta antes se pode descartá-la. */
    private fun switchTo(switch: PanelSwitch) {
        val open = local.purchase
        val same = switch is PanelSwitch.Purchase && open?.id == switch.id
        if (same) return
        if (purchaseIsDirty) {
            changeLocal { it.copy(pendingSwitch = switch, dialog = StockDialog(StockDialogKind.DISCARD_PURCHASE)) }
        } else {
            perform(switch)
        }
    }

    private fun perform(switch: PanelSwitch) = when (switch) {
        is PanelSwitch.Item -> showItem(switch.ref)
        PanelSwitch.NewPurchase -> showNewPurchase()
        PanelSwitch.PurchaseFromShopping -> showPurchaseFromShopping()
        is PanelSwitch.Purchase -> showPurchase(switch.id)
    }

    // ------------------------------------------------------------------------------------------- painel do item

    fun openItem(ref: StockItemRef) {
        if (stock.itemFor(ref) == null) return
        switchTo(PanelSwitch.Item(ref))
    }

    private fun showItem(ref: StockItemRef) {
        val item = stock.itemFor(ref) ?: return
        changeLocal {
            it.copy(
                itemRef = ref,
                purchase = null,
                purchaseBaseline = null,
                thresholdText = item.lowThreshold?.toInputText(decimal).orEmpty(),
                spoolText = item.emptySpoolGrams?.toInputText(decimal).orEmpty(),
                thresholdError = null,
                spoolError = null,
            )
        }
    }

    fun setThreshold(text: String) = changeLocal { it.copy(thresholdText = text, thresholdError = null) }

    fun setSpool(text: String) = changeLocal { it.copy(spoolText = text, spoolError = null) }

    /** "Salvar" do painel do item: o "Avisar abaixo de" e o carretel vazio. Vazio é "sem". */
    fun saveItem() {
        val current = local
        val ref = current.itemRef ?: return
        val threshold = optionalAmount(current.thresholdText)
        val spool = optionalAmount(current.spoolText).takeIf { ref is StockItemRef.ColorRef } ?: ParsedAmount.Empty
        if (threshold is ParsedAmount.Invalid || spool is ParsedAmount.Invalid) {
            changeLocal { it.copy(thresholdError = threshold.errorOrNull(), spoolError = spool.errorOrNull()) }
            return
        }
        val lowThreshold = (threshold as? ParsedAmount.Value)?.number
        val emptySpool = if (ref is StockItemRef.ColorRef) (spool as? ParsedAmount.Value)?.number else stock.itemFor(ref)?.emptySpoolGrams
        stock.updateItemSettings(ref, lowThreshold, emptySpool)
        changeLocal { it.copy(itemRef = null, thresholdError = null, spoolError = null) }
        notify(Res.string.stock_notice_item_saved)
    }

    fun requestDeleteMovement(id: String) {
        val movement = stock.movements.value.find { it.id == id } ?: return
        if (!movement.isRemovableByHand) return
        changeLocal { it.copy(dialog = StockDialog(StockDialogKind.DELETE_MOVEMENT, targetId = id)) }
    }

    // ------------------------------------------------------------------------------------------------- diálogos

    fun startWeigh(ref: StockItemRef) {
        val item = stock.itemFor(ref) ?: return
        openDialog(StockDialog(StockDialogKind.WEIGH, entry = entryOf(ref), spoolText = item.emptySpoolGrams?.toInputText(decimal).orEmpty()))
    }

    fun startManualUse(ref: StockItemRef) {
        if (stock.itemFor(ref) == null) return
        openDialog(StockDialog(StockDialogKind.MANUAL_USE, entry = entryOf(ref)))
    }

    fun startAdjust(ref: StockItemRef) {
        if (stock.itemFor(ref) == null) return
        val balance = StockReport.balance(ref, stock.movements.value)
        openDialog(StockDialog(StockDialogKind.ADJUST, entry = entryOf(ref), amountText = balance.toInputText(decimal, 2)))
    }

    /** "Controlar saldo": pergunta o saldo de agora. */
    fun startTrack(ref: StockItemRef) {
        if (stock.itemFor(ref) != null) return
        openDialog(StockDialog(StockDialogKind.TRACK, entry = entryOf(ref)))
    }

    fun requestStopTracking(ref: StockItemRef) {
        if (stock.itemFor(ref) == null) return
        openDialog(StockDialog(StockDialogKind.STOP_TRACKING, entry = entryOf(ref)))
    }

    /** Pergunta se pode excluir a compra [id], ou a que está aberta no painel. */
    fun requestDeletePurchase(id: String? = local.purchase?.id) {
        id ?: return
        if (stock.purchases.value.none { it.id == id }) return
        openDialog(StockDialog(StockDialogKind.DELETE_PURCHASE, targetId = id))
    }

    private fun openDialog(dialog: StockDialog) = changeLocal { it.copy(dialog = dialog) }

    fun dismissDialog() = changeLocal { it.copy(dialog = null, pendingSwitch = null) }

    /** O número principal do diálogo: peso na balança, quanto saiu, saldo novo ou saldo atual. */
    fun setDialogAmount(text: String) = changeDialog { it.copy(amountText = text, error = null) }

    /** O carretel vazio do diálogo de pesar. */
    fun setDialogSpool(text: String) = changeDialog { it.copy(spoolText = text, error = null) }

    fun setDialogReason(reason: ManualUseReason) = changeDialog { it.copy(reason = reason) }

    fun setDialogNote(text: String) = changeDialog { it.copy(noteText = text) }

    private fun changeDialog(transform: (StockDialog) -> StockDialog) = changeLocal { s -> s.copy(dialog = s.dialog?.let(transform)) }

    /** O botão que confirma o diálogo aberto. Com um valor inválido, o diálogo continua aberto com o erro no campo. */
    fun confirmDialog() {
        val dialog = local.dialog ?: return
        val ref = dialog.entry?.ref
        when (dialog.kind) {
            StockDialogKind.WEIGH -> {
                ref ?: return
                val scale = requiredAmount(dialog.amountText, allowZero = true)
                val spool = optionalAmount(dialog.spoolText)
                val error = (scale as? ParsedAmount.Invalid)?.error ?: spool.errorOrNull()
                val scaleValue = (scale as? ParsedAmount.Value)?.number
                val spoolValue = (spool as? ParsedAmount.Value)?.number
                when {
                    error != null -> failDialog(error)

                    scaleValue == null -> failDialog(uiText(Res.string.common_error_required))

                    StockLedger.weighedBalance(scaleValue, spoolValue) < 0 -> failDialog(uiText(Res.string.stock_error_below_spool))

                    else -> {
                        stock.recordWeighing(ref, scaleValue, spoolValue)
                        finishDialog(Res.string.stock_notice_weighed)
                    }
                }
            }

            StockDialogKind.MANUAL_USE -> {
                ref ?: return
                when (val amount = requiredAmount(dialog.amountText, allowZero = false)) {
                    is ParsedAmount.Invalid -> failDialog(amount.error)

                    is ParsedAmount.Value -> {
                        stock.recordManualUse(ref, amount.number, dialog.reason, dialog.noteText.trim().ifEmpty { null })
                        finishDialog(Res.string.stock_notice_use_recorded)
                    }

                    ParsedAmount.Empty -> failDialog(uiText(Res.string.common_error_required))
                }
            }

            StockDialogKind.ADJUST -> {
                ref ?: return
                when (val amount = requiredAmount(dialog.amountText, allowZero = true)) {
                    is ParsedAmount.Invalid -> failDialog(amount.error)

                    is ParsedAmount.Value -> {
                        stock.adjustTo(ref, amount.number)
                        finishDialog(Res.string.stock_notice_adjusted)
                    }

                    ParsedAmount.Empty -> failDialog(uiText(Res.string.common_error_required))
                }
            }

            StockDialogKind.TRACK -> {
                ref ?: return
                when (val amount = requiredAmount(dialog.amountText, allowZero = true)) {
                    is ParsedAmount.Invalid -> failDialog(amount.error)

                    is ParsedAmount.Value -> {
                        stock.startTracking(ref, amount.number)
                        finishDialog(Res.string.stock_notice_tracking)
                    }

                    ParsedAmount.Empty -> failDialog(uiText(Res.string.common_error_required))
                }
            }

            StockDialogKind.STOP_TRACKING -> {
                ref ?: return
                stock.stopTracking(ref)
                changeLocal { it.copy(dialog = null, itemRef = it.itemRef.takeIf { open -> open != ref }) }
                notify(Res.string.stock_notice_stopped)
            }

            StockDialogKind.DELETE_MOVEMENT -> {
                dialog.targetId?.let(stock::deleteMovement)
                finishDialog(Res.string.stock_notice_movement_deleted)
            }

            StockDialogKind.DISCARD_PURCHASE -> {
                // Descartou: a compra pela metade sai e a troca que foi pedida acontece.
                val pending = local.pendingSwitch
                changeLocal { it.copy(dialog = null, pendingSwitch = null, purchase = null, purchaseBaseline = null) }
                pending?.let(::perform)
            }

            StockDialogKind.DELETE_PURCHASE -> {
                dialog.targetId?.let(stock::deletePurchase)
                changeLocal { it.copy(dialog = null, purchase = null, purchaseBaseline = null) }
                notify(Res.string.stock_notice_purchase_deleted)
            }
        }
    }

    private fun failDialog(error: UiText) = changeDialog { it.copy(error = error) }

    private fun finishDialog(notice: StringResource) {
        changeLocal { it.copy(dialog = null) }
        notify(notice)
    }

    // ------------------------------------------------------------------------------------------------- compras

    /** "Registrar compra" do cabeçalho: um formulário novo com uma linha em branco. */
    fun startPurchase() = switchTo(PanelSwitch.NewPurchase)

    private fun showNewPurchase() = openForm(PurchaseFormState(dateEpochDay = today(), lines = listOf(PurchaseLineForm(id = 1)), nextLineId = 2))

    private fun openForm(form: PurchaseFormState) = changeLocal { it.copy(purchase = form, purchaseBaseline = form, itemRef = null) }

    /** "Registrar compra" da lista de compras: as linhas e quantidades da lista, com o preço por preencher. */
    fun startPurchaseFromShopping() = switchTo(PanelSwitch.PurchaseFromShopping)

    private fun showPurchaseFromShopping() {
        val lines = state.value.shopping.lines.mapIndexed { index, row ->
            PurchaseLineForm(id = index + 1, ref = row.line.ref, amountText = row.amount.toInputText(decimal, 2))
        }
        if (lines.isEmpty()) return showNewPurchase()
        openForm(PurchaseFormState(dateEpochDay = today(), lines = lines, nextLineId = lines.size + 1))
    }

    /** Abre a compra [id] no painel, pra editar ou excluir. */
    fun openPurchase(id: String) {
        if (stock.purchases.value.none { it.id == id }) return
        switchTo(PanelSwitch.Purchase(id))
    }

    private fun showPurchase(id: String) {
        val purchase = stock.purchases.value.find { it.id == id } ?: return
        val lines = purchase.lines.mapIndexed { index, line ->
            PurchaseLineForm(
                id = index + 1,
                ref = line.ref,
                amountText = line.amount.toInputText(decimal, 2),
                priceText = line.price.toMoneyInputText(moneyDecimal),
            )
        }
        openForm(
            PurchaseFormState(
                id = purchase.id,
                dateEpochDay = purchase.dateEpochDay,
                supplier = purchase.supplier.orEmpty(),
                link = purchase.link.orEmpty(),
                shippingText = purchase.shipping.takeIf { it > 0 }?.toMoneyInputText(moneyDecimal).orEmpty(),
                note = purchase.note.orEmpty(),
                lines = lines,
                updatesMarkedByDefault = false,
                nextLineId = lines.size + 1,
            ),
        )
    }

    fun updatePurchase(transform: (PurchaseFormState) -> PurchaseFormState) = changeLocal { s ->
        s.copy(purchase = s.purchase?.let { transform(it).copy(shippingError = null, linesError = null) })
    }

    fun addPurchaseLine() = updatePurchase { it.copy(lines = it.lines + PurchaseLineForm(id = it.nextLineId), nextLineId = it.nextLineId + 1) }

    fun removePurchaseLine(lineId: Int) = updatePurchase { form ->
        form.copy(lines = form.lines.filterNot { it.id == lineId }.ifEmpty { listOf(PurchaseLineForm(id = form.nextLineId)) }, nextLineId = form.nextLineId + 1)
    }

    fun updatePurchaseLine(lineId: Int, transform: (PurchaseLineForm) -> PurchaseLineForm) = updatePurchase { form ->
        form.copy(lines = form.lines.map { if (it.id == lineId) transform(it).copy(refError = null, amountError = null, priceError = null) else it })
    }

    /** Os atalhos "1 kg" e "500 g" da linha de filamento. */
    fun fillLineGrams(lineId: Int, grams: Double) = updatePurchaseLine(lineId) { it.copy(amountText = grams.toInputText(decimal)) }

    /** Vira a caixa "Atualizar o preço" de [key] ([PriceUpdateRow.key]). */
    fun toggleUpdate(key: String) = updatePurchase { form ->
        form.copy(toggledUpdates = if (key in form.toggledUpdates) form.toggledUpdates - key else form.toggledUpdates + key)
    }

    /**
     * "Salvar" do formulário de compra: grava a compra, soma nos itens controlados, marca como "tem" as cores sem controle
     * que entraram, e atualiza o preço do cadastro só onde a caixa ficou marcada ([recordPurchase]).
     */
    fun savePurchase() {
        val form = local.purchase ?: return
        val checked = validatedLines(form)
        val shipping = if (form.shippingText.isBlank()) ParsedAmount.Value(0.0) else moneyAmount(form.shippingText)
        val shippingError = (shipping as? ParsedAmount.Invalid)?.error
        val noLines = if (form.lines.isEmpty()) uiText(Res.string.stock_error_no_items) else null
        if (checked.any { it.hasError } || shippingError != null || noLines != null) {
            updateWithErrors(form, checked, shippingError, noLines)
            return
        }
        val purchase = Purchase(
            id = form.id ?: newId(),
            dateEpochDay = form.dateEpochDay,
            supplier = form.supplier.trim().ifEmpty { null },
            link = form.link.trim().ifEmpty { null },
            shipping = (shipping as ParsedAmount.Value).number,
            note = form.note.trim().ifEmpty { null },
            lines = checked.mapNotNull { it.line },
        )
        val updates = marked(form, purchase.catalogPriceUpdates(filamentRepository.filaments.value, consumableRepository.consumables.value))
        stock.recordPurchase(purchase, updates, filamentRepository, consumableRepository)
        changeLocal { it.copy(purchase = null, purchaseBaseline = null) }
        notify(Res.string.stock_notice_purchase_saved)
    }

    private fun updateWithErrors(form: PurchaseFormState, checked: List<CheckedLine>, shippingError: UiText?, noLines: UiText?) {
        val withErrors = form.copy(
            lines = checked.map { it.form },
            shippingError = shippingError,
            linesError = noLines,
        )
        changeLocal { it.copy(purchase = withErrors) }
    }

    /** Cada linha do rascunho lida: a [PurchaseLine] pronta, ou o rascunho com os erros dos campos. */
    private class CheckedLine(val form: PurchaseLineForm, val line: PurchaseLine?) {
        val hasError: Boolean get() = line == null
    }

    private fun validatedLines(form: PurchaseFormState): List<CheckedLine> = form.lines.map { line ->
        val refError = uiText(Res.string.stock_error_pick_item).takeIf { line.ref == null }
        val amount = requiredAmount(line.amountText, allowZero = false)
        val price = if (line.priceText.isBlank()) ParsedAmount.Empty else moneyAmount(line.priceText)
        val priceError = when (price) {
            ParsedAmount.Empty -> uiText(Res.string.common_error_required)
            is ParsedAmount.Invalid -> price.error
            is ParsedAmount.Value -> null
        }
        val amountError = when (amount) {
            ParsedAmount.Empty -> uiText(Res.string.common_error_required)
            is ParsedAmount.Invalid -> amount.error
            is ParsedAmount.Value -> null
        }
        val ref = line.ref
        if (ref != null && amount is ParsedAmount.Value && price is ParsedAmount.Value) {
            CheckedLine(line, PurchaseLine(ref, amount.number, price.number))
        } else {
            CheckedLine(line.copy(refError = refError, amountError = amountError, priceError = priceError), null)
        }
    }

    /** As atualizações de preço de [all] que ficaram marcadas no formulário. */
    private fun marked(form: PurchaseFormState, all: List<CatalogPriceUpdate>): List<CatalogPriceUpdate> =
        all.filter { isChecked(form, updateKey(it)) }

    private fun isChecked(form: PurchaseFormState, key: String): Boolean = form.updatesMarkedByDefault != (key in form.toggledUpdates)

    // ---------------------------------------------------------------------------------------- lista de compras

    /** "Copiar lista": uma linha por item ("PLA Voolt Vermelho: 260 g"), pra colar numa mensagem pro fornecedor. */
    fun copyShoppingList() {
        val shopping = state.value.shopping
        scope.launch(main) {
            val text = shopping.lines.map { row -> "${row.entry.displayName}: ${amountText(row.entry.ref, row.amount)}" }.joinToString("\n")
            if (text.isEmpty()) return@launch
            notify(if (platform.copyToClipboard(text)) Res.string.stock_notice_list_copied else Res.string.stock_notice_copy_failed, error = false)
        }
    }

    private suspend fun amountText(ref: StockItemRef, amount: Double): String = when (ref) {
        is StockItemRef.ColorRef -> getString(Res.string.stock_amount_grams, amount.toWholeGramsNumber())
        is StockItemRef.ConsumableRef -> getString(Res.string.stock_amount_units, amount.toUnitsNumberText())
    }

    /** "Comprar de novo": abre no navegador o link da última compra do item que tinha um. */
    fun buyAgain(ref: StockItemRef) {
        val link = StockReport.lastPurchaseLink(ref, stock.purchases.value) ?: return
        if (!platform.openUrl(link)) notify(Res.string.stock_notice_link_failed, error = true)
    }

    // ------------------------------------------------------------------------------------------------ montagem

    private fun changeLocal(transform: (Local) -> Local) {
        local = transform(local)
        publish()
    }

    private fun notify(message: StringResource, error: Boolean = false) {
        noticeFlow.tryEmit(UiNotice(uiText(message), isError = error))
    }

    private fun publish() {
        state.value = compose(data)
    }

    /**
     * Tudo o que vem dos repositórios, calculado uma vez e guardado: o `core` refaz o saldo, o reservado e a lista de compras sobre
     * todos os pedidos, então isso só roda quando itens, movimentos, compras, pedidos, filamentos ou insumos mudam ([rebuildData]).
     * Digitar num campo, abrir um diálogo ou trocar de vista só refaz o rascunho e o que depende dele ([compose]).
     */
    private class Data(
        val movements: List<StockMovement>,
        val purchases: List<Purchase>,
        val filaments: List<Filament>,
        val consumables: List<Consumable>,
        val names: NameBook,
        val statusByRef: Map<StockItemRef, StockStatus>,
        val trackedRefs: Set<StockItemRef>,
        val alertCount: Int,
        val filamentGroups: List<StockFilamentGroup>,
        val consumableRows: List<StockRowState>,
        val untracked: List<StockEntry>,
        val purchaseRows: List<PurchaseRowState>,
        val shopping: ShoppingState,
        val choices: List<StockEntry>,
        val suppliers: List<String>,
    )

    private fun rebuildData() {
        data = buildData()
        dataBuilds++
    }

    private fun buildData(): Data {
        val items = stock.items.value
        val movements = stock.movements.value
        val purchases = stock.purchases.value
        val filaments = filamentRepository.filaments.value
        val consumables = consumableRepository.consumables.value
        val quotes = historyRepository.savedQuotes.value
        val overview = StockReport.overview(items, movements, quotes, filaments, consumables)
        val names = NameBook(filaments, consumables)

        val rows = overview.tracked.map { StockRowState(names.entryOf(it.item.ref), it) }
        val groups = rows.filter { it.ref is StockItemRef.ColorRef }
            .groupBy { (it.ref as StockItemRef.ColorRef).filamentId }
            .values.map { group -> StockFilamentGroup(group.first().entry.filamentName.orEmpty(), group) }
        val shopping = StockReport.shoppingList(items, movements, quotes, filaments, consumables)
        val trackedRefs = overview.tracked.mapTo(HashSet()) { it.item.ref }
        return Data(
            movements = movements,
            purchases = purchases,
            filaments = filaments,
            consumables = consumables,
            names = names,
            statusByRef = overview.tracked.associateBy { it.item.ref },
            trackedRefs = trackedRefs,
            alertCount = overview.alertCount,
            filamentGroups = groups,
            consumableRows = rows.filter { it.ref is StockItemRef.ConsumableRef },
            untracked = overview.untracked.map(names::entryOf),
            purchaseRows = purchases.withIndex()
                .sortedWith(compareByDescending<IndexedValue<Purchase>> { it.value.dateEpochDay }.thenByDescending { it.index })
                .map { (_, purchase) ->
                    PurchaseRowState(
                        id = purchase.id,
                        dateEpochDay = purchase.dateEpochDay,
                        supplier = purchase.supplier,
                        lines = purchase.lines.map { PurchaseSummaryLine(names.entryOf(it.ref), it.amount) },
                        total = purchase.total,
                        selected = false,
                    )
                },
            shopping = ShoppingState(
                lines = shopping.lines.map { ShoppingRowState(names.entryOf(it.ref), it, ceilAmount(it.amount, it.ref)) },
                outOfStock = shopping.outOfStockColors.map { OutOfStockRowState(names.entryOf(it.ref), it.orderCount) },
                hasTracked = items.isNotEmpty(),
            ),
            choices = (trackedRefs + overview.untracked).map(names::entryOf)
                .sortedWith(compareBy({ it.ref is StockItemRef.ConsumableRef }, { it.displayName.lowercase() })),
            suppliers = StockReport.suppliers(purchases),
        )
    }

    /** O estado da tela: o que veio dos repositórios ([data], já pronto) mais o rascunho de agora. */
    private fun compose(data: Data): StockUiState {
        val openPurchaseId = local.purchase?.id
        return StockUiState(
            view = local.view,
            alertCount = data.alertCount,
            filamentGroups = data.filamentGroups,
            consumables = data.consumableRows,
            untracked = data.untracked,
            purchases = data.purchaseRows.map { it.copy(selected = it.id == openPurchaseId) },
            shopping = data.shopping,
            panel = itemPanel(data)?.let(StockPanel::Item) ?: purchasePanel(data)?.let(StockPanel::Purchase),
            dialog = local.dialog?.let { withDerived(it, data.names, data.movements) },
            today = today(),
        )
    }

    private fun itemPanel(data: Data): StockItemPanel? {
        val ref = local.itemRef ?: return null
        val status = data.statusByRef[ref] ?: return null
        return StockItemPanel(
            entry = data.names.entryOf(ref),
            status = status,
            averageCost = StockReport.averagePurchaseCost(ref, data.purchases),
            buyAgainLink = StockReport.lastPurchaseLink(ref, data.purchases),
            movements = data.movements.withIndex().filter { it.value.ref == ref }
                .sortedWith(compareByDescending<IndexedValue<StockMovement>> { it.value.epochMillis }.thenByDescending { it.index })
                .map { (_, m) ->
                    MovementRowState(m.id, m.epochMillis, m.kind, m.amount, m.orderNumber?.let { "#" + it.toString().padStart(ORDER_DIGITS, '0') }, m.reason, m.note, m.isRemovableByHand)
                },
            thresholdText = local.thresholdText,
            spoolText = local.spoolText,
            thresholdError = local.thresholdError,
            spoolError = local.spoolError,
        )
    }

    private fun purchasePanel(data: Data): PurchasePanel? {
        val form = local.purchase ?: return null
        val names = data.names
        val ready = form.lines.mapNotNull { line ->
            val ref = line.ref ?: return@mapNotNull null
            val amount = (requiredAmount(line.amountText, allowZero = false) as? ParsedAmount.Value)?.number ?: return@mapNotNull null
            val price = (moneyAmount(line.priceText) as? ParsedAmount.Value)?.number ?: return@mapNotNull null
            line.id to PurchaseLine(ref, amount, price)
        }
        val shipping = (moneyAmount(form.shippingText) as? ParsedAmount.Value)?.number ?: 0.0
        val partial = ready.takeIf { it.isNotEmpty() }?.let { Purchase(id = form.id.orEmpty(), dateEpochDay = form.dateEpochDay, shipping = shipping, lines = it.map { (_, line) -> line }) }
        val costOf = ready.mapIndexed { index, (lineId, line) -> lineId to partial?.catalogUnitCost(partial.lines[index]) }.toMap()
        val allUpdates = partial?.catalogPriceUpdates(data.filaments, data.consumables).orEmpty()
        val total = partial?.total ?: shipping
        return PurchasePanel(
            form = form,
            total = total,
            lines = form.lines.map { line ->
                PurchaseLineView(line.id, line.ref?.let(names::entryOf), tracked = line.ref in data.trackedRefs, catalogUnitCost = costOf[line.id])
            },
            updates = allUpdates.map { update -> priceRow(update, form, data.filaments, data.consumables) },
            suppliers = data.suppliers,
            choices = data.choices,
        )
    }

    private fun priceRow(update: CatalogPriceUpdate, form: PurchaseFormState, filaments: List<Filament>, consumables: List<Consumable>): PriceUpdateRow {
        val key = updateKey(update)
        return when (update) {
            is CatalogPriceUpdate.FilamentPrice ->
                PriceUpdateRow(key, filaments.find { it.id == update.filamentId }?.name.orEmpty(), true, update.currentPricePerKg, update.newPricePerKg, isChecked(form, key))

            is CatalogPriceUpdate.ConsumableCost ->
                PriceUpdateRow(key, consumables.find { it.id == update.consumableId }?.name.orEmpty(), false, update.currentUnitCost, update.newUnitCost, isChecked(form, key))
        }
    }

    private fun updateKey(update: CatalogPriceUpdate): String = when (update) {
        is CatalogPriceUpdate.FilamentPrice -> "f:${update.filamentId}"
        is CatalogPriceUpdate.ConsumableCost -> "c:${update.consumableId}"
    }

    /** O saldo de agora e, ao pesar, o saldo que a balança dá. */
    private fun withDerived(dialog: StockDialog, names: NameBook, movements: List<StockMovement>): StockDialog {
        val ref = dialog.entry?.ref ?: return dialog
        val fresh = dialog.copy(entry = names.entryOf(ref), currentBalance = StockReport.balance(ref, movements))
        if (dialog.kind != StockDialogKind.WEIGH) return fresh
        val scale = (requiredAmount(dialog.amountText, allowZero = true) as? ParsedAmount.Value)?.number ?: return fresh.copy(newBalance = null)
        val spool = (optionalAmount(dialog.spoolText) as? ParsedAmount.Value)?.number
        return fresh.copy(newBalance = StockLedger.weighedBalance(scale, spool))
    }

    private fun entryOf(ref: StockItemRef): StockEntry = NameBook(filamentRepository.filaments.value, consumableRepository.consumables.value).entryOf(ref)

    /** Os nomes do cadastro de agora, pra escrever o item de cada referência. */
    private class NameBook(private val filaments: List<Filament>, private val consumables: List<Consumable>) {
        fun entryOf(ref: StockItemRef): StockEntry = when (ref) {
            is StockItemRef.ColorRef -> {
                val filament = filaments.find { it.id == ref.filamentId }
                val color = filament?.colors?.find { it.id == ref.colorId }
                if (filament == null || color == null) {
                    StockEntry(ref, removed = true)
                } else {
                    StockEntry(ref, filamentName = filament.name, name = color.name?.takeIf { it.isNotBlank() } ?: color.labelOrNull(), colorHex = color.hex)
                }
            }

            is StockItemRef.ConsumableRef -> consumables.find { it.id == ref.consumableId }
                ?.let { StockEntry(ref, name = it.name) }
                ?: StockEntry(ref, removed = true)
        }
    }

    // ---------------------------------------------------------------------------------------------- leitura de números

    /** Um número digitado: vazio, inválido (com o erro do campo) ou o valor. */
    private sealed interface ParsedAmount {
        data object Empty : ParsedAmount

        data class Invalid(val error: UiText) : ParsedAmount

        data class Value(val number: Double) : ParsedAmount
    }

    private fun ParsedAmount.errorOrNull(): UiText? = (this as? ParsedAmount.Invalid)?.error

    /** Gramas ou unidades opcionais: vazio é "sem", e um valor precisa ser maior que zero. */
    private fun optionalAmount(text: String): ParsedAmount =
        if (text.isBlank()) ParsedAmount.Empty else requiredAmount(text, allowZero = false)

    private fun requiredAmount(text: String, allowZero: Boolean): ParsedAmount {
        if (text.isBlank()) return ParsedAmount.Empty
        val number = parseDecimal(text, NumberKind.AMOUNT, decimal) ?: return ParsedAmount.Invalid(uiText(Res.string.common_error_number_invalid))
        return when {
            number < 0 -> ParsedAmount.Invalid(uiText(Res.string.common_error_negative))
            number == 0.0 && !allowZero -> ParsedAmount.Invalid(uiText(Res.string.common_error_positive))
            else -> ParsedAmount.Value(number)
        }
    }

    /** Dinheiro digitado (preço, frete): zero vale, negativo não. */
    private fun moneyAmount(text: String): ParsedAmount {
        if (text.isBlank()) return ParsedAmount.Empty
        val number = parseDecimal(text, NumberKind.AMOUNT, moneyDecimal) ?: return ParsedAmount.Invalid(uiText(Res.string.common_error_number_invalid))
        return if (number < 0) ParsedAmount.Invalid(uiText(Res.string.common_error_negative)) else ParsedAmount.Value(number)
    }

    /** Pra cima, pra comprar o que a conta pede e não menos: gramas inteiras, unidades com duas casas. */
    private fun ceilAmount(amount: Double, ref: StockItemRef): Double = when (ref) {
        is StockItemRef.ColorRef -> ceil(amount - StockReport.EPSILON)
        is StockItemRef.ConsumableRef -> ceil(amount * UNIT_SCALE - StockReport.EPSILON) / UNIT_SCALE
    }

    private companion object {
        const val NOTICE_BUFFER = 8
        const val ORDER_DIGITS = 4
        const val UNIT_SCALE = 100.0

        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
