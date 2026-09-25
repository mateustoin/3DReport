package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.Client
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Cadastro de clientes (decisão 106). Nasce sozinho de quem aparece nos pedidos: salvar um pedido com
 * cliente passa por [resolve], que acha o cliente pelo nome ou cria. Serve pra sugerir o cliente ao
 * digitar e é a base pra histórico por cliente e integrações. O pedido guarda um retrato (nome e
 * contato do momento), então editar o cadastro não muda um pedido antigo.
 */
interface ClientRepository : CatalogRepository<Client> {
    val clients: StateFlow<List<Client>>
        get() = items

    /**
     * O cliente do cadastro que corresponde a [client] (pelo id, ou pelo nome sem diferenciar
     * maiúsculas e espaços), criando um se não houver. Um contato novo atualiza o do cadastro. Devolve o
     * retrato com o id, pra gravar no pedido.
     */
    fun resolve(client: Client): Client
}

@OptIn(ExperimentalUuidApi::class)
class RecordClientRepository(collection: RecordCollection<Client>) : RecordCatalogRepository<Client>(collection), ClientRepository {

    override fun resolve(client: Client): Client {
        val name = client.name.trim()
        val contact = client.contact?.trim()?.ifEmpty { null }
        val existing = client.id?.let(collection::find)
            ?: items.value.firstOrNull { it.name.normalized() == name.normalized() }
        if (existing == null) {
            return Client(name = name, contact = contact, id = Uuid.random().toString()).also(collection::add)
        }
        val updated = existing.copy(name = name, contact = contact ?: existing.contact)
        collection.update(updated)
        return updated
    }

    private fun String.normalized() = trim().lowercase().replace(Regex("\\s+"), " ")
}
