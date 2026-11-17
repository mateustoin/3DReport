package com.threedreport.app.platform

import com.threedreport.app.AppLog
import com.threedreport.core.slicer.THREE_MF_ROOT_RELATIONSHIPS
import com.threedreport.core.slicer.findThreeMfThumbnailPath
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Maior arquivo de relações e de miniatura lido de dentro do pacote: protege contra ZIP que descompacta demais. */
private const val MAX_ENTRY_BYTES = 32 * 1024 * 1024

actual fun readThreeMfThumbnail(bytes: ByteArray): ThreeMfThumbnail = try {
    val names = ArrayList<String>()
    var relationships: String? = null
    forEachZipEntry(bytes) { name, read ->
        names += name
        if (name.removePrefix("/").equals(THREE_MF_ROOT_RELATIONSHIPS, ignoreCase = true)) relationships = read()?.decodeToString()
    }
    if (names.isEmpty()) {
        ThreeMfThumbnail.Unreadable
    } else {
        val path = findThreeMfThumbnailPath(relationships, names)
        var image: ByteArray? = null
        if (path != null) forEachZipEntry(bytes) { name, read -> if (name == path && image == null) image = read() }
        val found = image
        if (path != null && found != null && decodeToBufferedImage(found) != null) ThreeMfThumbnail.Found(found, path) else ThreeMfThumbnail.Missing
    }
} catch (e: Exception) {
    // ZipException, EOFException de pacote cortado, IllegalArgumentException de nome de entrada que não é UTF-8.
    AppLog.warn("Não consegui ler o 3MF como pacote", e)
    ThreeMfThumbnail.Unreadable
}

/**
 * Percorre as entradas do ZIP em [bytes], na ordem do arquivo. `read()` devolve o conteúdo da entrada atual, ou
 * `null` se passar de [MAX_ENTRY_BYTES]. Um arquivo que não é ZIP simplesmente não tem entrada.
 */
private fun forEachZipEntry(bytes: ByteArray, action: (name: String, read: () -> ByteArray?) -> Unit) {
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            action(entry.name) { readLimited(zip) }
        }
    }
}

private fun readLimited(zip: ZipInputStream): ByteArray? {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = zip.read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
        if (out.size() > MAX_ENTRY_BYTES) return null
    }
    return out.toByteArray()
}
