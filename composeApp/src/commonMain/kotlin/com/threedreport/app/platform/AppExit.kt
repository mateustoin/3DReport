package com.threedreport.app.platform

/**
 * Fecha o app imediatamente.
 *
 * Usado depois de restaurar um backup: os repositórios carregam os dados em
 * memória na inicialização, então continuar aberto com os arquivos já
 * trocados faria a próxima gravação sobrescrever o que acabou de ser
 * restaurado com o estado antigo que ainda está em memória. Fechar é seguro
 * porque toda alteração já é gravada em disco na hora (ver `JsonFileStore`),
 * não há nada pendente pra descarregar.
 */
expect fun exitApp()
