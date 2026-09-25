package com.threedreport.app.data.store

import com.threedreport.app.data.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Os metadados que uma sincronização futura precisa (decisão 106): criação, alteração e exclusão lógica. */
class RecordCollectionTest {

    private data class Item(val id: String, val name: String)

    private var now = 1_000L
    private val clock = Clock { now }

    private fun collection(file: MemoryDataFile<List<StoredRecord<Item>>> = MemoryDataFile("itens.json"), seed: List<Item> = emptyList()) =
        RecordCollection(file, Item::id, clock) { seed }

    @Test
    fun addStampsCreationAndChangeTimes() {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        collection(file).add(Item("a", "Primeiro"))

        val record = file.content!!.single()
        assertEquals(1_000L, record.createdAtEpochMillis)
        assertEquals(1_000L, record.updatedAtEpochMillis)
        assertNull(record.deletedAtEpochMillis)
    }

    @Test
    fun updateMovesTheChangeTimeOnlyWhenSomethingChanged() {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        val items = collection(file)
        items.add(Item("a", "Primeiro"))
        val writes = file.writeCount

        now = 2_000L
        items.update(Item("a", "Primeiro"))
        assertEquals(writes, file.writeCount, "a mesma coisa não grava de novo")

        items.update("a") { it.copy(name = "Renomeado") }
        val record = file.content!!.single()
        assertEquals(1_000L, record.createdAtEpochMillis)
        assertEquals(2_000L, record.updatedAtEpochMillis)
        assertEquals("Renomeado", items.find("a")?.name)
    }

    @Test
    fun deleteIsATombstoneThatRestoreUndoes() {
        val items = collection()
        items.add(Item("a", "Primeiro"))

        now = 3_000L
        assertTrue(items.delete("a"))
        assertTrue(items.items.value.isEmpty())
        assertEquals(3_000L, items.record("a")?.deletedAtEpochMillis)
        assertFalse(items.delete("a"), "excluir de novo não faz nada")

        assertEquals("Primeiro", items.restore("a")?.name)
        assertEquals(listOf("Primeiro"), items.items.value.map { it.name })
    }

    @Test
    fun afterThirtyDaysTheTrashLosesTheContentButKeepsTheMark() {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        collection(file).apply {
            add(Item("a", "Primeiro"))
            delete("a")
        }

        now += RecordCollection.TRASH_MILLIS + 1
        val reopened = collection(file)

        val record = reopened.record("a")!!
        assertNull(record.data)
        assertTrue(record.isDeleted, "a marca de exclusão fica, pra outro dispositivo saber")
        assertNull(reopened.restore("a"))

        // O arquivo também perde o conteúdo: o que foi apagado não pode seguir no disco nem nos backups.
        val onDisk = file.content!!.single()
        assertNull(onDisk.data)
        assertTrue(onDisk.isDeleted)
    }

    @Test
    fun theSeedIsWrittenOnTheFirstOpenSoItsIdsStayTheSame() {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        collection(file, seed = listOf(Item("aleatorio-1", "Exemplo")))

        val reopened = collection(file, seed = listOf(Item("aleatorio-2", "Exemplo")))

        assertEquals(listOf("aleatorio-1"), reopened.items.value.map { it.id })
    }

    @Test
    fun documentWritesOnlyWhenTheValueChanges() {
        val file = MemoryDataFile<StoredDocument<Int>>("numero.json")
        val document = DocumentValue(file, clock) { 0 }

        document.set(0)
        assertEquals(0, file.writeCount)

        now = 5_000L
        document.update { it + 1 }
        assertEquals(1, file.content?.data)
        assertEquals(5_000L, file.content?.updatedAtEpochMillis)
    }
}
