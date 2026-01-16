package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
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
        val trengerTalespraktolk = genererRandomBruker()
        trengerTalespraktolk.talespraaktolk = tolkesprakJapansk
        trengerTalespraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTalespraktolkSistOppdatert)

        val trengerTaleOgTegnspraktolkSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker()
        trengerTaleOgTegnspraktolk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTaleOgTegnspraktolkSistOppdatert)

        val brukerMedAndreTolkebehov = genererRandomBruker()
        brukerMedAndreTolkebehov.tegnspraaktolk = tolkesprakNorsk

        val brukerUtenTolkebehov = genererRandomBruker()
        brukerUtenTolkebehov.talespraaktolk = null
        brukerUtenTolkebehov.tegnspraaktolk = null

        val brukere = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            brukerMedAndreTolkebehov,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTalespraktolk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TALESPRAAKTOLK")
        )

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
        val trengerTegnspraktolk = genererRandomBruker()
        trengerTegnspraktolk.tegnspraaktolk = tolkesprakNorsk
        trengerTegnspraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert)

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker()
        trengerTaleOgTegnspraktolk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert)

        val brukerUtenTegnspraktolk = genererRandomBruker()
        brukerUtenTegnspraktolk.talespraaktolk = tolkesprakJapansk

        val brukerUtenTolkebehov = genererRandomBruker()
        brukerUtenTolkebehov.talespraaktolk = null
        brukerUtenTolkebehov.tegnspraaktolk = null

        val brukere = listOf(
            trengerTegnspraktolk,
            trengerTaleOgTegnspraktolk,
            brukerUtenTegnspraktolk,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTegnspraktolk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK")
        )

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
        val trengerTalespraktolk = genererRandomBruker()
        trengerTalespraktolk.talespraaktolk = tolkesprakJapansk
        trengerTalespraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTalespraktolkBehovSistOppdatert)

        val trengerTaleOgTegnspraktolkBehovSistOppdatert = "2021-03-23"
        val trengerTaleOgTegnspraktolk = genererRandomBruker()
        trengerTaleOgTegnspraktolk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTaleOgTegnspraktolkBehovSistOppdatert)

        val trengerTegnspraktolkBehovSistOppdatert = "2023-03-24"
        val trengerTegnspraktolk = genererRandomBruker()
        trengerTegnspraktolk.tegnspraaktolk = tolkesprakNorsk
        trengerTegnspraktolk.tolkBehovSistOppdatert = LocalDate.parse(trengerTegnspraktolkBehovSistOppdatert)

        val brukerUtenTolkebehov = genererRandomBruker()
        brukerUtenTolkebehov.talespraaktolk = null
        brukerUtenTolkebehov.tegnspraaktolk = null

        val brukere = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            trengerTegnspraktolk,
            brukerUtenTolkebehov,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrePaBadeTegnOgTalespraktolk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK")
        )

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
        val trengerTalespraktolkJapansk = genererRandomBruker()
        trengerTalespraktolkJapansk.talespraaktolk = tolkesprakJapansk
        trengerTalespraktolkJapansk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTalespraktolkJapanskBehovSistOppdatert)

        val trengerTegnspraktolkJapanskBehovSistOppdatert = "2023-02-22"
        val trengerTegnspraktolkJapansk = genererRandomBruker()
        trengerTegnspraktolkJapansk.talespraaktolk = tolkesprakSvensk
        trengerTegnspraktolkJapansk.tegnspraaktolk = tolkesprakJapansk
        trengerTegnspraktolkJapansk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTegnspraktolkJapanskBehovSistOppdatert)

        val trengerTaleOgTegnspraktolkMenIkkeJapansk = genererRandomBruker()
        trengerTaleOgTegnspraktolkMenIkkeJapansk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolkMenIkkeJapansk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolkMenIkkeJapansk.tolkBehovSistOppdatert = LocalDate.parse("2021-03-23")

        val trengerTegnspraktolkMenIkkeJapansk = genererRandomBruker()
        trengerTegnspraktolkMenIkkeJapansk.tegnspraaktolk = tolkesprakNorsk
        trengerTegnspraktolkMenIkkeJapansk.tolkBehovSistOppdatert = LocalDate.parse("2023-03-24")

        val brukerUtenTolkebehov = genererRandomBruker()
        brukerUtenTolkebehov.talespraaktolk = null
        brukerUtenTolkebehov.tegnspraaktolk = null

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
        val filtrerPaTolkebehovJapansk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK"),
            tolkBehovSpraak = listOf(tolkesprakJapansk)
        )

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
        val trengerTalespraktolkJapansk = genererRandomBruker()
        trengerTalespraktolkJapansk.talespraaktolk = tolkesprakJapansk
        trengerTalespraktolkJapansk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTalespraktolkJapanskBehovSistOppdatert)

        val trengerTegnspraktolkJapanskBehovSistOppdatert = "2023-02-22"
        val trengerTegnspraktolkJapansk = genererRandomBruker()
        trengerTegnspraktolkJapansk.talespraaktolk = tolkesprakSvensk
        trengerTegnspraktolkJapansk.tegnspraaktolk = tolkesprakJapansk
        trengerTegnspraktolkJapansk.tolkBehovSistOppdatert =
            LocalDate.parse(trengerTegnspraktolkJapanskBehovSistOppdatert)

        val trengerTaleOgTegnspraktolkMenIkkeJapansk = genererRandomBruker()
        trengerTaleOgTegnspraktolkMenIkkeJapansk.talespraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolkMenIkkeJapansk.tegnspraaktolk = tolkesprakSvensk
        trengerTaleOgTegnspraktolkMenIkkeJapansk.tolkBehovSistOppdatert = LocalDate.parse("2021-03-23")

        val trengerTegnspraktolkMenIkkeJapansk = genererRandomBruker()
        trengerTegnspraktolkMenIkkeJapansk.tegnspraaktolk = tolkesprakNorsk
        trengerTegnspraktolkMenIkkeJapansk.tolkBehovSistOppdatert = LocalDate.parse("2023-03-24")

        val brukerUtenTolkebehov = genererRandomBruker()
        brukerUtenTolkebehov.talespraaktolk = null
        brukerUtenTolkebehov.tegnspraaktolk = null

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
        val filtrerPaKunTolkesprakJapansk = getFiltervalgDefaults().copy(
            tolkebehov = listOf(),
            tolkBehovSpraak = listOf(tolkesprakJapansk)
        )

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

    @Test
    fun `Skal sortere på tolkespråk når det er filtrert på talespråktolk`() {
        // Given
        val bruker1Aktorid = AktorId.of("2222222222222")
        val bruker1 = genererRandomBruker(aktorId = bruker1Aktorid)
        bruker1.talespraaktolk = "AR"

        val bruker2Aktorid = AktorId.of("1111111111111")
        val bruker2 = genererRandomBruker(aktorId = bruker2Aktorid)
        bruker2.talespraaktolk = "FR"

        val bruker3Aktorid = AktorId.of("3333333333333")
        val bruker3 = genererRandomBruker(aktorId = bruker3Aktorid)
        bruker3.talespraaktolk = "NO"

        val brukere = listOf(
            bruker1,
            bruker2,
            bruker3
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTalespraktolk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TALESPRAAKTOLK")
        )

        val sorterteBrukere = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TOLKESPRAK,
            filtrerPaTalespraktolk,
            null,
            null
        )

        // Then
        assertThat(sorterteBrukere.antall).isEqualTo(3)
        assertThat(sorterteBrukere.brukere[0].aktoerid).isEqualTo(bruker1Aktorid.toString())
        assertThat(sorterteBrukere.brukere[1].aktoerid).isEqualTo(bruker2Aktorid.toString())
        assertThat(sorterteBrukere.brukere[2].aktoerid).isEqualTo(bruker3Aktorid.toString())
    }


    @Test
    fun `Skal sortere på tolkespråk når det er filtrert på tegnspråktolk`() {
        // Given
        val bruker1Aktorid = AktorId.of("2222222222222")
        val bruker1 = genererRandomBruker(aktorId = bruker1Aktorid)
        bruker1.tegnspraaktolk = "AR"

        val bruker2Aktorid = AktorId.of("1111111111111")
        val bruker2 = genererRandomBruker(aktorId = bruker2Aktorid)
        bruker2.tegnspraaktolk = "FR"

        val bruker3Aktorid = AktorId.of("3333333333333")
        val bruker3 = genererRandomBruker(aktorId = bruker3Aktorid)
        bruker3.tegnspraaktolk = "NO"

        val brukere = listOf(
            bruker1,
            bruker2,
            bruker3
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTegnspraktolk = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK")
        )

        val sorterteBrukere = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TOLKESPRAK,
            filtrerPaTegnspraktolk,
            null,
            null
        )

        // Then
        assertThat(sorterteBrukere.antall).isEqualTo(3)
        assertThat(sorterteBrukere.brukere[0].aktoerid).isEqualTo(bruker1Aktorid.toString())
        assertThat(sorterteBrukere.brukere[1].aktoerid).isEqualTo(bruker2Aktorid.toString())
        assertThat(sorterteBrukere.brukere[2].aktoerid).isEqualTo(bruker3Aktorid.toString())
    }

    @Test
    fun `Skal sortere på talespråk-språket når det er filtrert på både tale- og tegnspråktolk`() {
        // Given
        val bruker1Aktorid = AktorId.of("2222222222222")
        val bruker1 = genererRandomBruker(aktorId = bruker1Aktorid)
        bruker1.talespraaktolk = "AR"
        bruker1.tegnspraaktolk = "NO"

        val bruker2Aktorid = AktorId.of("1111111111111")
        val bruker2 = genererRandomBruker(aktorId = bruker2Aktorid)
        bruker2.talespraaktolk = "FR"
        bruker2.tegnspraaktolk = "SWE"

        val bruker3Aktorid = AktorId.of("3333333333333")
        val bruker3 = genererRandomBruker(aktorId = bruker3Aktorid)
        bruker3.talespraaktolk = "NO"
        bruker3.tegnspraaktolk = "AR"

        val brukere = listOf(
            bruker1,
            bruker2,
            bruker3
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTolkebehov = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TALESPRAAKTOLK", "TEGNSPRAAKTOLK")
        )

        val sorterteBrukere = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TOLKESPRAK,
            filtrerPaTolkebehov,
            null,
            null
        )

        // Then
        assertThat(sorterteBrukere.antall).isEqualTo(3)
        assertThat(sorterteBrukere.brukere[0].aktoerid).isEqualTo(bruker1Aktorid.toString())
        assertThat(sorterteBrukere.brukere[1].aktoerid).isEqualTo(bruker2Aktorid.toString())
        assertThat(sorterteBrukere.brukere[2].aktoerid).isEqualTo(bruker3Aktorid.toString())
    }

    @Test
    fun `Tolkespråkfiltrering skal fungere på ulike nullverdier`() {
        // Given
        val brukerMedNullverdier = genererRandomBruker()
        brukerMedNullverdier.talespraaktolk = null
        brukerMedNullverdier.tegnspraaktolk = null

        val brukerMedTommeStrenger = genererRandomBruker()
        brukerMedTommeStrenger.talespraaktolk = ""
        brukerMedTommeStrenger.tegnspraaktolk = ""


        val brukere = listOf(
            brukerMedNullverdier,
            brukerMedTommeStrenger,
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        // When
        val filtrerPaTolkebehov = getFiltervalgDefaults().copy(
            tolkebehov = listOf("TEGNSPRAAKTOLK", "TALESPRAAKTOLK")
        )

        val brukereMedTolkebehov = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TOLKESPRAK,
            filtrerPaTolkebehov,
            null,
            null
        )

        // Then
        assertThat(brukereMedTolkebehov.antall).isEqualTo(0)
    }

    private fun genererRandomBruker(
        enhet: String = TESTENHET, veilederId: String? = TESTVEILEDER, aktorId: AktorId? = randomAktorId()
    ): PortefoljebrukerOpensearchModell {
        val bruker =
            PortefoljebrukerOpensearchModell(
                aktoer_id = aktorId.toString(),
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
