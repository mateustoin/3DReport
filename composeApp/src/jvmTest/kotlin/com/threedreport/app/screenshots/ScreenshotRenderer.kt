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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threedreport.app.App
import com.threedreport.app.AppContainer
import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.data.OrdersView
import com.threedreport.app.data.deletingStockWith
import com.threedreport.app.data.reconcilingStock
import com.threedreport.app.platform.AppLocaleState
import com.threedreport.app.platform.DesktopLocale
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.UiLanguage
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.renderCatalogPdf
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.testClientTexts
import com.threedreport.app.ui.about.LatestRelease
import com.threedreport.app.ui.about.ReleaseSource
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.GCodeDropOverlay
import com.threedreport.app.ui.quote.GCodeDropTarget
import com.threedreport.app.ui.quote.ModelPreview
import com.threedreport.app.ui.quote.PrintInput
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.app.ui.theme.DarkAppColors
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.update.DownloadOutcome
import com.threedreport.app.update.DownloadedUpdate
import com.threedreport.app.update.InstallMethod
import com.threedreport.app.update.InstallOutcome
import com.threedreport.app.update.NoUpdateInstaller
import com.threedreport.app.update.ReleaseAsset
import com.threedreport.app.update.UpdateFailure
import com.threedreport.app.update.UpdateInstaller
import com.threedreport.app.update.UpdatePlan
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuoteTemplate
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.awaitCancellation
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.EncodedImageFormat
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import java.awt.Color as AwtColor
import org.jetbrains.skia.Image as SkiaImage

// Tamanho de todo print de tela do site (decisão 120): 1280 é exatamente o SIDEBAR_LABELS_MIN_WINDOW_WIDTH,
// então a barra lateral sai com os rótulos, e não só os ícones.
internal const val WIDTH = 1280
internal const val HEIGHT = 800

// Janela estreita, abaixo do ponto em que as telas viram uma coluna (a barra lateral já só com ícones).
private const val NARROW_WIDTH = 820
private const val NARROW_HEIGHT = 1000

// "Hoje" fixo pros dados de exemplo terem datas plausíveis (prazos, histórico) sem depender de quando o
// gerador roda.
internal val TODAY: LocalDate = LocalDate.of(2026, 9, 26)
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
        // O idioma das capturas (`-Pscreenshots.lang=en`), fixo e não o desta máquina: sem a propriedade, português.
        val locale = screenshotLocale(System.getProperty("screenshots.lang"))
        DesktopLocale.use(locale)

        val outDir = File(outDirPath).apply { mkdirs() }
        // O banner das redes fica uma pasta acima das capturas em português (site/assets/og-banner.png); nas de outro
        // idioma, junto delas, pra nada fora da pasta pedida ser regravado.
        val ogBannerFile = if (locale == AppLocaleState.BRAZILIAN_PORTUGUESE) File(outDir.parentFile, "og-banner.png") else File(outDir, "og-banner.png")
        val iconFile = System.getProperty("screenshots.iconPath")?.let(::File)
        val gcodesDir = System.getProperty("screenshots.gcodes")?.let(::File)?.takeIf { it.isDirectory }

        // Pasta de dados própria, nunca a de verdade (~/.3dreport): ver TestRepositories.kt e o
        // comentário no topo de MultiPrintQuoteTest.kt — um repositório criado antes de apontar pra cá
        // gravaria na pasta real.
        val dataDir = createTempDirectory("3dreport-screenshots").toFile()
        seedSampleData(dataDir, gcodesDir)

        // O relógio do app fixo no "agora" dos dados de exemplo: o que as capturas gravam (movimentos do estoque) tem a data deles.
        val container = buildContainer(dataDir, clock = Clock { atDaysAgo(0, 12) })
        // `-Pscreenshots.only12=true`: só as capturas da Leva 12 (rápido, pra conferir a tela).
        if (System.getProperty("screenshots.only12") != null) {
            renderLeva12Screenshots(container, outDir)
            return
        }

        renderAppPng(File(outDir, "orcamento.png")) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm -> fillDraftQuote(vm) },
            )
        }

        // "Negociar" (decisão 131): o painel com o preço combinado aberto, e o preço, o lucro e o mínimo já mudados por ele.
        renderAppPng(File(outDir, "orcamento-negociar.png"), afterSettle = { it.clickByText(ST.negotiate) }) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm ->
                    fillDraftQuote(vm)
                    vm.setTargetTotal(sample("160", "45"))
                },
            )
        }

        // Preço combinado abaixo do mínimo do pedido: o aviso do painel (aviso, não bloqueio).
        val settingsBefore = container.settings.settings.value
        container.settings.update(settingsBefore.copy(minimumOrderPrice = sample(250.0, 70.0)))
        renderAppPng(File(outDir, "orcamento-abaixo-minimo.png"), afterSettle = { it.clickByText(ST.negotiate) }) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm ->
                    fillDraftQuote(vm)
                    vm.setTargetTotal(sample("160", "45"))
                },
            )
        }
        container.settings.update(settingsBefore)

        // O formulário rolado até o fim: as seções Venda e Detalhes.
        renderAppPng(File(outDir, "orcamento-venda.png"), afterSettle = { it.scrollAllToEnd() }) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm -> fillDraftQuote(vm) },
            )
        }

        // O diálogo de configurações de impressão de uma impressão.
        renderAppPng(File(outDir, "orcamento-configuracoes.png"), afterSettle = { it.clickByText(ST.printSettings) }) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm -> fillDraftQuote(vm) },
            )
        }

        // O mesmo Orçamento no tema escuro, pra conferir o tema novo nos dois.
        container.theme.update(ThemeMode.DARK)
        renderAppPng(File(outDir, "orcamento-escuro.png")) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm -> fillDraftQuote(vm) },
            )
        }
        // E o painel de negociação no escuro, onde o azul-marinho e o campo se confundem mais fácil.
        renderAppPng(File(outDir, "orcamento-negociar-escuro.png"), afterSettle = { it.clickByText(ST.negotiate) }) {
            sampleApp(
                container,
                startAt = AppDestination.QUOTE,
                prepareQuote = { vm ->
                    fillDraftQuote(vm)
                    vm.setTargetTotal(sample("160", "45"))
                },
            )
        }
        container.theme.update(ThemeMode.LIGHT)

        // Pedidos e Catálogo (Leva 10B-4), nos dois temas e nas duas visualizações. O "hoje" e o "agora" são os dos dados de
        // exemplo (ver [App]), pra o prazo e os entregues recentes não envelhecerem. A visualização é a preferência
        // lembrada: cada captura a grava antes de abrir, em vez de clicar no seletor.
        for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
            container.theme.update(theme)
            container.preferences.setOrdersView(OrdersView.KANBAN)
            renderAppPng(File(outDir, "pedidos$suffix.png")) { historyApp(container, AppDestination.ORDERS) }
            renderAppPng(File(outDir, "pedidos-detalhes$suffix.png"), afterSettle = { it.clickByText(Names.headset) }) {
                historyApp(container, AppDestination.ORDERS)
            }
            container.preferences.setOrdersView(OrdersView.LIST)
            renderAppPng(File(outDir, "pedidos-lista$suffix.png")) { historyApp(container, AppDestination.ORDERS) }
            container.preferences.setOrdersView(OrdersView.KANBAN)

            container.preferences.setCatalogView(CatalogView.GRID)
            renderAppPng(File(outDir, "catalogo$suffix.png")) { historyApp(container, AppDestination.CATALOG) }
            if (theme == ThemeMode.LIGHT) {
                // O menu de ações de um cartão aberto, e o modo de seleção que o "Exportar catálogo" liga.
                renderAppPng(File(outDir, "pedidos-menu.png"), afterSettle = { it.clickByDescription(ST.moreActions) }) {
                    historyApp(container, AppDestination.ORDERS)
                }
                renderAppPng(File(outDir, "catalogo-selecao.png"), afterSettle = { it.clickByText(ST.exportCatalog) }) {
                    historyApp(container, AppDestination.CATALOG)
                }
            }
            container.preferences.setCatalogView(CatalogView.LIST)
            renderAppPng(File(outDir, "catalogo-lista$suffix.png")) { historyApp(container, AppDestination.CATALOG) }
            container.preferences.setCatalogView(CatalogView.GRID)
        }
        container.theme.update(ThemeMode.LIGHT)

        // Os cadastros (Leva 10B-2), nos dois temas: Insumos com a lista (no escuro, com os arquivados abertos) e
        // Serviços com o painel de edição aberto.
        for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
            container.theme.update(theme)
            renderAppPng(
                File(outDir, "insumos$suffix.png"),
                afterSettle = { if (theme == ThemeMode.DARK) it.clickByText(ST.archived) },
            ) {
                sampleApp(container, startAt = AppDestination.CONSUMABLES)
            }
            renderAppPng(File(outDir, "servicos$suffix.png"), afterSettle = { it.clickByText(Names.assembly) }) {
                sampleApp(container, startAt = AppDestination.SERVICES)
            }
            // Filamentos e Impressoras: a lista (no escuro, com os arquivados abertos) e o painel de edição aberto.
            renderAppPng(
                File(outDir, "filamentos$suffix.png"),
                afterSettle = { if (theme == ThemeMode.DARK) it.clickByText(ST.archived) },
            ) {
                sampleApp(container, startAt = AppDestination.FILAMENTS)
            }
            renderAppPng(File(outDir, "filamentos-painel$suffix.png"), afterSettle = { it.clickByText(Names.PLA_FILAMENT) }) {
                sampleApp(container, startAt = AppDestination.FILAMENTS)
            }
            renderAppPng(
                File(outDir, "filamentos-cores$suffix.png"),
                afterSettle = {
                    it.clickByText(Names.PLA_FILAMENT)
                    settle(it, frames = 4)
                    it.scrollAllToEnd()
                },
            ) {
                sampleApp(container, startAt = AppDestination.FILAMENTS)
            }
            renderAppPng(
                File(outDir, "impressoras$suffix.png"),
                afterSettle = { if (theme == ThemeMode.DARK) it.clickByText(ST.archived) },
            ) {
                sampleApp(container, startAt = AppDestination.PRINTERS)
            }
            renderAppPng(File(outDir, "impressoras-painel$suffix.png"), afterSettle = { it.clickByText(Names.BAMBU) }) {
                sampleApp(container, startAt = AppDestination.PRINTERS)
            }
        }
        container.theme.update(ThemeMode.LIGHT)

        // Os diálogos de Impressoras: a manutenção e a lista pré-cadastrada.
        renderAppPng(File(outDir, "impressoras-manutencao.png"), afterSettle = { it.clickByDescription(ST.maintenance) }) {
            sampleApp(container, startAt = AppDestination.PRINTERS)
        }
        container.theme.update(ThemeMode.DARK)
        renderAppPng(File(outDir, "impressoras-manutencao-escuro.png"), afterSettle = { it.clickByDescription(ST.maintenance) }) {
            sampleApp(container, startAt = AppDestination.PRINTERS)
        }
        container.theme.update(ThemeMode.LIGHT)
        renderAppPng(File(outDir, "impressoras-lista.png"), afterSettle = { it.clickByText(ST.pickPrinter) }) {
            sampleApp(container, startAt = AppDestination.PRINTERS)
        }

        // Configurações, Sobre e os diálogos (Leva 10B-5). Cada seção de Configurações no claro, e Negócio e Canais também no
        // escuro. As duas seções longas saem de novo rolada até o fim.
        renderSettingsScreenshots(container, outDir)

        // O Dashboard (Leva 10B-6): com dados nos dois temas ("dashboard.png" é a captura pública), numa janela estreita,
        // e os dois estados vazios (sem vendas e nada salvo) nos dois temas.
        renderDashboardScreenshots(container, outDir, gcodesDir)
        renderDropOverlayScreenshots(outDir)
        renderNarrowSettingsScreenshots(container, outDir)

        val portaCelular = container.quoteHistory.savedQuotes.value.first { it.name == Names.phoneStand }
        val photoBytes = container.quoteHistory.photoBytes(portaCelular)
        val branding = container.branding.branding.value
        renderPdfComposite(portaCelular, photoBytes, branding, File(outDir, "pdf.png"))
        renderWhatsAppPng(portaCelular, photoBytes, branding, File(outDir, "whatsapp.png"))

        // Peça preta no fundo automático: a imagem escolhe o tom claro (whatsapp-claro.png). A miniatura do G-code sai cinza clara
        // qualquer que seja o filamento, então a peça é a mesma foto escurecida.
        renderWhatsAppPng(portaCelular, darkened(photoBytes), branding, File(outDir, "whatsapp-claro.png"))

        // O PDF com tudo ligado: várias peças, serviços por peça e por pedido, frete, entrega expressa, cliente, tempo de impressão,
        // validade e borda. O catálogo segue o mesmo estilo.
        val completo = portaCelular.copy(
            quote = portaCelular.quote.copy(quantity = 3, rush = true, salePrice = portaCelular.quote.salePrice * 3),
            services = listOf(
                QuoteService(id = "pintura", name = sample("Pintura", "Painting"), price = sample(5.0, 2.0), chargedPerOrder = false),
                QuoteService(id = "embalagem", name = sample("Embalagem", "Packaging"), price = sample(3.0, 1.0), chargedPerOrder = true),
            ),
            shippingCost = sample(12.0, 5.0),
        )
        val brandingCompleto = branding.copy(showBorder = true, showPrintTime = true, showClientName = true, quoteValidityDays = 7)
        renderPdfComposite(completo, photoBytes, brandingCompleto, File(outDir, "pdf-completo.png"))
        val produtos = container.quoteHistory.savedQuotes.value.filter { it.kind == QuoteKind.PRODUCT }
        renderCatalogComposite(produtos.map { QuoteExportItem(it, container.quoteHistory.photoBytes(it)) }, branding, File(outDir, "pdf-catalogo.png"))

        if (iconFile != null && iconFile.exists()) {
            renderOgBanner(iconFile, File(outDir, "orcamento.png"), ogBannerFile)
        }

        // O conjunto novo troca o histórico (agora "pedidos.png", com o Kanban da barra lateral).
        File(outDir, "historico.png").delete()

        if (System.getProperty("screenshots.leva10c") != null) renderLeva10cScreenshots(container, outDir)
        if (System.getProperty("screenshots.leva11") != null) renderLeva11Screenshots(container, outDir)
        if (System.getProperty("screenshots.leva12") != null) renderLeva12Screenshots(container, outDir)
        if (System.getProperty("screenshots.presets") != null) renderPrinterPresetScreenshots(container, outDir)
    }
}

