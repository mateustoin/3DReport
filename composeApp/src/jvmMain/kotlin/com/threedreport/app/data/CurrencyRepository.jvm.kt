package com.threedreport.app.data

import com.threedreport.core.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class CurrencyRepository actual constructor() {
    private val file = File(appDataDir(), "currency.json")
    private val state = MutableStateFlow(readJsonFile(file, Currency.BRL))

    actual val currency: StateFlow<Currency> = state.asStateFlow()

    actual fun update(currency: Currency) {
        state.value = currency
        writeJsonFile(file, currency)
    }
}
