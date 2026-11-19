package com.threedreport.app.screenshots

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.filaments_menu_more
import _3dreport.composeapp.generated.resources.stock_action_register_purchase
import _3dreport.composeapp.generated.resources.stock_action_weigh
import _3dreport.composeapp.generated.resources.stock_field_amount
import _3dreport.composeapp.generated.resources.stock_field_price
import _3dreport.composeapp.generated.resources.stock_field_scale
import _3dreport.composeapp.generated.resources.stock_field_shipping
import _3dreport.composeapp.generated.resources.stock_field_spool
import _3dreport.composeapp.generated.resources.stock_field_supplier
import _3dreport.composeapp.generated.resources.stock_item_choose
import _3dreport.composeapp.generated.resources.stock_section_untracked
import _3dreport.composeapp.generated.resources.stock_view_purchases
import _3dreport.composeapp.generated.resources.stock_view_shopping
import com.threedreport.app.AppContainer
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import java.io.File

/** Altura das capturas do Orçamento com os avisos de estoque: o aviso da impressão e o dos insumos cabem sem rolar. */
private const val TALL_HEIGHT = 2100

private fun tr(resource: StringResource, vararg args: Any): String = runBlocking { getString(resource, *args) }

/** Os textos da tela Estoque que as capturas procuram na tela, lidos dos recursos no idioma em uso. */
private object Stock {
    val purchasesTab get() = tr(Res.string.stock_view_purchases)
    val shoppingTab get() = tr(Res.string.stock_view_shopping)
    val registerPurchase get() = tr(Res.string.stock_action_register_purchase)
    val weigh get() = tr(Res.string.stock_action_weigh)
    val scale get() = tr(Res.string.stock_field_scale)
    val spool get() = tr(Res.string.stock_field_spool)
    val chooseItem get() = tr(Res.string.stock_item_choose)
    val amountGrams get() = tr(Res.string.stock_field_amount, "g")
    val price get() = tr(Res.string.stock_field_price)
    val shipping get() = tr(Res.string.stock_field_shipping)
    val supplier get() = tr(Res.string.stock_field_supplier)
    val untrackedSection get() = tr(Res.string.stock_section_untracked, 0).substringBefore(" (")
    val moreActions get() = tr(Res.string.filaments_menu_more)
}

private val white = StockItemRef.ColorRef(Catalog.PLA_ID, Catalog.COR_BRANCO)
private val black = StockItemRef.ColorRef(Catalog.PLA_ID, Catalog.COR_PRETO)
private val gray = StockItemRef.ColorRef(Catalog.PLA_ID, Catalog.COR_CINZA)
private val natural = StockItemRef.ColorRef(Catalog.PETG_ID, Catalog.COR_NATURAL)
private val ring = StockItemRef.ConsumableRef("insumo-argola")
private val magnet = StockItemRef.ConsumableRef("insumo-ima")

/**
 * As capturas da Leva 12 (`-Pscreenshots.leva12=true`): a tela Estoque nas três vistas, sem dados e com dados (claro e escuro),
 * o painel do item, os diálogos, o formulário de compra, o selo da cor em Filamentos, "8 un em estoque" em Insumos e o aviso
 * de estoque no Orçamento. Não entram no conjunto do site. Roda por último: grava o estoque de exemplo.
 */
