package com.threedreport.app.platform

import com.threedreport.app.data.AppLanguage
import java.util.Locale

/**
 * Aplica o idioma do app no desktop (decisão 152). O Compose Multiplatform escolhe a pasta de textos (`values/` ou
 * `values-en/`) pelo idioma padrão da JVM, lido na hora: na tela, a cada recomposição ([ProvideAppLocale]); fora dela
 * (`getString`, PDF, mensagem, avisos), a cada leitura. Trocar o idioma é trocar o padrão da JVM e depois o
 * [AppLocale], nessa ordem, pra quem reage ao [AppLocale] já ler os textos novos.
 *
 * O idioma e a região do sistema são lidos uma vez, quando este objeto nasce, antes de qualquer troca: depois dela,
 * `Locale.getDefault()` já não é o do sistema.
 */
object DesktopLocale {
    private val systemDefault: Locale = Locale.getDefault()
    private val systemFormat: Locale = Locale.getDefault(Locale.Category.FORMAT)

    /** O idioma do sistema ("pt", "en", "de"): no Windows, o da interface do sistema. */
    val systemLanguage: String = systemDefault.language

    /**
     * A região do sistema ("BR", "US"), ou `null` se não tem. É a dos formatos regionais (no Windows, Configurações >
     * Hora e idioma > Região), que é onde a pessoa diz como escreve a data; sem ela, a do idioma do sistema.
     */
    val systemRegion: String? = systemFormat.country.ifEmpty { systemDefault.country }.ifEmpty { null }

    /** O idioma que [choice] dá neste sistema. */
    fun resolve(choice: AppLanguage): AppLocaleState = resolveAppLocale(choice, systemLanguage, systemRegion)

    /** Passa a usar o idioma de [choice]. Na thread da tela, ou antes de a janela abrir. */
    fun apply(choice: AppLanguage): AppLocaleState = resolve(choice).also(::use)

    /**
     * Passa a usar [locale]: muda o idioma padrão da JVM (textos do Compose, diálogos do Swing) e o [AppLocale]. A
     * categoria de formatos fica a do sistema: nada no app formata pelo padrão da JVM, e assim nada de fora muda.
     */
    fun use(locale: AppLocaleState) {
        Locale.setDefault(Locale.forLanguageTag(locale.languageTag))
        Locale.setDefault(Locale.Category.FORMAT, systemFormat)
        AppLocale.use(locale)
    }
}
