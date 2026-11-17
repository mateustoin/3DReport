package com.threedreport.app.update

import com.sun.net.httpserver.HttpServer
import com.threedreport.app.ui.about.LatestRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopUpdateInstallerTest {

    private lateinit var server: HttpServer
    private lateinit var downloads: File
    private val files = HashMap<String, ByteArray>()
    private val installer = ByteArray(300_000) { (it % 251).toByte() }
    private val debName = "threedreport_2.3.0-1_amd64.deb"

    /** Comandos que o instalador pediu pra rodar, com o código de saída que o teste quer devolver. */
    private val commands = ArrayList<List<String>>()
    private var exitCode = 0
    private val runner = object : CommandRunner {
        override fun runAndWait(command: List<String>): Int {
            commands += command
            return exitCode
        }

        override fun startDetached(command: List<String>) {
            commands += command
        }
    }
    private val openedFolders = ArrayList<String>()

    @BeforeTest
    fun start() {
        downloads = createTempDirectory("3dreport-update-test").toFile()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = files[exchange.requestURI.path.removePrefix("/")]
            if (body == null) {
                exchange.sendResponseHeaders(404, -1)
            } else {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            exchange.close()
        }
        server.start()
    }

    @AfterTest
    fun stop() {
        server.stop(0)
        downloads.deleteRecursively()
    }

    private fun url(name: String) = "http://127.0.0.1:${server.address.port}/$name"

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun release(withSums: Boolean = true) = LatestRelease(
        version = "2.3.0",
        url = "https://github.com/o/r/releases/tag/v2.3.0",
        assets = listOfNotNull(
            ReleaseAsset(debName, url(debName), installer.size.toLong()),
            ReleaseAsset("3DReport-2.3.0.msi", url("3DReport-2.3.0.msi"), 10),
            if (withSums) ReleaseAsset("SHA256SUMS.txt", url("SHA256SUMS.txt"), 200) else null,
        ),
    )

    private fun desktop(
        os: HostOs = HostOs.LINUX,
        appPath: String? = "/opt/threedreport/bin/3DReport",
        commandsOnPath: Set<String> = setOf("pkexec", "apt-get"),
    ) = DesktopUpdateInstaller(
        appVersion = "2.2.0",
        os = os,
        arch = HostArch.X64,
        appPath = appPath,
        downloadDir = { downloads },
        commandExists = { it in commandsOnPath },
        runner = runner,
        openFolder = { openedFolders += it },
        currentPid = 4242,
    )

    @Test
    fun noButtonWhenRunningOutsideAnInstaller() {
        assertNull(desktop(appPath = null).plan(release()), "rodando pelo Gradle")
        assertNull(desktop(appPath = "/home/eu/3DReport/bin/3DReport").plan(release()), "Linux fora do .deb")
        assertNull(desktop(os = HostOs.MACOS, appPath = "/Users/eu/3DReport/bin/3DReport").plan(release()), "Mac fora do .app")
    }

    @Test
    fun planPicksTheInstallerAndTheWayToInstall() {
        assertEquals(InstallMethod.LINUX_DEB, desktop().plan(release())?.method)
        assertEquals(debName, desktop().plan(release())?.asset?.name)
        assertEquals(InstallMethod.LINUX_DEB_MANUAL, desktop(commandsOnPath = setOf("apt-get")).plan(release())?.method)
        assertEquals(InstallMethod.WINDOWS_MSI, desktop(os = HostOs.WINDOWS, appPath = "C:\\Program Files\\3DReport\\3DReport.exe").plan(release())?.method)
        assertNull(desktop(os = HostOs.MACOS, appPath = "/Applications/3DReport.app/Contents/MacOS/3DReport").plan(release()), "release sem .dmg")
    }

    @Test
    fun downloadsChecksAndKeepsTheInstallerInTheTemporaryFolder() = runBlocking {
        files[debName] = installer
        files["SHA256SUMS.txt"] = "${sha256(installer)}  $debName\n".encodeToByteArray()
        val progress = ArrayList<Float>()

        val outcome = desktop().download(desktop().plan(release())!!) { progress += it }

        assertIs<DownloadOutcome.Ready>(outcome)
        val file = File(outcome.update.filePath)
        assertEquals(downloads, file.parentFile)
        assertContentEquals(installer, file.readBytes())
        assertEquals(1f, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun wrongChecksumDeletesTheFileAndDoesNotInstall() = runBlocking {
        files[debName] = installer
        files["SHA256SUMS.txt"] = "${"0".repeat(64)}  $debName\n".encodeToByteArray()

        val outcome = desktop().download(desktop().plan(release())!!) {}

        assertEquals(DownloadOutcome.Failed(UpdateFailure.CHECKSUM_MISMATCH), outcome)
        assertTrue(downloads.listFiles().orEmpty().isEmpty(), "o arquivo baixado sai")
    }

    @Test
    fun releaseWithoutChecksumsDoesNotEvenDownload() = runBlocking {
        files[debName] = installer
        assertEquals(DownloadOutcome.Failed(UpdateFailure.NO_CHECKSUMS), desktop().download(desktop().plan(release(withSums = false))!!) {})

        files["SHA256SUMS.txt"] = "${sha256(installer)}  outro-arquivo.deb\n".encodeToByteArray()
        assertEquals(DownloadOutcome.Failed(UpdateFailure.NO_CHECKSUMS), desktop().download(desktop().plan(release())!!) {})
        assertTrue(downloads.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun networkFailureIsReportedAsSuch() = runBlocking {
        files["SHA256SUMS.txt"] = "${sha256(installer)}  $debName\n".encodeToByteArray()
        // O instalador some do servidor: 404.
        assertEquals(DownloadOutcome.Failed(UpdateFailure.NETWORK), desktop().download(desktop().plan(release())!!) {})
        assertTrue(downloads.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun cancellingDeletesThePartialFile() {
        files[debName] = installer
        files["SHA256SUMS.txt"] = "${sha256(installer)}  $debName\n".encodeToByteArray()

        assertFailsWith<CancellationException> {
            runBlocking { desktop().download(desktop().plan(release())!!) { if (it > 0f) throw CancellationException("trocou de ideia") } }
        }
        assertTrue(downloads.listFiles().orEmpty().isEmpty())
    }

    private fun downloaded(method: InstallMethod, name: String = debName): DownloadedUpdate {
        val file = File(downloads, name).apply { writeBytes(installer) }
        return DownloadedUpdate(UpdatePlan(release(), ReleaseAsset(name, url(name), 1), method), file.path)
    }

    @Test
    fun linuxInstallsWithPkexecAndAptThenReopensAfterThisAppCloses() = runBlocking {
        val update = downloaded(InstallMethod.LINUX_DEB)

        assertEquals(InstallOutcome.CloseApp, desktop().install(update))
        assertEquals(listOf("pkexec", "apt-get", "install", "-y", update.filePath), commands[0])
        // Reabre só depois que este processo sair: a trava de instância única ainda é dele.
        val relaunch = commands[1]
        assertEquals("sh", relaunch[0])
        assertTrue(relaunch.contains("4242") && relaunch.contains("/opt/threedreport/bin/3DReport"), relaunch.toString())
    }

    @Test
    fun linuxWithoutAptUsesDpkg() = runBlocking {
        val update = downloaded(InstallMethod.LINUX_DEB)
        desktop(commandsOnPath = setOf("pkexec")).install(update)
        assertEquals(listOf("pkexec", "dpkg", "-i", update.filePath), commands[0])
    }

    @Test
    fun cancelledPasswordOrFailureChangesNothing() = runBlocking {
        exitCode = 126
        assertEquals(InstallOutcome.Cancelled, desktop().install(downloaded(InstallMethod.LINUX_DEB)))
        exitCode = 100
        assertEquals(InstallOutcome.Failed, desktop().install(downloaded(InstallMethod.LINUX_DEB)))
        assertEquals(2, commands.size, "sem reabrir o app")
    }

    @Test
    fun linuxWithoutPkexecOpensTheFolderAndGivesTheCommand() = runBlocking {
        val update = downloaded(InstallMethod.LINUX_DEB_MANUAL)

        val outcome = desktop(commandsOnPath = setOf("apt-get")).install(update)

        assertIs<InstallOutcome.Manual>(outcome)
        assertEquals(downloads.path, outcome.folderPath)
        assertEquals("sudo apt install '${update.filePath}'", outcome.command)
        assertEquals(listOf(downloads.path), openedFolders)
        assertTrue(commands.isEmpty())
    }

    @Test
    fun windowsAndMacOpenTheInstallerDetached() = runBlocking {
        val msi = downloaded(InstallMethod.WINDOWS_MSI, "3DReport-2.3.0.msi")
        assertEquals(InstallOutcome.CloseApp, desktop(os = HostOs.WINDOWS).install(msi))
        assertEquals(listOf("msiexec", "/i", msi.filePath), commands.last())

        val dmg = downloaded(InstallMethod.MACOS_DMG, "3DReport-2.3.0-intel.dmg")
        assertEquals(InstallOutcome.CloseApp, desktop(os = HostOs.MACOS).install(dmg))
        assertEquals(listOf("open", dmg.filePath), commands.last())
    }

    @Test
    fun discardDeletesTheDownloadedFile() {
        val update = downloaded(InstallMethod.LINUX_DEB)
        desktop().discard(update)
        assertFalse(File(update.filePath).exists())
    }

    @Test
    fun assetNameFromTheNetworkCannotLeaveTheTemporaryFolder() {
        assertEquals(".._.._etc_passwd", safeDownloadName("../../etc/passwd"))
        assertEquals("update", safeDownloadName(".."))
        assertEquals(debName, safeDownloadName(debName))
    }
}
