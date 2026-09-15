package com.threedreport.app.ui.quote

import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote

/**
 * Estado da tela de Orçamento.
 *
 * Os campos numéricos ficam como texto (entrada livre do usuário); [quote]
 * só é preenchido quando todos os campos são válidos.
 */
data class QuoteUiState(
    val filaments: List<Filament> = emptyList(),
    val selectedFilament: Filament? = null,
    val printers: List<PrinterProfile> = emptyList(),
    val selectedPrinter: PrinterProfile? = null,
    val lengthMetersText: String = "",
    val printTimeMinutesText: String = "",
    val quote: Quote? = null,
    val errorMessage: String? = null,
)
