package com.threedreport.app.ui.quote

/** Entradas da tela de Orçamento controladas pelo usuário (o resto vem dos repositórios). */
data class QuoteInputState(
    val filamentId: String? = null,
    val printerId: String? = null,
    val lengthMetersText: String = "",
    val printTimeMinutesText: String = "",
    val selectedServiceIds: Set<String> = emptySet(),
    val appliesMarketplaceFee: Boolean = false,
)
