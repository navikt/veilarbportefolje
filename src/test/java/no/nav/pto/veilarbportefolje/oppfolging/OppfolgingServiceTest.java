package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.val;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.UnleashServiceMock;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.util.Result;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService.brukerenIkkeLengerErUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingServiceTest {

    private final static AktoerId TEST_ID = AktoerId.of("testId");
    private final static VeilederId TEST_VEILEDER_ID = VeilederId.of("testVeilederId");

    private static OppfolgingService oppfolgingService;
    private static VeilarbVeilederClient veilarbVeilederClientMock;
    private static NavKontorService navKontorServiceMock;
    private static AktorregisterClient aktoerServiceMock;

    @BeforeClass
    public static void setUp() {
        veilarbVeilederClientMock = mock(VeilarbVeilederClient.class);
        navKontorServiceMock = mock(NavKontorService.class);
        aktoerServiceMock = mock(AktorregisterClient.class);

        OppfolgingRepository opppfolgingRepositoryMock = mock(OppfolgingRepository.class);
        ArbeidslisteService arbeidslisteMock = mock(ArbeidslisteService.class);
        ElasticIndexer elasticMock = mock(ElasticIndexer.class);

        oppfolgingService = new OppfolgingService(
                opppfolgingRepositoryMock,
                elasticMock,
                veilarbVeilederClientMock,
                navKontorServiceMock,
                arbeidslisteMock,
                new UnleashServiceMock(true),
                aktoerServiceMock,
                mock(MetricsClient.class)
        );

        when(arbeidslisteMock.deleteArbeidslisteForAktoerId(any(AktoerId.class))).thenReturn(Result.ok(1));
        when(opppfolgingRepositoryMock.hentOppfolgingData(any(AktoerId.class))).thenReturn(Result.of(() -> brukerInfo()));
        when(opppfolgingRepositoryMock.oppdaterOppfolgingData(any(OppfolgingStatus.class))).thenReturn(Result.ok(AktoerId.of("testId")));
        when(elasticMock.indekser(any(AktoerId.class))).thenReturn(Result.ok(new OppfolgingsBruker()));
    }

    private static BrukerOppdatertInformasjon brukerInfo() {
        return new BrukerOppdatertInformasjon().setVeileder("Z000000");
    }

    @Test
    public void skal_sjekke_om_bruker_ikke_lenger_er_under_oppfolging() {
        val dto = OppfolgingStatus.builder()
                .aktoerId(AktoerId.of("testId"))
                .oppfolging(false)
                .build();

        boolean result = brukerenIkkeLengerErUnderOppfolging(dto);
        assertThat(result).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void skal_kaste_exception_om_bruker_ikke_har_nav_kontor() {

        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.err(new IllegalStateException()));

        oppfolgingService.veilederHarTilgangTilBrukerensEnhet(TEST_VEILEDER_ID, TEST_ID);
    }

    @Test
    public void skal_returnere_false_om_henting_av_veiledere_paa_enhet_returnerer_tom_liste() {
        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(emptyList());

        boolean result = oppfolgingService.veilederHarTilgangTilBrukerensEnhet(TEST_VEILEDER_ID, TEST_ID);
        assertThat(result).isFalse();
    }

    @Test
    public void skal_returnere_true_om_eksisterende_veileder_har_tilgang_til_enhet() {

        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(singletonList(VeilederId.of("testVeilederId")));

        boolean result = oppfolgingService.veilederHarTilgangTilBrukerensEnhet(TEST_VEILEDER_ID, TEST_ID);
        assertThat(result).isTrue();
    }

    @Test
    public void skal_returnere_false_om_ny_veileder_ikke_eksisterer() {
        boolean result = oppfolgingService.eksisterendeVeilederHarIkkeTilgangTilBrukerensEnhet(
                TEST_ID,
                Optional.empty(),
                Optional.of(TEST_VEILEDER_ID)
        );
        assertThat(result).isFalse();
    }

    @Test
    public void skal_returnere_false_om_det_ikke_finnes_en_eksisterende_veileder() {
        boolean result = oppfolgingService.eksisterendeVeilederHarIkkeTilgangTilBrukerensEnhet(
                TEST_ID,
                Optional.of(TEST_VEILEDER_ID),
                Optional.empty()
        );

        assertThat(result).isFalse();
    }

    @Test
    public void eksisterende_veileder_skal_ikke_ha_tilgang_til_brukerens_enhet() {
        when(aktoerServiceMock.hentFnr(anyString()))
                .thenReturn("10101010101");

        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(Arrays.asList(VeilederId.of("1"), VeilederId.of("2"), VeilederId.of("3")));

        boolean result = oppfolgingService.eksisterendeVeilederHarIkkeTilgangTilBrukerensEnhet(
                TEST_ID,
                Optional.of(TEST_VEILEDER_ID),
                Optional.of(VeilederId.of("eksisterendeVeileder"))
        );

        assertThat(result).isTrue();
    }

    @Test
    public void skal_ikke_ha_tilgang_til_brukerens_enhet() {

        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(Arrays.asList(VeilederId.of("1"), VeilederId.of("2"), VeilederId.of("3")));

        boolean result = oppfolgingService.veilederHarTilgangTilBrukerensEnhet(TEST_VEILEDER_ID, TEST_ID);
        assertThat(result).isFalse();
    }

    @Test
    public void skal_ha_tilgang_til_brukerens_enhet() {

        when(navKontorServiceMock.hentEnhetForBruker(any(Fnr.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilarbVeilederClientMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(Arrays.asList(VeilederId.of("1"), VeilederId.of("2"), VeilederId.of("3"), TEST_VEILEDER_ID));

        boolean result = oppfolgingService.veilederHarTilgangTilBrukerensEnhet(TEST_VEILEDER_ID, TEST_ID);
        assertThat(result).isTrue();
    }

}