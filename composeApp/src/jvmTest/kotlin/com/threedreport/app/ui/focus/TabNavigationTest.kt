package com.threedreport.app.ui.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import com.threedreport.app.AppContainer
import com.threedreport.app.data.OrdersView
import com.threedreport.app.screenshots.clickByDescription
import com.threedreport.app.screenshots.clickByText
import com.threedreport.app.screenshots.settle
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppTextArea
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regressão do Tab (decisão desta correção): o teclado percorre campo, lista de escolha, seletor, caixa de marcar e botão na
 * ordem da tela, nunca perde o foco, e o diálogo mantém o foco dentro dele. Os testes pequenos usam os componentes do kit
 * sozinhos; os de tela abrem as telas de verdade com os dados de exemplo.
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
class TabNavigationTest {

    /** Monta [content] numa cena pequena, entrega a sonda e fecha a cena. */
    private fun <T> withScene(content: @Composable () -> Unit, block: (FocusProbe) -> T): T {
        val scene = ImageComposeScene(width = 800, height = 600, density = Density(1f)) {
            AppTheme(ThemeMode.LIGHT) { content() }
        }
        try {
            repeat(6) {
                scene.render(System.nanoTime())
                Thread.sleep(40)
            }
            return block(FocusProbe(scene, 800, 600))
        } finally {
            runCatching { scene.close() }
        }
    }

    private fun FocusProbe.tabs(count: Int, shift: Boolean = false): List<String> =
        (1..count).map {
            press(shift)
            currentLabel()
        }

    @Composable
    private fun EveryKindOfControl() {
        var option by remember { mutableStateOf(0) }
        Column {
            AppButton("Antes", {})
            AppTextField("Texto", "", {})
            AppNumberField("Número", "", {}, NumberKind.MEASURE)
            AppTextArea("Observação", "", {})
            AppDropdownField("Lista", listOf("a", "b"), "a", { it }, { it }, {})
            AppChoiceField("Escolha", listOf("Um", "Dois"), option, { option = it })
            AppCheckbox("Marcar", false, {})
            AppButton("Depois", {})
        }
    }

    @Test
    fun tabWalksEveryKindOfControlInOrderAndWraps() = withScene({ EveryKindOfControl() }) { probe ->
        val expected = listOf("Antes", "Texto", "Número", "Observação", "Lista: a", "Um", "Dois", "Marcar", "Depois", "Antes")
        val forward = probe.tabs(expected.size)
        assertEquals(expected, forward.map { it.substringBefore(" (") }, "ordem do Tab: $forward")
    }

    @Test
    fun shiftTabWalksBackInTheSameOrder() = withScene({ EveryKindOfControl() }) { probe ->
        val expected = listOf("Depois", "Marcar", "Dois", "Um", "Lista: a", "Observação", "Número", "Texto", "Antes")
        val backward = probe.tabs(expected.size, shift = true)
        assertEquals(expected, backward.map { it.substringBefore(" (") }, "ordem do Shift+Tab: $backward")
    }

    @Test
    fun focusNeverDisappearsWhileWalkingForm() = withScene({ EveryKindOfControl() }) { probe ->
        val stops = probe.tabs(30) + probe.tabs(30, shift = true)
        assertFalse(stops.any { it == "NADA" }, "o foco sumiu: $stops")
    }

    @Test
    fun helpIconsAreNotTabStops() = withScene({
        Column {
            AppButton("Antes", {})
            AppTextField("Campo", "", {}, help = HelpText("ajuda"))
            AppButton("Depois", {})
        }
    }) { probe ->
        assertEquals(listOf("Antes", "Campo", "Depois", "Antes"), probe.tabs(4).map { it.substringBefore(" (") })
    }

    @Test
    fun aTextFieldAtTheEndDoesNotTrapTheFocus() = withScene({
        Column {
            AppButton("Antes", {})
            AppTextField("Último", "", {})
        }
    }) { probe ->
        val stops = probe.tabs(6).map { it.substringBefore(" (") }
        assertTrue(stops.count { it == "Último" } >= 2 && stops.count { it == "Antes" } >= 2, "o Tab volta ao começo: $stops")
        assertFalse(stops.any { it == "NADA" }, "o foco sumiu: $stops")
    }

    @Test
    fun enterOpensTheListAndSpaceTogglesTheCheckbox() {
        var checked = false
        withScene({
            Column {
                AppDropdownField("Lista", listOf("a", "b"), "a", { it }, { it }, {})
                AppCheckbox("Marcar", checked, { checked = it })
            }
        }) { probe ->
            probe.press()
            assertEquals("Lista: a (DropdownList)", probe.currentLabel())
            val layers = probe.layerCount()
            probe.key(Key.Enter)
            assertTrue(probe.layerCount() > layers, "Enter abre a lista")
            probe.scene.clickByText("b")
            settle(probe.scene, frames = 8, delayMs = 100)
            assertEquals(layers, probe.layerCount(), "escolher fecha a lista")
            assertEquals("Lista: a (DropdownList)", probe.currentLabel(), "escolher devolve o foco ao campo")
            probe.press()
            assertEquals("Marcar (Checkbox)", probe.currentLabel())
            probe.key(Key.Spacebar)
            assertTrue(checked, "Espaço marca a caixa")
        }
    }

