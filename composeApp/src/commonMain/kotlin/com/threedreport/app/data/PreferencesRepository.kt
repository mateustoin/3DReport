package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Preferências **deste computador** (decisão 108), separadas dos dados do negócio: numa sincronização
 * futura, orçamentos, cadastros e configurações de custo vão pra nuvem; onde fica a pasta de backup
 * aqui, não.
 *
 * @property autoBackupEnabled faz um backup por dia ao abrir o app.
 * @property backupDirectory pasta dos backups automáticos, ou `null` pra padrão.
 * @property lastBackupEpochMillis quando foi o último backup (manual ou automático), pra mostrar em
 *   Configurações e lembrar quem nunca fez.
 * @property checkForUpdates consulta o GitHub ao abrir pra avisar de versão nova. Desligado por padrão:
 *   o app é 100% local, e só sai da máquina o que a pessoa escolheu.
 * @property window onde a janela estava ao fechar (decisão 111), pra abrir no mesmo lugar.
 */
@Serializable
data class AppPreferences(
    val autoBackupEnabled: Boolean = true,
    val backupDirectory: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val checkForUpdates: Boolean = false,
    val window: SavedWindowBounds? = null,
)

/**
 * Posição e tamanho da janela em pixels lógicos (os do `Dp` no desktop). Com [maximized], o retângulo é o
 * de antes de maximizar, pra voltar a ele quando a pessoa sair do maximizado.
 */
@Serializable
data class SavedWindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val maximized: Boolean = false,
)

interface PreferencesRepository : DocumentRepository<AppPreferences> {
    val preferences: StateFlow<AppPreferences>
        get() = value
}

class StoredPreferencesRepository(document: DocumentValue<AppPreferences>) :
    StoredDocumentRepository<AppPreferences>(document), PreferencesRepository