internal fun renderLeva12Screenshots(container: AppContainer, outDir: File) {
    container.theme.update(ThemeMode.LIGHT)
    val stock = container.stock
    val today = TODAY.toEpochDay()

    // Sem nenhum controle ligado: o Saldo só tem a seção recolhida (aberta aqui), e as outras duas vistas, o estado vazio.
    renderAppPng(File(outDir, "estoque-saldo-sem-controle.png"), afterSettle = { it.clickByText(Stock.untrackedSection) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    renderAppPng(File(outDir, "estoque-compras-vazio.png"), afterSettle = { it.clickByText(Stock.purchasesTab) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    renderAppPng(File(outDir, "estoque-lista-vazia.png"), afterSettle = { it.clickByText(Stock.shoppingTab) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }

    // As compras, antes do controle: o que já se pagou conta na média mesmo sem saldo.
    stock.savePurchase(
        Purchase(
            id = "compra-3", dateEpochDay = today - 41, supplier = sample("Mercado Livre", "eBay"),
            lines = listOf(PurchaseLine(black, 1000.0, sample(84.0, 21.0))),
        ),
    )
    stock.savePurchase(
        Purchase(
            id = "compra-2", dateEpochDay = today - 18, supplier = "Voolt3D", link = "https://example.com/voolt3d",
            shipping = sample(22.0, 6.0),
            lines = listOf(PurchaseLine(natural, 1000.0, sample(118.0, 29.9)), PurchaseLine(black, 1000.0, sample(89.9, 24.9))),
        ),
    )
    stock.savePurchase(
        Purchase(
            id = "compra-1", dateEpochDay = today - 3, supplier = "Voolt3D", link = "https://example.com/voolt3d",
            shipping = sample(18.0, 5.0),
            lines = listOf(
                PurchaseLine(white, 1000.0, sample(89.9, 24.9)),
                PurchaseLine(ring, 50.0, sample(12.0, 3.0)),
            ),
        ),
    )

    // O saldo de agora: um item folgado, um abaixo do limite, um acabando (saldo negativo) e os insumos.
    stock.startTracking(white, 640.0)
    stock.startTracking(black, 120.0)
    stock.updateItemSettings(black, lowThreshold = 200.0, emptySpoolGrams = 250.0)
    stock.startTracking(natural, 60.0)
    stock.recordManualUse(natural, 100.0, ManualUseReason.FAILURE, sample("Calibração do perfil", "Profile calibration"))
    stock.startTracking(ring, 8.0)
    stock.updateItemSettings(ring, lowThreshold = 20.0, emptySpoolGrams = null)
    stock.startTracking(magnet, 150.0)
    // O cinza acabou e não tem controle: entra na seção à parte da lista de compras.
    val pla = container.filaments.filaments.value.first { it.id == Catalog.PLA_ID }
    container.filaments.update(pla.copy(colors = pla.colors.map { if (it.id == Catalog.COR_CINZA) it.copy(inStock = false) else it }))

    renderAppPng(File(outDir, "estoque-saldo.png")) { sampleApp(container, startAt = AppDestination.STOCK) }
    renderAppPng(File(outDir, "estoque-saldo-painel.png"), afterSettle = { it.clickByText(sample("Preto", "Black")) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    renderAppPng(File(outDir, "estoque-compras.png"), afterSettle = { it.clickByText(Stock.purchasesTab) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    renderAppPng(File(outDir, "estoque-lista.png"), afterSettle = { it.clickByText(Stock.shoppingTab) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    // Uma compra aberta pra editar (o item, o preço, o frete e as caixas "Atualizar o preço").
    renderAppPng(
        File(outDir, "estoque-compra-editar.png"),
        afterSettle = {
            it.clickByText(Stock.purchasesTab)
            settle(it, frames = 4)
            it.clickByText("Voolt3D")
        },
        height = TALL_HEIGHT,
    ) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    // O diálogo de pesar o carretel, com o saldo novo calculado antes de gravar.
    renderAppPng(
        File(outDir, "estoque-pesar.png"),
        afterSettle = {
            it.clickByDescription(Stock.weigh)
            settle(it, frames = 4)
            it.setTextByLabel(Stock.scale, "890")
            it.setTextByLabel(Stock.spool, "250")
        },
    ) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    // O formulário de compra novo, preenchido: o custo por kg já com o frete rateado e a caixa de atualizar o preço.
    renderAppPng(
        File(outDir, "estoque-compra-nova.png"),
        afterSettle = {
            it.clickByText(Stock.purchasesTab)
            settle(it, frames = 4)
            it.clickByText(Stock.registerPurchase)
            settle(it, frames = 4)
            it.clickByText(Stock.chooseItem)
            settle(it, frames = 4)
            it.clickByText("PLA Voolt3D ${sample("Cinza", "Gray")}")
            settle(it, frames = 4)
            it.setTextByLabel(Stock.amountGrams, "1000")
            it.setTextByLabel(Stock.price, sample("97,40", "27.40"))
            it.setTextByLabel(Stock.shipping, sample("18,00", "5.00"))
            it.setTextByLabel(Stock.supplier, "Voolt3D")
        },
        height = TALL_HEIGHT,
    ) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }

    container.theme.update(ThemeMode.DARK)
    renderAppPng(File(outDir, "estoque-saldo-escuro.png")) { sampleApp(container, startAt = AppDestination.STOCK) }
    renderAppPng(File(outDir, "estoque-lista-escuro.png"), afterSettle = { it.clickByText(Stock.shoppingTab) }) {
        sampleApp(container, startAt = AppDestination.STOCK)
    }
    container.theme.update(ThemeMode.LIGHT)

    // As outras telas: o saldo no selo da cor, o menu de exportar e importar, e "8 un em estoque" no insumo.
    renderAppPng(File(outDir, "filamentos-saldo.png")) { sampleApp(container, startAt = AppDestination.FILAMENTS) }
    renderAppPng(File(outDir, "filamentos-menu.png"), afterSettle = { it.clickByDescription(Stock.moreActions) }) {
        sampleApp(container, startAt = AppDestination.FILAMENTS)
    }
    renderAppPng(File(outDir, "insumos-saldo.png")) { sampleApp(container, startAt = AppDestination.CONSUMABLES) }

    // O aviso no Orçamento: o preto (36 g pra uma base) não cabe no que sobra livre depois dos pedidos aprovados, e as 12 argolas
    // passam das 8 que restam. Só avisa; o preço e o Salvar seguem iguais.
    renderAppPng(File(outDir, "orcamento-aviso-estoque.png"), height = TALL_HEIGHT) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.toggleConsumable("insumo-argola")
                vm.setConsumableQuantity("insumo-argola", "12")
            },
        )
    }
    container.theme.update(ThemeMode.DARK)
    renderAppPng(File(outDir, "orcamento-aviso-estoque-escuro.png"), height = TALL_HEIGHT) {
        sampleApp(
            container,
            startAt = AppDestination.QUOTE,
            prepareQuote = { vm ->
                fillDraftQuote(vm)
                vm.toggleConsumable("insumo-argola")
                vm.setConsumableQuantity("insumo-argola", "12")
            },
        )
    }
    container.theme.update(ThemeMode.LIGHT)
}
