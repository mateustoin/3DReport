package com.threedreport.app.data

import kotlinx.serialization.json.Json
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
 */
internal const val DATA_FORMAT_VERSION = 2

private const val FORMAT_FILE_NAME = "format.json"

private val formatJson = Json { prettyPrint = true }

/** O que [prepareDataDir] fez com a pasta de dados. */
sealed interface DataDirResult {

    /** A pasta está pronta pra uso. [moved] diz onde os dados de outro formato ficaram, se houve. */
    data class Ready(val moved: MovedData? = null) : DataDirResult

    /**
     * Não deu pra guardar os dados de outro formato à parte. O app não pode abrir assim: os
     * arquivos antigos seriam lidos pela metade (campos renomeados viram vazio) e a próxima gravação
     * apagaria o resto.
     */
    data class Failed(val dataDir: File) : DataDirResult
}

/** Onde os dados de outro formato foram guardados e se eram de uma versão mais nova do app. */
data class MovedData(val path: File, val fromNewerVersion: Boolean)

/**
 * Confere o formato da pasta de dados **antes** de qualquer repositório ler dela (decisão 104).
 * Uma pasta de outro formato não é convertida: ela é movida inteira, sem apagar nada, pra uma
 * pasta irmã (`~/.3dreport-v1` pra dados da 1.x, `~/.3dreport-v3` pra dados de uma versão mais
 * nova, de quem voltou pra esta), e o app começa numa pasta nova. Quem quiser os dados de volta
 * abre a versão certa e renomeia a pasta.
 *
 * Pasta vazia ou nova só ganha o `format.json`. Um `format.json` que existe mas não abre
 * (gravação interrompida) conta como a versão atual: só esta versão grava esse arquivo, e mover os
 * dados por causa dele seria pior do que regravá-lo.
 */
fun prepareDataDir(): DataDirResult {
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
    val base = "${dataDir.name}-v${version ?: 1}"
    val target = generateSequence(1) { it + 1 }
        .map { attempt -> File(parent, if (attempt == 1) base else "$base-$attempt") }
        .first { !it.exists() }
    if (!moveDataDir(dataDir, target)) return DataDirResult.Failed(dataDir)
    writeDataFormatVersion(File(appDataDir(), FORMAT_FILE_NAME))
    return DataDirResult.Ready(MovedData(target, fromNewerVersion = version != null && version > DATA_FORMAT_VERSION))
}

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

private fun writeDataFormatVersion(formatFile: File) {
    formatFile.parentFile?.mkdirs()
    formatFile.writeText(formatJson.encodeToString(JsonObject.serializer(), buildJsonObject { put("version", DATA_FORMAT_VERSION) }))
}
