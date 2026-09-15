package com.threedreport.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.threedreport.app.platform.openUrl

/** Texto estilizado como link (sublinhado, cor de destaque) que abre [url] no navegador ao clicar. */
@Composable
fun LinkText(text: String, url: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.clickable { openUrl(url) },
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.bodyMedium,
    )
}
