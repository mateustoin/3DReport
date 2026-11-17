package com.threedreport.core.stl

/**
 * Recebe os triângulos de um STL, um por vez, como nove números (os três vértices), sem criar objeto por
 * triângulo. A normal gravada no arquivo fica de fora: nem a análise nem o visualizador usam (ver
 * [StlTriangle.computedNormal]).
 */
fun interface StlTriangleVisitor {
    fun triangle(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float)
}

/**
 * Progresso de um trabalho em cima do STL, de 0 a 1, nunca voltando. É chamado de tempos em tempos (não a cada
 * triângulo), por isso também é o ponto de cancelar: lançar uma exceção daqui (no app, o `ensureActive()` da
 * corrotina) interrompe o trabalho, e a exceção sai pela função que foi chamada.
 */
typealias StlProgress = (fraction: Float) -> Unit

/**
 * Lê um arquivo STL (binário ou ASCII) e devolve a malha de triângulos.
 *
 * Detecção de formato: um STL ASCII começa com "solid", mas alguns
 * exportadores gravam esse texto também no cabeçalho de um STL **binário**
 * (bug conhecido de alguns fatiadores/CAD) — por isso, mesmo quando o
 * arquivo começa com "solid", confere se o tamanho bate com a fórmula do
 * formato binário (`84 + contagem × 50` bytes) antes de decidir.
 *
 * Monta um [StlTriangle] por triângulo: pra arquivo de milhões de triângulos, use [forEachStlTriangle],
 * [analyzeStl] e [simplifyStl], que não guardam a malha inteira.
 *
 * @throws IllegalArgumentException se o arquivo binário estiver truncado
 *   (menor do que a contagem de triângulos declarada permite).
 */
fun parseStl(bytes: ByteArray): StlMesh = parseStl(bytes, plausibleStlTriangleCount(bytes), null)

/** [parseStl] com a contagem já feita ([plausibleStlTriangleCount]) e progresso, que é o ponto de cancelar. */
internal fun parseStl(bytes: ByteArray, triangleCount: Long, onProgress: StlProgress?): StlMesh {
    val triangles = ArrayList<StlTriangle>(preallocationHint(triangleCount))
    walkStl(bytes, onProgress) { nx, ny, nz, x1, y1, z1, x2, y2, z2, x3, y3, z3 ->
        triangles += StlTriangle(Vec3(nx, ny, nz), Vec3(x1, y1, z1), Vec3(x2, y2, z2), Vec3(x3, y3, z3))
    }
    return StlMesh(triangles)
}

/**
 * Quantos triângulos o arquivo pode ter de verdade: no binário, a contagem do cabeçalho limitada ao que cabe nos
 * bytes (um cabeçalho mentiroso ou um arquivo cortado não reserva memória pra triângulo que não existe; o corte é
 * acusado depois, na leitura); no ASCII, os `endfacet`. É a conta que o resto do pacote faz uma vez só e passa
 * adiante, porque no ASCII ela varre o arquivo inteiro.
 */
internal fun plausibleStlTriangleCount(bytes: ByteArray): Long = if (isBinaryStl(bytes)) {
    if (bytes.size >= 84) minOf(readUInt32LE(bytes, 80), (bytes.size - 84L) / 50L) else 0L
} else {
    countAsciiFacets(bytes)
}

/** Maior reserva antecipada, em triângulos. Acima disso as listas crescem conforme a leitura. */
private const val MAX_PREALLOCATED_TRIANGLES = 16_000_000L

/** Capacidade inicial pra [triangleCount] triângulos, sem passar de [MAX_PREALLOCATED_TRIANGLES]. */
internal fun preallocationHint(triangleCount: Long): Int = triangleCount.coerceIn(0L, MAX_PREALLOCATED_TRIANGLES).toInt()

