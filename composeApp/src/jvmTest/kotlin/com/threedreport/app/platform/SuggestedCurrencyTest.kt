package com.threedreport.app.platform

import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.core.model.Currency
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A moeda da primeira abertura pela região do sistema (decisão 16a da Leva 11B). */
class SuggestedCurrencyTest {

    @Test
    fun theRegionSuggestsTheCurrency() {
        assertEquals(Currency.BRL, suggestedCurrency("BR"))
        assertEquals(Currency.USD, suggestedCurrency("US"))
        assertEquals(Currency.EUR, suggestedCurrency("DE"))
        assertEquals(Currency.EUR, suggestedCurrency("pt"))
        assertEquals(Currency.GBP, suggestedCurrency("GB"))
        assertEquals(Currency.USD, suggestedCurrency("AU"))
        assertEquals(Currency.USD, suggestedCurrency(null))
    }

    /** Bulgária (no euro desde 2026-01-01) e os países fora da UE que usam o euro. */
    @Test
    fun theNewerAndNonUnionEuroCountriesSuggestTheEuro() {
        for (region in listOf("BG", "ME", "XK", "AD", "MC", "SM", "VA")) {
            assertEquals(Currency.EUR, suggestedCurrency(region), region)
        }
    }

    @Test
    fun onlyAnEmptyFolderIsANewInstall() {
        val dir = createTempDirectory("3dreport-test").toFile()
        val storage = LocalStorage(dir, Clock.System)
        assertTrue(storage.isNewInstall())

        // Quem já tem dados (mesmo sem a moeda gravada) não é instalação nova e fica no real.
        storage.onboarding().markCompleted()
        assertFalse(LocalStorage(dir, Clock.System).isNewInstall())
        assertEquals(Currency.BRL, LocalStorage(dir, Clock.System).currency().currency.value)
    }
}
