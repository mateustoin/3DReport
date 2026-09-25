package com.threedreport.app.ui.settings

import com.threedreport.app.PendingWrites
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.RestoreResult
import com.threedreport.app.platform.FakePlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Revisão do PR #2: restaurar um backup não pode deixar gravação em segundo plano cair na pasta nova. */
class BackupViewModelTest {

    private val calls = mutableListOf<String>()

    private inner class RecordingWrites : PendingWrites {
        override val allWritten: Boolean = true
        override suspend fun awaitAll() { calls += "awaitAll" }
        override fun retry() = Unit
        override suspend fun pause() { calls += "pause" }
        override fun resume() { calls += "resume" }
        override val isPaused: Boolean = false
    }

    private inner class FakeBackup(private val result: RestoreResult) : BackupRepository {
        override fun createBackup(targetPath: String) = Unit
        override fun suggestedBackupFileName() = "backup.zip"
        override fun restore(sourcePath: String): RestoreResult = result.also { calls += "restore" }
        override fun defaultAutomaticBackupDirectory() = "/tmp"
        override fun createAutomaticBackup(directory: String, keep: Int): String? = null
    }

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel(result: RestoreResult) = BackupViewModel(
        FakeBackup(result),
        PreferencesRepository(),
        RecordingWrites(),
        platform = FakePlatform().apply { nextPickPath = "/tmp/backup.zip" },
        scope = CoroutineScope(Dispatchers.Unconfined),
        io = Dispatchers.Unconfined,
        main = Dispatchers.Unconfined,
    )

    @Test
    fun pendingWritesAreFlushedAndHeldBeforeTheFolderIsSwapped() {
        val viewModel = viewModel(RestoreResult.Success(previousDataPath = "/tmp/anterior"))
        viewModel.pickBackupToRestore()

        viewModel.confirmRestore()

        assertEquals(listOf("awaitAll", "pause", "restore"), calls, "com sucesso, as gravações ficam paradas até o app fechar")
        assertNotNull(viewModel.uiState.value.restoredFromPreviousDataAt)
    }

    @Test
    fun aFailedRestoreLetsWritesGoOn() {
        val viewModel = viewModel(RestoreResult.Failure("não é um backup"))
        viewModel.pickBackupToRestore()

        viewModel.confirmRestore()

        assertEquals(listOf("awaitAll", "pause", "restore", "resume"), calls)
    }
}
