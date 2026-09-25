package com.threedreport.app.data

import com.threedreport.app.data.store.StorageHealth
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A gravação nunca deixa um arquivo pela metade, e um arquivo que não abre é guardado, não perdido. */
class JsonDataFileTest {

    private lateinit var dir: File
    private val serializer = ListSerializer(String.serializer())

    @BeforeTest
    fun createDir() {
        dir = createTempDirectory("3dreport-json").toFile()
    }

    private fun dataFile(health: StorageHealth? = null) = JsonDataFile(File(dir, "itens.json"), serializer, health)

    @Test
    fun writingKeepsThePreviousVersionAsABackupAndLeavesNoTemporaryFile() {
        val file = dataFile()
        file.write(listOf("primeiro"))
        file.write(listOf("segundo"))

        assertEquals(listOf("segundo"), file.read())
        assertEquals(listOf("primeiro"), JsonDataFile(File(dir, "itens.json.bak"), serializer).read())
        assertFalse(File(dir, "itens.json.tmp").exists())
    }

    @Test
    fun aTruncatedFileIsKeptAsideAndTheBackupIsUsed() {
        val health = StorageHealth()
        val file = dataFile(health)
        file.write(listOf("bom"))
        file.write(listOf("bom", "também"))
        // Queda de energia no meio de uma gravação feita por fora (sem o .tmp): o arquivo fica cortado.
        File(dir, "itens.json").writeText("""["bom", "tam""")

        assertEquals(listOf("bom"), file.read())

        val reported = health.unreadableFiles.value.single()
        assertEquals("itens.json", reported.name)
        assertTrue(reported.recoveredFromBackup)
        assertTrue(File(reported.keptAs).readText().startsWith("""["bom""""), "o arquivo cortado fica guardado, sem apagar")
    }

    @Test
    fun anUnreadableFileWithoutBackupStartsEmptyAndSaysSo() {
        val health = StorageHealth()
        File(dir, "itens.json").writeText("isso não é json")

        assertNull(dataFile(health).read())
        assertFalse(health.unreadableFiles.value.single().recoveredFromBackup)
    }

    @Test
    fun whenOnlyTheTemporaryFileSurvivedItIsTheNewestState() {
        // A gravação caiu entre as duas trocas de nome: o arquivo sumiu, o .tmp tem o estado novo.
        File(dir, "itens.json.tmp").writeText("""["novo"]""")
        File(dir, "itens.json.bak").writeText("""["antigo"]""")

        assertEquals(listOf("novo"), dataFile().read())
    }

    @Test
    fun aCutTemporaryFileFallsBackToTheBackup() {
        File(dir, "itens.json.tmp").writeText("""["no""")
        File(dir, "itens.json.bak").writeText("""["antigo"]""")

        assertEquals(listOf("antigo"), dataFile().read())
    }
}
