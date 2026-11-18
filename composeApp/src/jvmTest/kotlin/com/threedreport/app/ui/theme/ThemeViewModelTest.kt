package com.threedreport.app.ui.theme

import com.threedreport.app.data.AppLanguage
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.ThemeRepository
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tema e idioma valem na hora (decisões 35 e 152): o ViewModel grava e mostra a escolha, sem "Salvar". */
class ThemeViewModelTest {

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toFile().path)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun theLanguageStartsOnAutomaticAndChangesRightAway() {
        val preferences = PreferencesRepository()
        val viewModel = ThemeViewModel(ThemeRepository(), preferences, scope)
        assertEquals(AppLanguage.AUTO, viewModel.language.value)

        viewModel.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, viewModel.language.value)
        assertEquals(AppLanguage.EN, preferences.preferences.value.language)
        assertEquals(AppLanguage.EN, PreferencesRepository().preferences.value.language)
    }

    @Test
    fun changingTheLanguageKeepsTheTheme() {
        val themes = ThemeRepository()
        val viewModel = ThemeViewModel(themes, PreferencesRepository(), scope)
        viewModel.setMode(ThemeMode.DARK)
        viewModel.setLanguage(AppLanguage.PT_BR)
        assertEquals(ThemeMode.DARK, viewModel.mode.value)
        assertEquals(AppLanguage.PT_BR, viewModel.language.value)
    }
}
