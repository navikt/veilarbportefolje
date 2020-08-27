package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.mock.LeaderElectionClientMock;
import no.nav.pto.veilarbportefolje.mock.UnleashServiceMock;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OppfolgingFeedHandlerTest {

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private OppfolgingRepository oppfolgingRepository;
    private VeilarbVeilederClient veilarbVeilederClient;

    private OppfolgingFeedHandler oppfolgingFeedHandler;

    private static final AktoerId AKTOER_ID = AktoerId.of("DUMMY");

    @Before
    public void resetMocks() {
        arbeidslisteService = mock(ArbeidslisteService.class);
        brukerRepository = mock(BrukerRepository.class);
        elasticIndexer = mock(ElasticIndexer.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);
        veilarbVeilederClient = mock(VeilarbVeilederClient.class);

        oppfolgingFeedHandler = new OppfolgingFeedHandler(
                arbeidslisteService,
                brukerRepository,
                elasticIndexer,
                oppfolgingRepository,
                veilarbVeilederClient,
                new TestTransactor(),
                new LeaderElectionClientMock(),
                new UnleashServiceMock(false)
        );

    }

    private BrukerOppdatertInformasjon nyInformasjon = brukerInfo(true, "nyVeileder");
    private BrukerOppdatertInformasjon eksisterendeInformasjon = brukerInfo(true, "gammelVeileder");

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
    public void skalSletteArbeidslisteHvisBrukerHarEndretVeilederTilEnAnnenEnhet() {
        givenBrukersHarOppfolgingsData();
        givenBrukerHarVeilederFraAnnenEnhet();

        whenOppdatererOppfolgingData();
        thenArbeidslisteErSlettet();
    }

    @Test
    public void skalIkkeSletteArbeidslisteHvisBrukerHarEndretVeilederPaSammeEnhet() {
        givenBrukersHarOppfolgingsData();
        givenBrukerHarVeilederFraSammeEnhet();

        whenOppdatererOppfolgingData();
        thenArbeidslisteErIkkeSlettet();
    }


    @Test
    public void skalSletteArbeidslisteHvisBrukerHarEndretVeilederOgHaddeIkkeVeilederTidligere() {
        givenBrukersHarOppfolgingsData();
        whenOppdatererOppfolgingData();
        thenArbeidslisteErSlettet();
    }

    @Test
    public void skalSletteArbeidslisteHvisBrukerHarEndretVeilederOgManglerEnhet() {
        givenBrukersHarOppfolgingsData();
        givenBrukerManglerEnhet();
        whenOppdatererOppfolgingData();
        thenArbeidslisteErSlettet();
    }

    @Test
    public void skalSletteArbeidslisteHvisBrukerHarEndretVeilederOgManglerPersonId() {
        givenBrukersHarOppfolgingsData();
        givenBrukerManglerPersonId();
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
        Optional<BigDecimal> maxId = OppfolgingFeedHandler.finnMaxFeedId(Arrays.asList(
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

    private void givenBrukersHarOppfolgingsData() {
        when(oppfolgingRepository.retrieveOppfolgingData(AKTOER_ID))
                .thenReturn(Try.success(eksisterendeInformasjon));
    }

    private void givenBrukerHarVeilederFraAnnenEnhet() {
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveNavKontor(any(PersonId.class))).thenReturn(Try.success("enhet"));
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(Collections.singletonList("whatever"));

    }

    private void givenBrukerHarVeilederFraSammeEnhet() {
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveNavKontor(any(PersonId.class))).thenReturn(Try.success("enhet"));
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any()))
                .thenReturn(Collections.singletonList((eksisterendeInformasjon.getVeileder())));

    }

    private void givenBrukerManglerEnhet(){
        when(oppfolgingRepository.retrieveOppfolgingData(AKTOER_ID)).thenReturn(Try.success(eksisterendeInformasjon));
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveNavKontor(any(PersonId.class))).thenReturn(Try.failure(new RuntimeException()));
    }

    private void givenBrukerManglerPersonId(){
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.failure(new RuntimeException()));
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

    private void thenArbeidslisteErIkkeSlettet() {
        verify(arbeidslisteService, never()).deleteArbeidslisteForAktoerId(AKTOER_ID);
    }


    private BrukerOppdatertInformasjon brukerInfo(boolean oppfolging, String veileder) {
        return new BrukerOppdatertInformasjon()
                .setOppfolging(oppfolging)
                .setVeileder(veileder)
                .setAktoerid(AKTOER_ID.toString());
    }
}
