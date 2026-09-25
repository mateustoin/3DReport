package com.threedreport.app

/**
 * Registro de erros do app (decisão 108). Sem ele, um erro na máquina de quem usa o app não deixava
 * rastro nenhum pra entender o que aconteceu. A plataforma instala o destino ([sink]) ao abrir: no
 * desktop, um arquivo em `~/.3dreport/logs/`. Sem destino instalado (testes), vai pro console de erro.
 */
object AppLog {

    enum class Level { INFO, WARN, ERROR }

    /** Pra onde vão as mensagens. Trocado uma vez, na inicialização. */
    var sink: (Level, String, Throwable?) -> Unit = { level, message, error ->
        if (level != Level.INFO) {
            println("[$level] $message")
            error?.printStackTrace()
        }
    }

    fun info(message: String) = sink(Level.INFO, message, null)

    fun warn(message: String, error: Throwable? = null) = sink(Level.WARN, message, error)

    fun error(message: String, error: Throwable? = null) = sink(Level.ERROR, message, error)
}
