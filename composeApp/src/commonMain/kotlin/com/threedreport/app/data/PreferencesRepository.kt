package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

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
 * @property announcedUpdateVersion a última versão nova já avisada (decisão 116), pro aviso aparecer uma vez só.
 * @property ordersView como Pedidos abre (decisão 129): a última visualização escolhida, kanban por padrão.
 * @property catalogView como o Catálogo abre (decisão 132): a última visualização escolhida, grade por padrão.
 */
@Serializable
data class AppPreferences(
    val autoBackupEnabled: Boolean = true,
    val backupDirectory: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val checkForUpdates: Boolean = false,
    val window: SavedWindowBounds? = null,
    val announcedUpdateVersion: String? = null,
    val ordersView: OrdersView = OrdersView.KANBAN,
    val catalogView: CatalogView = CatalogView.GRID,
)

/**
 * Visualização da tela de Pedidos (decisão 129). O nome da constante é o que vai pro arquivo: não renomear.
 * Valor desconhecido no arquivo (versão mais nova, edição à mão) vira [KANBAN].
 */
@Serializable(with = OrdersViewSerializer::class)
enum class OrdersView { KANBAN, LIST }

/**
 * Visualização da tela do Catálogo (decisão 132). O nome da constante é o que vai pro arquivo: não renomear.
 * Valor desconhecido no arquivo vira [GRID].
 */
@Serializable(with = CatalogViewSerializer::class)
enum class CatalogView { GRID, LIST }

/**
 * Enum gravado pelo nome da constante que, ao ler, troca valor desconhecido (ou que nem é texto) por
 * [fallback] em vez de falhar: uma visualização que esta versão não conhece não pode fazer o arquivo de
 * preferências inteiro ser posto de lado como ilegível.
 */
internal open class FallbackEnumSerializer<E : Enum<E>>(
    serialName: String,
    private val entries: List<E>,
    private val fallback: E,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: E) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): E {
        val name = if (decoder is JsonDecoder) {
            (decoder.decodeJsonElement() as? JsonPrimitive)?.takeIf { it.isString }?.content
        } else {
            decoder.decodeString()
        }
        return entries.firstOrNull { it.name == name } ?: fallback
    }
}

internal object OrdersViewSerializer :
    FallbackEnumSerializer<OrdersView>("com.threedreport.app.data.OrdersView", OrdersView.entries, OrdersView.KANBAN)

internal object CatalogViewSerializer :
    FallbackEnumSerializer<CatalogView>("com.threedreport.app.data.CatalogView", CatalogView.entries, CatalogView.GRID)

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

    /** Lembra a visualização de Pedidos pra próxima abertura (decisão 129). */
    fun setOrdersView(view: OrdersView) = update(value.value.copy(ordersView = view))

    /** Lembra a visualização do Catálogo pra próxima abertura (decisão 132). */
    fun setCatalogView(view: CatalogView) = update(value.value.copy(catalogView = view))
}

class StoredPreferencesRepository(document: DocumentValue<AppPreferences>) :
    StoredDocumentRepository<AppPreferences>(document), PreferencesRepository
