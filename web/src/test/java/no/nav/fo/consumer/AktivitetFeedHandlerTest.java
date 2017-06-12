package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.Aktivitet.AktivitetData;
import no.nav.fo.domene.Aktivitet.AktivitetFullfortStatuser;
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
import java.util.Optional;

import static no.nav.fo.util.AktivitetUtils.erBrukersAktivitetAktiv;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
        List<AktivitetFullfortStatuser> fullfortStatuser = AktivitetData.fullførteStatuser;
        String ikkeFullfortStatus = "DenneErIkkeFullfort";


        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();
        List<String> statusliste = Arrays.asList(fullfortStatuser.get(0).toString(), ikkeFullfortStatus);

        assertThat(erBrukersAktivitetAktiv(statusliste)).isEqualTo(true);
    }

    @Test
    public void AktivitetSkalHaStatusFalse() {
        String ikkeFullfortStatus = "DenneErIkkeFullfort";

        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();

        List<String> statusliste = Arrays.asList(ikkeFullfortStatus);

        assertThat(erBrukersAktivitetAktiv(statusliste)).isEqualTo(false);
    }

    @Test
    public void getAktiviteterForAktoeridShouldBeCalledOnceForEachDistinctAktoerid() {

        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Optional.of("jegfantpersonid"));

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("dontcare", data);

        ArgumentCaptor<String> aktoeridCaptor = ArgumentCaptor.forClass(String.class);

        verify(brukerRepository, times(2)).getAktiviteterForAktoerid(aktoeridCaptor.capture());
        List<String> capturedAktoerids = aktoeridCaptor.getAllValues();


        assertThat(capturedAktoerids).contains("AktoerID1");
        assertThat(capturedAktoerids).contains("AktoerID2");
    }
}