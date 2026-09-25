package com.threedreport.app.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Formato da pasta de dados (decisão 104): pasta de versão anterior é guardada à parte, nunca apagada. */
class DataFormatTest {

    private lateinit var parent: File
    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        // A pasta antiga vai pra uma irmã da pasta de dados, então as duas moram dentro da pasta do teste.
        parent = createTempDirectory("3dreport-format-test").toFile()
        dataDir = File(parent, ".3dreport")
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
        parent.deleteRecursively()
    }

    @Test
    fun aNewInstallOnlyGetsTheFormatFile() {
        assertNull(prepareDataDir())

        assertEquals(DATA_FORMAT_VERSION, readDataFormatVersion(File(dataDir, "format.json")))
        assertEquals(listOf("format.json"), dataDir.list()!!.toList())
    }

    @Test
    fun dataFromVersionOneIsMovedAsideWithEverythingInIt() {
        dataDir.mkdirs()
        File(dataDir, "quotes.json").writeText("""[{"id":"1","quote":{"job":{}}}]""")
        File(dataDir, "photos").mkdirs()
        File(dataDir, "photos/foto.png").writeBytes(byteArrayOf(1, 2, 3))

        val moved = prepareDataDir()

        assertEquals(File(parent, ".3dreport-v1"), moved)
        assertEquals("""[{"id":"1","quote":{"job":{}}}]""", File(moved, "quotes.json").readText())
        assertTrue(File(moved, "photos/foto.png").isFile)
        assertEquals(listOf("format.json"), dataDir.list()!!.toList())
    }

    @Test
    fun aSecondOldFolderDoesNotOverwriteTheFirst() {
        File(parent, ".3dreport-v1").mkdirs()
        dataDir.mkdirs()
        File(dataDir, "settings.json").writeText("{}")

        assertEquals(File(parent, ".3dreport-v1-2"), prepareDataDir())
    }

    @Test
    fun currentDataIsLeftAlone() {
        prepareDataDir()
        File(dataDir, "quotes.json").writeText("[]")

        assertNull(prepareDataDir())
        assertTrue(File(dataDir, "quotes.json").isFile)
    }

    @Test
    fun aFileThatCannotBeReadIsKeptInsteadOfOverwritten() {
        dataDir.mkdirs()
        File(dataDir, "channels.json").writeText("""[{"formato": "que não existe"}]""")

        val repository = SalesChannelRepository()

        assertTrue(repository.channels.value.isEmpty())
        val kept = dataDir.listFiles()!!.single { it.name.startsWith("channels.ilegivel-") }
        assertEquals("""[{"formato": "que não existe"}]""", kept.readText())
    }

    @Test
    fun aBackupFromTheOldFormatIsRefusedAndNothingChanges() {
        prepareDataDir()
        File(dataDir, "quotes.json").writeText("[]")
        val oldBackup = zipOf("3dreport-backup.json" to """{"app": "3DReport", "appVersion": "1.44.0"}""", "quotes.json" to "[]")

        val result = BackupRepository().restoreFromZip(oldBackup)

        assertIs<RestoreResult.Failure>(result)
        assertTrue(result.message.contains("formato de dados anterior"))
        assertTrue(File(dataDir, "quotes.json").isFile)
    }

    @Test
    fun aBackupMadeByThisVersionCarriesTheFormatAndRestores() {
        prepareDataDir()
        File(dataDir, "quotes.json").writeText("[]")
        val backup = BackupRepository().createBackupZip()

        val result = BackupRepository().restoreFromZip(backup)

        assertIs<RestoreResult.Success>(result)
        assertEquals(DATA_FORMAT_VERSION, readDataFormatVersion(File(dataDir, "format.json")))
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.encodeToByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
