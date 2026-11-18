package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrandingSettingsTest {

    @Test
    fun contactsFollowAFixedOrderAndSkipBlanks() {
        val branding = BrandingSettings(contactWhatsApp = " (11) 99999-0000 ", contactEmail = "  ", contactInstagram = "minhaloja")

        assertEquals(
            listOf(ContactEntry(ContactChannel.WHATSAPP, "(11) 99999-0000"), ContactEntry(ContactChannel.INSTAGRAM, "@minhaloja")),
            branding.contacts,
        )
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

    @Test
    fun onlyTheEditedMessageTemplatesAreKept() {
        val kind = com.threedreport.core.message.MessageTemplateKind.FOLLOW_UP
        val edited = BrandingSettings().withMessageTemplate(kind, text = "Oi, {cliente}! E aí?", defaultText = "Oi, {cliente}! Conseguiu ver?")

        assertEquals(mapOf("followUp" to "Oi, {cliente}! E aí?"), edited.messageTemplates)
        assertEquals(emptyMap(), edited.withMessageTemplate(kind, text = " Oi, {cliente}! Conseguiu ver? ", defaultText = "Oi, {cliente}! Conseguiu ver?").messageTemplates)
        assertEquals(emptyMap(), edited.withMessageTemplate(kind, text = "", defaultText = "x").messageTemplates)
    }
}
