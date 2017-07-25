package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.service.AktivitetService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetFeedHandlerTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktivitetService aktivitetService;

    @InjectMocks
    private AktivitetFeedHandler aktivitetFeedHandler;


    @Test
    public void utledOgIndekserAktivitetstatuserForAktoeridShouldBeCalledOnceForEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("dontcare", data);

        ArgumentCaptor<String> aktoeridCaptor = ArgumentCaptor.forClass(String.class);

        verify(aktivitetService, times(2)).utledOgIndekserAktivitetstatuserForAktoerid(aktoeridCaptor.capture());
        List<String> capturedAktoerids = aktoeridCaptor.getAllValues();


        assertThat(capturedAktoerids).contains("AktoerID1");
        assertThat(capturedAktoerids).contains("AktoerID2");
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
}