    @Test
    fun spaceAndEnterChooseASegmentedOption() {
        var option = 0
        withScene({ AppChoiceField("Escolha", listOf("Um", "Dois", "Três"), option, { option = it }) }) { probe ->
            probe.tabs(2)
            assertEquals("Dois (RadioButton)", probe.currentLabel())
            probe.key(Key.Spacebar)
            assertEquals(1, option)
            probe.press()
            probe.key(Key.Enter)
            assertEquals(2, option)
        }
    }

    @Test
    fun dialogTakesTheFocusOnTheFirstFieldAndKeepsTabInside() {
        var open by mutableStateOf(true)
        withScene({
            Column {
                AppButton("Atrás", {})
                AppTextField("Atrás: campo", "", {})
                if (open) {
                    AppDialog(
                        title = "Título",
                        onDismiss = { open = false },
                        buttons = {
                            AppButton("Cancelar", {})
                            AppButton("Salvar", {})
                        },
                    ) {
                        AppTextField("Nome", "", {})
                        AppTextField("Contato", "", {}, help = HelpText("ajuda"))
                    }
                }
            }
        }) { probe ->
            val run = probe.run("diálogo", 12, dialog = true)
            assertEquals("Nome", run.initial?.name, run.report())
            assertFalse(run.escaped, run.report())
            assertFalse(run.lost, run.report())
            assertEquals(listOf("Contato", "Cancelar", "Salvar", "Nome"), run.forward.take(4).map { it?.name }, run.report())
        }
    }

    // As telas de verdade, com os dados de exemplo.

    private val container: AppContainer by lazy { sampleContainer() }

    private fun checkScreen(scenario: TabScenario, mustReach: List<String>) {
        val run = runScenario(container, scenario, frames = 4, delayMs = 100)
        val report = run.report()
        assertFalse(run.lost, report)
        assertFalse(run.stuck, report)
        assertFalse(run.escaped, report)
        assertNotNull(run.cycle, report)
        assertTrue(run.hidden.isEmpty(), report)
        assertTrue(run.unreached.isEmpty(), report)
        val labels = run.forward.filterNotNull().map { it.name }
        mustReach.forEach { assertTrue(labels.any { label -> label.startsWith(it) }, "o Tab não chegou em \"$it\": $report") }
    }

    @Test
    fun orderScreenReachesSearchAndEveryCard() {
        checkScreen(
            TabScenario("Pedidos", AppDestination.ORDERS, maxTabs = 80, walkBack = false, before = { it.preferences.setOrdersView(OrdersView.KANBAN) }),
            mustReach = listOf("Buscar", "Lista", "Kanban", "Mais ações"),
        )
    }

    @Test
    fun quoteScreenReachesEveryField() {
        checkScreen(
            TabScenario("Orçamento", AppDestination.QUOTE, maxTabs = 80),
            mustReach = listOf("Nome da peça", "Peso (g)", "Tempo de impressão", "Canal de venda", "Frete", "Cliente", "Enviar pro cliente"),
        )
    }

    @Test
    fun filamentPanelReachesItsFieldsAndStaysInViewWhenScrolling() {
        checkScreen(
            TabScenario("Filamentos", AppDestination.FILAMENTS, maxTabs = 90, setup = { it.clickByText("Novo filamento") }),
            mustReach = listOf("Nome", "Marca", "Preço por kg", "Salvar", "Cancelar"),
        )
    }

    @Test
    fun settingsBusinessSectionReachesItsFields() {
        checkScreen(
            TabScenario("Configurações", AppDestination.SETTINGS, maxTabs = 60, walkBack = false),
            mustReach = listOf("Preço do kWh", "Margem de lucro", "Preço mínimo do pedido"),
        )
    }

    @Test
    fun editDetailsDialogStartsOnTheFirstFieldAndStaysInside() {
        val scenario = TabScenario(
            "Editar detalhes",
            AppDestination.ORDERS,
            maxTabs = 40,
            dialog = true,
            before = { it.preferences.setOrdersView(OrdersView.KANBAN) },
            setup = {
                it.clickByDescription("Mais ações")
                settle(it, frames = 3, delayMs = 100)
                it.clickByText("Editar detalhes")
            },
        )
        val run = runScenario(container, scenario, frames = 4, delayMs = 100)
        assertEquals("Nome da peça", run.initial?.name, run.report())
        assertFalse(run.escaped, run.report())
        assertFalse(run.lost, run.report())
        assertNotNull(run.cycle, run.report())
        assertTrue(run.unreached.isEmpty(), run.report())
    }
}
