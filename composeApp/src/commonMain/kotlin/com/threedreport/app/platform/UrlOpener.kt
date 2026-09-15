package com.threedreport.app.platform

/**
 * Abre [url] no navegador padrão do sistema. Falha silenciosamente (não
 * lança) se a URL for inválida ou não houver navegador configurado — abrir
 * um link é uma conveniência, não deve derrubar o app.
 */
expect fun openUrl(url: String)
