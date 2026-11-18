package com.threedreport.app.screenshots

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.about_updates_install_now
import _3dreport.composeapp.generated.resources.catalog_action_export
import _3dreport.composeapp.generated.resources.common_archived_section
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.history_menu_message
import _3dreport.composeapp.generated.resources.history_menu_more
import _3dreport.composeapp.generated.resources.history_menu_payments
import _3dreport.composeapp.generated.resources.onboarding_action_next
import _3dreport.composeapp.generated.resources.payments_action_register
import _3dreport.composeapp.generated.resources.payments_register_confirm
import _3dreport.composeapp.generated.resources.printers_action_maintenance
import _3dreport.composeapp.generated.resources.printers_action_pick
import _3dreport.composeapp.generated.resources.printers_presets_field_search
import _3dreport.composeapp.generated.resources.quote_negotiate
import _3dreport.composeapp.generated.resources.quote_print_settings
import _3dreport.composeapp.generated.resources.settings_action_energy_calc
import _3dreport.composeapp.generated.resources.settings_docs_action_preview
import _3dreport.composeapp.generated.resources.settings_docs_template_list
import _3dreport.composeapp.generated.resources.settings_docs_template_save
import _3dreport.composeapp.generated.resources.settings_energy_field_bill_kwh
import _3dreport.composeapp.generated.resources.settings_energy_field_bill_total
import _3dreport.composeapp.generated.resources.settings_energy_mode_white
import _3dreport.composeapp.generated.resources.settings_field_failure_rate
import _3dreport.composeapp.generated.resources.settings_field_labor_rate
import _3dreport.composeapp.generated.resources.settings_field_margin
import _3dreport.composeapp.generated.resources.settings_section_appearance
import _3dreport.composeapp.generated.resources.settings_section_backup
import _3dreport.composeapp.generated.resources.settings_section_business
import _3dreport.composeapp.generated.resources.settings_section_channels
import _3dreport.composeapp.generated.resources.settings_section_documents
import _3dreport.composeapp.generated.resources.settings_section_orders
import _3dreport.composeapp.generated.resources.templates_action_load
import _3dreport.composeapp.generated.resources.update_install_and_close
import _3dreport.composeapp.generated.resources.update_open_folder
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.UiLanguage
import com.threedreport.core.model.Currency
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** As capturas saem em inglês com `-Pscreenshots.lang=en` (decisão 157): os dados de exemplo e os cliques seguem o idioma. */
internal val screenshotsInEnglish: Boolean get() = AppLocale.current.value.language == UiLanguage.ENGLISH

/** [portuguese] ou [english], conforme o idioma das capturas. Só pra dado de exemplo: texto da tela vem do recurso ([ST]). */
internal fun sample(portuguese: String, english: String): String = if (screenshotsInEnglish) english else portuguese

/** Um número do dado de exemplo: em inglês a moeda é o dólar, e os valores são de outra escala. */
internal fun sample(portuguese: Double, english: Double): Double = if (screenshotsInEnglish) english else portuguese

/** A moeda das capturas: real em português e dólar em inglês. */
internal fun sampleCurrency(): Currency = if (screenshotsInEnglish) Currency.USD else Currency.BRL

private fun tr(resource: StringResource, vararg args: Any): String = runBlocking { getString(resource, *args) }

/**
 * Os textos do app que as capturas procuram na tela (pra clicar ou digitar), lidos dos recursos no idioma em uso: nada
 * de texto fixo em português nos cliques. Cada propriedade lê na hora, depois de o idioma das capturas ser definido.
 */
internal object ST {
    val negotiate get() = tr(Res.string.quote_negotiate)
    val printSettings get() = tr(Res.string.quote_print_settings)
    val exportCatalog get() = tr(Res.string.catalog_action_export)

    /** O título da seção "Arquivados (N)", sem o número: o clique procura o texto contido. */
    val archived get() = tr(Res.string.common_archived_section, 0).substringBefore(" (")
    val moreActions get() = tr(Res.string.history_menu_more)
    val maintenance get() = tr(Res.string.printers_action_maintenance)
    val pickPrinter get() = tr(Res.string.printers_action_pick)
    val presetSearch get() = tr(Res.string.printers_presets_field_search)
    val updateNow get() = tr(Res.string.about_updates_install_now)
    val openFolder get() = tr(Res.string.update_open_folder)
    val installAndClose get() = tr(Res.string.update_install_and_close)
    val save get() = tr(Res.string.common_save)
    val next get() = tr(Res.string.onboarding_action_next)
    val energyCalc get() = tr(Res.string.settings_action_energy_calc)
    val whiteTariff get() = tr(Res.string.settings_energy_mode_white)
    val billTotal get() = tr(Res.string.settings_energy_field_bill_total)
    val billKwh get() = tr(Res.string.settings_energy_field_bill_kwh)
    val marginField get() = tr(Res.string.settings_field_margin)
    val laborField get() = tr(Res.string.settings_field_labor_rate)
    val failureField get() = tr(Res.string.settings_field_failure_rate)
    val templateList get() = tr(Res.string.settings_docs_template_list)
    val saveTemplate get() = tr(Res.string.settings_docs_template_save)
    val previewPdf get() = tr(Res.string.settings_docs_action_preview)
    val load get() = tr(Res.string.templates_action_load)
    val registerPayment get() = tr(Res.string.payments_action_register)
    val registerConfirm get() = tr(Res.string.payments_register_confirm)
    val menuPayments get() = tr(Res.string.history_menu_payments)
    val menuMessage get() = tr(Res.string.history_menu_message)

    val sectionBusiness get() = tr(Res.string.settings_section_business)
    val sectionChannels get() = tr(Res.string.settings_section_channels)
    val sectionDocuments get() = tr(Res.string.settings_section_documents)
    val sectionAppearance get() = tr(Res.string.settings_section_appearance)
    val sectionBackup get() = tr(Res.string.settings_section_backup)
    val sectionOrders get() = tr(Res.string.settings_section_orders)
}

/** Os nomes dos dados de exemplo que a tela mostra e as capturas procuram, no idioma das capturas. */
internal object Names {
    val painted get() = sample("Miniatura pintada", "Painted miniature")
    val vaseOrder get() = sample("Vaso geométrico (encomenda)", "Geometric vase")
    val headset get() = sample("Suporte de headset", "Headset stand")
    val keychainOrder get() = sample("Chaveiro personalizado", "Custom keychain")
    val phoneStand get() = sample("Porta-celular", "Phone stand")
    val axolotl get() = sample("Axolote de estimação", "Pet axolotl")
    val organizerOrder get() = sample("Organizador de mesa (encomenda)", "Desk organizer")
    val lampOrder get() = sample("Luminária lua (encomenda)", "Moon lamp")
    val boat get() = sample("Barquinho decorativo", "Decorative boat")
    val gift get() = sample("Presente de aniversário", "Birthday gift")

    const val PLA_FILAMENT = "PLA Voolt3D"
    const val BAMBU = "Bambu Lab A1"
    const val BAMBU_PRESET = "Bambu Lab A1 Combo"
    val assembly get() = sample("Montagem", "Assembly")

    val marketplace get() = sample("Shopee", "Etsy")
    val shopName get() = sample("Sua Loja 3D", "Your 3D Shop")
    val shopHandle get() = sample("sualoja3d", "your3dshop")
    val monitorStand get() = sample("Suporte de monitor", "Monitor stand")
}
