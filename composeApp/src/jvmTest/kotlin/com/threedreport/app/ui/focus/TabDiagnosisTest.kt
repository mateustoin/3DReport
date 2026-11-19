package com.threedreport.app.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.threedreport.app.AppContainer
import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.OrdersView
import com.threedreport.app.screenshots.buildContainer
import com.threedreport.app.screenshots.clickByDescription
import com.threedreport.app.screenshots.clickByText
import com.threedreport.app.screenshots.fillDraftQuote
import com.threedreport.app.screenshots.sampleApp
import com.threedreport.app.screenshots.seedSampleData
import com.threedreport.app.screenshots.settle
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.QuoteViewModel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

/**
 * Diagnóstico do Tab: pra cada tela e diálogo, aperta Tab (e Shift+Tab) e escreve a sequência de elementos focados.
 * Só roda com a variável de ambiente `TABPROBE_OUT=<arquivo>`; a regressão fica em [TabNavigationTest].
 */
@OptIn(ExperimentalComposeUiApi::class)
class TabDiagnosisTest {

    @Test
    fun diagnose() {
        val out = System.getenv("TABPROBE_OUT") ?: return
        System.setProperty("java.awt.headless", "true")
        val container = sampleContainer()
        val report = StringBuilder()
        for (scenario in tabScenarios()) {
            report.appendLine(runScenario(if (scenario.freshData) freshContainer() else container, scenario).report())
            File(out).writeText(report.toString())
        }
    }
}

/** Uma tela ou diálogo a percorrer. [dialog] diz que o foco deve ficar na camada do diálogo, a última. */
internal class TabScenario(
    val name: String,
    val start: AppDestination,
    val maxTabs: Int = 40,
    val dialog: Boolean = false,
    val freshData: Boolean = false,
    val walkBack: Boolean = true,
    val prepareQuote: (QuoteViewModel) -> Unit = {},
    val before: (AppContainer) -> Unit = {},
    val setup: (ImageComposeScene) -> Unit = {},
)

internal fun sampleContainer(): AppContainer {
    val dataDir = createTempDirectory("3dreport-tabprobe").toFile()
    seedSampleData(dataDir, null)
    return buildContainer(dataDir)
}

/** Pasta de dados vazia, sem o primeiro uso feito: abre o diálogo de boas-vindas. */
internal fun freshContainer(): AppContainer = buildContainer(createTempDirectory("3dreport-tabprobe-fresh").toFile())

internal fun runScenario(container: AppContainer, scenario: TabScenario, frames: Int = 6, delayMs: Long = 150): TabRun {
    scenario.before(container)
    val scene = ImageComposeScene(width = 1280, height = 800, density = Density(1f)) {
        sampleApp(container, startAt = scenario.start, prepareQuote = scenario.prepareQuote)
    }
    try {
        settle(scene, frames = frames, delayMs = delayMs)
        scenario.setup(scene)
        settle(scene, frames = frames, delayMs = delayMs)
        return FocusProbe(scene, 1280, 800).run(scenario.name, scenario.maxTabs, scenario.dialog, scenario.walkBack)
    } finally {
        runCatching { scene.close() }
    }
}

private fun ImageComposeScene.menu(item: String) {
    clickByDescription("Mais ações")
    settle(this, frames = 3, delayMs = 100)
    clickByText(item)
}

private fun ImageComposeScene.clickThen(vararg steps: String) {
    steps.forEach {
        clickByText(it)
        settle(this, frames = 3, delayMs = 100)
    }
}

private fun threePrints(vm: QuoteViewModel) {
    fillDraftQuote(vm)
    val third = vm.addPrint()
    vm.setPrintName("Tampa", third)
}

private fun ordersView(view: OrdersView): (AppContainer) -> Unit = { it.preferences.setOrdersView(view) }

private fun catalogView(view: CatalogView): (AppContainer) -> Unit = { it.preferences.setCatalogView(view) }

