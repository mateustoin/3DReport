package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrandingSettingsTest {

    @Test
    fun contactLinesFollowAFixedOrderAndSkipBlanks() {
        val branding = BrandingSettings(contactWhatsApp = " (11) 99999-0000 ", contactEmail = "  ", contactInstagram = "minhaloja")

        assertEquals(listOf("WhatsApp (11) 99999-0000", "@minhaloja"), branding.contactLines)
    }

    @Test
    fun instagramGetsExactlyOneAt() {
        assertEquals("@loja", BrandingSettings(contactInstagram = "@loja").instagramHandle)
        assertEquals("@loja", BrandingSettings(contactInstagram = "loja").instagramHandle)
        assertEquals(null, BrandingSettings(contactInstagram = " @ ").instagramHandle)
    }

    @Test
    fun identityIsLogoOrContactButNotTheNameAlone() {
        assertFalse(BrandingSettings(brandName = "Minha Loja").hasIdentity)
        assertTrue(BrandingSettings(logoFileName = "logo.png").hasIdentity)
        assertTrue(BrandingSettings(contactEmail = "a@b.com").hasIdentity)
    }
}
