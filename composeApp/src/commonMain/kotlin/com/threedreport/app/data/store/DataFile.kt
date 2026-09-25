package com.threedreport.app.data.store

/**
 * Um arquivo de dados do app: lido uma vez, ao abrir, e regravado inteiro a cada mudança.
 *
 * É o único ponto em que os repositórios tocam o disco (decisão 108). A implementação de cada
 * plataforma cuida do resto: gravar de forma atômica, guardar a cópia anterior e separar um arquivo
 * que não dá pra ler. Trocar o armazenamento (SQLite, nuvem) é trocar a implementação, não os
 * repositórios.
 */
interface DataFile<T> {

    /** Nome curto do arquivo, pra mensagens ("quotes.json"). */
    val name: String

    /** O conteúdo gravado, ou `null` se o arquivo ainda não existe (ou não deu pra aproveitar nada). */
    fun read(): T?

    /** Grava [value] no lugar do conteúdo anterior. */
    fun write(value: T)
}

/** [DataFile] só em memória: pra testes e pra renderizar telas sem tocar o disco. */
class MemoryDataFile<T>(override val name: String, initial: T? = null) : DataFile<T> {
    var content: T? = initial
        private set

    /** Quantas vezes foi gravado, pra testes conferirem que uma ação gravou (ou não). */
    var writeCount: Int = 0
        private set

    override fun read(): T? = content

    override fun write(value: T) {
        content = value
        writeCount++
    }
}
