package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.service.AktoerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static no.nav.fo.util.AktivitetUtils.erBrukersAktivitetAktiv;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetFeedHandlerTest {

    @Mock
    private BrukerRepository brukerRepository;
    @Mock
    private AktoerService aktoerService;
    @Mock
    private PersistentOppdatering persistentOppdatering;

    @InjectMocks
    private AktivitetFeedHandler aktivitetFeedHandler;

    @Test
    public void AktivitetSkalHaStatusTrue() {
        List<String> fullførteStatuser = Arrays.asList( new String[]{"FULLFORT1", "FULLFORT2"});
        List<String> statusliste = Arrays.asList( new String[]{"FULLFORT1", "IKKEFULLFORT1"});

        assertThat(erBrukersAktivitetAktiv(statusliste, fullførteStatuser)).isEqualTo(true);
    }

    @Test
    public void AktivitetSkalHaStatusFalse() {
        List<String> fullførteStatuser = Arrays.asList( new String[]{"FULLFORT1", "FULLFORT2"});
        List<String> statusliste = Arrays.asList( new String[]{"FULLFORT1", "FULLFORT2"});

        assertThat(erBrukersAktivitetAktiv(statusliste, fullførteStatuser)).isEqualTo(false);
    }

    @Test
    public void getAktiviteterForAktoeridShouldBeCalledOnceForEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1"));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1"));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2"));

        aktivitetFeedHandler.call("dontcare", data);

        ArgumentCaptor<String> aktoeridCaptor = ArgumentCaptor.forClass(String.class);

        verify(brukerRepository, times(2)).getAktiviteterForAktoerid(aktoeridCaptor.capture());
        List<String> capturedAktoerids = aktoeridCaptor.getAllValues();


        assertThat(capturedAktoerids).contains("AktoerID1");
        assertThat(capturedAktoerids).contains("AktoerID2");

    }


}