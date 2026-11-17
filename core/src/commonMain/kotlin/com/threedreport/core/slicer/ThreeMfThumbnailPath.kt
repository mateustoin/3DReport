package com.threedreport.core.slicer

/** Onde fica a lista de relações da raiz de um pacote 3MF (OPC), que aponta a miniatura. */
const val THREE_MF_ROOT_RELATIONSHIPS = "_rels/.rels"

/** Tipo de relação OPC da miniatura do pacote. */
private const val THUMBNAIL_RELATIONSHIP_TYPE = "http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"

/**
 * Caminhos conhecidos da miniatura, na ordem de preferência, pra quando a relação não existe ou aponta pra uma
 * entrada que não está no pacote: PrusaSlicer e Bambu Studio (`Metadata/`) e OrcaSlicer (`Auxiliaries/.thumbnails/`).
 */
private val KNOWN_THUMBNAIL_PATHS = listOf(
    "Metadata/thumbnail.png",
    "Metadata/plate_1.png",
    "Auxiliaries/.thumbnails/thumbnail_3mf.png",
    "Auxiliaries/.thumbnails/thumbnail_middle.png",
)

private val PLATE_IMAGE = Regex("""metadata/plate_(\d+)\.png""")
private val ORCA_THUMBNAIL = Regex("""auxiliaries/\.thumbnails/thumbnail_[^/]+\.png""")
private val RELATIONSHIP_ELEMENT = Regex("""<\s*Relationship\b[^>]*>""", RegexOption.IGNORE_CASE)

/**
 * Caminho (como está no ZIP) da imagem de miniatura que o fatiador gravou no 3MF, ou `null` se não há (decisão 141).
 *
 * Primeiro vale o que o próprio pacote diz: a relação de miniatura em [rootRelationships] (o texto de
 * [THREE_MF_ROOT_RELATIONSHIPS], ou `null` se o pacote não tem). Se ela não existe ou aponta pra uma entrada que
 * não está em [entryNames], ficam os caminhos conhecidos de Bambu Studio, OrcaSlicer e PrusaSlicer, e depois a
 * imagem da primeira mesa (`Metadata/plate_N.png`) ou qualquer miniatura da OrcaSlicer, nunca a imagem de seleção
 * (`pick_N.png`), que é colorida por objeto. Nome de entrada é comparado sem diferenciar maiúscula.
 */
fun findThreeMfThumbnailPath(rootRelationships: String?, entryNames: Collection<String>): String? {
    val byLowerName = HashMap<String, String>()
    for (name in entryNames) byLowerName.getOrPut(name.removePrefix("/").lowercase()) { name }
    fun entry(path: String): String? = byLowerName[path.removePrefix("/").lowercase()]

    for (target in thumbnailTargets(rootRelationships)) entry(target)?.let { return it }
    for (path in KNOWN_THUMBNAIL_PATHS) entry(path)?.let { return it }

    val plate = byLowerName.keys
        .mapNotNull { name -> PLATE_IMAGE.matchEntire(name)?.let { it.groupValues[1].toIntOrNull()?.to(name) } }
        .minByOrNull { it.first }
    if (plate != null) return byLowerName.getValue(plate.second)
    // Miniatura da OrcaSlicer com outro nome: a pequena só se não houver outra.
    return byLowerName.keys.filter { ORCA_THUMBNAIL.matches(it) }
        .sortedWith(compareBy<String> { it.endsWith("_small.png") }.thenBy { it })
        .firstOrNull()
        ?.let(byLowerName::getValue)
}

/** Os alvos das relações de miniatura, na ordem do arquivo, sem a barra inicial e com `%20` e afins decodificados. */
private fun thumbnailTargets(relationships: String?): List<String> {
    if (relationships == null) return emptyList()
    return RELATIONSHIP_ELEMENT.findAll(relationships).mapNotNull { element ->
        val type = attribute(element.value, "Type") ?: return@mapNotNull null
        if (!type.equals(THUMBNAIL_RELATIONSHIP_TYPE, ignoreCase = true)) return@mapNotNull null
        attribute(element.value, "Target")?.let(::percentDecoded)?.removePrefix("/")
    }.toList()
}

private fun attribute(element: String, name: String): String? =
    Regex("""\b$name\s*=\s*(["'])(.*?)\1""").find(element)?.groupValues?.get(2)

private fun percentDecoded(text: String): String {
    if ('%' !in text) return text
    val bytes = ArrayList<Byte>()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val hex = if (c == '%' && i + 2 < text.length) text.substring(i + 1, i + 3).toIntOrNull(16) else null
        if (hex != null) {
            bytes += hex.toByte()
            i += 3
        } else {
            bytes += c.toString().encodeToByteArray().toList()
            i++
        }
    }
    return bytes.toByteArray().decodeToString()
}
