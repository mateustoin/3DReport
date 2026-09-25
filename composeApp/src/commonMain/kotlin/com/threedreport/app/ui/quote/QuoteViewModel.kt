package com.threedreport.app.ui.quote

import com.threedreport.app.AppLog
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.attachmentExtension
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.ui.components.matchingClients
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseDurationMinutes
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.components.NoticeIds
import com.threedreport.app.ui.components.UserNotice
import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.report.PrintQueueReport
import com.threedreport.core.report.PrinterQueueEntry
import com.threedreport.core.slicer.GCodeMetadataParser
import com.threedreport.core.stl.StlAnalysis
import com.threedreport.core.stl.StlAnalyzer
import com.threedreport.core.stl.StlMesh
import com.threedreport.core.stl.parseStl
import com.threedreport.core.stl.peekStlTriangleCount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.round

/** A prévia 3D do STL anexado, calculada fora do thread da tela (decisão 108). */
sealed interface StlPreview {
    data object Loading : StlPreview

    /** Malha grande demais pra desenhar sem travar; o arquivo é salvo normalmente. */
    data class TooComplex(val triangleCount: Long) : StlPreview

    data object Unreadable : StlPreview

    data class Ready(val mesh: StlMesh, val analysis: StlAnalysis) : StlPreview
}

/**
 * ViewModel da tela de Orçamento.
 *
 * [filaments], [printers], [services] e [settings] são repassados diretamente dos repositórios
 * compartilhados (sem cópia), então refletem na hora qualquer cadastro/edição feito nas outras telas.
 * [currentResult] é a conta da tela e do Ctrl+S, uma fonte só.
 *
 * O trabalho pesado (ler G-code, analisar STL) roda em [background] e volta pra [main]; os testes
 * passam o padrão, `Unconfined`, e tudo acontece na hora.
 *
 * A importação de G-code ([GCodeImporter]) e o caminho de um orçamento salvo de volta pra tela
 * ([SavedQuoteMapper]) moram em arquivos próprios (decisão 108).
 */
