package com.threedreport.app.ui.quote

import com.threedreport.app.ui.history.ClientExport
import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service

/**
 * Os cadastros e as configurações que a tela de Orçamento lê, como estavam na última vez que o ViewModel
 * calculou. Vêm dos repositórios compartilhados: um cadastro mexido em outra tela chega aqui na hora.
 */
data class QuoteCatalog(
    val filaments: List<Filament>,
    val printers: List<PrinterProfile>,
    val services: List<Service>,
    val consumables: List<Consumable>,
    val salesChannels: List<SalesChannel>,
    /** Clientes do cadastro, pra sugerir ao digitar o nome. */
    val clients: List<Client>,
    val settings: PricingSettings,
    /** Histórico, só pra dica de prazo saber o que já está na fila de cada impressora. */
    val savedQuotes: List<SavedQuote>,
)

/**
 * Tudo o que a tela de Orçamento mostra, num valor só.
 *
 * @property input o que a pessoa digitou.
 * @property form o que é de salvar: nome, cliente, foto, STL, prazo e a operação em curso.
 * @property result a conta, feita pelo `core` a partir de [input] e [catalog]; é a mesma da tela e do Ctrl+S.
 * @property comparison o mesmo orçamento em cada impressora cadastrada (ver `QuoteViewModel.comparePrinters`); vazio
 *   com uma impressora só ou sem conta.
 * @property queueHints a fila de cada impressora que o pedido usa, pra dica ao escolher o prazo; vazia sem fila.
 * @property negotiating o campo "Preço combinado" do painel está aberto. Fica aqui, e não na tela, pra fechar quando o
 *   formulário recomeça (salvar, orçamento novo).
 * @property deliveryDaysLate quantos dias o prazo do formulário já passou (0 sem prazo ou em dia), pro painel e pro
 *   seletor de prazo dizerem o mesmo.
 * @property pendingSend o envio ("Enviar pro cliente") segurado porque o prazo já venceu, até a pessoa confirmar ou
 *   desistir; o mesmo aviso de Pedidos.
 * @property exporting o PDF ou a imagem do "Enviar pro cliente" está sendo gerado.
 * @property importing um G-code está sendo lido.
 * @property collapsedPrintIds cartões de impressão recolhidos, pelo id. Ficam no estado, e não na tela, pra
 *   continuar recolhido ao sair do Orçamento e voltar, e fora de [input] porque recolher não altera o pedido.
 */
data class QuoteUiState(
    val catalog: QuoteCatalog,
    val input: QuoteInputState = QuoteInputState(),
    val form: SaveQuoteFormState = SaveQuoteFormState(),
    val result: QuoteResult = QuoteResult(),
    val importing: Boolean = false,
    val stlPreview: StlPreview? = null,
    val collapsedPrintIds: Set<Int> = emptySet(),
    val exporting: Boolean = false,
    val comparison: List<Pair<PrinterProfile, Quote>> = emptyList(),
    val queueHints: List<QueueHint> = emptyList(),
    val negotiating: Boolean = false,
    val deliveryDaysLate: Int = 0,
    val pendingSend: ClientExport? = null,
)

/**
 * O que já está na frente de uma peça nova numa impressora: [queuedQuoteCount] pedidos aprovados ou em impressão,
 * somando [queuedMinutes] de máquina; [thisMinutes] é o tempo que este pedido pede nela.
 */
data class QueueHint(val printerName: String, val queuedMinutes: Double, val queuedQuoteCount: Int, val thisMinutes: Double)
