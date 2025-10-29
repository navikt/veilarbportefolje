package no.nav.pto.veilarbportefolje.opensearch

import lombok.SneakyThrows
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.hendelsesfilter.genererRandomHendelse
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.time.*
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.listOf
import kotlin.collections.setOf
import kotlin.collections.toTypedArray

class OpensearchServiceIntegrationDiverseTest @Autowired constructor(
    private val opensearchService: OpensearchService

) : EndToEndTest() {
    @Autowired
    private lateinit var veilarbVeilederClient: VeilarbVeilederClient
    private lateinit var veilederePaEnhet: List<String>
    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String
    private lateinit var TEST_VEILEDER_1: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value
        TEST_VEILEDER_1 = randomVeilederId().value

        veilederePaEnhet = listOf(TEST_VEILEDER_0)

        Mockito.doReturn(veilederePaEnhet).`when`(veilarbVeilederClient)
            .hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET))
    }

    @AfterEach
    fun cleanup() {
        Mockito.reset(veilarbVeilederClient)
    }

    @Test
    fun skal_kun_hente_ut_brukere_under_oppfolging() {
        val brukere = listOf(
            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET),

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET),  // Markert som slettet

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(false)
                .setEnhet_id(TEST_ENHET)
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg(),
            null,
            null
        )

        Assertions.assertThat(brukereMedAntall.antall).isEqualTo(2)
    }

    @Test
    fun skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
        val brukere = listOf(
            PortefoljebrukerOpensearchModell()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("foo"))
                .setVeileder_id(TEST_VEILEDER_0),

            PortefoljebrukerOpensearchModell()
                .setFnr(randomFnr().toString())
                .setAktoer_id(randomAktorId().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("foo"))
                .setVeileder_id(TEST_VEILEDER_1)
        )

        skrivBrukereTilTestindeks(brukere)

        val filtervalg = Filtervalg().setFerdigfilterListe(listOf(Brukerstatus.I_AVTALT_AKTIVITET))
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtervalg,
            null,
            null
        )


        Assertions.assertThat(response.antall).isEqualTo(2)

        val ufordeltBruker: PortefoljebrukerFrontendModell = response.brukere.stream()
            .filter { b: PortefoljebrukerFrontendModell -> TEST_VEILEDER_1 == b.veilederId }
            .toList().first()

        Assertions.assertThat(ufordeltBruker.isNyForEnhet).isTrue()
    }

    @Test
    fun skal_hente_ut_brukere_ved_soek_paa_flere_veiledere() {
        val now = Instant.now().toString()
        val brukere = listOf(
            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setNyesteutlopteaktivitet(now)
                .setVeileder_id(TEST_VEILEDER_0),

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setNyesteutlopteaktivitet(now)
                .setVeileder_id(TEST_VEILEDER_1),

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().get())
                .setFnr(randomFnr().toString())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setNyesteutlopteaktivitet(now)
                .setVeileder_id(null)

        )

        skrivBrukereTilTestindeks(brukere)

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(listOf(Brukerstatus.UTLOPTE_AKTIVITETER))
            .setVeiledere(listOf(TEST_VEILEDER_0, TEST_VEILEDER_1))


        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtervalg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
    }

    @Test
    fun skal_hente_riktig_antall_ufordelte_brukere() {
        val brukere = listOf(
            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(null),

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0),

            PortefoljebrukerOpensearchModell()
                .setAktoer_id(randomAktorId().toString())
                .setFnr(randomFnr().get())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
        )

        Mockito.`when`(veilarbVeilederClient.hentVeilederePaaEnhet(ArgumentMatchers.any())).thenReturn(
            listOf(
                TEST_VEILEDER_0
            )
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filtervalg = Filtervalg().setFerdigfilterListe(listOf(Brukerstatus.UFORDELTE_BRUKERE))
        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filtervalg,
            null,
            null
        )
        Assertions.assertThat(response.antall).isEqualTo(2)
    }

    @Test
    fun skal_hente_riktige_antall_brukere_per_veileder() {
        val veilederId1 = "Z000000"
        val veilederId2 = "Z000001"
        val veilederId3 = "Z000003"

        val brukere = Stream.of(
            veilederId1,
            veilederId1,
            veilederId1,
            veilederId1,
            veilederId2,
            veilederId2,
            veilederId2,
            veilederId3,
            veilederId3,
            null
        )
            .map { id: String? ->
                PortefoljebrukerOpensearchModell()
                    .setAktoer_id(randomAktorId().get())
                    .setFnr(randomFnr().toString())
                    .setVeileder_id(id)
                    .setOppfolging(true)
                    .setEnhet_id(TEST_ENHET)
            }
            .collect(Collectors.toList())

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val portefoljestorrelser = opensearchService.hentPortefoljestorrelser(TEST_ENHET)

        Assertions.assertThat(facetResultCountForVeileder(veilederId1, portefoljestorrelser)).isEqualTo(4L)
        Assertions.assertThat(facetResultCountForVeileder(veilederId2, portefoljestorrelser)).isEqualTo(3L)
        Assertions.assertThat(facetResultCountForVeileder(veilederId3, portefoljestorrelser)).isEqualTo(2L)
    }


    @Test
    fun skal_sortere_brukere_pa_aktivteter() {
        val tidspunkt1 = DateUtils.toIsoUTC(ZonedDateTime.now().plusDays(1))
        val tidspunkt2 = DateUtils.toIsoUTC(ZonedDateTime.now().plusDays(2))
        val tidspunkt3 = DateUtils.toIsoUTC(ZonedDateTime.now().plusDays(3))

        val tidligstfristBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAktivitet_egen_utlopsdato(tidspunkt3)
            .setAktivitet_mote_utlopsdato(tidspunkt1)
            .setAktiviteter(setOf("EGEN", "MOTE"))

        val senestFristBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAktivitet_egen_utlopsdato(tidspunkt2)
            .setAktiviteter(setOf("EGEN", "MOTE"))

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(tidligstfristBruker, senestFristBruker, nullBruker)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg1 = Filtervalg()
            .setAktiviteterForenklet(listOf("EGEN", "MOTE"))
            .setFerdigfilterListe(listOf())
        val filtervalg2 = Filtervalg()
            .setAktiviteterForenklet(listOf("MOTE", "EGEN"))
            .setFerdigfilterListe(listOf())

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.VALGTE_AKTIVITETER,
            filtervalg1,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.VALGTE_AKTIVITETER,
            filtervalg2,
            null,
            null
        )

        val brukere1 = brukereMedAntall.brukere
        val brukere2 = brukereMedAntall2.brukere

        // brukere1 Filter: List.of("EGEN", "MOTE"))
        // brukere2 Filter: List.of("MOTE", "EGEN"))
        Assertions.assertThat(brukere1.size).isEqualTo(2)
        Assertions.assertThat(brukere1[1].fnr).isEqualTo(brukere2[1].fnr)
        Assertions.assertThat(brukere1[0].fnr).isEqualTo(brukere2[0].fnr)

        // Generell sortering:
        Assertions.assertThat(brukere1.size).isEqualTo(2)
        Assertions.assertThat(brukere1[1].fnr).isEqualTo(tidligstfristBruker.fnr)
        Assertions.assertThat(brukere1[0].fnr).isEqualTo(senestFristBruker.fnr)
    }

    @Test
    fun skal_hente_brukere_som_trenger_vurdering_og_er_ny_for_enhet() {
        Mockito.`when`(veilarbVeilederClient.hentVeilederePaaEnhet(ArgumentMatchers.any())).thenReturn(
            listOf(
                TEST_VEILEDER_0
            )
        )
        val nyForEnhet = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
            .setTrenger_vurdering(true)

        val ikkeNyForEnhet = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setTrenger_vurdering(true)


        val liste = listOf(nyForEnhet, ikkeNyForEnhet)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val ferdigFiltere = listOf(
            Brukerstatus.UFORDELTE_BRUKERE
        )

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(ferdigFiltere),
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(userExistsInResponse(nyForEnhet, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(ikkeNyForEnhet, response)).isFalse()
    }

    @Test
    fun skal_ikke_kunne_hente_brukere_veileder_ikke_har_tilgang_til() {
        val brukerVeilederHarTilgangTil = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)

        val brukerVeilederIkkeHarTilgangTil = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id("NEGA_\$testEnhet")
            .setVeileder_id("NEGA_\$testVeileder")

        val liste = listOf(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg(),
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(userExistsInResponse(brukerVeilederHarTilgangTil, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerVeilederIkkeHarTilgangTil, response)).isFalse()
    }

    @Test
    fun skal_anse_bruker_som_ufordelt_om_bruker_har_veileder_som_ikke_har_tilgang_til_enhet() {
        Mockito.`when`(veilarbVeilederClient.hentVeilederePaaEnhet(ArgumentMatchers.any())).thenReturn(
            listOf(
                TEST_VEILEDER_0
            )
        )

        val brukerMedUfordeltStatus = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)

        val brukerMedFordeltStatus = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)

        val liste = listOf(brukerMedUfordeltStatus, brukerMedFordeltStatus)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(LITE_PRIVILEGERT_VEILEDER),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(listOf(Brukerstatus.UFORDELTE_BRUKERE)),
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(veilederExistsInResponse(LITE_PRIVILEGERT_VEILEDER, response)).isTrue()

        val statustall = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET,
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
        )
        Assertions.assertThat(statustall.ufordelteBrukere).isEqualTo(1)
    }

    @Test
    fun skal_hente_ut_brukere_som_har_avtale_om_a_soke_jobber() {
        val brukerMedSokeAvtale = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("sokeavtale"))

        val brukerMedBehandling = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("behandling"))

        val brukerMedUtenAktiviteter = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)


        val liste = listOf(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setAktiviteter(mapOf("SOKEAVTALE" to AktivitetFiltervalg.JA))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isTrue()
    }

    @Test
    fun skal_hente_ut_alle_brukere_unntatt_de_som_har_avtale_om_a_soke_jobber() {
        val brukerMedSokeAvtale = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("sokeavtale"))

        val brukerMedBehandling = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("behandling"))

        val brukerMedUtenAktiviteter = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setAktiviteter(mapOf("SOKEAVTALE" to AktivitetFiltervalg.NEI))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerMedUtenAktiviteter, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isFalse()
    }

    @Test
    fun skal_hente_ut_alle_brukere_med_tiltak() {
        val brukerMedTiltak = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("tiltak"))

        val brukerMedBehandling = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("behandling"))

        val brukerUtenAktiviteter = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setAktiviteter(mapOf("TILTAK" to AktivitetFiltervalg.JA))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(userExistsInResponse(brukerMedTiltak, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerMedBehandling, response)).isFalse()
        Assertions.assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isFalse()
    }

    @Test
    fun skal_hente_ut_alle_brukere_som_ikke_har_tiltak() {
        val brukerMedTiltak = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("tiltak"))
            .setTiltak(setOf("VASV"))

        val brukerMedBehandling = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setAktiviteter(setOf("behandling"))

        val brukerUtenAktiviteter = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setAktiviteter(mapOf("TILTAK" to AktivitetFiltervalg.NEI))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(brukerMedTiltak, response)).isFalse()
    }


    @Test
    fun skal_ikke_automatisk_sortere_nye_brukere_paa_top() {
        Mockito.`when`(defaultUnleash.isEnabled(ArgumentMatchers.anyString())).thenReturn(true)
        val nyBrukerForVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(true)
            .setEtternavn("A")
        val brukerForVeileder1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEtternavn("B")
        val brukerForVeileder2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEtternavn("C")

        val liste = listOf(nyBrukerForVeileder, brukerForVeileder1, brukerForVeileder2)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.ETTERNAVN,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.brukere).hasSize(3)
        Assertions.assertThat(response.brukere[0].etternavn).isEqualTo("C")
        Assertions.assertThat(response.brukere[2].etternavn).isEqualTo("A")
    }

    @Test
    fun skal_ikke_automatisk_sortere_ufordelte_brukere_paa_top() {
        Mockito.`when`(defaultUnleash.isEnabled(ArgumentMatchers.anyString())).thenReturn(true)
        val ufordeltBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(null)
            .setEtternavn("A")
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEtternavn("B")
        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEtternavn("C")

        val liste = listOf(ufordeltBruker, bruker1, bruker2)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.ETTERNAVN,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.brukere).hasSize(3)
        Assertions.assertThat(response.brukere[0].etternavn).isEqualTo("C")
        Assertions.assertThat(response.brukere[2].etternavn).isEqualTo("A")
    }

    @Test
    fun test_sortering_enslige_forsorgere() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setEnslige_forsorgere_overgangsstonad(
                EnsligeForsorgereOvergangsstonad(
                    "Hovedperiode",
                    true, LocalDate.now().plusMonths(4), LocalDate.now().minusMonths(2)
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setEnslige_forsorgere_overgangsstonad(
                EnsligeForsorgereOvergangsstonad(
                    "Forlengelse",
                    false, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(7)
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setEnslige_forsorgere_overgangsstonad(
                EnsligeForsorgereOvergangsstonad(
                    "Utvidelse",
                    false, LocalDate.now().plusMonths(1), LocalDate.now().minusMonths(3)
                )
            )

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setEnslige_forsorgere_overgangsstonad(
                EnsligeForsorgereOvergangsstonad(
                    "Periode før fødsel",
                    true, LocalDate.now().plusMonths(7), LocalDate.now().minusMonths(1)
                )
            )

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.ENSLIGE_FORSORGERE_UTLOP_YTELSE,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[0].fnr, bruker5.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[1].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[2].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[3].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[4].fnr, bruker4.fnr)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.ENSLIGE_FORSORGERE_OM_BARNET,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[0].fnr, bruker5.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[1].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[2].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[3].fnr, bruker4.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[4].fnr, bruker2.fnr)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.ENSLIGE_FORSORGERE_AKTIVITETSPLIKT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertTrue(response.brukere[0].fnr == bruker2.fnr || response.brukere[0].fnr == bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertTrue(response.brukere[1].fnr == bruker2.fnr || response.brukere[1].fnr == bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertTrue(response.brukere[2].fnr == bruker1.fnr || response.brukere[2].fnr == bruker4.fnr)
        org.junit.jupiter.api.Assertions.assertTrue(response.brukere[3].fnr == bruker1.fnr || response.brukere[3].fnr == bruker4.fnr)

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(5)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[0].fnr, bruker2.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[1].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[2].fnr, bruker4.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[3].fnr, bruker3.fnr)
    }


    @Test
    fun skal_sortere_pa_aktorid_som_standard_om_ikke_sorteringsfelt_er_valgt() {
        val fnrBruker1 = Fnr.of("11111111111")
        val fnrBruker2 = Fnr.of("22222222222")
        val fnrBruker3 = Fnr.of("33333333333")
        val aktoridBruker1 = AktorId.of("3333333333333")
        val aktoridBruker2 = AktorId.of("1111111111111")
        val aktoridBruker3 = AktorId.of("2222222222222")
        val datoBruker1 = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val datoBruker2 = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        val datoBruker3 = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault())

        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(fnrBruker1.get())
            .setAktoer_id(aktoridBruker1.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    datoBruker1.toLocalDateTime(),
                    "",
                    "",
                    Tiltakstype.ARBFORB,
                    fnrBruker1
                )
            )
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    datoBruker1
                )
            )

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(fnrBruker2.get())
            .setAktoer_id(aktoridBruker2.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    datoBruker2.toLocalDateTime(),
                    "",
                    "",
                    Tiltakstype.ARBFORB,
                    fnrBruker2
                )
            )
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    datoBruker2
                )
            )

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(fnrBruker3.get())
            .setAktoer_id(aktoridBruker3.get())
            .setEnhet_id(TEST_ENHET)
            .setOppfolging(true)
            .setTiltakshendelse(
                Tiltakshendelse(
                    UUID.randomUUID(),
                    datoBruker3.toLocalDateTime(),
                    "",
                    "",
                    Tiltakstype.ARBFORB,
                    fnrBruker3
                )
            )
            .setGjeldendeVedtak14a(
                GjeldendeVedtak14a(
                    Innsatsgruppe.STANDARD_INNSATS,
                    Hovedmal.SKAFFE_ARBEID,
                    datoBruker1
                )
            )

        val liste = listOf(bruker1, bruker2, bruker3)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        )
        Assertions.assertThat(respons.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(respons.brukere[0].aktoerid, bruker2.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(respons.brukere[1].aktoerid, bruker3.aktoer_id)
        org.junit.jupiter.api.Assertions.assertEquals(respons.brukere[2].aktoerid, bruker1.aktoer_id)
    }

    @Test
    fun skal_kunne_sortere_pa_alle_gyldige_sorteringsverdier() {
        val alleSorteringsfelt = Sorteringsfelt.entries.toTypedArray()
        val sorteringsfeltSomFeilerISortering = ArrayList<Sorteringsfelt>()

        for (sorteringsfelt in alleSorteringsfelt) {
            try {
                opensearchService.hentBrukere(
                    TEST_ENHET,
                    Optional.empty(),
                    Sorteringsrekkefolge.STIGENDE,
                    sorteringsfelt,
                    Filtervalg().setFerdigfilterListe(emptyList()),
                    null,
                    null
                )
            } catch (e: Exception) {
                sorteringsfeltSomFeilerISortering.add(sorteringsfelt)
            }
        }

        // Viser at vi får feil slik kodebasen er no. Målet er at sorteringsfeltSomFeilerISortering skal vere tom.
        Assertions.assertThat(sorteringsfeltSomFeilerISortering).isNotEmpty()
    }


    @Test
    fun skal_sortere_brukere_pa_tildelt_tidspunkt() {
        val tidspunkt1 = LocalDateTime.now()
        val tidspunkt2 = LocalDateTime.now().plusDays(2)
        val tidspunkt3 = LocalDateTime.now().plusDays(3)

        val tidligstTildeltBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTildelt_tidspunkt(tidspunkt1)

        val midtImellomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTildelt_tidspunkt(tidspunkt2)

        val senestTildeltBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTildelt_tidspunkt(tidspunkt3)

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTildelt_tidspunkt(null)


        val liste = listOf(midtImellomBruker, senestTildeltBruker, tidligstTildeltBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TILDELT_TIDSPUNKT,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.TILDELT_TIDSPUNKT,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(4)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTildeltBruker.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(nullBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(nullBruker.fnr)
        Assertions.assertThat(brukereSynkende[1].fnr).isEqualTo(senestTildeltBruker.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(tidligstTildeltBruker.fnr)
    }

    @Test
    @SneakyThrows
    fun skal_indeksere_hendelse_data_riktig_for_utgatt_varsel() {
        val hendelse = genererRandomHendelse(Kategori.UTGATT_VARSEL)
        val oppfolgingsBruker = PortefoljebrukerOpensearchModell()
            .setFnr("11111199999")
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setUtgatt_varsel(hendelse.hendelse)
        skrivBrukereTilTestindeks(listOf(oppfolgingsBruker))

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == 1 }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.IKKE_SATT,
            Filtervalg().setFerdigfilterListe(emptyList()),
            null,
            null
        )
        val bruker: PortefoljebrukerFrontendModell = respons.brukere.first()
        val utgattVarsel = bruker.utgattVarsel

        Assertions.assertThat(respons.antall).isEqualTo(1)
        Assertions.assertThat(utgattVarsel).isNotNull()
        Assertions.assertThat(utgattVarsel.beskrivelse).isEqualTo(oppfolgingsBruker.utgatt_varsel.beskrivelse)
        Assertions.assertThat(utgattVarsel.detaljer).isEqualTo(oppfolgingsBruker.utgatt_varsel.detaljer)
        Assertions.assertThat(utgattVarsel.lenke).isEqualTo(oppfolgingsBruker.utgatt_varsel.lenke)
    }

    private fun veilederExistsInResponse(veilederId: String, brukere: BrukereMedAntall): Boolean {
        return brukere.brukere.stream().anyMatch { bruker: PortefoljebrukerFrontendModell -> veilederId == bruker.veilederId }
    }

    private fun userExistsInResponse(bruker: PortefoljebrukerOpensearchModell, brukere: BrukereMedAntall): Boolean {
        return brukere.brukere.stream().anyMatch { b: PortefoljebrukerFrontendModell -> bruker.fnr == b.fnr }
    }

    private fun facetResultCountForVeileder(testVeileder1: String, portefoljestorrelser: FacetResults): Long {
        return portefoljestorrelser.facetResults.stream().filter { it: Facet -> testVeileder1 == it.value }
            .map { obj: Facet -> obj.count }.toList().first()
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }

    companion object {
        private val LITE_PRIVILEGERT_VEILEDER: String = randomVeilederId().value
    }
}
