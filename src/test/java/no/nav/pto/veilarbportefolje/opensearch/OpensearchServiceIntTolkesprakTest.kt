package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.domene.*
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

    @BeforeEach
    fun setup() {
        TESTENHET = randomNavKontor().value
        TESTVEILEDER = randomVeilederId().value
    }

    @Test
    fun skal_hente_alle_brukere_som_har_tolkbehov() {
        val tolkesprakJapansk = "JPN"
        val tolkesprakSvensk = "SWE"
        val tolkesprakNorsk = "NB"

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

        val brukerUtenTolkebehov1 = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk(null)
            .setTegnspraaktolk(null)

        val brukerUtenTolkebehov2 = genererRandomBruker(TESTENHET, TESTVEILEDER)
            .setTalespraaktolk("")
            .setTegnspraaktolk("")

        val brukere = listOf(
            trengerTalespraktolk,
            trengerTaleOgTegnspraktolk,
            trengerTegnspraktolk,
            brukerUtenTolkebehov1,
            brukerUtenTolkebehov2
        )

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        /* Skal hente alle med talespråktolk */
        val filtrerPaTalespraktolk = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setTolkebehov(listOf("TALESPRAAKTOLK"))

        val responsBrukereMedTalespraktolk = opensearchService.hentBrukere(
            TESTENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filtrerPaTalespraktolk,
            null,
            null
        )

        assertThat(responsBrukereMedTalespraktolk.antall).isEqualTo(2)
        assertTrue(
            responsBrukereMedTalespraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTalespraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegnspråktolk */
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
        assertThat(responsBrukereMedTegnspraktolk.antall).isEqualTo(2)
        assertTrue(
            responsBrukereMedTegnspraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        assertTrue(
            responsBrukereMedTegnspraktolk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakNorsk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


        /* Skal hente alle med tegn- eller talespråktolk */
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


        /* Skal hente alle med japansk som tegn- eller talespråk */
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
        assertThat(responsBrukereMedTolkebehovJapansk.antall).isEqualTo(1)
        assertTrue(
            responsBrukereMedTolkebehovJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })


        /* Skal hente alle med japansk som tegn- eller talespråk, også når ingen tolkebehov er valgt */
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
        assertThat(responsBrukereMedTolkesprakJapansk.antall).isEqualTo(1)
        assertTrue(
            responsBrukereMedTolkesprakJapansk.brukere.stream()
                .filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
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
