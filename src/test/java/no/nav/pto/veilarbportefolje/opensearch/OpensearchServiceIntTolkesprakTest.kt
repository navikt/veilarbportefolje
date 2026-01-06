package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OpensearchServiceIntTolkesprakTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest() {

    private lateinit var TESTENHET: String
    private lateinit var TESTVEILEDER: String

    private val tolkesprakJapansk = "JPN"
    private val tolkesprakSvensk = "SWE"
    private val tolkesprakNorsk = "NB"

    @BeforeEach
    fun setup() {
        TESTENHET = randomNavKontor().value
        TESTVEILEDER = randomVeilederId().value
    }

    @Test
    fun `Skal hente alle brukere med talespråktolk`() {
        // Given
        val trengerTalespraktolkSistOppdatert = "2022-02-22"
        val trengerTalespraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkSistOppdatert))

        val trengerTaleOgTegnspraktolkSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTaleOgTegnspraktolkSistOppdatert))

        val brukerMedAndreTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTegnspraaktolk(tolkesprakNorsk)

        val brukerUtenTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukere = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            brukerMedAndreTolkebehov,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtrerPaTalespraktolk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TALESPRAAKTOLK"))

        val response = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrerPaTalespraktolk,
            null,
            null
        )

        // Then
        assertThat(response.antall).isEqualTo(2)
        assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkSistOppdatert })
        assertTrue(
            response.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkSistOppdatert })
    }

    @Test
    fun `Skal hente alle brukere med tegnspråktolk`() {
        // Given
        val trengerTegnspraktolkBehovSistOppdatert = "2023-03-24"
        val trengerTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTegnspraaktolk(tolkesprakNorsk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert))

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert))

        val brukerUtenTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakJapansk)

        val brukerUtenTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukere = listOf(
            trengerTegnspraktolk,
            trengerTaleOgTegnspraktolk,
            brukerUtenTegnspraktolk,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtrerPaTegnspraktolk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK"))

        val responsBrukereMedTegnspraktolk = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrerPaTegnspraktolk,
            null,
            null
        )

        // Then
        assertThat(responsBrukereMedTegnspraktolk.antall).isEqualTo(2)
        assertTrue(
            responsBrukereMedTegnspraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTegnspraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakNorsk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })
    }

    @Test
    fun `Skal hente alle brukere med tale- eller tegnspråktolk`() {
        // Given
        val trengerTalespraktolkBehovSistOppdatert = "2022-02-22"
        val trengerTalespraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkBehovSistOppdatert))

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert))

        val trengerTegnspraktolkBehovSistOppdatert = "2023-03-24"
        val trengerTegnspraktolk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTegnspraaktolk(tolkesprakNorsk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert))

        val brukerUtenTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukere = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            trengerTegnspraktolk,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtrePaBadeTegnOgTalespraktolk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))

        val responsBrukereMedTegnEllerTalespraktolk = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrePaBadeTegnOgTalespraktolk,
            null,
            null
        )

        // Then
        assertThat(responsBrukereMedTegnEllerTalespraktolk.antall).isEqualTo(3)
        assertTrue(
            responsBrukereMedTegnEllerTalespraktolk.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTegnEllerTalespraktolk.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTegnEllerTalespraktolk.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })

    }

    @Test
    fun `Skal hente alle brukere med tegn- eller talespråktolkebehov for japansk`() {
        // Given
        val trengerTalespraktolkJapanskBehovSistOppdatert = "2022-02-22"
        val trengerTalespraktolkJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkJapanskBehovSistOppdatert))

        val trengerTegnspraktolkJapanskBehovSistOppdatert = "2023-02-22"
        val trengerTegnspraktolkJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkJapanskBehovSistOppdatert))

        val trengerTaleOgTegnspraktolkMenIkkeJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse("2021-03-23"))

        val trengerTegnspraktolkMenIkkeJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTegnspraaktolk(tolkesprakNorsk)
            .setTolkBehovSistOppdatert(LocalDate.parse("2023-03-24"))

        val brukerUtenTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukere = listOf(
            trengerTalespraktolkJapansk,
            trengerTegnspraktolkJapansk,
            trengerTaleOgTegnspraktolkMenIkkeJapansk,
            trengerTegnspraktolkMenIkkeJapansk,
            brukerUtenTolkebehov,
        )


        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtrerPaTolkebehovJapansk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"))
            .setTolkBehovSpraak(listOf(tolkesprakJapansk))

        val responsBrukereMedTolkebehovJapansk = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrerPaTolkebehovJapansk,
            null,
            null
        )

        // Then
        assertThat(responsBrukereMedTolkebehovJapansk.antall).isEqualTo(2)
        assertTrue(
            responsBrukereMedTolkebehovJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkJapanskBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTolkebehovJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkJapanskBehovSistOppdatert })
    }


    @Test
    fun `Skal hente alle brukere med tolkespråk japansk når det bare er filtret på tolkespråk`() {
        // Given
        val trengerTalespraktolkJapanskBehovSistOppdatert = "2022-02-22"
        val trengerTalespraktolkJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTalespraktolkJapanskBehovSistOppdatert))

        val trengerTegnspraktolkJapanskBehovSistOppdatert = "2023-02-22"
        val trengerTegnspraktolkJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakJapansk)
            .setTolkBehovSistOppdatert(LocalDate.parse(trengerTegnspraktolkJapanskBehovSistOppdatert))

        val trengerTaleOgTegnspraktolkMenIkkeJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(tolkesprakSvensk)
            .setTegnspraaktolk(tolkesprakSvensk)
            .setTolkBehovSistOppdatert(LocalDate.parse("2021-03-23"))

        val trengerTegnspraktolkMenIkkeJapansk = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTegnspraaktolk(tolkesprakNorsk)
            .setTolkBehovSistOppdatert(LocalDate.parse("2023-03-24"))

        val brukerUtenTolkebehov = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukere = listOf(
            trengerTalespraktolkJapansk,
            trengerTegnspraktolkJapansk,
            trengerTaleOgTegnspraktolkMenIkkeJapansk,
            trengerTegnspraktolkMenIkkeJapansk,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtrerPaKunTolkesprakJapansk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf())
            .setTolkBehovSpraak(listOf(tolkesprakJapansk))

        val responsBrukereMedTolkesprakJapansk = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrerPaKunTolkesprakJapansk,
            null,
            null
        )

        // Then
        assertThat(responsBrukereMedTolkesprakJapansk.antall).isEqualTo(2)
        assertTrue(
            responsBrukereMedTolkesprakJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkJapanskBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTolkesprakJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkJapanskBehovSistOppdatert })
    }

    private fun genererRandomBruker(
        enhet: String, veilederId: String?
    ): PortefoljebrukerOpensearchModell {
        val bruker =
            PortefoljebrukerOpensearchModell().setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().get())
                .setOppfolging(true)
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
