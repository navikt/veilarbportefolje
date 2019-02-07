package no.nav.fo.veilarbportefolje.consumer;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.database.OppfolgingFeedRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.fo.veilarbportefolje.domene.PersonId;
import no.nav.fo.veilarbportefolje.domene.VeilederId;
import no.nav.fo.veilarbportefolje.service.ArbeidslisteService;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.sbl.jdbc.Transactor;

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

    private class TestTransactor extends Transactor {

        public TestTransactor() {
            super(null);
        }

        @Override
        @SneakyThrows
        public void inTransaction(InTransaction inTransaction) {
            inTransaction.run();
        }

    }

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private IndekseringService indekseringService;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private VeilederService veilederService;

    private OppfolgingFeedHandler oppfolgingFeedHandler;

    private static final AktoerId AKTOER_ID = AktoerId.of("DUMMY");

    @Before
    public void resetMocks() {
        arbeidslisteService = mock(ArbeidslisteService.class);
        brukerRepository = mock(BrukerRepository.class);
        indekseringService = mock(IndekseringService.class);
        oppfolgingFeedRepository = mock(OppfolgingFeedRepository.class);
        veilederService = mock(VeilederService.class);

        oppfolgingFeedHandler = new OppfolgingFeedHandler(
                arbeidslisteService,
                brukerRepository,
                indekseringService,
                oppfolgingFeedRepository,
                veilederService,
                new TestTransactor());
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
        givenBrukerHarVeilderFraAnnenEnhet();

        whenOppdatererOppfolgingData();
        thenArbeidslisteErSlettet();
    }

    @Test
    public void skalIkkeSletteArbeidslisteHvisBrukerHarEndretVeilederPaSammeEnhet() {
        givenBrukersHarOppfolgingsData();
        givenBrukerHarVeilderFraSammeEnhet();

        whenOppdatererOppfolgingData();
        thenArbeidsliteErIkkeSlettet();
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
        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString()))
                .thenReturn(Try.failure(new RuntimeException()));
    }

    private void givenBrukerBlirFjernetFraOppfolging() {
        nyInformasjon = brukerInfo(false, null);
        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString()))
                .thenReturn(Try.success(nyInformasjon));
    }

    private void givenBrukersHarOppfolgingsData() {
        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString()))
                .thenReturn(Try.success(eksisterendeInformasjon));
    }

    private void givenBrukerHarVeilderFraAnnenEnhet() {
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveEnhet(any(PersonId.class))).thenReturn(Try.success("enhet"));
        when(veilederService.getIdenter(any())).thenReturn(Collections.singletonList(VeilederId.of("whatever")));

    }

    private void givenBrukerHarVeilderFraSammeEnhet() {
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveEnhet(any(PersonId.class))).thenReturn(Try.success("enhet"));
        when(veilederService.getIdenter(any()))
                .thenReturn(Collections.singletonList(VeilederId.of(eksisterendeInformasjon.getVeileder())));

    }

    private void givenBrukerManglerEnhet(){
        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString())).thenReturn(Try.success(eksisterendeInformasjon));
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.success(PersonId.of("dummy")));
        when(brukerRepository.retrieveEnhet(any(PersonId.class))).thenReturn(Try.failure(new RuntimeException()));
    }

    private void givenBrukerManglerPersonId(){
        when(brukerRepository.retrievePersonid(any())).thenReturn(Try.failure(new RuntimeException()));
    }

    private void whenOppdatererOppfolgingData() {
        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));
    }

    private void thenOppfolgingDataErOppdatert() {
        verify(indekseringService).indekserAsynkront(AKTOER_ID);
        verify(oppfolgingFeedRepository).oppdaterOppfolgingData(nyInformasjon);
    }

    private void thenArbeidslisteErSlettet() {
        verify(arbeidslisteService).deleteArbeidslisteForAktoerid(AKTOER_ID);
    }

    private void thenArbeidsliteErIkkeSlettet() {
        verify(arbeidslisteService, never()).deleteArbeidslisteForAktoerid(AKTOER_ID);
    }


    private BrukerOppdatertInformasjon brukerInfo(boolean oppfolging, String veileder) {
        return new BrukerOppdatertInformasjon()
                .setOppfolging(oppfolging)
                .setVeileder(veileder)
                .setAktoerid(AKTOER_ID.toString());
    }
}
