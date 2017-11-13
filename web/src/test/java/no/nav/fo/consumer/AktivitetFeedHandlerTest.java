package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.*;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperFraAktivitetsplanList;
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
        aktivitetDAO = new AktivitetDAO(db, namedParameterJdbcTemplate, ds);
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetServiceMock, aktoerService, solrService, aktivitetDAOMock);
    }


    @Test
    public void utledOgIndekserAktivitetstatuserForAktoeridShouldBeCalledWithEachDistinctAktoerid() {

        List<AktivitetDataFraFeed> data = new ArrayList<>();
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID1").setAvtalt(true));
        data.add(new AktivitetDataFraFeed().setAktorId("AktoerID2").setAvtalt(true));

        Map<AktoerId, Optional<PersonId>> idMap = new HashMap<>();
        idMap.put(AktoerId.of("AktoerID1"), Optional.of(PersonId.of("123123")));
        idMap.put(AktoerId.of("AktoerID2"), Optional.of(PersonId.of("123123")));

        when(aktoerService.hentPersonidsForAktoerids(any())).thenReturn(idMap);
        when(brukerRepository.retrieveOppfolgingstatus(anyList())).thenAnswer( (r) -> {
            Map<PersonId, Oppfolgingstatus> statuser = new HashMap<>();
            List<PersonId> personIds = r.getArgument(0);
            personIds.forEach( (personid) -> {
                statuser.put(personid,new Oppfolgingstatus().setOppfolgingsbruker(true));
            });
            return Try.success(statuser);
        });

        aktivitetFeedHandler.call("dontcare", data);

        ArgumentCaptor<List<AktoerId>> aktoeridCaptor = ArgumentCaptor.forClass(List.class);

        verify(aktivitetServiceMock, times(1)).utledOgIndekserAktivitetstatuserForAktoerid(aktoeridCaptor.capture());
        List<AktoerId> capturedAktoerids = aktoeridCaptor.getValue();


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

        verify(aktivitetDAOMock, times(3)).upsertAktivitet(any(AktivitetDataFraFeed.class));
    }

    @Test
    public void skalOppdatereStatusForBrukerUtenAktiviteter() {
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetService, aktoerService, solrService, aktivitetDAO);
        ArgumentCaptor<List<BrukerOppdatering>> captor = ArgumentCaptor.forClass(List.class);


        when(aktoerService.hentPersonidsForAktoerids(any())).thenAnswer(invokation -> {
            Map<AktoerId, Optional<PersonId>> aktoeridToPersonid = new HashMap<>();
            ((List) invokation.getArgument(0)).forEach( arg -> aktoeridToPersonid.put((AktoerId) arg, Optional.of(PersonId.of(arg.toString()))));
            return aktoeridToPersonid;
        });
        when(brukerRepository.retrieveOppfolgingstatus(anyList())).thenAnswer(invokation -> {
            Map<PersonId, Oppfolgingstatus> oppfolgingstatus = new HashMap<>();
            ((List) invokation.getArgument(0)).forEach(arg -> oppfolgingstatus.put((PersonId) arg, new Oppfolgingstatus().setOppfolgingsbruker(true)));
            return Try.success(oppfolgingstatus);
        });
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
        aktivitetFeedHandler = new AktivitetFeedHandler(brukerRepository, aktivitetService, aktoerService, solrService, aktivitetDAO);
        aktivitetFeedHandler.behandleAktivitetdata(Collections.emptyList());
        verify(aktoerService, never()).hentPersonidsForAktoerids(any());
    }
}