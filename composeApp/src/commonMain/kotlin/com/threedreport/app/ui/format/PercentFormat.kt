package com.threedreport.app.ui.format

/** Formata uma fração (ex.: `0.15`) como percentual pra exibição (ex.: `"15%"`, `"12,5%"`, `"12.5%"` em inglês). */
fun Double.toPercentText(): String = "${(this * 100).oneDecimalText()}%"
