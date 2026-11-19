package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * O arquivo de exportação de filamentos (decisão 164): `{"format": "3dreport-filaments", "version": 1, "filaments": [...]}`.
 * Só o que outro vendedor (ou outra instalação) precisa pra cadastrar: sem ids, saldo, compras nem "tem / acabou".
 *
 * Tudo nasce `null` de propósito: um arquivo sem `format` não pode passar por um do formato, e um filamento com um
 * campo faltando é contado como inválido em vez de derrubar o arquivo inteiro. O `core` só tem os tipos e as regras;
 * ler e escrever o texto JSON é do `composeApp`, que tem a biblioteca de JSON.
 */
@Serializable
data class FilamentExchangeFile(
    val format: String? = null,
    val version: Int? = null,
    val filaments: List<ExchangedFilament> = emptyList(),
)

/** Um filamento no arquivo de exportação ([FilamentExchangeFile]), com os mesmos nomes de campo do [Filament]. */
@Serializable
data class ExchangedFilament(
    val name: String? = null,
    val brand: String? = null,
    val materialType: String? = null,
    val densityGPerCm3: Double? = null,
    val diameterMm: Double? = null,
    val pricePerKg: Double? = null,
    val colors: List<ExchangedColor> = emptyList(),
)

/** Uma cor no arquivo de exportação: nome e hex, os dois opcionais. */
@Serializable
data class ExchangedColor(
    val name: String? = null,
    val hex: String? = null,
)

/** Por que um arquivo não pode ser importado. A tela escolhe o texto. */
enum class FilamentImportError {
    /** Não é um arquivo de filamentos do 3DReport (ou não deu pra ler). */
    NOT_THIS_FORMAT,

    /** É do formato, mas de uma versão mais nova que esta versão do app conhece. */
    NEWER_VERSION,
}

/** O que importar um [FilamentExchangeFile] faria ([FilamentExchange.plan]). */
sealed interface FilamentImportPlan {
    /**
     * @property toAdd os filamentos novos, com ids novos e todas as cores "tem".
     * @property alreadyPresent quantos foram pulados porque já estavam no cadastro (ou repetidos no arquivo).
     * @property invalid quantos foram pulados por dado inválido (nome vazio, densidade zero, preço faltando).
     */
    data class Ready(val toAdd: List<Filament>, val alreadyPresent: Int, val invalid: Int) : FilamentImportPlan

    /** O arquivo inteiro foi recusado: nada é importado. */
    data class Rejected(val error: FilamentImportError) : FilamentImportPlan
}

/** Exportar e importar filamentos (decisão 164). Funções puras. */
object FilamentExchange {
    const val FORMAT = "3dreport-filaments"
    const val VERSION = 1

    /** O arquivo com os filamentos não arquivados de [filaments]. */
    fun fileOf(filaments: List<Filament>): FilamentExchangeFile = FilamentExchangeFile(
        format = FORMAT,
        version = VERSION,
        filaments = filaments.filterNot { it.archived }.map { filament ->
            ExchangedFilament(
                name = filament.name,
                brand = filament.brand,
                materialType = filament.materialType,
                densityGPerCm3 = filament.densityGPerCm3,
                diameterMm = filament.diameterMm,
                pricePerKg = filament.pricePerKg,
                colors = filament.colors.map { ExchangedColor(it.name, it.hex) },
            )
        },
    )

    /**
     * Se o cabeçalho ([FilamentExchangeFile.format] e [FilamentExchangeFile.version]) diz que dá pra importar: `null`
     * quando dá. Quem lê o JSON confere isto antes de ler o resto, pra uma versão nova com campos diferentes dar
     * [FilamentImportError.NEWER_VERSION], e não arquivo inválido.
     */
    fun headerError(format: String?, version: Int?): FilamentImportError? = when {
        format != FORMAT || version == null || version < 1 -> FilamentImportError.NOT_THIS_FORMAT
        version > VERSION -> FilamentImportError.NEWER_VERSION
        else -> null
    }

    /**
     * O que importar [file] num cadastro com [existing] (inclusive arquivados) faria. Pula o filamento que já existe
     * com o mesmo nome, marca e tipo (ignorando maiúsculas e espaços sobrando), inclusive o repetido no próprio
     * arquivo, e o que tem dado inválido. Cada filamento e cada cor ganham id de [newId]; as cores nascem "tem".
     */
    fun plan(file: FilamentExchangeFile, existing: List<Filament>, newId: () -> String): FilamentImportPlan {
        headerError(file.format, file.version)?.let { return FilamentImportPlan.Rejected(it) }
        val known = existing.mapTo(HashSet()) { keyOf(it.name, it.brand, it.materialType) }
        val toAdd = mutableListOf<Filament>()
        var alreadyPresent = 0
        var invalid = 0
        for (entry in file.filaments) {
            val filament = entry.toFilament(newId)
            if (filament == null) {
                invalid++
                continue
            }
            if (!known.add(keyOf(filament.name, filament.brand, filament.materialType))) {
                alreadyPresent++
                continue
            }
            toAdd += filament
        }
        return FilamentImportPlan.Ready(toAdd, alreadyPresent, invalid)
    }

    /**
     * Nome sugerido pro arquivo exportado no dia [epochDay] (dias desde 01/01/1970): `filamentos-3dreport-2026-11-18.json`.
     * [prefix] fica com a tela, se ela quiser o nome no idioma de quem usa.
     */
    fun suggestedFileName(epochDay: Long, prefix: String = "filamentos-3dreport"): String {
        val (year, month, day) = civilDate(epochDay)
        return "$prefix-$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}.json"
    }

    private fun ExchangedFilament.toFilament(newId: () -> String): Filament? {
        val name = name.clean() ?: return null
        val density = densityGPerCm3?.takeIf { it > 0 && it.isFinite() } ?: return null
        val price = pricePerKg?.takeIf { it >= 0 && it.isFinite() } ?: return null
        val diameter = (diameterMm ?: Filament.DEFAULT_DIAMETER_MM).takeIf { it > 0 && it.isFinite() } ?: return null
        val importedColors = colors.map { FilamentColor(id = newId(), name = it.name.clean(), hex = it.hex.clean(), inStock = true) }
        return Filament(
            id = newId(),
            name = name,
            pricePerKg = price,
            densityGPerCm3 = density,
            diameterMm = diameter,
            brand = brand.clean(),
            materialType = materialType.clean(),
            colors = importedColors.ifEmpty { listOf(FilamentColor(id = "default")) },
        )
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

    private fun keyOf(name: String, brand: String?, materialType: String?) = listOf(name, brand.orEmpty(), materialType.orEmpty()).joinToString("\u0000") { it.normalized() }

    private val spaces = Regex("\\s+")

    private fun String.normalized() = trim().replace(spaces, " ").lowercase()

    /** Ano, mês e dia de [epochDay] no calendário gregoriano (algoritmo "civil_from_days", de Howard Hinnant). */
    private fun civilDate(epochDay: Long): Triple<Long, Int, Int> {
        val z = epochDay + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = yoe + era * 400 + (if (month <= 2) 1 else 0)
        return Triple(year, month, day)
    }
}
