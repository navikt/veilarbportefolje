package no.nav.fo.database;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.vavr.control.Try;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OppfolgingFeedRepositoryTest {

    private JdbcTemplate db;
    private DataSource ds;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private BrukerRepository brukerRepository;

    private static final String VEILEDER_ID = "X000000";
    private static final PersonId PERSON_ID = PersonId.of("1111");
    private static final AktoerId AKTOR_ID = AktoerId.of("0000");

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        oppfolgingFeedRepository = new OppfolgingFeedRepository(db);
        brukerRepository = new BrukerRepository(db,ds,namedParameterJdbcTemplate);

    }

    @Test
    public void skalInserteOppfolgingsdata_i_bruker_data() {
        db.execute("TRUNCATE TABLE BRUKER_DATA");
        Timestamp tildeltTidspunkt = new Timestamp(Instant.now().toEpochMilli());

        BrukerOppdatertInformasjon bruker = brukerInfo(tildeltTidspunkt);
        
        oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker, PERSON_ID);
        Brukerdata brukerdata = brukerRepository.retrieveBrukerdata(Collections.singletonList(PERSON_ID.toString())).get(0);
        assertThat(brukerdata.getVeileder(), is(VEILEDER_ID));
    }

    private BrukerOppdatertInformasjon brukerInfo(Timestamp tildeltTidspunkt) {
        return new BrukerOppdatertInformasjon()
                .setAktoerid(AKTOR_ID.toString())
                .setOppfolging(true)
                .setNyForVeileder(true)
                .setVeileder(VEILEDER_ID)
                .setEndretTimestamp(tildeltTidspunkt);
    }

    @Test
    public void skalUpserteOppfolgingsdata_i_bruker_data() {
        db.execute("TRUNCATE TABLE BRUKER_DATA");
        Timestamp tildeltTidspunkt = new Timestamp(Instant.now().toEpochMilli());

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid(PERSON_ID.toString())
                .setVeileder("Z000000")
                .setAktoerid(AKTOR_ID.toString())
                .setTildeltTidspunkt(tildeltTidspunkt)
                .setNyesteUtlopteAktivitet(new Timestamp(0))
                .setAktivitetStart(new Timestamp(1))
                .setNesteAktivitetStart(new Timestamp(2))
                .setForrigeAktivitetStart(new Timestamp(3))
                .setOppfolging(false);

        brukerRepository.insertOrUpdateBrukerdata(Collections.singletonList(brukerdata), Collections.emptyList());

        BrukerOppdatertInformasjon bruker = brukerInfo(tildeltTidspunkt);

        oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker, PERSON_ID);

        Brukerdata oppdatertBrukerdata = brukerRepository.retrieveBrukerdata(Collections.singletonList(PERSON_ID.toString())).get(0);
        assertThat(oppdatertBrukerdata, is(brukerdata.setVeileder(VEILEDER_ID).setOppfolging(true).setNyForVeileder(true)));

    }

    @Test
    public void skalInserteOppfolgingsdata_i_oppfolging_data() {
        db.execute("TRUNCATE TABLE OPPFOLGING_DATA");
        Timestamp oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());
        BrukerOppdatertInformasjon bruker = brukerInfo(oppdatertIKildesystem);

        oppfolgingFeedRepository.oppdaterOppfolgingData(bruker);

        Try<BrukerOppdatertInformasjon> eksisterendeData = oppfolgingFeedRepository.retrieveOppfolgingData(AKTOR_ID.toString());
        assertThat(eksisterendeData.isSuccess(), is(true));
        assertDbValues(oppdatertIKildesystem, true, VEILEDER_ID, eksisterendeData.get());
    }

    private void assertDbValues(
            Timestamp oppdatertIKildesystem, 
            boolean oppfolging, 
            String veilederId,
            BrukerOppdatertInformasjon infoFraDb) {
        assertThat(infoFraDb.getAktoerid(), is(AKTOR_ID.toString()));
        assertThat(infoFraDb.getOppfolging(), is(oppfolging));
        assertThat(infoFraDb.getEndretTimestamp(), is(oppdatertIKildesystem));
        assertThat(infoFraDb.getVeileder(), is(veilederId));
    }

    @Test
    public void skalUpserteOppfolgingsdata_i_oppfolging_data() {
        db.execute("TRUNCATE TABLE OPPFOLGING_DATA");
        Timestamp oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());

        BrukerOppdatertInformasjon bruker = brukerInfo(oppdatertIKildesystem);

        oppfolgingFeedRepository.oppdaterOppfolgingData(bruker);

        Try<BrukerOppdatertInformasjon> eksisterendeData = oppfolgingFeedRepository.retrieveOppfolgingData(AKTOR_ID.toString());
        assertDbValues(oppdatertIKildesystem, true, VEILEDER_ID, eksisterendeData.get());

        oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());
        bruker.setEndretTimestamp(oppdatertIKildesystem);
        bruker.setVeileder("");
        bruker.setOppfolging(false);
        oppfolgingFeedRepository.oppdaterOppfolgingData(bruker);

        eksisterendeData = oppfolgingFeedRepository.retrieveOppfolgingData(AKTOR_ID.toString());
        assertDbValues(oppdatertIKildesystem, false, "", eksisterendeData.get());

    }

}