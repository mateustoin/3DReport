package com.threedreport.app.screenshots

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.threedreport.app.AppContainer
import com.threedreport.app.data.QuoteDetails
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.ReceiptData
import com.threedreport.app.platform.renderReceiptPdf
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.testClientTexts
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.ThemeMode
import java.io.File

/** Altura das capturas de Configurações: a seção inteira cabe sem rolar. */
private const val TALL_HEIGHT = 1700

/**
 * As capturas da Leva 11 (`-Pscreenshots.leva11=true`): o kanban com os selos de pagamento, de parado e a observação, o
 * menu do pedido, os diálogos de pagamentos e de mensagem, Configurações (Pix, mensagens e Pedidos) e os PDFs com Pix e
 * o recibo. Não entram no conjunto do site: o que já está na pasta de dados de exemplo ganha os dados novos aqui.
 */
internal fun renderLeva11Screenshots(container: AppContainer, outDir: File) {
    container.theme.update(ThemeMode.LIGHT)
    val history = container.quoteHistory
    fun order(name: String): SavedQuote = history.savedQuotes.value.first { it.name == name }

    // Pix configurado (chave fictícia) e orçamento parado a partir de 1 dia, pra os dois exemplos de Orçado aparecerem parados.
    container.branding.update(container.branding.branding.value.copy(pixKey = "contato@example.com", pixReceiverName = "Sua Loja 3D", pixCity = "Sao Paulo"))
    container.orderSettings.update(OrderSettings(staleQuoteDays = 1))

    val today = TODAY.toEpochDay()
    // Orçado, com sinal de 30% ainda não pago: o pedido dos diálogos.
    val miniature = order("Miniatura pintada")
    history.updateDeposit(miniature.id, miniature.totalWithServices * 0.3)
    // Aprovado com sinal pago, aprovado com pagamento em parte, imprimindo com sinal por pagar e entregue pago.
    val headset = order("Suporte de headset")
    history.updateDeposit(headset.id, headset.totalWithServices * 0.3)
    history.addPayment(headset.id, history.savedQuotes.value.first { it.id == headset.id }.requestedDeposit, PaymentMethod.PIX, today - 2)
    history.addPayment(order("Chaveiro personalizado").id, 10.0, PaymentMethod.CASH, today - 1)
    val phoneStand = order("Porta-celular")
    history.updateDeposit(phoneStand.id, phoneStand.totalWithServices * 0.5)
    val gift = order("Presente de aniversário")
    history.addPayment(gift.id, gift.totalWithServices, PaymentMethod.CARD, today - 20)
    // Observação interna e código de rastreio (a observação mostra o ícone no cartão).
    withDetails(container, order("Axolote de estimação"), notes = "Cliente prefere retirar na sexta.")
    withDetails(container, order("Organizador de mesa (encomenda)"), tracking = "AB123456789BR")

    renderAppPng(File(outDir, "pedidos-kanban-selos.png")) { sampleApp(container, startAt = AppDestination.ORDERS) }

    // O menu do primeiro cartão de Orçado, parado: "Retomar conversa" e "Marcar como cancelado" no alto.
    renderAppPng(File(outDir, "pedidos-menu-parado.png"), afterSettle = { it.clickByDescription("Mais ações") }) {
        sampleApp(container, startAt = AppDestination.ORDERS)
    }
    renderAppPng(File(outDir, "pagamentos-dialogo.png"), afterSettle = { it.openMenuItem("Pagamentos") }) {
        sampleApp(container, startAt = AppDestination.ORDERS)
    }
    renderAppPng(
        File(outDir, "pagamentos-registrar.png"),
        afterSettle = {
            it.openMenuItem("Pagamentos")
            settle(it, frames = 4)
            it.clickByText("Registrar pagamento")
        },
    ) {
        sampleApp(container, startAt = AppDestination.ORDERS)
    }
    renderAppPng(File(outDir, "mensagem-dialogo.png"), afterSettle = { it.openMenuItem("Mensagem pro cliente") }) {
        sampleApp(container, startAt = AppDestination.ORDERS)
    }

    renderAppPng(
        File(outDir, "configuracoes-pix-mensagens.png"),
        afterSettle = { it.clickByText("Documentos pro cliente") },
        height = TALL_HEIGHT,
    ) {
        sampleApp(container, startAt = AppDestination.SETTINGS)
    }
    renderAppPng(File(outDir, "configuracoes-pedidos.png"), afterSettle = { it.clickLastByText("Pedidos") }) {
        sampleApp(container, startAt = AppDestination.SETTINGS)
    }

    // O PDF do orçamento com Pix (sinal por pagar) e o recibo do sinal já pago.
    val pdfBranding = container.branding.branding.value.resolvePdfBranding(container.branding.logoBytes())
    val options = pdfBranding.options.copy(issuedOnEpochDay = TODAY.toEpochDay())
    val pending = history.savedQuotes.value.first { it.id == miniature.id }
    val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(pending, history.photoBytes(pending))), testClientTexts, pdfBranding.brandName, pdfBranding.footerText, options)
    composePdfPage(pdf, File(outDir, "pdf-orcamento-pix.png"))

    val paid = history.savedQuotes.value.first { it.id == headset.id }
    val payment = paid.payments.first()
    val receipt = renderReceiptPdf(ReceiptData(paid, payment, paid.receiptNumber(payment.id)), testClientTexts, pdfBranding.brandName, pdfBranding.footerText, options)
    composePdfPage(receipt, File(outDir, "pdf-recibo.png"))
}

/** Grava a observação e o código de rastreio de [order] pelo mesmo caminho do "Editar detalhes", sem mexer no resto. */
private fun withDetails(container: AppContainer, order: SavedQuote, notes: String? = order.notes, tracking: String? = order.trackingCode) {
    val repository = container.quoteHistory
    val photo = repository.photoBytes(order)?.let { PickedFile(order.photoFileName ?: "foto.png", it) }
    repository.updateDetails(
        order.id,
        QuoteDetails(
            name = order.name,
            client = order.client,
            sourceLink = order.sourceLink,
            photo = photo,
            deliveryDateEpochDay = order.deliveryDateEpochDay,
            category = order.category,
            trackingCode = tracking,
            notes = notes,
        ),
    )
}

/** Abre o menu do primeiro cartão e clica no item [text]. */
private fun ImageComposeScene.openMenuItem(text: String) {
    clickByDescription("Mais ações")
    settle(this, frames = 3)
    clickByText(text)
}

/** Como `clickByText`, mas no último nó que casa: "Pedidos" existe na barra lateral e na lista de seções de Configurações. */
private fun ImageComposeScene.clickLastByText(text: String) {
    fun collect(node: SemanticsNode, into: MutableList<SemanticsNode>) {
        val texts = node.config.getOrNull(SemanticsProperties.Text)
        if (node.config.getOrNull(SemanticsActions.OnClick) != null && texts?.any { it.text == text } == true) into += node
        node.children.forEach { collect(it, into) }
    }
    val found = mutableListOf<SemanticsNode>()
    for (owner in semanticsOwners) collect(owner.rootSemanticsNode, found)
    found.lastOrNull()?.config?.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
}
