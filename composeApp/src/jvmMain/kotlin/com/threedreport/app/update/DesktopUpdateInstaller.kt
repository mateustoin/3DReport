package com.threedreport.app.update

import com.threedreport.app.AppLog
import com.threedreport.app.ui.about.LatestRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Duration

/** Como terminou um comando: o código de saída e o começo da saída de erro (pra reconhecer a causa, não pra mostrar). */
data class CommandResult(val exitCode: Int, val errorOutput: String = "")

/** Roda os comandos do sistema. Interface pra os testes não instalarem nada de verdade. */
interface CommandRunner {
    /** Roda [command] e espera terminar. */
    fun runAndWait(command: List<String>): CommandResult

    /** Inicia [command] sem esperar e sem prender a entrada e a saída a este processo, pra ele seguir depois que o app fechar. */
    fun startDetached(command: List<String>)
}

/** [CommandRunner] de verdade, com [ProcessBuilder]. */
object SystemCommandRunner : CommandRunner {
    override fun runAndWait(command: List<String>): CommandResult {
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        // Lida até o fim (o processo não trava com o cano cheio), guardando só o começo.
        val error = process.errorStream.use { stream ->
            val kept = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (kept.size() < MAX_ERROR_OUTPUT) kept.write(buffer, 0, minOf(n, MAX_ERROR_OUTPUT - kept.size()))
            }
            kept.toString(Charsets.UTF_8)
        }
        return CommandResult(process.waitFor(), error)
    }

    private const val MAX_ERROR_OUTPUT = 16 * 1024

    override fun startDetached(command: List<String>) {
        ProcessBuilder(command)
            .redirectInput(ProcessBuilder.Redirect.from(File(if (File("/dev/null").exists()) "/dev/null" else "NUL")))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }
}

/**
 * Atualização por dentro no desktop (decisão 143). Só conecta no [download], que a pessoa pede; nada do usuário vai
 * no pedido além do `User-Agent` de sempre. O arquivo vai pra uma pasta temporária do sistema, nunca pra pasta de
 * dados.
 *
 * @param appPath o executável instalado (`jpackage.app-path`, que só o lançador do instalador define). `null` é o
 *   app rodando pelo Gradle: sem botão.
 * @param downloadDir pasta onde o instalador é baixado (criada a cada download).
 */