/**
 * O idioma de `-Pscreenshots.lang` (decisão 157, item 25): "en" é inglês sem região (datas mês/dia, como en-US), "en-GB"
 * fixa a região; vazio ou ausente é português do Brasil, como sempre foram as capturas.
 */
internal fun screenshotLocale(tag: String?): AppLocaleState {
    if (tag.isNullOrBlank()) return AppLocaleState.BRAZILIAN_PORTUGUESE
    val locale = java.util.Locale.forLanguageTag(tag.trim())
    val language = UiLanguage.entries.firstOrNull { it.code == locale.language }
        ?: error("screenshots.lang=$tag: idioma sem textos no app (use ${UiLanguage.entries.joinToString { it.code }})")
    return AppLocaleState(language, locale.country.ifEmpty { null })
}

/** Impressoras da lista (2.2.1): o diálogo de escolha e o formulário que nasce de um preset, nos dois temas. */
private fun renderPrinterPresetScreenshots(container: AppContainer, outDir: File) {
    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "claro", ThemeMode.DARK to "escuro")) {
        container.theme.update(theme)
        renderAppPng(File(outDir, "impressoras-lista-$suffix.png"), afterSettle = { it.clickByText(ST.pickPrinter) }) {
            sampleApp(container, startAt = AppDestination.PRINTERS)
        }
        renderAppPng(
            File(outDir, "impressoras-formulario-preset-$suffix.png"),
            afterSettle = { scene ->
                scene.clickByText(ST.pickPrinter)
                settle(scene, frames = 3)
                scene.setTextByLabel(ST.presetSearch, "A1 Combo")
                settle(scene, frames = 3)
                scene.clickByText(Names.BAMBU_PRESET)
            },
        ) {
            sampleApp(container, startAt = AppDestination.PRINTERS)
        }
    }
    container.theme.update(ThemeMode.LIGHT)
}

/**
 * Os estados novos da Leva 10C: o arquivo do modelo com 3MF (miniatura) e com STL pesado (prévia simplificada) e a
 * janela de atualização (baixando e pronta pra instalar). Só com `-Pscreenshots.leva10c=true`: não entram no conjunto
 * do site.
 */
