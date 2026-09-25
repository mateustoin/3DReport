package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.QuoteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class QuoteHistoryRepository actual constructor() {
    private val file = File(appDataDir(), "quotes.json")
    private val photosDir = File(appDataDir(), "photos")
    private val modelsDir = File(appDataDir(), "models")
    private val state = MutableStateFlow(readJsonFile(file, emptyList<SavedQuote>()))

    actual val savedQuotes: StateFlow<List<SavedQuote>> = state.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    actual fun save(
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        photoReferenceFileName: String?,
        stlFile: PickedFile?,
        stlReferenceFileName: String?,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings?,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
        kind: QuoteKind,
        sourceProductId: String?,
    ): SavedQuote {
        val id = Uuid.random().toString()
        val isProduct = kind == QuoteKind.PRODUCT
        val photoFileName = resolveAttachment(photosDir, id, photo, photoReferenceFileName, "img")
        val stlFileName = resolveAttachment(modelsDir, id, stlFile, stlReferenceFileName, "stl")

        val saved = SavedQuote(
            id = id,
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            services = services,
            photoFileName = photoFileName,
            stlFileName = stlFileName,
            sourceLink = sourceLink?.trim()?.ifEmpty { null },
            savedAtEpochMillis = System.currentTimeMillis(),
            client = if (isProduct) null else client,
            printSettings = printSettings,
            shippingCost = if (isProduct) 0.0 else shippingCost,
            deliveryDateEpochDay = if (isProduct) null else deliveryDateEpochDay,
            kind = kind,
            sourceProductId = sourceProductId,
        )
        state.value = state.value + saved
        persist()
        return saved
    }

    actual fun update(
        id: String,
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        photoReferenceFileName: String?,
        stlFile: PickedFile?,
        stlReferenceFileName: String?,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings?,
        shippingCost: Double,
        deliveryDateEpochDay: Long?,
    ): SavedQuote? {
        val existing = state.value.find { it.id == id } ?: return null
        val isProduct = !existing.isOrder
        val photoFileName = resolveAttachment(photosDir, id, photo, photoReferenceFileName, "img")
        val stlFileName = resolveAttachment(modelsDir, id, stlFile, stlReferenceFileName, "stl")

        val updated = existing.copy(
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            services = services,
            photoFileName = photoFileName,
            stlFileName = stlFileName,
            sourceLink = sourceLink?.trim()?.ifEmpty { null },
            client = if (isProduct) null else client,
            lastEditedEpochMillis = System.currentTimeMillis(),
            printSettings = printSettings,
            shippingCost = if (isProduct) 0.0 else shippingCost,
            deliveryDateEpochDay = if (isProduct) null else deliveryDateEpochDay,
        )
        // O novo estado precisa estar visível antes de decidir se o arquivo antigo ainda é
        // referenciado por outra linha (ex.: um orçamento duplicado que ainda aponta pra ele).
        state.value = state.value.map { if (it.id == id) updated else it }
        cleanupIfOrphaned(photosDir, existing.photoFileName, photoFileName) { it.photoFileName }
        cleanupIfOrphaned(modelsDir, existing.stlFileName, stlFileName) { it.stlFileName }
        persist()
        return updated
    }

    actual fun delete(id: String) {
        val removed = state.value.find { it.id == id }
        state.value = state.value.filterNot { it.id == id }
        persist()
        removed?.photoFileName?.let { fileName ->
            if (state.value.none { it.photoFileName == fileName }) File(photosDir, fileName).delete()
        }
        removed?.stlFileName?.let { fileName ->
            if (state.value.none { it.stlFileName == fileName }) File(modelsDir, fileName).delete()
        }
    }

    /**
     * Resolve o nome do arquivo de um anexo (foto ou STL) a gravar num [SavedQuote]:
     * - [referenceFileName] não-nulo: reaproveita esse arquivo já existente **sem** gravar nada de
     *   novo (duplicar um orçamento cujo anexo não mudou, ou editar sem trocar o anexo) — evita
     *   duplicar o mesmo arquivo em disco a cada duplicação/edição.
     * - [picked] não-nulo (e sem referência): grava um arquivo novo, nomeado `"$id.extensão"`.
     * - Nenhum dos dois: sem anexo.
     */
    private fun resolveAttachment(dir: File, id: String, picked: PickedFile?, referenceFileName: String?, defaultExtension: String): String? {
        if (referenceFileName != null) return referenceFileName
        if (picked == null) return null

        val extension = picked.fileName.substringAfterLast('.', defaultExtension)
        val fileName = "$id.$extension"
        dir.mkdirs()
        File(dir, fileName).writeBytes(picked.bytes)
        return fileName
    }

    /**
     * Apaga [oldFileName] de [dir] se ele mudou (não é mais igual a [newFileName]) **e** nenhum
     * outro [SavedQuote] no histórico ainda referencia esse nome — do contrário apagaria o arquivo
     * de um orçamento duplicado que ainda depende dele.
     */
    private fun cleanupIfOrphaned(dir: File, oldFileName: String?, newFileName: String?, fileNameOf: (SavedQuote) -> String?) {
        if (oldFileName == null || oldFileName == newFileName) return
        if (state.value.none { fileNameOf(it) == oldFileName }) File(dir, oldFileName).delete()
    }

    actual fun convertToOrder(id: String) {
        if (state.value.none { it.id == id && !it.isOrder }) return
        state.value = state.value.map { if (it.id == id) it.copy(kind = QuoteKind.ORDER, status = OrderStatus.ORCADO) else it }
        persist()
    }

    actual fun updateStatus(id: String, status: OrderStatus) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status) else it }
        persist()
    }

    actual fun updatePrintSettings(id: String, printSettings: PrintSettings?) {
        state.value = state.value.map { if (it.id == id) it.copy(printSettings = printSettings) else it }
        persist()
    }

    actual fun updateDeliveryDate(id: String, deliveryDateEpochDay: Long?) {
        state.value = state.value.map { if (it.id == id) it.copy(deliveryDateEpochDay = deliveryDateEpochDay) else it }
        persist()
    }

    actual fun photoBytes(savedQuote: SavedQuote): ByteArray? {
        val fileName = savedQuote.photoFileName ?: return null
        val photoFile = File(photosDir, fileName)
        return if (photoFile.exists()) photoFile.readBytes() else null
    }

    actual fun stlBytes(savedQuote: SavedQuote): ByteArray? {
        val fileName = savedQuote.stlFileName ?: return null
        val stlFile = File(modelsDir, fileName)
        return if (stlFile.exists()) stlFile.readBytes() else null
    }

    private fun persist() = writeJsonFile(file, state.value)

    private fun defaultName(): String =
        SavedQuote.AUTO_NAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}
