package no.nav.pto.veilarbportefolje.oppfolging;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository.safeToJaNei;
import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingRepositoryTest {

    private JdbcTemplate db;
    private OppfolgingRepository oppfolgingRepository;

    private static final String VEILEDER_ID = "X000000";
    private static final AktoerId AKTOR_ID = AktoerId.of("0000");

    @BeforeEach
    public void setup() {
        DataSource ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        oppfolgingRepository = new OppfolgingRepository(db);
    }

    @Test
    public void skal_inserte_oppfolging__i_oppfolging_data() {
        db.execute("TRUNCATE TABLE OPPFOLGING_DATA");
        Timestamp oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());
        BrukerOppdatertInformasjon bruker = testBrukerInfo(oppdatertIKildesystem);

        oppfolgingRepository.oppdaterOppfolgingData(bruker);

        Try<BrukerOppdatertInformasjon> eksisterendeData = oppfolgingRepository.retrieveOppfolgingData(AKTOR_ID);
        assertThat(eksisterendeData.isSuccess()).isTrue();
        assertDbValues(oppdatertIKildesystem, true, VEILEDER_ID, eksisterendeData.get());
    }


    @Test
    public void skal_upserte_oppfolging_i_oppfolging_data() {
        db.execute("TRUNCATE TABLE OPPFOLGING_DATA");
        Timestamp oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());

        BrukerOppdatertInformasjon bruker = testBrukerInfo(oppdatertIKildesystem);

        oppfolgingRepository.oppdaterOppfolgingData(bruker);

        Try<BrukerOppdatertInformasjon> eksisterendeData = oppfolgingRepository.retrieveOppfolgingData(AKTOR_ID);
        assertDbValues(oppdatertIKildesystem, true, VEILEDER_ID, eksisterendeData.get());

        oppdatertIKildesystem = new Timestamp(Instant.now().toEpochMilli());
        bruker.setEndretTimestamp(oppdatertIKildesystem);
        bruker.setVeileder("");
        bruker.setOppfolging(false);
        oppfolgingRepository.oppdaterOppfolgingData(bruker);

        eksisterendeData = oppfolgingRepository.retrieveOppfolgingData(AKTOR_ID);
        assertDbValues(oppdatertIKildesystem, false, "", eksisterendeData.get());

    }

    @Test
    public void skal_returnere_ja_nei_streng() {
        assertThat(safeToJaNei(true)).isEqualTo("J");
        assertThat(safeToJaNei(false)).isEqualTo("N");
        assertThat(safeToJaNei(null)).isEqualTo("N");
    }

    private void assertDbValues(
            Timestamp oppdatertIKildesystem,
            boolean oppfolging,
            String veilederId,
            BrukerOppdatertInformasjon infoFraDb) {

        assertThat(infoFraDb.getAktoerid()).isEqualTo(AKTOR_ID.toString());
        assertThat(infoFraDb.getOppfolging()).isEqualTo(oppfolging);
        assertThat(infoFraDb.getEndretTimestamp()).isEqualTo(oppdatertIKildesystem);
        assertThat(infoFraDb.getVeileder()).isEqualTo(veilederId);
    }

    private static BrukerOppdatertInformasjon testBrukerInfo(Timestamp tildeltTidspunkt) {
        return new BrukerOppdatertInformasjon()
                .setAktoerid(AKTOR_ID.toString())
                .setOppfolging(true)
                .setNyForVeileder(true)
                .setVeileder(VEILEDER_ID)
                .setStartDato(Timestamp.from(Instant.now()))
                .setEndretTimestamp(tildeltTidspunkt)
                .setManuell(true);
    }

}
