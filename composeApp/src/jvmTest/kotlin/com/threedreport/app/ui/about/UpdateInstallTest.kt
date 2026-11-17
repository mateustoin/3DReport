package com.threedreport.app.ui.about

import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.update.DownloadOutcome
import com.threedreport.app.update.DownloadedUpdate
import com.threedreport.app.update.InstallMethod
import com.threedreport.app.update.InstallOutcome
import com.threedreport.app.update.ReleaseAsset
import com.threedreport.app.update.UpdateFailure
import com.threedreport.app.update.UpdateInstaller
import com.threedreport.app.update.UpdatePlan
import kotlinx.coroutines.CompletableDeferred
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O "Atualizar agora" (decisão 143): cada passo da janela, com um instalador falso que não baixa nem instala nada. */
class UpdateInstallTest {

    private val asset = ReleaseAsset("3DReport-2.3.0.deb", "https://x/3DReport-2.3.0.deb", 100)
    private val release = LatestRelease("2.3.0", "https://x/release", listOf(asset))

    private class FakeInstaller(var method: InstallMethod? = InstallMethod.LINUX_DEB) : UpdateInstaller {
        var downloadOutcome: (suspend (UpdatePlan, (Float) -> Unit) -> DownloadOutcome)? = null
        var installOutcome: InstallOutcome = InstallOutcome.CloseApp
        val downloaded = mutableListOf<UpdatePlan>()
        val installed = mutableListOf<DownloadedUpdate>()
        val discarded = mutableListOf<DownloadedUpdate>()

        override fun plan(release: LatestRelease): UpdatePlan? = method?.let { UpdatePlan(release, release.assets.first(), it) }

        override suspend fun download(plan: UpdatePlan, onProgress: (Float) -> Unit): DownloadOutcome {
            downloaded += plan
            return downloadOutcome?.invoke(plan, onProgress) ?: DownloadOutcome.Ready(DownloadedUpdate(plan, "/tmp/${plan.asset.name}"))
        }

        override suspend fun install(update: DownloadedUpdate): InstallOutcome {
            installed += update
            return installOutcome
        }

        override fun discard(update: DownloadedUpdate) {
            discarded += update
        }
    }

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private var closed = 0
    private var unsaved = false
    private val platform = FakePlatform()

    private fun viewModel(installer: UpdateInstaller, source: ReleaseSource = ReleaseSource { release }) = UpdateViewModel(
        PreferencesRepository(),
        source,
        "2.2.0",
        installer = installer,
        hasUnsavedWork = { unsaved },
        closeApp = { closed++ },
        platform = platform,
    ).apply { checkNow() }

    @Test
    fun theUpdateButtonOnlyExistsWhenTheInstallerHasAPlan() {
        assertTrue(viewModel(FakeInstaller()).uiState.value.canInstall)
        val without = viewModel(FakeInstaller(method = null))
        assertIs<UpdateState.Available>(without.uiState.value.status)
        assertFalse(without.uiState.value.canInstall, "sem instalador pro sistema, só Ver novidades")
        without.startUpdate()
        assertNull(without.uiState.value.install)
    }

    @Test
    fun downloadingShowsProgressThenCheckingThenReady() {
        val installer = FakeInstaller()
        val seen = mutableListOf<UpdateInstall?>()
        lateinit var vm: UpdateViewModel
        installer.downloadOutcome = { plan, onProgress ->
            seen += vm.uiState.value.install
            onProgress(0.4f)
            seen += vm.uiState.value.install
            onProgress(1f)
            seen += vm.uiState.value.install
            DownloadOutcome.Ready(DownloadedUpdate(plan, "/tmp/a.deb"))
        }
        vm = viewModel(installer)

        vm.startUpdate()

        assertEquals(
            listOf<UpdateInstall?>(UpdateInstall.Downloading(release, 0f, 100), UpdateInstall.Downloading(release, 0.4f, 100), UpdateInstall.Verifying(release)),
            seen,
        )
        val ready = assertIs<UpdateInstall.Ready>(vm.uiState.value.install)
        assertEquals(InstallMethod.LINUX_DEB, ready.update.plan.method)
        assertTrue(installer.installed.isEmpty(), "nada instala antes de a pessoa confirmar")
    }

