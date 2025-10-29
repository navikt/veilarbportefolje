package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.hendelsesfilter.genererRandomHendelse
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype
import no.nav.pto.veilarbportefolje.util.BrukerComparator
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class OpensearchServiceIntFargekatHendelserHuskelappTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest(){

    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value
    }

    @Test
    fun test_sortering_huskelapp() {
        val huskelapp1 = HuskelappForBruker(
            LocalDate.now().plusDays(20),
            "dddd Ringe fastlege",
            LocalDate.now().minusDays(10),
            TEST_VEILEDER_0,
            UUID.randomUUID().toString(),
            TEST_ENHET
        )
        val huskelapp2 = HuskelappForBruker(
            LocalDate.now().plusDays(30),
            "bbbb Ha et møte",
            LocalDate.now().minusDays(12),
            TEST_VEILEDER_0,
            UUID.randomUUID().toString(),
            TEST_ENHET
        )
        val huskelapp3 = HuskelappForBruker(
            LocalDate.now().plusMonths(2),
            "aaaa Snakke om idrett",
            LocalDate.now().minusDays(8),
            TEST_VEILEDER_0,
            UUID.randomUUID().toString(),
            TEST_ENHET
        )
        val huskelapp4 = HuskelappForBruker(
            LocalDate.now().plusDays(3),
            "cccc Huddle med Julie",
            LocalDate.now().minusDays(14),
            TEST_VEILEDER_0,
            UUID.randomUUID().toString(),
            TEST_ENHET
        )

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setHuskelapp(huskelapp1)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setHuskelapp(huskelapp2)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setHuskelapp(huskelapp3)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setHuskelapp(huskelapp4)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val bruker6 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())

        var response: BrukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.HUSKELAPP_KOMMENTAR,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        Assertions.assertThat(response.brukere[0].fnr).isEqualTo(bruker3.fnr)
        Assertions.assertThat(response.brukere[1].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(response.brukere[3].fnr).isEqualTo(bruker1.fnr)


        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.HUSKELAPP_FRIST,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        Assertions.assertThat(response.brukere[0].fnr).isEqualTo(bruker3.fnr)
        Assertions.assertThat(response.brukere[1].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker1.fnr)
        Assertions.assertThat(response.brukere[3].fnr).isEqualTo(bruker4.fnr)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.HUSKELAPP_FRIST,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        Assertions.assertThat(response.brukere[0].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(response.brukere[1].fnr).isEqualTo(bruker1.fnr)
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(response.brukere[3].fnr).isEqualTo(bruker3.fnr)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.HUSKELAPP_SIST_ENDRET,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(6)
        Assertions.assertThat(response.brukere[0].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(response.brukere[1].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker1.fnr)
        Assertions.assertThat(response.brukere[3].fnr).isEqualTo(bruker3.fnr)
    }

    @Test
    fun test_filtering_og_sortering_fargekategori() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_D.name)
            .setFargekategori_enhetId(TEST_ENHET)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name)
            .setFargekategori_enhetId(TEST_ENHET)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name)
            .setFargekategori_enhetId(TEST_ENHET)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_B.name)
            .setFargekategori_enhetId(TEST_ENHET)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val bruker6 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        var filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setFargekategorier(
                listOf(
                    FargekategoriVerdi.FARGEKATEGORI_B.name,
                    FargekategoriVerdi.FARGEKATEGORI_A.name
                )
            )

        var response: BrukereMedAntall = opensearchService.hentBrukere(
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
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(
                    listOf(bruker2.fnr, bruker3.fnr, bruker4.fnr)
                )
        )

        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setFargekategorier(listOf("INGEN_KATEGORI"))

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
            response.brukere.stream().map { obj: PortefoljebrukerFrontendModell -> obj.fnr }.toList()
                .containsAll(
                    listOf(bruker5.fnr, bruker6.fnr)
                )
        )


        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.FARGEKATEGORI,
            filterValg,
            null,
            null
        )

        val equealSortOrder = listOf(bruker2.fnr, bruker3.fnr)

        Assertions.assertThat(response.antall).isEqualTo(6)
        Assertions.assertThat(equealSortOrder.contains(response.brukere[0].fnr))
        Assertions.assertThat(equealSortOrder.contains(response.brukere[1].fnr))
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(response.brukere[3].fnr).isEqualTo(bruker1.fnr)
    }

    @Test
    fun test_filtrering_og_statustall_tiltakshendelser() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(null)


        val bruker2Fnr = Fnr.of("02020222222")
        val bruker2UUID = UUID.randomUUID()
        val bruker2Opprettet = LocalDateTime.now()
        val bruker2Tekst = "Forslag: Endre alt"
        val bruker2Lenke = "http.cat/200"
        val bruker2Tiltakstype = Tiltakstype.ARBFORB

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(bruker2Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(
                Tiltakshendelse(
                    bruker2UUID,
                    bruker2Opprettet,
                    bruker2Tekst,
                    bruker2Lenke,
                    bruker2Tiltakstype,
                    bruker2Fnr
                )
            )


        val bruker3Fnr = Fnr.of("03030333333")
        val bruker3UUID = UUID.randomUUID()
        val bruker3Opprettet = LocalDateTime.now()
        val bruker3Tekst = "Forslag: Endre alt"
        val bruker3Lenke = "http.cat/200"
        val bruker3Tiltakstype = Tiltakstype.ARBFORB

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(bruker3Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(
                Tiltakshendelse(
                    bruker3UUID,
                    bruker3Opprettet,
                    bruker3Tekst,
                    bruker3Lenke,
                    bruker3Tiltakstype,
                    bruker3Fnr
                )
            )


        val brukere = listOf(bruker1, bruker2, bruker3)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.TILTAKSHENDELSER))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        val sorterteBrukere = response.brukere.stream().sorted(BrukerComparator()).toList()

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(sorterteBrukere[0].fnr).isEqualTo(bruker2Fnr.toString())
        Assertions.assertThat(sorterteBrukere[1].fnr).isEqualTo(bruker3Fnr.toString())

        val statustall = opensearchService.hentStatustallForVeilederPortefolje(
            TEST_VEILEDER_0,
            TEST_ENHET
        )
        Assertions.assertThat(statustall.tiltakshendelser).isEqualTo(2)
    }

    @Test
    fun test_sortering_tiltakshendelser() {
        val bruker1Fnr = Fnr.of("01010111111")
        val bruker2Fnr = Fnr.of("02020222222")
        val bruker3Fnr = Fnr.of("03030333333")
        val bruker1Opprettet = LocalDateTime.of(2024, 6, 1, 0, 0)
        val bruker2Opprettet = LocalDateTime.of(2023, 6, 1, 0, 0)
        val bruker3Opprettet = LocalDateTime.of(2022, 6, 1, 0, 0)
        val bruker1tekst = "Dette er noko tekst som startar på D."
        val bruker2Tekst = "Akkurat slik startar du ein setning med bokstaven A."
        val bruker3Tekst = "Byrjinga av denne teksten er bokstaven B."
        val lenke = "http.cat/200"
        val tiltakstype = Tiltakstype.ARBFORB

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(bruker1Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    bruker1Opprettet,
                    bruker1tekst,
                    lenke,
                    tiltakstype,
                    bruker1Fnr
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(bruker2Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    bruker2Opprettet,
                    bruker2Tekst,
                    lenke,
                    tiltakstype,
                    bruker2Fnr
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(bruker3Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    bruker3Opprettet,
                    bruker3Tekst,
                    lenke,
                    tiltakstype,
                    bruker3Fnr
                )
            )


        val brukere = listOf(bruker1, bruker2, bruker3)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.TILTAKSHENDELSER))

        /* Om ein filtrerer på tiltakshendelse og ikkje har valgt sortering: sorter på opprettet-tidspunkt stigande. */
        val responseDefaultSortering = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        val brukereDefaultRekkefolge = responseDefaultSortering.brukere

        Assertions.assertThat(responseDefaultSortering.antall).isEqualTo(3)
        Assertions.assertThat(brukereDefaultRekkefolge[0].fnr).isEqualTo(bruker3Fnr.toString())
        Assertions.assertThat(brukereDefaultRekkefolge[1].fnr).isEqualTo(bruker2Fnr.toString())
        Assertions.assertThat(brukereDefaultRekkefolge[2].fnr).isEqualTo(bruker1Fnr.toString())

        val responseSortertNyesteDato = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.TILTAKSHENDELSE_DATO_OPPRETTET,
            filterValg,
            null,
            null
        )
        val brukereOpprettetSortertPaNyeste = responseSortertNyesteDato.brukere

        Assertions.assertThat(responseSortertNyesteDato.antall).isEqualTo(3)
        Assertions.assertThat(brukereOpprettetSortertPaNyeste[0].fnr).isEqualTo(bruker1Fnr.toString())
        Assertions.assertThat(brukereOpprettetSortertPaNyeste[1].fnr).isEqualTo(bruker2Fnr.toString())
        Assertions.assertThat(brukereOpprettetSortertPaNyeste[2].fnr).isEqualTo(bruker3Fnr.toString())

        val responseSortertAlfabetisk = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TILTAKSHENDELSE_TEKST,
            filterValg,
            null,
            null
        )
        val brukereTekstSortertAlfabetisk = responseSortertAlfabetisk.brukere

        Assertions.assertThat(responseSortertAlfabetisk.antall).isEqualTo(3)
        Assertions.assertThat(brukereTekstSortertAlfabetisk[0].fnr).isEqualTo(bruker2Fnr.toString())
        Assertions.assertThat(brukereTekstSortertAlfabetisk[1].fnr).isEqualTo(bruker3Fnr.toString())
        Assertions.assertThat(brukereTekstSortertAlfabetisk[2].fnr).isEqualTo(bruker1Fnr.toString())
    }

    @Test
    fun test_filtrering_og_statustall_utgatte_varsel() {
        val oppfolgingsBruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(null)


        val oppfolgingsBruker2Fnr = Fnr.of("02020222222")
        val utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL).hendelse

        val oppfolgingsBruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(oppfolgingsBruker2Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker2)


        val oppfolgingsBruker3Fnr = Fnr.of("03030333333")
        val utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL).hendelse

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(oppfolgingsBruker3Fnr.toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker3)


        val brukere = listOf(oppfolgingsBruker1, oppfolgingsBruker2, bruker3)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.UTGATTE_VARSEL))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )
        val sorterteBrukere = response.brukere.stream().sorted(BrukerComparator()).toList()

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(sorterteBrukere[0].fnr).isEqualTo(oppfolgingsBruker2Fnr.toString())
        Assertions.assertThat(sorterteBrukere[1].fnr).isEqualTo(oppfolgingsBruker3Fnr.toString())

        val statustallForVeiledar = opensearchService.hentStatustallForVeilederPortefolje(
            TEST_VEILEDER_0,
            TEST_ENHET
        )
        Assertions.assertThat(statustallForVeiledar.utgatteVarsel).isEqualTo(2)

        val statustallForEnhet = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET,
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
        )
        Assertions.assertThat(statustallForEnhet.utgatteVarsel).isEqualTo(2)
    }

    @Test
    fun skal_sortere_pa_hendelsesdato_som_standard_ved_filtrering_pa_utgatt_varsel() {
        // Given
        val aktoridBruker1 = AktorId.of("1111111111111")
        val aktoridBruker2 = AktorId.of("2222222222222")
        val aktoridBruker3 = AktorId.of("3333333333333")
        val hendelsedatoBruker1 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val hendelsedatoBruker2 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val hendelsedatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val utgattVarselBruker1 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker1).hendelse
        val utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker2).hendelse
        val utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker3).hendelse

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker1.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker1)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker2.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker2)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker3.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker3)

        val brukere = listOf(bruker1, bruker2, bruker3)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtervalg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.UTGATTE_VARSEL))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtervalg,
            null,
            null
        )

        // Then
        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[0].aktoerid, bruker3.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[1].aktoerid, bruker1.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[2].aktoerid, bruker2.aktoer_id)
    }

    @Test
    fun skal_kunne_sortere_pa_hendelsesdato_pa_utgatt_varsel() {
        // Given
        val aktoridBruker1 = AktorId.of("1111111111111")
        val aktoridBruker2 = AktorId.of("2222222222222")
        val aktoridBruker3 = AktorId.of("3333333333333")
        val hendelsedatoBruker1 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val hendelsedatoBruker2 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val hendelsedatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val utgattVarselBruker1 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker1).hendelse
        val utgattVarselBruker2 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker2).hendelse
        val utgattVarselBruker3 = genererRandomHendelse(Kategori.UTGATT_VARSEL, hendelsedatoBruker3).hendelse

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker1.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker1)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker2.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker2)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(aktoridBruker3.toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(utgattVarselBruker3)

        val brukere = listOf(bruker1, bruker2, bruker3)

        skrivBrukereTilTestindeks(brukere)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        // When
        val filtervalg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.UTGATTE_VARSEL))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.UTGATT_VARSEL_DATO,
            filtervalg,
            null,
            null
        )


        // Then
        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(bruker3.aktoer_id, response.brukere[0].aktoerid)
        org.junit.jupiter.api.Assertions.assertEquals(bruker1.aktoer_id, response.brukere[1].aktoerid)
        org.junit.jupiter.api.Assertions.assertEquals(bruker2.aktoer_id, response.brukere[2].aktoerid)
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
