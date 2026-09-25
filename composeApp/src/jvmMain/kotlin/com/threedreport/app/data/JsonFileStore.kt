package com.threedreport.app.data

import java.io.File

/**
 * Pasta de dados do app no usuário atual (`~/.3dreport`). Criada se não existir.
 *
 * Pode ser sobrescrita pela propriedade de sistema `threedreport.dataDir`
 * (usado pelos testes, para não escrever na pasta real do usuário).
 */
internal fun appDataDir(): File {
    val override = System.getProperty("threedreport.dataDir")
    val dir = if (override != null) File(override) else File(System.getProperty("user.home"), ".3dreport")
    return dir.apply { mkdirs() }
}
