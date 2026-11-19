package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_action_undo
import _3dreport.composeapp.generated.resources.quote_blocked_deleted
import _3dreport.composeapp.generated.resources.quote_blocked_incomplete
import _3dreport.composeapp.generated.resources.quote_blocked_prefix
import _3dreport.composeapp.generated.resources.quote_blocked_service_price
import _3dreport.composeapp.generated.resources.quote_error_channel_missing
import _3dreport.composeapp.generated.resources.quote_error_check_values
import _3dreport.composeapp.generated.resources.quote_error_consumable_quantity
import _3dreport.composeapp.generated.resources.quote_error_deductions
import _3dreport.composeapp.generated.resources.quote_error_filament_missing
import _3dreport.composeapp.generated.resources.quote_error_filament_missing_unnamed
import _3dreport.composeapp.generated.resources.quote_error_labor
import _3dreport.composeapp.generated.resources.quote_error_length
import _3dreport.composeapp.generated.resources.quote_error_print_time
import _3dreport.composeapp.generated.resources.quote_error_printer_missing
import _3dreport.composeapp.generated.resources.quote_error_printer_missing_unnamed
import _3dreport.composeapp.generated.resources.quote_error_quantity
import _3dreport.composeapp.generated.resources.quote_error_runs
import _3dreport.composeapp.generated.resources.quote_error_service_minutes
import _3dreport.composeapp.generated.resources.quote_error_service_value
import _3dreport.composeapp.generated.resources.quote_error_shipping_invalid
import _3dreport.composeapp.generated.resources.quote_error_shipping_negative
import _3dreport.composeapp.generated.resources.quote_error_target_below_extras
import _3dreport.composeapp.generated.resources.quote_error_target_invalid
import _3dreport.composeapp.generated.resources.quote_error_target_negative
import _3dreport.composeapp.generated.resources.quote_error_weight
import _3dreport.composeapp.generated.resources.quote_notice_print_removed
import _3dreport.composeapp.generated.resources.quote_photo_unreadable
import com.threedreport.app.AppLog
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.StockRepository
import com.threedreport.app.data.attachmentExtension
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickFailure
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.ThreeMfThumbnail
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.isModelFileName
import com.threedreport.app.platform.readThreeMfThumbnail
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.components.matchingClients
import com.threedreport.app.ui.filaments.labelOrNull
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.displayName
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseDurationMinutes
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toDurationInputText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.history.ClientExport
import com.threedreport.app.ui.history.QuoteExporter
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.text
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.ModelFileKind
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovement
import com.threedreport.core.pricing.OrderPricing
import com.threedreport.core.pricing.OrderTerms
import com.threedreport.core.pricing.PricingError
import com.threedreport.core.report.PrintQueueReport
import com.threedreport.core.report.PrinterQueueEntry
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockShortage
import com.threedreport.core.slicer.GCodeMetadataParser
import com.threedreport.core.stl.StlPreviewResult
import com.threedreport.core.stl.StlProgress
import com.threedreport.core.stl.prepareStlPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import kotlin.math.abs

/**
 * ViewModel da tela de Orçamento. Expõe um [uiState] só (ver [QuoteUiState]) e os [notices].
 *
 * Os cadastros vêm direto dos repositórios compartilhados, então qualquer cadastro/edição feito nas outras
 * telas chega em [QuoteUiState.catalog] na hora. [QuoteUiState.result] é a conta da tela e do Ctrl+S, uma fonte só.
 *
 * O trabalho pesado (ler G-code, analisar STL) roda em [background] e volta pra [main]; os testes
 * passam o padrão, `Unconfined`, e tudo acontece na hora.
 *
 * A importação de G-code ([GCodeImporter]) e o caminho de um orçamento salvo de volta pra tela
 * ([SavedQuoteMapper]) moram em arquivos próprios (decisão 108).
 */
