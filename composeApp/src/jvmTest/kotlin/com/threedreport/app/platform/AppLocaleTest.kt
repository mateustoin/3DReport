package com.threedreport.app.platform

import com.threedreport.app.data.AppLanguage
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.ui.format.DurationTexts
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.format.toWeightText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Datas e números saem de um ponto só, o [AppLocale] (Leva 10B, parte 7), que desde a Leva 11B é o idioma em uso
 * (decisões 152 e 153). Em português tudo sai como antes; em inglês a data segue a região do sistema.
 */
class AppLocaleTest {

    private val day = LocalDate.of(2026, 9, 30).toEpochDay()
    private val instant = LocalDateTime.of(2026, 9, 24, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @AfterTest
    fun backToPortuguese() {
        AppLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
    }

    // Item 2: automático usa português pra sistema em português (qualquer região) e inglês pra qualquer outro idioma.

    @Test
    fun automaticFollowsAPortugueseSystemInAnyRegion() {
        assertEquals(AppLocaleState(UiLanguage.PORTUGUESE, "BR"), resolveAppLocale(AppLanguage.AUTO, "pt", "BR"))
        assertEquals(AppLocaleState(UiLanguage.PORTUGUESE, "PT"), resolveAppLocale(AppLanguage.AUTO, "pt", "PT"))
        assertEquals(AppLocaleState(UiLanguage.PORTUGUESE, "AO"), resolveAppLocale(AppLanguage.AUTO, "PT", "ao"))
        assertEquals(AppLocaleState(UiLanguage.PORTUGUESE, null), resolveAppLocale(AppLanguage.AUTO, "pt", ""))
    }

    @Test
    fun automaticUsesEnglishForAnyOtherSystemLanguage() {
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, "US"), resolveAppLocale(AppLanguage.AUTO, "en", "US"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, "GB"), resolveAppLocale(AppLanguage.AUTO, "en", "GB"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, "DE"), resolveAppLocale(AppLanguage.AUTO, "de", "DE"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, "ES"), resolveAppLocale(AppLanguage.AUTO, "es", "ES"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, null), resolveAppLocale(AppLanguage.AUTO, "", null))
    }

    @Test
    fun aChosenLanguageIgnoresTheSystemLanguageButKeepsItsRegion() {
        assertEquals(AppLocaleState(UiLanguage.PORTUGUESE, "US"), resolveAppLocale(AppLanguage.PT_BR, "en", "US"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, "BR"), resolveAppLocale(AppLanguage.EN, "pt", "BR"))
        assertEquals(AppLocaleState(UiLanguage.ENGLISH, null), resolveAppLocale(AppLanguage.EN, "pt", null))
    }

    // Item 4: quem usa o sistema em português não vê nada mudar.

    @Test
    fun aPortugueseSystemOnAutomaticIsTheSameAsBefore() {
        val locale = resolveAppLocale(AppLanguage.AUTO, "pt", "BR")
        assertEquals(AppLocaleState.BRAZILIAN_PORTUGUESE, locale)
        assertEquals("pt-BR", locale.languageTag)
        assertEquals(',', locale.decimalSeparator)
        assertEquals("dd/MM/yyyy HH:mm", locale.dateTimePattern)
        assertEquals("dd/MM/yyyy", locale.datePattern)
        assertEquals("dd/MM", locale.shortDatePattern)
    }

    // Item 6: em inglês, en-US (e sistema sem região) escreve mês/dia; qualquer outra região, dia/mês. Ponto decimal.

    @Test
    fun englishInTheUnitedStatesOrWithoutRegionWritesMonthFirst() {
        for (locale in listOf(AppLocaleState(UiLanguage.ENGLISH, "US"), AppLocaleState(UiLanguage.ENGLISH, null))) {
            assertEquals("MM/dd/yyyy", locale.datePattern, locale.languageTag)
            assertEquals("MM/dd", locale.shortDatePattern, locale.languageTag)
            assertEquals("MM/dd/yyyy HH:mm", locale.dateTimePattern, locale.languageTag)
            assertEquals('.', locale.decimalSeparator, locale.languageTag)
        }
        assertEquals("en-US", AppLocaleState(UiLanguage.ENGLISH, "US").languageTag)
        assertEquals("en", AppLocaleState(UiLanguage.ENGLISH, null).languageTag)
    }

    @Test
    fun englishInAnyOtherRegionWritesDayFirst() {
        for (region in listOf("GB", "AU", "IN", "BR", "DE")) {
            val locale = AppLocaleState(UiLanguage.ENGLISH, region)
            assertEquals("dd/MM/yyyy", locale.datePattern, region)
            assertEquals("dd/MM", locale.shortDatePattern, region)
            assertEquals("dd/MM/yyyy HH:mm", locale.dateTimePattern, region)
            assertEquals('.', locale.decimalSeparator, region)
        }
    }

    @Test
    fun portugueseInAnyRegionWritesDayFirstWithComma() {
        for (region in listOf("BR", "PT", "US", null)) {
            val locale = AppLocaleState(UiLanguage.PORTUGUESE, region)
            assertEquals("dd/MM/yyyy", locale.datePattern)
            assertEquals(',', locale.decimalSeparator)
        }
    }

    // Item 5: o AppLocale é o idioma em uso, e as datas seguem a troca na hora.

    @Test
    fun datesLookTheSameAsBeforeInPortuguese() {
        assertEquals("pt-BR", AppLocale.languageTag)
        assertEquals(',', AppLocale.decimalSeparator)
        assertEquals("30/09/2026", formatDate(day))
        assertEquals("30/09", formatShortDate(day))
        assertEquals("24/09/2026 14:30", formatDateTime(instant))
        assertEquals("quarta", weekdayName(day))
        assertEquals("sexta", weekdayName(LocalDate.of(2026, 10, 2).toEpochDay()))
    }

    @Test
    fun datesFollowTheLanguageInUse() {
        AppLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))
        assertEquals("09/30/2026", formatDate(day))
        assertEquals("09/30", formatShortDate(day))
        assertEquals("09/24/2026 14:30", formatDateTime(instant))
        assertEquals("Wednesday", weekdayName(day))

        AppLocale.use(AppLocaleState(UiLanguage.ENGLISH, "GB"))
        assertEquals("30/09/2026", formatDate(day))
        assertEquals("30/09", formatShortDate(day))
        assertEquals("24/09/2026 14:30", formatDateTime(instant))

        AppLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        assertEquals("30/09/2026", formatDate(day))
        assertEquals("quarta", weekdayName(day))
    }

    @Test
    fun theAutomaticQuoteNameUsesTheDateOfTheLanguageInUse() {
        AppLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))
        assertEquals(true, LocalStorage.defaultQuoteName(instant).endsWith("09/24/2026 14:30"), LocalStorage.defaultQuoteName(instant))
    }

    @Test
    fun decimalsThatAreNotMoneyFollowTheLanguage() {
        val durations = DurationTexts(
            hoursAndMinutes = "%1\$d h %2\$d min",
            hoursOnly = "%1\$d h",
            minutesOnly = "%1\$d min",
            decimalHoursTemplate = "%1\$s h",
        )
        assertEquals("12,5 h", durations.decimalHours(750.0))
        assertEquals("12,5 g", 12.5.toWeightText())
        assertEquals("12,5%", 0.125.toPercentText())
        assertEquals("15%", 0.15.toPercentText())

        AppLocale.use(AppLocaleState(UiLanguage.ENGLISH, "GB"))
        assertEquals("12.5 h", durations.decimalHours(750.0))
        assertEquals("12.5 g", 12.5.toWeightText())
        assertEquals("12.5%", 0.125.toPercentText())
        assertEquals("12 g", 12.0.toWeightText())
    }
}
