package no.nav.pto.veilarbportefolje.feed.aktivitet;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatering;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetData.aktivitetTyperFraAktivitetsplanList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetFeedHandlerTest {

    private AktivitetService aktivitetService;
    private AktivitetDAO aktivitetDAO;

    @Mock
    private JdbcTemplate db;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private DataSource ds;

    @Mock
    private AktivitetService aktivitetServiceMock;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private PersistentOppdatering persistentOppdatering;

    @Mock
    private AktivitetDAO aktivitetDAOMock;

    @Mock
    private ElasticIndexer elasticIndexer;

    @Mock
    private AktoerService aktoerService;

    private AktivitetFeedHandler aktivitetFeedHandler;

    @Before
    public void resetMocks() {
        reset(brukerRepository, elasticIndexer, aktoerService, aktivitetDAOMock, persistentOppdatering, db, namedParameterJdbcTemplate, ds);
        aktivitetDAO = new AktivitetDAO(db, namedParameterJdbcTemplate);
        aktivitetService = new AktivitetService(aktoerService, aktivitetDAO, persistentOppdatering);
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetServiceMock, aktivitetDAOMock);
    }


    @Test
    public void utledOgIndekserAktivitetstatuserForAktoeridShouldBeCalledWithEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("1970-01-01T00:00:00Z", data);
        verify(aktivitetServiceMock, times(1)).utledOgIndekserAktivitetstatuserForAktoerid(eq(Arrays.asList(AktoerId.of("AktoerID1"), AktoerId.of("AktoerID2"))));
    }

    @Test
    public void upsertShouldBeCalledOnceForEachAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("1970-01-01T00:00:00Z", data);
        verify(aktivitetDAOMock, times(3)).upsertAktivitet(any(AktivitetDataFraFeed.class));
    }

    @Test
    public void skalOppdatereStatusForBrukerUtenAktiviteter() {
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetService, aktivitetDAO);
        ArgumentCaptor<List<BrukerOppdatering>> captor = ArgumentCaptor.forClass(List.class);

        when(aktoerService.hentPersonidFraAktoerid(any(AktoerId.class))).thenAnswer(invocation -> Try.success(PersonId.of(invocation.getArgument(0).toString())));

        aktivitetFeedHandler.behandleAktivitetdata(asList(AktoerId.of("a1"), AktoerId.of("a2")));
        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDBogIndekser(captor.capture());
        captor.getValue().forEach(value -> {
            assertThat(value.getAktiviteter().stream()
                    .map(AktivitetStatus::getAktivitetType)
                    .collect(toList()).size()).isEqualTo(aktivitetTyperFraAktivitetsplanList.size());
        });
    }

    @Test
    public void skalReturnereMedEnGangOmListenErTom() {
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetServiceMock, aktivitetDAO);
        aktivitetFeedHandler.behandleAktivitetdata(Collections.emptyList());
        verify(aktivitetServiceMock, never()).utledOgIndekserAktivitetstatuserForAktoerid(any());
    }
}
