package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.*
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
        val testBrukerFodselsdagSyvende = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setFodselsdag_i_mnd(7)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)

        val testBrukerFodselsdagNiende = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setFodselsdag_i_mnd(9)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)


        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setFodselsdagIMnd(listOf("7"))

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
        val mann = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setKjonn("M")

        val kvinne = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setKjonn("K")

        val liste = listOf(kvinne, mann)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setKjonn(Kjonn.K)

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
            .setTalespraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkBehovSistOppdatert))

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert))

        val trengerTegnspraktolkBehovSistOppdatert = "2023-03-24"
        val trengerTegnspraktolk = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
            .setTegnspraaktolk(tolkesprakNorsk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert))

        val brukerUtenTolkebehov1 = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukerUtenTolkebehov2 = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0
        )
            .setTalespraaktolk("")
            .setTegnspraaktolk("")

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
        var filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TALESPRAAKTOLK"))

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.talespraaktolk == tolkesprakSvensk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegnspråktolk */
        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.tegnspraaktolk == tolkesprakSvensk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.tegnspraaktolk == tolkesprakNorsk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegn- eller talespråktolk */
        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med japansk som tegn- eller talespråk */
        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))
            .setTolkBehovSpraak(listOf(tolkesprakJapansk))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })


        /* Skal hente alle med japansk som tegn- eller talespråk, også når ingen tolkebehov er valgt */
        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf())
            .setTolkBehovSpraak(listOf(tolkesprakJapansk))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.tolkebehov.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: Bruker -> x.tolkebehov.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
    }

    @Test
    fun skal_hente_alle_brukere_fra_landgruppe() {
        val brukerFraLandGruppe1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedeland("NOR")
            .setFoedelandFulltNavn("Norge")
            .setLandgruppe("1")

        val brukerFraLandGruppe2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedeland("EST")
            .setFoedelandFulltNavn("Estland")
            .setLandgruppe("2")
            .setHovedStatsborgerskap(Statsborgerskap("Estland", LocalDate.now(), null))

        val brukerfralandgruppe31 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedeland("AZE")
            .setFoedelandFulltNavn("Aserbajdsjan")
            .setLandgruppe("3")
            .setHovedStatsborgerskap(Statsborgerskap("Norge", LocalDate.now(), null))

        val brukerfralandgruppe32 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedeland("SGP")
            .setFoedelandFulltNavn("Singapore")
            .setLandgruppe("3")
            .setHovedStatsborgerskap(Statsborgerskap("Singapore", LocalDate.now(), null))

        val brukerUkjentLandGruppe = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setHovedStatsborgerskap(Statsborgerskap("Norge", LocalDate.now(), null))

        val liste = listOf(
            brukerFraLandGruppe1,
            brukerFraLandGruppe2,
            brukerfralandgruppe31,
            brukerfralandgruppe32,
            brukerUkjentLandGruppe
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        var filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setLandgruppe(listOf("LANDGRUPPE_3"))

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: Bruker -> x.foedeland == "Aserbajdsjan" })
        org.junit.jupiter.api.Assertions.assertTrue(
            (response.brukere.stream().anyMatch { x: Bruker -> x.foedeland == "Singapore" })
        )


        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setFoedeland(listOf("NOR"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: Bruker -> x.foedeland == "Norge" })


        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setLandgruppe(listOf("LANDGRUPPE_UKJENT"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().noneMatch { x: Bruker -> x.foedeland != null })

        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setLandgruppe(listOf("LANDGRUPPE_UKJENT", "LANDGRUPPE_3"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.foedeland != null }
                .anyMatch { x: Bruker -> x.foedeland == "Singapore" })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: Bruker -> x.foedeland != null }
                .anyMatch { x: Bruker -> x.foedeland == "Aserbajdsjan" })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: Bruker -> x.foedeland == null })
    }

    @Test
    fun test_sortering_landgruppe() {
        val brukerFraLandGruppe1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedeland("NOR")
            .setFoedelandFulltNavn("Norge")
            .setLandgruppe("1")

        val brukerFraLandGruppe2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedelandFulltNavn("Estland")
            .setLandgruppe("2")
            .setHovedStatsborgerskap(Statsborgerskap("Estland", LocalDate.now(), null))

        val brukerfralandgruppe31 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedelandFulltNavn("Aserbajdsjan")
            .setLandgruppe("3")
            .setHovedStatsborgerskap(Statsborgerskap("Norge", LocalDate.now(), null))

        val brukerfralandgruppe32 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedelandFulltNavn("Singapore")
            .setLandgruppe("3")
            .setHovedStatsborgerskap(Statsborgerskap("Singapore", LocalDate.now(), null))

        val brukerfralandgruppe33 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFoedelandFulltNavn("Botswana")
            .setLandgruppe("3")
            .setHovedStatsborgerskap(Statsborgerskap("Botswana", LocalDate.now(), null))

        val brukerUkjentLandGruppe = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setHovedStatsborgerskap(Statsborgerskap("Norge", LocalDate.now(), null))

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


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())

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
            response.brukere[0].hovedStatsborgerskap.getStatsborgerskap()
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            "Estland",
            response.brukere[1].hovedStatsborgerskap.getStatsborgerskap()
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            "Norge",
            response.brukere[2].hovedStatsborgerskap.getStatsborgerskap()
        )
    }

    @Test
    fun skal_hente_alle_brukere_med_bosted() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("10")
            .setBydelsnummer("1222")

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("12")
            .setBydelsnummer("1233")

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("12")
            .setBydelsnummer("1234")

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("10")
            .setBydelsnummer("1010")

        val brukerUkjentBosted = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        var filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setGeografiskBosted(listOf("10"))

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().allMatch { x: Bruker -> x.bostedKommune == "10" })


        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setGeografiskBosted(listOf("1233"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(1)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().allMatch { x: Bruker -> x.bostedBydel == "1233" })


        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setGeografiskBosted(listOf("10", "1233"))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(3)
        Assertions.assertThat(
            response.brukere.stream().filter { x: Bruker -> x.bostedKommune.equals("10", ignoreCase = true) }.count()
        )
            .isEqualTo(2)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().anyMatch { x: Bruker -> x.bostedBydel.equals("1233", ignoreCase = true) })
    }

    @Test
    fun test_sortering_bostedkommune() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("10")

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("12")
            .setBydelsnummer("1233")

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("12")
            .setBydelsnummer("1234")

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setKommunenummer("10")
            .setBydelsnummer("1010")

        val brukerUkjentBosted = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, brukerUkjentBosted)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())

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
        org.junit.jupiter.api.Assertions.assertEquals("10", response.brukere[0].bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("10", response.brukere[1].bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("12", response.brukere[2].bostedKommune)
        org.junit.jupiter.api.Assertions.assertEquals("12", response.brukere[3].bostedKommune)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[4].bostedKommune)

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
        org.junit.jupiter.api.Assertions.assertEquals("1234", response.brukere[0].bostedBydel)
        org.junit.jupiter.api.Assertions.assertEquals("1233", response.brukere[1].bostedBydel)
        org.junit.jupiter.api.Assertions.assertEquals("1010", response.brukere[2].bostedBydel)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[3].bostedBydel)
        org.junit.jupiter.api.Assertions.assertNull(response.brukere[4].bostedBydel)
    }

    private fun genererRandomBruker(
        enhet: String, veilederId: String?
    ): PortefoljebrukerOpensearchModell {
        val bruker =
            PortefoljebrukerOpensearchModell().setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().get()).setOppfolging(true)
                .setEnhet_id(enhet).setEgen_ansatt(false).setDiskresjonskode(null)

        if (veilederId != null) {
            bruker.setVeileder_id(veilederId)
        }
        return bruker
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
