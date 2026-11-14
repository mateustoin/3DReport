package com.threedreport.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.threedreport.app.ui.icons.AppIcons

/** Grupo da barra lateral (decisão 111). [title] `null` é o pé da barra, sem título. */
enum class DestinationGroup(val title: String?) {
    SALES("Vendas"),
    REGISTRIES("Cadastros"),
    FOOTER(null),
}

/**
 * As telas do app, na ordem da barra lateral (decisão 111): Vendas, Cadastros e, no pé, Configurações e
 * Sobre. A ordem também é a dos atalhos Ctrl/Cmd+1 a 9; o Sobre fica sem atalho.
 */
enum class AppDestination(
    val label: String,
    val group: DestinationGroup,
    private val outlinedIcon: () -> ImageVector,
    private val filledIcon: (() -> ImageVector)? = null,
) {
    QUOTE("Orçamento", DestinationGroup.SALES, { AppIcons.RequestQuote }, { AppIcons.RequestQuoteFilled }),
    ORDERS("Pedidos", DestinationGroup.SALES, { AppIcons.ReceiptLong }),
    CATALOG("Catálogo", DestinationGroup.SALES, { AppIcons.Storefront }),
    DASHBOARD("Dashboard", DestinationGroup.SALES, { AppIcons.BarChart }, { AppIcons.BarChartFilled }),
    FILAMENTS("Filamentos", DestinationGroup.REGISTRIES, { AppIcons.Spool }, { AppIcons.SpoolFilled }),
    PRINTERS("Impressoras", DestinationGroup.REGISTRIES, { AppIcons.Printer3d }, { AppIcons.Printer3dFilled }),
    SERVICES("Serviços", DestinationGroup.REGISTRIES, { AppIcons.Handyman }, { AppIcons.HandymanFilled }),
    CONSUMABLES("Insumos", DestinationGroup.REGISTRIES, { AppIcons.Inventory2 }, { AppIcons.Inventory2Filled }),
    SETTINGS("Configurações", DestinationGroup.FOOTER, { AppIcons.Settings }, { AppIcons.SettingsFilled }),
    ABOUT("Sobre", DestinationGroup.FOOTER, { AppIcons.Info }),
    ;

    /** O número do atalho Ctrl/Cmd+N, ou `null` pra quem não tem. */
    val shortcutNumber: Int?
        get() = if (this == ABOUT) null else ordinal + 1

    /**
     * Ícone contornado dos itens inativos e o preenchido do ativo (padrão do Material 3), quando existe
     * um preenchido. Os ícones entram por função, e não como valor, pra nenhum vetor ser montado antes
     * de a barra aparecer.
     */
    val icon: ImageVector
        get() = outlinedIcon()

    val selectedIcon: ImageVector
        get() = filledIcon?.invoke() ?: icon
}

/** A tela do atalho Ctrl/Cmd+[number], ou `null` se o número não é de nenhuma. */
fun destinationForShortcut(number: Int): AppDestination? = AppDestination.entries.firstOrNull { it.shortcutNumber == number }
