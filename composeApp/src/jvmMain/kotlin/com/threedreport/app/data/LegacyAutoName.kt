package com.threedreport.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Nome automático da 2.1 e anteriores, em português, do jeito que o app gravava ("Orçamento - 24/09/2026 14:30").
 * **Só serve pra ler dado da 2.1:** até ela, o app reconhecia o nome automático por este texto; da 2.2 em diante quem
 * diz é o campo `SavedQuote.autoNamed`, e o texto vem de `strings.xml` (`quote_auto_name`), que pode estar em outro
 * idioma.
 */
private val LEGACY_AUTO_NAME = Regex("""^Orçamento - \d{2}/\d{2}/\d{4} \d{2}:\d{2}$""")

/**
 * Marca `autoNamed` na leitura do `quotes.json` (Leva 10B, parte 7), sem mudar o formato da pasta de dados: o registro
 * que vem **sem a chave** (gravado pela 2.1, ou pela 2.1 de novo depois de a pessoa voltar de versão) ganha
 * `autoNamed` = nome igual ao [LEGACY_AUTO_NAME]. O que já tem a chave fica como está: a 2.2 grava todo campo
 * (`encodeDefaults` em [dataJson]), então um nome digitado que só parece automático vem com `false` e não é remarcado.
 * Vale pros da lixeira, pra cópia `.bak` e pro backup restaurado, que são lidos pelo mesmo caminho.
 */
internal fun markLegacyAutoNamedQuotes(file: JsonElement): JsonElement {
    if (file !is JsonArray) return file
    return JsonArray(
        file.map { record ->
            val data = (record as? JsonObject)?.get("data") as? JsonObject ?: return@map record
            if ("autoNamed" in data) return@map record
            val name = (data["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
            JsonObject(record + ("data" to JsonObject(data + ("autoNamed" to JsonPrimitive(LEGACY_AUTO_NAME.matches(name))))))
        },
    )
}
