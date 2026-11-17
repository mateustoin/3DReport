package com.threedreport.app.ui.navigation

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.nav_about
import _3dreport.composeapp.generated.resources.nav_catalog
import _3dreport.composeapp.generated.resources.nav_consumables
import _3dreport.composeapp.generated.resources.nav_dashboard
import _3dreport.composeapp.generated.resources.nav_filaments
import _3dreport.composeapp.generated.resources.nav_group_registries
import _3dreport.composeapp.generated.resources.nav_group_sales
import _3dreport.composeapp.generated.resources.nav_orders
import _3dreport.composeapp.generated.resources.nav_printers
import _3dreport.composeapp.generated.resources.nav_quote
import _3dreport.composeapp.generated.resources.nav_services
import _3dreport.composeapp.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

/** O nome da tela na barra lateral e nos atalhos, em recurso traduzível. */
fun AppDestination.labelRes(): StringResource = when (this) {
    AppDestination.QUOTE -> Res.string.nav_quote
    AppDestination.ORDERS -> Res.string.nav_orders
    AppDestination.CATALOG -> Res.string.nav_catalog
    AppDestination.DASHBOARD -> Res.string.nav_dashboard
    AppDestination.FILAMENTS -> Res.string.nav_filaments
    AppDestination.PRINTERS -> Res.string.nav_printers
    AppDestination.SERVICES -> Res.string.nav_services
    AppDestination.CONSUMABLES -> Res.string.nav_consumables
    AppDestination.SETTINGS -> Res.string.nav_settings
    AppDestination.ABOUT -> Res.string.nav_about
}

/** O título do grupo na barra lateral; `null` pro pé, que não tem. */
fun DestinationGroup.titleRes(): StringResource? = when (this) {
    DestinationGroup.SALES -> Res.string.nav_group_sales
    DestinationGroup.REGISTRIES -> Res.string.nav_group_registries
    DestinationGroup.FOOTER -> null
}
