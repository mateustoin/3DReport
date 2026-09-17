package com.threedreport.app.ui.filaments

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilamentColorTest {

    @Test
    fun nullOrBlankHexReturnsNull() {
        assertNull(parseHexColor(null))
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("   "))
    }

    @Test
    fun sixDigitHexParsesAsFullyOpaque() {
        assertEquals(Color(0xFFE53935), parseHexColor("#E53935"))
    }

    @Test
    fun eightDigitHexParsesWithAlpha() {
        assertEquals(Color(0x80E53935), parseHexColor("#80E53935"))
    }

    @Test
    fun invalidHexReturnsNull() {
        assertNull(parseHexColor("not-a-color"))
        assertNull(parseHexColor("#ABC"))
    }
}
