package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori
import no.nav.pto.veilarbportefolje.util.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.jupiter.api.Test
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
            getFiltervalgDefaults(),
            BrukerinnsynTilganger(true, true, true)
        )
        val fieldName: String = searchSourceBuilder.sorts()[0].toString()
        assertThat(fieldName).contains(Sorteringsfelt.ETTERNAVN.sorteringsverdi)
    }

    @Test
    fun skal_sortere_utdanning_og_situasjon_sist_endret_paa_v3_felt_naar_toggle_er_paa() {
        fun sortertFelt(brukV3: Boolean): String =
            sortQueryBuilder.sorterQueryParametere(
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.UTDANNING_OG_SITUASJON_SIST_ENDRET,
                SearchSourceBuilder(),
                getFiltervalgDefaults(),
                BrukerinnsynTilganger(true, true, true),
                brukV3
            ).sorts()[0].toString()

        assertThat(sortertFelt(brukV3 = true)).contains("arbeidssoekerregisteret.opplysninger.opplysningerSistSendtInnDato")
        assertThat(sortertFelt(brukV3 = false)).contains("\"utdanning_og_situasjon_sist_endret\"")
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
        val filtervalg = getFiltervalgDefaults().copy(
            aktiviteter = mapOf(
                AktivitetFilterverdier.BEHANDLING to AktivitetAvansertFilterverdier.JA,
                AktivitetFilterverdier.EGEN to AktivitetAvansertFilterverdier.NEI
            )
        )

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
        val filtervalg = getFiltervalgDefaults().copy(
            aktiviteter = mapOf(AktivitetFilterverdier.TILTAK to AktivitetAvansertFilterverdier.NEI)
        )

        val builders = filterQueryBuilder.byggAvansertAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

        val expectedJson: String = TestUtil.readFileAsJsonString("/nei_paa_tiltak.json", javaClass)
        val actualJson: String = builders[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_om_man_velger_ja_paa_behandling() {
        val filtervalg = getFiltervalgDefaults().copy(
            aktiviteter = mapOf(AktivitetFilterverdier.BEHANDLING to AktivitetAvansertFilterverdier.JA)
        )
        val builders = filterQueryBuilder.byggAvansertAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

        val expectedJson: String = TestUtil.readFileAsJsonString("/ja_paa_behandling.json", javaClass)
        val actualJson: String = builders[0].toString()

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson)
    }

    @Test
    fun skal_bygge_korrekt_json_om_man_velger_ja_paa_tiltak() {
        val filtervalg = getFiltervalgDefaults().copy(
            aktiviteter = mapOf(AktivitetFilterverdier.TILTAK to AktivitetAvansertFilterverdier.JA)
        )
        val builders = filterQueryBuilder.byggAvansertAktivitetFilterQuery(filtervalg, QueryBuilders.boolQuery())

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
    fun `byggUlestEndringsFilter skal bruke orQuery for endringskategorien når den finnes`() {
        val boolQuery = BoolQueryBuilder()
        val method = OpensearchFilterQueryBuilder::class.java.getDeclaredMethod(
            "byggUlestEndringsFilter", String::class.java, BoolQueryBuilder::class.java
        )
        method.isAccessible = true
        method.invoke(filterQueryBuilder, "AKTIVITET", boolQuery)

        val queryString = boolQuery.toString()
        assertThat(queryString)
            .contains("siste_endringer.AKTIVITET.erSett")
    }

    @Test
    fun `byggUlestEndringsFilter skal bruke orQuery for alle de mulige kategoriene når endringskategorien er null`() {
        val boolQuery = BoolQueryBuilder()
        val method = OpensearchFilterQueryBuilder::class.java.getDeclaredMethod(
            "byggUlestEndringsFilter", String::class.java, BoolQueryBuilder::class.java
        )
        method.isAccessible = true
        method.invoke(filterQueryBuilder, null, boolQuery)

        val queryString = boolQuery.toString()

        SisteEndringsKategori.entries.forEach {
            assertThat(queryString)
                .contains("siste_endringer.${it.name}.erSett")
        }
    }

    @Test
    fun `skal bruke matchQuery for navn med ett navneledd`() {
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
        val boolQuery = QueryBuilders.boolQuery()

        filterQueryBuilder.leggTilManuelleFilter(boolQuery, filtervalg)

        val queryString = boolQuery.toString().replace("\\s".toRegex(), "")
        assertThat(queryString).contains("\"match\":{\"fullt_navn\"")
        assertThat(queryString).contains("\"operator\":\"AND\"")
        assertThat(queryString).doesNotContain("\"term\":{\"fullt_navn\"")
    }

    @Test
    fun `skal matche alle navneledd, uten komma`() {
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola Nordmann")
        val boolQuery = QueryBuilders.boolQuery()

        filterQueryBuilder.leggTilManuelleFilter(boolQuery, filtervalg)

        val queryString = boolQuery.toString().replace("\\s".toRegex(), "")
        assertThat(queryString).contains("\"query\":\"ola\"")
        assertThat(queryString).contains("\"query\":\"nordmann\"")
        assertThat(queryString).contains("\"operator\":\"AND\"")
    }

    @Test
    fun `skal matche alle navneledd, med komma`() {
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Nordmann, Ola")
        val boolQuery = QueryBuilders.boolQuery()

        filterQueryBuilder.leggTilManuelleFilter(boolQuery, filtervalg)

        val queryString = boolQuery.toString().replace("\\s".toRegex(), "")
        assertThat(queryString).contains("\"query\":\"nordmann\"")
        assertThat(queryString).contains("\"query\":\"ola\"")
        assertThat(queryString).contains("\"operator\":\"AND\"")
    }

    @Test
    fun `arbeidssoekerfilter skal bruke v3-felter når toggle er på`() {
        val filtervalg = getFiltervalgDefaults().copy(
            registreringstype = listOf(JobbSituasjonBeskrivelse.HAR_SAGT_OPP),
            utdanning = listOf(UtdanningSvar.GRUNNSKOLE),
            utdanningBestatt = listOf(UtdanningBestattSvar.JA),
            utdanningGodkjent = listOf(UtdanningGodkjentSvar.INGEN_DATA)
        )

        val v3Query = QueryBuilders.boolQuery()
        filterQueryBuilder.leggTilManuelleFilter(v3Query, filtervalg, brukV3AvArbeidssoekerregisteret = true)
        val v3QueryString = v3Query.toString()

        assertThat(v3QueryString).contains("arbeidssoekerregisteret.opplysninger.brukersSituasjoner")
        assertThat(v3QueryString).contains("arbeidssoekerregisteret.opplysninger.utdanning\"")
        assertThat(v3QueryString).contains("arbeidssoekerregisteret.opplysninger.utdanningBestatt")
        assertThat(v3QueryString).contains("arbeidssoekerregisteret.opplysninger.utdanningGodkjent")
        assertThat(v3QueryString).doesNotContain(
            "\"brukers_situasjoner\"",
            "\"utdanning\"",
            "\"utdanning_bestatt\"",
            "\"utdanning_godkjent\""
        )

        val v2Query = QueryBuilders.boolQuery()
        filterQueryBuilder.leggTilManuelleFilter(v2Query, filtervalg, brukV3AvArbeidssoekerregisteret = false)
        val v2QueryString = v2Query.toString()

        assertThat(v2QueryString).contains(
            "\"brukers_situasjoner\"",
            "\"utdanning\"",
            "\"utdanning_bestatt\"",
            "\"utdanning_godkjent\""
        )
        assertThat(v2QueryString).doesNotContain("arbeidssoekerregisteret")
    }


    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            System.setProperty("FASIT_ENVIRONMENT_NAME", "test")
        }
    }
}
