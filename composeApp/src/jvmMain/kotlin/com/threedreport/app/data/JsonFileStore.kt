package com.threedreport.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

/**
 * Pasta de dados do app no usuário atual (`~/.3dreport`). Criada se não existir.
 *
 * Pode ser sobrescrita pela propriedade de sistema `threedreport.dataDir`
 * (usado pelos testes, para não escrever na pasta real do usuário).
 */
internal fun appDataDir(): File {
    val override = System.getProperty("threedreport.dataDir")
    val dir = if (override != null) File(override) else File(System.getProperty("user.home"), ".3dreport")
    return dir.apply { mkdirs() }
}

/**
 * Lê e desserializa [file], ou devolve [default] se o arquivo não existe ou não dá pra ler.
 *
 * O arquivo que não dá pra ler é guardado ao lado, como `<nome>.ilegivel-<millis>.json`, antes de
 * devolver o padrão (decisão 104). Sem isso, a próxima gravação do repositório sobrescrevia o
 * arquivo e o histórico sumia em silêncio. Dados de outro formato nem chegam aqui: [prepareDataDir]
 * guarda a pasta inteira antes.
 */
internal inline fun <reified T> readJsonFile(file: File, default: T): T {
    if (!file.exists()) return default
    val text = runCatching { file.readText() }.getOrElse { return default }
    return try {
        json.decodeFromString(serializer<T>(), text)
    } catch (_: IllegalArgumentException) {
        // Só erro de formato (a `SerializationException` e um `require` de modelo são os dois
        // `IllegalArgumentException`). Uma falha de leitura do disco não quer dizer que o conteúdo
        // está errado, e o arquivo fica onde está.
        file.renameTo(File(file.parentFile, "${file.nameWithoutExtension}.ilegivel-${System.currentTimeMillis()}.${file.extension}"))
        default
    }
}

/** Serializa [value] e grava em [file], substituindo o conteúdo anterior. */
internal inline fun <reified T> writeJsonFile(file: File, value: T) {
    file.parentFile?.mkdirs()
    file.writeText(json.encodeToString(value))
}
