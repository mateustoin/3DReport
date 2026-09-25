package com.threedreport.app.data

import com.threedreport.app.APP_VERSION
import com.threedreport.app.AppLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Arquivo de identificação gravado na raiz do `.zip`. Não faz parte dos dados
 * do app: serve pra reconhecer, na restauração, que o arquivo escolhido é
 * mesmo um backup do 3DReport e não um `.zip` qualquer.
 */
private const val MANIFEST_ENTRY = "3dreport-backup.json"

private const val MANIFEST_APP_NAME = "3DReport"

private const val BACKUP_FILE_PREFIX = "3dreport-backup-"

private val manifestJson = Json { prettyPrint = true }

/**
 * [BackupRepository] da pasta de dados local. Tudo em fluxo, arquivo por arquivo (decisão 108): nem o
 * backup nem a restauração montam o `.zip` inteiro na memória.
 *
 * @param migrations as mesmas de [prepareDataDir]: backup de um formato anterior é convertido ao
 *   restaurar, em vez de recusado.
 */
class LocalBackupRepository(
    private val dataDir: File,
    private val migrations: Map<Int, DataMigration> = DATA_MIGRATIONS,
) : BackupRepository {

    override fun createBackup(targetPath: String) {
        val target = File(targetPath)
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "${target.name}.parcial")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(partial))).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestContent().encodeToByteArray())
            zip.closeEntry()

            dataDir.walkTopDown()
                .onEnter { dir -> dir == dataDir || dir.relativeTo(dataDir).invariantSeparatorsPath !in SKIPPED_DIRS }
                .filter { it.isFile && !isSkippedFile(it) }
                .forEach { file ->
                    val relativePath = file.relativeTo(dataDir).invariantSeparatorsPath
                    if (relativePath == MANIFEST_ENTRY) return@forEach
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        // Só troca o nome no fim: um backup interrompido nunca parece completo.
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
    }

    override fun suggestedBackupFileName(): String =
        "$BACKUP_FILE_PREFIX${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.zip"

    override fun defaultAutomaticBackupDirectory(): String =
        File(System.getProperty("user.home"), "3DReport Backups").path

    override fun createAutomaticBackup(directory: String, keep: Int): String? {
        val dir = File(directory)
        val today = File(dir, suggestedBackupFileName())
        if (today.exists()) return null
        createBackup(today.path)
        // Os nomes têm a data ISO, então a ordem alfabética é a cronológica.
        dir.listFiles { file -> file.isFile && file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(".zip") }
            ?.sortedByDescending { it.name }
            ?.drop(keep.coerceAtLeast(1))
            ?.forEach { old -> if (!old.delete()) AppLog.warn("Não consegui apagar o backup antigo ${old.name}") }
        return today.path
    }

    override fun restore(sourcePath: String): RestoreResult {
        val parent = dataDir.parentFile ?: return RestoreResult.Failure("Não consegui acessar a pasta de dados do app.")

        // Pastas irmãs da pasta de dados (mesmo disco), pra que a troca no fim seja uma renomeação
        // simples em vez de copiar arquivo por arquivo — copiar tem como falhar no meio.
        val staging = File(parent, "${dataDir.name}-restaurando")
        val previous = File(parent, "${dataDir.name}-anterior-${timestamp()}")

        staging.deleteRecursively()
        if (runCatching { extractInto(File(sourcePath), staging) }.isFailure) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Não consegui ler esse arquivo — ele pode não ser um .zip válido ou estar corrompido.")
        }

        val manifest = File(staging, MANIFEST_ENTRY)
        if (!isBackupOfThisApp(manifest)) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Esse arquivo não parece ser um backup do 3DReport. Nada foi alterado.")
        }
        val backupFormat = dataFormatVersionOf(manifest) ?: 1
        if (backupFormat > DATA_FORMAT_VERSION) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Esse backup é de uma versão mais nova do 3DReport. Atualize o app pra restaurar. Nada foi alterado.")
        }
        if (backupFormat < DATA_FORMAT_VERSION) {
            // Backup de um formato anterior é convertido, como a pasta de dados ao abrir (decisão 106).
            val path = migrationPath(backupFormat, migrations)
            val converted = path != null && runCatching { path.forEach { it.migrate(staging) } }.isSuccess
            if (!converted) {
                staging.deleteRecursively()
                return RestoreResult.Failure(
                    "Esse backup é de uma versão com formato de dados anterior (1.x) e não abre nesta. " +
                        "Abra com a versão em que ele foi feito. Nada foi alterado.",
                )
            }
        }
        manifest.delete()
        writeDataFormatVersion(File(staging, "format.json"))

        if (dataDir.exists() && !dataDir.renameTo(previous)) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Não consegui guardar uma cópia dos dados atuais. Feche outros programas que usem a pasta e tente de novo. Nada foi alterado.")
        }
        if (!staging.renameTo(dataDir)) {
            val rolledBack = previous.renameTo(dataDir)
            staging.deleteRecursively()
            return RestoreResult.Failure(
                if (rolledBack) {
                    "Não consegui aplicar o backup. Seus dados atuais foram mantidos."
                } else {
                    "Não consegui aplicar o backup, e seus dados atuais ficaram guardados em ${previous.path}. " +
                        "Renomeie essa pasta de volta pra ${dataDir.name} antes de abrir o app de novo."
                },
            )
        }
        return RestoreResult.Success(previousDataPath = previous.path)
    }

    /**
     * Um `.zip` pode conter nomes de entrada como `../../algo` que, extraídos sem cuidado,
     * gravariam arquivos fora da pasta de destino ("zip slip"). Só aceitamos entradas que,
     * já resolvidas, continuam dentro de [targetDir].
     */
    private fun extractInto(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        val targetRoot = targetDir.canonicalFile

        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(targetDir, entry.name)
                require(outFile.canonicalFile.startsWith(targetRoot)) { "entrada fora da pasta de destino: ${entry.name}" }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> outFile.outputStream().use { input.copyTo(it) } }
                }
            }
        }
    }

    private fun manifestContent(): String = manifestJson.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("app", MANIFEST_APP_NAME)
            put("appVersion", APP_VERSION)
            put("dataFormatVersion", DATA_FORMAT_VERSION)
            put("createdAtEpochMillis", System.currentTimeMillis())
        },
    )

    private fun isBackupOfThisApp(manifestFile: File): Boolean {
        if (!manifestFile.isFile) return false
        val app = runCatching {
            manifestJson.parseToJsonElement(manifestFile.readText()).jsonObject["app"]?.jsonPrimitive?.content
        }.getOrNull()
        return app == MANIFEST_APP_NAME
    }

    private fun dataFormatVersionOf(manifestFile: File): Int? = runCatching {
        manifestJson.parseToJsonElement(manifestFile.readText()).jsonObject["dataFormatVersion"]?.jsonPrimitive?.int
    }.getOrNull()

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))

    private companion object {
        /** Pastas que o app refaz sozinho e não precisam ir no backup (log, miniaturas). */
        val SKIPPED_DIRS = setOf("logs", "${LocalStorage.ATTACHMENTS_DIR}/thumbs")

        /** Arquivos de passagem da gravação atômica: o dado de verdade está no arquivo principal. */
        fun isSkippedFile(file: File) = file.name.endsWith(".tmp") || file.name.endsWith(".bak") || file.name == ".lock"
    }
}
