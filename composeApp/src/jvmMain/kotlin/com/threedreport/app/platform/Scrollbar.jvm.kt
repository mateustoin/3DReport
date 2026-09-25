package com.threedreport.app.platform

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun HorizontalScrollbarFor(scrollState: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(adapter = rememberScrollbarAdapter(scrollState), modifier = modifier)
}
