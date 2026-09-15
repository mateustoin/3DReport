package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
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
    private val state = MutableStateFlow(readJsonFile(file, emptyList<SavedQuote>()))

    actual val savedQuotes: StateFlow<List<SavedQuote>> = state.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    actual fun save(name: String, quote: Quote, photo: PickedFile?, sourceLink: String?): SavedQuote {
        val id = Uuid.random().toString()
        val photoFileName = photo?.let { picked ->
            val extension = picked.fileName.substringAfterLast('.', "img")
            "$id.$extension".also { photosDir.mkdirs() }
        }
        photoFileName?.let { File(photosDir, it).writeBytes(photo.bytes) }

        val saved = SavedQuote(
            id = id,
            name = name.trim().ifEmpty { defaultName() },
            quote = quote,
            photoFileName = photoFileName,
            sourceLink = sourceLink?.trim()?.ifEmpty { null },
            savedAtEpochMillis = System.currentTimeMillis(),
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
    }

    actual fun photoBytes(savedQuote: SavedQuote): ByteArray? {
        val fileName = savedQuote.photoFileName ?: return null
        val photoFile = File(photosDir, fileName)
        return if (photoFile.exists()) photoFile.readBytes() else null
    }

    private fun persist() = writeJsonFile(file, state.value)

    private fun defaultName(): String =
        "Orçamento - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}
