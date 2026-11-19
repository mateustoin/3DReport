package com.threedreport.app.platform

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.threedreport.core.pix.PixKey
import com.threedreport.core.pix.PixReceiver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** QR Code do Pix (decisão 145, item 4): a ZXing lê de volta exatamente o copia e cola. */
class PixQrCodeTest {

    private val payload = PixReceiver(PixKey("+5511999990000", com.threedreport.core.pix.PixKeyType.PHONE), "ATELIE DA ANA", "SAO PAULO")
        .code(amount = 174.34, orderNumber = 42)

    /** Desenha a matriz com [pixelsPerModule] pixels por módulo e lê com a ZXing. */
    private fun decode(matrix: QrMatrix, pixelsPerModule: Int = 4): com.google.zxing.Result {
        val side = matrix.size * pixelsPerModule
        val pixels = IntArray(side * side) { index ->
            val x = (index % side) / pixelsPerModule
            val y = (index / side) / pixelsPerModule
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
        return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.PURE_BARCODE to true))
    }

    @Test
    fun zxingReadsBackTheSamePayload() {
        val matrix = pixQrMatrix(payload)

        val result = decode(matrix)

        assertEquals(payload, result.text)
        assertEquals("M", result.resultMetadata[ResultMetadataType.ERROR_CORRECTION_LEVEL])
    }

    @Test
    fun theMatrixHasAQuietZoneOfFourModules() {
        val matrix = pixQrMatrix(payload)

        for (i in 0 until matrix.size) {
            for (border in 0 until 4) {
                assertFalse(matrix[i, border])
                assertFalse(matrix[border, i])
                assertFalse(matrix[i, matrix.size - 1 - border])
                assertFalse(matrix[matrix.size - 1 - border, i])
            }
        }
        // O canto do primeiro padrão de posição começa logo depois da margem.
        assertTrue(matrix[4, 4])
    }

    @Test
    fun theMatrixIsSquare() {
        val matrix = pixQrMatrix(payload)

        assertEquals(matrix.size * matrix.size, matrix.modules.size)
        // Versão do QR mais a margem dos dois lados: (17 + 4 × versão) + 8.
        assertEquals(0, (matrix.size - 8 - 17) % 4)
    }
}
