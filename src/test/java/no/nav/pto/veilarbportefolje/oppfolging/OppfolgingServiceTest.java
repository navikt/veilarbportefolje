package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.val;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.NavKontorService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.util.Result;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService.brukerenIkkeLengerErUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingServiceTest {

    private final static AktoerId TEST_ID = AktoerId.of("testId");

    private static OppfolgingService oppfolgingService;
    private VeilederService veilederServiceMock;
    private OppfolgingRepository repositoryMock;
    private NavKontorService navKontorServiceMock;

    @Before
    public void setUp() {
        repositoryMock = mock(OppfolgingRepository.class);
        veilederServiceMock = mock(VeilederService.class);
        navKontorServiceMock = mock(NavKontorService.class);

        oppfolgingService = new OppfolgingService(
                repositoryMock,
                mock(ElasticIndexer.class),
                veilederServiceMock,
                navKontorServiceMock,
                mock(ArbeidslisteService.class)
        );
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

    @Test
    public void skal_returnere_false_om_vi_ikke_kan_hente_oppfolging_data_ved_sjekk_pa_eksisterende_veileder_sin_tilgang() {
        when(repositoryMock.hentOppfolgingData(any(AktoerId.class)))
                .thenReturn(Result.err(new IllegalStateException()));

        boolean result = oppfolgingService.eksisterendeVeilederHarTilgangTilBruker(TEST_ID);
        assertThat(result).isFalse();
    }

    @Test
    public void skal_returnere_false_om_bruker_ikke_har_nav_kontor() {
        when(repositoryMock.hentOppfolgingData(any(AktoerId.class)))
                .thenReturn(Result.ok(new BrukerOppdatertInformasjon().setVeileder("testVeilederId")));

        when(navKontorServiceMock.hentEnhetForBruker(any(AktoerId.class)))
                .thenReturn(Result.err(new IllegalStateException()));

        boolean result = oppfolgingService.eksisterendeVeilederHarTilgangTilBruker(TEST_ID);
        assertThat(result).isFalse();
    }
    
    @Test
    public void skal_returnere_false_om_henting_av_veiledere_paa_enhet_feiler() {
        when(repositoryMock.hentOppfolgingData(any(AktoerId.class)))
                .thenReturn(Result.ok(new BrukerOppdatertInformasjon().setVeileder("testVeilederId")));

        when(navKontorServiceMock.hentEnhetForBruker(any(AktoerId.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilederServiceMock.hentVeilederePaaEnhet(anyString()))
                .thenThrow(new IllegalStateException());

        boolean result = oppfolgingService.eksisterendeVeilederHarTilgangTilBruker(TEST_ID);
        assertThat(result).isFalse();
    }

    @Test
    public void skal_returnere_true_om_eksisterende_veileder_har_tilgang_til_enhet() {
        when(repositoryMock.hentOppfolgingData(any(AktoerId.class)))
                .thenReturn(Result.ok(new BrukerOppdatertInformasjon().setVeileder("testVeilederId")));

        when(navKontorServiceMock.hentEnhetForBruker(any(AktoerId.class)))
                .thenReturn(Result.ok("testEnhetId"));

        when(veilederServiceMock.hentVeilederePaaEnhet(anyString()))
                .thenReturn(singletonList(VeilederId.of("testVeilederId")));

        boolean result = oppfolgingService.eksisterendeVeilederHarTilgangTilBruker(TEST_ID);
        assertThat(result).isTrue();
    }
}