private fun renderLeva10cScreenshots(container: AppContainer, outDir: File) {
    container.theme.update(ThemeMode.LIGHT)
    val photo = File("src/commonMain/composeResources/files/preview_sample_photo.png").readBytes()

    // 3MF com a miniatura do fatiador: vira foto do orçamento e aparece no lugar da prévia 3D.
    renderAppPng(File(outDir, "orcamento-3mf.png"), afterSettle = { it.scrollAllToEnd() }) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.attachModelFile(PickedFile("${Names.monitorStand}.3mf", threeMfWithThumbnail(photo)))
            },
        )
    }

    // 3MF sem miniatura: o aviso curto, e o arquivo guardado do mesmo jeito.
    renderAppPng(File(outDir, "orcamento-3mf-sem-miniatura.png"), afterSettle = { it.scrollAllToEnd() }) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.attachModelFile(PickedFile("Projeto sem miniatura.3mf", threeMfWithThumbnail(null)))
            },
        )
    }

    // STL de 600 mil triângulos: a prévia é simplificada, e a nota diz que as medidas usam o arquivo completo.
    var heavyVm: QuoteViewModel? = null
    renderAppPng(
        File(outDir, "orcamento-stl-simplificado.png"),
        afterSettle = { scene ->
            val deadline = System.currentTimeMillis() + 120_000
            while (heavyVm?.uiState?.value?.modelPreview !is ModelPreview.Stl && System.currentTimeMillis() < deadline) {
                Thread.sleep(200)
                scene.render(System.nanoTime())
            }
            settle(scene, frames = 3)
            scene.scrollAllToEnd()
        },
    ) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                heavyVm = vm
                vm.attachModelFile(PickedFile("Esfera pesada.stl", binaryStlSphere(segments = 550)))
            },
        )
    }

    // Revisão de design: o bloco do arquivo sem arquivo, com STL carregando e com 3MF no escuro.
    renderAppPng(File(outDir, "orcamento-modelo-vazio.png"), afterSettle = { it.scrollAllToEnd() }) {
        sampleApp(container, startAt = AppDestination.QUOTE, prepareQuote = { vm -> fillDraftQuote(vm) })
    }
    renderAppPng(File(outDir, "orcamento-stl-carregando.png"), afterSettle = { it.scrollAllToEnd() }) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.attachModelFile(PickedFile("Esfera enorme.stl", binaryStlSphere(segments = 1200)))
            },
        )
    }
    container.theme.update(ThemeMode.DARK)
    renderAppPng(File(outDir, "orcamento-3mf-escuro.png"), afterSettle = { it.scrollAllToEnd() }) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.attachModelFile(PickedFile("${Names.monitorStand}.3mf", threeMfWithThumbnail(photo)))
            },
        )
    }
    container.theme.update(ThemeMode.LIGHT)

    // A janela de atualização: baixando (barra e Cancelar) e pronta pra instalar (o que vai acontecer no sistema).
    val release = LatestRelease("9.9.9", "https://example.com/novidades", listOf(ReleaseAsset("3dreport_9.9.9-1_amd64.deb", "https://example.com/a.deb", 90_000_000)))
    for ((name, installer) in listOf("baixando" to PreviewInstaller(downloading = true), "pronto" to PreviewInstaller(downloading = false))) {
        val dir = createTempDirectory("3dreport-screenshots-update-$name").toFile()
        LocalStorage(dir, Clock.System).onboarding().markCompleted()
        val updateContainer = buildContainer(dir, ReleaseSource { release }, installer).also { it.currency.update(sampleCurrency()) }
        updateContainer.preferences.update(updateContainer.preferences.preferences.value.copy(checkForUpdates = true))
        for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
            updateContainer.theme.update(theme)
            renderAppPng(File(outDir, "atualizacao-$name$suffix.png"), afterSettle = { it.clickByText(ST.updateNow) }) {
                sampleApp(updateContainer, startAt = AppDestination.ABOUT)
            }
        }
    }

    // Erro de rede, Linux sem pkexec (comando pra copiar) e orçamento não salvo antes de instalar.
    data class State(val name: String, val installer: PreviewInstaller, val steps: List<String>, val draft: Boolean = false)
    for (state in listOf(
        State("erro", PreviewInstaller(downloading = false, fails = true), listOf(ST.updateNow)),
        State("manual", PreviewInstaller(downloading = false, manual = true), listOf(ST.updateNow, ST.openFolder)),
        State("nao-salvo", PreviewInstaller(downloading = false), listOf(ST.updateNow, ST.installAndClose), draft = true),
    )) {
        val dir = createTempDirectory("3dreport-screenshots-update-${state.name}").toFile()
        LocalStorage(dir, Clock.System).onboarding().markCompleted()
        val updateContainer = buildContainer(dir, ReleaseSource { release }, state.installer).also { it.currency.update(sampleCurrency()) }
        updateContainer.preferences.update(updateContainer.preferences.preferences.value.copy(checkForUpdates = true))
        for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
            updateContainer.theme.update(theme)
            renderAppPng(
                File(outDir, "atualizacao-${state.name}$suffix.png"),
                afterSettle = { scene -> state.steps.forEach { step -> scene.clickByText(step); settle(scene, frames = 4) } },
            ) {
                sampleApp(updateContainer, startAt = AppDestination.ABOUT, prepareQuote = { vm -> if (state.draft) fillDraftQuote(vm) })
            }
        }
    }
}

/** Um instalador de mentira pras capturas: ou fica baixando a 42%, ou entrega o arquivo na hora. */
private class PreviewInstaller(
    private val downloading: Boolean,
    private val fails: Boolean = false,
    private val manual: Boolean = false,
) : UpdateInstaller {
    override fun plan(release: LatestRelease) =
        UpdatePlan(release, release.assets.first(), if (manual) InstallMethod.LINUX_DEB_MANUAL else InstallMethod.LINUX_DEB)

    override suspend fun download(plan: UpdatePlan, onProgress: (Float) -> Unit): DownloadOutcome {
        if (downloading) {
            onProgress(0.42f)
            awaitCancellation()
        }
        if (fails) return DownloadOutcome.Failed(UpdateFailure.NETWORK)
        return DownloadOutcome.Ready(DownloadedUpdate(plan, "/tmp/${plan.asset.name}"))
    }

    override suspend fun install(update: DownloadedUpdate): InstallOutcome =
        if (manual) InstallOutcome.Manual("/tmp", "sudo apt-get install /tmp/${update.plan.asset.name}") else InstallOutcome.CloseApp

    override fun discard(update: DownloadedUpdate) = Unit
}

