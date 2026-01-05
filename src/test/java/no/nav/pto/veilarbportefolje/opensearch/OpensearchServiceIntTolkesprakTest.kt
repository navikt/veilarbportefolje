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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OpensearchServiceIntTolkesprakTest @Autowired constructor(
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
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })


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
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakSvensk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.tegnspraaktolk == tolkesprakNorsk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


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
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTaleOgTegnspraktolkBehovSistOppdatert })
        org.junit.jupiter.api.Assertions.assertTrue(
            response.brukere.stream()
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTegnspraktolkBehovSistOppdatert })


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
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })


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
            response.brukere.stream().filter { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.talespraaktolk == tolkesprakJapansk }
                .anyMatch { x: PortefoljebrukerFrontendModell -> x.tolkebehov?.sistOppdatert.toString() == trengerTalespraktolkBehovSistOppdatert })
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
