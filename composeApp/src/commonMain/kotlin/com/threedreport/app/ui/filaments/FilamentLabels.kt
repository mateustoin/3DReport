package com.threedreport.app.ui.filaments

import com.threedreport.app.ui.format.toWeightText
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentTotal

/** O nome escrito da cor ou, na falta dele, o hex; `null` se nenhum estiver preenchido (a tela escolhe o rótulo genérico). */
fun FilamentColor.labelOrNull(): String? = name?.takeIf { it.isNotBlank() } ?: hex

/**
 * Texto de exibição de uma cor pras telas antigas (Orçamento, Pedidos), que ainda montam texto fora de recurso e
 * migram quando forem reescritas. A tela de Filamentos usa [labelOrNull] e o recurso `filaments_color_unnamed`.
 */
fun FilamentColor.displayLabel(): String = labelOrNull() ?: "Sem nome"

/**
 * "PLA · Preto 42 g": consumo de um filamento no pedido, como o vendedor confere no estoque. A cor
 * só aparece quando foi registrada (filamento de uma cor só não tem o que diferenciar).
 */
fun FilamentTotal.displayText(): String =
    filament.name + (color?.takeIf { it.name != null || it.hex != null }?.let { " · ${it.displayLabel()}" } ?: "") + " " + weightGrams.toWeightText()
