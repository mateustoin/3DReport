package com.threedreport.app.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

actual fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(FORMATTER)
