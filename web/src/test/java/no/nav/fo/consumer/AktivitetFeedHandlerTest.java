package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetFeedHandlerTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktivitetService aktivitetService;

    @Mock
    private SolrService solrService;

    @Mock
    private AktoerService aktoerService;

    @InjectMocks
    private AktivitetFeedHandler aktivitetFeedHandler;

    @Before
    public void resetMocks() {
        reset(brukerRepository, aktivitetService, solrService, aktoerService);
    }


    @Test
    public void utledOgIndekserAktivitetstatuserForAktoeridShouldBeCalledOnceForEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(PersonId.of("123123")));
        when(brukerRepository.retrieveOppfolgingstatus(any())).thenReturn(Try.success(new Oppfolgingstatus().setOppfolgingsbruker(true)));

        aktivitetFeedHandler.call("dontcare", data);

        ArgumentCaptor<AktoerId> aktoeridCaptor = ArgumentCaptor.forClass(AktoerId.class);

        verify(aktivitetService, times(2)).utledOgIndekserAktivitetstatuserForAktoerid(aktoeridCaptor.capture());
        List<AktoerId> capturedAktoerids = aktoeridCaptor.getAllValues();


        assertThat(capturedAktoerids).contains(AktoerId.of("AktoerID1"));
        assertThat(capturedAktoerids).contains(AktoerId.of("AktoerID2"));
    }

    @Test
    public void upsertShouldBeCalledOnceForEachAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("dontcare", data);

        verify(brukerRepository, times(3)).upsertAktivitet(any(AktivitetDataFraFeed.class));
    }

    @Test
    public void skalIkkeIndeksereOmBrukerIkkeErUnderOppfolging() {

        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(PersonId.of("123123")));
        when(brukerRepository.retrieveOppfolgingstatus(any())).thenReturn(Try.success(new Oppfolgingstatus().setOppfolgingsbruker(false)));

        aktivitetFeedHandler.call("dontcare", Collections.singletonList(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true)));

        verify(aktivitetService, never()).utledOgIndekserAktivitetstatuserForAktoerid(any());
        verify(solrService, times(1)).slettBruker(any(PersonId.class));

    }
}