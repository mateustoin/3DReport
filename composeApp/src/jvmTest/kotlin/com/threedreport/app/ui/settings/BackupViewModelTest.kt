package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_backup_notice_failed
import _3dreport.composeapp.generated.resources.settings_backup_notice_saved
import com.threedreport.app.PendingWrites
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.RestoreFailure
import com.threedreport.app.data.RestoreResult
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.ui.text.uiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    private inner class FakeBackup(private val result: RestoreResult, private val backupFails: Boolean = false) : BackupRepository {
        override fun createBackup(targetPath: String) {
            if (backupFails) error("disco cheio")
        }
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

    private fun viewModel(result: RestoreResult, backupFails: Boolean = false) = BackupViewModel(
        FakeBackup(result, backupFails),
        PreferencesRepository(),
        RecordingWrites(),
        platform = FakePlatform().apply {
            nextPickPath = "/tmp/backup.zip"
            nextSaveLocation = "/tmp/"
        },
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
        val viewModel = viewModel(RestoreResult.Failure(RestoreFailure.NotABackup))
        viewModel.pickBackupToRestore()

        viewModel.confirmRestore()

        assertEquals(listOf("awaitAll", "pause", "restore", "resume"), calls)
    }

    @Test
    fun aBackupAnnouncesItWithAnActionToOpenTheFolderAndRemembersWhen() {
        val viewModel = viewModel(RestoreResult.Success(previousDataPath = "/tmp/anterior"))
        assertNull(viewModel.uiState.value.lastBackupEpochMillis)

        viewModel.createBackup()

        val notice = runBlocking { viewModel.notices.first() }
        assertEquals(uiText(Res.string.settings_backup_notice_saved), notice.message)
        assertNotNull(notice.action)
        assertFalse(notice.isError)
        assertNotNull(viewModel.uiState.value.lastBackupEpochMillis)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test
    fun aFailedBackupIsAnErrorNotice() {
        val viewModel = viewModel(RestoreResult.Success(previousDataPath = "/tmp/anterior"), backupFails = true)

        viewModel.createBackup()

        val notice = runBlocking { viewModel.notices.first() }
        assertEquals(uiText(Res.string.settings_backup_notice_failed), notice.message)
        assertTrue(notice.isError)
        assertNull(viewModel.uiState.value.lastBackupEpochMillis)
    }

    @Test
    fun pickingAFileOnlyAsksAndCancellingLeavesNothingPending() {
        val viewModel = viewModel(RestoreResult.Success(previousDataPath = "/tmp/anterior"))

        viewModel.pickBackupToRestore()

        assertEquals("backup.zip", viewModel.uiState.value.fileNameToRestore)
        assertTrue(calls.isEmpty(), "escolher não restaura")
        viewModel.cancelRestore()
        assertNull(viewModel.uiState.value.pathToRestore)
    }

    @Test
    fun aFailedRestoreExplainsWhyAndRestoresNothing() {
        val viewModel = viewModel(RestoreResult.Failure(RestoreFailure.NotABackup))
        viewModel.pickBackupToRestore()

        viewModel.confirmRestore()

        val notice = runBlocking { viewModel.notices.first() }
        assertEquals(RestoreFailure.NotABackup.text(), notice.message)
        assertTrue(notice.isError)
        assertNull(viewModel.uiState.value.restoredFromPreviousDataAt)
        assertFalse(viewModel.uiState.value.busy)
    }
}
