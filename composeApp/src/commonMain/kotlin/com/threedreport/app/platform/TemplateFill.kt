package com.threedreport.app.platform

/** O marcador que o `getString` do Compose preenche: `%1$s`, `%2$d`. */
private val PLACEHOLDER = Regex("""%(\d+)\$[ds]""")

/**
 * Preenche um modelo de `strings.xml` ("%1$s de %2$d") com [args], do mesmo jeito que o `getString` do Compose. É o único
 * ponto que faz isso fora da composição: os textos dos documentos e as durações usam esta função.
 */
fun fillTemplate(template: String, vararg args: Any): String =
    template.replace(PLACEHOLDER) { match -> args[match.groupValues[1].toInt() - 1].toString() }
