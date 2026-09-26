package com.threedreport.app.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threedreport.app.App
import com.threedreport.app.AppContainer
import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.ui.about.ReleaseSource
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.model.ThemeMode
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Color as AwtColor
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

// Tamanho de todo print de tela do site (decisão 120): 1280 é exatamente o SIDEBAR_LABELS_MIN_WINDOW_WIDTH,
// então a barra lateral sai com os rótulos, e não só os ícones.
private const val WIDTH = 1280
private const val HEIGHT = 800

// "Hoje" fixo pros dados de exemplo terem datas plausíveis (prazos, histórico) sem depender de quando o
// gerador roda.
private val TODAY: LocalDate = LocalDate.of(2026, 9, 26)
private val ZONE = ZoneId.systemDefault()

/**
 * Gera os prints do site (os PNGs de site/assets/screenshots e site/assets/og-banner.png) a partir do app de
 * verdade, renderizado sem janela com [ImageComposeScene] sobre dados de exemplo fictícios. Substitui o
 * processo manual da decisão 90, que nunca foi versionado.
 *
 * Não roda no `./gradlew build`/`jvmTest` normal: só quando a propriedade de sistema `screenshots.out`
 * está definida (ver a tarefa `renderScreenshots` em `composeApp/build.gradle.kts`), pra não deixar o
 * build normal mais lento nem escrever nada fora de um diretório temporário.
 */
class ScreenshotRenderer {

    @Test
    fun renderScreenshots() {
        val outDirPath = System.getProperty("screenshots.out") ?: return
        System.setProperty("java.awt.headless", "true")

        val outDir = File(outDirPath).apply { mkdirs() }
        val ogBannerFile = File(outDir.parentFile, "og-banner.png")
        val iconFile = System.getProperty("screenshots.iconPath")?.let(::File)
        val gcodesDir = System.getProperty("screenshots.gcodes")?.let(::File)?.takeIf { it.isDirectory }

        // Pasta de dados própria, nunca a de verdade (~/.3dreport): ver TestRepositories.kt e o
        // comentário no topo de MultiPrintQuoteTest.kt — um repositório criado antes de apontar pra cá
        // gravaria na pasta real.
        val dataDir = createTempDirectory("3dreport-screenshots").toFile()
        seedSampleData(dataDir, gcodesDir)

        val container = buildContainer(dataDir)

        renderAppPng(File(outDir, "orcamento.png")) {
            App(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm -> fillDraftQuote(vm) },
            )
        }

        // Kanban, não a lista: é o que mostra o pipeline de pedidos vivo (decisão 120).
        renderAppPng(File(outDir, "pedidos.png"), afterSettle = { it.clickByText("Kanban") }) {
            App(container, startAt = AppDestination.ORDERS)
        }

        renderAppPng(File(outDir, "dashboard.png")) {
            App(container, startAt = AppDestination.DASHBOARD)
        }

        renderAppPng(File(outDir, "catalogo.png")) {
            App(container, startAt = AppDestination.CATALOG)
        }

        val portaCelular = container.quoteHistory.savedQuotes.value.first { it.name == "Porta-celular" }
        val photoBytes = container.quoteHistory.photoBytes(portaCelular)
        val branding = container.branding.branding.value
        renderPdfComposite(portaCelular, photoBytes, branding, File(outDir, "pdf.png"))
        renderWhatsAppPng(portaCelular, photoBytes, branding, File(outDir, "whatsapp.png"))

        if (iconFile != null && iconFile.exists()) {
            renderOgBanner(iconFile, File(outDir, "orcamento.png"), ogBannerFile)
        }

        // O conjunto novo troca o histórico (agora "pedidos.png", com o Kanban da barra lateral).
        File(outDir, "historico.png").delete()
    }
}

