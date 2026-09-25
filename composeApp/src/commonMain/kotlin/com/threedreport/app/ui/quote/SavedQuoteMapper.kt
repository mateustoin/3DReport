package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.format.toDurationInputText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote

/**
 * Leva um orçamento salvo de volta pra tela (reabrir, duplicar, vender, guardar no catálogo). Fora do
 * ViewModel (decisão 108), pra a regra "o que volta de onde" ficar num lugar só.
 */
internal object SavedQuoteMapper {

    /**
     * As entradas da tela a partir do retrato salvo. Filamento, impressora e canal voltam pelo id; o que
     * não está mais no cadastro fica marcado com o nome antigo, e a tela pede pra escolher de novo (nunca
     * cai em outro em silêncio, o que mudaria o preço).
     */
    fun inputStateFrom(
        savedQuote: SavedQuote,
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        channels: List<SalesChannel>,
        newId: () -> Int,
    ): QuoteInputState {
        val quote = savedQuote.quote
        // Pelo id; um canal excluído e recriado com o mesmo nome ganha id novo, então o nome é a segunda
        // tentativa. Sem nenhum dos dois, a tela avisa: recalcular sem a taxa em silêncio baixaria o preço.
        val channel = quote.channelId?.let { id -> channels.firstOrNull { it.id == id } }
            ?: quote.channelName?.let { name -> channels.firstOrNull { it.name == name } }
        return QuoteInputState(
            kind = savedQuote.kind,
            prints = quote.prints.map { print ->
                PrintInput(
                    name = print.job.name.orEmpty(),
                    printerId = print.printerId,
                    missingPrinterName = print.printerName.takeIf { printers.none { it.id == print.printerId } },
                    filaments = print.job.filaments.map { usage ->
                        FilamentInput(
                            filamentId = usage.filament.id,
                            colorId = usage.color?.id,
                            lengthText = usage.lengthMeters.toInputText(),
                            missingFilamentName = usage.filament.name.takeIf { filaments.none { it.id == usage.filament.id } },
                            id = newId(),
                        )
                    },
                    printTimeText = print.job.printTimeMinutes.toDurationInputText(),
                    runsText = if (print.job.runs > 1) print.job.runs.toString() else "",
                    id = newId(),
                )
            },
            laborMinutesText = if (quote.laborMinutes > 0) quote.laborMinutes.toDurationInputText() else "",
            quantityText = if (quote.quantity > 1) quote.quantity.toString() else "",
            // Valor e forma de cobrança vêm do retrato salvo, não do catálogo atual: reabrir e salvar
            // não pode reprecificar o pedido em silêncio.
            selectedServices = savedQuote.services.associate { service ->
                service.id to ServiceInput(
                    name = service.name,
                    priceText = service.price.toInputText(),
                    chargedPerOrder = service.chargedPerOrder,
                )
            },
            salesChannelId = channel?.id,
            missingChannelName = quote.channelName.takeIf { channel == null },
            shippingCostText = if (savedQuote.shippingCost > 0) savedQuote.shippingCost.toInputText() else "",
            // Sem isso, reabrir um orçamento negociado e salvar de novo voltaria em silêncio pro preço
            // de tabela. O campo recebe o total do cliente, igual ao que foi digitado (ver `calculate`).
            // Pedido vendido pelo preço do catálogo volta com o anunciado no lugar dele, e não como preço
            // digitado: salvar de novo mantém o preço e continua fora dos números de negociação.
            targetTotalText = if (quote.isNegotiated && !savedQuote.soldAtCatalogPrice) savedQuote.totalWithServices.toInputText() else "",
            announcedUnitPrice = if (savedQuote.soldAtCatalogPrice) quote.unitSalePrice else null,
        )
    }

    /** O formulário de salvar a partir do retrato, com a foto e o STL já lidos. */
    fun saveFormFrom(savedQuote: SavedQuote, photo: PickedFile?, stlFile: PickedFile?) = SaveQuoteFormState(
        name = savedQuote.name,
        photo = photo,
        stlFile = stlFile,
        sourceLink = savedQuote.sourceLink.orEmpty(),
        clientName = savedQuote.client?.name.orEmpty(),
        clientContact = savedQuote.client?.contact.orEmpty(),
        clientId = savedQuote.client?.id,
        printSettings = savedQuote.printSettings ?: PrintSettings(),
        deliveryDateEpochDay = savedQuote.deliveryDateEpochDay,
        category = savedQuote.category.orEmpty(),
    )
}
