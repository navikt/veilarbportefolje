package no.nav.pto.veilarbportefolje.opensearch

import no.nav.poao_tilgang.client.Decision.Deny
import no.nav.poao_tilgang.client.Decision.Permit
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.BarnUnder18Aar
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class OpensearchServiceIntTilgangskontrollBarnTest @Autowired constructor(
    private val opensearchService: OpensearchService,
    private val poaoTilgangWrapper: PoaoTilgangWrapper
) : EndToEndTest() {
    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Deny("", ""))
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Deny("", ""))
    }

    @Test
    fun test_sortering_barn_under_18_veileder_tilgang_6_7() {

        val bruker1B = opprettBruker(listOf(BarnUnder18AarData(5, "7")), nyForVeileder = false)
        val bruker2barnU = opprettBruker(listOf(BarnUnder18AarData(8, null), BarnUnder18AarData(1, "6")))
        val bruker3barn1m62u = opprettBruker(
            listOf(
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(4, null)
            ), nyForVeileder = false
        )
        val bruker4barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(4, "6"),
                BarnUnder18AarData(1, "7")
            ), nyForVeileder = false
        )
        val bruker5barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            ), nyForVeileder = false
        )
        val bruker6b = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            ), nyForVeileder = false
        )
        val brukerTomListe = opprettBruker(emptyList(), nyForVeileder = false)
        val brukerIngenListe = opprettBruker(null, nyForVeileder = false)
        val brukerMedBarnMedKode19 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(4, "19"),
                BarnUnder18AarData(2, "19")
            ), nyForVeileder = false
        )

        val liste = listOf(
            bruker1B,
            bruker2barnU,
            bruker3barn1m62u,
            bruker4barn,
            bruker5barn,
            bruker6b,
            brukerTomListe,
            brukerIngenListe,
            brukerMedBarnMedKode19
        )

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )
        assertThat(response.antall).isEqualTo(7)
        assertThat(response.brukere[0].fnr).isEqualTo(bruker1B.fnr)
        assertThat(response.brukere[1].fnr).isEqualTo(bruker2barnU.fnr)
        assertThat(response.brukere[2].fnr).isEqualTo(bruker3barn1m62u.fnr)
        assertThat(response.brukere[3].fnr).isEqualTo(bruker4barn.fnr)
        assertThat(response.brukere[4].fnr).isEqualTo(bruker5barn.fnr)
        assertThat(response.brukere[5].fnr).isEqualTo(bruker6b.fnr)
        assertThat(response.brukere[6].fnr).isEqualTo(brukerMedBarnMedKode19.fnr)
    }

    @Test
    fun test_sortering_barn_under_18_veileder_ikke_tilgang_6_7() {
        val bruker1B = opprettBruker(listOf(BarnUnder18AarData(5, "7")), nyForVeileder = false)

        val bruker2barnU = opprettBruker(
            listOf(
                BarnUnder18AarData(8, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(1, "6")
            )
        )

        val bruker3barn1m62u = opprettBruker(
            listOf(
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(4, null),
                BarnUnder18AarData(4, null),
                BarnUnder18AarData(14, null)
            ),
            nyForVeileder = false
        )

        val bruker4barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(4, "6"),
                BarnUnder18AarData(1, "7")
            ),
            nyForVeileder = false
        )

        val bruker5barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(4, null),
                BarnUnder18AarData(14, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            ),
            nyForVeileder = false
        )

        val bruker6b = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(13, null),
                BarnUnder18AarData(3, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            ),
            nyForVeileder = false
        )

        val brukerTomListe = opprettBruker(emptyList(), nyForVeileder = false)

        val brukerIngenListe = opprettBruker(null, nyForVeileder = false)

        val brukerMedBarnMedKode19 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(4, "19"),
                BarnUnder18AarData(2, "19")
            ),
            nyForVeileder = false
        )

        val liste = listOf(
            bruker1B,
            bruker2barnU,
            bruker3barn1m62u,
            bruker4barn,
            bruker5barn,
            bruker6b,
            brukerTomListe,
            brukerIngenListe,
            brukerMedBarnMedKode19
        )


        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(5)
        assertThat(response.brukere[0].fnr).isEqualTo(bruker4barn.fnr)
        assertThat(response.brukere[1].fnr).isEqualTo(bruker2barnU.fnr)
        assertThat(response.brukere[2].fnr).isEqualTo(bruker6b.fnr)
        assertThat(response.brukere[3].fnr).isEqualTo(bruker5barn.fnr)
        assertThat(response.brukere[4].fnr).isEqualTo(bruker3barn1m62u.fnr)
    }

    @Test
    fun `skal sortere brukere med barn under 18 med tilgang til kode 6 og 7`() {

        val bruker1 = opprettBruker(listOf(BarnUnder18AarData(5, "7")))
        val bruker2 = opprettBruker(listOf(BarnUnder18AarData(8, null), BarnUnder18AarData(1, "6")))
        val bruker3 = opprettBruker(
            listOf(
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(4, null)
            )
        )
        val bruker4 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(4, "6"),
                BarnUnder18AarData(1, "7")
            )
        )
        val bruker5 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            )
        )
        val bruker6 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            )
        )
        val brukerTomListe = opprettBruker(emptyList())
        val brukerIngenListe = opprettBruker(null)
        val brukerKode19 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(4, "19"),
                BarnUnder18AarData(2, "19")
            )
        )

        val brukere = listOf(
            bruker1, bruker2, bruker3, bruker4, bruker5, bruker6,
            brukerTomListe, brukerIngenListe, brukerKode19
        )

        // Mock tilgang
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filterValg = Filtervalg().apply {
            ferdigfilterListe = emptyList()
            barnUnder18Aar = listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR)
        }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )

        val forventetRekkefolge = listOf(
            bruker1, bruker2, bruker3, bruker4, bruker5, bruker6, brukerKode19
        )

        assertThat(response.antall).isEqualTo(forventetRekkefolge.size)
        forventetRekkefolge.forEachIndexed { index, bruker ->
            assertThat(response.brukere[index].fnr).isEqualTo(bruker.fnr)
        }
    }


    @Test
    fun test_sortering_barn_under_18_veileder_tilgang_6_ikke_7() {
        val bruker1B = opprettBruker(listOf(BarnUnder18AarData(5, "7")), nyForVeileder = false)

        val bruker2barn6U = opprettBruker(
            listOf(
                BarnUnder18AarData(8, null),
                BarnUnder18AarData(2, null),
                BarnUnder18AarData(1, "6"),
                BarnUnder18AarData(1, "6")
            )
        )

        val bruker2barnU = opprettBruker(listOf(BarnUnder18AarData(4, null)), nyForVeileder = false)

        val bruker4barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(13, null),
                BarnUnder18AarData(4, "6"),
                BarnUnder18AarData(1, "7")
            ),
            nyForVeileder = false
        )

        val bruker5barn = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(13, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6")
            ),
            nyForVeileder = false
        )

        val bruker6b = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(11, "7"),
                BarnUnder18AarData(4, "6"),
                BarnUnder18AarData(14, "6")
            ),
            nyForVeileder = false
        )

        val brukerTomListe = opprettBruker(emptyList(), nyForVeileder = false)

        val brukerIngenListe = opprettBruker(null, nyForVeileder = false)

        val brukerMedBarnMedKode19 = opprettBruker(
            listOf(
                BarnUnder18AarData(5, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(11, "19"),
                BarnUnder18AarData(4, "19"),
                BarnUnder18AarData(2, "19")
            ),
            nyForVeileder = false
        )

        val liste = listOf(
            bruker1B,
            bruker2barn6U,
            bruker2barnU,
            bruker4barn,
            bruker5barn,
            bruker6b,
            brukerTomListe,
            brukerIngenListe,
            brukerMedBarnMedKode19
        )


        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(6)
        assertThat(response.brukere[0].fnr).isEqualTo(bruker2barnU.fnr)
        assertThat(response.brukere[1].fnr).isEqualTo(bruker4barn.fnr)
        assertThat(response.brukere[2].fnr).isEqualTo(bruker6b.fnr)
        assertThat(response.brukere[3].fnr).isEqualTo(bruker2barn6U.fnr)
        assertThat(response.brukere[4].fnr).isEqualTo(bruker5barn.fnr)
        assertThat(response.brukere[5].fnr).isEqualTo(brukerMedBarnMedKode19.fnr)
    }

    @Test
    fun test_filtrering_barn_under_18() {
        val bruker1 = opprettBruker(listOf(BarnUnder18AarData(8, null)))

        val bruker2 = opprettBruker(
            listOf(BarnUnder18AarData(1, "6"), BarnUnder18AarData(12, "7")),
            nyForVeileder = false
        )

        val bruker3 = opprettBruker(
            listOf(BarnUnder18AarData(5, "7"), BarnUnder18AarData(11, null)),
            nyForVeileder = false
        )

        val bruker4 = opprettBruker(emptyList(), nyForVeileder = false)

        val bruker5 = opprettBruker(null, nyForVeileder = false)

        val bruker6 = opprettBruker(
            listOf(BarnUnder18AarData(5, "19"), BarnUnder18AarData(11, null)),
            nyForVeileder = false
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(listOf(bruker1.fnr, bruker3.fnr, bruker6.fnr))
        )
        response.brukere.forEach { bruker ->
            when (bruker.fnr) {
                bruker1.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
                bruker2.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker3.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker4.fnr -> assertThat(bruker.barnUnder18AarData.size).isZero()
                bruker5.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
            }
        }
    }

    @Test
    fun test_filtrering_barn_under_18_ingen_tilganger() {
        val bruker1BU = brukerMed1BarnUtenDiskresjonskode()
        val bruker2B67 = brukerMed2Barn6og7()
        val bruker3B67U = brukerMed3Barn67ogIngen()
        val bruker2B7U = brukerMed2Barn7ogIngen()
        val bruker2BU7 = brukerMed2BarnIngenog7()
        val bruker2B7 = brukerMed2BarnMedKode7()
        val bruker2B6 = brukerMed2BarnKode6()
        val brukerTomListe = brukerMedTomBarnliste()
        val brukerIngenListe = brukerUtenBarnliste()
        val brukerMedKode19 = brukerMedBarnKode19()

        val liste = listOf(
            bruker1BU,
            bruker2B67,
            bruker3B67U,
            bruker2B7U,
            bruker2BU7,
            bruker2B7,
            bruker2B6,
            brukerTomListe,
            brukerIngenListe,
            brukerMedKode19
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(listOf(bruker1BU.fnr, bruker3B67U.fnr, bruker2B7U.fnr, bruker2BU7.fnr))
        )

        response.brukere.forEach { bruker ->
            when (bruker.fnr) {
                bruker1BU.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
                bruker3B67U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(3)
                bruker2B7U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2BU7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun test_filtrering_barn_under_18_tilgang_6() {
        val bruker1BU = brukerMed1BarnUtenDiskresjonskode()
        val bruker2B67 = brukerMed2Barn6og7()
        val bruker3B67U = brukerMed3Barn67ogIngen()
        val bruker2B7U = brukerMed2Barn7ogIngen()
        val bruker2BU7 = brukerMed2BarnIngenog7()
        val bruker2B7 = brukerMed2BarnMedKode7()
        val bruker2B6 = brukerMed2BarnKode6()
        val brukerTomListe = brukerMedTomBarnliste()
        val brukerIngenListe = brukerUtenBarnliste()
        val brukerMedKode19 = brukerMedBarnKode19()

        val liste = listOf(
            bruker1BU,
            bruker2B67,
            bruker3B67U,
            bruker2B7U,
            bruker2BU7,
            bruker2B7,
            bruker2B6,
            brukerTomListe,
            brukerIngenListe,
            brukerMedKode19
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(7)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(
                    listOf(
                        bruker1BU.fnr,
                        bruker2B67.fnr,
                        bruker3B67U.fnr,
                        bruker2B7U.fnr,
                        bruker2BU7.fnr,
                        bruker2B6.fnr,
                        brukerMedKode19.fnr
                    )
                )
        )
        response.brukere.forEach { bruker ->
            when (bruker.fnr) {
                bruker1BU.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
                bruker2B67.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker3B67U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(3)
                bruker2B7U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2BU7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2B6.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2B7.fnr, brukerTomListe.fnr, brukerIngenListe.fnr ->
                    assertThat(bruker.barnUnder18AarData).isEqualTo(null)

                brukerMedKode19.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun test_filtrering_barn_under_18_tilgang_7() {
        val bruker1BU = brukerMed1BarnUtenDiskresjonskode()
        val bruker2B67 = brukerMed2Barn6og7()
        val bruker3B67U = brukerMed3Barn67ogIngen()
        val bruker2B7U = brukerMed2Barn7ogIngen()
        val bruker2BU7 = brukerMed2BarnIngenog7()
        val bruker2B7 = brukerMed2BarnMedKode7()
        val bruker2B6 = brukerMed2BarnKode6()
        val brukerTomListe = brukerMedTomBarnliste()
        val brukerIngenListe = brukerUtenBarnliste()
        val brukerMedKode19 = brukerMedBarnKode19()

        val liste = listOf(
            bruker1BU,
            bruker2B67,
            bruker3B67U,
            bruker2B7U,
            bruker2BU7,
            bruker2B7,
            bruker2B6,
            brukerTomListe,
            brukerIngenListe,
            brukerMedKode19
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(6)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(
                    listOf(
                        bruker1BU.fnr,
                        bruker2B67.fnr,
                        bruker3B67U.fnr,
                        bruker2B7U.fnr,
                        bruker2BU7.fnr,
                        bruker2B7.fnr
                    )
                )
        )

        response.brukere.forEach { bruker ->
            when (bruker.fnr) {
                bruker1BU.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
                bruker2B67.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker3B67U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(3)
                bruker2B7U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2BU7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2B6.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(null)
                bruker2B7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                brukerTomListe.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(null)
                brukerIngenListe.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(null)
            }
        }
    }

    @Test
    fun test_filtrering_barn_under_18_tilgang_6_7() {
        val bruker1BU = brukerMed1BarnUtenDiskresjonskode()
        val bruker2B67 = brukerMed2Barn6og7()
        val bruker3B67U = brukerMed3Barn67ogIngen()
        val bruker2B7U = brukerMed2Barn7ogIngen()
        val bruker2BU7 = brukerMed2BarnIngenog7()
        val bruker2B7 = brukerMed2BarnMedKode7()
        val bruker2B6 = brukerMed2BarnKode6()
        val brukerTomListe = brukerMedTomBarnliste()
        val brukerIngenListe = brukerUtenBarnliste()
        val brukerMedKode19 = brukerMedBarnKode19()

        val liste = listOf(
            bruker1BU,
            bruker2B67,
            bruker3B67U,
            bruker2B7U,
            bruker2BU7,
            bruker2B7,
            bruker2B6,
            brukerTomListe,
            brukerIngenListe,
            brukerMedKode19
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18Aar(listOf(BarnUnder18Aar.HAR_BARN_UNDER_18_AAR))

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Permit)
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(8)
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(
                    listOf(
                        bruker1BU.fnr,
                        bruker2B67.fnr,
                        bruker3B67U.fnr,
                        bruker2B7U.fnr,
                        bruker2BU7.fnr,
                        bruker2B6.fnr,
                        bruker2B7.fnr
                    )
                )
        )

        response.brukere.forEach { bruker ->
            when (bruker.fnr) {
                bruker1BU.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(1)
                bruker2B67.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker3B67U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(3)
                bruker2B7U.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2BU7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2B6.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                bruker2B7.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
                brukerTomListe.fnr -> assertThat(bruker.barnUnder18AarData).isEqualTo(null)
                brukerIngenListe.fnr -> assertThat(bruker.barnUnder18AarData).isEqualTo(null)
                brukerMedKode19.fnr -> assertThat(bruker.barnUnder18AarData.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun test_filtrering_barn_under_18_med_alder_filter_ikke_tilgang_6_eller_7() {
        val bruker1 = opprettBruker(listOf(BarnUnder18AarData(8, null)))

        val bruker2 = opprettBruker(
            listOf(BarnUnder18AarData(2, null), BarnUnder18AarData(12, null)),
            nyForVeileder = false
        )

        val bruker3 = opprettBruker(
            listOf(BarnUnder18AarData(5, "7"), BarnUnder18AarData(11, null)),
            nyForVeileder = false
        )

        val bruker4 = opprettBruker(
            listOf(
                BarnUnder18AarData(16, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(3, null)
            ),
            nyForVeileder = false
        )

        val bruker5 = opprettBruker(null, nyForVeileder = false)

        val bruker6 = opprettBruker(
            listOf(BarnUnder18AarData(16, "19"), BarnUnder18AarData(12, "19")),
            nyForVeileder = false
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Deny("", ""))
        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Deny("", ""))

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18AarAlder(listOf("1-5"))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(2)
        assertThat(response.brukere[0].fnr).isEqualTo(bruker2.fnr)
        assertThat(response.brukere[1].fnr).isEqualTo(bruker4.fnr)
    }

    @Test
    fun test_filtrering_barn_under_18_med_alder_filter_har_tilgang_7() {
        val bruker1 = opprettBruker(listOf(BarnUnder18AarData(8, null)))

        val bruker2 = opprettBruker(
            listOf(BarnUnder18AarData(2, null), BarnUnder18AarData(12, null)),
            nyForVeileder = false
        )

        val bruker3 = opprettBruker(
            listOf(BarnUnder18AarData(5, "7"), BarnUnder18AarData(2, "7"), BarnUnder18AarData(11, null)),
            nyForVeileder = false
        )

        val bruker4 = opprettBruker(
            listOf(
                BarnUnder18AarData(16, null),
                BarnUnder18AarData(12, null),
                BarnUnder18AarData(1, "7"),
                BarnUnder18AarData(1, null),
                BarnUnder18AarData(3, null)
            ),
            nyForVeileder = false
        )

        val bruker5 = opprettBruker(null, nyForVeileder = false)

        val bruker6 = opprettBruker(
            listOf(BarnUnder18AarData(16, "19"), BarnUnder18AarData(12, "19")),
            nyForVeileder = false
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        `when`(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Permit)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setBarnUnder18AarAlder(listOf("1-5"))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.BARN_UNDER_18_AR,
            filterValg,
            null,
            null
        )

        assertThat(response.antall).isEqualTo(3)
        assertThat(response.brukere[0].fnr).isEqualTo(bruker2.fnr)
        assertThat(response.brukere[1].fnr).isEqualTo(bruker3.fnr)
        assertThat(response.brukere[2].fnr).isEqualTo(bruker4.fnr)
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }


    fun brukerMed1BarnUtenDiskresjonskode(): PortefoljebrukerOpensearchModell {
        return PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(randomVeilederId().toString())
            .setEnhet_id(TEST_ENHET)
            .setBarn_under_18_aar(listOf(BarnUnder18AarData(8, null)))
    }

    fun brukerMed2Barn6og7(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(1, "6"), BarnUnder18AarData(12, "7")),
            nyForVeileder = false
        )
    }

    fun brukerMed3Barn67ogIngen(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(
                BarnUnder18AarData(1, "7"),
                BarnUnder18AarData(12, "6"),
                BarnUnder18AarData(12, null)
            ),
            nyForVeileder = false
        )
    }

    fun brukerMed2Barn7ogIngen(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(5, "7"), BarnUnder18AarData(11, null)),
            nyForVeileder = false
        )
    }

    fun brukerMed2BarnIngenog7(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(5, null), BarnUnder18AarData(11, "7")),
            nyForVeileder = false
        )
    }

    fun brukerMed2BarnMedKode7(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(5, "7"), BarnUnder18AarData(11, "7")),
            nyForVeileder = false
        )
    }

    fun brukerMed2BarnKode6(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(5, "6"), BarnUnder18AarData(11, "6")),
            nyForVeileder = false
        )
    }

    fun brukerMedBarnKode19(): PortefoljebrukerOpensearchModell {
        return opprettBruker(
            listOf(BarnUnder18AarData(5, "19"), BarnUnder18AarData(3, "19")),
            nyForVeileder = false
        )
    }

    fun brukerMedTomBarnliste(): PortefoljebrukerOpensearchModell {
        return opprettBruker(emptyList(), nyForVeileder = false)
    }

    fun brukerUtenBarnliste(): PortefoljebrukerOpensearchModell {
        return opprettBruker(null, nyForVeileder = false)
    }

    fun opprettBruker(
        barn: List<BarnUnder18AarData>? = null,
        nyForVeileder: Boolean = false
    ): PortefoljebrukerOpensearchModell {
        return PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setNy_for_veileder(nyForVeileder)
            .setBarn_under_18_aar(barn)
    }
}