/**
 * Ids fixos dos cadastros de exemplo: escolhidos aqui (como um cadastro de verdade escolheria um id
 * gerado), usados tanto pra criar o catálogo em [seedSampleData] quanto pra montar os pedidos de
 * exemplo e o rascunho do print do Orçamento ([fillDraftQuote]).
 */
private object Catalog {
    const val PLA_ID = "fil-pla-voolt3d"
    const val COR_BRANCO = "cor-branco"
    const val COR_PRETO = "cor-preto"
    const val COR_CINZA = "cor-cinza"
    const val PETG_ID = "fil-petg-voolt3d"
    const val COR_NATURAL = "cor-natural"
    const val BAMBU_ID = "printer-bambu-a1"
    const val K1_ID = "printer-creality-k1"
    const val PINTURA_ID = "service-pintura"
    const val SHOPEE_ID = "channel-shopee"
    const val MERCADO_LIVRE_ID = "channel-ml"
    const val PIX_ID = "channel-pix"
}

/** O orçamento aberto na aba do print de tela do Orçamento: um pedido com duas impressões, negociação e prazo. */
private fun fillDraftQuote(vm: QuoteViewModel) {
    vm.setPrintName("Cabeça e corpo")
    vm.selectFilament(Catalog.PLA_ID)
    vm.selectFilamentColor(Catalog.COR_BRANCO)
    vm.selectPrinter(Catalog.BAMBU_ID)
    vm.setLengthMeters("48")
    vm.setPrintTimeMinutes("6h20")

    val secondPrintId = vm.addPrint()
    vm.setPrintName("Base", secondPrintId)
    vm.selectFilament(Catalog.PLA_ID, printId = secondPrintId)
    vm.selectFilamentColor(Catalog.COR_PRETO, printId = secondPrintId)
    vm.selectPrinter(Catalog.K1_ID, printId = secondPrintId)
    vm.setLengthMeters("15", secondPrintId)
    vm.setPrintTimeMinutes("1h40", secondPrintId)

    vm.toggleService(Catalog.PINTURA_ID)
    vm.setServicePrice(Catalog.PINTURA_ID, "35")
    vm.selectSalesChannel(Catalog.SHOPEE_ID)
    vm.setSaveName("Action figure (cabeça, corpo e base)")
    vm.setClientName("Larissa M.")
    vm.setDeliveryDate(deliveryEpochDay(5))
}

// ---------------------------------------------------------------------------------------------
// Dados de exemplo
// ---------------------------------------------------------------------------------------------

/** Relógio controlável: cada pedido de exemplo nasce e muda de andamento numa data diferente. */
private class SeedClock(var millis: Long) : Clock {
    override fun nowMillis(): Long = millis
}

private fun atDaysAgo(days: Int, hour: Int = 10): Long =
    TODAY.minusDays(days.toLong()).atTime(hour, 0).atZone(ZONE).toInstant().toEpochMilli()

private fun deliveryEpochDay(daysFromToday: Int): Long = TODAY.plusDays(daysFromToday.toLong()).toEpochDay()

private data class OrderSeed(
    val name: String,
    val gcodeFile: String? = null,
    val filamentId: String,
    val colorId: String,
    val printerId: String,
    val lengthMeters: String = "",
    val printTimeText: String = "",
    val channelId: String,
    val clientName: String,
    val clientContact: String,
    val servicePrice: String? = null,
    val createdDaysAgo: Int,
    /** Cada mudança de andamento, com há quantos dias (a partir da criação) ela aconteceu. */
    val statusSteps: List<Pair<OrderStatus, Int>> = emptyList(),
    val deliveryDaysFromToday: Int,
)

private data class ProductSeed(
    val name: String,
    val category: String,
    val filamentId: String,
    val colorId: String,
    val printerId: String,
    val lengthMeters: String,
    val printTimeText: String,
)

