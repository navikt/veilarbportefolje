package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.LeaderElectionClientMock;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OppfolgingFeedHandlerTest {

    private ArbeidslisteService arbeidslisteService;
    private ElasticIndexer elasticIndexer;
    private OppfolgingRepository oppfolgingRepository;
    private OppfolgingFeedHandler oppfolgingFeedHandler;

    private static final AktoerId AKTOER_ID = AktoerId.of("DUMMY");

    @Before
    public void resetMocks() {
        arbeidslisteService = mock(ArbeidslisteService.class);
        elasticIndexer = mock(ElasticIndexer.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);

        oppfolgingFeedHandler = new OppfolgingFeedHandler(
                arbeidslisteService,
                mock(BrukerService.class),
                elasticIndexer,
                oppfolgingRepository,
                new TestTransactor(),
                new LeaderElectionClientMock()
        );

    }

    private BrukerOppdatertInformasjon nyInformasjon = brukerInfo(true, "nyVeileder");

    @Test
    public void skal_ikke_sjekke_nav_kontor_paa_arbeidsliste_om_bruker_ikke_har_arbeidsliste() {
        boolean result = oppfolgingFeedHandler.brukerHarByttetNavKontor(AktoerId.of(""));
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void skalLagreBrukerOgOppdatereIndeksAsynkront() {
        givenBrukerIkkeHarNoeOppfolgingsData();
        whenOppdatererOppfolgingData();
        thenOppfolgingDataErOppdatert();
    }

    @Test
    public void skalSletteArbeidslisteHvisBrukerIkkeErUnderOppfolging() {
        givenBrukerBlirFjernetFraOppfolging();
        whenOppdatererOppfolgingData();
        thenArbeidslisteErSlettet();
    }

    @Test
    public void skalFinneMaxFeedId() {
        Optional<BigDecimal> maxId = OppfolgingFeedHandler.finnMaxFeedId(Arrays.asList(
                new BrukerOppdatertInformasjon().setFeedId(BigDecimal.valueOf(2)),
                new BrukerOppdatertInformasjon(),
                new BrukerOppdatertInformasjon().setFeedId(BigDecimal.valueOf(1))));
        assertThat(maxId.isPresent(), is(true));
        assertThat(maxId.get(), is(BigDecimal.valueOf(2)));
    }

    @Test
    public void skalHandtereBareNullIFeedId() {
        Optional<BigDecimal> maxId = OppfolgingFeedHandler.finnMaxFeedId(List.of(
                new BrukerOppdatertInformasjon()));
        assertThat(maxId.isPresent(), is(false));
    }

    @Test
    public void skalHandtereTomListeForFeedId() {
        Optional<BigDecimal> maxId = OppfolgingFeedHandler.finnMaxFeedId(new ArrayList<>());
        assertThat(maxId.isPresent(), is(false));
    }

    private void givenBrukerIkkeHarNoeOppfolgingsData() {
        when(oppfolgingRepository.retrieveOppfolgingData(AKTOER_ID))
                .thenReturn(Try.failure(new RuntimeException()));
    }

    private void givenBrukerBlirFjernetFraOppfolging() {
        nyInformasjon = brukerInfo(false, null);
        when(oppfolgingRepository.retrieveOppfolgingData(AKTOER_ID))
                .thenReturn(Try.success(nyInformasjon));
    }

    private void whenOppdatererOppfolgingData() {
        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));
    }

    private void thenOppfolgingDataErOppdatert() {
        verify(elasticIndexer).indekser(AKTOER_ID);
        verify(oppfolgingRepository).oppdaterOppfolgingData(nyInformasjon);
    }

    private void thenArbeidslisteErSlettet() {
        verify(arbeidslisteService).deleteArbeidslisteForAktoerId(AKTOER_ID);
    }


    private BrukerOppdatertInformasjon brukerInfo(boolean oppfolging, String veileder) {
        return new BrukerOppdatertInformasjon()
                .setOppfolging(oppfolging)
                .setVeileder(veileder)
                .setAktoerid(AKTOER_ID.toString());
    }
}
