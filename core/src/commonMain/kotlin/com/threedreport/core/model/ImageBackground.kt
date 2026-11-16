package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Fundo da imagem quadrada pro cliente (Leva 10B, parte 7), escolhido em Documentos pro cliente e guardado em
 * [BrandingSettings.imageBackground]. Sem texto: a interface escolhe o rótulo. Os nomes das constantes são o que vai
 * pro arquivo.
 */
@Serializable
enum class ImageBackground {
    /** Pelo brilho da peça ([resolve]): peça escura no fundo claro, as demais no azul-marinho. */
    AUTO,

    /** Sempre o azul-marinho. */
    DARK,

    /** Sempre o claro. */
    LIGHT,
    ;

    /**
     * O fundo que a imagem usa. [averageLuminance] é o brilho médio da peça, de 0 (preto) a 1 (branco), em luminância
     * relativa, ou `null` quando não há o que medir (sem foto, ou foto toda transparente), e aí fica o azul-marinho.
     */
    fun resolve(averageLuminance: Double?): ImageTone = when (this) {
        DARK -> ImageTone.DARK
        LIGHT -> ImageTone.LIGHT
        AUTO -> if (averageLuminance != null && averageLuminance < DARK_PIECE_LUMINANCE) ImageTone.LIGHT else ImageTone.DARK
    }

    companion object {
        /**
         * Abaixo desta luminância média, a peça é escura demais pro azul-marinho e vai pro fundo claro. É o ponto em
         * que o contraste com o azul-marinho (`#0C2742`, luminância 0,019) cai abaixo de 3:1, o mínimo que a WCAG
         * pede pra um objeto que não é texto: (0,16 + 0,05) / (0,019 + 0,05) ≈ 3. Cinza médio (#808080, 0,216) fica
         * no azul; cinza escuro (#555555, 0,091) e preto vão pro claro.
         */
        const val DARK_PIECE_LUMINANCE = 0.16
    }
}

/** O fundo que a imagem de fato usa, depois de [ImageBackground.resolve]. */
enum class ImageTone {
    DARK,
    LIGHT,
}
