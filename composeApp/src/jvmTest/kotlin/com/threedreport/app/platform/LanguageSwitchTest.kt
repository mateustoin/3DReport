package com.threedreport.app.platform

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_language_auto
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import com.threedreport.app.data.AppLanguage
import com.threedreport.app.ui.text.resolve
import com.threedreport.app.ui.text.uiText
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Trocar o idioma vale na hora, sem reiniciar (decisão 152): a tela inteira relê os textos, inclusive o que o Compose
 * pularia por não ter mudado (uma função com os mesmos parâmetros), o que está numa lista preguiçosa, numa
 * subcomposição, num popup e num diálogo; o estado guardado na tela sobrevive; e o texto lido fora da tela sai no idioma
 * novo.
 */
class LanguageSwitchTest {

    private val original = Locale.getDefault()

    @BeforeTest
    fun startInPortuguese() {
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
    }

    @AfterTest
    fun backToPortuguese() {
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        Locale.setDefault(original)
    }

    /** O último texto que cada pedaço da tela desenhou. */
    private val seen = ConcurrentHashMap<String, String>()

    @Composable
    private fun Probe(id: String) {
        val text = org.jetbrains.compose.resources.stringResource(Res.string.settings_language_auto)
        SideEffect { seen[id] = text }
        Text(text)
    }

    @Test
    fun theWholeScreenFollowsTheNewLanguageAndKeepsItsState() {
        val remembered = mutableListOf<Any>()
        val scene = ImageComposeScene(width = 400, height = 400, density = Density(1f)) {
            ProvideAppLocale {
                val state = remember { Any() }
                SideEffect { remembered += state }
                Column {
                    Probe("direct")
                    LazyColumn { item { Probe("lazy") } }
                    BoxWithConstraints { Probe("subcomposition") }
                    Popup { Probe("popup") }
                    Dialog(onDismissRequest = {}) { Probe("dialog") }
                }
            }
        }
        try {
            settle(scene)
            assertEquals(setOf("direct", "lazy", "subcomposition", "popup", "dialog"), seen.keys)
            assertEquals(setOf("Automático"), seen.values.toSet(), seen.toString())

            DesktopLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))
            settle(scene)
            assertEquals(setOf("Automatic"), seen.values.toSet(), seen.toString())

            DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
            settle(scene)
            assertEquals(setOf("Automático"), seen.values.toSet(), seen.toString())

            // Recompôs tudo três vezes sem recriar o estado da tela.
            assertSame(remembered.first(), remembered.last())
        } finally {
            scene.close()
        }
    }

    /** Por que o [ProvideAppLocale] existe: sem ele, o que o Compose pula fica no idioma de antes. */
    @Test
    fun withoutTheProviderTheSkippedPartsKeepTheOldLanguage() {
        val scene = ImageComposeScene(width = 400, height = 400, density = Density(1f)) {
            Column { Probe("direct") }
        }
        try {
            settle(scene)
            DesktopLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))
            settle(scene)
            assertEquals("Automático", seen["direct"])
        } finally {
            scene.close()
        }
    }

    @Test
    fun textsReadOutsideTheScreenFollowTheNewLanguage() = runBlocking {
        assertEquals("Automático", uiText(Res.string.settings_language_auto).resolve())

        DesktopLocale.use(AppLocaleState(UiLanguage.ENGLISH, "GB"))
        assertEquals("Automatic", uiText(Res.string.settings_language_auto).resolve())

        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        assertEquals("Automático", uiText(Res.string.settings_language_auto).resolve())
    }

    @Test
    fun anyOtherSystemLanguageReadsTheEnglishTexts() = runBlocking {
        // Sistema em alemão no automático: inglês, e não os textos padrão (português) que o Compose escolheria sozinho.
        val german = resolveAppLocale(AppLanguage.AUTO, "de", "DE")
        DesktopLocale.use(german)
        assertEquals("en-DE", Locale.getDefault().toLanguageTag())
        assertEquals("Automatic", uiText(Res.string.settings_language_auto).resolve())
        assertEquals("dd/MM/yyyy", AppLocale.datePattern)
    }

    private fun settle(scene: ImageComposeScene) {
        repeat(5) {
            scene.render(System.nanoTime())
            Thread.sleep(20)
        }
    }
}