private val ORDER_SEEDS = listOf(
    OrderSeed(
        name = "Miniatura pintada",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.BAMBU_ID,
        lengthMeters = "18", printTimeText = "1h50",
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = "Rafael N.", clientContact = "rafael.n@example.com",
        servicePrice = "20", createdDaysAgo = 1, deliveryDaysFromToday = 6,
    ),
    OrderSeed(
        name = "Vaso geométrico (encomenda)",
        filamentId = Catalog.PETG_ID, colorId = Catalog.COR_NATURAL, printerId = Catalog.K1_ID,
        lengthMeters = "85", printTimeText = "8h10",
        channelId = Catalog.PIX_ID, clientName = "Camila F.", clientContact = "camila.f@example.com",
        createdDaysAgo = 2, deliveryDaysFromToday = 10,
    ),
    OrderSeed(
        name = "Suporte de headset",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_PRETO, printerId = Catalog.BAMBU_ID,
        lengthMeters = "52", printTimeText = "5h",
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = "Juliana T.", clientContact = "juliana.t@example.com",
        createdDaysAgo = 4, statusSteps = listOf(OrderStatus.APROVADO to 1), deliveryDaysFromToday = 5,
    ),
    OrderSeed(
        name = "Chaveiro personalizado",
        gcodeFile = "Articulated_Keychains_PLA_4h57m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_PRETO, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.PIX_ID, clientName = "Carla S.", clientContact = "carla.s@example.com",
        createdDaysAgo = 6, statusSteps = listOf(OrderStatus.APROVADO to 1), deliveryDaysFromToday = 3,
    ),
    OrderSeed(
        name = "Porta-celular",
        gcodeFile = "smartphone_holder_PLA_1h15m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.K1_ID,
        channelId = Catalog.SHOPEE_ID, clientName = "Ana P.", clientContact = "ana.p@example.com",
        createdDaysAgo = 8, statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 6),
        deliveryDaysFromToday = 1,
    ),
    OrderSeed(
        name = "Axolote de estimação",
        gcodeFile = "articulated_axolotl_2_PLA_2h16m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = "Bruno M.", clientContact = "bruno.m@example.com",
        createdDaysAgo = 12, statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 11),
        deliveryDaysFromToday = 2,
    ),
    OrderSeed(
        name = "Organizador de mesa (encomenda)",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.K1_ID,
        lengthMeters = "42", printTimeText = "3h40",
        channelId = Catalog.PIX_ID, clientName = "Marcos L.", clientContact = "marcos.l@example.com",
        createdDaysAgo = 9,
        statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 8),
        deliveryDaysFromToday = 0,
    ),
    OrderSeed(
        name = "Luminária lua (encomenda)",
        filamentId = Catalog.PETG_ID, colorId = Catalog.COR_NATURAL, printerId = Catalog.K1_ID,
        lengthMeters = "68", printTimeText = "6h30",
        channelId = Catalog.SHOPEE_ID, clientName = "Patrícia G.", clientContact = "patricia.g@example.com",
        createdDaysAgo = 18,
        statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 15),
        deliveryDaysFromToday = 1,
    ),
    OrderSeed(
        name = "Barquinho decorativo",
        gcodeFile = "3DBenchy_PLA_31m44s.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.SHOPEE_ID, clientName = "Diego A.", clientContact = "diego.a@example.com",
        createdDaysAgo = 22,
        statusSteps = listOf(
            OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 12, OrderStatus.ENTREGUE to 20,
        ),
        deliveryDaysFromToday = -2,
    ),
    OrderSeed(
        name = "Presente de aniversário",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.K1_ID,
        lengthMeters = "33", printTimeText = "3h",
        channelId = Catalog.PIX_ID, clientName = "Fernanda R.", clientContact = "fernanda.r@example.com",
        servicePrice = "15", createdDaysAgo = 25,
        statusSteps = listOf(
            OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 10, OrderStatus.ENTREGUE to 23,
        ),
        deliveryDaysFromToday = -2,
    ),
)

