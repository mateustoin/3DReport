package com.threedreport.app.ui.quote

/** Entradas da tela de Orçamento controladas pelo usuário (o resto vem dos repositórios). */
data class QuoteInputState(
    val filamentId: String? = null,
    val filamentColorId: String? = null,
    val printerId: String? = null,
    val lengthMetersText: String = "",
    val printTimeMinutesText: String = "",
    val selectedServiceIds: Set<String> = emptySet(),
    val appliesMarketplaceFee: Boolean = false,
    /** Mensagem sobre a última tentativa de importar dados de um G-code, exibida abaixo do botão. */
    val gcodeImportMessage: String? = null,
)
