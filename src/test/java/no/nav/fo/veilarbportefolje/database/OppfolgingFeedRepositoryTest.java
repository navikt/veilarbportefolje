package no.nav.fo.veilarbportefolje.database;

import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatertInformasjon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import io.vavr.control.Try;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.fo.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbportefolje.database.OppfolgingFeedRepository.safeToJaNei;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OppfolgingFeedRepositoryTest {

    private JdbcTemplate db;
    private DataSource ds;
    private OppfolgingFeedRepository oppfolgingFeedRepository;

    private static final String VEILEDER_ID = "X000000";
    private static final AktoerId AKTOR_ID = AktoerId.of("0000");

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        oppfolgingFeedRepository = new OppfolgingFeedRepository(db);

    }

    private BrukerOppdatertInformasjon brukerInfo(Timestamp tildeltTidspunkt) {
        return new BrukerOppdatertInformasjon()
                .setAktoerid(AKTOR_ID.toString())
                .setOppfolging(true)
                .setNyForVeileder(true)
                .setVeileder(VEILEDER_ID)
                .setEndretTimestamp(tildeltTidspunkt)
                .setManuell(true);
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

    @Test
    public void skalReturnereJaNeiStreng() {
        assertThat(safeToJaNei(true), is("J"));
        assertThat(safeToJaNei(false), is("N"));
        assertThat(safeToJaNei(null), is("N"));
    }
}
