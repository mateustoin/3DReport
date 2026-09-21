package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
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
        services: List<Service>,
        photo: PickedFile?,
        stlFile: PickedFile?,
        sourceLink: String?,
        client: Client?,
    ): SavedQuote {
        val id = Uuid.random().toString()
        val photoFileName = photo?.let { picked ->
            val extension = picked.fileName.substringAfterLast('.', "img")
            "$id.$extension".also { photosDir.mkdirs() }
        }
        photoFileName?.let { File(photosDir, it).writeBytes(photo.bytes) }

        val stlFileName = stlFile?.let { picked ->
            val extension = picked.fileName.substringAfterLast('.', "stl")
            "$id.$extension".also { modelsDir.mkdirs() }
        }
        stlFileName?.let { File(modelsDir, it).writeBytes(stlFile.bytes) }

        val saved = SavedQuote(
            id = id,
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            services = services,
            photoFileName = photoFileName,
            stlFileName = stlFileName,
            sourceLink = sourceLink?.trim()?.ifEmpty { null },
            savedAtEpochMillis = System.currentTimeMillis(),
            client = client,
        )
        state.value = state.value + saved
        persist()
        return saved
    }

    actual fun delete(id: String) {
        val removed = state.value.find { it.id == id }
        state.value = state.value.filterNot { it.id == id }
        persist()
        removed?.photoFileName?.let { File(photosDir, it).delete() }
        removed?.stlFileName?.let { File(modelsDir, it).delete() }
    }

    actual fun updateStatus(id: String, status: OrderStatus) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status) else it }
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
        "Orçamento - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}
