package com.threedreport.app.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Barra de rolagem horizontal visível. No desktop, sem ela, quem não tem trackpad não descobre que o
 * Kanban continua pra direita (a roda do mouse rola na vertical).
 */
@Composable
expect fun HorizontalScrollbarFor(scrollState: ScrollState, modifier: Modifier = Modifier)
