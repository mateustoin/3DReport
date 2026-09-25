package com.threedreport.app

import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.OnboardingRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.data.ThemeRepository
import com.threedreport.app.data.UsageProfileRepository
import com.threedreport.app.data.store.StorageHealth

/**
 * Tudo que o app usa, montado uma vez na inicialização da plataforma (decisão 108), fora da composição:
 * antes, cada repositório era criado com `remember` dentro do `App()`, lendo o disco no meio do
 * desenho da tela. As telas recebem daqui só interfaces, então trocar uma implementação (outro
 * armazenamento, uma versão em memória pra testes ou prints) é montar outro contêiner.
 */
class AppContainer(
    val filaments: FilamentRepository,
    val printers: PrinterRepository,
    val services: ServiceRepository,
    val salesChannels: SalesChannelRepository,
    val templates: TemplateRepository,
    val clients: ClientRepository,
    val maintenance: MaintenanceRepository,
    val quoteHistory: QuoteHistoryRepository,
    val settings: SettingsRepository,
    val branding: BrandingRepository,
    val theme: ThemeRepository,
    val currency: CurrencyRepository,
    val usageProfile: UsageProfileRepository,
    val onboarding: OnboardingRepository,
    val preferences: PreferencesRepository,
    val backup: BackupRepository,
    val storageHealth: StorageHealth,
    val pendingWrites: PendingWrites = PendingWrites.None,
)

/** Gravações pedidas e ainda não confirmadas no disco (ver [com.threedreport.app.data.store.WriteBehindFile]). */
interface PendingWrites {
    val allWritten: Boolean

    /** Espera tudo o que já foi pedido chegar ao disco. */
    suspend fun awaitAll()

    /** Tenta de novo as gravações que falharam. */
    fun retry()

    object None : PendingWrites {
        override val allWritten: Boolean = true

        override suspend fun awaitAll() = Unit

        override fun retry() = Unit
    }
}

/** Um aviso sobre a pasta de dados ao abrir (decisões 104 e 106). */
data class DataFolderNotice(val kind: Kind, val path: String) {
    enum class Kind {
        /** Dados de um formato anterior sem conversão: guardados à parte, o app começa em branco. */
        MOVED_FROM_OLDER,

        /** Dados de uma versão mais nova do app: guardados à parte. */
        MOVED_FROM_NEWER,

        /** Dados de um formato anterior convertidos; a cópia de antes ficou em [path]. */
        MIGRATED,
    }
}
