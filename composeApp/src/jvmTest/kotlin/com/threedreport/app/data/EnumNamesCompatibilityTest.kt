package com.threedreport.app.data

import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os nomes de [OrderStatus], [QuoteKind] e [ThemeMode] gravados em disco não mudam quando o rótulo sai do
 * `core` (Leva 10B, parte 7): um arquivo gravado antes continua abrindo, e o que se grava agora sai igual.
 */
class EnumNamesCompatibilityTest {

    @Test
    fun orderStatusKeepsItsStoredNames() {
        val stored = mapOf(
            OrderStatus.ORCADO to "quoted",
            OrderStatus.APROVADO to "approved",
            OrderStatus.EM_IMPRESSAO to "printing",
            OrderStatus.PRONTO to "ready",
            OrderStatus.ENTREGUE to "delivered",
            OrderStatus.CANCELADO to "cancelled",
        )
        assertEquals(OrderStatus.entries.toSet(), stored.keys)
        for ((status, name) in stored) {
            assertEquals("\"$name\"", dataJson.encodeToString(OrderStatus.serializer(), status))
            assertEquals(status, dataJson.decodeFromString(OrderStatus.serializer(), "\"$name\""))
        }
    }

    @Test
    fun quoteKindAndThemeModeKeepTheirStoredNames() {
        assertEquals(listOf("ORDER", "PRODUCT"), QuoteKind.entries.map { dataJson.encodeToString(QuoteKind.serializer(), it).trim('"') })
        assertEquals(listOf("LIGHT", "DARK", "SYSTEM"), ThemeMode.entries.map { dataJson.encodeToString(ThemeMode.serializer(), it).trim('"') })
        assertEquals(ThemeMode.DARK, dataJson.decodeFromString(ThemeMode.serializer(), "\"DARK\""))
    }

    @Test
    fun aSavedProductWithStatusHistoryFromBeforeStillOpens() {
        val json = """
            {"id":"1","name":"Peça","savedAtEpochMillis":0,"status":"printing","kind":"PRODUCT",
             "statusHistory":[{"status":"approved","atEpochMillis":10},{"status":"printing","atEpochMillis":20}],
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":23.0}}
        """.trimIndent()
        val saved = dataJson.decodeFromString(SavedQuote.serializer(), json)

        assertEquals(OrderStatus.EM_IMPRESSAO, saved.status)
        assertEquals(QuoteKind.PRODUCT, saved.kind)
        assertEquals(listOf(OrderStatus.APROVADO, OrderStatus.EM_IMPRESSAO), saved.statusHistory.map { it.status })
    }
}