private val PRODUCT_SEEDS = listOf(
    ProductSeed("Vaso geométrico", "Decoração", Catalog.PETG_ID, Catalog.COR_NATURAL, Catalog.BAMBU_ID, "60", "5h30"),
    ProductSeed("Organizador de mesa", "Casa", Catalog.PLA_ID, Catalog.COR_BRANCO, Catalog.K1_ID, "45", "4h"),
    ProductSeed("Luminária lua", "Decoração", Catalog.PETG_ID, Catalog.COR_NATURAL, Catalog.K1_ID, "70", "7h"),
    ProductSeed("Chaveiro geométrico", "Chaveiros", Catalog.PLA_ID, Catalog.COR_PRETO, Catalog.BAMBU_ID, "8", "40min"),
    ProductSeed("Suporte de fone (over-ear)", "Acessórios", Catalog.PLA_ID, Catalog.COR_BRANCO, Catalog.BAMBU_ID, "50", "4h30"),
    ProductSeed("Dragão articulado", "Colecionáveis", Catalog.PLA_ID, Catalog.COR_CINZA, Catalog.K1_ID, "90", "9h"),
)

/** Monta o catálogo e o histórico de exemplo, com dados fictícios brasileiros, na pasta [dataDir]. */
private fun seedSampleData(dataDir: File, gcodesDir: File?) {
    val clock = SeedClock(atDaysAgo(30))
    val storage = LocalStorage(dataDir, clock)

    val filamentRepo = storage.filaments()
    filamentRepo.items.value.forEach { filamentRepo.delete(it.id) } // tira o seed de fábrica (PLA/ABS/PETG genéricos)
    filamentRepo.add(
        Filament(
            id = Catalog.PLA_ID,
            name = "PLA Voolt3D",
            pricePerKg = 89.90,
            densityGPerCm3 = 1.24,
            materialType = "PLA",
            brand = "Voolt3D",
            colors = listOf(
                FilamentColor(id = Catalog.COR_BRANCO, name = "Branco", hex = "#F5F5F5"),
                FilamentColor(id = Catalog.COR_PRETO, name = "Preto", hex = "#161616"),
                FilamentColor(id = Catalog.COR_CINZA, name = "Cinza", hex = "#9E9E9E"),
            ),
        ),
    )
    filamentRepo.add(
        Filament(
            id = Catalog.PETG_ID,
            name = "PETG Voolt3D",
            pricePerKg = 118.0,
            densityGPerCm3 = 1.27,
            materialType = "PETG",
            brand = "Voolt3D",
            colors = listOf(FilamentColor(id = Catalog.COR_NATURAL, name = "Natural", hex = "#F0EDE4")),
        ),
    )

    val printerRepo = storage.printers()
    printerRepo.items.value.forEach { printerRepo.delete(it.id) } // tira a "Minha impressora" de fábrica
    printerRepo.add(
        PrinterProfile(
            id = Catalog.BAMBU_ID,
            name = "Bambu Lab A1",
            printerPowerWatts = 1300.0,
            maintenanceCostPerHour = 0.15,
            machineInvestment = MachineInvestment(machinePrice = 2200.0, paybackMonths = 12, printingDaysPerMonth = 24, printingHoursPerDay = 10.0),
        ),
    )
    printerRepo.add(
        PrinterProfile(
            id = Catalog.K1_ID,
            name = "Creality K1",
            printerPowerWatts = 350.0,
            maintenanceCostPerHour = 0.20,
            machineInvestment = MachineInvestment(machinePrice = 2600.0, paybackMonths = 12, printingDaysPerMonth = 24, printingHoursPerDay = 10.0),
        ),
    )

    val serviceRepo = storage.services()
    serviceRepo.add(Service(id = Catalog.PINTURA_ID, name = "Pintura", suggestedPrice = 25.0, chargedPerOrder = false))

    val channelRepo = storage.salesChannels()
    channelRepo.add(SalesChannel(id = Catalog.SHOPEE_ID, name = "Shopee", feeRate = 0.20))
    channelRepo.add(SalesChannel(id = Catalog.MERCADO_LIVRE_ID, name = "Mercado Livre", feeRate = 0.16))
    channelRepo.add(SalesChannel(id = Catalog.PIX_ID, name = "Pix", feeRate = 0.0))

    storage.settings().update(
        PricingSettings(
            energyPricePerKwh = 0.85,
            failureRate = 0.08,
            finishingRate = 0.05,
            laborRatePerHour = 25.0,
            monthlyFixedCost = 200.0,
            productiveHoursPerMonth = 150.0,
            taxRate = 0.06,
            profitMargin = 1.0,
        ),
    )
    storage.branding().update(
        BrandingSettings(
            brandName = "Sua Loja 3D",
            showWatermark = true,
            showFooter = true,
            contactEmail = "contato@example.com",
            contactInstagram = "sualoja3d",
            quoteValidityDays = 7,
        ),
    )
    storage.theme().update(ThemeMode.LIGHT)
    storage.currency().update(Currency.BRL)
    storage.onboarding().markCompleted()

    val clientRepo = storage.clients()
    val history = storage.quoteHistory()
    val settingsRepo = storage.settings()

    fun newViewModel() = QuoteViewModel(filamentRepo, printerRepo, settingsRepo, serviceRepo, channelRepo, history, clientRepository = clientRepo)

    for (spec in ORDER_SEEDS) {
        clock.millis = atDaysAgo(spec.createdDaysAgo)
        val vm = newViewModel()
        val gcode = spec.gcodeFile?.let { fileName -> gcodesDir?.let { File(it, fileName) } }?.takeIf { it.exists() }
        if (gcode != null) vm.importGCode(PickedFile(gcode.name, gcode.readBytes()))
        vm.selectFilament(spec.filamentId)
        vm.selectFilamentColor(spec.colorId)
        vm.selectPrinter(spec.printerId)
        if (spec.lengthMeters.isNotEmpty()) vm.setLengthMeters(spec.lengthMeters)
        if (spec.printTimeText.isNotEmpty()) vm.setPrintTimeMinutes(spec.printTimeText)
        vm.selectSalesChannel(spec.channelId)
        if (spec.servicePrice != null) {
            vm.toggleService(Catalog.PINTURA_ID)
            vm.setServicePrice(Catalog.PINTURA_ID, spec.servicePrice)
        }
        vm.setSaveName(spec.name)
        vm.setClientName(spec.clientName)
        vm.setClientContact(spec.clientContact)
        vm.setDeliveryDate(deliveryEpochDay(spec.deliveryDaysFromToday))
        val ok = vm.saveCurrentQuote()
        check(ok) { "Não consegui salvar o pedido de exemplo \"${spec.name}\": ${vm.saveForm.value.blockedMessage}" }
        val savedId = history.savedQuotes.value.last().id
        for ((status, daysAfterCreation) in spec.statusSteps) {
            clock.millis = atDaysAgo(spec.createdDaysAgo - daysAfterCreation)
            history.updateStatus(savedId, status)
        }
    }

    for (spec in PRODUCT_SEEDS) {
        clock.millis = atDaysAgo(15)
        val vm = newViewModel()
        vm.setKind(QuoteKind.PRODUCT)
        vm.selectFilament(spec.filamentId)
        vm.selectFilamentColor(spec.colorId)
        vm.selectPrinter(spec.printerId)
        vm.setLengthMeters(spec.lengthMeters)
        vm.setPrintTimeMinutes(spec.printTimeText)
        vm.setSaveName(spec.name)
        vm.setCategory(spec.category)
        val ok = vm.saveCurrentQuote()
        check(ok) { "Não consegui salvar o produto de exemplo \"${spec.name}\": ${vm.saveForm.value.blockedMessage}" }
    }
}

