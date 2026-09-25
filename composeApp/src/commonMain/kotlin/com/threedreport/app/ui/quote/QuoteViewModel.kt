package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.pickGCodeFile
import com.threedreport.app.platform.pickImageFile
import com.threedreport.app.platform.pickStlFile
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.report.PrintQueueReport
import com.threedreport.core.report.PrinterQueueEntry
import com.threedreport.core.slicer.CatalogMatcher
import com.threedreport.core.slicer.ExtruderMatch
import com.threedreport.core.slicer.FilamentMatch
import com.threedreport.core.slicer.GCodeMetadata
import com.threedreport.core.slicer.GCodeMetadataParser
import com.threedreport.core.slicer.PrinterMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.round

/**
 * ViewModel da tela de Orçamento.
 *
 * [filaments], [printers], [services] e [settings] são repassados diretamente
 * dos repositórios compartilhados (sem cópia), então refletem na hora
 * qualquer cadastro/edição feito nas outras telas. [calculate] é uma função
 * pura, chamada pela tela a cada recomposição com os valores atuais desses
 * fluxos.
 */
class QuoteViewModel(
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
    serviceRepository: ServiceRepository,
    salesChannelRepository: SalesChannelRepository,
    private val historyRepository: QuoteHistoryRepository,
    /** Pedido ou produto, conforme o perfil de uso (decisão 103): o que vem escolhido num orçamento novo. */
    private val defaultKind: () -> QuoteKind = { QuoteKind.ORDER },
) {
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings
    val services: StateFlow<List<Service>> = serviceRepository.services
    val salesChannels: StateFlow<List<SalesChannel>> = salesChannelRepository.channels

    private val inputState = MutableStateFlow(QuoteInputState(kind = defaultKind()))
    val input: StateFlow<QuoteInputState> = inputState.asStateFlow()

    private val saveFormState = MutableStateFlow(SaveQuoteFormState())
    val saveForm: StateFlow<SaveQuoteFormState> = saveFormState.asStateFlow()

    /** Histórico, só pra dica de prazo saber o que já está na fila de cada impressora. */
    val savedQuotes: StateFlow<List<SavedQuote>> = historyRepository.savedQuotes

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

    fun selectFilament(id: String, print: Int = 0, slot: Int = 0) = updateFilament(print, slot) { it.copy(filamentId = id, colorId = null) }
    fun selectFilamentColor(id: String, print: Int = 0, slot: Int = 0) = updateFilament(print, slot) { it.copy(colorId = id) }
    fun selectPrinter(id: String, print: Int = 0) = updatePrint(print) { it.copy(printerId = id) }
    fun setLengthMeters(text: String, print: Int = 0, slot: Int = 0) = updatePrint(print) { current ->
        current.copy(filaments = current.filaments.replaced(slot) { it.copy(lengthText = text) }, gcodeImportMessage = null)
    }
    fun setPrintTimeMinutes(text: String, print: Int = 0) = updatePrint(print) { it.copy(printTimeText = text, gcodeImportMessage = null) }

    /**
     * "+ Adicionar filamento" (decisão 105). A linha nova já vem com o filamento da última, porque
     * numa peça multicolor o mais comum é o mesmo filamento em outra cor; a cor e os metros ficam
     * pra escolher.
     */
    fun addFilament(print: Int = 0) = updatePrint(print) { current ->
        val inStock = filaments.value.filter { it.hasStockAvailable }
        // Com uma linha só, nada escolhido (ou um filamento que esgotou) aparece como o primeiro em
        // estoque (ver [calculate]). Com duas, essa regra não vale mais, então o que a tela mostrava vira
        // escolha de verdade; senão a linha 1 trocaria sozinha pra "Escolha o filamento".
        val rows = if (current.filaments.size == 1) {
            current.filaments.map { row -> if (inStock.none { it.id == row.filamentId }) row.copy(filamentId = inStock.firstOrNull()?.id) else row }
        } else {
            current.filaments
        }
        current.copy(filaments = rows + FilamentInput(filamentId = rows.last().filamentId))
    }

    /** Remove uma linha de filamento; a última que sobra não sai, porque impressão sem filamento não existe. */
    fun removeFilament(slot: Int, print: Int = 0) = updatePrint(print) { current ->
        if (current.filaments.size <= 1) current else current.copy(filaments = current.filaments.filterIndexed { index, _ -> index != slot })
    }

    private fun updatePrint(print: Int, transform: (PrintInput) -> PrintInput) = inputState.update { input ->
        if (print !in input.prints.indices) input else input.copy(prints = input.prints.replaced(print, transform))
    }

    private fun updateFilament(print: Int, slot: Int, transform: (FilamentInput) -> FilamentInput) = updatePrint(print) { current ->
        if (slot !in current.filaments.indices) current else current.copy(filaments = current.filaments.replaced(slot, transform))
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

    /** Abre o seletor de arquivo e importa o G-code escolhido (ver [importGCode]). */
    fun pickAndImportGCode() {
        val picked = pickGCodeFile() ?: return
        importGCode(picked)
    }

    /**
     * Monta o orçamento a partir de um G-code, escolhido pelo botão ou arrastado pra janela
     * (decisão 89): preenche comprimento de filamento, tempo de impressão, configurações de
     * impressão (ver [PrintSettings]) e a foto, se o arquivo tiver miniatura e nenhuma foto já
     * tiver sido escolhida (não sobrescreve uma foto própria). Além disso, escolhe a impressora e
     * o filamento que o fatiador gravou, **quando batem com os cadastrados** ([CatalogMatcher]):
     * o que só parece ou não está cadastrado vira explicação na mensagem, nunca escolha.
     *
     * Tudo continua editável depois — é um atalho pra preencher, não uma trava. [undoGCodeImport]
     * desfaz de uma vez, inclusive a impressora e o filamento que estavam escolhidos antes.
     */
    fun importGCode(file: PickedFile, print: Int = 0) {
        unsupportedGCodeMessage(file.fileName)?.let { message ->
            updatePrint(print) { it.copy(gcodeImportMessage = message) }
            return
        }

        val metadata = GCodeMetadataParser.parse(file.bytes.decodeToString())
        val thumbnail = metadata.thumbnail
        val photoApplied = thumbnail != null && saveFormState.value.photo == null
        val printSettingsApplied = metadata.layerHeightMm != null || metadata.infillPercentage != null ||
            metadata.infillPattern != null || metadata.supportsEnabled != null
        if (photoApplied || printSettingsApplied) {
            saveFormState.update { form ->
                form.copy(
                    photo = if (photoApplied) PickedFile("miniatura_do_gcode.${thumbnail.fileExtension}", thumbnail.bytes) else form.photo,
                    photoFromGCode = if (photoApplied) true else form.photoFromGCode,
                    photoReferenceFileName = if (photoApplied) null else form.photoReferenceFileName,
                    printSettings = form.printSettings.copy(
                        layerHeightMm = metadata.layerHeightMm ?: form.printSettings.layerHeightMm,
                        infillPercentage = metadata.infillPercentage ?: form.printSettings.infillPercentage,
                        infillPattern = metadata.infillPattern ?: form.printSettings.infillPattern,
                        supportsEnabled = metadata.supportsEnabled ?: form.printSettings.supportsEnabled,
                    ),
                    savedConfirmation = false,
                )
            }
        }

        val current = inputState.value.prints.getOrNull(print) ?: return
        val inStock = filaments.value.filter { it.hasStockAvailable }
        val preferredFilamentIds = current.filaments.mapNotNull { it.filamentId }.ifEmpty { listOfNotNull(inStock.firstOrNull()?.id) }
        val printerMatch = CatalogMatcher.matchPrinter(metadata, printers.value)
        val extruders = CatalogMatcher.matchFilaments(metadata, filaments.value, preferredFilamentIds)
        val chosenPrinter = (printerMatch as? PrinterMatch.Found)?.printer
        // Peça multicolor: uma linha por extrusor, cada uma com o próprio consumo (decisão 105). O que
        // não casou fica sem filamento, pra escolher: cobrar a linha pelo filamento errado mudaria o
        // preço sem ninguém ver.
        val perExtruder = extruders.size > 1 && extruders.all { it.lengthMeters != null }
        val rows = if (perExtruder) {
            extruders.map { extruder ->
                val found = extruder.match as? FilamentMatch.Found
                FilamentInput(filamentId = found?.filament?.id, colorId = found?.color?.id, lengthText = formatImportedNumber(extruder.lengthMeters!!))
            }
        } else if (extruders.size > 1 && current.filaments.size > 1) {
            // Vários extrusores sem o consumo de cada um, e a pessoa já tinha separado as linhas à mão:
            // juntar tudo numa linha cobraria tudo pelo primeiro filamento. As linhas ficam como estão.
            current.filaments
        } else {
            val first = current.filaments.first()
            val single = extruders.singleOrNull()?.match as? FilamentMatch.Found
            // Vários slots que casam com o mesmo filamento (ex.: quatro PLA no AMS) ainda dão um
            // filamento só, sem cor, que não dá pra saber.
            val common = extruders.map { (it.match as? FilamentMatch.Found)?.filament }.distinct().singleOrNull()
            val filament = single?.filament ?: common
            listOf(
                FilamentInput(
                    filamentId = filament?.id ?: first.filamentId,
                    colorId = if (filament != null) single?.color?.id else first.colorId,
                    lengthText = metadata.filamentLengthMeters?.let(::formatImportedNumber) ?: first.lengthText,
                ),
            )
        }
        val keptManualRows = rows === current.filaments

        updatePrint(print) {
            it.copy(
                printerId = chosenPrinter?.id ?: it.printerId,
                filaments = rows,
                printTimeText = metadata.printTimeMinutes?.let(::formatImportedNumber) ?: it.printTimeText,
                beforeGCode = it.beforeGCode ?: it.copy(gcodeImportMessage = null),
                gcodeImportMessage = gcodeImportMessage(metadata, photoApplied, printerMatch, extruders, perExtruder, keptManualRows),
            )
        }
    }

    /**
     * Desfaz a importação de G-code da impressão [print]: devolve a impressão exatamente como estava
     * antes da primeira importação (impressora, linhas de filamento, comprimentos e tempo), limpa as
     * configurações de impressão e, se a foto atual também veio de lá, remove ela também, sem mexer
     * numa foto que o usuário tenha escolhido manualmente antes ou depois.
     */
    fun undoGCodeImport(print: Int = 0) {
        updatePrint(print) { it.beforeGCode ?: it.copy(gcodeImportMessage = null) }
        saveFormState.update {
            it.copy(
                photo = if (it.photoFromGCode) null else it.photo,
                photoFromGCode = if (it.photoFromGCode) false else it.photoFromGCode,
                printSettings = PrintSettings(),
                savedConfirmation = false,
            )
        }
    }

    /** Substitui as configurações de impressão do formulário de salvar (ver [PrintSettings]). */
    fun setPrintSettings(printSettings: PrintSettings) = saveFormState.update {
        it.copy(printSettings = printSettings, savedConfirmation = false)
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
            priceText = service.suggestedPrice?.let(::formatSavedNumber).orEmpty(),
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

    fun applyShowcasePrice(value: Double) = inputState.update { it.copy(targetTotalText = formatSavedNumber(value)) }

    fun setSaveName(text: String) = saveFormState.update { it.copy(name = text, savedConfirmation = false) }
    fun setSourceLink(text: String) = saveFormState.update { it.copy(sourceLink = text, savedConfirmation = false) }
    fun setClientName(text: String) = saveFormState.update { it.copy(clientName = text, savedConfirmation = false) }
    fun setClientContact(text: String) = saveFormState.update { it.copy(clientContact = text, savedConfirmation = false) }
    fun setCategory(text: String) = saveFormState.update { it.copy(category = text, savedConfirmation = false) }
    fun setDeliveryDate(epochDay: Long?) = saveFormState.update { it.copy(deliveryDateEpochDay = epochDay, savedConfirmation = false) }
    fun clearPhoto() = saveFormState.update {
        it.copy(photo = null, photoFromGCode = false, photoReferenceFileName = null, savedConfirmation = false)
    }

    fun pickPhoto() {
        val picked = pickImageFile() ?: return
        saveFormState.update {
            it.copy(photo = picked, photoFromGCode = false, photoReferenceFileName = null, savedConfirmation = false)
        }
    }

    fun clearStlFile() = saveFormState.update {
        it.copy(stlFile = null, stlReferenceFileName = null, savedConfirmation = false)
    }

    /**
     * Anexa o arquivo STL do modelo ao orçamento — guardado pra o criador recuperar depois no
     * Histórico e reaproveitar numa venda futura da mesma peça (não usado pra visualização/cálculo
     * ainda, ver Fase 1 do roadmap).
     */
    fun pickStl() {
        val picked = pickStlFile() ?: return
        saveFormState.update { it.copy(stlFile = picked, stlReferenceFileName = null, savedConfirmation = false) }
    }

    /** Usa uma captura do visualizador 3D (`Stl3DViewerState.captureSnapshot`) como foto do orçamento. */
    fun setPhotoFromStlSnapshot(pngBytes: ByteArray) {
        saveFormState.update {
            it.copy(
                photo = PickedFile("captura_stl.png", pngBytes),
                photoFromGCode = false,
                photoReferenceFileName = null,
                savedConfirmation = false,
            )
        }
    }

    /** Ver `SettingsViewModel.consumeSavedConfirmation`. */
    fun consumeSavedConfirmation() = saveFormState.update { it.copy(savedConfirmation = false) }

    fun saveQuote(quote: Quote, services: List<QuoteService>) {
        val form = saveFormState.value
        val isProduct = inputState.value.isProduct
        // Pelo "Vender", sem outro preço digitado: o pedido saiu pelo preço do catálogo, e isso não é
        // negociação com o cliente (decisão 103).
        val soldAtCatalogPrice = !isProduct && inputState.value.announcedUnitPrice != null && inputState.value.targetTotalText.isBlank()
        val client = if (isProduct) null else form.clientName.trim().ifEmpty { null }?.let { name ->
            Client(name = name, contact = form.clientContact.trim().ifEmpty { null })
        }
        val editingId = form.editingQuoteId
        if (editingId != null) {
            historyRepository.update(
                id = editingId,
                name = form.name,
                quote = quote,
                services = services,
                photo = form.photo,
                photoReferenceFileName = form.photoReferenceFileName,
                stlFile = form.stlFile,
                stlReferenceFileName = form.stlReferenceFileName,
                sourceLink = form.sourceLink,
                client = client,
                printSettings = form.printSettings.takeUnless { it.isEmpty },
                shippingCost = shippingCostToSave(),
                deliveryDateEpochDay = form.deliveryDateEpochDay.takeUnless { isProduct },
                category = form.category,
                soldAtCatalogPrice = soldAtCatalogPrice,
            )
        } else {
            historyRepository.save(
                name = form.name,
                quote = quote,
                services = services,
                photo = form.photo,
                photoReferenceFileName = form.photoReferenceFileName,
                stlFile = form.stlFile,
                stlReferenceFileName = form.stlReferenceFileName,
                sourceLink = form.sourceLink,
                client = client,
                printSettings = form.printSettings.takeUnless { it.isEmpty },
                shippingCost = shippingCostToSave(),
                deliveryDateEpochDay = form.deliveryDateEpochDay.takeUnless { isProduct },
                kind = inputState.value.kind,
                sourceProductId = form.sourceProductId.takeUnless { isProduct },
                category = form.category,
                soldAtCatalogPrice = soldAtCatalogPrice,
            )
        }
        saveFormState.value = SaveQuoteFormState(savedConfirmation = true, savedAsProduct = isProduct)
    }

    /** Produto não tem frete (decisão 101): o campo some da tela, e o que ficou digitado nele não vai junto. */
    private fun shippingCostToSave(): Double =
        if (inputState.value.isProduct) 0.0 else parseDecimal(inputState.value.shippingCostText) ?: 0.0

    /**
     * Reabre [savedQuote] pra edição na aba Orçamento: preenche filamento/cor/impressora/
     * comprimento/tempo/serviços/marketplace com os valores salvos, e o formulário de salvar
     * (nome/foto/STL/link/cliente) com os anexos recarregados do disco. Salvar depois disso
     * atualiza o mesmo orçamento no histórico (ver [saveQuote]) em vez de criar um novo — não
     * muda [SavedQuote.savedAtEpochMillis], só marca [SavedQuote.lastEditedEpochMillis].
     */
    fun loadForEditing(savedQuote: SavedQuote) {
        inputState.value = inputStateFrom(savedQuote)
        saveFormState.value = saveFormFrom(savedQuote).copy(editingQuoteId = savedQuote.id)
    }

    /**
     * Reabre [savedQuote] como um **orçamento novo** na aba Orçamento — mesmos dados de
     * [loadForEditing] (filamento/impressora/comprimento/tempo/serviços/nome/foto/STL/link/
     * cliente, prontos pra ajustar), mas sem marcar `editingQuoteId`: salvar cria uma linha nova no
     * Histórico (data de criação e status `ORCADO` novos), em vez de sobrescrever o original. Se a
     * foto/STL não mudarem antes de salvar, o arquivo em disco é reaproveitado, não duplicado (ver
     * `QuoteHistoryRepository.save`).
     */
    fun duplicateForNewQuote(savedQuote: SavedQuote) {
        inputState.value = inputStateFrom(savedQuote)
        // O prazo não vem junto: uma data de outro pedido, provavelmente já passada, é exatamente o
        // prazo vencido que o cliente não pode receber (decisão 85).
        // A origem vem junto (decisão 103): reimprimir uma venda do catálogo continua sendo daquele
        // produto no ranking do Dashboard.
        saveFormState.value = saveFormFrom(savedQuote).copy(
            duplicatedFromName = savedQuote.name,
            deliveryDateEpochDay = null,
            sourceProductId = savedQuote.sourceProductId,
        )
    }

    /**
     * "Vender" um produto do catálogo (decisão 101): mesmo caminho de [duplicateForNewQuote], mas o
     * que nasce é um **pedido**, que guarda de qual produto veio ([SavedQuote.sourceProductId]). Sem
     * cliente e sem prazo, que são da venda nova. O produto continua no catálogo, intacto. Com preço
     * anunciado, a peça sai por ele ([QuoteInputState.announcedUnitPrice]), e frete e serviços somam
     * por fora (decisão 102).
     */
    fun sellFromProduct(product: SavedQuote) {
        val announced = product.quote.takeIf { it.isNegotiated }?.unitSalePrice
        inputState.value = inputStateFrom(product).copy(kind = QuoteKind.ORDER, targetTotalText = "", announcedUnitPrice = announced)
        saveFormState.value = saveFormFrom(product).copy(
            sourceProductId = product.id,
            soldFromProductName = product.name,
            clientName = "",
            clientContact = "",
            deliveryDateEpochDay = null,
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
        inputState.value = inputStateFrom(order).copy(kind = QuoteKind.PRODUCT, shippingCostText = "", targetTotalText = "", announcedUnitPrice = null)
        saveFormState.value = saveFormFrom(order).copy(
            copiedFromOrderName = order.name,
            clientName = "",
            clientContact = "",
            deliveryDateEpochDay = null,
        )
    }

    private fun inputStateFrom(savedQuote: SavedQuote): QuoteInputState {
        val quote = savedQuote.quote
        // Pelo id; um canal excluído e recriado com o mesmo nome ganha id novo, então o nome é a segunda
        // tentativa. Sem nenhum dos dois, a tela avisa: recalcular sem a taxa em silêncio baixaria o preço.
        val channel = quote.channelId?.let { id -> salesChannels.value.firstOrNull { it.id == id } }
            ?: quote.channelName?.let { name -> salesChannels.value.firstOrNull { it.name == name } }
        return QuoteInputState(
            kind = savedQuote.kind,
            prints = quote.prints.map { print ->
                PrintInput(
                    name = print.job.name.orEmpty(),
                    printerId = print.printerId,
                    filaments = print.job.filaments.map { usage ->
                        FilamentInput(filamentId = usage.filament.id, colorId = usage.color?.id, lengthText = formatSavedNumber(usage.lengthMeters))
                    },
                    printTimeText = formatSavedNumber(print.job.printTimeMinutes),
                    runsText = if (print.job.runs > 1) print.job.runs.toString() else "",
                )
            },
            laborMinutesText = if (quote.laborMinutes > 0) formatSavedNumber(quote.laborMinutes) else "",
            quantityText = if (quote.quantity > 1) quote.quantity.toString() else "",
            // Valor e forma de cobrança vêm do retrato salvo, não do catálogo atual: reabrir e salvar
            // não pode reprecificar o pedido em silêncio.
            selectedServices = savedQuote.services.associate { service ->
                service.id to ServiceInput(
                    name = service.name,
                    priceText = formatSavedNumber(service.price),
                    chargedPerOrder = service.chargedPerOrder,
                )
            },
            salesChannelId = channel?.id,
            missingChannelName = quote.channelName.takeIf { channel == null },
            shippingCostText = if (savedQuote.shippingCost > 0) formatSavedNumber(savedQuote.shippingCost) else "",
            // Sem isso, reabrir um orçamento negociado e salvar de novo voltaria em silêncio pro preço
            // de tabela. O campo recebe o total do cliente, igual ao que foi digitado (ver `calculate`).
            // Pedido vendido pelo preço do catálogo volta com o anunciado no lugar dele, e não como preço
            // digitado: salvar de novo mantém o preço e continua fora dos números de negociação.
            targetTotalText = if (quote.isNegotiated && !savedQuote.soldAtCatalogPrice) formatSavedNumber(savedQuote.totalWithServices) else "",
            announcedUnitPrice = if (savedQuote.soldAtCatalogPrice) quote.unitSalePrice else null,
        )
    }

    private fun saveFormFrom(savedQuote: SavedQuote): SaveQuoteFormState {
        val photo = savedQuote.photoFileName?.let { fileName ->
            historyRepository.photoBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        val stlFile = savedQuote.stlFileName?.let { fileName ->
            historyRepository.stlBytes(savedQuote)?.let { bytes -> PickedFile(fileName, bytes) }
        }
        return SaveQuoteFormState(
            name = savedQuote.name,
            photo = photo,
            photoReferenceFileName = if (photo != null) savedQuote.photoFileName else null,
            stlFile = stlFile,
            stlReferenceFileName = if (stlFile != null) savedQuote.stlFileName else null,
            sourceLink = savedQuote.sourceLink.orEmpty(),
            clientName = savedQuote.client?.name.orEmpty(),
            clientContact = savedQuote.client?.contact.orEmpty(),
            printSettings = savedQuote.printSettings ?: PrintSettings(),
            deliveryDateEpochDay = savedQuote.deliveryDateEpochDay,
            category = savedQuote.category.orEmpty(),
        )
    }

    /** Atalho de teclado (Ctrl/Cmd+S): recalcula com os valores atuais e salva, se houver um orçamento válido. */
    fun saveCurrentQuote() {
        // A mesma lista que a tela usa (só o que está em estoque): o atalho não pode salvar um preço
        // diferente do que a pessoa está vendo.
        val result = calculate(filaments.value.filter { it.hasStockAvailable }, printers.value, settings.value, services.value, input.value)
        val quote = result.quote ?: return
        if (result.missingServicePrice) return
        saveQuote(quote, result.selectedServices)
    }

    /** Atalho de teclado (Ctrl/Cmd+N): limpa a peça e o formulário de salvar, pra começar um orçamento novo. */
    /**
     * O perfil de uso mudou em Configurações (decisão 103): o orçamento em branco passa a vir no
     * tipo novo na hora. Se já tem algo digitado, ou uma operação/edição em andamento, fica como
     * está, pra trocar o perfil nunca mexer no que a pessoa estava fazendo.
     */
    fun applyDefaultKindIfUntouched() {
        val input = inputState.value
        val untouched = input == QuoteInputState(kind = input.kind) && saveFormState.value == SaveQuoteFormState()
        if (untouched) inputState.value = QuoteInputState(kind = defaultKind())
    }

    fun resetForm() {
        inputState.value = QuoteInputState(kind = defaultKind())
        saveFormState.value = SaveQuoteFormState()
    }

    fun calculate(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        services: List<Service>,
        input: QuoteInputState,
        channels: List<SalesChannel> = salesChannels.value,
    ): QuoteResult {
        val selectedServices = input.selectedServices.mapNotNull { (id, serviceInput) ->
            val price = parseDecimal(serviceInput.priceText)?.takeIf { it >= 0 } ?: return@mapNotNull null
            QuoteService(
                id = id,
                name = services.find { it.id == id }?.name ?: serviceInput.name,
                price = price,
                chargedPerOrder = serviceInput.chargedPerOrder,
            )
        }
        val missingServicePrice = selectedServices.size < input.selectedServices.size
        val resolved = input.prints.map { print ->
            ResolvedPrint(
                printer = printers.find { it.id == print.printerId } ?: printers.firstOrNull(),
                filaments = print.filaments.map { row ->
                    // Com uma linha só, nada escolhido ainda vale o primeiro filamento: é a tela de
                    // sempre. Numa peça multicolor, cada linha precisa de uma escolha de verdade.
                    val filament = filaments.find { it.id == row.filamentId } ?: filaments.firstOrNull()?.takeIf { print.filaments.size == 1 }
                    val available = filament?.colors?.filter { it.inStock }.orEmpty()
                    ResolvedFilament(filament, available.find { it.id == row.colorId } ?: available.firstOrNull())
                },
            )
        }
        val channel = channels.find { it.id == input.salesChannelId }
        // Produto do catálogo não tem frete (decisão 101): o campo some da tela, e o que tiver ficado
        // digitado nele não pode mexer no preço. O preço fechado, em produto, é o preço anunciado no
        // catálogo (decisão 102), e por isso continua valendo.
        val shippingCost = if (input.isProduct) 0.0 else parseDecimal(input.shippingCostText) ?: 0.0
        // O preço alvo é o total que o cliente paga, então serviços e frete saem antes de sobrar o
        // que de fato é a peça. Se o alvo nem cobre os extras, a peça vale zero e o prejuízo
        // aparece no lucro, que é justamente o aviso.
        val servicesTotal = selectedServices.sumOf { it.total(input.quantity) }
        val negotiatedSalePrice = parseDecimal(input.targetTotalText)
            ?.let { (it - servicesTotal - shippingCost).coerceAtLeast(0.0) }
            ?: input.announcedUnitPrice?.takeUnless { input.isProduct }?.let { it * input.quantity }

        val base = QuoteResult(
            prints = resolved,
            selectedServices = selectedServices,
            salesChannel = channel,
            shippingCost = shippingCost,
            missingServicePrice = missingServicePrice,
        )
        return runCatching {
            val jobs = printJobs(input, resolved) ?: return base
            PricingCalculator.calculate(
                prints = jobs,
                settings = settings,
                channel = channel,
                quantity = input.quantity,
                // O tempo digitado já é do pedido inteiro, então entra uma vez só, sem multiplicar.
                laborMinutes = parseDecimal(input.laborMinutesText) ?: 0.0,
                negotiatedSalePrice = negotiatedSalePrice,
            )
        }.fold(
            onSuccess = { base.copy(quote = it) },
            onFailure = { base.copy(errorMessage = it.message) },
        )
    }

    /** As impressões prontas pro cálculo, ou `null` enquanto falta escolher ou preencher alguma coisa. */
    private fun printJobs(input: QuoteInputState, resolved: List<ResolvedPrint>): List<Pair<PrintJob, PrinterProfile>>? =
        input.prints.zip(resolved).map { (print, resolvedPrint) ->
            val printer = resolvedPrint.printer ?: return null
            val usages = print.filaments.zip(resolvedPrint.filaments).map { (row, resolvedFilament) ->
                val filament = resolvedFilament.filament ?: return null
                val length = parseDecimal(row.lengthText) ?: return null
                FilamentUsage(filament = filament, lengthMeters = length, color = resolvedFilament.color)
            }
            val time = parseDecimal(print.printTimeText) ?: return null
            PrintJob(filaments = usages, printTimeMinutes = time, runs = print.runs, name = print.name.trim().ifEmpty { null }) to printer
        }

    /**
     * O mesmo orçamento calculado em cada impressora cadastrada, com todas as impressões nela, pra
     * responder "em qual máquina essa peça sai mais barata". Só faz sentido com mais de uma impressora; devolve lista vazia
     * quando não há o que comparar ou quando os dados da peça ainda não dão um cálculo válido.
     */
    fun comparePrinters(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        services: List<Service>,
        input: QuoteInputState,
        channels: List<SalesChannel> = salesChannels.value,
    ): List<Pair<PrinterProfile, Quote>> {
        if (printers.size < 2) return emptyList()
        return printers.mapNotNull { printer ->
            val allOnThisPrinter = input.copy(prints = input.prints.map { it.copy(printerId = printer.id) })
            val result = calculate(filaments, listOf(printer), settings, services, allOnThisPrinter, channels)
            result.quote?.let { printer to it }
        }
    }

    private fun gcodeImportMessage(
        metadata: GCodeMetadata,
        photoApplied: Boolean,
        printerMatch: PrinterMatch,
        extruders: List<ExtruderMatch>,
        perExtruder: Boolean,
        keptManualRows: Boolean,
    ): String {
        val filled = buildList {
            when {
                perExtruder -> add("consumo de cada filamento")
                keptManualRows -> Unit
                metadata.filamentLengthMeters != null -> add("comprimento de filamento")
            }
            if (metadata.printTimeMinutes != null) add("tempo de impressão")
            if (photoApplied) add("foto do modelo")
            if (metadata.layerHeightMm != null || metadata.infillPercentage != null ||
                metadata.infillPattern != null || metadata.supportsEnabled != null
            ) {
                add("configurações de impressão")
            }
        }
        val sentences = buildList {
            when {
                filled.isNotEmpty() -> add("Preenchido a partir do G-code: ${filled.joinToString(", ")}.")
                // As linhas mantidas já têm os comprimentos; a frase delas explica o resto.
                !keptManualRows -> add("Não encontrei comprimento nem tempo nesse G-code — preencha manualmente.")
            }
            printerSentence(printerMatch)?.let(::add)
            when {
                perExtruder -> add(extrudersSentence(extruders))
                keptManualRows -> add(
                    "O G-code usa ${extruders.size} filamentos, mas não informa o consumo de cada um" +
                        (metadata.filamentLengthMeters?.let { " (${formatImportedNumber(it)} m no total)" } ?: "") +
                        ": mantive as suas linhas de filamento como estavam.",
                )
                extruders.size > 1 -> add(
                    "O G-code usa ${extruders.size} filamentos, mas não informa o consumo de cada um: o total ficou numa " +
                        "linha só. Separe em \"+ Adicionar filamento\" pra cobrar cada um pelo seu preço.",
                )
                else -> extruders.singleOrNull()?.let { filamentSentence(it.match) }?.let(::add)
            }
            if (metadata.thumbnail != null && !photoApplied) add("Havia uma foto nesse G-code, mas mantive a que você já tinha escolhido.")
        }
        return sentences.joinToString(" ")
    }

    private fun printerSentence(match: PrinterMatch): String? = when (match) {
        is PrinterMatch.Found -> "Impressora: ${match.printer.name}."
        is PrinterMatch.Similar ->
            "O G-code é de uma \"${match.gcodeName}\"; a mais parecida cadastrada é \"${match.printer.name}\" — escolha na lista se for ela."
        is PrinterMatch.NotRegistered ->
            "O G-code é de uma \"${match.gcodeName}\", que não está cadastrada (Impressoras → Escolher da lista)."
        PrinterMatch.Unknown -> null
    }

    private fun filamentSentence(match: FilamentMatch): String? = when (match) {
        is FilamentMatch.Found -> "Filamento: ${match.filament.name}" + (match.color?.let { ", cor ${it.displayLabel()}" } ?: "") + "."
        is FilamentMatch.Ambiguous -> "Há ${match.count} filamentos ${match.type} em estoque — escolha qual usou."
        is FilamentMatch.NotRegistered ->
            "O G-code usa ${match.type}" + (match.vendor?.let { " da $it" } ?: "") + ", que não está cadastrado em estoque."
        FilamentMatch.Unknown -> null
    }

    /** "Um filamento por extrusor: 1) PLA, cor Verde; 2) PETG (não cadastrado em estoque, escolha)." */
    private fun extrudersSentence(extruders: List<ExtruderMatch>): String =
        "Um filamento por extrusor: " + extruders.mapIndexed { index, extruder ->
            "${index + 1}) " + when (val match = extruder.match) {
                is FilamentMatch.Found -> match.filament.name + (match.color?.let { ", cor ${it.displayLabel()}" } ?: "")
                is FilamentMatch.Ambiguous -> "${match.type} (há ${match.count} em estoque, escolha qual)"
                is FilamentMatch.NotRegistered -> match.type + (match.vendor?.let { " da $it" } ?: "") + " (não cadastrado em estoque, escolha)"
                FilamentMatch.Unknown -> "material não informado (escolha)"
            }
        }.joinToString("; ") + "."

    /**
     * Arquivos que parecem G-code mas o app ainda não lê, com o caminho pra resolver. `null`
     * quando dá pra tentar ler (inclusive extensão desconhecida, que o parser decide).
     */
    private fun unsupportedGCodeMessage(fileName: String): String? {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".bgcode") ->
                "G-code binário (.bgcode) ainda não é lido. No PrusaSlicer, desligue \"G-code binário\" nas " +
                    "configurações da impressora e exporte de novo."
            name.endsWith(".3mf") ->
                "Esse é um arquivo de projeto (.3mf), não um G-code. No fatiador, use \"Exportar G-code\" e " +
                    "arraste o arquivo .gcode."
            GCODE_EXTENSIONS.none { name.endsWith(it) } ->
                "\"$fileName\" não é um G-code. Use o arquivo .gcode exportado pelo fatiador."
            else -> null
        }
    }

    /**
     * Número salvo de volta num campo, ao reabrir um orçamento ou marcar um serviço. Mantém até 6
     * casas, e não 2 como [formatImportedNumber]: arredondar mudaria o valor ao salvar de novo
     * (2,345 por peça em 1000 peças viraria R$ 5 a mais só por reabrir). As 6 casas ainda limpam o
     * ruído de ponto flutuante de somas (36.190000000000005) e nunca caem em notação científica,
     * que o campo não entenderia. Valores são sempre >= 0.
     */
    private fun formatSavedNumber(value: Double): String {
        val scaled = round(value * 1_000_000).toLong()
        val integerPart = (scaled / 1_000_000).toString()
        val fraction = (scaled % 1_000_000).toString().padStart(6, '0').trimEnd('0')
        return if (fraction.isEmpty()) integerPart else "$integerPart.$fraction"
    }

    /** Arredonda pra 2 casas decimais e evita ".0" à toa (ex.: 5.0 vira "5", não "5.0"). */
    private fun formatImportedNumber(value: Double): String {
        val rounded = round(value * 100) / 100
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }
}

/** Extensões de G-code em texto que os fatiadores exportam. */
private val GCODE_EXTENSIONS = listOf(".gcode", ".gco", ".g")

private fun <T> List<T>.replaced(index: Int, transform: (T) -> T): List<T> = mapIndexed { i, item -> if (i == index) transform(item) else item }
