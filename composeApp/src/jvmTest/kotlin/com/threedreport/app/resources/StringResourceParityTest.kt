package com.threedreport.app.resources

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Paridade entre os textos em português (`values/`) e em inglês (`values-en/`), decisão 154, item 11 da Leva 11B:
 * mesmas chaves nos dois sentidos, mesmos marcadores em cada texto (`%1$s`, `%2$d` e os `{cliente}` dos modelos de
 * mensagem), mesmas quantidades em cada plural, e nenhum travessão nem aspas com barra no inglês.
 *
 * O Compose junta todos os `.xml` de uma pasta (`values-en/` pode ter um arquivo por área), mas não reclama de uma chave
 * repetida em dois arquivos da mesma pasta: um deles ganha calado. Por isso este teste também confere que cada chave
 * aparece uma vez só em cada pasta.
 *
 * Só roda se `values-en/` existir. Enquanto a tradução não termina, [EVERY_KEY_TRANSLATED] fica desligado: o inglês
 * pode ter menos chaves (o que falta sai em português), mas tudo o que já tem precisa estar certo.
 */
class StringResourceParityTest {

    private val resourcesDir = File("src/commonMain/composeResources")

    /** Os textos de uma pasta: chave → texto, e chave → (quantidade → texto) pros plurais. */
    private class Texts(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>) {
        val keys: Set<String> get() = strings.keys + plurals.keys
    }

    private fun read(folder: String): Texts? {
        val dir = File(resourcesDir, folder)
        if (!dir.isDirectory) return null
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val origin = mutableMapOf<String, String>()
        val files = dir.listFiles { file -> file.extension == "xml" }.orEmpty().sortedBy { it.name }
        for (file in files) {
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            for (element in root.childElements()) {
                val name = element.getAttribute("name")
                origin.put(name, file.name)?.let { first -> fail("$folder: a chave $name está em $first e em ${file.name}") }
                when (element.tagName) {
                    "string" -> strings[name] = element.textContent

                    "plurals" -> plurals[name] = element.childElements().filter { it.tagName == "item" }
                        .associate { it.getAttribute("quantity") to it.textContent }

                    else -> fail("$folder/${file.name}: elemento <${element.tagName}> não esperado ($name)")
                }
            }
        }
        return Texts(strings, plurals)
    }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

    /** Os marcadores de um texto, sem repetição: `%1$s`, `%2$d` e os `{cliente}` dos modelos de mensagem. */
    private fun markers(text: String): Set<String> = MARKER.findAll(text).map { it.value }.toSet()

    private fun portuguese(): Texts = read("values") ?: fail("sem a pasta values/ em ${resourcesDir.absolutePath}")

    @Test
    fun everyEnglishKeyExistsInPortugueseAndTheOtherWayAround() {
        val pt = portuguese()
        val en = read("values-en") ?: return
        assertEquals(emptySet(), en.strings.keys - pt.strings.keys, "textos em values-en/ que não existem em values/")
        assertEquals(emptySet(), en.plurals.keys - pt.plurals.keys, "plurais em values-en/ que não existem em values/")
        if (EVERY_KEY_TRANSLATED) {
            assertEquals(emptySet(), pt.strings.keys - en.strings.keys, "textos de values/ sem tradução em values-en/")
            assertEquals(emptySet(), pt.plurals.keys - en.plurals.keys, "plurais de values/ sem tradução em values-en/")
        }
    }

    @Test
    fun everyTranslatedTextHasTheSameMarkers() {
        val pt = portuguese()
        val en = read("values-en") ?: return
        val wrong = en.strings.mapNotNull { (key, english) ->
            val portuguese = pt.strings[key] ?: return@mapNotNull null
            if (markers(english) == markers(portuguese)) null else "$key: ${markers(portuguese)} em português, ${markers(english)} em inglês"
        }
        assertEquals(emptyList(), wrong)
        val unnumbered = en.strings.filterValues { UNNUMBERED.containsMatchIn(it) }.keys
        assertEquals(emptySet(), unnumbered, "marcador sem número (%s, %d) em values-en/: use %1\$s, %1\$d")
    }

    @Test
    fun everyTranslatedPluralHasTheSameQuantitiesAndMarkers() {
        val pt = portuguese()
        val en = read("values-en") ?: return
        for ((key, english) in en.plurals) {
            val portuguese = pt.plurals[key] ?: continue
            assertEquals(portuguese.keys, english.keys, "quantidades do plural $key")
            for ((quantity, text) in english) {
                assertEquals(markers(portuguese.getValue(quantity)), markers(text), "marcadores de $key ($quantity)")
            }
        }
    }

    @Test
    fun englishTextsFollowTheWritingRules() {
        val en = read("values-en") ?: return
        val all = en.strings + en.plurals.flatMap { (key, items) -> items.map { (quantity, text) -> "$key ($quantity)" to text } }
        assertEquals(emptySet(), all.filterValues { '—' in it }.keys, "travessão em values-en/")
        assertEquals(emptySet(), all.filterValues { "\\\"" in it || "\\'" in it }.keys, "aspas com barra em values-en/ (a barra aparece na tela)")
    }

    @Test
    fun languageNamesAreTheSameInBothLanguages() {
        val pt = portuguese()
        val en = read("values-en") ?: return
        for (key in listOf("settings_language_portuguese", "settings_language_english")) {
            assertTrue(key in pt.strings, key)
            assertEquals(pt.strings[key], en.strings[key], "$key fica no próprio idioma, igual nas duas pastas")
        }
    }

    @Test
    fun eachPortugueseKeyAppearsOnce() {
        // A leitura falha se uma chave aparecer em dois arquivos de values/.
        assertTrue(portuguese().keys.isNotEmpty())
    }

    private companion object {
        /**
         * Liga quando a tradução de `values-en/` terminar (etapa de tradução da Leva 11B): daí em diante, toda chave de
         * `values/` precisa existir em inglês, e o build falha se faltar alguma.
         */
        const val EVERY_KEY_TRANSLATED = true

        val MARKER = Regex("""%\d+\$[sd]|\{[a-z_]+\}""")
        val UNNUMBERED = Regex("""%[sd]""")
    }
}
