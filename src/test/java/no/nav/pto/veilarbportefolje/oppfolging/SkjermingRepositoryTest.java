package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.oppfolging.response.SkjermingData;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Optional;

public class SkjermingRepositoryTest {
    private SkjermingRepository skjermingRepository;

    @BeforeEach
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        db.execute("TRUNCATE " + PostgresTable.NOM_SKJERMING.TABLE_NAME);
        skjermingRepository = new SkjermingRepository(db);
    }

    @Test
    public void testSavingSkjermingStatus() {
        skjermingRepository.settSkjerming(Fnr.of("fnr123"), true);
        Optional<SkjermingData> skjermingDataOptional = skjermingRepository.hentSkjermingData(Fnr.of("fnr123"));

        Assertions.assertTrue(skjermingDataOptional.isPresent());
        Assertions.assertTrue(skjermingDataOptional.get().isEr_skjermet());

        skjermingRepository.settSkjerming(Fnr.of("fnr123"), false);
        skjermingDataOptional = skjermingRepository.hentSkjermingData(Fnr.of("fnr123"));
        Assertions.assertTrue(skjermingDataOptional.isPresent());
        Assertions.assertFalse(skjermingDataOptional.get().isEr_skjermet());
    }

    @Test
    public void testSavingSkjermingPeriode() {
        skjermingRepository.settSkjermingPeriode(Fnr.of("fnr123"), Timestamp.valueOf("2022-02-21 13:14:00"), null);
        Optional<SkjermingData> skjermingDataOptional = skjermingRepository.hentSkjermingData(Fnr.of("fnr123"));

        Assertions.assertTrue(skjermingDataOptional.isPresent());
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_fra(), Timestamp.valueOf("2022-02-21 13:14:00"));
        Assertions.assertNull(skjermingDataOptional.get().getSkjermet_til());

        skjermingRepository.settSkjermingPeriode(Fnr.of("fnr123"), Timestamp.valueOf("2022-02-21 13:14:00"), Timestamp.valueOf("2022-04-21 13:14:00"));
        skjermingDataOptional = skjermingRepository.hentSkjermingData(Fnr.of("fnr123"));
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_fra(), Timestamp.valueOf("2022-02-21 13:14:00"));
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_til(), Timestamp.valueOf("2022-04-21 13:14:00"));

    }

}