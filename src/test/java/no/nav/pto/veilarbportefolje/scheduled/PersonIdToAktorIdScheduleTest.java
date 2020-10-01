package no.nav.pto.veilarbportefolje.scheduled;

import io.vavr.control.Try;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock.AKTOER_ID;
import static no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock.FNR;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersonIdToAktorIdScheduleTest {

    private JdbcTemplate db;
    private BrukerService brukerService;
    private PersonIdToAktorIdSchedule scheduler;

    private BrukerRepository brukerRepository;
    private AktorregisterClient aktorregisterClient;
    private static final String PERSON_ID = "111111";


    @Before
    public void setUp() {

        db = new JdbcTemplate(setupInMemoryDatabase());
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        aktorregisterClient = new AktorregisterClientMock();

        brukerRepository = new BrukerRepository(db);
        brukerService = new BrukerService(brukerRepository, aktorregisterClient);

        LeaderElectionClient leaderElectionClient = mock(LeaderElectionClient.class);
        scheduler = new PersonIdToAktorIdSchedule(brukerService, db, mock(ElasticIndexer.class), leaderElectionClient);

        when(leaderElectionClient.isLeader()).thenReturn(true);

    }

    @Test
    public void mapAktorId_skal_mappe_alle_aktorer_som_ikke_har_mapping() {

        insert(db, "OPPFOLGING_DATA")
                .value("AKTOERID", AKTOER_ID)
                .value("OPPFOLGING", "J")
                .execute();

        insert(db, "OPPFOLGINGSBRUKER")
                .value("FODSELSNR", FNR)
                .value("PERSON_ID", PERSON_ID)
                .execute();

        //assert no mappings exist
        assertThat(getMappedPersonidFromDb(AKTOER_ID).isFailure()).isTrue();

        scheduler.mapAktorId();

        Try<String> mappedPersonid = getMappedPersonidFromDb(AKTOER_ID);
        assertThat(mappedPersonid.get()).isEqualTo(PERSON_ID);

    }


    private Try<String> getMappedPersonidFromDb(String aktoerID) {
        return Try.of(() -> db.queryForObject(
                "SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE AKTOERID = ?",
                new Object[]{aktoerID},
                (rs, rowNum) -> rs.getString("PERSONID")));
    }

}
