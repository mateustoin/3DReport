package com.threedreport.app.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O caminho de restauração troca a pasta de dados inteira do usuário, então o que está sendo
 * verificado aqui é principalmente o que **não** pode acontecer: perder os dados atuais quando a
 * restauração falha, e aceitar um `.zip` que grave arquivos fora da pasta de dados.
 */
class BackupRepositoryTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun backupRestoresFilesAndSubfoldersExactlyAsTheyWere() {
        File(dataDir, "quotes.json").writeText("orçamentos originais")
        File(dataDir, "photos").mkdirs()
        val photoBytes = byteArrayOf(1, 2, 3, 4)
        File(dataDir, "photos/produto.png").writeBytes(photoBytes)

        val repository = BackupRepository()
        val backup = repository.createBackupZip()

        // Simula o estrago que a restauração precisa desfazer.
        File(dataDir, "quotes.json").writeText("orçamentos estragados")
        File(dataDir, "photos/produto.png").delete()

        val result = repository.restoreFromZip(backup)

        assertIs<RestoreResult.Success>(result)
        assertEquals("orçamentos originais", File(dataDir, "quotes.json").readText())
        assertContentEquals(photoBytes, File(dataDir, "photos/produto.png").readBytes())
    }

    @Test
    fun theSellerLogoTravelsInTheBackupAndComesBackOnRestore() {
        val logoBytes = byteArrayOf(9, 8, 7)
        BrandingRepository().update(
            com.threedreport.core.model.BrandingSettings(brandName = "Loja"),
            LogoChange.Replace(com.threedreport.app.platform.PickedFile("logo.png", logoBytes)),
        )
        val backup = BackupRepository().createBackupZip()

        File(dataDir, "branding").deleteRecursively()
        BackupRepository().restoreFromZip(backup)

        assertContentEquals(logoBytes, BrandingRepository().logoBytes())
    }

    @Test
    fun restoreKeepsTheReplacedDataAsACopy() {
        File(dataDir, "quotes.json").writeText("dados atuais")
        val repository = BackupRepository()
        val backup = repository.createBackupZip()
        File(dataDir, "quotes.json").writeText("dados que serão substituídos")

        val result = repository.restoreFromZip(backup)

        val previousDir = File(assertIs<RestoreResult.Success>(result).previousDataPath)
        assertTrue(previousDir.isDirectory, "a cópia dos dados anteriores deveria existir")
        assertEquals("dados que serão substituídos", File(previousDir, "quotes.json").readText())
    }

    @Test
    fun restoreRejectsAZipThatIsNotABackupAndKeepsCurrentData() {
        File(dataDir, "quotes.json").writeText("dados atuais")
        val notABackup = zipOf("qualquer-coisa.txt" to "conteúdo".encodeToByteArray())

        val result = BackupRepository().restoreFromZip(notABackup)

        assertIs<RestoreResult.Failure>(result)
        assertEquals("dados atuais", File(dataDir, "quotes.json").readText())
    }

    @Test
    fun restoreRejectsACorruptedFileAndKeepsCurrentData() {
        File(dataDir, "quotes.json").writeText("dados atuais")

        val result = BackupRepository().restoreFromZip(byteArrayOf(0, 1, 2, 3, 4, 5))

        assertIs<RestoreResult.Failure>(result)
        assertEquals("dados atuais", File(dataDir, "quotes.json").readText())
    }

    @Test
    fun restoreRefusesEntriesThatWouldWriteOutsideTheDataFolder() {
        File(dataDir, "quotes.json").writeText("dados atuais")
        val zipSlip = zipOf(
            "3dreport-backup.json" to """{"app":"3DReport","appVersion":"1.0.0","createdAtEpochMillis":0}""".encodeToByteArray(),
            "../invasor.txt" to "não deveria ser gravado".encodeToByteArray(),
        )

        val result = BackupRepository().restoreFromZip(zipSlip)

        assertIs<RestoreResult.Failure>(result)
        assertFalse(File(dataDir.parentFile, "invasor.txt").exists(), "o arquivo fora da pasta de dados não pode ser gravado")
        assertEquals("dados atuais", File(dataDir, "quotes.json").readText())
    }

    @Test
    fun backupDoesNotLeaveItsOwnManifestInsideTheRestoredData() {
        File(dataDir, "quotes.json").writeText("dados")
        val repository = BackupRepository()

        repository.restoreFromZip(repository.createBackupZip())

        assertFalse(File(dataDir, "3dreport-backup.json").exists())
    }

    @Test
    fun automaticBackupRunsOncePerDayAndKeepsOnlyTheLatestOnes() {
        File(dataDir, "quotes.json").writeText("dados")
        val backups = createTempDirectory("3dreport-backups").toFile()
        listOf("2026-01-01", "2026-01-02", "2026-01-03").forEach { day ->
            File(backups, "3dreport-backup-$day.zip").writeText("antigo")
        }
        File(backups, "outro-arquivo.zip").writeText("não é backup do app")
        val repository = BackupRepository()

        val created = repository.createAutomaticBackup(backups.path, keep = 2)

        assertTrue(File(created!!).isFile)
        assertEquals(null, repository.createAutomaticBackup(backups.path, keep = 2), "o do dia já existe")
        val remaining = backups.listFiles()!!.map { it.name }.sorted()
        assertEquals(listOf("3dreport-backup-2026-01-03.zip", File(created).name, "outro-arquivo.zip").sorted(), remaining)
    }

    @Test
    fun backupLeavesOutLogsThumbnailsAndHalfWrittenFiles() {
        File(dataDir, "quotes.json").writeText("dados")
        File(dataDir, "quotes.json.tmp").writeText("pela metade")
        File(dataDir, "logs").mkdirs()
        File(dataDir, "logs/3dreport.log").writeText("log")
        File(dataDir, "attachments/thumbs/256").mkdirs()
        File(dataDir, "attachments/thumbs/256/x.png").writeText("miniatura")
        File(dataDir, "attachments/foto.png").writeText("foto")

        val names = java.util.zip.ZipInputStream(BackupRepository().createBackupZip().inputStream()).use { zip ->
            generateSequence { zip.nextEntry?.name }.toList()
        }

        assertTrue("quotes.json" in names)
        assertTrue("attachments/foto.png" in names)
        assertFalse(names.any { it.startsWith("logs") || it.contains("thumbs") || it.endsWith(".tmp") }, "entradas: $names")
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    /**
     * Revisão do PR #2 (zip slip): uma entrada que sai pra uma pasta irmã cujo nome começa igual ao da
     * pasta de trabalho ("...-restaurando2") também é recusada. A comparação é por partes do caminho, e
     * não por começo de texto.
     */
    @Test
    fun restoreRefusesEntriesIntoASiblingFolderWithTheSamePrefix() {
        File(dataDir, "quotes.json").writeText("dados atuais")
        val sibling = "${dataDir.name}-restaurando2"
        val zipSlip = zipOf(
            "3dreport-backup.json" to """{"app":"3DReport","appVersion":"1.0.0","createdAtEpochMillis":0,"dataFormatVersion":2}""".encodeToByteArray(),
            "../$sibling/invasor.txt" to "não deveria ser gravado".encodeToByteArray(),
        )

        val result = BackupRepository().restoreFromZip(zipSlip)

        assertIs<RestoreResult.Failure>(result)
        assertFalse(File(dataDir.parentFile, "$sibling/invasor.txt").exists())
        assertEquals("dados atuais", File(dataDir, "quotes.json").readText())
    }
}