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

/**
 * Confere o formato da pasta de dados **antes** de qualquer repositório ler dela. Uma pasta de uma
 * versão anterior (sem `format.json`, ou com versão menor) não é convertida (decisão 104): ela é
 * movida inteira, sem apagar nada, pra uma pasta irmã (`~/.3dreport-v1`), e o app começa numa pasta
 * nova. Quem quiser os dados de volta reinstala a versão anterior e renomeia a pasta.
 *
 * Pasta vazia ou nova só ganha o `format.json`. Uma pasta de versão **mais nova** que esta não é
 * tocada: mover os dados de quem voltou pra uma versão anterior seria pior.
 *
 * @return onde os dados antigos foram guardados, pra avisar a pessoa uma vez; `null` quando nada
 *   foi movido.
 */
fun prepareDataDir(): File? {
    val dataDir = appDataDir()
    val formatFile = File(dataDir, FORMAT_FILE_NAME)
    val version = readDataFormatVersion(formatFile)
    if (version != null && version >= DATA_FORMAT_VERSION) return null

    val hasData = dataDir.listFiles()?.any { it.name != FORMAT_FILE_NAME } == true
    if (!hasData) {
        writeDataFormatVersion(formatFile)
        return null
    }

    val parent = dataDir.parentFile ?: return null
    val base = "${dataDir.name}-v${version ?: 1}"
    val target = generateSequence(1) { it + 1 }
        .map { attempt -> File(parent, if (attempt == 1) base else "$base-$attempt") }
        .first { !it.exists() }
    // Sem conseguir mover, não mexe em nada: os arquivos antigos que não abrirem são guardados um a um
    // por `readJsonFile`, em vez de sobrescritos.
    if (!dataDir.renameTo(target)) return null
    writeDataFormatVersion(File(appDataDir(), FORMAT_FILE_NAME))
    return target
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