    @Test
    fun theDownloadKnowsHowManyMegabytesItHas() {
        val state = UpdateInstall.Downloading(release, 0.5f, 98L * 1024 * 1024)
        assertEquals(49L to 98L, state.megabytes)
        assertNull(UpdateInstall.Downloading(release, 0.5f, 0).megabytes)
    }

    @Test
    fun theDownloadProgressOnlyChangesTheStateEveryOnePercent() {
        val installer = FakeInstaller()
        val seen = mutableListOf<Float>()
        lateinit var vm: UpdateViewModel
        installer.downloadOutcome = { plan, onProgress ->
            for (i in 1..1000) {
                onProgress(i / 1001f)
                (vm.uiState.value.install as? UpdateInstall.Downloading)?.progress?.let { if (seen.lastOrNull() != it) seen += it }
            }
            DownloadOutcome.Ready(DownloadedUpdate(plan, "/tmp/a.deb"))
        }
        vm = viewModel(installer)

        vm.startUpdate()

        assertTrue(seen.size in 90..101, "1000 avisos viraram ${seen.size} mudanças")
    }

    @Test
    fun cancellingWhileDownloadingStopsItAndKeepsNothing() {
        val installer = FakeInstaller()
        val gate = CompletableDeferred<DownloadOutcome>()
        installer.downloadOutcome = { _, _ -> gate.await() }
        val vm = viewModel(installer)
        vm.startUpdate()
        assertIs<UpdateInstall.Downloading>(vm.uiState.value.install)

        vm.dismissUpdate()

        assertNull(vm.uiState.value.install)
        // Um download que termina depois de cancelado não reabre a janela, e o arquivo que sobrou é apagado.
        val late = DownloadedUpdate(installer.downloaded.single(), "/tmp/tarde.deb")
        gate.complete(DownloadOutcome.Ready(late))
        assertNull(vm.uiState.value.install)
    }

    @Test
    fun dismissingAfterItIsReadyDiscardsTheFile() {
        val installer = FakeInstaller()
        val vm = viewModel(installer)
        vm.startUpdate()
        val ready = assertIs<UpdateInstall.Ready>(vm.uiState.value.install)

        vm.dismissUpdate()

        assertEquals(listOf(ready.update), installer.discarded)
        assertNull(vm.uiState.value.install)
    }

    @Test
    fun aNetworkFailureOffersToTryAgain() {
        val installer = FakeInstaller()
        var attempts = 0
        installer.downloadOutcome = { plan, _ ->
            if (++attempts == 1) DownloadOutcome.Failed(UpdateFailure.NETWORK) else DownloadOutcome.Ready(DownloadedUpdate(plan, "/tmp/a.deb"))
        }
        val vm = viewModel(installer)

        vm.startUpdate()
        val failed = assertIs<UpdateInstall.Failed>(vm.uiState.value.install)
        assertEquals(UpdateError.NETWORK, failed.reason)
        assertTrue(failed.canRetry)

        vm.retryUpdate()

        assertIs<UpdateInstall.Ready>(vm.uiState.value.install)
        assertEquals(2, attempts)
    }

    @Test
    fun missingOrWrongChecksumsNeverOfferToInstall() {
        for ((failure, error) in listOf(UpdateFailure.NO_CHECKSUMS to UpdateError.NO_CHECKSUMS, UpdateFailure.CHECKSUM_MISMATCH to UpdateError.CHECKSUM_MISMATCH)) {
            val installer = FakeInstaller()
            installer.downloadOutcome = { _, _ -> DownloadOutcome.Failed(failure) }
            val vm = viewModel(installer)

            vm.startUpdate()

            val failed = assertIs<UpdateInstall.Failed>(vm.uiState.value.install)
            assertEquals(error, failed.reason)
            assertFalse(failed.canRetry, "só resta Ver novidades")
            vm.installNow()
            assertTrue(installer.installed.isEmpty())
        }
    }

