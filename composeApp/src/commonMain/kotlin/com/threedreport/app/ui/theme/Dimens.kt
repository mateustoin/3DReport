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

    /** Painel fixo à direita: o resultado do Orçamento e o formulário dos cadastros. */
    val sidePanelWidth = 380.dp
    val resultPanelWidth = sidePanelWidth
    val formMaxWidth = 760.dp

    /** Lista de cadastro: não passa disso quando o painel está fechado. */
    val listMaxWidth = 760.dp

    /** Abaixo dessa largura útil, o painel do formulário ocupa a área toda. */
    val sidePanelFullWidthBelow = 960.dp

    /**
     * Abaixo disso a tela de Orçamento volta pra uma coluna só (o painel de resultado vai pro fim). Com o painel
     * de 380 e a margem de 32 de cada lado, sobram uns 500 dp pra grade de três colunas do formulário.
     */
    val twoColumnMinWidth = 960.dp

    /** Margem de cima da tela (as laterais são [AppSpacing.xxl]). */
    val screenTopPadding = 26.dp

    /** Transições de hover, foco e troca de conteúdo (120 a 180 ms). */
    const val MOTION_MILLIS = 150

    val borderWidth = 1.dp
    val focusRingWidth = 2.dp
    val dashedBorderWidth = 1.5.dp

    val iconSize = 18.dp

    /** Caixa de marcar e o visto dentro dela. */
    val checkboxSize = 20.dp
    val checkmarkSize = 14.dp

    /** O recuo do que fica sob uma caixa de marcar: a caixa (20) e o espaço até o texto (12), pra alinhar com o rótulo. */
    val checkboxIndent = 32.dp

    /** A sombra do menu: o sistema de design só a usa em menu, dica e diálogo. */
    val menuElevation = 8.dp
    val navIconSize = 18.dp
    val sectionMarkerSize = 24.dp
    val dropZoneHeight = 56.dp

    /** As zonas de soltar do aviso de arrastar um arquivo ([com.threedreport.app.ui.components.DropZone]): largura mínima e ícone do aviso. */
    val dropZoneMinWidth = 360.dp
    val dropOverlayIconSize = 48.dp
    val compositionBarHeight = 10.dp
    val compositionGap = 2.dp
    val legendSwatchSize = 10.dp
    val pendingDotSize = 8.dp
    val thumbnailSize = 64.dp
    val printThumbnailSize = 40.dp
    val smallControlHeight = 32.dp

    /**
     * A lista de seções de Configurações, ao lado do conteúdo. Com 264, os rótulos dos campos truncavam numa janela
     * de 900; o "Não salvo" vai embaixo do nome da seção, então 200 cabe "Documentos pro cliente" inteiro.
     */
    val sectionListWidth = 200.dp

    /** Item de menu (o de ações do pedido tem 12 itens: com 48 não cabe numa janela baixa). */
    val menuItemHeight = 40.dp

    /** Altura mínima da linha de par rótulo e valor ([com.threedreport.app.ui.components.InfoRow]). */
    val infoRowHeight = 40.dp

    /** Tabela (Pedidos em lista, Catálogo em lista): linha, cabeçalho e as colunas de largura fixa. */
    val tableRowHeight = 52.dp
    val tableHeaderHeight = 34.dp
    val tableNumberColumn = 64.dp
    val tableStatusColumn = 130.dp
    val tableDeadlineColumn = 150.dp
    val tableMoneyColumn = 110.dp
    val tableCategoryColumn = 150.dp
    val tableActionColumn = 96.dp

    /** Faixa de cor da situação no topo da coluna do kanban, e a largura mínima da coluna antes de rolar. */
    val statusBandHeight = 3.dp
    val kanbanColumnMinWidth = 188.dp

    /** Da largura da coluna pra cima, o selo de prazo e o valor ficam na mesma linha do cartão; abaixo, empilham. */
    val kanbanRowMinWidth = 232.dp

    /** A foto pequena do cartão de pedido do kanban. */
    val kanbanPhotoSize = 32.dp

    /** Foto em destaque do cartão do Catálogo, e a largura mínima do cartão na grade. */
    val productPhotoHeight = 132.dp
    val productCardMinWidth = 200.dp

    /** Campo de filtro de lista (situação, período, categoria) ao lado da busca. */
    val filterFieldWidth = 200.dp

    /** A busca das listas não passa disso, pra a linha de filtros não pular ao trocar de visualização. */
    val searchMaxWidth = 400.dp

    /** Amostra de cor (filamento): a do formulário e a pequena, dentro do selo da lista. */
    val swatchSize = 24.dp
    val swatchSmallSize = 16.dp

    /** Lista com rolagem própria dentro de um diálogo (as impressoras pré-cadastradas). */
    val dialogListMaxHeight = 360.dp
    val stlViewerHeight = 280.dp
}
