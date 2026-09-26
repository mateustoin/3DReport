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
 * Sobre. A ordem também é a dos atalhos Ctrl/Cmd+1 a 8; o Sobre fica sem atalho.
 */
enum class AppDestination(val label: String, val group: DestinationGroup) {
    QUOTE("Orçamento", DestinationGroup.SALES),
    ORDERS("Pedidos", DestinationGroup.SALES),
    CATALOG("Catálogo", DestinationGroup.SALES),
    DASHBOARD("Dashboard", DestinationGroup.SALES),
    FILAMENTS("Filamentos", DestinationGroup.REGISTRIES),
    PRINTERS("Impressoras", DestinationGroup.REGISTRIES),
    SERVICES("Serviços", DestinationGroup.REGISTRIES),
    SETTINGS("Configurações", DestinationGroup.FOOTER),
    ABOUT("Sobre", DestinationGroup.FOOTER);

    /** O número do atalho Ctrl/Cmd+N, ou `null` pra quem não tem. */
    val shortcutNumber: Int?
        get() = if (this == ABOUT) null else ordinal + 1

    /**
     * Ícone contornado dos itens inativos e o preenchido do ativo (padrão do Material 3), quando existe
     * um preenchido. Por `when`, e não no construtor, pra nenhum vetor ser montado antes de a barra
     * aparecer.
     */
    val icon: ImageVector
        get() = when (this) {
            QUOTE -> AppIcons.RequestQuote
            ORDERS -> AppIcons.ReceiptLong
            CATALOG -> AppIcons.Storefront
            DASHBOARD -> AppIcons.BarChart
            FILAMENTS -> AppIcons.Spool
            PRINTERS -> AppIcons.Printer3d
            SERVICES -> AppIcons.Handyman
            SETTINGS -> AppIcons.Settings
            ABOUT -> AppIcons.Info
        }

    val selectedIcon: ImageVector
        get() = when (this) {
            QUOTE -> AppIcons.RequestQuoteFilled
            DASHBOARD -> AppIcons.BarChartFilled
            FILAMENTS -> AppIcons.SpoolFilled
            PRINTERS -> AppIcons.Printer3dFilled
            SERVICES -> AppIcons.HandymanFilled
            SETTINGS -> AppIcons.SettingsFilled
            else -> icon
        }
}

/** A tela do atalho Ctrl/Cmd+[number], ou `null` se o número não é de nenhuma. */
fun destinationForShortcut(number: Int): AppDestination? = AppDestination.entries.firstOrNull { it.shortcutNumber == number }