/** Um 3MF mínimo no formato do Bambu Studio; com [thumbnail] nulo, sem a imagem. */
private fun threeMfWithThumbnail(thumbnail: ByteArray?): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    java.util.zip.ZipOutputStream(out).use { zip ->
        fun entry(name: String, bytes: ByteArray) {
            zip.putNextEntry(java.util.zip.ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        entry("[Content_Types].xml", "<Types/>".encodeToByteArray())
        entry("3D/3dmodel.model", "<model/>".encodeToByteArray())
        if (thumbnail != null) {
            entry(
                "_rels/.rels",
                """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Target="/Metadata/plate_1.png" Id="rel-2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"/></Relationships>""".encodeToByteArray(),
            )
            entry("Metadata/plate_1.png", thumbnail)
        }
    }
    return out.toByteArray()
}

/** Uma esfera em STL binário de uns 2 x [segments] x [segments] triângulos, pra passar do limite da prévia completa. */
private fun binaryStlSphere(segments: Int): ByteArray {
    val triangles = 2 * segments * (segments - 1)
    val buffer = java.nio.ByteBuffer.allocate(84 + 50 * triangles).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    buffer.position(80)
    buffer.putInt(triangles)
    fun point(lat: Int, lon: Int): FloatArray {
        val theta = Math.PI * lat / segments
        val phi = 2 * Math.PI * (lon % segments) / segments
        return floatArrayOf((30 * Math.sin(theta) * Math.cos(phi)).toFloat(), (30 * Math.sin(theta) * Math.sin(phi)).toFloat(), (30 * Math.cos(theta)).toFloat())
    }
    fun put(a: FloatArray, b: FloatArray, c: FloatArray) {
        repeat(3) { buffer.putFloat(0f) }
        for (v in listOf(a, b, c)) v.forEach { buffer.putFloat(it) }
        buffer.putShort(0)
    }
    for (lat in 0 until segments) {
        for (lon in 0 until segments) {
            val p00 = point(lat, lon)
            val p01 = point(lat, lon + 1)
            val p10 = point(lat + 1, lon)
            val p11 = point(lat + 1, lon + 1)
            if (lat > 0) put(p00, p10, p01)
            if (lat < segments - 1) put(p01, p10, p11)
        }
    }
    return buffer.array()
}

/**
 * Configurações, Sobre, o primeiro uso e os diálogos da leva 10B-5. Os cliques e as digitações vão pela árvore de
 * semântica ([clickByText], [setTextByLabel]), sem coordenadas; nada disso grava nos dados de exemplo, a não ser os canais e
 * os modelos que as capturas precisam ter e que ficam só nesta pasta temporária.
 */
private fun renderSettingsScreenshots(container: AppContainer, outDir: File) {
    val sections = listOf(ST.sectionBusiness to "negocio", ST.sectionChannels to "canais", ST.sectionDocuments to "documentos", ST.sectionAppearance to "aparencia", ST.sectionBackup to "backup")
    // A lista de modelos ainda vazia (o EmptyState do diálogo), antes de os modelos de exemplo entrarem.
    renderAppPng(
        File(outDir, "configuracoes-modelos-vazio.png"),
        afterSettle = {
            it.clickByText(ST.sectionDocuments)
            settle(it, frames = 3)
            it.clickByText(ST.templateList)
        },
    ) {
        sampleApp(container, startAt = AppDestination.SETTINGS)
    }
    // Um canal com faixas de preço e dois modelos, pra o diálogo do canal e a lista de modelos terem o que mostrar.
    val shopee = container.salesChannels.channels.value.first { it.id == Catalog.SHOPEE_ID }
    // "Simples" primeiro: é diferente do que está gravado, então o "Carregar" da primeira linha muda o rascunho.
    container.templates.add(QuoteTemplate(id = "tpl-simples", name = sample("Simples", "Simple"), brandName = null, showWatermark = false, showFooter = true, showBorder = true))
    container.templates.add(QuoteTemplate(id = "tpl-formal", name = "Formal", brandName = Names.shopName, showWatermark = true, showFooter = true))

    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
        container.theme.update(theme)
        for ((title, slug) in sections) {
            renderAppPng(File(outDir, "configuracoes-$slug$suffix.png"), afterSettle = { if (slug != "negocio") it.clickByText(title) }) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
        }
        // Uma alteração por salvar: a barra embaixo e o "Não salvo" na lista.
        renderAppPng(
            File(outDir, "configuracoes-negocio-alterado$suffix.png"),
            afterSettle = { it.setTextByLabel(ST.marginField, "150") },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        if (theme == ThemeMode.LIGHT) {
            // O erro de cada campo, depois de "Salvar".
            renderAppPng(
                File(outDir, "configuracoes-negocio-erro.png"),
                afterSettle = {
                    it.setTextByLabel(ST.laborField, "-5")
                    it.setTextByLabel(ST.failureField, "abc")
                    settle(it, frames = 3)
                    it.clickByText(ST.save)
                },
            ) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
            renderAppPng(File(outDir, "configuracoes-negocio-fim.png"), afterSettle = { it.scrollAllToEnd() }) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
            renderAppPng(
                File(outDir, "configuracoes-documentos-fim.png"),
                afterSettle = {
                    it.clickByText(ST.sectionDocuments)
                    settle(it, frames = 3)
                    it.scrollAllToEnd()
                },
            ) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
        }

        // O formulário de um canal com faixas de preço (a tabela do kit) e a tarifa de energia.
        container.salesChannels.update(
            shopee.copy(
                feeRate = 0.0,
                tiers = listOf(
                    ChannelFeeTier(upToUnitPrice = sample(8.0, 10.0), feeRate = sample(0.20, 0.065), fixedFeePerItem = sample(4.0, 0.20)),
                    ChannelFeeTier(upToUnitPrice = sample(80.0, 40.0), feeRate = sample(0.14, 0.065), fixedFeePerItem = sample(16.0, 0.40)),
                    ChannelFeeTier(upToUnitPrice = null, feeRate = sample(0.14, 0.065), fixedFeePerItem = sample(26.0, 0.80)),
                ),
            ),
        )
        renderAppPng(
            File(outDir, "configuracoes-canal$suffix.png"),
            afterSettle = {
                it.clickByText(ST.sectionChannels)
                settle(it, frames = 3)
                it.clickByText(Names.marketplace)
            },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        container.salesChannels.update(shopee)
        renderAppPng(File(outDir, "configuracoes-tarifa$suffix.png"), afterSettle = { it.clickByText(ST.energyCalc) }) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        renderAppPng(
            File(outDir, "configuracoes-tarifa-branca$suffix.png"),
            afterSettle = {
                it.clickByText(ST.energyCalc)
                settle(it, frames = 3)
                it.clickByText(ST.whiteTariff)
            },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        renderAppPng(
            File(outDir, "configuracoes-tarifa-resultado$suffix.png"),
            afterSettle = {
                it.clickByText(ST.energyCalc)
                settle(it, frames = 3)
                it.setTextByLabel(ST.billTotal, "230")
                it.setTextByLabel(ST.billKwh, "250")
            },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        renderAppPng(
            File(outDir, "configuracoes-templates$suffix.png"),
            afterSettle = {
                it.clickByText(ST.sectionDocuments)
                settle(it, frames = 3)
                it.clickByText(ST.templateList)
            },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        // "Carregar" põe o modelo no rascunho: a barra de alterações aparece e nada foi gravado.
        renderAppPng(
            File(outDir, "configuracoes-modelo-carregado$suffix.png"),
            afterSettle = {
                it.clickByText(ST.sectionDocuments)
                settle(it, frames = 3)
                it.clickByText(ST.templateList)
                settle(it, frames = 3)
                it.clickByDescription(ST.load)
            },
        ) {
            sampleApp(container, startAt = AppDestination.SETTINGS)
        }
        if (theme == ThemeMode.LIGHT) {
            renderAppPng(
                File(outDir, "configuracoes-template-salvar.png"),
                afterSettle = {
                    it.clickByText(ST.sectionDocuments)
                    settle(it, frames = 3)
                    it.clickByText(ST.saveTemplate)
                },
            ) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
            renderAppPng(
                File(outDir, "configuracoes-documentos-previa.png"),
                afterSettle = {
                    it.clickByText(ST.sectionDocuments)
                    settle(it, frames = 3)
                    it.clickByText(ST.previewPdf)
                },
            ) {
                sampleApp(container, startAt = AppDestination.SETTINGS)
            }
        }

        renderAppPng(File(outDir, "sobre$suffix.png")) { sampleApp(container, startAt = AppDestination.ABOUT) }
    }
    container.theme.update(ThemeMode.LIGHT)

    // O Sobre com uma versão nova disponível (a verificação ligada e uma fonte que responde com uma versão à frente).
    val updatedDir = createTempDirectory("3dreport-screenshots-update").toFile()
    LocalStorage(updatedDir, Clock.System).onboarding().markCompleted()
    val updatedContainer = buildContainer(updatedDir, ReleaseSource { LatestRelease("9.9.9", "https://example.com/novidades") })
    updatedContainer.preferences.update(updatedContainer.preferences.preferences.value.copy(checkForUpdates = true))
    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
        updatedContainer.theme.update(theme)
        renderAppPng(File(outDir, "sobre-atualizacao$suffix.png")) { sampleApp(updatedContainer, startAt = AppDestination.ABOUT) }
    }

    // O primeiro uso, numa pasta de dados vazia (onde o onboarding ainda não foi feito): os dois passos, no claro e no escuro.
    val emptyContainer = buildContainer(createTempDirectory("3dreport-screenshots-onboarding").toFile())
    emptyContainer.currency.update(sampleCurrency())
    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
        emptyContainer.theme.update(theme)
        renderAppPng(File(outDir, "primeiro-uso$suffix.png")) { sampleApp(emptyContainer, startAt = AppDestination.QUOTE) }
        renderAppPng(File(outDir, "primeiro-uso-custos$suffix.png"), afterSettle = { it.clickByText(ST.next) }) {
            sampleApp(emptyContainer, startAt = AppDestination.QUOTE)
        }
    }
}

/**
 * O Dashboard e os estados que ele tem: com dados (claro e escuro, e numa janela estreita, onde as colunas viram uma),
 * "nenhuma venda" (só orçamentos em aberto e o catálogo) e "nada salvo". Os dois últimos usam pastas de dados à parte,
 * pra não mexer nos dados de exemplo das outras capturas.
 */
private fun renderDashboardScreenshots(container: AppContainer, outDir: File, gcodesDir: File?) {
    // Sem venda: os dados de exemplo sem os pedidos que já foram aprovados; ficam os orçamentos em aberto e o catálogo.
    val noSalesDir = createTempDirectory("3dreport-screenshots-dashboard").toFile()
    seedSampleData(noSalesDir, gcodesDir)
    val noSales = buildContainer(noSalesDir)
    noSales.quoteHistory.savedQuotes.value.filter { it.isOrder && !it.status.isOpen }.forEach { noSales.quoteHistory.delete(it.id) }

    // Nada salvo: uma pasta sem nada, com o primeiro uso já feito (senão o app abriria o onboarding).
    val emptyDir = createTempDirectory("3dreport-screenshots-dashboard-empty").toFile()
    LocalStorage(emptyDir, Clock.System).onboarding().markCompleted()
    val empty = buildContainer(emptyDir)

    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
        for (target in listOf(container, noSales, empty)) target.theme.update(theme)
        renderAppPng(File(outDir, "dashboard$suffix.png")) { sampleApp(container, startAt = AppDestination.DASHBOARD) }
        renderAppPng(File(outDir, "dashboard-sem-vendas$suffix.png")) { sampleApp(noSales, startAt = AppDestination.DASHBOARD) }
        renderAppPng(File(outDir, "dashboard-vazio$suffix.png")) { sampleApp(empty, startAt = AppDestination.DASHBOARD) }
    }
    container.theme.update(ThemeMode.LIGHT)
    renderAppPng(File(outDir, "dashboard-estreito.png"), width = NARROW_WIDTH, height = NARROW_HEIGHT) {
        sampleApp(container, startAt = AppDestination.DASHBOARD)
    }

    // Prejuízo: uma única venda fechada por menos do que custou, pra o lucro sair em vermelho.
    val lossDir = createTempDirectory("3dreport-screenshots-dashboard-loss").toFile()
    seedSampleData(lossDir, gcodesDir)
    val loss = buildContainer(lossDir)
    val sample = loss.quoteHistory.savedQuotes.value.first { it.isOrder && it.status.isSold }
    loss.quoteHistory.savedQuotes.value.filter { it.isOrder && !it.status.isOpen }.forEach { loss.quoteHistory.delete(it.id) }
    val sold = loss.quoteHistory.save(name = sample("Pedido com prejuízo", "Order at a loss"), quote = sample.quote.copy(salePrice = 1.0), services = emptyList(), photo = null, sourceLink = null)
    loss.quoteHistory.updateStatus(sold.id, OrderStatus.APROVADO)
    renderAppPng(File(outDir, "dashboard-prejuizo.png")) { sampleApp(loss, startAt = AppDestination.DASHBOARD) }
}

/**
 * O aviso de arrastar G-code, nas duas formas (uma zona só; uma zona por impressão mais "Adicionar"), nos dois temas.
 * O estado de arrasto é do `App` (não há como forçá-lo de fora), então o aviso é renderizado sozinho, sobre o fundo do
 * tema, com uma das zonas "em cima" pra mostrar o destaque.
 */
private fun renderDropOverlayScreenshots(outDir: File) {
    val filled = PrintInput(name = Names.monitorStand, printTimeText = "190", id = 1)
    val another = PrintInput(name = "", printTimeText = "45", id = 2)
    for ((theme, suffix) in listOf(ThemeMode.LIGHT to "", ThemeMode.DARK to "-escuro")) {
        renderAppPng(File(outDir, "arrastar-gcode-vazio$suffix.png")) {
            AppTheme(theme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    GCodeDropOverlay(prints = listOf(PrintInput(id = 1)), hovered = { null }, onZonePlaced = { _, _ -> }, onDispose = {})
                }
            }
        }
        renderAppPng(File(outDir, "arrastar-gcode-zonas$suffix.png")) {
            AppTheme(theme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    GCodeDropOverlay(
                        prints = listOf(filled, another),
                        hovered = { GCodeDropTarget.Replace(2) },
                        onZonePlaced = { _, _ -> },
                        onDispose = {},
                    )
                }
            }
        }
    }
}

