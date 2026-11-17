package com.threedreport.app.platform

import com.threedreport.app.AppLog
import com.threedreport.core.slicer.THREE_MF_ROOT_RELATIONSHIPS
import com.threedreport.core.slicer.findThreeMfThumbnailPath
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

/** Maior arquivo de relações ou imagem lido de dentro do pacote: protege contra ZIP que descompacta demais. */
private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024

/** Soma das imagens guardadas durante a leitura, enquanto não se sabe qual é a miniatura. */
private const val MAX_KEPT_IMAGE_BYTES = 64 * 1024 * 1024

/**
 * Maior miniatura aceita, em pixels (4096 x 4096). Os fatiadores gravam umas centenas de pixels de lado; um PNG
 * declarando muito mais é arquivo estragado ou malicioso, e decodificar pediria GB de memória.
 */
internal const val MAX_THUMBNAIL_PIXELS = 4096L * 4096L

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg")

/**
 * Uma passada só pelo ZIP: o `_rels/.rels` e as imagens (pequenas) ficam guardados, porque a relação que diz qual é a
 * miniatura pode vir depois dela; o resto (a malha em XML, que é o grosso do arquivo) é pulado.
 */
actual fun readThreeMfThumbnail(bytes: ByteArray): ThreeMfThumbnail = try {
    val names = ArrayList<String>()
    val images = HashMap<String, ByteArray>()
    var keptBytes = 0L
    var relationships: String? = null
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            val name = entry.name
            names += name
            val extension = name.substringAfterLast('.', "").lowercase()
            when {
                name.removePrefix("/").equals(THREE_MF_ROOT_RELATIONSHIPS, ignoreCase = true) -> relationships = readLimited(zip)?.decodeToString()

                extension in IMAGE_EXTENSIONS && keptBytes < MAX_KEPT_IMAGE_BYTES -> readLimited(zip)?.let {
                    images[name] = it
                    keptBytes += it.size
                }
            }
        }
    }
    if (names.isEmpty()) {
        ThreeMfThumbnail.Unreadable
    } else {
        val path = findThreeMfThumbnailPath(relationships, names)
        val image = path?.let(images::get)
        if (path != null && image != null && isDecodableThumbnail(image)) ThreeMfThumbnail.Found(image, path) else ThreeMfThumbnail.Missing
    }
} catch (e: Exception) {
    // ZipException, EOFException de pacote cortado, IllegalArgumentException de nome de entrada que não é UTF-8.
    AppLog.warn("Não consegui ler o 3MF como pacote", e)
    ThreeMfThumbnail.Unreadable
} catch (e: OutOfMemoryError) {
    // Não deveria chegar aqui (as entradas e as dimensões são limitadas antes), mas um 3MF nunca derruba o app.
    AppLog.warn("Sem memória pra ler o 3MF", e)
    ThreeMfThumbnail.Unreadable
}

/** Se [image] tem tamanho razoável (lido do cabeçalho, sem decodificar) e abre como imagem. */
private fun isDecodableThumbnail(image: ByteArray): Boolean {
    val (width, height) = imageSizeFromHeader(image) ?: return false
    if (width <= 0 || height <= 0 || width.toLong() * height > MAX_THUMBNAIL_PIXELS) {
        AppLog.warn("Miniatura do 3MF com tamanho fora do normal (${width}x$height); ignorada")
        return false
    }
    return decodeToBufferedImage(image) != null
}

/** Largura e altura de uma imagem PNG ou JPEG lidas só do cabeçalho, sem decodificar os pixels; `null` se não der. */
internal fun imageSizeFromHeader(bytes: ByteArray): Pair<Int, Int>? = runCatching {
    ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return@use null
        try {
            reader.input = input
            reader.getWidth(0) to reader.getHeight(0)
        } finally {
            reader.dispose()
        }
    }
}.getOrNull()

/** O conteúdo da entrada atual, ou `null` se passar de [MAX_ENTRY_BYTES]. */
private fun readLimited(zip: ZipInputStream): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = zip.read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
        if (out.size() > MAX_ENTRY_BYTES) return null
    }
    return out.toByteArray()
}
