package com.threedreport.core.model

/**
 * [value] arredondado ao centavo mais próximo. A regra do `core` é arredondar dinheiro só ao exibir;
 * esta função é a exceção deliberada pra valores que são **cobrados** em centavos e gravados assim: o pagamento
 * registrado, o sinal pedido e o que cobrar agora ([SavedQuote.amountDue]), e o preço por kg que uma compra grava no
 * cadastro do filamento ([Purchase.catalogPriceUpdates]). As contas de preço e custo continuam sem arredondar.
 */
fun roundToCents(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
