package com.threedreport.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Nome automático da 2.1 e anteriores, em português, do jeito que o app gravava ("Orçamento - 24/09/2026 14:30").
 * **Só serve pra migração:** até a 2.1, o app reconhecia o nome automático por este texto; da 2.2 em diante quem diz
 * é o campo `SavedQuote.autoNamed`, e o texto vem de `strings.xml` (`quote_auto_name`), que pode estar em outro idioma.
 */
private val LEGACY_AUTO_NAME = Regex("""^Orçamento - \d{2}/\d{2}/\d{4} \d{2}:\d{2}$""")

/**
 * Formato 2 pra 3 (Leva 10B, parte 7): marca `autoNamed` nos pedidos do `quotes.json` cujo nome é o automático da
 * 2.1 ([LEGACY_AUTO_NAME]), inclusive os da lixeira. O nome não muda. Registro que já tem o campo fica como está.
 *
 * A cópia anterior (`quotes.json.bak`) também é convertida, porque é dela que a leitura recupera quando o principal
 * não abre. Um arquivo que não é JSON fica como está: a leitura o guarda como ilegível e avisa, como sempre; não é
 * motivo pra deixar a pasta inteira de lado.
 */
internal val MARK_AUTO_NAMED_QUOTES = DataMigration { dataDir ->
    listOf("quotes.json", "quotes.json.bak")
        .map { File(dataDir, it) }
        .filter { it.isFile && isJson(it) }
        .forEach { file -> file.updateJson(::markAutoNamed) }
}

private fun isJson(file: File): Boolean = runCatching { Json.parseToJsonElement(file.readText()) }.isSuccess

private fun markAutoNamed(file: JsonElement): JsonElement {
    if (file !is JsonArray) return file
    return JsonArray(
        file.map { record ->
            val data = (record as? JsonObject)?.get("data") as? JsonObject ?: return@map record
            if ("autoNamed" in data) return@map record
            val name = (data["name"] as? JsonPrimitive)?.takeIf { it.isString }?.jsonPrimitive?.contentOrNull.orEmpty()
            JsonObject(record + ("data" to JsonObject(data + ("autoNamed" to JsonPrimitive(LEGACY_AUTO_NAME.matches(name))))))
        },
    )
}
