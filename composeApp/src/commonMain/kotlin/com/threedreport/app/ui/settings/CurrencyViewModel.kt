package com.threedreport.app.ui.settings

import com.threedreport.app.data.CurrencyRepository
import com.threedreport.core.model.Currency
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel da preferência de moeda. Diferente de [SettingsViewModel], não
 * tem rascunho/"Salvar" — trocar de moeda é uma ação instantânea.
 */
class CurrencyViewModel(private val repository: CurrencyRepository) {
    val currency: StateFlow<Currency> = repository.currency

    fun setCurrency(currency: Currency) = repository.update(currency)
}
