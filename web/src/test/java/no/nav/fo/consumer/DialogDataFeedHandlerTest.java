package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
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

    @Mock
    private DialogFeedRepository dialogFeedRepository;

    @InjectMocks
    private DialogDataFeedHandler dialogDataFeedHandler;

    @Before
    public void resetMocks() {
        reset(persistentOppdatering,aktoerService,brukerRepository,solrService, dialogFeedRepository);
    }

    @Test
    public void skalPopulereIndeksForBrukereSomErUnderOppfolging() {
        PersonId personId = PersonId.of("0000");
        AktoerId aktoerId = AktoerId.of("1111");

        ArgumentCaptor<List<PersonId>> captorUnderOppfolging = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PersonId>> captorIkkeUnderOppfolging = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<DialogDataFraFeed> captorDialogData = ArgumentCaptor.forClass(DialogDataFraFeed.class);
        ArgumentCaptor<PersonId> captorPersonid = ArgumentCaptor.forClass(PersonId.class);

        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId(aktoerId.toString());

        Map<AktoerId, Optional<PersonId>> identMap = new HashMap<>();
        identMap.put(aktoerId, Optional.of(personId));
        when(aktoerService.hentPersonidsForAktoerids(any())).thenReturn(identMap);

        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = new HashMap<>();
        oppfolgingstatus.put(personId, new Oppfolgingstatus().setOppfolgingsbruker(true));
        when(brukerRepository.retrieveOppfolgingstatus(anyList())).thenReturn(oppfolgingstatus);

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));

        verify(dialogFeedRepository, times(1)).upsertDialogdata(captorDialogData.capture(), captorPersonid.capture());
        verify(solrService, times(1)).slettBrukere(captorIkkeUnderOppfolging.capture());
        verify(brukerRepository, times(1)).deleteBrukerdataForPersonIds(captorIkkeUnderOppfolging.capture());
        verify(solrService, times(1)).commit();

        assertThat(captorDialogData.getValue()).isEqualTo(dialogdata);
        assertThat(captorPersonid.getValue()).isEqualTo(personId);

        assertLengthOfSublists(captorUnderOppfolging.getAllValues(), 1);
        assertLengthOfSublists(captorIkkeUnderOppfolging.getAllValues(), 0);
    }

    @Test
    public void skalSletteBrukereSomIkkeErUnderOppfolging() {
        PersonId personId = PersonId.of("0000");
        AktoerId aktoerId = AktoerId.of("1111");

        ArgumentCaptor<List<PersonId>> captorUnderOppfolging = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PersonId>> captorIkkeUnderOppfolging = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<DialogDataFraFeed> captorDialogData = ArgumentCaptor.forClass(DialogDataFraFeed.class);
        ArgumentCaptor<PersonId> captorPersonid = ArgumentCaptor.forClass(PersonId.class);

        DialogDataFraFeed dialogdata = new DialogDataFraFeed().setAktorId(aktoerId.toString());

        Map<AktoerId, Optional<PersonId>> identMap = new HashMap<>();
        identMap.put(aktoerId, Optional.of(personId));
        when(aktoerService.hentPersonidsForAktoerids(any())).thenReturn(identMap);

        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = new HashMap<>();
        oppfolgingstatus.put(personId, new Oppfolgingstatus().setOppfolgingsbruker(false));
        when(brukerRepository.retrieveOppfolgingstatus(anyList())).thenReturn(oppfolgingstatus);

        dialogDataFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(dialogdata));

        verify(dialogFeedRepository, times(1)).upsertDialogdata(captorDialogData.capture(), captorPersonid.capture());
        verify(solrService, times(1)).slettBrukere(captorIkkeUnderOppfolging.capture());
        verify(brukerRepository, times(1)).deleteBrukerdataForPersonIds(captorIkkeUnderOppfolging.capture());
        verify(solrService, times(1)).commit();

        assertThat(captorDialogData.getValue()).isEqualTo(dialogdata);
        assertThat(captorPersonid.getValue()).isEqualTo(personId);

        assertLengthOfSublists(captorUnderOppfolging.getAllValues(), 0);
        assertLengthOfSublists(captorIkkeUnderOppfolging.getAllValues(), 1);
    }

    private void assertLengthOfSublists(List<List<PersonId>> lists, int size) {
        lists.forEach(l -> assertThat(l.size()).isEqualTo(size));
    }
}