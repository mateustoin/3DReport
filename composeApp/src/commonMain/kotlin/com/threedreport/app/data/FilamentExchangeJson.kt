package com.threedreport.app.data

import com.threedreport.core.model.ExchangedFilament
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentExchange
import com.threedreport.core.model.FilamentExchangeFile
import com.threedreport.core.model.FilamentImportError
import com.threedreport.core.model.FilamentImportPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * O texto JSON do arquivo de filamentos (decisão 164). As regras (o que vai, o que é repetido, o que é inválido) são do
 * `core` ([FilamentExchange]); aqui só se lê e escreve o texto, porque o `core` não tem a biblioteca de JSON.
 */
object FilamentExchangeJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** O arquivo com os filamentos não arquivados de [filaments] ([FilamentExchange.fileOf]). */
    fun encode(filaments: List<Filament>): String = json.encodeToString(FilamentExchangeFile.serializer(), FilamentExchange.fileOf(filaments))

    /**
     * O que importar [text] num cadastro com [existing] faria ([FilamentExchange.plan]). Lê primeiro só o cabeçalho:
     * uma versão mais nova, com campos que esta não conhece, dá [FilamentImportError.NEWER_VERSION]. Texto que não é
     * JSON, ou não é deste formato (sem cabeçalho certo, ou com `filaments` que não é lista), dá
     * [FilamentImportError.NOT_THIS_FORMAT].
     *
     * Depois cada filamento é lido sozinho: um com campo de tipo errado (preço como texto, cores `null`) conta como
     * inválido, como o que tem dado fora da regra, e não impede os outros. O BOM do UTF-8 no começo do texto, que alguns
     * editores gravam, é ignorado.
     */
    fun plan(text: String, existing: List<Filament>, newId: () -> String): FilamentImportPlan {
        val root = runCatching { json.parseToJsonElement(text.removePrefix(BYTE_ORDER_MARK)) }.getOrNull() as? JsonObject
            ?: return FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT)
        val header = runCatching { json.decodeFromJsonElement(Header.serializer(), root) }.getOrNull()
            ?: return FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT)
        FilamentExchange.headerError(header.format, header.version)?.let { return FilamentImportPlan.Rejected(it) }
        val entries = when (val filaments = root["filaments"]) {
            null -> emptyList()
            is JsonArray -> filaments
            else -> return FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT)
        }
        val decoded = entries.mapNotNull { entry -> runCatching { json.decodeFromJsonElement(ExchangedFilament.serializer(), entry) }.getOrNull() }
        val file = FilamentExchangeFile(format = header.format, version = header.version, filaments = decoded)
        return when (val plan = FilamentExchange.plan(file, existing, newId)) {
            is FilamentImportPlan.Ready -> plan.copy(invalid = plan.invalid + (entries.size - decoded.size))
            is FilamentImportPlan.Rejected -> plan
        }
    }

    private val BYTE_ORDER_MARK = Char(0xFEFF).toString()

    @Serializable
    private data class Header(val format: String? = null, val version: Int? = null)
}

/** "Exportar filamentos": o texto do arquivo com os filamentos não arquivados do cadastro. */
fun FilamentRepository.exportFilamentsJson(): String = FilamentExchangeJson.encode(filaments.value)

/**
 * "Importar filamentos": lê [text] e acrescenta ao cadastro os filamentos novos, com ids novos, pulando os repetidos e
 * os inválidos. Devolve o plano aplicado, com as contagens pro aviso, ou o motivo da recusa (e aí nada é importado).
 */
@OptIn(ExperimentalUuidApi::class)
fun FilamentRepository.importFilaments(text: String, newId: () -> String = { Uuid.random().toString() }): FilamentImportPlan {
    val plan = FilamentExchangeJson.plan(text, filaments.value, newId)
    if (plan is FilamentImportPlan.Ready) plan.toAdd.forEach(::add)
    return plan
}
