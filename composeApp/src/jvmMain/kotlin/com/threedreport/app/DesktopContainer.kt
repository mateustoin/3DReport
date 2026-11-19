package com.threedreport.app

import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.data.WriteBehind
import com.threedreport.app.data.appDataDir
import com.threedreport.app.data.deletingStockWith
import com.threedreport.app.data.reconcilingStock
import com.threedreport.app.data.store.StorageHealth
import com.threedreport.app.platform.DesktopLocale
import com.threedreport.app.platform.GitHubReleaseSource
import com.threedreport.app.platform.suggestedCurrency
import com.threedreport.app.update.DesktopUpdateInstaller
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
    // Antes de qualquer repositório gravar: só instalação nova sugere a moeda pela região do sistema (decisão 16a).
    val newInstall = storage.isNewInstall()
    val orderSettings = storage.orderSettings()
    // Estoque (Leva 12): os pedidos dão baixa ao ficar prontos, e excluir uma cor, um filamento ou um insumo apaga o
    // controle dele. A lista de pedidos de agora é só a base: abrir o app não desconta nada.
    val stock = storage.stock()
    val filaments = storage.filaments().deletingStockWith(stock)
    val consumables = storage.consumables().deletingStockWith(stock)
    val quoteHistory = storage.quoteHistory { orderSettings.settings.value.defaultDepositRate }.reconcilingStock(stock) { filaments.filaments.value }
    val branding = storage.branding()

    runCatching {
        storage.attachments.collectGarbage(quoteHistory.referencedAttachments() + setOfNotNull(branding.branding.value.logoFileName))
    }.onFailure { AppLog.warn("Limpeza de anexos falhou", it) }

    return AppContainer(
        filaments = filaments,
        printers = storage.printers(),
        services = storage.services(),
        salesChannels = storage.salesChannels(),
        consumables = consumables,
        templates = storage.templates(),
        clients = storage.clients(),
        maintenance = storage.maintenance(),
        stock = stock,
        quoteHistory = quoteHistory,
        settings = storage.settings(),
        branding = branding,
        orderSettings = orderSettings,
        theme = storage.theme(),
        currency = storage.currency().also { if (newInstall) it.update(suggestedCurrency(DesktopLocale.systemRegion)) },
        onboarding = storage.onboarding(),
        preferences = storage.preferences(),
        backup = storage.backup(),
        storageHealth = health,
        pendingWrites = writeBehind,
        releases = GitHubReleaseSource(APP_VERSION),
        updateInstaller = DesktopUpdateInstaller(APP_VERSION),
    )
}