/** Monta o [AppContainer] pra renderizar, como [com.threedreport.app.DesktopContainer.createDesktopContainer], mas
 * sobre a pasta temporária e sem gravação em segundo plano nem verificação de atualização de verdade. */
private fun buildContainer(dataDir: File): AppContainer {
    val storage = LocalStorage(dataDir, Clock.System)
    return AppContainer(
        filaments = storage.filaments(),
        printers = storage.printers(),
        services = storage.services(),
        salesChannels = storage.salesChannels(),
        templates = storage.templates(),
        clients = storage.clients(),
        maintenance = storage.maintenance(),
        quoteHistory = storage.quoteHistory(),
        settings = storage.settings(),
        branding = storage.branding(),
        theme = storage.theme(),
        currency = storage.currency(),
        onboarding = storage.onboarding(),
        preferences = storage.preferences(),
        backup = storage.backup(),
        storageHealth = storage.health,
        releases = ReleaseSource { null },
    )
}

// ---------------------------------------------------------------------------------------------
// Renderização (ImageComposeScene)
// ---------------------------------------------------------------------------------------------

/**
 * Renderiza [content] num [ImageComposeScene] de [WIDTH]x[HEIGHT] e grava em [outFile]. Desenha alguns
 * quadros com uma pequena espera entre eles até o estado assentar (a composição usa Dispatchers.Main/
 * Default de verdade, ver App.kt), com [afterSettle] rodando entre a primeira leva e a captura final —
 * usado pra simular um clique (ver [clickByText]).
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun renderAppPng(outFile: File, afterSettle: (ImageComposeScene) -> Unit = {}, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = WIDTH, height = HEIGHT, density = Density(1f), content = content)
    try {
        settle(scene)
        afterSettle(scene)
        val image = settle(scene)
        outFile.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    } finally {
        scene.close()
    }
}

private fun settle(scene: ImageComposeScene, frames: Int = 10, delayMs: Long = 300): SkiaImage {
    var image = scene.render(System.nanoTime())
    repeat(frames - 1) {
        Thread.sleep(delayMs)
        image = scene.render(System.nanoTime())
    }
    return image
}

/**
 * Clica no primeiro nó de semântica com [text] que tem ação de clique (o mesmo que um teste de UI faria
 * com `onNodeWithText(text).performClick()`), sem precisar de coordenadas de tela. Usado pra trocar
 * Pedidos de Lista pra Kanban, que é estado só da tela (não do ViewModel).
 */
