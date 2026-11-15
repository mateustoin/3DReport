package com.threedreport.app

import com.threedreport.app.data.Clock
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.about.UpdateViewModel
import com.threedreport.app.ui.consumables.ConsumableListViewModel
import com.threedreport.app.ui.dashboard.DashboardViewModel
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.history.QuoteHistoryViewModel
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.app.ui.settings.BackupViewModel
import com.threedreport.app.ui.settings.BrandingViewModel
import com.threedreport.app.ui.settings.CurrencyViewModel
import com.threedreport.app.ui.settings.SalesChannelViewModel
import com.threedreport.app.ui.settings.SettingsViewModel
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.QuoteKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Os ViewModels do app, criados uma vez no `App()`. Cada um nasce na primeira vez que alguém o lê.
 * O `App()` lê desde o primeiro desenho, e por isso eles nascem na abertura: os dois do Orçamento, as
 * Configurações (com o Branding), o tema, a moeda e a atualização. Os outros esperam a tela abrir.
 */
class AppViewModels(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    /** Pra onde "ver pedidos" leva (o Catálogo usa depois de vender): abre Pedidos. */
    private val showOrders: () -> Unit,
    private val prepareQuote: (QuoteViewModel) -> Unit = {},
    /** Hoje e agora de Pedidos e do Catálogo; só o gerador de capturas troca, pra os prazos de exemplo não envelhecerem. */
    private val today: () -> Long = ::todayEpochDay,
    private val clock: Clock = Clock.System,
) {
    private fun newQuoteViewModel() = QuoteViewModel(
        container.filaments, container.printers, container.settings, container.services,
        container.salesChannels, container.quoteHistory,
        clientRepository = container.clients,
        currency = { container.currency.currency.value },
        scope = scope,
        background = Dispatchers.Default,
        main = Dispatchers.Main,
        consumableRepository = container.consumables,
        brandingRepository = container.branding,
    )

    /** O da aba. Cedo: é a tela de abertura, e o gerador de capturas ([prepareQuote]) o preenche antes de desenhar. */
    val quoteViewModel: QuoteViewModel by lazy { newQuoteViewModel().also(prepareQuote) }

    /**
     * O da edição de um orçamento salvo (decisão 108): usar o da aba apagava o rascunho que estivesse em
     * andamento lá. A edição aparece no próprio Orçamento (decisão 113), e o rascunho da aba volta quando
     * ela termina. Cedo: a barra lateral lê o "Editando" dele a cada desenho, em qualquer tela.
     */
    val editQuoteViewModel: QuoteViewModel by lazy { newQuoteViewModel() }

    // Pedidos e Catálogo são a mesma tela com o tipo fixo (decisão 111): cada uma com o seu ViewModel,
    // pra busca, filtros e seleção de uma não vazarem pra outra.
    private fun newHistoryViewModel(kind: QuoteKind) = QuoteHistoryViewModel(
        container.quoteHistory, container.branding,
        container.filaments, container.printers, container.settings, container.salesChannels, container.preferences,
        today = today,
        clientRepository = container.clients,
        clock = clock,
        scope = scope,
        background = Dispatchers.Default,
        main = Dispatchers.Main,
        kind = kind,
        showOrders = { showOrders() },
        consumableRepository = container.consumables,
        currency = { container.currency.currency.value },
    )

    val ordersViewModel: QuoteHistoryViewModel by lazy { newHistoryViewModel(QuoteKind.ORDER) }
    val catalogViewModel: QuoteHistoryViewModel by lazy { newHistoryViewModel(QuoteKind.PRODUCT) }
    val dashboardViewModel: DashboardViewModel by lazy { DashboardViewModel(container.quoteHistory, container.settings) }
    val filamentListViewModel: FilamentListViewModel by lazy {
        FilamentListViewModel(container.filaments, scope, container.quoteHistory::quotesIncludingTrash)
    }
    val printerListViewModel: PrinterListViewModel by lazy {
        PrinterListViewModel(container.printers, container.quoteHistory, container.maintenance, scope)
    }
    val serviceListViewModel: ServiceListViewModel by lazy {
        ServiceListViewModel(container.services, scope, container.quoteHistory::quotesIncludingTrash)
    }
    val consumableListViewModel: ConsumableListViewModel by lazy {
        ConsumableListViewModel(container.consumables, scope, container.quoteHistory::quotesIncludingTrash)
    }

    /** Cedo, junto com [brandingViewModel]: o ponto de "alteração não salva" da barra lateral lê o rascunho dos dois. */
    val settingsViewModel: SettingsViewModel by lazy { SettingsViewModel(container.settings) }
    val brandingViewModel: BrandingViewModel by lazy {
        BrandingViewModel(container.branding, container.templates, currency = { container.currency.currency.value })
    }
    val templateListViewModel: TemplateListViewModel by lazy { TemplateListViewModel(container.templates, container.branding) }
    val salesChannelViewModel: SalesChannelViewModel by lazy {
        SalesChannelViewModel(container.salesChannels, container.quoteHistory::quotesIncludingTrash)
    }
    val backupViewModel: BackupViewModel by lazy {
        BackupViewModel(container.backup, container.preferences, container.pendingWrites, scope = scope)
    }

    /** Cedo: o tema e a moeda valem pra janela inteira desde o primeiro desenho. */
    val themeViewModel: ThemeViewModel by lazy { ThemeViewModel(container.theme) }
    val currencyViewModel: CurrencyViewModel by lazy { CurrencyViewModel(container.currency) }

    /** Cedo: a verificação roda ao abrir (decisão 116), e o aviso pode aparecer em qualquer tela. */
    val updateViewModel: UpdateViewModel by lazy {
        UpdateViewModel(container.preferences, container.releases, APP_VERSION, scope = scope, background = Dispatchers.IO, main = Dispatchers.Main)
    }
}
