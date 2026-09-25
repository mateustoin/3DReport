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
}