class QuoteViewModel(
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
    serviceRepository: ServiceRepository,
    salesChannelRepository: SalesChannelRepository,
    private val historyRepository: QuoteHistoryRepository,
    /** Cadastro de clientes (decisão 106): o cliente do pedido é achado ou criado ao salvar. */
    private val clientRepository: ClientRepository? = null,
    /** Moeda dos orçamentos novos (decisão 106), gravada no orçamento ao salvar. */
    private val currency: () -> Currency = { Currency.BRL },
    private val platform: PlatformServices = defaultPlatform,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val background: CoroutineDispatcher = Dispatchers.Unconfined,
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
) {
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings
    val services: StateFlow<List<Service>> = serviceRepository.services
    val salesChannels: StateFlow<List<SalesChannel>> = salesChannelRepository.channels

    /** Clientes do cadastro, pra sugerir ao digitar o nome. */
    val clients: StateFlow<List<Client>> = clientRepository?.clients ?: MutableStateFlow(emptyList())

    private val inputState = MutableStateFlow(QuoteInputState())
    val input: StateFlow<QuoteInputState> = inputState.asStateFlow()

    private val saveFormState = MutableStateFlow(SaveQuoteFormState())
    val saveForm: StateFlow<SaveQuoteFormState> = saveFormState.asStateFlow()

    private val importingState = MutableStateFlow(false)

    /** Um G-code está sendo lido. */
    val importing: StateFlow<Boolean> = importingState.asStateFlow()

    private val stlPreviewState = MutableStateFlow<StlPreview?>(null)
    val stlPreview: StateFlow<StlPreview?> = stlPreviewState.asStateFlow()

    /** Histórico, só pra dica de prazo saber o que já está na fila de cada impressora. */
    val savedQuotes: StateFlow<List<SavedQuote>> = historyRepository.savedQuotes

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
        if (row.weightText != null) changed.withWeight(row.weightText, filaments.value.find { it.id == id }) else changed
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
        val rowIndex = rowIndexOf(inputState.value.prints[printIndex], rowId) ?: return
        val filament = currentResult().prints.getOrNull(printIndex)?.filaments?.getOrNull(rowIndex)?.filament
        updateFilament(printId, rowId) { it.withWeight(text, filament) }
    }

    private fun FilamentInput.withWeight(text: String, filament: Filament?): FilamentInput {
        val grams = parseDecimal(text, NumberKind.AMOUNT)
        val meters = when {
            text.isBlank() -> ""
            grams == null || grams < 0 || filament == null -> lengthText
            // Duas casas, como o G-code: com mais, o campo mostrava "16,764181" pra 50 g.
            else -> GCodeImporter.formatLength(filament.lengthMeters(grams))
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
        val inStock = this@QuoteViewModel.filaments.value.firstOrNull { it.hasStockAvailable && !it.archived } ?: return this
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
        inputState.update { input ->
            val last = input.prints.last()
            val printerId = last.printerId ?: printers.value.firstOrNull { !it.archived }?.id
            val rows = last.withDefaultFilamentChosen().filaments.map { FilamentInput(filamentId = it.filamentId, colorId = it.colorId, id = newId()) }
            input.copy(prints = input.prints + PrintInput(printerId = printerId, filaments = rows, id = id))
        }
        return id
    }

    /** Uma cópia da impressão logo depois dela, com tudo preenchido (duas mesas iguais em impressoras diferentes). */
    fun duplicatePrint(printId: Int) = inputState.update { input ->
        val index = input.prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 } ?: return@update input
        val original = input.prints[index]
        val copy = original.copy(
            filaments = original.filaments.map { it.copy(id = newId()) },
            beforeGCode = null,
            gcodeImportMessage = null,
            createdByImport = false,
            id = newId(),
        )
        input.copy(prints = input.prints.take(index + 1) + copy + input.prints.drop(index + 1))
    }

    /** A impressão removida por último e onde ela estava, pro "Desfazer" do aviso. */
    private var removedPrint: Pair<Int, PrintInput>? = null

    /**
     * Remove a impressão [printId], com "Desfazer" no aviso. A última não sai: pedido sem impressão não
     * existe (sobra a tela de sempre).
     */
    fun removePrint(printId: Int) {
        val prints = inputState.value.prints
        val index = prints.indexOfFirst { it.id == printId }
        if (index < 0 || prints.size <= 1) return
        removedPrint = index to prints[index]
        // A foto é do pedido: remover a impressão de onde a miniatura veio não tira a foto (e o Desfazer não
        // teria como devolvê-la).
        inputState.update { it.copy(prints = it.prints.filterNot { print -> print.id == printId }) }
        if (photoGCodePrintId == printId) {
            photoGCodePrintId = null
            photoBeforeGCode = null
        }
        notify("Impressão ${index + 1} removida.", actionLabel = "Desfazer", action = ::restoreRemovedPrint)
    }

    private fun restoreRemovedPrint() {
        val (index, print) = removedPrint ?: return
        removedPrint = null
        inputState.update { input -> input.copy(prints = input.prints.take(index) + print + input.prints.drop(index)) }
    }

    private fun printIndexOf(printId: Int?): Int? {
        val prints = inputState.value.prints
        return if (printId == null) 0 else prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 }
    }

    private fun rowIndexOf(print: PrintInput, rowId: Int?): Int? =
        if (rowId == null) 0 else print.filaments.indexOfFirst { it.id == rowId }.takeIf { it >= 0 }

    private fun updatePrint(printId: Int?, transform: (PrintInput) -> PrintInput) = inputState.update { input ->
        val index = if (printId == null) 0 else input.prints.indexOfFirst { it.id == printId }
        if (index !in input.prints.indices) input else input.copy(prints = input.prints.replaced(index, transform))
    }

    private fun updateFilament(printId: Int?, rowId: Int?, transform: (FilamentInput) -> FilamentInput) = updatePrint(printId) { current ->
        val index = rowIndexOf(current, rowId)
        if (index == null) current else current.copy(filaments = current.filaments.replaced(index, transform))
    }

    fun setLaborMinutes(text: String) = inputState.update { it.copy(laborMinutesText = text) }
    fun setQuantity(text: String) = inputState.update { it.copy(quantityText = text) }

    /**
     * Trocar entre pedido e produto limpa o preço fechado: com cliente ele é o total combinado,
     * frete incluso, e em produto é o preço anunciado. Levar um pro outro mudaria o preço em silêncio.
     */
    fun setKind(kind: QuoteKind) = inputState.update {
        if (it.kind == kind) it else it.copy(kind = kind, targetTotalText = "", announcedUnitPrice = null)
    }

    /** Abre o seletor de arquivo e importa o G-code escolhido na impressão [printId] (ver [importGCode]). */
    fun pickAndImportGCode(printId: Int? = null) = importDropped(platform.pickFile(FileKind.GCODE), printId)

    /**
     * Um arquivo arrastado pra janela (ver `fileDropTarget`) ou escolhido: G-code é importado; outro tipo,
     * ou um arquivo que não deu pra ler, vira a mensagem do bloco de importação.
     */
    fun importDropped(result: PickResult, printId: Int? = null) {
        when (result) {
            is PickResult.Picked -> importGCode(result.file, printId)
            is PickResult.Failed -> updatePrint(printId) { it.copy(gcodeImportMessage = result.message) }
            PickResult.Cancelled -> Unit
        }
    }

    /**
     * Vários arquivos soltos de uma vez (decisão 114): cada um vira uma impressão, na ordem. O primeiro vai
     * pra impressão que ainda está em branco, se houver só ela; o resto abre impressões novas.
     */
    fun importDroppedFiles(results: List<PickResult>) {
        val files = results.filterNot { it == PickResult.Cancelled }
        // Um arquivo só num pedido de uma impressão continua substituindo ela, como sempre foi.
        val replaceSingle = files.size == 1 && inputState.value.prints.size == 1
        files.forEachIndexed { index, result ->
            val onlyBlank = index == 0 && inputState.value.prints.singleOrNull()?.isBlank == true
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
        val notGCode = when (result) {
            is PickResult.Failed -> result.message
            is PickResult.Picked -> GCodeImporter.unsupportedMessage(result.file.fileName)
            PickResult.Cancelled -> null
        }
        if (notGCode != null) {
            updatePrint(inputState.value.prints.last().id) { it.copy(gcodeImportMessage = notGCode) }
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
        val id = inputState.value.prints.getOrNull(printIndexOf(printId) ?: return)?.id ?: return
        GCodeImporter.unsupportedMessage(file.fileName)?.let { message ->
            updatePrint(id) { it.copy(gcodeImportMessage = message) }
            return
        }
        importingState.value = true
        scope.launch(background) {
            val metadata = runCatching { GCodeMetadataParser.parse(file.bytes.decodeToString()) }
                .onFailure { AppLog.warn("Falha ao ler o G-code ${file.fileName}", it) }
                .getOrNull()
            withContext(main) {
                importingState.value = false
                if (metadata == null) {
                    updatePrint(id) { it.copy(gcodeImportMessage = "Não consegui ler esse G-code. Confira se é o arquivo exportado pelo fatiador.") }
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
        val index = inputState.value.prints.indexOfFirst { it.id == printId }.takeIf { it >= 0 } ?: return
        val current = inputState.value.prints[index]
        val form = saveFormState.value
        val thumbnail = metadata.thumbnail
        // A foto do pedido é da primeira impressão (ou de quem chegou primeiro sem foto nenhuma): a miniatura
        // da mesa 3 não troca a foto que veio da mesa 1.
        val photoApplied = thumbnail != null &&
            (form.photo == null || (form.photoFromGCode && (photoGCodePrintId == printId || index == 0)))
        // O G-code só escolhe entre o que não está arquivado (decisão 115).
        val imported = GCodeImporter.apply(
            current, metadata, filaments.value.filterNot { it.archived }, printers.value.filterNot { it.archived }, photoApplied, ::newId,
        )

        if (photoApplied) {
            if (photoBeforeGCode == null) photoBeforeGCode = form
            photoGCodePrintId = printId
            saveFormState.update {
                it.copy(photo = imported.thumbnail, photoFromGCode = true, savedConfirmation = false)
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
        val print = inputState.value.prints.getOrNull(printIndexOf(printId) ?: return) ?: return
        if (print.createdByImport && inputState.value.prints.size > 1) {
            inputState.update { it.copy(prints = it.prints.filterNot { p -> p.id == print.id }) }
        } else {
            updatePrint(print.id) { it.beforeGCode ?: it.copy(gcodeImportMessage = null) }
        }
        if (photoGCodePrintId == print.id) undoGCodePhoto()
    }

    private fun undoGCodePhoto() {
        val before = photoBeforeGCode
        saveFormState.update {
            if (!it.photoFromGCode) {
                it
            } else {
                it.copy(photo = before?.photo, photoFromGCode = before?.photoFromGCode == true, savedConfirmation = false)
            }
        }
        photoBeforeGCode = null
        photoGCodePrintId = null
    }

    /** Substitui as configurações de impressão da impressão [printId] (ver [PrintSettings]). */
    fun setPrintSettings(printSettings: PrintSettings, printId: Int? = null) = updatePrint(printId) { it.copy(settings = printSettings) }

    private val noticeIds = NoticeIds()
    private val noticeState = MutableStateFlow<UserNotice?>(null)

    /** O retorno de uma ação com "Desfazer" (remover uma impressão), pra barra de avisos. */
    val notice: StateFlow<UserNotice?> = noticeState.asStateFlow()

    fun consumeNotice(shown: UserNotice) = noticeState.update { if (it == shown) null else it }

    private fun notify(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        noticeState.value = UserNotice(message, actionLabel, action, isError = false, id = noticeIds.next())
    }

    /**
     * Marca ou desmarca o serviço [id]. Ao marcar, o valor vem preenchido com o sugerido do
     * catálogo (ou vazio, pra digitar) e a forma de cobrança com o padrão do cadastro.
     */
    fun toggleService(id: String) = inputState.update { input ->
        if (id in input.selectedServices) return@update input.copy(selectedServices = input.selectedServices - id)
        val service = services.value.find { it.id == id } ?: return@update input
        val serviceInput = ServiceInput(
            name = service.name,
            priceText = service.suggestedPrice?.toInputText().orEmpty(),
            chargedPerOrder = service.chargedPerOrder,
        )
        input.copy(selectedServices = input.selectedServices + (id to serviceInput))
    }

    fun setServicePrice(id: String, text: String) = updateServiceInput(id) { it.copy(priceText = text) }

    fun setServiceChargedPerOrder(id: String, chargedPerOrder: Boolean) =
        updateServiceInput(id) { it.copy(chargedPerOrder = chargedPerOrder) }

    private fun updateServiceInput(id: String, transform: (ServiceInput) -> ServiceInput) = inputState.update { input ->
        val current = input.selectedServices[id] ?: return@update input
        input.copy(selectedServices = input.selectedServices + (id to transform(current)))
    }

    fun selectSalesChannel(id: String?) = inputState.update { it.copy(salesChannelId = id, missingChannelName = null) }
    fun setShippingCost(text: String) = inputState.update { it.copy(shippingCostText = text) }
    fun setTargetTotal(text: String) = inputState.update { it.copy(targetTotalText = text) }

    /**
     * "Arredondar pra R$ 18,90" do preço anunciado (decisão 102): o próximo valor terminado em ,90
     * a partir do [total] calculado, ou `null` quando ele já termina assim. Função pura; a tela
     * mostra o chip só quando há sugestão.
     */
    fun showcasePriceSuggestion(total: Double): Double? {
        val cents = round(total * 100).toLong()
        val suggestion = ((cents - 90 + 99) / 100) * 100 + 90
        return (suggestion / 100.0).takeIf { suggestion != cents }
    }

    fun applyShowcasePrice(value: Double) = inputState.update { it.copy(targetTotalText = value.toInputText()) }

    fun setSaveName(text: String) = saveFormState.update { it.copy(name = text, savedConfirmation = false) }
    fun setSourceLink(text: String) = saveFormState.update { it.copy(sourceLink = text, savedConfirmation = false) }

    /** Digitar o nome desvincula do cliente escolhido na sugestão: pode ser outra pessoa. */
    fun setClientName(text: String) = saveFormState.update { it.copy(clientName = text, clientId = null, savedConfirmation = false) }
    fun setClientContact(text: String) = saveFormState.update { it.copy(clientContact = text, savedConfirmation = false) }

    /** Escolhe um cliente do cadastro na sugestão: nome e contato vêm dele. */
    fun chooseClient(client: Client) = saveFormState.update {
        it.copy(clientName = client.name, clientContact = client.contact.orEmpty(), clientId = client.id, savedConfirmation = false)
    }

    /** Clientes do cadastro que batem com o que foi digitado, pra sugerir (até cinco). */
    fun clientSuggestions(typed: String, clients: List<Client>): List<Client> = matchingClients(typed, clients)

    fun setCategory(text: String) = saveFormState.update { it.copy(category = text, savedConfirmation = false) }
    fun setDeliveryDate(epochDay: Long?) = saveFormState.update { it.copy(deliveryDateEpochDay = epochDay, savedConfirmation = false) }
    fun clearPhoto() = saveFormState.update { it.copy(photo = null, photoFromGCode = false, photoError = null, savedConfirmation = false) }

    /**
     * Escolhe a foto e confere na hora se ela abre: descobrir que a imagem não abre só ao mandar o PDF
     * pro cliente seria tarde demais (e uma imagem que não abre derrubava a tela do Histórico).
     */
    fun pickPhoto() {
        when (val picked = platform.pickFile(FileKind.IMAGE)) {
            is PickResult.Picked -> {
                val readable = runCatching { decodeImageBitmap(picked.file.bytes) }.isSuccess
                saveFormState.update {
                    if (readable) {
                        it.copy(photo = picked.file, photoFromGCode = false, photoError = null, savedConfirmation = false)
                    } else {
                        it.copy(photoError = "Não consegui ler essa imagem. Tente um PNG ou JPG.")
                    }
                }
            }
            is PickResult.Failed -> saveFormState.update { it.copy(photoError = picked.message) }
            PickResult.Cancelled -> Unit
        }
    }

    fun clearStlFile() {
        saveFormState.update { it.copy(stlFile = null, savedConfirmation = false) }
        loadStlPreview(null)
    }

    /**
     * Anexa o arquivo STL do modelo ao orçamento — guardado pra o criador recuperar depois no
     * Histórico e reaproveitar numa venda futura da mesma peça.
     */
    fun pickStl() {
        when (val picked = platform.pickFile(FileKind.STL)) {
            is PickResult.Picked -> {
                saveFormState.update { it.copy(stlFile = picked.file, savedConfirmation = false) }
                loadStlPreview(picked.file)
            }
            is PickResult.Failed -> stlPreviewState.value = StlPreview.Unreadable
            PickResult.Cancelled -> Unit
        }
    }

    /** Usa uma captura do visualizador 3D (`Stl3DViewerState.captureSnapshot`) como foto do orçamento. */
    fun setPhotoFromStlSnapshot(pngBytes: ByteArray) {
        saveFormState.update {
            it.copy(photo = PickedFile("captura_stl.png", pngBytes), photoFromGCode = false, photoError = null, savedConfirmation = false)
        }
    }

    /**
     * Lê e analisa o STL fora do thread da tela: uma malha de centenas de milhares de triângulos levava
     * segundos e travava a tela no meio do desenho.
     */
    private fun loadStlPreview(file: PickedFile?) {
        if (file == null) {
            stlPreviewState.value = null
            return
        }
        stlPreviewState.value = StlPreview.Loading
        scope.launch(background) {
            val preview = runCatching {
                val triangles = peekStlTriangleCount(file.bytes)
                if (triangles > MAX_RENDERABLE_STL_TRIANGLES) {
                    StlPreview.TooComplex(triangles)
                } else {
                    val mesh = parseStl(file.bytes)
                    StlPreview.Ready(mesh, StlAnalyzer.analyze(mesh))
                }
            }.getOrElse { StlPreview.Unreadable }
            withContext(main) {
                // Outro arquivo pode ter sido escolhido enquanto este era lido.
                if (saveFormState.value.stlFile === file) stlPreviewState.value = preview
            }
        }
    }

    /** Ver `SettingsViewModel.consumeSavedConfirmation`. */
    fun consumeSavedConfirmation() = saveFormState.update { it.copy(savedConfirmation = false) }

    fun consumeBlockedMessage() = saveFormState.update { it.copy(blockedMessage = null) }

    /**
     * Salva o orçamento no histórico. Num orçamento novo, o formulário volta ao começo depois de salvar
     * (mantendo pedido/produto): antes, os campos continuavam preenchidos e um segundo Ctrl+S criava um
     * pedido repetido. Reabrindo, atualiza o mesmo orçamento.
     */
    fun saveQuote(calculated: Quote, services: List<QuoteService>) {
        val form = saveFormState.value
        val input = inputState.value
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
        val shippingCost = if (isProduct) 0.0 else parseDecimal(input.shippingCostText) ?: 0.0

        val editing = form.operation as? QuoteOperation.Editing
        if (editing != null) {
            historyRepository.update(
                id = editing.savedQuote.id,
                name = form.name,
                quote = quote,
                services = services,
                photo = form.photo,
                stlFile = form.stlFile,
                sourceLink = form.sourceLink,
                client = client,
                printThumbnails = thumbnails,
                shippingCost = shippingCost,
                deliveryDateEpochDay = form.deliveryDateEpochDay.takeUnless { isProduct },
                category = form.category,
                soldAtCatalogPrice = soldAtCatalogPrice,
            )
            saveFormState.update { it.copy(savedConfirmation = true, savedAsProduct = isProduct, savedNumber = editing.savedQuote.displayNumber) }
            return
        }

        val saved = historyRepository.save(
            name = form.name,
            quote = quote,
            services = services,
            photo = form.photo,
            stlFile = form.stlFile,
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
        )
        inputState.value = QuoteInputState(kind = input.kind)
        saveFormState.value = SaveQuoteFormState(savedConfirmation = true, savedAsProduct = isProduct, savedNumber = saved.displayNumber)
        forgetUndo()
        stlPreviewState.value = null
    }

    /**
     * Reabre [savedQuote] pra edição: preenche as entradas com os valores salvos e o formulário de salvar
     * com os anexos recarregados. Salvar depois disso atualiza o mesmo orçamento (ver [saveQuote]). Enquanto
     * nada que muda o preço for mexido, o cálculo continua o salvo ([currentResult]).
     */
    fun loadForEditing(savedQuote: SavedQuote) {
        val input = inputFrom(savedQuote)
        val form = formFrom(savedQuote)
        inputState.value = input
        setForm(form.copy(operation = QuoteOperation.Editing(savedQuote, input, form)))
    }

    /**
     * Reabre [savedQuote] como um **orçamento novo**: mesmos dados de [loadForEditing], prontos pra
     * ajustar, mas salvar cria uma linha nova no Histórico (data de criação e status novos). Não vêm
     * junto o prazo (uma data de outro pedido, provavelmente já passada, decisão 85) nem o cliente
     * (duplicar é, quase sempre, vender a mesma peça pra outra pessoa). A origem vem junto (decisão 103):
     * reimprimir uma venda do catálogo continua sendo daquele produto no ranking do Dashboard.
     */
    fun duplicateForNewQuote(savedQuote: SavedQuote) {
        inputState.value = inputFrom(savedQuote)
        setForm(
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
        inputState.value = inputFrom(product).copy(kind = QuoteKind.ORDER, targetTotalText = "", announcedUnitPrice = announced)
        setForm(
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
        inputState.value = inputFrom(order).copy(kind = QuoteKind.PRODUCT, shippingCostText = "", targetTotalText = "", announcedUnitPrice = null)
        setForm(
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
        SavedQuoteMapper.inputStateFrom(savedQuote, filaments.value, printers.value, salesChannels.value, ::newId) { key ->
            historyRepository.attachmentBytes(key)?.let { PickedFile("miniatura_do_gcode.${attachmentExtension(key)}", it) }
        }

    private fun formFrom(savedQuote: SavedQuote): SaveQuoteFormState {
        val photo = savedQuote.photoFileName?.let { fileName ->
            historyRepository.photoBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        val stlFile = savedQuote.stlFileName?.let { fileName ->
            historyRepository.stlBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        return SavedQuoteMapper.saveFormFrom(savedQuote, photo, stlFile)
    }

    private fun setForm(form: SaveQuoteFormState) {
        saveFormState.value = form
        forgetUndo()
        loadStlPreview(form.stlFile)
    }

    /** Um formulário novo não desfaz nada do anterior. */
    private fun forgetUndo() {
        photoBeforeGCode = null
        photoGCodePrintId = null
        removedPrint = null
    }

    /**
     * Atalho de teclado (Ctrl/Cmd+S): salva o que a tela mostra ([currentResult], a mesma conta). Se não
     * der, diz por quê em [SaveQuoteFormState.blockedMessage], em vez de não fazer nada. Devolve se salvou.
     */
    fun saveCurrentQuote(): Boolean {
        val result = currentResult()
        val quote = result.quote
        val blocked = when {
            result.fieldErrors.isNotEmpty() -> "Não salvei: ${result.fieldErrors.values.first()}"
            quote == null -> result.errorMessage?.let { "Não salvei: $it" }
                ?: "Não salvei: preencha filamento, impressora, comprimento e tempo."
            result.missingServicePrice -> "Não salvei: informe o valor de cada serviço marcado."
            else -> null
        }
        if (blocked != null || quote == null) {
            saveFormState.update { it.copy(blockedMessage = blocked) }
            return false
        }
        saveQuote(quote, result.selectedServices)
        return true
    }

    /** Reabrindo um orçamento, se algo foi mudado desde que ele abriu (cancelar perderia a mudança). */
    val hasUnsavedEdits: Boolean
        get() {
            val editing = saveFormState.value.operation as? QuoteOperation.Editing ?: return false
            val form = saveFormState.value.copy(operation = null, savedConfirmation = false, blockedMessage = null, photoError = null)
            return inputState.value != editing.originalInput || form != editing.originalForm
        }

    /** Se há algo digitado ou uma operação em andamento, que "Limpar" ou outra operação jogaria fora. */
    val hasDraft: Boolean
        get() {
            val input = inputState.value
            val form = saveFormState.value.copy(savedConfirmation = false, savedAsProduct = false, savedNumber = null, blockedMessage = null)
            return input != QuoteInputState(kind = input.kind) || form != SaveQuoteFormState()
        }

    /** Limpa a peça e o formulário de salvar, pra começar um orçamento novo (Ctrl/Cmd+N, cancelar). */
    fun resetForm() {
        inputState.value = QuoteInputState()
        saveFormState.value = SaveQuoteFormState()
        forgetUndo()
        stlPreviewState.value = null
    }

    /**
     * A conta da tela, do Ctrl+S e do salvar: uma fonte só. Reabrindo um orçamento sem mudar nada que
     * mexe no preço, o resultado é o cálculo congelado dele (decisão 108): corrigir o contato do cliente
     * não pode trocar o preço que o cliente já recebeu pelos custos de hoje. Com alguma mudança, a conta é
     * refeita e o preço antigo vem junto, pra tela mostrar a diferença.
     */
    fun currentResult(): QuoteResult {
        val input = inputState.value
        val result = calculate(filaments.value, printers.value, settings.value, services.value, input, salesChannels.value)
        val editing = saveFormState.value.operation as? QuoteOperation.Editing ?: return result
        val original = editing.savedQuote
        return if (input.pricingKey() == editing.originalInput.pricingKey()) {
            result.copy(
                quote = original.quote,
                selectedServices = original.services,
                shippingCost = original.shippingCost,
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
        channels: List<SalesChannel> = salesChannels.value,
    ): QuoteResult {
        val errors = LinkedHashMap<String, String>()
        val quantity = input.quantityOrNull ?: 1.also { errors[QuoteFields.QUANTITY] = "Quantidade: use um número inteiro, 1 ou mais." }
        val selectedServices = input.selectedServices.mapNotNull { (id, serviceInput) ->
            val price = parseDecimal(serviceInput.priceText)?.takeIf { it >= 0 }
            if (price == null) {
                if (serviceInput.priceText.isNotBlank()) errors[QuoteFields.service(id)] = "Valor de ${serviceInput.name}: não é um número."
                return@mapNotNull null
            }
            QuoteService(
                id = id,
                name = services.find { it.id == id }?.name ?: serviceInput.name,
                price = price,
                chargedPerOrder = serviceInput.chargedPerOrder,
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
        // digitado nele não pode mexer no preço.
        val shippingCost = if (input.isProduct) 0.0 else amount(input.shippingCostText, QuoteFields.SHIPPING, "Frete", errors) ?: 0.0
        val servicesTotal = selectedServices.sumOf { it.total(quantity) }
        // O preço alvo é o total que o cliente paga, então serviços e frete saem antes de sobrar o
        // que de fato é a peça. Um alvo que nem cobre os extras é erro no campo: zerar a peça salvaria
        // um total diferente do digitado, sem ninguém ver.
        val target = amount(input.targetTotalText, QuoteFields.TARGET, "Preço", errors)?.let { typed ->
            val extras = servicesTotal + shippingCost
            if (typed < extras) {
                errors[QuoteFields.TARGET] = "Esse preço não cobre serviços e frete (${extras.toInputText()}): digite pelo menos esse valor."
                null
            } else {
                typed
            }
        }
        val negotiatedSalePrice = target?.let { it - servicesTotal - shippingCost }
            ?: input.announcedUnitPrice?.takeUnless { input.isProduct }?.let { it * quantity }
        val laborMinutes = if (input.laborMinutesText.isBlank()) {
            0.0
        } else {
            parseDurationMinutes(input.laborMinutesText) ?: 0.0.also {
                errors[QuoteFields.LABOR] = "Tempo de trabalho: use minutos (90) ou horas e minutos (1h30)."
            }
        }

        val base = QuoteResult(
            prints = resolved,
            selectedServices = selectedServices,
            salesChannel = channel,
            shippingCost = shippingCost,
            missingServicePrice = missingServicePrice,
        )
        missingFromCatalog(input, resolved)?.let { return base.copy(errorMessage = it, fieldErrors = errors) }
        val jobs = printJobs(input, resolved, errors)
        if (errors.isNotEmpty()) return base.copy(fieldErrors = errors)
        jobs ?: return base
        return runCatching {
            PricingCalculator.calculate(
                prints = jobs,
                settings = settings,
                channel = channel,
                quantity = quantity,
                // O tempo digitado já é do pedido inteiro, então entra uma vez só, sem multiplicar.
                laborMinutes = laborMinutes,
                negotiatedSalePrice = negotiatedSalePrice,
                extrasTotal = servicesTotal + shippingCost,
            )
        }.fold(
            onSuccess = { base.copy(quote = it) },
            onFailure = { error ->
                // As mensagens do `core` que chegam aqui já foram escritas pra tela (a de canal + imposto
                // chegando a 100%); o resto é validação que a tela já fez, e não deveria acontecer.
                val message = error.message?.takeIf { it.startsWith("A taxa do canal") } ?: "Confira os valores digitados."
                base.copy(errorMessage = message)
            },
        )
    }

    /** Valor em dinheiro opcional: vazio é `null`, e o que não é número (ou é negativo) vira erro no campo. */
    private fun amount(text: String, field: String, label: String, errors: MutableMap<String, String>): Double? {
        if (text.isBlank()) return null
        val value = parseDecimal(text, NumberKind.AMOUNT)
        return when {
            value == null -> null.also { errors[field] = "$label: não é um número. Use vírgula nos centavos (19,90)." }
            value < 0 -> null.also { errors[field] = "$label não pode ser negativo." }
            else -> value
        }
    }

    /** Um filamento ou impressora escolhidos que saíram do cadastro (orçamento reaberto), com o nome. */
    private fun missingFromCatalog(input: QuoteInputState, resolved: List<ResolvedPrint>): String? {
        // Canal que saiu do cadastro: recalcular sem a taxa baixaria o preço em silêncio. Escolher outro
        // canal, ou "Venda direta", limpa isto (ver [selectSalesChannel]).
        input.missingChannelName?.let { name ->
            return "O canal \"$name\" não existe mais. Escolha outro canal, ou \"Venda direta\", pra calcular o preço."
        }
        input.prints.zip(resolved).forEach { (print, resolvedPrint) ->
            if (print.printerId != null && resolvedPrint.printer == null) {
                return "A impressora \"${print.missingPrinterName ?: "escolhida"}\" não está mais cadastrada. Escolha outra."
            }
            print.filaments.zip(resolvedPrint.filaments).forEach { (row, resolvedFilament) ->
                if (row.filamentId != null && resolvedFilament.filament == null) {
                    return "O filamento \"${row.missingFilamentName ?: "escolhido"}\" não está mais cadastrado. Escolha outro."
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
        errors: MutableMap<String, String>,
    ): List<Pair<PrintJob, PrinterProfile>>? {
        var complete = true
        val jobs = input.prints.zip(resolved).map { (print, resolvedPrint) ->
            val usages = print.filaments.zip(resolvedPrint.filaments).map { (row, resolvedFilament) ->
                val length = parseDecimal(row.lengthText, NumberKind.MEASURE)
                when {
                    row.weightText != null && row.weightText.isNotBlank() && (parseDecimal(row.weightText, NumberKind.AMOUNT) ?: -1.0) < 0 ->
                        errors[QuoteFields.length(print.id, row.id)] = "Peso: use um número em gramas (42 ou 42,5)."
                    row.lengthText.isNotBlank() && (length == null || length < 0) ->
                        errors[QuoteFields.length(print.id, row.id)] = "Comprimento: use um número em metros (27,5)."
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
                errors[QuoteFields.runs(print.id)] = "Vezes: use um número inteiro, 1 ou mais."
            }
            val time = print.printTimeMinutes
            if (print.printTimeText.isNotBlank() && time == null) {
                errors[QuoteFields.printTime(print.id)] = "Tempo de impressão: use minutos (200) ou horas e minutos (3h20)."
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
        channels: List<SalesChannel> = salesChannels.value,
    ): List<Pair<PrinterProfile, Quote>> {
        val candidates = printers.filterNot { it.archived }
        if (candidates.size < 2) return emptyList()
        return candidates.mapNotNull { printer ->
            val allOnThisPrinter = input.copy(prints = input.prints.map { it.copy(printerId = printer.id, missingPrinterName = null) })
            val result = calculate(filaments, listOf(printer), settings, services, allOnThisPrinter, channels)
            result.quote?.let { printer to it }
        }
    }

    private companion object {
        /** Acima disso, a prévia 3D não é desenhada (decisão 63): o arquivo é salvo normalmente. */
        const val MAX_RENDERABLE_STL_TRIANGLES = 500_000L
    }
}

private fun <T> List<T>.replaced(index: Int, transform: (T) -> T): List<T> = mapIndexed { i, item -> if (i == index) transform(item) else item }
