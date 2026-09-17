package com.threedreport.app.data

import com.threedreport.core.model.Currency
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda a moeda usada pra formatar valores no app ([Currency]). A
 * implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), começando em [Currency.BRL].
 */
expect class CurrencyRepository() {
    val currency: StateFlow<Currency>
    fun update(currency: Currency)
}