private fun ImageComposeScene.clickByText(text: String) {
    for (owner in semanticsOwners) {
        val node = findClickableNodeWithText(owner.rootSemanticsNode, text) ?: continue
        node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        return
    }
}

private fun findClickableNodeWithText(node: SemanticsNode, text: String): SemanticsNode? {
    val texts = node.config.getOrNull(SemanticsProperties.Text)
    val hasClick = node.config.getOrNull(SemanticsActions.OnClick) != null
    if (hasClick && texts?.any { it.text.contains(text) } == true) return node
    for (child in node.children) {
        findClickableNodeWithText(child, text)?.let { return it }
    }
    return null
}

// ---------------------------------------------------------------------------------------------
// PDF, WhatsApp e banner de compartilhamento
// ---------------------------------------------------------------------------------------------

/** O PDF de verdade (mesmo exportador do app), a primeira página rasterizada e composta num fundo neutro. */
private fun renderPdfComposite(savedQuote: SavedQuote, photoBytes: ByteArray?, branding: BrandingSettings, outFile: File) {
    val resolved = branding.resolvePdfBranding(logoBytes = null)
    val pdfBytes = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photoBytes)), resolved.brandName, resolved.footerText, resolved.options)
    val pageImage = Loader.loadPDF(pdfBytes).use { document -> PDFRenderer(document).renderImageWithDPI(0, 150f) }

    val canvas = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.color = AwtColor(0xF0, 0xF1, 0xF5)
    g.fillRect(0, 0, WIDTH, HEIGHT)

    val maxHeight = HEIGHT - 80
    val scale = maxHeight.toDouble() / pageImage.height
    val pageW = (pageImage.width * scale).toInt()
    val pageH = (pageImage.height * scale).toInt()
    val x = (WIDTH - pageW) / 2
    val y = (HEIGHT - pageH) / 2

    g.color = AwtColor(0, 0, 0, 35)
    g.fillRoundRect(x - 6, y - 6 + 12, pageW + 12, pageH + 12, 8, 8)

    g.drawImage(pageImage, x, y, pageW, pageH, null)
    g.color = AwtColor(0xD6, 0xD8, 0xDE)
    g.drawRect(x, y, pageW, pageH)
    g.dispose()

    ImageIO.write(canvas, "png", outFile)
}

