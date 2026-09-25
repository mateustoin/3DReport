package com.threedreport.app.ui.filaments

import com.threedreport.app.ui.format.toWeightText
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentTotal

/** Texto de exibição de uma cor: nome escrito, ou o hex, ou um rótulo genérico se nenhum estiver preenchido. */
fun FilamentColor.displayLabel(): String = name?.takeIf { it.isNotBlank() } ?: hex ?: "Sem nome"

/**
 * "PLA · Preto 42 g": consumo de um filamento no pedido, como o vendedor confere no estoque. A cor
 * só aparece quando foi registrada (filamento de uma cor só não tem o que diferenciar).
 */
fun FilamentTotal.displayText(): String =
    filament.name + (color?.takeIf { it.name != null || it.hex != null }?.let { " · ${it.displayLabel()}" } ?: "") + " " + weightGrams.toWeightText()
