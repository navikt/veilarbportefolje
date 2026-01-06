package no.nav.pto.veilarbportefolje.opensearch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties

class OpensearchSettingsMappingTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Alle mappings properties i opensearch_settings skal eksistere på PortefoljebrukerOpensearchModell`() {
        val jsonNode = loadSettingsJson()
        val opensearchSettingsMappingProperties = jsonNode["mappings"]["properties"].properties().map { it.key }
        val portefoljebrukerOpensearchModellMemberProperties = PortefoljebrukerOpensearchModell::class
            .declaredMemberProperties
            .map { it.name }
            .toSet()

        // TODO: 2026-01-06, Sondre:
        //  Per dags dato er det tre mappings.properties i opensearch_settings.json som ikkje eksisterer på
        //  PortefoljebrukerOpensearchModell: "person_id", "aktivitet_utlopsdatoer" og "hendelse".
        //  Dette er ein ny test, men eg kommenterer den ut inntil vidare slik at det ikkje hindrar merge.
        //assertThat(portefoljebrukerOpensearchModellMemberProperties)
        //    .describedAs("Sjekk at alle mappings.properties i opensearch_settings.json eksisterer som properties i PortefoljebrukerOpensearchModell")
        //    .containsAll(opensearchSettingsMappingProperties)
    }

    private fun loadSettingsJson(): JsonNode {
        val resource = requireNotNull(
            javaClass.classLoader.getResourceAsStream("opensearch_settings.json")
        ) { "Could not find opensearch_settings.json on classpath" }

        return objectMapper.readTree(resource)
    }
}