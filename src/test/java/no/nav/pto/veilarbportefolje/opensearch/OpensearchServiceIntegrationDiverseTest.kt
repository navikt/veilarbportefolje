package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
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
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
            ),
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
            ),  // Markert som slettet
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = false,
                enhet_id = TEST_ENHET,
            )
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            getFiltervalgDefaults(),
            null,
            null
        )

        Assertions.assertThat(brukereMedAntall.antall).isEqualTo(2)
    }

    @Test
    fun skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
        val brukere = listOf(
            PortefoljebrukerOpensearchModell(
                fnr = randomFnr().toString(),
                aktoer_id = randomAktorId().toString(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                aktiviteter = setOf("foo"),
                veileder_id = TEST_VEILEDER_0,
            ),
            PortefoljebrukerOpensearchModell(
                fnr = randomFnr().toString(),
                aktoer_id = randomAktorId().toString(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                aktiviteter = setOf("foo"),
                veileder_id = TEST_VEILEDER_1,
            )
        )

        skrivBrukereTilTestindeks(brukere)

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf(Brukerstatus.I_AVTALT_AKTIVITET)
        )
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

        Assertions.assertThat(ufordeltBruker.etiketter.nyForEnhet).isTrue()
    }

    @Test
    fun skal_hente_ut_brukere_ved_soek_paa_flere_veiledere() {
        val now = Instant.now().toString()
        val brukere = listOf(
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().get(),
                fnr = randomFnr().toString(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                nyesteutlopteaktivitet = now,
                veileder_id = TEST_VEILEDER_0,
            ),
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().get(),
                fnr = randomFnr().toString(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                nyesteutlopteaktivitet = now,
                veileder_id = TEST_VEILEDER_1,
            ),
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().get(),
                fnr = randomFnr().toString(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                nyesteutlopteaktivitet = now,
                veileder_id = null,
            )
        )

        skrivBrukereTilTestindeks(brukere)

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf(Brukerstatus.UTLOPTE_AKTIVITETER),
            veiledere = listOf(TEST_VEILEDER_0, TEST_VEILEDER_1)
        )

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
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                veileder_id = null,
            ),
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                veileder_id = TEST_VEILEDER_0,
            ),
            PortefoljebrukerOpensearchModell(
                aktoer_id = randomAktorId().toString(),
                fnr = randomFnr().get(),
                oppfolging = true,
                enhet_id = TEST_ENHET,
                veileder_id = LITE_PRIVILEGERT_VEILEDER,
            )
        )

        Mockito.`when`(veilarbVeilederClient.hentVeilederePaaEnhet(ArgumentMatchers.any())).thenReturn(
            listOf(
                TEST_VEILEDER_0
            )
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf(Brukerstatus.UFORDELTE_BRUKERE)
        )
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
                PortefoljebrukerOpensearchModell(
                    aktoer_id = randomAktorId().get(),
                    fnr = randomFnr().toString(),
                    veileder_id = id,
                    oppfolging = true,
                    enhet_id = TEST_ENHET,
                )
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

        val tidligstfristBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aktivitet_egen_utlopsdato = tidspunkt3,
            aktivitet_mote_utlopsdato = tidspunkt1,
            aktiviteter = setOf("EGEN", "MOTE"),
        )

        val senestFristBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aktivitet_egen_utlopsdato = tidspunkt2,
            aktiviteter = setOf("EGEN", "MOTE"),
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(tidligstfristBruker, senestFristBruker, nullBruker)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg1 = getFiltervalgDefaults().copy(
            aktiviteterForenklet = listOf("EGEN", "MOTE"),
            ferdigfilterListe = listOf()
        )
        val filtervalg2 = getFiltervalgDefaults().copy(
            aktiviteterForenklet = listOf("MOTE", "EGEN"),
            ferdigfilterListe = listOf()
        )

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
        val nyForEnhet = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = LITE_PRIVILEGERT_VEILEDER,
            trenger_vurdering = true,
        )

        val ikkeNyForEnhet = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            trenger_vurdering = true,
        )


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
            getFiltervalgDefaults().copy(
                ferdigfilterListe = ferdigFiltere
            ),
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(userExistsInResponse(nyForEnhet, response)).isTrue()
        Assertions.assertThat(userExistsInResponse(ikkeNyForEnhet, response)).isFalse()
    }

    @Test
    fun skal_ikke_kunne_hente_brukere_veileder_ikke_har_tilgang_til() {
        val brukerVeilederHarTilgangTil = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
        )

        val brukerVeilederIkkeHarTilgangTil = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = "NEGA_\$testEnhet",
            veileder_id = "NEGA_\$testVeileder",
        )

        val liste = listOf(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            getFiltervalgDefaults(),
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

        val brukerMedUfordeltStatus = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = LITE_PRIVILEGERT_VEILEDER,
        )

        val brukerMedFordeltStatus = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
        )

        val liste = listOf(brukerMedUfordeltStatus, brukerMedFordeltStatus)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(LITE_PRIVILEGERT_VEILEDER),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            getFiltervalgDefaults().copy(
                ferdigfilterListe = listOf(Brukerstatus.UFORDELTE_BRUKERE)
            ),
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
        val brukerMedSokeAvtale = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("sokeavtale"),
        )

        val brukerMedBehandling = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("behandling"),
        )

        val brukerMedUtenAktiviteter = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
        )


        val liste = listOf(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            aktiviteter = mutableMapOf("SOKEAVTALE" to AktivitetFiltervalg.JA)
        )

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
        val brukerMedSokeAvtale = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("sokeavtale"),
        )

        val brukerMedBehandling = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("behandling"),
        )

        val brukerMedUtenAktiviteter = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().toString(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(brukerMedSokeAvtale, brukerMedUtenAktiviteter, brukerMedBehandling)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            aktiviteter = mutableMapOf("SOKEAVTALE" to AktivitetFiltervalg.NEI)
        )

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
        val brukerMedTiltak = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("tiltak"),
        )

        val brukerMedBehandling = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("behandling"),
        )

        val brukerUtenAktiviteter = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            aktiviteter = mutableMapOf("TILTAK" to AktivitetFiltervalg.JA)
        )

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
        val brukerMedTiltak = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().toString(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("tiltak"),
            tiltak = setOf("VASV"),
        )

        val brukerMedBehandling = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().toString(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            aktiviteter = setOf("behandling"),
        )

        val brukerUtenAktiviteter = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().toString(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            aktiviteter = mutableMapOf("TILTAK" to AktivitetFiltervalg.NEI)
        )

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
        val nyBrukerForVeileder = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = true,
            etternavn = "A",
        )

        val brukerForVeileder1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            etternavn = "B",
        )

        val brukerForVeileder2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            etternavn = "C",
        )

        val liste = listOf(nyBrukerForVeileder, brukerForVeileder1, brukerForVeileder2)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults()

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
        val ufordeltBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = null,
            etternavn = "A",
        )

        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            etternavn = "B",
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            etternavn = "C",
        )

        val liste = listOf(ufordeltBruker, bruker1, bruker2)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults()

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
        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            enslige_forsorgere_overgangsstonad = EnsligeForsorgereOvergangsstonad(
                "Hovedperiode",
                true,
                LocalDate.now().plusMonths(4),
                LocalDate.now().minusMonths(2)
            ),
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            enslige_forsorgere_overgangsstonad = EnsligeForsorgereOvergangsstonad(
                "Forlengelse",
                false,
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(7)
            ),
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            enslige_forsorgere_overgangsstonad = EnsligeForsorgereOvergangsstonad(
                "Utvidelse",
                false,
                LocalDate.now().plusMonths(1),
                LocalDate.now().minusMonths(3)
            ),
        )

        val bruker4 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            enslige_forsorgere_overgangsstonad = EnsligeForsorgereOvergangsstonad(
                "Periode før fødsel",
                true,
                LocalDate.now().plusMonths(7),
                LocalDate.now().minusMonths(1)
            ),
        )

        val bruker5 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf()
        )

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

        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = fnrBruker1.get(),
            aktoer_id = aktoridBruker1.get(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            tiltakshendelse = Tiltakshendelse(
                UUID.randomUUID(),
                datoBruker1.toLocalDateTime(),
                "",
                "",
                Tiltakstype.ARBFORB,
                fnrBruker1
            ),
            gjeldendeVedtak14a = GjeldendeVedtak14a(
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.SKAFFE_ARBEID,
                datoBruker1
            ),
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = fnrBruker2.get(),
            aktoer_id = aktoridBruker2.get(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            tiltakshendelse = Tiltakshendelse(
                UUID.randomUUID(),
                datoBruker2.toLocalDateTime(),
                "",
                "",
                Tiltakstype.ARBFORB,
                fnrBruker2
            ),
            gjeldendeVedtak14a = GjeldendeVedtak14a(
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.SKAFFE_ARBEID,
                datoBruker2
            ),
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = fnrBruker3.get(),
            aktoer_id = aktoridBruker3.get(),
            enhet_id = TEST_ENHET,
            oppfolging = true,
            tiltakshendelse = Tiltakshendelse(
                UUID.randomUUID(),
                datoBruker3.toLocalDateTime(),
                "",
                "",
                Tiltakstype.ARBFORB,
                fnrBruker3
            ),
            gjeldendeVedtak14a = GjeldendeVedtak14a(
                Innsatsgruppe.STANDARD_INNSATS,
                Hovedmal.SKAFFE_ARBEID,
                datoBruker1
            ),
        )

        val liste = listOf(bruker1, bruker2, bruker3)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val respons = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            getFiltervalgDefaults(),
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
                    getFiltervalgDefaults().copy(
                        ferdigfilterListe = emptyList()
                    ),
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

        val tidligstTildeltBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tildelt_tidspunkt = tidspunkt1,
        )

        val midtImellomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tildelt_tidspunkt = tidspunkt2,
        )

        val senestTildeltBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tildelt_tidspunkt = tidspunkt3,
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tildelt_tidspunkt = null,
        )


        val liste = listOf(midtImellomBruker, senestTildeltBruker, tidligstTildeltBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList()
        )

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

    private fun veilederExistsInResponse(veilederId: String, brukere: BrukereMedAntall): Boolean {
        return brukere.brukere.stream()
            .anyMatch { bruker: PortefoljebrukerFrontendModell -> veilederId == bruker.veilederId }
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