class DesktopUpdateInstaller(
    private val appVersion: String,
    private val os: HostOs = hostOsOf(System.getProperty("os.name").orEmpty()),
    private val arch: HostArch = hostArchOf(System.getProperty("os.arch").orEmpty()),
    private val appPath: String? = System.getProperty("jpackage.app-path"),
    private val downloadDir: () -> File = { Files.createTempDirectory(UPDATE_DOWNLOAD_PREFIX).toFile() },
    private val commandExists: (String) -> Boolean = ::isOnPath,
    private val runner: CommandRunner = SystemCommandRunner,
    private val openFolder: (String) -> Unit = { com.threedreport.app.platform.openFolder(it) },
    private val currentPid: Long = ProcessHandle.current().pid(),
) : UpdateInstaller {

    // Só nasce no primeiro download: sem a pessoa pedir, nenhuma conexão.
    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // O browser_download_url do GitHub redireciona pro armazenamento dos arquivos.
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    override fun plan(release: LatestRelease): UpdatePlan? {
        if (!isInstalledApp()) return null
        val asset = selectInstallerAsset(release.assets, os, arch) ?: return null
        val method = when (os) {
            HostOs.WINDOWS -> InstallMethod.WINDOWS_MSI
            HostOs.LINUX -> if (commandExists("pkexec")) InstallMethod.LINUX_DEB else InstallMethod.LINUX_DEB_MANUAL
            HostOs.MACOS -> InstallMethod.MACOS_DMG
            HostOs.OTHER -> return null
        }
        return UpdatePlan(release, asset, method)
    }

    /**
     * Se o app veio de um instalador: o lançador do jpackage define o `jpackage.app-path`. No Linux, só o `.deb`
     * (em `/opt`) troca de versão com outro `.deb`; no Mac, só o app dentro de um `.app`.
     */
    private fun isInstalledApp(): Boolean {
        val path = appPath ?: return false
        return when (os) {
            HostOs.LINUX -> path.startsWith("/opt/")
            HostOs.MACOS -> ".app/" in path
            HostOs.WINDOWS -> true
            HostOs.OTHER -> false
        }
    }

    override suspend fun download(plan: UpdatePlan, onProgress: (Float) -> Unit): DownloadOutcome = withContext(Dispatchers.IO) {
        val sumsAsset = checksumsAsset(plan.release.assets) ?: return@withContext DownloadOutcome.Failed(UpdateFailure.NO_CHECKSUMS)
        val sums = try {
            val response = client.send(request(sumsAsset.downloadUrl), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) return@withContext DownloadOutcome.Failed(UpdateFailure.NETWORK)
            parseSha256Sums(response.body())
        } catch (e: IOException) {
            AppLog.warn("Não consegui baixar a lista de somas da atualização", e)
            return@withContext DownloadOutcome.Failed(UpdateFailure.NETWORK)
        }
        if (plan.asset.name !in sums) return@withContext DownloadOutcome.Failed(UpdateFailure.NO_CHECKSUMS)

        val dir = downloadDir().apply { mkdirs() }
        val file = File(dir, safeDownloadName(plan.asset.name))
        var kept = false
        try {
            val response = client.send(request(plan.asset.downloadUrl), HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() != 200) {
                response.body().close()
                return@withContext DownloadOutcome.Failed(UpdateFailure.NETWORK)
            }
            val total = response.headers().firstValueAsLong("Content-Length").orElse(plan.asset.sizeBytes).coerceAtLeast(1L)
            val digest = MessageDigest.getInstance("SHA-256")
            onProgress(0f)
            response.body().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        onProgress((done.toDouble() / total).toFloat().coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            when (checkChecksum(sums, plan.asset.name, actual)) {
                ChecksumCheck.MATCH -> {
                    kept = true
                    DownloadOutcome.Ready(DownloadedUpdate(plan, file.path))
                }

                ChecksumCheck.MISMATCH -> {
                    AppLog.warn("A soma do instalador baixado não bate com a publicada; arquivo apagado")
                    DownloadOutcome.Failed(UpdateFailure.CHECKSUM_MISMATCH)
                }

                ChecksumCheck.MISSING -> DownloadOutcome.Failed(UpdateFailure.NO_CHECKSUMS)
            }
        } catch (e: IOException) {
            AppLog.warn("Não consegui baixar a atualização", e)
            DownloadOutcome.Failed(UpdateFailure.NETWORK)
        } finally {
            // Falhou, a soma não bateu ou a pessoa cancelou: nada fica pra trás.
            if (!kept) {
                file.delete()
                dir.delete()
            }
        }
    }

    private fun request(url: String): HttpRequest = HttpRequest.newBuilder(URI(url))
        .timeout(Duration.ofSeconds(30))
        .header("User-Agent", "3DReport/$appVersion")
        .GET()
        .build()

    override suspend fun install(update: DownloadedUpdate): InstallOutcome = withContext(Dispatchers.IO) {
        val path = update.filePath
        try {
            when (update.plan.method) {
                InstallMethod.WINDOWS_MSI -> {
                    runner.startDetached(listOf("msiexec", "/i", path))
                    InstallOutcome.CloseApp
                }

                InstallMethod.MACOS_DMG -> {
                    runner.startDetached(listOf("open", path))
                    InstallOutcome.CloseApp
                }

                InstallMethod.LINUX_DEB -> installDeb(path)

                InstallMethod.LINUX_DEB_MANUAL -> manualInstall(path)
            }
        } catch (e: IOException) {
            AppLog.warn("Não consegui abrir o instalador da atualização", e)
            InstallOutcome.Failed
        }
    }

    /** Sem como pedir a senha: abre a pasta do arquivo e devolve o comando de terminal que instala. */
    private fun manualInstall(path: String): InstallOutcome {
        val folder = File(path).parent
        openFolder(folder)
        return InstallOutcome.Manual(folderPath = folder, command = "sudo apt install '${path.replace("'", "'\\''")}'")
    }

    private fun installDeb(path: String): InstallOutcome {
        val command = if (commandExists("apt-get")) listOf("pkexec", "apt-get", "install", "-y", path) else listOf("pkexec", "dpkg", "-i", path)
        val result = runner.runAndWait(command)
        return when {
            result.exitCode == 0 -> {
                relaunchAfterExit()
                InstallOutcome.CloseApp
            }

            // A pessoa fechou a janela de senha.
            result.exitCode == PKEXEC_DISMISSED -> InstallOutcome.Cancelled

            // Sessão sem agente do polkit: não há janela de senha, e tentar de novo não adianta. O 127 sem essa
            // mensagem é "não autorizado" (senha errada demais): falha, nada mudou.
            result.exitCode == PKEXEC_NOT_AUTHORIZED && result.errorOutput.contains("authentication agent", ignoreCase = true) -> {
                AppLog.warn("Sem agente de autenticação do polkit; instalação pelo terminal")
                manualInstall(path)
            }

            else -> {
                AppLog.warn("O instalador do .deb terminou com código ${result.exitCode}: ${result.errorOutput.take(500)}")
                InstallOutcome.Failed
            }
        }
    }

    /**
     * Abre a versão nova assim que este processo sair. Não dá pra abrir antes: a trava de instância única ainda é
     * deste app, e a nova fecharia dizendo que já está aberto. O `tail --pid` (coreutils) só espera o processo
     * terminar.
     */
    private fun relaunchAfterExit() {
        val executable = appPath ?: ProcessHandle.current().info().command().orElse(null) ?: return
        runner.startDetached(listOf("sh", "-c", "tail --pid=\"$1\" -f /dev/null; exec \"$2\"", "3dreport-relaunch", currentPid.toString(), executable))
    }

    override fun discard(update: DownloadedUpdate) {
        val file = File(update.filePath)
        file.delete()
        file.parentFile?.delete()
    }

    private companion object {
        const val PKEXEC_DISMISSED = 126
        const val PKEXEC_NOT_AUTHORIZED = 127
    }
}

/** Nome do arquivo baixado a partir do nome do asset (que vem da rede): sem pasta, só letras, números, `.`, `_` e `-`. */
internal fun safeDownloadName(assetName: String): String {
    val cleaned = assetName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (cleaned.isBlank() || cleaned.all { it == '.' }) "update" else cleaned
}

/** Se [command] está em alguma pasta do `PATH`. */
private fun isOnPath(command: String): Boolean = System.getenv("PATH").orEmpty()
    .split(File.pathSeparatorChar)
    .any { dir -> dir.isNotBlank() && File(dir, command).let { it.isFile && it.canExecute() } }

/** Prefixo das pastas de download da atualização na pasta temporária do sistema (ver [DesktopUpdateInstaller]). */
internal const val UPDATE_DOWNLOAD_PREFIX = "3dreport-update"

/** Idade mínima pra apagar: um instalador recém entregue ao sistema (o `msiexec` esperando a confirmação) ainda usa o arquivo. */
private const val UPDATE_DOWNLOAD_MIN_AGE_MILLIS = 60 * 60 * 1000L

/**
 * Apaga as pastas de download de atualizações antigas ([UPDATE_DOWNLOAD_PREFIX]) da pasta temporária do sistema.
 * O instalador entregue ao sistema não pode ser apagado na hora (o app fecha logo depois), então o app chama isto
 * ao abrir. Pasta com menos de uma hora fica pra próxima. Devolve quantas pastas saíram.
 */
fun deleteOldUpdateDownloads(
    tempDir: File = File(System.getProperty("java.io.tmpdir")),
    nowMillis: Long = System.currentTimeMillis(),
): Int {
    val old = tempDir.listFiles { file ->
        file.isDirectory && file.name.startsWith(UPDATE_DOWNLOAD_PREFIX) && nowMillis - file.lastModified() >= UPDATE_DOWNLOAD_MIN_AGE_MILLIS
    }.orEmpty()
    val deleted = old.count { it.deleteRecursively() }
    if (deleted > 0) AppLog.info("Limpeza de downloads de atualização: $deleted pasta(s) removida(s)")
    return deleted
}