internal fun tabScenarios(): List<TabScenario> = listOf(
    TabScenario("Orçamento (3 impressões)", AppDestination.QUOTE, maxTabs = 170, prepareQuote = ::threePrints),
    TabScenario("Orçamento vazio", AppDestination.QUOTE, maxTabs = 90),
    TabScenario("Orçamento: Negociar", AppDestination.QUOTE, maxTabs = 170, prepareQuote = ::threePrints, setup = { it.clickByText("Negociar") }),
    TabScenario("Orçamento: configurações de impressão", AppDestination.QUOTE, dialog = true, prepareQuote = ::threePrints, setup = { it.clickByText("Configurações de impressão") }),
    TabScenario("Orçamento: escolher data", AppDestination.QUOTE, dialog = true, maxTabs = 60, prepareQuote = ::threePrints, setup = { it.clickByText("Escolher data") }),
    TabScenario("Pedidos (kanban)", AppDestination.ORDERS, maxTabs = 120, before = ordersView(OrdersView.KANBAN)),
    TabScenario("Pedidos (lista)", AppDestination.ORDERS, maxTabs = 120, before = ordersView(OrdersView.LIST)),
    TabScenario("Pedidos: menu do cartão", AppDestination.ORDERS, dialog = true, before = ordersView(OrdersView.KANBAN), setup = { it.clickByDescription("Mais ações") }),
    TabScenario("Pedidos: Editar detalhes", AppDestination.ORDERS, dialog = true, maxTabs = 60, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Editar detalhes") }),
    TabScenario("Pedidos: Excluir (confirmação)", AppDestination.ORDERS, dialog = true, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Excluir") }),
    TabScenario("Pedidos: Pagamentos", AppDestination.ORDERS, dialog = true, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Pagamentos") }),
    TabScenario(
        "Pedidos: Registrar pagamento",
        AppDestination.ORDERS,
        dialog = true,
        before = ordersView(OrdersView.KANBAN),
        setup = {
            it.menu("Pagamentos")
            settle(it, frames = 3, delayMs = 100)
            it.clickByText("Registrar pagamento")
        },
    ),
    TabScenario("Pedidos: Mensagem pro cliente", AppDestination.ORDERS, dialog = true, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Mensagem pro cliente") }),
    TabScenario("Pedidos: Configurações de impressão", AppDestination.ORDERS, dialog = true, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Configurações de impressão") }),
    TabScenario("Pedidos: prazo de entrega", AppDestination.ORDERS, dialog = true, maxTabs = 60, before = ordersView(OrdersView.KANBAN), setup = { it.menu("Alterar prazo de entrega") }),
    TabScenario("Catálogo (grade)", AppDestination.CATALOG, maxTabs = 120, before = catalogView(CatalogView.GRID)),
    TabScenario("Catálogo (lista)", AppDestination.CATALOG, maxTabs = 120, before = catalogView(CatalogView.LIST)),
    TabScenario("Dashboard", AppDestination.DASHBOARD, maxTabs = 80),
    TabScenario("Filamentos (lista)", AppDestination.FILAMENTS, maxTabs = 80),
    TabScenario("Filamentos: painel", AppDestination.FILAMENTS, maxTabs = 90, setup = { it.clickByText("PLA Voolt3D") }),
    TabScenario("Filamentos: novo", AppDestination.FILAMENTS, maxTabs = 90, setup = { it.clickByText("Novo filamento") }),
    TabScenario("Filamentos: excluir (confirmação)", AppDestination.FILAMENTS, dialog = true, setup = { it.clickByDescription("Excluir") }),
    TabScenario("Impressoras (lista)", AppDestination.PRINTERS, maxTabs = 80),
    TabScenario("Impressoras: painel", AppDestination.PRINTERS, maxTabs = 90, setup = { it.clickByText("Bambu Lab A1") }),
    TabScenario("Impressoras: Manutenção", AppDestination.PRINTERS, dialog = true, maxTabs = 60, setup = { it.clickByDescription("Manutenção") }),
    TabScenario("Impressoras: lista pré-cadastrada", AppDestination.PRINTERS, dialog = true, maxTabs = 60, setup = { it.clickByText("Escolher da lista") }),
    TabScenario("Serviços (lista)", AppDestination.SERVICES, maxTabs = 80),
    TabScenario("Serviços: painel", AppDestination.SERVICES, maxTabs = 90, setup = { it.clickByText("Montagem") }),
    TabScenario("Insumos (lista)", AppDestination.CONSUMABLES, maxTabs = 80),
    TabScenario("Insumos: novo", AppDestination.CONSUMABLES, maxTabs = 90, setup = { it.clickByText("Novo insumo") }),
    TabScenario("Configurações: Negócio", AppDestination.SETTINGS, maxTabs = 100),
    TabScenario("Configurações: Canais", AppDestination.SETTINGS, maxTabs = 80, setup = { it.clickByText("Canais de venda") }),
    TabScenario("Configurações: Documentos", AppDestination.SETTINGS, maxTabs = 120, setup = { it.clickByText("Documentos pro cliente") }),
    TabScenario("Configurações: Aparência", AppDestination.SETTINGS, maxTabs = 60, setup = { it.clickByText("Aparência") }),
    TabScenario("Configurações: Backup", AppDestination.SETTINGS, maxTabs = 60, setup = { it.clickByText("Backup") }),
    TabScenario("Configurações: calcular kWh", AppDestination.SETTINGS, dialog = true, maxTabs = 60, setup = { it.clickByText("Calcular o kWh") }),
    TabScenario("Configurações: canal", AppDestination.SETTINGS, dialog = true, maxTabs = 60, setup = { it.clickThen("Canais de venda", "Shopee") }),
    TabScenario("Configurações: modelos salvos", AppDestination.SETTINGS, dialog = true, setup = { it.clickThen("Documentos pro cliente", "Ver modelos salvos") }),
    TabScenario("Sobre", AppDestination.ABOUT, maxTabs = 40),
    TabScenario("Primeiro uso (boas-vindas)", AppDestination.QUOTE, dialog = true, freshData = true),
    TabScenario("Primeiro uso (custos)", AppDestination.QUOTE, dialog = true, freshData = true, setup = { it.clickByText("Continuar") }),
)
