package com.threedreport.app

import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.data.WriteBehind
import com.threedreport.app.data.appDataDir
import com.threedreport.app.data.store.StorageHealth
import kotlinx.coroutines.CoroutineScope

/**
 * Monta o [AppContainer] do desktop sobre a pasta de dados, com gravação em segundo plano, e limpa os
 * anexos que ninguém mais usa (fotos de orçamentos que saíram da lixeira, logos trocadas, anexos
 * escolhidos e não salvos). Lança [com.threedreport.app.data.DataReadException] se um arquivo de dados
 * existe e não dá pra ler do disco.
 */
fun createDesktopContainer(scope: CoroutineScope): AppContainer {
    val health = StorageHealth()
    val writeBehind = WriteBehind(scope, health)
    val storage = LocalStorage(appDataDir(), Clock.System, health, writeBehind)
    val quoteHistory = storage.quoteHistory()
    val branding = storage.branding()

    runCatching {
        storage.attachments.collectGarbage(quoteHistory.referencedAttachments() + setOfNotNull(branding.branding.value.logoFileName))
    }.onFailure { AppLog.warn("Limpeza de anexos falhou", it) }

    return AppContainer(
        filaments = storage.filaments(),
        printers = storage.printers(),
        services = storage.services(),
        salesChannels = storage.salesChannels(),
        templates = storage.templates(),
        clients = storage.clients(),
        maintenance = storage.maintenance(),
        quoteHistory = quoteHistory,
        settings = storage.settings(),
        branding = branding,
        theme = storage.theme(),
        currency = storage.currency(),
        usageProfile = storage.usageProfile(),
        onboarding = storage.onboarding(),
        preferences = storage.preferences(),
        backup = storage.backup(),
        storageHealth = health,
        pendingWrites = writeBehind,
    )
}
