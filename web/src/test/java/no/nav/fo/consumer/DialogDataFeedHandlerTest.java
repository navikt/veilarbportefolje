package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static no.nav.fo.consumer.DialogDataFeedHandler.DIALOGAKTOR_SIST_OPPDATERT;
import static org.mockito.Mockito.*;

public class DialogDataFeedHandlerTest {

    private SolrService solrService;
    private DialogFeedRepository dialogFeedRepository;
    private BrukerRepository brukerRepository;

    private DialogDataFeedHandler dialogDataFeedHandler;

    @Before
    public void setUp() {
        solrService = mock(SolrService.class);                
        dialogFeedRepository = mock(DialogFeedRepository.class);
        brukerRepository = mock(BrukerRepository.class);
        dialogDataFeedHandler = new DialogDataFeedHandler(brukerRepository, solrService, dialogFeedRepository);
    }

    @Test
    public void skalLagreData_og_indeksere_og_oppdatere_feed_metadata() {
        AktoerId aktoerId = AktoerId.of("1111");

        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId(aktoerId.toString());

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));

        verify(dialogFeedRepository, times(1)).oppdaterDialogInfoForBruker(dialogdata);
        verify(solrService, times(1)).indekserAsynkront(aktoerId);
        verify(brukerRepository, times(1)).updateMetadata(eq(DIALOGAKTOR_SIST_OPPDATERT), any(Date.class));
    }
}