package com.threedreport.app.platform

import com.threedreport.app.data.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** O idioma em que o app escreve: o já resolvido, nunca "automático" (decisão 152). */
enum class UiLanguage(val code: String) {
    PORTUGUESE("pt"),
    ENGLISH("en"),
}

/**
 * O idioma em uso e a região do sistema, de onde saem os formatos que não são dinheiro (decisão 153): datas e o separador
 * decimal das horas, do peso e do percentual.
 *
 * - Português: `dd/MM/yyyy`, `dd/MM` e vírgula, em qualquer região (é o formato de antes da Leva 11B).
 * - Inglês com região `US` ou sem região: `MM/dd/yyyy` e `MM/dd`. Com qualquer outra região (`GB`, `AU`, `IN`, e também
 *   `BR` ou `DE` de quem usa o app em inglês num sistema em outro idioma): `dd/MM/yyyy` e `dd/MM`. Ponto decimal.
 * - Hora sempre em 24 h.
 *
 * Dinheiro não passa por aqui: segue a moeda de cada orçamento ([com.threedreport.core.model.Currency], decisão 107).
 *
 * @property region código ISO 3166 da região do sistema, em maiúsculas ("BR", "US"), ou `null` se o sistema não tem.
 */
data class AppLocaleState(val language: UiLanguage, val region: String? = null) {

    /** Tag BCP 47 ("pt-BR", "en-US", "en"): o idioma resolvido com a região do sistema. */
    val languageTag: String get() = if (region == null) language.code else "${language.code}-$region"

    /** Separador decimal dos números que não são dinheiro ("12,5 h", "12.5 h"). */
    val decimalSeparator: Char get() = if (language == UiLanguage.PORTUGUESE) ',' else '.'

    private val monthFirst: Boolean get() = language == UiLanguage.ENGLISH && (region == null || region == "US")

    /** Data, ex.: "30/09/2026" ou "09/30/2026" (padrão do `java.time`). */
    val datePattern: String get() = if (monthFirst) "MM/dd/yyyy" else "dd/MM/yyyy"

    /** Data curta, sem o ano, ex.: "30/09" ou "09/30". */
    val shortDatePattern: String get() = if (monthFirst) "MM/dd" else "dd/MM"

    /** Data e hora em 24 h, ex.: "24/09/2026 14:30" ou "09/24/2026 14:30". */
    val dateTimePattern: String get() = "$datePattern HH:mm"

    companion object {
        /** Português do Brasil: o idioma de antes da Leva 11B, e o de quem não escolheu nada num sistema em português. */
        val BRAZILIAN_PORTUGUESE = AppLocaleState(UiLanguage.PORTUGUESE, "BR")
    }
}

/**
 * O idioma de [choice] num sistema com idioma [systemLanguage] (código ISO 639, "pt", "en", "de") e região
 * [systemRegion] (decisão 152). Automático usa português se o sistema está em português, em qualquer região, e inglês
 * em qualquer outro idioma: inglês é a reserva pra quem não fala português. A região é sempre a do sistema, mesmo com
 * o idioma escolhido à mão.
 */
fun resolveAppLocale(choice: AppLanguage, systemLanguage: String, systemRegion: String?): AppLocaleState {
    val language = when (choice) {
        AppLanguage.PT_BR -> UiLanguage.PORTUGUESE
        AppLanguage.EN -> UiLanguage.ENGLISH
        AppLanguage.AUTO -> if (systemLanguage.trim().lowercase() == UiLanguage.PORTUGUESE.code) UiLanguage.PORTUGUESE else UiLanguage.ENGLISH
    }
    val region = systemRegion?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
    return AppLocaleState(language, region)
}

/**
 * O idioma em uso no app (Leva 10B, parte 7, e decisão 152): o ponto único de onde saem o idioma dos textos e os formatos
 * de data e de decimal. Ninguém mais escreve `pt-BR` nem um padrão de data. Começa em português do Brasil e muda quando
 * a pessoa troca o idioma em Configurações > Aparência (no desktop, quem aplica é o `DesktopLocale`), com o app aberto.
 *
 * Quem formata lê o valor na hora ([current]): não guarde um formato pronto, ele ficaria no idioma de antes.
 */
object AppLocale {
    private val state = MutableStateFlow(AppLocaleState.BRAZILIAN_PORTUGUESE)

    /** O idioma em uso, pra acompanhar a troca (a raiz da tela recompõe tudo quando ele muda). */
    val current: StateFlow<AppLocaleState> = state.asStateFlow()

    /** Tag BCP 47 do idioma em uso ("pt-BR"). */
    val languageTag: String get() = state.value.languageTag

    /** Separador decimal dos números que não são dinheiro ("12,5 h"). */
    val decimalSeparator: Char get() = state.value.decimalSeparator

    /** Data e hora, ex.: "24/09/2026 14:30" (padrão do `java.time`). */
    val dateTimePattern: String get() = state.value.dateTimePattern

    /** Data, ex.: "30/09/2026". */
    val datePattern: String get() = state.value.datePattern

    /** Data curta, sem o ano, ex.: "30/09". */
    val shortDatePattern: String get() = state.value.shortDatePattern

    /**
     * Passa a usar [locale]. Só o código que troca o idioma da plataforma chama (no desktop, `DesktopLocale`, que muda o
     * idioma padrão da JVM antes, pros textos lidos fora da tela já saírem no novo).
     */
    internal fun use(locale: AppLocaleState) {
        state.value = locale
    }
}
