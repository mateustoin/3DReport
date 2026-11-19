package com.threedreport.app.data

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_auto_name
import _3dreport.composeapp.generated.resources.seed_printer_name
import com.threedreport.app.data.store.DataFile
import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.data.store.RecordCollection
import com.threedreport.app.data.store.StorageHealth
import com.threedreport.app.data.store.StoredDocument
import com.threedreport.app.data.store.StoredRecord
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.AppLocaleState
import com.threedreport.app.platform.fillTemplate
import com.threedreport.app.platform.formatDateTime
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.QuoteTemplate
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockMovement
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.getString
import java.io.File

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

    private fun <T> recordFile(
        name: String,
        serializer: KSerializer<T>,
        upgrade: ((JsonElement) -> JsonElement)? = null,
        upgradeNeeded: (String) -> Boolean = { true },
    ): DataFile<List<StoredRecord<T>>> =
        wrapper.wrap(JsonDataFile(File(dataDir, name), ListSerializer(StoredRecord.serializer(serializer)), health, upgrade, upgradeNeeded))

    private fun <T> records(
        name: String,
        serializer: KSerializer<T>,
        idOf: (T) -> String,
        seed: () -> List<T> = { emptyList() },
        upgrade: ((JsonElement) -> JsonElement)? = null,
        upgradeNeeded: (String) -> Boolean = { true },
    ) = RecordCollection(recordFile(name, serializer, upgrade, upgradeNeeded), idOf, clock, seed)

    private fun <T> document(name: String, serializer: KSerializer<T>, default: () -> T) =
        DocumentValue(wrapper.wrap(JsonDataFile(File(dataDir, name), StoredDocument.serializer(serializer), health)), clock, default)

    fun filaments(): FilamentRepository = RecordFilamentRepository(records("filaments.json", Filament.serializer(), { it.id }, Seeds::filaments))

    fun printers(): PrinterRepository = RecordPrinterRepository(records("printers.json", PrinterProfile.serializer(), { it.id }, { Seeds.printers(loadSeedPrinterName()) }))

    fun services(): ServiceRepository = RecordServiceRepository(records("services.json", Service.serializer(), { it.id }))

    fun salesChannels(): SalesChannelRepository = RecordSalesChannelRepository(records("channels.json", SalesChannel.serializer(), { it.id }))
    fun consumables(): ConsumableRepository = RecordConsumableRepository(records("consumables.json", Consumable.serializer(), { it.id }))

    fun templates(): TemplateRepository = RecordTemplateRepository(records("templates.json", QuoteTemplate.serializer(), { it.id }))

    fun clients(): ClientRepository = RecordClientRepository(records("clients.json", Client.serializer(), { requireNotNull(it.id) { "cliente do cadastro sem id" } }))

    fun maintenance(): MaintenanceRepository = StoredMaintenanceRepository(
        components = records("maintenance-components.json", MaintenanceComponent.serializer(), { it.id }),
        log = records("maintenance-log.json", MaintenanceLogEntry.serializer(), { it.id }),
        manualUsage = records("maintenance-usage.json", ManualUsageEntry.serializer(), { it.id }),
    )

    /**
     * Estoque e compras (Leva 12), em três arquivos novos. Uma pasta sem eles (gravada antes da 2.2 ter estoque) abre com
     * tudo vazio, e nada é gravado até alguém ligar um controle ou registrar uma compra. Quem monta o app liga o histórico
     * e o cadastro de filamentos a ele ([reconcilingStock] e [deletingStockWith]).
     */
    fun stock(): StockRepository = StoredStockRepository(
        itemRecords = records("stock-items.json", StockItem.serializer(), { it.id }),
        movementRecords = records("stock-movements.json", StockMovement.serializer(), { it.id }),
        purchaseRecords = records("purchases.json", Purchase.serializer(), { it.id }),
        clock = clock,
    )

    /**
     * O histórico de orçamentos. [defaultDepositRate] é o sinal padrão de hoje (do [OrderSettingsRepository] do
     * contêiner), lido a cada pedido novo; sem ele, sinal zero.
     */
    fun quoteHistory(defaultDepositRate: () -> Double = { 0.0 }): QuoteHistoryRepository = StoredQuoteHistoryRepository(
        collection = records("quotes.json", SavedQuote.serializer(), { it.id }, upgrade = ::markLegacyAutoNamedQuotes, upgradeNeeded = ::quotesNeedAutoNamedUpgrade),
        lastNumber = document("quote-number.json", Int.serializer()) { 0 },
        attachments = attachments,
        clock = clock,
        defaultName = ::defaultQuoteName,
        defaultDepositRate = defaultDepositRate,
    ).also { warmUpAutoNameTemplate() }

    fun settings(): SettingsRepository = StoredSettingsRepository(document("settings.json", PricingSettings.serializer()) { Seeds.settings })

    fun orderSettings(): OrderSettingsRepository = StoredOrderSettingsRepository(document("order-settings.json", OrderSettings.serializer()) { OrderSettings() })

    fun branding(): BrandingRepository = StoredBrandingRepository(document("branding.json", BrandingSettings.serializer()) { BrandingSettings() }, attachments)

    fun theme(): ThemeRepository = StoredThemeRepository(document("theme.json", ThemeMode.serializer()) { ThemeMode.SYSTEM })

    fun currency(): CurrencyRepository = StoredCurrencyRepository(document("currency.json", Currency.serializer()) { Currency.BRL })

    fun onboarding(): OnboardingRepository = StoredOnboardingRepository(document("onboarding.json", Boolean.serializer()) { false })

    fun preferences(): PreferencesRepository = StoredPreferencesRepository(document("preferences.json", AppPreferences.serializer()) { AppPreferences() })

    fun backup(): BackupRepository = LocalBackupRepository(dataDir)

    /**
     * Se esta pasta ainda não tem dado nenhum: uma instalação nova (decisão 16a da Leva 11B). Lê antes de qualquer repositório
     * gravar, e quem já usa o app (qualquer arquivo de dados) fica de fora.
     */
    fun isNewInstall(): Boolean = INSTALL_MARKERS.none { File(dataDir, it).exists() }

    companion object {
        private val INSTALL_MARKERS = listOf("onboarding.json", "currency.json", "settings.json", "quotes.json", "filaments.json", "printers.json")

        const val ATTACHMENTS_DIR = "attachments"

        /** O modelo do nome automático ("Orçamento - %1$s") e o idioma em que foi lido, lido uma vez por idioma e guardado. */
        private class AutoNameTemplate(val locale: AppLocaleState, val text: String)

        @Volatile
        private var autoNameTemplate: AutoNameTemplate? = null

        /**
         * O modelo no idioma em uso. Guardado de outro idioma (a pessoa acabou de trocar), é lido de novo: o nome vai pro
         * arquivo, então não pode sair no idioma de antes. A troca já relê em segundo plano ([reloadTexts]); ler aqui, na
         * thread da tela, só acontece se alguém salvar no instante da troca.
         */
        private fun loadAutoNameTemplate(): String {
            val locale = AppLocale.current.value
            autoNameTemplate?.takeIf { it.locale == locale }?.let { return it.text }
            return AutoNameTemplate(locale, runBlocking { getString(Res.string.quote_auto_name) }).also { autoNameTemplate = it }.text
        }

        /**
         * Relê os textos guardados pra o idioma em uso (decisão 152). Quem troca o idioma chama, fora da thread da tela,
         * depois de [com.threedreport.app.platform.DesktopLocale] trocar o idioma da JVM.
         */
        fun reloadTexts() {
            loadAutoNameTemplate()
        }

        /**
         * O nome da impressora de exemplo. Só é lido quando o `printers.json` ainda não existe, ou seja, na primeira
         * abertura do app, no `main` e antes de a janela subir: nunca na thread da tela.
         */
        private fun loadSeedPrinterName(): String = runBlocking { getString(Res.string.seed_printer_name) }

        /** Lê o modelo na abertura do app, pra o primeiro salvamento não esperar o arquivo de textos na thread da tela. */
        private fun warmUpAutoNameTemplate() {
            loadAutoNameTemplate()
        }

        /**
         * Nome que o app dá quando o campo fica em branco ("Orçamento - 24/09/2026 14:30"), no momento [epochMillis].
         * O texto vem de `strings.xml` (`quote_auto_name`); quem pergunta se o nome é automático lê
         * [SavedQuote.autoNamed], e não o texto. O modelo é lido uma vez ([warmUpAutoNameTemplate]) e guardado: salvar
         * não bloqueia a thread da tela a cada vez. O texto e a data saem no idioma em uso (item 13 da Leva 11B): o modelo
         * guardado é de um idioma só, e a data vem do [AppLocale].
         */
        fun defaultQuoteName(epochMillis: Long): String = fillTemplate(loadAutoNameTemplate(), formatDateTime(epochMillis))
    }
}

/**
 * Se o texto do `quotes.json` pode ter registro sem a chave `autoNamed` (gravado pela 2.1), caso em que o
 * [markLegacyAutoNamedQuotes] precisa completar. Conta, sem montar árvore, as chaves `"data"` e `"autoNamed"` entre aspas:
 * texto digitado que as cite vem escapado (`\"autoNamed\"`) e não conta. Na dúvida (menos chaves que registros) completa.
 */
internal fun quotesNeedAutoNamedUpgrade(text: String): Boolean = countKey(text, "\"data\"") > countKey(text, "\"autoNamed\"")

private fun countKey(text: String, key: String): Int {
    var count = 0
    var from = text.indexOf(key)
    while (from >= 0) {
        count++
        from = text.indexOf(key, from + key.length)
    }
    return count
}
