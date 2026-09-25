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
        assertEquals(DataDirResult.Ready(), prepareDataDir())

        assertEquals(DATA_FORMAT_VERSION, readDataFormatVersion(File(dataDir, "format.json")))
        assertEquals(listOf("format.json"), dataDir.list()!!.toList())
    }

    @Test
    fun dataFromVersionOneIsMovedAsideWithEverythingInIt() {
        dataDir.mkdirs()
        File(dataDir, "quotes.json").writeText("""[{"id":"1","quote":{"job":{}}}]""")
        File(dataDir, "photos").mkdirs()
        File(dataDir, "photos/foto.png").writeBytes(byteArrayOf(1, 2, 3))

        val moved = assertIs<DataDirResult.Ready>(prepareDataDir()).moved!!.path

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

        assertEquals(DataDirResult.Ready(MovedData(File(parent, ".3dreport-v1-2"), fromNewerVersion = false)), prepareDataDir())
    }

    @Test
    fun currentDataIsLeftAlone() {
        prepareDataDir()
        File(dataDir, "quotes.json").writeText("[]")

        assertEquals(DataDirResult.Ready(), prepareDataDir())
        assertTrue(File(dataDir, "quotes.json").isFile)
    }

    /** Só esta versão grava o `format.json`; um que não abre (gravação interrompida) não é motivo pra mover dados. */
    @Test
    fun aDamagedFormatFileCountsAsTheCurrentVersion() {
        prepareDataDir()
        File(dataDir, "quotes.json").writeText("[]")
        File(dataDir, "format.json").writeText("{\"versi")

        assertEquals(DataDirResult.Ready(), prepareDataDir())
        assertTrue(File(dataDir, "quotes.json").isFile)
        assertEquals(DATA_FORMAT_VERSION, readDataFormatVersion(File(dataDir, "format.json")))
    }

    @Test
    fun dataFromANewerVersionIsAlsoMovedAsideAndSaysSo() {
        dataDir.mkdirs()
        File(dataDir, "format.json").writeText("""{"version": ${DATA_FORMAT_VERSION + 1}}""")
        File(dataDir, "quotes.json").writeText("[]")

        val moved = assertIs<DataDirResult.Ready>(prepareDataDir()).moved!!

        assertEquals(File(parent, ".3dreport-v${DATA_FORMAT_VERSION + 1}"), moved.path)
        assertTrue(moved.fromNewerVersion)
        assertTrue(File(moved.path, "quotes.json").isFile)
    }

    /** No Windows, renomear falha com a pasta aberta em outro programa: aí copia e só então esvazia. */
    @Test
    fun whenRenamingFailsTheFolderIsCopiedAndThenEmptied() {
        dataDir.mkdirs()
        File(dataDir, "photos").mkdirs()
        File(dataDir, "photos/foto.png").writeBytes(byteArrayOf(1, 2, 3))
        File(dataDir, "quotes.json").writeText("[1]")
        val target = File(parent, ".3dreport-v1")

        assertTrue(moveDataDir(dataDir, target, rename = { _, _ -> false }))

        assertEquals("[1]", File(target, "quotes.json").readText())
        assertTrue(File(target, "photos/foto.png").isFile)
        assertTrue(dataDir.list()!!.isEmpty())
    }

    @Test
    fun whenNothingCanBeMovedTheOriginalIsUntouched() {
        dataDir.mkdirs()
        File(dataDir, "quotes.json").writeText("[1]")
        // Um arquivo comum no lugar do destino faz a cópia falhar.
        val target = File(parent, "ocupado").apply { writeText("x") }

        assertEquals(false, moveDataDir(dataDir, File(target, "dentro"), rename = { _, _ -> false }))

        assertEquals("[1]", File(dataDir, "quotes.json").readText())
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
