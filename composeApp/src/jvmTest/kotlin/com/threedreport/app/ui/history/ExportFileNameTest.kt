package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os nomes de arquivo sugeridos ao salvar (Leva 10B, parte 7) vêm de strings.xml, com plural de verdade, e saem como
 * antes em português.
 */
class ExportFileNameTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        dataDir.deleteRecursively()
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = Quote(
        prints = listOf(
            QuotedPrint(
                job = PrintJob(filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24), filamentLengthMeters = 1.0, printTimeMinutes = 10.0),
                printerId = "p",
                printerName = "Impressora",
                cost = PrintCost(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ),
        ),
        costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        salePrice = 10.0,
    )

    private fun saved(name: String, number: Int = 0, autoNamed: Boolean = false, kind: QuoteKind = QuoteKind.ORDER) =
        SavedQuote(id = name + number, name = name, number = number, autoNamed = autoNamed, kind = kind, quote = quote, savedAtEpochMillis = 0L)

    /**
     * O que [action] gravou. Espera o aviso de salvo: na primeira leitura, os recursos chegam de outro thread, e com
     * `Unconfined` o resto continua lá.
     */
    private fun namesSaved(action: (QuoteExporter) -> Unit): List<String> {
        val platform = FakePlatform().apply { nextSaveLocation = "/tmp/" }
        val done = CompletableDeferred<UiNotice>()
        action(QuoteExporter(platform, BrandingRepository(), QuoteHistoryRepository(), CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Dispatchers.Unconfined) { done.complete(it) })
        runBlocking { withTimeout(10_000) { done.await() } }
        return platform.written.keys.map { it.removePrefix("/tmp/") }
    }

    @Test
    fun severalQuotesInOnePdfCountTheItemsWithTheRightPlural() {
        assertEquals(listOf("Orçamentos _2 itens_.pdf"), namesSaved { it.exportQuotesPdf(listOf(saved("A"), saved("B"))) })
        assertEquals(listOf("Orçamentos _1 item_.pdf"), namesSaved { it.exportQuotesPdf(listOf(saved("A"))) })
    }

    @Test
    fun theCatalogCountsTheProductsWithTheRightPlural() {
        val products = listOf(saved("A", kind = QuoteKind.PRODUCT), saved("B", kind = QuoteKind.PRODUCT), saved("C", kind = QuoteKind.PRODUCT))
        assertEquals(listOf("Catálogo _3 itens_.pdf"), namesSaved { it.exportCatalogPdf(products) })
        assertEquals(listOf("Catálogo _1 item_.pdf"), namesSaved { it.exportCatalogPdf(products.take(1)) })
    }

    @Test
    fun anAutomaticNameBecomesTheQuoteNumber() {
        assertEquals(listOf("Orçamento 0042.pdf"), namesSaved { it.exportPdf(saved("Orçamento - 24/09/2026 14:30", number = 42, autoNamed = true)) })
        assertEquals(listOf("Orçamento.pdf"), namesSaved { it.exportPdf(saved("Orçamento - 24/09/2026 14:30", autoNamed = true)) })
        assertEquals(listOf("Suporte de fone.pdf"), namesSaved { it.exportPdf(saved("Suporte de fone", number = 42)) })
    }

    @Test
    fun aNameWithNothingUsableFallsBackToAPlainOne() {
        assertEquals(listOf("orcamento.pdf"), namesSaved { it.exportPdf(saved("   ")) })
    }
}
