package no.nav.pto.veilarbportefolje.feed.dialog;

import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static no.nav.pto.veilarbportefolje.feed.dialog.DialogDataFeedHandler.DIALOGAKTOR_SIST_OPPDATERT;
import static org.mockito.Mockito.*;

public class DialogDataFeedHandlerTest {

    private ElasticIndexer elasticIndexer;
    private DialogFeedRepository dialogFeedRepository;
    private BrukerRepository brukerRepository;

    private DialogDataFeedHandler dialogDataFeedHandler;

    @Before
    public void setUp() {
        elasticIndexer = mock(ElasticIndexer.class);
        when(elasticIndexer.indekserAsynkront(any())).thenReturn(CompletableFuture.runAsync(() -> {}));
        dialogFeedRepository = mock(DialogFeedRepository.class);
        brukerRepository = mock(BrukerRepository.class);
        dialogDataFeedHandler = new DialogDataFeedHandler(brukerRepository, elasticIndexer, dialogFeedRepository, mock(UnleashService.class));
    }

    @Test
    public void skalLagreData_og_indeksere_og_oppdatere_feed_metadata() {
        AktoerId aktoerId = AktoerId.of("1111");

        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId(aktoerId.toString()).setSisteEndring(new Date());

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));

        verify(dialogFeedRepository, times(1)).oppdaterDialogInfoForBruker(dialogdata);
        verify(elasticIndexer, times(1)).indekserAsynkront(aktoerId);
        verify(brukerRepository, times(1)).updateMetadata(eq(DIALOGAKTOR_SIST_OPPDATERT), any(Date.class));
    }
}
