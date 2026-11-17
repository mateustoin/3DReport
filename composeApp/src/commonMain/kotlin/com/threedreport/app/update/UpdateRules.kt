package com.threedreport.app.update

/**
 * Um arquivo publicado numa release (decisão 143): o nome, o endereço pra baixar (`browser_download_url` da API do
 * GitHub) e o tamanho em bytes.
 */
data class ReleaseAsset(val name: String, val downloadUrl: String, val sizeBytes: Long)

/** Sistema em que o app está rodando, pra escolher o instalador. */
enum class HostOs { WINDOWS, LINUX, MACOS, OTHER }

/** Arquitetura do processador, pra escolher o instalador. */
enum class HostArch { X64, ARM64, OTHER }

/** O sistema a partir do `os.name` da JVM. */
fun hostOsOf(osName: String): HostOs {
    val name = osName.lowercase()
    return when {
        name.startsWith("windows") -> HostOs.WINDOWS
        name.startsWith("linux") -> HostOs.LINUX
        name.startsWith("mac") || name.startsWith("darwin") -> HostOs.MACOS
        else -> HostOs.OTHER
    }
}

/** A arquitetura a partir do `os.arch` da JVM. */
fun hostArchOf(osArch: String): HostArch = when (osArch.lowercase()) {
    "amd64", "x86_64", "x64" -> HostArch.X64
    "aarch64", "arm64" -> HostArch.ARM64
    else -> HostArch.OTHER
}

/** Nome do arquivo de somas SHA-256 que a release publica junto com os instaladores (formato do `sha256sum`). */
const val SHA256_SUMS_ASSET_NAME = "SHA256SUMS.txt"

/**
 * O instalador de [assets] pro sistema em uso, ou `null` se a release não tem um (aí não há botão "Atualizar agora"):
 * `.msi` no Windows (64 bits), `.deb` da arquitetura no Linux (o `_amd64` ou `_arm64` que o jpackage põe no nome) e
 * `.dmg` com `-apple-silicon` ou `-intel` no macOS (sufixo do workflow da release).
 */
fun selectInstallerAsset(assets: List<ReleaseAsset>, os: HostOs, arch: HostArch): ReleaseAsset? {
    if (arch == HostArch.OTHER) return null
    fun named(predicate: (String) -> Boolean) = assets.firstOrNull { predicate(it.name.lowercase()) }
    return when (os) {
        HostOs.WINDOWS -> if (arch == HostArch.X64) named { it.endsWith(".msi") } else null

        HostOs.LINUX -> {
            val token = if (arch == HostArch.X64) "_amd64.deb" else "_arm64.deb"
            named { it.endsWith(token) }
        }

        HostOs.MACOS -> {
            val suffix = if (arch == HostArch.ARM64) "-apple-silicon.dmg" else "-intel.dmg"
            named { it.endsWith(suffix) }
        }

        HostOs.OTHER -> null
    }
}

/** O [SHA256_SUMS_ASSET_NAME] da release, ou `null` (releases antigas não têm). */
fun checksumsAsset(assets: List<ReleaseAsset>): ReleaseAsset? = assets.firstOrNull { it.name == SHA256_SUMS_ASSET_NAME }

private val SUM_LINE = Regex("""^([0-9a-fA-F]{64}) [ *](.+)$""")

/**
 * Lê um arquivo no formato do `sha256sum` (`<soma>  <arquivo>`, ou `<soma> *<arquivo>` no modo binário). Devolve o
 * nome do arquivo (sem pasta) e a soma em minúsculas. Linha que não é soma é ignorada.
 */
fun parseSha256Sums(text: String): Map<String, String> = text.lineSequence()
    .mapNotNull { SUM_LINE.matchEntire(it.trim('\r', ' ', '\t'))?.groupValues }
    .associate { (_, sum, path) -> path.trim().substringAfterLast('/').substringAfterLast('\\') to sum.lowercase() }

/** Resultado de conferir a soma de um instalador baixado. */
enum class ChecksumCheck {
    MATCH,

    /** A soma é outra: arquivo corrompido ou trocado. Não instala. */
    MISMATCH,

    /** O arquivo de somas não lista esse instalador. Não instala. */
    MISSING,
}

/** Confere a soma SHA-256 [actualSha256Hex] do arquivo [fileName] contra [sums] (de [parseSha256Sums]). */
fun checkChecksum(sums: Map<String, String>, fileName: String, actualSha256Hex: String): ChecksumCheck {
    val expected = sums[fileName] ?: return ChecksumCheck.MISSING
    return if (expected.equals(actualSha256Hex, ignoreCase = true)) ChecksumCheck.MATCH else ChecksumCheck.MISMATCH
}
