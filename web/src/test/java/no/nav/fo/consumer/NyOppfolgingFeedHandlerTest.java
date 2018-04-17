package no.nav.fo.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.sbl.jdbc.Transactor;

public class NyOppfolgingFeedHandlerTest {

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
    private SolrService solrService;
    private OppfolgingFeedRepository oppfolgingFeedRepository;

    private NyOppfolgingFeedHandler oppfolgingFeedHandler;

    private static final AktoerId AKTOER_ID = AktoerId.of("DUMMY");

    @Before
    public void resetMocks() {
        arbeidslisteService = mock(ArbeidslisteService.class);
        brukerRepository = mock(BrukerRepository.class);
        solrService = mock(SolrService.class);
        oppfolgingFeedRepository = mock(OppfolgingFeedRepository.class);

        oppfolgingFeedHandler = new NyOppfolgingFeedHandler(
                arbeidslisteService, 
                brukerRepository, 
                solrService, 
                oppfolgingFeedRepository, 
                new TestTransactor());
    }

    @Test
    public void skalLagreBrukerOgOppdatereIndeksAsynkront() {

        BrukerOppdatertInformasjon nyInformasjon = brukerInfo(false, null);

        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString())).thenReturn(Try.failure(new RuntimeException()));

        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));

        verify(solrService).indekserAsynkront(AKTOER_ID);
        verify(oppfolgingFeedRepository).oppdaterOppfolgingData(nyInformasjon);

    }

    private BrukerOppdatertInformasjon brukerInfo(boolean oppfolging, String veileder) {
        return new BrukerOppdatertInformasjon()
                .setOppfolging(oppfolging)
                .setVeileder(veileder)
                .setAktoerid(AKTOER_ID.toString());
    }

    @Test
    public void skalSletteArbeidslisteHvisBrukerIkkeErUnderOppfolging() {

        BrukerOppdatertInformasjon nyInformasjon = brukerInfo(false, null);

        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString())).thenReturn(Try.failure(new RuntimeException()));

        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));

        verify(arbeidslisteService).deleteArbeidslisteForAktoerid(AKTOER_ID);
        verify(solrService).indekserAsynkront(AKTOER_ID);
        verify(oppfolgingFeedRepository).oppdaterOppfolgingData(nyInformasjon);

    }

    @Test
    public void skalSletteArbeidslisteHvisBrukerHarEndretVeileder() {

        BrukerOppdatertInformasjon nyInformasjon = brukerInfo(true, "nyVeileder");
        BrukerOppdatertInformasjon eksisterendeInformasjon = brukerInfo(true, "gammelVeileder");

        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString())).thenReturn(Try.success(eksisterendeInformasjon));

        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));

        verify(arbeidslisteService).deleteArbeidslisteForAktoerid(AKTOER_ID);
        verify(solrService).indekserAsynkront(AKTOER_ID);
        verify(oppfolgingFeedRepository).oppdaterOppfolgingData(nyInformasjon);

    }

    @Test
    public void skalIkkeSletteArbeidslisteHvisBrukerHarSammeVeilederOgErUnderOppfolging() {

        BrukerOppdatertInformasjon nyInformasjon = brukerInfo(true, "nyVeileder");
        BrukerOppdatertInformasjon eksisterendeInformasjon = brukerInfo(true, "nyVeileder");

        when(oppfolgingFeedRepository.retrieveOppfolgingData(AKTOER_ID.toString())).thenReturn(Try.success(eksisterendeInformasjon));

        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(nyInformasjon));

        verify(arbeidslisteService, never()).deleteArbeidslisteForAktoerid(AKTOER_ID);
        verify(solrService).indekserAsynkront(AKTOER_ID);
        verify(oppfolgingFeedRepository).oppdaterOppfolgingData(nyInformasjon);

    }
}
