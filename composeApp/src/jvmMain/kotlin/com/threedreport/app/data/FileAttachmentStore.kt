package com.threedreport.app.data

import com.threedreport.app.AppLog
import com.threedreport.app.platform.decodeToBufferedImage
import com.threedreport.app.platform.encodePng
import com.threedreport.app.platform.scaledToFit
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * [AttachmentStore] numa pasta (`~/.3dreport/attachments`). A chave é o SHA-256 dos bytes mais a
 * extensão: o mesmo arquivo nunca é gravado duas vezes, e um arquivo gravado nunca muda. Miniaturas
 * ficam em `thumbs/`, geradas uma vez.
 */
class FileAttachmentStore(private val dir: File) : AttachmentStore {

    private val thumbsDir = File(dir, "thumbs")

    override fun put(bytes: ByteArray, fileName: String): String {
        val key = sha256(bytes) + "." + attachmentExtension(fileName)
        val target = File(dir, key)
        if (!target.isFile || target.length() != bytes.size.toLong()) {
            dir.mkdirs()
            val tmp = File(dir, "$key.tmp")
            FileOutputStream(tmp).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }
        return key
    }

    override fun read(key: String): ByteArray? {
        val file = fileOf(key) ?: return null
        return if (file.isFile) retrying("ler o anexo $key") { file.readBytes() } else null
    }

    override fun exists(key: String): Boolean = fileOf(key)?.isFile == true

    override fun thumbnail(key: String, maxSizePx: Int): ByteArray? {
        if (fileOf(key) == null) return null
        val cached = File(thumbsDir, "$maxSizePx/$key.png")
        if (cached.isFile) return runCatching { cached.readBytes() }.getOrNull()
        val original = read(key) ?: return null
        val image = decodeToBufferedImage(original) ?: return null
        val png = image.scaledToFit(maxSizePx).encodePng()
        runCatching {
            cached.parentFile.mkdirs()
            cached.writeBytes(png)
        }.onFailure { AppLog.warn("Não consegui guardar a miniatura de $key", it) }
        return png
    }

    override fun collectGarbage(referencedKeys: Set<String>): Int {
        val files = dir.listFiles()?.filter { it.isFile } ?: return 0
        val orphans = files.filter { it.name !in referencedKeys }
        orphans.forEach { it.delete() }
        thumbsDir.listFiles()?.filter { it.isDirectory }?.forEach { sizeDir ->
            sizeDir.listFiles()?.filter { it.name.removeSuffix(".png") !in referencedKeys }?.forEach { it.delete() }
        }
        if (orphans.isNotEmpty()) AppLog.info("Limpeza de anexos: ${orphans.size} arquivo(s) sem uso removido(s)")
        return orphans.size
    }

    /** O arquivo da chave, ou `null` se a chave não tem o formato esperado (evita sair da pasta com "../"). */
    private fun fileOf(key: String): File? = if (KEY_PATTERN.matches(key)) File(dir, key) else null

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val KEY_PATTERN = Regex("^[0-9a-f]{64}\\.[a-z0-9]{1,8}$")
    }
}
