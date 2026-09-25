package com.threedreport.app.ui.about

import com.threedreport.app.data.PreferencesRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun versionsCompareAsNumbers() {
        assertTrue(isNewerVersion("v2.1.10", "2.1.9"))
        assertTrue(isNewerVersion("3.0.0", "2.9.9"))
        assertTrue(isNewerVersion("2.2", "2.1.0"))
        assertFalse(isNewerVersion("2.1.0", "2.1.0"))
        assertFalse(isNewerVersion("2.0.9", "2.1.0"))
        assertFalse(isNewerVersion("2.2.0-beta", "2.2.0"))
        assertFalse(isNewerVersion("latest", "2.1.0"))
    }

    @Test
    fun offByDefaultAndNothingIsAskedOnStart() {
        var asked = 0
        val viewModel = UpdateViewModel(PreferencesRepository(), { asked++; LatestRelease("9.0.0", "https://x") }, "2.1.0")

        viewModel.checkOnStart()

        assertFalse(viewModel.enabled.value)
        assertEquals(0, asked)
        assertEquals(UpdateState.Idle, viewModel.state.value)
    }

    @Test
    fun turningOnSavesTheChoiceAndChecks() {
        val preferences = PreferencesRepository()
        val viewModel = UpdateViewModel(preferences, { LatestRelease("2.2.0", "https://x") }, "2.1.0")

        viewModel.setEnabled(true)

        assertTrue(preferences.preferences.value.checkForUpdates)
        assertEquals(UpdateState.Available(LatestRelease("2.2.0", "https://x")), viewModel.state.value)
    }

    @Test
    fun sameVersionIsUpToDateAndAFailureSaysSo() {
        assertEquals(
            UpdateState.UpToDate,
            UpdateViewModel(PreferencesRepository(), { LatestRelease("2.1.0", "https://x") }, "2.1.0").apply { checkNow() }.state.value,
        )
        assertEquals(
            UpdateState.Failed,
            UpdateViewModel(PreferencesRepository(), { error("sem internet") }, "2.1.0").apply { checkNow() }.state.value,
        )
    }

    @Test
    fun aCheckThatEndsAfterTurningOffShowsNothing() {
        var finish: (() -> Unit)? = null
        val preferences = PreferencesRepository()
        val viewModel = UpdateViewModel(
            preferences,
            { LatestRelease("9.0.0", "https://x") },
            "2.1.0",
            // A leitura fica parada até a gente soltar, como uma rede lenta.
            background = object : kotlinx.coroutines.CoroutineDispatcher() {
                override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) { finish = { block.run() } }
            },
        )
        viewModel.setEnabled(true)
        viewModel.setEnabled(false)

        finish!!.invoke()

        assertEquals(UpdateState.Idle, viewModel.state.value)
    }

    @Test
    fun aNewVersionIsAnnouncedOnce() {
        val preferences = PreferencesRepository()
        val viewModel = UpdateViewModel(preferences, { LatestRelease("2.2.0", "https://x") }, "2.1.0")
        viewModel.checkNow()
        val release = assertNotNull(viewModel.unannounced(viewModel.state.value))

        viewModel.markAnnounced(release)

        assertEquals(null, UpdateViewModel(preferences, { release }, "2.1.0").apply { checkNow() }.let { it.unannounced(it.state.value) })
    }
}