/**
 * Percorre os triângulos de um STL (binário ou ASCII, mesma detecção de [parseStl]) na ordem do arquivo, sem
 * montar a malha. O STL ASCII é lido linha a linha, sem converter o arquivo inteiro em texto.
 *
 * @param onProgress ver [StlProgress]; termina sempre em 1.
 * @throws IllegalArgumentException se o arquivo binário estiver truncado.
 */
fun forEachStlTriangle(bytes: ByteArray, onProgress: StlProgress? = null, visitor: StlTriangleVisitor) {
    walkStl(bytes, onProgress) { _, _, _, x1, y1, z1, x2, y2, z2, x3, y3, z3 -> visitor.triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3) }
}

/** [StlTriangleVisitor] com a normal gravada no arquivo, só pro [parseStl], que guarda ela no [StlTriangle]. */
private fun interface NormalTriangleVisitor {
    fun triangle(nx: Float, ny: Float, nz: Float, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float)
}

private fun walkStl(bytes: ByteArray, onProgress: StlProgress?, visitor: NormalTriangleVisitor) {
    if (isBinaryStl(bytes)) walkBinary(bytes, onProgress, visitor) else walkAscii(bytes, onProgress, visitor)
    onProgress?.invoke(1f)
}

/**
 * Conta os triângulos de um STL **sem montar a malha inteira** — só lê os 4 bytes da contagem no
 * caso binário, ou conta ocorrências de `endfacet` no caso ASCII (sem converter o arquivo em texto).
 * Pensado pra decidir **antes** de ler a malha se vale a pena desenhar o modelo inteiro.
 */
fun peekStlTriangleCount(bytes: ByteArray): Long = if (isBinaryStl(bytes)) {
    if (bytes.size >= 84) readUInt32LE(bytes, 80) else 0L
} else {
    countAsciiFacets(bytes)
}

private val ENDFACET = "endfacet".encodeToByteArray()

private fun countAsciiFacets(bytes: ByteArray): Long {
    var count = 0L
    var i = 0
    val last = bytes.size - ENDFACET.size
    while (i <= last) {
        var match = true
        for (k in ENDFACET.indices) {
            val b = bytes[i + k].toInt()
            val lower = if (b in 'A'.code..'Z'.code) b + 32 else b
            if (lower != ENDFACET[k].toInt()) {
                match = false
                break
            }
        }
        if (match) {
            count++
            i += ENDFACET.size
        } else {
            i++
        }
    }
    return count
}

private fun isBinaryStl(bytes: ByteArray): Boolean {
    val looksAscii = bytes.size >= 5 && bytes.decodeToString(0, minOf(bytes.size, 5)).trim().startsWith("solid", ignoreCase = true)
    return if (!looksAscii) {
        true
    } else if (bytes.size < 84) {
        false
    } else {
        val triangleCount = readUInt32LE(bytes, 80)
        84L + triangleCount * 50L == bytes.size.toLong()
    }
}

/** De quantos em quantos triângulos o progresso é avisado. */
internal const val PROGRESS_STEP = 16_384

private fun walkBinary(bytes: ByteArray, onProgress: StlProgress?, visitor: NormalTriangleVisitor) {
    require(bytes.size >= 84) { "Arquivo STL binário inválido: menor que o cabeçalho mínimo (84 bytes)." }
    val triangleCount = readUInt32LE(bytes, 80)
    val expectedSize = 84L + triangleCount * 50L
    require(bytes.size.toLong() >= expectedSize) {
        "Arquivo STL binário truncado: esperava $expectedSize bytes pra $triangleCount triângulos, mas o arquivo tem ${bytes.size}."
    }
    val count = triangleCount.toInt()
    var offset = 84
    for (index in 0 until count) {
        if (onProgress != null && index % PROGRESS_STEP == 0) onProgress(index.toFloat() / count)
        visitor.triangle(
            readFloatLE(bytes, offset), readFloatLE(bytes, offset + 4), readFloatLE(bytes, offset + 8),
            readFloatLE(bytes, offset + 12), readFloatLE(bytes, offset + 16), readFloatLE(bytes, offset + 20),
            readFloatLE(bytes, offset + 24), readFloatLE(bytes, offset + 28), readFloatLE(bytes, offset + 32),
            readFloatLE(bytes, offset + 36), readFloatLE(bytes, offset + 40), readFloatLE(bytes, offset + 44),
        )
        offset += 50
    }
}

