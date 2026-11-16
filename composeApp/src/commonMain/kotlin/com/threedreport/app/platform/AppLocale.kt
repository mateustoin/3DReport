package com.threedreport.app.platform

/**
 * O idioma dos formatos do app: datas, dia da semana e a vírgula das horas decimais (Leva 10B, parte 7). É o ponto
 * único de onde esses formatos saem; ninguém mais escreve `pt-BR` nem um padrão de data. Na 2.2 continua português do
 * Brasil, fixo; a escolha do idioma (Leva 11B) passa a mudar só este objeto.
 *
 * Dinheiro não passa por aqui: segue a moeda de cada orçamento ([com.threedreport.core.model.Currency], decisão 107).
 */
object AppLocale {
    /** Tag BCP 47 do idioma ("pt-BR"). */
    val languageTag: String = "pt-BR"

    /** Separador decimal dos números que não são dinheiro ("12,5 h"). */
    val decimalSeparator: Char = ','

    /** Data e hora, ex.: "24/09/2026 14:30" (padrão do `java.time`). */
    val dateTimePattern: String = "dd/MM/yyyy HH:mm"

    /** Data, ex.: "30/09/2026". */
    val datePattern: String = "dd/MM/yyyy"

    /** Data curta, sem o ano, ex.: "30/09". */
    val shortDatePattern: String = "dd/MM"
}
