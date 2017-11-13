package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
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
public class OppfolgingFeedHandlerTest {

    @Mock
    private OppfolgingFeedRepository oppfolgingFeedRepository;

    @Mock
    private ArbeidslisteService arbeidslisteService;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktoerService aktoerService;

    @Mock
    private SolrService solrService;

    @InjectMocks
    private OppfolgingFeedHandler oppfolgingFeedHandler;

    @Before
    public void resetMocks() {
        reset(arbeidslisteService,brukerRepository,aktoerService,solrService);
    }

    @Test
    public void skalSletteBrukerOgArbeidslisteNaarDenIkkeErUnderOppfolging() {
        AktoerId aktoerId = AktoerId.of("DUMMY");
        PersonId personId = PersonId.of("1111111");

        ArgumentCaptor<List<PersonId>> captorUnderOppfolging = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PersonId>> captorIkkeUnderOppfolging = ArgumentCaptor.forClass(List.class);


        Oppfolgingstatus arenaStatus = new Oppfolgingstatus()
                .setFormidlingsgruppekode("DUMMY")
                .setServicegruppekode("DUMMY");

        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon()
                .setOppfolging(false)
                .setAktoerid(aktoerId.toString());

        Map<PersonId, Oppfolgingstatus> oppfolgingsstatus = new HashMap<>();
        oppfolgingsstatus.put(personId, arenaStatus);
        when(brukerRepository.retrieveOppfolgingstatus(anyList())).thenReturn(Try.success(oppfolgingsstatus));

        Map<AktoerId, Optional<PersonId>> identMap = new HashMap<>();
        identMap.put(aktoerId, Optional.of(personId));
        when(aktoerService.hentPersonidsForAktoerids(any())).thenReturn(identMap);

        oppfolgingFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(bruker));

        verify(solrService, times(1)).populerIndeksForPersonids(captorUnderOppfolging.capture());
        verify(brukerRepository, times(1)).deleteBrukerdataForPersonIds(captorIkkeUnderOppfolging.capture());
        verify(solrService, times(1)).slettBrukere(captorIkkeUnderOppfolging.capture());
        verify(solrService, times(1)).commit();

        assertLengthOfSublists(captorUnderOppfolging.getAllValues(), 0);
        assertLengthOfSublists(captorIkkeUnderOppfolging.getAllValues(), 1);
    }

    private void assertLengthOfSublists(List<List<PersonId>> lists, int size) {
        lists.forEach(l -> assertThat(l.size()).isEqualTo(size));
    }
}