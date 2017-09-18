package no.nav.fo.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static no.nav.fo.util.DbUtils.toSet;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class PersistentOppdateringTest {

    @Mock
    private BrukerRepository brukerRepository;

    @InjectMocks
    private PersistentOppdatering persistentOppdatering;

    @Test
    public void lagreAktivitetStatuserTest() {
        PersonId personId = PersonId.of("111111");
        AktoerId aktoerId = AktoerId.of("222222");

        AktivitetStatus a1 = AktivitetStatus.of(personId, aktoerId, "aktivitetstype1", true, new Timestamp(0));
        AktivitetStatus a2 = AktivitetStatus.of(personId, aktoerId, "aktivitetstype1", false, new Timestamp(0));


        Map<PersonId, Set<AktivitetStatus>> returnStatuser = new HashMap<>();
        returnStatuser.put(personId, toSet(a1));
        when(brukerRepository.getAktivitetstatusForBrukere(any())).thenReturn(returnStatuser);

        persistentOppdatering.lagreAktivitetstatuser(asList(a1,a2));

        ArgumentCaptor<List<AktivitetStatus>> statuserCaptor = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<List<Tuple2<PersonId,String>>> finnesIDbCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(brukerRepository, times(1)).insertOrUpdateAktivitetStatus(statuserCaptor.capture(), finnesIDbCaptor.capture());
        assertThat(finnesIDbCaptor.getValue()).isEqualTo(Collections.singletonList(Tuple.of(personId, "aktivitetstype1")));

    }
}