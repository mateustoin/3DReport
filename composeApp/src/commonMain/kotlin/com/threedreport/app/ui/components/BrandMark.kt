package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType

/**
 * A marca: "3D" em branco e "Report" em âmbar, sobre o azul-marinho (a barra lateral e o Sobre). Com [compact], só o "3D".
 * Quem usa põe o fundo `navy`.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.appColors
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append("3D") }
            if (!compact) withStyle(SpanStyle(color = colors.amber)) { append("Report") }
        },
        style = MaterialTheme.appType.brand,
        maxLines = 1,
        modifier = modifier.semantics { contentDescription = "3DReport" },
        textAlign = if (compact) TextAlign.Center else TextAlign.Start,
    )
}
