package com.threedreport.app.data

import com.threedreport.app.APP_VERSION
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Arquivo de identificação gravado na raiz do `.zip`. Não faz parte dos dados
 * do app: serve pra reconhecer, na restauração, que o arquivo escolhido é
 * mesmo um backup do 3DReport e não um `.zip` qualquer.
 */
private const val MANIFEST_ENTRY = "3dreport-backup.json"

private const val MANIFEST_APP_NAME = "3DReport"

// Montado com a API de JSON em runtime (e não com uma classe `@Serializable`) porque o módulo
// `composeApp` não aplica o plugin de serialização — todas as classes serializáveis do projeto
// vivem no módulo `core`.
private val manifestJson = Json { prettyPrint = true }

actual class BackupRepository actual constructor() {

    actual fun createBackupZip(): ByteArray {
        val dataDir = appDataDir()
        val output = ByteArrayOutputStream()

        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestContent().encodeToByteArray())
            zip.closeEntry()

            dataDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relativePath = file.relativeTo(dataDir).invariantSeparatorsPath
                if (relativePath == MANIFEST_ENTRY) return@forEach
                zip.putNextEntry(ZipEntry(relativePath))
                zip.write(file.readBytes())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    actual fun suggestedBackupFileName(): String =
        "3dreport-backup-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.zip"

    actual fun restoreFromZip(zipBytes: ByteArray): RestoreResult {
        val dataDir = appDataDir()
        val parent = dataDir.parentFile ?: return RestoreResult.Failure("Não consegui acessar a pasta de dados do app.")

        // Pastas irmãs da pasta de dados (mesmo disco), pra que a troca no fim seja uma renomeação
        // simples em vez de copiar arquivo por arquivo — copiar tem como falhar no meio.
        val staging = File(parent, "${dataDir.name}-restaurando")
        val previous = File(parent, "${dataDir.name}-anterior-${timestamp()}")

        staging.deleteRecursively()
        if (runCatching { extractInto(zipBytes, staging) }.isFailure) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Não consegui ler esse arquivo — ele pode não ser um .zip válido ou estar corrompido.")
        }

        val manifest = File(staging, MANIFEST_ENTRY)
        if (!isBackupOfThisApp(manifest)) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Esse arquivo não parece ser um backup do 3DReport. Nada foi alterado.")
        }
        // Um backup de outro formato de dados não abriria aqui (decisão 104): restaurar só trocaria os
        // dados atuais por arquivos que o app não lê.
        val backupFormat = dataFormatVersionOf(manifest)
        if (backupFormat != DATA_FORMAT_VERSION) {
            staging.deleteRecursively()
            return RestoreResult.Failure(
                if (backupFormat == null || backupFormat < DATA_FORMAT_VERSION) {
                    "Esse backup é de uma versão com formato de dados anterior (1.x) e não abre nesta. " +
                        "Abra com a versão em que ele foi feito. Nada foi alterado."
                } else {
                    "Esse backup é de uma versão mais nova do 3DReport. Atualize o app pra restaurar. Nada foi alterado."
                },
            )
        }
        manifest.delete()

        if (dataDir.exists() && !dataDir.renameTo(previous)) {
            staging.deleteRecursively()
            return RestoreResult.Failure("Não consegui guardar uma cópia dos dados atuais. Nada foi alterado.")
        }
        if (!staging.renameTo(dataDir)) {
            previous.renameTo(dataDir)
            staging.deleteRecursively()
            return RestoreResult.Failure("Não consegui aplicar o backup. Seus dados atuais foram mantidos.")
        }
        return RestoreResult.Success(previousDataPath = previous.path)
    }

    /**
     * Um `.zip` pode conter nomes de entrada como `../../algo` que, extraídos sem cuidado,
     * gravariam arquivos fora da pasta de destino ("zip slip"). Só aceitamos entradas que,
     * já resolvidas, continuam dentro de [targetDir].
     */
    private fun extractInto(zipBytes: ByteArray, targetDir: File) {
        targetDir.mkdirs()
        val targetRoot = targetDir.canonicalFile

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val current = entry
                val outFile = File(targetDir, current.name)
                require(outFile.canonicalFile.startsWith(targetRoot)) { "entrada fora da pasta de destino: ${current.name}" }

                if (current.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
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
}
