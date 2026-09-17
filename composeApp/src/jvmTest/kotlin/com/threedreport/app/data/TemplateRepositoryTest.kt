package com.threedreport.app.data

import com.threedreport.core.model.QuoteTemplate
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que o catálogo de templates sobrevive a uma nova instância do repositório (persistência em disco). */
class TemplateRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsEmpty() {
        // Não há um template "padrão" que sirva pra qualquer criador (mesmo princípio de ServiceRepository).
        assertTrue(TemplateRepository().templates.value.isEmpty())
    }

    @Test
    fun addedTemplateSurvivesNewRepositoryInstance() {
        val original = TemplateRepository()
        original.add(QuoteTemplate(id = "formal", name = "Formal", watermarkText = "Minha Loja"))

        val reloaded = TemplateRepository().templates.value
        assertTrue(reloaded.any { it.id == "formal" && it.name == "Formal" && it.watermarkText == "Minha Loja" })
    }

    @Test
    fun updatePersistsChange() {
        val repository = TemplateRepository()
        repository.add(QuoteTemplate(id = "formal", name = "Formal", watermarkText = "Minha Loja"))
        val template = repository.templates.value.first()

        repository.update(template.copy(watermarkText = "Outra Marca"))

        val reloaded = TemplateRepository().templates.value.first { it.id == template.id }
        assertEquals("Outra Marca", reloaded.watermarkText)
    }

    @Test
    fun deleteRemovesFromDisk() {
        val repository = TemplateRepository()
        repository.add(QuoteTemplate(id = "formal", name = "Formal"))
        val template = repository.templates.value.first()

        repository.delete(template.id)

        val reloaded = TemplateRepository().templates.value
        assertTrue(reloaded.none { it.id == template.id })
    }
}
