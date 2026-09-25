package com.threedreport.app.data

import com.threedreport.app.data.store.DataFile
import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.data.store.RecordCollection
import com.threedreport.app.data.store.StorageHealth
import com.threedreport.app.data.store.StoredDocument
import com.threedreport.app.data.store.StoredRecord
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteTemplate
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.model.ThemeMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Como cada arquivo é gravado: direto (testes) ou em segundo plano ([com.threedreport.app.data.store.WriteBehindFile]). */
interface DataFileWrapper {
    fun <T> wrap(file: DataFile<T>): DataFile<T>

    /** Sem embrulho: cada mudança é gravada na hora, no próprio thread. */
    object Direct : DataFileWrapper {
        override fun <T> wrap(file: DataFile<T>): DataFile<T> = file
    }
}

/**
 * Os repositórios guardados em arquivos JSON numa pasta (decisão 108): um arquivo por assunto, cada
 * lista com registros carimbados ([StoredRecord]) e cada documento com a data da última mudança
 * ([StoredDocument]). Anexos no [FileAttachmentStore].
 *
 * Cada chamada cria um repositório novo, lendo do disco: o app cria cada um uma vez ([com.threedreport.app.AppContainer]),
 * e os testes criam de novo pra conferir que o dado voltou do disco.
 */
class LocalStorage(
    val dataDir: File = appDataDir(),
    val clock: Clock = Clock.System,
    val health: StorageHealth = StorageHealth(),
    private val wrapper: DataFileWrapper = DataFileWrapper.Direct,
) {
    val attachments: AttachmentStore by lazy { FileAttachmentStore(File(dataDir, ATTACHMENTS_DIR)) }

    private fun <T> recordFile(name: String, serializer: KSerializer<T>): DataFile<List<StoredRecord<T>>> =
        wrapper.wrap(JsonDataFile(File(dataDir, name), ListSerializer(StoredRecord.serializer(serializer)), health))

    private fun <T> records(name: String, serializer: KSerializer<T>, idOf: (T) -> String, seed: () -> List<T> = { emptyList() }) =
        RecordCollection(recordFile(name, serializer), idOf, clock, seed)

    private fun <T> document(name: String, serializer: KSerializer<T>, default: () -> T) =
        DocumentValue(wrapper.wrap(JsonDataFile(File(dataDir, name), StoredDocument.serializer(serializer), health)), clock, default)

    fun filaments(): FilamentRepository = RecordFilamentRepository(records("filaments.json", Filament.serializer(), { it.id }, Seeds::filaments))

    fun printers(): PrinterRepository = RecordPrinterRepository(records("printers.json", PrinterProfile.serializer(), { it.id }, Seeds::printers))

    fun services(): ServiceRepository = RecordServiceRepository(records("services.json", Service.serializer(), { it.id }))

    fun salesChannels(): SalesChannelRepository = RecordSalesChannelRepository(records("channels.json", SalesChannel.serializer(), { it.id }))

    fun templates(): TemplateRepository = RecordTemplateRepository(records("templates.json", QuoteTemplate.serializer(), { it.id }))

    fun clients(): ClientRepository = RecordClientRepository(records("clients.json", Client.serializer(), { requireNotNull(it.id) { "cliente do cadastro sem id" } }))

    fun maintenance(): MaintenanceRepository = StoredMaintenanceRepository(
        components = records("maintenance-components.json", MaintenanceComponent.serializer(), { it.id }),
        log = records("maintenance-log.json", MaintenanceLogEntry.serializer(), { it.id }),
        manualUsage = records("maintenance-usage.json", ManualUsageEntry.serializer(), { it.id }),
    )

    fun quoteHistory(): QuoteHistoryRepository = StoredQuoteHistoryRepository(
        collection = records("quotes.json", SavedQuote.serializer(), { it.id }),
        lastNumber = document("quote-number.json", Int.serializer()) { 0 },
        attachments = attachments,
        clock = clock,
        defaultName = ::defaultQuoteName,
    )

    fun settings(): SettingsRepository = StoredSettingsRepository(document("settings.json", PricingSettings.serializer()) { Seeds.settings })

    fun branding(): BrandingRepository = StoredBrandingRepository(document("branding.json", BrandingSettings.serializer()) { BrandingSettings() }, attachments)

    fun theme(): ThemeRepository = StoredThemeRepository(document("theme.json", ThemeMode.serializer()) { ThemeMode.SYSTEM })

    fun currency(): CurrencyRepository = StoredCurrencyRepository(document("currency.json", Currency.serializer()) { Currency.BRL })

    fun onboarding(): OnboardingRepository = StoredOnboardingRepository(document("onboarding.json", Boolean.serializer()) { false })

    fun preferences(): PreferencesRepository = StoredPreferencesRepository(document("preferences.json", AppPreferences.serializer()) { AppPreferences() })

    fun backup(): BackupRepository = LocalBackupRepository(dataDir)

    companion object {
        const val ATTACHMENTS_DIR = "attachments"

        /** Nome que o app dá quando o campo fica em branco ("Orçamento - 24/09/2026 14:30"). */
        fun defaultQuoteName(): String =
            SavedQuote.AUTO_NAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
}
