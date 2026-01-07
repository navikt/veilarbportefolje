package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.filtervalgDefaults
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OpensearchServiceIntFilterGeoAlderKjonnTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest() {

    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value
    }

    @Test
    fun skal_returnere_brukere_basert_paa_fodselsdag_i_maaneden() {
        val testBrukerFodselsdagSyvende = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            fodselsdag_i_mnd = 7,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
        )

        val testBrukerFodselsdagNiende = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            fodselsdag_i_mnd = 9,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
        )

        val filterValg = filtervalgDefaults.copy(
            fodselsdagIMnd = listOf("7")
        )

        val liste = listOf(testBrukerFodselsdagSyvende, testBrukerFodselsdagNiende)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null

        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertEquals(
            testBrukerFodselsdagSyvende.fnr,
            response.brukere.first().fnr
        )
    }

    @Test
    fun skal_hente_ut_brukere_basert_paa_kjonn() {
        val mann = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            kjonn = "M",
        )

        val kvinne = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            kjonn = "K",
        )

        val liste = listOf(kvinne, mann)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = filtervalgDefaults.copy(
            kjonn = Kjonn.K
        )

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertEquals(kvinne.fnr, response.brukere.first().fnr)
    }

    @Test
    fun skal_hente_alle_brukere_som_har_tolkbehov() {
        val tolkesprakJapansk = "JPN"
        val tolkesprakSvensk = "SWE"
        val tolkesprakNorsk = "NB"

        val trengerTalespraktolkBehovSistOppdatert = "2022-02-22"
        val trengerTalespraktolk = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
        trengerTalespraktolk.talespraaktolk = tolkesprakJapansk
        trengerTalespraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTalespraktolkBehovSistOppdatert)

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
        trengerTaleOgTegnspraktolk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert)

        val trengerTegnspraktolkBehovSistOppdatert = "2023-03-24"
        val trengerTegnspraktolk = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
        trengerTegnspraktolk.tegnspraaktolk = tolkesprakNorsk
        trengerTegnspraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert)

        val brukerUtenTolkebehov1 = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
        brukerUtenTolkebehov1.talespraaktolk = null
        brukerUtenTolkebehov1.tegnspraaktolk = null

        val brukerUtenTolkebehov2 = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
        brukerUtenTolkebehov2.talespraaktolk = ""
        brukerUtenTolkebehov2.tegnspraaktolk = ""

        val liste = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            trengerTegnspraktolk,
            brukerUtenTolkebehov1,
            brukerUtenTolkebehov2
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        /* Skal hente alle med talespråktolk */
        val filterValgTalespraaktolk = filtervalgDefaults.copy(
            tolkebehov = listOf("TALESPRAAKTOLK")
        )

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgTalespraaktolk,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegnspråktolk */
        val filterValgTegnspraaktolk = filtervalgDefaults.copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgTegnspraaktolk,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakNorsk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegn- eller talespråktolk */
        val filtervalgBegge = filtervalgDefaults.copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtervalgBegge,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med japansk som tegn- eller talespråk */
        val filterValgBeggeJapansk = filtervalgDefaults.copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"),
            tolkBehovSpraak = listOf(tolkesprakJapansk)
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgBeggeJapansk,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })


        /* Skal hente alle med japansk som tegn- eller talespråk, også når ingen tolkebehov er valgt */
        val filterValgIngenJapansk = filtervalgDefaults.copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"),
            tolkBehovSpraak = listOf(tolkesprakJapansk)
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgIngenJapansk,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
    }

    @Test
    fun skal_hente_alle_brukere_fra_landgruppe() {
        val brukerFraLandGruppe1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedeland = "NOR",
            foedelandFulltNavn = "Norge",
            landgruppe = "1",
        )

        val brukerFraLandGruppe2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedeland = "EST",
            foedelandFulltNavn = "Estland",
            landgruppe = "2",
            hovedStatsborgerskap = Statsborgerskap("Estland", LocalDate.now(), null),
        )

        val brukerfralandgruppe31 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedeland = "AZE",
            foedelandFulltNavn = "Aserbajdsjan",
            landgruppe = "3",
            hovedStatsborgerskap = Statsborgerskap("Norge", LocalDate.now(), null),
        )

        val brukerfralandgruppe32 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedeland = "SGP",
            foedelandFulltNavn = "Singapore",
            landgruppe = "3",
            hovedStatsborgerskap = Statsborgerskap("Singapore", LocalDate.now(), null),
        )

        val brukerUkjentLandGruppe = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            hovedStatsborgerskap = Statsborgerskap("Norge", LocalDate.now(), null),
        )

        val liste = listOf(
            brukerFraLandGruppe1,
            brukerFraLandGruppe2,
            brukerfralandgruppe31,
            brukerfralandgruppe32,
            brukerUkjentLandGruppe
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValgGr3 = filtervalgDefaults.copy(
            landgruppe = listOf("LANDGRUPPE_3")
        )

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgGr3,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == "Aserbajdsjan" })
        org.junit.jupiter.api.Assertions.assertTrue(
            (response.brukere.stream().anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == "Singapore" })
        )

        val filterValgFoedelandNorge = filtervalgDefaults.copy(
            foedeland = listOf("NOR")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgFoedelandNorge,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == "Norge" })

        val filterValgLandgruppeUkjent = filtervalgDefaults.copy(
            landgruppe = listOf("LANDGRUPPE_UKJENT")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgLandgruppeUkjent,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().noneMatch { x: PortefoljebrukerFrontendModell -> x.foedeland != null })

        val filterValgLandgruppeUkjentOgTre = filtervalgDefaults.copy(
            landgruppe = listOf("LANDGRUPPE_UKJENT", "LANDGRUPPE_3")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgLandgruppeUkjentOgTre,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.foedeland != null }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == "Singapore" })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.foedeland != null }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == "Aserbajdsjan" })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: PortefoljebrukerFrontendModell -> x.foedeland == null })
    }

    @Test
    fun test_sortering_landgruppe() {
        val brukerFraLandGruppe1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedeland = "NOR",
            foedelandFulltNavn = "Norge",
            landgruppe = "1",
        )

        val brukerFraLandGruppe2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedelandFulltNavn = "Estland",
            landgruppe = "2",
            hovedStatsborgerskap = Statsborgerskap("Estland", LocalDate.now(), null),
        )

        val brukerfralandgruppe31 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedelandFulltNavn = "Aserbajdsjan",
            landgruppe = "3",
            hovedStatsborgerskap = Statsborgerskap("Norge", LocalDate.now(), null),
        )

        val brukerfralandgruppe32 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedelandFulltNavn = "Singapore",
            landgruppe = "3",
            hovedStatsborgerskap = Statsborgerskap("Singapore", LocalDate.now(), null),
        )

        val brukerfralandgruppe33 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            foedelandFulltNavn = "Botswana",
            landgruppe = "3",
            hovedStatsborgerskap = Statsborgerskap("Botswana", LocalDate.now(), null),
        )

        val brukerUkjentLandGruppe = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            hovedStatsborgerskap = Statsborgerskap("Norge", LocalDate.now(), null),
        )

        val liste = listOf(
            brukerFraLandGruppe1,
            brukerFraLandGruppe2,
            brukerfralandgruppe31,
            brukerfralandgruppe32,
            brukerfralandgruppe33,
            brukerUkjentLandGruppe
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = filtervalgDefaults

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.FODELAND,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        org.junit.jupiter.api.Assertions.assertEquals("Aserbajdsjan", response.brukere[0].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Botswana", response.brukere[1].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Estland", response.brukere[2].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Norge", response.brukere[3].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Singapore", response.brukere[4].foedeland)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[5].foedeland)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.FODELAND,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        org.junit.jupiter.api.Assertions.assertEquals("Singapore", response.brukere[0].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Norge", response.brukere[1].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Estland", response.brukere[2].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Botswana", response.brukere[3].foedeland)
        org.junit.jupiter.api.Assertions.assertEquals("Aserbajdsjan", response.brukere[4].foedeland)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[5].foedeland)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.STATSBORGERSKAP,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        org.junit.jupiter.api.Assertions.assertEquals(
            "Botswana",
            response.brukere[0].hovedStatsborgerskap!!.statsborgerskap
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            "Estland",
            response.brukere[1].hovedStatsborgerskap!!.statsborgerskap
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            "Norge",
            response.brukere[2].hovedStatsborgerskap!!.statsborgerskap
        )
    }

    @Test
    fun skal_hente_alle_brukere_med_bosted() {
        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "10",
            bydelsnummer = "1222",
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "12",
            bydelsnummer = "1233",
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "12",
            bydelsnummer = "1234",
        )

        val bruker4 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "10",
            bydelsnummer = "1010",
        )

        val brukerUkjentBosted = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValgGeo10 = filtervalgDefaults.copy(
            geografiskBosted = listOf("10")
        )

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgGeo10,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .allMatch { x: PortefoljebrukerFrontendModell -> x.geografiskBosted.bostedKommune == "10" })

        val filterValgGeo1233 = filtervalgDefaults.copy(
            geografiskBosted = listOf("1233")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgGeo1233,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .allMatch { x: PortefoljebrukerFrontendModell -> x.geografiskBosted.bostedBydel == "1233" })

        val filterValgGeoBegge = filtervalgDefaults.copy(
            geografiskBosted = listOf("10", "1233")
        )

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValgGeoBegge,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(3)
        Assertions.assertThat(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell ->
                    x.geografiskBosted.bostedKommune.equals(
                        "10",
                        ignoreCase = true
                    )
                }.count()
        )
            .isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell ->
                    x.geografiskBosted.bostedBydel.equals(
                        "1233",
                        ignoreCase = true
                    )
                })
    }

    @Test
    fun test_sortering_bostedkommune() {
        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "10",
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "12",
            bydelsnummer = "1233",
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "12",
            bydelsnummer = "1234",
        )

        val bruker4 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            kommunenummer = "10",
            bydelsnummer = "1010",
        )

        val brukerUkjentBosted = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = filtervalgDefaults

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BOSTED_KOMMUNE,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertEquals("10", response.brukere[0].geografiskBosted.bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("10", response.brukere[1].geografiskBosted.bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("12", response.brukere[2].geografiskBosted.bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("12", response.brukere[3].geografiskBosted.bostedKommune)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[4].geografiskBosted.bostedKommune)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.BOSTED_BYDEL,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertEquals("1234", response.brukere[0].geografiskBosted.bostedBydel)
        org.junit.jupiter.api.Assertions.assertEquals("1233", response.brukere[1].geografiskBosted.bostedBydel)
        org.junit.jupiter.api.Assertions.assertEquals("1010", response.brukere[2].geografiskBosted.bostedBydel)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[3].geografiskBosted.bostedBydel)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[4].geografiskBosted.bostedBydel)
    }

    private fun genererRandomBruker(
        enhet: String, veilederId: String?
    ): PortefoljebrukerOpensearchModell {
        val bruker =
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = enhet,
                egen_ansatt = false,
                diskresjonskode = null,
            )

        if (veilederId != null) {
            bruker.veileder_id = veilederId
        }
        return bruker
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
