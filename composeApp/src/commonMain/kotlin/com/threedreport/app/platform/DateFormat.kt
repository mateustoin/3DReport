package com.threedreport.app.platform

/** Formata um instante (epoch millis) para exibição, ex.: "15/09/2026 18:42". */
expect fun formatDateTime(epochMillis: Long): String
