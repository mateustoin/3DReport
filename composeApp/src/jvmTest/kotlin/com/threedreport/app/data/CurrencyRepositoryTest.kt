package com.threedreport.app.data

import com.threedreport.core.model.Currency
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Valida que a moeda escolhida sobrevive a uma nova instância do repositório (persistência em disco). */
class CurrencyRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsWithBrl() {
        assertEquals(Currency.BRL, CurrencyRepository().currency.value)
    }

    @Test
    fun updatedCurrencySurvivesNewRepositoryInstance() {
        val original = CurrencyRepository()
        original.update(Currency.USD)

        assertEquals(Currency.USD, CurrencyRepository().currency.value)
    }
}
