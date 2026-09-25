package com.threedreport.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * Versão do formato dos arquivos da pasta de dados (decisão 104). A 1 vale até a 1.44; a 2 veio com
 * a leva 9 (pedido com várias impressões e vários filamentos, e sem os campos de compatibilidade).
 * Só muda quando o formato quebra: campo novo com valor padrão não muda a versão.
 *
 * **A partir da 2.0.0 publicada, toda mudança que quebra o formato sobe esta versão e vem com uma
 * [DataMigration] em [DATA_MIGRATIONS], com teste** (decisão 106). Recomeçar do zero só foi aceitável
 * enquanto o app não tinha sido divulgado.
 */
internal const val DATA_FORMAT_VERSION = 2

private const val FORMAT_FILE_NAME = "format.json"

private val formatJson = Json { prettyPrint = true }

/**
 * Converte a pasta de dados de uma versão do formato pra seguinte, reescrevendo os arquivos no lugar.
 * Roda sempre sobre uma cópia (ver [prepareDataDir] e a restauração de backup): se lançar exceção, os
 * dados originais continuam intactos.
 */
fun interface DataMigration {
    fun migrate(dataDir: File)
}

/**
 * Migrações conhecidas: a chave é a versão de origem, e a migração leva pra versão seguinte. Vazio na
 * 2.0.0, que é a base; a primeira quebra depois dela entra aqui como `2 to DataMigration { ... }`.
 */
internal val DATA_MIGRATIONS: Map<Int, DataMigration> = emptyMap()

/** As migrações, em ordem, de [from] até a versão atual, ou `null` se falta alguma no caminho. */
internal fun migrationPath(from: Int, migrations: Map<Int, DataMigration>): List<DataMigration>? =
    (from until DATA_FORMAT_VERSION).map { version -> migrations[version] ?: return null }

/** Lê o JSON de [this], aplica [transform] e grava de volta. Pra escrever migrações. */
fun File.updateJson(transform: (JsonElement) -> JsonElement) {
    val element = Json.parseToJsonElement(readText())
    writeText(formatJson.encodeToString(JsonElement.serializer(), transform(element)))
}

/** O que [prepareDataDir] fez com a pasta de dados. */
sealed interface DataDirResult {

    /**
     * A pasta está pronta pra uso. [moved] diz onde os dados de outro formato ficaram, se foram
     * guardados à parte; [migrated] diz de qual versão os dados foram convertidos e onde ficou a cópia
     * original.
     */
    data class Ready(val moved: MovedData? = null, val migrated: MigratedData? = null) : DataDirResult

    /**
     * Não deu pra guardar os dados de outro formato à parte. O app não pode abrir assim: os
     * arquivos antigos seriam lidos pela metade (campos renomeados viram vazio) e a próxima gravação
     * apagaria o resto.
     */
    data class Failed(val dataDir: File) : DataDirResult
}

/** Onde os dados de outro formato foram guardados e se eram de uma versão mais nova do app. */
data class MovedData(val path: File, val fromNewerVersion: Boolean)

/** Os dados foram convertidos da versão [fromVersion]; a cópia de antes da conversão ficou em [originalCopy]. */
data class MigratedData(val fromVersion: Int, val originalCopy: File)

/**
 * Confere o formato da pasta de dados **antes** de qualquer repositório ler dela (decisão 104).
 *
 * - Versão atual: nada a fazer.
 * - Versão anterior com migração conhecida ([migrations]): os dados são convertidos numa cópia, e só
 *   então trocam de lugar com os originais, que ficam guardados em `~/.3dreport-v<versão>`.
 * - Versão anterior sem migração, ou de uma versão mais nova (quem voltou pra esta): a pasta é movida
 *   inteira, sem apagar nada, pra uma pasta irmã, e o app começa numa pasta nova.
 *
 * Pasta vazia ou nova só ganha o `format.json`. Um `format.json` que existe mas não abre
 * (gravação interrompida) conta como a versão atual: só esta versão grava esse arquivo, e mover os
 * dados por causa dele seria pior do que regravá-lo.
 */
