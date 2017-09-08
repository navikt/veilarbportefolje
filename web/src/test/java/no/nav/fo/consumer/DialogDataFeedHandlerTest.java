package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DialogDataFeedHandlerTest {

    @Mock
    private PersistentOppdatering persistentOppdatering;

    @Mock
    private AktoerService aktoerService;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private SolrService solrService;

    @InjectMocks
    private DialogDataFeedHandler dialogDataFeedHandler;

    @Before
    public void resetMocks() {
        reset(persistentOppdatering,aktoerService,brukerRepository,solrService);
    }

    @Test
    public void skalIkkeKalleLagreDersomBrukerIkkeErUnderOppfolging() {
        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId("aktoerid");

        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(PersonId.of("123123")));
        when(brukerRepository.retrieveOppfolgingstatus(any())).thenReturn(Try.success(new Oppfolgingstatus().setOppfolgingsbruker(false)));

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));
        verify(solrService, times(1)).slettBruker(any(PersonId.class));
        verify(persistentOppdatering, never()).lagre(any());
    }

    @Test
    public void skalKalleLagreDersomBrukerErUnderOppfolging() {
        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId("aktoerid");

        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(PersonId.of("123123")));
        when(brukerRepository.retrieveOppfolgingstatus(any())).thenReturn(Try.success(new Oppfolgingstatus().setOppfolgingsbruker(true)));

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));
        verify(solrService, never()).slettBruker(any(PersonId.class));
        verify(persistentOppdatering, times(1)).lagre(any());
    }
}