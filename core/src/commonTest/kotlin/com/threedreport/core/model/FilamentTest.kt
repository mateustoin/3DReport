package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilamentTest {

    private val base = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    @Test
    fun newFilamentHasOneColorInStockByDefault() {
        assertTrue(base.hasStockAvailable)
        assertEquals(1, base.colors.size)
    }

    @Test
    fun hasStockAvailableIsTrueWhenAnyColorIsInStock() {
        val filament = base.copy(
            colors = listOf(
                FilamentColor(id = "red", inStock = false),
                FilamentColor(id = "blue", inStock = true),
            ),
        )

        assertTrue(filament.hasStockAvailable)
    }

    @Test
    fun hasStockAvailableIsFalseWhenAllColorsAreOut() {
        val filament = base.copy(
            colors = listOf(
                FilamentColor(id = "red", inStock = false),
                FilamentColor(id = "blue", inStock = false),
            ),
        )

        assertFalse(filament.hasStockAvailable)
    }
}