/** Configurações numa janela estreita (900x700): a lista de seções de 264 ao lado do conteúdo. */
private fun renderNarrowSettingsScreenshots(container: AppContainer, outDir: File) {
    container.theme.update(ThemeMode.LIGHT)
    renderAppPng(File(outDir, "configuracoes-negocio-janela-estreita.png"), width = 900, height = 700) {
        sampleApp(container, startAt = AppDestination.SETTINGS)
    }
    renderAppPng(
        File(outDir, "configuracoes-documentos-janela-estreita.png"),
        afterSettle = { it.clickByText(ST.sectionDocuments) },
        width = 900,
        height = 700,
    ) {
        sampleApp(container, startAt = AppDestination.SETTINGS)
    }
}

/** Pedidos ou Catálogo com o "hoje" e o "agora" dos dados de exemplo. */
@Composable
private fun historyApp(container: AppContainer, startAt: AppDestination) {
    sampleApp(container, startAt = startAt)
}

/**
 * O [App] com o "hoje" e o "agora" dos dados de exemplo em todas as telas (Orçamento, Pedidos, Catálogo...): nenhuma
 * captura depende do dia em que é gerada.
 */
@Composable
internal fun sampleApp(container: AppContainer, startAt: AppDestination, prepareQuote: (QuoteViewModel) -> Unit = {}) {
    App(container, startAt = startAt, prepareQuote = prepareQuote, today = { TODAY.toEpochDay() }, clock = { atDaysAgo(0, 12) })
}

/**
 * Ids fixos dos cadastros de exemplo: escolhidos aqui (como um cadastro de verdade escolheria um id
 * gerado), usados tanto pra criar o catálogo em [seedSampleData] quanto pra montar os pedidos de
 * exemplo e o rascunho do print do Orçamento ([fillDraftQuote]).
 */
internal object Catalog {
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
internal fun fillDraftQuote(vm: QuoteViewModel) {
    vm.setPrintName(sample("Cabeça e corpo", "Head and body"))
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
    vm.setServicePrice(Catalog.PINTURA_ID, sample("35", "10"))
    vm.selectSalesChannel(Catalog.SHOPEE_ID)
    vm.setSaveName(sample("Action figure (cabeça, corpo e base)", "Action figure (head, body and base)"))
    vm.setClientName(sample("Larissa M.", "Emma R."))
    vm.setDeliveryDate(deliveryEpochDay(5))
}

// ---------------------------------------------------------------------------------------------
// Dados de exemplo
// ---------------------------------------------------------------------------------------------

/** Relógio controlável: cada pedido de exemplo nasce e muda de andamento numa data diferente. */
private class SeedClock(var millis: Long) : Clock {
    override fun nowMillis(): Long = millis
}

internal fun atDaysAgo(days: Int, hour: Int = 10): Long =
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
    val lengthMeters: String = "",
    val printTimeText: String = "",
    /** G-code de onde vêm as medidas e a miniatura (a foto do produto); sem ele, o produto fica sem foto. */
    val gcodeFile: String? = null,
)

