package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.util.Optional;

public class SkjermingServiceTest {
    private SkjermingService skjermingService;
    private SkjermingRepository skjermingRepository;

    @BeforeEach
    public void setUp() {
        skjermingRepository = Mockito.mock(SkjermingRepository.class);
        BrukerServiceV2 brukerServiceV2 = Mockito.mock(BrukerServiceV2.class);
        Mockito.when(brukerServiceV2.hentAktorId(Mockito.any())).thenReturn(Optional.of(AktorId.of("1111")));
        OpensearchIndexerV2 opensearchIndexerV2 = Mockito.mock(OpensearchIndexerV2.class);
        skjermingService = new SkjermingService(skjermingRepository, brukerServiceV2, opensearchIndexerV2);

    }

    @Test
    public void testSavingSkjermingStatus() {
        Fnr fnr = Fnr.of("fnr123");
        ConsumerRecord consumerRecord = new ConsumerRecord("topic", 1, 2, fnr.get(), "true");
        skjermingService.behandleSkjermingStatus(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjerming(fnr, true);

        consumerRecord = new ConsumerRecord("topic", 1, 2, fnr.get(), "false");
        skjermingService.behandleSkjermingStatus(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).deleteSkjermingData(fnr);
    }

    @Test
    public void testSavingSkjermingPersoner() {
        Fnr fnr = Fnr.of("fnr123");

        ConsumerRecord consumerRecord = new ConsumerRecord("topic", 1, 2, fnr.get(), new SkjermingDTO(new Integer[]{2022, 02, 22, 13, 14, 00}, null));
        skjermingService.behandleSkjermedePersoner(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-22 13:14:00"), null);

        consumerRecord = new ConsumerRecord("topic", 1, 2, fnr.get(), new SkjermingDTO(new Integer[]{2022, 02, 22, 13, 14, 00}, new Integer[]{2022, 04, 22, 13, 14, 00}));
        skjermingService.behandleSkjermedePersoner(consumerRecord);

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-22 13:14:00"), Timestamp.valueOf("2022-04-22 13:14:00"));
    }
}