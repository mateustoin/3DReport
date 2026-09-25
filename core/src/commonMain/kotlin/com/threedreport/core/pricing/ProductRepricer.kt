package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import kotlin.math.abs

/** Resultado de recalcular um produto do catálogo com os cadastros de hoje (ver [ProductRepricer]). */
sealed interface RepriceResult {

    /**
     * @property quote o mesmo produto calculado com os custos de hoje.
     * @property changed se o preço **calculado** mudou (o anunciado, quando existe, é mantido e
     *   não entra na comparação).
     */
    data class Repriced(val quote: Quote, val changed: Boolean) : RepriceResult

    /** Não dá pra recalcular sem a pessoa escolher de novo o que sumiu dos cadastros. */
    data class Unavailable(val reason: Reason, val missingName: String? = null) : RepriceResult

    enum class Reason { FILAMENT_MISSING, PRINTER_MISSING, CHANNEL_MISSING, INVALID }
}

/**
 * Recalcula um produto do catálogo com os cadastros atuais (decisão 102). O pedido é um retrato
 * congelado (KDoc de [SavedQuote]), e isso está certo pra venda; o produto é vitrine e precisa
 * acompanhar o filamento que subiu e a hora de trabalho que mudou.
 *
 * Função pura, no estilo do [PricingCalculator]: acha cada filamento e cada impressora de cada
 * impressão, e o canal, pelo id que o produto guardou, e refaz a conta com as mesmas quantidades
 * (comprimentos, tempos, rodadas, quantidade, tempo de trabalho e cores). O preço anunciado (o preço fechado da decisão 84, guardado como
 * [Quote.salePrice] com o calculado em [Quote.tableSalePrice]) continua o mesmo: mudar o que se
 * anuncia é decisão de quem vende, não do app.
 */
object ProductRepricer {

    /** Diferença abaixo da qual o preço é considerado igual: meio centavo, o que a tela arredonda. */
    private const val TOLERANCE = 0.005

    fun reprice(
        saved: SavedQuote,
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        channels: List<SalesChannel>,
    ): RepriceResult {
        val quote = saved.quote
        val prints = quote.prints.map { print ->
            val printer = printers.firstOrNull { it.id == print.printerId }
                ?: return RepriceResult.Unavailable(RepriceResult.Reason.PRINTER_MISSING, print.printerName)
            val usages = print.job.filaments.map { usage ->
                val filament = filaments.firstOrNull { it.id == usage.filament.id }
                    ?: return RepriceResult.Unavailable(RepriceResult.Reason.FILAMENT_MISSING, usage.filament.name)
                usage.copy(filament = filament)
            }
            print.job.copy(filaments = usages) to printer
        }
        val channel = quote.channelId?.let { id ->
            channels.firstOrNull { it.id == id }
                ?: return RepriceResult.Unavailable(RepriceResult.Reason.CHANNEL_MISSING, quote.channelName)
        }

        val repriced = runCatching {
            PricingCalculator.calculate(
                prints = prints,
                settings = settings,
                channel = channel,
                quantity = quote.quantity,
                laborMinutes = quote.laborMinutes,
                negotiatedSalePrice = if (quote.isNegotiated) quote.salePrice else null,
            )
        }.getOrElse { return RepriceResult.Unavailable(RepriceResult.Reason.INVALID, it.message) }

        return RepriceResult.Repriced(repriced, changed = abs(repriced.calculatedSalePrice - quote.calculatedSalePrice) >= TOLERANCE)
    }

    /** Preço que a margem dá, com ou sem um preço fechado/anunciado por cima. */
    private val Quote.calculatedSalePrice: Double
        get() = tableSalePrice ?: salePrice
}
