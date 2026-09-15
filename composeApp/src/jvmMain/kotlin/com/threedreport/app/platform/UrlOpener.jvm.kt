package com.threedreport.app.platform

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    runCatching {
        val normalized = if (url.contains("://")) url else "https://$url"
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(normalized))
        }
    }
}
