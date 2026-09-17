package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Uma variante de cor de um [Filament] — mesma marca/preço/densidade, rolo
 * diferente. Existe pra evitar cadastro duplicado de filamentos idênticos
 * que só diferem na cor (ex.: mesma marca de PLA em vermelho e azul).
 *
 * @property id identificador único dentro da lista [Filament.colors].
 * @property name nome da cor por escrito (ex.: "Vermelho Fosco"), opcional.
 * @property hex cor visual, em hex (ex.: "#E53935"), opcional.
 * @property inStock controle **manual** de estoque desta cor específica —
 *   ver KDoc de [Filament.colors].
 */
@Serializable
data class FilamentColor(
    val id: String,
    val name: String? = null,
    val hex: String? = null,
    val inStock: Boolean = true,
)
