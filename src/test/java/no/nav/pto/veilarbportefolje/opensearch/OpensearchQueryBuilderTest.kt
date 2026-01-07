package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.util.TestUtil
import org.junit.BeforeClass
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ApplicationConfigTest::class])
class OpensearchQueryBuilderTest {

    @Autowired
    lateinit var sortQueryBuilder: OpensearchSortQueryBuilder

    @Autowired
    lateinit var filterQueryBuilder: OpensearchFilterQueryBuilder

    @Test
    fun skal_sortere_etternavn_paa_etternavn_feltet() {
        println("SortQueryBuilder: $sortQueryBuilder") // Should not be null

        val searchSourceBuilder = sortQueryBuilder.sorterQueryParametere(
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.ETTERNAVN,
            SearchSourceBuilder(),
            Filtervalg(),
            BrukerinnsynTilganger(true, true, true)
        )
        val fieldName: String = searchSourceBuilder.sorts()[0].toString()
        assertThat(fieldName).contains(Sorteringsfelt.ETTERNAVN.sorteringsverdi)
    }

    @Test
    fun skal_bygge_riktig_filtrer_paa_veileder_script() {
        val actualScript: String = sortQueryBuilder.byggVeilederPaaEnhetScript(listOf("Z000000", "Z000001", "Z000002"))
        val expectedScript =
            "(doc.veileder_id.size() != 0 && [\"Z000000\",\"Z000001\",\"Z000002\"].contains(doc.veileder_id.value)).toString()"

        assertThat(actualScript).isEqualTo(expectedScript)
    }

    @Test
    fun skal_sortere_paa_aktiviteter_som_er_satt_til_ja() {
        val navnPaAktivitet = "behandling"
        val filtervalg: Filtervalg = Filtervalg().apply {
            aktiviteter[navnPaAktivitet] = AktivitetFiltervalg.JA
            aktiviteter["egen"] = AktivitetFiltervalg.NEI
        }

        val sorteringer =
            sortQueryBuilder.sorterValgteAktiviteter(
                filtervalg,
                SearchSourceBuilder(),
                org.opensearch.search.sort.SortOrder.ASC
            )

        val expectedJson: String = TestUtil.readFileAsJsonString("/sorter_aktivitet_behandling.json", javaClass)
        val actualJson: String = sorteringer.sorts()[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_om_man_velger_nei_paa_tiltak() {
        val filtervalg: Filtervalg = Filtervalg().apply {
            aktiviteter["tiltak"] = AktivitetFiltervalg.NEI
        }

        val builders = filterQueryBuilder.byggAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

        val expectedJson: String = TestUtil.readFileAsJsonString("/nei_paa_tiltak.json", javaClass)
        val actualJson: String = builders[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_om_man_velger_ja_paa_behandling() {
        val filtervalg: Filtervalg = Filtervalg().apply {
            aktiviteter["behandling"] = AktivitetFiltervalg.JA
        }
        val builders = filterQueryBuilder.byggAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

        val expectedJson: String = TestUtil.readFileAsJsonString("/ja_paa_behandling.json", javaClass)
        val actualJson: String = builders[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_om_man_velger_ja_paa_tiltak() {
        val filtervalg: Filtervalg = Filtervalg().apply {
            aktiviteter["tiltak"] = AktivitetFiltervalg.JA
        }
        val builders = filterQueryBuilder.byggAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

        val expectedJson: String = TestUtil.readFileAsJsonString("/ja_paa_tiltak.json", javaClass)
        val actualJson: String = builders[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_for_aa_hente_ut_brukere_som_er_19_aar_og_under() {
        val builder = BoolQueryBuilder()
        filterQueryBuilder.byggAlderQuery("19-og-under", builder)

        val actualJson: String = builder.toString()
        val expectedJson: String = TestUtil.readFileAsJsonString("/19_aar_og_under.json", javaClass)

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_for_aa_hente_ut_brukere_mellom_20_24_aar() {
        val builder = BoolQueryBuilder()
        filterQueryBuilder.byggAlderQuery("20-24", builder)

        val actualJson: String = builder.toString()
        val expectedJson: String = TestUtil.readFileAsJsonString("/mellom_20_24_aar.json", javaClass)

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_for_aa_hente_ut_portefoljestorrelser() {
        val request = filterQueryBuilder.byggPortefoljestorrelserQuery("0000")

        val actualJson: String = request.aggregations().toString()
        val expectedJson: String = TestUtil.readFileAsJsonString("/portefoljestorrelser.json", javaClass)

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun `byggUlestEndringsFilter should add orQuery for single category`() {
        val boolQuery = BoolQueryBuilder()
        val categories = listOf("AKTIVITET")
        val method = OpensearchFilterQueryBuilder::class.java.getDeclaredMethod(
            "byggUlestEndringsFilter", List::class.java, BoolQueryBuilder::class.java
        )
        method.isAccessible = true

        method.invoke(filterQueryBuilder, categories, boolQuery)

        val queryString = boolQuery.toString()
        assertThat(queryString).contains("siste_endringer.AKTIVITET.erSett")
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            System.setProperty("FASIT_ENVIRONMENT_NAME", "test")
        }
    }
}
