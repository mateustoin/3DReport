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
 * A marca. Por padrão ([onNavy]), "3D" em branco e "Report" em âmbar, pra a barra lateral: quem usa põe o fundo `navy`.
 * Com [onNavy] desligado, a variante pra o fundo da tela (o Sobre): "3D" na cor do texto e "Report" em âmbar de texto,
 * sem bloco de fundo. Com [compact], só o "3D".
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false, onNavy: Boolean = true) {
    val colors = MaterialTheme.appColors
    val first = if (onNavy) Color.White else MaterialTheme.colorScheme.onBackground
    val second = if (onNavy) colors.amber else colors.amberText
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = first)) { append("3D") }
            if (!compact) withStyle(SpanStyle(color = second)) { append("Report") }
        },
        style = MaterialTheme.appType.brand,
        maxLines = 1,
        modifier = modifier.semantics { contentDescription = "3DReport" },
        textAlign = if (compact) TextAlign.Center else TextAlign.Start,
    )
}
