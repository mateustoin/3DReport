package com.threedreport.app.data.store

import com.threedreport.app.data.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Um registro gravado em lista (orçamento, filamento, impressora…) com os metadados que uma
 * sincronização futura precisa (decisão 106): quando nasceu, quando mudou pela última vez e, se foi
 * excluído, quando. Os modelos do `core` continuam sem saber disso: é o armazenamento que carimba.
 *
 * @property data o registro, ou `null` depois que um excluído passa da lixeira ([RecordCollection.TRASH_MILLIS]):
 *   aí só fica a marca de que o [id] foi excluído, que é o que outro dispositivo precisaria saber.
 * @property deletedAtEpochMillis exclusão lógica. Enquanto está na lixeira, dá pra desfazer.
 */
@Serializable
data class StoredRecord<T>(
    val id: String,
    val data: T? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
) {
    val isDeleted: Boolean
        get() = deletedAtEpochMillis != null
}

/** Um documento único (configurações, marca) com o momento da última mudança. */
@Serializable
data class StoredDocument<T>(
    val data: T,
    val updatedAtEpochMillis: Long,
)

/**
 * Uma lista de registros num arquivo só, com o estado em memória que as telas observam.
 *
 * Excluir não apaga: marca o registro (tombstone) e o tira de [items]. Enquanto está na lixeira dá pra
 * [restore]; depois de [TRASH_MILLIS] o conteúdo é descartado ao abrir o app e fica só a marca.
 * Mudança que não muda nada não grava nem mexe na data de alteração.
 *
 * Mudanças acontecem no thread da tela; a gravação pode ir pra segundo plano ([WriteBehindFile]).
 *
 * @param seed registros de quem abre o app pela primeira vez. Gravados na hora, pra os ids (aleatórios)
 *   continuarem os mesmos na próxima abertura.
 */
class RecordCollection<T>(
    private val file: DataFile<List<StoredRecord<T>>>,
    private val idOf: (T) -> String,
    private val clock: Clock,
    seed: () -> List<T> = { emptyList() },
) {
    private val records: MutableStateFlow<List<StoredRecord<T>>>
    private val live: MutableStateFlow<List<T>>

    /** Os registros vivos (não excluídos), na ordem em que foram criados. */
    val items: StateFlow<List<T>>

    init {
        val now = clock.nowMillis()
        val loaded = file.read()
        val initial = loaded?.map { it.withTrashEmptied(now) } ?: seed().map { StoredRecord(idOf(it), it, now, now) }
        records = MutableStateFlow(initial)
        live = MutableStateFlow(liveOf(initial))
        items = live.asStateFlow()
        // Grava o seed na primeira abertura, e grava de novo quando a lixeira descartou algum conteúdo
        // vencido: sem isso, o que foi apagado continuava no arquivo e nos backups.
        if ((loaded == null && initial.isNotEmpty()) || (loaded != null && initial != loaded)) file.write(initial)
    }

    /** Todos os registros, inclusive excluídos (pra quem precisa saber o que ainda é referenciado). */
    val allRecords: List<StoredRecord<T>>
        get() = records.value

    /** O registro vivo [id], ou `null`. */
    fun find(id: String): T? = records.value.firstOrNull { it.id == id && !it.isDeleted }?.data

    /** O registro [id] com os metadados, inclusive excluído, ou `null`. */
    fun record(id: String): StoredRecord<T>? = records.value.firstOrNull { it.id == id }

    fun add(item: T) {
        val id = idOf(item)
        require(records.value.none { it.id == id }) { "já existe um registro com o id $id" }
        val now = clock.nowMillis()
        commit(records.value + StoredRecord(id, item, now, now))
    }

    /** Troca o registro de mesmo id por [item]. `false` se não existe (ou foi excluído). */
    fun update(item: T): Boolean = update(idOf(item)) { item } != null

    /**
     * Aplica [transform] ao registro vivo [id] e grava. Devolve o registro novo, ou `null` se [id] não
     * existe (ou foi excluído). Se nada mudou, não grava.
     */
    fun update(id: String, transform: (T) -> T): T? {
        val existing = records.value.firstOrNull { it.id == id && !it.isDeleted } ?: return null
        val data = existing.data ?: return null
        val updated = transform(data)
        require(idOf(updated) == id) { "a alteração não pode trocar o id do registro" }
        if (updated == data) return data
        val now = clock.nowMillis()
        commit(records.value.map { if (it.id == id) it.copy(data = updated, updatedAtEpochMillis = now) else it })
        return updated
    }

    /** Manda o registro [id] pra lixeira. `false` se não existe ou já foi excluído. */
    fun delete(id: String): Boolean {
        if (records.value.none { it.id == id && !it.isDeleted }) return false
        val now = clock.nowMillis()
        commit(records.value.map { if (it.id == id) it.copy(deletedAtEpochMillis = now, updatedAtEpochMillis = now) else it })
        return true
    }

    /** Tira o registro [id] da lixeira. Devolve o registro, ou `null` se não há o que desfazer. */
    fun restore(id: String): T? {
        val existing = records.value.firstOrNull { it.id == id && it.isDeleted } ?: return null
        val data = existing.data ?: return null
        val now = clock.nowMillis()
        commit(records.value.map { if (it.id == id) it.copy(deletedAtEpochMillis = null, updatedAtEpochMillis = now) else it })
        return data
    }

    private fun commit(newRecords: List<StoredRecord<T>>) {
        records.value = newRecords
        live.value = liveOf(newRecords)
        file.write(newRecords)
    }

    private fun liveOf(all: List<StoredRecord<T>>): List<T> = all.mapNotNull { if (it.isDeleted) null else it.data }

    private fun StoredRecord<T>.withTrashEmptied(now: Long): StoredRecord<T> {
        val deletedAt = deletedAtEpochMillis ?: return this
        return if (data != null && now - deletedAt > TRASH_MILLIS) copy(data = null) else this
    }

    companion object {
        /** Quanto tempo um excluído fica na lixeira, dando pra desfazer: 30 dias. */
        const val TRASH_MILLIS: Long = 30L * 24 * 60 * 60 * 1000
    }
}

/**
 * Um documento único (configurações, marca, tema) com o estado em memória que as telas observam.
 * Gravar o mesmo valor não grava de novo.
 */
class DocumentValue<T>(
    private val file: DataFile<StoredDocument<T>>,
    private val clock: Clock,
    default: () -> T,
) {
    private val state = MutableStateFlow(file.read()?.data ?: default())

    val value: StateFlow<T> = state.asStateFlow()

    fun set(value: T) {
        if (value == state.value) return
        state.value = value
        file.write(StoredDocument(value, clock.nowMillis()))
    }

    fun update(transform: (T) -> T) = set(transform(state.value))
}
