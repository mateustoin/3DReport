package com.threedreport.app.data

import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderSettings
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Configurações novas da Leva 11: as de Pedidos em `order-settings.json` e as do Pix e das mensagens na marca. */
class OrderSettingsRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsNeutral() {
        val settings = OrderSettingsRepository().settings.value

        assertEquals(0.0, settings.defaultDepositRate)
        assertEquals(3, settings.staleQuoteDays)
        assertNull(settings.trackingUrlTemplate)
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, settings.effectiveTrackingUrlTemplate(Currency.BRL))
    }

    @Test
    fun changesSurviveReopening() {
        OrderSettingsRepository().update(OrderSettings(defaultDepositRate = 0.3, staleQuoteDays = 0, trackingUrlTemplate = ""))

        val reloaded = OrderSettingsRepository().settings.value
        assertEquals(0.3, reloaded.defaultDepositRate)
        assertEquals(0, reloaded.staleQuoteDays)
        assertNull(reloaded.trackingUrl("AA1", Currency.BRL), "site apagado fica apagado, não volta ao padrão")
    }

    @Test
    fun pixAndEditedTemplatesLiveInTheBranding() {
        val repository = BrandingRepository()
        val edited = repository.branding.value
            .copy(pixKey = "a@b.co", pixReceiverName = "Ana", pixCity = "Campinas")
            .withMessageTemplate(MessageTemplateKind.READY, text = "{peca} pronto!", defaultText = "{peca} ficou pronto!")

        repository.update(edited)

        val reloaded = BrandingRepository().branding.value
        assertEquals("a@b.co", reloaded.pixKey)
        assertEquals("{peca} pronto!", reloaded.editedMessageTemplate(MessageTemplateKind.READY))
        assertEquals(mapOf("ready" to "{peca} pronto!"), reloaded.messageTemplates)
    }

    @Test
    fun aTemplateBackToTheDefaultIsNotSaved() {
        val branding = BrandingSettings(messageTemplates = mapOf("ready" to "x", "futuro" to "de outra versão"))

        val reset = branding.withMessageTemplate(MessageTemplateKind.READY, text = "{peca} ficou pronto!", defaultText = "{peca} ficou pronto!")
        val blank = branding.withMessageTemplate(MessageTemplateKind.READY, text = "  ", defaultText = "{peca} ficou pronto!")

        assertNull(reset.editedMessageTemplate(MessageTemplateKind.READY))
        assertNull(blank.editedMessageTemplate(MessageTemplateKind.READY))
        assertEquals(mapOf("futuro" to "de outra versão"), reset.messageTemplates, "modelo de outra versão fica no arquivo")
    }
}
