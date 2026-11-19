package com.threedreport.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.threedreport.app.ui.icons.AppIcons

/** Grupo da barra lateral (decisão 111). O pé da barra não tem título; os nomes vêm de [titleRes]. */
enum class DestinationGroup {
    SALES,
    REGISTRIES,
    FOOTER,
}

/**
 * As telas do app, na ordem da barra lateral (decisão 111): Vendas, Cadastros e, no pé, Configurações e
 * Sobre. A ordem também é a dos atalhos Ctrl/Cmd+1 a 9, até o Estoque (decisão 163). Configurações tem o atalho comum de
 * preferências, Ctrl/Cmd+vírgula ([isPreferences]); o Sobre fica sem atalho.
 */
enum class AppDestination(
    val group: DestinationGroup,
    private val outlinedIcon: () -> ImageVector,
    private val filledIcon: (() -> ImageVector)? = null,
) {
    QUOTE(DestinationGroup.SALES, { AppIcons.RequestQuote }, { AppIcons.RequestQuoteFilled }),
    ORDERS(DestinationGroup.SALES, { AppIcons.ReceiptLong }),
    CATALOG(DestinationGroup.SALES, { AppIcons.Storefront }),
    DASHBOARD(DestinationGroup.SALES, { AppIcons.BarChart }, { AppIcons.BarChartFilled }),
    FILAMENTS(DestinationGroup.REGISTRIES, { AppIcons.Spool }, { AppIcons.SpoolFilled }),
    PRINTERS(DestinationGroup.REGISTRIES, { AppIcons.Printer3d }, { AppIcons.Printer3dFilled }),
    SERVICES(DestinationGroup.REGISTRIES, { AppIcons.Handyman }, { AppIcons.HandymanFilled }),
    CONSUMABLES(DestinationGroup.REGISTRIES, { AppIcons.Inventory2 }, { AppIcons.Inventory2Filled }),
    STOCK(DestinationGroup.REGISTRIES, { AppIcons.Shelves }, { AppIcons.ShelvesFilled }),
    SETTINGS(DestinationGroup.FOOTER, { AppIcons.Settings }, { AppIcons.SettingsFilled }),
    ABOUT(DestinationGroup.FOOTER, { AppIcons.Info }),
    ;

    /** O número do atalho Ctrl/Cmd+N, ou `null` pra quem não tem (Configurações usa a vírgula, o Sobre não tem). */
    val shortcutNumber: Int?
        get() = if (this == ABOUT || isPreferences) null else ordinal + 1

    /** Configurações: o atalho é Ctrl/Cmd+vírgula, o de preferências de quase todo programa. */
    val isPreferences: Boolean
        get() = this == SETTINGS

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
