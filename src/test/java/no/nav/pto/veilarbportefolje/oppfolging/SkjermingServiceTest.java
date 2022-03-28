package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.Fnr;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Timestamp;

public class SkjermingServiceTest {
    private SkjermingService skjermingService;
    private SkjermingRepository skjermingRepository;

    @BeforeEach
    public void setUp() {
        skjermingRepository = Mockito.mock(SkjermingRepository.class);
        skjermingService = new SkjermingService(skjermingRepository);
    }

    @Test
    public void testSavingSkjermingStatus() {
        ConsumerRecord consumerRecord = new ConsumerRecord("topic", 1, 2, "fnr123", "true");
        skjermingService.behandleSkjermingStatus(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjerming(Fnr.of("fnr123"), true);

        consumerRecord = new ConsumerRecord("topic", 1, 2, "fnr123", "false");
        skjermingService.behandleSkjermingStatus(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjerming(Fnr.of("fnr123"), false);
    }

    @Test
    public void testSavingSkjermingPersoner() {
        ConsumerRecord consumerRecord = new ConsumerRecord("topic", 1, 2, "fnr123", new SkjermingDTO(new Integer[]{2022, 02, 22, 13, 14, 00}, null));
        skjermingService.behandleSkjermedePersoner(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjermingPeriode(Fnr.of("fnr123"), Timestamp.valueOf("2022-02-22 13:14:00"), null);

        consumerRecord = new ConsumerRecord("topic", 1, 2, "fnr123", new SkjermingDTO(new Integer[]{2022, 02, 22, 13, 14, 00}, new Integer[]{2022, 04, 22, 13, 14, 00}));
        skjermingService.behandleSkjermedePersoner(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjermingPeriode(Fnr.of("fnr123"), Timestamp.valueOf("2022-02-22 13:14:00"), Timestamp.valueOf("2022-04-22 13:14:00"));
    }
}