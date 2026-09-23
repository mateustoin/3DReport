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
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.slicer.GCodeMetadata
import com.threedreport.core.slicer.GCodeMetadataParser
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
) {
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings
    val services: StateFlow<List<Service>> = serviceRepository.services
    val salesChannels: StateFlow<List<SalesChannel>> = salesChannelRepository.channels

    private val inputState = MutableStateFlow(QuoteInputState())
    val input: StateFlow<QuoteInputState> = inputState.asStateFlow()

    private val saveFormState = MutableStateFlow(SaveQuoteFormState())
    val saveForm: StateFlow<SaveQuoteFormState> = saveFormState.asStateFlow()

    fun selectFilament(id: String) = inputState.update { it.copy(filamentId = id, filamentColorId = null) }
    fun selectFilamentColor(id: String) = inputState.update { it.copy(filamentColorId = id) }
    fun selectPrinter(id: String) = inputState.update { it.copy(printerId = id) }
    fun setLengthMeters(text: String) = inputState.update { it.copy(lengthMetersText = text, gcodeImportMessage = null) }
    fun setPrintTimeMinutes(text: String) = inputState.update { it.copy(printTimeMinutesText = text, gcodeImportMessage = null) }
    fun setLaborMinutes(text: String) = inputState.update { it.copy(laborMinutesText = text) }
    fun setQuantity(text: String) = inputState.update { it.copy(quantityText = text) }
    fun setSetupMinutes(text: String) = inputState.update { it.copy(setupMinutesText = text) }

    /**
     * Abre o seletor de arquivo pra escolher um G-code exportado pelo fatiador e preenche
     * comprimento de filamento / tempo de impressão / configurações de impressão (altura de
     * camada, preenchimento, suporte — ver [PrintSettings]) a partir dos comentários de metadados
     * dele ([GCodeMetadataParser]) — e a foto do orçamento, se o arquivo tiver uma miniatura
     * embutida e nenhuma foto já tiver sido escolhida (não sobrescreve uma foto própria do
     * usuário). Tudo continua editável/removível manualmente depois — é um atalho pra preencher,
     * não uma trava, já que nem todo fatiador grava esses dados num formato reconhecido. Ver
     * [undoGCodeImport] pra desfazer de uma vez.
     */
    fun pickAndImportGCode() {
        val picked = pickGCodeFile() ?: return
        val metadata = GCodeMetadataParser.parse(picked.bytes.decodeToString())
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
        inputState.update {
            it.copy(
                lengthMetersText = metadata.filamentLengthMeters?.let(::formatImportedNumber) ?: it.lengthMetersText,
                printTimeMinutesText = metadata.printTimeMinutes?.let(::formatImportedNumber) ?: it.printTimeMinutesText,
                gcodeImportMessage = gcodeImportMessage(metadata, photoApplied),
            )
        }
    }

    /**
     * Desfaz a última importação de G-code: limpa comprimento/tempo/configurações de impressão
     * (volta pro texto em branco/vazio, não pro valor anterior a importar) e, se a foto atual
     * também veio de lá, remove ela também — sem mexer numa foto que o usuário tenha escolhido
     * manualmente antes ou depois.
     */
    fun undoGCodeImport() {
        inputState.update { it.copy(lengthMetersText = "", printTimeMinutesText = "", gcodeImportMessage = null) }
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

    fun toggleService(id: String) = inputState.update {
        it.copy(selectedServiceIds = if (id in it.selectedServiceIds) it.selectedServiceIds - id else it.selectedServiceIds + id)
    }

    fun selectSalesChannel(id: String?) = inputState.update { it.copy(salesChannelId = id) }
    fun setShippingCost(text: String) = inputState.update { it.copy(shippingCostText = text) }
    fun setTargetTotal(text: String) = inputState.update { it.copy(targetTotalText = text) }

    fun setSaveName(text: String) = saveFormState.update { it.copy(name = text, savedConfirmation = false) }
    fun setSourceLink(text: String) = saveFormState.update { it.copy(sourceLink = text, savedConfirmation = false) }
    fun setClientName(text: String) = saveFormState.update { it.copy(clientName = text, savedConfirmation = false) }
    fun setClientContact(text: String) = saveFormState.update { it.copy(clientContact = text, savedConfirmation = false) }
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

    fun saveQuote(quote: Quote, services: List<Service>) {
        val form = saveFormState.value
        val client = form.clientName.trim().ifEmpty { null }?.let { name ->
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
                shippingCost = parseDecimal(inputState.value.shippingCostText) ?: 0.0,
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
                shippingCost = parseDecimal(inputState.value.shippingCostText) ?: 0.0,
            )
        }
        saveFormState.value = SaveQuoteFormState(savedConfirmation = true)
    }

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
        saveFormState.value = saveFormFrom(savedQuote).copy(duplicatedFromName = savedQuote.name)
    }

    private fun inputStateFrom(savedQuote: SavedQuote): QuoteInputState {
        val quote = savedQuote.quote
        val job = quote.job
        return QuoteInputState(
            filamentId = job.filament.id,
            filamentColorId = job.filamentColor?.id,
            printerId = quote.printerId,
            lengthMetersText = formatImportedNumber(job.filamentLengthMeters),
            printTimeMinutesText = formatImportedNumber(job.printTimeMinutes),
            laborMinutesText = if (job.laborMinutes > 0) formatImportedNumber(job.laborMinutes) else "",
            quantityText = if (quote.quantity > 1) quote.quantity.toString() else "",
            setupMinutesText = if (quote.setupMinutes > 0) formatImportedNumber(quote.setupMinutes) else "",
            selectedServiceIds = savedQuote.services.map { it.id }.toSet(),
            salesChannelId = salesChannels.value.firstOrNull { it.name == quote.channelName }?.id,
            shippingCostText = if (savedQuote.shippingCost > 0) formatImportedNumber(savedQuote.shippingCost) else "",
            // Sem isso, reabrir um orçamento negociado e salvar de novo voltaria em silêncio pro preço
            // de tabela. O campo recebe o total do cliente, igual ao que foi digitado (ver `calculate`).
            targetTotalText = if (quote.isNegotiated) formatImportedNumber(savedQuote.totalWithServices) else "",
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
        )
    }

    /** Atalho de teclado (Ctrl/Cmd+S): recalcula com os valores atuais e salva, se houver um orçamento válido. */
    fun saveCurrentQuote() {
        val result = calculate(filaments.value, printers.value, settings.value, services.value, input.value)
        val quote = result.quote ?: return
        saveQuote(quote, result.selectedServices)
    }

    /** Atalho de teclado (Ctrl/Cmd+N): limpa a peça e o formulário de salvar, pra começar um orçamento novo. */
    fun resetForm() {
        inputState.value = QuoteInputState()
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
        val filament = filaments.find { it.id == input.filamentId } ?: filaments.firstOrNull()
        val printer = printers.find { it.id == input.printerId } ?: printers.firstOrNull()
        val selectedServices = services.filter { it.id in input.selectedServiceIds }
        val availableColors = filament?.colors?.filter { it.inStock }.orEmpty()
        val filamentColor = availableColors.find { it.id == input.filamentColorId } ?: availableColors.firstOrNull()
        val length = parseDecimal(input.lengthMetersText)
        val time = parseDecimal(input.printTimeMinutesText)
        val channel = channels.find { it.id == input.salesChannelId }
        val shippingCost = parseDecimal(input.shippingCostText) ?: 0.0
        // O preço alvo é o total que o cliente paga, então serviços e frete saem antes de sobrar o
        // que de fato é a peça. Se o alvo nem cobre os extras, a peça vale zero e o prejuízo
        // aparece no lucro, que é justamente o aviso.
        val servicesTotal = selectedServices.sumOf { it.price } * input.quantity
        val negotiatedSalePrice = parseDecimal(input.targetTotalText)
            ?.let { (it - servicesTotal - shippingCost).coerceAtLeast(0.0) }

        if (filament == null || printer == null || length == null || time == null) {
            return QuoteResult(
                filament = filament,
                filamentColor = filamentColor,
                printer = printer,
                selectedServices = selectedServices,
                salesChannel = channel,
                shippingCost = shippingCost,
            )
        }

        val job = PrintJob(
            filament = filament,
            filamentLengthMeters = length,
            printTimeMinutes = time,
            filamentColor = filamentColor,
            laborMinutes = parseDecimal(input.laborMinutesText) ?: 0.0,
        )
        return runCatching {
            PricingCalculator.calculate(
                job = job,
                printer = printer,
                settings = settings,
                channel = channel,
                quantity = input.quantity,
                setupMinutes = parseDecimal(input.setupMinutesText) ?: 0.0,
                negotiatedSalePrice = negotiatedSalePrice,
            )
        }.fold(
            onSuccess = {
                QuoteResult(
                    filament, filamentColor, printer, quote = it, selectedServices = selectedServices,
                    salesChannel = channel, shippingCost = shippingCost,
                )
            },
            onFailure = {
                QuoteResult(
                    filament, filamentColor, printer, errorMessage = it.message, selectedServices = selectedServices,
                    salesChannel = channel, shippingCost = shippingCost,
                )
            },
        )
    }

    /**
     * O mesmo orçamento calculado em cada impressora cadastrada, pra responder "em qual máquina
     * essa peça sai mais barata". Só faz sentido com mais de uma impressora; devolve lista vazia
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
            val result = calculate(filaments, listOf(printer), settings, services, input.copy(printerId = printer.id), channels)
            result.quote?.let { printer to it }
        }
    }

    private fun gcodeImportMessage(metadata: GCodeMetadata, photoApplied: Boolean): String {
        val filled = buildList {
            if (metadata.filamentLengthMeters != null) add("comprimento de filamento")
            if (metadata.printTimeMinutes != null) add("tempo de impressão")
            if (photoApplied) add("foto do modelo")
            if (metadata.layerHeightMm != null || metadata.infillPercentage != null ||
                metadata.infillPattern != null || metadata.supportsEnabled != null
            ) {
                add("configurações de impressão")
            }
        }
        val skippedPhotoNote = if (metadata.thumbnail != null && !photoApplied) {
            " Havia uma foto nesse G-code, mas mantive a que você já tinha escolhido."
        } else {
            ""
        }
        return if (filled.isEmpty()) {
            "Não encontrei nenhum dado reconhecido nesse G-code — preencha manualmente.$skippedPhotoNote"
        } else {
            "Preenchido a partir do G-code: ${filled.joinToString(", ")}.$skippedPhotoNote"
        }
    }

    /** Arredonda pra 2 casas decimais e evita ".0" à toa (ex.: 5.0 vira "5", não "5.0"). */
    private fun formatImportedNumber(value: Double): String {
        val rounded = round(value * 100) / 100
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }
}
