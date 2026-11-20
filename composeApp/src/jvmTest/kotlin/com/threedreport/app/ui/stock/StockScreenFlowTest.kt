package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_stock_warning_empty
import _3dreport.composeapp.generated.resources.stock_action_track
import _3dreport.composeapp.generated.resources.stock_action_track_confirm
import _3dreport.composeapp.generated.resources.stock_amount_grams
import _3dreport.composeapp.generated.resources.stock_field_current_balance
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import com.threedreport.app.AppContainer
import com.threedreport.app.screenshots.Catalog
import com.threedreport.app.screenshots.buildContainer
import com.threedreport.app.screenshots.clickByText
import com.threedreport.app.screenshots.fillDraftQuote
import com.threedreport.app.screenshots.sampleApp
import com.threedreport.app.screenshots.seedSampleData
import com.threedreport.app.screenshots.settle
import com.threedreport.app.ui.format.toWholeGramsNumber
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.report.StockReport
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * O teste manual da Leva 12 pela tela, com o app de verdade renderizado sem janela (como as capturas) sobre os dados de
 * exemplo numa pasta temporária: ligar o controle pelo diálogo, ver o saldo mudar quando um pedido fica Pronto e ver o
 * aviso de falta no Orçamento. Clica e digita pela árvore de semântica, sem coordenadas.
 */
@OptIn(ExperimentalComposeUiApi::class)
class StockScreenFlowTest {

    private lateinit var container: AppContainer

    @BeforeTest
    fun setUp() {
        val dataDir = createTempDirectory("3dreport-test-stock-ui").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
        seedSampleData(dataDir, null)
        container = buildContainer(dataDir)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun trackingFromTheScreenThenAReadyOrderLowersTheBalanceOnScreen() = withScene(AppDestination.STOCK) { scene ->
        assertTrue(container.stock.items.value.isEmpty(), "os dados de exemplo começam sem controle")

        // "Controlar saldo" na primeira linha da lista "Sem controle de saldo", 1000 no campo, "Controlar".
        scene.clickByText(tr(Res.string.stock_action_track))
        settle(scene, frames = 4, delayMs = 100)
        assertTrue(scene.setTextStartingWith(tr(Res.string.stock_field_current_balance, "g")), "o diálogo abriu com o campo do saldo:\n${scene.texts()}")
        settle(scene, frames = 3, delayMs = 100)
        assertTrue(scene.clickExact(tr(Res.string.stock_action_track_confirm)), "o botão Controlar está no diálogo:\n${scene.texts()}")
        settle(scene, frames = 4, delayMs = 100)

        val item = container.stock.items.value.single()
        val ref = assertNotNull(item.ref as? StockItemRef.ColorRef, "a primeira linha é uma cor")
        assertEquals(1000.0, StockReport.balance(ref, container.stock.movements.value), 1e-6)
        assertTrue(grams(1000.0) in scene.texts(), "o saldo aparece na tela:\n${scene.texts()}")

        // Um pedido de 100 g nessa cor, levado a Pronto: a tela mostra 900 g.
        val order = container.quoteHistory.save(name = "Pedido do teste", quote = quoteUsing(ref, grams = 100.0), services = emptyList(), photo = null, sourceLink = null)
        container.quoteHistory.updateStatus(order.id, OrderStatus.PRONTO)
        settle(scene, frames = 4, delayMs = 100)
        assertEquals(900.0, StockReport.balance(ref, container.stock.movements.value), 1e-6)
        assertTrue(grams(900.0) in scene.texts(), "o saldo novo aparece na tela:\n${scene.texts()}")
    }

    @Test
    fun theQuoteWarnsWhenTheTrackedColorIsShort() {
        // O rascunho de exemplo usa PLA branco; com só 10 g controlados, falta.
        container.stock.startTracking(StockItemRef.ColorRef(Catalog.PLA_ID, Catalog.COR_BRANCO), 10.0)
        withScene(AppDestination.QUOTE, prepareQuote = ::fillDraftQuote) { scene ->
            val warning = scene.texts().firstOrNull { it.startsWith(SHORTAGE_PREFIX) }
            assertNotNull(warning, "o aviso de falta aparece no Orçamento:\n${scene.texts()}")
        }
    }

    private fun <T> withScene(
        start: AppDestination,
        prepareQuote: (QuoteViewModel) -> Unit = {},
        block: (ImageComposeScene) -> T,
    ): T {
        val scene = ImageComposeScene(width = 1280, height = 1600, density = Density(1f)) {
            sampleApp(container, startAt = start, prepareQuote = prepareQuote)
        }
        try {
            settle(scene, frames = 6, delayMs = 150)
            return block(scene)
        } finally {
            runCatching { scene.close() }
        }
    }

    /** Um orçamento que gasta [grams] g da cor [ref], com o filamento do cadastro de exemplo. */
    private fun quoteUsing(ref: StockItemRef.ColorRef, grams: Double): Quote {
        val filament = container.filaments.filaments.value.single { it.id == ref.filamentId }
        val color = filament.colors.single { it.id == ref.colorId }
        val job = PrintJob(filaments = listOf(FilamentUsage(filament, filament.lengthMeters(grams), color)), printTimeMinutes = 60.0)
        return Quote(
            prints = listOf(QuotedPrint(job = job, printerId = "p", printerName = "Impressora", cost = PrintCost(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))),
            costs = CostBreakdown(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            salePrice = 20.0,
        )
    }

    private fun grams(amount: Double) = tr(Res.string.stock_amount_grams, amount.toWholeGramsNumber())

    private companion object {
        /** O começo comum dos quatro textos do aviso de falta ("Falta %1$s: ..."), no idioma em uso. */
        val SHORTAGE_PREFIX: String by lazy {
            tr(Res.string.quote_stock_warning_empty, "\u0000", "", "").substringBefore("\u0000")
        }
    }
}

private fun tr(resource: StringResource, vararg args: Any): String = runBlocking { getString(resource, *args) }

/** Todos os textos visíveis na árvore de semântica, de todas as camadas (a tela e os diálogos). */
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.texts(): List<String> {
    val out = mutableListOf<String>()
    fun walk(node: SemanticsNode) {
        node.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
        node.children.forEach(::walk)
    }
    semanticsOwners.forEach { walk(it.rootSemanticsNode) }
    return out
}

/** Clica no nó cujo texto é exatamente [text], começando pela camada de cima (o diálogo aberto). */
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.clickExact(text: String): Boolean {
    fun find(node: SemanticsNode): SemanticsNode? {
        val click = node.config.getOrNull(SemanticsActions.OnClick)
        if (click != null && node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true) return node
        return node.children.firstNotNullOfOrNull(::find)
    }
    for (owner in semanticsOwners.reversed()) {
        val node = find(owner.rootSemanticsNode) ?: continue
        node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        return true
    }
    return false
}

/** Digita 1000 no campo cuja descrição começa com [label], na camada de cima. */
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.setTextStartingWith(label: String, text: String = "1000"): Boolean {
    fun find(node: SemanticsNode): SemanticsNode? {
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription)
        if (node.config.getOrNull(SemanticsActions.SetText) != null && descriptions?.any { it.startsWith(label) } == true) return node
        return node.children.firstNotNullOfOrNull(::find)
    }
    for (owner in semanticsOwners.reversed()) {
        val node = find(owner.rootSemanticsNode) ?: continue
        node.config.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(text))
        return true
    }
    return false
}
