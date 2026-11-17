package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Fundo da imagem quadrada pro cliente (Leva 10B, parte 7): no automático, peça escura vai pro fundo claro. */
class ImageBackgroundTest {

    @Test
    fun aBlackPieceGoesToTheLightBackground() {
        assertEquals(ImageTone.LIGHT, ImageBackground.AUTO.resolve(averageLuminance = 0.0))
        // PLA preto fotografado (#222222): quase a mesma luminância do azul-marinho.
        assertEquals(ImageTone.LIGHT, ImageBackground.AUTO.resolve(averageLuminance = 0.016))
    }

    @Test
    fun aWhitePieceStaysOnNavy() {
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(averageLuminance = 1.0))
    }

    @Test
    fun greyIsDecidedByTheThreshold() {
        // Cinza médio (#808080, luminância 0,216) aparece bem no azul-marinho; cinza escuro (#555555, 0,091), não.
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(averageLuminance = 0.216))
        assertEquals(ImageTone.LIGHT, ImageBackground.AUTO.resolve(averageLuminance = 0.091))
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(averageLuminance = ImageBackground.DARK_PIECE_LUMINANCE))
        assertEquals(ImageTone.LIGHT, ImageBackground.AUTO.resolve(averageLuminance = ImageBackground.DARK_PIECE_LUMINANCE - 0.001))
    }

    @Test
    fun withoutAPieceToMeasureTheImageStaysOnNavy() {
        // Sem foto, ou foto toda transparente: o azul-marinho é o fundo da opção A.
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(averageLuminance = null))
    }

    @Test
    fun aChosenBackgroundIgnoresThePiece() {
        assertEquals(ImageTone.DARK, ImageBackground.DARK.resolve(averageLuminance = 0.0))
        assertEquals(ImageTone.LIGHT, ImageBackground.LIGHT.resolve(averageLuminance = 1.0))
        assertEquals(ImageTone.LIGHT, ImageBackground.LIGHT.resolve(averageLuminance = null))
    }

    @Test
    fun brandingFromBeforeOpensAsAutomatic() {
        assertEquals(ImageBackground.AUTO, BrandingSettings().imageBackground)
    }
}
