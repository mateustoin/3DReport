package com.threedreport.core.model

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelFileKindTest {

    @Test
    fun kindComesFromTheExtensionIgnoringCase() {
        assertEquals(ModelFileKind.STL, ModelFileKind.of("peça.stl"))
        assertEquals(ModelFileKind.STL, ModelFileKind.of("PEÇA.STL"))
        assertEquals(ModelFileKind.THREE_MF, ModelFileKind.of("projeto.3mf"))
        assertEquals(ModelFileKind.THREE_MF, ModelFileKind.of("0f3a…9c.3MF"))
        assertNull(ModelFileKind.of("foto.png"))
        assertNull(ModelFileKind.of("sem-extensao"))
    }

    @Test
    fun extensionToSaveTheFileBack() {
        assertEquals("stl", ModelFileKind.STL.extension)
        assertEquals("3mf", ModelFileKind.THREE_MF.extension)
    }

    @Test
    fun savedQuoteKnowsTheKindOfItsModelFile() {
        val base = SavedQuote(
            id = "1",
            name = "Peça",
            quote = Quote(prints = listOf(quotedPrint(PrintJob(filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24), filamentLengthMeters = 1.0, printTimeMinutes = 10.0))), costs = costsOf(), salePrice = 10.0),
            savedAtEpochMillis = 0L,
        )
        assertNull(base.modelFileKind)
        assertEquals(ModelFileKind.STL, base.copy(modelFileName = "abc.stl").modelFileKind)
        assertEquals(ModelFileKind.THREE_MF, base.copy(modelFileName = "abc.3mf").modelFileKind)
        // Arquivo gravado por versão antiga, que só aceitava STL, pode ter outra extensão (".bin"): continua STL.
        assertEquals(ModelFileKind.STL, base.copy(modelFileName = "abc.bin").modelFileKind)
    }
}
