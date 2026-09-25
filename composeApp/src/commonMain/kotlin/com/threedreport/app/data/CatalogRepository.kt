package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.data.store.RecordCollection
import kotlinx.coroutines.flow.StateFlow

/**
 * Um cadastro em lista (filamentos, impressoras, serviços, canais, templates, clientes). Interface, e
 * não classe de plataforma (decisão 108): as telas dependem só disto, e trocar o armazenamento (SQLite,
 * nuvem) ou usar uma versão em memória nos testes é trocar a implementação.
 */
interface CatalogRepository<T> {

    /** Os itens vivos, na ordem de cadastro. */
    val items: StateFlow<List<T>>

    fun add(item: T)

    /** Troca o item de mesmo id. Não faz nada se ele não existe. */
    fun update(item: T)

    /** Manda o item pra lixeira (dá pra desfazer com [restore] por um tempo). */
    fun delete(id: String)

    /** Desfaz uma exclusão ainda na lixeira. */
    fun restore(id: String)
}

/** [CatalogRepository] guardado numa [RecordCollection]. */
open class RecordCatalogRepository<T>(protected val collection: RecordCollection<T>) : CatalogRepository<T> {
    override val items: StateFlow<List<T>> = collection.items

    override fun add(item: T) = collection.add(item)

    override fun update(item: T) {
        collection.update(item)
    }

    override fun delete(id: String) {
        collection.delete(id)
    }

    override fun restore(id: String) {
        collection.restore(id)
    }
}

/** Um documento único (configurações, tema, moeda). Mesmo motivo de [CatalogRepository] pra ser interface. */
interface DocumentRepository<T> {
    val value: StateFlow<T>

    fun update(value: T)
}

/** [DocumentRepository] guardado num [DocumentValue]. */
open class StoredDocumentRepository<T>(protected val document: DocumentValue<T>) : DocumentRepository<T> {
    override val value: StateFlow<T> = document.value

    override fun update(value: T) = document.set(value)
}
