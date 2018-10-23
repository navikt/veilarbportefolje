package no.nav.fo.veilarbportefolje.consumer;

import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.database.PersistentOppdatering;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.veilarbportefolje.service.AktivitetService;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.vavr.control.Try;

import javax.sql.DataSource;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.domene.aktivitet.AktivitetData.aktivitetTyperFraAktivitetsplanList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
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
    private SolrService solrService;

    @Mock
    private AktoerService aktoerService;

    private AktivitetFeedHandler aktivitetFeedHandler;

    @Before
    public void resetMocks() {
        reset(brukerRepository, solrService, aktoerService, aktivitetDAOMock, persistentOppdatering, db, namedParameterJdbcTemplate, ds);
        aktivitetDAO = new AktivitetDAO(db, namedParameterJdbcTemplate, ds);
        aktivitetService = new AktivitetService(aktoerService, aktivitetDAO, persistentOppdatering);
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetServiceMock, aktivitetDAOMock);
    }


    @Test
    public void utledOgIndekserAktivitetstatuserForAktoeridShouldBeCalledWithEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("dontcare", data);

        verify(aktivitetServiceMock, times(1)).utledOgIndekserAktivitetstatuserForAktoerid(eq(Arrays.asList(AktoerId.of("AktoerID1"), AktoerId.of("AktoerID2"))));
    }

    @Test
    public void upsertShouldBeCalledOnceForEachAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        aktivitetFeedHandler.call("dontcare", data);

        verify(aktivitetDAOMock, times(3)).upsertAktivitet(any(AktivitetDataFraFeed.class));
    }

    @Test
    public void skalOppdatereStatusForBrukerUtenAktiviteter() {
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetService, aktivitetDAO);
        ArgumentCaptor<List<BrukerOppdatering>> captor = ArgumentCaptor.forClass(List.class);

        when(aktoerService.hentPersonidFraAktoerid(any(AktoerId.class))).thenAnswer(invocation -> Try.success(PersonId.of(invocation.getArgument(0).toString())));

        aktivitetFeedHandler.behandleAktivitetdata(asList(AktoerId.of("a1"), AktoerId.of("a2")));
        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDBogIndekser(captor.capture());
        captor.getValue().forEach( value -> {
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
