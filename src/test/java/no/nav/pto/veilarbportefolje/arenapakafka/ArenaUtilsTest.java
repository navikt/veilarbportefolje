package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArenaUtilsTest {
    private JdbcTemplate db;
    private ArenaHendelseRepository arenaHendelseRepository;
    private final String hendelsesId = "T-123";

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        arenaHendelseRepository = new ArenaHendelseRepository(db);

        db.execute("DELETE FROM "+ PostgresTable.LEST_ARENA_HENDELSE_AKTIVITETER.TABLE_NAME);
    }

    @Test
    public void skalLagreNyMeldinger(){
        Long hendelseIDB = arenaHendelseRepository.retrieveAktivitetHendelse(hendelsesId);
        boolean erGammelHendelse = ArenaUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, 1L,false);

        assertThat(hendelseIDB).isNull();
        assertThat(erGammelHendelse).isFalse();
    }

    @Test
    public void skalIkkeLagreGammelMelding(){
        long lagretHendelse = 3L;
        arenaHendelseRepository.upsertAktivitetHendelse(hendelsesId, lagretHendelse);
        Long hendelseIDB = arenaHendelseRepository.retrieveAktivitetHendelse(hendelsesId);
        boolean erGammelHendelse = ArenaUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, 1L, false);

        assertThat(erGammelHendelse).isTrue();
        assertThat(hendelseIDB).isEqualTo(lagretHendelse);
    }

    @Test
    public void deleteMeldingerSkalLagresHvisHendelsesIDErLikSomIDB(){
        long lagretHendelse = 4L;
        arenaHendelseRepository.upsertAktivitetHendelse(hendelsesId, lagretHendelse);
        Long hendelseIDB = arenaHendelseRepository.retrieveAktivitetHendelse(hendelsesId);
        boolean deleteHendelseErGammel = ArenaUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, lagretHendelse, true);
        boolean insertHendelseErGammel = ArenaUtils.erGammelHendelseBasertPaOperasjon(hendelseIDB, lagretHendelse, false);

        assertThat(deleteHendelseErGammel).isFalse();
        assertThat(insertHendelseErGammel).isTrue();
    }

    @Test
    public void testArenaDato(){
        ZonedDateTime dato1 = ZonedDateTime.parse("2023-05-25T23:00:00+02:00");
        ZonedDateTime dato2 = ZonedDateTime.parse("2023-10-03T23:59:59+02:00");
        ZonedDateTime dato3 = ZonedDateTime.parse("2023-05-25T23:00:00+00:00");
        ZonedDateTime dato4 = ZonedDateTime.parse("2023-10-03T23:59:59+00:00");

        Assertions.assertThat(ArenaDato.of(dato1).getDato()).isEqualTo(new ArenaDato("2023-05-25").getDato());
        Assertions.assertThat(ArenaDato.of(dato2).getDato()).isEqualTo(new ArenaDato("2023-10-03").getDato());
        Assertions.assertThat(ArenaDato.of(dato3).getDato()).isEqualTo(new ArenaDato("2023-05-25").getDato());
        Assertions.assertThat(ArenaDato.of(dato4).getDato()).isEqualTo(new ArenaDato("2023-10-03").getDato());
    }

}