private fun orderSeeds() = listOf(
    OrderSeed(
        name = Names.painted,
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.BAMBU_ID,
        lengthMeters = "18", printTimeText = "1h50",
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = sample("Rafael N.", "Noah B."), clientContact = sample("rafael.n@example.com", "noah.b@example.com"),
        servicePrice = sample("20", "6"), createdDaysAgo = 1, deliveryDaysFromToday = 6,
    ),
    OrderSeed(
        name = Names.vaseOrder,
        filamentId = Catalog.PETG_ID, colorId = Catalog.COR_NATURAL, printerId = Catalog.K1_ID,
        lengthMeters = "85", printTimeText = "8h10",
        channelId = Catalog.PIX_ID, clientName = sample("Camila F.", "Olivia K."), clientContact = sample("camila.f@example.com", "olivia.k@example.com"),
        createdDaysAgo = 2, deliveryDaysFromToday = 10,
    ),
    OrderSeed(
        name = Names.headset,
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_PRETO, printerId = Catalog.BAMBU_ID,
        lengthMeters = "52", printTimeText = "5h",
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = sample("Juliana T.", "Sophia L."), clientContact = sample("juliana.t@example.com", "sophia.l@example.com"),
        createdDaysAgo = 4, statusSteps = listOf(OrderStatus.APROVADO to 1), deliveryDaysFromToday = 5,
    ),
    OrderSeed(
        name = Names.keychainOrder,
        gcodeFile = "Articulated_Keychains_PLA_4h57m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_PRETO, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.PIX_ID, clientName = sample("Carla S.", "Mia H."), clientContact = sample("carla.s@example.com", "mia.h@example.com"),
        createdDaysAgo = 6, statusSteps = listOf(OrderStatus.APROVADO to 1), deliveryDaysFromToday = 3,
    ),
    OrderSeed(
        name = Names.phoneStand,
        gcodeFile = "smartphone_holder_PLA_1h15m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.K1_ID,
        channelId = Catalog.SHOPEE_ID, clientName = sample("Ana P.", "Ava P."), clientContact = sample("ana.p@example.com", "ava.p@example.com"),
        createdDaysAgo = 8, statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 6),
        deliveryDaysFromToday = 1,
    ),
    OrderSeed(
        name = Names.axolotl,
        gcodeFile = "articulated_axolotl_2_PLA_2h16m.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.MERCADO_LIVRE_ID, clientName = sample("Bruno M.", "Liam M."), clientContact = sample("bruno.m@example.com", "liam.m@example.com"),
        createdDaysAgo = 12, statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 11),
        deliveryDaysFromToday = 2,
    ),
    OrderSeed(
        name = Names.organizerOrder,
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.K1_ID,
        lengthMeters = "42", printTimeText = "3h40",
        channelId = Catalog.PIX_ID, clientName = sample("Marcos L.", "Lucas W."), clientContact = sample("marcos.l@example.com", "lucas.w@example.com"),
        createdDaysAgo = 9,
        statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 8),
        deliveryDaysFromToday = 0,
    ),
    OrderSeed(
        name = Names.lampOrder,
        filamentId = Catalog.PETG_ID, colorId = Catalog.COR_NATURAL, printerId = Catalog.K1_ID,
        lengthMeters = "68", printTimeText = "6h30",
        channelId = Catalog.SHOPEE_ID, clientName = sample("Patrícia G.", "Grace T."), clientContact = sample("patricia.g@example.com", "grace.t@example.com"),
        createdDaysAgo = 18,
        statusSteps = listOf(OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 15),
        deliveryDaysFromToday = -2,
    ),
    OrderSeed(
        name = Names.boat,
        gcodeFile = "3DBenchy_PLA_31m44s.gcode",
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_BRANCO, printerId = Catalog.BAMBU_ID,
        channelId = Catalog.SHOPEE_ID, clientName = sample("Diego A.", "Ethan A."), clientContact = sample("diego.a@example.com", "ethan.a@example.com"),
        createdDaysAgo = 22,
        statusSteps = listOf(
            OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 12, OrderStatus.ENTREGUE to 20,
        ),
        deliveryDaysFromToday = -2,
    ),
    OrderSeed(
        name = Names.gift,
        filamentId = Catalog.PLA_ID, colorId = Catalog.COR_CINZA, printerId = Catalog.K1_ID,
        lengthMeters = "33", printTimeText = "3h",
        channelId = Catalog.PIX_ID, clientName = sample("Fernanda R.", "Chloe R."), clientContact = sample("fernanda.r@example.com", "chloe.r@example.com"),
        servicePrice = sample("15", "5"), createdDaysAgo = 25,
        statusSteps = listOf(
            OrderStatus.APROVADO to 1, OrderStatus.EM_IMPRESSAO to 4, OrderStatus.PRONTO to 10, OrderStatus.ENTREGUE to 23,
        ),
        deliveryDaysFromToday = -2,
    ),
)

private fun productSeeds() = listOf(
    ProductSeed(sample("Vaso geométrico", "Geometric vase"), sample("Decoração", "Decor"), Catalog.PETG_ID, Catalog.COR_NATURAL, Catalog.BAMBU_ID, "60", "5h30"),
    ProductSeed(sample("Organizador de mesa", "Desk organizer"), sample("Casa", "Home"), Catalog.PLA_ID, Catalog.COR_BRANCO, Catalog.K1_ID, "45", "4h"),
    ProductSeed(sample("Luminária lua", "Moon lamp"), sample("Decoração", "Decor"), Catalog.PETG_ID, Catalog.COR_NATURAL, Catalog.K1_ID, "70", "7h"),
    ProductSeed(
        sample("Chaveiro geométrico", "Geometric keychain"), sample("Chaveiros", "Keychains"), Catalog.PLA_ID, Catalog.COR_PRETO, Catalog.BAMBU_ID,
        gcodeFile = "Articulated_Keychains_PLA_4h57m.gcode",
    ),
    ProductSeed(
        sample("Suporte de fone (over-ear)", "Headset stand"), sample("Acessórios", "Accessories"), Catalog.PLA_ID, Catalog.COR_BRANCO, Catalog.BAMBU_ID,
        gcodeFile = "smartphone_holder_PLA_1h15m.gcode",
    ),
    ProductSeed(
        sample("Dragão articulado", "Articulated dragon"), sample("Colecionáveis", "Collectibles"), Catalog.PLA_ID, Catalog.COR_CINZA, Catalog.K1_ID,
        gcodeFile = "articulated_axolotl_2_PLA_2h16m.gcode",
    ),
)

