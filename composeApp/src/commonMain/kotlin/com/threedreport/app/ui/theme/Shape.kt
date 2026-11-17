package com.threedreport.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Raios do sistema de design: 6 (selo), 8 (botão, campo, item da navegação), 10 (cartão, bloco),
 * 12 (coluna do kanban, diálogo). Nos papéis do Material 3: extraSmall, small, medium, large/extraLarge.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
