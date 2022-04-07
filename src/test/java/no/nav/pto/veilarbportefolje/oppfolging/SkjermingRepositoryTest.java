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
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Fnr fnr = Fnr.of("fnr123");

        skjermingRepository.settSkjerming(fnr, true);
        Set<Fnr> skjermingDataOptional = skjermingRepository.hentSkjermetPersoner(List.of(fnr));

        Assertions.assertTrue(skjermingDataOptional.contains(fnr));

        skjermingRepository.settSkjerming(fnr, false);
        skjermingDataOptional = skjermingRepository.hentSkjermetPersoner(List.of(fnr));
        Assertions.assertFalse(skjermingDataOptional.contains(fnr));
    }

    @Test
    public void testSavingSkjermingPeriode() {
        Fnr fnr = Fnr.of("fnr123");

        skjermingRepository.settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-21 13:14:00"), null);
        Optional<SkjermingData> skjermingDataOptional = skjermingRepository.hentSkjermingData(fnr);

        Assertions.assertTrue(skjermingDataOptional.isPresent());
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_fra(), Timestamp.valueOf("2022-02-21 13:14:00"));
        Assertions.assertNull(skjermingDataOptional.get().getSkjermet_til());

        skjermingRepository.settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-21 13:14:00"), Timestamp.valueOf("2022-04-21 13:14:00"));
        skjermingDataOptional = skjermingRepository.hentSkjermingData(fnr);
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_fra(), Timestamp.valueOf("2022-02-21 13:14:00"));
        Assertions.assertEquals(skjermingDataOptional.get().getSkjermet_til(), Timestamp.valueOf("2022-04-21 13:14:00"));

    }

    @Test
    public void testDeleteOfSkjermingData() {
        Fnr fnr = Fnr.of("fnr123");

        skjermingRepository.settSkjerming(fnr, true);
        Set<Fnr> skjermingDataOptional = skjermingRepository.hentSkjermetPersoner(List.of(fnr));

        Assertions.assertTrue(skjermingDataOptional.contains(fnr));

        skjermingRepository.deleteSkjermingData(fnr);
        skjermingDataOptional = skjermingRepository.hentSkjermetPersoner(List.of(fnr));
        Assertions.assertFalse(skjermingDataOptional.contains(fnr));
    }

    @Test
    public void testHentingAvSkjermingData() {
        Fnr fnr1 = Fnr.of("fnr123");
        Fnr fnr2 = Fnr.of("fnr124");
        Fnr fnr3 = Fnr.of("fnr125");

        skjermingRepository.settSkjerming(fnr1, true);
        skjermingRepository.settSkjerming(fnr2, true);
        skjermingRepository.settSkjerming(fnr3, false);

        Set<Fnr> fnrSkjermingOptional = skjermingRepository.hentSkjermetPersoner(List.of(fnr1, fnr2, fnr3));
        Assertions.assertTrue(fnrSkjermingOptional.contains(fnr1));
        Assertions.assertTrue(fnrSkjermingOptional.contains(fnr2));
        Assertions.assertFalse(fnrSkjermingOptional.contains(fnr3));
    }

}