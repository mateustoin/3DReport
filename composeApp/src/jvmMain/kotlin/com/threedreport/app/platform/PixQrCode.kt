package com.threedreport.app.platform

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/** Margem branca em volta do QR, em módulos (o mínimo que o padrão pede pra leitura). */
const val PIX_QR_QUIET_ZONE = 4

/**
 * Matriz de módulos de um QR Code, já com a margem: [size] × [size], linha por linha em [modules] (`true` = escuro).
 * Quem desenha (o PDF) escala cada módulo pro tamanho que quiser, sem perder nitidez.
 */
class QrMatrix(val size: Int, val modules: BooleanArray) {
    init {
        require(modules.size == size * size) { "matriz de $size × $size com ${modules.size} módulos" }
    }

    /** Se o módulo da coluna [x] e linha [y] é escuro. */
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]
}

/**
 * QR Code do Pix copia e cola [payload] (decisão 145, item 4): ZXing, nível de correção M e margem de
 * [PIX_QR_QUIET_ZONE] módulos. Gerado no computador, sem internet. O texto do BR Code é só ASCII, então vai como
 * está (sem declarar outra codificação no QR, o que alguns leitores de banco não aceitam).
 */
fun pixQrMatrix(payload: String): QrMatrix {
    // Sem a dica de codificação: com ela, a ZXing grava um ECI no QR, e o padrão (ISO-8859-1) já cobre o ASCII.
    val code = Encoder.encode(payload, ErrorCorrectionLevel.M)
    val inner = code.matrix
    val size = inner.width + 2 * PIX_QR_QUIET_ZONE
    val modules = BooleanArray(size * size)
    for (y in 0 until inner.height) {
        for (x in 0 until inner.width) {
            if (inner.get(x, y).toInt() == 1) modules[(y + PIX_QR_QUIET_ZONE) * size + x + PIX_QR_QUIET_ZONE] = true
        }
    }
    return QrMatrix(size, modules)
}
