package com.threedreport.app.ui.quote

import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
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
)