/** A imagem quadrada de verdade (mesmo exportador do "Salvar imagem" do Histórico), salva como está. */
private fun renderWhatsAppPng(savedQuote: SavedQuote, photoBytes: ByteArray?, branding: BrandingSettings, outFile: File) {
    val currency = savedQuote.currency
    val quantity = savedQuote.quote.quantity
    val piecesTotal = savedQuote.totalWithServices - savedQuote.shippingCost
    val bytes = renderQuoteImage(
        title = savedQuote.name,
        priceText = savedQuote.totalWithServices.toCurrencyText(currency),
        unitPriceText = if (quantity > 1) "$quantity peças · ${(piecesTotal / quantity).toCurrencyText(currency)} cada" else null,
        photoBytes = photoBytes,
        brandText = branding.imageBrandLine(),
        deliveryText = savedQuote.deliveryDateEpochDay?.let { "Entrega até ${formatShortDate(it)}" },
    )
    outFile.writeBytes(bytes)
}

/** Banner de compartilhamento (Open Graph, 1200x630), montado com Compose como o resto dos prints. */
@OptIn(ExperimentalComposeUiApi::class)
private fun renderOgBanner(iconFile: File, orcamentoScreenshot: File, outFile: File) {
    val iconBitmap = decodeImageBitmap(iconFile.readBytes())
    // Um recorte do canto do print do Orçamento (o cartão de Resultado), pra ilustrar o app sem
    // espremer a tela inteira num banner bem mais baixo que largo. Mesma proporção da caixa do banner
    // (420x480), pro ContentScale.Crop não cortar os valores da direita.
    val full = ImageIO.read(orcamentoScreenshot)
    val crop = full.getSubimage(812, 16, 462, 528)
    val cropBytes = java.io.ByteArrayOutputStream().use { out -> ImageIO.write(crop, "png", out); out.toByteArray() }
    val cropBitmap = decodeImageBitmap(cropBytes)

    val scene = ImageComposeScene(width = 1200, height = 630, density = Density(1f), content = { OgBanner(iconBitmap, cropBitmap) })
    try {
        val image = settle(scene, frames = 3, delayMs = 50)
        outFile.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    } finally {
        scene.close()
    }
}

@Composable
private fun OgBanner(icon: ImageBitmap, screenshot: ImageBitmap) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0B5FA8), Color(0xFF083E6E)))),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(84.dp))
                    Spacer(Modifier.width(24.dp))
                    Text("3DReport", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    "O sistema de quem vende\nimpressão 3D",
                    color = Color.White,
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Grátis, open source e 100% no seu computador",
                    color = Color(0xFFD3E4FF),
                    fontSize = 23.sp,
                )
            }
            Spacer(Modifier.width(32.dp))
            Box(
                modifier = Modifier
                    .size(420.dp, 480.dp)
                    .shadow(elevation = 28.dp, shape = RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White),
            ) {
                Image(
                    bitmap = screenshot,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
