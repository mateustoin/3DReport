package com.threedreport.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaço do sistema de design: 4, 8, 12, 16, 20, 24, 32. Nenhum valor fora dela. */
@Immutable
data class AppSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

/** Medidas fixas do sistema de design (alturas, larguras de coluna, tamanhos de marcador e ícone). */
object AppDimens {
    /** Campo e botão. */
    val controlHeight = 40.dp

    /** Botão principal do painel de resultado. */
    val primaryActionHeight = 46.dp

    /** Item da navegação. */
    val navItemHeight = 36.dp

    val sidebarWidth = 212.dp
    val sidebarCompactWidth = 72.dp
    val resultPanelWidth = 380.dp
    val formMaxWidth = 760.dp

    /** Margem de cima da tela (as laterais são [AppSpacing.xxl]). */
    val screenTopPadding = 26.dp

    val borderWidth = 1.dp
    val focusRingWidth = 2.dp
    val dashedBorderWidth = 1.5.dp

    val iconSize = 18.dp
    val navIconSize = 18.dp
    val sectionMarkerSize = 24.dp
    val dropZoneHeight = 56.dp
    val compositionBarHeight = 10.dp
    val compositionGap = 2.dp
    val legendSwatchSize = 10.dp
    val pendingDotSize = 8.dp
    val thumbnailSize = 64.dp
    val printThumbnailSize = 40.dp
    val smallControlHeight = 32.dp
    val stlViewerHeight = 280.dp
}
