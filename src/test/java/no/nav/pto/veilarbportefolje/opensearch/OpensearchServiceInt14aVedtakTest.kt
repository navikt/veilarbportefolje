package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakForBruker
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class OpensearchServiceInt14aVedtakTest @Autowired constructor(
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
    fun skal_hente_alle_brukere_som_har_vedtak() {
        val brukerMedVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtkast_14a_status("Utkast")
            .setUtkast_14a_ansvarlig_veileder("BVeileder")

        val brukerMedVedtak1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtkast_14a_status("Venter på tilbakemelding")
            .setUtkast_14a_ansvarlig_veileder("CVeileder")

        val brukerMedVedtak2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtkast_14a_status("Venter på tilbakemelding")
            .setUtkast_14a_ansvarlig_veileder("AVeileder")

        val brukerMedVedtakUtenAnsvarligVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setUtkast_14a_status("Utkast")

        val brukerUtenVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("egen"))

        val liste = listOf(
            brukerMedVedtak,
            brukerMedVedtak1,
            brukerMedVedtak2,
            brukerMedVedtakUtenAnsvarligVeileder,
            brukerUtenVedtak
        )

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.UNDER_VURDERING))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.UTKAST_14A_ANSVARLIG_VEILEDER,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(4)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .containsExactlyInAnyOrder(
                brukerMedVedtak.fnr,
                brukerMedVedtak1.fnr,
                brukerMedVedtak2.fnr,
                brukerMedVedtakUtenAnsvarligVeileder.fnr
            )

        Assertions.assertThat(response.brukere[0].utkast14a.ansvarligVeileder).isEqualTo("AVeileder")
        Assertions.assertThat(response.brukere[1].utkast14a.ansvarligVeileder).isEqualTo("BVeileder")
        Assertions.assertThat(response.brukere[2].utkast14a.ansvarligVeileder).isEqualTo("CVeileder")
        Assertions.assertThat(response.brukere[3].utkast14a.ansvarligVeileder).isNull()
    }


    @Test
    fun skal_filtrere_brukere_med_riktige_avvikstyper_naar_filter_for_avvik_er_valgt_hvor_noen_brukere_har_avvik_og_noen_ikke_har_avvik() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_ULIK)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.HOVEDMAAL_ULIK)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setAvvik14aVedtak(
                listOf(
                    Avvik14aVedtak.INNSATSGRUPPE_ULIK,
                    Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK
                )
            )

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.brukere)
            .hasSize(2)
            .extracting<Avvik14aVedtak, RuntimeException> { obj: PortefoljebrukerFrontendModell -> obj.avvik14aVedtak }
            .containsExactlyInAnyOrder(
                Avvik14aVedtak.INNSATSGRUPPE_ULIK,
                Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK
            )
    }

    @Test
    fun skal_filtrere_brukere_med_riktige_avvikstyper_naar_filter_for_avvik_er_valgt_hvor_alle_brukere_ikke_har_avvik() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setAvvik14aVedtak(listOf(Avvik14aVedtak.INGEN_AVVIK))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.brukere)
            .hasSize(5)
            .extracting<Avvik14aVedtak, RuntimeException> { obj: PortefoljebrukerFrontendModell -> obj.avvik14aVedtak }
            .containsOnly(Avvik14aVedtak.INGEN_AVVIK)
    }

    @Test
    fun skal_ikke_filtrere_paa_avvikstype_naar_filter_for_avvik_ikke_er_valgt() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INGEN_AVVIK)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE)

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_ULIK)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.HOVEDMAAL_ULIK)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setAvvik14aVedtak(Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.brukere).hasSize(5)
    }

    @Test
    fun skal_hente_brukere_med_gjeldendeVedtak14a() {
        val brukerMedSiste14aVedtakFnr = randomFnr()
        val brukerUtenSiste14aVedtakFnr = randomFnr()
        val brukerMedSiste14aVedtakAktorId = randomAktorId()
        val brukerUtenSiste14aVedtakAktorId = randomAktorId()
        val innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        val hovedmal = Hovedmal.BEHOLDE_ARBEID
        val fattetDato = ZonedDateTime.now()
        val fraArena = false
        val siste14aVedtakForBruker = Siste14aVedtakForBruker(
            brukerMedSiste14aVedtakAktorId,
            innsatsgruppe,
            hovedmal,
            fattetDato,
            fraArena
        )
        skrivBrukereTilTestindeks(
            listOf(
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerMedSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
                    .setGjeldendeVedtak14a(
                        GjeldendeVedtak14a(
                            siste14aVedtakForBruker.innsatsgruppe,
                            siste14aVedtakForBruker.hovedmal,
                            siste14aVedtakForBruker.fattetDato
                        )
                    ),
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerUtenSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
            )
        )
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 2 }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList()).setGjeldendeVedtak14a(listOf("HAR_14A_VEDTAK")),
            null,
            null
        )
        Assertions.assertThat(respons.antall).isEqualTo(1)
        val brukerFraOpenSearch: PortefoljebrukerFrontendModell = respons.brukere.first()
        Assertions.assertThat(brukerFraOpenSearch.fnr).isEqualTo(brukerMedSiste14aVedtakFnr.get())
        Assertions.assertThat(brukerFraOpenSearch.aktoerid).isEqualTo(brukerMedSiste14aVedtakAktorId.get())
        val brukerFraOpenSearchGjeldendeVedtak14a = brukerFraOpenSearch.gjeldendeVedtak14a
        Assertions.assertThat(brukerFraOpenSearchGjeldendeVedtak14a).isNotNull()
        Assertions.assertThat(brukerFraOpenSearchGjeldendeVedtak14a.innsatsgruppe).isEqualTo(innsatsgruppe)
        Assertions.assertThat(brukerFraOpenSearchGjeldendeVedtak14a.hovedmal).isEqualTo(hovedmal)
        Assertions.assertThat(brukerFraOpenSearchGjeldendeVedtak14a.fattetDato)
            .isEqualTo(fattetDato.toOffsetDateTime().toZonedDateTime())
    }

    @Test
    fun skal_hente_brukere_uten_gjeldendeVedtak14a() {
        val brukerMedSiste14aVedtakFnr = randomFnr()
        val brukerUtenSiste14aVedtakFnr = randomFnr()
        val brukerMedSiste14aVedtakAktorId = randomAktorId()
        val brukerUtenSiste14aVedtakAktorId = randomAktorId()
        val innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        val hovedmal = Hovedmal.BEHOLDE_ARBEID
        val fattetDato = ZonedDateTime.now()
        val fraArena = false
        val siste14aVedtakForBruker = Siste14aVedtakForBruker(
            brukerMedSiste14aVedtakAktorId,
            innsatsgruppe,
            hovedmal,
            fattetDato,
            fraArena
        )
        skrivBrukereTilTestindeks(
            listOf(
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerMedSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
                    .setGjeldendeVedtak14a(
                        GjeldendeVedtak14a(
                            siste14aVedtakForBruker.innsatsgruppe,
                            siste14aVedtakForBruker.hovedmal,
                            siste14aVedtakForBruker.fattetDato
                        )
                    ),
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerUtenSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
            )
        )
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 2 }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList()).setGjeldendeVedtak14a(listOf("HAR_IKKE_14A_VEDTAK")),
            null,
            null
        )
        Assertions.assertThat(respons.antall).isEqualTo(1)
        val brukerFraOpenSearch: PortefoljebrukerFrontendModell = respons.brukere.first()
        Assertions.assertThat(brukerFraOpenSearch.fnr).isEqualTo(brukerUtenSiste14aVedtakFnr.get())
        Assertions.assertThat(brukerFraOpenSearch.aktoerid).isEqualTo(brukerUtenSiste14aVedtakAktorId.get())
        val brukerFraOpenSearchGjeldendeVedtak14a = brukerFraOpenSearch.gjeldendeVedtak14a
        Assertions.assertThat(brukerFraOpenSearchGjeldendeVedtak14a).isNull()
    }

    @Test
    fun skal_hente_brukere_med_og_uten_gjeldendeVedtak14a() {
        val brukerMedSiste14aVedtakFnr = randomFnr()
        val brukerUtenSiste14aVedtakFnr = randomFnr()
        val brukerMedSiste14aVedtakAktorId = randomAktorId()
        val brukerUtenSiste14aVedtakAktorId = randomAktorId()
        val innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        val hovedmal = Hovedmal.BEHOLDE_ARBEID
        val fattetDato = ZonedDateTime.now()
        val fraArena = false
        val siste14aVedtakForBruker = Siste14aVedtakForBruker(
            brukerMedSiste14aVedtakAktorId,
            innsatsgruppe,
            hovedmal,
            fattetDato,
            fraArena
        )
        skrivBrukereTilTestindeks(
            listOf(
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerMedSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerMedSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
                    .setGjeldendeVedtak14a(
                        GjeldendeVedtak14a(
                            siste14aVedtakForBruker.innsatsgruppe,
                            siste14aVedtakForBruker.hovedmal,
                            siste14aVedtakForBruker.fattetDato
                        )
                    ),
                PortefoljebrukerOpensearchModell()
                    .setFnr(brukerUtenSiste14aVedtakFnr.get())
                    .setAktoer_id(brukerUtenSiste14aVedtakAktorId.get())
                    .setEnhet_id(TEST_ENHET)
                    .setOppfolging(true)
            )
        )
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 2 }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList())
                .setGjeldendeVedtak14a(listOf("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK")),
            null,
            null
        )
        Assertions.assertThat(respons.antall).isEqualTo(2)
    }

    @Test
    fun sorter_pa_vedtaksdato_som_standard_ved_filtrering_pa_alle_gjeldendeVedtak14a_filter() {
        val aktoridBrukerMedGjeldendeVedtak14a1 = AktorId.of("4444444444444")
        val aktoridBrukerMedGjeldendeVedtak14a2 = AktorId.of("3333333333333")
        val aktoridBrukerMedGjeldendeVedtak14a3 = AktorId.of("2222222222222")
        val aktoridBrukerUtenVedtak = AktorId.of("1111111111111")

        val vedtaksdatoBruker1 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val vedtaksdatoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val vedtaksdatoBruker3 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().get())
            .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a1.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    Hovedmal.OKE_DELTAKELSE,
                    vedtaksdatoBruker1
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().get())
            .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a2.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    vedtaksdatoBruker2
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().get())
            .setAktoer_id(aktoridBrukerMedGjeldendeVedtak14a3.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.BEHOLDE_ARBEID,
                    vedtaksdatoBruker3
                )
            )

        val brukerUtenGjeldendeVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().get())
            .setAktoer_id(aktoridBrukerUtenVedtak.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)

        val liste = listOf(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtrertHarGjeldendeVedtak = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setGjeldendeVedtak14a(listOf("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK"))

        val responsFiltrertGjeldendeVedtak = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtrertHarGjeldendeVedtak,
            null,
            null
        )
        Assertions.assertThat(responsFiltrertGjeldendeVedtak.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[0].aktoerid,
            bruker1.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[1].aktoerid,
            bruker2.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[2].aktoerid,
            bruker3.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[3].aktoerid,
            brukerUtenGjeldendeVedtak.aktoer_id
        )

        /* Nuller sorteringa ved å sortere på etternamn */
        sorterBrukerePaStandardsorteringenAktorid(opensearchService)

        val filtrertInnsatsgruppe = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setInnsatsgruppeGjeldendeVedtak14a(
                listOf(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
                )
            )

        val responsFiltrertInnsatsgruppe = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtrertInnsatsgruppe,
            null,
            null
        )
        Assertions.assertThat(responsFiltrertInnsatsgruppe.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[0].aktoerid,
            bruker1.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[1].aktoerid,
            bruker2.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[2].aktoerid,
            bruker3.aktoer_id
        )

        /* Nuller sorteringa ved å sortere på etternamn */
        val responsNullstilling: BrukereMedAntall = sorterBrukerePaStandardsorteringenAktorid(opensearchService)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsNullstilling.brukere[0].aktoerid,
            brukerUtenGjeldendeVedtak.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(responsNullstilling.brukere[1].aktoerid, bruker3.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(responsNullstilling.brukere[2].aktoerid, bruker2.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(responsNullstilling.brukere[3].aktoerid, bruker1.aktoer_id)

        val filtrertHovedmal = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setHovedmalGjeldendeVedtak14a(
                listOf(
                    Hovedmal.SKAFFE_ARBEID,
                    Hovedmal.BEHOLDE_ARBEID,
                    Hovedmal.OKE_DELTAKELSE
                )
            )

        val responsFiltrertHovedmal = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtrertHovedmal,
            null,
            null
        )
        Assertions.assertThat(responsFiltrertHovedmal.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[0].aktoerid,
            bruker1.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[1].aktoerid,
            bruker2.aktoer_id
        )
        org.junit.jupiter.api.Assertions.assertEquals(
            responsFiltrertGjeldendeVedtak.brukere[2].aktoerid,
            bruker3.aktoer_id
        )
    }

    @Test
    fun skal_kunne_sortere_brukere_med_og_uten_gjeldendeVedtak14a_pa_14a_kolonner() {
        val brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111")
        val brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222")
        val brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333")
        val brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444")
        val innsatsgruppeBruker1 = Innsatsgruppe.VARIG_TILPASSET_INNSATS
        val innsatsgruppeBruker2 = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
        val innsatsgruppeBruker3 = Innsatsgruppe.STANDARD_INNSATS
        val hovedmalBruker1 = Hovedmal.OKE_DELTAKELSE
        val hovedmalBruker2 = Hovedmal.SKAFFE_ARBEID
        val hovedmalBruker3 = Hovedmal.BEHOLDE_ARBEID
        val vedtaksdatoBruker1 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val vedtaksdatoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val vedtaksdatoBruker3 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr1.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    innsatsgruppeBruker1,
                    hovedmalBruker1,
                    vedtaksdatoBruker1
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr2.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    innsatsgruppeBruker2,
                    hovedmalBruker2,
                    vedtaksdatoBruker2
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr3.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    innsatsgruppeBruker3,
                    hovedmalBruker3,
                    vedtaksdatoBruker3
                )
            )

        val brukerUtenGjeldendeVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(brukerUtenSiste14aVedtakFnr.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)

        val liste = listOf(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setGjeldendeVedtak14a(listOf("HAR_14A_VEDTAK", "HAR_IKKE_14A_VEDTAK"))

        /* Innsatsgruppe, stigande. Forventa rekkefølgje: 2, 3, 1, Uten */
        val responsInnsatsgruppeStigende = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
            filtervalg,
            null,
            null
        )
        Assertions.assertThat(responsInnsatsgruppeStigende.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeStigende.brukere[0].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeStigende.brukere[1].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeStigende.brukere[2].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsInnsatsgruppeStigende.brukere[3].fnr,
            brukerUtenGjeldendeVedtak.fnr
        )

        /* Innsatsgruppe, synkande. Forventa rekkefølgje: 1, 3, 2, Uten */
        val responsInnsatsgruppeSynkende = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
            filtervalg,
            null,
            null
        )
        Assertions.assertThat(responsInnsatsgruppeSynkende.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeSynkende.brukere[0].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeSynkende.brukere[1].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsInnsatsgruppeSynkende.brukere[2].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsInnsatsgruppeSynkende.brukere[3].fnr,
            brukerUtenGjeldendeVedtak.fnr
        )

        /* Hovedmål, stigande. Forventa: 3, 1, 2, Uten */
        val responsHovedmalStigende = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL,
            filtervalg,
            null,
            null
        )
        Assertions.assertThat(responsHovedmalStigende.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertEquals(responsHovedmalStigende.brukere[0].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsHovedmalStigende.brukere[1].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsHovedmalStigende.brukere[2].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsHovedmalStigende.brukere[3].fnr,
            brukerUtenGjeldendeVedtak.fnr
        )

        /* Vedtaksdato, stigande. Forventa rekkefølgje: 3, 2, 1, Uten */
        val responsVedtaksdatoStigende = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_VEDTAKSDATO,
            filtervalg,
            null,
            null
        )
        Assertions.assertThat(responsVedtaksdatoStigende.antall).isEqualTo(4)
        org.junit.jupiter.api.Assertions.assertEquals(responsVedtaksdatoStigende.brukere[0].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsVedtaksdatoStigende.brukere[1].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(responsVedtaksdatoStigende.brukere[2].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(
            responsVedtaksdatoStigende.brukere[3].fnr,
            brukerUtenGjeldendeVedtak.fnr
        )
    }

    @Test
    fun skal_hente_brukere_med_innsatsgruppeGjeldendeVedtak14a() {
        val brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111")
        val brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222")
        val brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333")
        val brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444")

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr1.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    Hovedmal.OKE_DELTAKELSE,
                    ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr2.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr3.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.BEHOLDE_ARBEID,
                    ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val brukerUtenGjeldendeVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(brukerUtenSiste14aVedtakFnr.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)

        val liste = listOf(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setInnsatsgruppeGjeldendeVedtak14a(
                listOf(
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.STANDARD_INNSATS
                )
            )

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE,
            filtervalg,
            null,
            null
        )

        Assertions.assertThat(respons.antall).isEqualTo(2)
        val brukerFraOpenSearch = respons.brukere[0]
        Assertions.assertThat(brukerFraOpenSearch.fnr).isEqualTo(brukerMedSiste14aVedtakFnr3.get())
        val brukerFraOpenSearch1 = respons.brukere[1]
        Assertions.assertThat(brukerFraOpenSearch1.fnr).isEqualTo(brukerMedSiste14aVedtakFnr1.get())
    }

    @Test
    fun skal_hente_brukere_med_hovedmalGjeldendeVedtak14a() {
        val brukerMedSiste14aVedtakFnr1 = Fnr.of("11111111111")
        val brukerMedSiste14aVedtakFnr2 = Fnr.of("22222222222")
        val brukerMedSiste14aVedtakFnr3 = Fnr.of("33333333333")
        val brukerUtenSiste14aVedtakFnr = Fnr.of("44444444444")

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr1.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Hovedmal.OKE_DELTAKELSE,
                    ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr2.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(brukerMedSiste14aVedtakFnr3.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.BEHOLDE_ARBEID,
                    ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
                )
            )

        val brukerUtenGjeldendeVedtak = PortefoljebrukerOpensearchModell()
            .setFnr(brukerUtenSiste14aVedtakFnr.get())
            .setAktoer_id(randomAktorId().get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)

        val liste = listOf(bruker1, bruker2, bruker3, brukerUtenGjeldendeVedtak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setHovedmalGjeldendeVedtak14a(listOf(Hovedmal.SKAFFE_ARBEID, Hovedmal.OKE_DELTAKELSE))

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL,
            filtervalg,
            null,
            null
        )

        Assertions.assertThat(respons.antall).isEqualTo(2)
        val brukerFraOpenSearch = respons.brukere[0]
        Assertions.assertThat(brukerFraOpenSearch.fnr).isEqualTo(brukerMedSiste14aVedtakFnr1.get())
        val brukerFraOpenSearch1 = respons.brukere[1]
        Assertions.assertThat(brukerFraOpenSearch1.fnr).isEqualTo(brukerMedSiste14aVedtakFnr2.get())
    }
    private fun sorterBrukerePaStandardsorteringenAktorid(osService: OpensearchService): BrukereMedAntall {
        return osService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        )
    }
    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