/** Monta o catálogo e o histórico de exemplo, com dados fictícios brasileiros, na pasta [dataDir]. */
internal fun seedSampleData(dataDir: File, gcodesDir: File?) {
    val clock = SeedClock(atDaysAgo(30))
    val storage = LocalStorage(dataDir, clock)

    val filamentRepo = storage.filaments()
    filamentRepo.items.value.forEach { filamentRepo.delete(it.id) } // tira o seed de fábrica (PLA/ABS/PETG genéricos)
    filamentRepo.add(
        Filament(
            id = Catalog.PLA_ID,
            name = "PLA Voolt3D",
            pricePerKg = sample(89.90, 24.90),
            densityGPerCm3 = 1.24,
            materialType = "PLA",
            brand = "Voolt3D",
            colors = listOf(
                FilamentColor(id = Catalog.COR_BRANCO, name = sample("Branco", "White"), hex = "#F5F5F5"),
                FilamentColor(id = Catalog.COR_PRETO, name = sample("Preto", "Black"), hex = "#161616"),
                FilamentColor(id = Catalog.COR_CINZA, name = sample("Cinza", "Gray"), hex = "#9E9E9E"),
            ),
        ),
    )
    filamentRepo.add(
        Filament(
            id = Catalog.PETG_ID,
            name = "PETG Voolt3D",
            pricePerKg = sample(118.0, 29.90),
            densityGPerCm3 = 1.27,
            materialType = "PETG",
            brand = "Voolt3D",
            colors = listOf(FilamentColor(id = Catalog.COR_NATURAL, name = "Natural", hex = "#F0EDE4")),
        ),
    )

    filamentRepo.add(
        Filament(
            id = "filamento-abs",
            name = "ABS Voolt3D",
            pricePerKg = sample(99.0, 27.0),
            densityGPerCm3 = 1.04,
            materialType = "ABS",
            brand = "Voolt3D",
            colors = listOf(FilamentColor(id = "abs-preto", name = sample("Preto", "Black"), hex = "#161616")),
            archived = true,
        ),
    )

    val printerRepo = storage.printers()
    printerRepo.items.value.forEach { printerRepo.delete(it.id) } // tira a "Minha impressora" de fábrica
    printerRepo.add(
        PrinterProfile(
            id = Catalog.BAMBU_ID,
            name = "Bambu Lab A1",
            printerPowerWatts = 1300.0,
            maintenanceCostPerHour = sample(0.15, 0.04),
            machineInvestment = MachineInvestment(machinePrice = sample(2200.0, 399.0), paybackMonths = 12, printingDaysPerMonth = 24, printingHoursPerDay = 10.0),
        ),
    )
    printerRepo.add(
        PrinterProfile(
            id = "printer-ender",
            name = "Ender 3 V2",
            printerPowerWatts = 350.0,
            maintenanceCostPerHour = sample(0.10, 0.03),
            machineInvestment = MachineInvestment(machinePrice = sample(1100.0, 229.0), paybackMonths = 12, printingDaysPerMonth = 20, printingHoursPerDay = 8.0),
            archived = true,
        ),
    )
    printerRepo.add(
        PrinterProfile(
            id = Catalog.K1_ID,
            name = "Creality K1",
            printerPowerWatts = 350.0,
            maintenanceCostPerHour = sample(0.20, 0.05),
            machineInvestment = MachineInvestment(machinePrice = sample(2600.0, 499.0), paybackMonths = 12, printingDaysPerMonth = 24, printingHoursPerDay = 10.0),
        ),
    )

    // A manutenção de cada impressora: o contador de cada componente já corre, pra lista mostrar o que está perto.
    val maintenanceRepo = storage.maintenance()
    maintenanceRepo.addComponent(MaintenanceComponent(id = "comp-bico", printerId = Catalog.BAMBU_ID, name = sample("Bico", "Nozzle"), intervalHours = 100.0, hoursAtLastService = -92.0))
    maintenanceRepo.addComponent(MaintenanceComponent(id = "comp-correias", printerId = Catalog.BAMBU_ID, name = sample("Correias", "Belts"), intervalHours = 300.0, hoursAtLastService = -40.0))
    maintenanceRepo.addComponent(MaintenanceComponent(id = "comp-mesa", printerId = Catalog.K1_ID, name = sample("Limpeza da mesa", "Bed cleaning"), intervalHours = 200.0, hoursAtLastService = -20.0))

    val serviceRepo = storage.services()
    serviceRepo.add(Service(id = Catalog.PINTURA_ID, name = sample("Pintura", "Painting"), suggestedPrice = sample(25.0, 8.0), chargedPerOrder = false))
    serviceRepo.add(Service(id = "service-lixamento", name = sample("Lixamento", "Sanding"), suggestedPrice = sample(12.0, 4.0), laborMinutes = 20.0))
    serviceRepo.add(Service(id = "service-montagem", name = Names.assembly, suggestedPrice = sample(18.0, 6.0), laborMinutes = 30.0))
    serviceRepo.add(Service(id = "service-modelagem", name = sample("Modelagem sob medida", "Custom modeling"), chargedPerOrder = true, laborMinutes = 90.0))
    serviceRepo.add(Service(id = "service-verniz", name = sample("Acabamento com verniz", "Varnish finish"), suggestedPrice = sample(15.0, 5.0), archived = true))

    val consumableRepo = storage.consumables()
    consumableRepo.add(Consumable(id = "insumo-argola", name = sample("Argola de chaveiro", "Keyring"), unitCost = sample(0.35, 0.08)))
    consumableRepo.add(Consumable(id = "insumo-ima", name = sample("Ímã de neodímio 8 mm", "Neodymium magnet 8 mm"), unitCost = sample(1.2, 0.25)))
    consumableRepo.add(Consumable(id = "insumo-parafuso", name = sample("Parafuso M3", "M3 screw"), unitCost = sample(0.15, 0.03)))
    consumableRepo.add(Consumable(id = "insumo-caixa", name = sample("Caixa de papelão", "Cardboard box"), unitCost = sample(2.8, 0.9), chargedPerOrder = true))
    consumableRepo.add(Consumable(id = "insumo-bolha", name = sample("Embalagem com plástico bolha", "Bubble wrap packaging"), unitCost = sample(1.5, 0.5), chargedPerOrder = true))
    consumableRepo.add(Consumable(id = "insumo-saquinho", name = sample("Saquinho zip", "Zip bag"), unitCost = sample(0.25, 0.05), archived = true))

    val channelRepo = storage.salesChannels()
    channelRepo.add(SalesChannel(id = Catalog.SHOPEE_ID, name = Names.marketplace, feeRate = sample(0.20, 0.10)))
    channelRepo.add(SalesChannel(id = Catalog.MERCADO_LIVRE_ID, name = sample("Mercado Livre", "eBay"), feeRate = sample(0.16, 0.13)))
    channelRepo.add(SalesChannel(id = Catalog.PIX_ID, name = sample("Pix", "Amazon"), feeRate = sample(0.0, 0.15)))

    storage.settings().update(
        PricingSettings(
            energyPricePerKwh = sample(0.85, 0.16),
            failureRate = 0.08,
            finishingRate = 0.05,
            laborRatePerHour = sample(25.0, 20.0),
            monthlyFixedCost = sample(200.0, 40.0),
            productiveHoursPerMonth = 150.0,
            taxRate = 0.06,
            profitMargin = 1.0,
        ),
    )
    storage.branding().update(
        BrandingSettings(
            brandName = Names.shopName,
            showWatermark = true,
            showFooter = true,
            contactEmail = sample("contato@example.com", "hello@example.com"),
            contactInstagram = Names.shopHandle,
            quoteValidityDays = 7,
        ),
    )
    storage.theme().update(ThemeMode.LIGHT)
    storage.currency().update(sampleCurrency())
    storage.onboarding().markCompleted()

    val clientRepo = storage.clients()
    val history = storage.quoteHistory()
    val settingsRepo = storage.settings()

    fun newViewModel() = QuoteViewModel(filamentRepo, printerRepo, settingsRepo, serviceRepo, channelRepo, history, clientRepository = clientRepo, currency = { sampleCurrency() })

    for (spec in orderSeeds()) {
        clock.millis = atDaysAgo(spec.createdDaysAgo)
        val vm = newViewModel()
        val gcode = spec.gcodeFile?.let { fileName -> gcodesDir?.let { File(it, fileName) } }?.takeIf { it.exists() }
        if (gcode != null) vm.importGCode(PickedFile(gcode.name, gcode.readBytes()))
        vm.selectFilament(spec.filamentId)
        vm.selectFilamentColor(spec.colorId)
        vm.selectPrinter(spec.printerId)
        // Sem o G-code (a pasta de exemplo é opcional), as medidas vêm do texto, como nos produtos, e o pedido fica sem foto.
        if (spec.lengthMeters.isNotEmpty()) {
            vm.setLengthMeters(spec.lengthMeters)
        } else if (gcode == null) {
            vm.setLengthMeters("30")
        }
        if (spec.printTimeText.isNotEmpty()) {
            vm.setPrintTimeMinutes(spec.printTimeText)
        } else if (gcode == null) {
            vm.setPrintTimeMinutes("3h")
        }
        vm.selectSalesChannel(spec.channelId)
        // Sem a pasta de G-codes, o pedido que vai pro PDF e pra imagem do WhatsApp ainda ganha foto: a miniatura de exemplo
        // do repositório (um suporte de celular), pelo mesmo caminho do 3MF com miniatura.
        if (gcode == null && spec.gcodeFile == "smartphone_holder_PLA_1h15m.gcode") {
            val photo = File("src/commonMain/composeResources/files/preview_sample_photo.png").takeIf { it.exists() }?.readBytes()
            if (photo != null) vm.attachModelFile(PickedFile("phone_stand.3mf", threeMfWithThumbnail(photo)))
        }
        if (spec.servicePrice != null) {
            vm.toggleService(Catalog.PINTURA_ID)
            vm.setServicePrice(Catalog.PINTURA_ID, spec.servicePrice)
        }
        vm.setSaveName(spec.name)
        vm.setClientName(spec.clientName)
        vm.setClientContact(spec.clientContact)
        vm.setDeliveryDate(deliveryEpochDay(spec.deliveryDaysFromToday))
        val ok = vm.saveCurrentQuote()
        check(ok) { "Não consegui salvar o pedido de exemplo \"${spec.name}\": ${vm.uiState.value.form.blockedMessage}" }
        val savedId = history.savedQuotes.value.last().id
        for ((status, daysAfterCreation) in spec.statusSteps) {
            clock.millis = atDaysAgo(spec.createdDaysAgo - daysAfterCreation)
            history.updateStatus(savedId, status)
        }
    }

    for (spec in productSeeds()) {
        clock.millis = atDaysAgo(15)
        val vm = newViewModel()
        vm.setKind(QuoteKind.PRODUCT)
        val gcode = spec.gcodeFile?.let { fileName -> gcodesDir?.let { File(it, fileName) } }?.takeIf { it.exists() }
        if (gcode != null) vm.importGCode(PickedFile(gcode.name, gcode.readBytes()))
        vm.selectFilament(spec.filamentId)
        vm.selectFilamentColor(spec.colorId)
        vm.selectPrinter(spec.printerId)
        // Sem o G-code (a pasta de exemplo é opcional), as medidas vêm do texto, e o produto fica sem foto.
        if (gcode == null) {
            vm.setLengthMeters(spec.lengthMeters.ifEmpty { "30" })
            vm.setPrintTimeMinutes(spec.printTimeText.ifEmpty { "3h" })
        }
        vm.setSaveName(spec.name)
        vm.setCategory(spec.category)
        val ok = vm.saveCurrentQuote()
        check(ok) { "Não consegui salvar o produto de exemplo \"${spec.name}\": ${vm.uiState.value.form.blockedMessage}" }
    }
}

