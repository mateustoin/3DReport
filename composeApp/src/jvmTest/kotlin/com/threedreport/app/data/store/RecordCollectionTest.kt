package com.threedreport.app.data.store

import com.threedreport.app.data.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Os metadados que uma sincronização futura precisa (decisão 106): criação, alteração e exclusão lógica. */
@OptIn(ExperimentalCoroutinesApi::class)
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
    fun aBatchAppliesEverythingWithOneWriteAndOneEmission() = runTest(UnconfinedTestDispatcher()) {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        val items = collection(file)
        items.add(Item("a", "Primeiro"))
        items.add(Item("b", "Segundo"))
        items.add(Item("c", "Terceiro"))
        val writes = file.writeCount
        val emitted = mutableListOf<List<Item>>()
        backgroundScope.launch { items.items.collect { emitted += it } }

        now = 2_000L
        val changed = items.applyBatch(
            added = listOf(Item("d", "Quarto"), Item("e", "Quinto")),
            updated = listOf(Item("b", "Renomeado"), Item("c", "Terceiro")),
            deleted = listOf("a"),
        )

        assertTrue(changed)
        assertEquals(writes + 1, file.writeCount, "uma gravação só")
        assertEquals(2, emitted.size, "o valor de antes e um valor novo só")
        assertEquals(listOf("Renomeado", "Terceiro", "Quarto", "Quinto"), items.items.value.map { it.name })
        // Os carimbos de cada operação, como nas chamadas uma a uma.
        val records = file.content!!.associateBy { it.id }
        assertEquals(2_000L, records.getValue("a").deletedAtEpochMillis)
        assertEquals(2_000L, records.getValue("a").updatedAtEpochMillis)
        assertEquals(1_000L, records.getValue("b").createdAtEpochMillis)
        assertEquals(2_000L, records.getValue("b").updatedAtEpochMillis)
        assertEquals(1_000L, records.getValue("c").updatedAtEpochMillis, "o que não mudou não ganha data de alteração")
        assertEquals(2_000L, records.getValue("d").createdAtEpochMillis)
        assertEquals(2_000L, records.getValue("d").updatedAtEpochMillis)
        assertNull(records.getValue("d").deletedAtEpochMillis)
        assertEquals("Primeiro", items.restore("a")?.name, "excluir em lote também vai pra lixeira")
    }

    @Test
    fun aBatchThatChangesNothingDoesNotWrite() {
        val file = MemoryDataFile<List<StoredRecord<Item>>>("itens.json")
        val items = collection(file)
        items.add(Item("a", "Primeiro"))
        items.add(Item("b", "Segundo"))
        items.delete("b")
        val writes = file.writeCount

        // Alterar igual, alterar e excluir quem não existe ou já está na lixeira: nada a fazer.
        val changed = items.applyBatch(updated = listOf(Item("a", "Primeiro"), Item("x", "Novo")), deleted = listOf("b", "y"))

        assertFalse(changed)
        assertEquals(writes, file.writeCount)
    }

    @Test
    fun aBatchCannotAddAnIdThatExists() {
        val items = collection()
        items.add(Item("a", "Primeiro"))
        items.delete("a")

        assertFailsWith<IllegalArgumentException> { items.applyBatch(added = listOf(Item("a", "De novo"))) }
        assertFailsWith<IllegalArgumentException> { items.applyBatch(added = listOf(Item("z", "Um"), Item("z", "Dois"))) }
        assertTrue(items.items.value.isEmpty(), "o lote que falha não aplica nada")
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