fun prepareDataDir(migrations: Map<Int, DataMigration> = DATA_MIGRATIONS): DataDirResult {
    val dataDir = appDataDir()
    val formatFile = File(dataDir, FORMAT_FILE_NAME)
    val version = when {
        !formatFile.isFile -> null
        else -> readDataFormatVersion(formatFile) ?: DATA_FORMAT_VERSION.also { writeDataFormatVersion(formatFile) }
    }
    if (version == DATA_FORMAT_VERSION) return DataDirResult.Ready()

    val hasData = dataDir.listFiles()?.any { it.name != FORMAT_FILE_NAME } == true
    if (!hasData) {
        writeDataFormatVersion(formatFile)
        return DataDirResult.Ready()
    }

    val parent = dataDir.parentFile ?: return DataDirResult.Failed(dataDir)
    val sourceVersion = version ?: 1
    val target = freeSibling(parent, "${dataDir.name}-v$sourceVersion")

    val path = if (sourceVersion < DATA_FORMAT_VERSION) migrationPath(sourceVersion, migrations) else null
    if (path != null) {
        val staging = File(parent, "${dataDir.name}-migrando")
        staging.deleteRecursively()
        val migrated = runCatching {
            dataDir.copyRecursively(staging, overwrite = false)
            path.forEach { it.migrate(staging) }
            writeDataFormatVersion(File(staging, FORMAT_FILE_NAME))
        }.isSuccess
        if (migrated && moveDataDir(dataDir, target) && staging.renameTo(dataDir)) {
            return DataDirResult.Ready(migrated = MigratedData(sourceVersion, target))
        }
        // Algo falhou: devolve os originais pro lugar (se já tinham saído) e segue pelo caminho de guardar à parte.
        staging.deleteRecursively()
        if (!dataDir.exists() && target.exists()) target.renameTo(dataDir)
    }

    if (!moveDataDir(dataDir, target)) return DataDirResult.Failed(dataDir)
    writeDataFormatVersion(File(appDataDir(), FORMAT_FILE_NAME))
    return DataDirResult.Ready(MovedData(target, fromNewerVersion = version != null && version > DATA_FORMAT_VERSION))
}

/** [base], ou `base-2`, `base-3`… o primeiro nome que ainda não existe em [parent]. */
private fun freeSibling(parent: File, base: String): File = generateSequence(1) { it + 1 }
    .map { attempt -> File(parent, if (attempt == 1) base else "$base-$attempt") }
    .first { !it.exists() }

/**
 * Move a pasta inteira. Renomear é o normal (mesmo disco), mas falha no Windows quando algum
 * programa está com a pasta aberta; aí copia tudo e só depois esvazia a original. Se nem a cópia
 * der certo, desfaz a cópia parcial e devolve `false`, sem ter mexido na original.
 */
internal fun moveDataDir(from: File, to: File, rename: (File, File) -> Boolean = File::renameTo): Boolean {
    if (rename(from, to)) return true
    val copied = runCatching { from.copyRecursively(to, overwrite = false) }.getOrDefault(false)
    if (!copied) {
        to.deleteRecursively()
        return false
    }
    from.listFiles()?.forEach { it.deleteRecursively() }
    return true
}

/** Versão gravada em [formatFile], ou `null` sem arquivo (pasta de antes da versão 2) ou ilegível. */
internal fun readDataFormatVersion(formatFile: File): Int? {
    if (!formatFile.isFile) return null
    return runCatching { formatJson.parseToJsonElement(formatFile.readText()).jsonObject["version"]?.jsonPrimitive?.int }.getOrNull()
}

internal fun writeDataFormatVersion(formatFile: File) {
    formatFile.parentFile?.mkdirs()
    formatFile.writeText(formatJson.encodeToString(JsonObject.serializer(), buildJsonObject { put("version", DATA_FORMAT_VERSION) }))
}