    @Test
    fun installingClosesTheAppThroughTheNormalPath() {
        val installer = FakeInstaller()
        val vm = viewModel(installer)
        vm.startUpdate()

        vm.installNow()

        assertEquals(1, installer.installed.size)
        assertEquals(1, closed)
        assertNull(vm.uiState.value.install)
    }

    @Test
    fun anUnsavedQuoteAsksBeforeInstallingAndBackKeepsEverything() {
        unsaved = true
        val installer = FakeInstaller()
        val vm = viewModel(installer)
        vm.startUpdate()

        vm.installNow()
        assertIs<UpdateInstall.ConfirmUnsaved>(vm.uiState.value.install)
        assertTrue(installer.installed.isEmpty())
        assertEquals(0, closed)

        vm.backToReady()
        assertIs<UpdateInstall.Ready>(vm.uiState.value.install)

        vm.installNow()
        vm.installAnyway()
        assertEquals(1, installer.installed.size)
        assertEquals(1, closed)
    }

    @Test
    fun aCancelledPasswordChangesNothingAndAllowsAnotherTry() {
        val installer = FakeInstaller()
        installer.installOutcome = InstallOutcome.Cancelled
        val vm = viewModel(installer)
        vm.startUpdate()

        vm.installNow()

        val ready = assertIs<UpdateInstall.Ready>(vm.uiState.value.install)
        assertTrue(ready.passwordCancelled)
        assertEquals(0, closed)
        assertTrue(installer.discarded.isEmpty(), "o arquivo fica pra tentar de novo")
    }

    @Test
    fun aFailedInstallDiscardsTheFileAndShowsAnError() {
        val installer = FakeInstaller()
        installer.installOutcome = InstallOutcome.Failed
        val vm = viewModel(installer)
        vm.startUpdate()

        vm.installNow()

        assertEquals(UpdateError.INSTALL, assertIs<UpdateInstall.Failed>(vm.uiState.value.install).reason)
        assertEquals(1, installer.discarded.size)
        assertEquals(0, closed)
    }

    @Test
    fun withoutPkexecTheCommandIsShownAndCopied() {
        val installer = FakeInstaller(InstallMethod.LINUX_DEB_MANUAL)
        installer.installOutcome = InstallOutcome.Manual("/tmp/3dreport", "sudo apt-get install /tmp/3dreport/a.deb")
        val vm = viewModel(installer)
        vm.startUpdate()
        assertEquals(InstallMethod.LINUX_DEB_MANUAL, assertIs<UpdateInstall.Ready>(vm.uiState.value.install).update.plan.method)

        vm.installNow()

        val manual = assertIs<UpdateInstall.Manual>(vm.uiState.value.install)
        assertEquals("/tmp/3dreport", manual.folderPath)
        assertEquals(0, closed, "o app continua aberto")
        vm.copyManualCommand()
        assertEquals(listOf("sudo apt-get install /tmp/3dreport/a.deb"), platform.copied)
        assertTrue(assertIs<UpdateInstall.Manual>(vm.uiState.value.install).copied, "o botão vira Copiado por uns segundos")
        vm.dismissUpdate()
        assertNull(vm.uiState.value.install)
        assertTrue(installer.discarded.isEmpty(), "a pessoa ainda precisa do arquivo")
    }

    @Test
    fun theInstallerIsNotAskedAgainWhileADownloadIsRunning() {
        val installer = FakeInstaller()
        installer.downloadOutcome = { _, _ -> CompletableDeferred<DownloadOutcome>().await() }
        val vm = viewModel(installer)

        vm.startUpdate()
        vm.startUpdate()

        assertEquals(1, installer.downloaded.size)
    }
}