private val asciiFloatRegex = Regex("""-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?""")

/**
 * Mesma regra da leitura ASCII de sempre (linha que começa com `facet`, `vertex` ou `endfacet`), só que linha a
 * linha: só vira texto a linha que pode ser uma dessas três, o resto (`outer loop`, `endloop`, `solid`) é pulado
 * pelo primeiro byte.
 */
private fun walkAscii(bytes: ByteArray, onProgress: StlProgress?, visitor: NormalTriangleVisitor) {
    val vertices = FloatArray(9)
    var nx = 0f
    var ny = 0f
    var nz = 0f
    var vertexCount = 0
    var lineStart = 0
    var lines = 0
    val size = bytes.size
    while (lineStart < size) {
        var lineEnd = lineStart
        while (lineEnd < size && bytes[lineEnd] != '\n'.code.toByte() && bytes[lineEnd] != '\r'.code.toByte()) lineEnd++
        if (onProgress != null && ++lines % PROGRESS_STEP == 0) onProgress(lineStart.toFloat() / size)

        if (mayBeFacetLine(bytes, lineStart, lineEnd)) {
            val line = bytes.decodeToString(lineStart, lineEnd).trim()
            when {
                line.startsWith("facet", ignoreCase = true) -> {
                    val numbers = asciiFloatRegex.findAll(line).map { it.value.toFloat() }.toList()
                    val hasNormal = numbers.size >= 3
                    nx = if (hasNormal) numbers[0] else 0f
                    ny = if (hasNormal) numbers[1] else 0f
                    nz = if (hasNormal) numbers[2] else 0f
                    vertexCount = 0
                }

                line.startsWith("vertex", ignoreCase = true) -> {
                    val numbers = asciiFloatRegex.findAll(line).map { it.value.toFloat() }.toList()
                    if (numbers.size >= 3) {
                        if (vertexCount < 3) {
                            vertices[vertexCount * 3] = numbers[0]
                            vertices[vertexCount * 3 + 1] = numbers[1]
                            vertices[vertexCount * 3 + 2] = numbers[2]
                        }
                        vertexCount++
                    }
                }

                line.startsWith("endfacet", ignoreCase = true) -> {
                    if (vertexCount == 3) {
                        visitor.triangle(
                            nx, ny, nz,
                            vertices[0], vertices[1], vertices[2],
                            vertices[3], vertices[4], vertices[5],
                            vertices[6], vertices[7], vertices[8],
                        )
                    }
                    vertexCount = 0
                }
            }
        }
        lineStart = lineEnd + 1
    }
}

/** Se a linha pode começar com `facet`, `vertex` ou `endfacet`: o primeiro byte depois de espaço não é outra letra. */
private fun mayBeFacetLine(bytes: ByteArray, start: Int, end: Int): Boolean {
    var i = start
    while (i < end && (bytes[i] == ' '.code.toByte() || bytes[i] == '\t'.code.toByte())) i++
    if (i == end) return false
    val first = bytes[i].toInt()
    val isLetter = first in 'a'.code..'z'.code || first in 'A'.code..'Z'.code
    if (!isLetter) return true
    val lower = first or 0x20
    return lower == 'f'.code || lower == 'v'.code || lower == 'e'.code
}

private fun readUInt32LE(bytes: ByteArray, offset: Int): Long =
    (bytes[offset].toLong() and 0xFF) or
        ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
        ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
        ((bytes[offset + 3].toLong() and 0xFF) shl 24)

private fun readFloatLE(bytes: ByteArray, offset: Int): Float = Float.fromBits(
    (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24),
)
