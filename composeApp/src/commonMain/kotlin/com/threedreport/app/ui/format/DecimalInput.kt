package com.threedreport.app.ui.format

/** Converte texto digitado pelo usuário (aceita `,` ou `.` como separador decimal) em [Double]. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
