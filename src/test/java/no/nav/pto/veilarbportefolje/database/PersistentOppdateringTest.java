package no.nav.pto.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.util.DbUtils.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class PersistentOppdateringTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktivitetDAO aktivitetDAO;


    @InjectMocks
    private PersistentOppdatering persistentOppdatering;

    @Test
    public void lagreAktivitetStatuserTest() {
        PersonId personId = PersonId.of("111111");
        AktorId aktoerId = AktorId.of("222222");

        AktivitetStatus a1 = new AktivitetStatus()
                .setPersonid(personId)
                .setAktoerid(aktoerId)
                .setAktivitetType("aktivitetstype1")
                .setAktiv(true)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop(new Timestamp(0));

        AktivitetStatus a2 = new AktivitetStatus()
                .setPersonid(personId)
                .setAktoerid(aktoerId)
                .setAktivitetType("aktivitetstype1")
                .setAktiv(false)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop(new Timestamp(0));


        Map<PersonId, Set<AktivitetStatus>> returnStatuser = new HashMap<>();
        returnStatuser.put(personId, toSet(a1));
        when(aktivitetDAO.getAktivitetstatusForBrukere(any())).thenReturn(returnStatuser);

        persistentOppdatering.lagreAktivitetstatuser(asList(a1, a2));

        ArgumentCaptor<List<AktivitetStatus>> statuserCaptor = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<List<Tuple2<PersonId, String>>> finnesIDbCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(aktivitetDAO, times(1)).insertOrUpdateAktivitetStatus(statuserCaptor.capture(), finnesIDbCaptor.capture());
        assertThat(finnesIDbCaptor.getValue()).isEqualTo(Collections.singletonList(Tuple.of(personId, "aktivitetstype1")));

    }
}
