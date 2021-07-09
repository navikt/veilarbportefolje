package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaAktivitetUtils;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArenaAktivitetUtilsTest {
    private JdbcTemplate db;
    private ArenaHendelseRepository arenaHendelseRepository;
    private final String hendelsesId = "T-123";

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        arenaHendelseRepository = new ArenaHendelseRepository(db);

        db.execute("DELETE FROM "+ PostgresTable.LEST_ARENA_HENDELSE.TABLE_NAME);
    }

    @Test
    public void skalLagreNyMeldinger(){
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(hendelsesId);
        boolean erGammelHendelse = ArenaAktivitetUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, 1L, GoldenGateOperations.INSERT);

        assertThat(hendelseIDB).isNull();
        assertThat(erGammelHendelse).isFalse();
    }

    @Test
    public void skalIkkeLagreGammelMelding(){
        long lagretHendelse = 3L;
        arenaHendelseRepository.upsertHendelse(hendelsesId, lagretHendelse);
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(hendelsesId);
        boolean erGammelHendelse = ArenaAktivitetUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, 1L, GoldenGateOperations.INSERT);

        assertThat(erGammelHendelse).isTrue();
        assertThat(hendelseIDB).isEqualTo(lagretHendelse);
    }

    @Test
    public void deleteMeldingerSkalLagresHvisHendelsesIDErLikSomIDB(){
        long lagretHendelse = 4L;
        arenaHendelseRepository.upsertHendelse(hendelsesId, lagretHendelse);
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(hendelsesId);
        boolean deleteHendelseErGammel = ArenaAktivitetUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, lagretHendelse, GoldenGateOperations.DELETE);
        boolean insertHendelseErGammel = ArenaAktivitetUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, lagretHendelse, GoldenGateOperations.INSERT);

        assertThat(deleteHendelseErGammel).isFalse();
        assertThat(insertHendelseErGammel).isTrue();
    }
}
