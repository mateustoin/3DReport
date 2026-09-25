package com.threedreport.app.data

/**
 * Onde moram os arquivos anexados (foto do orçamento, STL, miniatura do G-code, logo), fora do JSON
 * (decisão 108).
 *
 * Cada arquivo é guardado pelo **conteúdo**: a chave é o hash dos bytes mais a extensão. Por isso um
 * arquivo nunca é sobrescrito (bytes diferentes, chave diferente), duplicar ou vender um orçamento
 * reaproveita o mesmo arquivo sem copiar, e a chave serve igual num armazenamento na nuvem. Quem guarda
 * a chave (o orçamento) não precisa saber onde o arquivo está.
 *
 * Nada é apagado na hora: arquivos sem ninguém apontando pra eles saem na [collectGarbage], que o app
 * roda ao abrir.
 */
interface AttachmentStore {

    /** Guarda [bytes] e devolve a chave. Guardar os mesmos bytes de novo devolve a mesma chave, sem regravar. */
    fun put(bytes: ByteArray, fileName: String): String

    /** Os bytes de [key], ou `null` se o arquivo não existe (ou não deu pra ler). */
    fun read(key: String): ByteArray?

    fun exists(key: String): Boolean

    /**
     * Miniatura de uma imagem guardada, com no máximo [maxSizePx] no lado maior, gerada uma vez e
     * reaproveitada. `null` se a chave não existe ou não é uma imagem que dê pra ler.
     */
    fun thumbnail(key: String, maxSizePx: Int): ByteArray?

    /** Apaga tudo que não está em [referencedKeys]. Devolve quantos arquivos saíram. */
    fun collectGarbage(referencedKeys: Set<String>): Int
}

/** [AttachmentStore] em memória, pra testes. A chave é um contador, não um hash. */
class MemoryAttachmentStore : AttachmentStore {
    private val files = LinkedHashMap<String, ByteArray>()

    val keys: Set<String>
        get() = files.keys

    override fun put(bytes: ByteArray, fileName: String): String {
        files.entries.firstOrNull { it.value.contentEquals(bytes) }?.let { return it.key }
        val extension = attachmentExtension(fileName)
        val key = "mem-${files.size + 1}.$extension"
        files[key] = bytes
        return key
    }

    override fun read(key: String): ByteArray? = files[key]

    override fun exists(key: String): Boolean = key in files

    override fun thumbnail(key: String, maxSizePx: Int): ByteArray? = files[key]

    override fun collectGarbage(referencedKeys: Set<String>): Int {
        val orphans = files.keys - referencedKeys
        orphans.forEach(files::remove)
        return orphans.size
    }
}

/** Extensão de [fileName] em minúsculas, só letras e números, ou "bin". */
fun attachmentExtension(fileName: String): String =
    fileName.substringAfterLast('.', "").lowercase().takeIf { it.isNotEmpty() && it.length <= 8 && it.all(Char::isLetterOrDigit) } ?: "bin"