/** Monta o [AppContainer] pra renderizar, como [com.threedreport.app.DesktopContainer.createDesktopContainer], mas
 * sobre a pasta temporária e sem gravação em segundo plano nem verificação de atualização de verdade. */
internal fun buildContainer(dataDir: File, releases: ReleaseSource = ReleaseSource { null }, installer: UpdateInstaller = NoUpdateInstaller, clock: Clock = Clock.System): AppContainer {
    val storage = LocalStorage(dataDir, clock)
    val orderSettings = storage.orderSettings()
    val stock = storage.stock()
    val filaments = storage.filaments().deletingStockWith(stock)
    return AppContainer(
        filaments = filaments,
        printers = storage.printers(),
        services = storage.services(),
        salesChannels = storage.salesChannels(),
        consumables = storage.consumables(),
        templates = storage.templates(),
        clients = storage.clients(),
        maintenance = storage.maintenance(),
        stock = stock,
        quoteHistory = storage.quoteHistory { orderSettings.settings.value.defaultDepositRate }.reconcilingStock(stock) { filaments.filaments.value },
        settings = storage.settings(),
        branding = storage.branding(),
        orderSettings = orderSettings,
        theme = storage.theme(),
        currency = storage.currency(),
        onboarding = storage.onboarding(),
        preferences = storage.preferences(),
        backup = storage.backup(),
        storageHealth = storage.health,
        releases = releases,
        updateInstaller = installer,
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
internal fun renderAppPng(
    outFile: File,
    afterSettle: (ImageComposeScene) -> Unit = {},
    width: Int = WIDTH,
    height: Int = HEIGHT,
    content: @Composable () -> Unit,
) {
    val scene = ImageComposeScene(width = width, height = height, density = Density(1f), content = content)
    try {
        settle(scene)
        afterSettle(scene)
        val image = settle(scene)
        outFile.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    } finally {
        scene.close()
    }
}

internal fun settle(scene: ImageComposeScene, frames: Int = 10, delayMs: Long = 300): SkiaImage {
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
internal fun ImageComposeScene.clickByText(text: String) {
    for (owner in semanticsOwners) {
        val node = findClickableNodeWithText(owner.rootSemanticsNode, text) ?: continue
        node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        return
    }
}

/** Como [clickByText], pelo texto de descrição (botão só de ícone, como as ações da linha). */
internal fun ImageComposeScene.clickByDescription(description: String) {
    fun find(node: SemanticsNode): SemanticsNode? {
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription)
        if (node.config.getOrNull(SemanticsActions.OnClick) != null && descriptions?.any { it == description } == true) return node
        return node.children.firstNotNullOfOrNull(::find)
    }
    for (owner in semanticsOwners) {
        val node = find(owner.rootSemanticsNode) ?: continue
        node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        return
    }
}

/**
 * Digita [text] no campo cuja descrição é [label] (o que um teste de UI faria com `performTextInput`), sem foco nem
 * teclado: usa a ação de semântica do próprio campo.
 */
internal fun ImageComposeScene.setTextByLabel(label: String, text: String) {
    fun find(node: SemanticsNode): SemanticsNode? {
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription)
        if (node.config.getOrNull(SemanticsActions.SetText) != null && descriptions?.any { it == label } == true) return node
        return node.children.firstNotNullOfOrNull(::find)
    }
    for (owner in semanticsOwners) {
        val node = find(owner.rootSemanticsNode) ?: continue
        node.config.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(text))
        return
    }
}

/** Rola até o fim tudo o que rola na tela (o painel do formulário), pra capturar o que fica embaixo. */
internal fun ImageComposeScene.scrollAllToEnd() {
    fun walk(node: SemanticsNode) {
        node.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, SCROLL_TO_END)
        node.children.forEach(::walk)
    }
    for (owner in semanticsOwners) walk(owner.rootSemanticsNode)
}

private const val SCROLL_TO_END = 5000f

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
    val pdfBytes = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photoBytes)), testClientTexts, resolved.brandName, resolved.footerText, resolved.options.copy(issuedOnEpochDay = TODAY.toEpochDay()))
    composePdfPage(pdfBytes, outFile)
}

/** O catálogo de verdade (mesmo exportador do app), a primeira página composta num fundo neutro. */
private fun renderCatalogComposite(items: List<QuoteExportItem>, branding: BrandingSettings, outFile: File) {
    val resolved = branding.resolvePdfBranding(logoBytes = null)
    composePdfPage(renderCatalogPdf(items, testClientTexts, resolved.brandName, resolved.footerText, resolved.options.copy(issuedOnEpochDay = TODAY.toEpochDay())), outFile)
}

internal fun composePdfPage(pdfBytes: ByteArray, outFile: File) {
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

/** A mesma foto com o brilho dos pixels reduzido a 15% (alfa intacto): uma peça preta no lugar da cinza clara da miniatura. */
private fun darkened(photoBytes: ByteArray?): ByteArray? {
    val photo = photoBytes?.let { ImageIO.read(java.io.ByteArrayInputStream(it)) } ?: return null
    val dark = BufferedImage(photo.width, photo.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until photo.height) {
        for (x in 0 until photo.width) {
            val argb = photo.getRGB(x, y)
            fun channel(shift: Int) = (((argb shr shift) and 0xFF) * 0.15).toInt()
            dark.setRGB(x, y, (argb and 0xFF000000.toInt()) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0))
        }
    }
    return java.io.ByteArrayOutputStream().also { ImageIO.write(dark, "png", it) }.toByteArray()
}

/** A imagem quadrada de verdade (mesmo exportador do "Salvar imagem" do Histórico), salva como está. */
private fun renderWhatsAppPng(savedQuote: SavedQuote, photoBytes: ByteArray?, branding: BrandingSettings, outFile: File) {
    val currency = savedQuote.currency
    val quantity = savedQuote.quote.quantity
    val piecesTotal = savedQuote.totalWithServices - savedQuote.shippingCost
    val bytes = renderQuoteImage(
        texts = testClientTexts,
        title = savedQuote.name,
        priceText = savedQuote.totalWithServices.toCurrencyText(currency),
        unitPriceText = if (quantity > 1) testClientTexts.imageUnitPrice(quantity, (piecesTotal / quantity).toCurrencyText(currency)) else null,
        photoBytes = photoBytes,
        brandText = branding.imageBrandLine(),
        deliveryText = savedQuote.deliveryDateEpochDay?.let { testClientTexts.imageDelivery(formatShortDate(it)) },
        background = branding.imageBackground,
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
private fun OgBanner(icon: ImageBitmap, screenshot: ImageBitmap) = AppTheme(ThemeMode.LIGHT) {
    val colors = MaterialTheme.appColors
    val type = MaterialTheme.appType
    Box(
        // Do azul da marca (tema claro) ao azul do tema escuro: os dois são tokens do sistema de design.
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.navy, DarkAppColors.navy), end = Offset(1200f, 630f))),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(84.dp))
                    Spacer(Modifier.width(24.dp))
                    // A mesma composição do BrandMark: "3D" em onNavy e "Report" em âmbar, Space Grotesk 700.
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = colors.onNavy)) { append("3D") }
                            withStyle(SpanStyle(color = colors.amber)) { append("Report") }
                        },
                        style = type.brand.copy(fontSize = 54.sp, lineHeight = 64.sp),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    sample("O sistema de quem vende\nimpressão 3D", "The system for people\nwho sell 3D prints"),
                    color = colors.onNavy,
                    style = type.panelTitle.copy(fontSize = 34.sp, lineHeight = 42.sp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    sample("Grátis, open source e 100% no seu computador", "Free, open source and 100% on your computer"),
                    color = colors.onNavyMuted,
                    style = type.body.copy(fontSize = 23.sp, lineHeight = 30.sp),
                )
            }
            Spacer(Modifier.width(32.dp))
            Box(
                modifier = Modifier
                    .size(420.dp, 480.dp)
                    .shadow(elevation = 28.dp, shape = MaterialTheme.shapes.large, clip = false)
                    .clip(MaterialTheme.shapes.large)
                    .background(colors.card),
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
