package com.threedreport.app.platform

import _3dreport.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking

/**
 * Os arquivos de fonte do app (Inter e Space Grotesk, licença OFL), lidos dos mesmos recursos que o Compose usa, pra o
 * PDF e a imagem do cliente saírem na mesma tipografia da interface. Lidos uma vez só.
 *
 * `space_grotesk_bold_tnum.ttf` é o `space_grotesk.ttf` (arquivo variável) fixado no peso 700 e com os algarismos
 * tabulares no lugar dos comuns: o PDF e o Java2D não variam o peso de uma fonte variável nem aplicam `tnum`.
 */
internal object ClientDocumentFonts {
    /** Inter 400: texto corrido. */
    val interRegular: ByteArray by lazy { read("font/inter_regular.ttf") }

    /** Inter 500: o nome da peça na imagem. */
    val interMedium: ByteArray by lazy { read("font/inter_medium.ttf") }

    /** Inter 600: valores e datas. */
    val interSemiBold: ByteArray by lazy { read("font/inter_semibold.ttf") }

    /** Space Grotesk 700 com algarismos tabulares: marca, título e preço. */
    val spaceGroteskBold: ByteArray by lazy { read("font/space_grotesk_bold_tnum.ttf") }

    private fun read(path: String): ByteArray = runBlocking { Res.readBytes(path) }
}
