package com.threedreport.app.platform

/** Abre o seletor nativo pra escolher um arquivo `.zip` de backup do app, ou `null` se cancelado. */
expect fun pickBackupFile(): PickedFile?
