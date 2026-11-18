package com.threedreport.app.ui.format

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_auto_name
import androidx.compose.runtime.Composable
import com.threedreport.app.platform.fillTemplate
import com.threedreport.app.platform.formatDateTime
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.stringResource

/**
 * O nome automático ("Quote - 09/24/2026 14:30") montado com o [template] do idioma em uso (`quote_auto_name`) e a data
 * de criação [createdAtEpochMillis] (decisão 139, item 7b da Leva 11B). Quem lista usa isto em vez do texto gravado, que
 * ficou no idioma e no formato de data de quando o pedido foi salvo.
 */
fun autoNameText(template: String, createdAtEpochMillis: Long): String =
    fillTemplate(template, formatDateTime(createdAtEpochMillis))

/** O nome a mostrar: o digitado, ou o automático montado agora no idioma do app ([autoNameText]). */
fun SavedQuote.displayName(autoNameTemplate: String): String =
    if (autoNamed) autoNameText(autoNameTemplate, savedAtEpochMillis) else name

/**
 * O mesmo nome fora da tela (busca, avisos, diálogos montados no ViewModel): [autoName] monta o automático no idioma em
 * uso a partir da data de criação, normalmente [com.threedreport.app.data.QuoteHistoryRepository.autoName].
 */
fun SavedQuote.displayName(autoName: (createdAtEpochMillis: Long) -> String): String =
    if (autoNamed) autoName(savedAtEpochMillis) else name

/** [displayName] na tela, com o modelo do idioma em uso. */
@Composable
fun SavedQuote.shownName(): String = displayName(stringResource(Res.string.quote_auto_name))