class QuoteViewModel(
    private val filamentRepository: FilamentRepository,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository,
    private val salesChannelRepository: SalesChannelRepository,
    private val historyRepository: QuoteHistoryRepository,
    /** Cadastro de clientes (decisão 106): o cliente do pedido é achado ou criado ao salvar. */
    private val clientRepository: ClientRepository? = null,
    /** Moeda dos orçamentos novos (decisão 106), gravada no orçamento ao salvar. */
    private val currency: () -> Currency = { Currency.BRL },
    private val platform: PlatformServices = defaultPlatform,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    private val background: CoroutineDispatcher = Dispatchers.Unconfined,
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
    /** Cadastro de insumos (decisão 122). Sem ele, a seção de insumos não aparece. */
    private val consumableRepository: ConsumableRepository? = null,
    /** Marca e opções do PDF e da imagem, pro "Enviar pro cliente" (decisão 131). Sem ele, enviar só salva. */
    brandingRepository: BrandingRepository? = null,
    /** Hoje, em dias desde 01/01/1970: o prazo vencido é medido contra ele (os testes fixam o dia). */
    private val today: () -> Long = ::todayEpochDay,
    /** Prepara a prévia de um STL (pesado: roda em [background]); os testes trocam por uma versão controlável. */
    private val stlPreviewer: (ByteArray, StlProgress) -> StlPreviewResult = { bytes, progress -> prepareStlPreview(bytes, progress) },
    /** Lê a miniatura de um 3MF (roda em [background]). */
    private val threeMfReader: (ByteArray) -> ThreeMfThumbnail = ::readThreeMfThumbnail,
    /** Avisa quando o idioma muda, pra o rascunho passar os números que não são dinheiro pro separador novo. */
    languageChanges: Flow<Any?> = AppLocale.current,
    /** O estoque (Leva 12): com ele, um pedido que usa mais do que sobra livre ganha o aviso em âmbar. Sem ele, nenhum aviso. */
    private val stockRepository: StockRepository? = null,
) {
    private val scope: CoroutineScope = scope

    /** Separador decimal da moeda em uso: os campos de número são lidos e preenchidos com ele, como na tela. */
    private val separator: Char get() = currency().decimalSeparator

    private val state = MutableStateFlow(QuoteUiState(catalog = readCatalog()).withResult())

    /** Tudo o que a tela mostra. */
    val uiState: StateFlow<QuoteUiState> = state.asStateFlow()

    // Aviso emitido com a tela fora de composição (a pessoa foi pra Pedidos) fica guardado e chega quando ela
    // voltar, uma vez só: um canal com o último aviso, e não um fluxo que perde o que ninguém estava ouvindo.
    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)

    /** O retorno de uma ação (remover uma impressão com "Desfazer", PDF salvo), pra barra de avisos. */
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    // A geração do PDF, da imagem e do link do WhatsApp é a mesma de Pedidos (decisão 131).
    private val exporter: QuoteExporter? = brandingRepository?.let { branding ->
        QuoteExporter(platform, branding, historyRepository, scope, background, main) { notice -> noticeChannel.trySend(notice) }
    }

    init {
        exporter?.busy?.onEach { busy -> state.update { it.copy(exporting = busy != null) } }?.launchIn(scope)
    }

    private var pendingImports = 0

    /** O separador com que os números que não são dinheiro do rascunho foram escritos: o do idioma em que a pessoa digitou. */
    private var draftNumberSeparator: Char = AppLocale.decimalSeparator

    init {
        languageChanges.onEach { followLanguage() }.launchIn(scope)
    }

    /**
     * O idioma mudou com o rascunho aberto (decisão 153): os números que não são dinheiro passam pro separador novo, senão
     * "1,250" g escrito em inglês seria lido como 1,25 g em português. O retrato do orçamento reaberto passa junto, com a
     * mesma regra: nada que muda o preço foi mexido, e o preço congelado continua (decisão 108).
     */
    private fun followLanguage() {
        val from = draftNumberSeparator
        val to = AppLocale.decimalSeparator
        if (from == to) return
        draftNumberSeparator = to
        removedPrint = removedPrint?.let { (index, print) -> index to print.withNumberSeparator(from, to) }
        change { current ->
            val operation = current.form.operation
            current.copy(
                input = current.input.withNumberSeparator(from, to),
                form = if (operation is QuoteOperation.Editing) {
                    current.form.copy(operation = operation.copy(originalInput = operation.originalInput.withNumberSeparator(from, to)))
                } else {
                    current.form
                },
            )
        }
    }

    init {
        // Um cadastro mexido em outra tela muda a conta desta: o estado acompanha os repositórios. Um `combine`
        // só: com um fluxo por cadastro, cada valor inicial refazia a conta (oito vezes ao abrir a tela).
        combine(
            listOf(
                filamentRepository.filaments.map { },
                printerRepository.printers.map { },
                settingsRepository.settings.map { },
                serviceRepository.services.map { },
                salesChannelRepository.channels.map { },
                historyRepository.savedQuotes.map { },
                (consumableRepository?.consumables ?: MutableStateFlow(emptyList<Consumable>())).map { },
                (clientRepository?.clients ?: MutableStateFlow(emptyList<Client>())).map { },
                (stockRepository?.items ?: MutableStateFlow(emptyList<StockItem>())).map { },
                (stockRepository?.movements ?: MutableStateFlow(emptyList<StockMovement>())).map { },
            ),
        ) { }.onEach { change { it } }.launchIn(scope)
    }

    private fun readCatalog() = QuoteCatalog(
        filaments = filamentRepository.filaments.value,
        printers = printerRepository.printers.value,
        services = serviceRepository.services.value,
        consumables = consumableRepository?.consumables?.value.orEmpty(),
        salesChannels = salesChannelRepository.channels.value,
        clients = clientRepository?.clients?.value.orEmpty(),
        settings = settingsRepository.settings.value,
        savedQuotes = historyRepository.savedQuotes.value,
    )

    /** Relê os cadastros e refaz a conta: o resultado do estado nunca fica atrás de quem o gerou. */
    private fun QuoteUiState.withResult(): QuoteUiState {
        val fresh = copy(catalog = readCatalog())
        val result = resultFor(fresh)
        val deadline = fresh.form.deliveryDateEpochDay.takeUnless { fresh.input.isProduct }
        return fresh.copy(
            result = result,
            comparison = comparisonFor(fresh),
            queueHints = queueHintsFor(fresh, result),
            deliveryDaysLate = deliveryDaysLate(deadline, today()),
            suggestRush = shouldSuggestRush(
                fresh.form.deliveryDateEpochDay,
                today(),
                fresh.input.rush,
                fresh.catalog.settings.rushSurchargeRate,
                fresh.input.isProduct,
            ),
            today = today(),
            stockWarnings = stockWarningsFor(fresh, result),
        )
    }

    /**
     * O aviso de estoque (item 13 da Leva 12): os itens controlados que este pedido usa mais do que sobra livre. Só pra pedido,
     * nunca pra produto do catálogo. Ao editar um pedido, ele sai do reservado (não conta duas vezes); e um pedido já Pronto ou
     * Entregue não avisa, porque o que ele gastou já saiu do saldo e a conta ficaria dobrada.
     */
    private fun stockWarningsFor(state: QuoteUiState, result: QuoteResult): List<QuoteStockWarning> {
        val stock = stockRepository ?: return emptyList()
        val quote = result.quote ?: return emptyList()
        if (state.input.isProduct) return emptyList()
        val editingId = state.form.editingQuoteId
        val catalog = state.catalog
        if (editingId != null && catalog.savedQuotes.find { it.id == editingId }?.status in StockReport.PRINTED_STATUSES) return emptyList()
        val snapshot = stockSnapshotFor(stock.items.value, stock.movements.value, catalog.savedQuotes, catalog.filaments)
        val needed = StockReport.consumptionOf(quote, catalog.filaments).filterKeys { it in snapshot.tracked }
        // O pedido em edição sai do reservado: o consumo dele, já guardado, é subtraído (e não conta duas vezes).
        val own = editingId?.let { snapshot.consumptionByOrder[it] }.orEmpty()
        val shortages = needed.mapNotNull { (ref, amount) ->
            val reserved = ((snapshot.reserved[ref] ?: 0.0) - (own[ref] ?: 0.0)).let { if (abs(it) < StockReport.EPSILON) 0.0 else it }
            val available = (snapshot.balances[ref] ?: 0.0) - reserved
            if (amount > available + StockReport.EPSILON) StockShortage(ref, amount, available, reserved) else null
        }
        return shortages.map { shortage ->
            val ref = shortage.ref
            QuoteStockWarning(
                ref = ref,
                name = stockItemName(ref, catalog),
                needed = shortage.needed,
                available = shortage.available,
                reserved = shortage.reserved,
                printIds = if (ref is StockItemRef.ColorRef) {
                    quote.prints.mapIndexedNotNull { index, print ->
                        state.input.prints.getOrNull(index)?.id?.takeIf { print.job.filaments.any { StockReport.colorRefOf(it, catalog.filaments) == ref } }
                    }.toSet()
                } else {
                    emptySet()
                },
            )
        }
    }

    /**
     * O que o aviso de estoque precisa dos pedidos e do estoque, guardado: o reservado de todos os pedidos, o consumo de cada um (pra
     * tirar o que está em edição) e os saldos. Só é refeito quando itens, movimentos, pedidos ou filamentos mudam (a identidade das
     * listas dos repositórios); mexer no orçamento, que é o caso de toda tecla, reaproveita o que já está pronto.
     */
    private class StockSnapshot(
        val items: List<StockItem>,
        val movements: List<StockMovement>,
        val savedQuotes: List<SavedQuote>,
        val filaments: List<Filament>,
        val tracked: Set<StockItemRef>,
        val balances: Map<StockItemRef, Double>,
        val reserved: Map<StockItemRef, Double>,
        val consumptionByOrder: Map<String, Map<StockItemRef, Double>>,
    )

    private var stockSnapshot: StockSnapshot? = null

    /** Quantas vezes o [StockSnapshot] foi refeito: o teste confere que digitar não recalcula o reservado. */
    internal var stockSnapshotBuilds: Int = 0
        private set

    private fun stockSnapshotFor(items: List<StockItem>, movements: List<StockMovement>, savedQuotes: List<SavedQuote>, filaments: List<Filament>): StockSnapshot {
        stockSnapshot?.takeIf { it.items === items && it.movements === movements && it.savedQuotes === savedQuotes && it.filaments === filaments }?.let { return it }
        stockSnapshotBuilds++
        val consumptionByOrder = StockReport.reservingOrders(savedQuotes).associate { it.id to StockReport.consumptionOf(it, filaments) }
        val reserved = HashMap<StockItemRef, Double>()
        consumptionByOrder.values.forEach { consumption -> consumption.forEach { (ref, amount) -> reserved[ref] = (reserved[ref] ?: 0.0) + amount } }
        return StockSnapshot(items, movements, savedQuotes, filaments, items.mapTo(HashSet()) { it.ref }, StockReport.balances(movements), reserved, consumptionByOrder)
            .also { stockSnapshot = it }
    }

    /** "PLA Voolt Vermelho" ou "Argola de chaveiro": o nome do item de estoque, como o vendedor o conhece. */
    private fun stockItemName(ref: StockItemRef, catalog: QuoteCatalog): String = when (ref) {
        is StockItemRef.ColorRef -> {
            val filament = catalog.filaments.find { it.id == ref.filamentId }
            val color = filament?.colors?.find { it.id == ref.colorId }
            listOfNotNull(filament?.name, color?.labelOrNull()).joinToString(" ")
        }

        is StockItemRef.ConsumableRef -> catalog.consumables.find { it.id == ref.consumableId }?.name.orEmpty()
    }

    /** "Em qual impressora sai mais barato" (ver [comparePrinters]), pro painel: feito aqui, e não na tela. */
    private fun comparisonFor(state: QuoteUiState): List<Pair<PrinterProfile, Quote>> = with(state.catalog) {
        comparePrinters(filaments, printers, settings, services, state.input, salesChannels, consumables)
    }

    /** Uma dica de fila por impressora que o pedido usa: cada uma tem a sua. Nada em produto nem sem conta. */
    private fun queueHintsFor(state: QuoteUiState, result: QuoteResult): List<QueueHint> {
        val quote = result.quote
        if (quote == null || state.input.isProduct) return emptyList()
        return quote.printerIds.mapNotNull { printerId ->
            val printer = state.catalog.printers.firstOrNull { it.id == printerId } ?: return@mapNotNull null
            queueAheadOf(printer, state.catalog.savedQuotes, state.form.editingQuoteId)?.let { queue ->
                QueueHint(printer.name, queue.queuedMinutes, queue.queuedQuoteCount, quote.printMinutesOn(printerId))
            }
        }
    }

    /** Muda o estado e refaz a conta. */
    private fun change(transform: (QuoteUiState) -> QuoteUiState) = state.update { transform(it).withResult() }

    private fun updateInput(transform: (QuoteInputState) -> QuoteInputState) = change { it.copy(input = transform(it.input)) }

    private fun updateForm(transform: (SaveQuoteFormState) -> SaveQuoteFormState) = change { it.copy(form = transform(it.form)) }

    // Os cadastros lidos na hora, direto dos repositórios, pra uma mudança de outra tela valer já no próximo toque.
    private val filaments: List<Filament> get() = filamentRepository.filaments.value
    private val printers: List<PrinterProfile> get() = printerRepository.printers.value
    private val services: List<Service> get() = serviceRepository.services.value
    private val settings: PricingSettings get() = settingsRepository.settings.value
    private val salesChannels: List<SalesChannel> get() = salesChannelRepository.channels.value
    private val consumables: List<Consumable> get() = consumableRepository?.consumables?.value.orEmpty()

    private var lastId = 0

    private fun newId(): Int = ++lastId

    /**
     * Função pura: o que está na frente de uma peça nova em [printer] — pedidos aprovados ou em
     * impressão (ver `PrintQueueReport`) —, ou `null` se a fila estiver vazia. Serve de dica ao
     * escolher o prazo e nunca preenche a data sozinha: o app sabe o tempo de máquina, mas não sabe
     * quantas horas por dia a impressora roda nem quanto tempo o acabamento leva.
     *
     * O orçamento sendo editado ([editingQuoteId]) fica de fora, senão ele estaria na frente de si mesmo.
     */
    fun queueAheadOf(printer: PrinterProfile, savedQuotes: List<SavedQuote>, editingQuoteId: String?): PrinterQueueEntry? =
        PrintQueueReport.summarize(
            printers = listOf(printer),
            savedQuotes = savedQuotes.filterNot { it.id == editingQuoteId },
            statuses = setOf(OrderStatus.APROVADO, OrderStatus.EM_IMPRESSAO),
        ).single().takeIf { it.queuedQuoteCount > 0 }

    // Toda função que mexe numa impressão ou numa linha recebe o id dela (decisão 114); `null` é a primeira,
    // que é o caso de sempre com uma impressão só.

    fun selectFilament(id: String, printId: Int? = null, rowId: Int? = null) = updateFilament(printId, rowId) { row ->
        val changed = row.copy(filamentId = id, colorId = null, missingFilamentName = null)
        // Quem digitou em gramas continua com o mesmo peso: os metros mudam com a densidade do filamento novo.
        if (row.weightText != null) changed.withWeight(row.weightText, filaments.find { it.id == id }) else changed
    }

    fun selectFilamentColor(id: String, printId: Int? = null, rowId: Int? = null) = updateFilament(printId, rowId) { it.copy(colorId = id) }

    fun selectPrinter(id: String, printId: Int? = null) = updatePrint(printId) { it.copy(printerId = id, missingPrinterName = null) }

    fun setLengthMeters(text: String, printId: Int? = null, rowId: Int? = null) =
        updateFilament(printId, rowId) { it.copy(lengthText = text, weightText = null) }

    /**
     * Peso em gramas (decisão 107): quem vende pensa "essa peça gasta 42 g", e o fatiador mostra isso.
     * Cada mudança vira metros pelo filamento da linha (densidade e diâmetro), que é o que vale pra conta.
     */
    fun setWeightGrams(text: String, printId: Int? = null, rowId: Int? = null) {
        val printIndex = printIndexOf(printId) ?: return
        val rowIndex = rowIndexOf(state.value.input.prints[printIndex], rowId) ?: return
        // O estado já tem a conta de agora (cada mudança refaz): calcular de novo dobraria o trabalho a cada tecla.
        val filament = state.value.result.prints.getOrNull(printIndex)?.filaments?.getOrNull(rowIndex)?.filament
        updateFilament(printId, rowId) { it.withWeight(text, filament) }
    }

    private fun FilamentInput.withWeight(text: String, filament: Filament?): FilamentInput {
        val grams = parseDecimal(text, NumberKind.AMOUNT, AppLocale.decimalSeparator)
        val meters = when {
            text.isBlank() -> ""

            grams == null || grams < 0 || filament == null -> lengthText

            // Duas casas, como o G-code: com mais, o campo mostrava "16,764181" pra 50 g.
            else -> GCodeImporter.formatLength(filament.lengthMeters(grams), AppLocale.decimalSeparator)
        }
        return copy(weightText = text, lengthText = meters)
    }

    fun setPrintTimeMinutes(text: String, printId: Int? = null) = updatePrint(printId) { it.copy(printTimeText = text) }

    /** Nome da impressão ("Cabeça"), de uso interno (decisão 114). */
    fun setPrintName(text: String, printId: Int? = null) = updatePrint(printId) { it.copy(name = text) }

    /** "× N vezes": quantas vezes a mesma mesa roda no pedido; vazio conta como 1. */
    fun setRuns(text: String, printId: Int? = null) = updatePrint(printId) { it.copy(runsText = text) }

    /**
     * "+ Adicionar filamento" (decisão 105). A linha nova já vem com o filamento da última, porque
     * numa peça multicolor o mais comum é o mesmo filamento em outra cor; a cor e os metros ficam
     * pra escolher.
     */
    fun addFilament(printId: Int? = null) = updatePrint(printId) { current ->
        val rows = current.withDefaultFilamentChosen().filaments
        current.copy(filaments = rows + FilamentInput(filamentId = rows.last().filamentId, id = newId()))
    }

    /**
     * Com uma linha só, nada escolhido aparece como o primeiro em estoque (ver [calculate]). Com duas, essa
     * regra não vale mais, então o que a tela mostrava vira escolha de verdade; senão a linha 1 trocaria
     * sozinha pra "Escolha o filamento".
     */
    private fun PrintInput.withDefaultFilamentChosen(): PrintInput {
        if (filaments.size != 1 || filaments.single().filamentId != null) return this
        val inStock = this@QuoteViewModel.filaments.firstOrNull { it.hasStockAvailable && !it.archived } ?: return this
        return copy(filaments = listOf(filaments.single().copy(filamentId = inStock.id)))
    }

    /** Remove uma linha de filamento; a última que sobra não sai, porque impressão sem filamento não existe. */
    fun removeFilament(rowId: Int, printId: Int? = null) = updatePrint(printId) { current ->
        if (current.filaments.size <= 1) current else current.copy(filaments = current.filaments.filterNot { it.id == rowId })
    }

    /**
     * "Adicionar outra impressão" (decisão 114): a nova já vem com a impressora e os filamentos da última,
     * que é o mais comum (a mesma máquina e o mesmo material em outra mesa); metros e tempo ficam em
     * branco. Devolve o id da impressão nova.
     */
    fun addPrint(): Int {
        val id = newId()
        updateInput { input ->
            val last = input.prints.last()
            val printerId = last.printerId ?: printers.firstOrNull { !it.archived }?.id
            val rows = last.withDefaultFilamentChosen().filaments.map { FilamentInput(filamentId = it.filamentId, colorId = it.colorId, id = newId()) }
            input.copy(prints = input.prints + PrintInput(printerId = printerId, filaments = rows, id = id))
        }
        return id
    }

    /** Uma cópia da impressão logo depois dela, com tudo preenchido (duas mesas iguais em impressoras diferentes). */
    fun duplicatePrint(printId: Int) = updateInput { input ->
        val index = input.prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 } ?: return@updateInput input
        val original = input.prints[index]
        val copy = original.copy(
            filaments = original.filaments.map { it.copy(id = newId()) },
            beforeGCode = null,
            gcodeImport = null,
            createdByImport = false,
            id = newId(),
        )
        input.copy(prints = input.prints.take(index + 1) + copy + input.prints.drop(index + 1))
    }

    /** Recolhe ou expande o cartão da impressão [printId] (ver [QuoteUiState.collapsedPrintIds]). */
    fun toggleCollapsed(printId: Int) = state.update {
        it.copy(collapsedPrintIds = if (printId in it.collapsedPrintIds) it.collapsedPrintIds - printId else it.collapsedPrintIds + printId)
    }

    /** A impressão removida por último e onde ela estava, pro "Desfazer" do aviso. */
    private var removedPrint: Pair<Int, PrintInput>? = null

    /**
     * Remove a impressão [printId], com "Desfazer" no aviso. A última não sai: pedido sem impressão não
     * existe (sobra a tela de sempre).
     */
    fun removePrint(printId: Int) {
        val prints = state.value.input.prints
        val index = prints.indexOfFirst { it.id == printId }
        if (index < 0 || prints.size <= 1) return
        removedPrint = index to prints[index]
        // A foto é do pedido: remover a impressão de onde a miniatura veio não tira a foto (e o Desfazer não
        // teria como devolvê-la).
        updateInput { it.copy(prints = it.prints.filterNot { print -> print.id == printId }) }
        if (photoGCodePrintId == printId) {
            photoGCodePrintId = null
            photoBeforeGCode = null
        }
        notify(uiText(Res.string.quote_notice_print_removed, index + 1), uiText(Res.string.quote_action_undo), ::restoreRemovedPrint)
    }

    private fun restoreRemovedPrint() {
        val (index, print) = removedPrint ?: return
        removedPrint = null
        updateInput { input -> input.copy(prints = input.prints.take(index) + print + input.prints.drop(index)) }
    }

    private fun printIndexOf(printId: Int?): Int? {
        val prints = state.value.input.prints
        return if (printId == null) 0 else prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 }
    }

    private fun rowIndexOf(print: PrintInput, rowId: Int?): Int? =
        if (rowId == null) 0 else print.filaments.indexOfFirst { it.id == rowId }.takeIf { it >= 0 }

    private fun updatePrint(printId: Int?, transform: (PrintInput) -> PrintInput) = updateInput { input ->
        val index = if (printId == null) 0 else input.prints.indexOfFirst { it.id == printId }
        if (index !in input.prints.indices) input else input.copy(prints = input.prints.replaced(index, transform))
    }

    private fun updateFilament(printId: Int?, rowId: Int?, transform: (FilamentInput) -> FilamentInput) = updatePrint(printId) { current ->
        val index = rowIndexOf(current, rowId)
        if (index == null) current else current.copy(filaments = current.filaments.replaced(index, transform))
    }

    fun setLaborMinutes(text: String) = updateInput { it.copy(laborMinutesText = text) }
    fun setQuantity(text: String) = updateInput { it.copy(quantityText = text) }

    /**
     * Trocar entre pedido e produto limpa o preço fechado: com cliente ele é o total combinado,
     * frete incluso, e em produto é o preço anunciado. Levar um pro outro mudaria o preço em silêncio.
     */
    fun setKind(kind: QuoteKind) = change { current ->
        if (current.input.kind == kind) {
            current
        } else {
            current.copy(
                input = current.input.copy(kind = kind, targetTotalText = "", announcedUnitPrice = null),
                // O pedido que "Enviar pro cliente" gravou fica como está: o produto é outra coisa e, salvando, nasce um novo.
                form = current.form.copy(sent = null),
                negotiating = false,
            )
        }
    }

    /** Abre ou fecha o campo "Preço combinado" do painel (ver [QuoteUiState.negotiating]). */
    fun setNegotiating(open: Boolean) = state.update { it.copy(negotiating = open) }

    /** Abre o seletor de arquivo e importa o G-code escolhido na impressão [printId] (ver [importGCode]). */
    fun pickAndImportGCode(printId: Int? = null) = importDropped(platform.pickFile(FileKind.GCODE), printId)

    /**
     * Um arquivo arrastado pra janela (ver `fileDropTarget`) ou escolhido: G-code é importado; outro tipo,
     * ou um arquivo que não deu pra ler, vira a mensagem do bloco de importação.
     */
    fun importDropped(result: PickResult, printId: Int? = null) {
        when (result) {
            is PickResult.Picked -> importGCode(result.file, printId)
            is PickResult.Failed -> updatePrint(printId) { it.copy(gcodeImport = GCodeImportResult.PickFailed(result)) }
            PickResult.Cancelled -> Unit
            is PickResult.Deferred -> Unit
        }
    }

    /**
     * Vários arquivos soltos de uma vez (decisão 114): cada um vira uma impressão, na ordem. O primeiro vai
     * pra impressão que ainda está em branco, se houver só ela; o resto abre impressões novas.
     */
    fun importDroppedFiles(results: List<PickResult>) {
        val files = results.filterNot { it == PickResult.Cancelled }
        // Um arquivo só num pedido de uma impressão continua substituindo ela, como sempre foi.
        val replaceSingle = files.size == 1 && state.value.input.prints.size == 1
        files.forEachIndexed { index, result ->
            val onlyBlank = index == 0 && state.value.input.prints.singleOrNull()?.isBlank == true
            if (onlyBlank || replaceSingle) importDropped(result) else importDroppedAsNewPrint(result)
        }
    }

    /** Arquivos soltos em cima de "Substituir a impressão N": o primeiro vai pra ela, o resto abre impressões novas. */
    fun importDroppedInto(printId: Int, results: List<PickResult>) {
        val files = results.filterNot { it == PickResult.Cancelled }
        files.firstOrNull()?.let { importDropped(it, printId) }
        files.drop(1).forEach(::importDroppedAsNewPrint)
    }

    /** Escolhe um G-code pelo seletor e abre uma impressão nova pra ele. */
    fun pickGCodeAsNewPrint() = importDroppedAsNewPrint(platform.pickFile(FileKind.GCODE))

    /**
     * "Adicionar como nova impressão": o G-code abre uma impressão só dele. O que não é G-code não abre
     * impressão nenhuma: a explicação vai pra última.
     */
    fun importDroppedAsNewPrint(result: PickResult) {
        if (result == PickResult.Cancelled) return
        val notGCode: GCodeImportResult? = when (result) {
            is PickResult.Failed -> GCodeImportResult.PickFailed(result)
            is PickResult.Picked -> GCodeImporter.unsupported(result.file.fileName)
            PickResult.Cancelled -> null
            is PickResult.Deferred -> null
        }
        if (notGCode != null) {
            updatePrint(state.value.input.prints.last().id) { it.copy(gcodeImport = notGCode) }
            return
        }
        val id = addPrint()
        updatePrint(id) { it.copy(createdByImport = true) }
        importDropped(result, id)
    }

    /**
     * Monta a impressão [printId] a partir de um G-code, escolhido pelo botão ou arrastado pra janela
     * (decisão 89), pelo [GCodeImporter]. A leitura roda em [background] (um G-code tem dezenas de
     * megabytes) enquanto [importing] avisa a tela; a impressão é achada de novo pelo id quando a leitura
     * termina, porque a lista pode ter mudado nesse meio-tempo. A foto do pedido vem da miniatura do
     * arquivo quando não há foto escolhida à mão.
     *
     * Tudo continua editável depois — é um atalho pra preencher, não uma trava. [undoGCodeImport]
     * desfaz de uma vez, inclusive a impressora e o filamento que estavam escolhidos antes.
     */
    fun importGCode(file: PickedFile, printId: Int? = null) {
        val id = state.value.input.prints.getOrNull(printIndexOf(printId) ?: return)?.id ?: return
        GCodeImporter.unsupported(file.fileName)?.let { unsupported ->
            updatePrint(id) { it.copy(gcodeImport = unsupported) }
            return
        }
        state.update { it.copy(importing = true) }
        pendingImports++
        scope.launch(background) {
            val metadata = runCatching { GCodeMetadataParser.parse(file.bytes.decodeToString()) }
                .onFailure { AppLog.warn("Falha ao ler o G-code ${file.fileName}", it) }
                .getOrNull()
            withContext(main) {
                // Vários G-codes soltos juntos são lidos ao mesmo tempo: "Lendo…" só sai depois do último.
                pendingImports--
                state.update { it.copy(importing = pendingImports > 0) }
                if (metadata == null) {
                    updatePrint(id) { it.copy(gcodeImport = GCodeImportResult.Unreadable) }
                } else {
                    applyGCode(metadata, id)
                }
            }
        }
    }

    /** A foto do pedido antes de uma miniatura de G-code entrar no lugar dela, e de qual impressão veio. */
    private var photoBeforeGCode: SaveQuoteFormState? = null
    private var photoGCodePrintId: Int? = null

    private fun applyGCode(metadata: com.threedreport.core.slicer.GCodeMetadata, printId: Int) {
        val index = state.value.input.prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 } ?: return
        val current = state.value.input.prints[index]
        val form = state.value.form
        val thumbnail = metadata.thumbnail
        // A foto do pedido é da primeira impressão (ou de quem chegou primeiro sem foto nenhuma): a miniatura
        // da mesa 3 não troca a foto que veio da mesa 1.
        val photoApplied = thumbnail != null &&
            (form.photo == null || (form.photoAutomatic && (photoGCodePrintId == printId || index == 0)))
        // O G-code só escolhe entre o que não está arquivado (decisão 115).
        val imported = GCodeImporter.apply(
            current, metadata, filaments.filterNot { it.archived }, printers.filterNot { it.archived }, photoApplied, ::newId, AppLocale.decimalSeparator,
        )

        if (photoApplied) {
            if (photoBeforeGCode == null) photoBeforeGCode = form
            photoGCodePrintId = printId
            updateForm {
                it.copy(photo = imported.thumbnail, photoAutomatic = true, savedConfirmation = false)
            }
        }
        updatePrint(printId) { imported }
    }

    /**
     * Desfaz a importação de G-code da impressão [printId]: devolve a impressão exatamente como estava
     * antes da primeira importação (impressora, linhas de filamento, comprimentos, tempo, configurações e
     * miniatura), e a foto do pedido, se ela veio desta impressão (uma foto escolhida à mão depois fica).
     * Uma impressão que nasceu do próprio G-code sai.
     */
    fun undoGCodeImport(printId: Int? = null) {
        val print = state.value.input.prints.getOrNull(printIndexOf(printId) ?: return) ?: return
        if (print.createdByImport && state.value.input.prints.size > 1) {
            updateInput { it.copy(prints = it.prints.filterNot { p -> p.id == print.id }) }
        } else {
            updatePrint(print.id) { it.beforeGCode ?: it.copy(gcodeImport = null) }
        }
        if (photoGCodePrintId == print.id) undoGCodePhoto()
    }

    private fun undoGCodePhoto() {
        val before = photoBeforeGCode
        updateForm {
            if (!it.photoAutomatic) {
                it
            } else {
                it.copy(photo = before?.photo, photoAutomatic = before?.photoAutomatic == true, savedConfirmation = false)
            }
        }
        photoBeforeGCode = null
        photoGCodePrintId = null
    }

    /** Substitui as configurações de impressão da impressão [printId] (ver [PrintSettings]). */
    fun setPrintSettings(printSettings: PrintSettings, printId: Int? = null) = updatePrint(printId) { it.copy(settings = printSettings) }

    private fun notify(message: UiText, actionLabel: UiText? = null, action: (() -> Unit)? = null) {
        noticeChannel.trySend(UiNotice(message, actionLabel, action))
    }

    /**
     * Marca ou desmarca o serviço [id]. Ao marcar, o valor vem preenchido com o sugerido do
     * catálogo, ou, sem ele, com o tempo do serviço pago pela sua hora (decisão 123), ou vazio pra
     * digitar; a forma de cobrança e o tempo vêm do cadastro.
     */
    fun toggleService(id: String) = updateInput { input ->
        if (id in input.selectedServices) return@updateInput input.copy(selectedServices = input.selectedServices - id)
        val service = services.find { it.id == id } ?: return@updateInput input
        val serviceInput = ServiceInput(
            name = service.name,
            priceText = service.suggestedPriceFor(settings.laborRatePerHour)?.toInputText(separator).orEmpty(),
            chargedPerOrder = service.chargedPerOrder,
            laborMinutesText = service.laborMinutes?.takeIf { it > 0 }?.toDurationInputText().orEmpty(),
        )
        input.copy(selectedServices = input.selectedServices + (id to serviceInput))
    }

    fun setServicePrice(id: String, text: String) = updateServiceInput(id) { it.copy(priceText = text) }

    fun setServiceLaborMinutes(id: String, text: String) = updateServiceInput(id) { it.copy(laborMinutesText = text) }

    /** Marca ou desmarca o insumo [id] (decisão 122): marcado, entra com 1 unidade e a forma do cadastro. */
    fun toggleConsumable(id: String) = updateInput { input ->
        if (id in input.selectedConsumables) return@updateInput input.copy(selectedConsumables = input.selectedConsumables - id)
        val consumable = consumables.find { it.id == id } ?: return@updateInput input
        val consumableInput = ConsumableInput(
            name = consumable.name,
            unitCost = consumable.unitCost,
            chargedPerOrder = consumable.chargedPerOrder,
        )
        input.copy(selectedConsumables = input.selectedConsumables + (id to consumableInput))
    }

    fun setConsumableQuantity(id: String, text: String) = updateConsumableInput(id) { it.copy(quantityText = text) }

    fun setConsumableChargedPerOrder(id: String, chargedPerOrder: Boolean) =
        updateConsumableInput(id) { it.copy(chargedPerOrder = chargedPerOrder) }

    private fun updateConsumableInput(id: String, transform: (ConsumableInput) -> ConsumableInput) = updateInput { input ->
        val current = input.selectedConsumables[id] ?: return@updateInput input
        input.copy(selectedConsumables = input.selectedConsumables + (id to transform(current)))
    }

    /** Pedido urgente (decisão 125). */
    fun setRush(rush: Boolean) = updateInput { it.copy(rush = rush) }

    /** Frete grátis pro cliente, pago por você (decisão 124). */
    fun setShippingAbsorbed(absorbed: Boolean) = updateInput { it.copy(shippingAbsorbed = absorbed) }

    /**
     * "Pra manter a margem, cobre R$ X" do frete grátis (decisão 124): põe no preço fechado o total que o
     * cliente pagaria com a peça que cobre o frete. Não faz nada sem frete grátis na conta.
     */
    fun useShippingCoveringPrice() {
        val result = freshResult()
        val covering = result.quote?.shippingCoveringSalePrice ?: return
        setTargetTotal((covering + result.servicesTotal).toInputText(separator))
    }

    fun setServiceChargedPerOrder(id: String, chargedPerOrder: Boolean) =
        updateServiceInput(id) { it.copy(chargedPerOrder = chargedPerOrder) }

    private fun updateServiceInput(id: String, transform: (ServiceInput) -> ServiceInput) = updateInput { input ->
        val current = input.selectedServices[id] ?: return@updateInput input
        input.copy(selectedServices = input.selectedServices + (id to transform(current)))
    }

    fun selectSalesChannel(id: String?) = updateInput { it.copy(salesChannelId = id, missingChannelName = null) }
    fun setShippingCost(text: String) = updateInput { it.copy(shippingCostText = text) }
    fun setTargetTotal(text: String) = updateInput { it.copy(targetTotalText = text) }

    fun applyShowcasePrice(value: Double) = updateInput { it.copy(targetTotalText = value.toInputText(separator)) }

    fun setSaveName(text: String) = updateForm { it.copy(name = text, savedConfirmation = false) }
    fun setSourceLink(text: String) = updateForm { it.copy(sourceLink = text, savedConfirmation = false) }

    /** Digitar o nome desvincula do cliente escolhido na sugestão: pode ser outra pessoa. */
    fun setClientName(text: String) = updateForm { it.copy(clientName = text, clientId = null, savedConfirmation = false) }
    fun setClientContact(text: String) = updateForm { it.copy(clientContact = text, savedConfirmation = false) }

    /** Escolhe um cliente do cadastro na sugestão: nome e contato vêm dele. */
    fun chooseClient(client: Client) = updateForm {
        it.copy(clientName = client.name, clientContact = client.contact.orEmpty(), clientId = client.id, savedConfirmation = false)
    }

    /** Clientes do cadastro que batem com o que foi digitado, pra sugerir (até cinco). */
    fun clientSuggestions(typed: String, clients: List<Client>): List<Client> = matchingClients(typed, clients)

    fun setCategory(text: String) = updateForm { it.copy(category = text, savedConfirmation = false) }
    fun setDeliveryDate(epochDay: Long?) = updateForm { it.copy(deliveryDateEpochDay = epochDay, savedConfirmation = false) }
    fun clearPhoto() = updateForm { it.copy(photo = null, photoAutomatic = false, photoError = null, savedConfirmation = false) }

    /**
     * Escolhe a foto e confere na hora se ela abre: descobrir que a imagem não abre só ao mandar o PDF
     * pro cliente seria tarde demais (e uma imagem que não abre derrubava a tela do Histórico).
     */
    fun pickPhoto() {
        when (val picked = platform.pickFile(FileKind.IMAGE)) {
            is PickResult.Picked -> {
                val readable = runCatching { decodeImageBitmap(picked.file.bytes) }.isSuccess
                updateForm {
                    if (readable) {
                        it.copy(photo = picked.file, photoAutomatic = false, photoError = null, savedConfirmation = false)
                    } else {
                        it.copy(photoError = uiText(Res.string.quote_photo_unreadable))
                    }
                }
            }

            is PickResult.Failed -> updateForm { it.copy(photoError = picked.text()) }

            PickResult.Cancelled -> Unit

            is PickResult.Deferred -> Unit
        }
    }

    /** Tira o arquivo do modelo e cancela a leitura da prévia, se ainda estiver rodando. */
    fun clearModelFile() {
        updateForm { it.copy(modelFile = null, savedConfirmation = false) }
        loadModelPreview(null)
    }

    /**
     * Anexa o arquivo do modelo (STL ou 3MF) ao orçamento, guardado pra a pessoa baixar de novo em Pedidos ou no
     * Catálogo e reaproveitar numa venda futura da mesma peça. Um arquivo só: escolher outro troca o anterior.
     */
    fun pickModelFile() {
        when (val picked = platform.pickFile(FileKind.MODEL)) {
            is PickResult.Picked -> attachModelFile(picked.file)
            is PickResult.Failed -> notify(picked.text())
            PickResult.Cancelled -> Unit
            is PickResult.Deferred -> Unit
        }
    }

    /**
     * Um arquivo solto na janela: se é STL ou 3MF, vira o arquivo do modelo; o resto não é com este ViewModel. Um
     * arquivo ainda por ler ([PickResult.Deferred]) é lido em [background], com [QuoteUiState.readingModelName] já
     * mostrando o carregamento, e só então anexado.
     */
    fun attachDroppedModel(result: PickResult) {
        when (result) {
            is PickResult.Picked -> attachModelFile(result.file)
            is PickResult.Failed -> notify(result.text())
            PickResult.Cancelled -> Unit
            is PickResult.Deferred -> readDroppedModel(result)
        }
    }

    private var readingJob: Job? = null

    private fun readDroppedModel(deferred: PickResult.Deferred) {
        readingJob?.cancel()
        state.update { it.copy(readingModelName = deferred.fileName) }
        val job = scope.launch(background, start = CoroutineStart.LAZY) {
            val read = try {
                deferred.read()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                AppLog.warn("Não consegui ler ${deferred.fileName}", e)
                PickResult.Failed(PickFailure.UNREADABLE, deferred.fileName)
            }
            withContext(main) {
                readingJob = null
                state.update { it.copy(readingModelName = null) }
                attachDroppedModel(read)
            }
        }
        readingJob = job
        job.start()
    }

    /**
     * Guarda [file] como o arquivo do modelo e começa a ler a prévia. Num 3MF, a miniatura vira a foto do orçamento
     * pela mesma regra da miniatura do G-code: só se ainda não há foto, ou se a foto atual também veio de uma
     * miniatura automática.
     */
    fun attachModelFile(file: PickedFile) {
        if (!isModelFileName(file.fileName)) return
        updateForm { it.copy(modelFile = file, savedConfirmation = false) }
        loadModelPreview(file, applyPhoto = true)
    }

    /** "Usar como foto do orçamento": a miniatura do 3MF vira a foto, e a regra automática deixa de valer. */
    fun useModelThumbnailAsPhoto() {
        val preview = state.value.modelPreview as? ModelPreview.ThreeMf ?: return
        updateForm { it.copy(photo = preview.thumbnail, photoAutomatic = false, photoError = null, savedConfirmation = false) }
    }

    /** Usa uma captura do visualizador 3D (`Stl3DViewerState.captureSnapshot`) como foto do orçamento. */
    fun setPhotoFromStlSnapshot(pngBytes: ByteArray) {
        updateForm {
            it.copy(photo = PickedFile("captura_stl.png", pngBytes), photoAutomatic = false, photoError = null, savedConfirmation = false)
        }
    }

    /** A leitura da prévia em andamento; trocar ou tirar o arquivo a cancela. */
    private var previewJob: Job? = null

    /**
     * Lê a prévia do arquivo do modelo fora da thread da tela: um STL de milhões de triângulos leva segundos, e um
     * 3MF é um pacote ZIP. O progresso do STL (0 a 1) chega em [ModelPreview.Loading]. Trocar ou tirar o arquivo no
     * meio cancela o trabalho: o progresso é o ponto onde o STL para. [applyPhoto] só é verdade quando a pessoa
     * acabou de anexar o arquivo; abrir um orçamento salvo não mexe na foto dele.
     */
    private fun loadModelPreview(file: PickedFile?, applyPhoto: Boolean = false) {
        cancelPreview()
        if (file == null) {
            state.update { it.copy(modelPreview = null) }
            return
        }
        state.update { it.copy(modelPreview = ModelPreview.Loading()) }
        // Preguiçoso: o trabalho só começa depois de o job estar guardado, pra trocar o arquivo sempre achar quem cancelar.
        val job = scope.launch(background, start = CoroutineStart.LAZY) {
            val preview = when (ModelFileKind.of(file.fileName)) {
                ModelFileKind.THREE_MF -> threeMfPreview(file)
                else -> stlPreview(file)
            }
            withContext(main) {
                // Outro arquivo pode ter sido escolhido enquanto este era lido.
                if (state.value.form.modelFile !== file) return@withContext
                state.update { it.copy(modelPreview = preview) }
                if (applyPhoto && preview is ModelPreview.ThreeMf) applyThreeMfPhoto(preview.thumbnail)
            }
        }
        previewJob = job
        job.start()
    }

    private fun cancelPreview() {
        readingJob?.cancel()
        readingJob = null
        state.update { if (it.readingModelName != null) it.copy(readingModelName = null) else it }
        previewJob?.cancel()
        previewJob = null
    }

    private fun CoroutineScope.stlPreview(file: PickedFile): ModelPreview = try {
        when (val result = stlPreviewer(file.bytes) { progress -> ensureActive(); reportPreviewProgress(file, progress) }) {
            is StlPreviewResult.Ready -> ModelPreview.Stl(result.mesh, result.analysis, result.isSimplified)
            is StlPreviewResult.TooLarge -> ModelPreview.TooLarge(result.fileSizeBytes)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // Inclui a falta de memória: o arquivo é guardado do mesmo jeito, só sem a prévia.
        AppLog.warn("Não consegui ler o STL ${file.fileName}", e)
        ModelPreview.Unreadable
    }

    private fun threeMfPreview(file: PickedFile): ModelPreview {
        // Um erro na leitura não pode deixar a prévia em "carregando" pra sempre: o 3MF é guardado e fica ilegível.
        val thumbnail = try {
            threeMfReader(file.bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            AppLog.warn("Não consegui ler o 3MF ${file.fileName}", e)
            ThreeMfThumbnail.Unreadable
        }
        return threeMfPreviewOf(thumbnail)
    }

    private fun threeMfPreviewOf(thumbnail: ThreeMfThumbnail): ModelPreview = when (thumbnail) {
        is ThreeMfThumbnail.Found -> {
            val extension = thumbnail.path.substringAfterLast('.', "png").lowercase()
            ModelPreview.ThreeMf(PickedFile("miniatura_do_3mf.$extension", thumbnail.imageBytes))
        }

        ThreeMfThumbnail.Missing -> ModelPreview.ThreeMfNoThumbnail

        ThreeMfThumbnail.Unreadable -> ModelPreview.ThreeMfUnreadable
    }

    /** Só sobe o progresso de quem ainda é o arquivo atual, e em passos de 1%, pra a tela não redesenhar a cada triângulo. */
    private fun reportPreviewProgress(file: PickedFile, progress: Float) {
        state.update { current ->
            val loading = current.modelPreview as? ModelPreview.Loading
            if (loading == null || current.form.modelFile !== file || progress - loading.progress < PROGRESS_STEP) {
                current
            } else {
                current.copy(modelPreview = ModelPreview.Loading(progress))
            }
        }
    }

    private fun applyThreeMfPhoto(thumbnail: PickedFile) {
        val form = state.value.form
        if (form.photo != null && !form.photoAutomatic) return
        // A miniatura do 3MF toma o lugar da do G-code: desfazer a importação não devolve mais a foto de antes.
        photoBeforeGCode = null
        photoGCodePrintId = null
        updateForm { it.copy(photo = thumbnail, photoAutomatic = true, savedConfirmation = false) }
    }

    /** Ver `SettingsViewModel.consumeSavedConfirmation`. */
    fun consumeSavedConfirmation() = updateForm { it.copy(savedConfirmation = false) }

    fun consumeBlockedMessage() = updateForm { it.copy(blockedMessage = null) }

    /**
     * Salva o orçamento no histórico. Num orçamento novo, o formulário volta ao começo depois de salvar
     * (mantendo pedido/produto): antes, os campos continuavam preenchidos e um segundo Ctrl+S criava um
     * pedido repetido. Reabrindo, atualiza o mesmo orçamento.
     */
    fun saveQuote(calculated: Quote, services: List<QuoteService>): Boolean {
        val form = state.value.form
        val input = state.value.input
        val isProduct = input.isProduct
        // Já enviado pro cliente e nada mudou desde então: o pedido existe, não grava de novo.
        val saved = alreadySent()?.savedQuote ?: persist(calculated, services) ?: return false

        val editing = form.operation as? QuoteOperation.Editing
        if (editing != null) {
            updateForm { it.copy(savedConfirmation = true, savedAsProduct = isProduct, savedNumber = editing.savedQuote.displayNumber) }
            return true
        }
        forgetUndo()
        cancelPreview()
        change {
            it.copy(
                input = QuoteInputState(kind = input.kind),
                form = SaveQuoteFormState(savedConfirmation = true, savedAsProduct = isProduct, savedNumber = saved.displayNumber),
                modelPreview = null,
                collapsedPrintIds = emptySet(),
                negotiating = false,
            )
        }
        return true
    }

    /** O pedido que "Enviar pro cliente" já gravou, se a tela ainda é exatamente o que foi gravado. */
    private fun alreadySent(): SentQuote? {
        val current = state.value
        val sent = liveSent() ?: return null
        return sent.takeIf { current.input == it.input && current.form.forComparison() == it.form }
    }

    /** O pedido do "Enviar pro cliente", se ele ainda existe: excluído (lixeira ou de vez), é como se nunca tivesse sido enviado. */
    private fun liveSent(): SentQuote? {
        val sent = state.value.form.sent ?: return null
        return sent.takeIf { kept -> historyRepository.savedQuotes.value.any { it.id == kept.savedQuote.id } }
    }

    private fun SaveQuoteFormState.forComparison() =
        copy(savedConfirmation = false, savedAsProduct = false, savedNumber = null, blockedMessage = null, photoError = null, sent = null)

    /**
     * Grava o que a tela mostra no histórico, sem mexer no formulário: reabrindo, atualiza o mesmo orçamento; depois
     * de "Enviar pro cliente", atualiza o pedido que o envio criou (e não cria outro); senão, cria um novo.
     * Devolve o orçamento gravado, ou `null` (e diz por quê em [SaveQuoteFormState.blockedMessage]).
     */
    private fun persist(calculated: Quote, services: List<QuoteService>): SavedQuote? {
        val form = state.value.form
        val input = state.value.input
        // Nome e configurações de cada impressão não mudam o preço, então vêm da tela mesmo quando o cálculo
        // é o congelado de um pedido reaberto (decisão 108): senão, corrigir só isso se perderia ao salvar.
        val quote = if (calculated.prints.size != input.prints.size) {
            calculated
        } else {
            calculated.copy(
                prints = calculated.prints.zip(input.prints) { quoted, print ->
                    quoted.copy(job = quoted.job.copy(name = print.name.trim().ifEmpty { null }, settings = print.settings.takeUnless { it.isEmpty }))
                },
            )
        }
        val thumbnails = input.prints.map { it.thumbnail }
        val isProduct = input.isProduct
        // Pelo "Vender", sem outro preço digitado: o pedido saiu pelo preço do catálogo, e isso não é
        // negociação com o cliente (decisão 103).
        val soldAtCatalogPrice = !isProduct && input.announcedUnitPrice != null && input.targetTotalText.isBlank()
        val client = if (isProduct) {
            null
        } else {
            form.clientName.trim().ifEmpty { null }?.let { name ->
                val typed = Client(name = name, contact = form.clientContact.trim().ifEmpty { null }, id = form.clientId)
                clientRepository?.resolve(typed) ?: typed
            }
        }
        val shippingCost = if (isProduct) 0.0 else parseDecimal(input.shippingCostText, NumberKind.AMOUNT, separator) ?: 0.0
        // Se o frete grátis vale de fato é a mesma regra da conta (decisão 124), que é do `core`.
        val shippingAbsorbed =
            OrderTerms(shippingCost = shippingCost, shippingAbsorbed = input.shippingAbsorbed, catalogProduct = isProduct).absorbsShipping

        val editing = form.operation as? QuoteOperation.Editing
        val existing = editing?.savedQuote ?: liveSent()?.savedQuote
        if (existing != null) {
            val update = {
                historyRepository.update(
                    id = existing.id,
                    name = form.name,
                    quote = quote,
                    services = services,
                    photo = form.photo,
                    stlFile = form.modelFile,
                    sourceLink = form.sourceLink,
                    client = client,
                    printThumbnails = thumbnails,
                    shippingCost = shippingCost,
                    deliveryDateEpochDay = form.deliveryDateEpochDay.takeUnless { isProduct },
                    category = form.category,
                    soldAtCatalogPrice = soldAtCatalogPrice,
                    shippingAbsorbed = shippingAbsorbed,
                    autoNamed = form.keepsAutoName,
                )
            }
            // A edição não é mais modal (decisão 113): o pedido pode ter ido pra lixeira enquanto era editado.
            // Salvar tira ele de lá com as alterações; sem ele nem na lixeira, avisa em vez de dizer que salvou.
            val updated = update() ?: historyRepository.restore(existing.id)?.let { update() }
            if (updated != null) return updated
            if (editing != null) {
                updateForm { it.copy(blockedMessage = uiText(Res.string.quote_blocked_deleted, existing.displayName(historyRepository::autoName))) }
                return null
            }
        }

        val saved = historyRepository.save(
            name = form.name,
            quote = quote,
            services = services,
            photo = form.photo,
            stlFile = form.modelFile,
            sourceLink = form.sourceLink,
            client = client,
            printThumbnails = thumbnails,
            shippingCost = shippingCost,
            deliveryDateEpochDay = form.deliveryDateEpochDay.takeUnless { isProduct },
            kind = input.kind,
            sourceProductId = form.sourceProductId.takeUnless { isProduct },
            category = form.category,
            soldAtCatalogPrice = soldAtCatalogPrice,
            currency = currency(),
            shippingAbsorbed = shippingAbsorbed,
            autoNamed = form.keepsAutoName,
        )
        return saved
    }

    /**
     * "Enviar pro cliente" (decisão 131): grava o orçamento como pedido (o mesmo caminho do salvar, com os mesmos
     * bloqueios) e deixa a tela como está, pra negociar mais se precisar. Depois disso, enviar de novo sem mudar
     * nada usa o mesmo pedido, e mudando algo atualiza esse pedido, sem criar outro. Devolve se o menu de envio
     * pode abrir. Não vale pro produto do catálogo, que não tem cliente.
     */
    fun sendToClient(): Boolean {
        if (state.value.input.isProduct) return false
        if (alreadySent() != null) return true
        val result = freshResult()
        val quote = result.quote
        val blocked = blockedReason(result)
        if (blocked != null || quote == null) {
            updateForm { it.copy(blockedMessage = blocked) }
            return false
        }
        val saved = persist(quote, result.selectedServices) ?: return false
        val sentInput = state.value.input
        updateForm { form ->
            // Editando, o que está em disco agora é o que o cancelar e o "tem alteração" comparam: a operação acompanha.
            val operation = (form.operation as? QuoteOperation.Editing)?.copy(
                savedQuote = saved,
                originalInput = sentInput,
                originalForm = form.forComparison().copy(operation = null),
            ) ?: form.operation
            val next = form.copy(operation = operation, savedConfirmation = true, savedAsProduct = false, savedNumber = saved.displayNumber)
            next.copy(sent = SentQuote(saved, sentInput, next.forComparison()))
        }
        return true
    }

    /** O pedido que o último "Enviar pro cliente" gravou, pra os quatro jeitos de enviar. */
    private fun sentQuote(): SavedQuote? = liveSent()?.savedQuote

    fun sendPdf() = send(ClientExport.PDF)

    fun sendImage() = send(ClientExport.IMAGE)

    fun sendWhatsApp() = send(ClientExport.WHATSAPP)

    fun sendCopy() = send(ClientExport.COPY)

    /**
     * Faz o envio escolhido, ou o segura quando o prazo do pedido já venceu: a tela mostra o mesmo aviso de Pedidos
     * a partir de [QuoteUiState.pendingSend], e o envio só sai em [confirmPendingSend].
     */
    private fun send(export: ClientExport) {
        val saved = sentQuote() ?: return
        if (saved.isDeliveryOverdue(today())) {
            state.update { it.copy(pendingSend = export) }
            return
        }
        perform(export, saved)
    }

    private fun perform(export: ClientExport, saved: SavedQuote) {
        val exporter = exporter ?: return
        when (export) {
            ClientExport.PDF -> exporter.exportPdf(saved)

            ClientExport.IMAGE -> exporter.saveShareableImage(saved)

            ClientExport.WHATSAPP -> exporter.openInWhatsApp(saved)

            ClientExport.COPY -> exporter.copyText(saved)

            // O Orçamento envia um pedido só; a exportação em lote é de Pedidos.
            ClientExport.SELECTED_PDF -> Unit
        }
    }

    /** "Enviar assim mesmo" no aviso de prazo vencido: faz o envio que tinha sido segurado, sem perguntar de novo. */
    fun confirmPendingSend() {
        val export = state.value.pendingSend ?: return
        state.update { it.copy(pendingSend = null) }
        sentQuote()?.let { perform(export, it) }
    }

    /** Desiste do envio segurado ("Alterar prazo" e "Cancelar" no aviso): nada é enviado. O prazo se muda em Detalhes. */
    fun dismissPendingSend() = state.update { it.copy(pendingSend = null) }

    /**
     * Reabre [savedQuote] pra edição: preenche as entradas com os valores salvos e o formulário de salvar
     * com os anexos recarregados. Salvar depois disso atualiza o mesmo orçamento (ver [saveQuote]). Enquanto
     * nada que muda o preço for mexido, o cálculo continua o salvo ([QuoteUiState.result]).
     */
    fun loadForEditing(savedQuote: SavedQuote) {
        val input = inputFrom(savedQuote)
        val form = formFrom(savedQuote)
        setInputAndForm(input, form.copy(operation = QuoteOperation.Editing(savedQuote, input, form)))
    }

    /**
     * Reabre [savedQuote] como um **orçamento novo**: mesmos dados de [loadForEditing], prontos pra
     * ajustar, mas salvar cria uma linha nova no Histórico (data de criação e status novos). Não vêm
     * junto o prazo (uma data de outro pedido, provavelmente já passada, decisão 85) nem o cliente
     * (duplicar é, quase sempre, vender a mesma peça pra outra pessoa). A origem vem junto (decisão 103):
     * reimprimir uma venda do catálogo continua sendo daquele produto no ranking do Dashboard.
     */
    fun duplicateForNewQuote(savedQuote: SavedQuote) {
        setInputAndForm(
            inputFrom(savedQuote),
            formFrom(savedQuote).copy(
                operation = QuoteOperation.Duplicating(savedQuote.name, savedQuote.kind),
                deliveryDateEpochDay = null,
                sourceProductId = savedQuote.sourceProductId,
                clientName = "",
                clientContact = "",
                clientId = null,
            ),
        )
    }

    /**
     * "Vender" um produto do catálogo (decisão 101): o que nasce é um **pedido**, que guarda de qual
     * produto veio ([SavedQuote.sourceProductId]). Sem cliente e sem prazo, que são da venda nova. O
     * produto continua no catálogo, intacto. Com preço anunciado, a peça sai por ele
     * ([QuoteInputState.announcedUnitPrice]), e frete e serviços somam por fora (decisão 102).
     */
    fun sellFromProduct(product: SavedQuote) {
        val announced = product.quote.takeIf { it.isNegotiated }?.unitSalePrice
        setInputAndForm(
            inputFrom(product).copy(kind = QuoteKind.ORDER, targetTotalText = "", announcedUnitPrice = announced),
            formFrom(product).copy(
                operation = QuoteOperation.Selling(product),
                sourceProductId = product.id,
                clientName = "",
                clientContact = "",
                clientId = null,
                deliveryDateEpochDay = null,
            ),
        )
    }

    /**
     * Categorias já usadas nos produtos, pra sugerir no campo (decisão 102): sem tela de cadastro,
     * a lista nasce do que a pessoa já digitou. Uma por grafia, em ordem alfabética.
     */
    fun knownCategories(savedQuotes: List<SavedQuote>): List<String> =
        savedQuotes.mapNotNull { it.category }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

    /**
     * "Guardar no catálogo" a partir de um pedido (decisão 101): abre uma cópia em modo produto pra
     * revisar antes de salvar. Passa pelo Orçamento de propósito: um preço negociado com aquele
     * cliente volta pro preço de tabela, e frete, cliente e prazo ficam pra trás. O pedido não muda,
     * porque é histórico de venda.
     */
    fun copyToCatalog(order: SavedQuote) {
        setInputAndForm(
            inputFrom(order).copy(kind = QuoteKind.PRODUCT, shippingCostText = "", targetTotalText = "", announcedUnitPrice = null),
            formFrom(order).copy(
                operation = QuoteOperation.CopyingToCatalog(order.name),
                clientName = "",
                clientContact = "",
                clientId = null,
                deliveryDateEpochDay = null,
            ),
        )
    }

    private fun inputFrom(savedQuote: SavedQuote): QuoteInputState =
        SavedQuoteMapper.inputStateFrom(
            savedQuote, filaments, printers, salesChannels, ::newId,
            thumbnail = { key -> historyRepository.attachmentBytes(key)?.let { PickedFile("miniatura_do_gcode.${attachmentExtension(key)}", it) } },
            decimalSeparator = separator,
            numberSeparator = AppLocale.decimalSeparator,
        )

    private fun formFrom(savedQuote: SavedQuote): SaveQuoteFormState {
        val photo = savedQuote.photoFileName?.let { fileName ->
            historyRepository.photoBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        val modelFile = savedQuote.modelFileName?.let { fileName ->
            historyRepository.modelBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        return SavedQuoteMapper.saveFormFrom(savedQuote, photo, modelFile)
    }

    /** Troca o pedido e o formulário de uma vez, pra conta nunca ver um sem o outro. */
    private fun setInputAndForm(input: QuoteInputState, form: SaveQuoteFormState) {
        forgetUndo()
        change { it.copy(input = input, form = form, collapsedPrintIds = emptySet(), negotiating = false) }
        loadModelPreview(form.modelFile)
    }

    /** Um formulário novo não desfaz nada do anterior (os cartões recolhidos saem junto, no estado). */
    private fun forgetUndo() {
        photoBeforeGCode = null
        photoGCodePrintId = null
        removedPrint = null
    }

    /**
     * Atalho de teclado (Ctrl/Cmd+S): salva o que a tela mostra ([QuoteUiState.result], a mesma conta). Se não
     * der, diz por quê em [SaveQuoteFormState.blockedMessage], em vez de não fazer nada. Devolve se salvou.
     */
    fun saveCurrentQuote(): Boolean {
        val result = freshResult()
        val quote = result.quote
        val blocked = blockedReason(result)
        if (blocked != null || quote == null) {
            updateForm { it.copy(blockedMessage = blocked) }
            return false
        }
        return saveQuote(quote, result.selectedServices)
    }

    /** Por que [result] não dá pra salvar, ou `null` se dá. Quem salva e quem envia pro cliente dizem a mesma coisa. */
    private fun blockedReason(result: QuoteResult): UiText? = when {
        result.fieldErrors.isNotEmpty() -> notSavedBecause(result.fieldErrors.values.first())
        result.quote == null -> result.errorMessage?.let(::notSavedBecause) ?: uiText(Res.string.quote_blocked_incomplete)
        result.missingServicePrice -> uiText(Res.string.quote_blocked_service_price)
        else -> null
    }

    private fun notSavedBecause(reason: UiText): UiText = UiText.Joined(listOf(uiText(Res.string.quote_blocked_prefix), reason))

    /** Reabrindo um orçamento, se algo foi mudado desde que ele abriu (cancelar perderia a mudança). */
    val hasUnsavedEdits: Boolean
        get() {
            val editing = state.value.form.operation as? QuoteOperation.Editing ?: return false
            val form = state.value.form.copy(operation = null).forComparison()
            return state.value.input != editing.originalInput || form != editing.originalForm
        }

    /** Se há algo digitado ou uma operação em andamento, que "Limpar" ou outra operação jogaria fora. */
    val hasDraft: Boolean
        get() {
            // Enviado pro cliente e sem mudança desde então: já está gravado, não há trabalho por salvar.
            if (alreadySent() != null) return false
            val input = state.value.input
            val form = state.value.form.copy(savedConfirmation = false, savedAsProduct = false, savedNumber = null, blockedMessage = null)
            return input != QuoteInputState(kind = input.kind) || form != SaveQuoteFormState()
        }

    /** Limpa a peça e o formulário de salvar, pra começar um orçamento novo (Ctrl/Cmd+N, cancelar). */
    fun resetForm() {
        forgetUndo()
        cancelPreview()
        change { it.copy(input = QuoteInputState(), form = SaveQuoteFormState(), modelPreview = null, collapsedPrintIds = emptySet(), negotiating = false) }
    }

    /**
     * A conta da tela, do Ctrl+S e do salvar: uma fonte só. Reabrindo um orçamento sem mudar nada que
     * mexe no preço, o resultado é o cálculo congelado dele (decisão 108): corrigir o contato do cliente
     * não pode trocar o preço que o cliente já recebeu pelos custos de hoje. Com alguma mudança, a conta é
     * refeita e o preço antigo vem junto, pra tela mostrar a diferença.
     */
    private fun freshResult(): QuoteResult = resultFor(state.value.copy(catalog = readCatalog()))

    private fun resultFor(state: QuoteUiState): QuoteResult {
        val input = state.input
        val catalog = state.catalog
        val result = calculate(catalog.filaments, catalog.printers, catalog.settings, catalog.services, input, catalog.salesChannels, catalog.consumables)
        val editing = state.form.operation as? QuoteOperation.Editing ?: return result
        val original = editing.savedQuote
        return if (input.pricingKey() == editing.originalInput.pricingKey()) {
            result.copy(
                quote = original.quote,
                selectedServices = original.services,
                shippingCost = original.shippingCost,
                shippingAbsorbed = original.shippingAbsorbed,
                keepsOriginalPrice = true,
                todaysQuote = result.quote,
                errorMessage = null,
                fieldErrors = emptyMap(),
                missingServicePrice = false,
            )
        } else {
            result.copy(originalQuote = original.quote)
        }
    }

    /**
     * A conta pra [input] com os cadastros dados. Função pura (a tela de testes e [comparePrinters] usam).
     *
     * Um valor que não é número (ou negativo onde não pode) vira erro no campo ([QuoteResult.fieldErrors])
     * e não há orçamento: antes, frete inválido virava zero, quantidade inválida virava 1, e frete negativo
     * derrubava o salvar. Um filamento ou impressora escolhidos que saíram do cadastro também não caem no
     * primeiro da lista: a tela pede pra escolher de novo.
     *
     * @param filaments todos os filamentos, inclusive os esgotados: um escolhido que esgotou continua
     *   valendo (a tela marca). Só o padrão, quando nada foi escolhido, sai dos que estão em estoque.
     */
    fun calculate(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        services: List<Service>,
        input: QuoteInputState,
        channels: List<SalesChannel> = this.salesChannels,
        consumables: List<Consumable> = this.consumables,
    ): QuoteResult {
        val errors = LinkedHashMap<String, UiText>()
        val quantity = input.quantityOrNull ?: 1.also { errors[QuoteFields.QUANTITY] = uiText(Res.string.quote_error_quantity) }
        val selectedServices = input.selectedServices.mapNotNull { (id, serviceInput) ->
            val price = parseDecimal(serviceInput.priceText, NumberKind.AMOUNT, separator)?.takeIf { it >= 0 }
            if (price == null) {
                if (serviceInput.priceText.isNotBlank()) errors[QuoteFields.service(id)] = uiText(Res.string.quote_error_service_value, serviceInput.name)
                return@mapNotNull null
            }
            val laborMinutes = if (serviceInput.laborMinutesText.isBlank()) {
                0.0
            } else {
                parseDurationMinutes(serviceInput.laborMinutesText) ?: 0.0.also {
                    errors[QuoteFields.serviceMinutes(id)] = uiText(Res.string.quote_error_service_minutes, serviceInput.name)
                }
            }
            QuoteService(
                id = id,
                name = services.find { it.id == id }?.name ?: serviceInput.name,
                price = price,
                chargedPerOrder = serviceInput.chargedPerOrder,
                laborMinutes = laborMinutes,
            )
        }
        // Com o insumo no cadastro, vale o custo de hoje, como o filamento; saído do cadastro, o do pedido.
        val usedConsumables = input.selectedConsumables.mapNotNull { (id, consumableInput) ->
            val quantity = parseDecimal(consumableInput.quantityText, NumberKind.AMOUNT, AppLocale.decimalSeparator)?.takeIf { it > 0 }
            if (quantity == null) {
                errors[QuoteFields.consumable(id)] = uiText(Res.string.quote_error_consumable_quantity, consumableInput.name)
                return@mapNotNull null
            }
            val fromCatalog = consumables.find { it.id == id }
            QuotedConsumable(
                id = id,
                name = fromCatalog?.name ?: consumableInput.name,
                unitCost = fromCatalog?.unitCost ?: consumableInput.unitCost,
                quantity = quantity,
                chargedPerOrder = consumableInput.chargedPerOrder,
            )
        }
        val missingServicePrice = selectedServices.size < input.selectedServices.size
        // Arquivado não é escolhido sozinho (decisão 115); escolhido antes, continua valendo.
        val inStock = filaments.filter { it.hasStockAvailable && !it.archived }
        val resolved = input.prints.map { print ->
            ResolvedPrint(
                printer = if (print.printerId == null) printers.firstOrNull { !it.archived } else printers.find { it.id == print.printerId },
                filaments = print.filaments.map { row ->
                    // Com uma linha só, nada escolhido ainda vale o primeiro filamento em estoque: é a tela
                    // de sempre. Numa peça multicolor, cada linha precisa de uma escolha de verdade.
                    val filament = if (row.filamentId == null) {
                        inStock.firstOrNull()?.takeIf { print.filaments.size == 1 }
                    } else {
                        filaments.find { it.id == row.filamentId }
                    }
                    val color = filament?.let { chosen ->
                        row.colorId?.let { id -> chosen.colors.find { it.id == id } } ?: chosen.colors.firstOrNull { it.inStock }
                    }
                    ResolvedFilament(filament, color)
                },
            )
        }
        val channel = channels.find { it.id == input.salesChannelId }
        // Produto do catálogo não tem frete (decisão 101): o campo some da tela, e o que tiver ficado
        // digitado nele não é lido (nem vira erro). O `core` também o ignora na conta.
        val shippingCost = if (input.isProduct) 0.0 else amount(input.shippingCostText, QuoteFields.SHIPPING, Res.string.quote_error_shipping_invalid, Res.string.quote_error_shipping_negative, errors) ?: 0.0
        // O que vem antes da conta (preço da peça a partir do total fechado, frete grátis, produto, urgência)
        // é regra do `core` (decisão 130); aqui só entram os valores lidos.
        val typedTerms = OrderTerms(
            quantity = quantity,
            services = selectedServices,
            consumables = usedConsumables,
            shippingCost = shippingCost,
            shippingAbsorbed = input.shippingAbsorbed,
            catalogProduct = input.isProduct,
            customerTotal = amount(input.targetTotalText, QuoteFields.TARGET, Res.string.quote_error_target_invalid, Res.string.quote_error_target_negative, errors),
            announcedUnitPrice = input.announcedUnitPrice,
            rush = input.rush,
        )
        // Um total que nem cobre os extras é erro no campo: zerar a peça salvaria um total diferente do
        // digitado, sem ninguém ver.
        if (!typedTerms.coversExtras) {
            errors[QuoteFields.TARGET] = uiText(Res.string.quote_error_target_below_extras, typedTerms.extrasTotal.toInputText(separator))
        }
        val laborMinutes = if (input.laborMinutesText.isBlank()) {
            0.0
        } else {
            parseDurationMinutes(input.laborMinutesText) ?: 0.0.also {
                errors[QuoteFields.LABOR] = uiText(Res.string.quote_error_labor)
            }
        }
        // O tempo digitado já é do pedido inteiro, então entra uma vez só, sem multiplicar.
        val terms = typedTerms.copy(laborMinutes = laborMinutes)

        val base = QuoteResult(
            prints = resolved,
            selectedServices = selectedServices,
            salesChannel = channel,
            shippingCost = shippingCost,
            shippingAbsorbed = terms.absorbsShipping,
            missingServicePrice = missingServicePrice,
        )
        missingFromCatalog(input, resolved)?.let { return base.copy(errorMessage = it, fieldErrors = errors) }
        val jobs = printJobs(input, resolved, errors)
        if (errors.isNotEmpty()) return base.copy(fieldErrors = errors)
        jobs ?: return base
        return runCatching {
            OrderPricing.quote(prints = jobs, settings = settings, channel = channel, terms = terms)
        }.fold(
            onSuccess = { base.copy(quote = it) },
            onFailure = { error ->
                // O `core` diz o que aconteceu pelo tipo e a frase é escolhida aqui (decisão 130); o resto é
                // validação que a tela já fez, e não deveria acontecer.
                val message = (error as? PricingError)?.let(::pricingErrorMessage) ?: uiText(Res.string.quote_error_check_values)
                base.copy(errorMessage = message)
            },
        )
    }

    /** Valor em dinheiro opcional: vazio é `null`, e o que não é número (ou é negativo) vira erro no campo. */
    private fun amount(
        text: String,
        field: String,
        invalid: StringResource,
        negative: StringResource,
        errors: MutableMap<String, UiText>,
    ): Double? {
        if (text.isBlank()) return null
        val value = parseDecimal(text, NumberKind.AMOUNT, separator)
        return when {
            value == null -> null.also { errors[field] = uiText(invalid) }
            value < 0 -> null.also { errors[field] = uiText(negative) }
            else -> value
        }
    }

    /** Um filamento ou impressora escolhidos que saíram do cadastro (orçamento reaberto), com o nome. */
    private fun missingFromCatalog(input: QuoteInputState, resolved: List<ResolvedPrint>): UiText? {
        // Canal que saiu do cadastro: recalcular sem a taxa baixaria o preço em silêncio. Escolher outro
        // canal, ou "Venda direta", limpa isto (ver [selectSalesChannel]).
        input.missingChannelName?.let { name ->
            return uiText(Res.string.quote_error_channel_missing, name)
        }
        input.prints.zip(resolved).forEach { (print, resolvedPrint) ->
            if (print.printerId != null && resolvedPrint.printer == null) {
                return print.missingPrinterName?.let { uiText(Res.string.quote_error_printer_missing, it) }
                    ?: uiText(Res.string.quote_error_printer_missing_unnamed)
            }
            print.filaments.zip(resolvedPrint.filaments).forEach { (row, resolvedFilament) ->
                if (row.filamentId != null && resolvedFilament.filament == null) {
                    return row.missingFilamentName?.let { uiText(Res.string.quote_error_filament_missing, it) }
                        ?: uiText(Res.string.quote_error_filament_missing_unnamed)
                }
            }
        }
        return null
    }

    /**
     * As impressões prontas pro cálculo, ou `null` enquanto falta escolher ou preencher alguma coisa.
     * Um campo preenchido com o que não é número vira erro em [errors].
     */
    private fun printJobs(
        input: QuoteInputState,
        resolved: List<ResolvedPrint>,
        errors: MutableMap<String, UiText>,
    ): List<Pair<PrintJob, PrinterProfile>>? {
        var complete = true
        val jobs = input.prints.zip(resolved).map { (print, resolvedPrint) ->
            val usages = print.filaments.zip(resolvedPrint.filaments).map { (row, resolvedFilament) ->
                val length = parseDecimal(row.lengthText, NumberKind.MEASURE, AppLocale.decimalSeparator)
                when {
                    row.weightText != null && row.weightText.isNotBlank() && (parseDecimal(row.weightText, NumberKind.AMOUNT, AppLocale.decimalSeparator) ?: -1.0) < 0 ->
                        errors[QuoteFields.length(print.id, row.id)] = uiText(Res.string.quote_error_weight)

                    row.lengthText.isNotBlank() && (length == null || length < 0) ->
                        errors[QuoteFields.length(print.id, row.id)] = uiText(Res.string.quote_error_length)
                }
                val filament = resolvedFilament.filament
                if (filament == null || length == null || length < 0) {
                    complete = false
                    null
                } else {
                    FilamentUsage(filament = filament, lengthMeters = length, color = resolvedFilament.color)
                }
            }
            if (print.runsText.isNotBlank() && (parseWholeNumber(print.runsText) ?: 0) < 1) {
                errors[QuoteFields.runs(print.id)] = uiText(Res.string.quote_error_runs)
            }
            val time = print.printTimeMinutes
            if (print.printTimeText.isNotBlank() && time == null) {
                errors[QuoteFields.printTime(print.id)] = uiText(Res.string.quote_error_print_time)
            }
            val printer = resolvedPrint.printer
            if (printer == null || time == null || usages.any { it == null }) {
                complete = false
                null
            } else {
                PrintJob(
                    filaments = usages.filterNotNull(),
                    printTimeMinutes = time,
                    runs = print.runs,
                    name = print.name.trim().ifEmpty { null },
                    settings = print.settings.takeUnless { it.isEmpty },
                ) to printer
            }
        }
        return if (complete) jobs.filterNotNull() else null
    }

    /**
     * O mesmo orçamento calculado em cada impressora cadastrada, com todas as impressões nela, pra
     * responder "em qual máquina essa peça sai mais barata". Só faz sentido com mais de uma impressora;
     * devolve lista vazia quando não há o que comparar ou quando os dados da peça ainda não dão um cálculo
     * válido. Compara o preço que a conta dá: um preço fechado ou anunciado é o mesmo em todas e não diria
     * nada (a tela usa `tableSalePrice` quando existe).
     */
    fun comparePrinters(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        services: List<Service>,
        input: QuoteInputState,
        channels: List<SalesChannel> = this.salesChannels,
        consumables: List<Consumable> = this.consumables,
    ): List<Pair<PrinterProfile, Quote>> {
        val candidates = printers.filterNot { it.archived }
        if (candidates.size < 2) return emptyList()
        return candidates.mapNotNull { printer ->
            val allOnThisPrinter = input.copy(prints = input.prints.map { it.copy(printerId = printer.id, missingPrinterName = null) })
            val result = calculate(filaments, listOf(printer), settings, services, allOnThisPrinter, channels, consumables)
            result.quote?.let { printer to it }
        }
    }

    private companion object {
        /** Quantos avisos esperam a tela buscar, se vierem em sequência. */
        const val NOTICE_BUFFER = 8

        /** O progresso da prévia só é publicado a cada 1%. */
        const val PROGRESS_STEP = 0.01f
    }
}

private fun <T> List<T>.replaced(index: Int, transform: (T) -> T): List<T> = mapIndexed { i, item -> if (i == index) transform(item) else item }

/** O texto de cada erro de preço do `core` (decisão 130), escolhido pelo tipo. */
internal fun pricingErrorMessage(error: PricingError): UiText = when (error) {
    is PricingError.DeductionsReachFullPrice -> uiText(Res.string.quote_error_deductions)
}
